/*
 * Created on Oct 8, 2010
 *
 * Created by PGS Kannan
 * Useage :
 *
 *  Change History
 *	Change By	Change Date		Description
 * =============================================================================================
 */
package config.java.schedule.vcsv3;
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
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.Vector;
import ariba.util.formatter.DateFormatter;
import ariba.util.formatter.IntegerFormatter;

import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import config.java.common.CatEmailNotificationUtil;
import config.java.schedule.util.CATFaltFileUtil;

public class CATEzopenInvoiceFlatFilePush extends ScheduledTask {

	private String classname = "CATEzopenInvoiceFlatFilePush";
	private String fileExtDateTime = "";
	private String flatFilePath = "";
	private String controlFlatFilePath = "";

	private String triggerFile = "";

	private AQLOptions options;
	private Partition partition = Base.getService().getPartition("ezopen");

	private AQLQuery aqlIRQuery,aqlctrlQuery;
	private AQLResultCollection irResultSet,ctrlResultSet;
	private String startTime, endTime;
	private FastStringBuffer message = null;
	private String mailSubject = null;
	private int totalNumberOfIrs;
	private BaseVector IrLineItem=null;
	private int irSplitaccSize;
	private SplitAccountingCollection irAccounting = null;
	private BaseVector irSplitAccounting = null;
	private SplitAccountingCollection sacol = null;

	private int resultCount, pushedCount;

	//private BigDecimal totamt,InvoiceSplitDiscountDollarAmount,TotalInvoiceAmountMinusDiscount;
	private String iRFmtBlockStampDate,iRFmtCatInvoiceNumber,iRFmtTimeCreated,iRFmtSupplierLocation,iRFmtCurrency;
	private String iRFmtcdg2Csens,iRFmtTotalCost,iRFmtBVRNumber,iRFmtSupplierInvoiceDate,iRFmtInvoiceNumber,iRFmtOriginVATCountry,iRFmtIsVATReverseCharge,iRFmtCatVATCountryCode,iRFmtPaymentDueDate,iRFmtRelatedCatInvoice,iRFmtLineTypeCategory,iRLnFmtSplitAccountingsAccountingFacility,iRLnFmtSplitAccountingsDepartment,iRLnFmtSplitAccountingsDivision,iRLnFmtSplitAccountingsSection,iRLnFmtSplitAccountingsExpenseAccount,iRLnFmtSplitAccountingsOrder,iRLnFmtSplitAccountingsMisc,iRLnFmtSplitAccountingsAmountAmount,irFmtIlQuantity,iRLnFmtunitOfMeasure,iRLnFmtPriceAmount,iRLnFmtDescription,iRLnFmtSplitAccountingsAmount,iRLnFmtPONumber,iRLnFmtIsVatRecoverable,iRLnFmtSplitAccountingsCompDivision,iRLnFmtSplitAccountingsCompSection,iRLnFmtSplitAccountingsCompExpenseAccount,iRLnFmtPOLineItemNumber,iRLnFmtUnitOfMeasure,iRFmtControlDate;

	private String controlid;
	private Date datetimezone;
	private java.math.BigDecimal bdTotCost;
	private int iSpAcct ;


	private PrintWriter outPW_FlatFile = null;
	private PrintWriter outPW_CTRLFlatFile = null;

	/* (non-Javadoc)
	 * @see ariba.util.scheduler.ScheduledTask#run()
	 */
	public void run() throws ScheduledTaskException {

	        // last accessed IR - used for exceptions
		InvoiceReconciliation lastAccessedIR = null;

	        // we're starting a new transaction in a method called by the run() method
            // the object held by 'lastAccessedIR' will be flushed if an exception
		    // occurs after the method call getControlFileData (...) happens. We need
		    // to lookup the object in this scenario in the catch block while reverting
		    // the flag
		boolean flushed = false;
		boolean isActionFlagSet = false;

		try {
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject ="CATEZOPENInvoiceFlatFile Task Completion Status - Completed Successfully";
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			flatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_EZOPEN_INVOICE_PUSH."+ fileExtDateTime + ".txt";
			controlFlatFilePath = "/msc/arb9r1/downstream/catdata/INV/MSC_EZOPEN_INVOICE_CTRL."+ fileExtDateTime + ".txt";
			triggerFile = "/msc/arb9r1/downstream/catdata/INV/MSC_EZOPEN_INVOICE_PUSH."+ fileExtDateTime + ".dstrigger";

			Log.customer.debug("flatFilePath " + flatFilePath);
			Log.customer.debug("controlFlatFilePath " + controlFlatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);

			File ezopenIRFlatFile = new File(flatFilePath);
			File ezopenIRCTRLFlatFile = new File(controlFlatFilePath);


			options = new AQLOptions(partition);

			if (!ezopenIRFlatFile.exists()) {
				Log.customer.debug("File not exist creating file ..");
				ezopenIRFlatFile.createNewFile();
			}

			if (!ezopenIRCTRLFlatFile.exists()) {
				Log.customer.debug("CTRL File not exist creating file ..");
				ezopenIRCTRLFlatFile.createNewFile();
			}

			InvoiceReconciliation invrecon = null;

			outPW_FlatFile =new PrintWriter(IOUtil.bufferedOutputStream(ezopenIRFlatFile),true);
			Log.customer.debug("outPW_FlatFile " + outPW_FlatFile);

			outPW_CTRLFlatFile =new PrintWriter(IOUtil.bufferedOutputStream(ezopenIRCTRLFlatFile),true);
			Log.customer.debug("outPW_CTRLFlatFile " + outPW_CTRLFlatFile);



			String iRQuery = new String( "select from ariba.invoicing.core.InvoiceReconciliation  where ActionFlag = 'InProcess'");
			Log.customer.debug("iRQuery ==> " + iRQuery);
			aqlIRQuery = AQLQuery.parseQuery(iRQuery);
			Log.customer.debug("aqlIRQuery ==> execute " + aqlIRQuery);
			irResultSet = Base.getService().executeQuery(aqlIRQuery, options);
			Log.customer.debug("irResultSet ### ==> after execute " + irResultSet.getSize());

			if (irResultSet.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResult");

			totalNumberOfIrs = irResultSet.getSize();
			resultCount = totalNumberOfIrs;
			Log.customer.debug("totalNumberOfIrs ==> @@@ " + totalNumberOfIrs);
			int commitCount = 0;

			while(irResultSet.next()){

                    isActionFlagSet = false;
					flushed = false;
					invrecon = (InvoiceReconciliation)(irResultSet.getBaseId("InvoiceReconciliation").get());
					lastAccessedIR = invrecon;

					if(invrecon != null){

		                Log.customer.debug("Setting Action Flag as Completed");

						    // set control flag - will revert this if an exception occurs
		                invrecon.setFieldValue("ActionFlag", "Completed");
						isActionFlagSet = true;

						int lineCount = invrecon.getLineItemsCount();
						BaseVector IrLineItemVector = (BaseVector)invrecon.getLineItems();
                                                iSpAcct = 0;


                                          for (int k=0; k<invrecon.getLineItemsCount();k++)
                                          {
                                        Log.customer.debug("Getting the Split Accounting total");
                                         InvoiceReconciliationLineItem IRLineItem = (InvoiceReconciliationLineItem)IrLineItemVector.get(k);
                                         if (IRLineItem.getFieldValue("Accountings") != null)

								        sacol = (SplitAccountingCollection)IRLineItem.getFieldValue("Accountings");
								       if (sacol != null && sacol.getSplitAccountings() != null)
									   iSpAcct+= ((BaseVector)sacol.getSplitAccountings()).size();
									   Log.customer.debug("Final Split total is :" +iSpAcct);
                                        }


						Log.customer.debug("%s::Line Item count for IR:%s ",classname,lineCount);
						if (lineCount > 0){

						for (int i =0; i<lineCount;i++){
							Log.customer.debug("inside for (int i =0; i<IrLineItem.size();i++) ");
							InvoiceReconciliationLineItem IrLineItem2 = (InvoiceReconciliationLineItem)IrLineItemVector.get(i);
							Log.customer.debug("%s::... IrLineItem2 %s ",classname,IrLineItem2);

							irAccounting = (ariba.common.core.SplitAccountingCollection)IrLineItem2.getFieldValue("Accountings");
							Log.customer.debug("%s::... irAccounting %s ",classname,irAccounting);

						if (irAccounting!=null && irAccounting.getSplitAccountings() != null){
							Log.customer.debug("inside if (irAccounting!=null)",classname);

                                                           //  iSpAcct+= ((BaseVector)irAccounting.getSplitAccountings()).size();
                                                           //Log.customer.debug("Split Accounting Final Size calculation");

                         				irSplitAccounting = (BaseVector)irAccounting.getSplitAccountings();
							irSplitaccSize = irSplitAccounting.size();

							Log.customer.debug("%Calculating number of Splits");

							if (irSplitaccSize > 0){
							//	iSpAcct+= ((BaseVector))irAccounting.getSplitAccountings()).size();


							for(Iterator s= irAccounting.getSplitAccountingsIterator(); s.hasNext();) {
								SplitAccounting splitAcc = (SplitAccounting) s.next();


							if (splitAcc != null) {



								if (invrecon.getDottedFieldValue("TotalCost.Amount") != null)
									bdTotCost = (java.math.BigDecimal)invrecon.getDottedFieldValue("TotalCost.Amount");


								Log.customer.debug ("%s::Inside splitAcc != null",classname);



								//for (int j = 0; j<irSplitaccSize;j++){
									//Log.customer.debug ("Inside for (int j = 0; j<irSplitaccSize;j++)");

									 //  LineType	X(01)	For header records DATA STAGE will need to send an 'H'
									String iRFmtiRHeadder = "H";
									Log.customer.debug("iRFmtiRHeadder ==> " +iRFmtiRHeadder);

									 //  MSCInvoiceNumber	X(25)	UniqueName
									String invFmtUniqueName = "";
									String invUnique = (java.lang.String)invrecon.getFieldValue("UniqueName");

									Log.customer.debug("UniqueName Is..." + invUnique);
									if (invUnique.length() >= 25)
									{
										invUnique =  invUnique.substring(invUnique.length() - 25);
									}


									Log.customer.debug("Last 25 Chars of the IR Is..." + invUnique);
									controlid = new String (invUnique);
									String ID = "EZO-";
									//Changed by Sandeep as per Eric Lox's Request to concatenate EZO
									Log.customer.debug("Adding EZO to Control ID");
									//controlid = getDateTime(datetimezone) + controlid;
									controlid = ID + controlid;
									Log.customer.debug("ControlIdentifier IS..." + controlid);

									invrecon.setFieldValue("ControlIdentifier",controlid);
									invrecon.setFieldValue( "ControlDate", datetimezone );

									invFmtUniqueName   = CATFaltFileUtil.getFormattedTxt( invUnique.toUpperCase(),25);
									Log.customer.debug("invFmtUniqueName ==> " +invFmtUniqueName);


									 //  CDG2-NEX-GEN	9(04)	FacilityFlag

									String iRFacilityFlag = null;
									String iRFmtFacilityFlag = null;

									if (invrecon.getFieldValue("FacilityFlag") != null){
										iRFacilityFlag = invrecon.getFieldValue("FacilityFlag").toString();
									}
									else
										iRFacilityFlag = "AA";
									iRFmtFacilityFlag = CATFaltFileUtil.getFormattedTxt(iRFacilityFlag.toUpperCase(), 4);
									Log.customer.debug("iRFmtFacilityFlag ==> " +iRFmtFacilityFlag);


									 //  InvoiceReceivedDate	9(08)	BlockStampDate
									Date iRBlockStampDate = (Date)invrecon.getFieldValue("BlockStampDate");
									Log.customer.debug("iRBlockStampDate ==> " +iRBlockStampDate);
									iRFmtBlockStampDate = CATFaltFileUtil.getEzFormattedDate(iRBlockStampDate);
									Log.customer.debug("iRFmtBlockStampDate ==> " +iRFmtBlockStampDate);


									 //  CDG2-NPCE	9(08)	CatInvoiceNumber
									String iRCatInvoiceNumber  ="";
									if ( invrecon.getFieldValue("CatInvoiceNumber") != null)
										iRCatInvoiceNumber =  invrecon.getFieldValue("CatInvoiceNumber").toString();
										Log.customer.debug("iRCatInvoiceNumber ==> " +iRCatInvoiceNumber);
										//added by Sandeep as per Eric Lox Change
										iRCatInvoiceNumber = iRCatInvoiceNumber.substring(2,iRCatInvoiceNumber.length());
									Log.customer.debug("iRCatInvoiceNumber after dropping first 2 char ==> " +iRCatInvoiceNumber);
									String iRFmtCatInvoiceNumber = CATFaltFileUtil.getFormattedTxt(iRCatInvoiceNumber,8);
									Log.customer.debug("iRFmtCatInvoiceNumber ==> " +iRFmtCatInvoiceNumber);


									 //  CDG2-DPCE	9(08)	TimeCreated
									Date iRTimeCreated  =null;
									 if ( invrecon.getFieldValue("TimeCreated") != null) {
										 iRTimeCreated =  (Date )invrecon.getFieldValue("TimeCreated");
									 }

									Log.customer.debug("iRTimeCreated ==> " +iRTimeCreated);
									String iRFmtTimeCreated = CATFaltFileUtil.getEzFormattedDate(iRTimeCreated);
									Log.customer.debug("iRFmtTimeCreated ==> " +iRFmtTimeCreated);


									 //  CDG2-CCPTE	X(15)	Supplier.Location.UniqueName
									String iRSupplierLocation = invrecon.getDottedFieldValue("SupplierLocation.UniqueName").toString();
									Log.customer.debug("iRSupplierLocation ==> " +iRSupplierLocation);
									iRFmtSupplierLocation = CATFaltFileUtil.getFormattedTxt(iRSupplierLocation,15);
									Log.customer.debug("iRFmtSupplierLocation ==> " +iRFmtSupplierLocation);

									 //  CDG2-CDEV	X(03)	TotalCost.Currency.UniqueName
									String iRCurrency = invrecon.getDottedFieldValue("ReportingCurrency.UniqueName").toString();
									Log.customer.debug("iRCurrency ==> " +iRCurrency);
									iRFmtCurrency = CATFaltFileUtil.getFormattedTxt(iRCurrency,3);
									Log.customer.debug("iRFmtiRCurrency ==> " +iRFmtCurrency);

									Log.customer.debug("Total Cost before converting to double ==> " +invrecon.getTotalCost().getAmount());
									double iRTotalCost  = invrecon.getTotalCost().getAmountAsDouble();
									Log.customer.debug("Total Cost after conversion ==> " + iRTotalCost);

									BigDecimal result;
									double iRFinalTotalCost = 0.00;

									 //  CDG2-CSENS	X(01)	N/A
									String iRFmtcdg2Csens = "";
									if (iRTotalCost >0.0)
									    iRFmtcdg2Csens = "C";
									else
										iRFmtcdg2Csens = "D";
									Log.customer.debug("iRFmtcdg2Csens ==> " +iRFmtcdg2Csens);

									 //  CDG2-MTRANS	9(13)V9(03)	TotalCost.Amount
									//iRTotalCost  = invrecon.getTotalCost().getAmountAsDouble();



									// Check if the TotalCost is negative

									if(iRTotalCost < 0){
										int multi = -1;
										result = new BigDecimal(iRTotalCost).multiply(new BigDecimal(multi));
										iRFinalTotalCost = result.doubleValue();
									}
									else {
										iRFinalTotalCost = iRTotalCost;
									}
									Log.customer.debug("iRTotalCost ==> " +iRFinalTotalCost);
									//Changed by Sandeep as per Eric's Req
									iRFmtTotalCost = CATFaltFileUtil.getEzThousandFormattedNumber(iRFinalTotalCost,"0000000000000000");
									Log.customer.debug("iRFmtTotalCost ==> " +iRFinalTotalCost);

									 //  CDG2-LREF-TRANS	X(40)	BVRNumber
									String iRBVRNumber  ="";
									if ( invrecon.getFieldValue("BVRNumber") != null)
										iRBVRNumber =  invrecon.getFieldValue("BVRNumber").toString();
									Log.customer.debug("iRBVRNumber ==> " +iRBVRNumber);
									String iRFmtBVRNumber = CATFaltFileUtil.getFormattedTxt(iRBVRNumber,40);
									Log.customer.debug("iRFmtBVRNumber ==> " +iRFmtBVRNumber);

									 //  CDG2-DTRANS-EXT	9(08)	SupplierInvoiceDate
									Date iRSupplierInvoiceDate  =null;
									 if ( invrecon.getFieldValue("SupplierInvoiceDate") != null) {
									          iRSupplierInvoiceDate =  (Date )invrecon.getFieldValue("SupplierInvoiceDate");
									 }
									 else {
										 iRSupplierInvoiceDate = new Date();
										 Log.customer.debug("iRSupplierInvoiceDate getting null date need to investigate  ==> " +iRSupplierInvoiceDate);

									 }
									Log.customer.debug("iRSupplierInvoiceDate ==> " +iRSupplierInvoiceDate);
									iRFmtSupplierInvoiceDate = CATFaltFileUtil.getEzFormattedDate(iRSupplierInvoiceDate);
									Log.customer.debug("iRFmtSupplierInvoiceDate ==> " +iRFmtSupplierInvoiceDate);


									 //  CDG2-CTRANS-EXT	X(25)	InvoiceNumber
									String iRInvoiceNumber  ="";
									if ( invrecon.getFieldValue("InvoiceNumber") != null)
										iRInvoiceNumber =  invrecon.getFieldValue("InvoiceNumber").toString();
									Log.customer.debug("iRInvoiceNumber ==> " +iRInvoiceNumber);
									String iRFmtInvoiceNumber = CATFaltFileUtil.getFormattedTxt(iRInvoiceNumber,25);
									Log.customer.debug("iRFmtInvoiceNumber ==> " +iRFmtInvoiceNumber);



									 //  OriginCountry	X(2)	OriginVATCountry.UniqueName

									String iROriginVATCountry  ="";
									if ( invrecon.getFieldValue("OriginVATCountry") != null)
										iROriginVATCountry =  invrecon.getDottedFieldValue("OriginVATCountry.UniqueName").toString();
									Log.customer.debug("iROriginVATCountry ==> " +iROriginVATCountry);
									String iRFmtOriginVATCountry = CATFaltFileUtil.getFormattedTxt(iROriginVATCountry,2);
									Log.customer.debug("iRFmtOriginVATCountry ==> " +iRFmtOriginVATCountry);

									 //  ReverseCharge	X(3)	IsVATReverseCharge
									String iRIsVATReverseChargeStr  ="";
									Boolean iROriginVATCountryBool = null;
									if ( invrecon.getFieldValue("IsVATReverseCharge") != null)
										iROriginVATCountryBool = (Boolean) invrecon.getFieldValue("IsVATReverseCharge");
									Log.customer.debug("iROriginVATCountryBool ==> " +iROriginVATCountryBool);
									if(iROriginVATCountryBool.booleanValue())
										iRIsVATReverseChargeStr = "yes";
									else
										iRIsVATReverseChargeStr = "no";

									String iRFmtIsVATReverseCharge = CATFaltFileUtil.getFormattedTxt(iRIsVATReverseChargeStr,3);
									Log.customer.debug("iRFmtIsVATReverseCharge ==> " +iRFmtIsVATReverseCharge);

									 //  CATVATCountryCode	X(2)	CatVATCountryCode
									String iRCatVATCountryCode  ="";
									if ( invrecon.getFieldValue("CatVATCountryCode") != null)
										iRCatVATCountryCode =  invrecon.getFieldValue("CatVATCountryCode").toString();
									Log.customer.debug("iRCatVATCountryCode ==> " +iRCatVATCountryCode);
									String iRFmtCatVATCountryCode = CATFaltFileUtil.getFormattedTxt(iRCatVATCountryCode,2);
									Log.customer.debug("iRFmtCatVATCountryCode ==> " +iRFmtCatVATCountryCode);




									 //  DueDate	X(8)	PaymentDueDate
									Date iRPaymentDueDate  =null;
									 if ( invrecon.getFieldValue("PaymentDueDate") != null) {
										 iRPaymentDueDate =  (Date )invrecon.getFieldValue("PaymentDueDate");
									 }
									 else {
										 iRPaymentDueDate = new Date();
										 Log.customer.debug("iRPaymentDueDate getting null date need to investigate  ==> " +iRPaymentDueDate);

									 }
									Log.customer.debug("iRPaymentDueDate ==> " +iRPaymentDueDate);
									String iRFmtPaymentDueDate = CATFaltFileUtil.getEzFormattedDate(iRPaymentDueDate);
									Log.customer.debug("iRFmtPaymentDueDate ==> " +iRFmtPaymentDueDate);

									 //  RefCatInvoiceNumber	9(08)	RelatedCatInvoice
									String iRRelatedCatInvoice  =" ";
									if ( invrecon.getFieldValue("RelatedCatInvoice") != null)
										iRRelatedCatInvoice =  invrecon.getFieldValue("RelatedCatInvoice").toString();
									Log.customer.debug("iRRelatedCatInvoice ==> " +iRRelatedCatInvoice);
									String iRFmtRelatedCatInvoice = CATFaltFileUtil.getFormattedTxt(iRRelatedCatInvoice,8);
									Log.customer.debug("iRFmtRelatedCatInvoice ==> " +iRFmtRelatedCatInvoice);

									 //  LineType	X(01)	N/A LineType.Category.
									// If LineType.Category is not = 2 send 'L'If LineType.Category = 2 send 'T'
									String iRLineTypeCategory = "";
									String iRFmtLineTypeCategory = "";
									if (IrLineItem2.getDottedFieldValue("LineType.Category") != null){


										Log.customer.debug("Category is not null ==> " );
										iRLineTypeCategory = IrLineItem2.getDottedFieldValue("LineType.Category").toString();
										//if ( !(iRLineTypeCategory.equals("2")))
										//	  iRFmtLineTypeCategory = "L";

										if ( (iRLineTypeCategory.equals("2")))
										{
											  iRFmtLineTypeCategory = "T";
										  }
										  else
										  {
											  iRFmtLineTypeCategory = "L";
									  }
									}
									else{
										Log.customer.debug("RelatedCatInvoice is NULL  ==> " );
									}


									Log.customer.debug("iRFmtLineTypeCategory ==> " +iRFmtLineTypeCategory);


									 //  AccountingFacility	X(2)	LineItems.Accountings.SplitAccountings.AccountingFacility

									String iRAccountingFacility = "";
									iRLnFmtSplitAccountingsAccountingFacility = "";
									if (splitAcc.getFieldValue("AccountingFacility") != null){
										iRAccountingFacility = (String)splitAcc.getFieldValue("AccountingFacility");
										iRLnFmtSplitAccountingsAccountingFacility = CATFaltFileUtil.getFormattedTxt(iRAccountingFacility.toUpperCase(),2);
									}
									Log.customer.debug("iRLnFmtSplitAccountingsAccountingFacility ==> " +iRLnFmtSplitAccountingsAccountingFacility);

									 //  Department	X(5) 	LineItems.Accountings.SplitAccountings.Department
									String iRLnSplitAccountingsDepartment = "";
									iRLnFmtSplitAccountingsDepartment = "";
									if (splitAcc.getFieldValue("Department") != null){
										iRLnSplitAccountingsDepartment = (String)splitAcc.getFieldValue("Department");
										iRLnFmtSplitAccountingsDepartment = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsDepartment.toUpperCase(),5);
									}
									Log.customer.debug("iRLnFmtSplitAccountingsDepartment ==> " +iRLnFmtSplitAccountingsDepartment);

									 //  Division	X(3)	LineItems.Accountings.SplitAccountings.Division
									String iRLnSplitAccountingsDivision = "";
									iRLnFmtSplitAccountingsDivision = "";
									if (splitAcc.getFieldValue("Division") != null){
										iRLnSplitAccountingsDivision = (String)splitAcc.getFieldValue("Division");
										iRLnFmtSplitAccountingsDivision = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsDivision.toUpperCase(),3);
									}
									Log.customer.debug("iRLnFmtSplitAccountingsDivision ==> " +iRLnFmtSplitAccountingsDivision);

									 //  Section	X(2)	LineItems.Accountings.SplitAccountings.Section
									String iRLnSplitAccountingsSection ="  ";
									iRLnFmtSplitAccountingsSection = "";
									if (splitAcc.getFieldValue("Section")!= null){
										iRLnSplitAccountingsSection = (String)splitAcc.getFieldValue("Section");
										iRLnFmtSplitAccountingsSection = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsSection.toUpperCase(),2);
									}
									Log.customer.debug("iRLnFmtSplitAccountingsSection ==> " +iRLnFmtSplitAccountingsSection);

									 //  ExpenseAccount	X(4)	LineItems.Accountings.SplitAccountings.ExpenseAccount
									String iRLnSplitAccountingsExpenseAccount = "";
									iRLnFmtSplitAccountingsExpenseAccount = "";
									if (splitAcc.getFieldValue("ExpenseAccount") != null){
										iRLnSplitAccountingsExpenseAccount = (String)splitAcc.getFieldValue("ExpenseAccount");

										iRLnFmtSplitAccountingsExpenseAccount = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsExpenseAccount.toUpperCase(),4);
									}
									Log.customer.debug("iRLnFmtSplitAccountingsExpenseAccount ==> " +iRLnFmtSplitAccountingsExpenseAccount);


									 //  Order	X(5)	LineItems.Accountings.SplitAccountings.Order
									String iRLnSplitAccountingsOrder = "";
									String iRLnFmtSplitAccountingsOrder = "";
									if (splitAcc.getFieldValue("Order") != null){
										iRLnSplitAccountingsOrder = (String)splitAcc.getFieldValue("Order");

									}
									iRLnFmtSplitAccountingsOrder = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsOrder.toUpperCase(),5);
									Log.customer.debug("iRLnFmtSplitAccountingsOrder ==> " +iRLnFmtSplitAccountingsOrder);

									 //  Misc	X(3)	LineItems.Accountings.SplitAccountings.Misc
									String iRLnSplitAccountingsMisc = "";
									String iRLnFmtSplitAccountingsMisc = "";
									if (splitAcc.getFieldValue("Misc") != null){
										iRLnSplitAccountingsMisc = (String)splitAcc.getFieldValue("Misc");

									}
									iRLnFmtSplitAccountingsMisc = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsMisc.toUpperCase(),3);
									Log.customer.debug("iRLnFmtSplitAccountingsMisc ==> " +iRLnFmtSplitAccountingsMisc);

									 //  CDG7-MLIG	9(13)V9(03)	LineItems.Accountings.SplitAccountings.Amount.Amount
									BigDecimal iRLnSplitAccountingsAmountAmount = null;
									iRLnFmtSplitAccountingsAmountAmount = null;
									double iRLnSplitAccountingsAmountAmountStr =0.0;

									if (splitAcc.getDottedFieldValue("Amount.Amount") != null){
										iRLnSplitAccountingsAmountAmount = (BigDecimal)splitAcc.getDottedFieldValue("Amount.Amount");
                                                                                double iRLnSplitAccountingsAmountAmount1 = iRLnSplitAccountingsAmountAmount.doubleValue();
									//Added by Sandeep to remove any -ve Amounts
									  if(iRLnSplitAccountingsAmountAmount1 < 0)
									  {
										  Log.customer.debug("Split Accounting Amt is -ve ");
										  int multi1 = -1;
										  BigDecimal result1 = new BigDecimal(iRLnSplitAccountingsAmountAmount1).multiply(new BigDecimal(multi1));
										  iRLnSplitAccountingsAmountAmountStr = result1.doubleValue();
									  }
										  else
										  {
											  Log.customer.debug("Split Accounting Amt is +ve ");
										iRLnSplitAccountingsAmountAmountStr = iRLnSplitAccountingsAmountAmount.doubleValue();
								           }

									}


									iRLnFmtSplitAccountingsAmountAmount = CATFaltFileUtil.getEzThousandFormattedNumber(iRLnSplitAccountingsAmountAmountStr,"0000000000000000");
									Log.customer.debug("iRLnFmtSplitAccountingsAmountAmount ==> " +iRLnFmtSplitAccountingsAmountAmount);

									 //  CDG7-QUNITE	S9(11)V9(02)	LineItems.Accountings.SplitAccountings.Quantity
									BigDecimal irIlQuantity = null;

									String irFmtIlQuantity = "";
									double irIlQuantityDouble =0.0;
									if(splitAcc.getFieldValue("Quantity") != null) {
										irIlQuantity = (BigDecimal)splitAcc.getFieldValue("Quantity");
										irIlQuantityDouble = irIlQuantity.doubleValue();

									 }
									irFmtIlQuantity = CATFaltFileUtil.getEzThousandFormattedNumber(irIlQuantityDouble,"0000000000000");
									Log.customer.debug("irFmtIlQuantity ==> " +irFmtIlQuantity);


									 //  CDG7-CUNITE	X(04)	LineItems.Description.UnitOfMeasure.UniqueName
									String iRLnunitOfMeasure = "  ";
									String iRLnFmtunitOfMeasure = "  ";
									if (IrLineItem2.getDottedFieldValue("LineType.Category").toString().equals("2")){

										iRLnunitOfMeasure="PC";
										Log.customer.debug("The Line Type is TAX hence UOM shud be PC");

                                     }

									else
									{
										//Sandeep Changed as per Eric's Request

								        UnitOfMeasure iRLnunitOfMeasureObj = (UnitOfMeasure)IrLineItem2.getDottedFieldValue("Description.UnitOfMeasure");
										iRLnunitOfMeasure = iRLnunitOfMeasureObj.getFieldValue("UniqueName").toString();
                                      Log.customer.debug("UOM of Line item " +iRLnunitOfMeasure);


									}

                                    iRLnFmtunitOfMeasure = CATFaltFileUtil.getFormattedTxt(iRLnunitOfMeasure,4);
									Log.customer.debug("iRLnFmtunitOfMeasure ==> " +iRLnFmtunitOfMeasure);


									 //  CDG7-MUNITE	S9(13)V9(03)	LineItems.Description.Price.Amount

									String iRLnFmtPriceAmount = " ";
                                                                        double iRLnPriceAmountdouble= 0.0;
									if (IrLineItem2.getFieldValue("Description") != null){
										Money iRLnPriceAmountMoney = (Money)IrLineItem2.getDottedFieldValue("Description.Price");
                                                                                double iRLnPriceAmountMoney1= iRLnPriceAmountMoney.getAmountAsDouble();
										Log.customer.debug("iRLnPriceAmountMoney ==> " +iRLnPriceAmountMoney);

										if(iRLnPriceAmountMoney1 < 0)
										{
											Log.customer.debug("Price amount is -ve");
											int multi2 = -1;
								BigDecimal result2 = new BigDecimal(iRLnPriceAmountMoney1).multiply(new BigDecimal(multi2));
										 iRLnPriceAmountdouble = result2.doubleValue();
									Log.customer.debug("iRLnFmtPriceAmount ==> " +iRLnFmtPriceAmount);
								}
									else
								{
									Log.customer.debug("Price amt is + ve");
									iRLnPriceAmountdouble = iRLnPriceAmountMoney1;
								}

								iRLnFmtPriceAmount = CATFaltFileUtil.getEzThousandFormattedNumber(iRLnPriceAmountdouble,"0000000000000000");

									}
									Log.customer.debug("iRLnFmtPriceAmount ==> " +iRLnFmtPriceAmount);


									 //  CDG7-LREF	X(40)	LineItems.Description.Description
									String iRLnDescription = "  ";
									String iRLnFmtDescription = "  ";
                                                                        String iRLnFmtDescription_temp = "  ";
									if (IrLineItem2.getFieldValue("Description") != null){
					            					iRLnDescription = (String)IrLineItem2.getDottedFieldValue("Description.Description");

										iRLnFmtDescription_temp = CATFaltFileUtil.getFormattedTxt(iRLnDescription.toUpperCase(),40);
                                                                                // Commented out by Sandeep to allow MultiByte Character cleansing
                                                                                //iRLnFmtDescription = iRLnFmtDescription_temp.replaceAll("[^\\p{Print}[\\|\\^\\n]]", " ");
                                                              String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%&'\\*\"?/<>.,-_=+()[]{}"; 
                                                                       for (int z = 0; z < iRLnFmtDescription_temp.length(); z++) { 
                                                                               if (validChars.indexOf(iRLnFmtDescription_temp.charAt(z)) >= 0) 
                                                                                  {
                                                                                   iRLnFmtDescription += iRLnFmtDescription_temp.charAt(z); 
                                                                                      } 
                                                                                        
                                                                                       else 
                                                                              {
                                                                               iRLnFmtDescription += ' '; 
                                              
									}
                                                                   }
									Log.customer.debug("iRLnFmtDescription ==> " +iRLnFmtDescription);
                                                                  }

									 //  CDG5-MLIG	S9(13)V9(03)	LineItems.Accountings.SplitAccountings.Amount.Amount
									String iRLnSplitAccountingsAmount = "";
									String iRLnFmtSplitAccountingsAmount = null;
                                                			double iRLnFmtSplitAccountings1  = 0.0;
									if (splitAcc.getFieldValue("Amount") != null){
										Money iRLnSplitAccountingsAmountMoney = (Money)splitAcc.getFieldValue("Amount");
                                                                               double iRLnSplitAccountingsAmount1 = iRLnSplitAccountingsAmountMoney.getAmountAsDouble();
										//Added by Sandeep for not allowing -ve value
										if (iRLnSplitAccountingsAmount1 < 0)

										{
											Log.customer.debug("iRLnSplitAccountingsAmountMoney is -ve");
											int multi3 = -1;
											BigDecimal result3 = new BigDecimal(iRLnSplitAccountingsAmount1).multiply(new BigDecimal(multi3));
										iRLnFmtSplitAccountings1 = result3.doubleValue();
										}
										else
										{
									Log.customer.debug("iRLnSplitAccountingsAmountMoney is +ve");
									iRLnFmtSplitAccountings1   = iRLnSplitAccountingsAmount1;
									  }

									}
									iRLnFmtSplitAccountingsAmount = CATFaltFileUtil.getEzThousandFormattedNumber(iRLnFmtSplitAccountings1,"0000000000000000");
									Log.customer.debug("iRLnFmtSplitAccountingsAmount ==> " +iRLnFmtSplitAccountingsAmount);

									 //  CDG7-CANALYSE1	X(15)	LineItems.PONumber
									String iRLnPONumber = "";
									String iRLnFmtPONumber = "";
									if (IrLineItem2.getFieldValue("PONumber") != null){
										iRLnPONumber = (String)IrLineItem2.getFieldValue("PONumber");

										iRLnFmtPONumber = CATFaltFileUtil.getFormattedTxt(iRLnPONumber,15);
									}
									Log.customer.debug("iRLnFmtPONumber ==> " +iRLnFmtPONumber);

									 //  IsVATRecoverable	X(3)	LineItems.IsVatRecoverable
									Boolean iRLnIsVatRecoverableBool = null;
									String iRLnFmtIsVatRecoverable = "";
									/** VIJAY : COMMENT CODE : Issue identified on the Prod Cutover : 05/08/2011 : START **/
									/*
									String iRLnFmtIsVatRecoverable = "";
									Log.customer.debug("IrLineItem2.getFieldValue(IsVatRecoverabl) ==> " +IrLineItem2.getFieldValue("IsVATRecoverable"));
									if (IrLineItem2.getFieldValue("IsVATRecoverable") != null && IrLineItem2.getDottedFieldValue("LineType.Category").toString() !="2"){
										iRLnIsVatRecoverableBool = (Boolean)IrLineItem2.getFieldValue("IsVATRecoverable");
										Log.customer.debug("iRLnIsVatRecoverableBool ==> " +iRLnIsVatRecoverableBool);
										if(iRLnIsVatRecoverableBool.booleanValue()){
											iRLnIsVatRecoverable = "yes";
										} 
										//else {
										//	iRLnIsVatRecoverable = "no";
										//}
									}
										//Sandeep Changed as per Eric's request.
										else if (IrLineItem2.getDottedFieldValue("LineType.Category").toString().equals("2")){
											iRLnIsVatRecoverable = "no";
									Log.customer.debug("Line Cat is 2 hence Vat Recoverable is N");

										}
                                              else
                                              {
												  iRLnIsVatRecoverable = "no";
											  }
									*/
									/** VIJAY : COMMENT CODE : Issue identified on the Prod Cutover : 05/08/2011 : END **/
									/** VIJAY : FIX CODE : Issue identified on the Prod Cutover : 05/08/2011 : START **/
									String iRLnIsVatRecoverable = "no";
									if (IrLineItem2.getFieldValue("IsVATRecoverable") != null) {
										iRLnIsVatRecoverableBool = (Boolean)IrLineItem2.getFieldValue("IsVATRecoverable");
										String strLineTypeCategory = null;
										if (IrLineItem2.getDottedFieldValue("LineType.Category") != null) {
											strLineTypeCategory = IrLineItem2.getDottedFieldValue("LineType.Category").toString();
											if(iRLnIsVatRecoverableBool.booleanValue() && strLineTypeCategory != null && !strLineTypeCategory.equals("2")) {
												iRLnIsVatRecoverable = "yes";
											}
										}
									}
									/** VIJAY : FIX CODE : Issue identified on the Prod Cutover : 05/08/2011 : END **/

										iRLnFmtIsVatRecoverable = CATFaltFileUtil.getFormattedTxt(iRLnIsVatRecoverable,3);
										Log.customer.debug("iRLnFmtIsVatRecoverable inside ==> " +iRLnFmtIsVatRecoverable);



									Log.customer.debug("iRLnFmtIsVatRecoverable ==> " +iRLnFmtIsVatRecoverable);

									 //  CompDivision	X(3)	LineItems.Accountings.SplitAccountings.CompDivision
									String iRLnSplitAccountingsCompDivision = "";
									String iRLnFmtSplitAccountingsCompDivision = "";
									if (splitAcc.getFieldValue("CompDivision") != null){
										iRLnSplitAccountingsCompDivision = (String)splitAcc.getFieldValue("CompDivision");

									}
									iRLnFmtSplitAccountingsCompDivision = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsCompDivision,3);
									Log.customer.debug("iRLnFmtSplitAccountingsCompDivision ==> " +iRLnFmtSplitAccountingsCompDivision);



									 //  CompSection	X(2)	LineItems.Accountings.SplitAccountings.CompSection
									String iRLnSplitAccountingsCompSection = "";
									String iRLnFmtSplitAccountingsCompSection = "";
									if (splitAcc.getFieldValue("CompSection") != null){
										iRLnSplitAccountingsCompSection= (String)splitAcc.getFieldValue("CompSection");

									}
									iRLnFmtSplitAccountingsCompSection = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsCompSection,2);
									Log.customer.debug("iRLnFmtSplitAccountingsCompSection ==> " +iRLnFmtSplitAccountingsCompSection);

									 //  CompExpenseAccount	X(4)	LineItems.Accountings.SplitAccountings.CompExpenseAccount
									String iRLnSplitAccountingsCompExpenseAccount = "";
									String iRLnFmtSplitAccountingsCompExpenseAccount = "";
									if (splitAcc.getFieldValue("CompExpenseAccount") != null){
										iRLnSplitAccountingsCompSection= (String)splitAcc.getFieldValue("CompExpenseAccount");

									}
									iRLnFmtSplitAccountingsCompExpenseAccount = CATFaltFileUtil.getFormattedTxt(iRLnSplitAccountingsCompExpenseAccount,4);
									Log.customer.debug("iRLnFmtSplitAccountingsCompExpenseAccount ==> " +iRLnFmtSplitAccountingsCompExpenseAccount);

									//  ReferencePOLIne	X(3)	LineItems.POLineItemNumber
									//String iRLnPOLineItemNumber = "";
									int iRLnPOLineItemNumberINT = 0;
									String iRLnFmtPOLineItemNumber = "   ";
									if (IrLineItem2.getFieldValue("POLineItemNumber") != null){
										Object iRLnPOLineItemNumberObj = IrLineItem2.getFieldValue("POLineItemNumber");
										Log.customer.debug(" inside testing iRLnPOLineItemNumberObj 1 ==> " + iRLnPOLineItemNumberObj);
										String iRLnPOLineItemNumberIntStr1 = ""+iRLnPOLineItemNumberObj;
										Log.customer.debug(" inside testing iRLnPOLineItemNumberIntStr1 1 ==> " + iRLnPOLineItemNumberIntStr1);
										iRLnPOLineItemNumberINT = Integer.parseInt(iRLnPOLineItemNumberIntStr1);
										String iRLnPOLineItemNumberIntStr = ""+iRLnPOLineItemNumberINT;
										iRLnFmtPOLineItemNumber = CATFaltFileUtil.getFormattedTxt(iRLnPOLineItemNumberIntStr,3);
									}
									Log.customer.debug("iRLnFmtPOLineItemNumber ==> " +iRLnFmtPOLineItemNumber);

									 //  UnitOfMeaseureName	X(20)	LineItems.Description.UnitOfMeasure.Name
									String iRLnUnitOfMeasureDsc = "";
									String iRLnFmtUnitOfMeasureDsc = "";
									if (IrLineItem2.getDottedFieldValue("Description.UnitOfMeasure") != null){
										UnitOfMeasure  unitOfMeasureDsc = (UnitOfMeasure ) IrLineItem2.getDottedFieldValue("Description.UnitOfMeasure");
										iRLnUnitOfMeasureDsc = (String)unitOfMeasureDsc.getDottedFieldValue("Name.PrimaryString");
									}
                                                                                iRLnFmtUnitOfMeasureDsc = CATFaltFileUtil.getFormattedTxt(iRLnUnitOfMeasureDsc,20);
									Log.customer.debug("iRLnFmtUnitOfMeasureDsc UnitOfMeaseureName	X(20) ==> " +iRLnFmtUnitOfMeasureDsc);


									 //  ControlDate	X(26)	ControlDate
									Date iRControlDate  =null;
									String iRFmtControlDate ="";
									if ( invrecon.getFieldValue("ControlDate") != null){
										iRControlDate =  (Date) invrecon.getFieldValue("ControlDate");
										Log.customer.debug("iRControlDate @@@ ==> " +iRControlDate);
										iRFmtControlDate = CATFaltFileUtil.getFormattedDate(iRControlDate,"yyyy-MM-dd-hh.mm.ss.ssssss");
									}

									Log.customer.debug("iRFmtControlDate ==> " +iRFmtControlDate);



						Log.customer.debug("IR data writing to file  ==> ");
						String iRData =  iRFmtiRHeadder+"~|"+invFmtUniqueName+"~|"+iRFmtFacilityFlag+"~|"+iRFmtBlockStampDate+"~|"+iRFmtCatInvoiceNumber+"~|"+iRFmtTimeCreated+"~|"+iRFmtSupplierLocation+"~|"+iRFmtCurrency+"~|"+iRFmtcdg2Csens+"~|"+iRFmtTotalCost+"~|"+iRFmtBVRNumber+"~|"+iRFmtSupplierInvoiceDate+"~|"+iRFmtInvoiceNumber+"~|"+iRFmtOriginVATCountry+"~|"+iRFmtIsVATReverseCharge+"~|"+iRFmtCatVATCountryCode+"~|"+iRFmtPaymentDueDate+"~|"+iRFmtRelatedCatInvoice+"~|"+iRFmtLineTypeCategory+"~|"+iRLnFmtSplitAccountingsAccountingFacility+"~|"+iRLnFmtSplitAccountingsDepartment+"~|"+iRLnFmtSplitAccountingsDivision+"~|"+iRLnFmtSplitAccountingsSection+"~|"+iRLnFmtSplitAccountingsExpenseAccount+"~|"+iRLnFmtSplitAccountingsOrder+"~|"+iRLnFmtSplitAccountingsMisc+"~|"+iRLnFmtSplitAccountingsAmountAmount+"~|"+irFmtIlQuantity+"~|"+iRLnFmtunitOfMeasure+"~|"+iRLnFmtPriceAmount+"~|"+iRLnFmtDescription+"~|"+iRLnFmtSplitAccountingsAmount+"~|"+iRLnFmtPONumber+"~|"+iRLnFmtIsVatRecoverable+"~|"+iRLnFmtSplitAccountingsCompDivision+"~|"+iRLnFmtSplitAccountingsCompSection+"~|"+iRLnFmtSplitAccountingsCompExpenseAccount+"~|"+iRLnFmtPOLineItemNumber+"~|"+iRLnFmtUnitOfMeasureDsc+"~|"+iRFmtControlDate;

						Log.customer.debug("IR data writing to file  ==> " +iRData);
						outPW_FlatFile.write(iRData);
						Log.customer.debug("New Line writing to file  ==> ");
						outPW_FlatFile.write("\n");



								//}
							}

								} //for (int j = 0; j<irSplitaccSize;j++)

							} //if (irSplitaccSize > 0)

							} // if (irAccounting!=null)

								} //for (int i =0; i<IrLineItemvector;i++)
						} //if (lineCount > 0)

						    // marking flushed as 'true'. the method getControlFileData
						    // starts a new transaction so the ir object will be flushed
                        flushed = true;
						String ctrlDataForIr = getControlFileData(invrecon,controlid);
						//Log.customer.debug("write to control file.. " + ctrlDataForIr + "for controlid = "+ controlid);
						Log.customer.debug("IR control ### data writing to file  ==> " +ctrlDataForIr);
						outPW_CTRLFlatFile.write(ctrlDataForIr);
						Log.customer.debug(" ### New Line writing to file  ==> ");
						outPW_CTRLFlatFile.write("\n");
						pushedCount++;

					}  // if(invrecon != null)

					commitCount++;
					if(commitCount == 25)
					   {
							Log.customer.debug("**********Commiting IR Records*******  ",commitCount);
							Base.getSession().transactionCommit();
							commitCount = 0;
						}
						continue;

			} //  while
			Base.getSession().transactionCommit();

		} catch (Exception e) {
			//add message
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , classname);
            if (lastAccessedIR != null) {
            	String uniqueName = lastAccessedIR.getUniqueName();

            	    // the object is flushed. we need to reconstitute the object from the db
            	if (flushed) {

        			Log.customer.debug(
        					"%s IR is flushed. Fetching DB Copy",
        					classname);
            		String [] uniqueKeys = {uniqueName};
            		InvoiceReconciliation irDBCopy =
            			(InvoiceReconciliation) InvoiceReconciliation.lookup(
            					uniqueKeys,
            					InvoiceReconciliation.ClassName,
            					partition);
        			Log.customer.debug(
        					"%s Database Copy: IRID %s, Reference %s",
        					classname,
        					uniqueName,
        					irDBCopy);

        			    // action flag has been set. we need to revert this
        			if (isActionFlagSet) {
            		    irDBCopy.setFieldValue("ActionFlag", "InProcess");
            		    isActionFlagSet = false;
        			}
            	}
            	else {

    			        // action flag has been set. we need to revert this
            		if (isActionFlagSet) {
                        lastAccessedIR.setFieldValue("ActionFlag", "InProcess");
            		    isActionFlagSet = false;
            		}
            	}
    			Log.customer.debug(
    					"%s IR ID: %s, Reference: %s - Action flag reverted to 'InProcess'",
    					classname,
    					uniqueName,
    					lastAccessedIR);
            }
            else {
    			Log.customer.debug("" +
    					"%s Last Accessed IR is null. Unable to revert flag to 'InProcess'",
    					classname);
            }

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("EZOPENIRPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "EasyOpenInvoiceReconciliationPush Task Status - Failed";
			new ScheduledTaskException("Error : " + e.toString(), e);

			Log.customer.debug(e);
		}
		finally {
			if (outPW_CTRLFlatFile != null)  {
				outPW_CTRLFlatFile.flush();
				outPW_CTRLFlatFile.close();}

			if (outPW_FlatFile != null)  {
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
			Log.customer.debug("%s: Inside Finally ", classname);
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", classname);
			message.append("\n");
			endTime = DateFormatter.getStringValue(new Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("Records to be pushed : "+ resultCount);
			message.append("\n");
			message.append("No. of records successfully pushed : "+ pushedCount);
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , classname);
			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "EasyOpenIRPushNotify");
			message = null;
			pushedCount =0;
			resultCount =0;


		}


	}





	public String getControlFileData(InvoiceReconciliation invrecon, String controlid) throws Exception
	{
		Log.customer.debug("Inside Control file data generation ... invrecon .."+invrecon );
		Log.customer.debug("Inside Control file data generation ... controlid .."+controlid );
		String topicname1 = new String("ControlObjectPush");
		Partition p2 = Base.getService().getPartition("None");
		ClusterRoot cluster = null;


		String ctrlQuery = new String( "Select from cat.core.ControlPullObject where UniqueName = '"+controlid + "'" );
		Log.customer.debug("iRQuery ==> " + ctrlQuery);
		aqlctrlQuery = AQLQuery.parseQuery(ctrlQuery);
		ctrlResultSet = Base.getService().executeQuery(ctrlQuery, options);

		if (ctrlResultSet.getErrors() != null)
		     Log.customer.debug("ERROR GETTING RESULTS in irResultDX");

		int totalNumberOfCtrl = ctrlResultSet.getSize();

		Log.customer.debug("Inside getControlFileData ... totalNumberOfCtrl "+totalNumberOfCtrl );

		if (totalNumberOfCtrl == 0) {
		        cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p2);
		        Log.customer.debug("Inside else part getControlFileData ... cluster "+cluster );
		}
		else
			while(ctrlResultSet.next()){
			cluster = (ClusterRoot)ctrlResultSet.getBaseId("ControlPullObject").get();
			Log.customer.debug("Inside else part getControlFileData ... cluster "+cluster );
			}
		Log.customer.debug("Inside getControlFileData ... controlid "+controlid );
		cluster.setFieldValue("UniqueName", controlid);
		Log.customer.debug("Inside getControlFileData ... datetimezone "+datetimezone );
		cluster.setFieldValue("ControlDate", datetimezone);
		Log.customer.debug("Inside getControlFileData ... MSC_EZOPEN_INVOICES ");
		cluster.setFieldValue("InterfaceName", "MSC_EZOPEN_INVOICES");
		Log.customer.debug("Inside getControlFileData SourceSystem..........");
		cluster.setFieldValue("SourceSystem", "Ariba_vcsv3_ezopen");
		Log.customer.debug("5..........");
		cluster.setFieldValue("SourceFacility", "        ");	//8 Spaces
		Log.customer.debug("6..........");
		cluster.setFieldValue("TargetSystem", "EZOPEN");
		Log.customer.debug("7..........");
		cluster.setFieldValue("TargetFacility", "EZOPEN");
		Log.customer.debug("8..........");
		cluster.setFieldValue("RecordCount", new Integer(1));
		String ctrlData = "";

		if (bdTotCost != null)
		{
			bdTotCost =  bdTotCost.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
			cluster.setFieldValue("TotalAmount", bdTotCost);
			cluster.save();
		}
		Log.customer.debug("Area2 .........."+ iSpAcct);
		cluster.setFieldValue("Area2", new Integer(iSpAcct));	//Sum of splitaccountings
		cluster.save();
		Log.customer.debug("Area2.......... saved");
		cluster.setFieldValue("Area3", "                                             ");	//48 Spaces
		Log.customer.debug("Area2..........");
		cluster.save();
		Log.customer.debug("saved ..........");

		//Base.getSession().transactionCommit();
		//Log.customer.debug("after  transactionCommit() ..........");

		//if(cluster.isSaved() && cluster != null)
		try
		{
			//UNIQUE-ID(30)   UniqueName
			String uniqueNameCRTLObj = (String)cluster.getFieldValue("UniqueName");
			String uniqueNameCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(uniqueNameCRTLObj,30);
			//DATE-TIME(26) ControlDate
			Date controlDateCRTLObj = (Date)cluster.getFieldValue("ControlDate");
			//String CcontrolDateCRTLObjFmt =  CATFaltFileUtil.getFormattedDate(controlDateCRTLObj,"yyyy-MM-dd-hh.mm.ss");
			//Changed by sandeep
			String CcontrolDateCRTLObjFmt =
				CATFaltFileUtil.getFormattedDate(controlDateCRTLObj,"yyyy-MM-dd-hh.mm.ss.ssssss");

			//INTERFACE-NAME(80)  InterfaceName
			String interfaceNameCRTLObj = (String)cluster.getFieldValue("InterfaceName");
			String interfaceNameCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(interfaceNameCRTLObj,80);

			//SOURCE-SYSTEM(20)  SourceSystem
			String sourceSystemCRTLObj = (String)cluster.getFieldValue("SourceSystem");
			String sourceSystemCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(sourceSystemCRTLObj,20);

			//SOURCE-FACILITY(8)  SourceFacility
			String sourceFacilityCRTLObj = (String)cluster.getFieldValue("SourceFacility");
			String sourceFacilityCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(sourceFacilityCRTLObj,8);

			//TARGET-SYSTEM(20)  TargetSystem
			String targetSystemCRTLObj = (String)cluster.getFieldValue("TargetSystem");
			String targetSystemCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(targetSystemCRTLObj,20);

			//TARGET-FACILITY(8) TargetFacility
			String targetFacilityCRTLObj = (String)cluster.getFieldValue("TargetFacility");
			String targetFacilityCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(targetFacilityCRTLObj,8);

			//RECORD-COUNT(15)  RecordCount Numeric value of number of records sent on the day. (For ezopen this will always be 1)
			String recordCountCRTLObj = "1";
			String recordCountCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(recordCountCRTLObj,15);

			//USER-AREA-1(45)    TotalAmount Total invoice amount
                        double totalAmountCRTLObjDbl = 0.0;
			BigDecimal totalAmountCRTLObj1 = (BigDecimal)cluster.getFieldValue("TotalAmount");
                        double totalAmountCRTLObj = totalAmountCRTLObj1.doubleValue();
			if (totalAmountCRTLObj < 0)
			{
				Log.customer.debug("TotalCost in CNTRL File is -ve");
	       		int multi4 = -1;
	       		BigDecimal result4 = new BigDecimal(totalAmountCRTLObj).multiply(new BigDecimal(multi4));
	       		 totalAmountCRTLObjDbl = result4.doubleValue();
			}
			else
			{
				Log.customer.debug("TotalCost in CNTRL File is +ve");
				totalAmountCRTLObjDbl = totalAmountCRTLObj;
			}

		//	String totalAmountCRTLObjStr = Double.toString(totalAmountCRTLObjDbl);
		//	String totalAmountCRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(totalAmountCRTLObjStr,45);
		//Changed by Sandeep
String totalAmountCRTLObjFmt = CATFaltFileUtil.getEzThousandFormattedNumber(totalAmountCRTLObjDbl,"0000000000000000");

			//USER-AREA-2(45)  Area2
			String area2CRTLObj = (String)cluster.getFieldValue("Area2");
			String area2CRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(area2CRTLObj,45);

			//USER-AREA-3(45) Not used
			String area3CRTLObj = " ";
			String area3CRTLObjFmt =  CATFaltFileUtil.getFormattedTxt(area3CRTLObj,45);

			ctrlData = uniqueNameCRTLObjFmt+"~|"+CcontrolDateCRTLObjFmt+"~|"+interfaceNameCRTLObjFmt+"~|"+sourceSystemCRTLObjFmt+"~|"+sourceFacilityCRTLObjFmt+"~|"+targetSystemCRTLObjFmt+"~|"+targetFacilityCRTLObjFmt+"~|"+recordCountCRTLObjFmt+"~|"+totalAmountCRTLObjFmt+"~|"+area2CRTLObjFmt+"~|"+area3CRTLObjFmt;
			Log.customer.debug("before return ctrl data ...."+ ctrlData);


		}
		catch(Exception e)
		{
			if (cluster == null) {
					Log.customer.debug("Cluster is null after the push....");
			}
			Log.customer.debug(e.toString());
			throw e;
		}
		//Base.getSession().transactionCommit();
		Log.customer.debug("return ctrl data ...."+ ctrlData);
		return ctrlData;


	}

	String getDateTime(Date datetime)
	{
		int yy = (new Integer(Date.getYear(datetime))).intValue();
		int mm = (new Integer(Date.getMonth(datetime))).intValue();
		int dd = (new Integer(Date.getDayOfMonth(datetime))).intValue();
		int hh = (new Integer(Date.getHours(datetime))).intValue();
		int mn = (new Integer(Date.getMinutes(datetime))).intValue();
		int ss = (new Integer(Date.getSeconds(datetime))).intValue();
		mm++;
		String retstr = new String ("");
		retstr = retstr + yy;

		if ( mm/10 == 0)	retstr = retstr + "0" + mm;
		else	retstr = retstr + mm;

		if ( dd/10 == 0)	retstr = retstr + "0" + dd;
		else	retstr = retstr + dd;

		if ( hh/10 == 0)	retstr = retstr + "0" + hh;
		else	retstr = retstr + hh;

		if ( mn/10 == 0)	retstr = retstr + "0" + mn;
		else	retstr = retstr + mn;

		if ( ss/10 == 0)	retstr = retstr + "0" + ss;
		else	retstr = retstr + ss;

		return retstr;
    }



}

