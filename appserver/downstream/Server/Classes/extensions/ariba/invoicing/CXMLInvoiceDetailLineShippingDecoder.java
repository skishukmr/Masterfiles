/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/core_java/ariba/invoicing/CXMLInvoiceDetailLineShippingDecoder.java#1 $

    Responsible: mpoolu
*/
package ariba.invoicing;

import ariba.base.core.LocalizedString;
import ariba.base.core.Partition;
import ariba.basic.core.Money;
import ariba.encoder.xml.AXComponent;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceUtil;
import ariba.invoicing.core.Log;
import ariba.procure.server.cxml.CXMLDecodedContact;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;

/**
    CXML decoding component for creating invoice lines from InvoiceDetailShipping
    elements.

    @aribaapi documented
*/
public class CXMLInvoiceDetailLineShippingDecoder extends AXComponent

{
    public static final String ClassName =
        CXMLInvoiceDetailLineShippingDecoder.class.getName();

    public Money shippingAmount;
    public CXMLDecodedContact contact;
    public InvoiceLineItem parentLineItem;
    public java.util.Hashtable request;

    public static final String DefaultShippingDescriptionKey =
            "DefaultShippingDescription";

    protected CXMLInvoiceLineItemCreator creator = new CXMLInvoiceLineItemCreator();

    /**
     * The awake method is used to reinitialize any variables which were nulled
     * out when this instance of this class was put to sleep. It should always
     * invoke the super to make sure anything the super depends on is also woken.
     */
    public void awake ()
    {
        super.awake();
        creator = new CXMLInvoiceLineItemCreator();
        creator.initVars();
    }

    /**
     * The sleep method is used to null out any instance variables. This makes
     * sure that when the object is reused it will not inherit any data from
     * the last run. This null state is enforced by the AW layer. In addition
     * error reporting on a slept object is likely to cause some form of
     * object out date exception. It is not sufficient to simply clear lists,
     * tables, etc, they actually need to be nulled out.
     */
    protected void sleep ()
    {
        super.sleep();
        shippingAmount = null;
        contact = null;
        parentLineItem = null;
        request = null;
        creator = null;
    }

    public void initVars ()
    {
        Log.invoiceLoading.debug("%s.initVars", ClassName);
        creator.initVars();
        shippingAmount = null;
        contact = null;
        parentLineItem = null;
        request = null;
    }

    public void setInvoice (Invoice invoice)
    {
        creator.invoice = invoice;
    }

    public void createLineItem ()
    {
        Partition partition = creator.invoice.getPartition();
        boolean ignoreZeroShipping = InvoiceUtil.ignoreZeroLineShipping(partition);

        if (shippingAmount.isApproxZero() && ignoreZeroShipping) {
            return;
        }

        creator.reference = parentLineItem.getSupplierOrderInfo();
        ProcureLineType lineType = (ProcureLineType)request.get("freightLineType");
        InvoiceLineItem invoiceLineItem =
            creator.createLineItem(lineType,
                                   new LocalizedString(
                                       LoadingConstants.StringTable,
                                       DefaultShippingDescriptionKey).toString(),
                                   parentLineItem.getInvoiceLineNumber(),
                                   Constants.OneBigDecimal,
                                   shippingAmount,
                                   request);

            //do the parent-child linking here now
            // cat specific modification
        if (invoiceLineItem == null) {
            return;
        }
        creator.linkParentAndChild(parentLineItem, invoiceLineItem);
        Log.invoiceLoading.debug("Created line level handling line item: %s, %s",
                                 invoiceLineItem, shippingAmount);
    }

}
