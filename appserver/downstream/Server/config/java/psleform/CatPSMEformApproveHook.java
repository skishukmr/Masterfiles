/*
 * CatUpdateFieldsOnPSM.java
 * Created by Chandra on Sep 01, 2005
 *
 */

package config.java.psleform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BooleanFormatter;

/*
 * Approve hook for Preferred Supplier Maintenance Eform.
 * To restrict approvers from approving the form when
 * the supplier is not selected.
 */
public class CatPSMEformApproveHook implements ApprovableHook {

    private static final int PSMWarning = 1;  //warning
    private static final int PSMError = -1; //error

    private static final List NoErrorResult =
        ListUtil.list(Constants.getInteger(NoError));

    public List run (Approvable approvable) {
        List results = null;


        if (approvable != null) {
            Supplier supplier = null;
            SupplierLocation supplierLocation = null;

            String maintType = (String)approvable.getFieldValue("MaintenanceType");
            String requester = (String)approvable.getDottedFieldValue("Requester.Name.PrimaryString");
            boolean isSupplierPSL = false;

            if (maintType.equals("Create")) {
                supplier = (Supplier)approvable.getFieldValue("PreferredSupplierToCreate");
                isSupplierPSL = BooleanFormatter.getBooleanValue(supplier.getFieldValue("PreferredSupplier"));
            } else if(maintType.equals("Update")) {
                supplier = (Supplier)approvable.getFieldValue("PreferredSupplierToUpdate");
            } else if(maintType.equals("Delete")) {
                supplier = (Supplier)approvable.getFieldValue("PreferredSupplierToDelete");
            }

            if (supplier == null) {
                return
                    ListUtil.list( Constants.getInteger(PSMError),
                                        ResourceService.getString("cat.psm.eform", "NoSupplierMsg"));
            }

            if(isSupplierPSL) {
                User pslOwner = (User)supplier.getFieldValue("PrefSuppCreator");
                String pslcreatorname = pslOwner.getMyName();


                return
                    ListUtil.list( Constants.getInteger(PSMWarning),
                                        Fmt.Sil("cat.psm.eform", "SupplierAlreadyPSLMsg",pslcreatorname));
            }





        }
        return NoErrorResult;
    }

}
