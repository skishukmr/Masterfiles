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
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import ariba.util.core.Fmt;
import java.util.List;
import ariba.util.core.ListUtil;

/**
    Renumbers all of the line items in case any line items were deleted.
*/
public class GetNextInvoiceLineNumber extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        ClusterRoot invoice = ((BaseObject)object).getClusterRoot();

        if (invoice != null) {
            List lineItems = (List)invoice.getFieldValue("LineItems");

            int size = ListUtil.getListSize(lineItems);
            for (int i = 0; i < size; i++) {
                String path = Fmt.S("LineItems[%s].InvoiceLineNumber", i);
                object.setDottedFieldValue(path,
                                           Constants.getInteger(i+1));
            }
        }
    }
}
