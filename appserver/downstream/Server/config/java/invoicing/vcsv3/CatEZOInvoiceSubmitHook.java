package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;

/**
 * @author KS.
 *	Validates a bunch of stuff (see case error messages).
 */

public class CatEZOInvoiceSubmitHook implements ApprovableHook {

	private static final String ClassName = "CatEZOInvoiceSubmitHook";
	private static final String ComponentStringTable = "cat.invoicejava.vcsv3";
	private static final int ValidationError = -1;
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));
    private String fmt;
    private Money computedVAT;
    private Money enteredVAT;

	public List run(Approvable approvable) {
		Invoice inv = (Invoice) approvable;
		BaseVector invLineItems = (BaseVector) inv.getLineItems();

		if ((inv.getLoadedFrom() == Invoice.LoadedFromFile) || (inv.getLoadedFrom() == Invoice.LoadedFromACSN)) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("This is a File or ASN loaded Invoice");
		}

		if (inv.getLoadedFrom() == Invoice.LoadedFromEForm) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("This is a EForm loaded Invoice");
		}

		if (inv.getLoadedFrom() == Invoice.LoadedFromUI) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("This is a UI loaded Invoice, here we go - Validating!");

			computedVAT = null; enteredVAT = null;
			int errorResult = findErrors (inv);
			if (errorResult > 0) {
			    switch (errorResult) {
			    	case 1:
			    	    fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFTaxLinesError");
			    	    break;
			    	case 2:
			    	    fmt = Fmt.Sil(ComponentStringTable, "Hook_INVLineLevelVATError");
			    	    break;
			        case 3:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVCurrencyMismatchError");
			            break;
			        case 4:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFLineVATCurrencyError");
			            break;
			        case 5:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVCurrencyMismatchError_MA");
			            break;
			        case 6:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVPayTermsMismatchError");
			            break;
			        case 7:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVBillToMismatchError");
			            break;
			        case 8:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFMissingVATLineError");
			            break;
			        case 9:
				        //if (Log.customer.debugOn)
				            Log.customer.debug("%s *** enteredVAT, computedVAT: %s, %s",ClassName,
				                    enteredVAT.getAmount(),computedVAT.getAmount());
			            if (computedVAT != null && enteredVAT != null) {
			                fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFVATCalculationError_Detail");
		            		fmt = Fmt.S(fmt,enteredVAT.asString(),computedVAT.asString());
			            }
			            else
			                fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFVATCalculationError");
			            break;
			        case 10:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVPayTermsChangeError");
			            break;
			        case 11:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVNoMaterialLineError");
			            break;
			        case 12:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INEFZeroLineAmountError");
			            break;
			        case 13:
			            fmt = Fmt.Sil(ComponentStringTable, "AcctngValidation_AcctFacMismatch");
			            break;
			        case 14:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVCatInvoiceNumError_Prefix");
			            break;
			        case 15:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVCatInvoiceNumError_Suffix");
			            break;
			        case 20:
			            fmt = Fmt.Sil(ComponentStringTable, "Hook_INVCatInvoiceNumError_Duplicate");
			    }
			    return ListUtil.list(Constants.getInteger(ValidationError), fmt);
			}
		}
		return NoErrorResult;
	}

	private int findErrors(Invoice invoice) {

	    boolean nonVATLineFound = false;
	    boolean vatLineFound = false;
	    Object baseTerms = null;
	    ClusterRoot baseBillTo = null;
	    Currency baseCurrency = null;
	    int matLineCount = 0;
		String acctFac = null;
		List maList = ListUtil.list();

	    ClusterRoot currentPayTerms = invoice.getPaymentTerms();
	    String catInvoiceNum = (String)invoice.getFieldValue("CatInvoiceNumber");
	    Money computedTaxAmount = new Money(Constants.ZeroBigDecimal, invoice.getTotalCost().getCurrency());
	    Money vatAmount = new Money(Constants.ZeroBigDecimal, invoice.getTotalCost().getCurrency());

	    List lineItems = invoice.getLineItems();
	    int size = lineItems.size();
	    for (int i=0; i < size; i++) {

	        InvoiceLineItem li = (InvoiceLineItem) lineItems.get(i);
			Money lineAmt = li.getAmount();

			// 12.09.09 Ensure all lines have non-zero value
		    if (lineAmt.isZero()) {
		        return 12;
		    }

			// Check for multiple VAT lines or line level VAT
			ProcureLineType plt = (ProcureLineType)li.getLineType();
			if (plt.getUniqueName().equals("VATCharge")) {
			    if (vatLineFound)
			        return 1;

				if ((li.getDescription().getShortName().indexOf("line level") > 0)
						|| (((String) li.getFieldValue("MatchedToString")).indexOf("Item") > 0)) {
				    return 2;
				}
			    vatLineFound = true;
			    vatAmount = lineAmt;
			}
			else {
			    nonVATLineFound = true;
				Money taxAmount = (Money)li.getFieldValue("TaxAmount");
				if (taxAmount != null)
					computedTaxAmount = Money.add(computedTaxAmount, taxAmount);

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** computedTaxAmount: %s", ClassName,computedTaxAmount);

				// keep count of material lines
				if (plt.getCategory() == ProcureLineType.LineItemCategory) {
				    matLineCount++;

				    // 12.09.09 Check for common AccountingFacility (all lines, first split only)
					List splits = li.getAccountings().getSplitAccountings();
					if (!splits.isEmpty()) {
						SplitAccounting sa = (SplitAccounting) splits.get(0);
						if (matLineCount==1) {
						    acctFac = (String)sa.getFieldValue("AccountingFacility");
						    if (acctFac == null)
						        return 13;

						    //12.09.09 validate CatInvoiceNum
						    else {
						        int errorKey =
						            validateCatInvoiceNum(catInvoiceNum,acctFac);
						        if (errorKey >0)
						            return errorKey == 1 ? 14 : 15;
						    }
						}
						else {
						    String facility = (String)sa.getFieldValue("AccountingFacility");
							if (facility == null || !facility.equals(acctFac)) {
							    //if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found AccountingFacility mismatch!",ClassName);
							    return 13;
							}
						}
					}
				}
			}
			// Invoice line Currencies must match
			Currency lineCurrency = lineAmt.getCurrency();
			if (i==0)
			    baseCurrency = lineCurrency;
			else if (lineCurrency != baseCurrency)
			    return 3;

			// Check if Currencies of VAT and Amount match
			Money taxAmt = (Money)li.getFieldValue("TaxAmount");
			if (taxAmt != null && taxAmt.getCurrency() != lineCurrency)
			    return 4;

			// Collect unique MAs
			Contract ma = li.getMasterAgreement();
			if (ma != null)
					ListUtil.addElementIfAbsent(maList, ma);
		}
	    // 12.03.06 Check that at least one material line exists
	    if (matLineCount < 1)
	        return 11;

	    // Check MAs for common PaymentTerms and BillTo and Currency matching Invoice Line
	    size = maList.size();
	    for (int i=0; i<size; i++) {
	        Contract ma = (Contract)maList.get(i);
	        ContractLineItem mali = (ContractLineItem) ma.getLineItem(1);
		    // MALineItem and Invoice Currencies must match
	        if (mali != null && mali.getAmount().getCurrency() != baseCurrency)
	            return 5;

            // Check PaymentTerms
	        Object payTerms = ma.getPaymentTerms();
		    if (i==0)
		        baseTerms = payTerms;
		    else {
		        // check if PayTerms match between contracts (if more than one)
		        if (payTerms != baseTerms)
		            return 6;
		    }
	        // check if Invoice PayTerms match Contract PayTerms
	        if (currentPayTerms != baseTerms)
	            return 10;

		    if (mali != null) {
		        // BillTos must match
			    ClusterRoot billTo = mali.getBillingAddress();
				if (i==0)
				    baseBillTo = billTo;
				else if (billTo != baseBillTo)
				    return 7;
			}
        }

		// Check if VAT Line exists and matches total of all TaxAmounts
		if (nonVATLineFound) {
		    if (!vatLineFound && !computedTaxAmount.isApproxZero())
		        return 8;
		    else if (vatAmount.approxCompareTo(computedTaxAmount) != 0) {
		        enteredVAT = vatAmount;
		        computedVAT = computedTaxAmount;
		        return 9;
		    }
		}
		// 01.05.07 Verify unique CatInvoiceNumber (last check for peformance reasons)
        if (CatEZOInvoiceSubmitHook.getDuplicateBarcode(catInvoiceNum, invoice.getPartition(),false) != null)
	            return 20;

	    return 0;
	}

	public static int validateCatInvoiceNum(String barcode, String facility) {

	    int result = 0;
	    if (barcode != null && facility != null) {

	        try {
	            result = 1;
	            String prefix = barcode.substring(0,2);
	            //if (Log.customer.debugOn)
	                Log.customer.debug("%s *** barcode prefix: %s",ClassName, prefix);
	            if (!prefix.equals(facility))
	                return result;  // CatInvoiceNum prefix must match common AcctngFac
	            result = 2;
	            if (!isValidCatInvoiceNumSuffix(barcode))
	                return result;
	        }
	        catch (Exception e) {
	            //if (Log.customer.debugOn)
	                Log.customer.debug("%s *** Exception in CatInvoice# numeric test: %s",ClassName, e);
	            return result;
	        }
	    }
	   return 0;  // no validation error
	}

	public static boolean isValidCatInvoiceNumSuffix(String barcode) {

	    if (barcode != null) {
	        try {
	            String suffix = barcode.substring(2);
	            //if (Log.customer.debugOn)
	                Log.customer.debug("%s *** barcode suffix: %s",ClassName, suffix);
	            char [] chars = suffix.toCharArray();
	            for(int i=0;i<chars.length;i++) {
	                char value = chars[i];
	                if (!Character.isDigit(value)){
			            //if (Log.customer.debugOn)
			                Log.customer.debug("SubmitHook found non-digit value at [i]!" + i);
	                    return false;
	                }
	            }
	        }
	        catch (Exception e) {
	            //if (Log.customer.debugOn)
	                Log.customer.debug("%s *** Exception in CatInvoice# numeric test: %s",ClassName, e);
	            return false;
	        }
	    }
	    return true;
	}

	public static String getDuplicateBarcode(String barcode, Partition partition, boolean isReconciling) {

	    String result = null;
	    AQLQuery query = AQLQuery.parseQuery(Fmt.S("%s %s %s '%s'",
				"SELECT UniqueName FROM ariba.invoicing.core.InvoiceReconciliation",
				"WHERE StatusString <> 'Rejected' ",
				"AND CatInvoiceNumber = ",
				barcode));

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: The query is %s", ClassName, query.toString());

		AQLResultCollection results = Base.getService().executeQuery(query, new AQLOptions(partition));

		if (results != null && results.getSize() > 0) {
		    //if (Log.customer.debugOn)
				Log.customer.debug("CatEZOInvoiceSubmitHook ::: results SIZE " + results.getSize());

		    if (!isReconciling || results.getSize() > 1)
		        result = "DUP";

		    else {
		        if (results.next())
		            result = (String)results.getObject(0);
		    }
		}
	    //if (Log.customer.debugOn)
			Log.customer.debug("CatEZOInvoiceSubmitHook ::: Returning Result: " + result);

	    return result;
	}

}