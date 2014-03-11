/* Created by KS/Chandra on April 19, 2006 for unique US format requirements
 * -------------------------------------------------------------------------
 * Ensures Req with Add Charges prints AC lines after reference Material line
 * Handles additional precision for Price fields
 * Other changes inherited from R1/R2 that apply for US (e.g., BillingAddress)
 * Removed R2 changes not applicable for US (e.g., remove Caterpillar, Inc. in addresses)
 *
 *	05.17.06 	KS 		Added special printing for Add. Charge Ref Line Num (PO only)
 *
 * 	01.16.08	Amit 	Removed printHTMLLineItems() as it was affecting the PR Print if PR had additional charges as line item
 * 15/01/2014     IBM Parita Shah	SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8) MSC Tax Gaps Correct Legal Entity
 */

package config.java.print.vcsv1;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.print.Approvable_Print;
import ariba.approvable.core.print.LineItemCollection_Print;
import ariba.base.core.Base;
import ariba.base.core.BaseObject_Print;
import ariba.base.fields.Print;
import ariba.basic.core.Money;
import ariba.basic.core.MoneyFormatter;
import ariba.basic.core.UnitOfMeasure;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.print.ProcureLineItem_Print;
// Start SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
import ariba.common.core.Address;
import ariba.common.core.print.Address_Print;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.StringUtil;
// End SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.ListUtil;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;




public class CatCSVProcureLineItem_Print extends ProcureLineItem_Print {

    private static final String THISCLASS = "CatCSVProcureLineItem_Print";
    private final int PRECISION = 5;
    private static String emergencyText = ResourceService.getString("cat.java.common","PO_EmergencyBuyText");

   /* public void printHTMLLineItems(PrintWriter out, LineItemCollection approvable, List lineItems,
            Money total, Locale locale)
    {
        boolean hasCharges = false;
        Log.customer.debug("%s *** STEP 6 - printHTMLLineItems!",THISCLASS);
        if (!lineItems.isEmpty() && approvable.getPartitionNumber() == 2) { // US partition only uses add. charges
            int size = lineItems.size();
            if (approvable instanceof Requisition) {
	            for (int i=0; i<size; i++) {
	                ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);
	                Integer refNum = (Integer)pli.getFieldValue("ReferenceLineNumber");
	                if (refNum == null) { // indicates errors on Req
	                    Log.customer.debug("%s *** refNum is null, not resequencing!",THISCLASS);
	                    break;
	                }
	                else if (refNum.intValue() != pli.getNumberInCollection()) {
	                    Log.customer.debug("CatCSVProcureLineItem_Print *** Found Req Add Charge, Line# " + pli.getNumberInCollection());
	                    hasCharges = true;
	                    break;
	                }
	            }
            }
       	   else if (approvable instanceof PurchaseOrder) {
	            for (int i=0; i<size; i++) {
	                ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);
	                if (CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {
	                    Log.customer.debug("CatCSVProcureLineItem_Print *** Found PO Add Charge, Line# " + pli.getNumberInCollection());
	                    hasCharges = true;
	                    break;
	                }
	            }
            }
        }
        if (hasCharges) {
            Log.customer.debug("%s *** Using printHTMLineItemsWithAddCharges()!",THISCLASS);
            List reorderedLines = CatTaxCollector.reorderLineItems(lineItems, false);
            Log.customer.debug(" Got the Ordered LineItems from CatTaxCollector ");
            printHTMLLineItemsWithAddCharges(out, approvable, reorderedLines, total, locale);
            Log.customer.debug(" Completed printHTMLLineItemsWithAddCharges() ");
        }
        else {
            Log.customer.debug("%s *** Using super.printHTMLineItems()!",THISCLASS);
            super.printHTMLLineItems(out, approvable, lineItems, total, locale);
        }
    }
*/
    // Overriden to ensure BillTo prints even if Accounting is hidden on PO
    public void printShippingAndAccountingInfo(ProcureLineItem lineItem, Approvable approvable,
            PrintWriter out, Locale locale, boolean printShippingInfo, boolean printAccountingInfo,
            boolean splitAccountingInfo, boolean onHeader)
    {
        Log.customer.debug("%s *** STEP 7 - printShippingAndAccountingInfo!",THISCLASS);
        // Start :  Q1 2014 - RSD111 - FDD 3.0
        Log.customer.debug("CatCSVProcureLineItem_Print *** printShippingInfo = " + printShippingInfo);
        Log.customer.debug("CatCSVProcureLineItem_Print *** printAccountingInfo = " + printAccountingInfo);
        Log.customer.debug("CatCSVProcureLineItem_Print *** splitAccountingInfo = " + splitAccountingInfo);
        Log.customer.debug("CatCSVProcureLineItem_Print *** onHeader = " + onHeader);
        // End :  Q1 2014 - RSD111 - FDD 3.0

        ProcureLineItem pLineItem = lineItem;
        printNewHTMLRow(out);
        if(printShippingInfo)
            printShipTo(lineItem, approvable, out, groupE(onHeader, true), locale);
        if(!hiddenField(lineItem, "BillingAddress", onHeader, true)) {
        //Log.customer.debug("%s *** !hiddenField(BillingAddress) = TRUE",THISCLASS);
        //Log.customer.debug("CatCSVProcureLineItem_Print *** printAccountingInfo = " + printAccountingInfo);
            printBillTo(lineItem, approvable, out, groupE(onHeader, true), locale);
        }
        printEndHTMLRow(out);
        printNewHTMLRow(out);
        if(printShippingInfo && !hiddenField(lineItem, "DeliverTo", onHeader, true))
            printDeliverTo(lineItem, approvable, out, locale);
        if(printAccountingInfo && lineItem.getAccountings() != null && !splitAccountingInfo)
            printAccounting(lineItem, approvable, out, locale);
        if(printAccountingInfo && lineItem.getAccountings() != null && splitAccountingInfo)
        {
            printEndHTMLRow(out);
            printNewHTMLRow(out);
            printSplitAccounting(lineItem, approvable, out, locale);
        }
        printEndHTMLRow(out);
    }



	    // Start SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
	    public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
	    {
	        Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);
	        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
	        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));

			ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
			if(plic != null)
			{
				String accFac = (String)plic.getDottedFieldValue("AccountingFacilityName");
				Log.customer.debug("CatCSVProcureLineItem_Print AccountingFacilityName", accFac);
				if (!StringUtil.nullOrEmptyOrBlankString(accFac))
				{

					MIME.crlf(out, "%s", accFac);
					MIME.crlf(out, "<BR>");
				}

			}

	        Address shipTo = pli.getShipTo();
	        //printAddressHeaderForPartition(approvable,shipTo,out,locale);
	        if(shipTo != null)
	        {   //MIME.crlf(out, "%s", shipTo.getName());
	        	//MIME.crlf(out, "<BR>");
	            ((Address_Print)Print.get(shipTo)).printHTMLAddressInLocale(shipTo, out, pli, "ShipTo", group, locale);
	        }
	        MIME.crlf(out, "</Font></TD>");
	    }
	// End SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)




    //  Overridden to add Additional Info section label (for consistency with ASN orders)
    protected void printHTMLApprovableUserFields(LineItemCollection approvable,
            PrintWriter out, String group, Locale locale)
    {

        Log.customer.debug("%s *** STEP 9 - printHTMLApprovableUserFields!",THISCLASS);
        String sectionHdr = "Additional Information";
        int partitionNum = approvable.getPartitionNumber();
        Log.customer.debug("CatCSVProcureLineItem_Print *** Approvable: " + approvable);
        Log.customer.debug("CatCSVProcureLineItem_Print *** Partition Num: " + partitionNum);
        MIME.crlf(out, "<P>");
        MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
        printNewHTMLRow(out);
        MIME.crlf(out, "<TD> <font size=1>");
        MIME.crlf(out, "<B>%s</B><BR>",sectionHdr);
        if (partitionNum == 2 && approvable instanceof PurchaseOrder) { // US partition only
  //          Log.customer.debug("CatCSVProcureLineItem_Print *** emergencyText: " + emergencyText);
            Boolean emergency = (Boolean)approvable.getFieldValue("EmergencyBuy");
            if (emergency != null && emergency.booleanValue()) {
	            if (emergencyText == null)
	                emergencyText = "**EMERGENCY BUY**";
	            MIME.crlf(out, "<B>%s</B><BR>",emergencyText);
            }
        }
        Approvable_Print printer = (Approvable_Print)Print.get(approvable);
        printer.printHTMLUserFields(approvable, out, printer.getUserFieldPrintGroup(approvable, true), locale);
        MIME.crlf(out, "</Font></TD>");
        printEndHTMLRow(out);
        MIME.crlf(out, "</TABLE>");
        MIME.crlf(out, "</P>");
    }

    // Had to create new method since Ariba's OOB is static final
    private static String groupE(boolean onHeader, boolean html)
    {
        if(!html)
        {
            if(onHeader)
                return "LineItemPrintTextSummary";
            else
                return "LineItemPrintTextDetails";
        } else
        {
            return "LineItemPrint";
        }
    }

//  04.17.06 (ks) - added to ensure req prints with AC lines following Material line
    public void printHTMLLineItemsWithAddCharges(PrintWriter out, LineItemCollection approvable, List lineItems, Money total, Locale locale)
    {
        boolean printSupplierLocationOnTop = printSupplierLocationOnTop(approvable, lineItems);
        boolean printShippingInfoOnTop = printShippingInfoOnTop(approvable, lineItems);
        boolean printAccountingInfoOnTop = printAccountingInfoOnTop(approvable, lineItems, "LineItemPrint");
        boolean splitAccountingInfo = splitAccountingInfo(approvable, lineItems, "LineItemPrint", printAccountingInfoOnTop);
        List commonFields = ListUtil.list();
        List uniqueFields = ListUtil.list();
        ProcureLineItem firstLineItem;
        Log.customer.debug(" To Print AC lines following Material line ");
        if(!lineItems.isEmpty())
            firstLineItem = (ProcureLineItem)ListUtil.firstElement(lineItems);
        else
            firstLineItem = null;
        if(firstLineItem != null)
        {
            getCommonFields(approvable, firstLineItem.getTypeName(), commonFields, uniqueFields, locale);
            if(printSupplierLocationOnTop)
            {
                MIME.crlf(out, "<P>");
                MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
                printSupplierLocation(firstLineItem, approvable, out, locale, true);
                MIME.crlf(out, "</TABLE>");
            }
            boolean printShipping = printShippingInfoOnTop && !hiddenField(firstLineItem, "ShipTo", true, true);
            boolean printAccounting = printAccountingInfoOnTop && !hiddenField(firstLineItem, "Accountings", true, true);
            if(printShipping || printAccounting)
            {
                MIME.crlf(out, "<P>");
                MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
                printShippingAndAccountingInfo(firstLineItem, approvable, out, locale, printShipping, printAccounting, splitAccountingInfo, true);
                MIME.crlf(out, "</TABLE>");
            }
            MIME.crlf(out, "</P>");
        }
        printHTMLApprovableUserFields(approvable, out, ((LineItemCollection_Print)Print.get(approvable)).getUserFieldPrintGroup(approvable, true), locale);
        if(!commonFields.isEmpty())
        {
            MIME.crlf(out, "<P>");
            MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
            printNewHTMLRow(out);
            MIME.crlf(out, "<TD> <font size=1>");
            ((BaseObject_Print)Print.get(firstLineItem)).printHTMLUserFields(firstLineItem, out, "LineItemPrint", commonFields.iterator(), locale);
            MIME.crlf(out, "</Font></TD>");
            printEndHTMLRow(out);
            MIME.crlf(out, "</TABLE>");
            MIME.crlf(out, "</P>");
        }
        MIME.crlf(out, "<P>");
        MIME.crlf(out, "<TABLE BORDER=1 cellpadding=\"4\" cellspacing=\"0\" rules=\"groups\" frame=\"hsides\" width=\"100%\" bordercolorlight=\"#ffffff\" bordercolordark=\"#000000\">");
        int lineItemsSize = 0;
        int numLineItemsPrinted = 0;

        if (!lineItems.isEmpty())
        {
            lineItemsSize = lineItems.size();
            for(int i = 0; i < lineItemsSize; i++)
            {
                ProcureLineItem pLineItem = (ProcureLineItem)lineItems.get(i);
	            if(pLineItem == null)
	            {
	                Log.fixme.warning(1236, approvable);
	            } else
	            {
	                printHTML(pLineItem, approvable, out, locale);
	                printRelatedInfo(pLineItem, approvable, out, locale);
	                if(!printSupplierLocationOnTop)
	                    printSupplierLocation(pLineItem, approvable, out, locale, false);
	                if(!printShippingInfoOnTop || !printAccountingInfoOnTop)
	                {
	                    boolean printShipping = !printShippingInfoOnTop && !hiddenField(pLineItem, "ShipTo", false, true);
	                    boolean printAccounting = !printAccountingInfoOnTop && !hiddenField(pLineItem, "Accountings", false, true);
	                    printShippingAndAccountingInfo(pLineItem, approvable, out, locale, printShipping, printAccounting, splitAccountingInfo, false);
	                }
	                printNewHTMLRow(out);
	                MIME.crlf(out, "<TD COLSPAN=%s> <font size=1>", Constants.getInteger(getLineItemColspan(approvable)));
	                printHTMLUniqueFields(pLineItem, out, "LineItemPrint", uniqueFields.iterator(), locale);
	                MIME.crlf(out, "</Font></TD>");
	                printEndHTMLRow(out);
	                MIME.crlf(out, "<TR>");
	                MIME.crlf(out, "<TD COLSPAN=%s>&nbsp;</TD>", Constants.getInteger(getLineItemColspan(approvable)));
	                printEndHTMLRow(out);
	                numLineItemsPrinted++;
	            }
            }
        }
        if(numLineItemsPrinted < lineItemsSize)
            Log.fixme.warning(4611, lineItems);
        printHTMLLineTotal(approvable, out, locale, total);
        MIME.crlf(out, "</TABLE>");
        MIME.crlf(out, "</P>");
    }

	/**** 03.20.06 (Chandra)
	* To print the Price without rounding off and overide the currency presicion
	*******/
    protected void printTextLineDetailWithoutLineNumber(ProcureLineItem lineItem, PrintWriter out, String uomStr, Locale locale)
    {
		Log.customer.debug("%s *** printTextLineDetailWithoutLineNumber!",THISCLASS);

        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "QtyUOMAmount",
        								BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
        								uomStr,
        								MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
        								lineItem.getAmount().asString()));
        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
    }

	/* To print the Price without rounding off and overide the currency presicion */
    protected void printTextLineDetailWithLineNumber(ProcureLineItem lineItem, PrintWriter out, String uomStr, Locale locale)
    {
		Log.customer.debug("%s *** printTextLineDetailWithLineNumber!",THISCLASS);

        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "ItemQtyUOMAmount", Integer.toString(lineItem.getNumberInCollection()),
        									BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
        									uomStr,
        									MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
        									lineItem.getAmount().asString()));
        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
    }

	/* To print the Price without rounding off and overide the currency presicion */
    protected void printHTMLRow(ProcureLineItem pLineItem, Approvable approvable, PrintWriter out, Locale locale)
    {
        printNewCol(out);
        MIME.crlf(out, "%s", HTML.fullyEscape(Integer.toString(pLineItem.getNumberInCollection())));
        printEndCol(out);
        if(approvable.hasPreviousVersion())
        {
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
        if(needBy != null)
        {
            String needByStr = DateFormatter.getStringValue(pLineItem.getNeedBy(), ResourceService.getService().getLocalizedString("resource.date", "DayDateMonthYearFmt", locale), locale, Base.getSession().getTimezone());
            MIME.crlf(out, HTML.fullyEscape(needByStr));
        } else
        {
            MIME.crlf(out, ResourceService.getService().getLocalizedString("resource.ordering", "NoValue", locale));
        }
        printEndCol(out);
		Log.customer.debug("%s *** printHTMLRow changing price and amount!",THISCLASS);

        MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

        MIME.crlf(out, "%s", HTML.escape(MoneyFormatter.getStringValue(pLineItem.getDescription().getPrice(), locale, null, true, PRECISION)));
        printEndCol(out);
        MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

        MIME.crlf(out, "%s", HTML.escape(pLineItem.getAmount().asString()));
        printEndCol(out);
    }

    // 05.16.06 (KS) override method to control Additional Charge Ref. Num printing on PO (never print at header)
    public void getCommonFields(LineItemCollection lic, String className, List commonFields, List uniqueFields, Locale locale)
    {
        super.getCommonFields(lic,className,commonFields,uniqueFields,locale);
        if (lic instanceof PurchaseOrder && !commonFields.isEmpty()) {
	        int size = commonFields.size();
	        Log.customer.debug("CatCSVProcureLineItem_Print *** size: " + size);
            while (size > 0) {
                String fieldName = (String)commonFields.get(--size);
                Log.customer.debug("%s *** fieldName: %s",THISCLASS,fieldName);
                // this will ensure ReferenceLineNumber never gets printed at header (on PO)
                if (fieldName.equals("ReferenceLineNumber"))  {
                    Log.customer.debug("%s *** found RefLineNum : remove from commonFields, add to uniqueFields",THISCLASS);
                    commonFields.remove(fieldName);
                    uniqueFields.add(fieldName);
                    break;
                }
            }
        }
    }

    // 05.16.06 (KS) override method to control Add. Charge Ref. Num printing on PO (only print if not a material line)
    protected void printHTMLUniqueFields(ProcureLineItem pLineItem, PrintWriter out, String group, Iterator fields, Locale locale)
    {
  //      Log.customer.debug("%s *** procureLineItem: %s",THISCLASS,pLineItem);
        if (fields != null && pLineItem instanceof POLineItem && !CatAdditionalChargeLineItem.isAdditionalCharge(pLineItem)) {
            Log.customer.debug("%s *** Found Material POLineItem, creating new Iterator!",THISCLASS);
            List newFields = new ArrayList();
            while(fields.hasNext()) {
                String fieldName = (String)fields.next();
                Log.customer.debug("%s *** fieldName: %s",THISCLASS,fieldName);
                if (!fieldName.equals("ReferenceLineNumber"))  {
                    newFields.add(fieldName);
                }
                else {
                    Log.customer.debug("%s *** found RefLineNum : remove from UniqueFields",THISCLASS);
                }
            }
            ((BaseObject_Print)Print.get(pLineItem)).printHTMLUserFields(pLineItem, out, group, newFields.iterator(), locale);
        }
        else {
   //         Log.customer.debug("%s *** Not Material POLineItem, using OOB fields iterator!",THISCLASS);
            ((BaseObject_Print)Print.get(pLineItem)).printHTMLUserFields(pLineItem, out, group, fields, locale);
        }

    }


    public CatCSVProcureLineItem_Print() {
        super();
    }

}
