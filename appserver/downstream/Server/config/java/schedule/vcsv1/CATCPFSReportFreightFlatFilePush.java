package config.java.schedule.vcsv1;

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
import ariba.util.core.IOUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;


public class CATCPFSReportFreightFlatFilePush  extends ScheduledTask    {

    private String thisclass = "CATCPFSReportFreightFlatFilePush";
    private Date datetimezone;
    private String startTime, endTime;
    private FastStringBuffer message = null;
    private String mailSubject = null;
    private String fileExtDateTime = "";
    private String flatFilePath, controlFlatFilePath;
	//change made by soumya begings
	private String archiveFileDataPath;
	//change made by soumya ends
    private String triggerFile = "";
    private PrintWriter outPW_FlatFile, outCtrl_FlatFile;
    private AQLOptions options, options1;
    private Partition partition = null;
    private AQLResultCollection fResultSet;
    private int totalNumberOfFRforms;
    private AQLQuery aqlFQuery;
    private SplitAccountingCollection poAccounting = null;
    private int iSpAcct;
    private int resultCount, pushedCount;
    private java.math.BigDecimal bdTotCost;
    ClusterRoot fobj = null;

    public void run() throws ScheduledTaskException      {
    	try{
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject ="CATCPFSReportFreightFlatFilePush Task Completion Status - Completed Successfully";
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_CPFS_REPORT_FREIGHT."+ fileExtDateTime + ".txt";
			triggerFile = "/msc/arb9r1/downstream/catdata/INV/MSC_CPFS_REPORT_FREIGHT."+ fileExtDateTime + ".dstrigger";

			//Change made by soumya begins
			archiveFileDataPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_CPFS_REPORT_FREIGHT_ARCHIVE."+ fileExtDateTime + ".txt";
			//Change made by soumya ends

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);
			//Change made by soumya begins
			Log.customer.debug("CATCPFSReportFreightFlatFilePush:archiveFlatFile " + archiveFileDataPath);
			//Change made by soumya ends
			File cpfsReportFreightFlatFile = new File(flatFilePath);

            options = new AQLOptions(partition,true);

			if (!cpfsReportFreightFlatFile.exists()) {
				Log.customer.debug("File not exist creating file ..");
				cpfsReportFreightFlatFile.createNewFile();
			}

			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(cpfsReportFreightFlatFile),true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);
                         //Added condition to the query on Randy's request.
			String fQuery = new String( "select FreightsPayableEform from ariba.core.FreightsPayableEform where StatusString <> 'Composing' and CarrierCode.UniqueName <> '8496' and  ActionFlag IS NULL");
			Log.customer.debug("fQuery ==> " + fQuery);
			aqlFQuery = AQLQuery.parseQuery(fQuery);
			fResultSet = Base.getService().executeQuery(aqlFQuery, options);

			if (fResultSet.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in CATCPFSReportFreight");

			totalNumberOfFRforms = fResultSet.getSize();
			resultCount = totalNumberOfFRforms;
			Log.customer.debug("totalNumberOfs ==> " + totalNumberOfFRforms);
			while(fResultSet.next()){

				fobj = (ClusterRoot)fResultSet.getBaseId("FreightsPayableEform").get();
				if(fobj != null){
                                                Log.customer.debug("Setting the flag to completed");
                                         //Commented out by Sandeep to be moved to Production.
                    fobj.setFieldValue("ActionFlag", "Completed");
                              //Added by Sandeep to fetech value in email.
                                   pushedCount++;

					//carrierProNumber X(14)
					String carrierProNumberFmt ="";
					String carrierProNumber = "";
					if (fobj.getDottedFieldValue("CarrierProNumber") != null) {
						carrierProNumber = fobj.getFieldValue("CarrierProNumber").toString();
						Log.customer.debug("carrierProNumber ==> " +carrierProNumber);
					}
					carrierProNumberFmt = CATFaltFileUtil.getFormattedTxt(carrierProNumber,14);
					Log.customer.debug("carrierProNumberFmt ==> " +carrierProNumberFmt);

					//FreightsPayableEform_Export.CarrierCode.UniqueName	X(05)
					String carrierCodeFmt ="";
					String carrierCode = "";
					if (fobj.getDottedFieldValue("CarrierCode") != null) {
						carrierCode = fobj.getDottedFieldValue("CarrierCode.UniqueName").toString();
						Log.customer.debug("carrierCode ==> " +carrierCode);
					}
					carrierCodeFmt = CATFaltFileUtil.getFormattedTxt(carrierCode,14);
					Log.customer.debug("carrierCodeFmt ==> " +carrierCodeFmt);


					//FreightsPayableEform_Export.ReceivingFacility	X(02)
					String receivingFacilityFmt ="";
					String receivingFacility = "";
					if (fobj.getFieldValue("ReceivingFacility") != null) {
						receivingFacility = fobj.getFieldValue("ReceivingFacility").toString();
						Log.customer.debug("receivingFacility ==> " +receivingFacility);
					}
					receivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(receivingFacility,02);
					Log.customer.debug("receivingFacilityFmt ==> " +receivingFacilityFmt);

					//FreightsPayableEform_Export.DockCode	X(02)
					String dockCodeFmt ="";
					String dockCode = "";
					if (fobj.getFieldValue("DockCode") != null) {
						dockCode = fobj.getFieldValue("DockCode").toString();
						Log.customer.debug("dockCode ==> " +dockCode);
					}
					dockCodeFmt = CATFaltFileUtil.getFormattedTxt(dockCode,02);
					Log.customer.debug("dockCodeFmt ==> " +dockCodeFmt);

					//FreightsPayableEform_Export.ShippingDate	X(10)
					String shippingDateFmt ="";
					Date shippingDate = null;
					if (fobj.getFieldValue("ShippingDate") != null) {
						shippingDate = (Date)fobj.getFieldValue("ShippingDate");
						Log.customer.debug("shippingDate ==> " +shippingDate);
					}
					else
					{
						// Change made by Soumya add for null shippingDate
						shippingDate = new Date();
						Log.customer.debug("shippingDate getting null date need to investigate  ==> " +shippingDate);
					}
					shippingDateFmt = CATFaltFileUtil.getFormattedDate(shippingDate);
					Log.customer.debug("shippingDateFmt ==> " +shippingDateFmt);


					//FreightsPayableEform_Export.ReceivingDate	X(10)
					String receivingDateFmt ="";
					Date receivingDate = null;
					if (fobj.getFieldValue("ReceivingDate") != null) {
						receivingDate = (Date)fobj.getFieldValue("ReceivingDate");
						Log.customer.debug("receivingDate ==> " +receivingDate);
					}
					else
					{
						// Change made by Soumya add for null receivingDate
						receivingDate = new Date();
						Log.customer.debug("receivingDate getting null date need to investigate  ==> " +receivingDate);
					}
					receivingDateFmt = CATFaltFileUtil.getFormattedDate(receivingDate);
					Log.customer.debug("receivingDateFmt ==> " +receivingDateFmt);


					//FreightsPayableEform_Export.ShipmentWeight	X(08)

					String shipmentWeightFmt ="";
					String shipmentWeight = "";
					if (fobj.getFieldValue("ShipmentWeight") != null) {
						shipmentWeight = fobj.getFieldValue("ShipmentWeight").toString();
						Log.customer.debug("shipmentWeight ==> " +shipmentWeight);
					}
					shipmentWeightFmt = CATFaltFileUtil.getFormattedTxt(shipmentWeight,8);
					Log.customer.debug("shipmentWeightFmt ==> " +shipmentWeightFmt);

					//FreightsPayableEform_Export.PONumber	X(12)
					String pONumberFmt ="";
					String pONumber = "";
					if (fobj.getFieldValue("PONumber") != null) {
						pONumber = fobj.getFieldValue("PONumber").toString();
						Log.customer.debug("pONumber ==> " +pONumber);
					}
					pONumberFmt = CATFaltFileUtil.getFormattedTxt(pONumber,12);
					Log.customer.debug("pONumberFmt ==> " +pONumberFmt);


					//FreightsPayableEform_Export.SupplierCode	X(10)
					String supplierCodeFmt ="";
					String supplierCode = "";
					if (fobj.getFieldValue("SupplierCode") != null) {
						supplierCode = fobj.getFieldValue("SupplierCode").toString();
						Log.customer.debug("supplierCode ==> " +supplierCode);
					}
					supplierCodeFmt = CATFaltFileUtil.getFormattedTxt(supplierCode,10);
					Log.customer.debug("supplierCodeFmt ==> " +supplierCodeFmt);


					//FreightsPayableEform_Export. CityStateCode.UniqueName	X(06)
					String cityStateCodeFmt ="";
					String cityStateCode = "";
					if (fobj.getFieldValue("CityStateCode") != null) {
						cityStateCode = fobj.getDottedFieldValue("CityStateCode.UniqueName").toString();
						Log.customer.debug("cityStateCode ==> " +cityStateCode);
					}
					cityStateCodeFmt = CATFaltFileUtil.getFormattedTxt(cityStateCode,10);
					Log.customer.debug("CityStateCodeFmt ==> " +cityStateCodeFmt);


					//FreightsPayableEform_Export.UniqueName	X(14)
					String uniqueNameFmt ="";
					String uniqueName = "";
					if (fobj.getFieldValue("UniqueName") != null) {
						uniqueName = fobj.getFieldValue("UniqueName").toString();
						Log.customer.debug("uniqueName ==> " +uniqueName);
					}
					uniqueNameFmt = CATFaltFileUtil.getFormattedTxt(uniqueName,10);
					Log.customer.debug("uniqueNameFmt ==> " +uniqueNameFmt);





					String cPFSReportFreightData = carrierProNumberFmt+"~|"+carrierCodeFmt+"~|"+receivingFacilityFmt+"~|"+dockCodeFmt+"~|"+shippingDateFmt+"~|"+receivingDateFmt+"~|"+shipmentWeightFmt+"~|"+pONumberFmt+"~|"+supplierCodeFmt+"~|"+cityStateCodeFmt+"~|"+uniqueNameFmt;
					Log.customer.debug("cPFSReportFreightData writing to file  ==> " +cPFSReportFreightData);
					outPW_FlatFile.write(cPFSReportFreightData);
					Log.customer.debug("New Line writing to file  ==> ");
					outPW_FlatFile.write("\n");
			//		Log.customer.debug("Setting the flag to completed");
					 //Commented out by Sandeep to be moved to Production.
//                    fobj.setFieldValue("ActionFlag", "Completed");


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
			message.append("CATCPFSReportFreight Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATCPFSReportFreight Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);
			throw new ScheduledTaskException( e.toString(), e);
		}
       finally {

			if (outCtrl_FlatFile != null)  {
				outCtrl_FlatFile.flush();
				outCtrl_FlatFile.close();}


			if (outPW_FlatFile != null)  {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();

				//Change made by Soumya begins
				Log.customer.debug("CATCPFSReportFreightFlatFilePush:Starting Copying the flat file to Archive ");
				CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
				Log.customer.debug("CATCPFSReportFreightFlatFilePush:Completed Copying the flat file to Archive ");
				//Change made by Soumya end

				try {
					//Change made by Soumya begins
					Log.customer.debug("CATCPFSReportFreightFlatFilePush:Changing file permission of Data file.");
					Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
					Log.customer.debug("CATCPFSReportFreightFlatFilePush:Changed file permission of Data file.");
					//Change made by Soumya end

					File f=new File(triggerFile);
					if(!f.exists()){
						 f.createNewFile();
						Log.customer.debug("triggerFile has been created "+ message.toString());
						  }
					 else {
						Log.customer.debug("triggerFile allready exit. "+ message.toString());
					 }
					 //Change made by Soumya begins
					Log.customer.debug("CATCPFSReportFreightFlatFilePush:Changing file permission of trigger files ");
					Runtime.getRuntime().exec("chmod 666 " + triggerFile);
					Log.customer.debug("CATCPFSReportFreightFlatFilePush:Changed file permission of trigger files ");
					//Change made by Soumya end
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


    }
