/*
 * Created for R2 by KS on Jul 7, 2005
 * Updated for R4 by KS on Oct 8, 2005
 * -------------------------------------------------------------------------------
 * 1) always print BillTo on HTML PO; 2) exclude Company Name if Perkins (mfg1);
 * 3) print Emergency Buy text for US (pcsv1)
 */
package config.java.ordering;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.print.Approvable_Print;
import ariba.base.fields.Print;
import ariba.basic.core.Money;
import ariba.common.core.Address;
import ariba.common.core.print.Address_Print;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.print.ProcureLineItem_Print;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatProcureLineItem_Print extends ProcureLineItem_Print {

    private static final String THISCLASS = "CatProcureLineItem_Print";
    private static String emergencyText = ResourceService.getString("cat.java.common","PO_EmergencyBuyText");

    public void printHTMLLineItems(PrintWriter out, LineItemCollection approvable, List lineItems,
            Money total, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 6 - printHTMLLineItems!",THISCLASS);
        super.printHTMLLineItems(out, approvable, lineItems, total, locale);
    }

    // Overriden to ensure BillTo prints even if Accounting is hidden on PO
    public void printShippingAndAccountingInfo(ProcureLineItem lineItem, Approvable approvable,
            PrintWriter out, Locale locale, boolean printShippingInfo, boolean printAccountingInfo,
            boolean splitAccountingInfo, boolean onHeader)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 7 - printShippingAndAccountingInfo!",THISCLASS);
        ProcureLineItem pLineItem = lineItem;
        printNewHTMLRow(out);
        if(printShippingInfo)
            printShipTo(lineItem, approvable, out, groupE(onHeader, true), locale);
        if(!hiddenField(lineItem, "BillingAddress", onHeader, true)) {
 //         Log.customer.debug("%s *** !hiddenField(BillingAddress) = TRUE",THISCLASS);
 //         Log.customer.debug("CatProcureLineItem_Print *** printAccountingInfo = " + printAccountingInfo);
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

    // Overridden to exclude Company Name (Caterpillar, Inc.) for UK orders
    public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);
        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));
        Address shipTo = pli.getShipTo();
        printAddressHeaderForPartition(approvable,shipTo,out,locale);
        if(shipTo != null)
        {
            ((Address_Print)Print.get(shipTo)).printHTMLAddressInLocale(shipTo, out, pli, "ShipTo", group, locale);
        }
        MIME.crlf(out, "</Font></TD>");
    }

    // Overridden to exclude Company Name (Caterpillar, Inc.) for UK orders
    public void printBillTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 8 - printBillTo!",THISCLASS);
        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "BillTo", locale));
        Address billTo = pli.getBillingAddress();
        printAddressHeaderForPartition(approvable,billTo,out,locale);
        if(billTo != null)
            ((Address_Print)Print.get(billTo)).printHTMLAddressInLocale(billTo, out, pli, "BillingAddress", group, locale);
        MIME.crlf(out, "</Font></TD>");
    }

    //  Overridden to add Additional Info section label (for consistency with ASN orders)
    protected void printHTMLApprovableUserFields(LineItemCollection approvable,
            PrintWriter out, String group, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 9 - printHTMLApprovableUserFields!",THISCLASS);
        String sectionHdr = "Additional Information";
        int partitionNum = approvable.getPartitionNumber();
        if (CatConstants.DEBUG) {
            Log.customer.debug("CatProcureLineItem_Print *** Approvable: " + approvable);
            Log.customer.debug("CatProcureLineItem_Print *** Partition Num: " + partitionNum);
        }
        MIME.crlf(out, "<P>");
        MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
        printNewHTMLRow(out);
        MIME.crlf(out, "<TD> <font size=1>");
        MIME.crlf(out, "<B>%s</B><BR>",sectionHdr);
        if (partitionNum == 2 && approvable instanceof PurchaseOrder) { // US partition only
            Log.customer.debug("CatProcureLineItem_Print *** emergencyText: " + emergencyText);
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

    public void printAddressHeaderForPartition(Approvable approvable, Address address, PrintWriter out, Locale locale)
    {
        if (approvable.getPartition().getName().equals("mfg1") && address != null)
            MIME.crlf(out, "%s", address.getName());
        else
            printCompanyName(approvable.getPartition(), locale, address, out);
        MIME.crlf(out, "<BR>");
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

    public CatProcureLineItem_Print() {
        super();
    }

}
