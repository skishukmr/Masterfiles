package config.java.print.sap;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.print.LineItemCollection_Print;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseObject_Print;
import ariba.base.core.Partition;
import ariba.base.fields.Print;
import ariba.basic.core.Money;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.print.ProcureLineItem_Print;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;

public class CatSAPProcureLineItem_Print extends ProcureLineItem_Print {

    private static final String THISCLASS = "CatSAPProcureLineItem_Print";
    private final int PRECISION = 5;
    private static String emergencyText = ResourceService.getString("cat.java.common","PO_EmergencyBuyText");
    private ProcureLineItem pli=null;
    private String displayCompanyNameFromAddress="DisplayCompanyNameFromAddress";

    public void printHTMLLineItems(PrintWriter out, LineItemCollection approvable, List lineItems,
            Money total, Locale locale)
    {
         Log.customer.debug("entered the printHTMLLineItems");
         boolean printSupplierLocationOnTop = printSupplierLocationOnTop(approvable, lineItems);
	     boolean printShippingInfoOnTop = printShippingInfoOnTop(approvable, lineItems);
	     boolean printAccountingInfoOnTop = printAccountingInfoOnTop(approvable, lineItems, "LineItemPrint");
	     boolean splitAccountingInfo = splitAccountingInfo(approvable, lineItems, "LineItemPrint", printAccountingInfoOnTop);
	     List commonFields = ListUtil.list();
	     List uniqueFields = ListUtil.list();
	     ProcureLineItem firstLineItem;
	     if(!lineItems.isEmpty())
	        firstLineItem = (ProcureLineItem)ListUtil.firstElement(lineItems);
	     else
	        firstLineItem = null;
         if(firstLineItem != null)
         {
            getCommonFields(approvable, firstLineItem.getTypeName(), commonFields, uniqueFields, locale);
            if(printSupplierLocationOnTop)
            {
	             MIME.crlf(out, "<P>");
	             MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
	             printSupplierLocation(firstLineItem, approvable, out, locale, true);
	             MIME.crlf(out, "</TABLE>");
            }
	        boolean printShipping = printShippingInfoOnTop && !hiddenField(firstLineItem, "ShipTo", true, true);
	        boolean printAccounting = printAccountingInfoOnTop && !hiddenField(firstLineItem, "Accountings", true, true);
	        if(printShipping || printAccounting)
	        {
	             MIME.crlf(out, "<P>");
	             MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
	             pli=firstLineItem;
	             printShippingAndAccountingInfo(firstLineItem, approvable, out, locale, printShipping, printAccounting, splitAccountingInfo, true);
	             MIME.crlf(out, "</TABLE>");
	        }

	     }
	     Log.customer.debug(" before the PO fileds method");
	     //printPOFields(firstLineItem,approvable, out, locale);
         printHTMLApprovableUserFields(approvable, out, ((LineItemCollection_Print)Print.get(approvable)).getUserFieldPrintGroup(approvable, true), locale);
	     if(!commonFields.isEmpty())
         {
			   String sectionHdr = "Additional Information";
	           MIME.crlf(out, "<P>");
	           MIME.crlf(out, "<TABLE BORDER=0 WIDTH=\"95%%\">");
	           printNewHTMLRow(out);
	           MIME.crlf(out, "<TD> <font size=1>");
               MIME.crlf(out, "<B>%s</B><BR>",sectionHdr);
	           //MIME.crlf(out, "<TD WIDTH=\"100%\"> <font size=1>");
	           ((BaseObject_Print)Print.get(firstLineItem)).printHTMLUserFields(firstLineItem, out, "LineItemPrint", commonFields.iterator(), locale);
	           MIME.crlf(out, "</Font></TD>");
	           printEndHTMLRow(out);
	           MIME.crlf(out, "</TABLE>");
	           MIME.crlf(out, "</P>");
	      }
	           MIME.crlf(out, "<P>");
	           MIME.crlf(out, "<TABLE BORDER=1 cellpadding=\"4\" cellspacing=\"0\" rules=\"groups\" frame=\"hsides\" width=\"100%\" bordercolorlight=\"#ffffff\" bordercolordark=\"#000000\">");
	           int lineItemsSize = lineItems.size();
	           int numLineItemsPrinted = 0;
	           int currNumInCollection = 0;
	           ProcureLineItem pLineItem = null;
	           int biggestNumInColl = 0;
	           for(int i = 0; i < lineItemsSize; i++)
	           {
	              pLineItem = (ProcureLineItem)lineItems.get(i);
	              pli=pLineItem;
	              if(pLineItem != null)
	              {
	                  int currNum = pLineItem.getNumberInCollection();
	                  if(currNum > biggestNumInColl)
	                      biggestNumInColl = currNum;
	              }
	           }

	           while(numLineItemsPrinted < lineItemsSize && lineItemsSize != 0)
	           {
	              boolean found = false;
	              for(int i = 0; i < lineItemsSize && !found; i++)
	              {
	                  pLineItem = (ProcureLineItem)lineItems.get(i);
	                  if(pLineItem.getNumberInCollection() == currNumInCollection)
	                  {
	                      currNumInCollection++;
	                      found = true;
	                      numLineItemsPrinted++;
	                  }
	            }

	              if(!found)
	              {
	                  if(currNumInCollection < biggestNumInColl)
	                  {
	                      currNumInCollection++;
	                      continue;
	                  }
	                  if(numLineItemsPrinted < lineItemsSize)
	                      ariba.procure.core.Log.fixme.warning(4611, lineItems);
	                  break;
	              }
	              if(pLineItem == null)
	              {
	                  ariba.procure.core.Log.fixme.warning(1236, approvable);
	              } else
	              {
	                  printHTML(pLineItem, approvable, out, locale);
	                  printRelatedInfo(pLineItem, approvable, out, locale);
	                  if(!printSupplierLocationOnTop)
	                      printSupplierLocation(pLineItem, approvable, out, locale, false);
	                  if(!printShippingInfoOnTop || !printAccountingInfoOnTop)
	                  {
	                      boolean printShipping = !printShippingInfoOnTop && !hiddenField(pLineItem, "ShipTo", false, true);
	                      boolean printAccounting = !printAccountingInfoOnTop && !hiddenField(pLineItem, "Accountings", false, true);
	                      printShippingAndAccountingInfo(pLineItem, approvable, out, locale, printShipping, printAccounting, splitAccountingInfo, false);
	                  }
	                  printNewHTMLRow(out);
	                  MIME.crlf(out, "<TD COLSPAN=%s> <font size=1>", Constants.getInteger(getLineItemColspan(approvable)));
	                  printHTMLUniqueFields(pLineItem, out, "LineItemPrint", uniqueFields.iterator(), locale);
	                  MIME.crlf(out, "</Font></TD>");
	                  printEndHTMLRow(out);
	                  MIME.crlf(out, "<TR>");
	                  MIME.crlf(out, "<TD COLSPAN=%s>&nbsp;</TD>", Constants.getInteger(getLineItemColspan(approvable)));
	                  printEndHTMLRow(out);
	              }
	          }
	          printHTMLLineTotal(approvable, out, locale, total);
	          MIME.crlf(out, "</TABLE>");
        MIME.crlf(out, "</P>");
    }

    // 05.16.06 (KS) override method to control Additional Charge Ref. Num printing on PO (never print at header)
    public void getCommonFields(LineItemCollection lic, String className, List commonFields, List uniqueFields, Locale locale)
    {
        super.getCommonFields(lic,className,commonFields,uniqueFields,locale);
        if (lic instanceof PurchaseOrder && !commonFields.isEmpty()) {
	        int size = commonFields.size();
	        Log.customer.debug("CatSAPProcureLineItem_Print *** size: " + size);
            while (size > 0) {
                String fieldName = (String)commonFields.get(--size);
                Log.customer.debug("%s *** fieldName: %s",THISCLASS,fieldName);
                // this will ensure ReferenceLineNumber never gets printed at header (on PO)
                if (fieldName.equals("ReferenceLineNumber"))  {
                    Log.customer.debug("%s *** found RefLineNum : remove from commonFields, add to uniqueFields",THISCLASS);
                    commonFields.remove(fieldName);
                    uniqueFields.add(fieldName);
                    break;
                }
            }
        }
    }

    protected void printHTMLUniqueFields(ProcureLineItem pLineItem, PrintWriter out, String group, Iterator fields, Locale locale)
    {
  //      Log.customer.debug("%s *** procureLineItem: %s",THISCLASS,pLineItem);
        if (fields != null && pLineItem instanceof POLineItem && !CatSAPAdditionalChargeLineItem.isAdditionalCharge(pLineItem)) {
            Log.customer.debug("%s *** Found Material POLineItem, creating new Iterator!",THISCLASS);
            List newFields = new ArrayList();
            while(fields.hasNext()) {
                String fieldName = (String)fields.next();
                Log.customer.debug("%s *** fieldName: %s",THISCLASS,fieldName);
                if (!fieldName.equals("ReferenceLineNumber"))  {
                    newFields.add(fieldName);
                }
                else {
                    Log.customer.debug("%s *** found RefLineNum : remove from UniqueFields",THISCLASS);
                }
            }
            ((BaseObject_Print)Print.get(pLineItem)).printHTMLUserFields(pLineItem, out, group, newFields.iterator(), locale);
        }
        else {
   //         Log.customer.debug("%s *** Not Material POLineItem, using OOB fields iterator!",THISCLASS);
            ((BaseObject_Print)Print.get(pLineItem)).printHTMLUserFields(pLineItem, out, group, fields, locale);
        }

    }

    public boolean checkForDisplay(ProcureLineItem plineitem,String targetfieldName)
	{
		Log.customer.debug("entered the check mathod");
		List dispoprint = (List)plineitem.getDottedFieldValue("LineItemCollection.CompanyCode.DisplayPOPrintFields");
		Log.customer.debug(" list the DisplayPOPrintFields values "+dispoprint);
		BaseObject dispo;
		if(dispoprint != null)
		{
			for(Iterator it = dispoprint.iterator(); it.hasNext(); )
			{
				dispo = (BaseObject)it.next();
				String fieldName = (String)dispo.getDottedFieldValue("FieldName");
				String display = (String)dispo.getDottedFieldValue("Display");
				if(targetfieldName.equalsIgnoreCase(fieldName))
				{
					if(display.equalsIgnoreCase("Y"))
					{
					  Log.customer.debug(" returning true");
					  return true;
				  }

				}

			}
			return false;
		}
		else
		  return false;

	}
	public String checkForLocalOrImport(ProcureLineItem plineitem,String targetfieldName)
		{
			Log.customer.debug("entered the checkForLocalOrImport mathod");
			List dispoprint = (List)plineitem.getDottedFieldValue("LineItemCollection.CompanyCode.DisplayPOPrintFields");
			Log.customer.debug(" list the DisplayPOPrintFields values "+dispoprint);
			String SuppLocationCountry = (String)plineitem.getDottedFieldValue("SupplierLocation.PostalAddress.Country.UniqueName");
			Log.customer.debug("SupplierLocation Country " + SuppLocationCountry);
			String CompanyRegAddCountyr = (String)plineitem.getDottedFieldValue("LineItemCollection.CompanyCode.RegisteredAddress.Country.UniqueName");
			Log.customer.debug("Company Registered Address Country " + CompanyRegAddCountyr);

			BaseObject dispo;
			if(dispoprint != null)
			{
				for(Iterator it = dispoprint.iterator(); it.hasNext(); )
				{
					dispo = (BaseObject)it.next();
					String fieldName = (String)dispo.getDottedFieldValue("FieldName");
					String display = (String)dispo.getDottedFieldValue("Display");
					String localID = (String)dispo.getDottedFieldValue("LocalID");
					Log.customer.debug("localID"+localID);
					String importID = (String)dispo.getDottedFieldValue("ImportID");
					Log.customer.debug("importID"+importID);
					if(targetfieldName.equalsIgnoreCase(fieldName))
					{
						if(display.equalsIgnoreCase("Y"))
						{
						  Log.customer.debug(" returning true");
						  if(SuppLocationCountry!=null && CompanyRegAddCountyr != null && SuppLocationCountry.equals(CompanyRegAddCountyr) )
								return localID;
						  return importID;
					  	}

					}

				}
				return null;
			}
			else
			  return null;

	}

    // Overriden to ensure BillTo prints even if Accounting is hidden on PO
    public void printShippingAndAccountingInfo(ProcureLineItem lineItem, Approvable approvable,
            PrintWriter out, Locale locale, boolean printShippingInfo, boolean printAccountingInfo,
            boolean splitAccountingInfo, boolean onHeader)
    {
        Log.customer.debug("%s *** STEP 7 - printShippingAndAccountingInfo!",THISCLASS);
        ProcureLineItem pLineItem = lineItem;
        printNewHTMLRow(out);
        if(printShippingInfo){
            printShipTo(lineItem, approvable, out, group(onHeader, true), locale);
        }
        if(!hiddenField(lineItem, "BillingAddress", onHeader, true)) {
 //         Log.customer.debug("%s *** !hiddenField(BillingAddress) = TRUE",THISCLASS);
          Log.customer.debug("CatSAPProcureLineItem_Print *** printAccountingInfo = " + printAccountingInfo);
            printBillTo(lineItem, approvable, out, group(onHeader, true), locale);
        }
        printEndHTMLRow(out);
        printNewHTMLRow(out);
        if(printShippingInfo && !hiddenField(lineItem, "DeliverTo", onHeader, true))
            {
        	printDeliverTo(lineItem, approvable, out, locale);
            }
        if(printAccountingInfo && lineItem.getAccountings() != null && !splitAccountingInfo)
            printAccounting(lineItem, approvable, out, locale);
        if(printAccountingInfo && lineItem.getAccountings() != null && splitAccountingInfo)
        {
            printEndHTMLRow(out);
            printNewHTMLRow(out);
            printSplitAccounting(lineItem, approvable, out, locale);
        }
        printEndHTMLRow(out);
    }
/*
	 public void printSupplierLocation(ProcureLineItem pLineItem, Approvable approvable, PrintWriter out, Locale locale, boolean onHeader)
	    {
	        printNewHTMLRow(out);
	        MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
	        MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "Supplier", locale));
	        Supplier supplier = pLineItem.getSupplier();
	        String supplierName = supplier != null ? supplier.getName() : "";
	        SupplierLocation supplierLocation = pLineItem.getSupplierLocation();
	        String contactName = supplierLocation != null ? supplierLocation.getContact() : "";
	        MIME.crlf(out, "%s", HTML.fullyEscape(supplierName));
	        MIME.crlf(out, "<BR>");
	        if(!hiddenField(pLineItem, "SupplierLocation", onHeader, true))
	        {
	            Address addr = pLineItem.getSupplierLocation();
	            if(addr != null)
	                ((Address_Print)Print.get(addr)).printHTMLAddressInLocale(addr, out, pLineItem, "SupplierLocation", group(onHeader, true), locale);
	        }
	        if(!hiddenField(pLineItem, "SupplierLocation.Contact", onHeader, true) && !StringUtil.nullOrEmptyString(contactName))
	            MIME.crlf(out, "%s<BR>", Fmt.Sil(locale, "resource.ordering", "Contact", HTML.fullyEscape(contactName)));
	        MIME.crlf(out, "</Font></TD>");
	    }
*/
	private static final String group(boolean onHeader, boolean html)
	    {
	        if(!html)
	        {
	            if(onHeader)
	                return "LineItemPrintTextSummary";
	            else
	                return "LineItemPrintTextDetails";
	        } else
	        {
	            return "LineItemPrint";
	        }
    }
	public void printCompanyName(Partition partition, Locale locale, Address address, PrintWriter out)
	{
		if(pli!=null && checkForDisplay(pli, displayCompanyNameFromAddress)){
			if(address != null)
				MIME.crlf(out, "%s", address.getName());
		}
		else{
			super.printCompanyName(partition, locale, address, out);
		}
	}

    public CatSAPProcureLineItem_Print() {
        super();
    }

}
