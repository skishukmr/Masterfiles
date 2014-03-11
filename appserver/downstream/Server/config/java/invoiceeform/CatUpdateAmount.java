/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to update the amount on the LineItem based on the quantity and price fields.
*/

package config.java.invoiceeform;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.util.core.PropertyTable;

public class CatUpdateAmount extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
		Log.customer.debug("in CatUpdateAmount");
        BigDecimal quantity = (BigDecimal)object.getFieldValue("Quantity");
        Money price = (Money)object.getFieldValue("Price");

        if (quantity != null && price != null) {
            Money amount = Money.multiply(price, quantity);
            amount.setCurrency(price.getCurrency());
            Log.customer.debug("amount = " + amount.asString());
            object.setDottedFieldValue("Amount", amount);
        }
    }
}
