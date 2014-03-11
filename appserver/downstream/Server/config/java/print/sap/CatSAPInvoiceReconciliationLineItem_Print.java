/* Created by KS/Chandra on April 19, 2006 for unique US format requirements
 * -------------------------------------------------------------------------
 * Ensures Req with Add Charges prints AC lines after reference Material line
 * Handles additional precision for Price fields
 * Other changes inherited from R1/R2 that apply for US (e.g., BillingAddress)
 * Removed R2 changes not applicable for US (e.g., remove Caterpillar, Inc. in addresses)
 20/01/2014	IBM Parita Shah	  SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6) MSC Tax Gaps Correct Legal Entity
 */
package config.java.print.sap;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.basic.core.MoneyFormatter;
import ariba.basic.core.UnitOfMeasure;
import ariba.procure.core.ProcureLineItem;
import ariba.statement.core.print.StatementReconciliationLineItem_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
//Starts SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)
import ariba.util.core.ListUtil;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseObject_Print;
import ariba.common.core.Address;
import ariba.base.core.Partition;
import ariba.util.core.StringUtil;
import ariba.common.core.Address;
import ariba.common.core.print.Address_Print;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.StringUtil;
import ariba.base.fields.Print;
import java.util.Iterator;
import java.util.List;
//Ends SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)


public class CatSAPInvoiceReconciliationLineItem_Print extends StatementReconciliationLineItem_Print {

    private static final String THISCLASS = "CatSAPInvoiceReconciliationLineItem_Print";
    private final int PRECISION = 5;
    //Starts SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)
    private String displayCompanyNameFromAddress="DisplayCompanyNameFromAddress";
    private ProcureLineItem pli=null;
    //Ends SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)

	/**** 03.20.06 (Chandra)
	* To print the Price without rounding off and overide the currency presicion
	*******/
    protected void printTextLineDetailWithoutLineNumber(ProcureLineItem lineItem, PrintWriter out, String uomStr, Locale locale)
    {
		Log.customer.debug("%s *** printTextLineDetailWithoutLineNumber!",THISCLASS);

        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "QtyUOMAmount",
        								BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
        								uomStr,
        								MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
        								lineItem.getAmount().asString()));
        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
    }

	/* To print the Price without rounding off and overide the currency presicion */
    protected void printTextLineDetailWithLineNumber(ProcureLineItem lineItem, PrintWriter out, String uomStr, Locale locale)
    {
		Log.customer.debug("%s *** printTextLineDetailWithLineNumber!",THISCLASS);

        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "ItemQtyUOMAmount", Integer.toString(lineItem.getNumberInCollection()),
        									BigDecimalFormatter.getStringValue(lineItem.getQuantity(), locale),
        									uomStr,
        									MoneyFormatter.getStringValue(lineItem.getDescription().getPrice(), locale, null, true, PRECISION),
        									lineItem.getAmount().asString()));
        Fmt.F(out, "%s", Fmt.Sil(locale, "resource.ordering", "DescriptionLine", lineItem.getDescription().getDescription()));
    }

	/* To print the Price without rounding off and overide the currency presicion */
    protected void printHTMLRow(ProcureLineItem pLineItem, Approvable approvable, PrintWriter out, Locale locale)
    {
        printNewCol(out);
        MIME.crlf(out, "%s", HTML.fullyEscape(Integer.toString(pLineItem.getNumberInCollection())));
        printEndCol(out);
        if(approvable.hasPreviousVersion())
        {
            printNewCol(out);
            MIME.crlf(out, "%s", HTML.fullyEscape(pLineItem.getChangedStateLocalized(locale)));
            printEndCol(out);
        }
        MIME.crlf(out, "<TD WIDTH=\"40%%\"> <font size=1><b>");
        MIME.crlf(out, "%s", HTML.fullyEscape(insertNewLineBreaks(pLineItem.getDescription().getShortName())));
        MIME.crlf(out, "</b>");
        printEndCol(out);
        printNewCol(out);
        MIME.crlf(out, "%s", HTML.fullyEscape(pLineItem.getDescription().getSupplierPartNumber()));
        printEndCol(out);
        printNewCol(out);
        UnitOfMeasure unitOfMeasure = pLineItem.getDescription().getUnitOfMeasure();
        MIME.crlf(out, "%s", HTML.fullyEscape(unitOfMeasure != null ? unitOfMeasure.getName().getString(locale) : ""));
        printEndCol(out);
        printNewCol(out);
        MIME.crlf(out, "%s", HTML.fullyEscape(BigDecimalFormatter.getStringValue(pLineItem.getQuantity(), locale)));
        printEndCol(out);
        printNewCol(out);
        ariba.util.core.Date needBy = pLineItem.getNeedBy();
        if(needBy != null)
        {
            String needByStr = DateFormatter.getStringValue(pLineItem.getNeedBy(), ResourceService.getService().getLocalizedString("resource.date", "DayDateMonthYearFmt", locale), locale, Base.getSession().getTimezone());
            MIME.crlf(out, HTML.fullyEscape(needByStr));
        } else
        {
            MIME.crlf(out, ResourceService.getService().getLocalizedString("resource.ordering", "NoValue", locale));
        }
        printEndCol(out);
		Log.customer.debug("%s *** printHTMLRow changing price and amount!",THISCLASS);

        MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

        MIME.crlf(out, "%s", HTML.escape(MoneyFormatter.getStringValue(pLineItem.getDescription().getPrice(), locale, null, true, PRECISION)));
        printEndCol(out);
        MIME.crlf(out, "<TD WIDTH=\"14%%\"> <font size=1>");

        MIME.crlf(out, "%s", HTML.escape(pLineItem.getAmount().asString()));
        printEndCol(out);
    }


	//Starts SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)

    public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
   		{
   			Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);
   			MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
   			MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));

   			ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
   			if(plic != null && checkForDisplay(pli, displayCompanyNameFromAddress))
   			{
   				String companyName = (String)plic.getDottedFieldValue("CompanyCode.Description");
   				Log.customer.debug("%s *** Inside printCompanyName CompanyCode name is",companyName);
   				if(!StringUtil.nullOrEmptyOrBlankString(companyName))
   				{
   					MIME.crlf(out, "%s", companyName);
   				}
   			}
   			//printCompanyName(approvable.getPartition(), locale, pLineItem.getShipTo(), out);
   			MIME.crlf(out, "<BR>");
   			Address shipTo = pli.getShipTo();
   			if(shipTo != null)
   			{
   				Address addr = pli.getShipTo();
   				((Address_Print)Print.get(addr)).printHTMLAddressInLocale(addr, out, pli, "ShipTo", group, locale);
   			}
   			MIME.crlf(out, "</Font></TD>");
       }


   	public boolean checkForDisplay(ProcureLineItem plineitem,String targetfieldName)
   	{
   		Log.customer.debug("entered the check method");
   		Log.customer.debug("%s *** Inside checkForDisplay!",THISCLASS);
   		Log.customer.debug("*** Inside checkForDisplay targetname is %s:",targetfieldName);
   		Log.customer.debug("*** Inside checkForDisplay plineitem is:",plineitem);

		ProcureLineItemCollection plic1 = (ProcureLineItemCollection)plineitem.getLineItemCollection();
   		Log.customer.debug("checkForDisplay ProcureLineItemCollection value is: %s",plic1.getUniqueName());

   		if(plic1.getFieldValue("CompanyCode") != null)
		{

			List dispoprint = (List)plic1.getDottedFieldValue("CompanyCode.DisplayPOPrintFields");
			Log.customer.debug("checkForDisplay list the DisplayPOPrintFields values "+dispoprint);

			BaseObject dispo;
			if(dispoprint != null)
			{
				Log.customer.debug("checkForDisplay dispoprint is not null");
				for(Iterator it = dispoprint.iterator(); it.hasNext(); )
				{
					Log.customer.debug("checkForDisplay within for loop");
					dispo = (BaseObject)it.next();
					String fieldName = (String)dispo.getDottedFieldValue("FieldName");
					String display = (String)dispo.getDottedFieldValue("Display");

					Log.customer.debug("checkForDisplay...returning true"+fieldName);
					Log.customer.debug("checkForDisplay...returning true"+display);

					if(targetfieldName.equalsIgnoreCase(fieldName))
					{
						if(display.equalsIgnoreCase("Y"))
						{
						  Log.customer.debug("checkForDisplay...returning true"+fieldName);
						  Log.customer.debug("checkForDisplay...returning true"+display);
						  return true;
					  }

					}

				}
				return false;
			}
			else
			  return false;
		}
		else
		return false;

   	}
   	//Ends SpringRelease_RSD 111(FDD4.5,4.6/TDD1.5,1.6)



    public CatSAPInvoiceReconciliationLineItem_Print() {
        super();
    }

}
