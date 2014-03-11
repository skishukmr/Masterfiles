/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import java.util.List;

/**
    Copies information from the selected PurchaseOrders to the Invoice Eform.

    This will create line items for each of the line items on the PO, copying
    all relevant information over to the Eform.

    It also defaults the Supplier and SupplierLocation fields from the first
    PO if it has not been set yet.
*/
public class SetInvoiceInfoFromOrder extends Action
{
    private static final ValueInfo valueInfo =
        new ValueInfo(0,
                               Approvable.ClassName);

    public void fire (ValueSource object, PropertyTable params)
    {
        Approvable invoice = (Approvable)object;
        List orders = (List)invoice.getFieldValue("Orders");

            // Do nothing if there are no orders
        if (ListUtil.nullOrEmptyList(orders)) {
            return;
        }

            // Get number of invoice lines to use for the invoice line number
        List invoiceLines = (List)invoice.getFieldValue("LineItems");
        int invoiceLineNumber = ListUtil.getListSize(invoiceLines) + 1;

            // Go through each order
        int size = orders.size();
        for (int i = 0; i < size; i++) {
            BaseId orderBaseId = (BaseId)orders.get(i);
            PurchaseOrder order = (PurchaseOrder)
                Base.getSession().objectFromId(orderBaseId);
            List poLines = order.getLineItems();

                // Default the supplier from the order (if not set yet)
            if (invoice.getFieldValue("Supplier") == null) {
                invoice.setFieldValue("Supplier", order.getSupplier());
            }

                // Default the supplier location from the order (if not set yet)
            if (invoice.getFieldValue("SupplierLocation") == null) {
                invoice.setFieldValue("SupplierLocation", order.getSupplierLocation());
            }

                // No go through each line on the order
            int lines = ListUtil.getListSize(poLines);
            for (int j = 0; j < lines; j++) {
                POLineItem poLI = (POLineItem)poLines.get(j);

                    // Create a new invoice line item and add it to the invoice
                BaseObject invoiceLI = (BaseObject)
                    BaseObject.create("config.java.invoiceeform.InvoiceEformLineItem",
                                      invoice.getPartition());
                invoiceLines.add(invoiceLI);

                    // Get the LineType from the PO line and set it on the invoice line item
                ProcureLineType lineType = ProcureLineType.lookupByLineItem(poLI);
                invoiceLI.setFieldValue("LineType", lineType);

                    // Set the InvoiceLineNumber to the next number and increment
                invoiceLI.setFieldValue("InvoiceLineNumber",
                                        Constants.getInteger(invoiceLineNumber));
                invoiceLineNumber++;

                    // Set the order information
                invoiceLI.setFieldValue("Order", order);
                invoiceLI.setFieldValue("OrderNumber", order.getOrderID());
                invoiceLI.setFieldValue("OrderLineItem", poLI);
                invoiceLI.setFieldValue(
                    "OrderLineNumber",
                    Constants.getInteger(poLI.getExternalLineNumber()));

                    // Get the product description from the po line and copy that info over
                LineItemProductDescription pd = poLI.getDescription();
                invoiceLI.setFieldValue("Price", pd.getPrice());
                invoiceLI.setFieldValue("UnitOfMeasure", pd.getUnitOfMeasure());
                invoiceLI.setFieldValue("Description", pd.getDescription());

                if (pd.getSupplierPartNumber() != null)
                invoiceLI.setFieldValue("SupplierPartNumber", pd.getSupplierPartNumber());

                    // Set the quantity and amount
                invoiceLI.setFieldValue("Quantity", poLI.getQuantity());
                invoiceLI.setFieldValue("Amount", poLI.getAmount());
            }
        }

        orders.clear();
    }

    /**
        Returns the list of valid value types.

        @return the list of valid value types.
    */
    protected ValueInfo getValueInfo ()
    {
        return valueInfo;
    }

}
