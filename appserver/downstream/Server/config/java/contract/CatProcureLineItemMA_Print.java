/*
 * 05-10-2007     Amit Kumar    Overridden OOB method to exclude company name Caterpillar Inc.and
 *								instead print Perkins Engines Company on contract print for mfg1.
 * 20/01/2014	IBM Parita Shah SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4) MSC Tax Gaps Correct Legal Entity
 17/01/2014  IBM Parita Shah	SpringRelease_RSD 111(FDD4.9,4.10/TDD1.9,1.10) New file created for MSC Tax Gaps Correct Legal Entity
 * -------------------------------------------------------------------------------------------
 *
 *
 */
package config.java.contract;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Print;
import ariba.basic.core.Money;
import ariba.common.core.Address;
import ariba.common.core.print.Address_Print;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.print.ContractCoreApprovableLineItem_Print;
//Starts SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4)
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.StringUtil;
//Ends SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4)
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.MIME;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatProcureLineItemMA_Print extends ContractCoreApprovableLineItem_Print {

    private static final String THISCLASS = "CatProcureLineItemMA_Print";

    public void printHTMLLineItems(PrintWriter out, LineItemCollection approvable, List lineItems,
            Money total, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 6 - printHTMLLineItems!",THISCLASS);
        super.printHTMLLineItems(out, approvable, lineItems, total, locale);
    }

    // Overridden to exclude Company Name (Caterpillar, Inc.) for UK orders
    public void printShipTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 8 - printShipTo!",THISCLASS);
         	MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
        	MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "ShipTo", locale));
        	Address shipTo = pli.getShipTo();
        	String shipAddress = (String)shipTo.getUniqueName();
        	Log.customer.debug( " getShipTo() returned : %s",shipAddress);
        	if(approvable instanceof Contract) {
				Log.customer.debug("Master Agreement Shipto ");
               	printAddressHeaderForPartition((Contract)approvable,shipTo,out,locale);
			}
			else {
				Log.customer.debug("Master Agreement Request Shipto");
				printAddressHeaderForPartition((ContractRequest)approvable,shipTo,out,locale);
			}

        	if(shipTo != null)
        	{
        	    ((Address_Print)Print.get(shipTo)).printHTMLAddressInLocale(shipTo, out, pli, "ShipTo", group, locale);
        	}
        	MIME.crlf(out, "</Font></TD>");

    }

    // Overridden to exclude Company Name (Caterpillar, Inc.) for UK orders
    public void printBillTo(ProcureLineItem pli, Approvable approvable, PrintWriter out, String group, Locale locale)
    {
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** STEP 8 - printBillTo!",THISCLASS);
		  	MIME.crlf(out, "<TD COLSPAN=4> <font size=1>");
        	MIME.crlf(out, "<B>%s</B><BR>", ResourceService.getService().getLocalizedString("resource.ordering", "BillTo", locale));
        	Address billTo = pli.getBillingAddress();
        	String billAddress = (String)billTo.getUniqueName();
        	Log.customer.debug(" getBillingAddress() returned : %s ", billAddress);
        	if(approvable instanceof Contract) {
				Log.customer.debug("Master Agreement BillingAddress ");
        		printAddressHeaderForPartition((Contract)approvable,billTo,out,locale);
			}
			else {
				Log.customer.debug("Master Agreement Request BillingAddress");
			    printAddressHeaderForPartition((ContractRequest)approvable,billTo,out,locale);
			}

        	if(billTo != null)
        	{
        	    ((Address_Print)Print.get(billTo)).printHTMLAddressInLocale(billTo, out, pli, "BillingAddress", group, locale);
			}
        	MIME.crlf(out, "</Font></TD>");

    }

  public void printAddressHeaderForPartition(Approvable approvable, Address address, PrintWriter out, Locale locale)
    {
        String StrAddress = (String)address.getUniqueName();
		if (approvable.getPartition().getName().equals("mfg1") && address != null)
		{
		   	Log.customer.debug(" Partition in printAddressHeaderForPartition is : %s",approvable.getPartition().getName());
		   	Log.customer.debug("Address in printAddressHeaderForPartition is : %s",StrAddress);
		    MIME.crlf(out, "%s", address.getName());
		}
		// Starts SpringRelease_RSD 111(FDD4.9,4.10/TDD1.9,1.10)
		else if (approvable.getPartition().getName().equals("pcsv1") && address != null)
		{
		   	Log.customer.debug(" Partition is pcsv1 printAddressHeaderForPartition is : %s",approvable.getPartition().getName());
		   	Log.customer.debug("Address in printAddressHeaderForPartition is : %s",StrAddress);

		   	if(approvable instanceof ProcureLineItemCollection)
		   	{
		   		ProcureLineItemCollection plic = (ProcureLineItemCollection)approvable;
						//Requisition req = (Requisition)pli.getLineItemCollection();
						if(plic != null)
						{
							String accFac = (String)plic.getDottedFieldValue("AccountingFacilityName");
							Log.customer.debug("CatCSVProcureLineItem_Print AccountingFacilityName", accFac);
							if (!StringUtil.nullOrEmptyOrBlankString(accFac))
							{

								MIME.crlf(out, "%s", accFac);
								MIME.crlf(out, "<BR>");
							}
							else
							{
								MIME.crlf(out, "%s", address.getName());
								MIME.crlf(out, "<BR>");
							}


						}
			}

		    //MIME.crlf(out, "%s", address.getName());
		}
		// Ends SpringRelease_RSD 111(FDD4.9,4.10/TDD1.9,1.10)
		//Starts SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4)
		else if (approvable.getPartition().getName().equals("SAP") && address != null)
				{
				   	Log.customer.debug(" Partition is SAP printAddressHeaderForPartition is : %s",approvable.getPartition().getName());
				   	Log.customer.debug("Address in printAddressHeaderForPartition is : %s",StrAddress);

				   	if(approvable instanceof ProcureLineItemCollection)
				   	{
				   		ProcureLineItemCollection plic = (ProcureLineItemCollection)approvable;
						//Requisition req = (Requisition)pli.getLineItemCollection();
						if(plic != null)
						{
							if(plic.getFieldValue("CompanyCode") != null)
							{
								String companyCode = (String)plic.getDottedFieldValue("CompanyCode.Description");
								Log.customer.debug("CatProcureLineItemMA_Print CompanyCode is", companyCode);
								if (!StringUtil.nullOrEmptyOrBlankString(companyCode))
								{

									MIME.crlf(out, "%s", companyCode);
									MIME.crlf(out, "<BR>");
								}
								else
								{
									MIME.crlf(out, "%s", address.getName());
									MIME.crlf(out, "<BR>");
								}
							}
							else
							{
								MIME.crlf(out, "%s", address.getName());
								MIME.crlf(out, "<BR>");

							}



						}
					}

				    //MIME.crlf(out, "%s", address.getName());
		}
		//Ends SpringRelease_RSD 111(FDD4.3,4.4/TDD1.3,1.4)
		else
		{
			Log.customer.debug("Address in else is : %s",StrAddress);
		    printCompanyName(approvable.getPartition(), locale, address, out);
		}

        MIME.crlf(out, "<BR>");
    }

    // Had to create new method since Ariba's OOB is static final
    private static String groupE(boolean onHeader, boolean html)
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

    public CatProcureLineItemMA_Print() {
        super();
    }

}
