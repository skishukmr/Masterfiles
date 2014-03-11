package config.java.condition;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

/** @author kstanley  Mar 05, 2007
 * Condition used to drive validity for BaseObject field (e.g., InvoiceEform)
 */

public class CatSimpleStringFieldEqualTo extends Condition {

	private static final String classname = "CatStringFieldEqualTo";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String"),
 							      					  					new ValueInfo("TestValue", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "TestField","TestValue" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

		boolean result = false;
        	Log.customer.debug("%s *** Object: %s", classname, object);

	    if (object instanceof BaseObject) {
	     	BaseObject bo = (BaseObject)object;

        	String testfield = (String)params.getPropertyForKey("TestField");
        	String testvalue = (String)params.getPropertyForKey("TestValue");

        	if (!StringUtil.nullOrEmptyOrBlankString(testfield) && testvalue != null) {

        		String fieldvalue = (String)bo.getDottedFieldValue(testfield);
        		if (fieldvalue != null && testvalue.equals(fieldvalue)) {
        			result = true;
        		}
        	}
		}
	    return result;
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}

}