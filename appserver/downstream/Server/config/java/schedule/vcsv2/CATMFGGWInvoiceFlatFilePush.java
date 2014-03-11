/***********************************************************************************************

	Creator: Vikram J Singh
	Description: Performing invoice push via flat file for GW Facility

************************************************************************************************/

package config.java.schedule.vcsv2;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import config.java.common.CatEmailNotificationUtil;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.core.Vector;
import ariba.util.formatter.DateFormatter;


import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;


public class CATMFGGWInvoiceFlatFilePush extends ScheduledTask {

	private String thisclass = "CATMFGGWInvoiceFlatFilePush";

	private PrintWriter outPW_FlatFile = null;

	//String fileExtDateTime = getFileExtDateTime();
	//String flatFilePath = "/msc/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".txt";
	//String triggerFile =  "/msc/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".dstrigger";
	String fileExtDateTime ="";
	String flatFilePath ="";
	String triggerFile ="";

	AQLOptions options;
	AQLQuery aqlIRQueryGW,aqlTCSumQueryGW;
	AQLResultCollection irResultSetGW,tcSumResultSetGW;
	int totalNumberOfIrsGW;


	private FastStringBuffer message = null;
	private String mailSubject = null;
	private int resultCountGW, pushedCountGW;
    private String startTime, endTime;
	Partition partition = Base.getService().getPartition("mfg1");

	private Vector iDGWDataVector = new Vector();



	/*
	 * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	 * Reason		: Along with 9r Server path might get changed.
	 */
	 /*
		public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
			super.init(scheduler, scheduledTaskName, arguments);

		    Date date = new Date();
			fileExtDateTime = (String)getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".txt";
			triggerFile =  "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".dstrigger";

			for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
				String key = (String) e.next();
				if (key.equals("FlatFilePath")) {
					flatFilePath = (String) arguments.get(key);
					flatFilePath = flatFilePath + fileExtDateTime + ".txt";
					Log.customer.debug("CATUSDWInvoicePush_FlatFile : FlatFilePath "+ flatFilePath);
				}
				if (key.equals("TriggerFilePath")) {
					triggerFile = (String) arguments.get(key);
					triggerFile = triggerFile + fileExtDateTime + ".dstrigger";
					Log.customer.debug("CATUSDWInvoicePush_FlatFile : TriggerFilePath "+ triggerFile);
				}
			}
		}
		*/
	/*
	 * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	 * Reason		: Along with 9r Server path might get changed.
	 */



	public void run() throws ScheduledTaskException {

		try {

			startTime = ariba.util.formatter.DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject = "CATMFGGWInvoiceFlatFilePush Task Completion Status - Completed Successfully";

			Date date = new Date();
			fileExtDateTime = getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_GW_MFGPro."+fileExtDateTime+".txt";
			triggerFile =  "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_GW_MFGPro."+fileExtDateTime+".dstrigger";

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);
			File mfgGWIRFlatFile = new File(flatFilePath);
			options = new AQLOptions(partition);
			InvoiceReconciliation invrecon = null;


			if (!mfgGWIRFlatFile.exists()) {
				Log.customer.debug("File not exist");
				mfgGWIRFlatFile.createNewFile();
			}
			outPW_FlatFile = new PrintWriter(IOUtil.bufferedOutputStream(mfgGWIRFlatFile), true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);



           // For Facility GW BA Data

			String iRQueryGW = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag  = 'DX' and SiteFacility.UniqueName = 'GW'");
			Log.customer.debug("iRQueryGW ==> " +iRQueryGW);

			aqlIRQueryGW = AQLQuery.parseQuery(iRQueryGW);
			irResultSetGW = Base.getService().executeQuery(aqlIRQueryGW, options);

			if(irResultSetGW.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResultSetGW");

			totalNumberOfIrsGW = irResultSetGW.getSize();
			Log.customer.debug("totalNumberOfIrsGW ==> " +totalNumberOfIrsGW);
			resultCountGW = totalNumberOfIrsGW;


			if (totalNumberOfIrsGW >0){


			String iRTCSumQueryGW = new String ("select sum(TotalCost.Amount ) from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'DX' and SiteFacility.UniqueName = 'GW'");
			Log.customer.debug("totalNumberOfIrsGW ==> " +iRTCSumQueryGW);
			aqlTCSumQueryGW = AQLQuery.parseQuery(iRTCSumQueryGW);
			tcSumResultSetGW = Base.getService().executeQuery(aqlTCSumQueryGW, options);

			if(tcSumResultSetGW.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in tcSumResultSetGW");

			String recordTypeBA = "BA";
			String facilityGW = "GW";

			// GW sum of IR TotalCost

			double GWcontrol =0.0;

			while(tcSumResultSetGW.next()){
				GWcontrol = (double )(tcSumResultSetGW.getDouble(0));
				Log.customer.debug("GWcontrol => "+ GWcontrol);

			}

			String GWcontrolFormated = getFormatedAmt(GWcontrol);

			// Write BA info to file
			Log.customer.debug(recordTypeBA + facilityGW + GWcontrolFormated);
			String bAData = recordTypeBA + facilityGW + GWcontrolFormated;
			outPW_FlatFile.write(bAData + "\n");

			// For GW IH data

            String iHGWFileData ="";
			String iDGWData ="";
			while(irResultSetGW.next()){
				invrecon = (InvoiceReconciliation)(irResultSetGW.getBaseId("InvoiceReconciliation").get());

			if(invrecon != null){

			String iHGWRecordType = "IH";
			String iHGWFacility = "GW";

			// IR total cost
			//double iHGWVoControl = -1000.00;
			double iHGWVoControl = invrecon.getTotalCost().getAmountAsDouble();
			String iHGWFormattedVoControl = getFormatedAmt(iHGWVoControl);
			Log.customer.debug("iHGWFormattedVoControl => "+ iHGWFormattedVoControl);

			// Supplier Location Code

			//String iHGWSupplierCode = "80782";
			String iHGWSupplierLocCode = invrecon.getSupplierLocation().getUniqueName();
			String iHGWFomattedSupplierLocCode = addSpacesToSLC(iHGWSupplierLocCode);
			Log.customer.debug("iHGWFomattedSupplierLocCode => "+ iHGWFomattedSupplierLocCode);

			// Tax Date
			Date iHGWTaxDate = (Date)invrecon.getDottedFieldValue("Invoice.TimeCreated");
			String iHGWFormattedTaxDate = getFormattedTaxDate(iHGWTaxDate);
			Log.customer.debug("iHGWFormattedTaxDate => "+ iHGWFormattedTaxDate);

			/*// shipTo
			String iHGWFormattedShipTo ="";
			String iHGWShipTo ="";
			iHGWShipTo = (String)invrecon.getFieldValue("FacilityFlag");
			//String iHGWShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
			if (iHGWShipTo != null)
			iHGWFormattedShipTo = getFormatettedTxt(iHGWShipTo, 8);
			else
				iHGWFormattedShipTo = getFormatettedTxt(" ", 8);
			Log.customer.debug("iHGWFormattedShipTo => "+ iHGWFormattedShipTo);
			*/

			// shipTo
			String iHGWFormattedShipTo ="";
			String iHGWShipTo ="";
			//iHGWShipTo = (String)invrecon.getFieldValue("FacilityFlag");
			//String iHGWShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
			//if (iHGWShipTo != null)
			iHGWFormattedShipTo = getFormatettedTxt(iHGWShipTo, 8);
			//else
				//iHGWFormattedShipTo = getFormatettedTxt(" ", 8);
			Log.customer.debug("iHGWFormattedShipTo => "+ iHGWFormattedShipTo);

			String iHGWCurrency = (String) invrecon.getTotalCost().getCurrency().getUniqueName();
			Log.customer.debug("iHGWCurrency => "+ iHGWCurrency);

			String iHGWSupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
			String iHGWFormattedSupInvNumber = getFormatettedTxt(iHGWSupInvNumber, 20);
			Log.customer.debug("iHGWFormattedSupInvNumber => "+ iHGWFormattedSupInvNumber);

			Date iHGWInvoiceDate = (Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate");
			String iHGWFormattedInvoiceDate = getFormattedTaxDate(iHGWInvoiceDate);
			Log.customer.debug("iHGWFormattedInvoiceDate => "+ iHGWFormattedInvoiceDate);

			String iHGWRemark = invrecon.getUniqueName();
			int invleng = iHGWSupInvNumber.length();
			String iHGWFormattedRemark = getFormatattedTxt(iHGWRemark, invleng);
			Log.customer.debug("iHGWFormattedRemark => "+ iHGWFormattedRemark);

			//double iHGWTaxTotal = invrecon.getTotalTax().getAmountAsDouble();
			//String iHGWFormattedtaxToat = getFormatedAmt(iHGWTaxTotal);
			//Log.customer.debug("iHGWFormattedtaxToat => "+ iHGWFormattedtaxToat);





			// RECORD TYPE: INVOICE DETAIL
			BaseVector irLineItemVector=null;
			int irLineItemVectorSize = 0;
			irLineItemVector = (BaseVector)invrecon.getLineItems();
			irLineItemVectorSize = invrecon.getLineItemsCount();

			double iHGWTaxTotal = 0.00;


			if (irLineItemVectorSize > 0){


				for (int i =0; i<irLineItemVectorSize;i++){

				InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem)irLineItemVector.get(i);

				String lineTypeCategoryStr = irLineItem.getDottedFieldValue("LineType.Category").toString();
				int lineTypeCategoryInt = Integer.parseInt(lineTypeCategoryStr);

				if ( lineTypeCategoryInt != 2 ) {


				String iDGWrecordType = "ID";
				String iDGWFacility = "GW";

				// Accountings. SplitAccountings.Account. AccountCode


				SplitAccountingCollection sacol =(SplitAccountingCollection) irLineItem.getFieldValue("Accountings");
				if (sacol != null && sacol.getSplitAccountings() != null) {
						BaseVector sas = sacol.getSplitAccountings();
						for (int j =0; j <sas.size();j++){
			                   SplitAccounting splitAccounting  = (SplitAccounting)sas.get(j);
			if(splitAccounting != null)
			{
				String iDGWFormattedAccount ="";
				String iDGWAccount ="";
				//iDGWAccount = (String)irLineItem.getDottedFieldValue("Accountings.SplitAccountings.Account.AccountCode");
				iDGWAccount = (String)splitAccounting.getDottedFieldValue("Account.AccountCode");

				if (iDGWAccount != null) {
					Log.customer.debug("iDGWAccount => "+ iDGWAccount);
				iDGWFormattedAccount = getFormatettedTxt(iDGWAccount, 8);

				}
				else {
					iDGWAccount ="";
					Log.customer.debug("else ...iDGWAccount => "+ iDGWAccount);
					iDGWFormattedAccount = getFormatettedTxt(iDGWAccount, 8);
				}
				//LineItems. Accountings. SplitAccountings.SubAccount.UniqueName
				String iDGWSubaccount = "";
				String iDGWFormattedSubaccount = "";
				iDGWSubaccount = (String)splitAccounting.getDottedFieldValue("SubAccount.UniqueName");

				if (iDGWSubaccount != null) {
					Log.customer.debug("iDGWSubaccount => "+ iDGWSubaccount);
				iDGWFormattedSubaccount = getFormatettedTxt(iDGWSubaccount, 8);
				}
				else {
					iDGWSubaccount = "";
					Log.customer.debug(" else .. iDGWSubaccount => "+ iDGWSubaccount);
					iDGWFormattedSubaccount = getFormatettedTxt(iDGWSubaccount, 8);
				}

				//LineItems. Accountings. SplitAccountings.CostCenter.CostCenterCode
				String iDGWCostCenter = "";
				String iDGWFormattedCostCenter ="";
				iDGWCostCenter = (String)splitAccounting.getDottedFieldValue("CostCenter.CostCenterCode");
				if (iDGWCostCenter != null) {
				Log.customer.debug("iDGWCostCenter => "+ iDGWCostCenter);
				iDGWFormattedCostCenter = getFormatettedTxt(iDGWCostCenter, 4);
				}
				else {
					iDGWCostCenter = "";
					Log.customer.debug(" else ..iDGWCostCenter => "+ iDGWCostCenter);
					iDGWFormattedCostCenter = getFormatettedTxt(iDGWCostCenter, 4);
				}
                // LineItems. Accountings. SplitAccountings.Project.ProjectCode
				String iDGWProject = "";
				String iDGWFormattedProject ="";

				iDGWProject = (String)splitAccounting.getDottedFieldValue("Project.ProjectCode");
				if (iDGWProject != null) {
				Log.customer.debug("iDGWProject => "+ iDGWProject);
				iDGWFormattedProject = getFormatettedTxt(iDGWProject, 8);
				}
				else {
					iDGWProject = "";
					Log.customer.debug("else iDGWProject => "+ iDGWProject);
					iDGWFormattedProject = getFormatettedTxt(iDGWProject, 8);

				}
                // <BLANK> Blank out of MSC. For Peterborough  this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
				String iDGWTaxUsage = " ";

				String iDGWFormattedTaxUsage = getFormatettedTxt(iDGWTaxUsage,8);
				// For Peterborough this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
				String iDGWTaxClass = " ";
				String iDGWFormattedTaxClass= "";
				if (irLineItem.getFieldValue("VATClass") != null) {

				iDGWTaxClass = (String)irLineItem.getDottedFieldValue("VATClass.UniqueName");
				Log.customer.debug("iDGWTaxClass => "+ iDGWTaxClass);
				iDGWFormattedTaxClass = getFormatettedTxt(iDGWTaxClass,3);
				Log.customer.debug("iDGWFormattedTaxClass => "+ iDGWFormattedTaxClass);
				}
				else {
					Log.customer.debug("VAT Class is null  => "+ iDGWTaxClass );
					iDGWFormattedTaxClass = getFormatettedTxt(iDGWTaxClass,3);
					Log.customer.debug("iDGWFormattedTaxClass => "+ iDGWFormattedTaxClass);
				}


				//Line description
				/*
				String iDGWdescription = "";
				String iDGWFormattedDescription = "";
				iDGWdescription = (String)irLineItem.getDottedFieldValue("Description.Description");
				if (iDGWdescription != null) {
					Log.customer.debug("iDGWdescription => "+ iDGWdescription);
					iDGWFormattedDescription = getFormatettedTxt(iDGWdescription, 20);
					}
					else {
						iDGWdescription = "";
						Log.customer.debug("else iDGWdescription => "+ iDGWdescription);
						iDGWFormattedDescription = getFormatettedTxt(iDGWdescription, 20);
					}

				*/

				// Making description null. Passing 20 white spaces

				String iDGWFormattedDescription = "                    ";



				double iDGWamount = 0.0;
				String iDGWFormattedAmount ="";

				iDGWamount = splitAccounting.getAmount().getAmountAsDouble();
				Log.customer.debug("iDGWamount => "+ iDGWamount);
				iDGWFormattedAmount = getFormatedAmt(iDGWamount);
				/*
				if (iDGWamount > 0.0) {
					Log.customer.debug("iDGWamount => "+ iDGWamount);
					iDGWFormattedAmount = getFormatedAmt(iDGWamount);
					}
					else {
						Log.customer.debug("else iDGWamount => "+ iDGWamount);
						iDGWFormattedAmount = getFormatedAmt(iDGWamount);
					}
				*/
				// TAXABLE Y String for all
				String iDGWTaxableTxt = "Y";
				//String LINE TAX TOTAL
				double iDGWLineTaxTotal = irLineItem.getTaxAmount().getAmountAsDouble();
				Log.customer.debug("iDGWLineTaxTotal => "+ iDGWLineTaxTotal);
				iHGWTaxTotal += iDGWLineTaxTotal;
				Log.customer.debug("Added line tax to head tax iHGWTaxTotal => "+ iHGWTaxTotal);


				String iDGWFormattedLineTaxTotal = getFormatedAmt(iDGWLineTaxTotal);
				Log.customer.debug("iDGWFormattedLineTaxTotal => "+ iDGWFormattedLineTaxTotal);


				String iDGWRemark = "";
				String iDGWFormattedRemark ="";


				iDGWRemark = invrecon.getUniqueName();
				String iDGWSupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
				int invlen = iDGWSupInvNumber.length();
				iDGWFormattedRemark = getFormatattedTxt(iDGWRemark, invlen);
				Log.customer.debug("iDGWRemark => "+ iDGWRemark);


				Log.customer.debug("Calculated ..iHGWFormattedtaxToat => "+ iHGWTaxTotal);
				String iHGWFormattedtaxToat = getFormatedAmt(iHGWTaxTotal);
				Log.customer.debug("iHGWFormattedtaxToat => "+ iHGWFormattedtaxToat);

				iHGWFileData = iHGWRecordType + iHGWFacility
									+ iHGWFormattedVoControl + iHGWFomattedSupplierLocCode
									+ iHGWFormattedTaxDate + iHGWFormattedShipTo + iHGWCurrency
									+ iHGWFormattedSupInvNumber + iHGWFormattedInvoiceDate
									+ iHGWFormattedRemark + iHGWFormattedtaxToat;
				Log.customer.debug("iHGWFileData => "+ iHGWFileData);



				iDGWData = iDGWrecordType + iDGWFacility
						+ iDGWFormattedAccount + iDGWFormattedSubaccount
						+ iDGWFormattedCostCenter + iDGWFormattedProject
						+ iDGWFormattedTaxUsage + iDGWFormattedTaxClass
						+ iDGWFormattedDescription + iDGWFormattedAmount
						+ iDGWTaxableTxt+ iDGWFormattedLineTaxTotal+
						iDGWFormattedRemark;
				iDGWDataVector.add(iDGWData);

				Log.customer.debug("iDGWDataVector added => "+ iDGWData);



				}
						}
				}
				}

			}
			//iDGWDataVector
			}


				} //if(invrecon != null)
				boolean isPushedGW = false;
					try {

						outPW_FlatFile.write(iHGWFileData + "\n");
						Log.customer.debug("iHGWFileData has been writen to File => "+ iHGWFileData);
						for (int k=0;k<iDGWDataVector.size();k++)
						{
								  String  iDGWDataStr = (String) iDGWDataVector.elementAt(k);
							      Log.customer.debug("iDGWData has been writen to File => "+ iDGWData);
							      outPW_FlatFile.write(iDGWDataStr + "\n");
						}




						isPushedGW = true;
						invrecon.setFieldValue("ActionFlag", "Completed");
						iDGWDataVector.clear();
					} catch (Exception e1) {
						isPushedGW = false;
						Log.customer.debug("In Catch..." + (String)invrecon.getFieldValue("ActionFlag"));
						invrecon.setFieldValue("ActionFlag", "InProcess");
						Log.customer.debug("In Catch After resetting the ActionFlag..." + (String)invrecon.getFieldValue("ActionFlag"));
						Log.customer.debug(e1.toString());
						e1.printStackTrace();

					}
				if (isPushedGW)	{
				pushedCountGW++;
			}

			} // while






			}  // GW result set count if





			else {
				Log.customer.debug("GW resultset count 0, Nothing to push for GW");
			}







		} catch (Exception e) {
			Log.customer.debug(e);
//			add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed for GW: "+ pushedCountGW);
			message.append("\n");
			message.append("No of records queued for GW :"+ (resultCountGW - pushedCountGW));
			message.append("\n");

			message.append("CATMFGGWInvoiceFlatFilePush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATMFGGWInvoiceFlatFilePush Task Failed";
			Log.customer.debug("%s: CATMFGGWInvoiceFlatFilePush  Inside Exception message "+ message.toString());

					  throw new ScheduledTaskException( e.toString());
		} finally {
			if (outPW_FlatFile != null) {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();
				try {
					File f=new File(triggerFile);
					if(!f.exists()){
						 f.createNewFile();
						Log.customer.debug("triggerFile has been created "+ message.toString());
						  }
					 else {
						Log.customer.debug("triggerFile allready exit. "+ message.toString());
					 }
				} catch (IOException e1) {
					Log.customer.debug("triggerFile allready exit. "+ e1);
				}

			}
			Log.customer.debug("%s: CATMFGGWInvoiceFlatFilePush Inside Finally  ");
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			message.append("\n");
			endTime = ariba.util.formatter.DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("IRs count to be pushed GW: "+ resultCountGW);
			message.append("\n");
			message.append("No. of records GW ( IR details)successfully pushed : "+ pushedCountGW);
			message.append("\n");

			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "MFGIRFlatFilePushNotify");
			message = null;
			pushedCountGW =0;
			resultCountGW =0;

		}

	}



	// Util methods


	private String getFormattedTaxDate(Date date) {
		Log.customer.debug("date => "+ date);
		SimpleDateFormat formatter   = new SimpleDateFormat ("ddMMyy");
		String dateString = formatter.format(date);
		Log.customer.debug("dateString => "+ dateString);
		return dateString;
	}

	private String getFormatedAmt(double amount) {
		String amtPattern = "00000000000.000";
		DecimalFormat amtFormatter = new DecimalFormat(amtPattern);
		String formatedAmt = amtFormatter.format(amount);
		if (amount >= 0.0)
			formatedAmt = " " + formatedAmt;

		return formatedAmt;

	}

	private String addLeadingZerosToSC(String suppliercode) {
		int temp;
		temp = 8 - suppliercode.length();
		for (int i = 0; i < temp; i++) {
			suppliercode = "0" + suppliercode;
		}
		return suppliercode;
	}

	private String addSpacesToSLC(String supplierloccode) {
		int temp;
		temp = 8 - supplierloccode.length();
		for (int i = 0; i < temp; i++) {
			supplierloccode = supplierloccode + " ";
		}
		return supplierloccode;
	}

	private String getFormatettedTxt(String inputTxt, int txtLength) {
		int temp = txtLength - inputTxt.length();
		String formattedTxt = "";

		Log.customer.debug("int temp  " + temp);
		if (temp == 0) {
			return inputTxt;
		}

		if (temp > 0) {
			for (int i = 0; i < temp; i++) {
				inputTxt = inputTxt + " ";
			}
		} else {
			inputTxt = inputTxt.substring(0, txtLength);

		}
		formattedTxt = inputTxt;
		Log.customer.debug("formattedTxt " + formattedTxt);
		return formattedTxt;

	}

	private String getFormatattedTxt(String inputTxt, int txtLength) {
		int fulllength = inputTxt.length(); // full length gives the length of IRX number
		int rellength = txtLength + 4; //Inv Number + 'IRX' + '-'
		int temp = fulllength - rellength;
		String formattedTxt = "";

		Log.customer.debug("int temp  " + temp);

		inputTxt = inputTxt.substring(rellength, fulllength);

		int temp2 = 20 - temp;

		for (int i = 0; i < temp2; i++) {
				inputTxt = inputTxt + " ";
		}

		formattedTxt = inputTxt;
		Log.customer.debug("formattedTxt " + formattedTxt);
		return formattedTxt;

	}

   private String getFileExtDateTime(Date date){
   	        Date date1 = date;
	        Log.customer.debug("date1 => "+ date1);
			SimpleDateFormat formatter   = new SimpleDateFormat ("yyyyMMddhhmmss");
			String dateTimeString = formatter.format(date1);
			Log.customer.debug("dateTimeString for file => "+ dateTimeString);

			return dateTimeString;


}


}
