/******************************************************************************
	Change Author:	Dharmang Shelat
	Date Created:	10/19/2006
	Description:	Handles additional precision for Price fields
					Other changes inherited from R1/R2/R4 that apply for US
					(e.g., BillingAddress)
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.print.vcsv3;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.print.Approvable_Print;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.fields.Print;
import ariba.basic.core.Money;
import ariba.basic.core.MoneyFormatter;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.print.ProcureLineItem_Print;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
public class CatEZOProcureLineItem_Print extends ProcureLineItem_Print {

	private static final String ClassName = "CatEZOProcureLineItem_Print";
	private final int PRECISION = 5;
	private static String emergencyText = ResourceService.getString("cat.java.common", "PO_EmergencyBuyText");

	public void printHTMLLineItems(
		PrintWriter out,
		LineItemCollection approvable,
		List lineItems,
		Money total,
		Locale locale)
	{
		//if (Log.customer.debugOn){
			Log.customer.debug("%s ::: printHTMLLineItems!", ClassName);
			Log.customer.debug("%s ::: Using super.printHTMLineItems()!", ClassName);
		//}
		super.printHTMLLineItems(out, approvable, lineItems, total, locale);
	}

	// Overriden to ensure BillTo prints even if Accounting is hidden on PO
	public void printShippingAndAccountingInfo(
		ProcureLineItem lineItem,
		Approvable approvable,
		PrintWriter out,
		Locale locale,
		boolean printShippingInfo,
		boolean printAccountingInfo,
		boolean splitAccountingInfo,
		boolean onHeader)
	{
		Log.customer.debug("%s ::: printShippingAndAccountingInfo!", ClassName);
		Log.customer.debug("%s ::: printShippingInfo/printAccountingInfo/splitAccountingInfo/onHeader: " + printShippingInfo + "/" + printAccountingInfo + "/" + splitAccountingInfo + "/" + onHeader, ClassName);
		ProcureLineItem pLineItem = lineItem;
		printNewHTMLRow(out);
		if (printShippingInfo)
			printShipTo(lineItem, approvable, out, groupE(onHeader, true), locale);
		if (!hiddenField(lineItem, "BillingAddress", onHeader, true)) {
			//if (Log.customer.debugOn){
				Log.customer.debug("%s ::: !hiddenField(BillingAddress) = TRUE",ClassName);
				Log.customer.debug("CatEZOProcureLineItem_Print ::: printAccountingInfo = " + printAccountingInfo);
			//}
			printBillTo(lineItem, approvable, out, groupE(onHeader, true), locale);
		}
		printEndHTMLRow(out);
		printNewHTMLRow(out);
		if (printShippingInfo && !hiddenField(lineItem, "DeliverTo", onHeader, true))
			printDeliverTo(lineItem, approvable, out, locale);
		if (printAccountingInfo && lineItem.getAccountings() != null && !splitAccountingInfo){
			Log.customer.debug("%s ::: Printing the Accounting", ClassName);
			printAccounting(lineItem, approvable, out, locale);
		}
		if (printAccountingInfo && lineItem.getAccountings() != null && splitAccountingInfo) {
			printEndHTMLRow(out);
			printNewHTMLRow(out);
			Log.customer.debug("%s ::: Printing the Split Accounting", ClassName);
			printSplitAccounting(lineItem, approvable, out, locale);
		}
		printEndHTMLRow(out);
	}

	// Overridden to add Additional Info section label (for consistency with ASN orders)
	protected void printHTMLApprovableUserFields(
		LineItemCollection approvable,
		PrintWriter out,
		String group,
		Locale locale)
	{
		Log.customer.debug("%s ::: printHTMLApprovableUserFields!", ClassName);
		String sectionHdr = "Additional Information";
		int partitionNum = approvable.getPartitionNumber();
		Log.customer.debug("CatEZOProcureLineItem_Print ::: Approvable: " + approvable);
		Log.customer.debug("CatEZOProcureLineItem_Print ::: Partition Num: " + partitionNum);
		MIME.crlf(out, "<P>");
		MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
		printNewHTMLRow(out);
		MIME.crlf(out, "<TD> <font size=1>");
		MIME.crlf(out, "<B>%s</B><BR>", sectionHdr);
		if (partitionNum == 4 && approvable instanceof PurchaseOrder) { // US partition only
			//if (Log.customer.debugOn)
				Log.customer.debug("CatEZOProcureLineItem_Print ::: emergencyText: " + emergencyText);
			Boolean emergency = (Boolean) approvable.getFieldValue("EmergencyBuy");
			if (emergency != null && emergency.booleanValue()) {
				if (emergencyText == null)
					emergencyText = "**EMERGENCY BUY**";
				MIME.crlf(out, "<B>%s</B><BR>", emergencyText);
			}
		}
		Approvable_Print printer = (Approvable_Print) Print.get(approvable);
		printer.printHTMLUserFields(approvable, out, printer.getUserFieldPrintGroup(approvable, true), locale);
		MIME.crlf(out, "</Font></TD>");
		printEndHTMLRow(out);
		MIME.crlf(out, "</TABLE>");
		MIME.crlf(out, "</P>");
	}

	// Had to create new method since Ariba's OOB is static final
	private static String groupE(boolean onHeader, boolean html) {
		if (!html) {
			if (onHeader)
				return "LineItemPrintTextSummary";
			else
				return "LineItemPrintTextDetails";
		}
		else {
			return "LineItemPrint";
		}
	}

	// To print the Price without rounding off and overide the currency presicion
	protected void printTextLineDetailWithoutLineNumber(
		ProcureLineItem lineItem,
		PrintWriter out,
		String uomStr,
		Locale locale)
	{
		Log.customer.debug("%s ::: printTextLineDetailWithoutLineNumber!", ClassName);

		Fmt.F(
			out,
			"%s",
			Fmt.Sil(
				locale,
				"resource.ordering",
				"QtyUOMAmount",
				BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
				uomStr,
				MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
				lineItem.getAmount().asString()));
		Fmt.F(
			out,
			"%s",
			Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
	}

	// To print the Price without rounding off and overide the currency presicion
	protected void printTextLineDetailWithLineNumber(
		ProcureLineItem lineItem,
		PrintWriter out,
		String uomStr,
		Locale locale)
	{
		Log.customer.debug("%s ::: printTextLineDetailWithLineNumber!", ClassName);

		Fmt.F(
			out,
			"%s",
			Fmt.Sil(
				locale,
				"resource.ordering",
				"ItemQtyUOMAmount",
				Integer.toString(lineItem.getNumberInCollection()),
				BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
				uomStr,
				MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
				lineItem.getAmount().asString()));
		Fmt.F(
			out,
			"%s",
			Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
	}

	// To print the Price without rounding off and overide the currency presicion
	protected void printHTMLRow(ProcureLineItem pLineItem, Approvable approvable, PrintWriter out, Locale locale) {
		printNewCol(out);
		MIME.crlf(out, "%s", HTML.fullyEscape(Integer.toString(pLineItem.getNumberInCollection())));
		printEndCol(out);
		if (approvable.hasPreviousVersion()) {
			printNewCol(out);
			MIME.crlf(out, "%s", HTML.fullyEscape(pLineItem.getChangedStateLocalized(locale)));
			printEndCol(out);
		}
		MIME.crlf(out, "<TD WIDTH=\"40%%\"> <font size=1><b>");
		MIME.crlf(out, "%s", HTML.fullyEscape(insertNewLineBreaks(pLineItem.getDescription().getShortName())));
		MIME.crlf(out, "</b>");
		printEndCol(out);
		printNewCol(out);
		MIME.crlf(out, "%s", HTML.fullyEscape(pLineItem.getDescription().getSupplierPartNumber()));
		printEndCol(out);
		printNewCol(out);
		UnitOfMeasure unitOfMeasure = pLineItem.getDescription().getUnitOfMeasure();
		MIME.crlf(out, "%s", HTML.fullyEscape(unitOfMeasure != null ? unitOfMeasure.getName().getString(locale) : ""));
		printEndCol(out);
		printNewCol(out);
		MIME.crlf(out, "%s", HTML.fullyEscape(BigDecimalFormatter.getStringValue(pLineItem.getQuantity(), locale)));
		printEndCol(out);
		printNewCol(out);
		ariba.util.core.Date needBy = pLineItem.getNeedBy();
		if (needBy != null) {
			String needByStr =
				DateFormatter.getStringValue(
					pLineItem.getNeedBy(),
					ResourceService.getService().getLocalizedString("resource.date", "DayDateMonthYearFmt", locale),
					locale,
					Base.getSession().getTimezone());
			MIME.crlf(out, HTML.fullyEscape(needByStr));
		}
		else {
			MIME.crlf(out, ResourceService.getService().getLocalizedString("resource.ordering", "NoValue", locale));
		}
		printEndCol(out);
		Log.customer.debug("%s ::: printHTMLRow changing price and amount!", ClassName);

		MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

		MIME.crlf(
			out,
			"%s",
			HTML.escape(
				MoneyFormatter.getStringValue(pLineItem.getDescription().getPrice(), locale, null, true, PRECISION)));
		printEndCol(out);
		MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

		MIME.crlf(out, "%s", HTML.escape(pLineItem.getAmount().asString()));
		printEndCol(out);
	}

	// To print the BillTo Name on the PO print
	public void printCompanyName(Partition partition, Locale locale, Address address, PrintWriter out)
	{
		//String companyName = Procure.getCompanyName(partition, locale);
		//if(!StringUtil.nullOrEmptyString(companyName))
		//	MIME.crlf(out, "%s", Procure.getCompanyName(partition, locale));
		//else
		if(address != null)
			MIME.crlf(out, "%s", address.getName());
	}

	public CatEZOProcureLineItem_Print() {
		super();
	}
}