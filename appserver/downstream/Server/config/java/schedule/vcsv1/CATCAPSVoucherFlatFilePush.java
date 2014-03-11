/********************************************************************************************

Changed by : Dharshan
199  IBM AMS_Dharshan  Voucher eforms not getting pushed to CAPS due to Currency issue.


 ********************************************************************************************/


package config.java.schedule.vcsv1;



import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.TimeZone;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccountingCollection;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;
import config.java.schedule.util.CATFaltFileUtil;


public class CATCAPSVoucherFlatFilePush extends ScheduledTask {

	private String thisclass = "CATCAPSVoucherFlatFilePush";
	private Date datetimezone;
	private String startTime, endTime;
	private FastStringBuffer message = null;
	private String mailSubject = null;
	private String fileExtDateTime = "";
	public String flatFilePath,controlFlatFilePath;
	//change made by soumya begings
	private String archiveFileDataPath;
	private String archiveFileCtrlPath;
	//change made by soumya ends
	private String triggerFile = "";
	private PrintWriter outPW_FlatFile,outCtrl_FlatFile;
	private AQLOptions options,options1;
	private Partition partition = null;
	private AQLResultCollection voResultSet,ctrlResultSet;
	private int totalNumberOfPOs;
	private AQLQuery aqlVOQuery,aqlctrlQuery;
	private SplitAccountingCollection poAccounting = null;
	private int  iSpAcct;
	private int resultCount, pushedCount;


	private java.math.BigDecimal bdTotCost;
	ClusterRoot vobj = null;
	public void run() throws ScheduledTaskException {

		try{
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject ="CATCAPSVoucherFlatFilePush Task Completion Status - Completed Successfully";
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_CAPS_VOUCHER_PUSH."+ fileExtDateTime + ".txt";
			controlFlatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_CAPS_VOUCHER_CTRL."+ fileExtDateTime + ".txt";
			triggerFile = "/msc/arb9r1/downstream/catdata/INV/MSC_CAPS_VOUCHER_PUSH."+ fileExtDateTime + ".dstrigger";
			//Change made by soumya begins
			archiveFileDataPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_CAPS_VOUCHER_PUSH_ARCHIVE." + fileExtDateTime + ".txt";
			archiveFileCtrlPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_CAPS_VOUCHER_CTRL_ARCHIVE." + fileExtDateTime + ".txt";
			//Change made by soumya ends

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("controlFlatFilePath " + controlFlatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);
			//Change made by soumya begins
			Log.customer.debug("triggerFile " + archiveFileDataPath);
			Log.customer.debug("triggerFile " + archiveFileCtrlPath);
			//Change made by soumya ends
			File capsVoucherFlatFile = new File(flatFilePath);
			File capsCtrlFlatFile = new File(controlFlatFilePath);

			options = new AQLOptions(partition,true);

			if (!capsVoucherFlatFile.exists()) {
				Log.customer.debug("File not exist creating file ..");
				capsVoucherFlatFile.createNewFile();
			}
			if (!capsCtrlFlatFile.exists()) {
				Log.customer.debug("File not exist creating file ..");
				capsCtrlFlatFile.createNewFile();
			}





			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(capsVoucherFlatFile),true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);

			outCtrl_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(capsCtrlFlatFile),true);
			Log.customer.debug("outCtrl_FlatFile " + outCtrl_FlatFile);



			String vOQuery = new String( "select from config.java.vcsv1.vouchereform.VoucherEform where StatusString = 'Approved' and ActionFlag IS NULL");
			Log.customer.debug("vOQuery ==> " + vOQuery);
			aqlVOQuery = AQLQuery.parseQuery(vOQuery);
			voResultSet = Base.getService().executeQuery(aqlVOQuery, options);

			if (voResultSet.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResultSetUSCliDB");

			totalNumberOfPOs = voResultSet.getSize();
			resultCount = totalNumberOfPOs;
			Log.customer.debug("totalNumberOfs ==> " + totalNumberOfPOs);
                        int commitCount = 0;

			while(voResultSet.next()){

				vobj = (ClusterRoot)voResultSet.getBaseId("VoucherEform").get();
				if(vobj != null){

                                 Log.customer.debug("Setting the Voucher Supplier to Completed");
                                //Commented out by Sandeep to be moved to Production.
                                    vobj.setFieldValue("ActionFlag", "Completed");


					//VoucherSupplier.UniqueName
					String voucherSupplierUniqueNameFmt ="";
					String voucherSupplierUniqueName = vobj.getDottedFieldValue("VoucherSupplier.UniqueName").toString();
					Log.customer.debug("VoucherSupplier.UniqueName ==> " +voucherSupplierUniqueName);
					voucherSupplierUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(voucherSupplierUniqueName,10);
					Log.customer.debug("voucherSupplierUniqueNameFmt ==> " +voucherSupplierUniqueNameFmt);


					//TimeCreated	10
					String timeCreatedFmt ="";
					Date timeCreated = (Date)vobj.getFieldValue("TimeCreated");
					Log.customer.debug("timeCreated ==> " +timeCreated);
					timeCreatedFmt = CATFaltFileUtil.getFormattedDate(timeCreated);
					Log.customer.debug("timeCreatedFmt ==> " +timeCreatedFmt);

					//TotalInvoiced.Amount	17
					Money totalInvoicedAmountMoney  = (Money)vobj.getFieldValue("TotalInvoiced");
					String totalInvoicedAmountFmt ="";
					double totalInvoicedAmountDouble = totalInvoicedAmountMoney.getAmountAsDouble();
					Log.customer.debug("totalInvoicedAmountDouble ==> " +totalInvoicedAmountDouble);
					totalInvoicedAmountFmt = CATFaltFileUtil.getFormattedNumber(totalInvoicedAmountDouble,"0000000000000.00");
					Log.customer.debug("totalInvoicedAmountFmt ==> " +totalInvoicedAmountFmt);

                                   //Start 199 - changed ApproximateAmountInBaseCurrency to Amount
					bdTotCost = (java.math.BigDecimal)vobj.getDottedFieldValue("TotalInvoiced.Amount");
                                   Log.customer.debug("bdTotCost ==> " +bdTotCost);
                                   //End 199

					//TotalInvoiced.Currency.UniqueName	3
					String currencyUniqueNameFmt ="";
					Currency currency =  totalInvoicedAmountMoney.getCurrency();
					String currencyUniqueName = currency.getUniqueName();
					currencyUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(currencyUniqueName,3);


					//InvoiceDate	10
					String invoiceDateFmt ="";
					Date invoiceDate = (Date)vobj.getFieldValue("InvoiceDate");
					Log.customer.debug("invoiceDate ==> " +invoiceDate);
					invoiceDateFmt = CATFaltFileUtil.getFormattedDate(invoiceDate);
					Log.customer.debug("invoiceDateFmt ==> " +invoiceDateFmt);

					//InvoiceNumber	24
					String invoiceNumberFmt ="";
					//String invoiceNumber = vobj.getFieldValue("InvoiceNumber").toString();
					//Log.customer.debug("invoiceNumber ==> " +invoiceNumber);
					//invoiceNumberFmt = CATFaltFileUtil.getFormattedTxt(invoiceNumber,24);
					//Log.customer.debug("invoiceNumberFmt ==> " +invoiceNumberFmt);
					String controlid ="";
					if (vobj.getFieldValue("InvoiceNumber") != null) {
						String invunique = (java.lang.String)vobj.getFieldValue("InvoiceNumber");
						Log.customer.debug("%s InvoiceNumber Is..." + invunique);

						if (invunique.length() > 24) {
							//invunique = "pcsv1-" + invunique.substring(invunique.length() - 18);
							invunique = invunique.substring(0,24);
						}
						//else invunique = "pcsv1-" + invunique;
						else
							invunique = invunique;

						Log.customer.debug("%s Last 18 Chars of the IR Is..." + invunique);

						invoiceNumberFmt = CATFaltFileUtil.getFormattedTxt(invunique,24);
						Log.customer.debug("invoiceNumberFmt ==> " +invoiceNumberFmt);

						invunique = getDateTime(datetimezone) + invunique;
						controlid = new String (invunique);
						Log.customer.debug("%s ControlIdentifier IS..." + controlid);
						vobj.setFieldValue("ControlIdentifier", controlid);
					}

					//BatchNum	3
					String batchNumFmt = " 82";

					//TimeCreated	10
					timeCreatedFmt = CATFaltFileUtil.getFormattedDate(timeCreated);
					Log.customer.debug("timeCreatedFmt ==> " +timeCreatedFmt);

					//TotalInvoiced.Amount	17
					String totalInvoicedAmountFmt2 = CATFaltFileUtil.getFormattedNumber(totalInvoicedAmountDouble,"0000000000.00000");
					Log.customer.debug("totalInvoicedAmountFmt2 ==> " +totalInvoicedAmountFmt2);

					//TotalInvoiced.Amount	17
					String totalInvoicedAmountFmt3 = CATFaltFileUtil.getFormattedNumber(totalInvoicedAmountDouble,"0000000000000.00");
					Log.customer.debug("totalInvoicedAmountFmt3 ==> " +totalInvoicedAmountFmt3);

					//AccountingFacility	2
					String accountingFacilityFmt ="";
					String accountingFacility = vobj.getFieldValue("AccountingFacility").toString();
					Log.customer.debug("accountingFacility ==> " +accountingFacility);
					accountingFacilityFmt = CATFaltFileUtil.getFormattedTxt(accountingFacility,2);
					Log.customer.debug("accountingFacilityFmt ==> " +accountingFacilityFmt);

					//Department	5
					String departmentFmt ="";
					String department = "";
					if (vobj.getFieldValue("Department") != null) {
					department = vobj.getFieldValue("Department").toString();
					Log.customer.debug("department ==> " +department);
					departmentFmt = CATFaltFileUtil.getFormattedTxt(department,5);
					}
					Log.customer.debug("departmentFmt ==> " +departmentFmt);

					//Division	3
					String divisionFmt ="";
					String division = "";
					if (vobj.getFieldValue("Division") != null) {
						division = vobj.getFieldValue("Division").toString();
					Log.customer.debug("Division ==> " +division);
					divisionFmt = CATFaltFileUtil.getFormattedTxt(division,3);
					}
					Log.customer.debug("divisionFmt ==> " +divisionFmt);

					//Section	2
					String sectionFmt ="";
					String section = "";
					if (vobj.getFieldValue("Section") != null) {
						section = vobj.getFieldValue("Section").toString();
					Log.customer.debug("Section ==> " +section);
					sectionFmt = CATFaltFileUtil.getFormattedTxt(section,2);
					}
					Log.customer.debug("sectionFmt ==> " +sectionFmt);

					//ExpenseAccount	4
					String expenseAccountFmt ="";
					String expenseAccount = "";
					if (vobj.getFieldValue("ExpenseAccount") != null) {
						expenseAccount = vobj.getFieldValue("ExpenseAccount").toString();
					Log.customer.debug("ExpenseAccount ==> " +expenseAccount);
					expenseAccountFmt = CATFaltFileUtil.getFormattedTxt(expenseAccount,4);
					}
					Log.customer.debug("expenseAccountFmt ==> " +expenseAccountFmt);

					//Order	5
					String orderFmt ="";
					String order = "";
					if (vobj.getFieldValue("Order") != null) {
						order = vobj.getFieldValue("Order").toString();
					Log.customer.debug("order ==> " +order);
					orderFmt = CATFaltFileUtil.getFormattedTxt(order,5);
					}
					Log.customer.debug("orderFmt ==> " +orderFmt);


					//Misc	3
					String miscFmt ="";
					String misc = "";
					if (vobj.getFieldValue("Misc") != null) {
						misc = vobj.getFieldValue("Misc").toString();
					Log.customer.debug("Misc ==> " +misc);

					}
					miscFmt = CATFaltFileUtil.getFormattedTxt(misc,3);
					Log.customer.debug("MiscFmt ==> " +miscFmt);

					//Description	56
					String descriptionFmt ="";
					String description = "";
					if (vobj.getFieldValue("Description") != null) {
						description = vobj.getFieldValue("Description").toString();
					Log.customer.debug("Description ==> " +description);

					}
					descriptionFmt = CATFaltFileUtil.getFormattedTxt(description,56);
					Log.customer.debug("DescriptionFmt ==> " +descriptionFmt);

					//ControlDate	26
					datetimezone = new Date();
					vobj.setFieldValue( "ControlDate", datetimezone );
					String controlDateFmt;

					Log.customer.debug("datetimezone ==> " +datetimezone);
					controlDateFmt = CATFaltFileUtil.getFormattedDate(datetimezone,"yyyy-MM-dd-hh.mm.SS.SSSSSS");
					Log.customer.debug("controlDateFmt ==> " +controlDateFmt);

					String voucherData = voucherSupplierUniqueNameFmt+"~|"+timeCreatedFmt+"~|"+totalInvoicedAmountFmt+"~|"+currencyUniqueNameFmt+"~|"+
					                                   invoiceDateFmt+"~|"+invoiceNumberFmt+"~|"+batchNumFmt+"~|"+timeCreatedFmt+"~|"+totalInvoicedAmountFmt2+"~|"+totalInvoicedAmountFmt3+"~|"+
					                                   accountingFacilityFmt+"~|"+departmentFmt+"~|"+divisionFmt+"~|"+sectionFmt+"~|"+expenseAccountFmt+"~|"+orderFmt+"~|"+
					                                   miscFmt+"~|"+descriptionFmt+"~|"+controlDateFmt;
					Log.customer.debug("voucher data writing to file  ==> " +voucherData);
					outPW_FlatFile.write(voucherData);
					Log.customer.debug("New Line writing to file  ==> ");
					outPW_FlatFile.write("\n");

                    String controlVoucherData = getControlVoucherData(vobj,controlid,voucherSupplierUniqueName);

                    Log.customer.debug("write to control file ==> " + controlVoucherData);
                    outCtrl_FlatFile.write(controlVoucherData);
                    outCtrl_FlatFile.write("\n");
                    Log.customer.debug("%s: For Testting don't make Completed state" , thisclass);
                    //Commented out by Sandeep to be moved to Production.
                    //vobj.setFieldValue("ActionFlag", "Completed");
                    pushedCount++;

			} //if(vpoj != null)
                      //Added by Sandeep to commit 25records at time
                     commitCount++;
               if(commitCount == 25)
                      {
                      Log.customer.debug("******Performing Commit******* ",commitCount);
                       Base.getSession().transactionCommit();
                      commitCount = 0;
                      }
                      continue;

			} // while
                      Base.getSession().transactionCommit();
		}
		catch (Exception e) {

			Log.customer.debug(e);
			//add message
			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("VoucherPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATVoucherPush Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);
			throw new ScheduledTaskException("Error : VoucherEform="+ vobj +" ***If Requisition field has FatalAssertionException, set to null and Repush***" +vobj.toString() + e.toString(), e);
		}
		finally {

			if (outCtrl_FlatFile != null)  {
				outCtrl_FlatFile.flush();
				outCtrl_FlatFile.close();}


			if (outPW_FlatFile != null)  {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();
				//Change made by Soumya begins
				CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
				CATFaltFileUtil.copyFile(controlFlatFilePath, archiveFileCtrlPath);
				//Change made by Soumya end
				try {
					//Change made by Soumya begins

					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changing file permission of Data file.");
					Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changed file permission of Data file.");

					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changed file permission of Control file.");
					Runtime.getRuntime().exec("chmod 666 " + controlFlatFilePath);
					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changed file permission of Control file.");

					//Change made by Soumya end
					File f=new File(triggerFile);
					if(!f.exists()){
						 f.createNewFile();
						Log.customer.debug("triggerFile has been created "+ message.toString());
						  }
					 else {
						Log.customer.debug("triggerFile allready exit. "+ message.toString());
					 }
					// for changing file permission
					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changing file permission of trigger files ");
					Runtime.getRuntime().exec("chmod 666 " + triggerFile);
					Log.customer.debug("CATCAPSVoucherFlatFilePush:Changed file permission of trigger files ");

				} catch (IOException e1) {
					Log.customer.debug("triggerFile allready exit. "+ e1);
				}

			}
		}
		Log.customer.debug("%s: Inside Finally ", thisclass);
		message.append("Task start time : "+ startTime);
		Log.customer.debug("%s: Inside Finally added start time", thisclass);
		endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
		message.append("\n");
		message.append("Task end time : " + endTime);
		message.append("\n");
		message.append("Records to be pushed : "+ resultCount);
		message.append("\n");
		message.append("No. of records successfully pushed : "+ pushedCount);
		message.append("\n");
		Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

		// Sending email
		CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CATVoucherPushNotify");
		message = null;
		pushedCount =0;
		resultCount =0;



	}

	String getControlVoucherData(ClusterRoot vobj ,String controlid ,String voucherSupplierUniqueName) throws Exception {

		Log.customer.debug("Inside Control file data generation ... vobj .."+vobj );
		Log.customer.debug("Inside Control file data generation ... controlid .."+controlid );
		String topicname1 = new String("ControlObjectPush");
		Partition p2 = Base.getService().getPartition("None");
		ClusterRoot cluster =null;
		options1 = new AQLOptions(p2,true);



		String ctrlQuery = new String( "Select from cat.core.ControlPullObject where UniqueName = '"+controlid + "'" );
		Log.customer.debug("iRQuery ==> " + ctrlQuery);
		aqlctrlQuery = AQLQuery.parseQuery(ctrlQuery);
		ctrlResultSet = Base.getService().executeQuery(aqlctrlQuery, options1);

		if (ctrlResultSet.getErrors() != null)
		     Log.customer.debug("ERROR GETTING RESULTS in ctrlResultSet");

		int totalNumberOfCtrl = ctrlResultSet.getSize();

		Log.customer.debug("Inside getControlFileData ... totalNumberOfCtrl "+totalNumberOfCtrl );

//		Base.getSession().transactionBegin();


		if (totalNumberOfCtrl == 0) {
		        cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p2);
		        Log.customer.debug("Inside else part creating new cluster ..getControlFileData ... cluster "+cluster );
		        cluster.save();
		}
		else
			while(ctrlResultSet.next()){
			cluster = (ClusterRoot)ctrlResultSet.getBaseId("ControlPullObject").get();
			Log.customer.debug("Inside else part getControlFileData ... cluster "+cluster );
			}
		Log.customer.debug("Inside getControlFileData ... controlid "+controlid );
		cluster.setFieldValue("UniqueName", controlid);
		cluster.save();
		Log.customer.debug("Inside getControlFileData ... datetimezone "+datetimezone );
		cluster.setFieldValue("ControlDate", datetimezone);
		cluster.save();
		Log.customer.debug("Inside getControlFileData ... MSC_CAPS_MnBM_VOUCHERS ");
		cluster.setFieldValue("InterfaceName", "MSC_CAPS_MnBM_VOUCHERS");
		Log.customer.debug("Inside getControlFileData SourceSystem..........");
		cluster.setFieldValue("SourceSystem", " ");
		Log.customer.debug("5..........");
		cluster.setFieldValue("SourceFacility", "        ");	//8 Spaces
		Log.customer.debug("6..........");
		cluster.setFieldValue("TargetSystem", "CAPS");
		Log.customer.debug("7..........");
		cluster.setFieldValue("TargetFacility", "CAPS");
		Log.customer.debug("8..........");
		cluster.setFieldValue("RecordCount", new Integer(1));
		cluster.save();
		String ctrlData = "";

		if (bdTotCost != null)
		{
			bdTotCost =  bdTotCost.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
			cluster.setFieldValue("TotalAmount", bdTotCost);
			cluster.save();
		}
		//Code added by sneha as per the value for Area2 in 8.22 PROD
		iSpAcct = 1;
		//Code ended by sneha as per the value for Area2 in 8.22 PROD
		Log.customer.debug("Area2 .........."+ iSpAcct);
		cluster.setFieldValue("Area2", new Integer(iSpAcct));	//Sum of splitaccountings
		cluster.save();
		Log.customer.debug("Area2.......... saved");
		cluster.setFieldValue("Area3", "                                             ");	//48 Spaces
		Log.customer.debug("Area2..........");
		cluster.save();
		Log.customer.debug("saved .......... cluster.save();" + cluster.save());
		//Base.getSession().transactionCommit();
		Log.customer.debug("saved .......... cluster.save();.." + cluster.save());
		Log.customer.debug("cluster != null;..." + cluster);

		//if(cluster.isSaved()) {
			try
			{
				Log.customer.debug("cluster  before getting values" + cluster);
		//UniqueName 30
		String uniqueNameCRTLObj = (String)cluster.getFieldValue("UniqueName");
		String uniqueNameCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(uniqueNameCRTLObj.substring(14),30);
		Log.customer.debug("uniqueNameCRTLObjFmt.inside try......... uniqueNameCRTLObjFmt");

		//ControlDate	26
		Date controlDateCRTLObj = (Date)cluster.getFieldValue("ControlDate");
		String CcontrolDateCRTLObjFmt =  CATFaltFileUtil.getFormattedDate(controlDateCRTLObj,"yyyy-MM-dd-hh.mm.SS.SSSSSS");


		//InterfaceName	80
		String interfaceNameCRTLObj = (String)cluster.getFieldValue("InterfaceName");
		String interfaceNameCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(interfaceNameCRTLObj,80);

		//SourceSystem	20
		String sourceSystemCRTLObj = (String)cluster.getFieldValue("SourceSystem");
		String sourceSystemCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(sourceSystemCRTLObj,20);

		//SourceFacility 8
		String sourceFacilityCRTLObj = (String)cluster.getFieldValue("SourceFacility");
		String sourceFacilityCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(sourceFacilityCRTLObj,8);
		//TargetSystem	20
		String targetSystemCRTLObj = (String)cluster.getFieldValue("TargetSystem");
		String targetSystemCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(targetSystemCRTLObj,20);
		//TargetFacility 8
		String targetFacilityCRTLObj = (String)cluster.getFieldValue("TargetFacility");
		String targetFacilityCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(targetFacilityCRTLObj,8);
		//RcdCnt	15
		String recordCountCRTLObj = "1";
		String recordCountCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(recordCountCRTLObj,15);
		//TotalInvcAmt	45
		BigDecimal totalAmountCRTLObj = (BigDecimal)cluster.getFieldValue("TotalAmount");
		double totalAmountCRTLObjDbl = totalAmountCRTLObj.doubleValue();
		//String totalAmountCRTLObjStr = Double.toString(totalAmountCRTLObjDbl);
		String totalAmountCRTLObjFmt =  CATFaltFileUtil.getFormattedNumber(totalAmountCRTLObjDbl,"0000000000000.00");

		//TotalInvcLns	45
        //Code added by sneha as per the value for Area2 in 8.22 PROD
        String totalInvcLnsCRTLObj = (String)cluster.getFieldValue("Area2");
        String totalInvcLnsFmt = CATFaltFileUtil.getFormattedTxt(totalInvcLnsCRTLObj,45);

		// Commented the hard code by kannan : String totalInvcLnsFmt = "PIC 999";

		//Area3	45
		//String voucherSupplierUniqueNameFmt ="";
		//String voucherSupplierUniqueName = vobj.getDottedFieldValue("VoucherSupplier.UniqueName").toString();
		Log.customer.debug("VoucherSupplier.UniqueName ==> " +voucherSupplierUniqueName);
		String voucherSupplierUniqueNameFmt1 = CATFaltFileUtil.getFormattedTxt(voucherSupplierUniqueName,45);
		Log.customer.debug("voucherSupplierUniqueNameFmt for ctrl 0 ==> " +voucherSupplierUniqueNameFmt1);
		Log.customer.debug("voucherSupplierUniqueNameFmt  ctrlData Building .. ==> ");
		ctrlData = uniqueNameCRTLObjFmt+"~|"+CcontrolDateCRTLObjFmt+"~|"+interfaceNameCRTLObjFmt+"~|"+sourceSystemCRTLObjFmt+"~|"+ sourceFacilityCRTLObjFmt+"~|"+targetSystemCRTLObjFmt+"~|"+targetFacilityCRTLObjFmt+"~|"+recordCountCRTLObjFmt+"~|"+totalAmountCRTLObjFmt+"~|"+totalInvcLnsFmt+"~|"+voucherSupplierUniqueNameFmt1;
		Log.customer.debug("return ctrl data ....11"+ ctrlData);


			}
			catch(Exception e)
			{
				if (cluster == null)
						Log.customer.debug("Cluster is null after the push....");
				Log.customer.debug(e.toString());
				throw e;

			}
			//}
		//else{
		//	Log.customer.debug("cluster is NULL!!!!!..."+ ctrlData);
		//}
               //Commeted out by Sandeep
          // Base.getSession().transactionCommit();
		Log.customer.debug("return ctrl data ....22"+ ctrlData);
		return ctrlData;



	}

	String getDateTime(Date datetime) {
		int yy = (new Integer(Date.getYear(datetime))).intValue();
		int mm = (new Integer(Date.getMonth(datetime))).intValue();
		int dd = (new Integer(Date.getDayOfMonth(datetime))).intValue();
		int hh = (new Integer(Date.getHours(datetime))).intValue();
		int mn = (new Integer(Date.getMinutes(datetime))).intValue();
		int ss = (new Integer(Date.getSeconds(datetime))).intValue();
		mm++;
		String retstr = new String ("");
		retstr = retstr + yy;

		if ( mm/10 == 0)
		   retstr = retstr + "0" + mm;
		else
		   retstr = retstr + mm;

		if ( dd/10 == 0)
		   retstr = retstr + "0" + dd;
		else
		   retstr = retstr + dd;

		if ( hh/10 == 0)
		   retstr = retstr + "0" + hh;
		else
		   retstr = retstr + hh;

		if ( mn/10 == 0)
		   retstr = retstr + "0" + mn;
		else
		   retstr = retstr + mn;

		if ( ss/10 == 0)
		   retstr = retstr + "0" + ss;
		else
		   retstr = retstr + ss;

		return retstr;
    }


			}

