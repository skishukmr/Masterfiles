/* Created by KS on Apr 17, 2006
 * -------------------------------------------------------------------------------
 * Required to call special print methods (Add Charge printing & Price precision changes)
 */
package config.java.print.vcsv1;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.invoicing.core.Invoice;
import ariba.statement.core.print.Statement_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;


public class CatCSVInvoice_Print extends Statement_Print {

    private static final String THISCLASS = "CatCSVInvoice_Print";

    public void printHTML(BaseObject bo, PrintWriter out, boolean referenceImages,
            Locale locale) {

        Log.customer.debug("%s *** 2. printHTML w/out Locale",THISCLASS);
        String group = getUserFieldPrintGroup((Approvable)bo, true);
        printHTML(bo, out, group, referenceImages, null);
    }

    protected void printHTML(BaseObject bo, PrintWriter out, String userFieldPrintGroup,
            boolean referenceImages, Locale locale) {

        Log.customer.debug("%s *** 2. printHTML with Locale",THISCLASS);
        if(locale == null)
            locale = Base.getSession().getLocale();
        ApprovableClassProperties cp = (ApprovableClassProperties)((Approvable)bo).getClassProperties();
 // 	*** Removed OOB condition since results in loop (calls PrintHook again)
 //     String printHook = cp.getPrintHook();
 //     if(!StringUtil.nullOrEmptyString(printHook))
 //		     runPrintHook((Approvable)bo,out,userFields,out,locale);
        LineItemCollection lic = (LineItemCollection)bo;
        HTMLPrintWriter hout = new HTMLPrintWriter(out);
        hout.HTMLOpen();
        hout.headOpen();
        hout.meta("text/html", MIME.getMetaCharset(locale));
        String title = getTitle(lic);
        if(!StringUtil.nullOrEmptyString(title))
            hout.title(HTML.fullyEscape(title));
        else
            hout.title("");
        hout.headClose();
        hout.bodyOpen();
        String fonts = ResourceService.getString("ariba.server.ormsserver", "FontFace", locale);
        Fmt.F(out, "<FONT FACE=\"%s\" SIZE=1>\r\n", fonts);
        printHeaderInfo(lic, out, referenceImages, locale);
        printHTMLLineItems(lic, out, locale);
        if(shouldPrintChangeRecords())
            printHTMLChangeRecords(lic, out, locale);
        if(shouldPrintApprovalFlow())
        {
            out.print("<P>\r\n");
            printHTMLApprovalFlow(lic, out, locale);
            out.print("</P>\r\n");
        }
        if(shouldPrintComments())
            printHTMLCommentable(lic, out, locale);
        out.print("</FONT>");
        hout.bodyClose();
        hout.HTMLClose();
        hout.flush();
    }


    protected void printHTMLLineItems(LineItemCollection lic, PrintWriter out, Locale locale) {
		//overrides super method
        Invoice invoice = (Invoice)lic;
        printHeaderInformation(invoice, out, locale, true);
        Log.customer.debug("%s *** 3. printHTMLLineItems",THISCLASS);
        if (!lic.getLineItems().isEmpty()) {
            CatCSVInvoiceLineItem_Print catIlip = new CatCSVInvoiceLineItem_Print();
            catIlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);
        }
    }

    public CatCSVInvoice_Print() {
        super();
    }

}
