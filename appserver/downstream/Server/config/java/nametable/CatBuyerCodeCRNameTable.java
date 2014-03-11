 /*********************************************************************************
Name: Sudheer Kumar Jain
Date:25/08/08
Issue No.:850
Description:To get Buyer code in the contract request that will be populating to the
            contract line item also.
*************************************************************************************/
package config.java.nametable;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.util.log.Log;


public class CatBuyerCodeCRNameTable extends AQLNameTable {

		public AQLQuery buildQuery(AQLQuery query,String field, String pattern) {

			try{
		         Log.customer.debug("IN CatBuyerCodeCRNameTable class ");
		         Log.customer.debug(" *** IN CatBuyerCodeCRNameTable class Field IS: " + field);
				 Log.customer.debug(" *** IN CatBuyerCodeCRNameTable class  pattern IS: " + pattern);
		         ValueSource obj = getValueSourceContext();
		         Log.customer.debug(classname + " ValueSource= "+obj);
		         ContractRequest mar  = (ContractRequest)obj;
		         Log.customer.debug("The object for ContractRequest is"+mar);
		         Partition partition = Base.getSession().getPartition();
		         Log.customer.debug("partition IN CatBuyerCodeCRNameTable class is" + partition);

		         if (mar != null)
				         {
				 			// User requester = (User)mar.getRequester();
				 			String facility = (String)mar.getDottedFieldValue("Requester.PayrollFacility");
				 			String Commoditycode = (String)mar.getDottedFieldValue("LineItems.CommodityCode.UniqueName");

				 			if((facility != null) && (Commoditycode != null))
				 			{
								Log.customer.debug("The Requester facility  in CatBuyerCodeCRNameTable class is"+facility);
								Log.customer.debug("The Commoditycode in CatBuyerCodeCRNameTable class is is"+Commoditycode);

				 			   String queryText =  "select BuyerCode.UniqueName from cat.core.FacilityCommodityBuyerCodeMap "+
				                                   "where FacilityCode||'/'||PartitionedCommodityCode.UniqueName in"+
				                                 "(select"+ facility +"||'/'||"+ Commoditycode + "from"+
				                                 "ariba.contract.core.ContractRequest as mar1 join"+
				                                 "ariba.contract.core.ContractRequestLineItem as mali1 using mar1.LineItems";

				 			Log.customer.debug("Query Text IN CatBuyerCodeCRNameTable"+queryText);
				 			query = AQLQuery.parseQuery(queryText);
				 			AQLOptions options = new AQLOptions(partition);
				 			AQLResultCollection results = Base.getService().executeQuery(query,options);
				 			Log.customer.debug("Results Statement= %s", results);
	       	                //return results.getRawResults();
		 			     // query = super.buildQuery(query, field, pattern, searchTermQuery);

					}
				}
			}
				catch (Exception e)
{}
				return query;
}


public CatBuyerCodeCRNameTable(){
	}

    private static final String classname = "CatBuyerCodeCRNameTable : ";
}