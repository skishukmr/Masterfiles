/*
 * Created by KS on May 16, 2005
 * -------------------------------------------------------------------------------
 * Returns TRUE if MSDSIncluded boolean field is TRUE or LineItem does not qualify as HazMat
 */
package config.java.condition.vcsv2;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;


public class CatValidMSDSIndicator extends Condition {

	private static final String THISCLASS = "CatValidMSDSIndicator";
	private static final String ERROR_MSDS = ResourceService.getString("cat.java.vcsv2","ErrorMSDSIncluded");


    public boolean evaluate(Object object, PropertyTable params)
    {
        boolean isValid = true;
        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
 //           Log.customer.debug("%s *** In EVALUATE!",THISCLASS);
            if (CatHazmatLineItem.isHazmatSuspect(pli, params)) {
                Boolean indicator = (Boolean)pli.getFieldValue("MSDSIncluded");
                if (indicator != null && !indicator.booleanValue())
                    isValid = false;
            }
        }
        return isValid;
    }


    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    throws ConditionEvaluationException {

		if(!evaluate(object, params)) {
            return new ConditionResult(ERROR_MSDS);
    	}
		return null;
	}


	public CatValidMSDSIndicator() {
		super();
	}


}

