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
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.SystemUtil;

/**
    Checks that the UnitOfMeasure field exists and matches the UOM on the PO line.
*/
public class ValidUOM extends Condition
{
    private static final String requiredParameterNames[] = {"LineItem"};
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem",
                               0,
                               "config.java.invoiceeform.InvoiceEformLineItem")
    };

    private static final String ComponentStringTable = "aml.InvoiceEform";

    protected static final String InvalidUOMMsg = "InvalidUOM";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
            // Get the invoice line item
        BaseObject invoiceLineItem = (BaseObject)
            params.getPropertyForKey("LineItem");

            // Get the unit of measure
        UnitOfMeasure invoiceUOM = (UnitOfMeasure)value;

            // UOM is only necessary for line item category lines
        ProcureLineType lineType = (ProcureLineType)
            invoiceLineItem.getFieldValue("LineType");
        if (!ProcureLineType.isLineItemCategory(lineType)) {
            return true;
        }

            // And then it must at least be non-null
        if (invoiceUOM == null) {
            return false;
        } else {
			Log.customer.debug("UOM = " + invoiceUOM.getUniqueName());
		}

            // Otherwise check it against the UOM on the OrderItem if it exists
        POLineItem poLineItem = (POLineItem)
            invoiceLineItem.getFieldValue("OrderLineItem");

            // No POLineItem so nothing else to check
        if (poLineItem == null) {
            return true;
        } else {
			Log.customer.debug("PO Line = " + poLineItem.getNumberInCollection());
		}

            // Otherwise compare the UOMs
        return SystemUtil.equal(invoiceUOM,
                                poLineItem.getDescription().getUnitOfMeasure());
    }

    /**
        Tests the condition and return an error message
    */
    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               "InvalidUOM",
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

}
