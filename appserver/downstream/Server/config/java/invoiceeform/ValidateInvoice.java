/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Supplier;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;

/**
    Checks whether another invoice has been created for the same supplier and invoice number.
*/
public class ValidateInvoice extends Condition
{

    private static final ValueInfo valueInfo = new ValueInfo(0);
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("Invoice",
                               0,
                               "config.java.invoiceeform.InvoiceEform")
    };
    private static final String requiredParameterNames[] = {
           "Invoice"
    };

    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String StringTable = "ariba.common.core.condition";

    private static final String ErrorMsgKey = "DuplicateInvoice";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        boolean isValid = evaluate(value, params);

        if (isValid) {
            return null;
        }
        else {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               "DuplicateInvoice",
                                               subjectForMessages(params)));
        }
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
        ClusterRoot invoice = (ClusterRoot)params.getPropertyForKey("Invoice");

        Supplier supplier = (Supplier)invoice.getFieldValue("Supplier");
        String number = (String)invoice.getFieldValue("InvoiceNumber");

            // Just return OK if supplier or number not set yet
        if (supplier == null ||
            StringUtil.nullOrEmptyOrBlankString(number)) {
            return true;
        }

            // Setup the query to search for an invoice with same supplier and number
        AQLQuery query = AQLQuery.parseQuery(
            Fmt.S("SELECT InvoiceEform " +
                  "FROM config.java.invoiceeform.InvoiceEform " +
                  "WHERE Supplier = %s " +
                  "AND InvoiceNumber = '%s'",
                  AQLScalarExpression.buildLiteral(supplier).toString(),
                  number));

            // Execute the query
        AQLOptions options = new AQLOptions(Base.getSession().getPartition());
        AQLResultCollection results = Base.getService().executeQuery(query,options);

            // If matching invoice found, check if it is actually this one
        boolean valid = true;
        while (results.next()) {
            BaseId baseId = results.getBaseId(0);

                // If not equal, the another one already exists, so return false
            if (!SystemUtil.equal(baseId, invoice.getBaseId())) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    /**
        Returns the valueInfo
    */
    public ValueInfo getValueInfo ()
    {
        return valueInfo;
    }

    /**
        Returns the valid parameter types
    */
    public ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    /**
        Returns required parameter names for the class
    */
    public String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }

}

