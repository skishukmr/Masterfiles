/* Created by KS/Chandra on April 19, 2006 for unique US format requirements
 * -------------------------------------------------------------------------
 * Ensures Req with Add Charges prints AC lines after reference Material line
 * Handles additional precision for Price fields
 * Other changes inherited from R1/R2 that apply for US (e.g., BillingAddress)
 * Removed R2 changes not applicable for US (e.g., remove Caterpillar, Inc. in addresses) *19-06-09   : Ashwini    :  Added functionality to remove CaterpillarInc from Invoice print screen. -Issue  974
 */
package config.java.print.vcsv3;

import java.io.PrintWriter;import java.util.Iterator;import java.util.List;import java.util.Locale;import ariba.approvable.core.Approvable;import ariba.approvable.core.LineItemCollection;import ariba.base.core.Base;import ariba.base.core.BaseService;import ariba.base.core.BaseVector;import ariba.base.fields.Print;import ariba.basic.core.Money;import ariba.basic.core.MoneyFormatter;import ariba.basic.core.UnitOfMeasure;import ariba.common.core.Address;import ariba.common.core.SplitAccounting;import ariba.common.core.SplitAccountingCollection;import ariba.common.core.print.Address_Print;import ariba.procure.core.ProcureLineItem;import ariba.statement.core.print.StatementReconciliationLineItem_Print;import ariba.util.core.Fmt;import ariba.util.core.HTML;import ariba.util.core.MIME;import ariba.util.core.ResourceService;import ariba.util.formatter.BigDecimalFormatter;import ariba.util.formatter.DateFormatter;import ariba.util.log.Log;import config.java.common.CatConstants;



public class CatEZOInvoiceReconciliationLineItem_Print extends StatementReconciliationLineItem_Print {

    private static final String THISCLASS = "CatEZOInvoiceReconciliationLineItem_Print";
    private final int PRECISION = 5;
	public void printHTMLLineItems(PrintWriter out, LineItemCollection approvable, List lineItems,            Money total, Locale locale)    {        if (CatConstants.DEBUG)            Log.customer.debug("%s *** STEP 6 - printHTMLLineItems!",THISCLASS);        super.printHTMLLineItems(out, approvable, lineItems, total, locale);    }
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
    }  //Added ShipTO and BillTo Address - code starts  (Ashwini)	public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)    {        if (CatConstants.DEBUG)            Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));       Address shipTo = pli.getShipTo();	   		Log.customer.debug("%s SHIPTOAddress&*&*&*& *** %s",THISCLASS,shipTo);			Log.customer.debug("%s SHIPTO *** %s",THISCLASS,shipTo.getUniqueName());			String nameshipping = shipTo.getName() ;			Log.customer.debug("%s SHIPTO Name Test *** %s",THISCLASS,shipTo.getName());			if(nameshipping.equals(""))			{			SplitAccountingCollection AccFac = pli.getAccountings();			BaseVector splits = AccFac.getSplitAccountings();			String accfacility = "" ;			if (!splits.isEmpty()) {			Log.customer.debug("********Inside Loop**********");								for (Iterator itr1 = splits.iterator() ; itr1.hasNext() ;)  {								Log.customer.debug("********Inside For Loop**********");			  							SplitAccounting sa = (SplitAccounting)itr1.next();										accfacility = (String)sa.getDottedFieldValue("AccountingFacility");										Log.customer.debug("********Account Facility fetched**********%s",accfacility);										}			}						BaseService service = Base.getService();			if(accfacility.equals("36")){			String add = "36_ShipTo";			ariba.common.core.Address shiptoassign1 = (ariba.common.core.Address) Base.getSession().objectFromName(add, "ariba.common.core.Address", Base.getSession().getPartition());			String name1 = (String)shiptoassign1.getDottedFieldValue("Name");			printAddressHeaderForPartition(approvable,shiptoassign1,out,locale);			}			else if(accfacility.equals("NF")){			String add2 = "NF_ShipTo";			ariba.common.core.Address shiptoassign2 = (ariba.common.core.Address) Base.getSession().objectFromName(add2, "ariba.common.core.Address", Base.getSession().getPartition());			String name2 = (String)shiptoassign2.getDottedFieldValue("Name");			printAddressHeaderForPartition(approvable,shiptoassign2,out,locale);			}			else if(accfacility.equals("NG")){			String add3 = "NG_ShipTo";			ariba.common.core.Address shiptoassign3 = (ariba.common.core.Address) Base.getSession().objectFromName(add3, "ariba.common.core.Address", Base.getSession().getPartition());			String name3 = (String)shiptoassign3.getDottedFieldValue("Name");			printAddressHeaderForPartition(approvable,shiptoassign3,out,locale);			}			}			else			{			printAddressHeaderForPartition(approvable,shipTo,out,locale);			}		 if(shipTo != null)        {		   ((Address_Print)Print.get(shipTo)).printHTMLAddressInLocale(shipTo, out, pli, "ShipTo", group, locale);        MIME.crlf(out, "</Font></TD>");    }	}	public void printBillTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)    {        if (CatConstants.DEBUG)            Log.customer.debug("%s *** STEP 8 - printBillTo!",THISCLASS);        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "BillTo", locale));        Address billTo = pli.getBillingAddress();		Log.customer.debug("%s BILLTO *** %s",THISCLASS,billTo.getUniqueName());        printAddressHeaderForPartition(approvable,billTo,out,locale);        if(billTo != null)            ((Address_Print)Print.get(billTo)).printHTMLAddressInLocale(billTo, out, pli, "BillingAddress", group, locale);        MIME.crlf(out, "</Font></TD>");    }	public void printAddressHeaderForPartition(Approvable approvable, Address address, PrintWriter out, Locale locale)    {        if (approvable.getPartition().getName().equals("ezopen") && address != null)			{			String shipName= address.getName() ;			Log.customer.debug("%s SHIPNAME IN PRINTHEADER *** %s",THISCLASS,shipName);			String shipId= address.getName() ;			Log.customer.debug("%s SHIPID IN PRINTHEADER *** %s",THISCLASS,shipId);			MIME.crlf(out, "%s", address.getName());			}			else            printCompanyName(approvable.getPartition(), locale, address, out);        MIME.crlf(out, "<BR>");    }	 //Added ShipTO and BillTo Address - code ends  (Ashwini)

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

    public CatEZOInvoiceReconciliationLineItem_Print() {
        super();
    }

}
