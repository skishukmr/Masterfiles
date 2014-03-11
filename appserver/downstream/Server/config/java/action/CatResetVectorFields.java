/*
Modified By Amit Kumar on Jan 4th 2008 - Issue 728 - warning removed from logs for setting basevector
*/

package config.java.action;

import java.util.List;

import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Log;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.base.fields.action.SetField;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

public class CatResetVectorFields extends SetField
{

    public void fire(ValueSource object, PropertyTable params)
    {
        Log.customer.debug(" %s firing trigger..vs=" + object, "CatResetVectorFields: ");
        if(object != null)
        {
            ClusterRoot cr = (ClusterRoot)object;
            Log.customer.debug("%s: cr = " + cr, "CatResetVectorFields: ");
            List targetValues = getTargetValues(params);
            for(int i = targetValues.size() - 1; i >= 0; i--)
            {
                String targetValue = (String)targetValues.get(i);
                Log.customer.debug("%s targetValue=%s", "CatResetVectorFields: ", targetValue);
                BaseVector vecToClear = (BaseVector)cr.getFieldValue(targetValue);
                Log.customer.debug("CatResetVectorFields: vecToClear is: " + vecToClear);
                if(vecToClear != null)
                {
                    vecToClear.clear();
                    //cr.setFieldValue(targetValue, vecToClear); [ Removed to avoid Data Type Mismatch Warning ]
                }
            }

        }
    }

    private final List getTargetValues(PropertyTable params)
    {
        List targetValues = ListUtil.list();
        Object targetValue = params.getPropertyForKey("TargetValue");
        if(targetValue != null)
            targetValues.add(targetValue);
        targetValue = params.getPropertyForKey("TargetValue1");
        if(targetValue != null)
            targetValues.add(targetValue);
        targetValue = params.getPropertyForKey("TargetValue2");
        if(targetValue != null)
            targetValues.add(targetValue);
        targetValue = params.getPropertyForKey("TargetValue3");
        if(targetValue != null)
            targetValues.add(targetValue);
        return targetValues;
    }

    public CatResetVectorFields()
    {
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String classname = "CatResetVectorFields: ";
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("TargetValue", 0), new ValueInfo("TargetValue1", 0), new ValueInfo("TargetValue2", 0), new ValueInfo("TargetValue3", 0)
    };
    private static final String requiredParameterNames[] = {
        "TargetValue"
    };
    private static final ValueInfo valueInfo = new ValueInfo(0);

}
