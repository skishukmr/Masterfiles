/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.

    Responsible: ariba
*/

package config.java.invoiceeform;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;

/**
    Checks that the OrderLineNumber is valid on charge category line items.
    The orderline number should be such that there is atleast one
    non-charge line item (for the same order) whose orderline number matches with this
    one.
*/
public class ValidOrderLine extends Condition
{
    private static final String requiredParameterNames[] = {"LineItem"};
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem",
                               0,
                               "config.java.invoiceeform.InvoiceEformLineItem")
    };

    private static final String ComponentStringTable = "aml.InvoiceEform";

    protected static final String InvalidLineItemKey = "InvalidLineItem";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
            // Get the invoice line item
        BaseObject eformLI = (BaseObject)
            params.getPropertyForKey("LineItem");

        BaseObject eform = eformLI.getClusterRoot();
        Assert.that(eform != null, "Cluster root not set");
        return validateLine(eform, eformLI);
    }

    /**
        Tests the condition and return an error message
    */
    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               InvalidLineItemKey,
                                               subjectForMessages(params)));
        }
        else {
            return null;
        }
    }

    /**
        Returns the valid parameter types
    */
    protected ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    /**
        Returns required parameter names for the class
    */
    protected String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }

    private boolean validateLine(BaseObject eform, BaseObject eformLI)
    {
        ProcureLineType lineType = (ProcureLineType)
            eformLI.getFieldValue("LineType");

        if (InvoiceEformUtil.isLinkableChildType(lineType)) {

            int orderLineNum = IntegerFormatter.getIntValue(
                    eformLI.getFieldValue("OrderLineNumber"));

            if (orderLineNum != 0 && !InvoiceEformUtil.hasParent(eform, eformLI)) {
                return false;
            }
        }

        return true;
    }


}
