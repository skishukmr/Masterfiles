/*
 * Created by Dibya Prakash  on 21/08/08
 * --------------------------------------------------------------
 * Editability for UsePreferred Supplier check box in ReqLineItem.
 *
*/

package config.java.condition;

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


public class CatPrefSuppEdit extends Condition {

public static final String ReqParam = "ReqParam";
	private static final String THISCLASS = "CatPrefSuppEdit";
private ValueInfo[] parameterInfo = { 	new ValueInfo(ReqParam,IsScalar,"ariba.purchasing.core.ReqLineItem")};
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

	    Log.customer.debug(THISCLASS + "Entering to code" );
		Log.customer.debug(THISCLASS + " instance  is " +params.getPropertyForKey(ReqParam) );

		String qryString;
		AQLQuery query;
		AQLOptions queryOptions;
		AQLResultCollection queryResults;
		Partition partition = Base.getSession().getPartition();

		Boolean FALSE = new Boolean(false);
        Boolean TRUE = new Boolean(true);

		ReqLineItem rli = (ReqLineItem)params.getPropertyForKey(ReqParam);

		Log.customer.debug(THISCLASS + "ReqLineItem " + rli);
		if (rli!=null)
		{
            Requisition req = (Requisition)rli.getLineItemCollection();
   			User requester = (User)req.getRequester();
   			String facility = (String)requester.getDottedFieldValue("PayrollFacility");
   			String category = (String)rli.getDottedFieldValue("CommodityCode.UniqueName");
   	        if((facility != null) || (category != null))
   	         {
		   	  Log.customer.debug(THISCLASS + "facility " + facility);
		   	  Log.customer.debug(THISCLASS + "category " + category);

   			String supId=(String)rli.getDottedFieldValue("Supplier.UniqueName");
   		    if(supId == null)
   		    { return true;}
   		    else{

   			Log.customer.debug(THISCLASS + "supId " + supId);

	   	    qryString = "select distinct from cat.core.CatPreferredSupplierData Pref where Pref.PreferredSupplier.UniqueName = '"+ supId +"' and Pref.Category.UniqueName like '%"+ category +"%' and Pref.Facility.FacilityCode = '"+ facility +"'";

			Log.customer.debug(THISCLASS + " qryString= " + qryString);
			query = AQLQuery.parseQuery(qryString);
			Log.customer.debug(THISCLASS + " *** query= " + query);
			queryOptions = new AQLOptions(partition);
			Log.customer.debug(THISCLASS + "queryOptions " + queryOptions);
			queryResults = Base.getService().executeQuery(query,queryOptions);
				if (queryResults.getErrors() != null)
				{
					Log.customer.debug(THISCLASS + " fire : result is errors 111");
					rli.setDottedFieldValue("UsePreferredSupplier",FALSE);
							 return false;
				 }
				if (queryResults.isEmpty())
				{
					Log.customer.debug(THISCLASS + " fire : result is emtyu");
					rli.setDottedFieldValue("UsePreferredSupplier",FALSE);
					return false;
				   }
						if (!(queryResults.isEmpty()))
						{
								Log.customer.debug(THISCLASS + " fire : result is not emtyu");
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
	public CatPrefSuppEdit() {

		}

}
