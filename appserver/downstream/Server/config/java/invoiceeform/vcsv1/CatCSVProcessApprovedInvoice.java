// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:20 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoiceeform.vcsv1;

import java.util.ArrayList;
import java.util.List;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.util.core.PropertyTable;

public class CatCSVProcessApprovedInvoice extends Action
{

    public CatCSVProcessApprovedInvoice()
    {
        invoice = null;
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Entering the fire method", "CatCSVProcessApprovedInvoice");
    }

    public static List reorderINVLineItems(List list)
    {
        ArrayList arraylist = null;
        ArrayList arraylist1 = new ArrayList();
        ArrayList arraylist2 = new ArrayList();
        ArrayList arraylist3 = new ArrayList();
        Object obj = null;
        Object obj1 = null;
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Entering the reorderINVLineItems method", "CatCSVProcessApprovedInvoice");
        if(list != null && !list.isEmpty())
        {
            int i = list.size();
            for(int j = 0; j < i; j++)
            {
                InvoiceLineItem invoicelineitem = (InvoiceLineItem)list.get(j);
                Integer integer = (Integer)invoicelineitem.getFieldValue("ReferenceLineNumber");
                if(integer != null && integer.intValue() == invoicelineitem.getNumberInCollection())
                {
                    arraylist1.add(invoicelineitem);
                    continue;
                }
                if(integer != null && integer.intValue() == 0)
                    arraylist3.add(invoicelineitem);
                else
                    arraylist2.add(invoicelineitem);
            }

            int k = arraylist3.size();
            int l = arraylist1.size();
            int i1 = arraylist2.size();
            Log.customer.debug("%s ::: Line Counts(Material/AC/Tax): " + l + "/" + i1 + "/" + k, "CatCSVProcessApprovedInvoice");
            arraylist = new ArrayList();
            if(l > 0)
            {
                for(int j1 = 0; j1 < l; j1++)
                {
                    InvoiceLineItem invoicelineitem1 = (InvoiceLineItem)arraylist1.get(j1);
                    int l1 = arraylist.size();
                    //if(Log.customer.debugOn)
                        Log.customer.debug("%s ::: Updated Material Ref Num From " + invoicelineitem1.getDottedFieldValue("ReferenceLineNumber") + "to " + (l1 + 1), "CatCSVProcessApprovedInvoice");
                    invoicelineitem1.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                    arraylist.add(invoicelineitem1);
                    if(i1 > 0)
                    {
                        for(int i2 = 0; i2 < i1; i2++)
                        {
                            InvoiceLineItem invoicelineitem2 = (InvoiceLineItem)arraylist2.get(i2);
                            Integer integer1 = (Integer)invoicelineitem2.getFieldValue("ReferenceLineNumber");
                            Log.customer.debug("%s ::: refNumInt: %s", "CatCSVProcessApprovedInvoice", integer1);
                            if(integer1 == null || integer1.intValue() != invoicelineitem1.getNumberInCollection())
                                continue;
                            //if(Log.customer.debugOn)
                                Log.customer.debug("%s ::: Updated AC Ref Num From " + invoicelineitem2.getDottedFieldValue("ReferenceLineNumber") + "to " + (l1 + 1), "CatCSVProcessApprovedInvoice");
                            invoicelineitem2.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                            arraylist.add(invoicelineitem2);
                        }

                    }
                }

            } else
            {
                return list;
            }
            if(k > 0)
            {
                for(int k1 = 0; k1 < k; k1++)
                    arraylist.add((InvoiceLineItem)arraylist3.get(k1));

            }
        }
        return arraylist;
    }

    private static final String ClassName = "CatCSVProcessApprovedInvoice";
    private Invoice invoice;
}