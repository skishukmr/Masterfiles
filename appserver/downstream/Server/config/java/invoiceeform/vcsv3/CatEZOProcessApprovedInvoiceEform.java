// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 3/21/2007 3:53:22 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatEZOProcessApprovedInvoiceEform.java

/******************************************************************************************************************
1	Issue 620 	10-29-2007 		Added aribaSystemUser for that partition as the Preparer and Requester	 	Amit
2      Issue 988     08-17-2010           Added code to copy comments to invoice                                              Lekshmi and Darshan
*******************************************************************************************************************/

package config.java.invoiceeform.vcsv3;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Country;
import ariba.basic.core.Money;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;
import ariba.common.core.Log;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.payment.core.PaymentTerms;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementOrderInfo;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;
import config.java.invoiceeform.InvoiceEformUtil;

public class CatEZOProcessApprovedInvoiceEform extends Action
{

    public CatEZOProcessApprovedInvoiceEform()
    {
    }

    public void fire(ValueSource object, PropertyTable parameters)
        throws ActionExecutionException
    {
        Approvable eform = (Approvable)object;
        //if (Log.customer.debugOn)
            Log.customer.debug("Processing InvoiceEform %s.", eform);
        ariba.base.core.Partition partition = eform.getPartition();
        Invoice invoice = (Invoice)BaseObject.create("ariba.invoicing.core.Invoice", partition);
        invoice.save();
        invoice.setLoadedFrom(3);
        invoice.setName((String)eform.getFieldValue("Name"));
        //invoice.setPreparer((User)eform.getFieldValue("Preparer"));
        //invoice.setRequester((User)eform.getFieldValue("Requester"));
        invoice.setPreparer(User.getAribaSystemUser(partition));
        invoice.setRequester(User.getAribaSystemUser(partition));
        invoice.setInvoiceNumber((String)eform.getFieldValue("InvoiceNumber"));

        //Code by lekshmi to copy coments fron Invoice Eform to Invoice
		        //Start

		        Log.customer
				.debug("CatCSVProcessApprovedInvoiceEform Adding Comment in Invoice");
		       List commentsOnApprovable = eform.getComments();
		   	   Log.customer
			          .debug("CatCSVProcessApprovedInvoiceEform Adding Comment in Invoice"+ commentsOnApprovable.size());
		       for (int i = 0; i < commentsOnApprovable.size(); i++) {
		    	   Log.customer
		   		.debug("CatCSVProcessApprovedInvoiceEform Entering for loop");
			         Comment commentItem = (Comment) commentsOnApprovable.get(i);
			         Log.customer
			 		.debug("CatCSVProcessApprovedInvoiceEform  Comment in Eform is "+commentItem);
			         invoice.addComment(commentItem);
			         Log.customer
				 		.debug("CatCSVProcessApprovedInvoiceEform  Comment in added to Invoice "+commentItem);
		       }

       //End

        invoice.setInvoiceDate((Date)eform.getFieldValue("InvoiceDate"));
        invoice.setSupplier((Supplier)eform.getFieldValue("Supplier"));
        invoice.setSupplierLocation((SupplierLocation)eform.getFieldValue("SupplierLocation"));
        invoice.setRemitToAddress((Address)eform.getFieldValue("RemitToAddress"));
        invoice.setPaymentTerms((PaymentTerms)eform.getFieldValue("PaymentTerms"));
        invoice.setDottedFieldValue("BlockStampDate", (Date)eform.getDottedFieldValue("BlockStampDate"));
        invoice.setDottedFieldValue("CatInvoiceNumber", (String)eform.getDottedFieldValue("CatInvoiceNumber"));
        invoice.setDottedFieldValue("IsVATReverseCharge", (Boolean)eform.getDottedFieldValue("IsVATReverseCharge"));
        invoice.setDottedFieldValue("CatVATCountryCode", (String)eform.getDottedFieldValue("CatVATCountryCode"));
        invoice.setDottedFieldValue("BVRNumber", (String)eform.getDottedFieldValue("BVRNumber"));
        Money totalInvoicedLessTax = (Money)eform.getFieldValue("TotalInvoicedLessTax");
        Money totalInvoiced = (Money)eform.getFieldValue("EnteredInvoiceAmount");
        Money totalTax = (Money)eform.getFieldValue("TotalTax");
        boolean isCreditMemo = "creditMemo".equals((String)eform.getFieldValue("Purpose"));
        if(isCreditMemo)
        {
            totalInvoicedLessTax = totalInvoicedLessTax.negate();
            totalTax = totalTax.negate();
            totalInvoiced = totalInvoiced.negate();
            invoice.setFieldValue("RelatedCatInvoice", (String)eform.getFieldValue("RelatedCatInvoice"));
        }
        invoice.setTotalInvoicedLessTax(totalInvoicedLessTax);
        invoice.setTotalTax(totalTax);
        invoice.setTotalInvoiced(totalInvoiced);
        invoice.setFieldValue("Eform", Boolean.TRUE);
        invoice.setFieldValue("InvoiceEform", eform);
        invoice.setFieldValue("Terms", (String)eform.getFieldValue("Terms"));
        Contract ma = (Contract)eform.getFieldValue("MasterAgreement");
        String maNumber = null;
        if(ma != null)
            maNumber = ma.getUniqueName();
        List eformLineItems = (List)eform.getFieldValue("LineItems");
        StatementOrderInfo prevInfo = null;
        boolean consolidated = false;
        boolean isTaxInLine = false;
        int size = ListUtil.getListSize(eformLineItems);
        for(int i = 0; i < size; i++)
        {
            BaseObject eformLI = (BaseObject)eformLineItems.get(i);
            InvoiceLineItem invoiceLI = new InvoiceLineItem(partition, invoice);
            invoiceLI.setDefaultsFromApprovable(invoice);
            ProcureLineType lineType = (ProcureLineType)eformLI.getFieldValue("LineType");
            invoiceLI.setLineType(lineType);
            invoiceLI.setInvoiceLineNumber(IntegerFormatter.getIntValue(eformLI.getFieldValue("InvoiceLineNumber")));
            invoiceLI.setOrderLineNumber(IntegerFormatter.getIntValue(eformLI.getFieldValue("OrderLineNumber")));
            invoiceLI.setSupplier((Supplier)eform.getFieldValue("Supplier"));
            invoiceLI.setSupplierLocation((SupplierLocation)eform.getFieldValue("SupplierLocation"));
            LineItemProductDescription pd = invoiceLI.getDescription();
            pd.setDescription((String)eformLI.getFieldValue("Description"));

            if (eformLI.getFieldValue("SupplierPartNumber") != null)
            pd.setSupplierPartNumber((String)eformLI.getFieldValue("SupplierPartNumber"));

            Money mPrice = (Money)eformLI.getFieldValue("Price");
            if(isCreditMemo && mPrice.getSign() > 0 && !ProcureLineType.isChargeCategory(lineType))
                mPrice = mPrice.negate();
            pd.setPrice(mPrice);
            if(ProcureLineType.isLineItemCategory(lineType))
                pd.setUnitOfMeasure((UnitOfMeasure)eformLI.getFieldValue("UnitOfMeasure"));
            StatementOrderInfo info = new StatementOrderInfo(partition);
            info.setOrderNumber((String)eformLI.getFieldValue("OrderNumber"));
            info.setMANumber(maNumber);
            invoiceLI.setSupplierOrderInfo(info);
            if(!consolidated && prevInfo != null)
                consolidated = !prevInfo.equals(info);
            prevInfo = info;
            BigDecimal qty = (BigDecimal)eformLI.getFieldValue("Quantity");
            Money amount = (Money)eformLI.getFieldValue("Amount");
            Money taxAmount = (Money)eformLI.getFieldValue("TaxAmount");
            if(isCreditMemo)
            {
                if(amount.getSign() > 0)
                    amount = amount.negate();
                if(taxAmount != null && taxAmount.getSign() > 0)
                    taxAmount = taxAmount.negate();
            }
            invoiceLI.setQuantity(qty);
            invoiceLI.setAmount(amount);
            invoiceLI.setTaxAmount(taxAmount);
            invoiceLI.setDottedFieldValue("VATRate", (BigDecimal)eformLI.getDottedFieldValue("VATRate"));
            invoiceLI.setDottedFieldValue("IsVATRecoverable", (Boolean)eformLI.getDottedFieldValue("IsVATRecoverable"));
            if(lineType.getCategory() == 2 && IntegerFormatter.getIntValue(eformLI.getFieldValue("OrderLineNumber")) > 0)
                isTaxInLine = true;
            invoice.getLineItems().add(invoiceLI);
        }

        invoice.setConsolidated(consolidated);
        if(!consolidated)
            invoice.setSupplierOrderInfo(prevInfo);
        invoice.setIsTaxInLine(isTaxInLine);
        invoice.setDottedFieldValue("OriginVATCountry", (Country)eform.getDottedFieldValue("OriginVATCountry"));
        invoice.setDottedFieldValue("VATRegistrationNumber", (String)eform.getDottedFieldValue("VATRegistrationNumber"));
        InvoiceEformUtil.linkParentAndChildren(invoice);
        invoice.processLoadedInvoice();
        FastStringBuffer invUniqueName = new FastStringBuffer(invoice.getUniqueName());
        invUniqueName.replace("INV", "INEF");
        eform.setUniqueName(invUniqueName.toString());
        //if (Log.customer.debugOn)
            Log.customer.debug("%s ::: Exiting the fire method for Invoice EForm %s", "CatEZOProcessApprovedInvoiceEform", eform);
    }

    protected ValueInfo getValueInfo()
    {
        return new ValueInfo(0, "config.java.invoiceeform.InvoiceEform");
    }

    public static final String ClassName = "CatEZOProcessApprovedInvoiceEform";
    private static final String invoiceIdentifier = "INV";
    private static final String inveformIdentifier = "INEF";
}