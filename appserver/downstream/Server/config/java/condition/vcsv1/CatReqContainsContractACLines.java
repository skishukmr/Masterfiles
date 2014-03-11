/* Created by KS on Nov 21, 2005
 * --------------------------------------------------------------
 * Used to make VISIBLE header level ChargesAddedMessage field
 */
package config.java.condition.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.contract.core.Contract;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatReqContainsContractACLines extends Condition {

	private static final String THISCLASS = "CatReqContainsContractACLines";
//	private static String HeaderMsg = Fmt.Sil("cat.java.vcsv1","ACChargesDefaultedWarning");


    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

        boolean hasAC = false;

        if (object instanceof Requisition) {
            Requisition r = (Requisition)object;

            if (r.getLineItemsCount() > 0) {
                BaseVector lines = r.getLineItems();
                int size = lines.size();
                for (;size>0;size--) {
                    ReqLineItem rli = (ReqLineItem)lines.get(size-1);
                    if (rli.getMALineItem() != null && CatAdditionalChargeLineItem.isAdditionalCharge(rli)) {
                    	Contract ma = rli.getMasterAgreement();
                        if (ma != null && ma.getTermType()==2) {  // Item-level contract only
                            hasAC = true;
                            Log.customer.debug("%s *** FOUND Contract AC Line!", THISCLASS);
                            break;
                        }
                    }
                }
            }
        }
        Log.customer.debug("CatReqContainsContractACLines *** hasAC? " + hasAC);
        return hasAC;
    }



	public CatReqContainsContractACLines() {
		super();
	}

}
