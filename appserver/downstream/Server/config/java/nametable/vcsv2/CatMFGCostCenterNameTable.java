/*
 * Created by KS on May 8, 2005
 * -------------------------------------------------------------------------------
 * Filters CostCenter clusterroots based on if AccountType is Capital or Revenue
 */
package config.java.nametable.vcsv2;

import ariba.approvable.core.LineItem;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatMFGCostCenterNameTable extends CatMFGAccountingNameTable {

    private static final String THISCLASS = "CatMFGCostCenterNameTable";
    private static final String CAPITAL = "Capital";
    private static String CCKEY = ResourceService.getString("cat.java.vcsv2","CapitalCostCenterNameTableKey");


    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){

        super.addQueryConstraints(query, field, pattern, searchQuery);
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** SUPER's Query %s", THISCLASS, query);
        ValueSource vs = getValueSourceContext();
        if (vs instanceof SplitAccounting) {
    	SplitAccounting sa = (SplitAccounting)vs;
        	LineItem li = sa.getLineItem();
    		if (li instanceof ProcureLineItem) {
    		    ProcureLineItem pli = (ProcureLineItem)li;
    		    ClusterRoot at = (ClusterRoot)pli.getFieldValue("AccountType");
   		        ClusterRoot facility = (ClusterRoot)sa.getFieldValue("Facility");
    		    if (CatConstants.DEBUG)
    		        Log.customer.debug("%s *** AccountType %s", THISCLASS, at);
    		    if (at != null && facility != null) {
    		        String conditionText = null;
    		        String key = facility.getUniqueName().concat(CCKEY);
		            if (CAPITAL.equals(at.getUniqueName()))
    		            conditionText = Fmt.S("UniqueName like '%s", key);
    		        else
    		            conditionText = Fmt.S("UniqueName not like '%s", key);
    		        conditionText += "%'";
    		        if (CatConstants.DEBUG)
    		            Log.customer.debug("%s *** conditionText %s", THISCLASS, conditionText);
    		        if (conditionText != null)
    		            query.and(AQLCondition.parseCondition(conditionText));
    		    }
		    }
		}
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }

    public CatMFGCostCenterNameTable() {
        super();
    }
}