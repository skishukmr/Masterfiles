package config.java.condition.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Accounting;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.log.Log;

public class CatEZOCheckEnterAccounting extends Condition {

	private static final String ClassName = "CatEZOCheckEnterAccounting";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestValue", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestValue" };

	public boolean evaluate(Object object, PropertyTable params)
			throws ConditionEvaluationException
	{
		boolean result = false;
		Log.customer.debug("%s ::: Object: %s", ClassName, object);
		LineItem li = null;
		if (object instanceof Accounting){
			Accounting sa = (Accounting)object;
			li = sa.getLineItem();
		}
		if (li == null && object instanceof SplitAccounting) {
			SplitAccounting sa = (SplitAccounting)object;
			li = sa.getLineItem();
		}
		if (li == null && object instanceof LineItem) {
			li = (LineItem)object;
		}
		if (li == null && object instanceof LineItemProductDescription) {
			LineItemProductDescription lipd = (LineItemProductDescription)object;
			li = lipd.getLineItem();
		}
		Log.customer.debug("%s ::: li: %s", ClassName, li);

		if (li instanceof ContractRequestLineItem) {
			ContractRequestLineItem marli = (ContractRequestLineItem)li;
	    	String testvalue = (String)params.getPropertyForKey("TestValue");
			Log.customer.debug("%s ::: testvalue: %s", ClassName, testvalue);
	    	if (!StringUtil.nullOrEmptyOrBlankString(testvalue)) {
	    		Boolean bfield = (Boolean)marli.getFieldValue("EnterAccounting");
        		Boolean bvalue = BooleanFormatter.parseStringAsBoolean(testvalue);
				Log.customer.debug("%s ::: bfield/bvalue: " + bfield + "/" + bvalue, ClassName);
				if (bvalue != null && bfield != null) {
					Log.customer.debug("%s ::: Compare Booleans: " + BooleanFormatter.compareBooleans(bvalue, bfield), ClassName);
					if (BooleanFormatter.compareBooleans(bvalue, bfield) == 0) {
						result = true;
					}
				}
			}
		}
       	Log.customer.debug("%s ::: Result = " + result);
	   	return result;
	}

	public CatEZOCheckEnterAccounting() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
    		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() 	{
   		return requiredParameterNames;
  	}
}