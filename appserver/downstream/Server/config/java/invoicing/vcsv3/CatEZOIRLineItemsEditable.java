package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.Log;
import ariba.invoicing.core.condition.IRLineItemsEditable;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;

public class CatEZOIRLineItemsEditable extends IRLineItemsEditable {

    public static final String ClassName = "CatEZOIRLineItemsEditable";
	private static String AP_ROLE = ResourceService.getString("cat.rulereasons.vcsv3","Role_AP");
	private static String PURCH_ROLE = ResourceService.getString("cat.rulereasons.vcsv3","Role_TransCtr");

	protected boolean evaluate(int operation, User user, PropertyTable params) {

	    InvoiceReconciliation ir = (InvoiceReconciliation) params.getPropertyForKey("LineItemCollection");
		Log.irEditability.debug("%s evaluate: editability for %s for %s", ClassName, ir.getUniqueName(), user.getMyName());

		// allow the requester or Purchasing or AP to edit the IR
		return isValidEditor(user,ir) ? true : super.evaluate(operation, user, params);
	}

	private boolean isValidEditor(User user, InvoiceReconciliation ir) {

	    User requester = null;

	    if (user.hasRole(Role.getRole(PURCH_ROLE))) {
            //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: User is Indirect Purchasing!", ClassName);
	        return true;
	    }
	    if (user.hasRole(Role.getRole(AP_ROLE))) {
            //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: User is Accounts Payable!", ClassName);
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
	        List requesters = CatEZOIRApprovalRulesUtil.getAllRequesters(ir);
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

/*	private boolean isRequester(User user, InvoiceReconciliation ir) {

		ariba.user.core.User currUser = (ariba.user.core.User) Base.getSession().getEffectiveUser();
		String currUserString = currUser.getMyName();
		String currUserUNString = currUser.getUniqueName();
		String requesterUNString = CatCSVIRApprovalRulesUtil.getRequester(ir).getUniqueName();
		String buyerUNString = CatCSVIRApprovalRulesUtil.getBuyer(ir).getUniqueName();
		//String verifierUNString = CatCSVIRApprovalRulesUtil.getVerifier(ir).getUniqueName();
		String preparerUNString = CatCSVIRApprovalRulesUtil.getPreparer(ir).getUniqueName();

		if (currUserUNString.equals(requesterUNString) ||
			currUserUNString.equals(buyerUNString) ||
			currUserUNString.equals(preparerUNString) ||
			currUser.hasRole(Role.getRole("Tax Manager"))||
			userIsVerifier(currUserUNString, ir)) {
			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Returning true as current user is the requester of the PO/MA", ClassName);
			}
			return true;
		}
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Returning false as current user is not the requester of the PO/MA", ClassName);
		}
		return false;
	}
*/

/*	private boolean userIsVerifier(String username, InvoiceReconciliation ir) {
		boolean userIsVerifier = false;
		boolean anyMileStone = false;
		User verifier = null;

		if (ir.getOrder() == null && ir.getMasterAgreement() != null){
			BaseVector irLineItems = ir.getLineItems();
			InvoiceReconciliationLineItem irli = null;
			for (int i=0; i<irLineItems.size(); i++){
				irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
				Milestone ms = irli.getMilestone();
				if (ms != null){
					anyMileStone = true;
					if (ms.getVerifier() != null){
						String msVerifierUNString = ms.getVerifier().getUniqueName();
						if (username.equals(msVerifierUNString)){
							userIsVerifier = true;
						}
					}
				}
			}
		}
		if (anyMileStone && userIsVerifier){
			return true;
		}
		if (anyMileStone && !userIsVerifier){
			return false;
		}
		return true;
	}
*/
