package config.java.hook.vcsv1;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.PrintApprovableHook;
import ariba.approvable.core.print.Approvable_Print;
import ariba.base.core.Base;
import ariba.base.core.BaseObject_Print;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.FieldPropertiesSource;
import ariba.base.fields.Fields;
import ariba.base.fields.ValueSource;
import ariba.base.fields.ValueSourceUtil;
import ariba.common.core.Core;
import ariba.common.core.User;
import ariba.procure.core.LineItemProductDescription;
import ariba.receiving.core.MilestoneTracker;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptCoreApprovable;
import ariba.receiving.core.ReceiptItem;
import ariba.receiving.core.ReceivableLineItem;
import ariba.user.core.Permission;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.ListUtil;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.Formatter;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;

public class CatReceiptPrintTagHook extends Approvable_Print
    implements PrintApprovableHook
{

    public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail)
    {
        Log.customer.debug("%s *** ReceiptPrintHook RUN!", "CatReceiptPrintTagHook");
        if(!(approvable instanceof Receipt))
            return ListUtil.list(Constants.getInteger(1), "Error: not a Receipt - not printing!");
        ReceiptCoreApprovable receipt = (ReceiptCoreApprovable)approvable;
        Log.customer.debug("%s *** The recipt id =", "CatReceiptPrintTagHook", receipt.getUniqueName());
        Log.customer.debug("%s *** ReceiptPrintHook-printForEmail==" + printForEmail, "CatReceiptPrintTagHook");
        if(printForEmail)
        {
            printHTML(receipt, out, locale);
            return ListUtil.list(Constants.getInteger(0));
        }
        User user = Core.getService().getEffectiveUser();
        Log.customer.debug("**%s**User printing this receipt==" + user, "CatReceiptPrintTagHook");
        Permission permission = Permission.getPermission("CatReceiving");
        boolean isCatReceiving = false;
        if(user != null)
            isCatReceiving = user.getUser().hasPermission(permission);
        Log.customer.debug("%s *** The user logged has role =" + isCatReceiving, "CatReceiptPrintTagHook");
        if(!isCatReceiving)
            printHTML(receipt, out, locale);
        else
            printTag(receipt, out, locale);
        return ListUtil.list(Constants.getInteger(0));
    }

    public void printTag(ReceiptCoreApprovable receipt, PrintWriter out, Locale locale)
    {
        boolean genTagSelected = false;
        HTMLPrintWriter hout = new HTMLPrintWriter(out);
        hout.HTMLOpen();
        hout.headOpen();
        hout.meta("text/html", MIME.getMetaCharset(locale));
        String title = getTitle(receipt);
        if(!StringUtil.nullOrEmptyString(title))
            hout.title(HTML.fullyEscape(title));
        else
            hout.title("");
        hout.headClose();
        hout.bodyOpen();
        String fonts = ResourceService.getString("ariba.server.ormsserver", "FontFace", locale);
        Fmt.F(out, "<FONT FACE=\"%s\" SIZE=3>\r\n", fonts);
        for(Iterator i = receipt.getReceiptItemsIterator(); i.hasNext();)
        {
            ReceiptItem receiptItem = (ReceiptItem)i.next();
            int receiptNumber = receiptItem.getNumberInCollection();
            Log.customer.debug("%s *** The recipt item number  =" + receiptNumber, "CatReceiptPrintTagHook");
            boolean genTag = false;
            Object moveTagVal = receiptItem.getFieldValue("MoveTag");
            if(moveTagVal != null)
                genTag = ((Boolean)moveTagVal).booleanValue();
            Log.customer.debug("%s *** ReceiptItem #" + receiptNumber + " has MoveTag marked =" + genTag, "CatReceiptPrintTagHook");
            if(genTag)
            {
                if(locale == null)
                    locale = Base.getSession().getLocale();
                genTagSelected = true;
                MIME.crlf(out, "<P><Font Size=3><B>MOVE TAG</B></Font></P>");
                MIME.crlf(out, "<TABLE BORDER=1 CELLPADDING=\"0\" CELLSPACING=\"2\" WIDTH=\"50%\">");
                ReceivableLineItem lineItem = (ReceivableLineItem)receiptItem.getFieldValue("LineItem");
                LineItemProductDescription poLineDescription = lineItem.getDescription();
                String supplierPartNumber = poLineDescription.getSupplierPartNumber();
                Log.customer.debug("%s *** Supplier partNum#=", "CatReceiptPrintTagHook", supplierPartNumber);
                String deliverTo = (String)lineItem.getDottedFieldValue("DeliverTo");
                Log.customer.debug("%s *** DeliverTo address=", "CatReceiptPrintTagHook", deliverTo);
                String purchaseOrderNumber = (String)lineItem.getDottedFieldValue("LineItemCollection.UniqueName");
                Log.customer.debug("%s *** purchaseOrder #=", "CatReceiptPrintTagHook", purchaseOrderNumber);
                printTagFieldValuePair(out, "Receipt", Formatter.getStringValue(getTitle(receipt), locale));
                printTagFieldValuePair(out, "Receiving Date", Formatter.getStringValue(receiptItem.getDate(), locale));
                printTagFieldValuePair(out, "Supplier Part Number", Formatter.getStringValue(supplierPartNumber, locale));
                printTagFieldValuePair(out, "Requester's Name", Formatter.getStringValue(receipt.getRequester().getMyName(locale), locale));
                printTagFieldValuePair(out, "Purchase Order Number", Formatter.getStringValue(purchaseOrderNumber, locale));
                printTagFieldValuePair(out, "Traffic Number", Formatter.getStringValue(receipt.getDottedFieldValue("TrafficEntry.UniqueName"), locale));
                printTagFieldValuePair(out, "Deliver To", Formatter.getStringValue(deliverTo, locale));
                MIME.crlf(out, "</TABLE>");
                MIME.crlf(out, "<P>&nbsp;</P>");
            }
        }

        if(!genTagSelected)
            printEmptyTag(out, locale);
        out.print("</FONT>");
        hout.bodyClose();
        hout.HTMLClose();
        hout.flush();
    }

    public void printEmptyTag(PrintWriter out, Locale locale)
    {
        MIME.crlf(out, "<P><Font Size=3><B>MOVE TAG</B></Font></P>");
        MIME.crlf(out, "<TABLE BORDER=1 CELLPADDING=\"0\" CELLSPACING=\"2\" WIDTH=\"50%\">");
        printTagFieldValuePair(out, "Receipt", " ");
        printTagFieldValuePair(out, "Receiving Date", " ");
        printTagFieldValuePair(out, "Supplier Part Number", " ");
        printTagFieldValuePair(out, "Requester's Name", " ");
        printTagFieldValuePair(out, "Purchase Order Number", " ");
        printTagFieldValuePair(out, "Traffic Number", " ");
        printTagFieldValuePair(out, "Deliver To", " ");
        MIME.crlf(out, "</TABLE>");
        MIME.crlf(out, "<P>&nbsp;</P>");
    }

    protected static void printTagFieldValuePair(PrintWriter out, String fieldName, String value)
    {
        BaseObject_Print.printNewHTMLRow(out);
        MIME.crlf(out, "<TD WIDTH=\"40%\" ALIGN=\"LEFT\" nowrap> <Font Size=3> <B>");
        MIME.crlf(out, HTML.fullyEscape(fieldName));
        MIME.crlf(out, "</B></Font></TD>");
        MIME.crlf(out, "<TD WIDTH=\"60%\" ALIGN=\"LEFT\" nowrap> <Font Size=3>&nbsp;");
        MIME.crlf(out, HTML.fullyEscape(value));
        MIME.crlf(out, "</Font></TD>");
        BaseObject_Print.printEndHTMLRow(out);
    }

    public void printHTML(ReceiptCoreApprovable receipt, PrintWriter out, Locale locale)
    {
        if(locale == null)
            locale = Base.getSession().getLocale();
        HTMLPrintWriter hout = new HTMLPrintWriter(out);
        hout.HTMLOpen();
        hout.headOpen();
        hout.meta("text/html", MIME.getMetaCharset(locale));
        String title = getTitle(receipt);
        if(!StringUtil.nullOrEmptyString(title))
            hout.title(HTML.fullyEscape(title));
        else
            hout.title("");
        hout.headClose();
        hout.bodyOpen();
        String fonts = ResourceService.getString("ariba.server.ormsserver", "FontFace", locale);
        Fmt.F(out, "<FONT FACE=\"%s\" SIZE=1>\r\n", fonts);
        printHeaderInfo(receipt, out, false, locale);
        printReceiptHeaderInfo(receipt, out, locale);
        printLineItems(receipt, out, locale);
        if(shouldPrintApprovalFlow())
        {
            out.print("<P>\r\n");
            printHTMLApprovalFlow(receipt, out, locale);
            out.print("</P>\r\n");
        }
        printHTMLCommentable(receipt, out, locale);
        out.print("</FONT>");
        hout.bodyClose();
        hout.HTMLClose();
        hout.flush();
    }

    public void printReceiptHeaderInfo(ReceiptCoreApprovable rec, PrintWriter out, Locale locale)
    {
        String group;
        if(rec instanceof MilestoneTracker)
            group = "ReceiptHeaderMilestonePrintGroup";
        else
            group = "ReceiptHeaderPrintGroup";
        FieldPropertiesSource fps = Fields.getService().getFpl(rec.getClassName(), Base.getSession().getVariant());
        List fields = getVisibleFieldsInGroup(rec, group, fps);
        MIME.crlf(out, "<TABLE BORDER=0 CELLPADDING=\"0\" CELLSPACING=\"2\" WIDTH=\"100%\">");
        for(int i = 0; i < fields.size(); i++)
        {
            String fieldName = (String)fields.get(i);
            BaseObject_Print.printNewHTMLRow(out);
            MIME.crlf(out, "<TD ALIGN=\"LEFT\"> <Font Size=1>");
            FieldProperties fp = fps.getFieldProperties(fieldName, group);
            printFieldValuePair(out, fp.getLabel(fieldName), Formatter.getStringValue(rec.getDottedFieldValue(fieldName), locale));
            MIME.crlf(out, "</TD>");
            BaseObject_Print.printEndHTMLRow(out);
        }

        MIME.crlf(out, "</TABLE>");
    }

    public void printLineItems(ReceiptCoreApprovable rec, PrintWriter out, Locale locale)
    {
        List items = rec.getReceiptItems();
        if(ListUtil.nullOrEmptyList(items))
            return;
        if(rec instanceof MilestoneTracker)
        {
            printItems(out, items, "MilestoneTrackerPrintGroup", locale);
        } else
        {
            List itemsByCount = ListUtil.list();
            List itemsByAmount = ListUtil.list();
            for(int i = 0; i < items.size(); i++)
            {
                ReceiptItem item = (ReceiptItem)items.get(i);
                if(item.getReceivingType() == 3)
                    itemsByAmount.add(item);
                else
                    itemsByCount.add(item);
            }

            printItems(out, itemsByAmount, "ReceiptItemAmountPrintGroup", locale);
            printItems(out, itemsByCount, "ReceiptItemPrintGroup", locale);
        }
    }

    private final void printItems(PrintWriter out, List items, String recPrintGroup, Locale locale)
    {
        if(ListUtil.nullOrEmptyList(items))
            return;
        FieldPropertiesSource fps = Fields.getService().getFpl("ariba.receiving.core.ReceiptItem", Base.getSession().getVariant());
        ValueSource obj = (ValueSource)ListUtil.firstElement(items);
        List fields = getVisibleFieldsInGroup(obj, recPrintGroup, fps);
        MIME.crlf(out, "<P><BR>");
        MIME.crlf(out, "<TABLE BORDER=\"1\" cellpadding=\"4\" cellspacing=\"0\" rules=\"groups\" frame=\"hsides\" width=\"95%\" bordercolorlight=\"#ffffff\" bordercolordark=\"#000000\">");
        BaseObject_Print.printNewHTMLRow(out);
        for(int j = 0; j < fields.size(); j++)
        {
            String fieldName = (String)fields.get(j);
            MIME.crlf(out, "<TD nowrap><font size=1><B>");
            FieldProperties fp = fps.getFieldProperties(fieldName, recPrintGroup);
            MIME.crlf(out, "%s", fp.getLabel(fieldName));
            MIME.crlf(out, "</B></Font></TD>");
        }

        BaseObject_Print.printEndHTMLRow(out);
        for(int i = 0; i < items.size(); i++)
        {
            obj = (ValueSource)items.get(i);
            printTBody(out);
            BaseObject_Print.printNewHTMLRow(out);
            int numFields = fields.size();
            for(int j = 0; j < numFields; j++)
            {
                String fieldName = (String)fields.get(j);
                Object value = obj.getDottedFieldValue(fieldName);
                printNewCol(out);
                MIME.crlf(out, "%s", Formatter.getStringValue(value, locale));
                printEndCol(out);
            }

            BaseObject_Print.printEndHTMLRow(out);
        }

        MIME.crlf(out, "</table>");
    }

    public List getVisibleFieldsInGroup(ValueSource item, String group, FieldPropertiesSource fps)
    {
        List allFields = FieldProperties.getFieldsInGroup(group, fps);
        List visibleFields = ListUtil.list();
        for(int i = 0; i < allFields.size(); i++)
        {
            boolean visible = ValueSourceUtil.evaluateConstraints(item, (String)allFields.get(i), "__visibility", group, true, null, null);
            if(visible)
                visibleFields.add(allFields.get(i));
        }

        return visibleFields;
    }

    protected static void printFieldValuePair(PrintWriter out, String fieldName, String value)
    {
        MIME.crlf(out, "<TD WIDTH=\"5%%\" nowrap> <Font Size=1> <B>");
        MIME.crlf(out, "%s", HTML.fullyEscape(fieldName));
        MIME.crlf(out, ":");
        MIME.crlf(out, "</B></Font></TD>");
        MIME.crlf(out, "<TD WIDTH=\"95%%\" nowrap> <Font Size=1>");
        MIME.crlf(out, "%s", HTML.fullyEscape(value));
        MIME.crlf(out, "</Font></TD>");
    }

    public void printNewCol(PrintWriter out)
    {
        MIME.crlf(out, "<TD> <FONT SIZE=\"1\">");
    }

    public void printNewCol(PrintWriter out, int colspan)
    {
        MIME.crlf(out, "<TD COLSPAN=\"%s\"><FONT SIZE=\"1\">", Constants.getInteger(colspan));
    }

    public void printEndCol(PrintWriter out)
    {
        MIME.crlf(out, "</FONT></TD>");
    }

    public void printTBody(PrintWriter out)
    {
        MIME.crlf(out, "<TBODY>");
    }

    public CatReceiptPrintTagHook()
    {
    }

    private static final String classname = "CatReceiptPrintTagHook";
    private static final String receivingRolePermission = "CatReceiving";
}
