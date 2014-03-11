package config.java.schedule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.TimeZone;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.basic.core.PostalAddress;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.DirectOrder;
import ariba.purchasing.core.POLineItem;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.schedule.util.CATFaltFileUtil;


public class CATCliDBFlatFilePush extends ScheduledTask {

	private String classname = "CATUSCliDBFlatFilePush";
	private Date datetimezone;
	private String startTime, endTime;
	private FastStringBuffer message = null;
	private String mailSubject = null;
	private String fileExtDateTime ;
	private String poLineItemflatFilePath, poAccDisflatFilePath;
	private String triggerFile = "";
	private PrintWriter outPW_poLineFlatFile,outPW_poAccDisFlatFile;
	private AQLOptions options;
	private Partition partition = null;
	private AQLResultCollection poResultSet;
	private int totalNumberOfPOs;
	private AQLQuery aqlPOQuery;
	private SplitAccountingCollection poAccounting = null;
	private int  poSplitaccSize,iSpAcct;
	private BaseVector poSplitAccounting = null;
	private String poFmtUniqueName;
	private Object requesterName = null;

	public void run() throws ScheduledTaskException {

		try{
			partition = Base.getService().getPartition();
			datetimezone = new Date();
			startTime =	ariba.util.formatter.DateFormatter.getStringValue(	new ariba.util.core.Date(),	"EEE MMM d hh:mm:ss a z yyyy",TimeZone.getTimeZone("CST"));
			message = new FastStringBuffer();
			mailSubject ="CATCliDBFlatFilePush Task Completion Status - Completed Successfully";
			Date date = new Date();
			fileExtDateTime = CATFaltFileUtil.getFileExtDateTime(date);
			if (partition.getName().equalsIgnoreCase("pcsv1")) {
				poLineItemflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_pcsv1_CLIDBPOLineItem_PUSH."+ fileExtDateTime + ".txt";
				poAccDisflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_pcsv1_CLIDBPOSPLITACC_PUSH."+ fileExtDateTime + ".txt";
			triggerFile = "/msc/arb9r1/downstream/catdata/CLI/MSC_CLIDB_PO_US_PUSH."+ fileExtDateTime + ".dstrigger";
			}
			if (partition.getName().equalsIgnoreCase("ezopen")) {
				poLineItemflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_ezopen_CLIDBPOLineItem_PUSH."+ fileExtDateTime + ".txt";
				poAccDisflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_ezopen_CLIDBPOSPLITACC_PUSH."+ fileExtDateTime + ".txt";
				triggerFile = "/msc/arb9r1/downstream/catdata/CLI/MSC_CLIDB_PO_EZOPEN_PUSH."+ fileExtDateTime + ".dstrigger";
		   }
			if (partition.getName().equalsIgnoreCase("mfg1")) {
				poLineItemflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_mfg1_CLIDBPOLineItem_PUSH."+ fileExtDateTime + ".txt";
				poAccDisflatFilePath = "/msc/arb9r1/downstream/catdata/CLI/MSC_mfg1_CLIDBPOSPLITACC_PUSH."+ fileExtDateTime + ".txt";
				triggerFile = "/msc/arb9r1/downstream/catdata/MSC_CLIDB_PO_MFG1_PUSH."+ fileExtDateTime + ".dstrigger";
				}

			Log.customer.debug("poLineItemflatFilePath " + poLineItemflatFilePath);
			Log.customer.debug("poAccDisflatFilePath " + poAccDisflatFilePath);
			Log.customer.debug("triggerFile " + triggerFile);

			File poLineItemflatFile = new File(poLineItemflatFilePath);
			File poAccDisflatFile = new File(poAccDisflatFilePath);


			options = new AQLOptions(partition,true);

			if (!poLineItemflatFile.exists()) {
				Log.customer.debug(" poLineItemflatFile File not exist creating file ..");
				poLineItemflatFile.createNewFile();
			}
			if (!poAccDisflatFile.exists()) {
				Log.customer.debug(" poAccDisflatFile File not exist creating file ..");
				poAccDisflatFile.createNewFile();
			}


			DirectOrder po = null;

			outPW_poLineFlatFile =new PrintWriter(IOUtil.bufferedOutputStream(poLineItemflatFile),true);
			Log.customer.debug("outPW_poLineFlatFile " + outPW_poLineFlatFile);
			outPW_poAccDisFlatFile =new PrintWriter(IOUtil.bufferedOutputStream(poAccDisflatFile),true);
			Log.customer.debug("outPW_poAccDisFlatFile " + outPW_poAccDisFlatFile);





			String pOQuery = new String( "select from ariba.purchasing.core.DirectOrder where StatusString = 'Receiving' OR StatusString = 'Ordered'");
			Log.customer.debug("pOQuery ==> " + pOQuery);
			aqlPOQuery = AQLQuery.parseQuery(pOQuery);
			poResultSet = Base.getService().executeQuery(aqlPOQuery, options);

			if (poResultSet.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in irResultSetUSCliDB");

			totalNumberOfPOs = poResultSet.getSize();
			Log.customer.debug("totalNumberOfs ==> " + totalNumberOfPOs);
			int count = 0;
			while(poResultSet.next()){

				po = (DirectOrder)(poResultSet.getBaseId("DirectOrder").get());
				if(po != null){
					int lineCount = po.getLineItemsCount();
					BaseVector PoLineItemVector = (BaseVector)po.getLineItems();

					Log.customer.debug("%s::Line Item count for IR:%s ",classname,lineCount);
					if (lineCount > 0){
						for (int i =0; i<lineCount;i++){
							Log.customer.debug("%s::inside for (int i =0; i<lineCount;();i++){ i value %s ",classname,i);
							POLineItem  poLineItem2 = (POLineItem )PoLineItemVector.get(i);
							Log.customer.debug("%s::... poLineItem2 %s ",classname,poLineItem2);

							poAccounting = poLineItem2.getAccountings();
							Log.customer.debug("%s::... irAccounting %s ",classname,poAccounting);

							if (poAccounting!=null){
								iSpAcct = 0;
								Log.customer.debug("%s::inside if (irAccounting!=null){ %s ",classname,poAccounting);

								poSplitAccounting = (BaseVector)poAccounting.getSplitAccountings();
								poSplitaccSize = poSplitAccounting.size();
								Log.customer.debug("%s::Split acc size:%s",classname,poSplitaccSize);
								if (poSplitaccSize > 0){
									if (poSplitaccSize == 1) {   // <<<<<<<<<<<<<<<<  poSplitaccSize  > 1 POLineItem File  Start  >>>>>>>>>>>>>>>>>>>>>>>
									iSpAcct+= poSplitaccSize;
									for(Iterator s= poAccounting.getSplitAccountingsIterator(); s.hasNext();) {
										SplitAccounting splitAcc = (SplitAccounting) s.next();
										if (splitAcc != null) {
											for (int j = 0; j<poSplitaccSize;j++){
												Log.customer.debug ("%s::Inside for (int j = 0; j<poSplitaccSize;j++){",classname);

												//UniqueName	12
												String poUniqueName = po.getFieldValue("UniqueName").toString();
												Log.customer.debug("poUniqueName ==> " +poUniqueName);
												poFmtUniqueName = CATFaltFileUtil.getFormattedTxt(poUniqueName,12);
												Log.customer.debug("poUniqueName ==> " +poFmtUniqueName);

												//LineItems. NumberInCollection	3
												String numberInCollection="";
												String numberInCollectionFmt="";
												if (poLineItem2.getFieldValue("NumberInCollection")!= null)
													numberInCollection = poLineItem2.getFieldValue("NumberInCollection").toString();
												Log.customer.debug("numberInCollection ==> " +numberInCollection);
												numberInCollectionFmt = CATFaltFileUtil.getFormattedTxt(numberInCollection,3);
												Log.customer.debug("numberInCollectionFmt ==> " +numberInCollectionFmt);

												//LineItems.BuyerCode.BuyerCode	2
												//left justified Always send upper case
												String buyerCode="";
												String buyerCodeFmt="";
												if (poLineItem2.getFieldValue("BuyerCode")!= null)
													buyerCode = poLineItem2.getDottedFieldValue("BuyerCode.BuyerCode").toString();
												Log.customer.debug("buyerCode ==> " +buyerCode);
												buyerCodeFmt = CATFaltFileUtil.getFormattedTxt(buyerCode,2);
												Log.customer.debug("buyerCodeFmt ==> " +buyerCodeFmt);


												//CreateDate	10
												String poFmtCreateDate ="";
												Date poCreateDate = (Date)po.getFieldValue("TimeCreated");
												Log.customer.debug("poCreateDate ==> " +poCreateDate);
												poFmtCreateDate = CATFaltFileUtil.getFormattedDate(poCreateDate);
												Log.customer.debug("poFmtCreateDate ==> " +poFmtCreateDate);

												//CreateDate	10
												String poFmtCreateDate1 = poFmtCreateDate;
												Log.customer.debug("poFmtCreateDate1 ==> " +poFmtCreateDate1);

												//SupplierLocation.UniqueName	7
												String supplierLocationUniqueName="";
												String supplierLocationUniqueNameFmt="";
												if (poLineItem2.getFieldValue("SupplierLocation")!= null)
													supplierLocationUniqueName = poLineItem2.getDottedFieldValue("SupplierLocation.UniqueName").toString();
												Log.customer.debug("supplierLocationUniqueName ==> " +supplierLocationUniqueName);
												supplierLocationUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(supplierLocationUniqueName,7);
												Log.customer.debug("supplierLocationUniqueNameFmt ==> " +supplierLocationUniqueNameFmt);

												//SupplierLocation.Name	40
												String supplierLocationName="";
												String supplierLocationNameFmt="";
												if (poLineItem2.getFieldValue("SupplierLocation")!= null)
													supplierLocationName = poLineItem2.getDottedFieldValue("SupplierLocation.Name").toString();
												Log.customer.debug("supplierLocationName ==> " +supplierLocationName);
												supplierLocationNameFmt = CATFaltFileUtil.getFormattedTxt(supplierLocationName,40);
												Log.customer.debug("supplierLocationNameFmt ==> " +supplierLocationNameFmt);

												//LineItems. BuyerCode. BuyerCode	2
												String buyerCodeFmt2=buyerCodeFmt;
												Log.customer.debug("buyerCodeFmt2 ==> " +buyerCodeFmt2);


												//LineItems. Accountings.SplitAccountings.AccountingFacility	2

												String accountingFacility ="";
												String accountingFacilityFmt ="";
												if (splitAcc.getFieldValue("AccountingFacility")!= null) {
													accountingFacility = splitAcc.getFieldValue("AccountingFacility").toString();
													Log.customer.debug("accountingFacility ==> " +accountingFacility);
												}
												accountingFacilityFmt = CATFaltFileUtil.getFormattedTxt(accountingFacility,2);
												Log.customer.debug("accountingFacilityFmt ==> " +accountingFacilityFmt);


												//LineItems. Accountings. SplitAccountings. AccountingFacility	2
												String accountingFacilityFmt2 = accountingFacilityFmt;
												Log.customer.debug("accountingFacilityFmt2 ==> " +accountingFacilityFmt2);



												//***pcsv1 only***LineItems. ShipTo. ReceivingFacility
												//***ezopen only*** LineItems.Accounting.SplitAccounting.AccountingFacility	2

												String receivingFacility = "";
												String receivingFacilityFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (poLineItem2.getFieldValue("ShipTo")!= null){
														Address shipTo = (Address)poLineItem2.getFieldValue("ShipTo");
														if ( shipTo.getFieldValue("ReceivingFacility")!= null){
														   receivingFacility = poLineItem2.getDottedFieldValue("ShipTo.ReceivingFacility").toString();
														}
														else
															receivingFacility = " ";

													}
													receivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(receivingFacility,2);
													Log.customer.debug("receivingFacilityFmt for pcsv1 ==> " +receivingFacilityFmt);
												}
												else{
													receivingFacilityFmt = accountingFacilityFmt;
													Log.customer.debug("receivingFacilityFmt/accountingFacilityFmt for Non pcsv1 ==> " +receivingFacilityFmt);
												}

												//***pcsv1 only*** LineItems. ShipTo. DockCode	2
												String dockCode = "";
												String dockCodeFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (poLineItem2.getFieldValue("ShipTo")!= null){
														Address shipTo = (Address)poLineItem2.getFieldValue("ShipTo");
														if ( shipTo.getFieldValue("DockCode")!= null){
														dockCode = poLineItem2.getDottedFieldValue("ShipTo.DockCode").toString();
														}
														else
															dockCode ="";

													}
													dockCodeFmt = CATFaltFileUtil.getFormattedTxt(dockCode,2);
													Log.customer.debug("dockCodeFmt for pcsv1 ==> " +dockCodeFmt);
												}
												else{
													dockCodeFmt =  CATFaltFileUtil.getFormattedTxt(dockCode,2);
													Log.customer.debug("dockCodeFmt  for non pcsv1 ==> " +dockCodeFmt);

												}
												//SupplierLocation.FOBPoint. UniqueName	6

												String fOBPointUniqueName ="";
												String fOBPointUniqueNameFmt ="";
												if (po.getDottedFieldValue("SupplierLocation.FOBPoint")!= null) {
													Address fOBPoint = (Address) po.getDottedFieldValue("SupplierLocation.FOBPoint");
													fOBPointUniqueName = fOBPoint.getFieldValue("UniqueName").toString();
													Log.customer.debug("fOBPointUniqueName ==> " +fOBPointUniqueName);
												}
												fOBPointUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(fOBPointUniqueName,6);
												Log.customer.debug("fOBPointUniqueNameFmt ==> " +fOBPointUniqueNameFmt);

												//***pcsv1 only*** LineItems. ShipTo.CityStateCode	7
												String cityStateCode = "";
												String cityStateCodeFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (poLineItem2.getFieldValue("ShipTo")!= null){
														Address shipTo = (Address)poLineItem2.getFieldValue("ShipTo");
														if (shipTo.getFieldValue("CityStateCode")!= null){
														    cityStateCode = poLineItem2.getDottedFieldValue("ShipTo.CityStateCode").toString();
														}
														else
															cityStateCode =""	;

													}
													cityStateCodeFmt = CATFaltFileUtil.getFormattedTxt(cityStateCode,7);
													Log.customer.debug("cityStateCodeFmt for pcsv1 ==> " +cityStateCodeFmt);
												}
												else{
													cityStateCodeFmt = CATFaltFileUtil.getFormattedTxt(cityStateCode,7);
													Log.customer.debug("cityStateCodeFmt  for non pcsv1 ==> " +cityStateCodeFmt);

												}


												//LineItems. ShipTo.PostalAddress.PostalCode	6
												String postalCode = "";
												String postalCodeFmt = "";
												if (poLineItem2.getDottedFieldValue("ShipTo.PostalAddress")!= null) {
													PostalAddress postalAddress = (PostalAddress) poLineItem2.getDottedFieldValue("ShipTo.PostalAddress");
													postalCode = postalAddress.getFieldValue("PostalCode").toString();
													Log.customer.debug("postalCode ==> " +postalCode);
												}
												postalCodeFmt = CATFaltFileUtil.getFormattedTxt(postalCode,6);
												Log.customer.debug("postalCodeFmt ==> " +postalCodeFmt);


												//LineItems. ShipTo. PostalAddress. Country. UniqueName	9
												String countryUniqueName = "";
												String countryUniqueNameFmt = "";
												if (poLineItem2.getDottedFieldValue("ShipTo.PostalAddress")!= null) {
													PostalAddress postalAddress = (PostalAddress) poLineItem2.getDottedFieldValue("ShipTo.PostalAddress");
													countryUniqueName = postalAddress.getDottedFieldValue("Country.UniqueName").toString();
													Log.customer.debug("countryUniqueName ==> " +countryUniqueName);
												}
												countryUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(countryUniqueName,9);
												Log.customer.debug("countryUniqueNameFmt ==> " +countryUniqueNameFmt);


												//RequesterName	2
												String requesterName ="";
												String requesterNameFmt ="";
												if (po.getFieldValue("RequesterName") != null) {
													requesterName = po.getFieldValue("RequesterName").toString();
													Log.customer.debug("requesterName ==> " +requesterName);
												}
												else {
													requesterName = po.getDottedFieldValue("LineItems[0].Requisition.Requester.Name.PrimaryString").toString();
													Log.customer.debug("else part and set requesterName ==> " +requesterName);
													if (! partition.getName().equalsIgnoreCase("mfg1")) {
													po.setFieldValue("RequesterName",requesterName.toString());
													}
												}
												requesterNameFmt = CATFaltFileUtil.getFormattedTxt(requesterName,20);
												Log.customer.debug("requesterNameFmt ==> " +requesterNameFmt);

												//LineItems. Accountings. SplitAccountings. AccountingFacility	20
												String accountingFacility21 ="";
												String accountingFacilityFmt21 ="";
												if (splitAcc.getFieldValue("AccountingFacility")!= null) {
													accountingFacility21 = splitAcc.getFieldValue("AccountingFacility").toString();
													Log.customer.debug("accountingFacility21 ==> " +accountingFacility21);
												}
												accountingFacilityFmt21 = CATFaltFileUtil.getFormattedTxt(accountingFacility21.toUpperCase(),20);
												Log.customer.debug("accountingFacilityFmt21 ==> " +accountingFacilityFmt21);

												//LineItems. Accountings. SplitAccountings. Department	2
												String departmentFmt ="";
												String department = "";
												if (splitAcc.getFieldValue("Department") != null) {
												department = splitAcc.getFieldValue("Department").toString();
												Log.customer.debug("department ==> " +department);
												departmentFmt = CATFaltFileUtil.getFormattedTxt(departmentFmt,5);
												}
												Log.customer.debug("departmentFmt ==> " +departmentFmt);

												//LineItems.Description.UnitOfMeasure.CAPSUnitOfMeasure	8
												String cAPSUnitOfMeasure1="";
												String cAPSUnitOfMeasureFmt1="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
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

												//LineItems. Description. UnitOfMeasure. UniqueName	3
												String unitOfMeasureUniqueName="";
												String unitOfMeasureUniqueNameFmt="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");

													if (description.getFieldValue("UnitOfMeasure") != null) {
														UnitOfMeasure uom = (UnitOfMeasure)description.getFieldValue("UnitOfMeasure");
														unitOfMeasureUniqueName = uom.getFieldValue("UniqueName").toString();
														Log.customer.debug("unitOfMeasureUniqueName ==> " +unitOfMeasureUniqueName);
													}

												}
												unitOfMeasureUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(unitOfMeasureUniqueName,3);
												Log.customer.debug("unitOfMeasureUniqueNameFmt ==> " +unitOfMeasureUniqueNameFmt);

												//LineItems. Quantity	9
												String quantity="";
												String quantityFmt="";
												if (poLineItem2.getFieldValue("Quantity")!= null)

												quantity = poLineItem2.getFieldValue("Quantity").toString();
												Log.customer.debug("quantity ==> " +quantity);
												quantityFmt = CATFaltFileUtil.getFormattedTxt(quantity,15);
												Log.customer.debug("quantityFmt ==> " +quantityFmt);


												//LineItems. Accountings. SplitAccountings. AccountingFacility	9
												String accountingFacility3 ="";
												String accountingFacilityFmt3 ="";
												if (splitAcc.getFieldValue("AccountingFacility")!= null) {
													accountingFacility3 = splitAcc.getFieldValue("AccountingFacility").toString();
													Log.customer.debug("accountingFacility3 ==> " +accountingFacility3);
												}
												accountingFacilityFmt3 = CATFaltFileUtil.getFormattedTxt(accountingFacility.toUpperCase(),9);
												Log.customer.debug("accountingFacilityFmt3 ==> " +accountingFacilityFmt3);


												//LineItems. Accountings. SplitAccountings. Department	2
												String department1 ="";
												String departmentFmt1 ="";
												if (splitAcc.getFieldValue("Department")!= null) {
													department1 = splitAcc.getFieldValue("Department").toString();
													Log.customer.debug("department1 ==> " +department1);
												}
												departmentFmt1 = CATFaltFileUtil.getFormattedTxt(department1.toUpperCase(),2);
												Log.customer.debug("departmentFmt1 ==> " +departmentFmt1);

												//LineItems. Accountings. SplitAccountings. Division/
												//for ezopen LineItems. Accountings. SplitAccountings.CompDivision
												///If null then LineItems. Accountings. SplitAccountings. Division	5

												String division = "";
												String divisionFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (splitAcc.getFieldValue("Division")!= null){
														division = splitAcc.getFieldValue("Division").toString();
														Log.customer.debug("divisionFmt for pcsv1 ==> " +divisionFmt);

													}
													divisionFmt = CATFaltFileUtil.getFormattedTxt(division,5);
													Log.customer.debug("division for pcsv1 ==> " +division);
												}
												else{
													if (splitAcc.getFieldValue("CompDivision")!= null){
														division = splitAcc.getFieldValue("CompDivision").toString();
														Log.customer.debug("CompDivision for non pcsv1 ==> " +division);

													}
													if (splitAcc.getFieldValue("Division")!= null){
														division = splitAcc.getFieldValue("Division").toString();
														Log.customer.debug("division for non pcsv1 ==> " +division);

													}

													divisionFmt = CATFaltFileUtil.getFormattedTxt(division,5);
													Log.customer.debug("divisionFmt for ALL ==> " +divisionFmt);

												}

												//LineItems. Accountings. SplitAccountings. Section
												//LineItems. Accountings. SplitAccountings.CompSection
												//LineItems. Accountings. SplitAccountings.Section	3


												String section = "";
												String sectionFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (splitAcc.getFieldValue("Section")!= null){
														section = splitAcc.getFieldValue("Section").toString();
														Log.customer.debug("section for pcsv1 ==> " +section);

													}
													sectionFmt = CATFaltFileUtil.getFormattedTxt(section,3);
													Log.customer.debug("sectionFmt for pcsv1 ==> " +sectionFmt);
												}
												else{
													if (splitAcc.getFieldValue("CompSection")!= null){
														section = splitAcc.getFieldValue("CompSection").toString();
														Log.customer.debug("CompSection for non pcsv1 ==> " +section);

													}
													if (splitAcc.getFieldValue("Section")!= null){
														section = splitAcc.getFieldValue("Section").toString();
														Log.customer.debug("Section for non pcsv1 ==> " +section);

													}

													sectionFmt = CATFaltFileUtil.getFormattedTxt(section,5);
													Log.customer.debug("sectionFmt for ALL ==> " +sectionFmt);

												}





												//LineItems. Accountings. SplitAccountings. ExpenseAccount
												//LineItems. Accountings. SplitAccountings.CompExpenseAccount
												//LineItems. Accountings. SplitAccountings.ExpenseAccount	2

												String expenseAccount = "";
												String expenseAccountFmt = "";
												if (partition.getName().equalsIgnoreCase("pcsv1")) {

													if (splitAcc.getFieldValue("ExpenseAccount")!= null){
														expenseAccount = splitAcc.getFieldValue("ExpenseAccount").toString();
														Log.customer.debug("expenseAccount for pcsv1 ==> " +expenseAccount);

													}
													expenseAccountFmt = CATFaltFileUtil.getFormattedTxt(expenseAccount,2);
													Log.customer.debug("expenseAccountFmt for pcsv1 ==> " +expenseAccountFmt);
												}
												else{
													if (splitAcc.getFieldValue("CompExpenseAccount")!= null){
														expenseAccount = splitAcc.getFieldValue("CompExpenseAccount").toString();
														Log.customer.debug("CompExpenseAccount for non pcsv1 ==> " +expenseAccount);

													}
													if (splitAcc.getFieldValue("ExpenseAccount")!= null){
														expenseAccount = splitAcc.getFieldValue("ExpenseAccount").toString();
														Log.customer.debug("ExpenseAccount for non pcsv1 ==> " +expenseAccount);

													}

													expenseAccountFmt = CATFaltFileUtil.getFormattedTxt(expenseAccount,2);
													Log.customer.debug("expenseAccountFmt for ALL ==> " +expenseAccountFmt);

												}





												//LineItems. Accountings. SplitAccountings. Order	4
												String poLnSplitAccountingsOrder = "  ";
												String opLnFmtSplitAccountingsOrder = "";
												if (splitAcc.getFieldValue("Order") != null){
													poLnSplitAccountingsOrder = (String)splitAcc.getFieldValue("Order");

												}
												opLnFmtSplitAccountingsOrder = CATFaltFileUtil.getFormattedTxt(poLnSplitAccountingsOrder,4);
												Log.customer.debug("opLnFmtSplitAccountingsOrder ==> " +opLnFmtSplitAccountingsOrder);

												//LineItems. Accountings. SplitAccountings. Misc	5
												String poLnSplitAccountingsMisc = "   ";
												String poLnFmtSplitAccountingsMisc = "";
												if (splitAcc.getFieldValue("Misc") != null){
													poLnSplitAccountingsMisc = (String)splitAcc.getFieldValue("Misc");

												}
												poLnFmtSplitAccountingsMisc = CATFaltFileUtil.getFormattedTxt(poLnSplitAccountingsMisc,3);

												Log.customer.debug("poLnFmtSplitAccountingsMisc ==> " +poLnFmtSplitAccountingsMisc);


												//LineItem.Description.Price.Amount  	1

												String priceAmountFmt ="";
												String priceAmount ="";

												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");

													if (description.getFieldValue("Price") != null) {
														Money price = (Money)description.getFieldValue("Price");
														double priceDbl = price.getAmountAsDouble();
														Log.customer.debug("priceDbl ==> " +priceDbl);
														priceAmountFmt = CATFaltFileUtil.getFormattedNumber(priceDbl, "00000000.0000");
														//Log.customer.debug("priceAmountFmt ==> " +priceAmountFmt);
													}

												}
												Log.customer.debug("priceAmountFmt ==> " +priceAmountFmt);

												//LineItems. Description.UnitOfMeasure.CAPSUnitOfMeasure	15

												String cAPSUnitOfMeasure2="";
												String cAPSUnitOfMeasureFmt2="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
													Log.customer.debug("description ==> " +description);

													if (description.getFieldValue("UnitOfMeasure") != null) {
														UnitOfMeasure uom1 = (UnitOfMeasure)description.getFieldValue("UnitOfMeasure");
														Log.customer.debug("uom1 ==> " +uom1);
														cAPSUnitOfMeasure2 = uom1.getFieldValue("CAPSUnitOfMeasure").toString();
														Log.customer.debug("cAPSUnitOfMeasure2 ==> " +cAPSUnitOfMeasure2);
													}

												}
												cAPSUnitOfMeasureFmt2 = CATFaltFileUtil.getFormattedTxt(cAPSUnitOfMeasure2,15);
												Log.customer.debug("cAPSUnitOfMeasureFmt2 ==> " +cAPSUnitOfMeasureFmt2);

												//LineItems. Description. UnitOfMeasure. UniqueName	3

												String unitOfMeasure="";
												String unitOfMeasureFmt="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
													Log.customer.debug("description ==> " +description);

													if (description.getFieldValue("UnitOfMeasure") != null) {
														UnitOfMeasure uom1 = (UnitOfMeasure)description.getFieldValue("UnitOfMeasure");
														Log.customer.debug("uom1 ==> " +uom1);
														unitOfMeasure = uom1.getFieldValue("UniqueName").toString();
														Log.customer.debug("unitOfMeasure ==> " +unitOfMeasure);
													}

												}
												unitOfMeasureFmt = CATFaltFileUtil.getFormattedTxt(unitOfMeasureFmt,3);
												Log.customer.debug("unitOfMeasureFmt ==> " +unitOfMeasureFmt);


												//LineItems. Description.ReasonCode	10
												String reasonCode="";
												String reasonCodeFmt="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
													Log.customer.debug("description ==> " +description);

													if (description.getFieldValue("ReasonCode") != null) {
														reasonCode  = description.getFieldValue("ReasonCode").toString();
														Log.customer.debug("reasonCode ==> " +reasonCode);
													}

												}
												reasonCodeFmt = CATFaltFileUtil.getFormattedTxt(reasonCode,10);
												Log.customer.debug("reasonCodeFmt ==> " +reasonCodeFmt);


												//TotalCost. Currency. UniqueName   11
												String currencyUniqueName="";
												String currencyUniqueNameFmt="";
												if (po.getFieldValue("TotalCost") != null){
													Money totalCost = (Money)po.getFieldValue("TotalCost");
													currencyUniqueName = totalCost.getDottedFieldValue("Currency.UniqueName").toString();
													Log.customer.debug("currencyUniqueName ==> " +currencyUniqueName);

												}
												currencyUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(currencyUniqueName,3);
												Log.customer.debug("currencyUniqueNameFmt ==> " +currencyUniqueNameFmt);

												//LineItems. ShipTo. PostalAddress. Country. UniqueName  3
												String countryUniqueName1 = "";
												String countryUniqueNameFmt1 = "";
												if (poLineItem2.getDottedFieldValue("ShipTo.PostalAddress")!= null) {
													PostalAddress postalAddress = (PostalAddress) poLineItem2.getDottedFieldValue("ShipTo.PostalAddress");
													countryUniqueName1 = postalAddress.getDottedFieldValue("Country.UniqueName").toString();
													Log.customer.debug("countryUniqueName1 ==> " +countryUniqueName);
												}
												countryUniqueNameFmt1 = CATFaltFileUtil.getFormattedTxt(countryUniqueName1,3);
												Log.customer.debug("countryUniqueNameFmt1 ==> " +countryUniqueNameFmt1);

												//LineItems. Accountings. SplitAccountings. AccountingFacility 10

												String accountingFacility5 ="";
												String accountingFacilityFmt5 ="";
												if (splitAcc.getFieldValue("AccountingFacility")!= null) {
													accountingFacility5 = splitAcc.getFieldValue("AccountingFacility").toString();
													Log.customer.debug("accountingFacility4 ==> " +accountingFacility5);
												}
												accountingFacilityFmt5 = CATFaltFileUtil.getFormattedTxt(accountingFacility5,10);

												Log.customer.debug("accountingFacilityFmt ==> " +accountingFacilityFmt5);

												//LineItems. Description. CommonCommodityCode.UniqueName 2
												String commonCommodityCode="";
												String commonCommodityCodeFmt="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
													Log.customer.debug("description ==> " +description);

													if (description.getFieldValue("CommonCommodityCode") != null) {
														commonCommodityCode = description.getDottedFieldValue("CommonCommodityCode.UniqueName").toString();
														Log.customer.debug("commonCommodityCode ==> " +commonCommodityCode);
													}

												}
												commonCommodityCodeFmt = CATFaltFileUtil.getFormattedTxt(commonCommodityCode,2);
												Log.customer.debug("commonCommodityCodeFmt ==> " +commonCommodityCodeFmt);


												//SupplierLocation. EmailAddress 4
												String supplierLocationEmailAddress="";
												String supplierLocationEmailAddressFmt="";
												if (poLineItem2.getFieldValue("SupplierLocation")!= null)
													supplierLocationEmailAddress = poLineItem2.getDottedFieldValue("SupplierLocation.EmailAddress").toString();
												Log.customer.debug("supplierLocationEmailAddress ==> " +supplierLocationEmailAddress);
												supplierLocationEmailAddressFmt = CATFaltFileUtil.getFormattedTxt(supplierLocationEmailAddress,25);
												Log.customer.debug("supplierLocationEmailAddressFmt ==> " +supplierLocationEmailAddressFmt);

												//LineItems. Description. Description 1
												String descriptionTxt="";
												String descriptionTxtFmt="";
												if (poLineItem2.getFieldValue("Description") != null){
													LineItemProductDescription description2 = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
													Log.customer.debug("description2 ==> " +description2);

													if (description2.getFieldValue("Description") != null) {
														descriptionTxt = description2.getFieldValue("Description").toString();
														Log.customer.debug("CarriageReturns before descriptionTxt ==> " +descriptionTxt);
														//StringUtil.removeCarriageReturns(descriptionTxt);
														//descriptionTxt.replaceAll("[\n\r]", "");
														Log.customer.debug("after CarriageReturns descriptionTxt ==> " +descriptionTxt+"Test456");
														Log.customer.debug("descriptionTxt ==> " +descriptionTxt);
													}

												}
												descriptionTxtFmt = CATFaltFileUtil.getFormattedTxt(descriptionTxt,25);
												Log.customer.debug("descriptionTxtFmt ==> " +descriptionTxtFmt);






												Log.customer.debug("POdata writing to file  ==> ");
												String cliDBPOData = poFmtUniqueName+"~|"+numberInCollectionFmt+"~|"+buyerCodeFmt+"~|"+poFmtCreateDate+"~|"+poFmtCreateDate1+"~|"+supplierLocationUniqueNameFmt+"~|"+supplierLocationNameFmt+"~|"+buyerCodeFmt2+"~|"+accountingFacilityFmt+"~|"+accountingFacilityFmt2+"~|"+receivingFacilityFmt+"~|"+dockCodeFmt+"~|"+fOBPointUniqueNameFmt+"~|"+cityStateCodeFmt+"~|"+postalCodeFmt+"~|"+countryUniqueNameFmt+"~|"+requesterNameFmt+"~|"+accountingFacilityFmt21+"~|"+departmentFmt+"~|"+cAPSUnitOfMeasureFmt1+"~|"+unitOfMeasureUniqueNameFmt+"~|"+quantityFmt+"~|"+accountingFacilityFmt3+"~|"+departmentFmt1+"~|"+divisionFmt+"~|"+sectionFmt+"~|"+expenseAccountFmt+"~|"+opLnFmtSplitAccountingsOrder+"~|"+poLnFmtSplitAccountingsMisc+"~|"+priceAmountFmt+"~|"+cAPSUnitOfMeasureFmt2+"~|"+unitOfMeasureFmt+"~|"+reasonCodeFmt+"~|"+currencyUniqueNameFmt+"~|"+countryUniqueNameFmt1+"~|"+accountingFacilityFmt5+"~|"+commonCommodityCodeFmt+"~|"+supplierLocationEmailAddressFmt+"~|"+descriptionTxtFmt;
												Log.customer.debug("PO data writing to file  ==> " +cliDBPOData);
												outPW_poLineFlatFile.write(cliDBPOData);
												Log.customer.debug("New Line writing to file  ==> ");
												outPW_poLineFlatFile.write("\n");
											} //for (int j = 0; j<poSplitaccSize;j++){

										} //if (splitAcc != null)

									}   //<<<<POLineItemFile end >>>>for(Iterator s= poAccounting.getSplitAccountingsIterator(); s.hasNext();) {

									}
									// <<<<<<<<<<<<< poSplitaccSize  > 1 AccDistribution File Start >>>>>>>>>>>>>>>>>>>>>>>>
									else {
										for(Iterator s= poAccounting.getSplitAccountingsIterator(); s.hasNext();) {
											SplitAccounting splitAcc = (SplitAccounting) s.next();
											if (splitAcc != null) {
												for (int j = 0; j<poSplitaccSize;j++){
													Log.customer.debug ("%s::Inside for (int j = 0; j<poSplitaccSize;j++){",classname);
													if ( po.getFieldValue("RequesterName") == null) {
														requesterName = po.getDottedFieldValue("LineItems[0].Requisition.Requester.Name.PrimaryString");
														po.setFieldValue("RequesterName",requesterName.toString());
													}


													//pcsv1 (U.S.) U9 /ezopen LineItems. Accountings. SplitAccountings. AccountingFacility 2
													String accountingFacilityAccDis ="";
													String accountingFacilityAccDisFmt ="";
													if (partition.getName().equalsIgnoreCase("pcsv1")) {
														accountingFacilityAccDis = "U9";
													}
													else
													{
														if (splitAcc.getFieldValue("AccountingFacility")!= null) {
															accountingFacilityAccDis = splitAcc.getFieldValue("AccountingFacility").toString();
															Log.customer.debug("accountingFacility AccDis ==> " +accountingFacilityAccDis);
														}
														accountingFacilityAccDisFmt = CATFaltFileUtil.getFormattedTxt(accountingFacilityAccDisFmt,10);

														Log.customer.debug("accountingFacilityAccDisFmt ==> " +accountingFacilityAccDisFmt);
													}




													//UniqueName	12
													String poUniqueName = po.getFieldValue("UniqueName").toString();
													Log.customer.debug("poUniqueName ==> " +poUniqueName);
													poFmtUniqueName = CATFaltFileUtil.getFormattedTxt(poUniqueName,12);
													Log.customer.debug("poFmtUniqueName ==> " +poFmtUniqueName);

													//LineItems. NumberInCollection	3
													String numberInCollectionAD="";
													String numberInCollectionADFmt="";
													if (poLineItem2.getFieldValue("NumberInCollection")!= null)
														numberInCollectionAD = poLineItem2.getFieldValue("NumberInCollection").toString();
													Log.customer.debug("numberInCollectionAD ==> " +numberInCollectionAD);
													numberInCollectionADFmt = CATFaltFileUtil.getFormattedTxt(numberInCollectionAD,3);
													Log.customer.debug("numberInCollectionFmt ==> " +numberInCollectionADFmt);

													//LineItems. Accountings. SplitAccountings. AccountingFacility	2
													String accountingFacilityAD ="";
													String accountingFacilityFmtAD ="";
													if (splitAcc.getFieldValue("AccountingFacility")!= null) {
														accountingFacilityAD = splitAcc.getFieldValue("AccountingFacility").toString();
														Log.customer.debug("accountingFacilityAD ==> " +accountingFacilityAD);
													}
													accountingFacilityFmtAD = CATFaltFileUtil.getFormattedTxt(accountingFacilityAD,2);

													Log.customer.debug("accountingFacilityFmtAD ==> " +accountingFacilityFmtAD);

													//LineItems. Accountings. SplitAccountings. Department	5
													String departmentAD ="";
													String departmentFmtAD ="";
													if (splitAcc.getFieldValue("Department")!= null) {
														departmentAD = splitAcc.getFieldValue("Department").toString();
														Log.customer.debug("department1 ==> " +departmentAD);
													}
													departmentFmtAD = CATFaltFileUtil.getFormattedTxt(departmentAD.toUpperCase(),2);
													Log.customer.debug("departmentFmtAD ==> " +departmentFmtAD);

													//LineItems. Accountings. SplitAccountings. Division
													//ezopen then LineItems. Accountings. SplitAccountings.CompDivision/LineItems. Accountings. SplitAccountings.Division 3

													String divisionAD = "";
													String divisionFmtAD = "";
													if (partition.getName().equalsIgnoreCase("pcsv1")) {

														if (splitAcc.getFieldValue("Division")!= null){
															divisionAD = splitAcc.getFieldValue("Division").toString();
															Log.customer.debug("divisionFmt for pcsv1 ==> " +divisionFmtAD);

														}
														divisionFmtAD = CATFaltFileUtil.getFormattedTxt(divisionAD,3);
														Log.customer.debug("division for pcsv1 ==> " +divisionAD);
													}
													else{
														if (splitAcc.getFieldValue("CompDivision")!= null){
															divisionAD = splitAcc.getFieldValue("CompDivision").toString();
															Log.customer.debug("CompDivision AD for non pcsv1 ==> " +divisionAD);

														}
														if (splitAcc.getFieldValue("Division")!= null){
															divisionAD = splitAcc.getFieldValue("Division").toString();
															Log.customer.debug("divisionAD for non pcsv1 ==> " +divisionAD);

														}

														divisionFmtAD = CATFaltFileUtil.getFormattedTxt(divisionAD,5);
														Log.customer.debug("divisionFmtAD for ALL ==> " +divisionFmtAD);

													}
													//LineItems. Accountings. SplitAccountings. Section
													//LineItems. Accountings. SplitAccountings.CompSection/LineItems. Accountings. SplitAccountings.Section 2

													String sectionAD = "";
													String sectionFmtAD = "";
													if (partition.getName().equalsIgnoreCase("pcsv1")) {

														if (splitAcc.getFieldValue("Section")!= null){
															sectionAD = splitAcc.getFieldValue("Section").toString();
															Log.customer.debug("sectionAD for pcsv1 ==> " +sectionAD);

														}
														sectionFmtAD = CATFaltFileUtil.getFormattedTxt(sectionAD,2);
														Log.customer.debug("sectionFmt for pcsv1 ==> " +sectionFmtAD);
													}
													else{
														if (splitAcc.getFieldValue("CompSection")!= null){
															sectionAD = splitAcc.getFieldValue("CompSection").toString();
															Log.customer.debug("CompSection for non pcsv1 ==> " +sectionAD);

														}
														if (splitAcc.getFieldValue("Section")!= null){
															sectionAD = splitAcc.getFieldValue("Section").toString();
															Log.customer.debug("Section for non pcsv1 ==> " +sectionAD);

														}

														sectionFmtAD = CATFaltFileUtil.getFormattedTxt(sectionAD,5);
														Log.customer.debug("sectionFmt for ALL ==> " +sectionFmtAD);

													}
													//LineItems. Accountings. SplitAccountings. ExpenseAccount
													//LineItems. Accountings. SplitAccountings.CompExpenseAccount/LineItems. Accountings. SplitAccountings.ExpenseAccount 4
													String expenseAccountAD = "";
													String expenseAccountFmtAD = "";
													if (partition.getName().equalsIgnoreCase("pcsv1")) {

														if (splitAcc.getFieldValue("ExpenseAccount")!= null){
															expenseAccountAD = splitAcc.getFieldValue("ExpenseAccount").toString();
															Log.customer.debug("expenseAccount for pcsv1 ==> " +expenseAccountAD);

														}
														expenseAccountFmtAD = CATFaltFileUtil.getFormattedTxt(expenseAccountAD,2);
														Log.customer.debug("expenseAccountFmtAD for pcsv1 ==> " +expenseAccountFmtAD);
													}
													else{
														if (splitAcc.getFieldValue("CompExpenseAccount")!= null){
															expenseAccountAD = splitAcc.getFieldValue("CompExpenseAccount").toString();
															Log.customer.debug("CompExpenseAccount for non pcsv1 ==> " +expenseAccountAD);

														}
														if (splitAcc.getFieldValue("ExpenseAccount")!= null){
															expenseAccountAD = splitAcc.getFieldValue("ExpenseAccount").toString();
															Log.customer.debug("ExpenseAccount for non pcsv1 ==> " +expenseAccountAD);

														}

														expenseAccountFmtAD = CATFaltFileUtil.getFormattedTxt(expenseAccountAD,4);
														Log.customer.debug("expenseAccountFmtAD for ALL ==> " +expenseAccountFmtAD);

													}
													//LineItems. Accountings. SplitAccountings. Order	5
													String poLnSplitAccountingsOrderAD = "  ";
													String opLnFmtSplitAccountingsOrderAD = "";
													if (splitAcc.getFieldValue("Order") != null){
														poLnSplitAccountingsOrderAD = (String)splitAcc.getFieldValue("Order");

													}
													opLnFmtSplitAccountingsOrderAD = CATFaltFileUtil.getFormattedTxt(poLnSplitAccountingsOrderAD,5);
													Log.customer.debug("opLnFmtSplitAccountingsOrderAD ==> " +opLnFmtSplitAccountingsOrderAD);

													//LineItems. Accountings. SplitAccountings. Misc	3
													String poLnSplitAccountingsMiscAD = "   ";
													String poLnFmtSplitAccountingsMiscAD = "";
													if (splitAcc.getFieldValue("Misc") != null){
														poLnSplitAccountingsMiscAD = (String)splitAcc.getFieldValue("Misc");

													}
													poLnFmtSplitAccountingsMiscAD = CATFaltFileUtil.getFormattedTxt(poLnSplitAccountingsMiscAD,3);

													Log.customer.debug("poLnFmtSplitAccountingsMiscAD ==> " +poLnFmtSplitAccountingsMiscAD);

													//LineItems. Accountings. SplitAccountings. Percentage	3
													BigDecimal  poLnSplitAccountingsPercentageAD = null;
													String poLnFmtSplitAccountingsPercentageAD = "";
													if (splitAcc.getFieldValue("Percentage") != null){
														poLnSplitAccountingsPercentageAD = (BigDecimal)splitAcc.getFieldValue("Percentage");
														double poLnSplitAccountingsPercentageADDble = poLnSplitAccountingsPercentageAD.doubleValue();
														poLnFmtSplitAccountingsPercentageAD = CATFaltFileUtil.getEzFormattedNumber(poLnSplitAccountingsPercentageADDble, "000.000");

													}


													Log.customer.debug("poLnFmtSplitAccountingsPercentageAD ==> " +poLnFmtSplitAccountingsPercentageAD);

													//LineItems. ShipTo. ReceivingFacility/LineItems.Accounting.SplitAccounting.AccountingFacility 2
													String receivingFacility = "";
													String receivingFacilityFmt = "";
													if (partition.getName().equalsIgnoreCase("pcsv1")) {

														if (poLineItem2.getFieldValue("ShipTo")!= null){
															Address shipTo = (Address)poLineItem2.getFieldValue("ShipTo");
															if ( shipTo.getFieldValue("ReceivingFacility")!= null){
															   receivingFacility = poLineItem2.getDottedFieldValue("ShipTo.ReceivingFacility").toString();
															}
															else
																receivingFacility = " ";

														}
														receivingFacilityFmt = CATFaltFileUtil.getFormattedTxt(receivingFacility,2);
														Log.customer.debug("AD receivingFacilityFmt for pcsv1 ==> " +receivingFacilityFmt);
													}
													else{
														receivingFacilityFmt = accountingFacilityFmtAD;
														Log.customer.debug(" AD receivingFacilityFmt/accountingFacilityFmt for Non pcsv1 ==> " +receivingFacilityFmt);
													}


													//LineItems. Description.Description	16
													String descriptionTxt="";
													String descriptionTxtFmt="";
													if (poLineItem2.getFieldValue("Description") != null){
														LineItemProductDescription description2 = (LineItemProductDescription)poLineItem2.getFieldValue("Description");
														Log.customer.debug("AD description2 ==> " +description2);

														if (description2.getFieldValue("Description") != null) {
															descriptionTxt = description2.getFieldValue("Description").toString();
															Log.customer.debug("AD CarriageReturns before descriptionTxt ==> " +descriptionTxt);
															//StringUtil.removeCarriageReturns(descriptionTxt);
															descriptionTxt.replaceAll("  ", " ");
															Log.customer.debug("AD after CarriageReturns descriptionTxt ==>"+descriptionTxt+"Test123");
														}

													}
													descriptionTxtFmt = CATFaltFileUtil.getFormattedTxt(descriptionTxt,16);
													Log.customer.debug("AD descriptionTxtFmt ==> " +descriptionTxtFmt);
													//CreateDate	10
													String poFmtCreateDate ="";
													Date poCreateDate = (Date)po.getFieldValue("TimeCreated");
													Log.customer.debug("AD poCreateDate ==> " +poCreateDate);
													poFmtCreateDate = CATFaltFileUtil.getFormattedDate(poCreateDate);
													Log.customer.debug("AD poFmtCreateDate ==> " +poFmtCreateDate);

													//LineItems. BuyerCode. BuyerCode	2
													String buyerCode="";
													String buyerCodeFmt="";
													if (poLineItem2.getFieldValue("BuyerCode")!= null)
														buyerCode = poLineItem2.getDottedFieldValue("BuyerCode.BuyerCode").toString();
													Log.customer.debug("AD buyerCode ==> " +buyerCode);
													buyerCodeFmt = CATFaltFileUtil.getFormattedTxt(buyerCode,2);
													Log.customer.debug("AD buyerCodeFmt ==> " +buyerCodeFmt);

													//SupplierLocation.UniqueName	7
													String supplierLocationUniqueName="";
													String supplierLocationUniqueNameFmt="";
													if (poLineItem2.getFieldValue("SupplierLocation")!= null)
														supplierLocationUniqueName = poLineItem2.getDottedFieldValue("SupplierLocation.UniqueName").toString();
													Log.customer.debug("AD supplierLocationUniqueName ==> " +supplierLocationUniqueName);
													supplierLocationUniqueNameFmt = CATFaltFileUtil.getFormattedTxt(supplierLocationUniqueName,7);
													Log.customer.debug("AD supplierLocationUniqueNameFmt ==> " +supplierLocationUniqueNameFmt);



													Log.customer.debug("accDisPOData writing to file  ==> ");
													String accDisPOData = accountingFacilityAccDis+"~|"+poFmtUniqueName+"~|"+numberInCollectionADFmt+"~|"+accountingFacilityFmtAD+"~|"+departmentFmtAD+"~|"+divisionFmtAD+"~|"+sectionFmtAD+"~|"+expenseAccountFmtAD+"~|"+opLnFmtSplitAccountingsOrderAD+"~|"+poLnFmtSplitAccountingsMiscAD+"~|"+poLnFmtSplitAccountingsPercentageAD+"~|"+receivingFacilityFmt+"~|"+descriptionTxtFmt+"~|"+poFmtCreateDate+"~|"+buyerCodeFmt+"~|"+supplierLocationUniqueNameFmt;
													Log.customer.debug("PO data writing to file  ==> " +accDisPOData);
													outPW_poAccDisFlatFile.write(accDisPOData);
													Log.customer.debug("New Line writing to file  ==> ");
													outPW_poAccDisFlatFile.write("\n");


									}
											}
										}
									}  //   << AccDistribution File end>>


								} //if (poSplitaccSize > 0){

							} //if (poAccounting!=null){

						} //for (int i =0; i<lineCount;i++){




					} //if (lineCount > 0){
				} //if(po != null)
				count++;
		    	if(count == 100)
			   {
					Log.customer.debug("**********Commiting PO Records*******  ",count);
					Base.getSession().transactionCommit();
					count = 0;
				}
				continue;

			} // while












		}
		catch (Exception e) {

			Log.customer.debug(e);
		}
		finally {
			if (outPW_poAccDisFlatFile != null)  {
				outPW_poAccDisFlatFile.flush();
				outPW_poAccDisFlatFile.close();	}


			if (outPW_poLineFlatFile != null)  {
				outPW_poLineFlatFile.flush();
				outPW_poLineFlatFile.close();

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
		}






	}


			}

