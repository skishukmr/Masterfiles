package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

//import config.java.common.CatEmailNotificationUtil;

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

import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Vector;
import ariba.util.formatter.DateFormatter;

import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;

import config.java.schedule.util.CATFaltFileUtil;


public class CATUSApexFlatFilePush extends ScheduledTask {

	private String classname = "CATUSApexFlatFilePush";
	private Date datetimezone;
	private String startTime, endTime;
	private FastStringBuffer message = null;
	private String mailSubject = null;
	private String fileExtDateTime = "";
	private String flatFilePath = "";
	//change made by soumya begings
	private String archiveFileDataPath;
	//change made by soumya ends
	private String triggerFile = "";
	private PrintWriter outPW_FlatFile = null;
	private AQLOptions options;
	private Partition partition = null;
	private AQLResultCollection irResultSet;
	private int totalNumberOfPOs;
	private AQLQuery aqlIRQuery;
	private SplitAccountingCollection irAccounting = null;
	private int  irSplitaccSize,iSpAcct;
	private BaseVector irSplitAccounting = null;
	private String irFmtUniqueName;
	private Object requesterName = null;

	public void run() throws ScheduledTaskException {

		try{
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject ="CATUSApexFlatFilePush Task Completion Status - Completed Successfully";
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			if (partition.getName().equalsIgnoreCase("pcsv1")) {
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_APEX_INVOICE_US_PUSH."+ fileExtDateTime + ".txt";
			triggerFile = "/msc/arb9r1/downstream/catdata/INV/MSC_APEX_INVOICE_US_PUSH."+ fileExtDateTime + ".dstrigger";
			//Change made by soumya begins
			archiveFileDataPath = "/msc/arb9r1/downstream/catdata/INV/archive/MSC_APEX_INVOICE_US_PUSH_ARCHIVE."+ fileExtDateTime + ".txt";
			//Change made by soumya ends
			}

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);

			//Change made by soumya begins
			Log.customer.debug("CATUSApexFlatFilePush:archiveFlatFile " + archiveFileDataPath);
			//Change made by soumya ends

			File cliDBPOFlatFile = new File(flatFilePath);

			options = new AQLOptions(partition,true);

			if (!cliDBPOFlatFile.exists()) {
				Log.customer.debug("File not exist creating file ..");
				cliDBPOFlatFile.createNewFile();
			}



			InvoiceReconciliation ir = null;

			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(cliDBPOFlatFile),true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);




			String iRQuery = new String( "Select from ariba.invoicing.core.InvoiceReconciliation where ActionFlag ='Completed' AND APEXFlag is null");
			Log.customer.debug("iRQuery ==> " + iRQuery);
			aqlIRQuery = AQLQuery.parseQuery(iRQuery);
			irResultSet = Base.getService().executeQuery(aqlIRQuery, options);

			if (irResultSet.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResultSetUSCliDB");

			totalNumberOfPOs = irResultSet.getSize();
			Log.customer.debug("totalNumberOfs ==> " + totalNumberOfPOs);

			while(irResultSet.next()){

				ir = (InvoiceReconciliation)(irResultSet.getBaseId("InvoiceReconciliation").get());
				if(ir != null){
					int lineCount = ir.getLineItemsCount();
					BaseVector irLineItemVector = (BaseVector)ir.getLineItems();

					Log.customer.debug("%s::Line Item count for IR:%s ",classname,lineCount);
					if (lineCount > 0){
						for (int i =0; i<lineCount;i++){
							Log.customer.debug("%s::inside for (int i =0; i<lineCount;();i++){ i value %s ",classname,i);
							InvoiceReconciliationLineItem  irLineItem2 = (InvoiceReconciliationLineItem )irLineItemVector.get(i);
							Log.customer.debug("%s::... poLineItem2 %s ",classname,irLineItem2);

							irAccounting = irLineItem2.getAccountings();
							Log.customer.debug("%s::... irAccounting %s ",classname,irAccounting);

							if (irAccounting!=null){
								iSpAcct = 0;
								Log.customer.debug("%s::inside if (irAccounting!=null){ %s ",classname,irAccounting);

								irSplitAccounting = (BaseVector)irAccounting.getSplitAccountings();
								irSplitaccSize = irSplitAccounting.size();
								Log.customer.debug("%s::Split acc size:%s",classname,irSplitaccSize);
								if (irSplitaccSize > 0){
									iSpAcct+= irSplitaccSize;
									for(Iterator s= irAccounting.getSplitAccountingsIterator(); s.hasNext();) {
										SplitAccounting splitAcc = (SplitAccounting) s.next();
										if (splitAcc != null) {
											for (int j = 0; j<irSplitaccSize;j++){
												Log.customer.debug ("%s::Inside for (int j = 0; j<irSplitaccSize;j++){",classname);


												//UniqueName	20
												String irUniqueName = ir.getFieldValue("UniqueName").toString();
												Log.customer.debug("iRSupplierLocation ==> " +irUniqueName);
												irFmtUniqueName = CATFaltFileUtil.getFormattedTxt(irUniqueName,20);
												Log.customer.debug("iRFmtSupplierLocation ==> " +irFmtUniqueName);

												//LineItems.Description.CAPSChargeCode.UniqueName
												//When null pull from LineItems. CAPSChargeCode.UniqueName 	3
												String cAPSChargeCode = "";
												String cAPSChargeCodeFmt ="";
												LineItemProductDescription description = null;
												Log.customer.debug("description");
												if (irLineItem2.getFieldValue("Description") != null){
													Log.customer.debug("Description If");
													description = (LineItemProductDescription)irLineItem2.getFieldValue("Description");
													if (description.getFieldValue("CAPSChargeCode") != null) {
														cAPSChargeCode = description.getDottedFieldValue("CAPSChargeCode.UniqueName").toString();
														Log.customer.debug("cAPSChargeCode ==> " +cAPSChargeCode);

													}
													else
													Log.customer.debug("cAPSChargeCode from line Item");
														if (irLineItem2.getFieldValue("CAPSChargeCode")!= null) {
															cAPSChargeCode = irLineItem2.getDottedFieldValue("CAPSChargeCode.UniqueName").toString();
															Log.customer.debug("cAPSChargeCode in else ==> " +cAPSChargeCode);

														}

													cAPSChargeCodeFmt = CATFaltFileUtil.getFormattedTxt(cAPSChargeCode,3);
												}



												Log.customer.debug("cAPSChargeCodeFmt ==> " +cAPSChargeCodeFmt);


												//InvoiceDate	10
												Date iRSupplierInvoiceDate  =null;
												String iRFmtSupplierInvoiceDate ="";
												 if ( ir.getDottedFieldValue("Invoice.InvoiceDate") != null) {
												          iRSupplierInvoiceDate =  (Date )ir.getDottedFieldValue("Invoice.InvoiceDate");
												 }
												 else {
													 iRSupplierInvoiceDate = new Date();
													 Log.customer.debug("iRSupplierInvoiceDate getting null date need to investigate  ==> " +iRSupplierInvoiceDate);

												 }
												Log.customer.debug("iRSupplierInvoiceDate ==> " +iRSupplierInvoiceDate);
												iRFmtSupplierInvoiceDate = CATFaltFileUtil.getFormattedDate(iRSupplierInvoiceDate);
												Log.customer.debug("iRFmtSupplierInvoiceDate ==> " +iRFmtSupplierInvoiceDate);

												//Supplier.Name	40
												String supplierName = "";
												String supplierNameFmt = "";
												if (ir.getFieldValue("Supplier")!= null)
													supplierName = ir.getDottedFieldValue("Supplier.Name").toString();
												Log.customer.debug("supplierName ==> " +supplierName);
												supplierNameFmt = CATFaltFileUtil.getFormattedTxt(supplierName,40);
												Log.customer.debug("supplierNameFmt ==> " +supplierNameFmt);

												//LineItems. NumberInCollection 	3
												String numberInCollection="";
												String numberInCollectionFmt="";
												if (irLineItem2.getFieldValue("NumberInCollection")!= null)
													numberInCollection = irLineItem2.getFieldValue("NumberInCollection").toString();
												Log.customer.debug("numberInCollection ==> " +numberInCollection);
												numberInCollectionFmt = CATFaltFileUtil.getFormattedTxt(numberInCollection,3);
												Log.customer.debug("numberInCollectionFmt ==> " +numberInCollectionFmt);


												//LineItems.Description.SupplierPartNumber	25

												String supplierPartNumber="";
												String supplierPartNumberFmt="";
												if (irLineItem2.getDottedFieldValue("Description.SupplierPartNumber")!= null)

													supplierPartNumber = irLineItem2.getFieldValue("NumberInCollection").toString();
												Log.customer.debug("numberInCollection ==> " +numberInCollection);
												numberInCollectionFmt = CATFaltFileUtil.getFormattedTxt(supplierPartNumber,25);
												Log.customer.debug("supplierPartNumberFmt ==> " +supplierPartNumberFmt);


												//LineItems.Description	56
												String descriptionTxt="";
												String descriptionTxtFmt="";
												if (irLineItem2.getFieldValue("Description") != null){
													description = (LineItemProductDescription)irLineItem2.getFieldValue("Description");
													descriptionTxt = description.getFieldValue("Description").toString();
												}
												descriptionTxtFmt = CATFaltFileUtil.getFormattedTxt(descriptionTxt,56);

												//LineItems.Description.UnitOfMeasure.CAPSUnitOfMeasure	4
												String cAPSUnitOfMeasure="";
												String cAPSUnitOfMeasureFmt="";
												if (irLineItem2.getFieldValue("Description") != null){
													description = (LineItemProductDescription)irLineItem2.getFieldValue("Description");

													if (description.getFieldValue("UnitOfMeasure") != null) {
														UnitOfMeasure uom = (UnitOfMeasure)description.getFieldValue("UnitOfMeasure");
														cAPSUnitOfMeasure = uom.getFieldValue("CAPSUnitOfMeasure").toString();
														Log.customer.debug("cAPSUnitOfMeasure ==> " +cAPSUnitOfMeasure);
													}

												}
												cAPSUnitOfMeasureFmt = CATFaltFileUtil.getFormattedTxt(cAPSUnitOfMeasure,4);
												Log.customer.debug("cAPSUnitOfMeasureFmt ==> " +cAPSUnitOfMeasureFmt);

												//LineItems.Description.Price.Amount	15

												String priceAmountFmt ="";

												if (irLineItem2.getFieldValue("Description") != null){
													description = (LineItemProductDescription)irLineItem2.getFieldValue("Description");

													if (description.getFieldValue("Price") != null) {
														Money price = (Money)description.getFieldValue("Price");
														double priceDbl = price.getAmountAsDouble();
														Log.customer.debug("priceDbl ==> " +priceDbl);
														priceAmountFmt = CATFaltFileUtil.getFormattedNumber(priceDbl, "00000000.0000");
														Log.customer.debug("priceAmountFmt ==> " +priceAmountFmt);
													}

												}
												Log.customer.debug("priceAmountFmt ==> " +priceAmountFmt);

												//LineItems. Quantity	15
												String quantity="";
												String quantityFmt="";
												if (irLineItem2.getFieldValue("Quantity")!= null)

												quantity = irLineItem2.getFieldValue("Quantity").toString();
												Log.customer.debug("quantity ==> " +quantity);
												quantityFmt = CATFaltFileUtil.getFormattedTxt(quantity,15);
												Log.customer.debug("quantityFmt ==> " +quantityFmt);

												//LineItems. Quantity	15
												//String quantityFmt1 = quantityFmt;
												//Log.customer.debug("quantityFmt1 ==> " +quantityFmt1);

												//LineItems. IRTaxRate	15
												BigDecimal irRTaxRate = null;
												String irRTaxRateFmt ="";
												if (irLineItem2.getFieldValue("IRTaxRate")!= null) {

													irRTaxRate = (BigDecimal)irLineItem2.getFieldValue("IRTaxRate");
												Log.customer.debug("irRTaxRate ==> " +irRTaxRate);
												irRTaxRateFmt = CATFaltFileUtil.getFormattedNumber(irRTaxRate.doubleValue(),"00000000.0000");

												}
												Log.customer.debug("irRTaxRateFmt ==> " +irRTaxRateFmt);

												//Supplier.UniqueName	10
												String supplierUniqueName = "";
												String supplierUniqueNameFmt = "";
												if (ir.getFieldValue("Supplier")!= null)
													supplierUniqueName = ir.getDottedFieldValue("Supplier.UniqueName").toString();
												Log.customer.debug("supplierUniqueName ==> " +supplierUniqueName);
												supplierUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(supplierUniqueName,10);
												Log.customer.debug("supplierUniqueNameFmt ==> " +supplierUniqueNameFmt);

												//LineItems.ShipTo. ReceivingFacility	2
												// Code change By darshan for null pointer Exception
												Log.customer.debug("inside ReceivingFacility part");
												String irReceivingFacility = null;
												String irReceivingFacilityFmt ="";
												Log.customer.debug("befor if condition in facility");
												if (irLineItem2.getFieldValue("ShipTo")!= null) {
													Log.customer.debug("inside if condition in facility");
													Address irReceivingFacilityAdd = (Address)irLineItem2.getFieldValue("ShipTo");
													Log.customer.debug("inside if before fetching receiving facility");
												if (irReceivingFacilityAdd.getFieldValue("ReceivingFacility")!= null){
												irReceivingFacility = irReceivingFacilityAdd.getFieldValue("ReceivingFacility").toString();
												Log.customer.debug("inside my if after getting facility");
												Log.customer.debug("irReceivingFacility ==> " +irReceivingFacility);
												irReceivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(irReceivingFacility,2);
												}
												}
												// Code end.
												Log.customer.debug("irReceivingFacilityFmt ==> " +irReceivingFacilityFmt);

												//LineItems.TaxCode.UniqueName	2
												String irTaxCodeUniqueName = null;
												String irTaxCodeUniqueNameFmt ="";
												if (irLineItem2.getFieldValue("TaxCode")!= null) {
													irTaxCodeUniqueName  = irLineItem2.getDottedFieldValue("TaxCode.UniqueName").toString();
													Log.customer.debug("irTaxCodeUniqueName ==> " +irTaxCodeUniqueName);
													irTaxCodeUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(irTaxCodeUniqueName,2);
												}
												Log.customer.debug("irTaxCodeUniqueNameFmt ==> " +irTaxCodeUniqueNameFmt);

												//PONumber	10
												String pONumber = "";
												String pONumberFmt = "";
												if (ir.getFieldValue("PONumber")!= null) {
													pONumber  = ir.getFieldValue("PONumber").toString();
													Log.customer.debug("pONumber ==> " +pONumber);
												}
												pONumberFmt = CATFaltFileUtil.getFormattedTxt(pONumber,10);
												Log.customer.debug("pONumberFmt ==> " +pONumberFmt);


												//Lineitems. TermsDiscountPercent  	15
												BigDecimal irTermsDiscountPercent = null;
												String irTermsDiscountPercentFmt ="";
												if (irLineItem2.getFieldValue("TermsDiscountPercent")!= null) {

													irTermsDiscountPercent = (BigDecimal)irLineItem2.getFieldValue("TermsDiscountPercent");
												Log.customer.debug("irTermsDiscountPercent ==> " +irTermsDiscountPercent);
												irTermsDiscountPercentFmt = CATFaltFileUtil.getFormattedNumber(irTermsDiscountPercent.doubleValue(),"00000000.0000");

												}
												Log.customer.debug("irTermsDiscountPercentFmt ==> " +irTermsDiscountPercentFmt);

												//LineItems.Description.UnitOfMeasure.CAPSUnitOfMeasure	4
												String cAPSUnitOfMeasure1="";
												String cAPSUnitOfMeasureFmt1="";
												if (irLineItem2.getFieldValue("Description") != null){
													description = (LineItemProductDescription)irLineItem2.getFieldValue("Description");
													Log.customer.debug("description ==> " +description);

													if (description.getFieldValue("UnitOfMeasure") != null) {
														UnitOfMeasure uom1 = (UnitOfMeasure)description.getFieldValue("UnitOfMeasure");
														Log.customer.debug("uom1 ==> " +uom1);
														cAPSUnitOfMeasure1 = uom1.getFieldValue("CAPSUnitOfMeasure").toString();
														Log.customer.debug("cAPSUnitOfMeasure1 ==> " +cAPSUnitOfMeasure1);
													}

												}
												cAPSUnitOfMeasureFmt1 = CATFaltFileUtil.getFormattedTxt(cAPSUnitOfMeasure1,4);
												Log.customer.debug("cAPSUnitOfMeasureFmt1 ==> " +cAPSUnitOfMeasureFmt1);


												//TotalCost.Currency.UniqueName	3

												String currencyUniqueName="";
												String currencyUniqueNameFmt="";
												if (ir.getFieldValue("TotalCost") != null){
													Money totalCost = (Money)ir.getFieldValue("TotalCost");
													currencyUniqueName = totalCost.getDottedFieldValue("Currency.UniqueName").toString();
													Log.customer.debug("currencyUniqueName ==> " +currencyUniqueName);

												}
												currencyUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(currencyUniqueName,3);
												Log.customer.debug("currencyUniqueNameFmt ==> " +currencyUniqueNameFmt);

												//ExchangeRate	 25
												BigDecimal irExchangeRate = null;
												String irExchangeRateFmt ="";
												if (ir.getFieldValue("USExchangeRate")!= null) {

													irExchangeRate = (BigDecimal)ir.getFieldValue("USExchangeRate");
												Log.customer.debug("irExchangeRate ==> " +irExchangeRate);
												irExchangeRateFmt = CATFaltFileUtil.getFormattedNumber(irExchangeRate.doubleValue(),"00000000.0000");

												}
												Log.customer.debug("irExchangeRateFmt ==> " +irExchangeRateFmt);







												Log.customer.debug("IRdata writing to file  ==> ");
												String apexIRData = irFmtUniqueName+"~|"+cAPSChargeCodeFmt+"~|"+iRFmtSupplierInvoiceDate+"~|"+supplierNameFmt+"~|"+numberInCollectionFmt+"~|"+supplierPartNumberFmt+"~|"+descriptionTxtFmt+"~|"+cAPSUnitOfMeasureFmt+"~|"+priceAmountFmt+"~|"+quantityFmt+"~|"+irRTaxRateFmt+"~|"+supplierUniqueNameFmt+"~|"+irReceivingFacilityFmt+"~|"+irTaxCodeUniqueNameFmt+"~|"+pONumberFmt+"~|"+irTermsDiscountPercentFmt+"~|"+cAPSUnitOfMeasureFmt1+"~|"+currencyUniqueNameFmt+"~|"+irExchangeRateFmt;
												Log.customer.debug("IR data writing to file  ==> " +apexIRData);
												outPW_FlatFile.write(apexIRData);
												Log.customer.debug("New Line writing to file  ==> ");
												outPW_FlatFile.write("\n");
											} //for (int j = 0; j<poSplitaccSize;j++){

										} //if (splitAcc != null)

									} //for(Iterator s= poAccounting.getSplitAccountingsIterator(); s.hasNext();) {

								} //if (poSplitaccSize > 0){

							} //if (poAccounting!=null){

						} //for (int i =0; i<lineCount;i++){




					} //if (lineCount > 0){
						Log.customer.debug("APEX Flag set to Completed");

						ir.setFieldValue("APEXFlag", "Completed");
				} //if(po != null)

			} // while












		}
		catch (Exception e) {

			Log.customer.debug(e);
		}
		finally {


			if (outPW_FlatFile != null)  {
				outPW_FlatFile.flush();
				outPW_FlatFile.close();

				//Change made by Soumya begins
				Log.customer.debug("CATUSApexFlatFilePush:Starting Copying the flat file to Archive ");
				CATFaltFileUtil.copyFile(flatFilePath, archiveFileDataPath);
				Log.customer.debug("CATUSApexFlatFilePush:Completed Copying the flat file to Archive ");
				//Change made by Soumya end

				try {

					//Change made by Soumya begins
					Log.customer.debug("CATUSApexFlatFilePush:Changing file permission of Data file.");
					Runtime.getRuntime().exec("chmod 666 " + flatFilePath);
					Log.customer.debug("CATUSApexFlatFilePush:Changed file permission of Data file.");
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
					Log.customer.debug("CATUSApexFlatFilePush:Changing file permission of trigger files ");
					Runtime.getRuntime().exec("chmod 666 " + triggerFile);
					Log.customer.debug("CATUSApexFlatFilePush:Changed file permission of trigger files ");
					 //Change made by Soumya end
				} catch (IOException e1) {
					Log.customer.debug("triggerFile allready exit. "+ e1);
				}

			}
		}






	}


			}

