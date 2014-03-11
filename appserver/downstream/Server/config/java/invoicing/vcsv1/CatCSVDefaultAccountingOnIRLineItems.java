/*
 *
Change history
	Chandra 	31-01-08    Fixed defaulting of Tax and Accounting fields for invoices against contracts.
							The pli is set to maline if orderline is null. If accounnting is null in maline, invline is used.
							Added comments.

	Dibya Prakash 11-09-08  Considering Acc distribution from Invoice always. Issue-845
	IBM AMS Lekshmi 09-03-2012 Issue 249 : Fix for Tax Line's Expense Account defaulting
 *
 */
package config.java.invoicing.vcsv1;

import java.math.BigDecimal;
import java.util.Iterator;

import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.common.core.AccountType;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.SplitAccountingType;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.StringUtil;

public class CatCSVDefaultAccountingOnIRLineItems {
	public static final String ClassName = "CatCSVDefaultAccountingOnIRLineItems";

	public static void defaultAccountingOnLines(InvoiceReconciliation ir) {
		Log.customer.debug("%s ::: Entering the defaultAccountingOnLines",
				ClassName);

		if (!ir.getInvoice().isStandardInvoice()) {
			Log.customer
					.debug(
							"%s ::: Didn't default the line item accountings as not a standard invoice",
							ClassName);
			return;
		}

		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		ProcureLineItem pli = null;

		// Get the last material line on the invoice
		Log.customer
				.debug("%s ::: Calling the getLastMatLineOnPOMA", ClassName);
		ProcureLineItem lastMatLineOnInv = getLastMatLineOnPOMA(ir);
		Log.customer.debug("%s ::: The last meterial line on the PO/MA is: %s",
				ClassName, lastMatLineOnInv);

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			ClusterRoot capsChargeCodeObj = (ClusterRoot) irli
					.getFieldValue("CapsChargeCode");
			String capsChargeCodeString = null;
			if (capsChargeCodeObj != null) {
				capsChargeCodeString = capsChargeCodeObj.getUniqueName();
			} else {
				capsChargeCodeString = "";
			}

			if ((irli.getLineType() != null)
					&& ((irli.getLineType().getCategory() != ProcureLineType.LineItemCategory) || ((irli
							.getLineType().getCategory() == ProcureLineType.LineItemCategory)
							&& (!capsChargeCodeString.equals("001"))
							&& (!capsChargeCodeString.equals("007")) && (!capsChargeCodeString
							.equals("019"))))) {

				Log.customer
						.debug(
								"%s ::: Entering special accounting assignments implementation",
								ClassName);
				pli = getProcureLineItem(irli);
				if (pli == null) {
					Log.customer
							.debug(
									"%s ::: Had to use the last material line for the ir line",
									ClassName);
					pli = lastMatLineOnInv;
				}
				if (pli == null) {
					Log.customer
							.debug(
									"%s ::: Encountered a null pli, skipping accounting assignment for this line",
									ClassName);
					continue;
				}
				// In case pli is not null, it could be either an orderline or
				// Maline or invoice line.
				// To Note, that the MAline may not have the accounting, in that
				// case accoutning info would be got from invoice line
				// and tax info got from procure line
				if (irli.getLineType().getCategory() == ProcureLineType.TaxChargeCategory) {
					if (!ir.getInvoice().getIsTaxInLine()) {
						// header level tax
						Log.customer.debug(
								"%s ::: Encountered header level tax",
								ClassName);
						// Header leveltax line setup with accoutning
						defaultAccountingForTaxLine(irli, pli);
					} else {
						// Line Level Tax - don't do anything for accounting as
						// this will be rejected
						Log.customer
								.debug(
										"%s ::: Encountered line level tax, won't assign accounting",
										ClassName);
					}
					// Setting the tax fields on tax line based on HJW request
					irli.setFieldValue("TaxUse", (ClusterRoot) pli
							.getFieldValue("TaxUse"));
					irli.setFieldValue("TaxQualifier", (String) pli
							.getFieldValue("TaxQualifier"));
				} else {
					if (capsChargeCodeString.equals("010")) {
						Log.customer
								.debug(
										"%s ::: Encountered line for State Motor Fuel Tax",
										ClassName);
						defaultAccountingForStateMotorFuelTax(irli, pli);
					}

					// Code added for Issue-845

					/*
					 * else if (capsChargeCodeString.equals("019")) {
					 * Log.customer
					 * .debug("%s ::: Encountered line for Freight In Bound",
					 * ClassName); defaultAccountingForFreightInBound(irli,
					 * pli); }
					 */
					else if (capsChargeCodeString.equals("0FH")) {
						Log.customer.debug(
								"%s ::: Encountered line for Indiana Fuel Tax",
								ClassName);
						defaultAccountingForINFuelTax(irli, pli);
					} else if (capsChargeCodeString.equals("0FL")) {
						Log.customer
								.debug(
										"%s ::: Encountered line for Illinois Fuel Tax",
										ClassName);
						defaultAccountingForILFuelTax(irli, pli);
					} else if (capsChargeCodeString.equals("0GF")) {
						Log.customer
								.debug(
										"%s ::: Encountered line for Georgia Motor Fuel Tax",
										ClassName);
						defaultAccountingForGAMotorFuelTax(irli, pli);
					} else {
						Log.customer
								.debug(
										"%s ::: Encountered line where accounting defaults from the last material line",
										ClassName);
						defaultAccountingForOtherCharges(irli, pli);
					}
				}
			} else {
				Log.customer.debug(
						"%s :::In else portion as caps charge is 001 or 007 ",
						ClassName);
				pli = getProcureLineItem(irli);

				if (pli != null) {

					irli.setFieldValue("ProjectNumber", pli
							.getFieldValue("ProjectNumber"));

					Log.customer.debug(
							"%s ::: for lines with caps 001, accountings are ="
									+ pli.getAccountings(), ClassName);
					// MALine may not have the accountings, use the invoice line
					// instead
					// TBD Note: if MALine does have accouting, invoice
					// accounting will be overridden
					if (pli.getAccountings() == null
							&& pli instanceof ContractLineItem) {
						pli = irli.getInvoiceLineItem();
					}

					irli.setFieldValue("AccountType", pli
							.getFieldValue("AccountType"));
					if (irli.getInvoiceLineItem() != null
							&& !(pli instanceof InvoiceLineItem)) {
						Log.customer
								.debug(
										"%s ::: pli is %s setting accouttype ininvoiceline of irli= %s ",
										ClassName, pli, irli);
						irli.getInvoiceLineItem().setFieldValue("AccountType",
								pli.getFieldValue("AccountType"));
					}

				}
			}
		}
	}

	public static ProcureLineItem getLastMatLineOnPOMA(InvoiceReconciliation ir) {

		Log.customer.debug("%s ::: In method getLastMatLineOnPOMA", ClassName);

		ProcureLineItemCollection plic = ir.getOrder();
		if (plic == null) {
			Log.customer.debug("%s ::: Getting the MasterAgreement as IAC",
					ClassName);
			// plic = ir.getInvoice();
			plic = ir.getMasterAgreement();
		}

		if (plic != null) {
			BaseVector pLineItems = (BaseVector) plic.getLineItems();
			ProcureLineItem pli = null;
			ClusterRoot capsChargeCodeObj = null;
			boolean foundMatLine = false;

			for (int i = pLineItems.size(); (i >= 1) && (!foundMatLine); i--) {
				pli = (ProcureLineItem) pLineItems.get(i - 1);
				capsChargeCodeObj = (ClusterRoot) pli.getDescription()
						.getFieldValue("CAPSChargeCode");
				String capsChargeCodeString = null;
				if (capsChargeCodeObj != null) {
					capsChargeCodeString = capsChargeCodeObj.getUniqueName();
					Log.customer.debug("%s ::: CAPS Charge Code is: %s",
							ClassName, capsChargeCodeString);
				} else {
					Log.customer.debug(
							"%s ::: Encountered a null CAPS Charge Code",
							ClassName);
					capsChargeCodeString = "";
				}

				if (capsChargeCodeString.equals("001")) {
					Log.customer.debug(
							"%s ::: Found the last material line: %s",
							ClassName, pli.toString());
					foundMatLine = true;
				}
			}
			return pli;
		}
		return null;
	}

	public static ProcureLineItem getProcureLineItem(
			InvoiceReconciliationLineItem irli) {

		Log.customer.debug("%s ::: Entering method getProcureLineItem",
				ClassName);
		ClusterRoot capsChargeCodeObj = (ClusterRoot) irli
				.getFieldValue("CapsChargeCode");
		String capsChargeCodeString = null;
		if (capsChargeCodeObj != null) {
			capsChargeCodeString = capsChargeCodeObj.getUniqueName();
			Log.customer.debug("%s ::: CAPS Charge Code is: %s", ClassName,
					capsChargeCodeString);
		} else {
			Log.customer.debug("%s ::: Encountered null CAPS Charge Code",
					ClassName);
			capsChargeCodeString = "";
		}

		ProcureLineItemCollection plic = null;
		ProcureLineItem pli = irli.getOrderLineItem();
		if (pli == null) {
			Log.customer.debug("%s ::: Getting MA Line Item as IAC", ClassName);
			if (irli.getMasterAgreement() != null) {
				// Commenting the below portion.Invoiceline was got.
				// Now it would use MALine, if null, then use invoice line

				// int isMANoRelease =
				// (irli.getMasterAgreement()).getReleaseType();
				// if (isMANoRelease == 0) {
				// pli = irli.getInvoiceLineItem();
				// }
				// else {
				// pli = irli.getMALineItem();
				// }
				pli = irli.getMALineItem();
			}
			if (pli == null)
				pli = irli.getInvoiceLineItem();

			Log.customer.debug("%s ::: pli111 is %s ", ClassName, pli);

		}

		// Code added for Issue-845
		if (!capsChargeCodeString.equals("001") && (pli != null)
				&& !capsChargeCodeString.equals("007")
				&& !capsChargeCodeString.equals("019")) {

			Log.customer
					.debug(
							"%s ::: CAPS Charge code is not 001/007/019, hence look for referenced line",
							ClassName);
			plic = (ProcureLineItemCollection) pli.getLineItemCollection();
			Log.customer.debug("%s ::: plic is : %s", ClassName, plic);
			if (pli.getFieldValue("ReferenceLineNumber") != null) {
				int refNum = ((Integer) pli
						.getFieldValue("ReferenceLineNumber")).intValue();
				Log.customer.debug("%s ::: RefNum is : %s", ClassName, refNum);

				if (refNum != 0) {

					pli = (ProcureLineItem) plic.getLineItem(refNum);
					Log.customer.debug("%s ::: pli11 is : %s", ClassName, pli);

				} else {
					pli = null;
					Log.customer.debug("%s ::: pli is null ", ClassName);
				}
				Log.customer.debug("%s ::: Referenced line is pli: %s",
						ClassName, pli);
			}
		}
		if (pli != null) {
			// This will return the pli in case of invoice lines that existed on
			// the po/ma
			return pli;
		}
		// Following is executed for the lines which didn't exist on the po/ma
		return null;
	}

	public static void defaultAccountingForTaxLine(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {

		Log.customer
				.debug("%s ::: Entering method defaultAccountingForTaxLine",
						ClassName);
		Money irliAmount = irli.getAmount();
		String capsChargeCodeString = null;
		boolean isExpense = false;
		boolean isSalesTax = false;
		boolean isSUT = false;
		boolean isVAT = false;
		Log.customer.debug("%s ::: defaultAccountingForTaxLine IR=" + irli
				+ "PLI=" + pli, ClassName);

		ClusterRoot capsChargeCodeObj = (ClusterRoot) irli
				.getFieldValue("CapsChargeCode");
		if (capsChargeCodeObj != null) {
			capsChargeCodeString = capsChargeCodeObj.getUniqueName();
			Log.customer.debug("%s ::: CAPS Charge Code is: %s", ClassName,
					capsChargeCodeString);
		} else {
			Log.customer.debug("%s ::: Encountered null CAPS Charge Code",
					ClassName);
			capsChargeCodeString = "";
		}

		if (capsChargeCodeString.equals("002")) {
			Log.customer.debug("%s ::: This IR line is a Sales Tax Line",
					ClassName);
			isSalesTax = true;
		}
		if (capsChargeCodeString.equals("003")) {
			Log.customer.debug("%s ::: This IR line is a SUT Line", ClassName);
			isSUT = true;
		}
		if (capsChargeCodeString.equals("096")) {
			Log.customer.debug("%s ::: This IR line is a VAT Line", ClassName);
			isVAT = true;
		}
		// Non accounting details
		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();

		}

		String accountType = (String) pli
				.getDottedFieldValue("AccountType.UniqueName");
		Log.customer
				.debug("%s ::: The Account Type =" + accountType, ClassName);

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));

		// setting
		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}

		// Issue 249 Fix for Tax Line's Expense Account defaulting

		if (StringUtil.nullOrEmptyOrBlankString(accountType)) {
			AccountType accTypeObj = null;

			// Defaulting The AccountType from Invoice as the IR is still not
			// Submitted.
			accTypeObj = getTaxAccountTypefromOtherInvoiceLine(irli);
			Log.customer
					.debug(
							"AccountypeObj value after getTaxAccountTypefromInvoiceLine method in %s is %s",
							ClassName, accTypeObj);
			try {
				accountType = accTypeObj.getUniqueName();
			} catch (NullPointerException npe) {
				Log.customer.debug("Caught Null Pointer Exception %s in %s ",
						npe, ClassName);
			}
			Log.customer
					.debug(
							"Accountype value after getTaxAccountTypefromInvoiceLine method in %s ",
							ClassName);
			irli.setFieldValue("AccountType", accTypeObj);
			irli.getInvoiceLineItem().setFieldValue("AccountType", accTypeObj);
		}

		// End Issue 249

		if (!StringUtil.nullOrEmptyOrBlankString(accountType)
				&& "Expense".equals(accountType)) {
			Log.customer.debug(
					"%s ::: The Account Type on the IR line is Expense",
					ClassName);
			isExpense = true;
		}
		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		if (!isVAT && pliSAC != null) {
			boolean firstSplit = true;
			SplitAccounting sa = null;

			for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
				SplitAccounting pliSA = (SplitAccounting) pliSAC
						.getSplitAccountings().get(i);
				String fac = (String) pliSA.getFieldValue("AccountingFacility");
				String dept = (String) pliSA.getFieldValue("Department");
				String div = (String) pliSA.getFieldValue("Division");
				String sect = (String) pliSA.getFieldValue("Section");
				String exp = (String) pliSA.getFieldValue("ExpenseAccount");
				String order = (String) pliSA.getFieldValue("Order");
				String misc = (String) pliSA.getFieldValue("Misc");
				BigDecimal percent = (BigDecimal) pliSA.getPercentage();

				percent = percent.divide(new BigDecimal("100.00000"), 5,
						BigDecimal.ROUND_HALF_UP);
				Money saAmount = Money.multiply(irliAmount, percent);
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
				if (StringUtil.nullOrEmptyOrBlankString(fac)
						|| StringUtil.nullOrEmptyOrBlankString(dept)
						|| StringUtil.nullOrEmptyOrBlankString(div)
						|| StringUtil.nullOrEmptyOrBlankString(sect)
						|| StringUtil.nullOrEmptyOrBlankString(exp)) {
					Log.customer.debug("%s ::: Entering to If", ClassName);
					SplitAccounting invMaterialLineSA = copyFromInvMaterialLine(
							irli, i);
					fac = (String) invMaterialLineSA
							.getFieldValue("AccountingFacility");
					dept = (String) invMaterialLineSA
							.getFieldValue("Department");
					div = (String) invMaterialLineSA.getFieldValue("Division");
					sect = (String) invMaterialLineSA.getFieldValue("Section");
					exp = (String) invMaterialLineSA
							.getFieldValue("ExpenseAccount");
					order = (String) invMaterialLineSA.getFieldValue("Order");
					misc = (String) invMaterialLineSA.getFieldValue("Misc");
					percent = (BigDecimal) invMaterialLineSA.getPercentage();

					percent = percent.divide(new BigDecimal("100.00000"), 5,
							BigDecimal.ROUND_HALF_UP);
					saAmount = Money.multiply(irliAmount, percent);
					Log.customer
							.debug(
									"%s ::: The percentage on the copied Inv  split accounting is %s",
									ClassName, percent.toString());
					Log.customer
							.debug(
									"%s ::: The li amount oon the copied Inv  split split accounting is %s",
									ClassName, saAmount.toString());
					Log.customer.debug("%s ::: Exit from If", ClassName);
				}
				if (firstSplit) {
					firstSplit = false;
				} else {
					irliSAC.addSplit();
				}

				sa = (SplitAccounting) irliSAC.getSplitAccountings()
						.lastElement();
				sa.setFieldValue("Amount", saAmount);
				sa.setFieldValue("AccountingFacility", fac);
				sa.setFieldValue("Department", dept);
				sa.setFieldValue("Division", div);
				sa.setFieldValue("Section", sect);

				if ((isSalesTax && isExpense) || (isSUT && isExpense)) {
					sa.setFieldValue("ExpenseAccount", "2615");
				} else {
					sa.setFieldValue("ExpenseAccount", exp);
				}
				sa.setFieldValue("Order", order);
				sa.setFieldValue("Misc", misc);
			}
		} else {
			String suppShipFrom = irli.getSupplierLocation().getCountry()
					.getUniqueName();
			String cityStateCode = (String) irli.getSupplierLocation()
					.getFieldValue("CityStateCode");

			if (StringUtil.nullOrEmptyOrBlankString(cityStateCode)) {
				cityStateCode = "";
			}
			String currencyOnIR = irli.getAmount().getCurrency()
					.getUniqueName();

			if ("GB".equals(suppShipFrom) && cityStateCode.startsWith("GB")
					&& "USD".equals(currencyOnIR)) {
				SplitAccounting sa = (SplitAccounting) irliSAC
						.getSplitAccountings().lastElement();
				sa.setFieldValue("Amount", irli.getAmount());
				sa.setFieldValue("AccountingFacility", "01");
				sa.setFieldValue("Department", "A3060");
				sa.setFieldValue("Division", "009");
				sa.setFieldValue("Section", "01");
				sa.setFieldValue("ExpenseAccount", "0000");
			} else if ("GB".equals(suppShipFrom)
					&& cityStateCode.startsWith("GB")
					&& "GBP".equals(currencyOnIR)) {
				SplitAccounting sa = (SplitAccounting) irliSAC
						.getSplitAccountings().lastElement();
				sa.setFieldValue("Amount", irli.getAmount());
				sa.setFieldValue("AccountingFacility", "01");
				sa.setFieldValue("Department", "A3060");
				sa.setFieldValue("Division", "009");
				sa.setFieldValue("Section", "95");
				sa.setFieldValue("ExpenseAccount", "0000");
			} else {
				SplitAccounting sa = (SplitAccounting) irliSAC
						.getSplitAccountings().lastElement();
				sa.setFieldValue("Amount", irli.getAmount());
				sa.setFieldValue("AccountingFacility", "01");
				sa.setFieldValue("Department", "S1737");
				sa.setFieldValue("Division", "010");
				sa.setFieldValue("Section", "00");
				sa.setFieldValue("ExpenseAccount", "0000");
			}
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	private static SplitAccounting copyFromInvMaterialLine(
			InvoiceReconciliationLineItem irli, int i) {
		Log.customer.debug("Entering copyFromInvMaterialLine method in ",
				ClassName);
		Iterator lineIterator = irli.getInvoice().getAllLineItems();
		SplitAccounting sa = new SplitAccounting();
		String facility = "";
		while (lineIterator.hasNext()) {
			Log.customer.debug(
					"Inside While of copyFromInvMaterialLine method ",
					ClassName);
			InvoiceLineItem invLine = (InvoiceLineItem) lineIterator.next();
			SplitAccountingCollection irliSAC = new SplitAccountingCollection(
					new SplitAccounting(invLine.getPartition()));
			irliSAC = invLine.getAccountings();
			int size = irliSAC.getSplitAccountings().size();
			Log.customer.debug("Split Acc Size", size);
			if (i < size) {
				sa = (SplitAccounting) irliSAC.getSplitAccountings().get(i);
				Log.customer.debug("Split Acc Value", sa);
				facility = (String) sa.getFieldValue("Facility");
				Log.customer.debug("Facility Value", facility);
			}
			if (!StringUtil.nullOrEmptyOrBlankString(facility)) {
				break;
			}

		}
		Log.customer
				.debug("Exit copyFromInvMaterialLine method in ", ClassName);
		return sa;
	}

	// Defaulting Account Type from Other Material Line

	private static AccountType getTaxAccountTypefromOtherInvoiceLine(
			InvoiceReconciliationLineItem irli) {
		Log.customer.debug(
				"Entering setTaxAccountTypefromInvoiceLine method in ",
				ClassName);
		AccountType accType = null;
		try {
			// Fetch the AccountType from Invoice object which is submitted for
			// the IR
			Iterator lineIterator = irli.getInvoice().getAllLineItems();
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

	public static void defaultAccountingForStateMotorFuelTax(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {
		Money irliAmount = irli.getAmount();

		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();
		}

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));

		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}

		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		// irliSAC.setType(pliSAC.getType());
		boolean firstSplit = true;
		SplitAccounting sa = null;

		if (pliSAC == null)
			return;
		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
			SplitAccounting pliSA = (SplitAccounting) pliSAC
					.getSplitAccountings().get(i);
			String fac = (String) pliSA.getFieldValue("AccountingFacility");
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5,
					BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			{
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
			}

			if (firstSplit) {
				firstSplit = false;
			} else {
				irliSAC.addSplit();
			}

			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", fac);
			sa.setFieldValue("Department", "S1739");
			sa.setFieldValue("Division", "000");
			sa.setFieldValue("Section", "00");
			sa.setFieldValue("ExpenseAccount", "0000");
			sa.setFieldValue("Order", "00000");
			sa.setFieldValue("Misc", "000");
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	// Code Commented for Issue-845
	/*
	 * public static void
	 * defaultAccountingForFreightInBound(InvoiceReconciliationLineItem irli,
	 * ProcureLineItem pli) { Money irliAmount = irli.getAmount(); boolean
	 * isExpense = false;
	 * 
	 * //Get split accoutning from pli SplitAccountingCollection pliSAC =
	 * pli.getAccountings(); Log.customer.debug("%s ::: pliSAC =" + pliSAC,
	 * ClassName);
	 * 
	 * irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));
	 * 
	 * //MALine may not have the accountings, use the invoice line instead //TBD
	 * Note: if MALine does have accouting, invoice accounting will be
	 * overridden if(pliSAC == null && pli instanceof MALineItem) { pli =
	 * irli.getInvoiceLineItem(); pliSAC = pli.getAccountings(); }
	 * 
	 * String accountType = (String)
	 * pli.getDottedFieldValue("AccountType.UniqueName");
	 * Log.customer.debug("%s ::: The Account Type ="+accountType, ClassName);
	 * if (!StringUtil.nullOrEmptyOrBlankString(accountType)) { if
	 * ("Expense".equals(accountType)) { isExpense = true; } }
	 * 
	 * irli.setFieldValue("AccountType", pli.getFieldValue("AccountType")); if
	 * (irli.getInvoiceLineItem() != null && !(pli instanceof InvoiceLineItem))
	 * { irli.getInvoiceLineItem().setFieldValue("AccountType",
	 * pli.getFieldValue("AccountType")); }
	 * 
	 * SplitAccountingCollection irliSAC = new SplitAccountingCollection(new
	 * SplitAccounting(irli.getPartition()));
	 * //irliSAC.setType(pliSAC.getType()); boolean firstSplit = true;
	 * SplitAccounting sa = null; if(pliSAC == null) return; for (int i = 0; i <
	 * pliSAC.getSplitAccountings().size(); i++) { SplitAccounting pliSA =
	 * (SplitAccounting) pliSAC.getSplitAccountings().get(i); String fac =
	 * (String) pliSA.getFieldValue("AccountingFacility"); String dept =
	 * (String) pliSA.getFieldValue("Department"); String div = (String)
	 * pliSA.getFieldValue("Division"); String sect = (String)
	 * pliSA.getFieldValue("Section"); String exp = (String)
	 * pliSA.getFieldValue("ExpenseAccount"); String order = (String)
	 * pliSA.getFieldValue("Order"); String misc = (String)
	 * pliSA.getFieldValue("Misc"); BigDecimal percent = (BigDecimal)
	 * pliSA.getPercentage(); percent = percent.divide(new
	 * BigDecimal("100.00000"), 5, BigDecimal.ROUND_HALF_UP); Money saAmount =
	 * Money.multiply(irliAmount, percent);
	 * 
	 * {Log.customer.debug(
	 * "%s ::: The percentage on the procure split accounting is %s", ClassName,
	 * percent.toString());Log.customer.debug(
	 * "%s ::: The li amount on the procure split accounting is %s", ClassName,
	 * saAmount.toString()); }
	 * 
	 * if (firstSplit) { firstSplit = false; } else { irliSAC.addSplit(); }
	 * 
	 * sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
	 * sa.setFieldValue("Amount", saAmount);
	 * sa.setFieldValue("AccountingFacility", fac);
	 * sa.setFieldValue("Department", dept); sa.setFieldValue("Division", div);
	 * sa.setFieldValue("Section", sect); if (isExpense) {
	 * sa.setFieldValue("ExpenseAccount", "2602"); } else {
	 * sa.setFieldValue("ExpenseAccount", exp); } sa.setFieldValue("Order",
	 * order); sa.setFieldValue("Misc", misc); }
	 * irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC); }
	 */

	public static void defaultAccountingForINFuelTax(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {

		Money irliAmount = irli.getAmount();
		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();
		}

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));
		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}

		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		// irliSAC.setType(pliSAC.getType());
		boolean firstSplit = true;
		SplitAccounting sa = null;
		if (pliSAC == null)
			return;

		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
			SplitAccounting pliSA = (SplitAccounting) pliSAC
					.getSplitAccountings().get(i);
			String fac = (String) pliSA.getFieldValue("AccountingFacility");
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5,
					BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			{
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
			}

			if (firstSplit) {
				firstSplit = false;
			} else {
				irliSAC.addSplit();
			}

			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", fac);
			sa.setFieldValue("Department", "S1739");
			sa.setFieldValue("Division", "000");
			sa.setFieldValue("Section", "00");
			sa.setFieldValue("ExpenseAccount", "0000");
			sa.setFieldValue("Order", "00000");
			sa.setFieldValue("Misc", "000");
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	public static void defaultAccountingForILFuelTax(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {
		Money irliAmount = irli.getAmount();

		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();
		}

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));
		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}

		if (pliSAC == null)
			return;
		boolean firstSplit = true;
		SplitAccounting sa = null;

		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
			SplitAccounting pliSA = (SplitAccounting) pliSAC
					.getSplitAccountings().get(i);
			String fac = (String) pliSA.getFieldValue("AccountingFacility");
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5,
					BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			{
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
			}

			if (firstSplit) {
				firstSplit = false;
			} else {
				irliSAC.addSplit();
			}
			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", fac);
			sa.setFieldValue("Department", "S1739");
			sa.setFieldValue("Division", "000");
			sa.setFieldValue("Section", "00");
			sa.setFieldValue("ExpenseAccount", "0000");
			sa.setFieldValue("Order", "00000");
			sa.setFieldValue("Misc", "000");
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	public static void defaultAccountingForGAMotorFuelTax(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {
		Money irliAmount = irli.getAmount();

		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();
		}

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));
		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}

		if (pliSAC == null)
			return;
		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		// irliSAC.setType(pliSAC.getType());
		boolean firstSplit = true;
		SplitAccounting sa = null;

		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
			SplitAccounting pliSA = (SplitAccounting) pliSAC
					.getSplitAccountings().get(i);
			String fac = (String) pliSA.getFieldValue("AccountingFacility");
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5,
					BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			{
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
			}

			if (firstSplit) {
				firstSplit = false;
			} else {
				irliSAC.addSplit();
			}

			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", fac);
			sa.setFieldValue("Department", "S1739");
			sa.setFieldValue("Division", "000");
			sa.setFieldValue("Section", "00");
			sa.setFieldValue("ExpenseAccount", "0000");
			sa.setFieldValue("Order", "00000");
			sa.setFieldValue("Misc", "000");
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}

	public static void defaultAccountingForOtherCharges(
			InvoiceReconciliationLineItem irli, ProcureLineItem pli) {
		Money irliAmount = irli.getAmount();

		// Get split accoutning from pli
		SplitAccountingCollection pliSAC = pli.getAccountings();
		Log.customer.debug("%s ::: pliSAC =" + pliSAC, ClassName);

		irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));

		// MALine may not have the accountings, use the invoice line instead
		// TBD Note: if MALine does have accouting, invoice accounting will be
		// overridden
		if (pliSAC == null && pli instanceof ContractLineItem) {
			pli = irli.getInvoiceLineItem();
			pliSAC = pli.getAccountings();
		}

		irli.setFieldValue("AccountType", pli.getFieldValue("AccountType"));
		if (irli.getInvoiceLineItem() != null
				&& !(pli instanceof InvoiceLineItem)) {
			irli.getInvoiceLineItem().setFieldValue("AccountType",
					pli.getFieldValue("AccountType"));
		}
		if (pliSAC == null)
			return;

		SplitAccountingCollection irliSAC = new SplitAccountingCollection(
				new SplitAccounting(irli.getPartition()));
		irliSAC.setType(SplitAccountingType.getPercentageType(irli
				.getPartition()));
		boolean firstSplit = true;
		SplitAccounting sa = null;

		for (int i = 0; i < pliSAC.getSplitAccountings().size(); i++) {
			SplitAccounting pliSA = (SplitAccounting) pliSAC
					.getSplitAccountings().get(i);
			String fac = (String) pliSA.getFieldValue("AccountingFacility");
			String dept = (String) pliSA.getFieldValue("Department");
			String div = (String) pliSA.getFieldValue("Division");
			String sect = (String) pliSA.getFieldValue("Section");
			String exp = (String) pliSA.getFieldValue("ExpenseAccount");
			String order = (String) pliSA.getFieldValue("Order");
			String misc = (String) pliSA.getFieldValue("Misc");
			BigDecimal percent = (BigDecimal) pliSA.getPercentage();
			percent = percent.divide(new BigDecimal("100.00000"), 5,
					BigDecimal.ROUND_HALF_UP);
			Money saAmount = Money.multiply(irliAmount, percent);

			{
				Log.customer
						.debug(
								"%s ::: The percentage on the procure split accounting is %s",
								ClassName, percent.toString());
				Log.customer
						.debug(
								"%s ::: The li amount on the procure split accounting is %s",
								ClassName, saAmount.toString());
			}

			if (firstSplit) {
				firstSplit = false;
			} else {
				irliSAC.addSplit();
			}

			sa = (SplitAccounting) irliSAC.getSplitAccountings().lastElement();
			// sa.setPercentage(percent);
			sa.setFieldValue("Amount", saAmount);
			sa.setFieldValue("AccountingFacility", fac);
			sa.setFieldValue("Department", dept);
			sa.setFieldValue("Division", div);
			sa.setFieldValue("Section", sect);
			sa.setFieldValue("ExpenseAccount", exp);
			sa.setFieldValue("Order", order);
			sa.setFieldValue("Misc", misc);
		}
		irli.setDottedFieldValueRespectingUserData("Accountings", irliSAC);
	}
}