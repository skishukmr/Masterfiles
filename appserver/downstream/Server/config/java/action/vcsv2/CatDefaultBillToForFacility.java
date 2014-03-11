/*
 * Created by KS on April 22, 2005
 * --------------------------------------------------------------
 * Used to set BillingAddress on ReqLineItem & MARLineItem based on Requester Facility
 *
 * S. Sato - AUL: Made the following changes
 * -----------------------------------------
 *
 *    - Modified the code so that it can handle pli as well as plic
 *    - Added a null check on requester to prevent NPE. This trigger gets called
 *      even during times when the requester is not set
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.*;

public class CatDefaultBillToForFacility extends Action {

	private static final String THISCLASS = "CatDefaultBillToForFacility";
	private static final String ADDRESSCLASS = "ariba.common.core.Address";
	private static final String LOOKUP = "DefaultBillToForFacility_";
	private static final String FACILITY = "AccountingFacility";


    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        Log.customer.debug("%s *** IN BILLTO DEFAULTER!", THISCLASS);

            // S. Sato - AUL - Modified section to handle contract requests
        ProcureLineItem pli = null;
        ProcureLineItemCollection plic = null;
        if (object instanceof ProcureLineItem) {
            pli = (ProcureLineItem)object;
            plic = (ProcureLineItemCollection)pli.getLineItemCollection();
        }
        else if (object instanceof ProcureLineItemCollection) {
            plic = (ProcureLineItemCollection) object;
        }
        else {
                // we are not going to process any other object types
            return;
        }

        if (plic != null) {
            User requester = plic.getRequester();
            String facility = null;

                // S. Sato - AUL - In situations where requester is not set. This is the case
                // when contracts are loaded via excel
            if (requester != null) {
                facility = (String)requester.getFieldValue(FACILITY);
            }
            if (facility != null) {
                String uname = Fmt.Sil("cat.java.vcsv2", LOOKUP.concat(facility));
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** facility UN: %s", THISCLASS, uname);
                if (uname != null) {
                    ClusterRoot address = Base.getService().objectMatchingUniqueName(ADDRESSCLASS,
                            Base.getSession().getPartition() ,uname);
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s **** address: %s", THISCLASS, address);
                    if (address != null) {
                        ProcureLineItem dli = plic.getDefaultProcureLineItem();
                        dli.setBillingAddress((Address)address);
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** BillTo after setting Default PLI: %s", THISCLASS, dli.getBillingAddress());
                    }
                }
            }
        }
    }

    public CatDefaultBillToForFacility() {
        super();
    }

}
