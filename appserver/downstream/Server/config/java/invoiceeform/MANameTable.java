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
import ariba.contract.core.Contract;
import ariba.util.core.Assert;
import ariba.util.core.Constants;

/**
    Adds the constraint that the MA is open and belongs to the
    same supplier as the invoice.
*/
public class MANameTable extends AQLNameTable
{

    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    public static final String ClassName =
        "ariba.contract.core.Contract";
    public static final String FormClassName =
        "config.java.invoiceeform.InvoiceEform";

    /*-----------------------------------------------------------------------
        Override NamedObjectNameTable
      -----------------------------------------------------------------------*/

    public void addQueryConstraints (AQLQuery query, String field, String pattern)
    {
            // Add the default field constraints for the pattern
        super.addQueryConstraints(query, field, pattern, null);

            // Add the constraint of an open MA
        query.andEqual(query.buildField("MAState"),
                       Constants.getInteger(Contract.MAStateOpen));

            // Get the supplier from the InvoiceEform
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
            query.andEqual(query.buildField("CommonSupplier"),
                           supplier.getCommonSupplier());
        }
    }
}

