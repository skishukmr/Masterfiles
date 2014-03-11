/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1	Chandra	    09/10/2007		Issue 217 - Tax to be assessed for Contracts (w/o release) same as tax call done for orders
2.  Kingshuk    10/12/2007      Issue 708 - Duplicate lines on IR fixed
3	Madahvan	01/12/2007		Issue 748 - Getting line item details from MA instead of Invoice.( if IR is not against an Order)
4	F.Al-Nouri	03/27/2008		Modified this file to override the split accounting's accounting
								distribution in the case of a supplier entering accounting
								distribution on the ASN for contracts with no release
								In addition to the customization on the ASN other files
								affected by this change are:
								config/variants/vcsv1/extensions/CatCSVInvoiceEntryExt.aml
								config/java/invoicing/CatCXMLInvoiceCreator.java
								For more details please review the design doc:
								Caterpillar Invoicing Enhancement Design.doc

4. Sudheer k Jain  08/27/08  Issue 850 --Copying buyer code value from MS or Order to Invocie Line item
6. Nandini Bheemaiah 20/01/12  Issue 229 : Modified the file to override the coding done earlier by F.Al-Nouri
                               As per the new functionality for contract with no release, copy contract accounting
							   by default always ti invoice that is loaded thru ASN. Copy ASN accounting to invoice 
							   only when Contract accounting is empty.
***************************************************************************************** */

package config.java.invoicing.vcsv1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.LineItem;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;
import ariba.common.core.SplitAccounting;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.DirectOrder;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.tax.core.TaxDetail;
import ariba.util.core.Date;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import config.java.invoicing.CatInvoiceReconciliationMethod;

public class CatCSVInvoiceReconciliationMethod extends CatInvoiceReconciliationMethod {
	public static final String ClassName = "CatCSVInvoiceReconciliationMethod";
	private static String GoodPORefDate = ResourceService.getString("aml.cat.Invoice","DateForGoodPOReferences");

	protected List createInvoiceReconciliations(Invoice invoice) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the createInvoiceReconciliations method", ClassName);

		Partition partition = Base.getSession().getPartition();
		BaseVector invLineItems = invoice.getLineItems();
		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;
		int invLoadingCat = invoice.getLoadedFrom();

		for (int i = 0; i < invLineItems.size(); i++) {
			Log.customer.debug("%s ::: The invoice line number " + (i + 1) + " is " + invLineItems.get(i), ClassName);
			Log.customer.debug(
				"%s ::: The order line item linked to the invoice line is " + ((InvoiceLineItem) invLineItems.get(i)).getFieldValue("OrderLineItem"),
				ClassName);
		}

		if (!invoice.isCreditMemo() && !invoice.isDebitMemo()) {
			if ((invLoadingCat == Invoice.LoadedFromACSN) || (invLoadingCat == Invoice.LoadedFromFile)) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Processing the invoice loaded from ASN or File", ClassName);
				processEdiAndAsnInvoices(invoice);
			}

			Supplier invSupplier = invoice.getSupplier();
			SupplierLocation invSuppLocation = invoice.getSupplierLocation();
			ReceivableLineItemCollection appr = null;
			appr = invoice.getOrder();
			if (appr == null) appr = invoice.getMasterAgreement();

			if(invoice != null)
			    {
					Log.customer.debug("%s ::: Setting BuyerCode on Invoice", ClassName);
					setBuyerCodesOnLines(invoice, invLineItems);
			    }


			if (invoice.getLoadedFrom() == Invoice.LoadedFromACSN || invoice.getLoadedFrom() == Invoice.LoadedFromFile) {
				setCAPSChargeCodesOnLines(invoice, invLineItems);
				/**	Beginning of code by F.Al-Nouri */
				if (invoice.getMasterAgreement() != null) {
					setSupplierAccountingDistributionOnSplitAccounting(invLineItems);
				}
				/**	End of code by F.Al-Nouri */
			}


			//calling the set tax for either ma or order - issue 217
			if (appr != null) setTaxValuesForInvAgnstPO(invoice, appr, invLineItems);

			//if (order != null) {
			//	setTaxValuesForInvAgnstPO(invoice, order, invLineItems);
			//}
			//else if (ma != null) {
			//	setTaxValuesForInvAgnstContract(invoice, ma, invLineItems);
			//}


			//Moved as was causing null pointer issue when populating tax data for Dell invoices
			/*
			if (invoice.getLoadedFrom() == Invoice.LoadedFromACSN || invoice.getLoadedFrom() == Invoice.LoadedFromFile) {
				setCAPSChargeCodesOnLines(invoice, invLineItems);
			}
			*/

			if (invLoadingCat == Invoice.LoadedFromACSN || invLoadingCat == Invoice.LoadedFromFile || invLoadingCat == Invoice.LoadedFromUI) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: The invoice is loaded from ASN or UI or File", ClassName);

				int additionalAC = 0;
				for (int i = 0; i < invLineItems.size(); i++) {
					invLineItem = (InvoiceLineItem) invLineItems.get(i);

					if (invLineItem.getLineType() != null){
						if ((invLineItem.getLineType().getCategory() == ProcureLineType.FreightChargeCategory)
							|| (invLineItem.getLineType().getCategory() == ProcureLineType.DiscountCategory)
							|| (invLineItem.getLineType().getCategory() == ProcureLineType.HandlingChargeCategory)) {
							additionalAC = additionalAC + 1;
						}
					}
				}

				if (additionalAC == 0) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Reordering the line items on the invoice", ClassName);

					List lineItems = (List) invoice.getFieldValue("LineItems");
					List orderedLineItems = reorderINVLineItems(lineItems);
					//BaseVector newlines = new BaseVector();
					//newlines.addAll(orderedLineItems);

					try{
						//Log.customer.debug("The clusterroot for the lineitems is - " + invLineItems.getClusterRoot());
						//invoice = (Invoice) invLineItems.getClusterRoot();
						//ClusterRoot cr = (Invoice) invLineItems.getClusterRoot();
						invoice.setFieldValue("LineItems", orderedLineItems);
					}
					catch(Exception e){
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Exception caused at time of setting reordered lines hence lines not reordered", ClassName);
					}
					//invoice.setDottedFieldValue("LineItems", newlines);
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Setting the lineitems field to the new ordered lines vector", ClassName);
				}
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Will be saving the Invoice now", ClassName);
		invoice.save();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Done saving the Invoice, will call the super method", ClassName);
		return super.createInvoiceReconciliations(invoice);
	}

	public void processEdiAndAsnInvoices(Invoice invoice) {
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the processEdiAndAsnInvoices method", ClassName);
		int invLoadingCat = invoice.getLoadedFrom();
		boolean goodPORef = false;

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: The invoice is loaded from category %s", ClassName, invLoadingCat);
		BaseVector invLineItems = (BaseVector) invoice.getLineItems();
		SupplierLocation suppLoc = invoice.getSupplierLocation();

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the TermsDiscount on the Invoice Object", ClassName);
		if (suppLoc != null) {
			String discPercent = (String) suppLoc.getFieldValue("DiscountPercent");
			if ((suppLoc.getFieldValue("DiscountPercent") != null) && (!StringUtil.nullOrEmptyOrBlankString("discPercent"))) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: The DiscountPercent on the Supp Loc is not null: %s", ClassName, discPercent);
				invoice.setFieldValue("TermsDiscount", new BigDecimal(discPercent));
			}
			else {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: The DiscountPercent on the Supp Loc is null, hence setting to 0.00", ClassName);
				invoice.setFieldValue("TermsDiscount", new BigDecimal("0.00"));
			}
		}

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the BlockStampDate to %s on the Invoice Object", ClassName, Date.getNow().toString());
		invoice.setFieldValue("BlockStampDate", Date.getNow());
/*
		Date currentDate = Date.getNow().makeCalendarDate();
		int currentYear = Date.getYear(currentDate);
		int currentMonth = Date.getMonth(currentDate);
		int currentDay = Date.getDayOfMonth(currentDate);

		if (Log.invoiceLoading.debugOn){
			Log.invoiceLoading.debug("%s ::: Setting the BlockStampDate to %s on the Invoice Object", ClassName, Date.getNow().toString());
			Log.invoiceLoading.debug("Current Date is " + currentMonth + "/" + currentDay + "/" + currentYear);
		}
		if (currentMonth==0 && (currentDay==1 || currentDay==2 || currentDay==3)){
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Current Day is in Jan");
			}
			Date dateToInsert = new Date(Date.getYear(Date.getNow())-1, 12, 28+currentDay);
			dateToInsert = dateToInsert.makeCalendarDate();
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Date being inserted into BlockStampDate is: %s" + dateToInsert.toString());
			}
			invoice.setFieldValue("BlockStampDate", dateToInsert);
		}
		else if ((currentMonth==1 || currentMonth==3 || currentMonth==5 || currentMonth==7 || currentMonth==8 || currentMonth==10) &&
			(currentDay==1 || currentDay==2 || currentDay==3)){
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Current Day is in Feb, Apr, Jun, Aug, Sept, Nov");
			}
			Date dateToInsert = new Date(Date.getYear(Date.getNow()), currentMonth-1, 28+currentDay);
			dateToInsert = dateToInsert.makeCalendarDate();
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Date being inserted into BlockStampDate is: %s" + dateToInsert.toString());
			}
			invoice.setFieldValue("BlockStampDate", dateToInsert);
		}
		else if ((currentMonth==4 || currentMonth==6 || currentMonth==9 || currentMonth==11) &&
			(currentDay==1 || currentDay==2 || currentDay==3)){
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Current Day is in May, Jul, Oct, Dec");
			}
			Date dateToInsert = new Date(Date.getYear(Date.getNow()), currentMonth-1, 27+currentDay);
			dateToInsert = dateToInsert.makeCalendarDate();
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Date being inserted into BlockStampDate is: %s" + dateToInsert.toString());
			}
			invoice.setFieldValue("BlockStampDate", dateToInsert);
		}
		else if ((currentMonth==2) && (currentDay==1 || currentDay==2 || currentDay==3)){
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Current Day is in Mar");
			}
			Date dateToInsert = new Date(Date.getYear(Date.getNow()), currentMonth-1, 25+currentDay);
			dateToInsert = dateToInsert.makeCalendarDate();
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Date being inserted into BlockStampDate is: %s" + dateToInsert.toString());
			}
			invoice.setFieldValue("BlockStampDate", dateToInsert);
		}
		else{
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Current Day is a normal no special day");
			}
			Date dateToInsert = new Date(Date.getYear(Date.getNow()), Date.getMonth(Date.getNow()), Date.getDayOfMonth(Date.getNow())-3);
			dateToInsert = dateToInsert.makeCalendarDate();
			if (Log.invoiceLoading.debugOn){
				Log.invoiceLoading.debug("Date being inserted into BlockStampDate is: %s" + dateToInsert.toString());
			}
			invoice.setFieldValue("BlockStampDate", dateToInsert);
		}
*/
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the SettlementCode on the Invoice Object", ClassName);
		ClusterRoot settlementCode = null;
		ClusterRoot settlementCodeFromOrder = null;

		if (invoice.getOrder() != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Setting the SettlementCode from the order", ClassName);
			PurchaseOrder order = invoice.getOrder();
			try{
				if (order != null){
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Order is %s", ClassName, order);
					settlementCodeFromOrder = (ClusterRoot) order.getFieldValue("SettlementCode");
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Order Line is %s", ClassName, order.getLineItem(1));

                        // S. Sato - Ariba - Fix for Defect 362
                        // settlementCode = (ClusterRoot) order.getLineItem(1).getFieldValue("SettlementCode");
                    List lineItems = order.getLineItems();
                    if (!lineItems.isEmpty()) {
                        LineItem li = (LineItem) lineItems.get(0);
                        settlementCode = (ClusterRoot) li.getFieldValue("SettlementCode");
                    }
				}
			}
			catch (Exception ge){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,ge);
			}

			String [] date = StringUtil.delimitedStringToArray(GoodPORefDate,',');
			try {
				Date goodPORefDate = new Date(Integer.parseInt(date[0]), Integer.parseInt(date[1]),
						Integer.parseInt(date[2]));
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: goodPORefDate: %s",ClassName, goodPORefDate);
				Date orderedDate = invoice.getOrder().getOrderedDate();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Order Ordered Date: %s",ClassName, orderedDate);
				if (goodPORefDate.before(orderedDate))
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Setting goodPORef to true as a new order with valid references",ClassName);
					goodPORef = true;
			}
			catch (ArrayIndexOutOfBoundsException aoe) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,aoe);
			}
			catch (NumberFormatException nfe) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,nfe);
			}
			catch (Exception ge){
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Exception : %s",ClassName,ge);
			}
		}
		else if (invoice.getMasterAgreement() != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Setting the SettlementCode from the master agreement", ClassName);

                // S. Sato - Ariba - Fix for Defect 362
                // settlementCode = (ClusterRoot) invoice.getMasterAgreement().getLineItem(1).getFieldValue("SettlementCode");
            List lineItems = invoice.getMasterAgreement().getLineItems();
            if (!lineItems.isEmpty()) {
                LineItem li = (LineItem) lineItems.get(0);
                settlementCode = (ClusterRoot) li.getFieldValue("SettlementCode");
            }
		}
		if (settlementCode != null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Settlement Code %s will be set on the invoice object", ClassName, settlementCode.getUniqueName());
			invoice.setFieldValue("SettlementCode", settlementCode);
		}
		else if (settlementCodeFromOrder != null){
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Settlement Code from Order %s will be set on the invoice object", ClassName, settlementCodeFromOrder.getUniqueName());
			invoice.setFieldValue("SettlementCode", settlementCodeFromOrder);
		}

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Setting the CapsChargeCode and RefLineNumber on inv lines from the linked order lines", ClassName);
		for (int i = 0; i < invLineItems.size(); i++) {
			InvoiceLineItem invLine = (InvoiceLineItem) invLineItems.get(i);
			ProcureLineItem pli = null;

//			if ((invLine.getLineType().getCategory() == ProcureLineType.TaxChargeCategory) && (invLine.getDottedFieldValue("Description.UnitOfMeasure") == null)){
			if (invLine.getLineType() != null && invLine.getLineType().getCategory() == ProcureLineType.TaxChargeCategory){
				LineItemProductDescription lipd = invLine.getDescription();
				if (lipd.getUnitOfMeasure() == null){
					UnitOfMeasure uom = UnitOfMeasure.lookupByUniqueName("EA", invoice.getPartition());
					lipd.setUnitOfMeasure(uom);
					invLine.setDescription(lipd);
				}
			}

			if (invLine.getLineType() != null && invLine.getLineType().getCategory() == ProcureLineType.HandlingChargeCategory){
				invLine.setLineType(ProcureLineType.lookupByUniqueName("SpecialCharge", invLine.getPartition()));
			}

			if (invLine.getOrderLineItem() != null) {
				pli = invLine.getOrderLineItem();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Getting the procure line item from the order %s", ClassName, pli);
			}
			else if (invLine.getMALineItem() != null) {
				pli = invLine.getMALineItem();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Getting the procure line item from the master agreement %s", ClassName, pli);
			}

			if (pli != null) {
				//if (Log.customer.debugOn) {
					Log.customer.debug("%s ::: The CapsChargeCode on the pli is %s", ClassName, pli.getDescription().getFieldValue("CAPSChargeCode.UniqueName"));
					Log.customer.debug("%s ::: The CapsChargeCode on the pli is %s", ClassName, pli.getDescription().getFieldValue("CAPSChargeCode"));
					Log.customer.debug(
						"%s ::: The Reference Line Number on the pli is %s",
						ClassName,
						(Integer) pli.getFieldValue("ReferenceLineNumber"));
				//}
				config.java.common.Log.customCATLog.debug("\n\n\n");
				config.java.common.Log.customCATLog.debug(invoice.getUniqueName() + " Before the values are set");
				config.java.common.Log.customCATLog.debug("The value for CapsChargeCode (before): " + invLine.getDottedFieldValue("CapsChargeCode"));
				config.java.common.Log.customCATLog.debug("The value for Description.CAPSChargeCode (before): " + invLine.getDottedFieldValue("Description.CAPSChargeCode"));
				config.java.common.Log.customCATLog.debug("The value for Description.CAPSChargeCodeID (before): " + invLine.getDottedFieldValue("Description.CAPSChargeCodeID"));

				invLine.setDottedFieldValue("CapsChargeCode", (ClusterRoot) pli.getDescription().getFieldValue("CAPSChargeCode"));
				invLine.setDottedFieldValue("Description.CAPSChargeCode", (ClusterRoot) pli.getDescription().getFieldValue("CAPSChargeCode"));
				invLine.setDottedFieldValue("Description.CAPSChargeCodeID", (String) pli.getDescription().getFieldValue("CAPSChargeCode.UniqueName"));
				//invLine.setDottedFieldValue("ReferenceLineNumber", (Integer) pli.getFieldValue("ReferenceLineNumber"));

// This is a temporary test for fetching the correct reference line number
				if (pli instanceof POLineItem) {
					if (!goodPORef) {
						Log.customer.debug(
							"%s ::: Old Reference Num on the pli is %s",
							ClassName,
							(Integer) pli.getFieldValue("ReferenceLineNumber"));
						PurchaseOrder po = (PurchaseOrder) pli.getLineItemCollection();
						Log.customer.debug("%s ::: Added For Reference : PO is %s",	ClassName,po);
						Requisition req = (Requisition) ((POLineItem) pli).getRequisition();
						Log.customer.debug("%s ::: Added For Reference : Req is %s",	ClassName,req);
						Log.customer.debug("%s ::: Added For Reference : numberonreq is %s",	ClassName,(((POLineItem) pli).getFieldValue("NumberOnReq")));
						ReqLineItem rli1 = (ReqLineItem) req.getLineItem(((Integer) ((POLineItem) pli).getFieldValue("NumberOnReq")).intValue());
						Log.customer.debug("%s ::: Added For Reference : rli1 is %s",	ClassName,rli1);
						Log.customer.debug("%s ::: Added For Reference : ReferenceNumber is %s",	ClassName,((Integer) rli1.getFieldValue("ReferenceLineNumber")).intValue());
						ReqLineItem rli2 = (ReqLineItem) req.getLineItem(((Integer) rli1.getFieldValue("ReferenceLineNumber")).intValue());
						Log.customer.debug("%s ::: Added For Reference : rli2 is %s",	ClassName,rli2);
						POLineItem poli = (POLineItem) po.getLineItem(((Integer) rli2.getFieldValue("NumberOnPO")).intValue());
						Log.customer.debug("%s ::: New Reference Num is %s", ClassName, new Integer(poli.getNumberInCollection()));
						invLine.setFieldValue("ReferenceLineNumber", new Integer(poli.getNumberInCollection()));
					}
					else{
						Log.customer.debug("%s ::: Added For Reference : not goodPORef Reference  is %s",	ClassName,(Integer) pli.getFieldValue("ReferenceLineNumber"));
						invLine.setDottedFieldValue("ReferenceLineNumber", (Integer) pli.getFieldValue("ReferenceLineNumber"));
						Log.customer.debug("%s ::: Not goodPORef Added For Reference : end ",	ClassName);
					}
				}
				if (pli instanceof ContractLineItem) {
					Log.customer.debug("%s ::: MALineItem Added For Reference :reference is %s ",	ClassName,pli.getFieldValue("ReferenceLineNumber"));
					invLine.setFieldValue("ReferenceLineNumber", (Integer) pli.getFieldValue("ReferenceLineNumber"));
					Log.customer.debug("%s ::: MLineItem Added For Reference : end ",	ClassName);
				}

				config.java.common.Log.customCATLog.debug(invoice.getUniqueName() + "After the values are set");
				config.java.common.Log.customCATLog.debug("The value for CapsChargeCode (after): " + invLine.getDottedFieldValue("CapsChargeCode"));
				config.java.common.Log.customCATLog.debug("The value for Description.CAPSChargeCode (after): " + invLine.getDottedFieldValue("Description.CAPSChargeCode"));
				config.java.common.Log.customCATLog.debug("The value for Description.CAPSChargeCodeID (after): " + invLine.getDottedFieldValue("Description.CAPSChargeCodeID"));
				config.java.common.Log.customCATLog.debug("\n\n\n");
			}
			else {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Could not find a pli to match to the invoice line", ClassName);
			}
		}
	}

	public void setTaxValuesForInvAgnstPO(Invoice invoice,ReceivableLineItemCollection appr, BaseVector invLineItems) {
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering the setTaxValuesForInvAgnstPO method", ClassName);
			Log.customer.debug("%s ::: The invoice passed into the method is %s", ClassName, invoice);
			Log.customer.debug("%s ::: The order or ma passed into the method is %s", ClassName, appr);
			Log.customer.debug("%s ::: The invLineItems basevector passed into the method is %s", ClassName, invLineItems);
		//}
		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;
		Address shipToAddress = null;
		boolean taxServiceUnAvlbOnPO = false;
		DirectOrder order = null;
		Contract ma = null;

		if (appr instanceof DirectOrder) order = (DirectOrder) appr;
		else if (appr instanceof Contract) ma = (Contract) appr;

		String AssessTaxMessageOnPO = null;
		AssessTaxMessageOnPO = (order !=null)?(String) order.getFieldValue("AssessTaxMessage"):(String) ma.getFieldValue("AssessTaxMessage");

		String taxModuleFailedString = ResourceService.getString("cat.java.vcsv1", "Error_AssessTaxWebServiceUnavailable");

		Supplier invSupplier = invoice.getSupplier();
		SupplierLocation invSuppLocation = invoice.getSupplierLocation();

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Checking to see if the AssessTaxMessage is populated on the order", ClassName);
		if (AssessTaxMessageOnPO != null && AssessTaxMessageOnPO.indexOf(taxModuleFailedString) > 0) {
			//Error encountered in Tax Web Service Call at PO Time hence will be assessed at Invoice Time
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Found an error was encountered by the tax web service at PO time", ClassName);
			taxServiceUnAvlbOnPO = true;
			//invoice.setDottedFieldValue("taxCallNotFailed", new Boolean("false"));
		}

		for (int i = 0; i < invLineItems.size(); i++) {
			String CapsCCUN = null;
			invLineItem = (InvoiceLineItem) invLineItems.get(i);
			invLineItem.setSupplier(invSupplier);
			invLineItem.setSupplierLocation(invSuppLocation);

			if(order != null) {
				pli = invLineItem.getOrderLineItem();
			} else {
				pli = invLineItem.getMALineItem();
			}

			if (shipToAddress == null){
				// Added pli != null for preventing the failure in IR creation as the pli is not
				// linked to the IR Invoice Line Item
				if (pli != null && pli.getShipTo() != null){
					shipToAddress = pli.getShipTo();
				}
			}
			invLineItem.setShipTo(shipToAddress);

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: The order line item on the inv line item is %s", ClassName, pli);

			if (invLineItem.getDottedFieldValue("CapsChargeCode") != null) {
				CapsCCUN = (String) invLineItem.getDottedFieldValue("CapsChargeCode.UniqueName");
			}

			//if (Log.customer.debugOn) {
				Log.customer.debug("%s ::: Setting the boolean fields on the invoice line", ClassName);
				Log.customer.debug("%s ::: The value of taxServiceUnAvlbOnPO is: " + taxServiceUnAvlbOnPO, ClassName);
				Log.customer.debug("%s ::: The value of CapsCCUN is: " + CapsCCUN, ClassName);
			//}
			//Code for Dell Freight Charges Exception
			if ( (!taxServiceUnAvlbOnPO && CapsCCUN != null) && ("001".equals(CapsCCUN) || ("019".equals(CapsCCUN) && ( "FreightCharge".equals( (String)invLineItem.getDottedFieldValue("LineType.UniqueName") ) ) ) ) ) {
			//if (!taxServiceUnAvlbOnPO && CapsCCUN != null && "001".equals(CapsCCUN)) {
				invLineItem.setDottedFieldValue("TaxCodeOverride", new Boolean(true));
				invLineItem.setDottedFieldValue("TaxAllFieldsOverride", new Boolean(true));
			}
			else {
				invLineItem.setDottedFieldValue("TaxCodeOverride", new Boolean(false));
				invLineItem.setDottedFieldValue("TaxAllFieldsOverride", new Boolean(false));
			}

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Setting the line level fields on the invoice line", ClassName);
			if (pli != null) {
				invLineItem.setFieldValue("TaxCode", (ClusterRoot) pli.getFieldValue("TaxCode"));
				invLineItem.setFieldValue("TaxUse", (ClusterRoot) pli.getFieldValue("TaxUse"));
				invLineItem.setFieldValue("TaxRate", (BigDecimal) pli.getFieldValue("TaxRate"));
				invLineItem.setFieldValue("TaxBase", (BigDecimal) pli.getFieldValue("TaxBase"));
				invLineItem.setFieldValue("TaxState", (ClusterRoot) pli.getFieldValue("TaxState"));
				invLineItem.setFieldValue("TaxQualifier", (String) pli.getFieldValue("TaxQualifier"));
				invLineItem.setFieldValue("TaxCodeMessage", (String) pli.getFieldValue("TaxCodeMessage"));
			}
			else {
				Integer refNum = (Integer) invLineItem.getFieldValue("ReferenceLineNumber");
				if (refNum != null && refNum.intValue() != 0) {
					InvoiceLineItem invLIRef = (InvoiceLineItem) invLineItems.get(refNum.intValue() - 1);
					// Added pli != null for preventing the failure in IR creation as the pli is not
					// linked to the IR Invoice Line Item
					if(invLIRef != null){
						invLineItem.setFieldValue("TaxCode", (ClusterRoot) invLIRef.getFieldValue("TaxCode"));
						invLineItem.setFieldValue("TaxUse", (ClusterRoot) invLIRef.getFieldValue("TaxUse"));
						invLineItem.setFieldValue("TaxRate", (BigDecimal) invLIRef.getFieldValue("TaxRate"));
						invLineItem.setFieldValue("TaxBase", (BigDecimal) invLIRef.getFieldValue("TaxBase"));
						invLineItem.setFieldValue("TaxState", (ClusterRoot) invLIRef.getFieldValue("TaxState"));
						invLineItem.setFieldValue("TaxQualifier", (String) invLIRef.getFieldValue("TaxQualifier"));
						invLineItem.setFieldValue("TaxCodeMessage", (String) invLIRef.getFieldValue("TaxCodeMessage"));
					}
				}
				//Code for Dell Freight Charges Exception
//				/*
				else if (refNum != null && refNum.intValue() == 0 && order != null)
				{
					if ( "FreightCharge".equals( (String)invLineItem.getDottedFieldValue("LineType.UniqueName") ) && ( "019".equals(CapsCCUN) ) )
					{
						ProcureLineItem firstMatLineOnInv = getFirstMatLineOnPO(invoice);
						if (firstMatLineOnInv != null)
						{
							invLineItem.setFieldValue("TaxCode", (ClusterRoot) firstMatLineOnInv.getFieldValue("TaxCode"));
							invLineItem.setFieldValue("TaxUse", (ClusterRoot) firstMatLineOnInv.getFieldValue("TaxUse"));
							invLineItem.setFieldValue("TaxRate", (BigDecimal) firstMatLineOnInv.getFieldValue("TaxRate"));
							invLineItem.setFieldValue("TaxBase", (BigDecimal) firstMatLineOnInv.getFieldValue("TaxBase"));
							invLineItem.setFieldValue("TaxState", (ClusterRoot) firstMatLineOnInv.getFieldValue("TaxState"));
							invLineItem.setFieldValue("TaxQualifier", (String) firstMatLineOnInv.getFieldValue("TaxQualifier"));
							invLineItem.setFieldValue("TaxCodeMessage", (String) firstMatLineOnInv.getFieldValue("TaxCodeMessage"));
						}
					}
				}
//				*/
			}
		}
	}

	public ProcureLineItem getFirstMatLineOnPO(Invoice inv)
	{
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: In method getFirstMatLineOnPO", ClassName);
		ProcureLineItemCollection plic = inv.getOrder();
		if (plic == null) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Got NULL Order ", ClassName);
			//issue 748	 - using MA
			//plic = inv;
			plic = inv.getMasterAgreement();
		}

		if (plic != null) {
			BaseVector pLineItems = (BaseVector) plic.getLineItems();
			ProcureLineItem pli = null;
			ClusterRoot capsChargeCodeObj = null;
			boolean foundMatLine = false;

			for (int i = 0;(i < pLineItems.size() ) && (!foundMatLine); i++) {
				pli = (ProcureLineItem) pLineItems.get(i);
				capsChargeCodeObj = (ClusterRoot) pli.getDescription().getFieldValue("CAPSChargeCode");
				String capsChargeCodeString = null;
				if (capsChargeCodeObj != null) {
					capsChargeCodeString = capsChargeCodeObj.getUniqueName();
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: CAPS Charge Code is: %s", ClassName, capsChargeCodeString);
				}
				else {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Encountered a null CAPS Charge Code", ClassName);
					capsChargeCodeString = "";
				}

				if (capsChargeCodeString.equals("001")) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Found the First material line: %s", ClassName, pli.toString());
					foundMatLine = true;
				}
			}
			return pli;
		}
		return null;
	}

	public void setTaxValuesForInvAgnstContract(Invoice invoice, Contract ma, BaseVector invLineItems) {
		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;

		Supplier invSupplier = invoice.getSupplier();
		SupplierLocation invSuppLocation = invoice.getSupplierLocation();

		for (int i = 0; i < invLineItems.size(); i++) {
			String CapsCCUN = null;
			invLineItem = (InvoiceLineItem) invLineItems.get(i);
			invLineItem.setSupplier(invSupplier);
			invLineItem.setSupplierLocation(invSuppLocation);

			pli = invLineItem.getMALineItem();

			if (invLineItem.getDottedFieldValue("CapsChargeCode") != null) {
				CapsCCUN = (String) invLineItem.getDottedFieldValue("CapsChargeCode.UniqueName");
			}

			invLineItem.setDottedFieldValue("TaxAllFieldsOverride", new Boolean(false));
			// Added pli != null for preventing the failure in IR creation as the pli is not
			// linked to the IR Invoice Line Item
			if (pli != null && CapsCCUN != null && "001".equals(CapsCCUN) && (pli.getFieldValue("TaxCode") != null)) {
				invLineItem.setDottedFieldValue("TaxCodeOverride", new Boolean(true));
			}
			else {
				invLineItem.setDottedFieldValue("TaxCodeOverride", new Boolean(false));
			}

			if (pli != null) {
				invLineItem.setFieldValue("TaxCode", (ClusterRoot) pli.getFieldValue("TaxCode"));
				invLineItem.setFieldValue("TaxQualifier", (String) pli.getFieldValue("TaxQualifier"));
				if (pli.getFieldValue("TaxUse") != null)
					invLineItem.setFieldValue("TaxUse", (ClusterRoot) pli.getFieldValue("TaxUse"));
			}
			else {
				Integer refNum = (Integer) invLineItem.getFieldValue("ReferenceLineNumber");
				if (refNum != null && refNum.intValue() != 0) {
					InvoiceLineItem invLIRef = (InvoiceLineItem) invLineItems.get(refNum.intValue() - 1);
					// Added pli != null for preventing the failure in IR creation as the pli is not
					// linked to the IR Invoice Line Item
					if (invLIRef != null){
						invLineItem.setFieldValue("TaxCode", (ClusterRoot) invLIRef.getFieldValue("TaxCode"));
						invLineItem.setFieldValue("TaxQualifier", (String) invLIRef.getFieldValue("TaxQualifier"));
						if (invLIRef.getFieldValue("TaxUse") != null)
							invLineItem.setFieldValue("TaxUse", (ClusterRoot) invLIRef.getFieldValue("TaxUse"));
					}
				}
			}
		}
	}

	/**	Beginning of code by F.Al-Nouri */
	public void setSupplierAccountingDistributionOnSplitAccounting(BaseVector invLineItems) {
		InvoiceLineItem invLineItem = null;
		Boolean flag = false;

		for (int i = 0; i < invLineItems.size(); i++) {
			invLineItem = (InvoiceLineItem) invLineItems.get(i);
			flag = false; // To reset the value of flag to false for every new LineItem under check.

				Log.customer.debug("%s ::: Displaying Accounting distribution Before SetSuppAcctDist - Start", ClassName);
				for (int j = 0; j < invLineItem.getAccountings().getSplitAccountings().size(); j++) 
				{
					SplitAccounting sa2 = (SplitAccounting)invLineItem.getAccountings().getSplitAccountings().get(j);
					Log.customer.debug("%s ::: Displaying Accounting distribution - AccountingFacility: %s", ClassName, sa2.getFieldValue("AccountingFacility"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - Department: %s", ClassName, sa2.getFieldValue("Department"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - Division: %s", ClassName, sa2.getFieldValue("Division"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - Section: %s", ClassName, sa2.getFieldValue("Section"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - ExpenseAccount: %s", ClassName, sa2.getFieldValue("ExpenseAccount"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - Order: %s", ClassName, sa2.getFieldValue("Order"));
					Log.customer.debug("%s ::: Displaying Accounting distribution - Misc: %s", ClassName, sa2.getFieldValue("Misc"));
				 
					//Code Begins : Nandini : Issue 229
					// Code to check for null Accountings in the invoices or Contract. At this point the Accounting is defaulted from Contract.
					
					if((StringUtil.nullOrEmptyOrBlankString((String)(sa2.getFieldValue("AccountingFacility"))))||(StringUtil.nullOrEmptyOrBlankString((String)(sa2.getFieldValue("Department"))))||(StringUtil.nullOrEmptyOrBlankString((String)(sa2.getFieldValue("Division"))))||(StringUtil.nullOrEmptyOrBlankString((String)(sa2.getFieldValue("Section"))))||(StringUtil.nullOrEmptyOrBlankString((String)(sa2.getFieldValue("ExpenseAccount")))))
					{
					    flag = true;
					}
				
				}
				Log.customer.debug("%s ::: Displaying Accounting distribution Before SetSuppAcctDist - End", ClassName);
			
				if(flag == true)
				{
					Log.customer.debug("One or more fields in Account Distribution is empty.. Considering ASN Accounting now", ClassName);
			
				
						if ((!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistAccountingFacility")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistDepartment")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistDivision")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistSection")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistExpenseAccount")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistOrder")))
							|| (!StringUtil.nullOrEmptyOrBlankString((String)invLineItem.getFieldValue("SuppAcctDistMisc")))) {
			
							SplitAccounting sa = (SplitAccounting)invLineItem.getAccountings().getSplitAccountings().get(0);
							Log.customer.debug("%s ::: Setting ASN Accounting to Invoice as Contract Accountings are null", ClassName);
							sa.setFieldValue("AccountingFacility", invLineItem.getFieldValue("SuppAcctDistAccountingFacility"));
							sa.setFieldValue("Department", invLineItem.getFieldValue("SuppAcctDistDepartment"));
							sa.setFieldValue("Division", invLineItem.getFieldValue("SuppAcctDistDivision"));
							sa.setFieldValue("Section", invLineItem.getFieldValue("SuppAcctDistSection"));
							sa.setFieldValue("ExpenseAccount", invLineItem.getFieldValue("SuppAcctDistExpenseAccount"));
							sa.setFieldValue("Order", invLineItem.getFieldValue("SuppAcctDistOrder"));
							sa.setFieldValue("Misc", invLineItem.getFieldValue("SuppAcctDistMisc"));
			
							sa.setPercentage(invLineItem.getAccountings().getTotalPercentage());
							sa.setQuantity(invLineItem.getAccountings().getTotalQuantity());
							sa.setAmount(invLineItem.getAccountings().getTotalAmount());
			
							Log.customer.debug("%s ::: totals are set to first split accounting", ClassName);
			
							invLineItem.getAccountings().getSplitAccountings().clear();
							Log.customer.debug("%s :::: Removed all splits from the collection", ClassName);
			
							invLineItem.getAccountings().getSplitAccountings().add(0, sa);
							Log.customer.debug("%s ::: Added the new split accounting", ClassName);
			
							//if (Log.customer.debugOn) {
								Log.customer.debug("%s ::: Displaying Accounting distribution After SetSuppAcctDist - Start", ClassName);
								for (int j = 0; j < invLineItem.getAccountings().getSplitAccountings().size(); j++) {
									SplitAccounting sa2 = (SplitAccounting)invLineItem.getAccountings().getSplitAccountings().get(j);
									Log.customer.debug("%s ::: Displaying Accounting distribution - AccountingFacility: %s", ClassName, sa2.getFieldValue("AccountingFacility"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - Department: %s", ClassName, sa2.getFieldValue("Department"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - Division: %s", ClassName, sa2.getFieldValue("Division"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - Section: %s", ClassName, sa2.getFieldValue("Section"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - ExpenseAccount: %s", ClassName, sa2.getFieldValue("ExpenseAccount"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - Order: %s", ClassName, sa2.getFieldValue("Order"));
									Log.customer.debug("%s ::: Displaying Accounting distribution - Misc: %s", ClassName, sa2.getFieldValue("Misc"));
								}
								Log.customer.debug("%s ::: Displaying Accounting distribution After SetSuppAcctDist - End", ClassName);
							//}
						}
						
				}
				
		}//Code Ends : Nandini : Issue 229
	}
	/**	End of code by F.Al-Nouri */

	//Method Added by Deepak for BuyerCode

	public void setBuyerCodesOnLines(Invoice invoice, BaseVector invLineItems) {
			PurchaseOrder order = invoice.getOrder();
			Contract ma = invoice.getMasterAgreement();

			InvoiceLineItem invLineItem = null;
			BaseVector procureLineItems = null;
			ProcureLineItem pli = null;
			for (int i = 0; invLineItems != null && i < invLineItems.size(); i++) {
				invLineItem = (InvoiceLineItem) invLineItems.get(i);
				ProcureLineType plt = invLineItem.getLineType();

				//************************************Added By Deepak**********


							if (invLineItem.getOrderLineItem() != null) {
								pli = invLineItem.getOrderLineItem();
								//  Issue 850 -- Copying buyer code value from MS or Order to Invocie Line item
								ClusterRoot buyerCodePO = (ClusterRoot) pli.getFieldValue("BuyerCode");
								if (buyerCodePO != null){
									Log.customer.debug("%s ::: ##### buyerCode ##### %s", ClassName, buyerCodePO);
								invLineItem.setFieldValue("BuyerCode",buyerCodePO );
								config.java.common.Log.customCATLog.debug("The value for BuyerCode (After setting on invoice): " + invLineItem.getFieldValue("BuyerCode"));
								}
									//if (Log.customer.debugOn)
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
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Getting the procure line item from the master agreement %s", ClassName, pli);
							}

				//**************************************************************
}
}

	//*************************************
	public void setCAPSChargeCodesOnLines(Invoice invoice, BaseVector invLineItems) {
		PurchaseOrder order = invoice.getOrder();
		Contract ma = invoice.getMasterAgreement();

		InvoiceLineItem invLineItem = null;
		BaseVector procureLineItems = null;
		ProcureLineItem pli = null;
		for (int i = 0; invLineItems != null && i < invLineItems.size(); i++) {
			invLineItem = (InvoiceLineItem) invLineItems.get(i);
			ProcureLineType plt = invLineItem.getLineType();

			Log.customer.debug("%s ::: In setCAPSChargeCodesOnLines ProcureLineType="+plt, ClassName);
			//Some of the invoices have null line type - continue if null
			if (plt == null) {
				Log.customer.debug("%s ::: In setCAPSChargeCodesOnLines ProcureLineType is null (handled)", ClassName);
				continue;
			}

			if (plt.getCategory() == 2) {
				BaseVector taxDetails = invoice.getTaxDetails();
				String capsChargeCode = null;
				String procureLineType = null;

				if (taxDetails != null && taxDetails.size() != 0) {
					String taxType = ((TaxDetail) (taxDetails.get(0))).getCategory();

					if (taxType.equals("sales")) {
						capsChargeCode = "002";
						procureLineType = "SalesTaxCharge";
					}
					if (taxType.equals("usage")) {
						capsChargeCode = "003";
						procureLineType = "ServiceUseTax";
					}
					if ((taxType.equals("vat"))
						|| (taxType.equals("gst"))
						|| (taxType.equals("pst"))
						|| (taxType.equals("qst"))
						|| (taxType.equals("hst"))) {
						capsChargeCode = "096";
						procureLineType = "VATCharge";
					}
				}
				else {
					capsChargeCode = "002";
					procureLineType = "SalesTaxCharge";
				}
				ClusterRoot capsCCObj = null;
				if (capsChargeCode != null) {
					capsCCObj =
						Base.getService().objectMatchingUniqueName("cat.core.CAPSChargeCode", Base.getSession().getPartition(), capsChargeCode);
				}

				ProcureLineType procureLTObj = null;
				if (procureLineType != null) {
					procureLTObj = ProcureLineType.lookupByUniqueName(procureLineType, invoice.getPartition());
					//procureLTObj = Base.getService().objectMatchingUniqueName("ariba.procure.core.ProcureLineType",Base.getSession().getPartition(),procureLineType);
				}

				invLineItem.setDottedFieldValue("CapsChargeCode", capsCCObj);
				invLineItem.setDottedFieldValue("ReferenceLineNumber", new Integer(0));
				invLineItem.getDescription().setFieldValue("CAPSChargeCode",capsCCObj);
				invLineItem.getDescription().setFieldValue("CAPSChargeCodeID",capsChargeCode);
				invLineItem.setLineType(procureLTObj);
				//invLineItem.setDottedFieldValue("LineType",procureLTObj);
			}
			else if (plt.getCategory() == 16) {
				String capsChargeCode = null;
				String procureLineType = null;
				capsChargeCode = "007";
				procureLineType = "SpecialCharge";

				InvoiceLineItem parentInvLine = (InvoiceLineItem) invLineItem.getParent();
				if (parentInvLine != null) {
					Log.customer.debug("%s ::: setCAPSChargeCodesOnLines: Added For Reference : rli1 is %s",	ClassName,parentInvLine.getNumberInCollection());
					invLineItem.setDottedFieldValue("ReferenceLineNumber", new Integer(parentInvLine.getNumberInCollection()));
				}
				ClusterRoot capsCCObj =
					Base.getService().objectMatchingUniqueName("cat.core.CAPSChargeCode", Base.getSession().getPartition(), capsChargeCode);
				ProcureLineType procureLTObj = ProcureLineType.lookupByUniqueName(procureLineType, invoice.getPartition());
				invLineItem.setDottedFieldValue("CapsChargeCode", capsCCObj);
				invLineItem.setLineType(procureLTObj);
				//invLineItem.getDescription().setFieldValue("CAPSChargeCode",capsCCObj);
				//invLineItem.getDescription().setFieldValue("CAPSChargeCodeID","007");
			}
			//Code for Dell Freight Charges Exception
//			/*
			else if (plt.getCategory() == 4) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: In setCAPSChargeCodesOnLines populating CAPSChargeCode for FreightCharge...", ClassName);
				String capsChargeCode = null;
				capsChargeCode = "019";
				ClusterRoot capsCCObj =
									Base.getService().objectMatchingUniqueName("cat.core.CAPSChargeCode", Base.getSession().getPartition(), capsChargeCode);
				invLineItem.setDottedFieldValue("CapsChargeCode", capsCCObj);
				invLineItem.setDottedFieldValue("ReferenceLineNumber", new Integer(0));
			}
			/*
			else {
				if (order != null)
					pli = invLineItem.getOrderLineItem();
				else if (ma != null)
					pli = invLineItem.getMALineItem();

				if (pli != null) {
					invLineItem.setFieldValue("CapsChargeCode", (ClusterRoot) pli.getDescription().getFieldValue("CAPSChargeCode"));
					invLineItem.setFieldValue("ReferenceLineNumber", (Integer) pli.getFieldValue("ReferenceLineNumber"));
				}
			}
			*/
		}
	}

	public static List reorderINVLineItems(List lines) {
		List orderedLines = null;
		ArrayList materialLines = new ArrayList();
		ArrayList acLines = new ArrayList();
		ArrayList taxLines = new ArrayList();
		ArrayList unmatchedLines = new ArrayList();
		Integer refNumInt = null;
		Integer invoiceLineNumInt = null;

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering the reorderINVLineItems method", ClassName);

		if ((lines != null) && !lines.isEmpty()) {
			int lineCount = lines.size();
			for (int i = 0; i < lineCount; i++) {
				InvoiceLineItem inefli = (InvoiceLineItem) lines.get(i);
				refNumInt = (Integer) inefli.getFieldValue("ReferenceLineNumber");
				//invoiceLineNumInt = (Integer) inefli.getFieldValue("InvoiceLineNumber");

				if ((refNumInt != null) && (refNumInt.intValue() == inefli.getNumberInCollection())) {
					materialLines.add(inefli);
				}
				else if ((refNumInt != null) && (refNumInt.intValue() == 0)) {
					taxLines.add(inefli);
				}
				else {
					acLines.add(inefli);
				}
			}
			int txCount = taxLines.size();
			int mlCount = materialLines.size();
			int aclCount = acLines.size();
			Log.customer.debug("%s ::: Line Counts(Material/AC/Tax): " + mlCount + "/" + aclCount + "/" + txCount, ClassName);
			orderedLines = new ArrayList();
			if (mlCount > 0) {
				//Issue# 708
				for (int j = 0, acindex = 0; j < mlCount; j++) {
					InvoiceLineItem mLine = (InvoiceLineItem) materialLines.get(j);
					int currentOLSize = orderedLines.size();
					//if (Log.customer.debugOn)
						Log.customer.debug(
							"%s ::: Updated Material Ref Num From " + mLine.getDottedFieldValue("ReferenceLineNumber") + "to " + (currentOLSize + 1),
							ClassName);
					mLine.setFieldValue("ReferenceLineNumber", new Integer(currentOLSize + 1));
					orderedLines.add(mLine);
					if (aclCount > 0) {
						// Issue# 708
						for (int k = acindex; k < aclCount; k++) {
							InvoiceLineItem acLine = (InvoiceLineItem) acLines.get(k);
							refNumInt = (Integer) acLine.getFieldValue("ReferenceLineNumber");
							Log.customer.debug("%s ::: refNumInt: %s", ClassName, refNumInt);
							if (refNumInt != null){
								//if (Log.customer.debugOn)
									Log.customer.debug(
										"%s ::: Material Line NIC is: "
											+ mLine.getNumberInCollection()
											+ "compared to AC Ref Number: "
											+ refNumInt.intValue(),
										ClassName);
							}
							if ((refNumInt != null) && (refNumInt.intValue() == mLine.getNumberInCollection())) {
								//if (Log.customer.debugOn)
								// Issue# 708
									acindex = k + 1;
									Log.customer.debug(
										"%s ::: Updated AC Ref Num From "
											+ acLine.getDottedFieldValue("ReferenceLineNumber")
											+ "to "
											+ (currentOLSize + 1),
										ClassName);
								acLine.setFieldValue("ReferenceLineNumber", new Integer(currentOLSize + 1));
								orderedLines.add(acLine);
							}
							// DJS - Added this logic so as to load unmatched additional charge lines from ASN
							else{
								Log.customer.debug("%s ::: Adding line %s to the unmatchedLines List", ClassName, acLine);
								unmatchedLines.add(acLine);
							}
						}
					}
				}
			}
			else {
				return lines;
			}

			// DJS - Added this logic so as to load unmatched additional charge lines from ASN
			if (unmatchedLines.size() > 0){
				int uCount = unmatchedLines.size();
				for (int j = 0; j < uCount; j++) {
					InvoiceLineItem uLine = (InvoiceLineItem) unmatchedLines.get(j);
					if (!orderedLines.contains(uLine)){
						Log.customer.debug("%s ::: Adding line %s from the unmatchedLines List to the ordered lines", ClassName, uLine);
						orderedLines.add(uLine);
					}
				}
			}

			if (txCount > 0) {
				for (int m = 0; m < txCount; m++)
					orderedLines.add((InvoiceLineItem) taxLines.get(m));
			}
		}
		return orderedLines;
	}
}