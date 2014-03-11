/***************************************************************************************************



****************************************************************************************************/



package config.java.condition;

import java.math.BigDecimal;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.common.core.Core;
import ariba.common.core.User;
import ariba.purchasing.core.ReqLineItem;
import ariba.user.core.Permission;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.log.Log;


public class CatValidateHazmatWt extends Condition {

	private static final String THISCLASS = "CatValidateHazmatWt";


public boolean evaluate(Object object, PropertyTable params) {

	   Log.customer.debug(" In the Evaluate method");
       return evaluateAndExplain(object, params) == null;
}


public ConditionResult evaluateAndExplain(Object object, PropertyTable params) {

 	Log.customer.debug(" In the Evaluate and Explain method ");
 	Log.customer.debug("%s *** Object %s", THISCLASS,object);
 	Boolean	 isHazmatB = null;
 	boolean hazmatWeightResult =false;
 	boolean hazmatPermissionResult = false;
    boolean isHazmatResult = false;
    ReqLineItem reqli =null;

    Permission permission = Permission.getPermission("CatHazmat");


       User user = Core.getService().getEffectiveUser();
	   Log.customer.debug("**%s**User lookingup this receipt==" + user, "CatValidateHazmatWt");
	   boolean userHasPermission = user.getUser().hasPermission(permission);
	   Log.customer.debug("**%s**User has permission==" + userHasPermission, "CatValidateHazmatWt");
	     if(!userHasPermission)
	        return null;

		   reqli = (ReqLineItem) object ;
           // check for isHazmat == true
		    isHazmatB = (Boolean) reqli.getFieldValue("IsHazmat");
		   	isHazmatResult = BooleanFormatter.getBooleanValue(isHazmatB);
            Log.customer.debug(" In CatValidateHazmatWt isHazmatResult "+ isHazmatResult);

             if (isHazmatResult == false) {
				 return null;
			 }



           // Check for Hazmatwt

           BigDecimal hazmatWeightObject = (BigDecimal) reqli.getFieldValue("HazmatWeight");
           Log.customer.debug(" In CatValidateHazmatWt hazmatWeightObject "+ hazmatWeightObject);

           if (hazmatWeightObject != null) {
			   BigDecimal hazmatWeight = new BigDecimal (hazmatWeightObject.toString());
				if (hazmatWeight.doubleValue() > 0.0) {
				   return null;
			   }

		   }

		   return new ConditionResult("Hazmat Weight value must be greater than zero");
		}

 	public CatValidateHazmatWt() {
 		super();
 	}

 }
