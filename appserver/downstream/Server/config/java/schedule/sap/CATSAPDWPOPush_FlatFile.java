/*******************************************************************************************************************************************

    Creator: Garima
    Description: Writing the fields from the PO to flat file
    ChangeLog:
    Date		Name				   History
    15/05/09  Sudheer K Jain     Issue 958  Removing special charcter single quotes "'".
    02/11/10  PGS Kannan         Issue # 1054 / CR202  deafult AccFac is 02 if CompanyCode.SAPSource") is CBS 	then AccFac = "86
	29/11/11  IBM AMS Vikram Singh Filtering non ASCII characters
	21/03/12  Rajat Bajpai        Changes made to run the task in LSAP partition
	15/06/2012 Dharshan   Issue #269	 IsAdHoc - catalog or non catalog,
    --------------------------------------------------------------------------------------------------------------

*******************************************************************************************************************************************/

package config.java.schedule.sap;

import java.io.File;
//change begin by Soumya
import java.io.IOException;
//change end by Soumya
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import ariba.approvable.core.LineItem;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLUpdate;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.DirectOrder;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
//change made by Soumya begins
import config.java.schedule.util.CATFaltFileUtil;
//change made by Soumya ends

public class CATSAPDWPOPush_FlatFile  extends ScheduledTask  {
    private Partition p;
    private String query;
    private String query1;
    private String partitionName;

    private String classname = "CATSAPDWPOPush_FlatFile";
    private String flatFilePath = "/msc/arb9r1/downstream/catdata/DW/MSC_DW_PO_PUSH_SAP.txt";
	//change made by soumya begings
	private String fileExtDateTime = "";
	private String archiveFileDataPath = "/msc/arb9r1/downstream/catdata/DW/archive/MSC_DW_PO_PUSH_SAP_ARCHIVE.";
	//change made by soumya ends
    private int partitionNumber;

    private FastStringBuffer message = null;
	String mailSubject = null;
	//private int resultCount, pushedCount;
	private String startTime, endTime;
	private String usPOFlagAction = null;
	private String periodenddate = null;
	private String datasetFacilityCode = "Z8";
	private boolean isCompletedFlgUpdate = false;
	private AQLResultCollection results = null;
	private AQLResultCollection results1 = null;
	private File out_FlatFile = null;
	private PrintWriter outPW_FlatFile =null;
	private boolean splitAccounting = true;
	private String RecordType = "PO";
    private String FileLayoutVersion = "8.1.1";
    private String Filler;
    private String PHRecordStatus = "A";
    private String PHPOType = "SD";
    private String PHDeskNo;
    private String PHFacCode;
    private String PHRecDock;
    private String PHCloseInd = "A";
    //private String PHCloseDate ;
    private String PLCloseInd = "A";
    //private String PLCloseDate ;
    private String PLRecStatus = "A";
    private String PLSpendType = "I";
    private String PLItemNo;
    private String PLItemCCode = "0 ";
    private String MatCode;
	private String MatCodeDes;
	private String PLItemStatusCode;
	private String PLSupInd = "Y";
	private String PLRSFInd = "F";
	private String PLKeyWord;
	private String PLDwgChgL;
	private String PLDwgVersNo;
	private String pLOrderNumber;
	private String PLLangCode = "E";
	private String AnnualDemdQuantity = "0";
	private String pLMiscQualifier;
	private String flatFilePathST;
	private String source;
	private Boolean isAdHoc;
	private boolean isAdHocBoolean;


    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
							super.init(scheduler, scheduledTaskName, arguments);
							for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
								String key = (String)e.next();
								if (key.equals("queryST")) {
								Log.customer.debug("queryST");
									query  = (String)arguments.get(key);
									Log.customer.debug("flatFilePathST "+flatFilePathST);
							}
							else if(key.equals("flatFilePathST"))
								    	{
								    		flatFilePath = (String)arguments.get(key);
								    		Log.customer.debug("flatFilePathST "+flatFilePathST);
								    	}


		                    }

                           }

    public void run() throws ScheduledTaskException  {
        Log.customer.debug("%s::Start of CATMFGDWPOPush_FlatFile task ...", classname);

        Log.customer.debug("%s::archiveFileDataPath:%s",classname,archiveFileDataPath);


        // Read the USPOFlagAction info from cat.dwAction.util
        usPOFlagAction = Fmt.Sil("cat.dwAction", "USPOFlagAction");
        Log.customer.debug("%s::usPOFlagAction ...", usPOFlagAction, classname);

           if (usPOFlagAction!=null && ! usPOFlagAction.equals("None") && usPOFlagAction.indexOf("not found")==-1)
        	   isCompletedFlgUpdate = false;
          if ( usPOFlagAction!=null && usPOFlagAction.equals("Completed")&& usPOFlagAction.indexOf("not found")==-1)
        	  isCompletedFlgUpdate = true;



        startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
        periodenddate = DateFormatter.toYearMonthDate(Date.getNow());
        Log.customer.debug("%s::IR_period_end_date:%s",classname,periodenddate);

        try {

			//Changes made by Rajat

			partitionName = Base.getService().getPartition().getName();

			Log.customer.debug("%s::Partition is:%s", classname,partitionName);

			if (partitionName == "LSAP")

			archiveFileDataPath = "/msc/arb9r1/downstream/catdata/DW/archive/MSC_DW_PO_PUSH_LSAP_ARCHIVE.";

			//Changes made by Rajat end

			//change made by Soumya begins
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			Log.customer.debug("%s::fileExtDateTime:%s",classname,fileExtDateTime);
			Log.customer.debug("%s::archiveFileDataPath:%s",classname,archiveFileDataPath);
			archiveFileDataPath = archiveFileDataPath + fileExtDateTime + ".txt";
			Log.customer.debug("%s::archiveFileDataPath:%s",classname,archiveFileDataPath);

			//Change made by Soumya begins

			Log.customer.debug("%s::FilePath:%s", classname, out_FlatFile);

			Log.customer.debug("CATSAPDWPOPush_FlatFile:Starting Copying the flat file to Archive ");
			CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
			Log.customer.debug("CATSAPDWPOPush_FlatFile:Completed Copying the flat file to Archive ");

			//change made by soumya ends

            out_FlatFile = new File(flatFilePath);
            Log.customer.debug("%s::FilePath:%s", classname, out_FlatFile);

            if (!out_FlatFile.exists()) {
                Log.customer.debug("%s::if file does not exit then create 1", classname);
                out_FlatFile.createNewFile();
            }
            Log.customer.debug("%s::Creating aprint writer obj:", classname);
            outPW_FlatFile = new PrintWriter(IOUtil.bufferedOutputStream(out_FlatFile), true);
            p = Base.getSession().getPartition();
            Log.customer.debug("%s::Partition is:%s", classname,p);
            message = new FastStringBuffer();
    		//mailSubject = "CATMGFDWPOPush_FlatFile Task Completion Status - Completed Successfully";

            try {

               // query = "select from ariba.purchasing.core.DirectOrder where DWPOFlag = 'InProcess' or DWPOFlag = 'Processing'";
                Log.customer.debug(query);

                AQLQuery aqlquery = null;
                AQLOptions options = null;
                AQLQuery aqlquery1 = null;
                AQLOptions options1 = null;

                ariba.purchasing.core.DirectOrder directOrder = null;

                aqlquery = AQLQuery.parseQuery(query);
                Log.customer.debug("aqlquery=>"+aqlquery);
                options = new AQLOptions(p);
                //options.setCursorMode(true);
                //options.setRowLimit(5);
                results = Base.getService().executeQuery(aqlquery, options);
                if( (results != null) && (!results.isEmpty()))
                Log.customer.debug("Result is not null"+results);
												{
												while (results.next()) {
													query1=(String) results.getString(0);
													Log.customer.debug("query1 from staging table=>"+query1);
												}
							          }
							          // Parsing the staging query for receipt process
									  			aqlquery1 = AQLQuery.parseQuery(query1);
									  			Log.customer.debug("aqlquery1=>"+aqlquery1);
									  			options1 = new AQLOptions(p);
			                    results1 = Base.getService().executeQuery(aqlquery1, options1);

                //resultCount = results.getSize();
                 int count = 0;

                if( (results1 != null) && (!results1.isEmpty()))
			         {
					Log.customer.debug("Result1 is not null"+results1);
                while (results1.next()) {
                    directOrder = (DirectOrder)(results1.getBaseId("DirectOrder").get());
                    Log.customer.debug("***********************************************************");

                    if (directOrder != null) {
                        try {

							// PO Line Item check
							BaseVector po_LineItems = (BaseVector)directOrder.getLineItems();

							Log.customer.debug("%s::POLineItem:%s",classname,po_LineItems);
							int lineCount = directOrder.getLineItemsCount();
				    		Log.customer.debug("%s::Line Item count for PO:%s ",classname,lineCount);

                            if ( lineCount > 0) {

                            //Partition
                            partitionNumber = directOrder.getPartitionNumber();
                            Log.customer.debug("%s::DO_Partition:%s", classname, partitionNumber);


                            if (partitionNumber == 5) {
                                partitionName = "SAP";
                                }

                           // Rajat - Changes made here to include LSAP

                             if (partitionNumber == 6) {

							     partitionName = "LSAP";
                                }

                           // Rajat - Changes end here


							for (int i =0; i<po_LineItems.size();i++){
								splitAccounting = true;

							LineItem poLineItem = (LineItem)po_LineItems.get(i);

							SplitAccountingCollection sacol =(SplitAccountingCollection) poLineItem.getFieldValue("Accountings");
							if (sacol != null && sacol.getSplitAccountings() != null) {
											BaseVector sas = sacol.getSplitAccountings();
											// for (int j =0; j <sas.size();j++){ -- As per mapping document :
											//It says Generate the data records based on LineItem array
											for (int j =0; j <1 ;j++){
							                   SplitAccounting sa  = (SplitAccounting)sas.get(j);
							if(splitAccounting)
							{

							//******  	 File write Start ********

                            // 0 PO Approved date
							if (directOrder.getFieldValue("ApprovedDate") != null) {
							Date approvedDate = (Date)  directOrder.getFieldValue("ApprovedDate");
							String approvedDate_yymmdd = DateFormatter.toYearMonthDate(approvedDate);
							//Writing Approved date-1
							outPW_FlatFile.write(approvedDate_yymmdd + "~|");
							Log.customer.debug("%s::approvedDate :%s",classname,approvedDate_yymmdd);
							}
							else { outPW_FlatFile.write("~|"); }


							//Writing partition name-2
							outPW_FlatFile.write(partitionName + "~|");
							Log.customer.debug("%s::partition Name:%s",classname,partitionName);

							//Writing Source -3
							if (directOrder.getDottedFieldValue("CompanyCode.SAPSource") != null){
							source = (String)directOrder.getDottedFieldValue("CompanyCode.SAPSource");
							// Commented out by Majid - Not going to write data into file
							//outPW_FlatFile.write( source+ "~|");
							Log.customer.debug("%s::source:%s",classname,source);
							}
							else {
								source = "BLANK";
								//Commented out by Majid - Not going to write data into file
								//outPW_FlatFile.write("~|");
								}

                            //Writing Accounting facility -4

                            // issue # 1054 / CR202  deafult is AccFac 02
                            /*
                            if (poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility") != null){
							String AccFac = (String)poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility");
							outPW_FlatFile.write( AccFac+ "~|");
							Log.customer.debug("%s::AccFac:%s",classname,AccFac);
							}
							else { outPW_FlatFile.write("~|"); }  */

					     // Rajat - Changes made here to set Accounting Facility as "GM" for CGM SAP

                              String AccFac = null;

                             if ( source.equalsIgnoreCase("MACH1") )

                                 {
									AccFac = "02";
							     }

						     else if ( source.equalsIgnoreCase("CBS") )

						          {
									AccFac = "86";
							      }

                             else if ( source.equalsIgnoreCase("CGM") )

                                   {
									AccFac = "GM";
							       }

					         else

                                  {
							        Log.customer.debug("%s:: Invalid SAP source",classname);
						           }

						  // Rajat - Changes end here

						    outPW_FlatFile.write( AccFac+ "~|");
						    Log.customer.debug("%s::AccFac:%s",classname,AccFac);



							 //writing PO.UniqueName-5
							if (directOrder.getFieldValue("UniqueName") != null){
							String uniqueName = (String)directOrder.getFieldValue("UniqueName");
							outPW_FlatFile.write( uniqueName+ "~|");
							Log.customer.debug("%s::uniqueName:%s",classname,uniqueName);
							}
							else { outPW_FlatFile.write("~|"); }

							//writing PH-Buying-Facility-Code-6
							if (poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility") != null){
							String BuyFacCode = (String)poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility");
							outPW_FlatFile.write( BuyFacCode+"~|");
							Log.customer.debug("%s::BuyFacCode:%s",classname,BuyFacCode);
							}
                            else { outPW_FlatFile.write("~|"); }


							//PH-Receiving-Facility-Code-7

							if (poLineItem.getDottedFieldValue("ShipTo.ReceivingFacility") != null ) {
							  String receivingFacility = poLineItem.getDottedFieldValue("ShipTo.ReceivingFacility").toString();
							   Log.customer.debug("%s::receivingFacility:%s",classname,receivingFacility);
								outPW_FlatFile.write(receivingFacility+"~|");

							}
							else {outPW_FlatFile.write("~|"); }


							// PH-Issue-Date -8
							if (directOrder.getCreateDate() !=null) {
							Date createDate =  (Date)directOrder.getCreateDate();
							String createDate_yymmdd = DateFormatter.toYearMonthDate(createDate);
							outPW_FlatFile.write(createDate_yymmdd + "~|");
							Log.customer.debug("%s::createDate.toString():%s",classname,createDate_yymmdd);
							}
							else {outPW_FlatFile.write("~|"); }


							//PH-Supplier-Code -9
							if ( poLineItem.getDottedFieldValue("SupplierLocation.UniqueName") != null){
							String  supplierCode = poLineItem.getDottedFieldValue("SupplierLocation.UniqueName").toString();
							Log.customer.debug("%s::supplierCode:%s",classname,supplierCode);
							// Added by Majid

							String formattedSuppLocCode = null;
							if(supplierCode.length()>1)
							{
								// To truncate string like VN,PI,GS or OA etc whcih is always for Suppliers for SAP partiion
								String last2Char = supplierCode.substring((supplierCode.length()-2),supplierCode.length());
								Log.customer.debug("%s::last2Char :%s",classname,last2Char);
								if(last2Char.equals("VN") ||last2Char.equals("PI") || last2Char.equals("GS") || last2Char.equals("OA") )
								{
								Log.customer.debug("%s::Inside Truncation section :%s",classname,last2Char);
								formattedSuppLocCode = supplierCode.substring(0,(supplierCode.length()-2));
								}
								else
								{
									Log.customer.debug("%s::OutSide Truncation section :%s",classname,last2Char);
									formattedSuppLocCode = supplierCode;
								}

								Log.customer.debug("%s::supplierCode after truncation:%s",classname,formattedSuppLocCode);
							}
							else
							{
								// If length of SuppLocation id is less than 2
								formattedSuppLocCode = supplierCode;
								Log.customer.debug("%s::supplierCode without truncation :%s",classname,formattedSuppLocCode);
							}

							outPW_FlatFile.write(formattedSuppLocCode + "~|");
							}
							else { outPW_FlatFile.write("~|");	}


							//PH-Product-Buyer-Code -10
							if (poLineItem.getDottedFieldValue("BuyerCode.BuyerCode") != null){
							String productBuyerCode = poLineItem.getDottedFieldValue("BuyerCode.BuyerCode").toString();
							outPW_FlatFile.write(productBuyerCode + "~|");
							Log.customer.debug("%s::productBuyerCode:%s",classname,productBuyerCode);
							}
							else {outPW_FlatFile.write("~|");  }


							//PL-Line-Seq-No -11

							if ( poLineItem.getFieldValue("NumberInCollection")!= null ) {
							String numberInCollection = poLineItem.getFieldValue("NumberInCollection").toString();
							outPW_FlatFile.write(numberInCollection + "~|");
							Log.customer.debug("%s::numberInCollection:%s",classname,numberInCollection);
							}
							else { outPW_FlatFile.write("~|");}


                                                         char rep = '?';
                                                         String itemDescription1 ="";

							//PL-Item-Description  -12
							if (poLineItem.getDottedFieldValue("Description.Description") != null) {
							// Filtering Non-ASCII characters
							String itemDescription_temp = poLineItem.getDottedFieldValue("Description.Description").toString();
							String itemDescription = itemDescription_temp.replaceAll("[^\\p{ASCII}]", "");
							itemDescription = StringUtil.replaceCharByChar(itemDescription,'\r',' ');
							itemDescription = StringUtil.replaceCharByChar(itemDescription,'\t',' ');
							itemDescription = StringUtil.replaceCharByChar(itemDescription,'\n',' ');
							itemDescription = StringUtil.replaceCharByChar(itemDescription,'\'',' ');
                                                        for (int g=0; g < itemDescription.length(); g++)
                                                         {
                                                         int ascii_val = (int) itemDescription.charAt(g);
                                                         char temp = itemDescription.charAt(g);
                                                         Log.customer.debug("Character Value of %s is %s" , itemDescription.charAt(g),ascii_val);
                                                             if ( ascii_val > 127 )
                                                               {
                                                                  Log.customer.debug("Replace %s with %s",temp,rep);
                                                               temp = rep;
                                                             }
                                                            itemDescription1 += temp;
                                                            }

							outPW_FlatFile.write(itemDescription1 + "~|");
							Log.customer.debug("%s::itemDescription:%s",classname,itemDescription1);
							}
							else { outPW_FlatFile.write("~|");	}


							//PL-UNSPSC-Code -13

							if (poLineItem.getDottedFieldValue("Description.CommonCommodityCode.UniqueName") != null) {
							String uNSPSCCode = poLineItem.getDottedFieldValue("Description.CommonCommodityCode.UniqueName").toString();
							outPW_FlatFile.write(uNSPSCCode + "~|");
							Log.customer.debug("%s::uNSPSCCode:%s",classname,uNSPSCCode);
							}
							else { outPW_FlatFile.write("~|");	}


							//PL-Supplier-Item-No  -14
							if (poLineItem.getDottedFieldValue("Description.SupplierPartNumber") != null) {
							String supplierItemNo = poLineItem.getDottedFieldValue("Description.SupplierPartNumber").toString();
							outPW_FlatFile.write(supplierItemNo + "~|");
							Log.customer.debug("%s::supplierItemNo:%s",classname,supplierItemNo);
							}
							else { outPW_FlatFile.write("~|");	}


							// PL-Hazardous-Item-Ind -15
							String isHazmatStr = null;

							if ( poLineItem.getFieldValue("IsHazmat") !=null) {
							Boolean isHazmatB = (Boolean) poLineItem.getFieldValue("IsHazmat");
							boolean isHazmatResult = BooleanFormatter.getBooleanValue(isHazmatB);
							    if (isHazmatResult == false)  isHazmatStr ="0";
							    else
							       isHazmatStr ="3";
							   // else  isHazmatStr ="0";

							    outPW_FlatFile.write(isHazmatStr + "~|");
								Log.customer.debug("%s::isHazmatStr:%s",classname,isHazmatStr);
							}
							else { outPW_FlatFile.write("0~|");	}



							//PL-Qty -16

							if ( poLineItem.getFieldValue("Quantity") != null ) {
							String pLQty = poLineItem.getFieldValue("Quantity").toString();
							outPW_FlatFile.write(pLQty + "~|");
							Log.customer.debug("%s::pLQty:%s",classname,pLQty);
							}
							else { outPW_FlatFile.write("~|");	}


							// PL-Qty-Unit-Of-Measure -17
							String pLUOM = null;
							if ( poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
							String uOMUniqueName = (String)poLineItem.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");
							//String pLUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
							Object pLUOM_Object = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");

							if(pLUOM_Object != null ) {
							pLUOM = pLUOM_Object.toString();
							if (!StringUtil.nullOrEmptyOrBlankString(pLUOM)){
							outPW_FlatFile.write(pLUOM + "~|");
							Log.customer.debug("%s::pLUOM:%s",classname,pLUOM);
							}
							else {
//								 IF CAPSUnitOfMeasure = Empty  THEN LineItems.Description.UnitOfMeasure.UniqueName
								if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueName))
								    outPW_FlatFile.write(uOMUniqueName+"~|");

							}
						}
						else {

								// IF CAPSUnitOfMeasure = NULL  THEN LineItems.Description.UnitOfMeasure.UniqueName
								if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueName))
								    outPW_FlatFile.write(uOMUniqueName+"~|");

							}
					    }
							else {	outPW_FlatFile.write("~|");	}


							// PL-Currency-Code -18
							if (directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName") != null) {
							String pLCurrencyCode = directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName").toString();
							outPW_FlatFile.write(pLCurrencyCode + "~|");
							Log.customer.debug("%s::pLCurrencyCode:%s",classname,pLCurrencyCode);
							}
							else {	outPW_FlatFile.write("~|");	}


							//PL-Unit-Price -19

							if ( poLineItem.getDottedFieldValue("Description.Price.Amount") != null) {
							String pLUnitPrice = poLineItem.getDottedFieldValue("Description.Price.Amount").toString();
							outPW_FlatFile.write(pLUnitPrice + "~|");
							Log.customer.debug("%s::pLUnitPrice:%s",classname,pLUnitPrice);
							}
							else { outPW_FlatFile.write("~|");	}

//**********************AddedBy Deepak**********
							//PL-Unit-Price-Unit-Of-Measure -20
/*
							if (poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
							String pLUnitPriceUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
							outPW_FlatFile.write(pLUnitPriceUOM + "~|");
							Log.customer.debug("%s::pLUnitPriceUOM:%s",classname,pLUnitPriceUOM);
							}
							else {	outPW_FlatFile.write("~|");	}
*/

							pLUOM = null;
							if ( poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
							String uOMUniqueNameLi = (String)poLineItem.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");
							//String pLUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
							Object pLUOM_Object = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");
							if(pLUOM_Object != null) {
							pLUOM = pLUOM_Object.toString();
							if (!StringUtil.nullOrEmptyOrBlankString(pLUOM)){
							outPW_FlatFile.write(pLUOM + "~|");
							Log.customer.debug("%s::pLUOM:%s",classname,pLUOM);
							}
							else {
                            //	 IF CAPSUnitOfMeasure = Empty  THEN LineItems.Description.UnitOfMeasure.UniqueName
								if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueNameLi))
								    outPW_FlatFile.write(uOMUniqueNameLi+"~|");
								}
							}
							else {
									//outPW_FlatFile.write("~|");
									// IF CAPSUnitOfMeasure = NULL  THEN LineItems.Description.UnitOfMeasure.UniqueName
								if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueNameLi))
								    outPW_FlatFile.write(uOMUniqueNameLi+"~|");
							}
					   		 }
							else {	outPW_FlatFile.write("~|");	}


							// PL-Account-Facility-Code -21
							if (poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility") != null){
							String AccFacCode = (String)poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility");
							outPW_FlatFile.write( AccFacCode+ "~|");
							Log.customer.debug("%s::AccFacCode:%s",classname,AccFacCode);
							}
							else { outPW_FlatFile.write("~|"); }

							//PL control account -22
						     if (directOrder.getDottedFieldValue("CompanyCode.UniqueName") != null){
							String ConAcct = (String)directOrder.getDottedFieldValue("CompanyCode.UniqueName");
							outPW_FlatFile.write( ConAcct+ "~|");
							Log.customer.debug("%s::ConAcct:%s",classname,ConAcct);
							}
							else { outPW_FlatFile.write("~|"); }


							// PL-Sub-Acct -23
							if (poLineItem.getDottedFieldValue("ShipTo.UniqueName") != null ) {
							  String PLSubAcct = poLineItem.getDottedFieldValue("ShipTo.UniqueName").toString();
							   Log.customer.debug("%s::PLSubAcct:%s",classname,PLSubAcct);
								outPW_FlatFile.write(PLSubAcct+"~|");

							}
							else {outPW_FlatFile.write("~|"); }


							//PL-Sub-Sub-Acct -24
							if ( sa.getFieldValue("CostCenterText") != null) {
							String pLSubSubAcct = sa.getFieldValue("CostCenterText").toString();
							outPW_FlatFile.write(pLSubSubAcct + "~|");
							Log.customer.debug("%s::pLSubSubAcct:%s",classname,pLSubSubAcct);
							}
							else {outPW_FlatFile.write("~|"); }


							// PL-Expense-Account -25

							if (sa.getFieldValue("GeneralLedgerText") != null) {
							String pLExpenseAccount = sa.getFieldValue("GeneralLedgerText").toString();
							outPW_FlatFile.write(pLExpenseAccount + "~|");
							Log.customer.debug("%s::pLExpenseAccount:%s",classname,pLExpenseAccount);
							}
							else { outPW_FlatFile.write("~|");	}

							//PL-Order-Number -26
							// Added by Majid


							if(source.equals("MACH1"))
							{
								Log.customer.debug("DW PO Push : Inside Mach1 "+source);
							if (sa.getFieldValue("WBSElementText") != null)
								{
								String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
								if(pLOrderNumber.length()>10)
								{
									Log.customer.debug("DW PO Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
									outPW_FlatFile.write(pLOrderNumber.substring(0,10) + "~|");
								}
								else
								{
									outPW_FlatFile.write(pLOrderNumber + "~|");
								}
								Log.customer.debug("%s::pLOrderNumber:%s",classname,pLOrderNumber);
								}
								else
								{
									Log.customer.debug("DW PO Push : Inside Mach1 : WBSElementText is null ");
									outPW_FlatFile.write("~|");
								}

							}
							else if(source.equals("CBS"))
							{
								Log.customer.debug("DW PO Push : Inside CBS "+source);
								if (sa.getFieldValue("InternalOrderText") != null)
								{
								String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
								if(pLOrderNumber.length()>10)
								{
									Log.customer.debug("DW PO Push : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
									outPW_FlatFile.write(pLOrderNumber.substring(0,10) + "~|");
								}
								else
								{
									outPW_FlatFile.write(pLOrderNumber + "~|");
								}
								Log.customer.debug("%s::pLOrderNumber:%s",classname,pLOrderNumber);
								}
								else
								{
									Log.customer.debug("DW PO Push : Inside Mach1 : IO is null ");
									outPW_FlatFile.write("~|");
								}
							}

						 //Rajat - Changes made here to include CGM SAP.

							else if(source.equalsIgnoreCase("CGM"))
							 {
								Log.customer.debug("CATSAPDWPOPush: Inside CGM "+source);

								if (sa.getFieldValue("InternalOrderText") != null)
								 {
									String pLOrderNumber = sa.getFieldValue("InternalOrderText").toString();
									if(pLOrderNumber.length()>10)
									 {
										Log.customer.debug("CATSAPDWPOPush: pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
										outPW_FlatFile.write(pLOrderNumber.substring(0,10) + "~|");

									 }
								    else
									 {
										outPW_FlatFile.write(pLOrderNumber + "~|");

									 }
								 }


								else if (sa.getFieldValue("WBSElementText") != null)

								 {
									String pLOrderNumber = sa.getFieldValue("WBSElementText").toString();
									if(pLOrderNumber.length()>10)
										{
											Log.customer.debug("CATSAPDWPOPush : pLOrderNumber has more than 10 chars: Needs to trucnate "+pLOrderNumber);
											outPW_FlatFile.write(pLOrderNumber.substring(0,10) + "~|");

										}
									else
										{
											outPW_FlatFile.write(pLOrderNumber + "~|");

										}

								 }

								 else
								 {
								 		Log.customer.debug("DW PO Push : Inside CGM : Both IOText and WBSElementText are null ");
								 		outPW_FlatFile.write("~|");

								 }


							}

			             // Rajat - Changes made to include CGM SAP end here
							else
							{
								Log.customer.debug("DW PO Push : Invalid SAP Source "+source);
								outPW_FlatFile.write("~|");
							}


							  // outPW_FlatFile.write("~|");
							  // Log.customer.debug("pLOrderNumber");


							//PL-Misc-Qualifier -27

							 outPW_FlatFile.write("~|");
							 Log.customer.debug("pLMiscQualifier");


							// Expense account desc -28

							if (poLineItem.getDottedFieldValue("AccountCategory") != null) {
								String ExpAcctDesc = (String) poLineItem.getDottedFieldValue("AccountCategory.Description.PrimaryString");
							outPW_FlatFile.write(ExpAcctDesc + "~|");
							Log.customer.debug("%s::ExpAcctDesc:%s",classname,ExpAcctDesc);
							}
							else { outPW_FlatFile.write("~|");	}


							//Writing Com buy code -29
							Log.customer.debug("ComBuyCode");
							//outPW_FlatFile.write("~|");
							// Added Buyer Code - Majid
							if (poLineItem.getDottedFieldValue("BuyerCode.BuyerCode") != null){
								String productBuyerCode1 = poLineItem.getDottedFieldValue("BuyerCode.BuyerCode").toString();
								outPW_FlatFile.write(productBuyerCode1 + "~|");
								Log.customer.debug("%s::productBuyerCode1:%s",classname,productBuyerCode1);
								}
								else {outPW_FlatFile.write("~|");  }



						   //Writing Source facility code -30
					       //outPW_FlatFile.write("~|");
                           Log.customer.debug("SourceFacilitycode");

//                         // Added Accounting Facility Code - Majid
                           if (poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility") != null){
   							String AccFacCode1 = (String)poLineItem.getDottedFieldValue("Requisition.Requester.AccountingFacility");
   							outPW_FlatFile.write( AccFacCode1+ "~|");
   							Log.customer.debug("%s::AccFacCode:%s",classname,AccFacCode1);
   							}
   							else { outPW_FlatFile.write("~|"); }
   							// Writing IsAdHoc - catalog or non catalog - 31
							// Issue #269 - Dharshan
							isAdHocBoolean = true;
							isAdHoc = null;
							if (poLineItem.getDottedFieldValue("IsAdHoc") != null) {
								isAdHoc = (Boolean) poLineItem.getDottedFieldValue("IsAdHoc");
								isAdHocBoolean = BooleanFormatter.getBooleanValue(isAdHoc);
								Log.customer.debug("%s::isAdHocBoolean:%s",classname,isAdHocBoolean);
								if(isAdHocBoolean == false){
									outPW_FlatFile.write("Catalog Item:");
								}
								else
								{
								    Log.customer.debug("%s::isAdHocBoolean is true, not catalog item",classname);
							    }
							}
							else Log.customer.debug("%s::isAdHocBoolean is null, leave blank",classname);

                           // Change the line after printing each record
                           outPW_FlatFile.write("\n");

							// Update DWPOFlag in DO based on config
							if(isCompletedFlgUpdate) {
								Log.customer.debug("%s::usPOFlagAction is Completed setting DWPOFlag ...", classname);
								directOrder.setFieldValue("DWPOFlag", "Processing");
							}
							else {
								Log.customer.debug("%s::usPOFlagAction is None no action req DWPOFlag ...", classname);
								continue;
							}
							  }

                                   splitAccounting = false;
								  }
							}
						 }
							}
                           else {
							Log.customer.debug("DO Line Item Count 0 ");
					       }

					      }


                        catch (Exception e) {
                            Log.customer.debug("Error in if loop " + e.toString());
                            throw e;
                            }
                        }


                    Log.customer.debug("Ending DWPOPush program .....");
                    }

                    // Added  by Deepak
                    count++;
					if(count == 200)
				   {
						Log.customer.debug("**********Commiting PO Records*******  ",count);
						Base.getSession().transactionCommit();
						count = 0;
					}
				}
                   Base.getSession().transactionCommit();
                   Log.customer.debug("  Transaction Commited ********  ");
				   String updateQuery = null;
				   updateQuery = "update  ariba.purchasing.core.DirectOrder set DWPOFlag = 'Completed' where DWPOFlag = 'Processing''";

								   Log.customer.debug("  update query ********  "+updateQuery);

								   AQLUpdate aqlqueryUpdate = null;
								   AQLOptions optionsUpdate = null;
								   aqlqueryUpdate = AQLUpdate.parseUpdate("update  ariba.purchasing.core.DirectOrder set DWPOFlag = 'Completed' where DWPOFlag = 'Processing'");

								   Log.customer.debug("  update query ********  "+aqlqueryUpdate);

								   optionsUpdate = new AQLOptions(p);
								   optionsUpdate.setSQLAccess(AQLOptions.AccessReadWrite);
								   optionsUpdate.setClassAccess(AQLOptions.AccessReadWrite);
					   int updateResults = Base.getService().executeUpdate(aqlqueryUpdate, optionsUpdate);
					   Log.customer.debug("Number of records updated**************** " + updateResults);
					   Base.getSession().transactionCommit();
                   // Ends here
                }

            catch (Exception e) {
                Log.customer.debug(e.toString());
                throw e;
                }

            if (outPW_FlatFile != null) {
                outPW_FlatFile.flush();
                outPW_FlatFile.close();

                }
	      }

        catch (Exception e) {
            Log.customer.debug(e.toString());
//          add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			/*
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("CATSAPDWPOPush_FlatFile Task Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			*/
			//mailSubject = "CATUSDWPOPush_FlatFile Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , classname);
			new ScheduledTaskException("Error : " + e.toString(), e);
            throw new ScheduledTaskException("Error : " + e.toString(), e);
            }
        finally {
        	results.close();
        	outPW_FlatFile.flush();
            outPW_FlatFile.close();

            /*

			//Change made by Soumya begins

			Log.customer.debug("CATSAPDWPOPush_FlatFile:Starting Copying the flat file to Archive ");
			CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
			Log.customer.debug("CATSAPDWPOPush_FlatFile:Completed Copying the flat file to Archive ");

			*/

			try
			{
				Log.customer.debug("CATSAPDWPOPush_FlatFile:Changing file permission of Data file.");
				Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
				Log.customer.debug("CATSAPDWPOPush_FlatFile:Changed file permission of Data file.");
			}catch (IOException e1) {
				Log.customer.debug("Error in changing Permission. "+ e1.toString());
			}

			//Change made by Soumya end

			Log.customer.debug("%s: Inside Finally ", classname);

			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", classname);
			message.append("\n");
			endTime = DateFormatter.getStringValue(new Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			//message.append("No. of POs to be pushed : "+ resultCount);
			message.append("\n");
			//message.append("No. of Lines successfully write : "+ pushedCount);
			message.append("\n");
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , classname);

			// Sending email
			//CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "DWPushNotify");
			message = null;
			//pushedCount =0;
			//resultCount =0;
	  }
        }

    public CATSAPDWPOPush_FlatFile()  {

        }
    }
