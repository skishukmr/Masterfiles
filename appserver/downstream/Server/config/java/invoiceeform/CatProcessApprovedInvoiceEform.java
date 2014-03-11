/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to process the approved invoice eform. This is called from the
         workflow file.
*/

package config.java.invoiceeform;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Address;
import ariba.basic.core.Money;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Log;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.payment.core.PaymentTerms;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementOrderInfo;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;

/**
    Creates an Ariba Invoice object based on an approved Invoice Eform.

    The information entered via the eform is copied over to the Invoice object
    and it is then entered into its workflow for processing and reconciliation.

    The UniqueName of the eform is then modified to match the new Invoice in
    order to ensure that the IDs of the eform, invoice, and IR all match.
*/
public class CatProcessApprovedInvoiceEform extends Action
{
    public void fire (ValueSource object, PropertyTable parameters)
      throws ActionExecutionException
    {
        Approvable eform = (Approvable)object;
        Log.customer.debug("Processing InvoiceEform %s.", eform);

		// Create a new Ariba Invoice object
        Partition partition = eform.getPartition();
        Invoice invoice = (Invoice)BaseObject.create(Invoice.ClassName,
                                                     partition);
        invoice.save();

		// Mark the Invoice as being loaded from an Eform
        invoice.setLoadedFrom(Invoice.LoadedFromEForm);

		// Copy the header fields onto the Invoice
        invoice.setName(
            (String)eform.getFieldValue("Name"));
        invoice.setPreparer(
            (User)eform.getFieldValue("Preparer"));
        invoice.setRequester(
            (User)eform.getFieldValue("Requester"));
        invoice.setInvoiceNumber(
            (String)eform.getFieldValue("InvoiceNumber"));
        invoice.setInvoiceDate(
            (Date)eform.getFieldValue("InvoiceDate"));
        invoice.setSupplier(
            (Supplier)eform.getFieldValue("Supplier"));
        invoice.setSupplierLocation(
            (SupplierLocation)eform.getFieldValue("SupplierLocation"));
        invoice.setRemitToAddress((Address)eform.getFieldValue("RemitToAddress"));

		//added by Nani on 4/19/2005
        invoice.setPaymentTerms((PaymentTerms)eform.getFieldValue("PaymentTerms"));
		//end-add

		// Get the totals from the Eform first
        Money totalInvoicedLessTax = (Money)
            eform.getFieldValue("TotalInvoicedLessTax");
        Money totalInvoiced = (Money)
            eform.getFieldValue("EnteredInvoiceAmount");
        Money totalTax = (Money)
            eform.getFieldValue("TotalTax");

		// And negate them if this is a credit memo
        boolean isCreditMemo =
            Invoice.PurposeCreditMemo.equals(
                (String)eform.getFieldValue("Purpose"));

        if (isCreditMemo) {
            totalInvoicedLessTax = totalInvoicedLessTax.negate();
            totalTax = totalTax.negate();
            totalInvoiced = totalInvoiced.negate();
        }

		// Then set the totals on the Invoice
        invoice.setTotalInvoicedLessTax(totalInvoicedLessTax);
        invoice.setTotalTax(totalTax);
        invoice.setTotalInvoiced(totalInvoiced);

		// Set the extrinsics on the Invoice
        invoice.setFieldValue("Eform", Boolean.TRUE);
        invoice.setFieldValue("InvoiceEform", eform);
        invoice.setFieldValue("Terms",
                              (String)eform.getFieldValue("Terms"));

		// Get the MasterAgreement number if it was set
        Contract ma = (Contract)eform.getFieldValue("MasterAgreement");
        String maNumber = null;
        if (ma != null) {
            maNumber = ma.getUniqueName();
        }

		// Now go through all of the Eform line items
        List eformLineItems = (List)eform.getFieldValue("LineItems");
        StatementOrderInfo prevInfo = null;
        boolean consolidated = false;
        int size = ListUtil.getListSize(eformLineItems);
        for (int i = 0; i < size; i++) {
            BaseObject eformLI = (BaseObject)eformLineItems.get(i);

			// Create a new Invoice Line Item
            InvoiceLineItem invoiceLI =
                new InvoiceLineItem(partition, invoice);
            invoiceLI.setDefaultsFromApprovable(invoice);

			// And copy fields from one line item to the other
            ProcureLineType lineType = (ProcureLineType)
                eformLI.getFieldValue("LineType");
            invoiceLI.setLineType(lineType);
            invoiceLI.setInvoiceLineNumber(
                IntegerFormatter.getIntValue(
                    eformLI.getFieldValue("InvoiceLineNumber")));
            invoiceLI.setOrderLineNumber(
                IntegerFormatter.getIntValue(
                    eformLI.getFieldValue("OrderLineNumber")));

			// Set the ProductDescription field
            LineItemProductDescription pd = invoiceLI.getDescription();
            pd.setDescription(
                (String)eformLI.getFieldValue("Description"));
            pd.setSupplierPartNumber(
                (String)eformLI.getFieldValue("SupplierPartNumber"));

            //modified by Nani on 4/19/2005.
            Money mPrice = (Money)eformLI.getFieldValue("Price");
            if (isCreditMemo) {
				if (mPrice.getSign() > 0 &&	!ProcureLineType.isChargeCategory(lineType)) {
					mPrice = mPrice.negate();
				}
			}
			Log.customer.debug("price is " + mPrice.asString());
            pd.setPrice(mPrice);
            //end-modify

            if (ProcureLineType.isLineItemCategory(lineType)) {
                pd.setUnitOfMeasure(
                    (UnitOfMeasure)eformLI.getFieldValue("UnitOfMeasure"));
            }

			// Set the Supplier Order Info field
            StatementOrderInfo info = new StatementOrderInfo(partition);
            info.setOrderNumber(
                (String)eformLI.getFieldValue("OrderNumber"));
            info.setMANumber(maNumber);
            invoiceLI.setSupplierOrderInfo(info);

			// Compare Order Info to check whether consolidated invoice
            if (!consolidated && prevInfo != null) {
                consolidated = !prevInfo.equals(info);
            }
            prevInfo = info;

			// Now get the quantity and amount from the Eform
            BigDecimal qty = (BigDecimal)
                eformLI.getFieldValue("Quantity");
            Money amount = (Money)
                eformLI.getFieldValue("Amount");

			// And negate them if this is a credit memo
            if (isCreditMemo) {

				/*** comment out by Nani on 4/19/2005.
                // Negate non charges if qty is positive
                if (qty.signum() > 0 &&
                    !ProcureLineType.isChargeCategory(lineType)) {
                    qty = qty.negate();
                }
                ***/

				// if its a credit memo then
				// the amount should be negated
                if (amount.getSign() > 0) {
                    amount = amount.negate();
                }
            }

			// Then set the quantity and amount on the Invoice
            invoiceLI.setQuantity(qty);
            invoiceLI.setAmount(amount);

			// Add the new invoice line item
            invoice.getLineItems().add(invoiceLI);
        }

		// Set consolidated flag based on the POs
        invoice.setConsolidated(consolidated);
        if (!consolidated) {
            invoice.setSupplierOrderInfo(prevInfo);
        }

        InvoiceEformUtil.linkParentAndChildren(invoice);

		// Process the loaded invoice
        invoice.processLoadedInvoice();

		// Set the invoiceeform id to be the same as that of the invoice id
        FastStringBuffer invUniqueName =
            new FastStringBuffer(invoice.getUniqueName());
        invUniqueName.replace(invoiceIdentifier, inveformIdentifier);
        eform.setUniqueName(invUniqueName.toString());
    }

    private static final String invoiceIdentifier = "INV";
    private static final String inveformIdentifier = "INEF";

    protected ValueInfo getValueInfo ()
    {
        return new ValueInfo(IsScalar,
                                      "config.java.invoiceeform.InvoiceEform");
    }



}
