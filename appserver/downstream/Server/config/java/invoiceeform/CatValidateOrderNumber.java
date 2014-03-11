/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to validate the order number entered on the invoice eform line item.
*/

package config.java.invoiceeform;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLCondition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;

public class CatValidateOrderNumber extends Condition
{
    private static final String requiredParameterNames[] = {"LineItem"};
    private static final String poClassName = "ariba.purchasing.core.PurchaseOrder";
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem",
                               0,
                               "config.java.invoiceeform.InvoiceEformLineItem")
    };

    private static final String ComponentStringTable = "aml.cat.Invoice";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
        // Get the invoice line item
        BaseObject invoiceLineItem = (BaseObject) params.getPropertyForKey("LineItem");

        // Get the order number
        String invoiceOrderNumber = (String)value;

        // And then it must at least be non-null
        if (invoiceOrderNumber == null) {
            return false;
        }

        invoiceOrderNumber = invoiceOrderNumber.toUpperCase();
        invoiceLineItem.setFieldValue("OrderNumber", invoiceOrderNumber);

        ClusterRoot cr = Base.getService().objectMatchingUniqueName(poClassName, invoiceLineItem.getPartition(), invoiceOrderNumber);

        if (cr == null) {
        	/*
        	 * AUL, sdey : In 9r1 contract based order has differnt UniqueName than OrderID.
        	 * Need to look up agaist the OrderID before returning false.
        	 */

        	cr = PurchaseOrder.lookupByOrderID(invoiceOrderNumber, AQLCondition.parseCondition("UniqueName IS NOT NULL"));

			if(cr == null)
				return false;

        	/*
        	 * AUL, sdey : In 9r1 contract based order has differnt UniqueName than OrderID.
        	 * Need to look up agaist the OrderID before returning false.
        	 */

		}

		return true;

    }

    /**
        Tests the condition and return an error message
    */
    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               "InvalidOrderNumber",
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
