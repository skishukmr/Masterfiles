// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)

/**
    AUL - S. Sato
    This class was decompiled as we didn't have the source file in the lab
*/
package config.java.invoiceeform.vcsv2;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.*;
import java.util.List;

public class CatMFGUpdateTotal extends Action
{

    public CatMFGUpdateTotal()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        Log.customer.debug("UpdateTotal: begin\n");
        Assert.that(valuesource instanceof BaseObject, "BaseObject expected");
        if(!(valuesource instanceof ClusterRoot))
        {
            valuesource = ((BaseObject)valuesource).getClusterRoot();
            Assert.that(valuesource != null, "Cluster root not set");
        }
        updateTotals(valuesource);
    }

    private static void updateTotals(ValueSource valuesource)
    {
        List list = (List)valuesource.getFieldValue("LineItems");
        Money money = (Money)valuesource.getFieldValue("EnteredInvoiceAmount");
        if(money == null)
            return;
        Log.customer.debug("UpdateTotal: EnteredInvoiceAmount = %s", money.asString());
        Assert.that(list != null, "LineItems field cannot be null");
        ariba.basic.core.Currency currency = money.getCurrency();
        Money money1 = new Money(Constants.ZeroBigDecimal, currency);
        Money money2 = new Money(Constants.ZeroBigDecimal, currency);
        Money money3 = new Money(Constants.ZeroBigDecimal, currency);
        Money money4 = new Money(Constants.ZeroBigDecimal, currency);
        Money money5 = new Money(Constants.ZeroBigDecimal, currency);
        Money money6 = new Money(Constants.ZeroBigDecimal, currency);
        int i = -1;
        String s = null;
        String s1 = null;
        int j = list.size();
        for(int k = 0; k < j; k++)
        {
            BaseObject baseobject1 = (BaseObject)list.get(k);
            Money money7 = (Money)baseobject1.getFieldValue("Amount");
            if(money7 == null)
                continue;
            ProcureLineType procurelinetype = (ProcureLineType)baseobject1.getFieldValue("LineType");
            int l = procurelinetype == null ? 1 : procurelinetype.getCategory();
            if(l == 2)
                money2 = money2.add(money7);
            else
            if(l == 4)
                money3 = money3.add(money7);
            else
                money1 = money1.add(money7);
            Log.customer.debug("1...LI #: " + k + " LI Size: " + j);
            if(k == 0)
            {
                if((String)baseobject1.getFieldValue("OrderNumber") != null)
                    s = s1 = (String)baseobject1.getFieldValue("OrderNumber");
            } else
            if((String)baseobject1.getFieldValue("OrderNumber") != null)
                s1 = (String)baseobject1.getFieldValue("OrderNumber");
            Log.customer.debug("prevPO is: " + s + " curPO is: " + s1);
            if(s1 != null && s != null)
            {
                Log.customer.debug("prevPO is: " + s + " curPO is: " + s1 + "Test..." + s.equals(s1));
                if(!s.equals(s1))
                {
                    if(i != -1)
                    {
                        BaseObject baseobject2 = (BaseObject)list.get(i);
                        if(((ProcureLineType)baseobject2.getFieldValue("LineType")).getCategory() == 2)
                            baseobject2.setDottedFieldValue("Amount", money5);
                        i = -1;
                    }
                    money6 = money6.add(money5);
                    money5 = new Money(Constants.ZeroBigDecimal, currency);
                    s = s1;
                }
            }
            Log.customer.debug("2...lineCategory: " + l);
            if(l != 2)
            {
                Money money8 = (Money)baseobject1.getFieldValue("TaxAmount");
                if(money8 == null)
                    continue;
                Log.customer.debug("LITotal: LITotal = %s", money8.asString());
                money5 = money5.add(money8);
            }
            if(l == 2 && i == -1)
            {
                i = k;
                Log.customer.debug("VATLine Index: " + i);
            }
        }

        if(i != -1)
        {
            BaseObject baseobject = (BaseObject)list.get(i);
            if(((ProcureLineType)baseobject.getFieldValue("LineType")).getCategory() == 2)
                baseobject.setDottedFieldValue("Amount", money5);
            i = -1;
            money6 = money6.add(money5);
            money5 = new Money(Constants.ZeroBigDecimal, currency);
        }
        money1 = Money.add(money1, money3);
        Log.customer.debug("UpdateTotal: shippingTotal = %s", money3.asString());
        Log.customer.debug("UpdateTotal: taxTotal = %s", money2.asString());
        Log.customer.debug("UpdateTotal: subTotal = %s", money1.asString());
        Log.customer.debug("UpdateTotal: vatTotal = %s", money5.asString());
        valuesource.setDottedFieldValue("TotalInvoicedLessTax", money1);
        valuesource.setDottedFieldValue("TotalTax", money6);
        valuesource.setDottedFieldValue("TotalShipping", money3);
    }
}