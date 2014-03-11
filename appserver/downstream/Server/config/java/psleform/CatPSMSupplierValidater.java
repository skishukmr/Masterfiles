/*
 * CatPSMSupplierValidater.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

import ariba.approvable.core.Approvable;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Log;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;


/*
 *  Condition to check if Supplier is selected
 *    when the Maintenance Type has a value
 */
public class CatPSMSupplierValidater extends Condition
{


    public CatPSMSupplierValidater() {}

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        Approvable psm = (Approvable)params.getPropertyForKey("PrefSupplierMaintEform");
        String fieldToTest = params.stringPropertyForKey("FieldName");
        String targetValue = params.stringPropertyForKey("TargetValue");

        Log.customer.debug("**%s**approvable got="+psm+"", thisclass);
        //Log.customer.debug("**%s**FieldToTest got=%s", thisclass, fieldToTest);
        //Log.customer.debug("**%s**targetvalue got=%s", thisclass, targetValue);

        if(value != null)
            Log.customer.debug("**%s**value received=%s", thisclass, value.toString());

        if(psm != null && !StringUtil.nullOrEmptyOrBlankString(fieldToTest)) {
            String fieldValue  = (String)psm.getFieldValue(fieldToTest);

            if(value == null && fieldValue.equals(targetValue))
                return new ConditionResult(ResourceService.getString("cat.psm.eform","PSMSupplierValidaterMsg"));
        }
        return null;
    }

    protected ValueInfo[] getParameterInfo() {
        return parameterInfo;
    }

    protected ValueInfo getValueInfo() {
        return valueInfo;
    }

    protected String[] getRequiredParameterNames()  {
        return requiredParameterNames;
    }


    private static final String thisclass = "CatPSMSupplierValidater";

    protected static final ValueInfo parameterInfo[];
    protected static final ValueInfo valueInfo;
    private static final String requiredParameterNames[] = { "FieldName","TargetValue","PrefSupplierMaintEform" };

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("PrefSupplierMaintEform", 0, "ariba.core.PrefSupplierMaintEform"),
            new ValueInfo("FieldName", 0, StringClass),
            new ValueInfo("TargetValue", 0, StringClass)
        });
        valueInfo = new ValueInfo(0, Constants.ObjectType, IntType, LongType, DoubleType);
    }
}
