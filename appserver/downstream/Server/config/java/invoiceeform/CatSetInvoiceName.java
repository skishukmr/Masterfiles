/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.base.core.BaseObject;

/**
    Defaults the Invoice name based on the Invoice Number.
*/
public class CatSetInvoiceName extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        String invNumber = (String)object.getFieldValue("InvoiceNumber");

        if (!StringUtil.nullOrEmptyString(invNumber)) {
            ((BaseObject) object).setDottedFieldValueRespectingUserData("Name", "Invoice: " + invNumber);
        }
    }
}
