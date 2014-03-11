/***********************************************************************************************

	Creator: Kannan PGS
	Description: Performing push via flat file

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------
	5 Aug 2010 	Vikram		1. Provide the cost center code instead of cost center uniquename
							2. Provide supplier location code instead of supplier code
							3. Add white space at the end of supplier location code
							4. Provide 8 white spaces instead of ship-to
							5. Provide the number after '-' symbol rather than the full IRX#
							6. Provide ProjectCode instead of Project UniqueName
	31 May 11	Vikram		Make the description blank (white spaces)
	11 Jan 12	Vikram		Changes for GW facility

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


public class CATMFGInvoiceFlatFilePush extends ScheduledTask {

	private String thisclass = "CATMFGInvoiceFlatFilePush";

	private PrintWriter outPW_FlatFile = null;

	//String fileExtDateTime = getFileExtDateTime();
	//String flatFilePath = "/msc/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".txt";
	//String triggerFile =  "/msc/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".dstrigger";
	String fileExtDateTime ="";
	String flatFilePath ="";
	String triggerFile ="";

	AQLOptions options;
	AQLQuery aqlIRQueryDX,aqlTCSumQueryDX,aqlIRQueryNA,aqlTCSumQueryNA;
	AQLResultCollection irResultSetDX,tcSumResultSetDX,irResultSetNA,tcSumResultSetNA;
	int totalNumberOfIrsDX,totalNumberOfIrsNA;


	private FastStringBuffer message = null;
	private String mailSubject = null;
	private int resultCountDX, pushedCountDX,resultCountNA, pushedCountNA;
    private String startTime, endTime;
	Partition partition = Base.getService().getPartition("mfg1");

	private Vector iDDXDataVector = new Vector();
	private Vector iDNADataVector = new Vector();



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
			mailSubject = "CATMFGInvoiceFlatFilePush Task Completion Status - Completed Successfully";

			Date date = new Date();
			fileExtDateTime = getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".txt";
			triggerFile =  "/msc/arb9r1/downstream/catdata/INV/MSC_InvoicePush_MFGPro."+fileExtDateTime+".dstrigger";

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);
			File mfgIRFlatFile = new File(flatFilePath);
			options = new AQLOptions(partition);
			InvoiceReconciliation invrecon = null;


			if (!mfgIRFlatFile.exists()) {
				Log.customer.debug("File not exist");
				mfgIRFlatFile.createNewFile();
			}
			outPW_FlatFile = new PrintWriter(IOUtil.bufferedOutputStream(mfgIRFlatFile), true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);

			// For Facility DX BA Data
			// Below changes for GW facility
			//String iRQueryDX = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag  = 'DX'");
			String iRQueryDX = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag  = 'DX' and SiteFacility.UniqueName != 'GW'");
			Log.customer.debug("iRQueryDX ==> " +iRQueryDX);

			aqlIRQueryDX = AQLQuery.parseQuery(iRQueryDX);
			irResultSetDX = Base.getService().executeQuery(aqlIRQueryDX, options);

			if(irResultSetDX.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResultSetDX");

			totalNumberOfIrsDX = irResultSetDX.getSize();
			Log.customer.debug("totalNumberOfIrsDX ==> " +totalNumberOfIrsDX);
			resultCountDX = totalNumberOfIrsDX;


			if (totalNumberOfIrsDX >0){


			//String iRTCSumQueryDX = new String ("select sum(TotalCost.Amount ) from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'DX'");
			String iRTCSumQueryDX = new String ("select sum(TotalCost.Amount ) from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'DX' and SiteFacility.UniqueName != 'GW'");
			Log.customer.debug("totalNumberOfIrsDX ==> " +iRTCSumQueryDX);
			aqlTCSumQueryDX = AQLQuery.parseQuery(iRTCSumQueryDX);
			tcSumResultSetDX = Base.getService().executeQuery(aqlTCSumQueryDX, options);

			if(tcSumResultSetDX.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in tcSumResultSetDX");

			String recordTypeBA = "BA";
			String facilityDX = "DX";

			// DX sum of IR TotalCost

			double DXcontrol =0.0;

			while(tcSumResultSetDX.next()){
				DXcontrol = (double )(tcSumResultSetDX.getDouble(0));
				Log.customer.debug("DXcontrol => "+ DXcontrol);

			}

			String DXcontrolFormated = getFormatedAmt(DXcontrol);

			// Write BA info to file
			Log.customer.debug(recordTypeBA + facilityDX + DXcontrolFormated);
			String bAData = recordTypeBA + facilityDX + DXcontrolFormated;
			outPW_FlatFile.write(bAData + "\n");

			// For DX IH data

            String iHDXFileData ="";
			String iDDXData ="";
			while(irResultSetDX.next()){
				invrecon = (InvoiceReconciliation)(irResultSetDX.getBaseId("InvoiceReconciliation").get());

			if(invrecon != null){

			String iHDXRecordType = "IH";
			String iHDXFacility = "DX";

			// IR total cost
			//double iHDXVoControl = -1000.00;
			double iHDXVoControl = invrecon.getTotalCost().getAmountAsDouble();
			String iHDXFormattedVoControl = getFormatedAmt(iHDXVoControl);
			Log.customer.debug("iHDXFormattedVoControl => "+ iHDXFormattedVoControl);

			// Supplier Location Code

			//String iHDXSupplierCode = "80782";
			String iHDXSupplierLocCode = invrecon.getSupplierLocation().getUniqueName();
			String iHDXFomattedSupplierLocCode = addSpacesToSLC(iHDXSupplierLocCode);
			Log.customer.debug("iHDXFomattedSupplierLocCode => "+ iHDXFomattedSupplierLocCode);

			// Tax Date
			Date iHDXTaxDate = (Date)invrecon.getDottedFieldValue("Invoice.TimeCreated");
			String iHDXFormattedTaxDate = getFormattedTaxDate(iHDXTaxDate);
			Log.customer.debug("iHDXFormattedTaxDate => "+ iHDXFormattedTaxDate);

			/*// shipTo
			String iHDXFormattedShipTo ="";
			String iHDXShipTo ="";
			iHDXShipTo = (String)invrecon.getFieldValue("FacilityFlag");
			//String iHDXShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
			if (iHDXShipTo != null)
			iHDXFormattedShipTo = getFormatettedTxt(iHDXShipTo, 8);
			else
				iHDXFormattedShipTo = getFormatettedTxt(" ", 8);
			Log.customer.debug("iHDXFormattedShipTo => "+ iHDXFormattedShipTo);
			*/

			// shipTo
			String iHDXFormattedShipTo ="";
			String iHDXShipTo ="";
			//iHDXShipTo = (String)invrecon.getFieldValue("FacilityFlag");
			//String iHDXShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
			//if (iHDXShipTo != null)
			iHDXFormattedShipTo = getFormatettedTxt(iHDXShipTo, 8);
			//else
				//iHDXFormattedShipTo = getFormatettedTxt(" ", 8);
			Log.customer.debug("iHDXFormattedShipTo => "+ iHDXFormattedShipTo);

			String iHDXCurrency = (String) invrecon.getTotalCost().getCurrency().getUniqueName();
			Log.customer.debug("iHDXCurrency => "+ iHDXCurrency);

			String iHDXSupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
			String iHDXFormattedSupInvNumber = getFormatettedTxt(iHDXSupInvNumber, 20);
			Log.customer.debug("iHDXFormattedSupInvNumber => "+ iHDXFormattedSupInvNumber);

			Date iHDXInvoiceDate = (Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate");
			String iHDXFormattedInvoiceDate = getFormattedTaxDate(iHDXInvoiceDate);
			Log.customer.debug("iHDXFormattedInvoiceDate => "+ iHDXFormattedInvoiceDate);

			String iHDXRemark = invrecon.getUniqueName();
			int invleng = iHDXSupInvNumber.length();
			String iHDXFormattedRemark = getFormatattedTxt(iHDXRemark, invleng);
			Log.customer.debug("iHDXFormattedRemark => "+ iHDXFormattedRemark);

			//double iHDXTaxTotal = invrecon.getTotalTax().getAmountAsDouble();
			//String iHDXFormattedtaxToat = getFormatedAmt(iHDXTaxTotal);
			//Log.customer.debug("iHDXFormattedtaxToat => "+ iHDXFormattedtaxToat);





			// RECORD TYPE: INVOICE DETAIL
			BaseVector irLineItemVector=null;
			int irLineItemVectorSize = 0;
			irLineItemVector = (BaseVector)invrecon.getLineItems();
			irLineItemVectorSize = invrecon.getLineItemsCount();

			double iHDXTaxTotal = 0.00;


			if (irLineItemVectorSize > 0){


				for (int i =0; i<irLineItemVectorSize;i++){

				InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem)irLineItemVector.get(i);

				String lineTypeCategoryStr = irLineItem.getDottedFieldValue("LineType.Category").toString();
				int lineTypeCategoryInt = Integer.parseInt(lineTypeCategoryStr);

				if ( lineTypeCategoryInt != 2 ) {


				String iDDXrecordType = "ID";
				String iDDXFacility = "DX";

				// Accountings. SplitAccountings.Account. AccountCode


				SplitAccountingCollection sacol =(SplitAccountingCollection) irLineItem.getFieldValue("Accountings");
				if (sacol != null && sacol.getSplitAccountings() != null) {
						BaseVector sas = sacol.getSplitAccountings();
						for (int j =0; j <sas.size();j++){
			                   SplitAccounting splitAccounting  = (SplitAccounting)sas.get(j);
			if(splitAccounting != null)
			{
				String iDDXFormattedAccount ="";
				String iDDXAccount ="";
				//iDDXAccount = (String)irLineItem.getDottedFieldValue("Accountings.SplitAccountings.Account.AccountCode");
				iDDXAccount = (String)splitAccounting.getDottedFieldValue("Account.AccountCode");

				if (iDDXAccount != null) {
					Log.customer.debug("iDDXAccount => "+ iDDXAccount);
				iDDXFormattedAccount = getFormatettedTxt(iDDXAccount, 8);

				}
				else {
					iDDXAccount ="";
					Log.customer.debug("else ...iDDXAccount => "+ iDDXAccount);
					iDDXFormattedAccount = getFormatettedTxt(iDDXAccount, 8);
				}
				//LineItems. Accountings. SplitAccountings.SubAccount.UniqueName
				String iDDXSubaccount = "";
				String iDDXFormattedSubaccount = "";
				iDDXSubaccount = (String)splitAccounting.getDottedFieldValue("SubAccount.UniqueName");

				if (iDDXSubaccount != null) {
					Log.customer.debug("iDDXSubaccount => "+ iDDXSubaccount);
				iDDXFormattedSubaccount = getFormatettedTxt(iDDXSubaccount, 8);
				}
				else {
					iDDXSubaccount = "";
					Log.customer.debug(" else .. iDDXSubaccount => "+ iDDXSubaccount);
					iDDXFormattedSubaccount = getFormatettedTxt(iDDXSubaccount, 8);
				}

				//LineItems. Accountings. SplitAccountings.CostCenter.CostCenterCode
				String iDDXCostCenter = "";
				String iDDXFormattedCostCenter ="";
				iDDXCostCenter = (String)splitAccounting.getDottedFieldValue("CostCenter.CostCenterCode");
				if (iDDXCostCenter != null) {
				Log.customer.debug("iDDXCostCenter => "+ iDDXCostCenter);
				iDDXFormattedCostCenter = getFormatettedTxt(iDDXCostCenter, 4);
				}
				else {
					iDDXCostCenter = "";
					Log.customer.debug(" else ..iDDXCostCenter => "+ iDDXCostCenter);
					iDDXFormattedCostCenter = getFormatettedTxt(iDDXCostCenter, 4);
				}
                // LineItems. Accountings. SplitAccountings.Project.ProjectCode
				String iDDXProject = "";
				String iDDXFormattedProject ="";

				iDDXProject = (String)splitAccounting.getDottedFieldValue("Project.ProjectCode");
				if (iDDXProject != null) {
				Log.customer.debug("iDDXProject => "+ iDDXProject);
				iDDXFormattedProject = getFormatettedTxt(iDDXProject, 8);
				}
				else {
					iDDXProject = "";
					Log.customer.debug("else iDDXProject => "+ iDDXProject);
					iDDXFormattedProject = getFormatettedTxt(iDDXProject, 8);

				}
                // <BLANK> Blank out of MSC. For Peterborough  this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
				String iDDXTaxUsage = " ";

				String iDDXFormattedTaxUsage = getFormatettedTxt(iDDXTaxUsage,8);
				// For Peterborough this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
				String iDDXTaxClass = " ";
				String iDDXFormattedTaxClass= "";
				if (irLineItem.getFieldValue("VATClass") != null) {

				iDDXTaxClass = (String)irLineItem.getDottedFieldValue("VATClass.UniqueName");
				Log.customer.debug("iDDXTaxClass => "+ iDDXTaxClass);
				iDDXFormattedTaxClass = getFormatettedTxt(iDDXTaxClass,3);
				Log.customer.debug("iDDXFormattedTaxClass => "+ iDDXFormattedTaxClass);
				}
				else {
					Log.customer.debug("VAT Class is null  => "+ iDDXTaxClass );
					iDDXFormattedTaxClass = getFormatettedTxt(iDDXTaxClass,3);
					Log.customer.debug("iDDXFormattedTaxClass => "+ iDDXFormattedTaxClass);
				}


				//Line description
				/*
				String iDDXdescription = "";
				String iDDXFormattedDescription = "";
				iDDXdescription = (String)irLineItem.getDottedFieldValue("Description.Description");
				if (iDDXdescription != null) {
					Log.customer.debug("iDDXdescription => "+ iDDXdescription);
					iDDXFormattedDescription = getFormatettedTxt(iDDXdescription, 20);
					}
					else {
						iDDXdescription = "";
						Log.customer.debug("else iDDXdescription => "+ iDDXdescription);
						iDDXFormattedDescription = getFormatettedTxt(iDDXdescription, 20);
					}

				*/

				// Making description null. Passing 20 white spaces

				String iDDXFormattedDescription = "                    ";



				double iDDXamount = 0.0;
				String iDDXFormattedAmount ="";

				iDDXamount = splitAccounting.getAmount().getAmountAsDouble();
				Log.customer.debug("iDDXamount => "+ iDDXamount);
				iDDXFormattedAmount = getFormatedAmt(iDDXamount);
				/*
				if (iDDXamount > 0.0) {
					Log.customer.debug("iDDXamount => "+ iDDXamount);
					iDDXFormattedAmount = getFormatedAmt(iDDXamount);
					}
					else {
						Log.customer.debug("else iDDXamount => "+ iDDXamount);
						iDDXFormattedAmount = getFormatedAmt(iDDXamount);
					}
				*/
				// TAXABLE Y String for all
				String iDDXTaxableTxt = "Y";
				//String LINE TAX TOTAL
				double iDDXLineTaxTotal = irLineItem.getTaxAmount().getAmountAsDouble();
				Log.customer.debug("iDDXLineTaxTotal => "+ iDDXLineTaxTotal);
				iHDXTaxTotal += iDDXLineTaxTotal;
				Log.customer.debug("Added line tax to head tax iHDXTaxTotal => "+ iHDXTaxTotal);


				String iDDXFormattedLineTaxTotal = getFormatedAmt(iDDXLineTaxTotal);
				Log.customer.debug("iDDXFormattedLineTaxTotal => "+ iDDXFormattedLineTaxTotal);


				String iDDXRemark = "";
				String iDDXFormattedRemark ="";


				iDDXRemark = invrecon.getUniqueName();
				String iDDXSupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
				int invlen = iDDXSupInvNumber.length();
				iDDXFormattedRemark = getFormatattedTxt(iDDXRemark, invlen);
				Log.customer.debug("iDDXRemark => "+ iDDXRemark);


				Log.customer.debug("Calculated ..iHDXFormattedtaxToat => "+ iHDXTaxTotal);
				String iHDXFormattedtaxToat = getFormatedAmt(iHDXTaxTotal);
				Log.customer.debug("iHDXFormattedtaxToat => "+ iHDXFormattedtaxToat);

				iHDXFileData = iHDXRecordType + iHDXFacility
									+ iHDXFormattedVoControl + iHDXFomattedSupplierLocCode
									+ iHDXFormattedTaxDate + iHDXFormattedShipTo + iHDXCurrency
									+ iHDXFormattedSupInvNumber + iHDXFormattedInvoiceDate
									+ iHDXFormattedRemark + iHDXFormattedtaxToat;
				Log.customer.debug("iHDXFileData => "+ iHDXFileData);



				iDDXData = iDDXrecordType + iDDXFacility
						+ iDDXFormattedAccount + iDDXFormattedSubaccount
						+ iDDXFormattedCostCenter + iDDXFormattedProject
						+ iDDXFormattedTaxUsage + iDDXFormattedTaxClass
						+ iDDXFormattedDescription + iDDXFormattedAmount
						+ iDDXTaxableTxt+ iDDXFormattedLineTaxTotal+
						iDDXFormattedRemark;
				iDDXDataVector.add(iDDXData);

				Log.customer.debug("iDDXDataVector added => "+ iDDXData);



				}
						}
				}
				}

			}
			//iDDXDataVector
			}


				} //if(invrecon != null)
				boolean isPushedDX = false;
					try {

						outPW_FlatFile.write(iHDXFileData + "\n");
						Log.customer.debug("iHDXFileData has been writen to File => "+ iHDXFileData);
						for (int k=0;k<iDDXDataVector.size();k++)
						{
								  String  iDDXDataStr = (String) iDDXDataVector.elementAt(k);
							      Log.customer.debug("iDDXData has been writen to File => "+ iDDXData);
							      outPW_FlatFile.write(iDDXDataStr + "\n");
						}




						isPushedDX = true;
						invrecon.setFieldValue("ActionFlag", "Completed");
						iDDXDataVector.clear();
					} catch (Exception e1) {
						isPushedDX = false;
						Log.customer.debug("In Catch..." + (String)invrecon.getFieldValue("ActionFlag"));
						invrecon.setFieldValue("ActionFlag", "InProcess");
						Log.customer.debug("In Catch After resetting the ActionFlag..." + (String)invrecon.getFieldValue("ActionFlag"));
						Log.customer.debug(e1.toString());
						e1.printStackTrace();

					}
				if (isPushedDX)	{
				pushedCountDX++;
			}

			} // while






			}  // DX result set count if


           // For Facility NA BA Data
					
					//Below changes for GW facility
		   			//String iRQueryNA = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag  = 'NA'");
					String iRQueryNA = new String ("select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag  = 'NA' and SiteFacility.UniqueName != 'GW'");
		   			Log.customer.debug("iRQueryNA ==> " +iRQueryNA);

		   			aqlIRQueryNA = AQLQuery.parseQuery(iRQueryNA);
		   			irResultSetNA = Base.getService().executeQuery(aqlIRQueryNA, options);

		   			if(irResultSetNA.getErrors() != null)
		   				Log.customer.debug("ERROR GETTING RESULTS in irResultSetNA");

		   			totalNumberOfIrsNA = irResultSetNA.getSize();
		   			Log.customer.debug("totalNumberOfIrsNA ==> " +totalNumberOfIrsNA);
		   			resultCountNA = totalNumberOfIrsNA;


		   			if (totalNumberOfIrsNA >0){


		   			//String iRTCSumQueryNA = new String ("select sum(TotalCost.Amount ) from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'NA'");
					String iRTCSumQueryNA = new String ("select sum(TotalCost.Amount ) from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess' and FacilityFlag = 'NA' and SiteFacility.UniqueName != 'GW'");
		   			Log.customer.debug("totalNumberOfIrsNA ==> " +iRTCSumQueryNA);
		   			aqlTCSumQueryNA = AQLQuery.parseQuery(iRTCSumQueryNA);
		   			tcSumResultSetNA = Base.getService().executeQuery(aqlTCSumQueryNA, options);

		   			if(tcSumResultSetNA.getErrors() != null)
		   				Log.customer.debug("ERROR GETTING RESULTS in tcSumResultSetNA");

		   			String recordTypeBA = "BA";
		   			String facilityNA = "NA";

		   			// NA sum of IR TotalCost

		   			double NAcontrol =0.0;

		   			while(tcSumResultSetNA.next()){
		   				NAcontrol = (double )(tcSumResultSetNA.getDouble(0));
		   				Log.customer.debug("NAcontrol => "+ NAcontrol);

		   			}

		   			String NAcontrolFormated = getFormatedAmt(NAcontrol);

		   			// Write BA info to file
		   			Log.customer.debug(recordTypeBA + facilityNA + NAcontrolFormated);
		   			String bAData = recordTypeBA + facilityNA + NAcontrolFormated;
		   			outPW_FlatFile.write(bAData + "\n");

		   			// For NA IH data

		               String iHNAFileData ="";
		   			String iDNAData ="";
		   			while(irResultSetNA.next()){
		   				invrecon = (InvoiceReconciliation)(irResultSetNA.getBaseId("InvoiceReconciliation").get());

		   			if(invrecon != null){

		   			String iHNARecordType = "IH";
		   			String iHNAFacility = "NA";

		   			// IR total cost
		   			//double iHNAVoControl = -1000.00;
		   			double iHNAVoControl = invrecon.getTotalCost().getAmountAsDouble();
		   			String iHNAFormattedVoControl = getFormatedAmt(iHNAVoControl);
		   			Log.customer.debug("iHNAFormattedVoControl => "+ iHNAFormattedVoControl);

		   			// Supplier Location Code

		   			//String iHNASupplierCode = "80782";
		   			String iHNASupplierLocCode = invrecon.getSupplierLocation().getUniqueName();
		   			String iHNAFomattedSupplierLocCode = addSpacesToSLC(iHNASupplierLocCode);
		   			Log.customer.debug("iHNAFomattedSupplierLocCode => "+ iHNAFomattedSupplierLocCode);

		   			// Tax Date
		   			Date iHNATaxDate = (Date)invrecon.getDottedFieldValue("Invoice.TimeCreated");
		   			String iHNAFormattedTaxDate = getFormattedTaxDate(iHNATaxDate);
		   			Log.customer.debug("iHNAFormattedTaxDate => "+ iHNAFormattedTaxDate);

		   			/*// shipTo
		   			String iHNAFormattedShipTo ="";
		   			String iHNAShipTo ="";
		   			iHNAShipTo = (String)invrecon.getFieldValue("FacilityFlag");
		   			//String iHNAShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
		   			if (iHNAShipTo != null)
		   			iHNAFormattedShipTo = getFormatettedTxt(iHNAShipTo, 8);
		   			else
		   				iHNAFormattedShipTo = getFormatettedTxt(" ", 8);
		   			Log.customer.debug("iHNAFormattedShipTo => "+ iHNAFormattedShipTo);
		   			*/

		   			// shipTo
		   			String iHNAFormattedShipTo ="";
		   			String iHNAShipTo ="";
		   			//iHNAShipTo = (String)invrecon.getFieldValue("FacilityFlag");
		   			//String iHNAShipTo = (String)invrecon.getDottedFieldValue("Invoice.SiteFacility.UniqueName");
		   			//if (iHNAShipTo != null)
		   			iHNAFormattedShipTo = getFormatettedTxt(iHNAShipTo, 8);
		   			//else
		   				//iHNAFormattedShipTo = getFormatettedTxt(" ", 8);
		   			Log.customer.debug("iHNAFormattedShipTo => "+ iHNAFormattedShipTo);

		   			String iHNACurrency = (String) invrecon.getTotalCost().getCurrency().getUniqueName();
		   			Log.customer.debug("iHNACurrency => "+ iHNACurrency);

		   			String iHNASupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
		   			String iHNAFormattedSupInvNumber = getFormatettedTxt(iHNASupInvNumber, 20);
		   			Log.customer.debug("iHNAFormattedSupInvNumber => "+ iHNAFormattedSupInvNumber);

		   			Date iHNAInvoiceDate = (Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate");
		   			String iHNAFormattedInvoiceDate = getFormattedTaxDate(iHNAInvoiceDate);
		   			Log.customer.debug("iHNAFormattedInvoiceDate ===> "+ iHNAFormattedInvoiceDate);

		   			String iHNARemark = invrecon.getUniqueName();
		   			int invleng = iHNASupInvNumber.length();
		   			String iHNAFormattedRemark = getFormatattedTxt(iHNARemark, invleng);
		   			Log.customer.debug("iHNAFormattedRemark => "+ iHNAFormattedRemark);

		   			//double iHNATaxTotal = invrecon.getTotalTax().getAmountAsDouble();
		   			//String iHNAFormattedtaxToat = getFormatedAmt(iHNATaxTotal);
		   			//Log.customer.debug("iHNAFormattedtaxToat => "+ iHNAFormattedtaxToat);





		   			// RECORD TYPE: INVOICE DETAIL
		   			BaseVector irLineItemVector=null;
		   			int irLineItemVectorSize = 0;
		   			irLineItemVector = (BaseVector)invrecon.getLineItems();
		   			irLineItemVectorSize = invrecon.getLineItemsCount();

		   			double iHNATaxTotal = 0.00;


		   			if (irLineItemVectorSize > 0){


		   				for (int i =0; i<irLineItemVectorSize;i++){

		   				InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem)irLineItemVector.get(i);

		   				String lineTypeCategoryStr = irLineItem.getDottedFieldValue("LineType.Category").toString();
		   				int lineTypeCategoryInt = Integer.parseInt(lineTypeCategoryStr);

		   				if ( lineTypeCategoryInt != 2 ) {


		   				String iDNArecordType = "ID";
		   				String iDNAFacility = "NA";

		   				// Accountings. SplitAccountings.Account. AccountCode


		   				SplitAccountingCollection sacol =(SplitAccountingCollection) irLineItem.getFieldValue("Accountings");
		   				if (sacol != null && sacol.getSplitAccountings() != null) {
		   						BaseVector sas = sacol.getSplitAccountings();
		   						for (int j =0; j <sas.size();j++){
		   			                   SplitAccounting splitAccounting  = (SplitAccounting)sas.get(j);
		   			if(splitAccounting != null)
		   			{
		   				String iDNAFormattedAccount ="";
		   				String iDNAAccount ="";
		   				//iDNAAccount = (String)irLineItem.getDottedFieldValue("Accountings.SplitAccountings.Account.AccountCode");
		   				iDNAAccount = (String)splitAccounting.getDottedFieldValue("Account.AccountCode");

		   				if (iDNAAccount != null) {
		   					Log.customer.debug("iDNAAccount => "+ iDNAAccount);
		   				iDNAFormattedAccount = getFormatettedTxt(iDNAAccount, 8);

		   				}
		   				else {
		   					iDNAAccount ="";
		   					Log.customer.debug("else ...iDNAAccount => "+ iDNAAccount);
		   					iDNAFormattedAccount = getFormatettedTxt(iDNAAccount, 8);
		   				}
		   				//LineItems. Accountings. SplitAccountings.SubAccount.UniqueName
		   				String iDNASubaccount = "";
		   				String iDNAFormattedSubaccount = "";
		   				iDNASubaccount = (String)splitAccounting.getDottedFieldValue("SubAccount.UniqueName");

		   				if (iDNASubaccount != null) {
		   					Log.customer.debug("iDNASubaccount => "+ iDNASubaccount);
		   				iDNAFormattedSubaccount = getFormatettedTxt(iDNASubaccount, 8);
		   				}
		   				else {
		   					iDNASubaccount = "";
		   					Log.customer.debug(" else .. iDNASubaccount => "+ iDNASubaccount);
		   					iDNAFormattedSubaccount = getFormatettedTxt(iDNASubaccount, 8);
		   				}

		   				//LineItems. Accountings. SplitAccountings.CostCenter.CostCenterCode
		   				String iDNACostCenter = "";
		   				String iDNAFormattedCostCenter ="";
		   				iDNACostCenter = (String)splitAccounting.getDottedFieldValue("CostCenter.CostCenterCode");
		   				if (iDNACostCenter != null) {
		   				Log.customer.debug("iDNACostCenter => "+ iDNACostCenter);
		   				iDNAFormattedCostCenter = getFormatettedTxt(iDNACostCenter, 4);
		   				}
		   				else {
		   					iDNACostCenter = "";
		   					Log.customer.debug(" else ..iDNACostCenter => "+ iDNACostCenter);
		   					iDNAFormattedCostCenter = getFormatettedTxt(iDNACostCenter, 4);
		   				}
		                   // LineItems. Accountings. SplitAccountings.Project.ProjectCode
		   				String iDNAProject = "";
		   				String iDNAFormattedProject ="";

		   				iDNAProject = (String)splitAccounting.getDottedFieldValue("Project.ProjectCode");
		   				if (iDNAProject != null) {
		   				Log.customer.debug("iDNAProject => "+ iDNAProject);
		   				iDNAFormattedProject = getFormatettedTxt(iDNAProject, 8);
		   				}
		   				else {
		   					iDNAProject = "";
		   					Log.customer.debug("else iDNAProject => "+ iDNAProject);
		   					iDNAFormattedProject = getFormatettedTxt(iDNAProject, 8);

		   				}
		                   // <BLANK> Blank out of MSC. For Peterborough  this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
		   				String iDNATaxUsage = " ";

		   				String iDNAFormattedTaxUsage = getFormatettedTxt(iDNATaxUsage,8);
		   				// For Peterborough this maps to Tax Class; for Shibaura it maps to usage - for the ICC to add
		   				String iDNATaxClass = " ";
		   				String iDNAFormattedTaxClass= "";
		   				if (irLineItem.getFieldValue("VATClass") != null) {

		   				iDNATaxClass = (String)irLineItem.getDottedFieldValue("VATClass.UniqueName");
		   				Log.customer.debug("iDNATaxClass => "+ iDNATaxClass);
		   				iDNAFormattedTaxClass = getFormatettedTxt(iDNATaxClass,3);
		   				Log.customer.debug("iDNAFormattedTaxClass => "+ iDNAFormattedTaxClass);
		   				}
		   				else {
		   					Log.customer.debug("VAT Class is null  => "+ iDNATaxClass );
		   					iDNAFormattedTaxClass = getFormatettedTxt(iDNATaxClass,3);
		   					Log.customer.debug("iDNAFormattedTaxClass => "+ iDNAFormattedTaxClass);
		   				}


		   				//Line description

						/*
		   				String iDNAdescription = "";
		   				String iDNAFormattedDescription = "";
		   				iDNAdescription = (String)irLineItem.getDottedFieldValue("Description.Description");
		   				if (iDNAdescription != null) {
		   					Log.customer.debug("iDNAdescription => "+ iDNAdescription);
		   					iDNAFormattedDescription = getFormatettedTxt(iDNAdescription, 20);
		   					}
		   					else {
		   						iDNAdescription = "";
		   						Log.customer.debug("else iDNAdescription => "+ iDNAdescription);
		   						iDNAFormattedDescription = getFormatettedTxt(iDNAdescription, 20);
		   					}

						*/

						// Making description null. Passing 20 white spaces

						String iDNAFormattedDescription = "                    ";



		   				double iDNAamount = 0.0;
		   				String iDNAFormattedAmount ="";

		   				iDNAamount = splitAccounting.getAmount().getAmountAsDouble();
		   				Log.customer.debug("iDNAamount => "+ iDNAamount);
		   				iDNAFormattedAmount = getFormatedAmt(iDNAamount);
		   				/*
		   				if (iDNAamount > 0.0) {
		   					Log.customer.debug("iDNAamount => "+ iDNAamount);
		   					iDNAFormattedAmount = getFormatedAmt(iDNAamount);
		   					}
		   					else {
		   						Log.customer.debug("else iDNAamount => "+ iDNAamount);
		   						iDNAFormattedAmount = getFormatedAmt(iDNAamount);
		   					}
		   				*/
		   				// TAXABLE Y String for all
		   				String iDNATaxableTxt = "Y";
		   				//String LINE TAX TOTAL
		   				double iDNALineTaxTotal = irLineItem.getTaxAmount().getAmountAsDouble();
		   				Log.customer.debug("iDNALineTaxTotal => "+ iDNALineTaxTotal);
		   				iHNATaxTotal += iDNALineTaxTotal;
		   				Log.customer.debug("Added line tax to head tax iHNATaxTotal => "+ iHNATaxTotal);


		   				String iDNAFormattedLineTaxTotal = getFormatedAmt(iDNALineTaxTotal);
		   				Log.customer.debug("iDNAFormattedLineTaxTotal => "+ iDNAFormattedLineTaxTotal);


		   				String iDNARemark = "";
		   				String iDNAFormattedRemark ="";


		   				iDNARemark = invrecon.getUniqueName();
		   				String iDNASupInvNumber = (String)invrecon.getDottedFieldValue("Invoice.InvoiceNumber");
		   				int invlen = iDNASupInvNumber.length();
		   				iDNAFormattedRemark = getFormatattedTxt(iDNARemark, invlen);
		   				Log.customer.debug("iDNARemark => "+ iDNARemark);


		   				Log.customer.debug("Calculated ..iHNAFormattedtaxToat => "+ iHNATaxTotal);
		   				String iHNAFormattedtaxToat = getFormatedAmt(iHNATaxTotal);
		   				Log.customer.debug("iHNAFormattedtaxToat => "+ iHNAFormattedtaxToat);

		   				iHNAFileData = iHNARecordType + iHNAFacility
		   									+ iHNAFormattedVoControl + iHNAFomattedSupplierLocCode
		   									+ iHNAFormattedTaxDate + iHNAFormattedShipTo + iHNACurrency
		   									+ iHNAFormattedSupInvNumber + iHNAFormattedInvoiceDate
		   									+ iHNAFormattedRemark + iHNAFormattedtaxToat;
		   				Log.customer.debug("iHNAFileData => "+ iHNAFileData);



		   				iDNAData = iDNArecordType + iDNAFacility
		   						+ iDNAFormattedAccount + iDNAFormattedSubaccount
		   						+ iDNAFormattedCostCenter + iDNAFormattedProject
		   						+ iDNAFormattedTaxUsage + iDNAFormattedTaxClass
		   						+ iDNAFormattedDescription + iDNAFormattedAmount
		   						+ iDNATaxableTxt+ iDNAFormattedLineTaxTotal+
		   						iDNAFormattedRemark;
		   				iDNADataVector.add(iDNAData);

		   				Log.customer.debug("iDNADataVector added => "+ iDNAData);



		   				}
		   						}
		   				}
		   				}

		   			}
		   			//iDNADataVector
		   			}


		   				} //if(invrecon != null)
		   				boolean isPushedNA = false;
		   					try {

		   						outPW_FlatFile.write(iHNAFileData + "\n");
		   						Log.customer.debug("iHNAFileData has been writen to File => "+ iHNAFileData);
		   						for (int k=0;k<iDNADataVector.size();k++)
		   						{
		   								  String  iDNADataStr = (String) iDNADataVector.elementAt(k);
		   							      Log.customer.debug("iDNAData has been writen to File => "+ iDNAData);
		   							      outPW_FlatFile.write(iDNADataStr + "\n");
		   						}




		   						isPushedNA = true;
		   						invrecon.setFieldValue("ActionFlag", "Completed");
		   						iDNADataVector.clear();
		   					} catch (Exception e1) {
		   						isPushedNA = false;
		   						Log.customer.debug("In Catch..." + (String)invrecon.getFieldValue("ActionFlag"));
		   						invrecon.setFieldValue("ActionFlag", "InProcess");
		   						Log.customer.debug("In Catch After resetting the ActionFlag..." + (String)invrecon.getFieldValue("ActionFlag"));
		   						Log.customer.debug(e1.toString());
								e1.printStackTrace();


		   					}
		   				if (isPushedNA)	{
		   				pushedCountNA++;
		   			}

		   			} // while






			}  // NA result set count if





			else {
				Log.customer.debug("DX and NA resultset cout 0, Nothing to push for DX and NA");
			}







		} catch (Exception e) {
			Log.customer.debug(e);
//			add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed for DX: "+ pushedCountDX);
			message.append("\n");
			message.append("No of records queued for DX :"+ (resultCountDX - pushedCountDX));
			message.append("\n");
			message.append("No of records pushed for NA: "+ pushedCountNA);
			message.append("\n");
			message.append("No of records queued for NA :"+ (resultCountNA - pushedCountNA));
			message.append("\n");

			message.append("CATMFGInvoiceFlatFilePush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATMFGInvoiceFlatFilePush Task Failed";
			Log.customer.debug("%s: CATMFGInvoiceFlatFilePush  Inside Exception message "+ message.toString());

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
			Log.customer.debug("%s: CATMFGInvoiceFlatFilePush Inside Finally  ");
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			message.append("\n");
			endTime = ariba.util.formatter.DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("IRs count to be pushed DX: "+ resultCountDX);
			message.append("\n");
			message.append("No. of records DX ( IR details) successfully pushed : "+ pushedCountDX);
			message.append("\n");
			message.append("\n");
			message.append("IRs count to be pushed NA: "+ resultCountNA);
			message.append("\n");
			message.append("No. of records NA ( IR details)successfully pushed : "+ pushedCountNA);
			message.append("\n");

			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "MFGIRFlatFilePushNotify");
			message = null;
			pushedCountDX =0;
			resultCountDX =0;
			pushedCountNA =0;
			resultCountNA =0;

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
