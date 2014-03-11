package config.java.invoiceeform.vcsv3;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.*;
import java.util.List;

/**  Author: KS.  Update OOB sample for eForm. 
 	 Uses TaxAmount vs. Amount for calculating TotalTax on non-Tax lines
*/

public class CatEZOUpdateTotal extends Action
{
    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        Log.customer.debug("UpdateTotal: begin\n");
        Assert.that(valuesource instanceof BaseObject, "BaseObject expected");
        if(!(valuesource instanceof ClusterRoot))
        {
            valuesource = ((BaseObject)valuesource).getClusterRoot();
 //           Assert.that(valuesource != null, "Cluster root not set");
        }
 //       updateTotals(valuesource);
        if (valuesource != null)
            updateTotals(valuesource);
    }

    private static void updateTotals(ValueSource valuesource)
    {
        List list = (List)valuesource.getFieldValue("LineItems");
        Money money = (Money)valuesource.getFieldValue("TotalInvoiced");
        Log.customer.debug("UpdateTotal: TotalInvoiced = %s", money.asString());
        Assert.that(list != null, "LineItems field cannot be null");
        Assert.that(money != null, "TotalInvoiced Money field cannot be null");
        ariba.basic.core.Currency currency = money.getCurrency();
        Money money1 = new Money(Constants.ZeroBigDecimal, currency);
        Money money2 = new Money(Constants.ZeroBigDecimal, currency);
        Money money3 = new Money(Constants.ZeroBigDecimal, currency);
        Money money4 = new Money(Constants.ZeroBigDecimal, currency);
        int i = list.size();
        for(int j = 0; j < i; j++)
        {
            BaseObject baseobject = (BaseObject)list.get(j);
            Money money5 = (Money)baseobject.getFieldValue("Amount");
            Assert.that(money5 != null, "LineItems.Amount Money field cannot be null");
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            int k = procurelinetype == null ? 1 : procurelinetype.getCategory();
            if(k == 2) {
                money2 = money2.add(money5);
                continue;
            }
            if(k == 4)
                money3 = money3.add(money5);
            else
                money1 = money1.add(money5);
            
            //(KS) added this to include TaxAmount in TotalTax
            Money moneyVAT = (Money)baseobject.getFieldValue("TaxAmount");
            if (moneyVAT != null) 
                money2 = money2.add(moneyVAT);
        }

        Log.customer.debug("UpdateTotal: shippingTotal = %s", money3.asString());
        Log.customer.debug("UpdateTotal: taxTotal = %s", money2.asString());
        Log.customer.debug("UpdateTotal: subTotal = %s", money1.asString());
        valuesource.setDottedFieldValue("TotalInvoicedLessTax", money1);
        valuesource.setDottedFieldValue("TotalTax", money2);
        valuesource.setDottedFieldValue("TotalShipping", money3);
        money4 = Money.add(money1, money2);
        Log.customer.debug("UpdateTotal: TotalInvoiced = %s", money4.asString());
        valuesource.setDottedFieldValue("TotalInvoiced", Money.add(money4, money3));
    }

    
    public CatEZOUpdateTotal()
    {
    }
}
