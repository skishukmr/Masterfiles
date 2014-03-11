/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default the extrinsic field HeaderTaxDetails on the invoice.

Change History
	Change By			Change Date		Description
=============================================================================================
1	Kavitha Udayasankar	25/12/2007		Added the null check on the variable invoiceDate to avoid null pointer exception
*/

package config.java.invoicing;

import java.util.List;

import ariba.base.core.BaseVector;
import ariba.invoicing.AribaInvoiceReconciliationMethod;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.Date;

public class CatInvoiceReconciliationMethod extends AribaInvoiceReconciliationMethod
{
	public static final String ClassName = "config.java.invoicing.CatInvoiceReconciliationMethod";

    protected List createInvoiceReconciliations(Invoice invoice)
    {

		Log.customer.debug(ClassName);

		if (invoice.getTaxDetails().size() == 1) {
			invoice.setDottedFieldValue("HeaderTaxDetail", invoice.getTaxDetails().get(0));
		}

		if (invoice.getLoadedFrom() == 3 || invoice.getLoadedFrom() == 4) {
			//if invoice was created using the invoice eform or invoice entry screen (paper invoice)

			BaseVector invLineItems = invoice.getLineItems();

			InvoiceLineItem invLineItem = null;

			for (int i = 0; i < invLineItems.size(); i++) {
				/***
				For every invoice line item, set the ShipTo, BillingAddress and DeliverTo from the corresponding
				order/MA. Use the first line item on the order/MA since ShipTo and BillTo will be anyway the same
				for all order/MA lines.
				***/
				invLineItem = (InvoiceLineItem) invLineItems.get(i);
				ProcureLineItemCollection plic = invLineItem.getOrder();
				if (plic == null) {
					plic = invLineItem.getMasterAgreement();
				}
				if (plic == null) {
					continue;
				}
				ProcureLineItem pli = (ProcureLineItem) plic.getLineItems().get(0);
				invLineItem.setShipTo(pli.getShipTo());
				invLineItem.setBillingAddress(pli.getBillingAddress());
				invLineItem.setDeliverTo(pli.getDeliverTo());
			}
		}

		//set the invoice time to 12:00 PM in order to show the same date regardless of the user's location
		Date invoiceDate = invoice.getInvoiceDate();
        // Added the nullcheck on the variable invoiceDate in order to remove the nullPointerException
		if(invoiceDate!=null){
		Date.setHours(invoiceDate, 12);
		invoice.setInvoiceDate(invoiceDate);
		}
		else{
			invoiceDate=Date.getNow();
			Date.setHours(invoiceDate,12);
			invoice.setInvoiceDate(invoiceDate);
	   }

		invoice.save();

		return super.createInvoiceReconciliations(invoice);

    }

}
