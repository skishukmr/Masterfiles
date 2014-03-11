/*******************************************************************************************************************
	Revision History

	1)Amit	 09-25-2007   	Added conditions to check for close order variance
	07/11/2012   IBM AMS_Lekshmi     WI 190          Auto Rejecting InvoiceReconciliation if the Order/Contract Currency different from Invoice Currency
	08/08/2012   IBM AMS_Manoj       WI 295          Close Order variance is added based on the value of Closed field.
********************************************************************************************************************/

package config.java.invoicing.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.statement.core.StatementReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.statement.core.StatementReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.payment.core.PaymentTermTier;
import ariba.payment.core.PaymentTerms;
import ariba.payment.core.PaymentTermsStep;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.tax.core.TaxDetail;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import config.java.invoicing.CatInvoiceReconciliationEngine;
import config.java.tax.CatTaxUtil;

public class CatEZOInvoiceReconciliationEngine extends CatInvoiceReconciliationEngine {

	public static final String ClassName = "CatEZOInvoiceReconciliationEngine";
	public static final String OverTaxExclusion = "VATCharge";

	private static final String UNMATCHED_INVOICE = "UnmatchedInvoice";
	private static final String MA_NOT_INVOICABLE = "MANotInvoiceable";
	private static final String OVER_TAX = "OverTaxVariance";
	private static final String PO_RCVD_Q_V = "POReceivedQuantityVariance";
	private static final String MA_RCVD_Q_V = "MAReceivedQuantityVariance";
	private static final String CAT_INVALID_ACCTNG = "CatInvalidAccounting";
	private static final String CLOSE_ORDER = "ClosePOVariance";
	public static final String ComponentStringTable = "cat.java.vcsv1.csv";
	private static final String StringTable = "cat.java.common";
//	private String[] autoRejectExcTypes = { UNMATCHED_INVOICE, MA_NOT_INVOICABLE, };
//	private String[] ignoreExceptions = { PO_RCVD_Q_V, MA_RCVD_Q_V };


	public boolean reconcile(Approvable approvable) {
		if (approvable instanceof InvoiceReconciliation) {

		    InvoiceReconciliation ir = (InvoiceReconciliation)approvable;
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Entering reconcile() for IR#: %s",ClassName,ir.getUniqueName());

			if (ir.isCreationInProgress()){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Running through to default from Invoice this is IR Creation.", ClassName);

				// copy Cat-specific fields from INV to IR
				copyFieldsFromInvoice(ir);
			}
			else{
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: IR not in Creation process!", ClassName);

				// must recopy TaxAmount (VAT Amount) from Invoice lines
				recopyFieldsFromInvoice(ir);
			}
			//if (Log.customer.debugOn)
				Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Exiting reconcile()");
		}
		return super.reconcile(approvable);
	}


	protected boolean validateHeader(Approvable approvable) {

	    Assert.that(approvable instanceof InvoiceReconciliation,
			"%s.validateHeader: approvable must be an IR",
			ClassName);

	    InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		// Reject invoices if necessary
		executeRejections(ir);
		// Start Of Issue 190
		autoRejectInvoiceCurrencyDiffFromOrderOrContract(ir);
		// issue 190 End
		return super.validateHeader(approvable);
	}

	/*
	 * Auto Rejecting IR if Invoice Currency is different from the Order or
	 * Contract Currency from which it got created
	 */


	protected List generateExceptions(BaseObject parent, List typesToValidate) {

		//if (Log.customer.debugOn)
			Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: In method for generating exceptions");

		List exceptions = super.generateExceptions(parent, typesToValidate);

		if (parent instanceof InvoiceReconciliation)
			exceptions = generateHeaderExceptions((InvoiceReconciliation) parent, typesToValidate, exceptions);
		else
			exceptions = generateLineExceptions((InvoiceReconciliationLineItem) parent, typesToValidate, exceptions);

		for (int i = 0; i < exceptions.size(); i++) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Exception generated is %s",
			        ((InvoiceException) exceptions.get(i)).getType().getUniqueName());
		}
		return exceptions;
	}

	/*
	 *  Returns Header Exceptions
	 *  ARajendren, Ariba Inc.,
     *	Changed the method signature from InvoiceReconciliation to StatementReconciliation
	 */

	protected List getExceptionTypesForHeader(StatementReconciliation ir) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In method for generating exception type for header",ClassName);

		List exceptions = super.getExceptionTypesForHeader(ir);

		/* close order imlementation

		if (ir.getFieldValue("Order") != null)
		{
			//if (Log.customer.debugOn)
				Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: CloseOrder Value IS: %s...", (Boolean)ir.getDottedFieldValue("Order.CloseOrder") );
			if ( ( ir.getDottedFieldValue("Order.CloseOrder") != null ) && ( ((Boolean)ir.getDottedFieldValue("Order.CloseOrder")).booleanValue() ) )
			{
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: CloseOrder Value IS: %s... Hence NOT removing the %s exception", (Boolean)ir.getDottedFieldValue("Order.CloseOrder"), CLOSE_ORDER);
			}
			else
			{
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Removing exception type: %s", CLOSE_ORDER);
				exceptions.remove(super.getInvoiceExceptionType(CLOSE_ORDER, ir.getPartition()));
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Successfully removed exception type: %s", CLOSE_ORDER);
			}
		}
                */
                //  ***** WI 295 Starts ********
	  	/* Check for Close Order Variance based on "Closed" field
		 * If Closed = 1 , Order is Open
		 * If Closed = 4 , Order is closed for invoicing and if Closed = 5 , Order is closed for all.
		 */
			         if (ir.getFieldValue("Order") != null)
			          {
						 ariba.purchasing.core.PurchaseOrder irOrder = (ariba.purchasing.core.PurchaseOrder)ir.getOrder();
			  			 Integer closeOrder = (Integer) irOrder.getFieldValue("Closed");
			  			 int closeState = closeOrder.intValue();
			  			 Log.customer.debug("%s::: CloseOrder Value IS: %s...",ClassName,closeOrder);
			  			 if ((closeState == 4 || closeState == 5))
			  			 {
			  				//if (Log.customer.debugOn)
			  			    		Log.customer.debug("CatCSVInvoiceReconciliationEngine ::: CloseOrder Value IS: %s... Hence NOT removing the %s exception", (Boolean)ir.getDottedFieldValue("Order.CloseOrder"), CLOSE_ORDER);
			  		     }
			  		     else
			  		     {
			  				    Log.customer.debug("CatSAPInvoiceReconciliationEngine ::: Removing exception type: %s", CLOSE_ORDER);
			  				   	exceptions.remove(super.getInvoiceExceptionType(CLOSE_ORDER, ir.getPartition()));
			  				    Log.customer.debug("CatSAPInvoiceReconciliationEngine ::: Successfully removed exception type: %s", CLOSE_ORDER);
			  			  }
				     }
		        //  ***** WI 295 Ends ********

		return exceptions;
	}

	/*
	 *  Returns LineItem Exceptions
	 *  ARajendren, Ariba Inc.,
     *	Changed the method signature from InvoiceReconciliationLineItem to StatementReconciliationLineItem
	 */
	protected List getExceptionTypesForLine(StatementReconciliationLineItem irli) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: In method for generating exception type for line");
		List exceptions = super.getExceptionTypesForLine(irli);

		ProcureLineType plt = irli.getLineType();
		if (plt != null) {
		    // remove quantity exceptions for non-material lines
			if (plt.getCategory() != ProcureLineType.LineItemCategory){
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Removing exception type: %s", PO_RCVD_Q_V);
				exceptions.remove(super.getInvoiceExceptionType(PO_RCVD_Q_V, irli.getPartition()));
				//if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Removing exception type: %s", MA_RCVD_Q_V);
				exceptions.remove(super.getInvoiceExceptionType(MA_RCVD_Q_V, irli.getPartition()));

				// remove OverTaxVariance if is VAT tax
				if (plt.getCategory() == ProcureLineType.TaxChargeCategory &&
				    		plt.getUniqueName().equals(OverTaxExclusion)) {
				        //if (Log.customer.debugOn)
				            Log.customer.debug("CatCSVInvoiceReconciliationEngine ::: Removing exception since VAT: %s", OVER_TAX);
				        exceptions.remove(super.getInvoiceExceptionType(OVER_TAX, irli.getPartition()));
				}
			}
			else { // remove CatInvalidAccounting if material line accounting is valid
			    if (CatEZOInvoiceAccountingValidation.validateIRLineAccounting((InvoiceReconciliationLineItem)irli)) {
					//if (Log.customer.debugOn)
						Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: Removing exception type: %s", CAT_INVALID_ACCTNG);
					exceptions.remove(super.getInvoiceExceptionType(CAT_INVALID_ACCTNG, irli.getPartition()));
			    }
			}
		}
		return exceptions;
	}

	//	Generate any custom Header Exceptions

	private List generateHeaderExceptions(InvoiceReconciliation ir, List typesToValidate, List exceptions) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: In method for generating HEADER Exceptions!");

	    return exceptions;
	}

	//	Generate any custom Line Exceptions

	private List generateLineExceptions(InvoiceReconciliationLineItem irli, List typesToValidate, List exceptions) {

	   //if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: In method for generating LINE Exceptions!",ClassName);
//			Log.customer.debug("%s ::: InvReconciliation# %s",ClassName,irli.getLineItemCollection().getUniqueName());
//			Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: IR Line#: " + irli.getNumberInCollection());
	    //}
/*		ProcureLineType plt = irli.getLineType();
		if (plt != null && plt.getCategory() == ProcureLineType.LineItemCategory) {

		    //	Add Invalid Accounting exception if applicable
		    if (!CatEZOInvoiceAccountingValidation.validateIRLineAccounting(irli)) {
			    if (Log.customer.debugOn)
			        Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: exceptions.size() BEFORE: " + exceptions.size());
				InvoiceExceptionType type = InvoiceExceptionType.lookupByUniqueName(CAT_INVALID_ACCTNG,irli.getPartition());
				InvoiceException ex = InvoiceException.createFromTypeAndParent(type,irli);
				if (Log.customer.debugOn)
					Log.customer.debug("%s ::: new InvoiceException (TEST): %s",ClassName,ex);
				if (ex != null) {
					exceptions.add(ex);
				}
				if (Log.customer.debugOn)
				    Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: exceptions.size() AFTER: " + exceptions.size());
		    }
		}
*/
	    return exceptions;
	}


	private void executeRejections(InvoiceReconciliation ir) {

	    //if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the method executeRejections()", ClassName);

		if (!ir.getInvoice().isStandardInvoice()) {
			return;
		}

		Invoice invoice = ir.getInvoice();
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irLineItem = null;

		// 1. Paper invoice rejections (not caught in SubmitHook)

/*		if (invoice.getLoadedFrom() == Invoice.LoadedFromEForm || invoice.getLoadedFrom() == Invoice.LoadedFromUI) {
			if (TRUE) {
				// ** Create Comment Here **
				ir.reject();
				return;
			}
			return;
		}
*/
		// 2. Electronic invoice rejections

		if (invoice.getLoadedFrom() == Invoice.LoadedFromACSN || invoice.getLoadedFrom() == Invoice.LoadedFromFile) {

		    boolean hasZeroAmount = false;
			boolean hasHeaderTax = false;
			boolean hasHeaderVAT = false;
			boolean hasMultipleHeaderTax = false;
			boolean hasLineVAT = false;
			boolean hasMultipleLineVAT = false;
			boolean hasDiscount = false;
			boolean hasDiffLineCurrency = false;
			boolean hasDiffCurrency = false;
			boolean hasDiffBillTo = false;
			boolean hasDiffPayTerms = false;
			Object baseCurrency = null;
			Object baseBillTo = null;
			Object basePayTerms = null;
			ProcureLineItem parentLine = null;
			int matLineCount = 0;

			boolean isTaxInLine = invoice.getIsTaxInLine();

			ProcureLineItemCollection plic = ir.getOrder();
			if (plic == null)
			    plic = ir.getMasterAgreement();
			if (plic != null) {
			    Money amount = plic.getLineItem(1).getAmount();
			    if (amount != null)
			        baseCurrency = amount.getCurrency();
			}
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: ASN PO/MA plic: %s : baseCurrency: %s",ClassName,plic,baseCurrency);

			// 01.14.07 Use TaxDetails to check for multiple header tax
			// Needed since only 1 header tax line created from ASN cXML invoice
			if (!isTaxInLine) {
			    List details = invoice.getTaxDetails();
			    if (details != null && !details.isEmpty()) {
			        hasHeaderTax = true;
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Found Header Tax in Details!", ClassName);
				    int dSize = details.size();
				    if (dSize > 1) {
					    hasMultipleHeaderTax = true;
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Found Multiple Header Taxes in Details!", ClassName);
				    }
				    while (dSize > 0) {
				        TaxDetail detail = (TaxDetail)details.get(--dSize);
				        String category = detail.getCategory();
				        if (category.equals("vat")) {
						    //if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found header VAT in Details!",ClassName);
				            hasHeaderVAT = true;
				            break;
				        }
				    }
			    }
			}

			int size = irLineItems.size();
			for (int i = 0; i < size; i++) {
				irLineItem = (InvoiceReconciliationLineItem) irLineItems.get(i);
			    //if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: IR Line#: " + (size+1));

				ProcureLineType lineType = irLineItem.getLineType();
				if(lineType != null){

				    int category = lineType.getCategory();
				    //if (Log.customer.debugOn)
						Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: LineType Category: " + category);

				    // Check if Discount
				    if (category == ProcureLineType.DiscountCategory) {
						hasDiscount = true;
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Found Discount Line!", ClassName);
					}
				    // Check if multiple line VATs for same material line
					else if (category == ProcureLineType.TaxChargeCategory) {
						if (irLineItem.getLineType().getUniqueName().equals("VATCharge")) {
							if (irLineItem.getDottedFieldValue("MatchedLineItem") != null) {
							    hasLineVAT = true;
							    //if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Found Line VAT in Line Items!",ClassName);
								if (!hasMultipleLineVAT) {
									if (parentLine == null)
									    parentLine = irLineItem.getParent();
									else if (parentLine == irLineItem.getParent()){
									    hasMultipleLineVAT = true;
										//if (Log.customer.debugOn)
											Log.customer.debug("%s ::: Found Multiple Line VATs for same Material Line", ClassName);
									}
								}
							}
							else { // must mean VAT is header
							    hasHeaderVAT = true;
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Found Header VAT in Line Items!", ClassName);
							}
						}
						// must mean non-VAT tax is header
						else if (irLineItem.getDottedFieldValue("MatchedLineItem") == null) {
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found Header Tax in Line Items!", ClassName);
						    hasHeaderTax = true;
						}
					}
					else if (category == ProcureLineType.LineItemCategory) { // reset necessary at each material line
					    parentLine = null;
					    matLineCount++;
					}
					Money lineAmt = irLineItem.getAmount();

					// 12.09.09 Check if Line Amount = 0.00 (not allowed for taxes & charges)
				    if (lineAmt.isZero()) {
				        if (category != ProcureLineType.LineItemCategory) {
				            hasZeroAmount = true;
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found 0.00 Line Amount for Non-Material Line!", ClassName);
				        }
				    }
					// Check for consistent currency (must match PO/MA)...only if non-zero amount
					else if (!hasDiffCurrency && baseCurrency != null && baseCurrency != lineAmt.getCurrency()) {
					    hasDiffLineCurrency = true;
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Found Different Currency for a Line Charge!", ClassName);
					}
/*					if (irLineItem.getLineType().getCategory() == ProcureLineType.LineItemCategory) {
						if ((irLineItem.getFieldValue("OrderLineItem") == null) && (irLineItem.getFieldValue("MALineItem") == null)) {
							additionalLinesPresent = true;
						}
					}
*/				}
			}
			// Check for consistent BillTo/Currency/PayTerms for summary invoice (across POs/MAs)
			//    ** assumes PO/MA lines are setup with same BillTo/Currency
			if (invoice.getConsolidated()) {
			    List matches = invoice.getMatchedLineItemCollections();
			    Object currency = null;
			    Object billTo = null;
			    Object payTerms = null;
			    if (matches != null && matches.size() > 1){
			        int numMatches = matches.size();
			        while (numMatches > 0) {
			            ProcureLineItemCollection pLIC = (ProcureLineItemCollection)matches.get(--numMatches);

			            billTo = ((ProcureLineItem)pLIC.getLineItem(1)).getBillingAddress();
			            if (baseBillTo == null)
			                baseBillTo = billTo;
			            else if (billTo != baseBillTo) {
			                hasDiffBillTo = true;
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found Different BillTos among Summary Inv PLICs!", ClassName);
			            }
			            currency = ((ProcureLineItem)pLIC.getLineItem(1)).getAmount().getCurrency();
			            if (baseCurrency == null)
			                baseCurrency = currency;
			            else if (currency != baseCurrency) {
			                hasDiffCurrency = true;
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found Different Currencies among Summary Inv PLICs!", ClassName);
			            }
			            payTerms = pLIC.getPaymentTerms();
			            if (basePayTerms == null)
			                basePayTerms = payTerms;
			            else if (payTerms != basePayTerms) {
			                hasDiffPayTerms = true;
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Found Different PaymentTerms among Summary Inv PLICs!", ClassName);
			            }
			        }
			    }
			}
			int rejections = 0;
			Date commentDate = new Date();
			User commentUser = User.getAribaSystemUser(ir.getPartition());
			String commentTitle = Fmt.Sil("cat.invoicejava.vcsv3","IR_RejectionTitle");

			// 12.09.09 Must have > 0 material lines
			if (matLineCount == 0) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains NO material lines!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_NoMaterialLines");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			// 12.09.06 Non-material line Amount must be > 0
			if (hasZeroAmount) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains a non-material line with 0.00 amount!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_ZeroAmount");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasDiscount){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains Discount!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_DiscountCharge");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			// 12.09.06 Must not have > 1 header tax line (of any kind)
			if (hasMultipleHeaderTax) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains more than 1 Header Tax!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_MultipleHeaderTax");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasLineVAT && hasHeaderVAT) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains both Header & Line VAT!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_HeaderAndLineVAT");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasMultipleLineVAT) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - contains more than 1 Line VAT!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_MultipleLineVAT");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasDiffLineCurrency) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - uses different currency than PO/MA line!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_DifferentLineCurrency");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasDiffBillTo) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - consoidated invoice with different PO/MA BillTos!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_DifferentSummaryBillTo");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasDiffPayTerms) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - consoidated invoice with different PO/MA PayTerms!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_DifferentSummaryPayTerms");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (hasDiffCurrency) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Rejecting Electronic Invoice - consoidated invoice with different PO/MA currencies!", ClassName);
				String rejectionMessage = Fmt.Sil("cat.invoicejava.vcsv3","IR_Rejection_DifferentSummaryCurrency");
				LongString commentText = new LongString(rejectionMessage);
				CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
				rejections++;
			}
			if (rejections > 0) {
			    //if (Log.customer.debugOn)
					Log.customer.debug("CatEZOInvReconciliationEngine ::: ir.reject() since rejections = " + rejections);
				ir.reject();
			}
			return;
		}
	}

	private void copyFieldsFromInvoice(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the method copyFieldsFromInvoice", ClassName);

		Invoice invoice = ir.getInvoice();
		ir.setFieldValue("BlockStampDate", (Date) invoice.getFieldValue("BlockStampDate"));
		ir.setFieldValue("PaymentTerms", (ClusterRoot) invoice.getFieldValue("PaymentTerms"));
		ir.setFieldValue("CatInvoiceNumber", (String) invoice.getFieldValue("CatInvoiceNumber"));
		ir.setFieldValue("IsVATReverseCharge", (Boolean) invoice.getFieldValue("IsVATReverseCharge"));
		ir.setFieldValue("CatVATCountryCode", (String) invoice.getFieldValue("CatVATCountryCode"));
		ir.setFieldValue("BVRNumber", (String) invoice.getFieldValue("BVRNumber"));
		ir.setFieldValue("OriginVATCountry", (ClusterRoot) invoice.getFieldValue("OriginVATCountry"));
		ir.setFieldValue("RelatedCatInvoice", (String) invoice.getFieldValue("RelatedCatInvoice"));
		ir.setFieldValue("VATRegistrationNumber", (String) invoice.getFieldValue("VATRegistrationNumber"));
		// Mar 2007 - added new OriginVATCountryCode field (part of VAT enhancement)
		//ir.setFieldValue("OriginVATCountryCode", (String) invoice.getFieldValue("OriginVATCountryCode"));

		// Set other integration-related fields
		ir.setFieldValue("InvoiceNumber",(String)invoice.getInvoiceNumber());
//		ir.setFieldValue("SupplierInvoiceDate", (Date) invoice.getFieldValue("InvoiceDate"));
		Date invDate = invoice.getInvoiceDate();
		if (invDate != null) {
		    invDate.calendarDate();
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: InvoiceDate (Calendar Date): %s", ClassName, invDate);
		    ir.setFieldValue("SupplierInvoiceDate",invDate);
		}
		// set PaymentDueDate based on PaymentTerms
//		if (ir.getFieldValue("PaymentDueDate") == null)
		    setPaymentDueDate(ir);

		// set VAT related fields from INV LineItem
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		InvoiceLineItem invli = null;

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			invli = irli.getInvoiceLineItem();

			irli.setFieldValue("IsVATRecoverable", (Boolean)invli.getFieldValue("IsVATRecoverable"));
			irli.setFieldValue("VATRate", (BigDecimal)invli.getFieldValue("VATRate"));
			irli.setTaxAmount(invli.getTaxAmount());
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: FINSHED setting 3 VAT Fields from INV!", ClassName);

			// set AccountType up front (01.26.07 modified since AcctType not always being set)
			ClusterRoot acctType = null;
			ProcureLineType plt = irli.getLineType();
			ProcureLineItem pli = getProcureLineItem (irli);

			if (plt == null || plt.getCategory() == ProcureLineType.LineItemCategory) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Setting INV/IR Line AccountType!", ClassName);
				acctType = (ClusterRoot)invli.getFieldValue("AccountType");
				if (acctType == null && pli != null) {
				    acctType = (ClusterRoot)pli.getFieldValue("AccountType");
				    invli.setDottedFieldValueWithoutTriggering("AccountType", acctType);
				}
			    irli.setDottedFieldValueWithoutTriggering("AccountType", acctType);
			}
			// also set ProjectNum
			if (pli != null)
			    irli.setFieldValue("ProjectNumber",pli.getFieldValue("ProjectNumber"));
			else
			    irli.setFieldValue("ProjectNumber",invli.getFieldValue("ProjectNumber"));
		}
	}

	public static ProcureLineItem getProcureLineItem(InvoiceReconciliationLineItem irli) {

	    //if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering method getProcureLineItem", ClassName);
		//}
		ProcureLineItem pli = irli.getOrderLineItem();
		if (pli == null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Getting InvoiceLineItem since no POLineItem.", ClassName);
			if (irli.getMasterAgreement() != null) {
				InvoiceLineItem ili = irli.getInvoiceLineItem();
				if (ili != null)
				    pli = ili.getMALineItem();
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: PLI returned from getProcureLineItem(): %s", ClassName,pli);
		return pli;
	}

	private void recopyFieldsFromInvoice(InvoiceReconciliation ir) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: ENTERING the method reCopyFieldsFromInvoice", ClassName);

		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		InvoiceLineItem invli = null;

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			invli = irli.getInvoiceLineItem();

			irli.setTaxAmount(invli.getTaxAmount());
			// for material lines set AccountType & ProjectNumber once matched to PO/MA line
			ProcureLineType plt = irli.getLineType();
			if (plt == null || plt.getCategory() == ProcureLineType.LineItemCategory) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Material Line - setting AccountType & ProjectNum!", ClassName);
			    ProcureLineItem pli = getProcureLineItem (irli);
			    if (pli != null) {
					irli.setDottedFieldValueWithoutTriggering("AccountType", pli.getFieldValue("AccountType"));
					irli.setFieldValue("ProjectNumber", pli.getFieldValue("ProjectNumber"));
			    }
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: EXITING the method reCopyFieldsFromInvoice", ClassName);
	}

	public static void setPaymentDueDate (InvoiceReconciliation ir){

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the method setPaymentDueDate", ClassName);

	    PaymentTerms pt = (PaymentTerms)ir.getFieldValue("PaymentTerms");
//	    Date rcvDate = (Date)ir.getFieldValue("BlockStampDate");
	    Date rcvDate = new Date((Date)ir.getFieldValue("BlockStampDate"));

        //if (Log.customer.debugOn)
            Log.customer.debug("%s ::: new Date (from BlockStampDate): %s", ClassName,rcvDate);

	    if (pt != null && rcvDate != null) {

	        BaseVector steps = pt.getPaymentTermsSteps();
	        PaymentTermsStep step = (PaymentTermsStep)pt.getPaymentTermsSteps().get(0);

	        if (step != null) {
		        PaymentTermTier tier = (PaymentTermTier)step.getPaymentTermTiers().get(0);
		        if (tier != null) {
		            int payInDays = tier.getPayInDays();
		            //if (Log.customer.debugOn)
		                Log.customer.debug("CatEZOInvoiceReconciliationEngine ::: payInDays (add days) " + payInDays);

		            Date.addDays(rcvDate,payInDays);

		            //if (Log.customer.debugOn)
		                Log.customer.debug("%s ::: PaymentDueDate to be set: %s", ClassName,rcvDate.toConciseDateString());

		            ir.setFieldValue("PaymentDueDate", rcvDate);
		        }
	        }
	    }
	}

}
