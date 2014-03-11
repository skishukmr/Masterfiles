/******************************************************************************
	Change Author:	Dharmang Shelat
	Date Created:	10/19/2006
	Description:	Required to call special print methods (Price precision
					changes)
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.print.vcsv3;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.procure.core.CategoryTemplateDetails;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.print.PurchaseOrder_Print;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.net.HTMLPrintWriter;

public class CatEZOPurchaseOrder_Print extends PurchaseOrder_Print {

	private static final String ClassName = "CatEZOPurchaseOrder_Print";

    private static final String VersionNumberKey = "VersionNumber";

	public void printHTML(BaseObject bo,
		PrintWriter out,
		boolean referenceImages,
		Locale locale)
	{
		Log.customer.debug("%s ::: 2. printHTML w/out Locale", ClassName);
		String group = getUserFieldPrintGroup((Approvable) bo, true);
		printHTML(bo, out, group, referenceImages, null);
	}

	protected void printHTML(BaseObject bo,
		PrintWriter out,
		String userFieldPrintGroup,
		boolean referenceImages,
		Locale locale)
	{
		Log.customer.debug("%s ::: 2. printHTML with Locale", ClassName);
		if (locale == null)
			locale = Base.getSession().getLocale();
		ApprovableClassProperties cp = (ApprovableClassProperties) ((Approvable) bo).getClassProperties();
		/*
		Removed OOB condition since results in loop (calls PrintHook again)
		String printHook = cp.getPrintHook();
		if(!StringUtil.nullOrEmptyString(printHook))
			runPrintHook((Approvable)bo,out,userFields,out,locale);
		*/
		LineItemCollection lic = (LineItemCollection) bo;
		HTMLPrintWriter hout = new HTMLPrintWriter(out);
		hout.HTMLOpen();
		hout.headOpen();
		hout.meta("text/html", MIME.getMetaCharset(locale));
		String title = getTitle(lic);
		if (!StringUtil.nullOrEmptyString(title))
			hout.title(HTML.fullyEscape(title));
		else
			hout.title("");
		hout.headClose();
		hout.bodyOpen();
		String fonts = ResourceService.getString("ariba.server.ormsserver", "FontFace", locale);
		Fmt.F(out, "<FONT FACE=\"%s\" SIZE=1>\r\n", fonts);
		printHeaderInfo(lic, out, referenceImages, locale);
		printHTMLLineItems(lic, out, locale);
		if (shouldPrintChangeRecords())
			printHTMLChangeRecords(lic, out, locale);
		if (shouldPrintApprovalFlow()) {
			out.print("<P>\r\n");
			printHTMLApprovalFlow(lic, out, locale);
			out.print("</P>\r\n");
		}
		if (shouldPrintComments())
			printHTMLCommentable(lic, out, locale);
		out.print("</FONT>");
		hout.bodyClose();
		hout.HTMLClose();
		hout.flush();
	}

	protected void printHTMLLineItems(LineItemCollection lic, PrintWriter out, Locale locale)
	{
		Log.customer.debug("%s ::: 3. printHTMLLineItems", ClassName);
		LineItem li = (LineItem) lic.getLineItems().get(0);
		if (li != null) {
			CatEZOProcureLineItem_Print catPlip = new CatEZOProcureLineItem_Print();
			catPlip.printHTMLLineItems(out, lic, lic.getLineItems(), lic.getTotalCost(), locale);
		}
	}

    /**
    SDey, Ariba, Inc : Override the method to hide
    IsInternalVersion for PurchaseOrder
    */
    protected void printHeaderInfo (Approvable lic,
                                PrintWriter out,
                                boolean referenceImages,
                                Locale locale)
   {
	    PurchaseOrder order = (PurchaseOrder)lic;

	        // print logo
	    super.printLogo(out, locale, referenceImages);

	        // Print name
	    String name = getName(lic, locale);
	    if (!StringUtil.nullOrEmptyString(name)) {
	        Fmt.F(out, "<H2>%s</H2>" + CRLF, HTML.fullyEscape(name));
	    }

	        //Print VersionNumber
	    Fmt.F(out, "%s: %s", ResourceService.getService().getLocalizedString(
	              OrderingTable, VersionNumberKey, locale), order.getVersionNumber());
	    out.print("<BR>");
	    out.print(CRLF);

	    /*
	     * SDey, Ariba, Inc : Commented out
	     *
	        //Print IsInternalVersion
	    Fmt.F(out, "%s: %s", ResourceService.getService().getLocalizedString(
	              OrderingTable, InternalVersionKey, locale), Constants
	          .getBoolean(order.getIsInternalVersion()));
	    out.print("<BR>");
	    out.print(CRLF);

	    *
	    * SDey, Ariba, Inc : Commented out
		*/
	        // Print date
	    out.print(
	        Fmt.Sil(
	            locale,
	            OrderingTable,
	            IssuedOnKey,
	            DateFormatter.getStringValue(
	                lic.getCreateDate(),
	                ResourceService.getService().getLocalizedString(DateFormatTable,
	                                                                DateFormatKey,
	                                                                locale),
	                locale,
	                Base.getSession().getTimezone())));
	    out.print("<BR>");
	    out.print(CRLF);

	        // Print preparer and requester
	    String creationDateString = DateFormatter.getStringValue(
	        lic.getCreateDate(),
	        ResourceService.getService().getLocalizedString(DateFormatTable,
	                                                        DateFormatKey,
	                                                        locale),
	        locale, Base.getSession().getTimezone());
	    if (SystemUtil.equal(lic.getPreparer(), lic.getRequester())) {
	        out.print(Fmt.Sil(locale, OrderingTable, CreatedOnKey,
	                          creationDateString, lic.getPreparer().getMyName(locale)));
	    }
	    else {
	        User requester = lic.getRequester();
	        out.print(Fmt.Sil(locale, OrderingTable, OnBehalfOfKey,
	                          creationDateString, lic.getPreparer().getMyName(locale),
	                          requester.getMyName(locale)));
	    }

	    out.print("<BR><BR>" + CRLF);
	    out.print("</P>" + CRLF);
	    CategoryTemplateDetails chd = order.getCategoryTemplateDetails();
	    if (chd != null) {
	        printCategoryTemplateDetailsTop(chd, out, locale);
	        printCategoryTemplateDetailsLR(chd, out, locale);
	    }
	    else {
	        printCategoryHeaderInfo((LineItemCollection)lic, out, locale);
	    }
   }

	public CatEZOPurchaseOrder_Print() {
		super();
	}
}