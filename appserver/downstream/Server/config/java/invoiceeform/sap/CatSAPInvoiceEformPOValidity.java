/****************************************************************************************
# Change History
# Change By				Change Date				Description
=============================================================================================
# Shailaja Salimath 	10/29/2009				Issue 982. Adding nametable to order field in inv eform
# Arasan Rajendren		03/17/2011				Modified the Validation to Line Level for 9R1 Upgrade
# Vikram J Singh		08/03/2011				Allow to create invoice for an order after credit invoice
# Abhishek Kumar		03/22/2012				Add the PO validity logic for LSAP partition
*****************************************************************************************/
package config.java.invoiceeform.sap;

import java.math.BigDecimal;
import java.util.List;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLCondition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Money;
import ariba.invoicing.core.Invoice;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPInvoiceEformPOValidity extends Condition
{
    private static final String className = "CatSAPInvoiceEformPOValidity: ";
    private static final String requiredParameterNames[] = {"LineItem"};
    private static ValueInfo parameterInfo[] = { new ValueInfo("LineItem", 0, "config.java.invoiceeform.InvoiceEformLineItem") };
    private static final String POClassName = "ariba.purchasing.core.PurchaseOrder";
    private String errorMessage = null;
    private static final String StringTable = "cat.java.sap";

    /*
     *  InvalidSupplier = "Please set the Supplier field.";
     *  WrongSupplier = "PO Supplier does not match Invoice Eform Supplier";
     *  FullyInvoiced1 = "This PO has been fully invoiced. No lines are available for invoicing.";
     *  FullyInvoiced2 = "The PO Number entered has a new version ("
     *  FullyInvoiced3 = "). The new PO has been fully invoiced. No lines are available for invoicing."
     *  InvalidOrder = "Purchase Order Entered Not Valid."
     */


    public CatSAPInvoiceEformPOValidity() {
        super();
    }

    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {
        return evaluateImpl(object,params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
        String FullyInvoiced1 = (String) ResourceService.getString(StringTable, "FullyInvoiced1");
        String FullyInvoiced2 = (String) ResourceService.getString(StringTable, "FullyInvoiced2");
        String FullyInvoiced3 = (String) ResourceService.getString(StringTable, "FullyInvoiced3");
        String InvalidOrder = (String) ResourceService.getString(StringTable, "InvalidOrder");

        String orderNumber = (String) value;
        BaseObject invoiceLineItem = (BaseObject) params.getPropertyForKey("LineItem");
        ClusterRoot invoiceEForm = invoiceLineItem != null ? invoiceLineItem.getClusterRoot() : null;

        if(invoiceEForm!= null && (invoiceEForm.getPartition().getName().equals("SAP")||invoiceEForm.getPartition().getName().equals("LSAP"))) {
			Log.customer.debug("CatSAPInvoiceEformPOValidity: Enters the validity logic");
	        orderNumber = orderNumber.toUpperCase();
	        PurchaseOrder order = (PurchaseOrder) PurchaseOrder.lookupByOrderID(orderNumber, AQLCondition.parseCondition("UniqueName IS NOT NULL"));
	        Log.customer.debug(className + " Found Order %s ", order);

	        if(order == null) {
	        	 errorMessage = InvalidOrder;
	        	 return false;
	        }

	        boolean fullyInvoiced = true;
	        while (order.getNextVersion() != null) {
	        	order = (PurchaseOrder) order.getNextVersion();
	        }

	        Log.customer.debug(className + " Validating Order # %s, Version # %s", order.getOrderID(), order.getVersion());
	        List poLines = order.getLineItems();
	        int lines = ListUtil.getListSize(poLines);
	        for (int j = 0; j < lines; j++)
	        {
	            POLineItem poLI = (POLineItem) poLines.get(j);
	            BigDecimal numberOrderedLessInvoiced = poLI.getNumberOrderedLessInvoiced();
	            BigDecimal numberOrderedLessReconciled = poLI.getNumberOrderedLessReconciled();

				Money amtAccepted;
				Money amtInvoiced;

				BigDecimal amountAccepted = new BigDecimal("0.00");
				BigDecimal amountInvoiced = new BigDecimal("0.00");

				amtAccepted = (Money)poLI.getFieldValue("AmountAccepted");
				if (amtAccepted!=null){
					amountAccepted = (BigDecimal)amtAccepted.getAmount();
				}
				Log.customer.debug(className + "Amount Accepted: %s", amountAccepted);

				amtInvoiced = (Money)poLI.getFieldValue("AmountInvoiced");
				if (amtInvoiced!=null)
				{
					amountInvoiced = (BigDecimal)amtInvoiced.getAmount();
				}
				Log.customer.debug(className + "Amount Invoiced: %s", amountInvoiced);

	            BigDecimal zeroBD = new BigDecimal(0);

				// Added the last (3rd) additional condition so as to accept creation of invoices after credit invoice has been done for the PO.
	            if (numberOrderedLessInvoiced.compareTo(zeroBD) > 0 || numberOrderedLessReconciled.compareTo(zeroBD) > 0 || amountAccepted.compareTo(amountInvoiced) > 0) {
	                fullyInvoiced = false;
	            }
	        }

	        boolean isCreditMemo = Invoice.PurposeCreditMemo.equals((String) invoiceEForm.getFieldValue("Purpose"));
	        if (fullyInvoiced && !isCreditMemo) {
	            if (order.getPreviousVersion() == null)
	                errorMessage = FullyInvoiced1;
	            else
	                errorMessage = FullyInvoiced2 + order.getUniqueName() + FullyInvoiced3;
	            return false;
	        }
			Log.customer.debug("CatSAPInvoiceEformPOValidity: condition bypassed the validation");
	        return true;

        } else {
        	Log.customer.debug(className + " Not a Valid Partition to perform this Validation, Skipping this Validation!!!");
        	return true;
        }
	}

    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {

            Log.customer.debug(className + " Returning condition result with message: " + errorMessage);
            return new ConditionResult(errorMessage);
        }
        else
        {
            return null;
        }
    }

    protected ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }
}
