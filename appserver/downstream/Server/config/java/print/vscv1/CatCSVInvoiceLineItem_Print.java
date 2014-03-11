/* Created by KS/Chandra on April 19, 2006 for unique US format requirements
 * -------------------------------------------------------------------------
 * Ensures Req with Add Charges prints AC lines after reference Material line
 * Handles additional precision for Price fields
 * Other changes inherited from R1/R2 that apply for US (e.g., BillingAddress)
 * Removed R2 changes not applicable for US (e.g., remove Caterpillar, Inc. in addresses)
 *	15/01/2014     IBM Parita Shah	SpringRelease_RSD 111(FDD4.11,4.12/TDD1.11,1.12) MSC Tax Gaps Correct Legal Entity
 */
package config.java.print.vcsv1;

import java.io.PrintWriter;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.basic.core.MoneyFormatter;
import ariba.basic.core.UnitOfMeasure;
import ariba.procure.core.ProcureLineItem;
import ariba.statement.core.print.StatementCoreApprovableLineItem_Print;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
// Starts SpringRelease_RSD 111(FDD4.11,4.12/TDD1.11,1.12)
import ariba.common.core.Address;
import ariba.common.core.print.Address_Print;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.StringUtil;
import ariba.base.core.BaseObject_Print;
import ariba.base.fields.Print;
// Ends SpringRelease_RSD 111(FDD4.11,4.12/TDD1.11,1.12)



public class CatCSVInvoiceLineItem_Print extends StatementCoreApprovableLineItem_Print {

    private static final String THISCLASS = "CatCSVInvoiceLineItem_Print";
    private final int PRECISION = 5;
    private static String emergencyText = ResourceService.getString("cat.java.common","PO_EmergencyBuyText");

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

     // Start SpringRelease_RSD 111(FDD4.11,4.12/TDD1.11,1.12)
			    public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
			    {
			        Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);
			        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
			        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));

					ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
					if(plic != null)
					{
						String accFac = (String)plic.getDottedFieldValue("AccountingFacilityName");
						Log.customer.debug("CatCSVInvoiceReconciliationLineItem_Print AccountingFacilityName", accFac);
						if (!StringUtil.nullOrEmptyOrBlankString(accFac))
						{

							MIME.crlf(out, "%s", accFac);
							MIME.crlf(out, "<BR>");
						}

					}

			        Address shipTo = pli.getShipTo();
			        //printAddressHeaderForPartition(approvable,shipTo,out,locale);
			        if(shipTo != null)
			        {   //MIME.crlf(out, "%s", shipTo.getName());
			        	//MIME.crlf(out, "<BR>");
			            ((Address_Print)Print.get(shipTo)).printHTMLAddressInLocale(shipTo, out, pli, "ShipTo", group, locale);
			        }
			        MIME.crlf(out, "</Font></TD>");
			    }

	 // End SpringRelease_RSD 111(FDD4.11,4.12/TDD1.11,1.12)

    public CatCSVInvoiceLineItem_Print() {
        super();
    }

}
