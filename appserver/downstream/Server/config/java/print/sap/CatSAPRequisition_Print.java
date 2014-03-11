/* Created by KS on Apr 17, 2006
 * -------------------------------------------------------------------------------
 * Required to call special print methods (Add Charge printing & Price precision changes)
 */
package config.java.print.sap;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.print.Requisition_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;



public class CatSAPRequisition_Print extends Requisition_Print {

    private static final String THISCLASS = "CatSAPRequisition_Print";

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
        Log.customer.debug( "before printing the apprval graph");
        if(shouldPrintChangeRecords())
            printHTMLChangeRecords(lic, out, locale);
        if(shouldPrintApprovalFlow())
        {
            out.print("<P>\r\n");
            printHTMLApprovalFlow(lic, out, locale);
            out.print("</P>\r\n");
        }
        Log.customer.debug( "end of the apprval graph");
        CatSAPProcureLineItem_Print catPlip = new CatSAPProcureLineItem_Print();
        ProcureLineItem pLineItem = (ProcureLineItem)lic.getLineItems().get(0);
       if(catPlip.checkForDisplay(pLineItem ,"DisplayTermsConditions"))
	   		{
	   			Log.customer.debug(" enter DisplayTermsConditions method");
	   			out.print("<P ALIGN=LEFT>\r\n");
	   			String resString = catPlip.checkForLocalOrImport(pLineItem ,"DisplayTermsConditions");
	   			Log.customer.debug(" enter DisplayTermsConditions method resString"+resString);
	   			if ( resString != null)
	   			out.print(Fmt.Sil(locale,"resource.ordering",resString));
	   			else
	   			out.print(Fmt.Sil(locale,"resource.ordering","TermsAndConditionsText"));
	   			out.print("</P>\r\n");
	   		}
		Log.customer.debug( "aftter DisplayTermsConditions method");
        if(shouldPrintComments())
            printHTMLCommentable(lic, out, locale);
        out.print("</FONT>");
        hout.bodyClose();
        hout.HTMLClose();
        hout.flush();
    }

    protected void printHTMLLineItems(LineItemCollection lic, PrintWriter out, Locale locale) {

        Log.customer.debug("%s *** 3. printHTMLLineItems",THISCLASS);
        if (!lic.getLineItems().isEmpty()) {
            CatSAPProcureLineItem_Print catPlip = new CatSAPProcureLineItem_Print();
            catPlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);
        }
    }
    public CatSAPRequisition_Print() {
        super();
    }

}
