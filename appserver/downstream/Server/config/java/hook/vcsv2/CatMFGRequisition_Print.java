/*
 * Created by KS on Jul 18, 2005
 * -------------------------------------------------------------------------------
 * Changes required to remove Caterpillar Inc. company name from Requisition print
 */

package config.java.hook.vcsv2;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.purchasing.core.print.Requisition_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;
import config.java.common.CatConstants;
import config.java.ordering.CatProcureLineItem_Print;

public class CatMFGRequisition_Print extends Requisition_Print {

    private static final String THISCLASS = "CatRequisition_Print";


    public void printHTML(BaseObject bo, PrintWriter out, boolean referenceImages,
            Locale locale) {

        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** 2. printHTML w/out Locale",THISCLASS);
        String group = getUserFieldPrintGroup((Approvable)bo, true);
        printHTML(bo, out, group, referenceImages, null);
    }

 /*   public void printHTML(BaseObject bo, PrintWriter out, boolean referenceImages) {

        super.printHTML(bo, out, referenceImages);
    }
*/
    protected void printHTML(BaseObject bo, PrintWriter out, String userFieldPrintGroup,
            boolean referenceImages, Locale locale) {

        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** 2. printHTML with Locale)",THISCLASS);
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

        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** 3. printHTMLLineItems",THISCLASS);
        LineItem li = (LineItem)lic.getLineItems().get(0);
        if (li != null) {
            CatProcureLineItem_Print catPlip = new CatProcureLineItem_Print();
//          Log.customer.debug("%s *** catPlip: %s",THISCLASS,catPlip);
            catPlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);
        }
    }

    public CatMFGRequisition_Print() {
        super();
        Log.customer.debug("%s *** Finished calling Requisition_Print super!", THISCLASS);
    }

}
