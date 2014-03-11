/*******************************************************************************************************************************************

	Creator: Naresh Babu
	Description: Pushing the FreightPayble Object if the ActionFlag of the object IS NULL and the set the ActionFlag as Completed

	ChangeLog:
	Date		Name															History
	--------------------------------------------------------------------------------------------------------------
	6/06/2012 	23-IBM_AMS_Naresh					Pushing the FreightPayble Object if the ActionFlag of the object IS NULL and the set the ActionFlag as Completed
	08/03/2013  -IBM_AMS_Naresh						Clearing the Cache memory for pushed records counts.
*******************************************************************************************************************************************/




package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TimeZone;
import config.java.common.CatEmailNotificationUtil;
import config.java.schedule.util.CATFaltFileUtil;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SplitAccountingCollection;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;


public class CATCPFSReportFreightFlatFilePush  extends ScheduledTask    {

    private String thisclass = "CATCPFSReportFreightFlatFilePush";
    private Date datetimezone;
    private String startTime, endTime;
    //WI - declared below fields as local variables inside the methods 
   // private FastStringBuffer message = null;
  //private int resultCount=0, pushedCount=0;
    //WI-End
    private String mailSubject = null;
    private String fileExtDateTime = "";
    private String usFlatFilePath;
    private String usArchiveFileDataPath;
    private String sapFlatFilePath;
    private String sapArchiveFileDataPath;
    private String triggerFile = "";
    private AQLOptions options, options1;
    private Partition partition = null;
    private AQLResultCollection fResultSet;
    private int totalNumberOfFRforms;
    private AQLQuery aqlFQuery;
    private SplitAccountingCollection poAccounting = null;
    private int iSpAcct;
    
    private java.math.BigDecimal bdTotCost;
    ClusterRoot fobj = null;
    private String controlFlatFilePath = "";
	private PrintWriter outPW_FlatFile=null, outCtrl_FlatFile=null;

    private static final String FlatFilePathUS = Fmt.Sil("cat.java.common", "FlatFilePathUS");
    private static final String ArchiveFileDataPathUS = Fmt.Sil("cat.java.common", "ArchiveFileDataPathUS");
    private static final String FlatFilePathSAP = Fmt.Sil("cat.java.common", "FlatFilePathSAP");
    private static final String ArchiveFileDataPathSAP = Fmt.Sil("cat.java.common", "ArchiveFileDataPathSAP");
    private static final String TriggerFile = Fmt.Sil("cat.java.common", "TriggerFile");


	public void run() throws ScheduledTaskException      {
		FastStringBuffer message = new FastStringBuffer();
    	try{
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);

			//triggerFile = "/msc/arb9r1/downstream/catdata/INV/MSC_CPFS_REPORT_FREIGHT."+ fileExtDateTime + ".dstrigger";
			triggerFile = TriggerFile+"."+ fileExtDateTime + ".dstrigger";
			Log.customer.debug(thisclass+":run():triggerFile " + triggerFile);

			/*usFlatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_CPFS_REPORT_FREIGHT."+ fileExtDateTime + ".txt";
			usArchiveFileDataPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_CPFS_REPORT_FREIGHT_ARCHIVE."+ fileExtDateTime + ".txt";
			sapFlatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_SAP_CPFS_REPORT_FREIGHT."+ fileExtDateTime + ".txt";
			sapArchiveFileDataPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_SAP_CPFS_REPORT_FREIGHT_ARCHIVE."+ fileExtDateTime + ".txt";
			*/
			usFlatFilePath = FlatFilePathUS +"."+ fileExtDateTime + ".txt";
			usArchiveFileDataPath = ArchiveFileDataPathUS +"."+ fileExtDateTime + ".txt";
			sapFlatFilePath = FlatFilePathSAP +"."+ fileExtDateTime + ".txt";
			sapArchiveFileDataPath = ArchiveFileDataPathUS +"."+ fileExtDateTime + ".txt";

			Log.customer.debug(thisclass+":run():usFlatFilePath " + usFlatFilePath);
			Log.customer.debug(thisclass+":run():usArchiveFileDataPath " + usArchiveFileDataPath);
			Log.customer.debug(thisclass+":run():sapFlatFilePath " + sapFlatFilePath);
			Log.customer.debug(thisclass+":run():sapArchiveFileDataPath " + sapArchiveFileDataPath);

			//Create US flat file
			createUSFlatFile(usFlatFilePath);

			//Create SAP flat file
			createSAPFlatFile(sapFlatFilePath);



    	}
    	catch (Exception e) {

			Log.customer.debug(e);
			throw new ScheduledTaskException( e.toString(), e);
		}
       finally {
		   			//Copying US flat file to Archive
					Log.customer.debug(thisclass+":run():CATCPFSReportFreightFlatFilePush:Starting Copying the US flat file to Archive ");
					CATFaltFileUtil.copyFile(usFlatFilePath, usArchiveFileDataPath);
					Log.customer.debug(thisclass+":run():CATCPFSReportFreightFlatFilePush:Completed Copying the US flat file to Archive ");

					//Copying SAP flat file to Archive
					Log.customer.debug(thisclass+":run():CATCPFSReportFreightFlatFilePush:Starting Copying the SAP flat file to Archive ");
					CATFaltFileUtil.copyFile(sapFlatFilePath, sapArchiveFileDataPath);
					Log.customer.debug(thisclass+":run():CATCPFSReportFreightFlatFilePush:Completed Copying the SAP flat file to Archive ");


				try {
					Log.customer.debug(thisclass+":run():Inside Finally try block ");
					File f=new File(triggerFile);
					Log.customer.debug(thisclass+":run():triggerFile object has been created ");
					if(!f.exists()){
						 f.createNewFile();
						Log.customer.debug(thisclass+":run():triggerFile has been created "+triggerFile);
						  }
					 else {
						Log.customer.debug(thisclass+":run():triggerFile allready exit. ");
					 }

					//Log.customer.debug(thisclass+":run():CATSAPCPFSReportFreightFlatFilePush:Changing file permission of trigger files ");
					//Runtime.getRuntime().exec("chmod 666 " + triggerFile);
					//Log.customer.debug(thisclass+":run():CATSAPCPFSReportFreightFlatFilePush:Changed file permission of trigger files ");

				} catch (IOException e1) {
					Log.customer.debug("triggerFile allready exit. "+ e1);
				}


		}
       Log.customer.debug("%s: Inside Finally ", thisclass);

   	  }
	public void createUSFlatFile(String flatFilePath)
	{
		//WI- Added below variables as local variable to avoid  cache storage issue.
		int resultCount=0, pushedCount=0;
		FastStringBuffer message = new FastStringBuffer();
		//WI-End
		try{
			
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			
			mailSubject ="CATCPFSReportFreightFlatFilePush Task Completion Status - Completed Successfully";
			File cpfsReportFreightFlatFile = new File(flatFilePath);

			options = new AQLOptions(partition,true);

			if (!cpfsReportFreightFlatFile.exists()) {
				Log.customer.debug(thisclass+":createUSFlatFile():File not exist creating file ..");
				cpfsReportFreightFlatFile.createNewFile();
			}

			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(cpfsReportFreightFlatFile),true);
			Log.customer.debug(thisclass+" :createUSFlatFile(): outPW_FlatFile :" + outPW_FlatFile);

			String fQuery = new String( "select FreightsPayableEform from ariba.core.FreightsPayableEform PARTITION pcsv1 where StatusString <> 'Composing' and CarrierCode.UniqueName <> '8496' and  ActionFlag IS NULL");
			Log.customer.debug(thisclass+" :createUSFlatFile():fQuery ==> " + fQuery);
			aqlFQuery = AQLQuery.parseQuery(fQuery);
			fResultSet = Base.getService().executeQuery(aqlFQuery, options);

			if (fResultSet.getErrors() != null)
				Log.customer.debug(thisclass+" :createUSFlatFile():ERROR GETTING RESULTS in CATCPFSReportFreight");

			totalNumberOfFRforms = fResultSet.getSize();
			resultCount = totalNumberOfFRforms;
			Log.customer.debug(thisclass+" :createUSFlatFile():totalNumberOfs ==> " + totalNumberOfFRforms);
			while(fResultSet.next()){

				fobj = (ClusterRoot)fResultSet.getBaseId("FreightsPayableEform").get();
				if(fobj != null){
												Log.customer.debug(thisclass+" :createUSFlatFile():Setting the flag to completed");

					fobj.setFieldValue("ActionFlag", "Completed");
							 	   pushedCount++;

					//carrierProNumber X(14)
					String carrierProNumberFmt ="";
					String carrierProNumber = "";
					if (fobj.getDottedFieldValue("CarrierProNumber") != null) {
						carrierProNumber = fobj.getFieldValue("CarrierProNumber").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():carrierProNumber ==> " +carrierProNumber);
					}
					carrierProNumberFmt = CATFaltFileUtil.getFormattedTxt(carrierProNumber,14);
					Log.customer.debug(thisclass+" :createUSFlatFile():carrierProNumberFmt ==> " +carrierProNumberFmt);

					//FreightsPayableEform_Export.CarrierCode.UniqueName	X(05)
					String carrierCodeFmt ="";
					String carrierCode = "";
					if (fobj.getDottedFieldValue("CarrierCode") != null) {
						carrierCode = fobj.getDottedFieldValue("CarrierCode.UniqueName").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():carrierCode ==> " +carrierCode);
					}
					carrierCodeFmt = CATFaltFileUtil.getFormattedTxt(carrierCode,14);
					Log.customer.debug(thisclass+" :createUSFlatFile():carrierCodeFmt ==> " +carrierCodeFmt);


					//FreightsPayableEform_Export.ReceivingFacility	X(02)
					String receivingFacilityFmt ="";
					String receivingFacility = "";
					if (fobj.getFieldValue("ReceivingFacility") != null) {
						receivingFacility = fobj.getFieldValue("ReceivingFacility").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():receivingFacility ==> " +receivingFacility);
					}
					receivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(receivingFacility,02);
					Log.customer.debug(thisclass+" :createUSFlatFile():receivingFacilityFmt ==> " +receivingFacilityFmt);

					//FreightsPayableEform_Export.DockCode	X(02)
					String dockCodeFmt ="";
					String dockCode = "";
					if (fobj.getFieldValue("DockCode") != null) {
						dockCode = fobj.getFieldValue("DockCode").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():dockCode ==> " +dockCode);
					}
					dockCodeFmt = CATFaltFileUtil.getFormattedTxt(dockCode,02);
					Log.customer.debug(thisclass+" :createUSFlatFile():dockCodeFmt ==> " +dockCodeFmt);

					//FreightsPayableEform_Export.ShippingDate	X(10)
					String shippingDateFmt ="";
					Date shippingDate = null;
					if (fobj.getFieldValue("ShippingDate") != null) {
						shippingDate = (Date)fobj.getFieldValue("ShippingDate");
						Log.customer.debug(thisclass+" :createUSFlatFile():shippingDate ==> " +shippingDate);
					}
					else
					{

						shippingDate = new Date();
						Log.customer.debug(thisclass+" :createUSFlatFile():shippingDate getting null date need to investigate  ==> " +shippingDate);
					}
					shippingDateFmt = CATFaltFileUtil.getFormattedDate(shippingDate);
					Log.customer.debug(thisclass+" :createUSFlatFile():shippingDateFmt ==> " +shippingDateFmt);


					//FreightsPayableEform_Export.ReceivingDate	X(10)
					String receivingDateFmt ="";
					Date receivingDate = null;
					if (fobj.getFieldValue("ReceivingDate") != null) {
						receivingDate = (Date)fobj.getFieldValue("ReceivingDate");
						Log.customer.debug(thisclass+" :createUSFlatFile():receivingDate ==> " +receivingDate);
					}
					else
					{


						receivingDate = new Date();
						Log.customer.debug(thisclass+" :createUSFlatFile():receivingDate getting null date need to investigate  ==> " +receivingDate);
					}
					receivingDateFmt = CATFaltFileUtil.getFormattedDate(receivingDate);
					Log.customer.debug(thisclass+" :createUSFlatFile():receivingDateFmt ==> " +receivingDateFmt);


					//FreightsPayableEform_Export.ShipmentWeight	X(08)

					String shipmentWeightFmt ="";
					String shipmentWeight = "";
					if (fobj.getFieldValue("ShipmentWeight") != null) {
						shipmentWeight = fobj.getFieldValue("ShipmentWeight").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():shipmentWeight ==> " +shipmentWeight);
					}
					shipmentWeightFmt = CATFaltFileUtil.getFormattedTxt(shipmentWeight,8);
					Log.customer.debug(thisclass+" :createUSFlatFile():shipmentWeightFmt ==> " +shipmentWeightFmt);

					//FreightsPayableEform_Export.PONumber	X(12)
					String pONumberFmt ="";
					String pONumber = "";
					if (fobj.getFieldValue("PONumber") != null) {
						pONumber = fobj.getFieldValue("PONumber").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():pONumber ==> " +pONumber);
					}
					pONumberFmt = CATFaltFileUtil.getFormattedTxt(pONumber,12);
					Log.customer.debug(thisclass+" :createUSFlatFile():pONumberFmt ==> " +pONumberFmt);


					//FreightsPayableEform_Export.SupplierCode	X(10)
					String supplierCodeFmt ="";
					String supplierCode = "";
					if (fobj.getFieldValue("SupplierCode") != null) {
						supplierCode = fobj.getFieldValue("SupplierCode").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():supplierCode ==> " +supplierCode);
					}
					supplierCodeFmt = CATFaltFileUtil.getFormattedTxt(supplierCode,10);
					Log.customer.debug(thisclass+" :createUSFlatFile():supplierCodeFmt ==> " +supplierCodeFmt);


					//FreightsPayableEform_Export. CityStateCode.UniqueName	X(06)
					String cityStateCodeFmt ="";
					String cityStateCode = "";
					if (fobj.getFieldValue("CityStateCode") != null) {
						cityStateCode = fobj.getDottedFieldValue("CityStateCode.UniqueName").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():cityStateCode ==> " +cityStateCode);
					}
					cityStateCodeFmt = CATFaltFileUtil.getFormattedTxt(cityStateCode,10);
					Log.customer.debug(thisclass+" :createUSFlatFile():CityStateCodeFmt ==> " +cityStateCodeFmt);


					//FreightsPayableEform_Export.UniqueName	X(14)
					String uniqueNameFmt ="";
					String uniqueName = "";
					if (fobj.getFieldValue("UniqueName") != null) {
						uniqueName = fobj.getFieldValue("UniqueName").toString();
						Log.customer.debug(thisclass+" :createUSFlatFile():uniqueName ==> " +uniqueName);
					}
					uniqueNameFmt = CATFaltFileUtil.getFormattedTxt(uniqueName,10);
					Log.customer.debug(thisclass+" :createUSFlatFile():uniqueNameFmt ==> " +uniqueNameFmt);

					String cPFSReportFreightData = carrierProNumberFmt+"~|"+carrierCodeFmt+"~|"+receivingFacilityFmt+"~|"+dockCodeFmt+"~|"+shippingDateFmt+"~|"+receivingDateFmt+"~|"+shipmentWeightFmt+"~|"+pONumberFmt+"~|"+supplierCodeFmt+"~|"+cityStateCodeFmt+"~|"+uniqueNameFmt;
					Log.customer.debug(thisclass+" :createUSFlatFile():cPFSReportFreightData writing to file  ==> " +cPFSReportFreightData);
					outPW_FlatFile.write(cPFSReportFreightData);
					Log.customer.debug(thisclass+" :createUSFlatFile():New Line writing to file  ==> ");
					outPW_FlatFile.write("\n");
				}
			}
		}catch (Exception e) {

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
					message.append("CATCPFSReportFreight Failed - Exception details below");
					message.append("\n");
					message.append(e.toString());
					mailSubject = "CATCPFSReportFreight Task Failed";
					Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);

				}
		       finally {

					if (outCtrl_FlatFile != null)  {
						outCtrl_FlatFile.flush();
						outCtrl_FlatFile.close();}


					if (outPW_FlatFile != null)  {
						outPW_FlatFile.flush();
						outPW_FlatFile.close();
						/*try{
							Log.customer.debug(thisclass+" :createUSFlatFile():CATCPFSReportFreightFlatFilePush Changing file permission of Data file.");
							Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
							Log.customer.debug(thisclass+" :createUSFlatFile(): CATCPFSReportFreightFlatFilePush Changed file permission of Data file.");
						}catch(Exception e){
							Log.customer.debug(e);

						}*/
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
				
	}
	public void createSAPFlatFile(String flatFilePath)
	{
		//WI- Added below variables as local variable to avoid  cache storage issue.
		FastStringBuffer message = new FastStringBuffer();
		int resultCount=0, pushedCount=0;
		//WI-End
		try{
		
		partition = Base.getService().getPartition();
		datetimezone = new Date();
		startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
		
		mailSubject ="CATCPFSReportFreightFlatFilePush Task Completion Status - Completed Successfully";
		File cpfsReportFreightFlatFile = new File(flatFilePath);

		options = new AQLOptions(partition,true);
		if (!cpfsReportFreightFlatFile.exists()) {
				Log.customer.debug(thisclass+" :createSAPFlatFile():File not exist creating file ..");
				cpfsReportFreightFlatFile.createNewFile();
			}

			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(cpfsReportFreightFlatFile),true);
			Log.customer.debug(thisclass+" :createSAPFlatFile():outPW_FlatFile " + outPW_FlatFile);

			String fQuery = new String( "select SAPFreightsPayableEform from ariba.core.SAPFreightsPayableEform PARTITION SAP where StatusString <> 'Composing' and CarrierCode.UniqueName <> '8496' and  ActionFlag IS NULL");
			Log.customer.debug(thisclass+" :createSAPFlatFile():fQuery ==> " + fQuery);
			aqlFQuery = AQLQuery.parseQuery(fQuery);
			fResultSet = Base.getService().executeQuery(aqlFQuery, options);

			if (fResultSet.getErrors() != null)
				Log.customer.debug(thisclass+" :createSAPFlatFile():ERROR GETTING RESULTS in CATCPFSReportFreight");

			totalNumberOfFRforms = fResultSet.getSize();
			resultCount = totalNumberOfFRforms;
			Log.customer.debug(thisclass+" :createSAPFlatFile():totalNumberOfs ==> " + totalNumberOfFRforms);
			while(fResultSet.next()){

				fobj = (ClusterRoot)fResultSet.getBaseId("SAPFreightsPayableEform").get();
				if(fobj != null){
												Log.customer.debug(thisclass+" :createSAPFlatFile():Setting the flag to completed");

					fobj.setFieldValue("ActionFlag", "Completed");

								   pushedCount++;

					//carrierProNumber X(14)
					String carrierProNumberFmt ="";
					String carrierProNumber = "";
					if (fobj.getDottedFieldValue("CarrierProNumber") != null) {
						carrierProNumber = fobj.getFieldValue("CarrierProNumber").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():carrierProNumber ==> " +carrierProNumber);
					}
					carrierProNumberFmt = CATFaltFileUtil.getFormattedTxt(carrierProNumber,14);
					Log.customer.debug(thisclass+" :createSAPFlatFile():carrierProNumberFmt ==> " +carrierProNumberFmt);

					//FreightsPayableEform_Export.CarrierCode.UniqueName	X(05)
					String carrierCodeFmt ="";
					String carrierCode = "";
					if (fobj.getDottedFieldValue("CarrierCode") != null) {
						carrierCode = fobj.getDottedFieldValue("CarrierCode.UniqueName").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():carrierCode ==> " +carrierCode);
					}
					carrierCodeFmt = CATFaltFileUtil.getFormattedTxt(carrierCode,14);
					Log.customer.debug(thisclass+" :createSAPFlatFile():carrierCodeFmt ==> " +carrierCodeFmt);


					//FreightsPayableEform_Export.ReceivingFacility	X(02)
					String receivingFacilityFmt ="";
					String receivingFacility = "";
					if (fobj.getFieldValue("ReceivingFacility") != null) {
						receivingFacility = fobj.getFieldValue("ReceivingFacility").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():receivingFacility ==> " +receivingFacility);
					}
					receivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(receivingFacility,02);
					Log.customer.debug(thisclass+" :createSAPFlatFile():receivingFacilityFmt ==> " +receivingFacilityFmt);

					//FreightsPayableEform_Export.DockCode	X(02)
					String dockCodeFmt ="";
					String dockCode = "";
					if (fobj.getFieldValue("DockCode") != null) {
						dockCode = fobj.getFieldValue("DockCode").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():dockCode ==> " +dockCode);
					}
					dockCodeFmt = CATFaltFileUtil.getFormattedTxt(dockCode,02);
					Log.customer.debug(thisclass+" :createSAPFlatFile():dockCodeFmt ==> " +dockCodeFmt);

					//FreightsPayableEform_Export.ShippingDate	X(10)
					String shippingDateFmt ="";
					Date shippingDate = null;
					if (fobj.getFieldValue("ShippingDate") != null) {
						shippingDate = (Date)fobj.getFieldValue("ShippingDate");
						Log.customer.debug(thisclass+" :createSAPFlatFile():shippingDate ==> " +shippingDate);
					}
					else
					{

						shippingDate = new Date();
						Log.customer.debug(thisclass+" :createSAPFlatFile():shippingDate getting null date need to investigate  ==> " +shippingDate);
					}
					shippingDateFmt = CATFaltFileUtil.getFormattedDate(shippingDate);
					Log.customer.debug(thisclass+" :createSAPFlatFile():shippingDateFmt ==> " +shippingDateFmt);


					//FreightsPayableEform_Export.ReceivingDate	X(10)
					String receivingDateFmt ="";
					Date receivingDate = null;
					if (fobj.getFieldValue("ReceivingDate") != null) {
						receivingDate = (Date)fobj.getFieldValue("ReceivingDate");
						Log.customer.debug(thisclass+" :createSAPFlatFile():receivingDate ==> " +receivingDate);
					}
					else
					{
						// Change made by Soumya add for null receivingDate
						receivingDate = new Date();
						Log.customer.debug(thisclass+" :createSAPFlatFile():receivingDate getting null date need to investigate  ==> " +receivingDate);
					}
					receivingDateFmt = CATFaltFileUtil.getFormattedDate(receivingDate);
					Log.customer.debug(thisclass+" :createSAPFlatFile():receivingDateFmt ==> " +receivingDateFmt);


					//FreightsPayableEform_Export.ShipmentWeight	X(08)

					String shipmentWeightFmt ="";
					String shipmentWeight = "";
					if (fobj.getFieldValue("ShipmentWeight") != null) {
						shipmentWeight = fobj.getFieldValue("ShipmentWeight").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():shipmentWeight ==> " +shipmentWeight);
					}
					shipmentWeightFmt = CATFaltFileUtil.getFormattedTxt(shipmentWeight,8);
					Log.customer.debug(thisclass+" :createSAPFlatFile():shipmentWeightFmt ==> " +shipmentWeightFmt);

					//FreightsPayableEform_Export.PONumber	X(12)
					String pONumberFmt ="";
					String pONumber = "";
					if (fobj.getFieldValue("PONumber") != null) {
						pONumber = fobj.getFieldValue("PONumber").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():pONumber ==> " +pONumber);
					}
					pONumberFmt = CATFaltFileUtil.getFormattedTxt(pONumber,12);
					Log.customer.debug(thisclass+" :createSAPFlatFile():pONumberFmt ==> " +pONumberFmt);


					//FreightsPayableEform_Export.SupplierCode	X(10)
					String supplierCodeFmt ="";
					String supplierCode = "";
					if (fobj.getFieldValue("SupplierCode") != null) {
						supplierCode = fobj.getFieldValue("SupplierCode").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():supplierCode ==> " +supplierCode);
					}
					supplierCodeFmt = CATFaltFileUtil.getFormattedTxt(supplierCode,10);
					Log.customer.debug(thisclass+" :createSAPFlatFile():supplierCodeFmt ==> " +supplierCodeFmt);


					//FreightsPayableEform_Export. CityStateCode.UniqueName	X(06)
					String cityStateCodeFmt ="";
					String cityStateCode = "";
					if (fobj.getFieldValue("CityStateCode") != null) {
						cityStateCode = fobj.getDottedFieldValue("CityStateCode.UniqueName").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():cityStateCode ==> " +cityStateCode);
					}
					cityStateCodeFmt = CATFaltFileUtil.getFormattedTxt(cityStateCode,10);
					Log.customer.debug(thisclass+" :createSAPFlatFile():CityStateCodeFmt ==> " +cityStateCodeFmt);


					//FreightsPayableEform_Export.UniqueName	X(14)
					String uniqueNameFmt ="";
					String uniqueName = "";
					if (fobj.getFieldValue("UniqueName") != null) {
						uniqueName = fobj.getFieldValue("UniqueName").toString();
						Log.customer.debug(thisclass+" :createSAPFlatFile():uniqueName ==> " +uniqueName);
					}
					uniqueNameFmt = CATFaltFileUtil.getFormattedTxt(uniqueName,10);
					Log.customer.debug(thisclass+" :createSAPFlatFile():uniqueNameFmt ==> " +uniqueNameFmt);





					String cPFSReportFreightData = carrierProNumberFmt+"~|"+carrierCodeFmt+"~|"+receivingFacilityFmt+"~|"+dockCodeFmt+"~|"+shippingDateFmt+"~|"+receivingDateFmt+"~|"+shipmentWeightFmt+"~|"+pONumberFmt+"~|"+supplierCodeFmt+"~|"+cityStateCodeFmt+"~|"+uniqueNameFmt;
					Log.customer.debug(thisclass+" :createSAPFlatFile():cPFSReportFreightData writing to file  ==> " +cPFSReportFreightData);
					outPW_FlatFile.write(cPFSReportFreightData);
					Log.customer.debug(thisclass+" :createSAPFlatFile():New Line writing to file  ==> ");
					outPW_FlatFile.write("\n");

				}

			}


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
			message.append("CATSAPCPFSReportFreight Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATSAPCPFSReportFreight Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);

		}
	   finally {

			if (outCtrl_FlatFile != null)  {
				outCtrl_FlatFile.flush();
				outCtrl_FlatFile.close();}


			if (outPW_FlatFile != null)  {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();

			/*try{
			Log.customer.debug(thisclass+" :createSAPFlatFile():CATCPFSReportFreightFlatFilePush:Changing file permission of Data file.");
			Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
			Log.customer.debug("CATCPFSReportFreightFlatFilePush:Changed file permission of Data file.");
		}catch(Exception e){
			Log.customer.debug(e);

			}*/
		}
		}
	  Log.customer.debug("%s: Inside Finally ", thisclass);
	message.append("Task start time : "+ startTime);
	Log.customer.debug("%s: Inside Finally added start time", thisclass);
	endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
	message.append("\n");
	message.append("Task end time : " + endTime);
	Log.customer.debug("%s: Inside Finally added End time" + endTime);
	message.append("\n");
	message.append("Records to be pushed : "+ resultCount);
	Log.customer.debug("%s: Inside Finally Records to be pushed :" + resultCount);
	message.append("\n");
	message.append("No. of records successfully pushed : "+ pushedCount);
	Log.customer.debug("%s: Inside Finally No. of records successfully pushed :" + pushedCount);
	message.append("\n");
	Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

	// Sending email
	CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CATVoucherPushNotify");
	

	
		}
	

	}



