/*

 Author: Nani Venkatesan (Ariba Inc.)

   Date; 5/29/2005

Purpose: The purpose of this class is to override the OOB reconciliation engine.
190  IBM AMS_Lekshmi  Auto Rejecting InvoiceReconciliation if the Order/Contract Currency different from Invoice Currency	
287  IBM AMS_Rahul-15/01/2013  Auto Rejecting InvoiceReconciliation if the Invoice Date is Future Date.

*/



package config.java.invoicing;
import java.text.SimpleDateFormat;

import java.util.List;

import config.java.tax.CatTaxUtil;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.basic.core.Currency;
import ariba.common.core.Supplier;
import ariba.contract.core.Contract;
import ariba.invoicing.AribaInvoiceReconciliationEngine;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.statement.core.StatementReconciliationLineItem;
import ariba.invoicing.core.Log;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;



public class CatInvoiceReconciliationEngine extends AribaInvoiceReconciliationEngine

{


	private static final String StringTable = "cat.java.common";
	private static final String QUANTITY = "POQuantityVariance";

	private static final String RECEIVED_QUANTITY = "POReceivedQuantityVariance";

	private static final String DUPLICATE_INVOICE = "CatDuplicateInvoice";



	//Quantity Received Exception Types - want to remove for negative invoices

	private String[] receiptExcTypes = {QUANTITY, RECEIVED_QUANTITY};



	public boolean reconcile(Approvable approvable)

	{

		if (approvable instanceof InvoiceReconciliation) {

			setHeaderTaxDetail(approvable);

		}



		return super.reconcile(approvable);

	}



    protected boolean validateHeader(Approvable approvable)

    {



        Assert.that(approvable instanceof InvoiceReconciliation, "%s.validateHeader: approvable must be an IR", "config.java.invoicing.CatInvoiceReconciliationEngine");

        InvoiceReconciliation ir = (InvoiceReconciliation)approvable;

        Log.customer.debug("\n\n\n");

        Log.customer.debug("%s ::: Calling the checkForDuplicateInvoice() method in validateHeader()", "CatInvoiceReconciliationEngine");

        checkForDuplicateInvoice(ir);

        Log.customer.debug("%s ::: Done calling the checkForDuplicateInvoice() method in validateHeader()", "CatInvoiceReconciliationEngine");

        Log.customer.debug("\n\n\n");

        Log.customer.debug("%s.validateHeader called with %s", "config.java.invoicing.CatInvoiceReconciliationEngine", ir);



		if ((ir.getInvoice().isCreditMemo()) || (ir.getInvoice().isDebitMemo())) {

			//reject credit and debit memos. shouldAutoReject method does not get called for credit and debit memos and

			//hence not used!

			Log.customer.debug("Rejecting credit and debit memos ...");

			ir.reject();

		}



		if (ir.getInvoice().getInvoiceOperation().equals(Invoice.OperationDelete)) {

			//reject replacement invoices

			Log.customer.debug("Rejecting replacement/cancel invoice ...");

			ir.reject();

		}



		return super.validateHeader(approvable);

    }
    
    
    // Issue 287: Auto Reject IR if the Invoice Date is Future Date
    
     protected boolean autoRejectInvoiceForFutureDate (Approvable approvable)
    {    	
    	Log.customer.debug("autoRejectInvoiceForFutureDate(): Method to Reject Invoices if Invoice Date is Future Date");
    	InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
    	BaseObject bo= ir.getInvoice();
    	if ((bo!=null) && (bo instanceof Invoice))
    	{
    			Log.customer.debug("autoRejectInvoiceForFutureDate():Bo is instance of Invoice");
    			Invoice inv=(Invoice)bo;
    			try
    			{
    				String invUniqueName=inv.getUniqueName();
    				Log.customer.debug("autoRejectInvoiceForFutureDate():Invoice UniqueName :-> " + invUniqueName);
    				
    				int invLoadedFrom=inv.getLoadedFrom();
    				Log.customer.debug("autoRejectInvoiceForFutureDate():Invocie LoadedFrom :-> " + invLoadedFrom);
    				
    				Date invoiceDate = (ariba.util.core.Date) inv.getInvoiceDate();
    				Log.customer.debug("autoRejectInvoiceForFutureDate():Invocie LoadedFrom :invoiceDate-> " + invoiceDate);
    				Date currentDate = new Date().getNow();
    				Log.customer.debug("autoRejectInvoiceForFutureDate():Invocie LoadedFrom :currentDate-> " +currentDate);
    				boolean checkDate = Date.sameDay(invoiceDate, currentDate);
    				if (invLoadedFrom==Invoice.LoadedFromACSN) 
    				{
    					if(!checkDate){
    					if (invoiceDate.compareTo(currentDate) > 0)
    					{   
    						Log.customer.debug("autoRejectInvoiceForFutureDate():invoiceDate is future than Current Date");
    						return true;
    					}
    					Log.customer.debug("autoRejectInvoiceForFutureDate():invoiceDate is before than Current Date");
    					
    					}
    					Log.customer.debug("autoRejectInvoiceForFutureDate():invoiceDate and Current Date are same day::"+checkDate);
					}
    				
    			}catch (NullPointerException npe) 
    			{
					Log.customer.debug("autoRejectInvoiceForFutureDate():Null Pointer Exception occured while auto rejecting Invoice ");
    			}    		
    		}
		return false;
    	} 
    	
    
	// End of issue 287
  
    /*
     * Issue 190 : Auto Rejecting IR if Invoice Currency is different from the Order or
	 * Contract Currency from which it got created
	 */

	protected void autoRejectInvoiceCurrencyDiffFromOrderOrContract(
			Approvable approvable) {
		Log.customer
				.debug("Entering In Method autoRejectIfInvoiceCurrencyDiffFromOrderOrContract");
		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		if (ir.getTotalCost() != null) {
			Currency irCurrency = ir.getTotalCost().getCurrency();
			boolean flag = false;
			BaseObject bo = ir.getOrder();
			if (bo == null) {
				bo = ir.getMasterAgreement();
			}
			if (bo != null) {
				if (bo instanceof PurchaseOrder) {
					Log.customer.debug("Bo is instance of Purchase Order");
					PurchaseOrder po = (PurchaseOrder) bo;
					try {
						Currency orderCurrency = po.getTotalCost()
								.getCurrency();
						if (!irCurrency.equals(orderCurrency)) {
							flag = true;
						}
					} catch (NullPointerException npe) {
						Log.customer
								.debug("Null Pointer Exception occured while auto rejecting Invoice ");
					}
				} else if (bo instanceof Contract) {
					Log.customer.debug("Bo is instance of Contract");
					Contract contract = (Contract) bo;
					Currency contractCurrency = contract.getTotalCost()
							.getCurrency();
					if (!irCurrency.equals(contractCurrency)) {
						flag = true;
					}
				}
				Log.customer.debug("isDiff Value is %s", flag);
				if (flag == true) {
					Log.customer.debug("Adding Comments to show the reason");	
					String rejectionMessage = (String)ResourceService.getString(StringTable, "CurrencyRejectionMessage");
					LongString commentText = new LongString(rejectionMessage);
					String commentTitle = "Reason For Invoice Rejection";
					Date commentDate = new Date();
					User commentUser = User.getAribaSystemUser(ir.getPartition());
					CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
					Log.customer.debug("IR is Auto Rejected");
					ir.reject();
				}
			}
		}
		Log.customer
				.debug("Exit from Method autoRejectIfInvoiceCurrencyDiffFromOrderOrContract");
	}


    private void checkForDuplicateInvoice(BaseObject parent) {



		Boolean notDuplicate = new Boolean(true);



		InvoiceReconciliation ir = (InvoiceReconciliation) parent;

        String irUniqueName = ir.getUniqueName();

        if(ir.isCreationInProgress())

        {

            //if(Log.customer.debugOn)

                Log.customer.debug("%s ::: Is Creation of the IR in progress " + ir.isCreationInProgress(), "ariba.invoicing.AribaInvoiceReconciliationEngine");

        } else

        {

            //if(Log.customer.debugOn)

            {

                Log.customer.debug("%s ::: Is Creation of the IR in progress " + ir.isCreationInProgress(), "ariba.invoicing.AribaInvoiceReconciliationEngine");

                Log.customer.debug("%s ::: Returning out of the checkForDuplicateInvoice() without checking for duplicate", "ariba.invoicing.AribaInvoiceReconciliationEngine");

            }

            return;

        }

		ir.setDottedFieldValue("IsNotDuplicate", notDuplicate);

		ir.getInvoice().setDottedFieldValue("IsNotDuplicate", notDuplicate);



        Supplier supplier = ir.getInvoice().getSupplier();

        String supplierInvoiceNumber 	= ir.getInvoice().getInvoiceNumber();

        //if(Log.customer.debugOn)

        {

            Log.customer.debug("%s ::: The Inv # object is %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", supplierInvoiceNumber);

        }

        if (supplier == null || StringUtil.nullOrEmptyOrBlankString(supplierInvoiceNumber)) {

            //if(Log.customer.debugOn) {

				Log.customer.debug("%s ::: The Supplier object is " + supplier, "ariba.invoicing.AribaInvoiceReconciliationEngine" );

                Log.customer.debug("%s ::: The Supplier object is null or invoice number is nullOrEmptyOrBlankString", "ariba.invoicing.AribaInvoiceReconciliationEngine");

			//}

            return;

        }



        supplierInvoiceNumber = supplierInvoiceNumber.toUpperCase();

        //if(Log.customer.debugOn)

        {

            Log.customer.debug("%s ::: The Supplier object is %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", supplier.toString());

            Log.customer.debug("%s ::: The Inv # in uppercase is %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", supplierInvoiceNumber);

        }

        AQLQuery tempQuery = AQLQuery.parseQuery(Fmt.S("%s %s %s %s %s '%s'",

        							"SELECT InvoiceReconciliation",

        							"FROM ariba.invoicing.core.InvoiceReconciliation",

        							"WHERE Invoice.Supplier =",

        							AQLScalarExpression.buildLiteral(supplier).toString(),

        							"AND UPPER(Invoice.InvoiceNumber) = ",

        							supplierInvoiceNumber));

        //if(Log.customer.debugOn)

            Log.customer.debug("%s ::: The tempQuery is %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", tempQuery.toString());
        AQLOptions options = new AQLOptions(ir.getPartition());

        AQLResultCollection results = Base.getService().executeQuery(tempQuery, options);

        if(results.getSize() > 0)
        {
           boolean isDuplicate = false;
           //if(Log.customer.debugOn)
               Log.customer.debug("%s ::: The size of result set is: " + results.getSize(), "ariba.invoicing.AribaInvoiceReconciliationEngine");

            while(results.next())
            {
                BaseId baseId = results.getBaseId(0);
                //if(Log.customer.debugOn)
                {
                    Log.customer.debug("%s ::: Base ID of result: %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", baseId.toString());
                    Log.customer.debug("%s ::: Base ID of ir: %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", ir.getBaseId().toString());
                }
                if(!SystemUtil.equal(baseId, ir.getBaseId()))
                {
                   //if(Log.customer.debugOn)
                        Log.customer.debug("%s ::: Encountered different Base IDs: %s != %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", baseId.toString(), ir.getBaseId().toString());
                    isDuplicate = true;
                    break;
                }
            }
            if(isDuplicate)
            {
                Log.customer.debug("%s ::: Setting duplicate flag for %s", "ariba.invoicing.AribaInvoiceReconciliationEngine", ir.getUniqueName());
				notDuplicate = new Boolean(false);
			    ir.setDottedFieldValue("IsNotDuplicate", notDuplicate);
				ir.getInvoice().setDottedFieldValue("IsNotDuplicate", notDuplicate);
			}
        }
	}

	/*
	 *  ARajendren, Ariba Inc.,
     *	Changed the method signature from InvoiceReconciliationLineItem to StatementReconciliationLineItem
	 *
	 */


	protected List getExceptionTypesForLine (StatementReconciliationLineItem irli)
	{
		List exceptions = super.getExceptionTypesForLine(irli);
	    InvoiceReconciliation ir = (InvoiceReconciliation)irli.getLineItemCollection();
		//Removing Quantity exceptions for credit lines
		if (irli.getAmount().isNegative()) {
			for (int i = 0; i < receiptExcTypes.length; i++) {
				exceptions.remove(getInvoiceExceptionType(receiptExcTypes[i], irli.getPartition()));
			}
		}
	    return exceptions;
    }

	/** Return InvoiceExceptionType for passed UniqueName **/

	protected static InvoiceExceptionType getInvoiceExceptionType (String uName, Partition p)

	{
		return InvoiceExceptionType.lookupByUniqueName(uName, p);
	}

	private void setHeaderTaxDetail(Approvable approvable) {

		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		if (ir.getTaxDetails().size() == 1) {
			ir.setDottedFieldValue("HeaderTaxDetail", ir.getTaxDetails().get(0));
		}



	}



}
