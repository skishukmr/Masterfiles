/*
 * kstanley
 */

// Dharmang added logic to make this class usable from SA, LI and LIPD

package config.java.condition;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.log.Log;

/*
 * Condition used to drive visibility/editability/validity tied to Boolean fields
 */
public class CatBooleanFieldEqualTo extends Condition {

	private static final String classname = "CatBooleanFieldEqualTo";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String"),
 							                          new ValueInfo("TestValue", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestField","TestValue" };
//	private static Boolean truevalue = BooleanFormatter.parseStringAsBoolean("true");

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException {

//		Base.getSession().transactionCommit();
		boolean result = false;
		LineItemCollection obj = null;
		if (object instanceof SplitAccounting) {
			SplitAccounting sa = (SplitAccounting)object;
			obj = sa.getLineItem().getLineItemCollection();
		}
		if (object instanceof LineItem) {
			obj = ((LineItem)object).getLineItemCollection();
		}
		if (object instanceof LineItemProductDescription) {
			LineItemProductDescription lipd = (LineItemProductDescription)object;
			obj = lipd.getLineItem().getLineItemCollection();
		}
		if (object instanceof Requisition) {
			obj = (LineItemCollection) object;
		}

	    if (obj instanceof Requisition) {
	    	Requisition r = (Requisition)obj;
	    	String testfield = (String)params.getPropertyForKey("TestField");
	    	String testvalue = (String)params.getPropertyForKey("TestValue");
	    	if (!StringUtil.nullOrEmptyOrBlankString(testfield) && !StringUtil.nullOrEmptyOrBlankString(testvalue)) {
	    		Boolean bfield = (Boolean)r.getFieldValue(testfield);
        		Boolean bvalue = BooleanFormatter.parseStringAsBoolean(testvalue);
				Log.customer.debug("CatBooleanFieldEqualTo *** bfield/bvalue: " + bfield + "/" + bvalue);
				if (bvalue != null && bfield != null) {
					Log.customer.debug("CatBooleanFieldEqualTo *** Compare Booleans: " + BooleanFormatter.compareBooleans(bvalue, bfield));
					if (BooleanFormatter.compareBooleans(bvalue, bfield) == 0) {
						result = true;
					}
				}
			}
/*
   	    	boolean bvalue = BooleanFormatter.getBooleanValue(testvalue);
        	Boolean testvalue = BooleanFormatter.parseStringAsBoolean((String)params.getPropertyForKey("TestValue"));
        	Log.customer.debug("CatVisibleEmergencyField *** Test Value: " + testvalue);
 			String testfield = (String)params.getPropertyForKey("TestField");
	      	if (testfield != null && testvalue != null) {
        		Boolean testObj = (Boolean)r.getFieldValue(testfield);
        		Log.customer.debug("%s *** Test Object: %s", classname, testObj);
			}
*/
		}
       	Log.customer.debug("CatBooleanFieldEqualTo *** Result = " + result);
	   	return result;
	}


	public CatBooleanFieldEqualTo() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}


}