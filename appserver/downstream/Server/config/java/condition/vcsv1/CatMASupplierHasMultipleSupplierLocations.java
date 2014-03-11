package config.java.condition.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Supplier;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatMASupplierHasMultipleSupplierLocations extends Condition
{

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        boolean result = false;
        Log.customer.debug("%s *** Object: %s", "CatMASupplierHasMultipleSupplierLocations", object);
        if(object instanceof ContractRequest)
        {
        	ContractRequest mar = (ContractRequest)object;
            Supplier supplier = mar.getSupplier();
            if(supplier != null)
            {
                BaseVector locations = supplier.getLocations();
                if(locations != null && locations.size() == 1)
                    result = true;
                else
                    result = false;
                if(!result)
                {
                    mar.setSupplierLocation(null);
                    Log.customer.debug("CatMASupplierHasMultipleSupplierLocations *** result is false, set loc to null");
                }
            }
        }
        Log.customer.debug("CatMASupplierHasMultipleSupplierLocations *** Result = " + result);
        return result;
    }

    public CatMASupplierHasMultipleSupplierLocations()
    {
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String classname = "CatMASupplierHasMultipleSupplierLocations";
    private static final ValueInfo parameterInfo[] = new ValueInfo[0];
    private static final String requiredParameterNames[] = new String[0];

}
