/*
 * Created by Dibya Prakash  on 21/08/08
 * --------------------------------------------------------------
 * Editability for UsePreferred Supplier check box in ReqLineItem.
 *
*/

package config.java.condition.sap;


import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatSAPPrefSuppEdit extends Condition {

public static final String ReqParam = "ReqParam";
private ValueInfo[] parameterInfo = { 	new ValueInfo(ReqParam,IsScalar,"ariba.procure.core.ProcureLineItem")};
private String[] requiredParameterNames = {ReqParam};
protected ValueInfo[] getParameterInfo()
	{
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

public boolean evaluate(Object obj, PropertyTable params)
{
	try{
		Log.customer.debug("CatSAPPrefSuppEdit obj => " + obj);
		String qryString;
		AQLQuery query;
		AQLOptions queryOptions;
		AQLResultCollection queryResults;
		Partition partition = Base.getSession().getPartition();

		Boolean FALSE = new Boolean(false);
        //Boolean TRUE = new Boolean(true);

		ReqLineItem rli = (ReqLineItem)params.getPropertyForKey(ReqParam);
		Log.customer.debug("CatSAPPrefSuppEdit rli => " + rli);
		if (rli!=null)
		{
            Requisition req = (Requisition)rli.getLineItemCollection();
            Log.customer.debug("CatSAPPrefSuppEdit req => " + req.getUniqueName() );
   			User requester = (User)req.getRequester();
   			Log.customer.debug("CatSAPPrefSuppEdit requester => " + requester.getUniqueName() );
   			String companyCode = (String)req.getDottedFieldValue("CompanyCode");
   			Log.customer.debug("CatSAPPrefSuppEdit companyCode => " + companyCode);
   			String category = (String)rli.getDottedFieldValue("CommodityCode.UniqueName");
   			Log.customer.debug("CatSAPPrefSuppEdit companyCode => " + category);
   	        if((companyCode != null) || (category != null))
   	         {
   	        	Log.customer.debug("CatSAPPrefSuppEdit companyCode => " + companyCode);
   	        	Log.customer.debug("CatSAPPrefSuppEdit companyCode => " + category);
   	        	String supLocId=(String)rli.getDottedFieldValue("SupplierLocation.UniqueName");
   		    if(supLocId == null)
   		    {
   		    	return true;
   		    }
   		    else
   		    {

   		    	Log.customer.debug("CatSAPPrefSuppEdit supLocId => " + supLocId);
   		    	qryString = "select distinct from cat.core.CatSAPPreferredSupplierData Pref " +
   		    			"where Pref.PreferredSupplier.UniqueName = '"+ supLocId +"' and " +
   		    					"Pref.Category.UniqueName like '%"+ category +"%' and " +
   		    							"Pref.CompanyCode = '"+ companyCode +"'";
   		    	Log.customer.debug("CatSAPPrefSuppEdit qryString => " + qryString);
				query = AQLQuery.parseQuery(qryString);
				Log.customer.debug("CatSAPPrefSuppEdit query => " + query);
				queryOptions = new AQLOptions(partition);
				Log.customer.debug("CatSAPPrefSuppEdit queryOptions => " + queryOptions);
				queryResults = Base.getService().executeQuery(query,queryOptions);
				if (queryResults.getErrors() != null)
				{
					Log.customer.debug("CatSAPPrefSuppEdit queryResults has error");
					rli.setDottedFieldValue("UsePreferredSupplier",FALSE);
							 return false;
				 }
				if (queryResults.isEmpty())
				{
					Log.customer.debug("CatSAPPrefSuppEdit queryResults is empty");
					rli.setDottedFieldValue("UsePreferredSupplier",FALSE);
					return false;
				}
				if (!(queryResults.isEmpty()))
				{
					Log.customer.debug("CatSAPPrefSuppEdit queryResults has valid choice");
					return false;
				}
			}
         }
	}
}catch ( Exception e)
{
}
return true;
	}
	public CatSAPPrefSuppEdit() {

		}

}
