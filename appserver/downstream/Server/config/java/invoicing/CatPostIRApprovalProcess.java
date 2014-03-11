/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to reset quantity accumulators and HoldIREscalation flag
         when an IR goes to Paying or Rejecting state.
*/

package config.java.invoicing;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;


public class CatPostIRApprovalProcess extends Action
{

	private static final String ClassName = "config.java.invoicing.CatPostIRApprovalProcess";

    public void fire (ValueSource object, PropertyTable params)
    {
		ariba.common.core.Log.customer.debug("in CatPostIRApprovalProcess...");

		if (!(object instanceof InvoiceReconciliation)) {
			return;
		}

		InvoiceReconciliation ir = (InvoiceReconciliation) object;

		if (!ir.getStatusString().equals("Paying") && !ir.getStatusString().equals("Rejecting")) {
			return;
		}

		ariba.base.fields.Log.customer.debug(ClassName + "IR status is " + ir.getStatusString());

		if (ir.getStatusString().equals("Paying")) {
			Iterator irLines = ir.getLineItemsIterator();

			while (irLines.hasNext())
			{
				InvoiceReconciliationLineItem irLi = (InvoiceReconciliationLineItem)irLines.next();

				ProcureLineType plt = irLi.getLineType();

				if (plt == null) {
					continue;
				}

				// If matched to a line item and the Amount is negative, then it's a credit.
				// Fix the accumulators on the PO line item to remove the quantity since we
				// treat credits as credits to the Amount only but Ariba accepts credits similar
				// to Quantity 1, Price -$5.00.  We don't want to accumulate the 1 for Quantity.
				if (irLi.getAmount().getAmount().signum() == -1) {
					if (plt.getCategory() == ProcureLineType.LineItemCategory) {
						adjustForCredits(irLi);
					}
				}
			}
		}

		//also, reset the hold IR for escalation flag unconditionally
		ir.setDottedFieldValue("HoldIREscalation", new Boolean(false));

		ir.save();
    }

	// Adjust accumulators for credits
	private void adjustForCredits(InvoiceReconciliationLineItem irLi)
	{

		/*** IMPORTANT ***
		Expand this method in the future for master agreements.
		***/

		ariba.common.core.Log.customer.debug("%s: Adjusting credit quantities for IR line %s", ClassName, irLi.getNumberInCollection());

		PurchaseOrder po = irLi.getOrder();
		POLineItem poLi = irLi.getOrderLineItem();

		if (po == null || poLi == null) {
			return;
		}

		poLi.setNumberInvoiced(poLi.getNumberInvoiced().subtract(irLi.getQuantity()));
		poLi.setNumberReconciled(poLi.getNumberReconciled().subtract(irLi.getQuantity()));

		//reconcile the pending standard invoices for a given order when a credit invoice is paid for that order
		InvoiceReconciliation.reconcile(po);

	}
}
