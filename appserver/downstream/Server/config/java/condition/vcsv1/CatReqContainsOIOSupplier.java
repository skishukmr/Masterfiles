/* Created by KS on Mar 11, 2006
 * --------------------------------------------------------------
 * Used to flag when Req/PO contains a line item for OIO Supplier (currently only 1)
 */
package config.java.condition.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatReqContainsOIOSupplier extends Condition {

	private static final String THISCLASS = "CatReqContainsOIOSupplier";
	private static String OIOSuplrLoc = ResourceService.getString("cat.java.vcsv1","Req_OIOSuplrLocUniqueName");


    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

        boolean hasOIO = false;

        if (object instanceof ProcureLineItemCollection) {
            ProcureLineItemCollection r = (ProcureLineItemCollection)object;

            if (r.getLineItemsCount() > 0) {
                BaseVector lines = r.getLineItems();
                int size = lines.size();
                for (;size>0;size--) {
                    ProcureLineItem pli = (ProcureLineItem)lines.get(size-1);
                    SupplierLocation sloc = pli.getSupplierLocation();
                    if (sloc != null && OIOSuplrLoc.equals(sloc.getUniqueName())) {
                        Log.customer.debug("%s *** FOUND OIO Supplier Location!", THISCLASS);
                        hasOIO = true;
                        break;
                    }
                }
            }
        }
        Log.customer.debug("CatReqContainsOIOSupplier *** hasOIOSupplier? " + hasOIO);
        return hasOIO;
    }



	public CatReqContainsOIOSupplier() {
		super();
	}

}
