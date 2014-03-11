package config.java.invoiceeform.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  @author kstanley  (Mar 2, 2007)
*
*    Copies iheader VAT information to non-Tax line items.
*    Adds or Removes summary VAT line if needed.
*/

public class CatEZOCopyHeaderVATToLines extends Action {

	private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	private static final String ClassName = "CatEZOCopyHeaderVATToLines";
	private static final String EformClassName = "config.java.invoiceeform.InvoiceEformLineItem";
	private static final BigDecimal ZERO = new BigDecimal(0);
	private static final BigDecimal div100 = new BigDecimal(".01");

	public void fire(ValueSource object, PropertyTable params) {

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** ENTERING %s",ClassName);

		Approvable invoice = (Approvable) object;
		BaseObject vatLine = null;
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

		List lines = (List)invoice.getFieldValue("LineItems");

		if (ListUtil.nullOrEmptyList(lines)) {
			return;   // no point in continuing
		}

		Money totalVAT = null;
		int listSize = ListUtil.getListSize(lines);
		for (int i=0; i<listSize; i++) {
			BaseObject li = (BaseObject)lines.get(i);
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

		// Determine if VAT line should be added/removed

		if (!isVAT) {
			if (vatLine != null)   // remove unnecessary VAT lines
				removeVATLines(invoice, lines);
		}
		else {
			if (vatLine == null)  {  // add VAT line since VAT invoice
				addVATLineForEform(invoice, lines, totalVAT);
			}
			else {  // just update the existing vat line amounts
				CatEZOUpdateVATLineAmount.updateVATLineAmount(invoice, lines);
			}
		}
		invoice.save();

		//if (Log.customer.debugOn)
		    Log.customer.debug(" *** EXITING %s",ClassName);
	}


	public static void removeVATLines (Approvable inv, List lineItems) {

		int size = ListUtil.getListSize(lineItems);
		while (size>0) {
			BaseObject li = (BaseObject)lineItems.get(--size);
			ProcureLineType plt = (ProcureLineType)li.getFieldValue("LineType");

			if (plt == null)
				continue;

			if (plt.getCategory() == ProcureLineType.TaxChargeCategory &&
					plt.getUniqueName().indexOf("VAT") > -1)  {
				lineItems.remove(li);
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOCopyHeaderVATToLines *** removing VAT line#: " + size+1);
			}
		}
	}

	public static void addVATLineForEform (Approvable inv, List lineItems, Money vatAmount)  {

		ProcureLineType vatLineType = ProcureLineType.lookupByUniqueName("VATCharge", inv.getPartition());

		if (vatLineType != null) {
			BaseObject vatLine = (BaseObject)BaseObject.create(EformClassName, inv.getPartition());
			if (vatLine != null) {

				// add VAT line
				int size = ListUtil.getListSize(lineItems);
				lineItems.add(vatLine);
				vatLine.setFieldValue("LineType", vatLineType);
				vatLine.setFieldValue("InvoiceLineNumber",Constants.getInteger(size+1));

				// set last PO info
				PurchaseOrder po = null;
				while (size > 0) {
					po = (PurchaseOrder)((BaseObject)lineItems.get(--size)).getFieldValue("Order");
					if (po != null)
						break;
				}
				//if (Log.customer.debugOn)
					Log.customer.debug("%s *** PO to use: %s",ClassName,po);
				if (po != null) {
					vatLine.setFieldValue("Order", po);
					vatLine.setFieldValue("OrderNumber", po.getOrderID());
					vatLine.setFieldValue("OrderLineItem", null);
					vatLine.setFieldValue("OrderLineNumber",Constants.getInteger(0));
				}
				// set other key fields
		        vatLine.setFieldValue("Quantity", new BigDecimal(1.00));
				vatLine.setFieldValue("Price", vatAmount);
		        vatLine.setFieldValue("Description", vatLineType.getName().getPrimaryString());
		        vatLine.setFieldValue("IsVATRecoverable", Boolean.FALSE);
			}
		}
	}

	protected ValueInfo getValueInfo() {
		return valueInfo;
	}

}
