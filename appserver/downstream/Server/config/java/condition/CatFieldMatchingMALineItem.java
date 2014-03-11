/*
 * Created by KS on Nov 30, 2004
 */
package config.java.condition;

import ariba.base.fields.*;
import ariba.util.core.*;
import ariba.purchasing.core.*;
import ariba.contract.core.*;
import ariba.util.log.Log;

public class CatFieldMatchingMALineItem extends CatHasMALineItem {

	private static final String classname = "CatFieldMatchingMALineItem";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("FieldName", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "FieldName" };
	private static final String MALI = "MALineItem";

    public boolean evaluate(Object object, PropertyTable params)
	 	throws ConditionEvaluationException {

    	Log.customer.debug("%s *** In evaluate", classname);
    	return (super.hasMALineItem(object) && hasMatchingMALineValue((ReqLineItem)object, params));
}

    public boolean hasMatchingMALineValue(ReqLineItem rli, PropertyTable params) {

    	boolean result = false;
    	if (rli != null) {
			String fieldname = (String)params.getPropertyForKey("FieldName");
			Log.customer.debug("%s ***  FieldName: %s ", classname, fieldname);
			if (!StringUtil.nullOrEmptyOrBlankString(fieldname)) {
		   			Object rvalue = rli.getDottedFieldValue(fieldname);
		   			Object cvalue = ((ContractLineItem)rli.getFieldValue(MALI)).getDottedFieldValue(fieldname);
		   			Log.customer.debug("%s ***  rvalue/cvalue: %s / %s", classname, rvalue, cvalue);
		   			if (rvalue instanceof String && cvalue instanceof String && rvalue.equals(cvalue))
		   				result = true;
		   			else if (rvalue == cvalue)
		   				result = true;
			}
    	}
    	Log.customer.debug("CatFieldMatchingMALineItem *** Result = " + result);
     	return result;
	}

	public CatFieldMatchingMALineItem() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

}
