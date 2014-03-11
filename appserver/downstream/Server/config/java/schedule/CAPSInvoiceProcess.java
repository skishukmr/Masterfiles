/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Processing the IR object depending upon the StatusString, ActionFlag and ApprovedState of the object -

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------
	10/05/2005 	Kingshuk	Populating the Extrinsic fields inside the IR and setting the FacilityFlag and ActionFlag accordingly
	08/01/2006 	Chandra	    Added the adjustCurrencyOnLines method

*******************************************************************************************************************************************/

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
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.util.core.Constants;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;


public class CAPSInvoiceProcess extends ScheduledTask {

    private Partition partition;
	private String query;
	private ClusterRoot cObj;
	private AQLQuery qry;
	private AQLOptions options;
	private AQLResultCollection results;
	private BaseVector invreconlicol;
	private InvoiceReconciliation invrecon;
	private ariba.receiving.core.Receipt rcpt;
	private ariba.purchasing.core.DirectOrder order;
	private ariba.approvable.core.LineItem li;
	private ariba.receiving.core.ReceiptItem ri;
	private ariba.approvable.core.LineItemCollection lic;
	private SplitAccounting splitAcc;
	private Currency repcur;
	private Money totcost;
	private ClusterRoot fac;
	private BaseVector bvec = null;
	private BigDecimal totamt, InvoiceSplitDiscountDollarAmount, TotalInvoiceAmountMinusDiscount;
	private BigDecimal irtax;
	private Integer iCAPSLineNo;
	private String sCAPSLineNo;
	private boolean IsForeign;
	private ariba.util.core.Date currDate;
	private InvoiceReconciliationLineItem doinvreconli;



	public void run() throws ScheduledTaskException {
		Log.customer.debug("%s Setting up the InvoiceReconciliation objects.....", classname);

		try {
			partition = Base.getSession().getPartition();
			currDate = ariba.util.core.Date.getNow();

			//Processing Invoices which are already rejected in Ariba and not pushed to WBI Before
			query = "select ir from ariba.invoicing.core.InvoiceReconciliation ir "
					+"where ActionFlag IS NULL "
					+"and StatusString = 'Rejected' "
					+"and ApprovedState = 4 ";
			populateValues("Complete", "Rejected");

			//Processing Invoices which are already reconciled in Ariba and not pushed
			//to WBI Before (Reconciled Push)
			//query = new String ("select * from InvoiceReconciliation where ActionFlag IS NULL and StatusString like '%Paid%' and ApprovedState = 4");
			query = "select ir from ariba.invoicing.core.InvoiceReconciliation ir "
						+"where (ActionFlag IS NULL "
							+"or ActionFlag NOT LIKE 'Completed'  "
							+"and ActionFlag NOT LIKE 'InProcess')  "
						+"and (StatusString = 'Paid')  "
						+"and ApprovedState = 4  "
						+"and TotalCost.Amount <> 0 ";
			populateValues("InProcess", "Reconciled");
		} catch (Exception st) {
			throw new ScheduledTaskException("Error CAPSInvoiceProcess:", st);
		}

	}

    void populateValues(String actionflag, String invstat) throws Exception {
		try {
    		Log.customer.debug("%s %s", classname, query);

    		qry = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(qry,options);

			if (results.getErrors() != null) {
				Log.customer.debug("%s:ERROR IN RESULTS :%s ", classname, results.getErrors());
				Log.customer.debug("%s:ERROR:",classname, results.getErrorStatementText());
				throw new ScheduledTaskException("Error in results= "+results.getErrorStatementText() );
			}

			while(results.next()) {
				IsForeign = false;
				String unique = null;
				String afac = "";
				String orderId = "";

				invrecon = (InvoiceReconciliation)results.getBaseId(0).get();
				Log.customer.debug("%s 2....%s", classname ,invrecon.toString());

				if (!(((String)invrecon.getDottedFieldValue("TotalCost.Currency.UniqueName") ).equals("USD"))) {
					IsForeign = true;
					Log.customer.debug("%s Its a Foreign Invoice", classname);
				}

				unique = (java.lang.String)invrecon.getFieldValue("UniqueName");

				//InvoiceDate is set as IR.Invocie.InvoiceDate
				Log.customer.debug("%s Populating......InvoiceDate", classname);
				Invoice irinv = (Invoice)invrecon.getFieldValue("Invoice");
				ariba.util.core.Date InvoiceDate = (ariba.util.core.Date) irinv.getFieldValue("InvoiceDate");
				invrecon.setFieldValue("InvoiceDate", InvoiceDate);

				//Setting InvoiceNumber inside the IR
				Log.customer.debug("%s Populating......InvoiceNumber", classname);
				invrecon.setFieldValue("InvoiceNumber", (String)irinv.getFieldValue("InvoiceNumber") );


				if (invrecon.getLineItemsCount() > 0) {
				 doinvreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(0);
				}

				if (doinvreconli != null) {
					//Setting FacilityFlag, PONumber, POCreateDate

					if (doinvreconli.getFieldValue("Order") != null) {
						order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");

						//POCreateDate, PONumber is set as Order.UniqueName
						Log.customer.debug("%s Populating...PONumber...POCreateDate" , classname);
						orderId = (String)order.getFieldValue("UniqueName");
						invrecon.setFieldValue("PONumber", orderId);
						ariba.util.core.Date POCreateDate = (ariba.util.core.Date) order.getFieldValue("TimeCreated");
						invrecon.setFieldValue("POCreateDate", POCreateDate);
					} else {
						Log.customer.debug("%s Populating...PONumber with MA #...POCreateDate as InvoiceDate" , classname);
						invrecon.setFieldValue("PONumber", (java.lang.String)invrecon.getDottedFieldValue("MasterAgreement.UniqueName"));
						invrecon.setFieldValue("POCreateDate", InvoiceDate);
					}
				}

				//Setting ExchangeRate
				if ((Money)invrecon.getFieldValue("TotalCost") != null) {

					Money irtotcost = (Money)invrecon.getFieldValue("TotalCost");
					if (irtotcost != null) {
						Currency cur = (Currency)irtotcost.getFieldValue("Currency");
						String curstr = (java.lang.String)cur.getFieldValue("UniqueName");

						if (curstr.equals("USD")) {
							//invrecon.setFieldValue("ExchangeRate", new BigDecimal(1.00D) );
							invrecon.setFieldValue("USExchangeRate", new BigDecimal(1.00D) );
						} else {
							//Uses the latest currency (push date currency)
							BigDecimal usdcurrate = null;
							String exquery = "select UniqueName, Rate "
												+ " from ariba.basic.core.CurrencyConversionRate as CurrencyConversionRate "
												+ " where UniqueName like 'USD:" + curstr  +"' "
												+ " order by CurrencyConversionRate.Date desc";

							/*
							String exquery = "select UniqueName, Rate, Modified, Month(Modified) MM, Year(Modified) YYYY "
												+" from CurrencyConversionRate "
												+" where UniqueName like 'USD:" + curstr  +"' "
												+" and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder "
												+" where UniqueName like '" + orderId + "') ORDER BY Modified";
							*/
							Log.customer.debug("%s Query for ExRate....", classname, exquery);

							AQLQuery exqry = AQLQuery.parseQuery(exquery);
							AQLOptions exoptions = new AQLOptions(partition);
							AQLResultCollection exresults = Base.getService().executeQuery(exqry,exoptions);

							if (exresults.getErrors() != null) {
								Log.customer.debug("ERROR GETTING RESULTS in Currency");
							}

							while(exresults.next()) {
								usdcurrate = exresults.getBigDecimal("Rate");
								Log.customer.debug("Rate from Query ...." + usdcurrate);
								break; //get first record as it is latest currency
							}

							Log.customer.debug("%s Setting the rate as .." + usdcurrate, classname);
							//invrecon.setFieldValue("ExchangeRate", usdcurrate );
							invrecon.setFieldValue("USExchangeRate", usdcurrate );
						}
					}
				}


				//Setting TopicName
				Log.customer.debug("Populating......TopicName");
				invrecon.setFieldValue("TopicName", "CAPSInvoiceReconciliationPush");


				//updating the currency rates on money objs with current push date currency
				if(!invstat.equals("Rejected")) {
					adjustCurrencyOnLines();
				}

				//Calculates the TotalAmount for all Non tax lines
				GetTotAmtForNonTax();

				//Setting the LineItems.Accountings.SplitAccountings.CapsLineNumber depending
				//upon the value of LineItems.Description.CAPSChargeCode.UniqueName
				GenerateCapsLineNumber ();

				//Setting CAPSUnitOfMeasure as InvoiceReconciliation.LineItems.Description.UnitOfMeasure.CAPSUnitOfMeasure
				int invlinecount = invrecon.getLineItemsCount();

				if (invlinecount > 0) {

					for (int i=0; i<invlinecount; i++) {
						InvoiceReconciliationLineItem invreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
						if ( invreconli.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure") != null ) {
							Log.customer.debug( "%s IRLine " +i+ " : CAPSUnitOfMeasure is " + (String)invreconli.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure"), classname );
							invreconli.setFieldValue("CAPSUnitOfMeasure", (String)invreconli.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure") );
						}
					}
				}

				//Setting ActionFlag as InProcess or Complete
				Log.customer.debug("Populating......ActionFlag");
				invrecon.setFieldValue("ActionFlag",new String(actionflag));

				Log.customer.debug("%s -----------------------End of IR----------------------", classname);

				Base.getSession().transactionCommit();
				IsForeign = false;
			} // end of while

			Log.customer.debug(" %s Ending IRProcess program .....", classname);

		}catch (Exception e) {
			Log.customer.debug(e.toString());
			throw e;
		}
    }

	//Calculates the total amount of all lines that is not linetype 2 ( all materials and services and not tax)
    void GetTotAmtForNonTax() {

		totamt = new BigDecimal(0.0D);
		Currency cur = null;
		int iLineType = 0;
		int invlinecount = invrecon.getLineItemsCount();

		for (int i=0; i< invlinecount; i++) {

			Log.customer.debug(" %s IRLine " + i , classname);
			InvoiceReconciliationLineItem invreconli = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);
			if (invreconli != null) {
				if (invreconli.getFieldValue("LineType") != null)
					iLineType = ( (Integer)invreconli.getDottedFieldValue("LineType.Category") ).intValue();

				if ( iLineType != 2 ) {
					if (invreconli.getFieldValue("TaxAmountAuth") != null) {
						if ( ((BigDecimal)invreconli.getDottedFieldValue("TaxAmountAuth")).doubleValue() != 0 ) {
							totamt = totamt.add ( (BigDecimal)invreconli.getDottedFieldValue("Amount.Amount") );
						}
					}
				}
				Log.customer.debug( "TotAmt Is: " + totamt );
			}
		}
	}

   void GenerateCapsLineNumber () {
		InvoiceSplitDiscountDollarAmount = Constants.ZeroBigDecimal;
		TotalInvoiceAmountMinusDiscount = Constants.ZeroBigDecimal;
		int iLineNo = 1;
		int iLineType = 0;

		Log.customer.debug("IR Object: " + invrecon );

		for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
			InvoiceReconciliationLineItem irLine = (InvoiceReconciliationLineItem)i.next();

			BigDecimal totSplitAmtToCompare = Constants.ZeroBigDecimal;

			if (irLine == null) continue;

			if (irLine.getFieldValue("LineType") != null)
				iLineType = ( (Integer)irLine.getDottedFieldValue("LineType.Category") ).intValue();

			Log.customer.debug(" IRLI #"+irLine.getNumberInCollection() +"LineType is. " + iLineType );

			for(Iterator s= irLine.getAccountings().getSplitAccountingsIterator(); s.hasNext();) {
				splitAcc = (SplitAccounting) s.next();

				if (splitAcc != null) {
					if (IsForeign) {
						BigDecimal splitAccBaseCurrValue = (BigDecimal) splitAcc.getAmount().getApproxAmountInBaseCurrency();

						totSplitAmtToCompare = totSplitAmtToCompare.add(splitAccBaseCurrValue);
					}

					Log.customer.debug("Inside the Loop and in If");

					iCAPSLineNo = new Integer(iLineNo);
					sCAPSLineNo = new String ( iCAPSLineNo.toString() );
					Log.customer.debug("Setting CapsLineNumber as "+sCAPSLineNo);
					splitAcc.setFieldValue("CapsLineNumber", sCAPSLineNo );
					iLineNo ++;

					if ( ( (irLine.getDottedFieldValue("Description.CAPSChargeCode.UniqueName") != null) )
										&& ( !s.hasNext()) && ( iLineType == 2 ) ) {
						Log.customer.debug("3....For the LI: " + irLine + " For the Split: " + splitAcc + " CapsLineNumber is: " + (String)splitAcc.getFieldValue("CapsLineNumber") );
						if ( ( (String)irLine.getDottedFieldValue("Description.CAPSChargeCode.UniqueName") ).equals("002"))
						splitAcc.setFieldValue("CapsLineNumber", new String ("999") );

							if ( ( (String)irLine.getDottedFieldValue("Description.CAPSChargeCode.UniqueName") ).equals("003"))
								splitAcc.setFieldValue("CapsLineNumber", new String ("998") );

							if ( ( (String)irLine.getDottedFieldValue("Description.CAPSChargeCode.UniqueName") ).equals("004"))
								splitAcc.setFieldValue("CapsLineNumber", new String ("997") );

							if ( ( (String)irLine.getDottedFieldValue("Description.CAPSChargeCode.UniqueName") ).equals("096"))
								splitAcc.setFieldValue("CapsLineNumber", new String ("996") );

							Log.customer.debug("4....CAPSLineNumber set is: " + (String)splitAcc.getFieldValue("CapsLineNumber") );
						}

						if (splitAcc.getFieldValue("InvoiceSplitDiscountDollarAmount") != null ) {
							Log.customer.debug("Calculating InvoiceSplitDiscountDollarAmount...");
							if (IsForeign) ((Money)splitAcc.getFieldValue("InvoiceSplitDiscountDollarAmount")).setConversionDate(currDate);
							InvoiceSplitDiscountDollarAmount = InvoiceSplitDiscountDollarAmount.add ( (BigDecimal)splitAcc.getDottedFieldValue("InvoiceSplitDiscountDollarAmount.Amount") );

							Log.customer.debug("InvoiceSplitDiscountDollarAmount IS..." + InvoiceSplitDiscountDollarAmount );
						}
					}
				}

				Money irTotalCost = invrecon.getTotalCost();
				Money InvoiceSplitDiscountDollarAmountMoney = null;
				Money TotalInvoiceAmountMinusDiscountMoney = null;

				if ( InvoiceSplitDiscountDollarAmount != null ) {
					 InvoiceSplitDiscountDollarAmountMoney = new Money(invrecon.getTotalCost() ,InvoiceSplitDiscountDollarAmount);
					InvoiceSplitDiscountDollarAmountMoney.setApproxAmountInBaseCurrency(BigDecimalFormatter.round(InvoiceSplitDiscountDollarAmountMoney.getApproxAmountInBaseCurrency(),2));
					invrecon.setFieldValue("TotalInvoiceDiscountDollarAmount",
										InvoiceSplitDiscountDollarAmountMoney);
					Log.customer.debug("TotalInvoiceDiscountDollarAmount IS set at the Header level");
					TotalInvoiceAmountMinusDiscount = ( (BigDecimal)invrecon.getDottedFieldValue("TotalCost.Amount") ).subtract(InvoiceSplitDiscountDollarAmount);
				}
				if ( TotalInvoiceAmountMinusDiscount!= null)  {
					TotalInvoiceAmountMinusDiscountMoney = new Money(invrecon.getTotalCost() ,TotalInvoiceAmountMinusDiscount);
					TotalInvoiceAmountMinusDiscountMoney.setApproxAmountInBaseCurrency(BigDecimalFormatter.round(TotalInvoiceAmountMinusDiscountMoney.getApproxAmountInBaseCurrency(),2));

					invrecon.setFieldValue("TotalInvoiceAmountMinusDiscount",
										TotalInvoiceAmountMinusDiscountMoney);
					Log.customer.debug("TotalInvoiceAmountMinusDiscount IS set at the Header level");

					BigDecimal irTotalInvoicePaymentAmt = Constants.ZeroBigDecimal;

					BigDecimal irTotalInvoicedAmt = (TotalInvoiceAmountMinusDiscountMoney.getApproxAmountInBaseCurrency()).add (
											InvoiceSplitDiscountDollarAmountMoney.getApproxAmountInBaseCurrency());

                    Log.customer.debug("### irTotalInvoicedAmt = ###"+irTotalInvoicedAmt);
                    Log.customer.debug("### irTotalCostAprox = ###"+ irTotalCost.getApproxAmountInBaseCurrency());

					Log.customer.debug("Adjusting invoice payment amt from discount");


					if(irTotalInvoicedAmt.compareTo(irTotalCost.getApproxAmountInBaseCurrency()) < 0 ) {
						//sum of Amounts is less than total, add the diff amount to discount
 						BigDecimal diff = irTotalCost.getApproxAmountInBaseCurrency().subtract(irTotalInvoicedAmt);
 						Log.customer.debug("Amounts is less than total, adding diff of "+diff);
 						InvoiceSplitDiscountDollarAmountMoney.setApproxAmountInBaseCurrency(InvoiceSplitDiscountDollarAmountMoney.getApproxAmountInBaseCurrency().add(diff));
					} else if (irTotalInvoicedAmt.compareTo(irTotalCost.getApproxAmountInBaseCurrency()) > 0  ) {
						//sum of Amounts is greater than total, minus the diff amount to the discount
						BigDecimal diff = irTotalInvoicedAmt.subtract(irTotalCost.getApproxAmountInBaseCurrency());
						Log.customer.debug("Amounts is greater than total, minus the diff "+diff);
						InvoiceSplitDiscountDollarAmountMoney.setApproxAmountInBaseCurrency(InvoiceSplitDiscountDollarAmountMoney.getApproxAmountInBaseCurrency().subtract(diff));
					}
				}

			Log.customer.debug("5....For the LI: " + irLine + "For the Split: LineNumber is: " + (String)splitAcc.getFieldValue("CapsLineNumber") );

			if ( iLineType == 2 ) {

				irtax = (BigDecimal)irLine.getDottedFieldValue("Amount.Amount");
				Log.customer.debug("LIAmt....." + irtax);
				Log.customer.debug("TotAmt...." + totamt);
				if ( (totamt != null) && (totamt.doubleValue() != 0) ) {
					if ( "002".equals ((String)irLine.getDottedFieldValue("CapsChargeCode.UniqueName"))
							|| "003".equals ((String)irLine.getDottedFieldValue("CapsChargeCode.UniqueName")) )
						irtax = irtax.divide(totamt, 5, 2);
					else
						irtax = Constants.ZeroBigDecimal;
				} else {
					irtax = Constants.ZeroBigDecimal;
				}
				irLine.setFieldValue("IRTaxRate", irtax );
				Log.customer.debug("IRTaxRt..." + irtax);
			} else {
				irLine.setFieldValue("IRTaxRate", Constants.ZeroBigDecimal);
			}
		}
	}


	/*
	* If its a Foreign Invoice (non-USD) then IT.TotalCost.AmtInBaseCurr must match with sum of
	* all line.Amount.AmtInBaseCurr. Each line's line.Amount.AmtInBaseCurr must match
	* the SplitAmounts for each of those lines. Adjustments are made to correct differrences.
	*/
	public void adjustCurrencyOnLines() {

		Log.customer.debug("CAPSInvoiceProcess: in adjustCurrencyOnLines");
		InvoiceReconciliationLineItem irLineItem;

		//Calculate the AmountinBaseCurrancy for latest currency as of Push date.
		//Done to TotalCost, LineItemAmt and SplitAmount
		if (IsForeign) (invrecon.getTotalCost()).setConversionDate(currDate);
		Money totalCost = invrecon.getTotalCost();
		totalCost.setApproxAmountInBaseCurrency(BigDecimalFormatter.round(totalCost.getApproxAmountInBaseCurrency(),2));
		BigDecimal irTotalCost = totalCost.getApproxAmountInBaseCurrency();
		BigDecimal irLineAmtTotalSum = Constants.ZeroBigDecimal;
		Log.customer.debug("CAPSInvoiceProcess: irTotalCost (rounded 2) ="+irTotalCost);

		for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
			irLineItem = (InvoiceReconciliationLineItem)i.next();
			if (IsForeign) (irLineItem.getAmount()).setConversionDate(currDate);
			irLineItem.getAmount().setApproxAmountInBaseCurrency(BigDecimalFormatter.round(irLineItem.getAmount().getApproxAmountInBaseCurrency(),2));
			Log.customer.debug("CAPSInvoiceProcess: ADD irLineAmtTotal="+irLineItem.getAmount().getApproxAmountInBaseCurrency());
			irLineAmtTotalSum = irLineAmtTotalSum.add(irLineItem.getAmount().getApproxAmountInBaseCurrency());
			Log.customer.debug("CAPSInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
		}
		Log.customer.debug("CAPSInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
		Log.customer.debug("CAPSInvoiceProcess: compare="+irLineAmtTotalSum.compareTo(irTotalCost));
		// -1 if less, 0 equals, 1 greater than
		if(irLineAmtTotalSum.compareTo(irTotalCost) < 0 ) {
			//sum of line Amounts is less than total, add to first line the diff amount
			BigDecimal diff = irTotalCost.subtract(irLineAmtTotalSum);
			Log.customer.debug("CAPSInvoiceProcess: diff1="+diff);
			Money firstLineAmount = invrecon.getLineItem(1).getAmount();
			Log.customer.debug("CAPSInvoiceProcess: firstLineAmount="+firstLineAmount.getApproxAmountInBaseCurrency());
			firstLineAmount.setApproxAmountInBaseCurrency(firstLineAmount.getApproxAmountInBaseCurrency().add(diff));
			Log.customer.debug("CAPSInvoiceProcess: after update firstLineAmount="+firstLineAmount.getApproxAmountInBaseCurrency());
		} else if (irLineAmtTotalSum.compareTo(irTotalCost) > 0 ) {
			//sum of line Amounts is greater than total, minus to first line the diff amount
			BigDecimal diff = irLineAmtTotalSum.subtract(irTotalCost);
			Log.customer.debug("CAPSInvoiceProcess: diff2="+diff);
			Money firstLineAmount = invrecon.getLineItem(1).getAmount();
			Log.customer.debug("CAPSInvoiceProcess: firstLineAmount="+firstLineAmount.getApproxAmountInBaseCurrency());
			firstLineAmount.setApproxAmountInBaseCurrency(firstLineAmount.getApproxAmountInBaseCurrency().subtract(diff));
			Log.customer.debug("CAPSInvoiceProcess: after update firstLineAmount="+firstLineAmount.getApproxAmountInBaseCurrency());
		}

		//once the total with lineItemAmts are corrected, check the lineAmount to its splitAmts.
		for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
			irLineItem = (InvoiceReconciliationLineItem)i.next();
			SplitAccounting splitAcc;
			BigDecimal irLineItemAmount = irLineItem.getAmount().getApproxAmountInBaseCurrency();
			BigDecimal lineItemSplitAmountSum = Constants.ZeroBigDecimal;

			for(Iterator s= irLineItem.getAccountings().getSplitAccountingsIterator(); s.hasNext();) {
				splitAcc = (SplitAccounting) s.next();
				if (IsForeign) (splitAcc.getAmount()).setConversionDate(currDate);
				splitAcc.getAmount().setApproxAmountInBaseCurrency(BigDecimalFormatter.round(splitAcc.getAmount().getApproxAmountInBaseCurrency(),2));
				Log.customer.debug("CAPSInvoiceProcess: ADD lineItemSplitAmount="+splitAcc.getAmount().getApproxAmountInBaseCurrency());
				lineItemSplitAmountSum = lineItemSplitAmountSum.add(splitAcc.getAmount().getApproxAmountInBaseCurrency());
				Log.customer.debug("CAPSInvoiceProcess: lineItemSplitAmountSum="+lineItemSplitAmountSum);
			}

			if(lineItemSplitAmountSum.compareTo(irLineItemAmount) < 0 ) {
				//sum of line Amounts is less than total, add to first line the diff amount
				BigDecimal diff = irLineItemAmount.subtract(lineItemSplitAmountSum);
				Log.customer.debug("CAPSInvoiceProcess: SplitAmt diff="+diff);
				Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
				Log.customer.debug("CAPSInvoiceProcess: firstSplitAmt="+firstSplitAmt.getApproxAmountInBaseCurrency());
				firstSplitAmt.setApproxAmountInBaseCurrency(firstSplitAmt.getApproxAmountInBaseCurrency().add(diff));
				Log.customer.debug("CAPSInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getApproxAmountInBaseCurrency());
			} else if (lineItemSplitAmountSum.compareTo(irLineItemAmount) > 0 ) {
				//sum of line Amounts is greater than total, add to first line the diff amount
				BigDecimal diff = lineItemSplitAmountSum.subtract(irLineItemAmount);
				Log.customer.debug("CAPSInvoiceProcess: SplitAmt diff="+diff);
				Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
				Log.customer.debug("CAPSInvoiceProcess: firstSplitAmt="+firstSplitAmt.getApproxAmountInBaseCurrency());
				firstSplitAmt.setApproxAmountInBaseCurrency(firstSplitAmt.getApproxAmountInBaseCurrency().subtract(diff));
				Log.customer.debug("CAPSInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getApproxAmountInBaseCurrency());
			}
		}
	}

    public CAPSInvoiceProcess(){}
	private static final String classname = "CAPSInvoiceProcess: ";
}
