/***********************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Processing the IR object depending upon the StatusString of the object -

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------
	5/31/2005 	Kingshuk	Populating the ReceiptInfo object inside the IRLineItem and setting
							the FacilityFlag and ActionFlag accordingly

	09/13/2006  Chandra		From receipt, check if order is null and ma is not null, then get the
							info from ma and send.

    01/10/08	Amit		Modified the query to remove the like statements but instead using " = " ;
    						and also checking if the IR line Item is not empty.

	02/13/08	Amit		Added the rounding off logic on the amount related fields to avoid
							the failure of the IR's at MFG.

    03/16/08  Shaila     Added rounding off logic on the Description.Price and Invoice total cost

	05/08/10   Kannan	    Query for only the paid IRs

	07/21/11	Vikram		No check for receipts

************************************************************************************************/

package config.java.schedule.vcsv2;

import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.LineItemProductDescription;
import ariba.basic.core.MoneyFormatter;
import ariba.invoicing.core.*;
import ariba.util.core.Date;
import ariba.util.log.Log;
import ariba.util.core.*;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import ariba.basic.core.Money;
import java.math.BigDecimal;
import ariba.util.log.Log;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.base.core.Base;
import ariba.invoicing.core.Invoice;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;

import ariba.basic.core.Currency;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.scheduler.*;
import java.io.*;

import ariba.util.core.Vector;
import java.util.List;
import ariba.util.formatter.BooleanFormatter;
import ariba.procure.core.ProcureLineType;
import config.java.common.CatEmailNotificationUtil;

public class CATInvoiceProcess extends ScheduledTask {

    private Partition partition;
	private String query, query1, query2;
	private ClusterRoot cObj;
	private AQLQuery qry, qry1, qry2;
	private AQLOptions options, options1, options2;
	private AQLResultCollection results, results1, results2;
	private InvoiceReconciliationLineItem invreconli;
	private BaseVector invreconlicol;
	private ariba.invoicing.core.InvoiceReconciliation invrecon;
	private ariba.receiving.core.Receipt rcpt;
	private ariba.purchasing.core.DirectOrder order;
	private ariba.contract.core.Contract ma;
	private ariba.approvable.core.LineItem li;
	private ariba.receiving.core.ReceiptItem ri;
	private ariba.approvable.core.LineItemCollection lic;
	private ariba.common.core.SplitAccountingCollection sacol;
	private ariba.common.core.SplitAccounting sa;
	private ariba.basic.core.Currency repcur;
	private ariba.basic.core.Money totcost;
	private ClusterRoot fac;

	private File mfgIRErrReport = null;
	private BufferedWriter outmfgIRErrReport = null;
	String flatFilePath ="";
	String fileExtDate ="";

	/*
	 * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	 * Reason		: Along with 9r Server path might get changed.
	 */
		public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
			super.init(scheduler, scheduledTaskName, arguments);

			Date date = new Date();
			fileExtDate = getFileExtDate(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_Grief_Report_"+fileExtDate+".csv";

			for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
				String key = (String) e.next();
				if (key.equals("FlatFilePath")) {
					flatFilePath = (String) arguments.get(key);
					flatFilePath = flatFilePath + fileExtDate + ".csv";
					Log.customer.debug("CATUSDWInvoicePush_FlatFile : FlatFilePath "+ flatFilePath);
				}
			}
		}
	/*
	 * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	 * Reason		: Along with 9r Server path might get changed.
	 */


	public void run() throws ScheduledTaskException {

		Log.customer.debug("Setting up the InvoiceReconciliation objects.....");

        partition = ariba.base.core.Base.getSession().getPartition();

        //Processing Invoices which are already reconciled in Ariba
        //and not pushed to WBI Before (Reconciled Push)
        query = new String ("select * from ariba.invoicing.core.InvoiceReconciliation "
        		+ " where StatusString = 'Paid' and  " +        		"( ActionFlag IS NULL or ActionFlag != 'Completed' or ActionFlag = 'InProcess-Error' and ActionFlag != 'InProcess' )");

        populateValues("InProcess", "Reconciled");

		sendErrorReportInMail(mfgIRErrReport);
	}

    void populateValues(String actionflag, String invstat) throws ScheduledTaskException {

		boolean IsNewIR = true;
        try {
    		Log.customer.debug(query);

    		qry = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(qry,options);

			if (results.getErrors() != null) {
				Log.customer.debug("ERROR GETTING RESULTS in Results1");
			}

			while(results.next()) {
				order = null;
				ma = null;
				IsNewIR = true;
				boolean IsIRHaveError = false;

				//Setting the FacilityFlag and the ActionFlag
				String unique = null;
				String afac = "";
				String orderId = "";

				invrecon = (InvoiceReconciliation)(results.getBaseId("InvoiceReconciliation").get());

				if (invrecon == null) continue;
				Log.customer.debug("2...." + invrecon.toString());

				unique = (java.lang.String)invrecon.getFieldValue("UniqueName");

				// To ensure no IR with zero line item is processed
				int lineCount = invrecon.getLineItemsCount();
				Log.customer.debug("Line Item count for IR "+invrecon+" is : "+lineCount);
				if (lineCount == 0) continue;

				InvoiceReconciliationLineItem doinvreconli = (InvoiceReconciliationLineItem) invrecon.getLineItems().get(0);
				if (doinvreconli == null) continue;

				// rounding off - to ensure the total amount on the IR and line item amounts match
				adjustPrecisionOnLines(invrecon);




				order = (ariba.purchasing.core.DirectOrder)doinvreconli.getFieldValue("Order");
				ma =  (ariba.contract.core.Contract)doinvreconli.getFieldValue("MasterAgreement");

				ariba.util.core.Date POCreateDate = null;
				if (order != null) {

					orderId = (String)order.getFieldValue("UniqueName");
					POCreateDate = (ariba.util.core.Date) order.getFieldValue("TimeCreated");

					if (order.getFieldValue("SiteFacility") != null) {
						ClusterRoot sfac = (ClusterRoot)order.getFieldValue("SiteFacility");
						invrecon.setFieldValue("SiteFacility", sfac);
					}

					fac = (ClusterRoot)order.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility");
					if (fac != null) {
						afac = (String)fac.getFieldValue("UniqueName");
					}

					//If ExchangeRate is null then set it.
					/* AEC COMMENT OUT - IS THIS REALLY NEEDED?  EXCHANGE RATE IS NOT USED ANYWHERE IN THE PUSH FILE. DOES IT CHANGE THE $ VALUES?

					if ( invrecon.getFieldValue("ExchangeRate") == null ) {
						if (order.getFieldValue("TotalCost") != null) {

							totcost = (ariba.basic.core.Money)order.getFieldValue("TotalCost");
							java.math.BigDecimal usdgbprate = null;
							java.math.BigDecimal otherusdrate = null;
							ariba.basic.core.Currency cur = (ariba.basic.core.Currency)totcost.getFieldValue("Currency");
							String curstr = (java.lang.String)cur.getFieldValue("UniqueName");

							//Finds GBP:USD
							String exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, "
																+ " Year(Modified) YYYY "
																+ " from ariba.basic.core.CurrencyConversionRate "
															+ " where UniqueName like 'GBP:USD' "
															+ " and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder "
															+ " where UniqueName like '" + orderId + "') ORDER BY Modified");
							//Log.customer.debug("Query for ExRate...." + exquery);
							AQLQuery exqry = AQLQuery.parseQuery(exquery);
							AQLOptions exoptions = new AQLOptions(partition);
							AQLResultCollection exresults = Base.getService().executeQuery(exqry,exoptions);
							if (exresults.getErrors() != null) {
								Log.customer.debug("ERROR GETTING RESULTS in Results1");
							}

							while(exresults.next()) {
								usdgbprate = exresults.getBigDecimal("Rate");
							}
							Log.customer.debug("Rate from Query...." + usdgbprate);

							//Finding other Currency Rate with USD
							exquery = new String("select UniqueName, Rate, Modified, Month(Modified) MM, "
													+ " Year(Modified) YYYY "
													+ " from ariba.basic.core.CurrencyConversionRate "
													+ " where UniqueName like 'USD:" + curstr + "' "
													+ " and Modified <= (select TimeCreated from ariba.purchasing.core.DirectOrder "
													+ " where UniqueName like '" + orderId + "') ORDER BY Modified");
							//Log.customer.debug("Query for ExRate...." + exquery);
							exqry = AQLQuery.parseQuery(exquery);
							exoptions = new AQLOptions(partition);
							exresults = Base.getService().executeQuery(exqry,exoptions);
							if (exresults.getErrors() != null) {
								Log.customer.debug("ERROR GETTING RESULTS in Results1");
							}

							while(exresults.next()) {
								otherusdrate = exresults.getBigDecimal("Rate");
							}
							Log.customer.debug("Rate from Query...." + otherusdrate);

							//Multiplies ExchangeRate = OTHER:USD X USD:GBP
							if (usdgbprate != null && otherusdrate != null) {
								double d = 1.0D;

								if (curstr.equals("GBP"))
									d = 1.0D;
								else
									d = (usdgbprate.doubleValue())*(otherusdrate.doubleValue());

								Log.customer.debug("Multiplication Value is.... " + (usdgbprate.doubleValue())*(otherusdrate.doubleValue()));
								java.math.BigDecimal exRate = new java.math.BigDecimal(d);

								//exRate.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);
								invrecon.setFieldValue("ExchangeRate",exRate);
							}
							else	Log.customer.debug("Either of USD:GBP or other CurrencyRate is null. ExchangeRate could not be calculated...");
						}
						else	Log.customer.debug("Either of TotalCost or ReportedCurrency is null. ExchangeRate could not be calculated...");
					}*/
				}//End of - if order != null

				//Incase of MA w/o Receipt, IRs still needs to be pushed to completed
				//AEC COMMENT OUT boolean isMAReceivable = true;
				//if order ==null but ma != null
				if(order == null && ma != null) {

					orderId = (String)ma.getFieldValue("UniqueName");
					POCreateDate = (ariba.util.core.Date) ma.getFieldValue("TimeCreated");

					ClusterRoot sfac = (ClusterRoot)ma.getDottedFieldValue("LineItems[0].Accountings.SplitAccountings[0].Facility");
					invrecon.setFieldValue("SiteFacility", sfac);

					if (sfac != null) {
						//afac for IR from MALineItem Accounting
						afac = (String)sfac.getFieldValue("UniqueName");
					}
					//AEC COMMENT OUT isMAReceivable = BooleanFormatter.getBooleanValue(ma.getFieldValue("IsReceivable"));
					//AEC COMMENT OUT Log.customer.debug("MA=%s...isMAReceivable=" + isMAReceivable, orderId );
				}


				if (invrecon.getFieldValue("ActionFlag") == null) {
					IsNewIR = true;
				} else {
					IsNewIR = false;
				}

				/**/
				//Code was moved down so after all processing of either order or ma, the values can be set to IR
				Invoice inv = (Invoice)invrecon.getFieldValue("Invoice");

				Invoice irinv = (Invoice)invrecon.getFieldValue("Invoice");
				ariba.util.core.Date InvoiceDate = (ariba.util.core.Date)irinv.getFieldValue("InvoiceDate");
				invrecon.setFieldValue("InvoiceDate", InvoiceDate);

				//Setting the PONumber, SiteFacility & InvoiceNumber inside the IR (values got from either order or ma)
				invrecon.setFieldValue("PONumber", orderId);
				invrecon.setFieldValue("InvoiceNumber", (String)inv.getFieldValue("InvoiceNumber") );
				invrecon.setFieldValue("POCreateDate", POCreateDate);
				invrecon.setFieldValue("TopicName", "MFGInvoiceReconciliationPush");

				// For Error Report
				if ( checkFlatFileFields(invrecon)) {

				    invrecon.setFieldValue("ActionFlag",new String(actionflag));
				}
				else {

				   invrecon.setFieldValue("ActionFlag","InProcess-Error");
				}


				if (invstat.equals("Reconciled") == false) {
					invrecon.setFieldValue("InvoiceStatus",new String(invstat));
				}

				invrecon.setFieldValue("FacilityFlag",afac);
				/**/

				//Setting the receiptInfo vector inside the InvoiceReconciliationLineItem object
				int invlinecount = invrecon.getLineItemsCount();

				//Checks if there is any special charge line with no material line
				boolean isNoMatLineWSpecialCharge = isNoMatLineWSpecialCharge();
				if (isNoMatLineWSpecialCharge)
					invrecon.setFieldValue("InvoiceStatus",new String("Reconciled"));


				Log.customer.debug("-------------------------------------------End of IR--------------------------------------------------------------------");

				ariba.base.core.Base.getSession().transactionCommit();
			} // end of while

			Log.customer.debug("Ending IRProcess program .....");

		} catch (Exception e) {
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error while running CATInvoiceProcess = "+ e.toString(), e);
		}
    }



    boolean checkFlatFileFields(InvoiceReconciliation invrecon) {
		Log.customer.debug(" Inside checkFlatFileFields " + invrecon.getUniqueName() );
    	String errorTxt = "";
		SplitAccounting splitAcc;
		int irLineItemVectorSize = 0;
		BaseVector irLineItemVector=null;
		irLineItemVector = (BaseVector)invrecon.getLineItems();
		irLineItemVectorSize = invrecon.getLineItemsCount();

		Money invtotalCost = (Money)invrecon.getFieldValue("TotalCost");
		BigDecimal iRTotalCost = (BigDecimal)invtotalCost.getAmount();
		iRTotalCost.setScale(2, BigDecimal.ROUND_HALF_UP);
		Log.customer.debug(" iRTotalCost  " + iRTotalCost );

        BigDecimal iRLineTotalCost = new BigDecimal("0.00");
		BigDecimal iRLineTotalCostWitTax = new BigDecimal("0.00");

		for (int i =0; i<irLineItemVectorSize;i++){

			InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem)irLineItemVector.get(i);


         //	 The VAT total equals the sum of the tax at line level. Grief if not.




		 Log.customer.debug(" before add iRLineTotalCost   for line item " +invrecon.getUniqueName()+ "  line item ## " + i +" "+ iRLineTotalCost );
		 Money linetotalCost = (Money)irLineItem.getFieldValue("Amount");
	     BigDecimal iRLineTotalCostAmt = (BigDecimal)linetotalCost.getAmount();
		 iRLineTotalCostAmt.setScale(2, BigDecimal.ROUND_HALF_UP);
		 Log.customer.debug("iRLineTotalCostAmt: "+iRLineTotalCostAmt);
		 iRLineTotalCost =  iRLineTotalCost.add(iRLineTotalCostAmt);
		 iRLineTotalCost.setScale(2, BigDecimal.ROUND_HALF_UP);
		 Log.customer.debug(" iRLineTotalCost   for line item " +invrecon.getUniqueName()+ "  line item ## " + i +" "+ iRLineTotalCost );

		 // Change by VJS to avoid null pointer exception
		 if (irLineItem.getDottedFieldValue("VATClass") != null && irLineItem.getFieldValue("TaxAmount") != null)
		 //if (irLineItem.getDottedFieldValue("VATClass") != null)
		 {
			  Log.customer.debug(" before add iRLineTotalCostWitTax   for line item " +invrecon.getUniqueName()+ "  line item ## " + i +" "+ iRLineTotalCostWitTax );
			  Money linetotalCostTax = (Money)irLineItem.getFieldValue("TaxAmount");
	          BigDecimal iRLineTotalCostTax = (BigDecimal)linetotalCostTax.getAmount();
			  iRLineTotalCostTax.setScale(2, BigDecimal.ROUND_HALF_UP);
			  Log.customer.debug("iRLineTotalCostTax: "+iRLineTotalCostTax);
			  iRLineTotalCostWitTax =  iRLineTotalCostWitTax.add(iRLineTotalCostAmt.add(iRLineTotalCostTax));
			  iRLineTotalCostWitTax.setScale(2, BigDecimal.ROUND_HALF_UP);
			  Log.customer.debug(" iRLineTotalCostWitTax   for line item " +invrecon.getUniqueName()+ "  line item ## " + i +" "+ iRLineTotalCostWitTax );

		 }


		    SplitAccountingCollection sacol =(SplitAccountingCollection) irLineItem.getFieldValue("Accountings");

			if (sacol != null && sacol.getSplitAccountings() != null) {
					BaseVector sas = sacol.getSplitAccountings();
				   for (int j =0; j <sas.size();j++){
						SplitAccounting splitAccounting  = (SplitAccounting)sas.get(j);
					    if(splitAccounting != null)   {
						if (splitAccounting.getFieldValue("Account") != null ) {}
						else{
							errorTxt = invrecon.getUniqueName() +" ,Line Number "+ irLineItem.getNumberInCollection() +  " grief in SplitAccountings Account ";
							Log.customer.debug(" errorTxt 1 "+ errorTxt );
							writeToErrReport(errorTxt+ "\n");

						}
						if (splitAccounting.getFieldValue("CostCenter") != null ) {}
						else {
							errorTxt = invrecon.getUniqueName() +" ,Line Number "+ irLineItem.getNumberInCollection() +  " grief in SplitAccountings CostCenter ";
							Log.customer.debug(" errorTxt 2 "+ errorTxt );
							writeToErrReport(errorTxt+ "\n");

						}

						//if (splitAccounting.getFieldValue("SubAccount") != null ) {}
						//else {
						//	errorTxt = invrecon.getUniqueName() +" : in Line Number "+ irLineItem.getNumberInCollection() +  " grief in SplitAccountings SubAccount ";
						//	Log.customer.debug(" errorTxt 3 "+ errorTxt );
						//	writeToErrReport(errorTxt+ "\n");
						//}
						if (splitAccounting.getFieldValue("Facility") != null ) {}
						else {
							errorTxt = invrecon.getUniqueName() +" ,Line Number "+ irLineItem.getNumberInCollection() +  " grief in SplitAccountings Facility ";
							Log.customer.debug(" errorTxt 4 "+ errorTxt );
							writeToErrReport(errorTxt+ "\n");

						}









					   } //if(splitAccounting != null)

					} //for (int j =0; j <sas.size();j++)



			}
			else {
				errorTxt = invrecon.getUniqueName() +", in Line Number "+ irLineItem.getNumberInCollection() +  " grief in Accountings ";
				Log.customer.debug(" errorTxt 5 "+ errorTxt );
				writeToErrReport(errorTxt+ "\n");

			}



		}
//		The invoice total equals the sum of the lines. Grief if not.
				 Log.customer.debug(invrecon.getUniqueName()+"@ checking iRTotalCost @"+ iRTotalCost );
				 Log.customer.debug(invrecon.getUniqueName()+"@ checking iRLineTotalCost@ "+ iRLineTotalCost );
				 if (iRTotalCost.compareTo(iRLineTotalCost) != 0){
					Log.customer.debug(invrecon.getUniqueName()+" iRLineTotalCost and iRTotalCost are not same");
					 errorTxt = invrecon.getUniqueName() +",Grief in IR total  " + iRTotalCost+" and sum of IR line Item Total cost "+ iRLineTotalCost  ;
					 Log.customer.debug(invrecon.getUniqueName() +" errorTxt 6 "+ errorTxt );
					 writeToErrReport(errorTxt+ "\n");
				 }

//		The invoice total equals the sum of the lines with tax. Grief if not.
				 Log.customer.debug(invrecon.getUniqueName()+"@ checking iRTotalCost @"+ iRTotalCost );
				 Log.customer.debug(invrecon.getUniqueName()+"@ checking iRLineTotalCost@ "+ iRLineTotalCostWitTax );
				 if (iRTotalCost.compareTo(iRLineTotalCostWitTax) != 0) {
					Log.customer.debug(invrecon.getUniqueName()+" iRLineTotalCostWitTax and iRTotalCost are not same");
					 errorTxt = invrecon.getUniqueName() +",Grief in IR total  " + iRTotalCost+" and sum of IR line Item with tax Total cost "+ iRLineTotalCostWitTax  ;
					 Log.customer.debug(invrecon.getUniqueName() +" errorTxt 7 "+ errorTxt );
					 writeToErrReport(errorTxt+ "\n");
				 }

//		The TaxAmount is null on the material line - Change by VJS
		for (int k =0; k<irLineItemVectorSize;k++){

			InvoiceReconciliationLineItem irLineItem2 = (InvoiceReconciliationLineItem)irLineItemVector.get(k);

				 if (irLineItem2.getDottedFieldValue("VATClass") != null && irLineItem2.getFieldValue("TaxAmount") == null){
					Log.customer.debug(invrecon.getUniqueName()+"Tax Amount is null on the material line");
					errorTxt = invrecon.getUniqueName() +", in Line Number "+ irLineItem2.getNumberInCollection() +  " tax amount is missing on material line ";
					Log.customer.debug(invrecon.getUniqueName() +" errorTxt 8 "+ errorTxt );
					writeToErrReport(errorTxt+ "\n");
				 }
		}

    	if (errorTxt != "") {
    		return false;
    	}
    	else {
    	return true;
    	}
    }

	private void writeToErrReport(String errorTxt) {
		Log.customer.debug("flatFilePath in  mfgIRErrReport .."+flatFilePath);
		try {
			mfgIRErrReport = new File(flatFilePath);
			if (!mfgIRErrReport.exists()) {
				Log.customer.debug("File not exist");
				mfgIRErrReport.createNewFile();
			}
			//outmfgIRErrReport = new PrintWriter(IOUtil.bufferedOutputStream(mfgIRErrReport), true);
			outmfgIRErrReport = new BufferedWriter(new FileWriter(mfgIRErrReport, true));
			Log.customer.debug("mfgIRErrReport " + mfgIRErrReport);
			// Write to file
			outmfgIRErrReport.write(errorTxt);
			outmfgIRErrReport.flush();





		} catch (Exception e) {
			Log.customer.debug("Exception in  mfgIRErrReport .."+e);

		}
		finally {
			try {
					if (outmfgIRErrReport != null) {
						//outmfgIRErrReport.flush();
						outmfgIRErrReport.close();
				   }
			} catch (Exception e1) {

				e1.printStackTrace();
			}

		}

		}


	private String getFileExtDate(Date date) {
			Log.customer.debug("date => "+ date);
			SimpleDateFormat formatter   = new SimpleDateFormat ("ddMMyy");
			String dateString = formatter.format(date);
			Log.customer.debug("dateString => "+ dateString);
			return dateString;
		}
		private void sendErrorReportInMail(File mfgIRErrReport){
          //	Sending email
         try {
			 Date date = new Date();
			 String mailSubject = "UK IR grief Daily Report "+ date;
			 List attachmentObjList = ListUtil.list();
			 attachmentObjList.add(mfgIRErrReport);
			 String message = "Please find Today "+ date + " UK IR grief Daily Report";
			 CatEmailNotificationUtil.sendEmailNotification(mailSubject,message,"cat.java.emails", "MFGIRFlatFilePushNotify",attachmentObjList);
		} catch (Exception e) {
			Log.customer.debug("ERROR in sending mail");
		}



		}


    ClusterRoot ObjectAlreadyExist(String receiptnique) {
		AQLQuery aqlqry = null;
		AQLOptions aqloptions = null;
		AQLResultCollection aqlresults = null;

		aqlqry = AQLQuery.parseQuery("select * from cat.core.receiptInfo where UniqueName like '" + receiptnique + "'");
		Log.customer.debug("select * from cat.core.receiptInfo where UniqueName like '" + receiptnique + "'");
		aqloptions = new AQLOptions(partition);
		aqlresults = Base.getService().executeQuery(aqlqry,aqloptions);
		if (aqlresults.getErrors() != null) {
			Log.customer.debug("ERROR GETTING RESULTS in Results1");
		}

		while(aqlresults.next()) {
			return (ClusterRoot)(aqlresults.getBaseId(0).get());
		}
		return null;
	}

	boolean isNoMatLineWSpecialCharge()	{
		boolean returnval = false;
		for (int i=0; i< invrecon.getLineItemsCount(); i++)	{
			InvoiceReconciliationLineItem invreconline = (InvoiceReconciliationLineItem)invrecon.getLineItems().get(i);

			int iCategory = 0;
			ProcureLineType linetype = null;

			linetype = (ProcureLineType)invreconline.getFieldValue("LineType");
			if (linetype != null) {
				iCategory = ((java.lang.Integer)linetype.getFieldValue("Category")).intValue();
			}
			Log.customer.debug("Invoice LineType Categoty is...." + iCategory + " For " + i +" th LI...");

			if (iCategory == 16) {
				returnval = true;
			}
			else if (iCategory == 1) {
				returnval = false;
				break;
			}
		}
		Log.customer.debug("IR isNoMatLineWSpecialCharge IS *** " + returnval);
		return returnval;
	}

	public void adjustPrecisionOnLines(InvoiceReconciliation invrecon){

 		Log.customer.debug("CATInvoiceProcess: in adjustPrecisionOnLines");
        InvoiceReconciliationLineItem irLineItem;

        // Rounding off done to TotalCost, LineItemAmt and SplitAmount
        Money totalCost = invrecon.getTotalCost();
        totalCost.setAmount(BigDecimalFormatter.round(totalCost.getAmount(),2));
        BigDecimal irTotalCost = totalCost.getAmount();
        BigDecimal irLineAmtTotalSum = Constants.ZeroBigDecimal;
        Log.customer.debug("CATInvoiceProcess: irTotalCost after rounding 2 places ="+irTotalCost);

        for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
            irLineItem = (InvoiceReconciliationLineItem)i.next();
            irLineItem.getAmount().setAmount(BigDecimalFormatter.round(irLineItem.getAmount().getAmount(),2));
            Log.customer.debug("CATInvoiceProcess: Adding to irLineAmtTotal="+irLineItem.getAmount().getAmount());

           LineItemProductDescription desc = (LineItemProductDescription)irLineItem.getDescription();
           Log.customer.debug("CATInvoiceProcess: desc ="+desc);
           Money price_amt = (Money)desc.getPrice();
           Log.customer.debug("CATInvoiceProcess: price_amt ="+price_amt);
		  // desc.setAmount(Money.round(price_amt.getAmount(),2));
		   irLineItem.getDescription().getPrice().setAmount(BigDecimalFormatter.round(irLineItem.getDescription().getPrice().getAmount(),2));
		  Log.customer.debug("CATInvoiceProcess: price_amt="+price_amt.getAmount());

		  /* BigDecimal price_amt = (BigDecimal)irLineItem.getDottedFieldValue("Description.Price.Amount");
		   irLineItem.setDottedFieldValue("Description.Amount.Amount",BigDecimalFormatter.round(price_amt,2)); */

            irLineAmtTotalSum = irLineAmtTotalSum.add(irLineItem.getAmount().getAmount());
            Log.customer.debug("CATInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
        }
        Log.customer.debug("CATInvoiceProcess: irLineAmtTotalSum="+irLineAmtTotalSum);
        Log.customer.debug("CATInvoiceProcess: Comparing IR Total cost with IR Line Amount total ="+irLineAmtTotalSum.compareTo(irTotalCost));

     	// -1 if less, 0 equals, 1 greater than
        if(irLineAmtTotalSum.compareTo(irTotalCost) < 0 ) {
            //sum of line Amounts is less than total, add to first line the diff amount
            BigDecimal diff = irTotalCost.subtract(irLineAmtTotalSum);
            Log.customer.debug("CATInvoiceProcess: Difference Amount(irTotalCost - irLineAmtTotalSum)="+diff);
            Money firstLineAmount = invrecon.getLineItem(1).getAmount();
            Log.customer.debug("CATInvoiceProcess: firstLineAmount="+firstLineAmount.getAmount());
            firstLineAmount.setAmount(firstLineAmount.getAmount().add(diff));
            Log.customer.debug("CATInvoiceProcess: after update firstLineAmount="+firstLineAmount.getAmount());
       } else if (irLineAmtTotalSum.compareTo(irTotalCost) > 0 ) {
            //sum of line Amounts is greater than total, minus to first line the diff amount
            BigDecimal diff = irLineAmtTotalSum.subtract(irTotalCost);
            Log.customer.debug("CATInvoiceProcess: diff2="+diff);
            Money firstLineAmount = invrecon.getLineItem(1).getAmount();
            Log.customer.debug("CATInvoiceProces	s: firstLineAmount="+firstLineAmount.getAmount());
            firstLineAmount.setAmount(firstLineAmount.getAmount().subtract(diff));
            Log.customer.debug("CATInvoiceProcess: after update firstLineAmount="+firstLineAmount.getAmount());
        }

        // Rounding Invoice totalCost Amount
        Invoice inv =(Invoice) invrecon.getInvoice();
        Money invTotalCost = inv.getTotalCost();
        invTotalCost.setAmount(BigDecimalFormatter.round(invTotalCost.getAmount(),2));
        Log.customer.debug("CATInvoiceProcess: after update invTotalCost="+invTotalCost.getAmount());

        //once the total with lineItemAmts are corrected, check the lineAmount to its split amounts total.
        for(Iterator i = invrecon.getLineItemsIterator(); i.hasNext();) {
            irLineItem = (InvoiceReconciliationLineItem)i.next();
            SplitAccounting splitAcc;
            BigDecimal irLineItemAmount = irLineItem.getAmount().getAmount();
            BigDecimal lineItemSplitAmountSum = Constants.ZeroBigDecimal;

            for(Iterator s= irLineItem.getAccountings().getSplitAccountingsIterator(); s.hasNext();) {
                splitAcc = (SplitAccounting) s.next();
                splitAcc.getAmount().setAmount(BigDecimalFormatter.round(splitAcc.getAmount().getAmount(),2));
                Log.customer.debug("CATInvoiceProcess: Adding lineItemSplitAmount="+splitAcc.getAmount().getAmount());
                lineItemSplitAmountSum = lineItemSplitAmountSum.add(splitAcc.getAmount().getAmount());
                Log.customer.debug("CATInvoiceProcess: lineItemSplitAmountSum="+lineItemSplitAmountSum);



            }

            if(lineItemSplitAmountSum.compareTo(irLineItemAmount) < 0 ) {
                //sum of split Amounts is less than line total, add to first line the diff amount
                BigDecimal diff = irLineItemAmount.subtract(lineItemSplitAmountSum);
                Log.customer.debug("CATInvoiceProcess: SplitAmt diff="+diff);
                Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
                Log.customer.debug("CATInvoiceProcess: firstSplitAmt="+firstSplitAmt.getAmount());
                firstSplitAmt.setAmount(firstSplitAmt.getAmount().add(diff));
                Log.customer.debug("CATInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getAmount());
            } else if (lineItemSplitAmountSum.compareTo(irLineItemAmount) > 0 ) {
                //sum of split Amounts is greater than line total, add to first line the diff amount
                BigDecimal diff = lineItemSplitAmountSum.subtract(irLineItemAmount);
                Log.customer.debug("CATInvoiceProcess: SplitAmt diff="+diff);
                Money firstSplitAmt = (Money)irLineItem.getDottedFieldValue("Accountings.SplitAccountings[0].Amount");
                Log.customer.debug("CATInvoiceProcess: firstSplitAmt="+firstSplitAmt.getAmount());
                firstSplitAmt.setAmount(firstSplitAmt.getAmount().subtract(diff));
                Log.customer.debug("CATInvoiceProcess: after update firstSplitAmt="+firstSplitAmt.getAmount());
            }
        }
	}

    public CATInvoiceProcess (){}




}
