/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import java.util.List;

import ariba.approvable.core.LineItemReference;
import ariba.base.core.BaseObject;
import ariba.common.core.Log;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementOrderInfo;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.SystemUtil;
import ariba.util.formatter.IntegerFormatter;

/**
    Some Util classes for invoice eform
*/
public class InvoiceEformUtil
{
    private static final String LineTypeKey = "LineType";
    private static final String OrderLineNumberKey = "OrderLineNumber";
    private static final String OrderNumberKey = "OrderNumber";
    private static final String LineItemsKey = "LineItems";


    public static void linkParentAndChildren (Invoice invoice)
    {

        List invoiceLineItems = invoice.getLineItems();
        int size = ListUtil.getListSize(invoiceLineItems);
        for (int i=0; i < size; i++) {

            InvoiceLineItem invoiceLineItem = (InvoiceLineItem)invoiceLineItems.get(i);
            ProcureLineType lineType = invoiceLineItem.getLineType();

                // link parent and children for charges
            if (isLinkableChildType(lineType)) {

                InvoiceLineItem parent = findParent(invoice, invoiceLineItem);
                if (parent != null) {
                    linkParentAndChild(parent, invoiceLineItem);
                }
                else {
                    Log.customer.debug("Cannot find parent line item for %s",
                            invoiceLineItem);
                }
            }
        }
    }

    public static boolean isLinkableChildType (ProcureLineType lineType)
    {
        if (lineType == null) {
            return false;
        }

        int category = lineType.getCategory();
        return category == ProcureLineType.TaxChargeCategory ||
                category == ProcureLineType.SpecialChargeCategory ||
                category == ProcureLineType.FreightChargeCategory ||
                category == ProcureLineType.DiscountCategory ||
                category == ProcureLineType.HandlingChargeCategory;
    }

    private static InvoiceLineItem findParent (Invoice invoice,
                                               InvoiceLineItem lineItem)
    {
        int orderLineNumber = lineItem.getOrderLineNumber();
        if (orderLineNumber == 0) {
                // Use the default line item as the parent for any
                // header-level charges
            return (InvoiceLineItem)invoice.getDefaultLineItem();
        }
        else {
            List invoiceLineItems = invoice.getLineItems();
            StatementOrderInfo orderInfo = lineItem.getSupplierOrderInfo();
            int size = ListUtil.getListSize(invoiceLineItems);
            for (int i=0; i < size; i++) {

                InvoiceLineItem invoiceLineItem =
                        (InvoiceLineItem)invoiceLineItems.get(i);
                ProcureLineType lineType = invoiceLineItem.getLineType();

                    // find line item whose orderno and order line number match with
                    // the corresponding values of the target item
                if (!isLinkableChildType(lineType) &&
                    invoiceLineItem.getOrderLineNumber() == orderLineNumber) {

                	StatementOrderInfo lineOrderInfo =
                            invoiceLineItem.getSupplierOrderInfo();
                    if ((orderInfo == null && lineOrderInfo == null) ||
                            (orderInfo != null &&
                            lineOrderInfo != null &&
                            lineOrderInfo.getOrderNumber().equalsIgnoreCase(
                                    orderInfo.getOrderNumber()))) {

                        return invoiceLineItem;
                    }
                }
            }
        }

        return null;
    }

    /**
        Checks whether a parent non-charge line item exists for this
        charge line item.
    */
    public static boolean hasParent (BaseObject eform, BaseObject chargeLI)
    {
        ProcureLineType lineType = (ProcureLineType)
            chargeLI.getFieldValue(LineTypeKey);

        Assert.that(isLinkableChildType(lineType),
                "Cannot be called for a non-linkable type");

        int chargeOrderLineNum = IntegerFormatter.getIntValue(
                chargeLI.getFieldValue(OrderLineNumberKey));

        String order = (String)chargeLI.getFieldValue(OrderNumberKey);
        if (order != null) {
            order = order.toLowerCase();
        }
        List eformLineItems = (List)eform.getFieldValue(LineItemsKey);
        int size = ListUtil.getListSize(eformLineItems);
        for (int i = 0; i < size; i++) {
            BaseObject eformLI = (BaseObject)eformLineItems.get(i);
            lineType = (ProcureLineType)eformLI.getFieldValue(LineTypeKey);

            if (isLinkableChildType(lineType)) {
                continue;
            }

            String lineOrder = (String)eformLI.getFieldValue(OrderNumberKey);
            if (lineOrder != null) {
                lineOrder = lineOrder.toLowerCase();
            }

                // make sure that we are comparing items against the same order
            if (!SystemUtil.equal(lineOrder, order)) {
                continue;
            }

            int orderLineNum = IntegerFormatter.getIntValue(
                    eformLI.getFieldValue(OrderLineNumberKey));

            if (orderLineNum == chargeOrderLineNum) {
                return true;
            }
        }

        return false;
    }


    private static void linkParentAndChild (InvoiceLineItem parent, InvoiceLineItem child)
    {
        if (parent != null && child != null) {
            parent.getChildren().add(new LineItemReference(child));
            child.setParent(parent);
        }
    }

}
