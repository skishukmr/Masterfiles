/* Created by KS on Nov 15, 2005
 * --------------------------------------------------------------
 * Used to determine if SupplierLocation is valid (i.e., can be null or must be set)
 */
package config.java.condition.vcsv1;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatValidSupplierLocation extends Condition {

	private static final String THISCLASS = "CatValidSupplierLocation";
	private static String ErrorMsg = ResourceService.getString("cat.java.vcsv1","Error_ValidSupplierLocation");


    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            Object supplier = pli.getSupplier();
            if (supplier != null) {
                return (pli.getSupplierLocation() != null);
            }
        }
        return true;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException  {

    if(!evaluate(object, params)) {
		Log.customer.debug("%s *** Error Message: %s", THISCLASS, ErrorMsg);
		return new ConditionResult(ErrorMsg);
	}
	return null;
}


	public CatValidSupplierLocation() {
		super();
	}

}
