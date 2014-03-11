/*

Author: RajendraPrasad (RP)
Date; 11/07/2005

Purpose:

The java program CatMFGInvoiceReconciliationMethod  basically gets the TaxDetails.Percent
From Invoice and  then based on the  Percent value the appropriate VATClass value is assigned

I.e.   if 	percent=0     	then VATClassValue =0
			percent=17.5 %  then VATClassValue =5
			percent=5  %  	then VATClassValue =8

And next  get the SupplierLocation.UniqueName from the invoice and  lookup for the reference
file(CATMFGSupplierVATReference .csv) that holds supplier UniqueName and appropriate VATClass
and if both the VATClass values from the reference file and from the  TaxDetails.Percent
appropriate VATClass are same then  update the line items  of the invoice with the appropriate
VATClass value for  all non-Tax items.

Shaila Sept 23 2008 - Issue 852 Added code to fefault BuyerCode from Order line item or MALineItem


Dibya Prakash  Nov 25 2008 - Issue 878:  Code changed for UK VAT (changing from 17.5 to 15)
Ashwini Jan 06,2010 - Code changed for UK VAT (changing from 15to 17.5)
*/


package config.java.invoicing.vcsv2;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.common.core.CommodityExportMapEntry;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.tax.core.TaxDetail;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BigDecimalFormatter;
import config.java.common.CatConstants;
import config.java.invoicing.CatInvoiceReconciliationMethod;


public class CatMFGInvoiceReconciliationMethod extends CatInvoiceReconciliationMethod {
	public static final String ClassName = "CatMFGInvoiceReconciliationMethod";
	private static String GoodPORefDate = ResourceService.getString("aml.cat.Invoice","DateForGoodPOReferences");
	private static final String VAT_RATE_CHANGED = ResourceService.getString("cat.java.vcsv2","VatRateChanges");

			        public static final String BC_CLASS = "cat.core.BuyerCode";
			        public static final String CapitalBC = "82";
			        public static final String NA_BC = CapitalBC;
        private static final String NonDX_BC = "86";

	protected List createInvoiceReconciliations(Invoice invoice) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the createInvoiceReconciliations method", ClassName);


		Partition partition = Base.getSession().getPartition();
		BaseVector invLineItems = invoice.getLineItems();
		BaseVector taxDetails=invoice.getTaxDetails();
		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem procureLineItem = null;
		ClusterRoot  vatClass=null;
		ProcureLineType procureLineType=null;
		BigDecimal percentage;
		List listValue=null;
		float percentageValue=0;
		float vatratechange=0;
		String supplierLocUniqueName=null;
		String fileVATClassValue=null;
		int VATValue =-1;

		int vatClassValue=-1;
		int category=0;
		Integer categoryValue;

		Log.customer.debug("%s ::: Setting BuyerCode on Invoice", ClassName);
					setBuyerCodesOnLines(invoice, invLineItems);

		int invLoadingCat = invoice.getLoadedFrom();
		if  (invLoadingCat == 1 || invLoadingCat==2) {
		 Log.customer.debug(ClassName + " Tax Details Size: " + taxDetails.size());
		 if (taxDetails.size()>0)
		 {
			if (((TaxDetail)taxDetails.get(0)).getPercent()!=null);
			percentage=(java.math.BigDecimal)((TaxDetail)taxDetails.get(0)).getPercent();
				if (percentage!=null) percentageValue=percentage.floatValue();
   					Log.customer.debug("%s value for invoice loading cat is %s ",ClassName, invLoadingCat);
					//Float f_vat = new Float(VAT_RATE_CHANGED);
					//vatratechange = f_vat.floatValue();
					String fvat = BigDecimalFormatter.getStringValue(percentage);
					if (percentageValue==0 && percentage!=null)
						vatClassValue =0;

					// Code changed for UK VAT ( from 17.5 to 15 )
					// Code changed for UK VAT ( from 15to 17.5 )
					else if (fvat.equals(VAT_RATE_CHANGED))
						vatClassValue =1;
					else if (percentageValue==5)
						vatClassValue =8;

					supplierLocUniqueName=(String)invoice.getDottedFieldValue("SupplierLocation.UniqueName");
					Log.customer.debug("%s value for invoice supplierLocUniqueName  is %s ",ClassName, supplierLocUniqueName);
					listValue=config.java.common.CatCommonUtil.makeValueListFromFile(supplierLocUniqueName,"/msc/arb821/Server/config/variants/vcsv2/data/CATMFGSupplierVATReference.csv");

					for (Iterator iterator = listValue.iterator(); iterator.hasNext();) {
						fileVATClassValue=(String)iterator.next();
						Log.customer.debug("%s value for fileVATClassValue  is %s ",ClassName, fileVATClassValue);
						VATValue=Integer.parseInt(fileVATClassValue.trim());
					}
					Log.customer.debug("%s value for invoice supplier vatclass  is %s ",ClassName, vatClassValue);
					Log.customer.debug("%s value for supplier from CSV File is  %s ",ClassName, VATValue);
					if (vatClassValue ==VATValue)  {
						Log.customer.debug("%s entered the if block when supp Vatclass and filevatclass are same ",ClassName);
					    Log.customer.debug("%s value for percentagevalue is   %s ",ClassName,percentage);
						if (percentageValue==0 && percentage!=null)
							vatClass = (ClusterRoot)Base.getService().objectMatchingUniqueName("cat.core.VATClass",partition,"0");

							// Code changed for UK VAT ( from 17.5 to 15 )
						else if (fvat.equals(VAT_RATE_CHANGED))
							vatClass = (ClusterRoot)Base.getService().objectMatchingUniqueName("cat.core.VATClass",partition,"1");
						else if (percentageValue==5.0)
							vatClass = (ClusterRoot)Base.getService().objectMatchingUniqueName("cat.core.VATClass",partition,"8");


							for (int i = 0; i < invLineItems.size(); i++) {
								 categoryValue=(Integer)((InvoiceLineItem)invLineItems.get(i)).getDottedFieldValue("LineType.Category");
								 category=categoryValue.intValue();
								 Log.customer.debug("%s value for category  in Line items %s  is %s ",ClassName, ((InvoiceLineItem)invLineItems.get(i)).getNumberInCollection(), category);
								if ( category != 2)  {
									((InvoiceLineItem)invLineItems.get(i)).setFieldValue("VATClass",vatClass);
								}
							}
					}
			  }
	    	}
			return super.createInvoiceReconciliations(invoice);
		}

//Method Added by Shaila for BuyerCode

	public void setBuyerCodesOnLines(Invoice invoice, BaseVector invLineItems) {

			/*PurchaseOrder order = invoice.getOrder();
			MasterAgreement ma = invoice.getMasterAgreement();


			BaseVector procureLineItems = null;
			ProcureLineItem pli = null;
			for (int i = 0; invLineItems != null && i < invLineItems.size(); i++) {
				invLineItem = (InvoiceLineItem) invLineItems.get(i);
				ProcureLineType plt = invLineItem.getLineType();

						if (invLineItem.getOrderLineItem() != null) {
								pli = invLineItem.getOrderLineItem();
								//  Issue 850 -- Copying buyer code value from MS or Order to Invocie Line item
								ClusterRoot buyerCodePO = (ClusterRoot) pli.getFieldValue("BuyerCode");
								if (buyerCodePO != null){
									Log.customer.debug("%s ::: ##### buyerCode ##### %s", ClassName, buyerCodePO);
								invLineItem.setFieldValue("BuyerCode",buyerCodePO );
								config.java.common.Log.customCATLog.debug("The value for BuyerCode (After setting on invoice): " + invLineItem.getFieldValue("BuyerCode"));
								}
									if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Getting the procure line item from the order %s", ClassName, pli);
							}
							else if (invLineItem.getMALineItem() != null) {
								pli = invLineItem.getMALineItem();
								//  Issue 850
								ClusterRoot buyerCodePO = (ClusterRoot) pli.getFieldValue("BuyerCode");
							    if (buyerCodePO != null){
										Log.customer.debug("%s ::: ##### buyerCode ##### %s", ClassName, buyerCodePO);
								        invLineItem.setFieldValue("BuyerCode",buyerCodePO );
							        	config.java.common.Log.customCATLog.debug("The value for BuyerCode (After setting on invoice): " + invLineItem.getFieldValue("BuyerCode"));
								}
								if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Getting the procure line item from the master agreement %s", ClassName, pli);
							}

				//****************************************
} */
InvoiceLineItem invLineItem = null;
Invoice inv = null;
	for (int i = 0; invLineItems != null && i < invLineItems.size(); i++) {
				invLineItem = (InvoiceLineItem) invLineItems.get(i);
					Log.customer.debug("setBuyerCodesOnLines invLineItem" +invLineItem );
            StringBuffer bcUnique = null;
            ClusterRoot acctType = (ClusterRoot)invLineItem.getFieldValue("AccountType");

          SplitAccountingCollection sac = invLineItem.getAccountings();
		                                if (sac != null) {
											Log.customer.debug("setBuyerCodesOnLines sac" +sac );
		                                        List sacList = sac.getSplitAccountings();
		                                        Log.customer.debug("setBuyerCodesOnLines saList" +sacList );
		                                        int sacSize = sacList.size();
		                                        for (int j = 0; j < sacSize; j++) {
                                    SplitAccounting sa = (SplitAccounting) sacList.get(j);
                     String facUN = (String)sa.getFieldValue("Facility.UniqueName");
                   if (facUN != null) {
					   Log.customer.debug("setBuyerCodesOnLines facUN" +facUN );
                    if (acctType != null && acctType.getUniqueName().equals("Capital")) {
                        bcUnique = new StringBuffer(CapitalBC).append(facUN);
                        Log.customer.debug("setBuyerCodesOnLines bcUnique" +bcUnique );
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (1) Set for Capital", ClassName);
                    }
                    else if (facUN.equals("NA")) {
                        bcUnique = new StringBuffer(NA_BC).append(facUN);
                        Log.customer.debug("setBuyerCodesOnLines bcUnique" +bcUnique );
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (2) Set for NA Site", ClassName);
                    }
                    else if (facUN.equals("DX")) {
                        bcUnique = new StringBuffer(NonDX_BC).append(facUN);
                        Log.customer.debug("setBuyerCodesOnLines bcUnique" + bcUnique );
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (3) Set for MX/MY Site" ,ClassName);
                    }
                    else {  // must mean Revenue and DX Site
                        CommodityExportMapEntry ceme = invLineItem.getCommodityExportMapEntry();
                        if (ceme != null) {
							Log.customer.debug("setBuyerCodesOnLines ceme" + ceme );
                            String bcPrefix = (String)ceme.getFieldValue("BuyerCodePrefix");
                            if (!StringUtil.nullOrEmptyOrBlankString(bcPrefix)) {
								Log.customer.debug("setBuyerCodesOnLines bcPrefix" + bcPrefix );
                                bcUnique = new StringBuffer(bcPrefix).append(facUN);
                                if (CatConstants.DEBUG)
                                    Log.customer.debug("%s *** (4) Set for CEME" );
                            }
                        }
                    }
            }
		}
		}
            if (bcUnique != null) {
                    ClusterRoot buyercode = Base.getService().objectMatchingUniqueName(BC_CLASS,invLineItem.getPartition(),bcUnique.toString());
                    if (buyercode != null)
                    Log.customer.debug("setBuyerCodesOnLines buyercode" + buyercode );
                        invLineItem.setFieldValue("BuyerCode",buyercode);
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s *** bcUnique: %s, BuyerCode obj: %s",
                                ClassName, bcUnique.toString(),buyercode);
            }
        }

	}// end
    }

