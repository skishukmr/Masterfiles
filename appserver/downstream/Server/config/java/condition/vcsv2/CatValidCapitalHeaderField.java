/*
 * Created by KS on April 15, 2005
 * -------------------------------------------------------------------------------
 * Returns FALSE with Error Msg if PLIC contains a line item with AccountType = "Capital"
 * && capital header field is not populated
 */
package config.java.condition.vcsv2;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatValidCapitalHeaderField extends Condition {

	private static final String THISCLASS = "CatValidCapitalHeaderField";
	private static final String ERROR_PLIC = ResourceService.getString("cat.java.vcsv2","ErrorPLICContainsCapital_Default");
	private static final String ERROR_REQ = ResourceService.getString("cat.java.vcsv2","ErrorPLICContainsCapital_Requisition");
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String")};
 	private static final String requiredParameterNames[] = { "TestField" };


    public boolean evaluate(Object object, PropertyTable params)
    {
        return hasValidHeaderField(object,params);
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    throws ConditionEvaluationException {

		if(!evaluate(object, params) && ERROR_PLIC != null) {
		    ConditionResult cr = new ConditionResult();
		    if (object instanceof Requisition && ERROR_REQ != null) {
		        if (CatConstants.DEBUG)
		            Log.customer.debug("%s **** Requisition Error! ", THISCLASS);
		        cr.addError(ERROR_REQ);
		    } else {
		        if (CatConstants.DEBUG)
		            Log.customer.debug("%s **** PLIC Error! ", THISCLASS);
		        cr.addError(ERROR_PLIC);
		    }
		    return cr;
		}
		return null;
	}

    public boolean hasValidHeaderField(Object object, PropertyTable params) {

        boolean result = false;
        if (object instanceof ProcureLineItemCollection) {
            ProcureLineItemCollection plic = (ProcureLineItemCollection)object;
            String testfield = (String)params.getPropertyForKey("TestField");
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** Test Field: %s", THISCLASS, testfield);
            if (!StringUtil.nullOrEmptyOrBlankString(testfield)) {
                String capitalfield = (String)plic.getFieldValue(testfield);
                if (!StringUtil.nullOrEmptyOrBlankString(capitalfield))
    	            result = true;
            }
    	}
        if (CatConstants.DEBUG)
            Log.customer.debug("CatValidCapitalHeaderField **** hasValidHeaderField: " + result);
        return result;
    }

	public CatValidCapitalHeaderField() {
		super();
	}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

}

