/* Created by KS on Sep 16, 2005
 * --------------------------------------------------------------
 * Used to determine if CAPSChargeCode object is set correctly for material line
 */
package config.java.condition.vcsv1;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatValidCAPSChargeCode extends Condition {

	private static final String THISCLASS = "CatAdditionalChargeLineItem";
	private static String ErrorMsg = ResourceService.getString("cat.java.vcsv1","Error_ValidCAPSChargeCode");
	private static String [] materialCodes = {"001","007"};

    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

		Log.customer.debug("%s *** In evaluate", THISCLASS);
        if (object instanceof ProcureLineItem) {
            return isValidChargeCode((ProcureLineItem)object);
        }
        return false;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException  {

    if(!evaluate(object, params)) {
		Log.customer.debug("%s *** Error Message: %s", THISCLASS, ErrorMsg);
		return new ConditionResult(ErrorMsg);
	}
	return null;
}

    public boolean isValidChargeCode(ProcureLineItem pli) {

        boolean valid = true;
        if (!CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {
            ClusterRoot caps = (ClusterRoot)pli.getDottedFieldValue("Description.CAPSChargeCode");
            if (caps != null) {
                String capsUN = caps.getUniqueName();
                if (!capsUN.equals(materialCodes[0]) && !capsUN.equals(materialCodes[1]))
                    valid = false;
            } else {
                valid = false;
            }
        }
        Log.customer.debug("CatValidCAPSChargeCode *** isValidChargeCode: " + valid);
		return valid;
    }


	public CatValidCAPSChargeCode() {
		super();
	}

}
