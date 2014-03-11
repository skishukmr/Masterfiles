/*
 * Created by Santanu
 * --------------------------------------------------------------
 * Used to set ReferenceLineNumber for material lines & contract additional charge lines. Used CompanyCode Logic
 */
package config.java.action.sap;

import config.java.condition.sap.CatSAPAdditionalChargeLineItem;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.Contract;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.*;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSetReferenceLineNumber extends Action {

    private static final String THISCLASS = "CatSAPSetReferenceLineNumber";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;

            int refNum = 0;

            if (CatSAPAdditionalChargeLineItem.isAdditionalCharge(pli)) {

                // Set based on MA line relationship if applicable
	            if (pli instanceof ReqLineItem) {
	                ReqLineItem rli = (ReqLineItem)pli;
	                ContractLineItem mali = rli.getMALineItem();
                    Log.customer.debug("%s *** MALI for RLI %s",THISCLASS, mali);
	                if (mali != null) {
                        Log.customer.debug("%s *** MALI is Additional Charge!",THISCLASS);
                        Contract ma = mali.getMasterAgreement();
	                    Integer maRefNumInt = (Integer)mali.getFieldValue("ReferenceLineNumber");
	                    Log.customer.debug("%s *** MALI refNum (Integer): %s",THISCLASS, maRefNumInt);
	                    if (maRefNumInt != null) {
		                    mali = (ContractLineItem)ma.getLineItem(maRefNumInt.intValue());
		                    Requisition r = (Requisition)rli.getLineItemCollection();
		                    BaseVector lines = r.getLineItems();
		                    int size = lines.size();
		                    int i = 0;
		                    while (i < size) {
		                        rli = (ReqLineItem)lines.get(i);	// reusing rli variable on purpose
		                        if (rli.getMALineItem() == mali) {  // matching mali means material line ref. on contract
		                            refNum = rli.getNumberInCollection();
		                            break;
		                        }
		                        i++;
		                    }
	                    }
	                } else {  // Set to existing Ref Line Num (addresses AC line copying)
	                    refNum = ((Integer)rli.getFieldValue("ReferenceLineNumber")).intValue();
	                }
                }
            } else {  // Must be non-AC (material/labor) so use NIC
                refNum = pli.getNumberInCollection();
            }
            Log.customer.debug("CatSAPSetReferenceLineNumber *** refNum: " + refNum);
            pli.setFieldValue("ReferenceLineNumber",new Integer(refNum));
            Log.customer.debug("%s *** getRefNum: %s",THISCLASS, pli.getFieldValue("ReferenceLineNumber"));
 // works   pli.setFieldValue("TaxCodeOverride",new Boolean(true));
        }
    }

    public CatSAPSetReferenceLineNumber() {
        super();
    }

}
