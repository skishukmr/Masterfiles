// NOT USED RIGHT NOW

package config.java.condition.vcsv3;

import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.FieldPropertiesSource;
import ariba.base.fields.Fields;
import ariba.base.fields.ValueInfo;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZOControlAccountingLabels extends Condition
{
	private static final ValueInfo [] parameterInfo = new ValueInfo[0];
	private static final String [] requiredParameterNames = new String[0];

	//Parameter to passed in through AML/
	private static final ValueInfo valueInfo = new ValueInfo("FieldToCheck", 0,
		    "java.lang.String");

	public CatEZOControlAccountingLabels()
	{
	}

	protected ValueInfo [] getParameterInfo()
	{
		return parameterInfo;
	}

	protected String [] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

	/*  Both evaluate and evaluateAndExplain always return true.
	 We do not want the condition to determine visibility. We
	 just want it to change the label.

	 Both evaluate and evaluateAndExplain make a call to the
	 ChangeLabel method, which always retuns true. This method
	 is the one that does the work in dynamically changing the
	 label.
	 */
	public boolean evaluate(Object value, PropertyTable params)
	{
		return this.ChangeLabel(value, params);
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
	{
		Log.customer.debug("ENTERING-EVALUATEANDEXPLAIN");

		boolean test = this.ChangeLabel(value, params);
		Log.customer.debug("TEST" + test);

		return null;
	}

	public boolean ChangeLabel(Object value, PropertyTable params)
	{
		Log.customer.debug("ENTERING-DynamicLabelCondition");

		Log.customer.debug("DynamicLabelCondition Object: " + value);

		/*
		 Get the field FieldPropertiesSource for a given class.
		 */
		FieldPropertiesSource fps = Fields.getService()
			                              .getFpl("ariba.common.core.SplitAccounting",
			    Base.getSession().getVariant());
		Log.customer.debug("DynamicLabelCondition fps" + fps);

		/*
		   Get the field FieldProperties for a given field.
		*/
		FieldProperties fpDept = fps.getFieldProperties("Department");
		FieldProperties fpDiv = fps.getFieldProperties("Division");
		FieldProperties fpSec = fps.getFieldProperties("Section");
		Log.customer.debug("DynamicLabelCondition fpDept" + fpDept);
		Log.customer.debug("DynamicLabelCondition fpDiv" + fpDiv);
		Log.customer.debug("DynamicLabelCondition fpSec" + fpSec);

		if ("E0290".equals(value) || "e0290".equals(value)) {
			Log.customer.debug("DynamicLabelCondition Setting New Label");
			fpDept.setPropertyForKey("Label", new String("Account"));
			fpDiv.setPropertyForKey("Label", new String("sub"));
			fpSec.setPropertyForKey("Label", new String("sub-sub"));
		}

		Log.customer.debug("DynamicLabelCondition FieldsNotEqual");

		return true;
	}

/*
    protected ConditionValueInfo getValueInfo() {
        return valueInfo;
    }

    protected ConditionValueInfo[] getParameterInfo() {
        return parameterInfo;
    }
*/
}