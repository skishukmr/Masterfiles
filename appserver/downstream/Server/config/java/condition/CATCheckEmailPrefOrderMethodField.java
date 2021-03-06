/***************************************************************************************************

 12-03-2007   Amit Kumar   This class determines if Preferred Ordering Method is Email and if the Email
 						   Address is null - a validity condition(Email Address cannot be blank) is
 						   thrown on Supplier Contact Email and AP Email field in the Supplier Eform.



****************************************************************************************************/



package config.java.condition;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CATCheckEmailPrefOrderMethodField extends Condition {

	private static final String THISCLASS = "CATCheckPrefOrderMethodField";
	private String prefOrdMethod =null;
	private String faxNo = null;
	private String currPrefOrdMethod = null;
	private String EmailID = null;



public boolean evaluate(Object object, PropertyTable params) {

	   Log.customer.debug(" In the Evaluate method");
       return evaluateAndExplain(object, params) == null;
}


public ConditionResult evaluateAndExplain(Object object, PropertyTable params) {

 	Log.customer.debug(" In the Evaluate and Explain method ");
 	Log.customer.debug("%s *** Object %s", THISCLASS,object);
       if (object instanceof ClusterRoot)
       {
             ClusterRoot cluster = (ClusterRoot)object;
             prefOrdMethod = (String)cluster.getFieldValue("PreferredOrderingMethod");
             EmailID = (String)cluster.getFieldValue("SupplierContactEmail");

             Log.customer.debug("Preferred Ordering Method is "+prefOrdMethod);
             Log.customer.debug(" Email Address is " +EmailID);


          if(cluster.getFieldValue("Action").toString().equals("Add Supplier"))
          {
				Log.customer.debug("Action is Add");

				if(prefOrdMethod.equalsIgnoreCase("Email") && StringUtil.nullOrEmptyOrBlankString(EmailID))
				{
					Log.customer.debug("Pref Order Method is Email and Supplier Mail ID is null");
					Log.customer.debug(" Return Email Add Error to Evaluate method");
					return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageEmail"));
				}
				else
				    Log.customer.debug(" Pref Ordering Method is ASN / Fax and Email Address is not NULL");
			}
			else
			if(cluster.getFieldValue("Action").toString().equals("Update"))
			{
					currPrefOrdMethod = (String)cluster.getFieldValue("CurrentPreferredOrderingMethod");
					Log.customer.debug(" Current Preferred Ordering method is " +currPrefOrdMethod);
					Log.customer.debug(" Action is Update ");

					if(StringUtil.nullOrEmptyOrBlankString(currPrefOrdMethod) && prefOrdMethod.equalsIgnoreCase("Email") && StringUtil.nullOrEmptyOrBlankString(EmailID))
					{
						Log.customer.debug("Supplier Location object not present . Hence CurrPrefOrdMethod = null");
						return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageEmail"));
					}

	    			if(currPrefOrdMethod != "Email" && prefOrdMethod.equalsIgnoreCase("Email") && StringUtil.nullOrEmptyOrBlankString(EmailID))
					{
						Log.customer.debug("Pref Order Method is Email and Supplier Mail ID is null");
						Log.customer.debug(" Return Email Update Error to Evaluate method");
						return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageEmail"));
					}
					else
						Log.customer.debug(" Pref Ordering Method is ASN / Fax and Email Address is not NULL ");
			}
			else
			Log.customer.debug(" Action is not Add or Update");

		}

        Log.customer.debug(" Return Null to evaluate method");
        return null;
}


 	public CATCheckEmailPrefOrderMethodField() {
 		super();
 	}

 }
