/*
 * Created by KS on Sep 23, 2005
 * --------------------------------------------------------------
 * Used to set CAPSChargeCode (object) to Material (initially)
 *
 * S. Sato - 17th Mar 2011
 *         - Modified this trigger to fire as a field change trigger
 *           at the procure line item level.
 *
 */
package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatSetCAPSChargeCodeAsMaterial extends Action {

    private static final String THISCLASS = "CatSetCAPSChargeCodeAsMaterial";
    private static final String CAPSCLASS = "cat.core.CAPSChargeCode";
    private static String MaterialCode = "001";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem) object;
            LineItemProductDescription lipd = pli.getDescription();

            ClusterRoot capsChargeCode =
                (ClusterRoot) lipd.getFieldValue("CAPSChargeCode");

            if (capsChargeCode == null) {
                Log.customer.debug("%s Defaulting CAPS Charge Code to material", THISCLASS);
                ClusterRoot caps = Base.getService().objectMatchingUniqueName(CAPSCLASS,
                            Partition.None, MaterialCode);
                if (caps != null) {
                    lipd.setFieldValue("CAPSChargeCode", caps);
                }
                Log.customer.debug("%s CAPS Charge Code: %s", THISCLASS, caps);
            }
        }
    }

    public CatSetCAPSChargeCodeAsMaterial() {
        super();
    }


}
