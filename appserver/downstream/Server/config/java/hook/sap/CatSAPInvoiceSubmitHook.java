/*********************************************************************************

Abhishek Kumar :	04/09/2013
Change         :	Mach1 R5.5 (FRD2.8/TD2.9): Allow multiple tax line at linelevel for contract based invoice form
Description    :	Mach1 R5.5 (FRD2.8/TD2.9): Remove the validation in the current system to allow multiple tax line
					at linelevel for contract based invoice form.

*********************************************************************************/

package config.java.hook.sap;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.basic.core.Money;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPInvoiceSubmitHook  implements ApprovableHook {

	private static final String ClassName = "CatSAPInvoiceSubmitHook";
	private static final String ComponentStringTable = "aml.InvoiceEform";
	private static final int ValidationError = -2;
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));

	public List run(Approvable approvable) {
		Invoice invoice = (Invoice) approvable;
		BaseVector invLineItems = (BaseVector) invoice.getLineItems();

		if ((invoice.getLoadedFrom() == Invoice.LoadedFromFile) || (invoice.getLoadedFrom() == Invoice.LoadedFromACSN)) {
			Log.customer.debug("This is a File or ASN loaded Invoice");
		}

		if (invoice.getLoadedFrom() == Invoice.LoadedFromEForm) {
			Log.customer.debug("This is a EForm loaded Invoice");
		}

		if (invoice.getLoadedFrom() == Invoice.LoadedFromUI) {
			Log.customer.debug("This is a UI loaded Invoice");


			boolean multipleMASelected = checkIfMultipleMA(invLineItems);
			if (multipleMASelected) {
				String fmt = ResourceService.getString(ComponentStringTable, "MASummaryInvoiceError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean isCurrInvalid = checkForInvalidCurrency(invoice);
			if (isCurrInvalid) {
				String fmt = ResourceService.getString("aml.cat.Invoice", "InvalidCurrency");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean isCurrMismatch = checkForCurrencyMismatch(invoice);
			if (isCurrMismatch) {
				String fmt = ResourceService.getString("aml.cat.Invoice", "CurrencyMismatchMA");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			// Start: Mach1 R5.5 (FRD2.8/TD2.9)
		/*
			boolean multipleTaxLines = hasMultipleTaxLines(invLineItems);
			Log.customer.debug("CatSAPInvoiceSubmitHook ::: hasMultipleTaxLines flag 111 ");
			if (multipleTaxLines) {
				Log.customer.debug("CatSAPInvoiceSubmitHook ::: hasMultipleTaxLines flag 3333 ");
				String fmt = ResourceService.getString(ComponentStringTable, "MultipleTaxLineError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean isLineLevelTaxCharged = hasLineLevelTaxLines(invLineItems);
			if (isLineLevelTaxCharged) {
				String fmt = ResourceService.getString(ComponentStringTable, "LineLevelTaxError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}
		*/
			// End: Mach1 R5.5 (FRD2.8/TD2.9)

			boolean anySpecialCharges = checkForSpecialCharges(invLineItems);
			if (anySpecialCharges) {
				String fmt = ResourceService.getString(ComponentStringTable, "SpecialChargeError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

		}

		return NoErrorResult;
	}

	public static boolean checkIfMultipleMA(BaseVector lineItems) {
		Object ma;
		List maList = ListUtil.list();

		for (int i = 0; i < lineItems.size(); i++) {
			BaseObject lineItem = (BaseObject) lineItems.get(i);

			ma = lineItem.getFieldValue("MasterAgreement");
			if (ma != null) {
				ListUtil.addElementIfAbsent(maList, ma);
			}
		}
		if (ListUtil.getListSize(maList) > 1) {
			return true;
		}
		return false;
	}

	public static boolean checkForInvalidCurrency(Invoice invoice) {
		BaseVector invLineItems = (BaseVector) invoice.getLineItems();
		int size = invLineItems.size();

		Money totalInvoiceAmount = (Money) invoice.getTotalCost();

		for (int i = 0; i < size; i++) {
			InvoiceLineItem invLineItem = (InvoiceLineItem) invLineItems.get(i);

			try {
				Money invLnAmt = (Money) invLineItem.getAmount();

				//make sure that header ccy and the lines ccy match
				if (invLnAmt.getCurrency() != totalInvoiceAmount.getCurrency()) {
					return true;
				}
			}
			catch (NullPointerException ne) {
			}
		}
		return false;
	}

	public static boolean checkForCurrencyMismatch(Invoice invoice) {
		BaseVector invLineItems = (BaseVector) invoice.getLineItems();
		int size = invLineItems.size();

		Money totalInvoiceAmount = (Money) invoice.getTotalCost();

		for (int i = 0; i < size; i++) {
			InvoiceLineItem invLineItem = (InvoiceLineItem) invLineItems.get(i);

			try {
				Money invLnAmt = (Money) invLineItem.getFieldValue("Amount");

				//make sure that ma ccy and invoice ccy match
				Contract ma = (Contract) invLineItem.getMasterAgreement();
				if (ma != null) {
					ContractLineItem mali = (ContractLineItem) ma.getLineItem(1);
					if (mali != null) {
						if (mali.getAmount().getCurrency() != totalInvoiceAmount.getCurrency()) {
							return true;
						}
					}
				}
			}
			catch (NullPointerException ne) {
			}
		}
		return false;
	}

	public static boolean hasMultipleTaxLines(BaseVector lineItems) {
		int taxLinesBasedOnProcureLineType = 0;
		Log.customer.debug("CatSAPInvoiceSubmitHook ::: hasMultipleTaxLines 222 " + taxLinesBasedOnProcureLineType);

		for (int i = 0; i < lineItems.size(); i++) {
			InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getLineType();

			if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				taxLinesBasedOnProcureLineType++;
				Log.customer.debug("CatSAPInvoiceSubmitHook ::: hasMultipleTaxLines 1 " + taxLinesBasedOnProcureLineType);
			}
		}
		if (taxLinesBasedOnProcureLineType > 1) {
			return true;
		}
		return false;
	}

	public static boolean hasLineLevelTaxLines(BaseVector lineItems) {
		boolean isLineLevelTax = false;

		for (int i = 0; i < lineItems.size(); i++) {
			Log.customer.debug("CatSAPInvoiceSubmitHook ::: enter hasLineLevelTaxLines ");
			InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getLineType();

			if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				Log.customer.debug("CatSAPInvoiceSubmitHook ::: enter ProcureLineType.TaxChargeCategory ");
				if ((li.getDescription().getShortName().indexOf("line level") > 0)
					|| (((String) li.getFieldValue("MatchedToString")).indexOf("Item") > 0)) {
					isLineLevelTax = true;
					Log.customer.debug("CatSAPInvoiceSubmitHook ::: isLineLevelTax is true ");
				}
			}
		}
		return isLineLevelTax;
	}

	public static boolean checkForSpecialCharges(BaseVector lineItems) {
		boolean anyAdditionalCharges = false;

		for (int i = 0; i < lineItems.size(); i++) {
			InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getLineType();

			if (plt.getCategory() == ProcureLineType.FreightChargeCategory
				|| plt.getCategory() == ProcureLineType.HandlingChargeCategory
				|| plt.getCategory() == ProcureLineType.DiscountCategory) {
				anyAdditionalCharges = true;
			}
		}
		return anyAdditionalCharges;
	}


}
