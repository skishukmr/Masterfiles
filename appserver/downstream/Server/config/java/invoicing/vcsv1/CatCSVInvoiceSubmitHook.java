package config.java.invoicing.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;

public class CatCSVInvoiceSubmitHook implements ApprovableHook {

	private static final String ClassName = "CatCSVInvoiceSubmitHook";
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

			setTermsDiscountPercent(invLineItems);

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

			boolean multipleTaxLines = hasMultipleTaxLines(invLineItems);
			if (multipleTaxLines) {
				String fmt = ResourceService.getString(ComponentStringTable, "MultipleTaxLineError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean isLineLevelTaxCharged = hasLineLevelTaxLines(invLineItems);
			if (isLineLevelTaxCharged) {
				String fmt = ResourceService.getString(ComponentStringTable, "LineLevelTaxError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean vatReasonable = vatReasonablnessCheck(invLineItems);
			if (!vatReasonable) {
				String fmt = ResourceService.getString(ComponentStringTable, "VATResonablenessError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}

			boolean anySpecialCharges = checkForSpecialCharges(invLineItems);
			if (anySpecialCharges) {
				String fmt = ResourceService.getString(ComponentStringTable, "SpecialChargeError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}
/*
			boolean isSpecialChargeValid = checkForValidSpecialCharge(invLineItems);
			if (!isSpecialChargeValid) {
				String fmt = ResourceService.getString(ComponentStringTable, "SpecialChargeError");
				return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}
*/
			InvoiceLineItem irli = null;
			SplitAccountingCollection sac = null;
			SplitAccounting sa = null;
			Response response = null;
			String AccountingErrorMsg = "";
			String sbrtnRtCode = null;
			String sbrtnMessage = null;

			for (int i = 0; i < invLineItems.size(); i++) {
				irli = (InvoiceLineItem) invLineItems.get(i);
				sac = irli.getAccountings();
				if (sac != null) {
					List sacList = sac.getSplitAccountings();
					int sacSize = sacList.size();
					for (int j = 0; j < sacSize; j++) {
						sa = (SplitAccounting) sacList.get(j);
						response = CatValidateInvAccountingString.validateAccounting(sa);

						    // S. Sato - AUL - Added a null check
						if (response == null) {
						    Log.customer.debug("" +
						            "%s No response returned. Web service may be down. " +
						            "Exiting",
						            ClassName);
						}
						else {
						    sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
						    sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
						    if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
							    AccountingErrorMsg = AccountingErrorMsg + "Line " + (i+1) + " Split " + (j+1) + ": Error - " + sbrtnMessage + ";\n";
						    }
						}
					}
				}
			}

			if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", ClassName, AccountingErrorMsg);
				return ListUtil.list(Constants.getInteger(ValidationError), AccountingErrorMsg);
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
		int taxLinesBasedOnCapsChargeCode = 0;

		for (int i = 0; i < lineItems.size(); i++) {
			InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getLineType();
			ClusterRoot CapsCCObj = (ClusterRoot) li.getFieldValue("CapsChargeCode");

			if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				taxLinesBasedOnProcureLineType++;
			}
			if (CapsCCObj != null) {
				String CapsCCObjUN = (String) CapsCCObj.getUniqueName();
				if ("002".equals(CapsCCObjUN)) {
					taxLinesBasedOnCapsChargeCode++;
				}
				if ("003".equals(CapsCCObjUN)) {
					taxLinesBasedOnCapsChargeCode++;
				}
				if ("096".equals(CapsCCObjUN)) {
					taxLinesBasedOnCapsChargeCode++;
				}
			}
		}
		if (taxLinesBasedOnCapsChargeCode > 1 || taxLinesBasedOnProcureLineType > 1) {
			return true;
		}
		return false;
	}

	public static boolean hasLineLevelTaxLines(BaseVector lineItems) {
		boolean isLineLevelTax = false;

		for (int i = 0; i < lineItems.size(); i++) {
			InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getLineType();

			if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				if ((li.getDescription().getShortName().indexOf("line level") > 0)
					|| (((String) li.getFieldValue("MatchedToString")).indexOf("Item") > 0)) {
					isLineLevelTax = true;
				}
			}
		}
		return isLineLevelTax;
	}

	public static boolean vatReasonablnessCheck(List lineItems){
		BigDecimal taxAmount = new BigDecimal("0.0000");
		BigDecimal nonTaxAmount = new BigDecimal("0.0000");
		BigDecimal centBD = new BigDecimal("100");
		BigDecimal maxAllowed = new BigDecimal("50.00");
		BigDecimal percentage = null;
		boolean vatCharged = false;

		for (int i = 0; i < lineItems.size(); i++) {
			BaseObject li = (BaseObject) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getFieldValue("LineType");
			ClusterRoot CapsCCObj = (ClusterRoot) li.getDottedFieldValue("CapsChargeCode");
			if (plt != null && plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				if ("VATCharge".equals(plt.getUniqueName())) {
					vatCharged = true;
					taxAmount = (BigDecimal) li.getDottedFieldValue("Amount.Amount");
				}
			}
			else{
				nonTaxAmount = nonTaxAmount.add((BigDecimal) li.getDottedFieldValue("Amount.Amount"));
			}
		}

		if (vatCharged){
			percentage = taxAmount.multiply(centBD);
			percentage = percentage.divide(nonTaxAmount, BigDecimal.ROUND_HALF_UP);
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: vatReasonablnessCheck: The percentage of VAT Charged is %s", ClassName, percentage.toString());
			if (percentage.compareTo(maxAllowed) > 0){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: vatReasonablnessCheck: Returning false", "CatCSVInvoiceEformSubmitHook");
				return false;
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: vatReasonablnessCheck: Returning true", "CatCSVInvoiceEformSubmitHook");
		return true;
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

	public static boolean checkForValidSpecialCharge(BaseVector lineItems) {
		boolean isSUTPresent = false;
		boolean specialChargePresent = false;

		for (int i = 0; i < lineItems.size(); i++) {
			BaseObject li = (BaseObject) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getFieldValue("LineType");
			ClusterRoot CapsCCObj = (ClusterRoot) li.getDottedFieldValue("CapsChargeCode");

			if (plt != null && plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				if ("ServiceUseTax".equals(plt.getUniqueName()) || "SalesTaxCharge".equals(plt.getUniqueName())) {
					isSUTPresent = true;
				}
			}
			if (CapsCCObj != null) {
				String CapsCCObjUN = (String) CapsCCObj.getUniqueName();
				if ("003".equals(CapsCCObjUN) || "002".equals(CapsCCObjUN)) {
					isSUTPresent = true;
				}
			}
		}

		for (int i = 0; i < lineItems.size(); i++) {
			BaseObject li = (BaseObject) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getFieldValue("LineType");
			if (plt != null && plt.getCategory() == ProcureLineType.SpecialChargeCategory) {
				specialChargePresent = true;
			}
		}

		if (specialChargePresent && !isSUTPresent) {
			return false;
		}
		return true;
	}

	public static void setTermsDiscountPercent(BaseVector lineItems) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering setTermsDiscountPercent method", ClassName);

		Invoice invoice = (Invoice) lineItems.getClusterRoot();

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Invoice Name is %s", ClassName, invoice.getUniqueName());

		if (invoice != null) {
			BigDecimal invTermsDisc = (BigDecimal) invoice.getDottedFieldValue("TermsDiscount");
			if (invTermsDisc == null) {
				invTermsDisc = new BigDecimal("0.00");
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: The terms discount on the invoice is %s", ClassName, invTermsDisc.toString());

			/*
			String supTermsDiscStrg = (String) invoice.getDottedFieldValue("SupplierLocation.DiscountPercent");
			BigDecimal supTermsDisc = new BigDecimal("0.00");
			if (supTermsDiscStrg != null) {
				supTermsDisc = new BigDecimal(supTermsDiscStrg);
				supTermsDisc = supTermsDisc.setScale(2, BigDecimal.ROUND_UP);
			}

			if (Log.customer.debugOn)
				Log.customer.debug("%s ::: The Supplier terms discount is %s", ClassName, supTermsDisc.toString());

			boolean useSupTerms = false;
			boolean useInvTerms = false;

			if (invTermsDisc.compareTo(supTermsDisc) < 0) {
				//Apply Supplier's Terms Discount
				if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Using the supplier's terms discount %s", ClassName, supTermsDisc.toString());
				useSupTerms = true;
			}
			else {
				//Apply Invoice Terms Discount
				if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Using the invoice terms discount %s", ClassName, invTermsDisc.toString());
				useInvTerms = true;
			}
			*/
			InvoiceLineItem invoiceLineItem = null;

			for (int i = 0; i < lineItems.size(); i++) {
				invoiceLineItem = (InvoiceLineItem) lineItems.get(i);
				/*
				if (useSupTerms) {
					if (Log.customer.debugOn)
						Log.customer.debug(
							"%s ::: Setting inv line item discount to supplier's terms discount %s",
							ClassName,
							supTermsDisc.toString());
					invoiceLineItem.setDottedFieldValue("TermsDiscountPercent", supTermsDisc.toString());
				}
				else {
					if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Setting inv line item discount to invoice terms discount %s", ClassName, invTermsDisc.toString());
					invoiceLineItem.setDottedFieldValue("TermsDiscountPercent", invTermsDisc.toString());
				}
				*/
				//Changed the field definition from String to BigDecimal for CAPSIntegration
				invoiceLineItem.setDottedFieldValue("TermsDiscountPercent", invTermsDisc);
			}
		}
		else {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: The invoice object passed into the method is null, hence skipping", ClassName);
		}
	}
}