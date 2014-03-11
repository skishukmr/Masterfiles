/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/core_java/ariba/invoicing/CXMLInvoiceLineItemCreator.java#1 $

    Responsible: mpoolu
*/
package ariba.invoicing;

import ariba.approvable.core.LineItemReference;
import ariba.base.core.Partition;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Address;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.Log;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementOrderInfo;
import ariba.util.core.Constants;
import ariba.util.log.Logger;
import java.math.BigDecimal;
import java.util.Hashtable;
import java.util.List;

/**
    A utility creator class to create invoice lines from various cxml invoice elements.
    It is used to actually create the invoice lines by other decoding components.
    elements.

    @aribaapi documented
*/
public class CXMLInvoiceLineItemCreator

{
    public static final Logger log = Log.invoiceLoading;

    public Invoice invoice;
    public InvoiceLineItem invoiceLineItem;
    public StatementOrderInfo reference;

    public int invoiceLineNumber;
    public Price price;

    public CXMLInvoiceLineItemCreator ()
    {
        initVars();
    }

    public void initVars ()
    {
        price = new Price();
        invoiceLineNumber = 0;
        invoice = null;
        invoiceLineItem = null;
        reference = null;

    }
    /**
        Create an invoice line item from InvoiceDetailItem data

        @aribaapi documented

        @param  lineType            line type
        @param  overrideDescription overriding description if any. If
                                   not null, then the LIPD.Description is set this value.
        @param  invoiceLineNumber   the invoice line number
        @param  quantity            quantity
        @param  amount              amount or the unit price
        @param  request             collection of information passed from cXML
        @return the newly created line item
    */
    protected InvoiceLineItem createLineItem (ProcureLineType lineType,
                                              String overrideDescription,
                                              int invoiceLineNumber,
                                              BigDecimal quantity,
                                              Money amount,
                                              Hashtable request)
    {
        return createLineItem(lineType,
                              overrideDescription,
                              invoiceLineNumber,
                              new Price(quantity, amount),
                              request);
    }


    /**
        Create an invoice line item from InvoiceDetailItem data

        @aribaapi documented

        @param  lineType            line type
        @param  overrideDescription overriding description if any. If
                                    not null, then the LIPD.Description is set this value.
        @param  invoiceLineNumber   the invoice line number
        @param  price               unit price and quantity for the item
        @param  request             collection of information passed from cXML
        @return the newly created line item
    */
    protected InvoiceLineItem createLineItem (ProcureLineType lineType,
                                              String overrideDescription,
                                              int invoiceLineNumber,
                                              Price price,
                                              Hashtable request)
    {
        this.price = price;
        Partition partition = invoice.getPartition();
        invoiceLineItem = new InvoiceLineItem(partition, invoice);
        // Set the SupplierOrderInfo field on the line item
        if (reference == null) {
            reference = invoice.getSupplierOrderInfo();
        }

        if (reference != null) {
            StatementOrderInfo ref = new StatementOrderInfo(partition);
            ref.setMANumber(reference.getMANumber());
            ref.setMAPayloadID(reference.getMAPayloadID());
            ref.setOrderNumber(reference.getOrderNumber());
            ref.setOrderPayloadID(reference.getOrderPayloadID());
            ref.setSupplierSalesOrderNumber(reference.getSupplierSalesOrderNumber());
            invoiceLineItem.setSupplierOrderInfo(ref);
        }

        if (price.quantity == null) {
            price.quantity = LoadingConstants.UnitQuantity;
        }
        invoiceLineItem.setInvoiceLineNumber(invoiceLineNumber);
        invoiceLineItem.setLineType(lineType);

        LineItemProductDescription lipd = invoiceLineItem.getDescription();

        addLineItemDetails(invoiceLineItem,
                           lipd,
                           overrideDescription,
                           lineType,
                           price);

        if (invoiceLineItem.isChargeLineItem()) {
            // for charge category line types we don't use the
            // UOM.  set it to null.
            lipd.setUnitOfMeasure(null);
        }

        if (overrideDescription != null) {
            lipd.setDescription(overrideDescription);
        }

        if (invoiceLineItem.getIsQuantifiable()) {

            lipd.setPrice(price.unitPrice);
            invoiceLineItem.setQuantity(price.quantity);
        }
        else {
            if (price.unitPrice == null) {
                price.unitPrice = new Money(Constants.ZeroBigDecimal,
                                      Currency.getLeadCurrency(partition));
            }
            log.debug(
                "%s: item is not quantifyable but amount is null. Using zero", this);
            invoiceLineItem.setAmount(Money.multiply(price.unitPrice,
                                                     price.quantity));
        }

        Object billTo = request.get(InvoiceCXMLConstants.KeyInvoiceLineBillingAddress);
        invoiceLineItem.setBillingAddress((billTo != null) ? (Address)billTo : null);

            // Process Invoice Line - Specific to Caterpillar
        catProcessInvoiceLineItem(lineType);

            // our process will add a null item
        if (invoiceLineItem != null) {
            invoice.addLineItem(invoiceLineItem);
        }

            // End of Caterpillar Customization
        return invoiceLineItem;
    }

    protected void addLineItemDetails (InvoiceLineItem li,
                                       LineItemProductDescription lipd,
                                       String overrideDescription,
                                       ProcureLineType lineType,
                                       Price price)
    {
        /* no-op -> subclasses specialize it */
    }


    public Money getAmount ()
    {
        if (price.unitPrice != null) {
            return price.unitPrice.multiply(price.quantity);
        }
        else {
            return null;
        }
    }


    protected void linkParentAndChild (InvoiceLineItem parent,
                                       InvoiceLineItem child)
    {
        if (parent != null && child != null) {
            parent.getChildren().add(new LineItemReference(child));
            child.setParent(parent);
        }
    }

    protected void catProcessInvoiceLineItem (ProcureLineType lineType)
    {
            // Caterpillar Customization

            // We're ready to add the line items to the collection.
            // (S. Sato AUL - migrated)
            // First let's do our caterpillar specific customization
            //ADDED BY DHARMANG
        if (invoice.getPartition().getName().equals("pcsv1")) {
            if (lineType != null && price.unitPrice != null) {
                if (((lineType.getCategory() == ProcureLineType.TaxChargeCategory)
                    || (lineType.getCategory() == ProcureLineType.FreightChargeCategory)
                    || (lineType.getCategory() == ProcureLineType.HandlingChargeCategory))
                    && (price.unitPrice.getAmount().compareTo(new BigDecimal("0.00")) == 0))
                {

                    Log.customer.debug(
                            "Encountered a ZERO dollar tax/freight/handling line hence " +
                            "not adding to invoice");
                    invoiceLineItem = null;
                }
            }
        }

            // ADDED BY KS (R5)
        if (invoice.getPartition().getName().equals("ezopen")) {
            if (lineType != null && price.unitPrice != null) {
                if (lineType.getCategory() != ProcureLineType.LineItemCategory &&
                        price.unitPrice.getAmount().compareTo(new BigDecimal("0.00")) == 0)
                {
                    if (lineType.getCategory() != ProcureLineType.TaxChargeCategory) {
                        Log.customer.debug(
                                "Encountered a ZERO dollar charge line hence not " +
                                "adding to invoice!");
                        invoiceLineItem = null;
                    }
                    List details = invoice.getTaxDetails();
                    if (details == null || details.isEmpty()) {

                            // means not an intentional 0.00 tax amount
                        Log.customer.debug(
                                "Encountered an unintentional ZERO TAX line hence " +
                                "not adding to invoice!");
                        invoiceLineItem = null;
                    }
                }
            }
        }
    }

    /**
        Internal convenience class to encapsulate unitprice and quantity.

        @aribaapi private
    */
    public static class Price
    {
        public Money unitPrice;
        public BigDecimal quantity;

        public Price (Money unitPrice)
        {
            this.unitPrice = unitPrice;
            this.quantity = LoadingConstants.UnitQuantity;
        }

        public Price (BigDecimal quantity, Money unitPrice)
        {
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        public Price ()
        {
            this.quantity = LoadingConstants.UnitQuantity;
            this.unitPrice = null;
        }
    }
}
