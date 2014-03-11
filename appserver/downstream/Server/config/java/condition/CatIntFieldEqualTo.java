/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/31/2006
	Description: 	Condition implementation to check if the test field integer
					is equal to the target value integer. Used in validity,
					visibility or editability conditions.
					Works from LineItemProductDescription, SplitAccounting or
					LineItem itself.
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.condition;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatIntFieldEqualTo extends Condition {

	private static final String ClassName = "CatIntFieldEqualTo";
 	private static final ValueInfo parameterInfo[] =
 	{
		new ValueInfo("TestField", IsScalar, "java.lang.String"),
		new ValueInfo("TestValue", IsScalar, "java.lang.String")
	};
    private static final String requiredParameterNames[] = { "TestField","TestValue" };

	public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException
	{
		boolean result = true;
		Log.customer.debug("%s ::: Object: %s", ClassName, object);

		LineItem li = null;
		if (object instanceof SplitAccounting) {
			SplitAccounting sa = (SplitAccounting) object;
			li = sa.getLineItem();
		}
		if (object instanceof LineItem) {
			li = (LineItem) object;
		}
		if (object instanceof LineItemProductDescription) {
			LineItemProductDescription lipd = (LineItemProductDescription) object;
			li = lipd.getLineItem();
		}
		if (li instanceof ProcureLineItem) {
			ProcureLineItem pli = (ProcureLineItem) li;
			String testfield = (String) params.getPropertyForKey("TestField");
			String testvalue = (String) params.getPropertyForKey("TestValue");
			Log.customer.debug("%s ::: Test Field/Test Value: %s / %s", ClassName, testfield, testvalue);
			if (!StringUtil.nullOrEmptyOrBlankString(testfield) && testvalue != null) {
				Integer testValueInteger = null;
				int testValueInt = -100;
				try {
					testValueInteger = new Integer(testvalue);
					Log.customer.debug("%s ::: testValueInteger: %s", ClassName, testValueInteger);
					if (testValueInteger != null) {
						testValueInt = testValueInteger.intValue();
					}
					else {
						Log.customer.debug("%s ::: testValueInteger is null", ClassName);
					}
					Log.customer.debug("%s ::: testValueInt: " + testValueInt, ClassName);
				}
				catch (Exception e) {
					Log.customer.debug("%s ::: Couldn't set the testValueInteger/testValueInt", ClassName);
				}
				if (testValueInt != -100) {
					Integer fieldvalue = (Integer) pli.getDottedFieldValue(testfield);
					int fieldValueInt = -100;
					if (fieldvalue != null) {
						fieldValueInt = fieldvalue.intValue();
					}
					if (fieldValueInt != -100) {
						result = (fieldValueInt == testValueInt);
					}
					else {
						Log.customer.debug("%s ::: fieldValueInt has not been set", ClassName);
					}
				}
				else {
					Log.customer.debug("%s ::: testValueInt has not been set", ClassName);
				}
			}
			else {
				Log.customer.debug("%s ::: Test Value or Test Field is null", ClassName);
			}
		}
		else {
			Log.customer.debug("%s ::: li not of ProcureLineItem type", ClassName);
		}
		return result;
	}

	public CatIntFieldEqualTo() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}
}