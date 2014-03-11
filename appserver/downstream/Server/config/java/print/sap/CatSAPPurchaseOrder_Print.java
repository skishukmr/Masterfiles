/* Created by KS on Apr 17, 2006
 * -------------------------------------------------------------------------------
 * Required to call special print methods (Add Charge printing and Price precision changes)
 *
 *28.12.2012 (Jayashree) - Issue 293:Singapore Tc's: For Singapore PO's,
 *                    Supplier should be able to view the Terms and Conditions in ASN.
 *01/13/2014  IBM Jayashree B S   SpringRelease_RSD 109 (FDD_109_4.2 / TDD_109_3.0)  Populate T&C in the comments section on Ariba Network PO(To comment the logic implemented as part for WI 293)
 *
 */
package config.java.print.sap;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableClassProperties;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.procure.core.CategoryTemplateDetails;
import ariba.procure.core.ProcureLineItem;
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


public class CatSAPPurchaseOrder_Print extends PurchaseOrder_Print {

    private static final String THISCLASS = "CatSAPPurchaseOrder_Print";

    private static final String VersionNumberKey = "VersionNumber";

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
 //     *** Removed OOB condition since results in loop (calls PrintHook again)
 //     String printHook = cp.getPrintHook();
 //     if(!StringUtil.nullOrEmptyString(printHook))
 //          runPrintHook((Approvable)bo,out,userFields,out,locale);
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
        Log.customer.debug( "before printing the apprval graph");
        if(shouldPrintApprovalFlow())
        {
            out.print("<P>\r\n");
            printHTMLApprovalFlow(lic, out, locale);
            out.print("</P>\r\n");
        }
        Log.customer.debug( "end of the apprval graph");
        CatSAPProcureLineItem_Print catPlip = new CatSAPProcureLineItem_Print();
    ProcureLineItem pLineItem = (ProcureLineItem)lic.getLineItems().get(0);

    // Starts : SpringRelease_RSD 109 (FDD_109_4.2 / TDD_109_3.0) To comment the logic implemented as part for WI 293
    /** Starts: Issue 293
     *  Purpose: For singapore PO's, on PO print the terms and conditions section is hided.
     *          As the terms and conditions will be displayed as part of Comment.
     *
    PurchaseOrder po = (PurchaseOrder)lic;
    Log.customer.debug("%s: PurchaseOrder = " + po);
        String companyCodeSG_Key = ResourceService.getString("cat.Ordering.FieldsAdded_Country_SG", "SG_CompanyAddRegCountry");
        Log.customer.debug("%s to check for resource file is missing"+companyCodeSG_Key);

    String companyRegAddCountry = null;
    if (po.getFieldValue("CompanyCode") != null){
        Log.customer.debug("CatSAPAllDirectOrder CompanyCode is not null");
        if((po.getDottedFieldValue("CompanyCode.RegisteredAddress") != null)){
            if((po.getDottedFieldValue("CompanyCode.RegisteredAddress.Country") != null)){
                companyRegAddCountry = (String) po.getDottedFieldValue("CompanyCode.RegisteredAddress.Country.UniqueName");
                Log.customer.debug("CatSAPAllDirectOrder field companyRegAddCountry is not null"+companyRegAddCountry);
            }
        }
    }

        if(!((companyRegAddCountry != null) &&(companyRegAddCountry.equalsIgnoreCase(companyCodeSG_Key)))){
                Log.customer.debug("CatSAPPurchaseOrder_Print: Enters If blk: when CompanyCode's registeredAddress Country is not SG");
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
        }

        // Ends: Issue 293 */
    // Ends : SpringRelease_RSD 109 (FDD_109_4.2 / TDD_109_3.0) To comment the logic implemented as part for WI 293
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
        LineItem li = (LineItem)lic.getLineItems().get(0);
        if (li != null) {
            CatSAPProcureLineItem_Print catPlip = new CatSAPProcureLineItem_Print();
            //catPlip.printHTMLApprovableUserFields(lic, out, ((LineItemCollection_Print)Print.get(lic)).getUserFieldPrintGroup(lic, true), locale);
            catPlip.printHTMLLineItems(out,lic,lic.getLineItems(),lic.getTotalCost(),locale);
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

    public CatSAPPurchaseOrder_Print() {
        super();
    }

}
