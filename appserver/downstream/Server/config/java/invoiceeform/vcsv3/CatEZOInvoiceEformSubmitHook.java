
package config.java.invoiceeform.vcsv3;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.invoiceeform.CatInvoiceEformSubmitHook;
import config.java.invoicing.vcsv3.CatEZOInvoiceSubmitHook;

/**
 * @author KS.
 *	Validates a bunch of stuff (too many to list).
 *
 */
public class CatEZOInvoiceEformSubmitHook extends CatInvoiceEformSubmitHook {

    private static final String ClassName = "CatEZOInvoiceEformSubmitHook";
    private static final String ComponentStringTable = "cat.invoicejava.vcsv3";
    private String fmt;
    private Money computedVAT;
    private Money enteredVAT;

	public List run(Approvable approvable) {

		List list = super.run(approvable);
		Integer integer = (Integer) list.get(0);

		if (integer != null) {
			if (integer.intValue() != NoError) {
				return list;
			}
		}
		ClusterRoot cr = (ClusterRoot) approvable;
		List lineItems = (List) cr.getFieldValue("LineItems");
		Money totalCost = (Money) cr.getFieldValue("TotalTax");

		computedVAT = null; enteredVAT = null;
		int errorType = findErrors(cr, lineItems, totalCost);
		if (errorType > 0) {
		    switch (errorType) {
		    	case 1:
		    	    fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFTaxLinesError");
		    	    break;
		        case 2:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFBillToMismatchError");
		            break;
		    	case 3:
		    	    fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFUomMismatchError");
		    	    break;
		        case 4:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFLineVATCurrencyError");
		            break;
		        case 5:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFMissingVATLineError");
		            break;
		        case 6:
			        //if (Log.customer.debugOn)
			            Log.customer.debug("%s *** enteredVAT, computedVAT: %s, %s",ClassName,
			                    enteredVAT.getAmount(),computedVAT.getAmount());
		            if (computedVAT != null && enteredVAT != null) {
		                fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFVATCalculationError_Detail");
		            	fmt = Fmt.S(fmt,enteredVAT.asString(),computedVAT.asString());
		            }
		            else
		                fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFVATCalculationError");
		            break;
		        case 7:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFZeroLineAmountError");
		            break;
		        case 8:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFAcctFacilityMismatch");
		            break;
		        case 9:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INVCatInvoiceNumError_Prefix");
		            break;
		        case 10:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INVCatInvoiceNumError_Suffix");
		            break;
		        case 11:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INVNoMaterialLineError");
		            break;
		        case 20:
		            fmt = ResourceService.getString(ComponentStringTable, "Hook_INVCatInvoiceNumError_Duplicate");
		    }
		    return ListUtil.list(Constants.getInteger(ValidationError), fmt);
		}

		Object po;
		List poList = ListUtil.list();
		int size = ListUtil.getListSize(lineItems);
		for (int i = 0; i < size; i++) {
			BaseObject lineItem = (BaseObject) lineItems.get(i);
			po = lineItem.getDottedFieldValue("Order");
			if (po == null) { // safety just in case order not set on EformLineItem
		        //if (Log.customer.debugOn)
		            Log.customer.debug("%s *** EformLineItem missing ORDER - getMissingPO()!",ClassName);
		        po = getMissingOrder(lineItem);
		        lineItem.setFieldValue("Order",po);
			}
			if (po != null)
			    ListUtil.addElementIfAbsent(poList, po);
		}
		if (diffPaymentTerms(poList)){
			fmt = ResourceService.getString(ComponentStringTable, "Hook_INEFPayTermsError");
			return ListUtil.list(Constants.getInteger(ValidationError), fmt);
		}
		return NoErrorResult;
	}

	public static boolean diffPaymentTerms(List poList) {

	    boolean hasDiffTerms = false;
	    ClusterRoot baseTerms = null;
	    for (int i=0;i<poList.size();i++) {
	        PurchaseOrder po = (PurchaseOrder)poList.get(i);
	        ClusterRoot terms = po.getPaymentTerms();
	        if (i==0)
	            baseTerms = terms;
	        else {
	            if (terms != baseTerms)
	                hasDiffTerms = true;
	        }
	    }
	    //if (Log.customer.debugOn)
	        Log.customer.debug("CatEZOInvoiceEformSubmitHook *** different PayTerms?" + hasDiffTerms);
	    return hasDiffTerms;
	}

	private int findErrors(ClusterRoot eform, List lineItems, Money referenceMoney) {

	    boolean nonTaxLineFound = false;
	    boolean taxLineFound = false;
	    Money computedTaxAmt = new Money(Constants.ZeroBigDecimal, referenceMoney.getCurrency());
	    Money vatAmount = new Money(Constants.ZeroBigDecimal, referenceMoney.getCurrency());
	    ClusterRoot baseBillTo = null;
	    PurchaseOrder basePO = null;
	    int matLineCount = 0;
	    String acctFac = null;

		for (int i = 0; i < lineItems.size(); i++) {
			BaseObject li = (BaseObject) lineItems.get(i);
			ProcureLineType plt = (ProcureLineType) li.getFieldValue("LineType");

		    //if (Log.customer.debugOn)
		        Log.customer.debug("CatEZOInvoiceEformSubmitHook *** Line# / Type: " + (i+1) + plt);

		    // 12.07.06 Ensure non-material lines have non-zero value
		    // if (plt.getCategory() != ProcureLineType.LineItemCategory) {
	        Money lineAmt = (Money)li.getFieldValue("Amount");
		    if (lineAmt.isZero())
		        return 7;

			// Check for VAT lines
// 				if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
			if (plt.getUniqueName().equals("VATCharge")) {
			    if (taxLineFound)
			        return 1;

			    taxLineFound = true;
			    vatAmount = (Money)li.getFieldValue("Amount");
			}
			else {
			    nonTaxLineFound = true;
				Money taxAmount = (Money)li.getFieldValue("TaxAmount");
				if (taxAmount != null)
					computedTaxAmt = Money.add(computedTaxAmt, taxAmount);

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** computedTaxAmount: %s", ClassName,computedTaxAmt);

				// count material lines
				if (plt.getCategory() == ProcureLineType.LineItemCategory)
				    matLineCount++;
			}

			PurchaseOrder po = (PurchaseOrder)li.getFieldValue("Order");
			if (po != null) {
				// Check if BillTos differ between POs (assumes consistent across lines on same PO)
			    if (basePO == null) {
			        basePO = po;
			        baseBillTo = ((POLineItem)basePO.getLineItem(1)).getBillingAddress();
			    }
				else if (po != basePO){
			        ClusterRoot billTo = ((POLineItem)po.getLineItem(1)).getBillingAddress();
				    if (billTo != baseBillTo)
				        return 2;
				}
				Integer lineNum = (Integer)li.getFieldValue("OrderLineNumber");
				if (lineNum != null){
				    POLineItem line = (POLineItem)po.getLineItem(lineNum.intValue());
				    if (line != null) {
						if (plt.getCategory() == ProcureLineType.LineItemCategory) {

						    // Check if UOM of PO and Invoice lines match (for material lines)
					        Object pUOM = line.getDottedFieldValue("Description.UnitOfMeasure");
					        Object iUOM = li.getFieldValue("UnitOfMeasure");
					        //if (Log.customer.debugOn)
					            Log.customer.debug("%s *** po UOM, invoice UOM: %s, %s", ClassName,pUOM,iUOM);

					        if (pUOM != iUOM)
					            return 3;

					        // 12.09.09 Check that AcctFac match (all lines, 1st split only)
							List splits = line.getAccountings().getSplitAccountings();
							if (!splits.isEmpty()) {
								SplitAccounting sa = (SplitAccounting) splits.get(0);
								if (matLineCount==1) {
								    acctFac = (String)sa.getFieldValue("AccountingFacility");

								    //12.09.09 validate CatInvoiceNum
								    if (acctFac != null) {
								        int errorKey = CatEZOInvoiceSubmitHook.
								            validateCatInvoiceNum((String)eform.getFieldValue("CatInvoiceNumber"),acctFac);
								        if (errorKey >0)
								            return errorKey == 1 ? 9 : 10;
								    }
								}
								else if (!((String)sa.getFieldValue("AccountingFacility")).equals(acctFac)) {
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: Found AccountingFacility mismatch!",ClassName);
									return 8;
								}
							}
						}
				    }
				}
			}
			// Check if Currencies of VAT and Amount match
			Money taxAmt = (Money)li.getFieldValue("TaxAmount");
	        //if (Log.customer.debugOn) {
	            Log.customer.debug("%s *** Line Amt, Tax Amt: %s, %s", ClassName,lineAmt,taxAmt);
	        //}
			if (taxAmt != null && lineAmt != null && taxAmt.getCurrency() != lineAmt.getCurrency())
			    return 4;
		}
	    // Check that at least one material line exists
	    if (matLineCount < 1)
	        return 11;

		// Check if VAT Line exists and matches total of all TaxAmounts
		if (nonTaxLineFound) {
		    if (!taxLineFound && !computedTaxAmt.isApproxZero())
			    return 5;
		    else if (vatAmount.approxCompareTo(computedTaxAmt) != 0) {
		        enteredVAT = vatAmount;
		        computedVAT = computedTaxAmt;
		        return 6;
		    }
		}
		// 01.05.07 Verify unique CatInvoiceNumber (last check for peformance reasons)
	    if (CatEZOInvoiceSubmitHook.getDuplicateBarcode((String)eform.getFieldValue("CatInvoiceNumber"),
	            	eform.getPartition(),false) != null)
		       return 20;

		return 0;  // all's well that ends well
	}

	public static Object getMissingOrder(BaseObject lineitem) {

	    Object order = null;
	    String orderNum = (String)lineitem.getFieldValue("OrderNumber");
	    if (orderNum != null) {
	        order = Base.getService().objectMatchingUniqueName("ariba.purchasing.core.DirectOrder",
	                lineitem.getPartition(),orderNum);
	        //if (Log.customer.debugOn)
	            Log.customer.debug("%s *** Getting PO - %s from OrderNum - %s!",ClassName,order,orderNum);
	    }
	    return order;
	}

}


