package config.java.invoicing.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementCoreApprovable;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  @author kstanley  (Mar 2, 2007)
*
*    Copies header VAT information to non-Tax line items.
*    Updates summary VAT line amount.
*    Adds or removes summary VAT line as needed (Invoice only).
*/

public class CatEZOCopyINVHeaderVATToLines extends Action {

	private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	private static final String ClassName = "CatEZOCopyINVHeaderVATToLines";
	private static final BigDecimal ZERO = new BigDecimal(0);
	private static final BigDecimal div100 = new BigDecimal(".01");
	private static final String VAT_Description = "VAT - header level";

	public void fire(ValueSource object, PropertyTable params) {

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** ENTERING %s",ClassName);

		if (!(object instanceof StatementCoreApprovable)) {
			return;
		}

		StatementCoreApprovable invoice = (StatementCoreApprovable) object;
		StatementCoreApprovableLineItem vatLine = null;
		boolean isVAT = false;

		// Capture header input params

		Boolean isVATInvoice = (Boolean)invoice.getFieldValue("IsVATInvoice");
		Boolean isRecoverable = (Boolean)invoice.getFieldValue("BaseVATRecoverable");
		BigDecimal vatRate = (BigDecimal)invoice.getFieldValue("BaseVATRate");

		if (isVATInvoice != null && isVATInvoice.booleanValue())
			isVAT = true;

		if(vatRate == null || !isVAT)
			isRecoverable = Boolean.FALSE;

		if (vatRate == null || !isVAT)
			vatRate = ZERO;

		//  Copy header VAT values to non-tax line items

		List lines = invoice.getLineItems();

		if (ListUtil.nullOrEmptyList(lines)) {
			return;   // no point in continuing
		}

		Money totalVAT = null;
		int listSize = ListUtil.getListSize(lines);
		for (int i=0; i<listSize; i++) {
			StatementCoreApprovableLineItem li = (StatementCoreApprovableLineItem)lines.get(i);
			ProcureLineType plt = (ProcureLineType)li.getFieldValue("LineType");

			if (plt == null)
				continue;

			if (plt.getCategory() != ProcureLineType.TaxChargeCategory) {   // if not a tax line, set line VAT fields
				li.setFieldValue("IsVATRecoverable", isRecoverable);
				li.setDottedFieldValueWithoutTriggering("VATRate", vatRate);
				Money taxAmt = ((Money)li.getFieldValue("Amount")).multiply(vatRate).multiply(div100);
				li.setDottedFieldValueWithoutTriggering("TaxAmount", taxAmt);
				if (totalVAT == null)
					totalVAT = new Money(taxAmt);
				else
					totalVAT.addTo(taxAmt);
				//if (Log.customer.debugOn) {
					Log.customer.debug("%s *** taxAmt: %s",ClassName,taxAmt);
					Log.customer.debug("%s *** totalVAT: %s",ClassName,totalVAT);
				//}
			}
			else {  // capture VAT line if exists
				if (plt.getUniqueName().indexOf("VAT") > -1)
					vatLine = li;
			}
		}

		// Determine if VAT line should be added/removed or updated

		if (invoice instanceof Invoice) {
			if (!isVAT) {
				if (vatLine != null) {   // remove uncessary Summary VAT lines (INV only)
					vatLine.setDescription(null);	// ** necessary to avoid ariba error when removing line
					lines.remove(vatLine);
			        invoice.deleteLineItemComments(vatLine); // to prevent possible errors when viewing history
				}
			}
			else {
				if (vatLine == null) {   // add VAT line since VAT invoice (INV only)
					addVATLineForInvoice ((Invoice)invoice, totalVAT);
				}
				else {   //	just update the existing Summary vat line amounts
					CatEZOUpdateInvVATLineAmount.updateVATLineAmount(invoice, lines);
				}
			}
			((Invoice)invoice).updateInvoiceTotals();
		}
	// COMMENTED OUT - for IR, rqmt is to not update VAT line	(even if non-ASN)..i.e., must be correct on INV
	/*	else {
			CatEZOUpdateInvVATLineAmount.updateVATLineAmount(invoice, lines);
	        Money totalTax = invoice.calculateTotalTax();
	        invoice.setTaxAmount(totalTax);
	        invoice.updateTotalCost();
		}
	*/
		invoice.save();

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** EXITING %s",ClassName);
	}


	public static void addVATLineForInvoice (Invoice inv, Money vatAmount)  {

		BaseVector lines = inv.getLineItems();

		// create VAT line
		InvoiceLineItem vatLine =  InvoiceLineItem.create(inv, "VATCharge");
		//if (Log.customer.debugOn)
			Log.customer.debug("%s *** vatLine: %s",ClassName,vatLine);
		if (vatLine == null)
			return;

        // set default values
		InvoiceLineItem parent = (InvoiceLineItem)inv.getDefaultLineItem();
       	vatLine.setParent(parent);
		//if (Log.customer.debugOn)
			Log.customer.debug("%s *** parent: %s",ClassName,vatLine.getParent());
		if (vatLine.getShipTo() == null)
			vatLine.setShipTo(parent.getShipTo());
		if (vatLine.getBillingAddress() == null)
			vatLine.setBillingAddress(parent.getBillingAddress());

		// set vat values
		LineItemProductDescription lipd = vatLine.getDescription();
		lipd.setDescription(VAT_Description);
        vatLine.setFieldValue("IsVATRecoverable", Boolean.FALSE);
		vatLine.setFieldValue("Quantity", new BigDecimal(1.00));
		// * NOTE * Price & Amount set by separate trigger activated when new line is added

		// add new VAT line to invoice
		lines.add(vatLine);
	}


	protected ValueInfo getValueInfo() {
		return valueInfo;
	}

}
