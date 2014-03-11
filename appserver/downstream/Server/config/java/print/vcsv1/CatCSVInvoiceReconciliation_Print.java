/* Created by KS on Apr 17, 2006
 */
import java.io.PrintWriter;

public class CatCSVInvoiceReconciliation_Print extends StatementReconciliation_Print {
    private static final String THISCLASS = "CatCSVInvoiceReconciliation_Print";
    public void printHTML(BaseObject bo, PrintWriter out, boolean referenceImages,
        Log.customer.debug("%s *** 2. printHTML w/out Locale",THISCLASS);
    protected void printHTML(BaseObject bo, PrintWriter out, String userFieldPrintGroup,
        Log.customer.debug("%s *** 2. printHTML with Locale",THISCLASS);
 // 	*** Removed OOB condition since results in loop (calls PrintHook again)
        // issue #776 Adding Invoice Eform requester in print
        Invoice inv = (Invoice)lic.getDottedFieldValue("Invoice");
        ClusterRoot invEform = null;

        if (inv != null)
            invEform = (ClusterRoot)inv.getFieldValue("InvoiceEform");

        // issue #776 Loaded from Invoice Eform

        if  (invEform != null) {
		   Log.customer.debug("%s getting Invoice Eform's requester Name @@@@ "+ name,THISCLASS);

           out.print("Invoice created on " + inv.getCreateDate() + " by " + name );
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
            Log.customer.debug("%s seetting Invoice from ASN",THISCLASS);
	    }
        printHTMLLineItems(lic, out, locale);
    private final void printHeaderExceptions(InvoiceReconciliation ir, PrintWriter out, Locale locale, boolean printHTML)
    protected void printHTMLLineItems(LineItemCollection lic, PrintWriter out, Locale locale) {
        Log.customer.debug("%s *** 3. printHTMLLineItems",THISCLASS);
}