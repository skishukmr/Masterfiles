/*
 * CatPreferredCategoryFilter.java
 * Created by Chandra
 *
 */
package config.java.psleform;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.contract.core.nametable.LIPDCommonCommodityCodeNameTable;
import ariba.util.core.Fmt;
import ariba.util.log.Log;
import ariba.base.core.aql.*; //SP 22 Change

/*
 *  Nametable to select only preferred categories/commodity codes.
 */
public class CatPreferredCategoryFilter extends LIPDCommonCommodityCodeNameTable
{
    private static final String classname = "CatPreferredCategoryFilter: ";

    public void addQueryConstraintsForClassReference(AQLQuery query,AQLClassReference classRef, String field, String pattern,SearchTermQuery searchTermQuery) //SP 22 Change
    {
        Log.customer.debug(classname + "firing... ");
        String partition = Base.getSession().getPartition().getName();
        super.addQueryConstraintsForClassReference(query,classRef, field, pattern,searchTermQuery);
        String conditionText = "";

        conditionText = Fmt.S("CommodityCode in (select distinct Category "+
                  "from cat.core.CatPreferredSupplierData PARTITION "+partition+" )");
        Log.customer.debug(classname + "Condition Text = " + conditionText);

        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug(classname + "Final Query = " + query);
    }

    public CatPreferredCategoryFilter() { }
}
