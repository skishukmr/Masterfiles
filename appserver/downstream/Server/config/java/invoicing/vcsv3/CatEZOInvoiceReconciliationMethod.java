/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1. Shaila Salimath  09/29/08   Copying buyer code value from MS or Order to Invocie Line itemimport ariba.contract.core.MasterAgreement;
***************************************************************************************** */

package config.java.invoicing.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.basic.core.Money;
import ariba.common.core.Address;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.payment.core.PaymentTerms;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.tax.core.TaxDetail;
import ariba.tax.core.TaxID;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import config.java.invoicing.CatInvoiceReconciliationMethod;

public class CatEZOInvoiceReconciliationMethod extends CatInvoiceReconciliationMethod {

    public static final String ClassName = "CatEZOInvoiceReconciliationMethod";
	private static String GoodPORefDate = Fmt.Sil("cat.invoicejava.vcsv3","IR_DateForGoodPOReferences");
	private static String vat = "VAT";

	protected List createInvoiceReconciliations(Invoice invoice) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the createInvoiceReconciliations method", ClassName);

		Partition partition = Base.getSession().getPartition();
		BaseVector invLineItems = invoice.getLineItems();
		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;
		int invLoadingCat = invoice.getLoadedFrom();

		for (int i = 0; i < invLineItems.size(); i++) {
			Log.customer.debug("%s ::: The invoice line number " + (i + 1) + " is " + invLineItems.get(i), ClassName);
			Log.customer.debug(
				"%s ::: The order line item linked to the invoice line is " + ((InvoiceLineItem) invLineItems.get(i)).getFieldValue("OrderLineItem"),
				ClassName);
		}

		if (!invoice.isCreditMemo() && !invoice.isDebitMemo()) {
			if ((invLoadingCat == Invoice.LoadedFromACSN) || (invLoadingCat == Invoice.LoadedFromFile)) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Processing the invoice loaded from ASN or File", ClassName);
				processEdiAndAsnInvoices(invoice);
				setValuesForElectronicInvoices(invoice, invLineItems);
			}
			if(invoice != null)
						    {
								Log.customer.debug("%s ::: Setting BuyerCode on Invoice", ClassName);
								setBuyerCodesOnLines(invoice, invLineItems);
						    }


/*			Supplier invSupplier = invoice.getSupplier();
			SupplierLocation invSuppLocation = invoice.getSupplierLocation();
			PurchaseOrder order = invoice.getOrder();
			MasterAgreement ma = invoice.getMasterAgreement();

			if (order != null) {
				// (include in setValuesforEInvoices if needed) setTaxValuesForInvAgnstPO(invoice, order, invLineItems);
			}
			else if (ma != null) {
				// include in setValuesforEInvoices if needed) setTaxValuesForInvAgnstContract(invoice, ma, invLineItems);
			}
*/
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Will be saving the Invoice now", ClassName);
		invoice.save();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Done saving the Invoice, will call the super method", ClassName);
		return super.createInvoiceReconciliations(invoice);
	}

	public void processEdiAndAsnInvoices(Invoice invoice) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the processEdiAndAsnInvoices method", ClassName);
		int invLoadingCat = invoice.getLoadedFrom();
		boolean goodPORef = false;

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: The invoice is loaded from category %s", ClassName, invLoadingCat);
		BaseVector invLineItems = (BaseVector) invoice.getLineItems();

		Date now = Date.getNow();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the Invoice BlockStampDate to %s ", ClassName, now);
		invoice.setFieldValue("BlockStampDate", now);

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the PaymentTerms on the Invoice Object", ClassName);
		PaymentTerms payTerms = null;

		if (invoice.getOrder() != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Setting the PaymentTerms from the order", ClassName);
			PurchaseOrder order = invoice.getOrder();
			try{
				if (order != null){
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Order is %s", ClassName, order);
					payTerms = order.getPaymentTerms();
				}
			}
			catch (Exception ge){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,ge);
			}

			String [] date = StringUtil.delimitedStringToArray(GoodPORefDate,',');
			try {
				Date goodPORefDate = new Date(Integer.parseInt(date[0]), Integer.parseInt(date[1]),
						Integer.parseInt(date[2]));
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: goodPORefDate: %s",ClassName, goodPORefDate);
				Date orderedDate = invoice.getOrder().getOrderedDate();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Order Ordered Date: %s",ClassName, orderedDate);
				if (goodPORefDate.before(orderedDate))
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Setting goodPORef to true as a new order with valid references",ClassName);
					goodPORef = true;
			}
			catch (ArrayIndexOutOfBoundsException aoe) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,aoe);
			}
			catch (NumberFormatException nfe) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,nfe);
			}
			catch (Exception ge){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,ge);
			}
		}
		else if (invoice.getMasterAgreement() != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Setting the PayTerms from the master agreement on Invoice", ClassName);
			payTerms = invoice.getMasterAgreement().getPaymentTerms();
		}
		if (payTerms != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: PayTerms %s from PO will be set on Invoice", ClassName, payTerms.getUniqueName());
			invoice.setFieldValue("PaymentTerms", payTerms);
		}
	}

		//Method Added for BuyerCode

		public void setBuyerCodesOnLines(Invoice invoice, BaseVector invLineItems) {
				PurchaseOrder order = invoice.getOrder();
				Contract ma = invoice.getMasterAgreement();

				InvoiceLineItem invLineItem = null;
				BaseVector procureLineItems = null;
				ProcureLineItem pli = null;
				for (int i = 0; invLineItems != null && i < invLineItems.size(); i++) {
					invLineItem = (InvoiceLineItem) invLineItems.get(i);
					ProcureLineType plt = invLineItem.getLineType();
								if (invLineItem.getOrderLineItem() != null) {
									pli = invLineItem.getOrderLineItem();
									//  Issue 850 -- Copying buyer code value from MS or Order to Invocie Line item
									ClusterRoot buyerCodePO = (ClusterRoot) pli.getFieldValue("BuyerCode");
									if (buyerCodePO != null){
										Log.customer.debug("%s ::: ##### buyerCode ##### %s", ClassName, buyerCodePO);
									invLineItem.setFieldValue("BuyerCode",buyerCodePO );
									config.java.common.Log.customCATLog.debug("The value for BuyerCode (After setting on invoice): " + invLineItem.getFieldValue("BuyerCode"));
									}
										//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: Getting the procure line item from the order %s", ClassName, pli);
								}
								else if (invLineItem.getMALineItem() != null) {
									pli = invLineItem.getMALineItem();
									//  Issue 850
									ClusterRoot buyerCodePO = (ClusterRoot) pli.getFieldValue("BuyerCode");
								    if (buyerCodePO != null){
											Log.customer.debug("%s ::: ##### buyerCode ##### %s", ClassName, buyerCodePO);
									        invLineItem.setFieldValue("BuyerCode",buyerCodePO );
								        	config.java.common.Log.customCATLog.debug("The value for BuyerCode (After setting on invoice): " + invLineItem.getFieldValue("BuyerCode"));
									}
									//if (Log.customer.debugOn)
										Log.customer.debug("%s ::: Getting the procure line item from the master agreement %s", ClassName, pli);
								}

	}
	}


	public void setValuesForElectronicInvoices(Invoice invoice, BaseVector invLineItems) {

		BigDecimal div100 = new BigDecimal(".01");
	    PurchaseOrder order = invoice.getOrder();
		Contract ma = invoice.getMasterAgreement();

		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;
		BigDecimal vatRate = null;
		boolean isVATInLine = false;
		boolean isOtherHeaderTax = false;
		boolean isTaxInLine = invoice.getIsTaxInLine();
		BaseVector taxDetails = invoice.getTaxDetails();
		int taxCounter = 0;

		//if (Log.customer.debugOn)
		    Log.customer.debug("%s ::: setValuesForElectronicInvoices - Invoice#: %s", ClassName,invoice.getUniqueName());

		// 1. Set VAT header info (VATRegistration#, OriginVATCountry, SupplierInvDate
		TaxID id = invoice.getSupplierTaxID();
		if (id != null) {
		    String vatId = id.getID();
		    if (vatId != null)
		        invoice.setFieldValue("VATRegistrationNumber",vatId);
		}
		Address sloc = (Address)invoice.getSupplierLocation();
		if (sloc != null) {
		    Address remitTo = (Address)sloc.getFieldValue("RemitTo");
		    if (remitTo != null)
		        invoice.setFieldValue("OriginVATCountry",remitTo.getCountry());
		}
		invoice.setFieldValue("SupplierInvoiceDate",invoice.getInvoiceDate());

		// 2. Set VAT-related info on line items
        if (!taxDetails.isEmpty()) {
		    taxCounter = taxDetails.size();
		    for (int i=0; i<taxCounter; i++) {
		        TaxDetail detail = (TaxDetail)taxDetails.get(i);
		        String taxType = detail.getCategory();
		        // assume always will have only 1 summary level VAT
		        if (taxType.equals("vat")){
		           vatRate = detail.getPercent();
		           if (isTaxInLine)
		               isVATInLine = true;  // since ASN invoice must always be all summary tax or all line tax
		           //if (Log.customer.debugOn)
				        Log.customer.debug("%s ::: Found VAT - Rate: %s", ClassName, vatRate);
		       }
		        else if (!isTaxInLine)
		            isOtherHeaderTax = true;
		    }
		}
		//if (Log.customer.debugOn) {
		    Log.customer.debug("CatEZOInvoiceReconciliationMethod ::: Checkpoint: taxCounter: " + taxCounter);
		    Log.customer.debug("CatEZOInvoiceReconciliationMethod ::: Checkpoint: isTaxInLine: " + isTaxInLine);
		    Log.customer.debug("CatEZOInvoiceReconciliationMethod ::: Checkpoint: isVATInLine: " + isVATInLine);
		    Log.customer.debug("CatEZOInvoiceReconciliationMethod ::: Checkpoint: isOtherHeaderTax: " + isOtherHeaderTax);
		    Log.customer.debug("%s ::: Checkpoint: last vatRate: %s", ClassName,vatRate);
		//}

		int size = invLineItems.size();
		for (int i = 0; invLineItems != null && i < size; i++) {

		    invLineItem = (InvoiceLineItem) invLineItems.get(i);
			Money forNothing = new Money(Constants.ZeroBigDecimal,invLineItem.getAmount().getCurrency());
		    ProcureLineType plt = invLineItem.getLineType();

	        //if (Log.customer.debugOn) {
	            Log.customer.debug("CatEZOInvoiceReconcilationMethod ::: PROCESSING LINE# : " + (i+1));
	            Log.customer.debug("%s ::: INITIAL LINE TYPE: %s", ClassName,plt.getUniqueName());
	        //}

			if (!isTaxInLine) { // means header level VAT (so set for VATRate for all lines)

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s ::: PROCEEDING PATH ONE (A) SINCE INVOICE HAS NO LINE TAX!", ClassName);

				if (plt.getCategory() == ProcureLineType.LineItemCategory) {
				    if (vatRate != null) {
					    invLineItem.setFieldValue("VATRate",vatRate);
					    invLineItem.setTaxAmount(invLineItem.getAmount().multiply(vatRate).multiply(div100));
				    }
				    else {
					    invLineItem.setFieldValue("VATRate",Constants.ZeroBigDecimal);
					    invLineItem.setTaxAmount(invLineItem.getAmount().multiply(Constants.ZeroBigDecimal));
				    }
				}
				else {
				    //  12.14.06 Handle other header level tax (need to set category to TaxCharge vs. VAT)
				    //  01.25.07 Add vatRate==null test to ensure proper handling of header VAT (QA problem)
				    if (plt.getCategory() == ProcureLineType.TaxChargeCategory) {
				        ProcureLineType procureLTObj = null;
				        if (!isOtherHeaderTax) {  //means only VAT header
					        procureLTObj= ProcureLineType.lookupByUniqueName("VATCharge", invoice.getPartition());
				        }
				        else {  // means either no VAT exists or multiple header taxes (will be rejected)
				            procureLTObj= ProcureLineType.lookupByUniqueName("TaxCharge", invoice.getPartition());
				        }
						if (procureLTObj != null) {
							invLineItem.setLineType(procureLTObj);
							//if (Log.customer.debugOn)
							    Log.customer.debug("%s ::: Switching to new LineType: %s", ClassName,procureLTObj.getUniqueName());
						}
				    }
				    //  set all non-material lines VATRate = 0 and IsVATRecoverable = False
				    invLineItem.setFieldValue("VATRate",Constants.ZeroBigDecimal);
				    invLineItem.setTaxAmount(forNothing);
				    // also set IsVATRecoverable for VAT lines
				    if (invLineItem.getLineType().getUniqueName().equals("VATCharge")) {
			            invLineItem.setFieldValue("IsVATRecoverable",Boolean.FALSE);
			            // 12.22.06 Set Description to VAT vs. Tax
			            if (!invLineItem.getDescription().getDescription().equals(vat)) {
			                invLineItem.setDottedFieldValue("Description.Description",vat);
			                //if (Log.customer.debugOn)
			                    Log.customer.debug("%s ::: Switching Desc to VAT: %s", ClassName,invLineItem.getDescription().getDescription());
			            }
				    }
				}
			    //if (Log.customer.debugOn)
			        Log.customer.debug("%s ::: (A) After Setting VATRate & TaxAmount for LineType: %s, %s, %s", ClassName,
			                invLineItem.getLineType().getUniqueName(),invLineItem.getFieldValue("VATRate"),invLineItem.getTaxAmount().getAmount());
			}
			else {  // line level VAT so must update mat lines

				//if (Log.customer.debugOn)
				    Log.customer.debug("%s ::: PROCEEDING PATH TWO (B) SINCE INVOICE HAS LINE-LEVEL TAX!", ClassName);

			    // 01.02.07 Rewrite to use detail vs. LineType check (all line-level tax was treated as VATCharge initially)
			    ProcureLineType taxType = null;
				TaxDetail detail = invLineItem.getTaxDetail();
			    if (detail != null) {
				    if (detail.getCategory().equals("vat")) {
					    InvoiceLineItem refLine = (InvoiceLineItem)invLineItem.getParent();
					    if (refLine != null) {
					        BigDecimal percent = detail.getPercent();
					        //if (Log.customer.debugOn)
					            Log.customer.debug("%s ::: TaxDetail percent: %s", ClassName, percent);
					        refLine.setFieldValue("VATRate",percent);
					        refLine.setTaxAmount(refLine.getAmount().multiply(percent).multiply(div100));
						    //if (Log.customer.debugOn)
						        Log.customer.debug("%s ::: B) Aftter Setting VATRate & TaxAmount for related Mat Line: %s, %s", ClassName,
						                refLine.getFieldValue("VATRate"),refLine.getTaxAmount());

							// 01.25.07 Added to set to VATCharge (QA problem calling VAT as SalesTaxCharge)
							if (!invLineItem.getLineType().getUniqueName().equals("VATCharge")) {
							    taxType = ProcureLineType.lookupByUniqueName("VATCharge", invoice.getPartition());
							    if (taxType != null) {
							        invLineItem.setLineType(taxType);
							        //if (Log.customer.debugOn)
							            Log.customer.debug("%s ::: Switching LineType to VATCharge: %s", ClassName,invLineItem.getLineType().getUniqueName());
							    }
							}
						    // 12.22.06 Reset Description to VAT vs. Tax
				            if (!invLineItem.getDescription().getDescription().equals(vat)) {
					            invLineItem.setDottedFieldValue("Description.Description",vat);
								//if (Log.customer.debugOn)
								    Log.customer.debug("%s ::: Switching Desc to VAT: %s", ClassName,invLineItem.getDescription().getDescription());
				            }
					    }
				    }
				    else { // set everything else to TaxCharge
						if (!invLineItem.getLineType().getUniqueName().equals("TaxCharge")) {
						    taxType = ProcureLineType.lookupByUniqueName("TaxCharge", invoice.getPartition());
						    if (taxType != null) {
						        invLineItem.setLineType(taxType);
						        //if (Log.customer.debugOn)
						            Log.customer.debug("%s ::: Switching LineType to TaxCharge: %s", ClassName,invLineItem.getLineType().getUniqueName());
						    }
						}
				    }
			    }
			}
		}
	    //if (Log.customer.debugOn)
	        Log.customer.debug("%s ::: FINISHED setValuesForElectronicInvoices().", ClassName);
	}

}