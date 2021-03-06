/* Created by KS on Apr 26, 2006 (to handle nulls & not equal to scenario)
 */
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
public class CatStringFieldNotEqualTo extends Condition {

	private static final String classname = "CatStringFieldNOTEqualTo";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String"),
 							      					  new ValueInfo("TestValue", IsScalar, "java.lang.String")};
    private static final String requiredParameterNames[] = { "TestField","TestValue" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

		boolean result = true;
        Log.customer.debug("%s *** Object: %s", classname, object);
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
        	Log.customer.debug("%s *** Test Field/Value: %s / %s", classname, testfield, testvalue);
        	if (!StringUtil.nullOrEmptyOrBlankString(testfield) && testvalue != null) {
        		String fieldvalue = (String)pli.getDottedFieldValue(testfield);
        		Log.customer.debug("%s *** Field Value: %s", classname, fieldvalue);
        		if (fieldvalue != null && testvalue.equals(fieldvalue)) {
        			result = false;
        		}
        	}
        	else {
        	    Log.customer.debug("%s *** Returning FALSE, TestField or TestValue param unsuable!", classname); 
        	    result = false;
        	}
		}
        Log.customer.debug("CatStringFieldEqualTo *** Result = " + result);
	    return result;
	}


	public CatStringFieldNotEqualTo() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}

}