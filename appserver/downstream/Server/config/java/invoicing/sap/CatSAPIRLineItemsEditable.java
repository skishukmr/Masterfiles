package config.java.invoicing.sap;

import java.util.List;

import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.Log;
import ariba.invoicing.core.condition.IRLineItemsEditable;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;

public class CatSAPIRLineItemsEditable extends IRLineItemsEditable {

    public static final String ClassName = "CatSAPIRLineItemsEditable";

	protected boolean evaluate(int operation, User user, PropertyTable params) {

	    InvoiceReconciliation ir = (InvoiceReconciliation) params.getPropertyForKey("LineItemCollection");
		Log.irEditability.debug("%s evaluate: editability for %s for %s", ClassName, ir.getUniqueName(), user.getMyName());

		// allow the requester or Purchasing or AP to edit the IR
		return isValidEditor(user,ir) ? true : super.evaluate(operation, user, params);
	}

	private boolean isValidEditor(User user, InvoiceReconciliation ir) {

	    User requester = null;

	    if (user.hasPermission("CatPurchasing")) {
            //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: User is Indirect Purchasing!", ClassName);
	        return true;
	    }
	    if (user.hasPermission("ReconcileInvoiceTaxCalculationFailed")) {
            //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: User is Tax person!", ClassName);
	        return true;
	    }
	    if (!ir.getInvoice().getConsolidated()) {
	        ProcureLineItemCollection plic = ir.getMatchedLineItemCollection();
	        if (plic instanceof PurchaseOrder)
	            requester = ((POLineItem)plic.getLineItem(1)).getRequisition().getRequester();
	        else
	            requester = plic.getRequester();
	        if (requester == user) {
                //if (Log.customer.debugOn)
    				Log.customer.debug("%s ::: User is PO/MA requester!", ClassName);
	            return true;
	        }
	    }
	    else {  // use consolidated list of Requesters
	        List requesters = CatSapIRApprovalRulesUtil.getAllRequesters(ir);
	        if (requesters != null && !requesters.isEmpty()){
	            int size = requesters.size();
	            while (size > 0) {
	                requester = (User)requesters.get(--size);
	                if (requester == user) {
	                    //if (Log.customer.debugOn)
	        				Log.customer.debug("%s ::: User is a Summary Inv Requester!", ClassName);
	                    return true;
	                }
	            }
	        }
	    }
	    return false;
	}
}
