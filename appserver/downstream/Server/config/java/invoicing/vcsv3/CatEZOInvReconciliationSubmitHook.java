
package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.common.core.Log;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;


public class CatEZOInvReconciliationSubmitHook implements ApprovableHook {

    private static final String ErrorMsg = ResourceService.getString("cat.invoicejava.vcsv3","Hook_IRResubmitError");
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));
	private static final String ClassName = "CatEZOInvReconciliationSubmitHook";

	public List run(Approvable approvable) {
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Looking at the IR: %s", ClassName, approvable.getUniqueName());
		//}
		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		String statusString = ir.getStatusString();
		if (statusString.equals("Reconciling")
			|| statusString.equals("Approving")
			|| statusString.equals("Rejected")
			|| statusString.equals("Rejecting")) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Cannot resubmit a reconciling, approving, rejecting or rejected IR", ClassName);
			return ListUtil.list(Constants.getInteger(-1), ErrorMsg);
		}
		return NoErrorResult;
	}
}
