
package config.java.condition;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatTrueBoolean extends Condition {

	private static final String ClassName = "CatTrueBoolean";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestField" };

	public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

	    if (object instanceof BaseObject) {
	        BaseObject bo = (BaseObject)object;
	    	try {
	    	  	String testfield = (String)params.getPropertyForKey("TestField");
	    		Boolean bfield = (Boolean)bo.getFieldValue(testfield);
	    		//if (Log.customer.debugOn)
	    		    Log.customer.debug("%s *** bfield: %s",ClassName,bfield);
				if (bfield != null && bfield.booleanValue()) {
				    //if (Log.customer.debugOn)
		    	        Log.customer.debug("%s *** Field is TRUE!", ClassName);
				    return true;
				}
	    	} catch (Exception e) {
	    	    //if (Log.customer.debugOn)
	    	        Log.customer.debug("%s *** Exception testing field! %s", ClassName, e);
	    	}
		}
	   	return false;
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

}