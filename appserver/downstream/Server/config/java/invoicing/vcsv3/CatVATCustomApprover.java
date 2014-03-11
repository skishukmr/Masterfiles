
package config.java.invoicing.vcsv3;

import java.util.List;
import java.util.Map;

import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.CustomApproverDelegateAdapter;
import ariba.base.core.BaseVector;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.Approver;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

/**
 * @author KS.
 *	Triggers defaulting of accounting to non-material lines. Also validates accounting all lines.
 *	Adds approver to flow if problem occurs in accounting defaulting/apportionment/validation.
 *  Adds approver to flow if unhandled exceptions still exist.
 */

public class CatVATCustomApprover extends CustomApproverDelegateAdapter {

    private static final String ClassName = "CatVATCustomApprover";
    private static String Role_AP = ResourceService.getString("cat.rulereasons.vcsv3","Role_AP");
	private static String Role_Purchasing = ResourceService.getString("cat.rulereasons.vcsv3","Role_TransCtr");
    private static String BadAccountingMsg = ResourceService.getString("cat.rulereasons.vcsv3","InvalidAccounting");
    private static String UnhandledExceptionsMsg = ResourceService.getString("cat.rulereasons.vcsv3","UnhandledExceptions");
    private static final String Separator = ", ";


    public void notifyApprovalRequired(ApprovalRequest ar, String token, boolean originalSubmission) {

        Approver approver = null;
        ApprovalRequest arNew = null;
        InvoiceReconciliation ir = (InvoiceReconciliation)ar.getApprovable();

		// 01.09.07 Added to skip all actions if IR is being Rejected
		if (ir.isForRejection() || ir.isRejecting() || ir.isRejected()){
		    //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: IR: %s is being rejected, not proceeding!", ClassName, ir.getUniqueName());
		    ar.setState(2);
		    return;
		}

        // default accounting for all non-material lines (VAT and other taxes, freight & charges)
        //if (Log.customer.debugOn)
            Log.customer.debug("CatVATCustomApprover **** Call defaultAccountingOnIrLineItems.");

        int errorCode = defaultAccountingOnIRLineItems(ir);

        //if (Log.customer.debugOn)
            Log.customer.debug("CatVATCustomApprover **** errorCode (after defaulting acctng): " + errorCode);

        if (errorCode > 0) {  // Add AP-Geneva to set VAT related accounting directly

            approver = Role.getRole(Role_AP);
	        if (approver == null)  // Add safety approver
	            approver = User.getAribaSystemUser(ar.getPartition());
	        arNew = ApprovalRequest.create(ir,approver,true,"cat.rulereasons.vcsv3","VATAccountingProblem");
        }
        else {    // Also validate accounting since no more approvers on flow
	        boolean badAccounting = false;
	        StringBuffer errorMsg = null;
	        BaseVector lineItems = ir.getLineItems();
	        int size = lineItems.size();
	        for (int i=0;i<size;i++) {
	            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lineItems.get(i);
	            if (!CatEZOInvoiceAccountingValidation.validateIRLineAccounting(irli)) {
	                badAccounting = true;
	                if (errorMsg == null) {
	                    errorMsg = new StringBuffer(BadAccountingMsg);
	                    errorMsg.append(String.valueOf(i+1));
	                }
	                else
	                    errorMsg.append(Separator).append(String.valueOf(i+1));

	    	        //if (Log.customer.debugOn)
	    	            Log.customer.debug("CatVATCustomApprover **** Bad Acctng - Line# " + (i+1));
	            }
	        }
	        if (badAccounting) {
		        //if (Log.customer.debugOn)
		            Log.customer.debug("%s **** Accounting errorMsg: %s ",ClassName,errorMsg);
	            approver = Role.getRole(Role_AP);
		        if (approver == null)  // Add safety approver
		            approver = User.getAribaSystemUser(ar.getPartition());
		        arNew = ApprovalRequest.create(ir,approver,true,"cat.rulereasons.vcsv3",errorMsg.toString());
	        }
        }
        if (arNew != null) {

	        BaseVector requests = ir.getApprovalRequests();
	        BaseVector depends = arNew.getDependencies();
	        depends.add(0,ar);
	        arNew.setFieldValue("Dependencies",depends);

	        // Update CustomApprover approval request
	        ar.setState(2);
	        ar.updateLastModified();

	        // Remove current position of VATCustomApprover (since now a dependency)
	        requests.removeAll(ar);

	        // Reset ApprovalRequests vector (to reflect all changes)
	        requests.add(0,arNew);
	        ir.setApprovalRequests(requests);

		    // New Tax Approver must be activated to trigger inbox & email
	        List arInboxes = ListUtil.list();
	        Map arAlreadyNotified = MapUtil.map();
	        boolean activated = arNew.activate(arInboxes, arAlreadyNotified);

	    }
        // Also add approver if any exceptions not handled by owner
        StringBuffer errorMsg = checkUnhandledExceptions(ir);

	    //if (Log.customer.debugOn)
	        Log.customer.debug("%s **** Unhandled ErrorMsg (null if no error): %s",ClassName,errorMsg);

	    if (errorMsg != null) {
            approver = Role.getRole(Role_Purchasing);
	        if (approver == null)  // Add safety approver
	            approver = User.getAribaSystemUser(ar.getPartition());
	        arNew = ApprovalRequest.create(ir,approver,true,"cat.rulereasons.vcsv3",errorMsg.toString());

	        BaseVector requests = ir.getApprovalRequests();
	        BaseVector depends = arNew.getDependencies();
	        depends.add(0,ar);
	        arNew.setFieldValue("Dependencies",depends);
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s **** UnhandledException AR - created new ar & updated dependencies! %s",ClassName);

	        // Update CustomApprover approval request
	        ar.setState(2);
	        ar.updateLastModified();
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s **** UnhandledException AR - set State & LastModified! %s",ClassName);

	        // Remove current position of VATCustomApprover (since now a dependency)
	        requests.removeAll(ar);
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s **** UnhandledException AR - removed AR from Requests list! %s",ClassName);

	        // Reset ApprovalRequests vector (to reflect all changes)
	        requests.add(0,arNew);
	        ir.setApprovalRequests(requests);
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s **** UnhandledException AR - added new AR to Requests! %s",ClassName);

		    // New Tax Approver must be activated to trigger inbox & email
	        List arInboxes = ListUtil.list();
	        Map arAlreadyNotified = MapUtil.map();
	        boolean activated = arNew.activate(arInboxes, arAlreadyNotified);
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s **** UnhandledException AR - activated notifications! %s",ClassName);
        }
        // Must still update status of VATCustomApprover (even though no new approver added)
    	if (arNew == null)  {
    	    ar.setState(2);
    	}
    }

	private int defaultAccountingOnIRLineItems(InvoiceReconciliation ir) {

	    int errorCode = 0;
		if (ir.getInvoice().isStandardInvoice()) { // only continue if standard

			errorCode = CatEZODefaultAccountingOnIR.defaultAccountingOnLines(ir);

			BaseVector irLineItems = ir.getLineItems();
			InvoiceReconciliationLineItem irli = null;
			for (int i = 0; i < irLineItems.size(); i++) {
				irli = (InvoiceReconciliationLineItem) irLineItems.get(i);

				if (irli.getLineType() != null){
					if ((irli.getLineType().getCategory() != ProcureLineType.LineItemCategory)) {
						InvoiceLineItem invLineItem = irli.getInvoiceLineItem();
						invLineItem.setAccountings(irli.getAccountings());
					}
				}
			}
			ir.save();
		}
		return errorCode;
	}

	public static StringBuffer checkUnhandledExceptions (InvoiceReconciliation ir) {

        boolean unhandled = false;
        StringBuffer errorMsg = new StringBuffer(UnhandledExceptionsMsg);
        int sizeOfExceptions = 0;
        List openExceptions = ir.getAllUnreconciledExceptions();
        List allExceptions = ir.getAllExceptions();
        // First check for any unhandled exceptions
        if (!openExceptions.isEmpty()) {
            unhandled = true;
 	        sizeOfExceptions = openExceptions.size();
	        for (int i=0;i<sizeOfExceptions;i++){
	            InvoiceException ie = (InvoiceException)openExceptions.get(i);
	            String type = (String)ie.getDottedFieldValue("Type.UniqueName");
     	        //if (Log.customer.debugOn)
    	            Log.customer.debug("%s **** Unhandled Exception Type: %s",ClassName,type);
                if (type != null)
                    errorMsg.append(Separator).append(type);
	        }
        }
        // Must also checked other exceptions - unhandled detected by Status
        else if (!allExceptions.isEmpty()) {
            int unhandledState = 8; // Cannot Resolve state
            sizeOfExceptions = allExceptions.size();
            while (sizeOfExceptions>0) {
                InvoiceException ie = (InvoiceException)allExceptions.get(--sizeOfExceptions);
                 if (ie.getState() == unhandledState) {
                     unhandled = true;
                     String type = (String)ie.getDottedFieldValue("Type.UniqueName");
          	         //if (Log.customer.debugOn) {
        	            Log.customer.debug("%s **** Unhandled Exception Type: %s",ClassName,type);
        	            Log.customer.debug("CatVATCustomApprover **** ReconcilieStatus: " + ie.getReconcileStatus());
          	         //}
                     if (type != null)
                         errorMsg.append(Separator).append(type);
                 }
            }
        }
        return unhandled ? errorMsg : null;
	}

    public CatVATCustomApprover() {
        super();
    }
}
