/* Created by Santanu on Jan 23, 2009
 * --------------------------------------------------------------
 * Used to determine if ProcureLineItem is an additional charge. Included CompanyCode configuration logic for SAP
 */
package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPAdditionalChargeLineItem extends Condition {

        private static final String THISCLASS = "CatSAPAdditionalChargeLineItem";
        private static String materialCC = "001";

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException {

        ProcureLineItem pli = null;
        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            LineItem li = sa.getLineItem();
            if (li instanceof ProcureLineItem)
                pli = (ProcureLineItem)li;
        }
        else if (object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;

        if (pli != null)
            return isAdditionalCharge(pli);

        return false;
    }

    public static boolean isAdditionalCharge(ProcureLineItem pli) {

        boolean isAC = false;
        if (pli != null && pli.getIsInternalPartId()) {
            String chargecode = (String)pli.getDescription().getFieldValue("CAPSChargeCodeID");
            Log.customer.debug("%s *** chargecode: %s",THISCLASS, chargecode);
            String additionalChargeEnabled = (String)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.AdditionalChargeEnabled");
            if (chargecode != null && !materialCC.equals(chargecode) && additionalChargeEnabled!=null && additionalChargeEnabled.equalsIgnoreCase("Y")) {
                isAC = true;
            }
        }
                return isAC;
    }


        public CatSAPAdditionalChargeLineItem() {
                super();
        }

}