
package config.java.condition;

import ariba.base.fields.*;
import ariba.common.core.*;
import ariba.approvable.core.*;
import ariba.util.core.PropertyTable;
import ariba.procure.core.*;
import ariba.util.log.Log;
import ariba.util.core.StringUtil;

/*
 * Condition used to drive validity/visibility/editability based on Line Item field.
 */
public class CatIntegerFieldNotEqualTo extends Condition {

	private static final String ClassName = "CatIntegerFieldNotEqualTo";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("TestValue", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "TestField","TestValue" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

		boolean result = false;

		Log.customer.debug("%s *** Object: %s", ClassName, object);
		LineItem li = null;
	    if (object instanceof SplitAccounting) {
	     	SplitAccounting sa = (SplitAccounting)object;
	     	li = sa.getLineItem();
	 	}
	 	if (object instanceof LineItem) {
			li = (LineItem)object;
		}
		if (object instanceof LineItemProductDescription) {
			LineItemProductDescription lipd = (LineItemProductDescription)object;
			li = lipd.getLineItem();
		}
	    if (li instanceof ProcureLineItem) {
	     	ProcureLineItem pli = (ProcureLineItem)li;
        	String testfield = (String)params.getPropertyForKey("TestField");
        	String testvalue = (String)params.getPropertyForKey("TestValue");

        	Log.customer.debug("%s *** Test Field/Value: %s / %s", ClassName, testfield, testvalue);
        	if (!StringUtil.nullOrEmptyOrBlankString(testfield) && testvalue != null) {
    		    try {
	        	    Integer fieldvalue = (Integer)pli.getDottedFieldValue(testfield);

	        	    Log.customer.debug("%s *** Field Value: %s", ClassName, fieldvalue);
       		        Integer IntValue = Integer.valueOf(testvalue);

       		        Log.customer.debug("%s *** Integer Value: %s", ClassName, IntValue);
                    if (IntValue.intValue() != fieldvalue.intValue())
                        result = true;
        		}
    		    catch (Exception e) {
    		        Log.customer.debug("%s *** Caught exception, try again! %s", ClassName, e);
    		    }
        	}
        	else {

        		Log.customer.debug("%s *** Returning FALSE, TestField or TestValue param unsuable!", ClassName);
        	}
		}

	    Log.customer.debug("CatIntegerFieldNotEqualTo *** Result = " + result);
	    return result;
	}


	public CatIntegerFieldNotEqualTo() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}

}