 /*************************************************************************************************
*Created by : Aswini M
*Date       : 19-09-2011
*Requirement: Created by Aswini for making TaxCode non editable during PR creation by Requester as part of Vertex
*************************************************************************************************/

package config.java.condition.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
//import ariba.base.fields.ValueInfo;
//import ariba.basic.core.Country;
//import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.user.core.User;
import ariba.base.core.Base;

public class CatSAPTaxCodeEditabilityforPR extends Condition {

	private static final String classname = "CatSAPTaxCodeEditabilityforPR : ";

	public boolean evaluate(Object value, PropertyTable params)
			throws ConditionEvaluationException {
		//BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : value %s ", classname, value);
			ReqLineItem li = null;
			if(value instanceof ReqLineItem){
			Log.customer.debug(" %s : value %s is a reqline item", classname, value);
			li = (ReqLineItem)value;
			Log.customer.debug(" %s : li %s ", classname, li);
			LineItemCollection lic = (LineItemCollection) li.getLineItemCollection();
			Log.customer.debug(" %s : lic %s ", classname, lic);				
			User requester = lic.getRequester();	
			Log.customer.debug(" %s : ** CompanyCode UniqueName is %s",classname, requester);			
					ClusterRoot cr = Base.getSession().getEffectiveUser();
					Log.customer.debug(" %s : ** Clusterroot of the login User is %s",classname, cr);	
					User userName1 = (User) cr;
					Log.customer.debug(" %s : ** UserID of the login User is %s",classname, userName1);	
             if(requester.hasPermission("CatPurchasing") || requester.hasPermission("TaxManager") || userName1.hasPermission("CatPurchasing") || userName1.hasPermission("TaxManager"))
			 {
			 Log.customer.debug(" %s : ** Requester has CatPurchasing or TaxManager permission",classname);	
			 return true;
			 }
			 
			ClusterRoot companycode = (ClusterRoot) lic.getDottedFieldValue("CompanyCode");
			Log.customer.debug(" %s : companycode %s ", classname, companycode);

		 	if (companycode == null) {
				return false;
			}
			
			String vertexflag = (String) lic.getDottedFieldValue("CompanyCode.CallToVertexEnabled");
			Log.customer.debug(" %s : ** CompanyCode UniqueName is %s",classname, vertexflag);
					
		    if (vertexflag == null || vertexflag.equals("N")) {
				return true;
			}
			else return false;
		//}
		
	}
	else return true;
}
	
	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		if (!evaluate(value, params)) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidTaxCode );
				return new ConditionResult(errorMessage);
		}
		else {
			return null;
		}
	}

	public static final String ComponentStringTable = "cat.java.sap";
	private static final String InvalidTaxCode = "InvalidTaxCode";

}

