/***************************************************************************************************

28-07-09 Shailaja Salimath: Issue 951:This class determines if Preferred Ordering Method is ASN and if the Fax
number is null -  a validity condition(Fax number cannot be blank) is thrown on Supplier Fax Number field in the Supplier Eform

****************************************************************************************************/



package config.java.condition;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CATCheckASNPrefOrderMethodField extends Condition {

	private static final String THISCLASS = "CATCheckPrefOrderMethodField";
	private String prefOrdMethod =null;
	private String ASNNo = null;
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

             ASNNo = (String)cluster.getFieldValue("ASNAccountID");


             Log.customer.debug("Preferred Ordering Method is ");

          if(cluster.getFieldValue("Action").toString().equals("Add Supplier"))
          {
				Log.customer.debug("Action is Add");
				if(prefOrdMethod.equalsIgnoreCase("URL") && StringUtil.nullOrEmptyOrBlankString(ASNNo))
				{
					Log.customer.debug("Adding supplier with ASN number as null");
					Log.customer.debug(" Return Fax Add Error to Evaluate method");
				 	return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageASN"));
				}
				else
				    Log.customer.debug(" Pref Ordering Method is ASN / Email and Fax number is not NULL");
			}
			else
			if(cluster.getFieldValue("Action").toString().equals("Update"))
			{

					currPrefOrdMethod = (String)cluster.getFieldValue("CurrentPreferredOrderingMethod");
					//Log.customer.debug(" Current Preferred Ordering method is " +currPrefOrdMethod);
					Log.customer.debug(" Action is Update ");

					if(StringUtil.nullOrEmptyOrBlankString(currPrefOrdMethod) && prefOrdMethod.equalsIgnoreCase("URL") && StringUtil.nullOrEmptyOrBlankString(ASNNo))
					{
						Log.customer.debug("Supplier Location object not present . Hence CurrPrefOrdMethod = null");
						return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageASN"));
					}


	     			if(currPrefOrdMethod != "URL" && prefOrdMethod.equalsIgnoreCase("URL") && StringUtil.nullOrEmptyOrBlankString(ASNNo))
	     			{
	     				Log.customer.debug("Preferred Ordering Method is Fax");
      					Log.customer.debug(" Return Fax Update Error to Evaluate method");
                		return new ConditionResult(ResourceService.getString("aml.cat.suppliereform","MessageASN"));
	    			}
					else
						Log.customer.debug(" Pref Ordering Method is ASN / Email and Fax number is not NULL ");
			}
			else
			Log.customer.debug(" Action is not Add or Update");

		}

        Log.customer.debug(" Return Null to evaluate method");
        return null;
}


 	public CATCheckASNPrefOrderMethodField() {
 		super();
 	}

 }
