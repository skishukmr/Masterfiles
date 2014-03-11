
package config.java.condition;

import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

/**
 * @author kstanley
 * @return TRUE if string length equals integer value specified in StringLength param.
 */

public class CatStringLength extends Condition {

	private static final String ClassName = "CatStringLength";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("ExactLength", IsScalar, "java.lang.String")};
 	private static final String requiredParameterNames[] = {"ExactLength"};

 	private int target = -1;

    public boolean evaluate(Object object, PropertyTable params)
 	           throws ConditionEvaluationException {

        boolean isValid = false;

        String targetLength = (String)params.getPropertyForKey("ExactLength");
        if (!StringUtil.nullOrEmptyOrBlankString(targetLength)) {
            try {
                target = (new Integer(targetLength)).intValue();
            }
            catch (NumberFormatException ne) {
                Log.customer.debug("CatStringLength **** Invalid param (String cannot be " +
                		"converted to an Integer! " + ne.getMessage());
            }
        }
        if (object instanceof String && target > -1) {

            String value = (String)object;
            //if (Log.customer.debugOn)
                Log.customer.debug("CatStringLength **** target/actual length: " + target +
                        value.length());
            if (value.length() == target)
                isValid = true;
        }
        //if (Log.customer.debugOn)
            Log.customer.debug("CatStringLength **** IsValid? " + isValid);
        return isValid;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException {

    	if(!evaluate(object, params)) {
    	    User user = (User)Base.getSession().getRealUser();
    	    String errorMsg = Fmt.Sil(user.getLocale(),"cat.invoicejava.vcsv3","Condition_ValidStringLengthMessage");
    	    //if (Log.customer.debugOn)
    	        Log.customer.debug("%s *** evaluateAndExplain error: %s", ClassName, errorMsg);
    	    String error = Fmt.S(errorMsg,target);
            return new ConditionResult(error);
    	} else
            return null;
    }

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

	public CatStringLength() {
		super();
	}

}
