package config.java.invoiceeform.sap;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import java.util.List;

public class CatSAPValidLineItems extends Condition{

    private static final String requiredParameterNames[] = {"invoice" };
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("invoice",
                               0,
                               "config.java.invoiceeform.InvoiceEform")
    };

    private static final String ComponentStringTable = "aml.cat.Invoice";

    protected static final String NoTaxLine = "NoTaxLine";
    
    int ErrorCode = 0;
    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {
        BaseObject invoice = (BaseObject)
            params.getPropertyForKey("invoice");

        // Go through and check all of the line items
        boolean valid = true;
        boolean hastaxline = false;

        List lineItems = (List)invoice.getFieldValue("LineItems");
        int size = lineItems.size();
        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);
        
            ProcureLineType lineType = (ProcureLineType)lineItem.getFieldValue("LineType");
            int category = lineType.getCategory();
            if(category == 2)
            {
            	hastaxline = true;
            }
        }
        if(size!=0 && !hastaxline)
        {
        	valid = false;
        	ErrorCode =2;
        }

        return valid;
    }

    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
       		return new ConditionResult(Fmt.Sil(ComponentStringTable,NoTaxLine));
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
