/*
 * Created by KS on Sep 13, 2005
 * --------------------------------------------------------------
 * Used to set ReferenceLineNumber for material lines & contract additional charge lines
 */
package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

/*
 * AUL : Changed MasterAgreement to Contract
 */


public class CatSetReferenceLineNumber extends Action {

    private static final String THISCLASS = "CatSetReferenceLineNumber";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;

            int refNum = 0;

            if (CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {

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
            Log.customer.debug("CatSetReferenceLineNumber *** refNum: " + refNum);
            pli.setFieldValue("ReferenceLineNumber",new Integer(refNum));
            Log.customer.debug("%s *** getRefNum: %s",THISCLASS, pli.getFieldValue("ReferenceLineNumber"));
 // works   pli.setFieldValue("TaxCodeOverride",new Boolean(true));
        }
    }

    public CatSetReferenceLineNumber() {
        super();
    }



}
