package config.java.invoicing.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.invoicing.core.condition.IRLineItemsEditable;
import ariba.procure.core.Milestone;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;

public class CatCSVIRLineItemsEditable extends IRLineItemsEditable {
	public static final String ClassName = "CatCSVIRLineItemsEditable";

	protected boolean evaluate(int operation, User user, PropertyTable params) {
		InvoiceReconciliation ir = (InvoiceReconciliation) params.getPropertyForKey("LineItemCollection");
		Log.irEditability.debug("%s evaluate: editability for %s for %s", ClassName, ir.getUniqueName(), user.getMyName());

		// allow the requester edit the IR
		if (isRequester(user, ir)) {
			return true;
		}
		return super.evaluate(operation, user, params);
	}

	private boolean isRequester(User user, InvoiceReconciliation ir) {
		/*
		// get the first PO line item and use it to retrieve original req's requester
		ReceivableLineItemCollection rlc = null, prevRLC = null;
		boolean userIsRequester = false;
		InvoiceReconciliationLineItem irli = null;

		Iterator irliIterator = ir.getLineItemsIterator();
		while (!userIsRequester && irliIterator.hasNext()) {
			irli = (InvoiceReconciliationLineItem) irliIterator.next();
			rlc = irli.getOrder();
			if (rlc == null) {
				rlc = irli.getMasterAgreement();
			}
			if (rlc != null) {
				if (!rlc.equals(prevRLC)) {
					userIsRequester = userIsRequester(user, rlc);
				}
			}
			prevRLC = rlc;
		}

		return userIsRequester;
		*/
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
			//}
			return true;
		}
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Returning false as current user is not the requester of the PO/MA", ClassName);
		//}
		return false;
	}

	private boolean userIsRequester(User user, ReceivableLineItemCollection rlc) {
		boolean userIsRequester = false;
		User requester = null;

		if (rlc instanceof PurchaseOrder) {
			PurchaseOrder po = (PurchaseOrder) rlc;
			POLineItem poline = (POLineItem) (po.getLineItems().get(0));
			requester = poline.getRequisition().getRequester();
			userIsRequester = user.equals(requester);
		}
		else {
			//instance of MA
			requester = rlc.getRequester();
			userIsRequester = user.equals(requester);
		}

		return userIsRequester;
	}

	private boolean userIsVerifier(String username, InvoiceReconciliation ir) {
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
}