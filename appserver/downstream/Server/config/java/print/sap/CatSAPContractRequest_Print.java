/*
 * 17-01-2014  Parita Shah  SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4) - New File created
 * -------------------------------------------------------------------------------------------------------------------
 *
 */



package config.java.print.sap;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.contract.core.print.ContractCoreApprovable_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;
import config.java.common.CatConstants;
import config.java.contract.CatProcureLineItemMA_Print;

public class CatSAPContractRequest_Print extends ContractCoreApprovable_Print {

    private static final String THISCLASS = "CatSAPContractRequest_Print";

    protected void printHTML(BaseObject bo, PrintWriter out, String userFieldPrintGroup,
            boolean referenceImages, Locale locale) {

        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** 2. printHTML with Locale",THISCLASS);
        if(locale == null)
            locale = Base.getSession().getLocale();
        ApprovableClassProperties cp = (ApprovableClassProperties)((Approvable)bo).getClassProperties();

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
        //ProcureLineItem pli = (ProcureLineItem)lic.getLineItems().get(0);
        LineItem li = (LineItem)lic.getLineItems().get(0);
        Approvable approvable = (Approvable)lic;
        if (li != null) {
            CatProcureLineItemMA_Print catPlip = new CatProcureLineItemMA_Print();
            catPlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);
		}

         MIME.crlf(out, "<P ALIGN=LEFT><font size=2>");
		 printHTMLUserFields(lic, out, "MAPermissionsPrint", locale);
		 MIME.crlf(out, "</font></P>");
		 MIME.crlf(out, "<P ALIGN=LEFT><font size=2>");
		 printHTMLUserFields(lic, out, "MAAttachmentsPrint", locale);
		 MIME.crlf(out, "</font></P>");
    }

    public CatSAPContractRequest_Print() {
        super();
        Log.customer.debug("%s *** Finished calling CatCSVMARequest_Print super!", THISCLASS);
    }

}
