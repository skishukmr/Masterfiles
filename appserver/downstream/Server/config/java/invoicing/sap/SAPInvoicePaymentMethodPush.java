// Source File Name:   SAPInvoicePaymentMethodPush.java

//Created  by Nagendra for Pushing IR details data to staging tables

// Sudheer K Jain Issue 958 Removing special character ' single quotes for Pushing IR details data to staging tables
//Soumya R added code for WH Tax Code logic
// 28-11-2011 Soumya made code fix for null pointer exception in checkforWHTax method.
// Soumya R modified the insert query to include the new fields added as part of Vertex
// 09-Mar-2012: Soumya Mohanty, replaced the null initialization of Incoterm will empty String Value, to prevent pushing "null" string value to the Staging table.
// 04-Apr-2012 Vikram J Singh, CR216 Modify PDW to provide all POs irrespective of whether invoiced or not
// 20-Apr-2012: Abhishek Kumar : Removed the hardcoding of SAP partition to incorporate the logic for LSAP partition for Bycyrus Project.
// 07-Sep-2012: Vikram_AMS Issue 326/MGPP 2027: Add UOM and UOM Description in MACH1 invoice flat file push for MACH1 company codes as a part of changes for MACH1 5.0 release
// 22-Jan-2013: IBM Niraj Kumar	   : Mach1 R5.5 (FRD10.3/TD10.3)  Added VATRegistration field in Payment push
// 22-Jan-2013: IBM Niraj Kumar	   : Mach1 R5.5 (FRD10.4/TD10.4)  Added ASNINVOICE field in Payment push
// 22-Feb-2013: IBM Niraj Kumar	   : Mach1 R5.5 (FRD11.2/TD11.2)  Changed logic WH Tax Code to add company code and country
// 21-May-2013: IBM Abhishek Kumar : Mach1 R5.5 (FRD 7.1/TD 7.1)  Added logic for rounding of HUF currency.

package config.java.invoicing.sap;
import java.math.BigDecimal;
import java.util.List;
import ariba.util.core.Date;
import ariba.util.core.StringUtil;
import ariba.common.core.SplitAccounting;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.payment.AribaInvoicePaymentMethod;
import ariba.procure.core.ProcureLineType;
import ariba.util.log.Log;
import ariba.base.core.*;
import ariba.base.core.aql.*;
import java.sql.*;
import ariba.util.formatter.DateFormatter;
import java.util.ArrayList;
import ariba.basic.core.Currency;
import java.util.Arrays;
import ariba.base.core.Partition;
import ariba.base.core.Base;

public class SAPInvoicePaymentMethodPush extends AribaInvoicePaymentMethod {

	protected List createPayments(InvoiceReconciliation ir) {

		Log.customer.debug("SAPInvoicePaymentMethodPush.createPayments called");

		// Method to push the data into Payment staging table "IR_HEADER_DETAIL"

		// and "IR_LINEITEM_ACCNTG"
		// WHTax Code Changes START
		if( ir.getDottedFieldValue("CompanyCode.SAPSource").equals("MACH1"))
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush.: SAPSource :" +  ir.getDottedFieldValue("CompanyCode.SAPSource"));
			Log.customer.debug("SAPInvoicePaymentMethodPush.: Started Calculating the WHTax Data Calculation");
			checkforWHTax(ir);
			Log.customer.debug("SAPInvoicePaymentMethodPush.: updated the GL account and IR comparision flag for WHTax");
			//Start: Mach1 R5.5 (FRD7.1/TD7.1)
			if ( ir.getDottedFieldValue("ReportingCurrency.AllowedDecimal").equals("0"))
			{
				Log.customer.debug("SAPInvoicePaymentMethodPush.: ReportingCurrency AllowedDecimal is 0 - HUF");
				Gethighestlineno(ir);
				Log.customer.debug("SAPInvoicePaymentMethodPush.: ReportingCurrency AllowedDecimal is 0 Calls the Gethighestlineno method");
				getamountdiffvalue(ir);
				Log.customer.debug("SAPInvoicePaymentMethodPush.: ReportingCurrency AllowedDecimal is 0 Calls the getamountdiffvalue method");
			}
			//End: Mach1 R5.5 (FRD7.1/TD7.1)
		}
		Log.customer.debug("SAPInvoicePaymentMethodPush.: Completed Calculating the WHTax Data Calculation");
		// WHTax Code ends
		Log.customer.debug("SAPInvoicePaymentMethodPush.: Started Pushing Data into staging table");
		pushToStagingTable(ir);
		Log.customer.debug("SAPInvoicePaymentMethodPush.: Completed Pushing Data into staging table");
		//ir.save();
		Log.customer.debug("SAPInvoicePaymentMethodPush.: Started Creating Payment Object");
		return super.createPayments(ir);

	}

	//private static Partition partition;

	private static String query, query1, query2;

	private static ClusterRoot cObj;

	private static AQLQuery qry;

	private static AQLOptions options, options1, options2;

	private static AQLResultCollection results, results1, results2;

	private static ariba.invoicing.core.InvoiceReconciliationLineItem invreconli;

	private static BaseVector invreconlicol;

	private static ariba.invoicing.core.InvoiceReconciliation invrecon;

	private static ariba.receiving.core.Receipt rcpt;

	private static ariba.purchasing.core.DirectOrder order;

	private static ariba.approvable.core.LineItem li;

	private static ariba.receiving.core.ReceiptItem ri;

	private static ariba.approvable.core.LineItemCollection lic;

	private static ariba.common.core.SplitAccountingCollection sacol;

	private static ariba.common.core.SplitAccounting sa;

	private static ariba.basic.core.Currency repcur;

	private static ariba.basic.core.Money totcost;

	private static ClusterRoot fac;

	private static BaseVector bvec = null;

	private static java.math.BigDecimal totamt;

	private static java.math.BigDecimal irtax;

	private static ariba.util.core.Date curdate;

	private static ariba.util.core.Date startdate;

	private static String strStart = null;

	private static String strEnd = null;

	public final String CBSSource = "CBS";
	public final String zAccCat = "Z";
	//Start: Mach1 R5.5 (FRD7.1/TD7.1)
	public static Integer highestLILN;
	public static Integer highestSALN;
	BigDecimal diff = new BigDecimal(0.00);
	//End: Mach1 R5.5 (FRD7.1/TD7.1)

	public static void setDWFlag(String iruniquename)

	{

		/****** Abhishek : Bycyrus Changes: Commented out the hardcoding of SAP partition starts ******/
				//partition = (Partition) Base.getService().getPartition("SAP");
				Partition partition = Base.getSession().getPartition();
		/****** Abhishek : commented out the hardcoding of SAP partition ends ******/

		Log.customer.debug("SAPInvoicePaymentMethodPush.partition called"
				+ partition);

		try

		{

			query = new String(
					"select from ariba.invoicing.core.InvoiceReconciliation where  DWInvoiceFlag IS NULL and UniqueName like '"
							+ iruniquename + "'");

			// ariba.base.core.Log.customer.debug("%s %s", THISCLASS, query);

			qry = AQLQuery.parseQuery(query);

			options = new AQLOptions(partition);

			results = Base.getService().executeQuery(qry, options);

			if (results.getErrors() != null)

			{

				// ariba.base.core.Log.customer.debug("%s ERROR GETTING RESULTS
				// in Results1", THISCLASS);

			}

			while (results.next())

			{

				String unique = null;

				ClusterRoot cr = null;

				String afac = "";

				String orderId = "";

				// System.out.println("****inside while");

				// invrecon =
				// (ariba.invoicing.core.InvoiceReconciliation)(results.getBaseId("InvoiceReconciliation").get());

				// invrecon =
				// (ariba.invoicing.core.InvoiceReconciliation)(results.getObject(0));

				invrecon = (ariba.invoicing.core.InvoiceReconciliation) (results
						.getBaseId("InvoiceReconciliation").get());

				Log.customer
						.debug("SAPInvoicePaymentMethodPush.invrecon called"
								+ invrecon);

				if (invrecon == null)
					continue;

				// ariba.base.core.Log.customer.debug("%s 2....%s", THISCLASS,
				// invrecon.toString());

				if (invrecon.getFieldValue("InvoiceDate") == null)

					invrecon
							.setFieldValue(
									"InvoiceDate",
									(ariba.util.core.Date) invrecon
											.getDottedFieldValue("Invoice.InvoiceDate"));

				// For MA Invoice get the BuyerCode from MA Preparer

				/*
				 * if (invrecon.getFieldValue("MasterAgreement") != null)
				 *  {
				 *
				 * BaseId maprepobj =
				 * ((ariba.user.core.User)invrecon.getDottedFieldValue("MasterAgreement.Preparer")).getBaseId();
				 *
				 * String maprepstr =
				 * (String)invrecon.getDottedFieldValue("MasterAgreement.Preparer.UniqueName");
				 *
				 * //ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "MA Preparer: " + maprepstr );
				 *
				 * //maprepstr = maprepstr.replaceAll(" ", "%%");
				 *
				 * //ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "After Replacing SPACES BuyerName IS: " + maprepstr );
				 *
				 *
				 *
				 * String buyerstr = "select from cat.core.BuyerCode where
				 * UserID = BaseId('" + maprepobj.toDBString() + "')";
				 *
				 * ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "QUERY IS: " + buyerstr );
				 *
				 * ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "BASEID IS: " + maprepobj.toDBString() );
				 *
				 * AQLQuery buyerquery =AQLQuery.parseQuery(buyerstr);
				 *
				 * AQLOptions buyeroptions = new
				 * AQLOptions(invrecon.getPartition());
				 *
				 * AQLResultCollection buyerresults =
				 * Base.getService().executeQuery(buyerquery, buyeroptions);
				 *
				 *
				 *
				 * if (buyerresults.getErrors() != null)
				 *  {
				 *
				 * //ariba.base.core.Log.customer.debug("%s ERROR GETTING
				 * RESULTS in Results1 %s", THISCLASS, buyerresults.getErrors() );
				 *
				 * //ariba.base.core.Log.customer.debug("%s ERROR Statement Text
				 * %s", THISCLASS, buyerresults.getErrorStatementText() );
				 *  }
				 *
				 *
				 *
				 * if (buyerresults.next())
				 *  {
				 *
				 * //ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "The BuyerCode : " + buyerresults.getObject(0) );
				 *
				 * cr = (ClusterRoot) buyerresults.getBaseId("BuyerCode").get();
				 *
				 * if (cr != null)
				 *
				 * //ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "Buyer for MA IS " + cr);
				 *
				 * else
				 *
				 * //ariba.base.core.Log.customer.debug("%s ....%s", THISCLASS,
				 * "Buyer for MA IS NULL");
				 *  }
				 *  }
				 */

				ariba.invoicing.core.InvoiceReconciliationLineItem doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem) invrecon
						.getLineItems().get(0);

				if (doinvreconli != null)

				{

					Log.customer
							.debug("SAPInvoicePaymentMethodPush.doinvreconli called"
									+ doinvreconli);

					// Setting FacilityFlag, PONumber, POCreateDate,
					// POLineItemNumber, BuyerCode

					// System.out.println("**** inside doinvreconli");

					if (doinvreconli.getFieldValue("Order") != null)

					{

						Log.customer.debug("**** inside doinvreconli Order");

						order = (ariba.purchasing.core.DirectOrder) doinvreconli
								.getFieldValue("Order");

						// POCreateDate, PONumber is set as Order.UniqueName

						orderId = (String) order.getFieldValue("UniqueName");

						invrecon.setFieldValue("PONumber", orderId);

						ariba.util.core.Date POCreateDate = (ariba.util.core.Date) order
								.getFieldValue("TimeCreated");

						invrecon.setFieldValue("POCreateDate", POCreateDate);

						// ariba.base.core.Log.customer.debug("%s
						// Populating......POCreateDate", THISCLASS);

						// ariba.base.core.Log.customer.debug("%s
						// Populating......ExchangeRate", THISCLASS);

					}
					//Santanu : Uncommented that part, Issue #941
					 else if(invrecon.getDottedFieldValue("MasterAgreement")!=null){
							 invrecon.setFieldValue("PONumber",(java.lang.String)invrecon.getDottedFieldValue("MasterAgreement.UniqueName"));
							 invrecon.setFieldValue("POCreateDate",(ariba.util.core.Date)invrecon.getDottedFieldValue("Invoice.InvoiceDate"));
						 }
					//Santanu : Uncommented that part, Issue #941
				}

				for (int i = 0; i < invrecon.getLineItemsCount(); i++)

				{

					Log.customer.debug("**** line item iteration");

					doinvreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem) invrecon
							.getLineItems().get(i);

					if (doinvreconli == null)
						continue;

					if (i == 0)

					{

						String strfac = (java.lang.String) doinvreconli
								.getDottedFieldValue("ShipTo.ReceivingFacility");

						if (strfac != null)

						{

							invrecon.setFieldValue("FacilityFlag", strfac);

						}

					}

					if (doinvreconli.getFieldValue("Order") != null)

					{

						Log.customer
								.debug("**** line item iteration doinvreconli if ");

						order = (ariba.purchasing.core.DirectOrder) doinvreconli
								.getFieldValue("Order");// Below fields commented out for CR216

						//order.setFieldValue("DWPOFlag", "InProcess");

						//order.setFieldValue("TopicName", "DWPOPush");

						if (doinvreconli.getDottedFieldValue("OrderLineItem") != null)

							doinvreconli
									.setFieldValue(
											"POLineItemNumber",
											(Integer) doinvreconli
													.getDottedFieldValue("OrderLineItem.NumberInCollection"));

						else

							doinvreconli
									.setFieldValue("POLineItemNumber", null);

						cr = (ClusterRoot) order.getFieldValue("BuyerCode");

					}

					else

					{

						Log.customer
								.debug("**** line item iteration doinvreconli else");

						// ariba.base.core.Log.customer.debug("%s For MA
						// POLineItemNumber was %s", THISCLASS,
						// (Integer)doinvreconli.getFieldValue("POLineItemNumber"));

						doinvreconli.setFieldValue("POLineItemNumber", null);

						// ariba.base.core.Log.customer.debug("%s For MA
						// POLineItemNumber After Update %s", THISCLASS,
						// (Integer)doinvreconli.getFieldValue("POLineItemNumber"));

						if (cr != null)

						{

							doinvreconli.setFieldValue("BuyerCode", cr);

						}

					}

				}

				invrecon.setFieldValue("DWInvoiceFlag", "InProcess");

				invrecon.setFieldValue("TopicName", "DWInvoicePush");

				//Base.getSession().transactionCommit();

			} // end of while

			// ariba.base.core.Log.customer.debug("%s Ending IRProcess program
			// .....", THISCLASS);

		} // end of try

		catch (Exception e)

		{

			// ariba.base.core.Log.customer.debug(e.toString());

			return;

		}

	}

//Start: Mach1 R5.5 (FRD7.1/TD7.1)
//Get the line number with highest amount for IR line and splitaccount line number
public static void Gethighestlineno(InvoiceReconciliation ir)
{
	Log.customer.debug("SAPInvoicePaymentMethodPush enters Gethighestlineno method");

	BaseVector irLineItems = ir.getLineItems();
	int irLineItemsSize = irLineItems.size();
	highestSALN=0;
	highestLILN=0;
	BigDecimal highestAmount=new BigDecimal("0");
	BigDecimal roundedAmount = new BigDecimal(0.00);
	SplitAccounting splitaccounting;

	for (int i = 0; i < irLineItemsSize; i++)

	{
		InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem) irLineItems.get(i);
		ProcureLineType lineType = irLineItem.getLineType();
		String linetypename= (String)lineType.getDottedFieldValue("UniqueName");
		Log.customer.debug("SAPInvoicePaymentMethodPush linetypename "+ linetypename);
		if(!linetypename.equalsIgnoreCase ("TaxCharge"))
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush linetypename is not TaxCharge");
			// Split Accounting Fields - started
			SplitAccountingCollection irsac = (SplitAccountingCollection) irLineItem.getAccountings();
			List accVector = (List) irsac.getSplitAccountings();
			int saaccVector = accVector.size();
			Log.customer.debug("SAPInvoicePaymentMethodPush ghl saaccVector "+ saaccVector);

			roundedAmount = new BigDecimal(0.00);

			for (int j = 0; j < saaccVector; j++)
			{
				Log.customer.debug("SAPInvoicePaymentMethodPush inside ghl splitaccounting object ");

				BaseObject sa = (BaseObject) accVector.get(j);
				splitaccounting = (SplitAccounting) accVector.get(j);

				Money Amount = (Money) sa.getFieldValue("Amount");
				if(Amount!=null)
				{
					Log.customer.debug("SAPInvoicePaymentMethodPush ghl Line Item Amount " + Amount);

					if (Amount.getDottedFieldValue("Currency.AllowedDecimal").equals("0"))
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush enters ghl huf consition Amount is  => " + Amount.getAmount());
						roundedAmount = Amount.roundAmount(Amount.getAmount(),Amount.getCurrency(), 0).abs();
						Log.customer.debug("SAPInvoicePaymentMethodPush GHL huf Line Item rounded amount Amount after abs => " + roundedAmount);

						if (highestAmount.compareTo(roundedAmount) >0)
						{
							Log.customer.debug("SAPInvoicePaymentMethodPush GHL highestAmount>roundedAmount: No action is required");
						}else
						{
							highestAmount=roundedAmount;
							Log.customer.debug("SAPInvoicePaymentMethodPush GHL highestAmount is "+highestAmount);
							highestSALN=j;
							Log.customer.debug("SAPInvoicePaymentMethodPush GHL highestSALN is "+highestSALN);
							highestLILN=i;
							Log.customer.debug("SAPInvoicePaymentMethodPush GHL highestLILN is "+highestLILN);
						}

					}
					else
					{

						Log.customer.debug("SAPInvoicePaymentMethodPush GHL No action is required");
					}
				}
				else
				{
					Log.customer.debug("SAPInvoicePaymentMethodPush GHL Line Item Amount is null => " + Amount);
				}
			}
		}
	}

}

public void getamountdiffvalue(InvoiceReconciliation ir) {

	Log.customer.debug("SAPInvoicePaymentMethodPush enters getamountdiffvalue method");
	BigDecimal totalinvroundedamount = new BigDecimal(0.00);
	BigDecimal roundedAmount = new BigDecimal(0.00);
	Money totalcost;
	BigDecimal totalcostroundedamount = new BigDecimal(0.00);

	SplitAccounting splitaccounting;
	BaseVector irLineItems = ir.getLineItems();
	int irLineItemsSize = irLineItems.size();

	for (int i = 0; i < irLineItemsSize; i++)
	{
		InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem) irLineItems.get(i);
		roundedAmount = new BigDecimal(0.00);
		ProcureLineType lineType = irLineItem.getLineType();
		Log.customer.debug("SAPInvoicePaymentMethodPush sequencenumber lineType "+ lineType);
		String linetypename= (String)lineType.getDottedFieldValue("UniqueName");
		Log.customer.debug("SAPInvoicePaymentMethodPush linetypename "+ linetypename);
		if(linetypename.equalsIgnoreCase ("TaxCharge"))
		{
			Money taxlineamount1=(Money)irLineItem.getDottedFieldValue("Amount");
			Log.customer.debug("SAPInvoicePaymentMethodPush taxlineamount1 "+ taxlineamount1);
			roundedAmount=taxlineamount1.roundAmount(taxlineamount1.getAmount(),taxlineamount1.getCurrency(), 0).abs();
			Log.customer.debug("SAPInvoicePaymentMethodPush tax line roundedAmount "+ roundedAmount);
			totalinvroundedamount = totalinvroundedamount.add(roundedAmount);
			Log.customer.debug("SAPInvoicePaymentMethodPush GADV taxline totalinvroundedamount "+ totalinvroundedamount);
		}
		else
		{
			// Split Accounting Fields - started
			SplitAccountingCollection irsac = (SplitAccountingCollection) irLineItem.getAccountings();
			List accVector = (List) irsac.getSplitAccountings();
			int saaccVector = accVector.size();
			Log.customer.debug("SAPInvoicePaymentMethodPush gadv saaccVector "+ saaccVector);
			for (int j = 0; j < saaccVector; j++)
			{
				Log.customer.debug("SAPInvoicePaymentMethodPush gadv inside splitaccounting object ");
				BaseObject sa = (BaseObject) accVector.get(j);
				splitaccounting = (SplitAccounting) accVector.get(j);
				Money Amount = (Money) sa.getFieldValue("Amount");
				if(Amount!=null)
				{
					Log.customer.debug("SAPInvoicePaymentMethodPush GADV Line Item Amount " + Amount);
					if (Amount.getDottedFieldValue("Currency.AllowedDecimal").equals("0"))
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush GADV enters huf consition Amount is  => " + Amount.getAmount());
						roundedAmount = Amount.roundAmount(Amount.getAmount(),Amount.getCurrency(), 0).abs();
						Log.customer.debug("SAPInvoicePaymentMethodPush GADV huf Line Item rounded amount Amount after abs => " + roundedAmount);
					}
					else
					{

						Log.customer.debug("SAPInvoicePaymentMethodPush GADV No action is required");
					}
				}
				else
				{
					Log.customer.debug("SAPInvoicePaymentMethodPush GADV Line Item Amount is null => " + Amount);
				}
				Log.customer.debug("SAPInvoicePaymentMethodPush GADV roundedAmount " + roundedAmount);
				totalinvroundedamount = totalinvroundedamount.add(roundedAmount);
				Log.customer.debug("SAPInvoicePaymentMethodPush GADV totalinvroundedamount "+ totalinvroundedamount);
			}
		}
	}
	totalcost = (Money) ir.getFieldValue("TotalCost");
	if(totalcost!=null){
	Log.customer.debug("SAPInvoicePaymentMethodPush GADV totalcost " +totalcost);
		if (totalcost.getDottedFieldValue("Currency.AllowedDecimal").equals("0"))
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush GADV Enters HUF currency totalcostamount " +totalcost.getAmount());
			totalcostroundedamount = totalcost.roundAmount(totalcost.getAmount(),totalcost.getCurrency(), 0).abs();
			Log.customer.debug("SAPInvoicePaymentMethodPush GADV huf totalcostroundedamount " +totalcostroundedamount);

		}
		else
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush GADV No action is required");
		}
	}
	else
	{
		Log.customer.debug("SAPInvoicePaymentMethodPush GADV totalcost is null " +totalcost);
	}
	if (totalinvroundedamount.compareTo(totalcostroundedamount) != 0)
	{
		diff = totalcostroundedamount.subtract(totalinvroundedamount);
		Log.customer.debug("SAPInvoicePaymentMethodPush GADV diff is " +diff);
	}
}
//End: Mach1 R5.5 (FRD7.1/TD7.1)
	/*
	 * public static String getStagingCurrentTime() {
	 *
	 * return Date.getNow().toString().substring(0, 20) +
	 * Date.getNow().toString().substring(24, 28);}
	 */

	public void pushToStagingTable(InvoiceReconciliation ir) {

		Log.customer.debug("SAPInvoicePaymentMethodPush.createPayments called");

		Connection db2Conn = null;

		Statement stmt = null;

		// Method to push the data into Payment staging table "IR_HEADER_DETAIL"
		// and

		// "IR_HEADER_DETAIL"

		// Header Fields declaration starts here

		String iruniquename, companycode, supplier, supppayment, hdrcurrency;

		String ordernumber, invoiceuniquename, supplierinvoicenumber, reftootherinv, taxcurrency, inveformnumber, invoicepurpose;

		String sapsource = "mach1";

		Date blockstampdate1, conversiondate1, pushtostagestamp, invoiceDate1;

		SplitAccounting splitaccounting;

		String sInsertqueryforHeader, sInsertqueryforLineitem, pushtosap, invoiceDate;

		pushtosap = "A";

		String doctype = null;

		String blockstampdate, conversiondate;

		// pushtostagestamp =Date.getNow();

		Date timestamp1 = Date.getNow();

		String timestamp = DateFormatter.getStringValue(timestamp1,
				"yyyy-MM-dd HH:mm:ss");

		String credimemo = "creditMemo";

		// String timestamp = getStagingCurrentTime();

		// Log.customer.debug("SAPInvoicePaymentMethodPush.UniqueName called" +
		// pushtostagestamp);

		Log.customer.debug("SAPInvoicePaymentMethodPush.timestamp called"
				+ timestamp);

		// Header Fields declaration ends here

		// LineItem Fields declaration starts here

		int numberincollection = 1;

		int splitacctngnumber = 1;

		// Vikram: Issue 326/MGPP 2027: New field uomDesc

		String percentage, quantity, uom, uomDesc, lineitemdescription;

		String generalledger, wbselement, costcenter, asset, internalorder, businessarea, profitcenter;

		String amount, licurrency, taxcode;

		String customsupploccity, customsupploczip, customsupplocstate, customsupploccountry;

		BigDecimal totalinvocieamount = new BigDecimal(0.00);

		BigDecimal roundedAmount = new BigDecimal(0.00);

		BigDecimal taxamount = new BigDecimal(0.00);
		BigDecimal taxlineamount = new BigDecimal(0.00);

		Money totalcost;

		BigDecimal totalcostroundedamount = new BigDecimal(0.00);

		// LineItem fields declaration ends here

		costcenter = null;

		generalledger = null;

		String withholdtax = null;

		String intercompanycode = null;

		BigDecimal lineTaxAmnt = new BigDecimal(0.00);
		BigDecimal splitLineTaxAmntCalc = new BigDecimal(0.00);
		String lineTaxAmntCurrency = null;

		// New Field Addition - For Invoice Push by Soumya Mohanty - START as part of Vertex

		// Header Level Fields
		//Holds partition for Bycyrus
		String currpartition = null;

		// Holds Currency Exchange Rate
		String currExchangeRate = null;
		//Start: Mach1 R5.5 (FRD10.4/TD10.4)
		String vatReg = null;
		int loadedFrom = 0;
		//End: Mach1 R5.5 (FRD10.4/TD10.4)

		// Line Level Fields

		// Holds Alt. Ship From State
		String altShipFromState = null;

		// Holds Alt. Ship From Country
		String altShipFromCountry = null;

		// Holds Alt. Ship-from City
		String altShipFromCity = null;

		// Holds Alt. Ship-from Postal Code
		String altShipFromPostalCode = null;

		// Holds Line item type
		String lineItemType = null;

		// Holds UNSPSC code
		String codeUNSPC = null;

		// Holds Manner of use
		String mannerofUse = null;

		// Holds Quantity
		//BigDecimal quantity = new BigDecimal(0.00);

		// Holds Mode of Transport
		String modeofTransport = null;

		// Holds Net Mass Kilogram
		String netMassKG = null;

		// Holds Incoterms
		// String incoterms = null;
		String incoterms = "";

		// Holds Nature of Transaction
		String natureofTransaction = null;

		// New Field Addition - For Invoice Push by Soumya Mohanty - END

		// DB2 Connections

		try {

			Class.forName("COM.ibm.db2.jdbc.app.DB2Driver");

			String DBName = Base.getService().getParameter(null,
					"System.Base.DBName");

			DBName = "jdbc:db2:" + DBName;

			String DBUser = Base.getService().getParameter(null,
					"System.Base.DBUser");

			String DBPwd = Base.getService().getParameter(null,
					"System.Base.DBPwd");

			db2Conn = DriverManager.getConnection(DBName, DBUser, DBPwd);

			stmt = db2Conn.createStatement();

		}

		catch (Exception ex)

		{

			String exceptionstrdb = ex.getMessage();

		}

		// DB2 connection end

		// Header Table Field : iruniquename

		iruniquename = ir.getUniqueName();

		Log.customer.debug("SAPInvoicePaymentMethodPush.UniqueName called"
				+ ir.getUniqueName());

		// Header Table Field : companycode

		if (ir.getDottedFieldValue("CompanyCode.UniqueName") != null)

		{

			companycode = (String) ir
					.getDottedFieldValue("CompanyCode.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush companycode "
					+ ir.getDottedFieldValue("CompanyCode.UniqueName"));

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush companycode "
					+ ir.getDottedFieldValue("CompanyCode.UniqueName"));

			companycode = null;

		}

		// Header Table Field : supplier

		if (ir.getDottedFieldValue("Supplier.UniqueName") != null)

		{

			supplier = (String) ir.getDottedFieldValue("Supplier.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush supplier "
					+ ir.getDottedFieldValue("Supplier.UniqueName"));

		}

		else

		{

			Log.customer.debug("SAPInvoicePaymentMethodPush supplier "
					+ ir.getDottedFieldValue("Supplier.UniqueName"));

			supplier = null;

		}

		// Header Table Field : supppayment

		if (ir.getDottedFieldValue("PaymentTerms.UniqueName") != null)

		{

			supppayment = (String) ir
					.getDottedFieldValue("PaymentTerms.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush PaymentTerms "
					+ ir.getDottedFieldValue("PaymentTerms.UniqueName"));

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush PaymentTerms "
					+ ir.getDottedFieldValue("PaymentTerms.UniqueName"));

			supppayment = null;

		}

		// Header Table Field : blockstampdate

		if (ir.getFieldValue("BlockStampDate") != null)

		{

			blockstampdate1 = (Date) ir.getFieldValue("BlockStampDate");

			// blockstampdate = blockstampdate.substring(0, 20) +
			// blockstampdate.substring(24, 28);

			blockstampdate = DateFormatter.getStringValue(blockstampdate1,
					"yyyy-MM-dd HH:mm:ss");

			Log.customer.debug("SAPInvoicePaymentMethodPush BlockStampDate "
					+ blockstampdate);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush BlockStampDate "
					+ ir.getDottedFieldValue("BlockStampDate"));

			blockstampdate = null;

		}

		// Header Table Field : hdrcurrency

		hdrcurrency = (String) ir
				.getDottedFieldValue("TotalCost.Currency.UniqueName");

		Log.customer.debug("SAPInvoicePaymentMethodPush hdrcurrency "
				+ hdrcurrency);

		// Header Table Field : conversiondate

		conversiondate1 = (Date) ir
				.getDottedFieldValue("TotalCost.ConversionDate");

		conversiondate = DateFormatter.getStringValue(conversiondate1,
				"yyyy-MM-dd HH:mm:ss");

		Log.customer.debug("SAPInvoicePaymentMethodPush conversiondate "
				+ conversiondate);

		// Header Table Field : ordernumber

		if (ir.getDottedFieldValue("Order.UniqueName") != null)

		{

			ordernumber = (String) ir.getDottedFieldValue("Order.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush ordernumber1 "
					+ ordernumber);

		}

		else if(ir.getDottedFieldValue("MasterAgreement.UniqueName") != null){

			Log.customer.debug("SAPInvoicePaymentMethodPush MasterAgreement.UniqueName "
					+ ir.getDottedFieldValue("MasterAgreement.UniqueName"));

			ordernumber = (String) ir.getDottedFieldValue("MasterAgreement.UniqueName");

		}else{
			ordernumber=null;
		}

		// Header Table Field : invoiceuniquename

		if (ir.getInvoice().getUniqueName() != null)

		{

			invoiceuniquename = ir.getInvoice().getUniqueName();

			Log.customer
					.debug("SAPInvoicePaymentMethodPush invoiceuniquename1 "
							+ invoiceuniquename);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush invoiceuniquename "
					+ ir.getInvoice().getUniqueName());

			invoiceuniquename = null;

		}

		// Header Table Field : supplierinvoicenumber

		if (ir.getInvoice().getInvoiceNumber() != null)

		{

			supplierinvoicenumber = ir.getInvoice().getInvoiceNumber();

			Log.customer
					.debug("SAPInvoicePaymentMethodPush supplierinvoicenumber11 "
							+ supplierinvoicenumber);

		}

		else {

			Log.customer
					.debug("SAPInvoicePaymentMethodPush supplierinvoicenumber "
							+ ir.getInvoice().getInvoiceNumber());

			supplierinvoicenumber = null;

		}

		// Header Table Field : reftootherinv

		if (ir.getFieldValue("RelatedCatInvoice") != null)

		{

			reftootherinv = (String) ir
					.getDottedFieldValue("RelatedCatInvoice");

			Log.customer.debug("SAPInvoicePaymentMethodPush reftootherinv "
					+ reftootherinv);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush RelatedCatInvoice "
					+ ir.getFieldValue("RelatedCatInvoice"));

			reftootherinv = null;

		}

		// Header Table Field : inveformnumber

		if (ir.getInvoice().getDottedFieldValue("InvoiceEform.UniqueName") != null)

		{

			inveformnumber = (String) ir.getInvoice().getDottedFieldValue(
					"InvoiceEform.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush inveformnumber1 "
					+ inveformnumber);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush inveformnumber "
					+ ir.getInvoice().getDottedFieldValue(
							"InvoiceEform.UniqueName"));

			inveformnumber = null;

		}

		// Header Table Field : taxamount

		if (ir.getDottedFieldValue("TaxAmount.Amount") != null)
		{
			//Start: Mach1 R5.5 (FRD7.1/TD7.1)
			Log.customer.debug("SAPInvoicePaymentMethodPush TaxAmount is not null");
			if (ir.getDottedFieldValue("TaxAmount.Currency.AllowedDecimal").equals("0"))
			{
				Log.customer.debug("SAPInvoicePaymentMethodPush enters HUF taxamounttmp");
				BigDecimal taxamounttmp = (BigDecimal) ir.getDottedFieldValue("TaxAmount.Amount");
				Log.customer.debug("SAPInvoicePaymentMethodPush huf taxamounttmp befor abs method => "+ taxamounttmp);
				taxamounttmp = taxamounttmp.abs();
				Log.customer.debug("SAPInvoicePaymentMethodPush huf taxamounttmp after abs method => "+ taxamounttmp);
				taxamount = taxamounttmp.setScale(0,taxamounttmp.ROUND_HALF_DOWN );
				Log.customer.debug("SAPInvoicePaymentMethodPush huf taxamount without decimal"+ taxamount);
			}
			//End: Mach1 R5.5 (FRD7.1/TD7.1)
			else
			{
				BigDecimal taxamounttmp = (BigDecimal) ir.getDottedFieldValue("TaxAmount.Amount");
				Log.customer.debug("SAPInvoicePaymentMethodPush taxamounttmp befor abs method => "+ taxamounttmp);
				taxamounttmp = taxamounttmp.abs();
				Log.customer.debug("SAPInvoicePaymentMethodPush taxamounttmp after abs method => "+ taxamounttmp);
				taxamount = taxamounttmp.setScale(8,taxamounttmp.ROUND_HALF_DOWN );
				Log.customer.debug("SAPInvoicePaymentMethodPush taxamount with less than 10 decimal digit =>"+ taxamount);
			}
		}

		else {
			Log.customer.debug("SAPInvoicePaymentMethodPush taxamount "	+ ir.getDottedFieldValue("TaxAmount.Amount"));
			taxamount = new BigDecimal("0");

		}
		/**
		if (ir.getInvoice().getDottedFieldValue("TotalTax.Amount") != null)

		{
			taxamount = (BigDecimal) ir.getInvoice().getDottedFieldValue("TotalTax.Amount");

			Log.customer.debug("SAPInvoicePaymentMethodPush taxamount => "+ taxamount);
			taxamount = taxamount.abs();
			Log.customer.debug("SAPInvoicePaymentMethodPush taxamount with abs method =>"+ taxamount);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush taxamount "
					+ ir.getInvoice().getDottedFieldValue("TotalTax.Amount"));

			taxamount = new BigDecimal("0");

		}
		**/

		// Header Table Field : taxcurrency

		if (ir.getDottedFieldValue("TaxAmount.Currency") != null)
		{
			taxcurrency = (String) ir.getDottedFieldValue("TaxAmount.Currency.UniqueName");
			Log.customer.debug("SAPInvoicePaymentMethodPush taxcurrency :=>  "+ taxcurrency);
		}

		else
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush taxcurrency "+ ir.getDottedFieldValue("TaxAmount.Currency.UniqueName"));
			taxcurrency = null;
		}

		/**
		if (ir.getInvoice().getDottedFieldValue("TotalTax.Currency") != null)

		{

			taxcurrency = (String) ir.getInvoice().getDottedFieldValue("TotalTax.Currency.UniqueName");

			Log.customer.debug("SAPInvoicePaymentMethodPush taxamount "
					+ taxcurrency);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush taxcurrency "
					+ ir.getInvoice().getDottedFieldValue("TotalTax.Currency.UniqueName"));

			taxcurrency = null;

		}
		**/

		// Sapsource

		if (ir.getDottedFieldValue("CompanyCode.SAPSource") != null)

		{

			sapsource = (String) ir
					.getDottedFieldValue("CompanyCode.SAPSource");

			Log.customer.debug("SAPInvoicePaymentMethodPush companycode "
					+ sapsource);

		}

		else {

			Log.customer.debug("SAPInvoicePaymentMethodPush companycode "
					+ ir.getDottedFieldValue("CompanyCode.SAPSource"));

			sapsource = null;

		}

		// invoicepurpose

		/*
		 * if(ir.getInvoice().getFieldValue("InvoicePurpose")!=null)
		 *  {
		 *
		 * invoicepurpose =
		 * (String)ir.getInvoice().getFieldValue("InvoicePurpose");
		 *
		 * Log.customer.debug("SAPInvoicePaymentMethodPush taxamount " +
		 * invoicepurpose);
		 *  }
		 *
		 * else{
		 *
		 * Log.customer.debug("SAPInvoicePaymentMethodPush taxcurrency " +
		 * ir.getInvoice().getFieldValue("InvoicePurpose"));
		 *
		 * invoicepurpose = null;
		 *  }
		 */

		if (ir.getInvoice().getDottedFieldValue("InvoiceEform.UniqueName") != null)

		{
			invoicepurpose = (String) ir.getInvoice().getDottedFieldValue(
					"InvoiceEform.Purpose");

			Log.customer.debug("SAPInvoicePaymentMethodPush invoicepurpose "
					+ invoicepurpose);

			if (invoicepurpose.equals(credimemo))
			{

				invoicepurpose = "G";

				doctype = (String) ir.getDottedFieldValue("CompanyCode.DocTypeForCreditMemo");

				Log.customer.debug("SAPInvoicePaymentMethodPush doctype "+ doctype);

			}

			else

			{
				invoicepurpose = "R";
				doctype = (String) ir.getDottedFieldValue("CompanyCode.DocTypeForInvoice");
				Log.customer.debug("SAPInvoicePaymentMethodPush doctype "+ doctype);
			}

		}

		else {
			//Santanu : Added for ASN Invoices
			Log.customer.debug("SAPInvoicePaymentMethodPush ir.getTotalCost() "	+ ir.getTotalCost());
			invoicepurpose = "R";
			doctype = (String) ir.getDottedFieldValue("CompanyCode.DocTypeForInvoice");
			if(ir.getTotalCost()!=null){
				int irCostSign = ir.getTotalCost().getSign();
				if(irCostSign < 0){
					invoicepurpose = "G";
					doctype = (String) ir.getDottedFieldValue("CompanyCode.DocTypeForCreditMemo");
				}
			}
			//Santanu : Added for ASN Invoices

		}

		if (ir.getFieldValue("InvoiceDate") != null)

		{

			// blockstampdate =
			// DateFormatter.getStringValue(blockstampdate1,"yyyy-MM-dd
			// HH:mm:ss");

			invoiceDate1 = (Date) ir.getFieldValue("InvoiceDate");

			invoiceDate = DateFormatter.getStringValue(invoiceDate1,
					"yyyy-MM-dd HH:mm:ss");

			Log.customer.debug("SAPInvoicePaymentMethodPush invoiceDate "
					+ invoiceDate);

		}

		else

		{

			invoiceDate = timestamp;

			Log.customer.debug("SAPInvoicePaymentMethodPush invoiceDate "
					+ invoiceDate);

		}

		// Header Table Field : totalcost

		// totalcost = (BigDecimal) ir.getDottedFieldValue("TotalCost.Amount");

		// totalcostroundedamount = totalcost.round

		totalcost = (Money) ir.getFieldValue("TotalCost");

		if(totalcost!=null){
		Log.customer.debug("SAPInvoicePaymentMethodPush totalcost " +totalcost);
			//Start: Mach1 R5.5 (FRD7.1/TD7.1)
			if (totalcost.getDottedFieldValue("Currency.AllowedDecimal").equals("0"))
			{
				Log.customer.debug("SAPInvoicePaymentMethodPush Enters HUF currency totalcostamount " +totalcost.getAmount());
				totalcostroundedamount = totalcost.roundAmount(totalcost.getAmount(),totalcost.getCurrency(), 0).abs();
				Log.customer.debug("SAPInvoicePaymentMethodPush huf totalcostroundedamount " +totalcostroundedamount);

			}
			else
			{
				totalcostroundedamount = totalcost.roundAmount(totalcost.getAmount(),totalcost.getCurrency(), 2).abs();
				Log.customer.debug("SAPInvoicePaymentMethodPush totalcostroundedamount " +totalcostroundedamount);
			}
			//End: Mach1 R5.5 (FRD7.1/TD7.1)
		}
		else
		{
			Log.customer.debug("SAPInvoicePaymentMethodPush totalcost is null " +totalcost);
		}

		//if (invoicepurpose.equals(credimemo))
			//totalcostroundedamount = totalcostroundedamount.negate();

		Log.customer.debug("SAPInvoicePaymentMethodPush totalcostroundedamount1 "+ totalcostroundedamount);

		// Added WITHHOLDTAX field for CBS - Mapping will be updated later once
		// Ariba IR objects are modified

		// withholdtax = null;

		if (ir.getDottedFieldValue("WithHoldTaxCode") != null)

		{

			withholdtax = (String) ir.getDottedFieldValue("WithHoldTaxCode.UniqueName");
			Log.customer.debug("SAPInvoicePaymentMethodPush WithHoldTaxCode "+ ir.getDottedFieldValue("WithHoldTaxCode.UniqueName"));

		}

		else {
			withholdtax = null;
			Log.customer.debug("SAPInvoicePaymentMethodPush WithHoldTaxCode is "+ withholdtax);


		}
		/****** Abhishek : Changes done for Bycyrus starts ******/
		if (ir.getPartition() != null)
		{
			currpartition = (String) ir.getPartition().getName();
			Log.customer.debug("SAPInvoicePaymentMethodPush currpartition is "+ currpartition);
		}
		/****** Abhishek : Changes done for Bycyrus ends ******/
		//Invoice Push Changes -HEADER LEVEL- Soumya Mohanty - START- as part of Vertex
		// Header Table Field : CurrExchangeRate
		if (ir.getFieldValue("CurrencyExchangeRate") != null)
		{
			currExchangeRate = (String) ir.getFieldValue("CurrencyExchangeRate");
			Log.customer.debug("SAPInvoicePaymentMethodPush CurrExchangeRate "+ ir.getFieldValue("CurrencyExchangeRate"));
		}
		else
		{
			currExchangeRate = null;
			Log.customer.debug("SAPInvoicePaymentMethodPush CurrExchangeRate is "+ currExchangeRate);
		}
		// Invoice Push Changes -HEADER LEVEL- Soumya Mohanty - END

		//Start: Mach1 R5.5 (FRD10.3/TD10.3)
		if (ir.getDottedFieldValue("Invoice.VATRegistration") != null)
		{
	               vatReg = (String) ir.getDottedFieldValue("Invoice.VATRegistration");
	               Log.customer.debug("SAPInvoicePaymentMethodPush vatReg "+ ir.getDottedFieldValue("Invoice.VATRegistration"));

		}
		else
		{
			vatReg = null;
			Log.customer.debug("SAPInvoicePaymentMethodPush vatReg is "+ vatReg);
		}
        //End: Mach1 R5.5 (FRD10.3/TD10.3)

		//Start: Mach1 R5.5 (FRD10.4/TD10.4)
		if (((Integer)ir.getDottedFieldValue("Invoice.LoadedFrom")).intValue() != 0)
		{
			int loadedFromASN = ((Integer)ir.getDottedFieldValue("Invoice.LoadedFrom")).intValue();
			Log.customer.debug("SAPInvoicePaymentMethodPush LoadedFrom "+ ir.getDottedFieldValue("Invoice.LoadedFrom"));
			if(loadedFromASN == 1)
						     {
				               loadedFrom = 1;}
		}
		else
		{
			loadedFrom = 0;
			Log.customer.debug("SAPInvoicePaymentMethodPush loadedFrom is "+ loadedFrom);
		}
        //End: Mach1 R5.5 (FRD10.4/TD10.4)

		// Line Item Level Fields :

		BaseVector irLineItems = ir.getLineItems();
		int irLineItemsSize = irLineItems.size();
		int sequencenumber = 0;

		for (int i = 0; i < irLineItemsSize; i++)

		{
			InvoiceReconciliationLineItem irLineItem = (InvoiceReconciliationLineItem) irLineItems.get(i);
			ProcureLineType lineType = irLineItem.getLineType();

			// LineItem Fields declaration starts here
			// String
			// numberincollection,splitacctngnumber,percentage,quantity,uom,lineitemdescription;
			// String
			// generalledger,wbselement,costcenter,asset,internalorder,businessarea,profitcenter;
			// String amount,licurrency,taxcode;
			// String
			// customsupploccity,customsupploczip,customsupplocstate,customsupploccountry;
		Log.customer.debug("SAPInvoicePaymentMethodPush sequencenumber "+ sequencenumber);
		Log.customer.debug("SAPInvoicePaymentMethodPush sequencenumber lineType "+ lineType);
			String linetypename= (String)lineType.getDottedFieldValue("UniqueName");
			Log.customer.debug("SAPInvoicePaymentMethodPush linetypename "+ linetypename);
			if(linetypename.equalsIgnoreCase ("TaxCharge"))
			{
			Log.customer.debug("SAPInvoicePaymentMethodPush inside ");
			Money taxlineamount1=(Money)irLineItem.getDottedFieldValue("Amount");
			Log.customer.debug("SAPInvoicePaymentMethodPush taxlineamount1 "+ taxlineamount1);
			taxlineamount=taxlineamount1.roundAmount(taxlineamount1.getAmount(),taxlineamount1.getCurrency(), 2).abs();
			Log.customer.debug("SAPInvoicePaymentMethodPush taxlineamount "+ taxlineamount);
			}

			if(!linetypename.equalsIgnoreCase ("TaxCharge"))
			{
					if (lineType != null)

					{
				// LineItem Level Fields : taxcurrency
				numberincollection = irLineItem.getNumberInCollection();
				Log.customer.debug("SAPInvoicePaymentMethodPush Line Item number "+ irLineItem.getNumberInCollection());

				// LineItem Level Fields : taxcode

				// Commented by Majid - 2008-10-7

				// if
				// (irLineItem.getDottedFieldValue("TaxCode.SAPSource")!=null)

				// taxcode = (String)
				// irLineItem.getDottedFieldValue("TaxCode.SAPSource");

				if (irLineItem.getDottedFieldValue("TaxCode.SAPTaxCode") != null) {
					taxcode = (String) irLineItem.getDottedFieldValue("TaxCode.SAPTaxCode");
				} else

				{
					taxcode = (String) irLineItem.getDottedFieldValue("TaxCode.UnqiueName");
					Log.customer.debug("SAPInvoicePaymentMethodPush taxcode "+ taxcode);
				// taxcode = null;
				}

				/* Wrong approach
				// Uom line item level field QuantityUOM

				if (irLineItem.getFieldValue("QuantityUOM") != null)
					uom = (String) irLineItem.getDottedFieldValue("QuantityUOM");

				else

				{
					Log.customer.debug("SAPInvoicePaymentMethodPush QuantityUOM is null ");
					uom = null;

				}
				*/

				// Vikram Issue 326/ MGPP 2027 changes for fields UOM and UOM Description starts

				// Uom line item level field QuantityUOM

				if (irLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null)
					uom = (String) irLineItem.getDottedFieldValue("Description.UnitOfMeasure.UniqueName");

				else

				{
					Log.customer.debug("SAPInvoicePaymentMethodPush QuantityUOM is null ");
					uom = null;

				}

				// UOM Description

				if (irLineItem.getDottedFieldValue("Description.UnitOfMeasure") != null)
					uomDesc = (String) irLineItem.getDottedFieldValue("Description.UnitOfMeasure.Description.PrimaryString");

				else

				{
					Log.customer.debug("SAPInvoicePaymentMethodPush QuantityUOM is null ");
					uomDesc = null;

				}

				// Vikram Issue 326/ MGPP 2027 changes for fields UOM and UOM Description ends

				// LineItem Level Fields : lineitemdescription
				/*
				 * Work item #1067 and MACH1 CBS-CR209: Changes to line item description
				 * Change: to send inter company accounting fields in case of inter company transactions
				 * Ravindra Prabhu (rprabhu1@in.ibm.com)
				 * Getting account category and company source
				 */
				String accCategory="";
				String companySource="";
				if(irLineItem.getOrderLineItem()!=null){
					accCategory=irLineItem.getOrderLineItem().getFieldValue("AccountCategory")!=null?((String)irLineItem.getOrderLineItem().getDottedFieldValue("AccountCategory.UniqueName")):"";
					companySource=irLineItem.getOrder().getFieldValue("CompanyCode")!=null?((String)irLineItem.getOrder().getDottedFieldValue("CompanyCode.SAPSource")):"";
				}
				else if(irLineItem.getMALineItem()!=null){
					accCategory=irLineItem.getMALineItem().getFieldValue("AccountCategory")!=null?((String)irLineItem.getMALineItem().getDottedFieldValue("AccountCategory.UniqueName")):"";
					companySource=irLineItem.getMasterAgreement().getFieldValue("CompanyCode")!=null?((String)irLineItem.getMasterAgreement().getDottedFieldValue("CompanyCode.SAPSource")):"";
				}
				String tradePartnerGL="";
				String tradePartnerCC="";
				/*
				 * end of first part, logic continues in split accounting loop ahead
				 */
				if (irLineItem.getDottedFieldValue("Description.Description") != null){
					lineitemdescription = (String) irLineItem.getDottedFieldValue("Description.Description");
					Log.customer.debug("SAPInvoicePaymentMethodPush lineitemdescription before removing special chararcteres "+ lineitemdescription);
					lineitemdescription = StringUtil.replaceCharByChar(lineitemdescription, '\r', ' ');
					Log.customer.debug("SAPInvoicePaymentMethodPush lineitemdescription after removing special chararcteres .1"+ lineitemdescription);
					lineitemdescription = StringUtil.replaceCharByChar(lineitemdescription, '\t', ' ');
					Log.customer.debug("SAPInvoicePaymentMethodPush lineitemdescription after removing special chararcteres .2"+ lineitemdescription);
					lineitemdescription = StringUtil.replaceCharByChar(lineitemdescription, '\n', ' ');
					Log.customer.debug("SAPInvoicePaymentMethodPush lineitemdescription after removing special chararcteres .3"+ lineitemdescription);
					lineitemdescription = StringUtil.replaceCharByChar(lineitemdescription, '\'', ' ');
					Log.customer.debug("SAPInvoicePaymentMethodPush lineitemdescription after removing special chararcteres .4"+ lineitemdescription);
				}
				else{
					lineitemdescription = null;
				}

				// LineItem Level Fields : CustomSuppLoc

				// Updated -- based on changes requedsted by Mach1 Team on 27th
				// Oct 2008

				//

				// if (irLineItem.getDottedFieldValue("CustomSuppLoc")!=null )

				if (irLineItem.getDottedFieldValue("ShipTo") != null && sapsource != null &&  (sapsource.equals("MACH1") || sapsource.equals("CGM")) )
				{

					customsupploccity = (String) irLineItem.getDottedFieldValue("ShipTo.City");
					customsupploczip = (String) irLineItem.getDottedFieldValue("ShipTo.PostalCode");
					customsupplocstate = (String) irLineItem.getDottedFieldValue("ShipTo.State");
					if (irLineItem.getDottedFieldValue("ShipTo.Country") != null) {

						customsupploccountry = (String) irLineItem.getDottedFieldValue("ShipTo.Country.UniqueName");
						}
					else
					{

											Log.customer.debug("SAPInvoicePaymentMethodPush CustomSuppLoc "+ irLineItem.getDottedFieldValue("ShipTo.Country"));
											customsupploccountry = null;
									}

				// Log.customer.debug("SAPInvoicePaymentMethodPush
				// reftootherinv " + reftootherinv);
				}

				else

				{

					Log.customer.debug("SAPInvoicePaymentMethodPush CustomSuppLoc "+ irLineItem.getDottedFieldValue("CustomSuppLoc"));

					customsupploccity = null;
					customsupploczip = null;
					customsupplocstate = null;
					customsupploccountry = null;
					}

				Log.customer.debug("SAPInvoicePaymentMethodPush customsupploccity "+ customsupploccity);

				Log.customer.debug("SAPInvoicePaymentMethodPush customsupploczip "
								+ customsupploczip);
				Log.customer.debug("SAPInvoicePaymentMethodPush customsupplocstate "+ customsupplocstate);

				Log.customer.debug("SAPInvoicePaymentMethodPush customsupploccountry "+ customsupploccountry);

				// Split Accounting Fields - started

				SplitAccountingCollection irsac = (SplitAccountingCollection) irLineItem.getAccountings();

				List accVector = (List) irsac.getSplitAccountings();

				int saaccVector = accVector.size();


				Log.customer.debug("SAPInvoicePaymentMethodPush saaccVector "
						+ saaccVector);

				roundedAmount = new BigDecimal(0.00);

				BigDecimal totalSplitTaxAmnt= new BigDecimal("0.00");


				for (int j = 0; j < saaccVector; j++)

				{
					Log.customer
							.debug("SAPInvoicePaymentMethodPush inside splitaccounting object ");

					BaseObject sa = (BaseObject) accVector.get(j);



					splitaccounting = (SplitAccounting) accVector.get(j);

					percentage = splitaccounting.getPercentage().toString();

					quantity = splitaccounting.getQuantity().toString();

					int spacnumber = j + 1;

					sequencenumber = sequencenumber + 1;

					profitcenter = null;

					asset = null;

					businessarea = null;

					Log.customer
							.debug("SAPInvoicePaymentMethodPush sequencenumber "
									+ sequencenumber);

					// String spacnumber =
					// sa.getFieldValue("NumberInCollection").toString();

					// Log.customer.debug("SAPInvoicePaymentMethodPush
					// spacnumber " + spacnumber);

					// if ((sa.getFieldValue("GeneralLedgerText"))! = null)

					// {

					// generalledger =
					// sa.getFieldValue("GeneralLedgerText").toString();

					// Log.customer.debug("SAPInvoicePaymentMethodPush
					// generalledger " + generalledger);
					// }

					/*
					 * Work item #1067 and MACH1 CBS-CR209: Changes to line item description
					 * Change: to send inter company accounting fields in case of inter company transactions
					 * Ravindra Prabhu (rprabhu1@in.ibm.com)
					 * Getting account category and company source
					 */
					if(accCategory.equalsIgnoreCase(this.zAccCat) && companySource.equalsIgnoreCase(this.CBSSource)){
						tradePartnerGL=sa.getFieldValue("TradingPartnerGL")!=null?((String) sa.getFieldValue("TradingPartnerGL")):"";
						tradePartnerCC=sa.getFieldValue("TradingPartnerCC")!=null?((String)sa.getFieldValue("TradingPartnerCC")):"";
						lineitemdescription = tradePartnerGL.concat("/".concat(tradePartnerCC));
					}
					/*
					 * End of CR 209
					 */
					if ((String) sa.getFieldValue("CostCenterText") == null){

						Log.customer
								.debug("SAPInvoicePaymentMethodPush costcenter is null");

					}

					else

					{

						costcenter = (String) sa
								.getFieldValue("CostCenterText");

						Log.customer
								.debug("SAPInvoicePaymentMethodPush costcenter "
										+ costcenter);

					}

					if ((String) sa.getFieldValue("GeneralLedgerText") == null)

					{

						Log.customer
								.debug("SAPInvoicePaymentMethodPush generalledger is null  ");

					}

					else

					{

						generalledger = (String) sa
								.getFieldValue("GeneralLedgerText");

						Log.customer
								.debug("SAPInvoicePaymentMethodPush generalledger "
										+ generalledger);

					}

					if ((String) sa.getFieldValue("WBSElementText") == null)

					{

						wbselement = null;

					}

					else

					{

						wbselement = sa.getFieldValue("WBSElementText")
								.toString();

					}

					if ((String) sa.getFieldValue("InternalOrderText") == null)

					{

						internalorder = null;

					}

					else {

						internalorder = sa.getFieldValue("InternalOrderText")
								.toString();

						Log.customer
								.debug("SAPInvoicePaymentMethodPush internalorder "
										+ internalorder);

					}

					// Added to have additional Logic for IntercompanyCode -
					// Logic will be added once IR objects are updated - Majid
					// intercompanycode = null;

					if (irLineItem.getFieldValue("TradingPartner") != null)
					{

					intercompanycode = (String) irLineItem.getDottedFieldValue("TradingPartner.UniqueName");
					Log.customer.debug("SAPInvoicePaymentMethodPush IntercompanyCode "	+ intercompanycode);

					}

					else
					{
					Log.customer.debug("SAPInvoicePaymentMethodPush intercompanycode is null  ");
					}

					// Line Item tax Amount -- Started


					// lineTaxAmnt

					// Money Amount = (Money) sa.getFieldValue("Amount");

					//Money lineTaxAmountMoney
					if(irLineItem.getFieldValue("TaxAmount")!= null)
					{
						// Get the Line Item Amount
						Log.customer.debug("SAPInvoicePaymentMethodPush TaxAmount object is not null");
						Money lineTaxAmountMoney  = (Money) irLineItem.getFieldValue("TaxAmount");
						Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney => "+ lineTaxAmountMoney);
						lineTaxAmnt = lineTaxAmountMoney.roundAmount(lineTaxAmountMoney.getAmount(),lineTaxAmountMoney.getCurrency(), 2).abs();
						Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney.Amount => "+ lineTaxAmnt);

						// LineTaxAmntCurrency

						if(lineTaxAmountMoney.getFieldValue("Currency")!=null)
						{
							Log.customer.debug("SAPInvoicePaymentMethodPush TaxAmount Currency object is not null");
							lineTaxAmntCurrency = (String) lineTaxAmountMoney.getDottedFieldValue("Currency.UniqueName");
							Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmntCurrency => " + lineTaxAmountMoney.getDottedFieldValue("Currency.UniqueName"));
							Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmntCurrency => " + lineTaxAmntCurrency);

						}
						else
						{
							Log.customer.debug("SAPInvoicePaymentMethodPush TaxAmount Currency object is null");
							lineTaxAmntCurrency = null;
							Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmntCurrency => " + lineTaxAmntCurrency);
						}


						// splitLineTaxAmntCalc
						// Needs to distribute the Line Item tax amount to Split Item level based on Percentage.
						// Assign the LineItem Tax amount value if the Split Accounting array size is 1
						// Assign the Split Tax amount for the last array element using Total Tax amount - cumulative line item split tax amount
						// or Percentage is 100 %

						Log.customer.debug("SAPInvoicePaymentMethodPush Split Account Array Number  => "+ spacnumber);
						Log.customer.debug("SAPInvoicePaymentMethodPush Split Account Array Size  => "+ saaccVector);


						if(saaccVector == 1)
						{
							Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney.Amount => "+ lineTaxAmnt);
							splitLineTaxAmntCalc = lineTaxAmnt;
							// Round off to two digit :
							Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney.Amount => "+ splitLineTaxAmntCalc);
						}
						else
						{
							// If Split account is the last element of an array then adjust with remaining amount
							if(spacnumber == saaccVector)
							{
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage => For Last Element of array => ");
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage => previuos totalSplitAmnt => " + totalSplitTaxAmnt);
								Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney.Amount => "+ lineTaxAmnt);
								splitLineTaxAmntCalc = lineTaxAmnt.subtract(totalSplitTaxAmnt);
							}
							else
							{
								// Get Perecentage
								BigDecimal split_percent = new BigDecimal("0.00");
								split_percent = splitaccounting.getPercentage();
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage => "+ split_percent);

								Log.customer.debug("SAPInvoicePaymentMethodPush lineTaxAmountMoney.Amount => "+ lineTaxAmnt);

								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage => Calculating the Splitted amount ");

								BigDecimal roundedsplitLineTaxAmntCalc  = lineTaxAmnt.multiply(split_percent).multiply(new BigDecimal("0.01"));
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage Before Calcualtion roundedsplitLineTaxAmntCalc => " +roundedsplitLineTaxAmntCalc);

								splitLineTaxAmntCalc = roundedsplitLineTaxAmntCalc.setScale(2, roundedsplitLineTaxAmntCalc.ROUND_HALF_DOWN);
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage Before Calcualtion splitLineTaxAmntCalc => " +splitLineTaxAmntCalc);
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage After Calcualtion and Round off splitLineTaxAmntCalc => " +splitLineTaxAmntCalc);

								// Cumulative SplitTax Amount
								totalSplitTaxAmnt = totalSplitTaxAmnt.add(splitLineTaxAmntCalc);
								Log.customer.debug("SAPInvoicePaymentMethodPush Percentage => After adding with previuos totalSplitAmnt => " + totalSplitTaxAmnt);
							}

						}

					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush TaxAmount object is null");
						lineTaxAmnt = new BigDecimal("0.0");
						lineTaxAmntCurrency = null;
						splitLineTaxAmntCalc = new BigDecimal("0.0");

					}


					// Line Item tax Amoun -- End

					// if ((String)sa.getFieldValue("WBSElementText"))! = null)

					// wbselement =
					// sa.getFieldValue("WBSElementText").toString();*/

					// internalorder =null;

					// wbselement =null;
					// ***********************************************
					// Added to have additional Fields for Invoice Push -
					// Logic will be added once IR objects are updated - Soumya Mohanty as part of Vertex


					// AltShipFromState - START
					// AltShipFromState - ShipFrom.PostalAddress.State
					if (irLineItem.getDottedFieldValue("ShipFrom.PostalAddress") != null)
					{
						altShipFromState = (String) irLineItem.getDottedFieldValue("ShipFrom.PostalAddress.State");
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromState "	+ altShipFromState);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromState is null  ");
					}
					// AltShipFromState - END


					// AltShipFromCountry - START
					// AltShipFromCountry - ShipFrom.PostalAddress.Country.UniqueName
					if (irLineItem.getDottedFieldValue("ShipFrom.PostalAddress.Country") != null)
					{
						altShipFromCountry = (String) irLineItem.getDottedFieldValue("ShipFrom.Country.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromCountry "	+ altShipFromCountry);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromCountry is null  ");
					}
					// AltShipFromCountry - END

					// AltShipFromCity - START
					// AltShipFromCity - ShipFrom.PostalAddress.City
					if (irLineItem.getDottedFieldValue("ShipFrom.PostalAddress") != null)
					{
						altShipFromCity = (String) irLineItem.getDottedFieldValue("ShipFrom.PostalAddress.City");
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromCity "	+ altShipFromCity);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromCity is null  ");
					}
					// AltShipFromCity - END

					// AltShipFromPostalCode - START
					// AltShipFromPostalCode - ShipFrom.PostalAddress.PostalCode
					if (irLineItem.getDottedFieldValue("ShipFrom.PostalAddress") != null)
					{
						altShipFromPostalCode = (String) irLineItem.getDottedFieldValue("ShipFrom.PostalAddress.PostalCode");
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromPostalCode "	+ altShipFromPostalCode);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush AltShipFromPostalCode is null  ");
					}
					// AltShipFromPostalCode - END

					// LineItemType - START
					// LineItemType - LineItemType
					if (irLineItem.getFieldValue("LineItemType") != null)
					{
						lineItemType = (String) irLineItem.getFieldValue("LineItemType");
						//
						if (lineItemType.contains("TQM"))
						{
							lineItemType = "M";
						}
						if (lineItemType.contains("TQB"))
						{
							lineItemType = "B";
						}
						if (lineItemType.contains("TQC"))
						{
							lineItemType = "C";
						}
						if (lineItemType.contains("TQS"))
						{
							lineItemType = "S";
						}
						//
						Log.customer.debug("SAPInvoicePaymentMethodPush LineItemType "	+ lineItemType);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush LineItemType is null  ");
					}
					// LineItemType - END

					// CodeUNSPC - START
					// CodeUNSPC - Description.CommonCommodityCode.UniqueName
					if (irLineItem.getDottedFieldValue("Description.CommonCommodityCode") != null)
					{
						codeUNSPC = (String) irLineItem.getDottedFieldValue("Description.CommonCommodityCode.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush CodeUNSPC "	+ codeUNSPC);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush CodeUNSPC is null  ");
					}
					// CodeUNSPC - END

					// MannerofUse - START
					// MannerofUse - TaxUse.UniqueName
					if (irLineItem.getFieldValue("TaxUse") != null)
					{
						mannerofUse = (String) irLineItem.getDottedFieldValue("TaxUse.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush MannerofUse "	+ mannerofUse);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush MannerofUse is null  ");
					}
					// MannerofUse - END

					// ModeofTransport START
					// ModeofTransport - TransportMode
					if (irLineItem.getFieldValue("TransportMode") != null)
					{
						modeofTransport = (String) irLineItem.getDottedFieldValue("TransportMode.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush ModeofTransport "	+ modeofTransport);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush ModeofTransport is null  ");
					}
					// ModeofTransport END

					// NetMassKG START
					// NetMassKG - NetWeight
					if (irLineItem.getFieldValue("NetWeight") != null)
					{
						netMassKG = (String) irLineItem.getFieldValue("NetWeight");
						Log.customer.debug("SAPInvoicePaymentMethodPush NetMassKG "	+ netMassKG);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush NetMassKG is null  ");
					}
					// NetMassKG - NetWeight END

					// Incoterms START
					// Incoterms - IncoTerms1

					if (irLineItem.getFieldValue("IncoTerms1") != null)
					{
						incoterms = (String) irLineItem.getDottedFieldValue("IncoTerms1.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush Incoterms "	+ incoterms);
					}
					else
					{
						incoterms = "";
						Log.customer.debug("SAPInvoicePaymentMethodPush Incoterms was NULL, hence setting to blank ");
					}
					// Incoterms END

					// Nature of Transaction START - CONFIRM
					// NatureofTransaction - TransactionNature

					if (irLineItem.getFieldValue("TransactionNature") != null)
					{
						natureofTransaction = (String) irLineItem.getDottedFieldValue("TransactionNature.UniqueName");
						Log.customer.debug("SAPInvoicePaymentMethodPush Nature of Transaction "	+ natureofTransaction);
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush Nature of Transaction is null  ");
					}
					// Nature of Transaction END - CONFIRM
					// natureofTransaction
					//**********************************************************

					Log.customer
							.debug("SAPInvoicePaymentMethodPush spacnumber "
									+ spacnumber);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush generalledger "
									+ generalledger);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush costcenter "
									+ costcenter);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush internalorder "
									+ internalorder);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush wbselement "
									+ wbselement);

					// Log.customer.debug("SAPInvoicePaymentMethodPush Amount "
					// + Amount);

					// int spacnumber =
					// (int)sa.getFieldValue("NumberInCollection");

					// Log.customer.debug("SAPInvoicePaymentMethodPush
					// spacnumber " + spacnumber);

					// TBD logic to make sure that total amount is going to be
					// correct


					Money Amount = (Money) sa.getFieldValue("Amount");

					if(Amount!=null)
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush Line Item Amount " + Amount);
						//Start: Mach1 R5.5 (FRD7.1/TD7.1)
						if (Amount.getDottedFieldValue("Currency.AllowedDecimal").equals("0"))
						{
							Log.customer.debug("SAPInvoicePaymentMethodPush enters huf consition Amount is  => " + Amount.getAmount());
							roundedAmount = Amount.roundAmount(Amount.getAmount(),Amount.getCurrency(), 0).abs();
							Log.customer.debug("SAPInvoicePaymentMethodPush huf Line Item rounded amount Amount after abs => " + roundedAmount);

							if (i==highestLILN && j==highestSALN)
							{
								Log.customer.debug("SAPInvoicePaymentMethodPush enters i=highestLILN && j=highestSALN => " + roundedAmount);

								roundedAmount=roundedAmount.add(diff);
								Log.customer.debug("SAPInvoicePaymentMethodPush huf Line Item rounded amount after adding diff => " + roundedAmount);
							}

						}
						else
						{
							roundedAmount = Amount.roundAmount(Amount.getAmount(),Amount.getCurrency(), 2).abs();
							Log.customer.debug("SAPInvoicePaymentMethodPush Line Item rounded amount Amount after abs => " + roundedAmount);
						}
						//End: Mach1 R5.5 (FRD7.1/TD7.1)
					}
					else
					{
						Log.customer.debug("SAPInvoicePaymentMethodPush Line Item Amount is null => " + Amount);
						Log.customer.debug("SAPInvoicePaymentMethodPush Line Item rounded Amount=> " + roundedAmount);
					}

					//if (invoicepurpose.equals(credimemo))

					//	roundedAmount = roundedAmount.negate();

					Log.customer.debug("SAPInvoicePaymentMethodPush roundedAmount " + roundedAmount);

					totalinvocieamount = totalinvocieamount.add(roundedAmount);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush totalinvocieamount "
									+ totalinvocieamount);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush iruniquename "
									+ iruniquename);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush numberincollection "
									+ numberincollection);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush spacnumber "
									+ spacnumber);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush percentage "
									+ percentage);

					Log.customer.debug("SAPInvoicePaymentMethodPush quantity "
							+ quantity);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush uom " + uom);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush lineitemdescription "
									+ lineitemdescription);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush generalledger "
									+ generalledger);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush costcenter "
									+ costcenter);

					Log.customer.debug("SAPInvoicePaymentMethodPush asset "
							+ asset);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush internalorder "
									+ internalorder);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush businessarea "
									+ businessarea);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush profitcenter "
									+ profitcenter);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush hdrcurrency "
									+ hdrcurrency);

					Log.customer.debug("SAPInvoicePaymentMethodPush taxcode "
							+ taxcode);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush customsupploccity "
									+ customsupploccity);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush customsupploczip "
									+ customsupploczip);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush customsupplocstate "
									+ customsupplocstate);

					Log.customer
							.debug("SAPInvoicePaymentMethodPush customsupploccountry "
									+ customsupploccountry);

					try {

						sInsertqueryforLineitem = "INSERT INTO IR_LINEITEM_ACCNTG(IRUNIQUENAME, NUMBERINCOLLECTION, SPLITACCTNGNUMBER, "
								+

								"PERCENTAGE, QUANTITY, "
								+

								"UOM, "
								+

								"LINEITEMDESCRIPTION, "
								+

								"GENERALLEDGER, "
								+

								"WBSELEMENT, "
								+

								"COSTCENTER, ASSET, "
								+

								"INTERNALORDER, BUSINESSAREA, "
								+

								"PROFITCENTER, AMOUNT, "
								+

								"CURRENCY, "
								+

								"TAXCODE, CUSTOMSUPPLOCCITY, CUSTOMSUPPLOCZIP,CUSTOMSUPPLOCSTATE,CUSTOMSUPPLOCCOUNTRY,SEQUENCENUMBER,INTERCOMPANYCODE , LINETAXAMNT, LINETAXAMNTCURRENCY,SPLITLINETAXAMNTCALC, "
								+
								// Vikram: Issue 326/MGPP 2027: New field UOMDESC

								"ALTSHIPFROMSTATE, ALTSHIPFROMCOUNTRY, ALTSHIPFROMCITY, ALTSHIPFROMPOSTALCODE, LINEITEMTYPE, CODEUNSPC, MANNEROFUSE, MODEOFTRANSPORT, NETMASSKG, INCOTERMS, NATUREOFTRANSACTION, UOMDESC) "
								+

								"VALUES ( '" + iruniquename + "', " +

								numberincollection + ", " +

								spacnumber + ", '" +

								percentage + "', '" +

								quantity + "', '" +

								uom + "', '" +

								lineitemdescription + "', '" +

								generalledger + "', '" +

								wbselement + "', '" +

								costcenter + "', '" +

								asset + "', '" +

								internalorder + "', '" +

								businessarea + "', '" +

								profitcenter + "', " +

								roundedAmount + ", '" +

								hdrcurrency + "', '" +

								taxcode + "', '" +

								customsupploccity + "', '" +

								customsupploczip + "', '" +

								customsupplocstate + "', '" +

								customsupploccountry + "'," +

								sequencenumber +

								",'" + intercompanycode + "'," +lineTaxAmnt + ",'" + lineTaxAmntCurrency + "',"+ splitLineTaxAmntCalc +

								// Vikram: Issue 326/MGPP 2027: New field uomDesc

								",'" + altShipFromState + "','" + altShipFromCountry + "','" + altShipFromCity + "','" + altShipFromPostalCode + "','" + lineItemType + "','" + codeUNSPC + "','" + mannerofUse + "','" + modeofTransport + "','" + netMassKG + "','" + incoterms + "','" + natureofTransaction + "','" + uomDesc + "')";

						Log.customer.debug(sInsertqueryforLineitem);

						stmt.executeUpdate(sInsertqueryforLineitem);

					}

					catch (Exception ex)

					{

						// db2Conn.rollback();

						String exceptionstr1 = ex.getMessage();

						Log.customer.debug("exception in line" + exceptionstr1);

					}

				}

				Log.customer.debug("completed inserting line item details");

				// Split Accounting Fields - ends

			}
			}//additional section

		}// loop close

		// if(invoicepurpose.equals(credimemo))

		// totalinvocieamount =totalinvocieamount.negate();

		// Log.customer.debug("SAPInvoicePaymentMethodPush totalinvocieamount "
		// + totalinvocieamount);

		// LineItem Fields declaration ends here

		if (totalinvocieamount.compareTo(totalcostroundedamount) != 0)

		{

			totalcostroundedamount = totalinvocieamount;

			Log.customer
					.debug(" Totalinvoice amount after rounding is not matching with the Header amount after rounding ");

			// TBD Logic for rounding off

		}


		// Adding up the Tax Amount with Total Cost :

		// totalcostroundedamount and taxamount

			//totalcostroundedamount = totalcostroundedamount.add(taxlineamount);
			//Log.customer.debug(" Total Invoice before adding Header Tax amount => " + totalcostroundedamount);
			Log.customer.debug(" Total Invoice before adding Header Tax amount => " + totalcostroundedamount);
			totalcostroundedamount = totalcostroundedamount.add(taxamount);
			Log.customer.debug(" Total Invoice after adding Header Tax amount => " + totalcostroundedamount);



		Log.customer
				.debug("SAPInvoicePaymentMethodPush :Header  Level Details");

		Log.customer.debug("SAPInvoicePaymentMethodPush iruniquename "
				+ iruniquename);

		Log.customer.debug("SAPInvoicePaymentMethodPush companycode "
				+ companycode);

		Log.customer.debug("SAPInvoicePaymentMethodPush supplier " + supplier);

		Log.customer.debug("SAPInvoicePaymentMethodPush supppayment "
				+ supppayment);

		Log.customer.debug("SAPInvoicePaymentMethodPush blockstampdate "
				+ blockstampdate);

		Log.customer
				.debug("SAPInvoicePaymentMethodPush totalcost " + totalcost);

		Log.customer.debug("SAPInvoicePaymentMethodPush hdrcurrency "
				+ hdrcurrency);

		Log.customer.debug("SAPInvoicePaymentMethodPush conversiondate "
				+ conversiondate);

		Log.customer.debug("SAPInvoicePaymentMethodPush ordernumber "
				+ ordernumber);

		Log.customer.debug("SAPInvoicePaymentMethodPush invoiceuniquename "
				+ invoiceuniquename);

		Log.customer.debug("SAPInvoicePaymentMethodPush supplierinvoicenumber "
				+ supplierinvoicenumber);

		Log.customer.debug("SAPInvoicePaymentMethodPush reftootherinv "
				+ reftootherinv);

		Log.customer
				.debug("SAPInvoicePaymentMethodPush taxamount " + taxamount);

		Log.customer.debug("SAPInvoicePaymentMethodPush taxcurrency "
				+ taxcurrency);

		Log.customer.debug("SAPInvoicePaymentMethodPush inveformnumber "
				+ inveformnumber);

		// Log.customer.debug("SAPInvoicePaymentMethodPush pushtostagestamp " +
		// pushtostagestamp);

		// Log.customer.debug("SAPInvoicePaymentMethodPush pushtostagestamp " +
		// pushtostagestamp);

		Log.customer
				.debug("SAPInvoicePaymentMethodPush sapsource " + sapsource);

		Log.customer
				.debug("SAPInvoicePaymentMethodPush timestamp " + timestamp);

		Log.customer.debug("SAPInvoicePaymentMethodPush invoicepurpose "
				+ invoicepurpose);

		Log.customer.debug("SAPInvoicePaymentMethodPush  withholdtax"
				+ withholdtax);

		try {

			sInsertqueryforHeader = "INSERT INTO IR_HEADER_DETAIL(IRUNIQUENAME, COMPANYCODE, SUPPLIER, "
					+

					"SUPPPAYMENT, BLOCKSTAMPDATE, "
					+

					"TOTALCOST, "
					+

					"CURRENCY, "
					+

					"CONVERSIONDATE, "
					+

					"ORDERNUMBER, "
					+

					"INVOICEUNIQUENAME, SUPPLIERINVOICENUMBER, "
					+

					"REFTOOTHERINV, TAXAMOUNT, "
					+

					"TAXCURRENCY, INVEFORMNUMBER, "
					+

					"PUSHTOSTAGTIMESTAMP, "
					+

					"PUSHTOSAP, SAPSOURCE, TIMESTAMP,INVOICEPURPOSE,INVOICEDATE,DOCTYPE,WITHHOLDTAX,PARTITION,CURREXCHANGERATE,VATREGISTRATION, "
					+

					"ASNINVOICE) "
					+

					"VALUES ( '" + iruniquename + "', '" +

					companycode + "', '" +

					supplier + "', '" +

					supppayment + "', '" +

					blockstampdate + "', " +

					totalcostroundedamount + ", '" +

					hdrcurrency + "', '" +

					conversiondate + "', '" +

					ordernumber + "', '" +

					invoiceuniquename + "', '" +

					supplierinvoicenumber + "', '" +

					reftootherinv + "', " +

					taxamount + ", '" +

					taxcurrency + "', '" +

					inveformnumber + "', '" +

					timestamp + "', '" +

					pushtosap + "', '" +

					sapsource + "', '" +

					timestamp + "', '" +

					invoicepurpose + "','" +

					invoiceDate + "','" +

					doctype + "','" +

					withholdtax + "','" +

					currpartition + "','" +

					currExchangeRate + "','" +

					vatReg + "','" +

					loadedFrom + "')";

			Log.customer.debug(sInsertqueryforHeader);
			stmt.executeUpdate(sInsertqueryforHeader);
			setDWFlag(iruniquename);

		}

		catch (Exception ex)

		{

			// stmt.close();

			// db2Conn.rollback();

			/*
			 * try
			 *  {
			 *
			 * String deleteQuery = "DELETE FROM IR_LINEITEM_ACCNTG AS A WHERE
			 * A.IRUNIQUENAME LIKE '"+iruniquename+"'";
			 *
			 * stmt.executeUpdate(deleteQuery);
			 *
			 *
			 *  }
			 *
			 * catch(Exception ex1)
			 *  {
			 *  }
			 */
			String exceptionstr = ex.getMessage();

			Log.customer.debug("exception in header" + exceptionstr);
		}
		Log.customer.debug("completed inserting header item details");

		// Check Sum of all line item amount 2 decimal place] is equals to Total
		// Amount at header level of Invoice

		// conversiondate = (Date)
		// ir.getDottedFieldValue("TotalCost.ConversionDate");

		// }

	}
// Method for WH Tax logic
	public void checkforWHTax(InvoiceReconciliation ir)
	{
		// Null Check for ir
		if(ir != null)
		{
		// InMethod/Local Variables Declaration START
		String taxfromGLlookup = null;
		String gltext = null;
		//Start: Mach1 R5.5 (FRD11.2/TD11.2)
		String compCode = null;
		String country = null;
		//End: Mach1 R5.5 (FRD11.2/TD11.2)
		//boolean WHflag = false;
		boolean accCategoryFfound = false;
		BaseVector irlines = ir.getLineItems();
			String WHtaxstring = "";
			// Null Check for irlines
			if(irlines != null)
			{
		ArrayList tempirlines = new ArrayList();

		Currency defaultCurrency = (Currency)ir.getDottedFieldValue("CompanyCode.DefaultCurrency");
		String stracccategory;
		int tempPrintCount = irlines.size();
				boolean flagGLFoundNoMatch = false;
				boolean flagGLFoundMatch = false;
		// InMethod/Local Variables Declaration END
		// Validating the IRLineItemList for Account Category Non - 'F' and filtering out Non-Service Line Items for Sorting
		Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax :  Validation and Filteration Started");
			for (int countFilterList = 0; countFilterList < tempPrintCount; countFilterList++)
			{
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)irlines.get(countFilterList);
					if(irli != null && irli.getDottedFieldValue("AccountCategory") != null)
					{
				stracccategory = (String)irli.getDottedFieldValue("AccountCategory.UniqueName");
						if(!StringUtil.nullOrEmptyOrBlankString(stracccategory) && stracccategory.equals("F"))
				{
					Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax : Skipping Line Item with Account Category 'F'"+ countFilterList+ ":" +stracccategory);
					accCategoryFfound = true;
					countFilterList = tempPrintCount;
					break;
				}
				// if(!(irli.getFieldValue("LineItemType").equals("Service Only (TQS)")))
				// {
					// Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax : Removing Line Item - Non-Service: Line Number:" +countFilterList +"; CurrentList Size:" +irlines.size()+"; Original List Size:" +tempPrintCount +"; Line Item Value Deleted = "+ irli.getFieldValue("LineItemType"));
					// irlines.remove(countFilterList);
				// }
				if((irli.getFieldValue("LineItemType") !=null && irli.getFieldValue("LineItemType").equals("Service Only (TQS)")))
				{
					Log.customer.debug(" sapinvoicepaymentmethodpush checkforWHTax : Considering line item - Service: line number:" +countFilterList +"; original list size:" +tempPrintCount);
					tempirlines.add(irli);
				}
					}
			}
			Log.customer.debug(" sapinvoicepaymentmethodpush checkforWHTax : Considering line item - Service: Service LineItem numbers:" +tempirlines.size() +"; original IR list size:" +tempPrintCount);
				if(tempirlines != null && tempirlines.size() > 0)
				{
		Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax :  Validation and Filteration Ended: No of Filtered LineItems -"+( tempPrintCount - tempirlines.size()));
				}
				else
				{
					Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax :  Validation and Filteration Ended: No of Filtered LineItems = 0");
				}
		if(!accCategoryFfound && tempirlines !=null && tempirlines.size() > 0)
		{
					Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax :  Validation and Filteration Ended: No of Filtered LineItems -"+( tempPrintCount - tempirlines.size()));
			Log.customer.debug(" SAPInvoicePaymentMethodPush :  sortedList Account Category F not found, contains Service Line Items; Condition Satisfied");
			List sortedList = sortIRLineItems(tempirlines, defaultCurrency);
			Log.customer.debug(" SAPInvoicePaymentMethodPush :  sortedList" + sortedList);
					if(sortedList != null)
					{
			int size = sortedList.size();
			Log.customer.debug(" SAPInvoicePaymentMethodPush :  size" + size);

						if(ir.getFieldValue("SupplierLocation") != null)
						{
							WHtaxstring = (String)ir.getDottedFieldValue("SupplierLocation.ShopURL");
							//String shopURLField = (String)ir.getDottedFieldValue("SupplierLocation.ShopURL");
							Log.customer.debug(" SAPInvoicePaymentMethodPush :  ShopURL = " + WHtaxstring);
							if(!StringUtil.nullOrEmptyOrBlankString(WHtaxstring))
							{
								Log.customer.debug(" SAPInvoicePaymentMethodPush :  ShopURL is not nullOrEmptyOrBlankString:");
				Log.customer.debug(" SAPInvoicePaymentMethodPush : SupplierLocation.ShopURL " + WHtaxstring);
				String acccategory;
				//String WHtaxstring = "01#01#Y!05#05#N!04#04#Y";
				String delims = "!";
				String[] WHtaxset = WHtaxstring.split(delims);
				ArrayList taxCodeHolderFromSupplier = new ArrayList();
				Log.customer.debug(" SAPInvoicePaymentMethodPush : SupplierLocation.ShopURL each set " + WHtaxset);
				for (int i = 0; i < WHtaxset.length; i++)
				{
					System.out.println(WHtaxset[i]);
					String delims1 = "\\*";
					String[] WHTaxValues = WHtaxset[i].split(delims1);
					Log.customer.debug(" SAPInvoicePaymentMethodPush : SupplierLocation.ShopURL each value in a set" + WHTaxValues);
					for (int j = 0; j < WHTaxValues.length; j++)
					{
						Log.customer.debug(" SAPInvoicePaymentMethodPush : SupplierLocation.ShopURL each value in a set" + WHTaxValues[j]);
						//System.out.println(WHTaxValues[j]);
						if (WHTaxValues[j].equals("X"))
						{
							Log.customer.debug(" SAPInvoicePaymentMethodPush : SupplierLocation.ShopURL each value in a set" + WHTaxValues[j]);
							//String taxfromSupplier = WHTaxValues[j-1];
							taxCodeHolderFromSupplier.add(WHTaxValues[j-1]);
							Log.customer.debug(" SAPInvoicePaymentMethodPush : taxfromSupplier" + WHTaxValues[j-1]);
						}
					}
				}
				/////////////////////////////////////////
				//Start: Mach1 R5.5 (FRD11.2/TD11.2)
				compCode = (String)ir.getDottedFieldValue("CompanyCode.UniqueName");
				country = (String)ir.getDottedFieldValue("CompanyCode.RegisteredAddress.Country.UniqueName");
				//End: Mach1 R5.5 (FRD11.2/TD11.2)
							for (int k=0;k<size;k++)
							{
								InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)sortedList.get(k);
								Log.customer.debug(" SAPInvoicePaymentMethodPush :  InvoiceReconciliationLineItem irli" + irli);
								acccategory = (String)irli.getDottedFieldValue("AccountCategory.UniqueName");
								Log.customer.debug(" SAPInvoicePaymentMethodPush :  acccategory in irli" + acccategory);

								SplitAccountingCollection  sac = (SplitAccountingCollection)irli.getDottedFieldValue("Accountings");
								List splitAccountings = (List)sac.getDottedFieldValue("SplitAccountings");
								Log.customer.debug(" SAPInvoicePaymentMethodPush : splitAccountings " + splitAccountings);
								if(splitAccountings!=null)
								{
									for(int m=0;m<splitAccountings.size();m++)
									{
										SplitAccounting sa = (SplitAccounting)splitAccountings.get(m);
										Log.customer.debug(" SAPInvoicePaymentMethodPush : SplitAccounting sa " + sa);
										gltext = (String)sa.getFieldValue("GeneralLedgerText");
										Log.customer.debug(" SAPInvoicePaymentMethodPush : GLValues" + gltext);
									}
									//Start: Mach1 R5.5 (FRD11.2/TD11.2)
									//String qryString = "Select WithHoldingTaxCode from cat.core.GLToTaxLookup where GLUniqueName  = '"+ gltext +"'";
									String qryString = "Select WithHoldingTaxCode from cat.core.GLToTaxLookup where CompanyCode.UniqueName = '"+ compCode +"' and Country.UniqueName = '"+ country +"' and GLUniqueName  = '"+ gltext +"'";
									//End: Mach1 R5.5 (FRD11.2/TD11.2)
									Log.customer.debug(" SAPInvoicePaymentMethodPush : qryString "+qryString);

									AQLOptions queryOptions = new AQLOptions(ir.getPartition());
									AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
										if (queryResults != null)
										{
									while(queryResults.next())
									{
										String taxfromGLLookup = queryResults.getString(0);
										Log.customer.debug(" SAPInvoicePaymentMethodPush : taxfromGLLookup" + taxfromGLLookup);
											if(!StringUtil.nullOrEmptyOrBlankString(taxfromGLLookup))
											{
							//if (taxfromGLLookup.equals(taxfromSupplier))
							if(taxCodeHolderFromSupplier.contains(taxfromGLLookup))
							{
													flagGLFoundMatch = true;
											 Log.customer.debug(" SAPInvoicePaymentMethodPush : taxfromGLLookup" + taxfromGLLookup);

											  Object[] taxCodelookupKeys = new Object[2];
											  taxCodelookupKeys[0] = taxfromGLLookup;
											  Log.customer.debug(" SAPInvoicePaymentMethodPush : GLAccount from the lookup "+taxCodelookupKeys[0]);
											  ClusterRoot comCode = (ClusterRoot)ir.getFieldValue("CompanyCode");
											  Log.customer.debug(" SAPInvoicePaymentMethodPush : Companycode to get country "+comCode);
											  taxCodelookupKeys[1] =  comCode.getDottedFieldValue("RegisteredAddress.PostalAddress.Country");
											  Log.customer.debug(" SAPInvoicePaymentMethodPush : Country "+taxCodelookupKeys[1]);
											  ClusterRoot WHTax = (ClusterRoot) Base.getSession().objectFromLookupKeys(taxCodelookupKeys, "ariba.tax.core.TaxCode", ir.getPartition());
											  Log.customer.debug(" SAPInvoicePaymentMethodPush : WHTax from Lookup "+WHTax);
											  ir.setFieldValue("WithHoldTaxCode", WHTax);
											  ir.setFieldValue("ProjectTitle","Y");
								  // WHflag = true;
												}
												else
												{
													flagGLFoundNoMatch = true;
									}
								}
										}
									}
								if(ir.getFieldValue("WithHoldTaxCode") != null)
								break;
								 if((ir.getFieldValue("WithHoldTaxCode") == null) && ((k+1) == size))
								 {
										if(flagGLFoundNoMatch && !flagGLFoundMatch)
										{
											Log.customer.debug(" SAPInvoicePaymentMethodPush : GL Found in the Look up. But no valid Tax Code");
									ir.setFieldValue("ProjectTitle","N");
								 }
							}
								}
							}
				////////////////////////////////////////////////
			}
		}
		else
		{
			Log.customer.debug(" SAPInvoicePaymentMethodPush : None of the lines are valid as contains Account Category 'F'");
					}
				}
				else
				{
					Log.customer.debug(" SAPInvoicePaymentMethodPush checkforWHTax :  Validation and Filteration Ended: No of Filtered LineItems = 0");
				}
			}
		}
	}
}
public List sortIRLineItems(ArrayList irlines, Currency defaultCurrency)
{
	Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : Sorting of Filtered Invoice Reconciliation List without Account 'F' and Non-Service Invoked");
	//Currency defaultCurrency = (Currency)ir.getDottedFieldValue("CompanyCode.DefaultCurrency");

	Object[] irliArray = irlines.toArray();

	int s = irlines.size();
	Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : length of the list s:"+s);

	for (int i = 0; i < s; i++)
	{
		for (int j = 0; j < s - i - 1; j++)
		{
			BaseObject irlvl = (BaseObject) irliArray[j];

			Money irlvlAmt = (Money) irlvl.getFieldValue("Amount");
			Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : first line amount irlvlAmt:"+irlvlAmt);
			Money irlvlAmtInBase = irlvlAmt.convertToCurrency(defaultCurrency);
			Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : first line amount in base currency:"+irlvlAmtInBase);

			BaseObject irlvl2 = (BaseObject) irliArray[j + 1];
			Money irlvl2Amt = (Money) irlvl2.getFieldValue("Amount");
			Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : next line amount irlvl2Amt:"+irlvl2Amt);
			Money irlvl2AmtInBase = irlvl2Amt.convertToCurrency(defaultCurrency);
			Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : next line amount in base currency irlvl2AmtInBase:"+irlvl2AmtInBase);

			if (irlvlAmtInBase.compareTo(irlvl2AmtInBase) < 0)
			{
			Object temp = irliArray[j];

			irliArray[j] = irliArray[j + 1];
			irliArray[j + 1] = temp;
			}
		}
	}
	for(int countPrint = 0; countPrint < s; countPrint++)
	{
		BaseObject irlvlPrint = (BaseObject) irliArray[countPrint];
		Money irlvlAmtPrint = (Money) irlvlPrint.getFieldValue("Amount");
		Log.customer.debug(" SAPInvoicePaymentMethodPush sortIRLineItems : Highest Dollar Amount Sorting: " + countPrint + ": " + irlvlAmtPrint);
	}
        return Arrays.asList(irliArray);
    }
	//code ended for WH Tax Logic
}
