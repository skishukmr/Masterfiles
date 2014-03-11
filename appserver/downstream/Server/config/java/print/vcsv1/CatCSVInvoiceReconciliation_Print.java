/* Created by KS on Apr 17, 2006 * ------------------------------------------------------------------------------- * Required to call special print methods (Add Charge printing & Price precision changes) * 02.19.08 (pgs kannan) - Added logic to print the invoice creator issue #776
 */package config.java.print.vcsv1;
import java.io.PrintWriter;import java.util.List;import java.util.Locale;import ariba.approvable.core.Approvable;import ariba.approvable.core.ApprovableClassProperties;import ariba.approvable.core.LineItemCollection;import ariba.base.core.Base;import ariba.base.core.BaseObject;import ariba.base.core.ClusterRoot;import ariba.invoicing.core.Invoice;import ariba.invoicing.core.InvoiceException;import ariba.invoicing.core.InvoiceReconciliation;import ariba.statement.core.print.StatementReconciliation_Print;import ariba.util.core.Fmt;import ariba.util.core.HTML;import ariba.util.core.ListUtil;import ariba.util.core.MIME;import ariba.util.core.ResourceService;import ariba.util.core.StringUtil;import ariba.util.log.Log;import ariba.util.net.HTMLPrintWriter;

public class CatCSVInvoiceReconciliation_Print extends StatementReconciliation_Print {
    private static final String THISCLASS = "CatCSVInvoiceReconciliation_Print";
    public void printHTML(BaseObject bo, PrintWriter out, boolean referenceImages,            Locale locale) {
        Log.customer.debug("%s *** 2. printHTML w/out Locale",THISCLASS);        String group = getUserFieldPrintGroup((Approvable)bo, true);        printHTML(bo, out, group, referenceImages, null);    }
    protected void printHTML(BaseObject bo, PrintWriter out, String userFieldPrintGroup,            boolean referenceImages, Locale locale) {
        Log.customer.debug("%s *** 2. printHTML with Locale",THISCLASS);        if(locale == null)            locale = Base.getSession().getLocale();        ApprovableClassProperties cp = (ApprovableClassProperties)((Approvable)bo).getClassProperties();
 // 	*** Removed OOB condition since results in loop (calls PrintHook again) //     String printHook = cp.getPrintHook(); //     if(!StringUtil.nullOrEmptyString(printHook)) //		     runPrintHook((Approvable)bo,out,userFields,out,locale);        LineItemCollection lic = (LineItemCollection)bo;        HTMLPrintWriter hout = new HTMLPrintWriter(out);        hout.HTMLOpen();        hout.headOpen();        hout.meta("text/html", MIME.getMetaCharset(locale));        String title = getTitle(lic);        if(!StringUtil.nullOrEmptyString(title))            hout.title(HTML.fullyEscape(title));        else            hout.title("");        hout.headClose();        hout.bodyOpen();        String fonts = ResourceService.getString("ariba.server.ormsserver", "FontFace", locale);        Fmt.F(out, "<FONT FACE=\"%s\" SIZE=1>\r\n", fonts);        printHeaderInfo(lic, out, referenceImages, locale);
        // issue #776 Adding Invoice Eform requester in print
        Invoice inv = (Invoice)lic.getDottedFieldValue("Invoice");
        ClusterRoot invEform = null;

        if (inv != null)
            invEform = (ClusterRoot)inv.getFieldValue("InvoiceEform");

        // issue #776 Loaded from Invoice Eform

        if  (invEform != null) {		   Log.customer.debug("%s getting Invoice Eform's requester Name",THISCLASS);		   String name = (String)invEform.getDottedFieldValue("Requester.Name.PrimaryString");
		   Log.customer.debug("%s getting Invoice Eform's requester Name @@@@ "+ name,THISCLASS);

           out.print("Invoice created on " + inv.getCreateDate() + " by " + name );	    }
	    // issue #776 Loaded from Contract
	    String loadedfromstr =  inv.getFieldValue("LoadedFrom").toString();
		Log.customer.debug("%s loadedfromstr "+ loadedfromstr ,THISCLASS);
	    if ( (invEform == null) && (loadedfromstr.equals("4")) ) {


			   Log.customer.debug("%s getting Invoice Eform's requester Name",THISCLASS);
			   String name = (String)inv.getDottedFieldValue("Requester.Name.PrimaryString");
			   Log.customer.debug("%s getting Invoice Eform's requester Name @@@@ "+ name,THISCLASS);

		       out.print("Invoice created on " + inv.getCreateDate() + " by " + name );
	    }
       // issue #776 Loaded from ASN
	    if ( (invEform == null) && !(loadedfromstr.equals("4")) ) {
            Log.customer.debug("%s seetting Invoice from ASN",THISCLASS);	        out.print("Invoice created on " + inv.getCreateDate() + " by ASN ") ;
	    }
        printHTMLLineItems(lic, out, locale);        if(shouldPrintChangeRecords())            printHTMLChangeRecords(lic, out, locale);        if(shouldPrintApprovalFlow())        {            out.print("<P>\r\n");            printHTMLApprovalFlow(lic, out, locale);            out.print("</P>\r\n");        }        if(shouldPrintComments())            printHTMLCommentable(lic, out, locale);        out.print("</FONT>");        hout.bodyClose();        hout.HTMLClose();        hout.flush();    }
    private final void printHeaderExceptions(InvoiceReconciliation ir, PrintWriter out, Locale locale, boolean printHTML)    {        List exceptions = ir.getHeaderExceptionsForCurrentUser();        if(ListUtil.nullOrEmptyList(exceptions) || hiddenField(ir, "Exceptions", "InvoiceHeaderDetailsPrint"))            return;        String label = ResourceService.getService().getLocalizedString("resource.invoicing", "HeaderExceptions", locale);        if(printHTML)        {            MIME.crlf(out, "<P><FONT SIZE=2><B>%s</B></FONT><BR>", label);            MIME.crlf(out, "<TABLE BORDER=1 WIDTH=\"95%\">");            printHTMLExceptions(exceptions, 3, out, locale);            MIME.crlf(out, "</TABLE></P>");        } else        {            /**                S. Sato - AUL                Commented this section as this logic will never be reached. This is a                private method and the only place where this method is called is in                printHTMLLineItems(...) where this argument is hardcoded to 'true'            */            /*            Fmt.F(out, "%s\n", label);            printTable(ir.getPartition(), out, "ExceptionTable", exceptions, locale, InvoiceException.KeyDescriptionForBuyerDisplay);            Fmt.F(out, "\n\n");            */        }    }
    protected void printHTMLLineItems(LineItemCollection lic, PrintWriter out, Locale locale) {		//from super method        InvoiceReconciliation ir = (InvoiceReconciliation)lic;        printHeaderInformation(ir, out, locale, true);        printHeaderExceptions(ir, out, locale, true);
        Log.customer.debug("%s *** 3. printHTMLLineItems",THISCLASS);        if (!lic.getLineItems().isEmpty()) {            CatCSVInvoiceReconciliationLineItem_Print catIRlip = new CatCSVInvoiceReconciliationLineItem_Print();            catIRlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);        }    }    public CatCSVInvoiceReconciliation_Print() {        super();    }
}
