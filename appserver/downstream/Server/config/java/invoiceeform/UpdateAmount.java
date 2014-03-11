/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.util.core.PropertyTable;
import java.math.BigDecimal;

/**
    Updates the amount on the LineItem based on the quantity and price fields.
*/
public class UpdateAmount extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        BigDecimal quantity = (BigDecimal)object.getFieldValue("Quantity");
        Money price = (Money)object.getFieldValue("Price");

        if (quantity != null && price != null) {
            Money amount = Money.multiply(price, quantity);
            object.setDottedFieldValue("Amount", amount);
        }
    }
}
