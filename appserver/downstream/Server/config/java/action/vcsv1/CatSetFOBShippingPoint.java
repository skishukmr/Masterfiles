/* Created by KS on Oct 5, 2005
 * ---------------------------------------------------------------------------------
 * Sets FOBPoint (a.k.a Shipping Point in R4) to Supplier Location / Plant address
 * Change History
   Chandra  10/31/07  Changed Reqlineitem to ProcureLineItem since the same will beused by CRs (issue 217)
 */
package config.java.action.vcsv1;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.SupplierLocation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.procure.core.ProcureLineItem;

public class CatSetFOBShippingPoint extends Action {

	private static final String THISCLASS = "CatSetFOBShippingPoint";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        Log.customer.debug("%s *** OBJECT: %s",THISCLASS,object);
        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            Address source = null;
            SupplierLocation loc = pli.getSupplierLocation();
            if (loc != null)  {
                source = (Address)loc.getFieldValue("FOBPoint");
	            if (source == null)
	                source = (Address)loc;
	            pli.setFieldValue("FOBPoint",source);
            }
            Log.customer.debug("%s *** address source: %s",THISCLASS,source);
        }
    }

    public CatSetFOBShippingPoint() {
        super();
    }

}
