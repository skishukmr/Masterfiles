/*
 * Created by KS on Dec 13, 2004
 */
package config.java.condition;

import ariba.base.fields.*;
import ariba.util.core.*;
import ariba.purchasing.core.*;
import ariba.contract.core.*;
import ariba.util.log.Log;

public class CatNonNullMALineItemField extends CatHasMALineItem {

	private static final String classname = "CatNonNullMALineItemField";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("FieldName", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "FieldName" };
	private static final String MALI = "MALineItem";

    public boolean evaluate(Object object, PropertyTable params)
 	               throws ConditionEvaluationException {

	return (super.hasMALineItem(object) && hasNonNullMALineValue((ReqLineItem)object, params));
    }

    public boolean hasNonNullMALineValue(ReqLineItem rli, PropertyTable params) {
    	boolean result = false;
    	if (rli != null) {
			String fieldName = (String)params.getPropertyForKey("FieldName");
			Log.customer.debug("%s ***  FieldName: %s", classname, fieldName);
			if (!StringUtil.nullOrEmptyOrBlankString(fieldName)) {
	   			Object cvalue = ((ContractLineItem)rli.getFieldValue(MALI)).getDottedFieldValue(fieldName);
		   		Log.customer.debug("%s *** MALineItem Field equal: %s", classname, cvalue);
		   		if (cvalue != null)
		   			result = true;
			}
    	}
    	Log.customer.debug("CatNonNullMALineItemField *** Result = " + result);
     	return result;
    }

	public CatNonNullMALineItemField() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
