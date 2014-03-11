/*******************************************************************************************************************************************
    Creator: Kannan PGS
    Description: Writing the fileds from the PO to flat file
    ChangeLog:
    Date        Name                       		History
    22/08/2008 	Deepak Sharma	   				Changing: CAPSUOM
    10/24/2009 	PGS kannan                   	//26 PL-Expense-Account-Desc IF PARTITION = EZOPEN THEN blank
	04/13/2012 	Vikram							CR216: Send POs with status, Ordered, Receiving, Received to PDW
	15/06/2012 	Dharshan   Issue #269	 		IsAdHoc - catalog or non catalog,
	01/23/2014	IBM Parita Shah					SpringRelease_RSD (FDD_129_4.3 / TDD_129_1.3) PO Line Item Unit Price should always go as Positive
    --------------------------------------------------------------------------------------------------------------
*******************************************************************************************************************************************/
package config.java.schedule;
import java.io.File;
//change begin by Soumya
import java.io.IOException;
//change end by Soumya
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.DirectOrder;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import config.java.common.CatEmailNotificationUtil;
//change made by Soumya begins
import config.java.schedule.util.CATFaltFileUtil;
//change made by Soumya ends

// Start :  SpringRelease_RSD 129 (FDD_129_4.3 / TDD_129_1.3)
import ariba.util.formatter.BigDecimalFormatter;
import java.math.BigDecimal;
// End :  SpringRelease_RSD 129 (FDD_129_4.3 / TDD_129_1.3)

public class CATEZOPENDWPOPush_FlatFile  extends ScheduledTask  {
    private Partition p;
    private String query;
    private String classname = "CATEZOPENDWPOPush_FlatFile";
    private Calendar calendar = new GregorianCalendar();
	private java.util.Date date = calendar.getTime();
	private DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
	private String fileExt = ""+ dateFormat.format(date);
    private String flatFilePath = "/msc/arb9r1/downstream/catdata/DW/MSC_DW_PO_PUSH_EZOPEN_"+fileExt+".txt";

    //change made by soumya begins

    private String archiveFileDataPath = "/msc/arb9r1/downstream/catdata/DW/archive/MSC_DW_PO_PUSH_EZOPEN_ARCHIVE_"+fileExt+".txt";

    //change made by soumya ends

    private int partitionNumber;
    private FastStringBuffer message = null;
    String mailSubject = null;
    private int resultCount, pushedCount;
    private String startTime, endTime;
    private String EzopenPOFlagAction = null;
    private boolean isCompletedFlgUpdate = false;
    private AQLResultCollection results = null;
    private File out_FlatFile = null;
    private PrintWriter outPW_FlatFile =null;
    private boolean splitAccounting = true;
	private Boolean isAdHoc;
	private boolean isAdHocBoolean;

	  /*
	   * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	   * Reason		: Along with 9r Server path might get changed.
	   */
		public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
			super.init(scheduler, scheduledTaskName, arguments);
			for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
				String key = (String) e.next();
				if (key.equals("FlatFilePath")) {
					flatFilePath = (String) arguments.get(key);
					flatFilePath = flatFilePath + fileExt + ".txt";
					Log.customer.debug("CATUSDWPOPush_FlatFile : FlatFilePath "+ flatFilePath);
				}
			}
		}
	  /*
	   * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
	   * Reason		: Along with 9r Server path might get changed.
	   */

    public void run() throws ScheduledTaskException  {
        Log.customer.debug("%s::Start of CATUSDWPOPush_FlatFile task ...", classname);
        //      Read the USPOFlagAction info from cat.dwAction.util
        EzopenPOFlagAction = ResourceService.getString("cat.dwAction", "EZOPENPOFlagAction");
        Log.customer.debug("%s::usPOFlagAction ...", EzopenPOFlagAction, classname);
         if (EzopenPOFlagAction!=null && ! EzopenPOFlagAction.equals("None") && EzopenPOFlagAction.indexOf("not found")==-1)
               isCompletedFlgUpdate = false;
         if ( EzopenPOFlagAction!=null && EzopenPOFlagAction.equals("Completed")&& EzopenPOFlagAction.indexOf("not found")==-1)
              isCompletedFlgUpdate = true;
        startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
        try {
            File out_FlatFile = new File(flatFilePath);
            Log.customer.debug("%s::FilePath:%s", classname, out_FlatFile);
            if (!out_FlatFile.exists()) {
                Log.customer.debug("%s::if file does not exit then create 1", classname);
                out_FlatFile.createNewFile();
            }
            Log.customer.debug("%s::Creating aprint writer obj:", classname);
            outPW_FlatFile = new PrintWriter(IOUtil.bufferedOutputStream(out_FlatFile), true);
            p = Base.getSession().getPartition();
            message = new FastStringBuffer();
            mailSubject = "CATEZOPENDWPOPush_FlatFile Task Completion Status - Completed Successfully";
//try 2
            try {
            query = "select from ariba.purchasing.core.DirectOrder where DWPOFlag = 'InProcess' and StatusString in ('Ordered','Receiving','Received')";
            Log.customer.debug(query);
            AQLQuery aqlquery = null;
            AQLOptions options = null;
            ariba.purchasing.core.DirectOrder directOrder = null;
            aqlquery = AQLQuery.parseQuery(query);
            options = new AQLOptions(p);
            results = Base.getService().executeQuery(aqlquery, options);
            resultCount = results.getSize();
            if (results.getErrors() != null)
                    Log.customer.debug("ERROR GETTING RESULTS in Results");
//try 3
try {
            while (results.next()) {
                    directOrder = (DirectOrder)(results.getBaseId("DirectOrder").get());
                    if (directOrder != null) {
                            BaseVector po_LineItems = (BaseVector)directOrder.getLineItems();
                            Log.customer.debug("%s::POLineItem:%s",classname,po_LineItems);
                            int lineCount = directOrder.getLineItemsCount();
                            Log.customer.debug("%s::Line Item count for PO:%s ",classname,lineCount);
                            if ( lineCount > 0) {
                            //Partition
                            partitionNumber = directOrder.getPartitionNumber();
                            Log.customer.debug("%s::DO_Partition:%s", classname, partitionNumber);
                            String partition_Name = null;
                            if (partitionNumber == 4) {
                                partition_Name = "ezopen";
                             }
                            for (int i =0; i<po_LineItems.size();i++){
								splitAccounting = true;
                            LineItem poLineItem = (LineItem)po_LineItems.get(i);
                            SplitAccountingCollection sacol =(SplitAccountingCollection) poLineItem.getFieldValue("Accountings");
                                    if (sacol != null && sacol.getSplitAccountings() != null) {
                                            BaseVector sas = sacol.getSplitAccountings();
                                            for (int j =0; j <sas.size();j++){
                                               SplitAccounting sa  = (SplitAccounting)sas.get(j);
                            if(splitAccounting)
							{
                            //******     File write Start ********
                            // 0 PO Approved date
                            if (directOrder.getFieldValue("ApprovedDate") != null) {
                            Date approvedDate = (Date)  directOrder.getFieldValue("ApprovedDate");
                            String approvedDate_yymmdd = DateFormatter.toYearMonthDate(approvedDate);
                            outPW_FlatFile.write(approvedDate_yymmdd + "~|");
                            Log.customer.debug("%s::approvedDate :%s",classname,approvedDate_yymmdd);
                            }
                            else { outPW_FlatFile.write("~|"); }
                            // 1  Partition
                            outPW_FlatFile.write(partition_Name + "~|");
                            Log.customer.debug("%s::partition_Name:%s",classname,partition_Name);
                            // 2 Source-Of-Record Facility  IF PARTITION = EZOPEN THEN LineItems. Accountings.SplitAccountings.Facility.UniqueName
								outPW_FlatFile.write("36" + "~|");
/*
                            if ( sa.getDottedFieldValue("Facility.UniqueName") != null){
                            String sourceOfRecord = (String)sa.getDottedFieldValue("Facility.UniqueName");
                            outPW_FlatFile.write(sourceOfRecord + "~|");
                             Log.customer.debug("%s::Source-Of-Record Facility:%s",classname,sourceOfRecord);
                            }
                            else {  outPW_FlatFile.write("~|");    }
*/
                             //3 PO.UniqueName
                            if (directOrder.getFieldValue("UniqueName") != null) {
                            String uniqueName = (String)directOrder.getFieldValue("UniqueName");
                            outPW_FlatFile.write( uniqueName+ "~|");
                            Log.customer.debug("%s::uniqueName:%s",classname,uniqueName);
                            }
                            else { outPW_FlatFile.write("~|"); }
                            //4 PH-Buying-Facility-Code  EZOPEN THEN LineItems. Accountings. SplitAccountings. Facility .UniqueName
                            outPW_FlatFile.write("36" + "~|");
                           /* if (sa.getDottedFieldValue("Facility.UniqueName") != null) {
                            String buyingFacilityCode = (String)sa.getDottedFieldValue("Facility.UniqueName");
                            if (!StringUtil.nullOrEmptyOrBlankString(buyingFacilityCode)) {
                            outPW_FlatFile.write(buyingFacilityCode + "~|");
                            Log.customer.debug("%s::PH-Buying-Facility-Code:%s",classname,buyingFacilityCode);
                            }
                            }
                            else {
                                outPW_FlatFile.write("~|");
                                //Log.customer.debug("%s::PH-Buying-Facility-Code Is NULL :%s",classname,buyingFacilityCode);
                            }  */
                            //5 PH-Receiving-Facility-Code IF Partition = EZOPEN SiteFacility.UniqueName
                            //String receivingFacility = directOrder.getDottedFieldValue("SiteFacility.UniqueName").toString();
                           /*  if (  directOrder.getDottedFieldValue("LineItems.ShipTo.ReceivingFacility")!= null) {
							    String receivingFacility = directOrder.getDottedFieldValue("LineItems.ShipTo.ReceivingFacility").toString();
                            Log.customer.debug("%s::receivingFacility:%s",classname,receivingFacility);
                            outPW_FlatFile.write(receivingFacility+"~|");
                            } //else {  outPW_FlatFile.write("~|"); }
                            else {  outPW_FlatFile.write("AA" + "~|"); }
                            */
                            if (poLineItem.getDottedFieldValue("ShipTo.ReceivingFacility") != null ) {
								  String receivingFacility = poLineItem.getDottedFieldValue("ShipTo.ReceivingFacility").toString();
								   Log.customer.debug("%s::receivingFacility:%s",classname,receivingFacility);
								  // Writing ShipTo.ReceivingFacility -- MC
								   if(!StringUtil.nullOrEmptyOrBlankString(receivingFacility)){
									outPW_FlatFile.write(receivingFacility+"~|");
								   }
								}
							else {



								//outPW_FlatFile.write("AA"+"~|");
								//*************DW Design change Adding Accounting facility whenever Receiving Facility is null*******
								 //19 PL-Account-Facility-Code IF Partition = EZOPEN THEN LineItems. Accountings. SplitAccountings.Account.UniqueName
								String pLAccountFacilityCodeforReceiving = null;
								//if (sa.getDottedFieldValue("Account.UniqueName") != null) {
								//    pLAccountFacilityCodeforReceiving = sa.getDottedFieldValue("Account.UniqueName").toString();
								if (sa.getFieldValue("AccountingFacility") != null) {
								   pLAccountFacilityCodeforReceiving = sa.getFieldValue("AccountingFacility").toString();
									outPW_FlatFile.write(pLAccountFacilityCodeforReceiving + "~|");
									Log.customer.debug("%s::pLAccountFacilityCodeforReceiving:%s",classname,pLAccountFacilityCodeforReceiving);
								}
                            else {  outPW_FlatFile.write("~|");     }




								}
                            //6 PH-Issue-Date  CreateDate
                            if (directOrder.getCreateDate() != null){
                            Date createDate =  (Date)directOrder.getCreateDate();
                            String createDate_yymmdd = DateFormatter.toYearMonthDate(createDate);
                            outPW_FlatFile.write(createDate_yymmdd + "~|");
                            Log.customer.debug("%s::createDate.toString():%s",classname,createDate_yymmdd);
                            }
                            else {  outPW_FlatFile.write("~|"); }
                            //7 PH-Supplier-Code
                            if ( directOrder.getDottedFieldValue("SupplierLocation.UniqueName") != null){
                            String  supplierCode = directOrder.getDottedFieldValue("SupplierLocation.UniqueName").toString();
                            Log.customer.debug("%s::supplierCode:%s",classname,supplierCode);
                            outPW_FlatFile.write(supplierCode + "~|");
                            }
                            else {  outPW_FlatFile.write("~|"); }
                            //8 PH-Product-Buyer-Code  LineItems.BuyerCode.BuyerCode
                            if (poLineItem.getDottedFieldValue("BuyerCode.BuyerCode") != null){
                            String productBuyerCode = poLineItem.getDottedFieldValue("BuyerCode.BuyerCode").toString();
                            outPW_FlatFile.write(productBuyerCode + "~|");
                            Log.customer.debug("%s::productBuyerCode:%s",classname,productBuyerCode);
                            }
                            else {  outPW_FlatFile.write("~|"); }
                            //9 PL-Line-Seq-No LineItems .NumberInCollection
                            if ( poLineItem.getFieldValue("NumberInCollection")!= null ) {
                            String numberInCollection = poLineItem.getFieldValue("NumberInCollection").toString();
                            outPW_FlatFile.write(numberInCollection + "~|");
                            Log.customer.debug("%s::numberInCollection:%s",classname,numberInCollection);
                            }
                            else {  outPW_FlatFile.write("~|"); }
                            //10 PL-Item-Description  LineItems. Description. Description
                            if (poLineItem.getDottedFieldValue("Description.Description") != null) {
                            String itemDescription = poLineItem.getDottedFieldValue("Description.Description").toString();
                            itemDescription = StringUtil.replaceCharByChar(itemDescription,'\r',' ');
                            itemDescription = StringUtil.replaceCharByChar(itemDescription,'\t',' ');
                            itemDescription = StringUtil.replaceCharByChar(itemDescription,'\n',' ');
                            outPW_FlatFile.write(itemDescription + "~|");
                            Log.customer.debug("%s::itemDescription:%s",classname,itemDescription);
                            }
                            else { outPW_FlatFile.write("~|");  }
                            //11 PL-UNSPSC-Code  LineItems. Description.CommonCommodityCode.UniqueName
                            if (poLineItem.getDottedFieldValue("Description.CommonCommodityCode.UniqueName") != null) {
                            String uNSPSCCode = poLineItem.getDottedFieldValue("Description.CommonCommodityCode.UniqueName").toString();
                            outPW_FlatFile.write(uNSPSCCode + "~|");
                            Log.customer.debug("%s::uNSPSCCode:%s",classname,uNSPSCCode);
                            }
                            else { outPW_FlatFile.write("~|");  }
                            //12 PL-Supplier-Item-No   LineItems. Description.SupplierPartNumber
                            if (poLineItem.getDottedFieldValue("Description.SupplierPartNumber") !=null){
                            String supplierItemNo = poLineItem.getDottedFieldValue("Description.SupplierPartNumber").toString();
                            outPW_FlatFile.write(supplierItemNo + "~|");
                            Log.customer.debug("%s::supplierItemNo:%s",classname,supplierItemNo);
                            }
                            else { outPW_FlatFile.write("~|");  }
                            //13 PL-Hazardous-Item-Ind
                            String isHazmatStr = null;
                            if ( poLineItem.getFieldValue("IsHazmat") !=null) {
                            Boolean isHazmatB = (Boolean) poLineItem.getFieldValue("IsHazmat");
                            boolean isHazmatResult = BooleanFormatter.getBooleanValue(isHazmatB);
                                if (isHazmatResult == false)
                                    isHazmatStr ="0";
                                else
                                   isHazmatStr ="3";
                                //else
                                //  isHazmatStr ="0";
                            outPW_FlatFile.write(isHazmatStr + "~|");
                            Log.customer.debug("%s::isHazmatStr:%s",classname,isHazmatStr);
                            }
                            else { outPW_FlatFile.write("0~|");  }
                            //14 PL-Qty
                            if ( poLineItem.getFieldValue("Quantity") != null ) {
                            String pLQty = poLineItem.getFieldValue("Quantity").toString();
                            outPW_FlatFile.write(pLQty + "~|");
                            Log.customer.debug("%s::pLQty:%s",classname,pLQty);
                            }
                            else {outPW_FlatFile.write("~|");   }
                            //15 PL-Qty-Unit-Of-Measure  LineItems. Description. UnitOfMeasure. CAPSUnitOfMeasure
                            String pLUOM = null;
                            if ( poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
                            	String uOMUniqueName = (String)poLineItem.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");
                            //String pLUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
                            Object pLUOM_Object = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");
                            if(pLUOM_Object != null) {
							pLUOM = pLUOM_Object.toString();
							if (!StringUtil.nullOrEmptyOrBlankString(pLUOM)){
                            outPW_FlatFile.write(pLUOM + "~|");
                            Log.customer.debug("%s::pLUOM:%s",classname,pLUOM);
                            }
                            else {
                            	// IF CAPSUnitOfMeasure = Empty  THEN LineItems.Description.UnitOfMeasure.UniqueName
 							   if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueName))
 								outPW_FlatFile.write(uOMUniqueName+"~|");
								 }
						   }
						   else {
						   		//outPW_FlatFile.write("~|");
						   		// IF CAPSUnitOfMeasure = NULL  THEN LineItems.Description.UnitOfMeasure.UniqueName
							   if (!StringUtil.nullOrEmptyOrBlankString(uOMUniqueName))
								outPW_FlatFile.write(uOMUniqueName+"~|");
								 }
					   }
                            else {  outPW_FlatFile.write("~|"); }
                            //16 PL-Currency-Code
                            if (directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName") != null) {
                            String pLCurrencyCode = directOrder.getDottedFieldValue("TotalCost.Currency.UniqueName").toString();
                            outPW_FlatFile.write(pLCurrencyCode + "~|");
                            Log.customer.debug("%s::pLCurrencyCode:%s",classname,pLCurrencyCode);
                            }
                            else { outPW_FlatFile.write("~|");  }


                            //17 PL-Unit-Price Description.Price.Amount
                            // Start :  SpringRelease_RSD 129 (FDD_129_4.3 / TDD_129_1.3)
							BigDecimal plUnitPrice=null;
						 if ( poLineItem.getDottedFieldValue("Description.Price.Amount") != null)
						 {
							plUnitPrice = (BigDecimal)poLineItem.getDottedFieldValue("Description.Price.Amount");
							Log.customer.debug("Unit Price without abs method "+plUnitPrice);
							plUnitPrice = plUnitPrice.abs();
							//String pLUnitPrice =  BigDecimalFormatter.getStringValue(lipd.getPrice().getAmount().abs());
							Log.customer.debug("Unit Price with abs method "+plUnitPrice);
							String strUnitPrice = BigDecimalFormatter.getStringValue(plUnitPrice);
							outPW_FlatFile.write(strUnitPrice + "~|");
							Log.customer.debug("%s::pLUnitPrice:%s",classname,strUnitPrice);
						 }
							// End :  SpringRelease_RSD 129 (FDD_129_4.3 / TDD_129_1.3)
                            else {  outPW_FlatFile.write("~|"); }



                            //18 PL-Unit-Price-Unit-Of-Measure   LineItems.Description.UnitOfMeasure.CAPSUnitOfMeasure
                            /*if (poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
                            String pLUnitPriceUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
                            outPW_FlatFile.write(pLUnitPriceUOM + "~|");
                            Log.customer.debug("%s::pLUnitPriceUOM:%s",classname,pLUnitPriceUOM);
                            }
                            else {  outPW_FlatFile.write("~|"); }
                            */

                            //*******************************Added by Deepak

							String pLUnitPriceUOM = null;
							if ( poLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null) {
								String uOMUniqueNameLi = (String)poLineItem.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");
							//String pLUOM = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure").toString();
							Object pLUnitPriceUOM_Object = poLineItem.getDottedFieldValue("Description.UnitOfMeasure.CAPSUnitOfMeasure");
							if(pLUnitPriceUOM_Object != null) {
							pLUnitPriceUOM = pLUnitPriceUOM_Object.toString();
							if (!StringUtil.nullOrEmptyOrBlankString(pLUnitPriceUOM)){
							outPW_FlatFile.write(pLUnitPriceUOM + "~|");
							Log.customer.debug("%s::pLUOM:%s",classname,pLUnitPriceUOM);
							}
							else {
								// IF CAPSUnitOfMeasure = Empty  THEN LineItems.Description.UnitOfMeasure.UniqueName
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
                            else {  outPW_FlatFile.write("~|"); }

                            //*******************************




                            //19 PL-Account-Facility-Code IF Partition = EZOPEN THEN LineItems. Accountings. SplitAccountings.Account.UniqueName
                            String pLAccountFacilityCode = null;
                            //if (sa.getDottedFieldValue("Account.UniqueName") != null) {
                            //    pLAccountFacilityCode = sa.getDottedFieldValue("Account.UniqueName").toString();
                            if (sa.getFieldValue("AccountingFacility") != null) {
                               pLAccountFacilityCode = sa.getFieldValue("AccountingFacility").toString();
                                outPW_FlatFile.write(pLAccountFacilityCode + "~|");
                                Log.customer.debug("%s::pLAccountFacilityCode:%s",classname,pLAccountFacilityCode);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                            //20 PL-Control-Account  LineItems. Accountings. SplitAccountings.CostCenter.CostCenterCode
                            String pLControlAccount = null;
                           // if (sa.getDottedFieldValue("CostCenter.CostCenterCode") != null) {
                           //       pLControlAccount = sa.getDottedFieldValue("CostCenter.CostCenterCode").toString();
                            if (sa.getFieldValue("Department") != null) {
								  pLControlAccount = sa.getFieldValue("Department").toString();
                                  outPW_FlatFile.write(pLControlAccount + "~|");
                                  Log.customer.debug("%s::pLControlAccount:%s",classname,pLControlAccount);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                            //21 PL-Sub-Acct  IF Partition = EZOPEN THEN LineItems. Accountings. SplitAccountings. SubAccount.UniqueName
                            String pLSubAcct = "";
                           if (sa.getDottedFieldValue("SubAccount.UniqueName") != null) {
                              pLSubAcct=sa.getDottedFieldValue("SubAccount.UniqueName").toString();
                            // if (sa.getFieldValue("Division") != null) {
                            //    pLSubAcct=sa.getFieldValue("Division").toString();
                                outPW_FlatFile.write(pLSubAcct + "~|");
                                Log.customer.debug("%s::pLSubAcct:%s",classname,pLSubAcct);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                            //22 PL-Sub-Sub-Acct IF Partition = EZOPEN THEN ""
                            String pLSubSubAcct = "";
                            outPW_FlatFile.write(pLSubSubAcct + "~|");
                            Log.customer.debug("%s::pLSubSubAcct:%s",classname,pLSubSubAcct);
						/*//21 PL-Sub-Acct  IF Partition = EZOPEN THEN LineItems. Accountings. SplitAccountings. SubAccount.UniqueName
										String pLSubSubAcct = "";
										//if (sa.getDottedFieldValue("SubAccount.UniqueName") != null) { Division
										//    pLSubAcct=sa.getDottedFieldValue("SubAccount.UniqueName").toString();
								 if (sa.getFieldValue("Section") != null) {
									pLSubSubAcct = sa.getFieldValue("Section").toString();
									outPW_FlatFile.write(pLSubSubAcct + "~|");
									Log.customer.debug("%s::pLSubAcct:%s",classname,pLSubSubAcct);
								}
								else {  outPW_FlatFile.write("~|");     }
						*/
                            //23 PL-Expense-Account IF Partition = EZOPEN THEN LineItems. Accountings. SplitAccountings.Account.AccountCode
                            String pLExpenseAccount = "";
                           // if (sa.getDottedFieldValue("Account.AccountCode")!= null){
                           //   pLExpenseAccount = sa.getDottedFieldValue("Account.AccountCode").toString();
                           if (sa.getFieldValue("ExpenseAccount")!= null){
                             pLExpenseAccount = sa.getFieldValue("ExpenseAccount").toString();
                              outPW_FlatFile.write(pLExpenseAccount + "~|");
                              Log.customer.debug("%s::pLExpenseAccount:%s",classname,pLExpenseAccount);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                            //24 PL-Order-Number IF Partition = EZOPEN THEN LineItems. Accountings.SplitAccountings.Order
                            if( sa.getDottedFieldValue("Order")!= null) {
                            String pLOrderNumber = sa.getDottedFieldValue("Order").toString();
                            outPW_FlatFile.write(pLOrderNumber + "~|");
                            Log.customer.debug("%s::pLOrderNumber:%s",classname,pLOrderNumber);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                            //25 PL-Misc-Qualifier IF Partition = EZOPEN THEN LineItems. Accountings.SplitAccountings.Misc
                            if (sa.getDottedFieldValue("Misc") != null) {
                            String pLMiscQualifier = sa.getDottedFieldValue("Misc").toString();
                            outPW_FlatFile.write(pLMiscQualifier + "~|");
                            Log.customer.debug("%s::pLMiscQualifier:%s",classname,pLMiscQualifier);
                            }
                            else {  outPW_FlatFile.write("~|");     }
                           //26 PL-Expense-Account-Desc IF PARTITION = EZOPEN THEN blank ~|
                             outPW_FlatFile.write("~|");

                            //27 PL-Commodity-Buyer-Code  Need get Requirement
                            if (poLineItem.getDottedFieldValue("BuyerCode.BuyerCode") != null) {
                            String pLCommodityBuyerCode = poLineItem.getDottedFieldValue("BuyerCode.BuyerCode").toString();
                            outPW_FlatFile.write(pLCommodityBuyerCode + "~|");
                            Log.customer.debug("%s::pLCommodityBuyerCode:%s",classname,pLCommodityBuyerCode);
                            }
                            else {  outPW_FlatFile.write("~|");     }
//                          28 PL-Central-Source-Facility-Code IF PARTITION = MFG1 THEN LineItems. Accountings. SplitAccountings. Facility .UniqueName
							outPW_FlatFile.write("36 ~|");
                           /* String pLCentralSourceFacilityCode = null;
                            if (sa.getDottedFieldValue("Facility.UniqueName") != null) {
                                  pLCentralSourceFacilityCode = sa.getDottedFieldValue("Facility.UniqueName").toString();
                                  outPW_FlatFile.write(pLCentralSourceFacilityCode);
                                    Log.customer.debug("%s::pLCentralSourceFacilityCode:%s",classname,pLCentralSourceFacilityCode);
                            }
                            else {  outPW_FlatFile.write("~|");     }
*/
							//29 IsAdHoc - catalog or non catalog, Issue #269 - Dharshan
							isAdHocBoolean = true;
							isAdHoc = null;
							if (poLineItem.getDottedFieldValue("IsAdHoc") != null) {
								isAdHoc = (Boolean) poLineItem.getDottedFieldValue("IsAdHoc");
								isAdHocBoolean = BooleanFormatter.getBooleanValue(isAdHoc);
								Log.customer.debug("%s::isAdHocBoolean:%s",classname,isAdHocBoolean);
								if(isAdHocBoolean == false){
									outPW_FlatFile.write("Catalog Item:");
								}
								else Log.customer.debug("%s::isAdHocBoolean is true, not catalog item",classname);
							}
							else Log.customer.debug("%s::isAdHocBoolean is null, leave blank",classname);
                            outPW_FlatFile.write("\n");
                            //Update DWPOFlag in DO based on config
                            if(isCompletedFlgUpdate) {
                                Log.customer.debug("%s::EzopenPOFlagAction is Completed setting DWPOFlag ...", classname);
                                directOrder.setFieldValue("DWPOFlag", "Completed");
                            }
                            else {
                                    Log.customer.debug("%s::EzopenPOFlagAction is None no action req DWPOFlag ...",classname);
                                    splitAccounting = false;
                                    continue;
                            }
                                                    } // for (int j =0; j <sas.size();j++)
                                                    pushedCount++;
                                  }// if (sacol != null && sacol.getSplitAccountings() != null)
                                                } // for (int i =0; i<po_LineItems.size();i++)
                                            } // if ( lineCount > 0)
                                        } // if (directOrder != null)
                                     else
                                        Log.customer.debug("DO Line Item Count 0 ");
                                }//while (results.next())
                                splitAccounting = false;
							}
							} // try 3
                        catch (Exception e) {
                            Log.customer.debug("Error in if loop " + e.toString());
                            throw e;
                            }
                } // try2
            catch (Exception e) {
                Log.customer.debug(e.toString());
                throw e;
                }
            if (outPW_FlatFile != null) {
                outPW_FlatFile.flush();
                outPW_FlatFile.close();
                } // if
}
    catch (Exception e) {
            Log.customer.debug(e.toString());
//          add message
            message.append("Task start time : "+ startTime);
            message.append("\n");
            message.append("Task end time : " + endTime);
            message.append("\n");
            message.append("No of records pushed : "+ pushedCount);
            message.append("\n");
            message.append("No of records queued  :"+ (resultCount - pushedCount));
            message.append("\n");
            message.append("CATUSDWPOPush_FlatFile Task Failed - Exception details below");
            message.append("\n");
            message.append(e.toString());
            mailSubject = "CATMFGDWPOPush_FlatFile Task Failed";
            Log.customer.debug("%s: Inside Exception message "+ message.toString() , classname);
            new ScheduledTaskException("Error : " + e.toString(), e);
            throw new ScheduledTaskException("Error : " + e.toString(), e);
            } // catch end
        finally {
            results.close();
            outPW_FlatFile.flush();
            outPW_FlatFile.close();

			//Change made by Soumya begins
			Log.customer.debug("%s::Archive File Path:%s",classname,archiveFileDataPath);
			Log.customer.debug("CATEZOPENDWPOPush_FlatFile:Starting Copying the flat file to Archive ");
			CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
			Log.customer.debug("CATEZOPENDWPOPush_FlatFile:Completed Copying the flat file to Archive ");

			try
			{
				Log.customer.debug("CATEZOPENDWPOPush_FlatFile:Changing file permission of Data file.");
				Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
				Log.customer.debug("CATEZOPENDWPOPush_FlatFile:Changed file permission of Data file.");
			}catch (IOException e1) {
				Log.customer.debug("Error in changing Permission. "+ e1);
			}

			//Change made by Soumya end


            Log.customer.debug("%s: Inside Finally ", classname);
            message.append("Task start time : "+ startTime);
            Log.customer.debug("%s: Inside Finally added start time", classname);
            message.append("\n");
            endTime = DateFormatter.getStringValue(new Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
            message.append("Task end time : " + endTime);
            message.append("\n");
            message.append("No. of POs to be pushed : "+ resultCount);
            message.append("\n");
            message.append("No. of Lines successfully write : "+ pushedCount);
            message.append("\n");
            Log.customer.debug("%s: Inside Finally message "+ message.toString() , classname);
            // Sending email
            CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "DWPushNotify");
            message = null;
            pushedCount =0;
            resultCount =0;
      } // finally
        }// run method
    public CATEZOPENDWPOPush_FlatFile()  {
        }
}