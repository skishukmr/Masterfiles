/*
    Copyright (c) 1996-2007 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/procure/15.28.1+/ariba/procure/core/nametable/CommodityCodeConditionBasedNameTable.java#1 $

    Responsible: achaudhry
*/

package ariba.procure.core.nametable;

import ariba.common.core.nametable.ConditionBasedNameTable;
import ariba.common.core.CommodityExportMapEntry;
import ariba.common.core.Log;
import ariba.basic.core.CommodityCode;
import ariba.basic.util.BaseUtil;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.fields.ValueSource;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureUtil;
import java.util.List;


/**
    It supports making lookup of CommodityExportMapEntry for  a commodity
    hierarchical. This is required since a super commodity of a commodity may
    be mapped to a Partitioned Commodity Code in the CommodityExportMapEntry

    @aribaapi private
*/
public class CommodityCodeConditionBasedNameTable extends
    ConditionBasedNameTable
{
    /**
        Calls the corresponding condition to build constraints for this nametable.

        @param q Query to add the constraints to
        @param field The field we are querying on.
        @param pattern The pattern that we want to query for.

        @aribaapi documented
    */
    public void addQueryConstraints (AQLQuery q, String field, String pattern)
    {
        super.addQueryConstraints(q, field, pattern);
        ValueSource ctxt = getValueSourceContext();
        Assert.that(ctxt instanceof ProcureLineItem,
            "CommodityCodeConditionBasedNameTable only works with " +
            "ProcureLineItem and subclasses");
        ProcureLineItem pli = (ProcureLineItem)ctxt;
        if (pli.getDescription() == null) {
            return;
        }
        if (pli.getDescription().getEffectiveCommodityCode() == null) {
            return;
        }

        CommodityCode origCC = pli.getDescription().getEffectiveCommodityCode();
        List results = null;
        CommodityCode cc = origCC;
        //q.clone() has a bug but should be the preferred way fo duplicating the
        //query
        String queryString = q.toString();
        AQLQuery qHierarchy = AQLQuery.parseQuery(queryString);

        //  This is the query we are trying to construct
        //    SELECT CommodityExportMapEntry,
        //         CommodityExportMapEntry.AccountType.UniqueName
        //     FROM ariba.common.core.CommodityExportMapEntry
        //                AS CommodityExportMapEntry PARTITION psap SUBCLASS NONE,
        //                ariba.basic.core.CommodityCode As cc
        //     WHERE CommodityExportMapEntry.Creator IS NULL
        //              and CommodityExportMapEntry.CommodityCode in
        //                  (c4t.7m, <c4t.7m.Parent>, <c4t.7m.ParentsParent>)
        //    ORDER BY CommodityExportMapEntry.AccountType ASC

        AQLFieldExpression ccInCEME = new AQLFieldExpression(Fmt.S("%s",
                    CommodityExportMapEntry.KeyEffectiveCommodityCode));

        AQLCondition ccIn = AQLCondition.buildIn(
            ccInCEME, cc.getAncestors());

        qHierarchy.and(ccIn);

        AQLResultCollection resultsColl =
                Base.getService().executeQuery(qHierarchy, super.buildOptions());
        List allcemes = BaseUtil.getResultVector(resultsColl);

        if (allcemes != null) {
            while (cc != null) {
                results = getCEMESForCC(cc, allcemes);
                if (!ListUtil.nullOrEmptyList(results)) {
                    break;
                }
                cc = (CommodityCode)cc.getParent();
            }
        }
        if (ListUtil.nullOrEmptyList(results)) {
             cc = origCC;
        }
        AQLCondition cond = AQLCondition.buildEqual(
                new AQLFieldExpression(Fmt.S("%s",
                    CommodityExportMapEntry.KeyEffectiveCommodityCode)), cc.getBaseId());


        q.and(cond);

        /*
         * 	Changes 	:	Arasan Rajendren
         *  Changed By 	: 	06/06/2011
         *  Changes		: 	Remove SUBCLASS NONE from the SELECT Query to Improve Performance.
         *
         */

        q.getFirstClass().setIncludeSubclassOp(AQLClassReference.SubclassUnset);

        Log.customer.debug("CommodityCodeConditionBasedNameTable - Value of Query is %s ", q.toString());

    }

    private List getCEMESForCC (CommodityCode cc, List allcemes)
    {
        List results = ListUtil.list();
        BaseId ccBaseId = cc.getBaseId();
        for (int i = 0, sz = allcemes.size(); i < sz; i++) {
            BaseId cemeBid = (BaseId)allcemes.get(i);
            CommodityExportMapEntry ceme = (CommodityExportMapEntry)cemeBid.get();
            if (ccBaseId.equals(ceme.getCommodityCode().getBaseId())) {
                results.add(ceme);
            }
        }
        return results;
    }

    /**
        Overriding the matchPattern of AQLNameTable so as to apply the TotalLineAmount
        constraints, if any.
    */
    protected List matchPattern (String field,
                                 String pattern,
                                 SearchTermQuery searchTermQuery)
    {
        setUseQueryCache(false);
        List results = super.matchPattern(field, pattern, searchTermQuery);
        results = ProcureUtil.applyMinAndMaxAmountConditionsIfAny(
                                                        results, getValueSourceContext());
        return results;
    }
}
