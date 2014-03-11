/*
     This class is used as the entry point for the SAPTax Watcher node. The node initiates the class SAPCatTaxUtil.java from this code.
	 This class gives the flexibility to put a approvalRequest as well as value object( This class can be invoked from a button as well a
	 s approver or watcher for future use.

   Author: Divya
   Change History
	#	Change Date 	Change By		    Issue#            Description
	#   13/08/2012      IBM AMS_Manoj       WI 318            Skipping Vertex Call for IR's in Rejecting or Rejected Status.
	=============================================================================================


*/
package config.java.customapprover;


import ariba.approvable.core.*;

import ariba.base.fields.ValueSource;
import ariba.base.core.Partition;
import ariba.procure.core.ProcureLineItem;
import ariba.tax.core.TaxCode;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.*;
import ariba.util.log.Log;


import ariba.invoicing.core.InvoiceReconciliation;

import java.io.*;


import java.io.File;
import java.io.IOException;
import java.util.List;

public class SAPCatTaxCustomApproverInv extends CustomApproverDelegateAdapter {

	String className = "SAPCatTaxCustomApproverInv";

	public void notifyApprovalRequired(ApprovalRequest ar, String token,boolean originalSubmission)  {

		//  ***** WI 318 Starts ********

		InvoiceReconciliation ir = (InvoiceReconciliation)ar.getApprovable();

					if (ir.isForRejection() || ir.isRejecting() || ir.isRejected()){
							    //if (Log.customer.debugOn)
									Log.customer.debug("%s ::: IR: %s is being rejected, not proceeding!", className, ir.getUniqueName());
								// Setting the status of IR as rejected.
							    ar.setState(2);
							    Log.customer.debug("%s ::: IR: %s is set to rejected  ", className, ir.getUniqueName());
							    return;
		}

		//  ***** WI 318 Ends ********

        Log.customer.debug("%s :: Triggering Web Service to VERTEX - Point of Source - SAP Custom Apporver IR Level ::",className);
		ValueSource valueSource= null;
		SAPCatTaxUtil.createRequestFile(ar,valueSource);
}
	/*
	 * public String getIcon(ApprovalRequest ar) { return super.getIcon(ar); }
	 */

	public SAPCatTaxCustomApproverInv() {
	}

}




