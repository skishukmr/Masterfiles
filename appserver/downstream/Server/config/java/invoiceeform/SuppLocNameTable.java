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
import ariba.common.core.SupplierLocation;
import ariba.util.core.Assert;

/*
    Adds a constraint that the SupplierLocation entries match the Invoice's supplier.
*/
public class SuppLocNameTable extends AQLNameTable
{
    public static final String ClassName =
        "config.java.invoiceeform.SuppLocNameTable";
    public static final String FormClassName =
       "config.java.invoiceeform.InvoiceEform";

    public void addQueryConstraints (AQLQuery query, String field, String pattern)
    {
            // Add the default field constraints for the pattern
        super.addQueryConstraints(query, field, pattern, null);

            // Get the supplier from the header data of the InvoiceEform
        ValueSource context = getValueSourceContext();
        Assert.that(context != null,
                    "context must exist");
        Assert.that(context.getTypeName().equals(FormClassName),
                    "context %s must be of type %s",
                    context,
                    FormClassName);

        Supplier supplier  = (Supplier)
            ((BaseObject)context).getFieldValue("Supplier");
        if (supplier != null) {
            query.andEqual(SupplierLocation.KeySupplier, supplier);
        }
    }
}
