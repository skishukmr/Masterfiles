/*
 * CatPreferredCategoryFilter.java
 * Created by Chandra
 * 
 */
package config.java.psleform.sap;


import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.contract.core.nametable.LIPDCommonCommodityCodeNameTable;
import ariba.base.core.aql.*; // SP 22 Change

/*
 *  Nametable to select only preferred categories/commodity codes.
 */
public class CatSAPPreferredCategoryFilter extends LIPDCommonCommodityCodeNameTable
{
    private static final String classname = "CatSAPPreferredCategoryFilter: ";

   public void addQueryConstraintsForClassReference(AQLQuery query,AQLClassReference classRef, String field, String pattern,SearchTermQuery searchTermQuery) // SP 22 Change
    {
        Log.customer.debug(classname + "firing... ");
        String partition = Base.getSession().getPartition().getName();
        super.addQueryConstraintsForClassReference(query,classRef,field, pattern,searchTermQuery);
        String conditionText = "";

        conditionText = Fmt.S("CommodityCode in (select distinct Category "+
                  "from cat.core.CatSAPPreferredSupplierData PARTITION "+partition+" )");
        Log.customer.debug(classname + "Condition Text = " + conditionText);

        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug(classname + "Final Query = " + query);
    }

    public CatSAPPreferredCategoryFilter() { }
}
