/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to validate if all the invoice eform line items belong
         to the entered Supplier and SupplierLocation in the header.
*/

package config.java.invoiceeform;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.SystemUtil;
import java.util.List;

/**
    This class is one validity condition that checks if the lineitems belong to the
    chosen supplier. There is a possiblity the user changes the supplier after selecting the PO lines
    for a supplier. This validity condition checks that the lineitems belong to the
    chosen supplier.
*/
public class CatValidLineItems extends Condition
{
    private static final String requiredParameterNames[] = {"invoice" };
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("invoice",
                               0,
                               "config.java.invoiceeform.InvoiceEform")
    };

    private static final String ComponentStringTable = "aml.cat.Invoice";

    private FastStringBuffer errorString = new FastStringBuffer("Orders :");
    protected static final String InvalidSupplierMsg = "InvalidSupplier";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
        BaseObject invoice = (BaseObject)
            params.getPropertyForKey("invoice");
        Supplier supplier = (Supplier)
            invoice.getFieldValue("Supplier");
        SupplierLocation supplierLocation = (SupplierLocation)
            invoice.getFieldValue("SupplierLocation");

		errorString = new FastStringBuffer("Orders :");

        // Just return OK if supplier or supplier location is not set yet
        if (supplier == null || supplierLocation == null) {
            return true;
        }

        // Go through and check all of the line items
        boolean valid = true;
        PurchaseOrder prevOrder = null;
        List lineItems = (List)invoice.getFieldValue("LineItems");
        int size = lineItems.size();
        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);
            PurchaseOrder order = (PurchaseOrder)lineItem.getFieldValue("Order");

            // Go to next one if order is not set or same as previous
            if (order == null ||
                SystemUtil.equal(order, prevOrder)) {
                continue;
            }

            // compare the supplier locations on the line and header
            SupplierLocation lineSupplierLocation = order.getSupplierLocation();
            if (!SystemUtil.equal(supplierLocation, lineSupplierLocation)) {
                valid = false;
                errorString.append(order.getOrderID());
                errorString.append(" ");
            }
            prevOrder = order;
        }

        return valid;
    }

    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               "InvalidSupplier",
                                               errorString,
                                               subjectForMessages(params)));
        }
        else {
            return null;
        }

    }

    /**
        Returns the valid parameter types
    */
    protected ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    /**
        Returns required parameter names for the class
    */
    protected String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }

}
