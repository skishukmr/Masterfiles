/***********************************************************************************************
	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------
	03/15/08	Shaila		Added the rounding off logic on the amount related fields to avoid
							the failure of the IR's at MFG.

************************************************************************************************/
package config.java.schedule;

import java.math.BigDecimal;
import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class EasyOpenInvoiceProcess extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        try
        {
            Log.customer.debug("Setting up the InvoiceReconciliation objects.....");
            partition = Base.getSession().getPartition();
            query = new String("select * from ariba.invoicing.core.InvoiceReconciliation   where ActionFlag IS NULL and StatusString like '%Rejected%'  and ApprovedState = 4");
            populateValues("Complete", "Rejected");
            query = new String("select * from ariba.invoicing.core.InvoiceReconciliation   where (ActionFlag IS NULL or ActionFlag NOT LIKE 'Completed'  and ActionFlag NOT LIKE 'InProcess') and (StatusString like '%Paid%')  and ApprovedState = 4 and TotalCost.Amount <> 0");
            populateValues("InProcess", "Reconciled");
        }
        catch(Exception e)
        {
            throw new ScheduledTaskException("Error : " + e.toString(), e);
        }
    }

    void populateValues(String actionflag, String invstat)
        throws Exception
    {
        try
        {
            Log.customer.debug(query);
            qry = AQLQuery.parseQuery(query);
            options = new AQLOptions(partition);
            results = Base.getService().executeQuery(qry, options);
            if(results.getErrors() != null)
                Log.customer.debug("ERROR GETTING RESULTS in Results1");
            while(results.next())
            {
                String unique = null;
                String afac = "";
                String orderId = "";
                invrecon = (InvoiceReconciliation)results.getBaseId("InvoiceReconciliation").get();
                if(invrecon != null)
                {
                    Log.customer.debug("2...." + invrecon.toString());
                    unique = (String)invrecon.getFieldValue("UniqueName");
                    Invoice irinv = (Invoice)invrecon.getFieldValue("Invoice");
                    Date InvoiceDate = (Date)irinv.getFieldValue("InvoiceDate");

                    // rounding off - to ensure the total amount on the IR and line item amounts match
					adjustPrecisionOnLines(invrecon);

                    int invlinecount = invrecon.getLineItemsCount();
                    InvoiceReconciliationLineItem invreconli = null;
                    if(invlinecount > 0)
                    {
                        zerothsplit = null;
                        for(int i = 0; i < invlinecount; i++)
                        {
                            Log.customer.debug("invlinecount count == " + invlinecount);
                            invreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
                            if(invreconli.getDottedFieldValue("OrderLineItem.NumberInCollection") != null)
                            {
                                Log.customer.debug(i + " the Line:\tnumberInCollection is " + invreconli.getDottedFieldValue("OrderLineItem.NumberInCollection"));
                                invreconli.setFieldValue("POLineItemNumber", invreconli.getDottedFieldValue("OrderLineItem.NumberInCollection"));
                            }
                            if(invreconli.getDottedFieldValue("Order.UniqueName") != null)
                            {
								Log.customer.debug(" Populating Order number as PONumber ");
                                invreconli.setFieldValue("PONumber", invreconli.getDottedFieldValue("Order.UniqueName"));
							}
                            else
                            {
								Log.customer.debug(" Populating MA number as PONumber");
                            	invreconli.setFieldValue("PONumber",invreconli.getDottedFieldValue("MasterAgreement.UniqueName"));
							}
                            if(invreconli != null && invreconli.getFieldValue("Accountings") != null)
                                sacol = (SplitAccountingCollection)invreconli.getFieldValue("Accountings");
                            afac = (String)invrecon.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].AccountingFacility");
                            if(afac != null)
                                invrecon.setFieldValue("FacilityFlag", afac);
                        }

                    }
                    invrecon.setFieldValue("TopicName", "EasyOpenInvoiceReconciliationPush");
                    Log.customer.debug("Populating......TopicName");
                    invrecon.setFieldValue("ActionFlag", new String(actionflag));
                    Log.customer.debug("Populating......ActionFlag");
                    Log.customer.debug("IRs values are populated.");
                    Base.getSession().transactionCommit();
                }
            }
            Log.customer.debug("Ending IRProcess program .....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            throw e;
        }
    }

	public void adjustPrecisionOnLines(InvoiceReconciliation invrecon){

		Log.customer.debug("EasyOpenInvoiceProcess: in adjustPrecisionOnLines");
        InvoiceReconciliationLineItem irLineItem;

        // Rounding off done to TotalCost, LineItemAmt and SplitAmount
        Money totalCost = invrecon.getTotalCost();
        totalCost.setAmount(BigDecimalFormatter.round(totalCost.getAmount(),2));
        BigDecimal irTotalCost = totalCost.getAmount();
        BigDecimal irLineAmtTotalSum = Constants.ZeroBigDecimal;
        Log.customer.debug("EasyOpenInvoiceProcess: irTotalCost after rounding 2 places ="+irTotalCost);

        for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
            irLineItem = (InvoiceReconciliationLineItem)i.next();
            irLineItem.getAmount().setAmount(BigDecimalFormatter.round(irLineItem.getAmount().getAmount(),2));
            Log.customer.debug("EasyOpenInvoiceProcess: Adding to irLineAmtTotal="+irLineItem.getAmount().getAmount());

           /* Money price_amt = (Money)invreconli.getDottedFieldValue("Description.Price");
			irLineItem.setAmount(BigDecimalFormatter.round(price_amt.getAmount(),2));
		    Log.customer.debug("EasyOpenInvoiceProcess: price_amt="+price_amt); */

	          LineItemProductDescription desc = (LineItemProductDescription)irLineItem.getDescription();
	           Log.customer.debug("EasyOpenInvoiceProcess: desc ="+desc);
	           Money price_amt = (Money)desc.getPrice();
	           Log.customer.debug("EasyOpenInvoiceProcess: price_amt ="+price_amt);
			  // desc.setAmount(Money.round(price_amt.getAmount(),2));
			   irLineItem.getDescription().getPrice().setAmount(BigDecimalFormatter.round(irLineItem.getDescription().getPrice().getAmount(),2));
		  Log.customer.debug("EasyOpenInvoiceProcess: price_amt="+price_amt.getAmount());

            irLineAmtTotalSum = irLineAmtTotalSum.add(irLineItem.getAmount().getAmount());
            Log.customer.debug("EasyOpenInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
        }
        Log.customer.debug("EasyOpenInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
        Log.customer.debug("EasyOpenInvoiceProcess: Comparing IR Total cost with IR Line Amount total ="+irLineAmtTotalSum.compareTo(irTotalCost));

     	// -1 if less, 0 equals, 1 greater than
        if(irLineAmtTotalSum.compareTo(irTotalCost) < 0 ) {
            //sum of line Amounts is less than total, add to first line the diff amount
            BigDecimal diff = irTotalCost.subtract(irLineAmtTotalSum);
            Log.customer.debug("EasyOpenInvoiceProcess: Difference Amount(irTotalCost - irLineAmtTotalSum)="+diff);
            Money firstLineAmount = invrecon.getLineItem(1).getAmount();
            Log.customer.debug("EasyOpenInvoiceProcess: firstLineAmount="+firstLineAmount.getAmount());
            firstLineAmount.setAmount(firstLineAmount.getAmount().add(diff));
            Log.customer.debug("EasyOpenInvoiceProcess: after update firstLineAmount="+firstLineAmount.getAmount());
       } else if (irLineAmtTotalSum.compareTo(irTotalCost) > 0 ) {
            //sum of line Amounts is greater than total, minus to first line the diff amount
            BigDecimal diff = irLineAmtTotalSum.subtract(irTotalCost);
            Log.customer.debug("EasyOpenInvoiceProcess: diff2="+diff);
            Money firstLineAmount = invrecon.getLineItem(1).getAmount();
            Log.customer.debug("EasyOpenInvoiceProcess: firstLineAmount="+firstLineAmount.getAmount());
            firstLineAmount.setAmount(firstLineAmount.getAmount().subtract(diff));
            Log.customer.debug("EasyOpenInvoiceProcess: after update firstLineAmount="+firstLineAmount.getAmount());
        }

        //once the total with lineItemAmts are corrected, check the lineAmount to its split amounts total.
        for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
            irLineItem = (InvoiceReconciliationLineItem)i.next();
            SplitAccounting splitAcc;
            BigDecimal irLineItemAmount = irLineItem.getAmount().getAmount();
            BigDecimal lineItemSplitAmountSum = Constants.ZeroBigDecimal;

            for(Iterator s= irLineItem.getAccountings().getSplitAccountingsIterator(); s.hasNext();) {
                splitAcc = (SplitAccounting) s.next();
                splitAcc.getAmount().setAmount(BigDecimalFormatter.round(splitAcc.getAmount().getAmount(),2));
                Log.customer.debug("EasyOpenInvoiceProcess: Adding lineItemSplitAmount="+splitAcc.getAmount().getAmount());
                lineItemSplitAmountSum = lineItemSplitAmountSum.add(splitAcc.getAmount().getAmount());
                Log.customer.debug("EasyOpenInvoiceProcess: lineItemSplitAmountSum="+lineItemSplitAmountSum);
            }

            if(lineItemSplitAmountSum.compareTo(irLineItemAmount) < 0 ) {
                //sum of split Amounts is less than line total, add to first line the diff amount
                BigDecimal diff = irLineItemAmount.subtract(lineItemSplitAmountSum);
                Log.customer.debug("EasyOpenInvoiceProcess: SplitAmt diff="+diff);
                Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
                Log.customer.debug("EasyOpenInvoiceProcess: firstSplitAmt="+firstSplitAmt.getAmount());
                firstSplitAmt.setAmount(firstSplitAmt.getAmount().add(diff));
                Log.customer.debug("EasyOpenInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getAmount());
            } else if (lineItemSplitAmountSum.compareTo(irLineItemAmount) > 0 ) {
                //sum of split Amounts is greater than line total, add to first line the diff amount
                BigDecimal diff = lineItemSplitAmountSum.subtract(irLineItemAmount);
                Log.customer.debug("EasyOpenInvoiceProcess: SplitAmt diff="+diff);
                Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
                Log.customer.debug("EasyOpenInvoiceProcess: firstSplitAmt="+firstSplitAmt.getAmount());
                firstSplitAmt.setAmount(firstSplitAmt.getAmount().subtract(diff));
                Log.customer.debug("EasyOpenInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getAmount());
            }
        }
	}
    public EasyOpenInvoiceProcess()
    {
        afac = "";
    }

    private Partition partition;
    private String query;
    private AQLQuery qry;
    private AQLOptions options;
    private AQLResultCollection results;
    private InvoiceReconciliation invrecon;
    private SplitAccountingCollection sacol;
    private BaseVector bvec;
    private SplitAccounting sa;
    private SplitAccounting zerothsplit;
    private ClusterRoot fac;
    String afac;
}