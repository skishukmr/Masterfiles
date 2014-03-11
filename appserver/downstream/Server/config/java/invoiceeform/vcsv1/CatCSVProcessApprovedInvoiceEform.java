// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:24 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
/************************************************************************************************************************************************
1	Issue 620 	10-29-2007 		Added aribaSystemUser for that partition as the Preparer and Requester	 	Amit
2      Issue 988     08-17-2010           Added code to copy comments to invoice                                              Lekshmi and Darshan
************************************************************************************************************************************************/
package config.java.invoiceeform.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Address;
import ariba.common.core.Log;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
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
public class CatCSVProcessApprovedInvoiceEform extends Action
{

    public CatCSVProcessApprovedInvoiceEform()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        Approvable approvable = (Approvable)valuesource;
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Entering the fire method for Invoice EForm %s", "CatCSVProcessApprovedInvoiceEform", approvable);
        ariba.base.core.Partition partition = approvable.getPartition();
        Invoice invoice = (Invoice)BaseObject.create("ariba.invoicing.core.Invoice", partition);
        invoice.save();
        invoice.setLoadedFrom(3);
        invoice.setName((String)approvable.getFieldValue("Name"));
        //invoice.setPreparer((User)approvable.getFieldValue("Preparer"));
        //invoice.setRequester((User)approvable.getFieldValue("Requester"));
        invoice.setPreparer(User.getAribaSystemUser(partition));
        invoice.setRequester(User.getAribaSystemUser(partition));
        invoice.setInvoiceNumber((String)approvable.getFieldValue("InvoiceNumber"));

        //Code by lekshmi to copy coments fron Invoice Eform to Invoice
		        //Start

		        Log.customer
				.debug("CatCSVProcessApprovedInvoiceEform Adding Comment in Invoice");
		       List commentsOnApprovable = approvable.getComments();
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


        int i = Date.getMonth((Date)approvable.getFieldValue("InvoiceDate"));
        int j = Date.getYear((Date)approvable.getFieldValue("InvoiceDate"));
        int k = Date.getDayOfMonth((Date)approvable.getFieldValue("InvoiceDate"));
        Date date = new Date(j, i, k, false);
        //if(Log.customer.debugOn)
        {
            Log.customer.debug("%s ::: Month: " + i + ", Day: " + k + ", Year: " + j, "CatCSVProcessApprovedInvoiceEform");
            Log.customer.debug("%s ::: Date: %s", "CatCSVProcessApprovedInvoiceEform", date);
        }
        Date.setHours(date, 8);
        Log.customer.debug("%s ::: Date after +8: %s", "CatCSVProcessApprovedInvoiceEform", date);
        invoice.setInvoiceDate(date);
        invoice.setSupplier((Supplier)approvable.getFieldValue("Supplier"));
        invoice.setSupplierLocation((SupplierLocation)approvable.getFieldValue("SupplierLocation"));
        invoice.setRemitToAddress((Address)approvable.getFieldValue("RemitToAddress"));
        invoice.setDottedFieldValue("SettlementCode", (ClusterRoot)approvable.getDottedFieldValue("SettlementCode"));
        invoice.setDottedFieldValue("BlockStampDate", (Date)approvable.getDottedFieldValue("BlockStampDate"));
        Money money = (Money)approvable.getFieldValue("TotalInvoicedLessTax");
        Money money1 = (Money)approvable.getFieldValue("EnteredInvoiceAmount");
        Money money2 = (Money)approvable.getFieldValue("TotalTax");
        boolean flag = "creditMemo".equals((String)approvable.getFieldValue("Purpose"));
        if(flag)
        {
            money = money.negate();
            money2 = money2.negate();
            money1 = money1.negate();
        }
        invoice.setTotalInvoicedLessTax(money);
        invoice.setTotalTax(money2);
        invoice.setTotalInvoiced(money1);
        invoice.setFieldValue("Eform", Boolean.TRUE);
        invoice.setFieldValue("InvoiceEform", approvable);
        invoice.setFieldValue("Terms", (String)approvable.getFieldValue("Terms"));
        Contract masteragreement = (Contract)approvable.getFieldValue("MasterAgreement");
        String s = null;
        if(masteragreement != null)
            s = masteragreement.getUniqueName();
        BigDecimal bigdecimal = (BigDecimal)approvable.getDottedFieldValue("TermsDiscount");
        if(bigdecimal == null)
            bigdecimal = new BigDecimal("0.00");
        invoice.setDottedFieldValue("TermsDiscount", bigdecimal);
        List list = (List)approvable.getFieldValue("LineItems");
        StatementOrderInfo invoiceorderinfo = null;
        boolean flag1 = false;
        int l = ListUtil.getListSize(list);
        for(int i1 = 0; i1 < l; i1++)
        {
            BaseObject baseobject = (BaseObject)list.get(i1);
            InvoiceLineItem invoicelineitem = new InvoiceLineItem(partition, invoice);
            invoicelineitem.setDefaultsFromApprovable(invoice);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            invoicelineitem.setLineType(procurelinetype);
            invoicelineitem.setInvoiceLineNumber(IntegerFormatter.getIntValue(baseobject.getFieldValue("InvoiceLineNumber")));
            invoicelineitem.setOrderLineNumber(IntegerFormatter.getIntValue(baseobject.getFieldValue("OrderLineNumber")));
            invoicelineitem.setSupplier((Supplier)approvable.getFieldValue("Supplier"));
            invoicelineitem.setSupplierLocation((SupplierLocation)approvable.getFieldValue("SupplierLocation"));
            LineItemProductDescription lineitemproductdescription = invoicelineitem.getDescription();
            lineitemproductdescription.setDescription((String)baseobject.getFieldValue("Description"));
            if (baseobject.getFieldValue("SupplierPartNumber") != null)
            lineitemproductdescription.setSupplierPartNumber((String)baseobject.getFieldValue("SupplierPartNumber"));
            Money money3 = (Money)baseobject.getFieldValue("Price");
            if(flag && money3.getSign() > 0 && !ProcureLineType.isChargeCategory(procurelinetype))
                money3 = money3.negate();
            Log.customer.debug("price is " + money3.asString());
            lineitemproductdescription.setPrice(money3);
            if(ProcureLineType.isLineItemCategory(procurelinetype))
                lineitemproductdescription.setUnitOfMeasure((UnitOfMeasure)baseobject.getFieldValue("UnitOfMeasure"));
            lineitemproductdescription.setFieldValue("CAPSChargeCode", baseobject.getFieldValue("CapsChargeCode"));
            if(baseobject.getFieldValue("CapsChargeCode") != null && !"001".equals(baseobject.getDottedFieldValue("CapsChargeCode.UniqueName")))
                lineitemproductdescription.setIsInternalPartId(true);
            invoicelineitem.setDottedFieldValue("ReferenceLineNumber", baseobject.getFieldValue("ReferenceLineNumber"));
            invoicelineitem.setDottedFieldValue("CapsChargeCode", baseobject.getFieldValue("CapsChargeCode"));
            invoicelineitem.setDottedFieldValue("TermsDiscountPercent", bigdecimal);
            StatementOrderInfo invoiceorderinfo1 = new StatementOrderInfo(partition);
            invoiceorderinfo1.setOrderNumber((String)baseobject.getFieldValue("OrderNumber"));
            invoiceorderinfo1.setMANumber(s);
            invoicelineitem.setSupplierOrderInfo(invoiceorderinfo1);
            if(!flag1 && invoiceorderinfo != null)
                flag1 = !invoiceorderinfo.equals(invoiceorderinfo1);
            invoiceorderinfo = invoiceorderinfo1;
            BigDecimal bigdecimal1 = (BigDecimal)baseobject.getFieldValue("Quantity");
            Money money4 = (Money)baseobject.getFieldValue("Amount");
            if(flag && money4.getSign() > 0)
                money4 = money4.negate();
            invoicelineitem.setQuantity(bigdecimal1);
            invoicelineitem.setAmount(money4);
            invoice.getLineItems().add(invoicelineitem);
        }

        invoice.setConsolidated(flag1);
        if(!flag1)
            invoice.setSupplierOrderInfo(invoiceorderinfo);
        InvoiceEformUtil.linkParentAndChildren(invoice);
        invoice.processLoadedInvoice();
        FastStringBuffer faststringbuffer = new FastStringBuffer(invoice.getUniqueName());
        faststringbuffer.replace("INV", "INEF");
        approvable.setUniqueName(faststringbuffer.toString());
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Exiting the fire method for Invoice EForm %s", "CatCSVProcessApprovedInvoiceEform", approvable);
    }

    protected ValueInfo getValueInfo()
    {
        return new ValueInfo(0, "config.java.invoiceeform.InvoiceEform");
    }

    public static final String ClassName = "CatCSVProcessApprovedInvoiceEform";
    private static final String invoiceIdentifier = "INV";
    private static final String inveformIdentifier = "INEF";
}