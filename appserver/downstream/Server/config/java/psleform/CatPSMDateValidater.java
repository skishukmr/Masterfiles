/*
 * CatPSMDateValidater.java
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
 *  Condition to check if either the Expirydate or review date is entered
 */
public class CatPSMDateValidater extends Condition
{


    public CatPSMDateValidater() {}

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        Approvable psm = (Approvable)params.getPropertyForKey("PrefSupplierMaintEform");
        String fieldToTest = params.stringPropertyForKey("FieldName");

        //Log.customer.debug("**%s**approvable got="+psm+"", thisclass);
        //Log.customer.debug("**%s**FieldToTest got=%s", thisclass, fieldToTest);

        if(value != null)
            Log.customer.debug("**%s**value received=%s", thisclass, value.toString());

        if(psm != null && !StringUtil.nullOrEmptyOrBlankString(fieldToTest)) {
            ariba.util.core.Date dt = (ariba.util.core.Date)psm.getFieldValue(fieldToTest);
            Log.customer.debug("**%s**date got for %s="+dt+".", thisclass, fieldToTest);

            if(value == null && dt == null )
                return new ConditionResult(ResourceService.getString("cat.psm.eform","PSMDateValidaterMsg"));

        }
        return null;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    private static final String thisclass = "CatPSMDateValidater";

    protected static final ValueInfo parameterInfo[];
    protected static final ValueInfo valueInfo;

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("PrefSupplierMaintEform", 0, "ariba.core.PrefSupplierMaintEform"),
            new ValueInfo("FieldName", 0, StringClass)
        });
        valueInfo = new ValueInfo(0, Constants.ObjectType, IntType, LongType, DoubleType);
    }
}
