package config.java.invoicing.vcsv3;

import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
 * @author kstanley
 * Simple condition to check line type from SplitAccounting or LineItem level.
 */
public class CatInvoiceLoadedFromCondition extends Condition {

	private static final String ClassName = "CatInvoiceLoadedFromCondition";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("TargetValue", IsScalar, "java.lang.String")};
 	private static final String requiredParameterNames[] = { "TargetValue", };

    public boolean evaluate(Object object, PropertyTable params) {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** object: %s",ClassName, object);

        Invoice inv = null;

        if (object instanceof Invoice)
            inv = (Invoice)object;

        else if (object instanceof InvoiceReconciliation)  {
            InvoiceReconciliation ir = (InvoiceReconciliation)object;
            inv = ir.getInvoice();
        }
        else if (object instanceof InvoiceLineItem) {
            InvoiceLineItem ili = (InvoiceLineItem)object;
            inv = ili.getInvoice();
        }
        else if (object instanceof InvoiceReconciliationLineItem) {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)object;
            inv = irli.getInvoice();
        }

        if (inv != null) {

            int loadedFrom = inv.getLoadedFrom();
            String source = null;

            if (loadedFrom == 1 || loadedFrom == 2)
                source = "e";
            else if (loadedFrom == 3 || loadedFrom == 4) {
                source = "p";
            }

            String testvalue = (String)params.getPropertyForKey("TargetValue");

            if (testvalue != null) {
                testvalue = testvalue.substring(0,1);
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** testValue substring: %s",ClassName, testvalue);
                if (testvalue.equalsIgnoreCase(source))
                    return true;
            }
        }
        return false;
    }

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

    public CatInvoiceLoadedFromCondition() {
        super();
    }

}
