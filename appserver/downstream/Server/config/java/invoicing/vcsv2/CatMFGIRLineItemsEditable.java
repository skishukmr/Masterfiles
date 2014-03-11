/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to give edit capability on the IR if the exception handler is the requester.
*/

package config.java.invoicing.vcsv2;

import java.util.Iterator;

import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.invoicing.core.condition.IRLineItemsEditable;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;

public class CatMFGIRLineItemsEditable extends IRLineItemsEditable
{
	public static final String ClassName = "config.java.invoicing.CatIRLineItemsEditable";

	protected boolean evaluate(int operation, User user, PropertyTable params)
	{
		InvoiceReconciliation ir = (InvoiceReconciliation)params.getPropertyForKey("LineItemCollection");
		Log.irEditability.debug("%s evaluate: editability for %s for %s", ClassName, ir.getUniqueName(), user.getMyName());

		// allow the requester edit the IR
		if(isRequester(user, ir)) {
			return true;
		}

		return super.evaluate(operation, user, params);
	}

	private boolean isRequester(User user, InvoiceReconciliation ir)
	{
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
	}

    private boolean userIsRequester(User user, ReceivableLineItemCollection rlc)
    {
		boolean userIsRequester = false;
		User requester = null;

		if (rlc instanceof PurchaseOrder) {
			PurchaseOrder po = (PurchaseOrder) rlc;
			POLineItem poline = (POLineItem)(po.getLineItems().get(0));
			requester = poline.getRequisition().getRequester();
			userIsRequester = user.equals(requester);
		} else {
			//instance of MA
			requester = rlc.getRequester();
			userIsRequester = user.equals(requester);
		}

        return userIsRequester;
    }
}