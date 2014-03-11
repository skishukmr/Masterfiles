/*
 * Created by KS on Sep 27, 2005
 * -------------------------------------------------------------------------
 * Used to set TaxUse based on PLI.AccountType (except for Additional Charge)
 * --------------------------------------------------------------------------
 * Kavitha Udayasankar Sep 28 2007  commented "if" condition for not making the
 *									Manner Of Use to Null for capital account type
 */
package config.java.action.vcsv1;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItem;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

public class CatSetTaxUse extends Action {

    private static final String THISCLASS = "CatSetTaxUse";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;

            if (!CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {

                ClusterRoot acctType = (ClusterRoot)pli.getFieldValue("AccountType");
                if (acctType != null) {
                    String type = acctType.getUniqueName();

                    //commented the if condition as per the Issue number 677
                    /*if (type.equals("Capital"))
                        pli.setFieldValue("TaxUse",null);
                    else*/

                    if (type.equals("Expense")){  // must set from partitioned user
                        LineItemCollection lic = pli.getLineItemCollection();
                        if (lic != null) {
                           User requester = lic.getRequester();
                           ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(requester,lic.getPartition());
                           if (partuser != null) {
                               pli.setDottedFieldValueRespectingUserData("TaxUse",partuser.getFieldValue("TaxUse"));
                           }
                        }

                    }
	            }
            }
        }
    }

    public CatSetTaxUse() {
        super();
    }



}
