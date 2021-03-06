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
public class CatEZORequisition_Print extends Requisition_Print {

	private static final String ClassName = "CatEZORequisition_Print";

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
		if (!lic.getLineItems().isEmpty()) {
			CatEZOProcureLineItem_Print catPlip = new CatEZOProcureLineItem_Print();
			catPlip.printHTMLLineItems(out, lic, lic.getLineItems(), lic.getTotalCost(), locale);
		}
	}

	public CatEZORequisition_Print() {
		super();
	}
}