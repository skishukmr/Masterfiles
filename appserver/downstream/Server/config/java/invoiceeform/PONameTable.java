/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.


    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.core.BaseObject;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.util.core.Assert;

/**
    Adds the constraint that the supplier matches the supplier of the Invoice Eform
*/
public class PONameTable extends AQLNameTable
{
    public static final String ClassName =
        "config.java.invoiceeform.Orders";
    public static final String FormClassName =
        "config.java.invoiceeform.InvoiceEform";

    public void addQueryConstraints (AQLQuery query, String field, String pattern)
    {
            // Add the default field constraints for the pattern

            // S. Sato - AUL - moved deprecated method to point to this one
            // with four arguments
        super.addQueryConstraints(query, field, pattern, null);

            // get the supplier from the header data of the Invoiceeform
        ValueSource context = getValueSourceContext();
        Assert.that(context != null,
                    "context must exist");
        Assert.that(context.getTypeName().equals(FormClassName),
                    "context %s must be of type %s",
                    context,
                    FormClassName);

        Supplier supplier = (Supplier)
            ((BaseObject)context).getFieldValue("Supplier");
        if (supplier != null) {
            query.andEqual(query.buildField("Supplier"), supplier);
        }
    }
}
