/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.


    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import java.util.List;

/**
    Updates the Invoice Header totals based on the line items.
*/
public class UpdateTotal extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        Log.customer.debug("UpdateTotal: begin\n");
        Assert.that(object instanceof BaseObject, "BaseObject expected");
        if (!(object instanceof ClusterRoot)) {
            object = ((BaseObject)object).getClusterRoot();
            Assert.that(object != null, "Cluster root not set");
        }

        updateTotals(object);
    }


    private static void updateTotals (ValueSource object)
    {
        List lineItems = (List)object.getFieldValue("LineItems");
        Money currentTotal = (Money)object.getFieldValue("TotalInvoiced");

        Log.customer.debug("UpdateTotal: TotalInvoiced = %s", currentTotal.asString());

        Assert.that(lineItems != null,
                    "LineItems field cannot be null");
        Assert.that(currentTotal != null,
                    "TotalInvoiced Money field cannot be null");

            // Zero out the subtotals
        Currency currency = currentTotal.getCurrency();
        Money subTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money taxTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money shippingTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money newTotal = new Money(Constants.ZeroBigDecimal, currency);

        int size = lineItems.size();
        for (int i=0; i < size; i++) {
            BaseObject lineItem = (BaseObject)
                lineItems.get(i);
            Money lineAmount = (Money)
                lineItem.getFieldValue("Amount");

            Assert.that(lineAmount != null,
                        "LineItems.Amount Money field cannot be null");

                // Get the line type and category
            ProcureLineType lineType = (ProcureLineType)
                lineItem.getFieldValue("LineType");
            int lineCategory =
                (lineType != null ? lineType.getCategory() :
                 ProcureLineType.LineItemCategory);

            if (lineCategory == ProcureLineType.TaxChargeCategory) {
                taxTotal = taxTotal.add(lineAmount);
            }
            else if (lineCategory == ProcureLineType.FreightChargeCategory) {
                shippingTotal = shippingTotal.add(lineAmount);
            }
            else {
                subTotal = subTotal.add(lineAmount);
            }
        }

        Log.customer.debug("UpdateTotal: shippingTotal = %s", shippingTotal.asString());
        Log.customer.debug("UpdateTotal: taxTotal = %s", taxTotal.asString());
        Log.customer.debug("UpdateTotal: subTotal = %s", subTotal.asString());

        object.setDottedFieldValue("TotalInvoicedLessTax", subTotal);
        object.setDottedFieldValue("TotalTax", taxTotal);
        object.setDottedFieldValue("TotalShipping", shippingTotal);

        newTotal = Money.add(subTotal, taxTotal);
        Log.customer.debug("UpdateTotal: TotalInvoiced = %s", newTotal.asString());
        object.setDottedFieldValue("TotalInvoiced", Money.add(newTotal, shippingTotal));

    }
}
