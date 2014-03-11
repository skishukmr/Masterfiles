/*
 * Issue 249  IBM AMS_Lekshmi  Defaulting Account Type for InvoiceEntry  tax line.FOs
 */

package config.java.action.vcsv1;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.AccountType;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;

// Defaulting Invoice AccountType created for contract from UI
public class CatDefaultAccountTypeForTaxLines  extends Action{

	public void fire(ValueSource valuesource, PropertyTable propertytable)
			throws ActionExecutionException {
		if (valuesource instanceof InvoiceLineItem) { // LineType on  LI
														// was changed

			InvoiceLineItem invLi = (InvoiceLineItem) valuesource;
			Invoice inv = invLi.getInvoice();
			int invLoadingCat = inv.getLoadedFrom();
			if (invLoadingCat == Invoice.LoadedFromUI) {
				if (invLi.getLineType().getCategory() == ProcureLineType.TaxChargeCategory) {
					AccountType accTypeObj = null;

					// Defaulting The AccountType from Invoice as the IR is
					// still not Submitted.
					accTypeObj = defaultAccountType(invLi);
					invLi.setFieldValue("AccountType",accTypeObj);
					Log.customer.debug("%s ::: The Account Type is updated to Tax Line ",ClassName);
				}
			}
		}
	}

	private AccountType defaultAccountType(InvoiceLineItem invLi) {
		AccountType accType = null;
		try {
			Iterator lineIterator = invLi.getInvoice().getAllLineItems();
			Log.customer.debug("Entering IR LineItems ", ClassName);

			while (lineIterator.hasNext()) {
				Log.customer.debug("Accessing Line ", ClassName);
				InvoiceLineItem irLine = (InvoiceLineItem) lineIterator.next();
				accType = (AccountType) irLine.getFieldValue("AccountType");
				Log.customer.debug("%s ::: The Account Type on the IR line ",
						ClassName);
				if (accType != null) {
					Log.customer
							.debug(
									"%s ::: The Account Type on the IR line inside iterator %s",
									ClassName, accType);
					break;
				}
			}
		} catch (NullPointerException npe) {
			Log.customer.debug("Exception Occured in $s", ClassName);
		}
		Log.customer.debug("Exit setTaxAccountTypefromInvoiceLine method in ",
				ClassName);
		return accType;
	}

	public CatDefaultAccountTypeForTaxLines() {
	}

	private static final String ClassName = "CatDefaultAccountTypeForTaxLines";


}
