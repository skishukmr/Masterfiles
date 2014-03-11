/*****************************************************************************
*
*
* Change History:
* Change By Change Date Description
* --------------------------------------------------------------------------------
* Ravi 11-May-2010 CR 232: Work Item 1116 - Bug fix to default currency from PO into invoice eform.
* IBM Niraj Kumar  10-Feb-2013 Mach1 R5.5 (FRD4.2/TD4.2) Set VATRegistration from RegistrationNumber value from Supplier location record
*******************************************************************************/

package config.java.invoiceeform.sap;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
// Start: Mach1 R5.5 (FRD4.2/TD4.2)
import ariba.common.core.SupplierLocation;
// End: Mach1 R5.5 (FRD4.2/TD4.2)

/**  @author Nagendra.

    Copies information from the selected PurchaseOrders to the Invoice Eform. Creates line items for each PO line,
    copying all relevant information. Defaults Supplier/Location fields from first PO if not yet set. Adds 1 VAT Line as the last line item.
    (Update - 03/02/07) 1) Commented out code that auto-adds VAT line; 2) reset EnteredInvoiceAmount w/last line currency
*/
public class CatSAPSetInvoiceInfoFromOrders extends Action {

	private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	private static final String ClassName = "CatSAPSetInvoiceInfoFromOrders";
//	private static final BigDecimal ZERO = new BigDecimal(0);

	public void fire(ValueSource object, PropertyTable params) {
		Approvable invoice = (Approvable) object;
		List orders = (List) invoice.getFieldValue("Orders");

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** ENTERING %s",ClassName);

		// Do nothing if there are no orders
		if (ListUtil.nullOrEmptyList(orders)) {
			return;
		}

		// Get number of invoice lines to use for the invoice line number
		List invoiceLines = (List) invoice.getFieldValue("LineItems");
		int invoiceLineNumber = ListUtil.getListSize(invoiceLines) + 1;
		ClusterRoot payTerms = null;

		// Go through each order
		int size = orders.size();
		PurchaseOrder order = null;
		for (int i = 0; i < size; i++) {
			BaseId orderBaseId = (BaseId) orders.get(i);
			order = (PurchaseOrder) Base.getSession().objectFromId(orderBaseId);
			List poLines = order.getLineItems();
			/*
			 * CR232: Work item #: 1116: Changes for PO currency defalting. PO currency should be defaulted in invoice eform.
			 */
			Currency poCurrency = null;
			if(order.getTotalCost()!=null)
				poCurrency = order.getTotalCost().getCurrency();
			if (poCurrency != null) {
	        	Money currentEnteredInvoiceAmount = (Money) invoice.getFieldValue("EnteredInvoiceAmount");
	        	Log.customer.debug(ClassName+": Current EnteredInvoiceAmount - "+currentEnteredInvoiceAmount.getAmount()+ " " +currentEnteredInvoiceAmount.getCurrency().getUniqueName() );
	        	Log.customer.debug(ClassName+": PO header currency " + poCurrency.getUniqueName());
	        	invoice.setFieldValue("EnteredInvoiceAmount", new Money(Constants.ZeroBigDecimal,poCurrency));
	        }
	        else
	        	Log.customer.debug(ClassName+": PO header currency is null");
			/*
			 * CR232 complete
			 */

			// Default the supplier from the order (if not set yet)
			if (invoice.getFieldValue("Supplier") == null) {
				invoice.setFieldValue("Supplier", order.getSupplier());
			}

			// Default the supplier location from the order (if not set yet)
			if (invoice.getFieldValue("SupplierLocation") == null) {
				invoice.setFieldValue("SupplierLocation", order.getSupplierLocation());
			}
			// Start: Mach1 R5.5 (FRD4.2/TD4.2)
				invoice.setFieldValue("VATRegistration", order.getDottedFieldValue("SupplierLocation.RegistrationNumber"));
				invoice.setFieldValue("VATRegistrationCopy", order.getDottedFieldValue("SupplierLocation.RegistrationNumber"));
			// End: Mach1 R5.5 (FRD4.2/TD4.2)

			// Now go through each line on the order
			Currency currency = null;
			int lines = ListUtil.getListSize(poLines);
			for (int j = 0; j < lines; j++) {
				POLineItem poLI = (POLineItem) poLines.get(j);

				// Create a new invoice line item and add it to the invoice
				BaseObject invoiceLI = (BaseObject) BaseObject.create("config.java.invoiceeform.InvoiceEformLineItem", invoice.getPartition());
				invoiceLines.add(invoiceLI);

				// Get the LineType from the PO line and set it on the invoice line item
				ProcureLineType lineType = ProcureLineType.lookupByLineItem(poLI);
				invoiceLI.setFieldValue("LineType", lineType);

				// Set the InvoiceLineNumber to the next number and increment
				invoiceLI.setFieldValue("InvoiceLineNumber", Constants.getInteger(invoiceLineNumber));
				invoiceLineNumber++;

				// Set the order information
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** Order used for setting fields: %s",ClassName,order);
				invoiceLI.setFieldValue("Order", order);
				invoiceLI.setFieldValue("OrderNumber", order.getOrderID());
		//		invoiceLI.setFieldValue("OrderNumber", order.getUniqueName());
				invoiceLI.setFieldValue("OrderLineItem", poLI);
				// Santanu changed poLI.getExternalLineNumber from poLI.getNumberInCollection()
				invoiceLI.setFieldValue("OrderLineNumber", Constants.getInteger(poLI.getExternalLineNumber()));
				//invoiceLI.setFieldValue("OrderLineNumber", Constants.getInteger(poLI.getNumberInCollection()));

				// Copy the Product Description and related info
				LineItemProductDescription pd = poLI.getDescription();
				invoiceLI.setFieldValue("ReferenceLineNumber", (Integer)poLI.getDottedFieldValue("ReferenceLineNumber"));
				invoiceLI.setFieldValue("Price", pd.getPrice());
				invoiceLI.setFieldValue("UnitOfMeasure", pd.getUnitOfMeasure());
				invoiceLI.setFieldValue("Description", pd.getDescription());
				//invoiceLI.setFieldValue("SupplierPartNumber", pd.getSupplierPartNumber());

				// Set the quantity and amount
				invoiceLI.setFieldValue("Quantity", poLI.getQuantity());
				invoiceLI.setFieldValue("Amount", poLI.getAmount());
                  if (pd.getSupplierPartNumber() != null)
                invoiceLI.setFieldValue("SupplierPartNumber", pd.getSupplierPartNumber());

				// Initialize the TaxAmount (VAT) to 0.00
				//currency = ((Money)invoiceLI.getFieldValue("Amount")).getCurrency();
				if (poLI.getTaxAmount() != null)
				{
			    invoiceLI.setFieldValue("TaxAmount", poLI.getTaxAmount());
			    }
			   else
			   {
			    currency = ((Money)invoiceLI.getFieldValue("Amount")).getCurrency();
			    invoiceLI.setFieldValue("TaxAmount", new Money(Constants.ZeroBigDecimal,currency));
			     }
			     Money taxamount = null;
			     if (poLI.getTaxAmount() != null)
			     { taxamount = (Money)poLI.getTaxAmount();}
			else
			{
				currency = ((Money)invoiceLI.getFieldValue("Amount")).getCurrency();
			     taxamount =new Money(Constants.ZeroBigDecimal,currency);
		    }
		    if (poLI.getAmount()!=null)
		    {
			     Log.customer.debug(" *** taxamount %s",taxamount);
			    Money amount = (Money)poLI.getAmount();
			    Log.customer.debug(" *** amount %s",amount);
			    Money TotalAmt = amount.add(taxamount);
			    Log.customer.debug(" *** TotalAmt %s",TotalAmt);
			    invoiceLI.setFieldValue("TotalAmount", TotalAmt);
			}

			if(poLI.getDottedFieldValue("TaxCode")!=null)
			{
				BaseObject taxcode =(BaseObject)poLI.getDottedFieldValue("TaxCode");
				Log.customer.debug(" *** taxcode %s",taxcode);
				invoiceLI.setFieldValue("TaxCode", taxcode);
			}


			}
			// Added for R5
	       if (payTerms == null) {
	            payTerms = order.getPaymentTerms();
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** Setting Eform PaymentTerms: %s",ClassName,payTerms);
	            invoice.setFieldValue("PaymentTerms",payTerms);
	        }

	        // 03.02.07 - Reset EnteredInvoiceAmount to 0.00 using currency of last line
	        /*if (currency != null) {
	        	invoice.setFieldValue("EnteredInvoiceAmount", new Money(Constants.ZeroBigDecimal,currency));
	        }*/
		}

		orders.clear();
		invoice.save();

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** EXITING %s",ClassName);
	}

	protected ValueInfo getValueInfo() {
		return valueInfo;
	}

}
