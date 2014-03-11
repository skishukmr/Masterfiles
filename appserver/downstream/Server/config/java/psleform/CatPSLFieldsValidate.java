/*
 * CatPSLFieldsValidate.java
 * Created by Chandra
 *
 */
package config.java.psleform;

import ariba.approvable.core.Approvable;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Log;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;


/*
 *  Condition to check if the required fields are validated
 */
public class CatPSLFieldsValidate extends Condition {


    public CatPSLFieldsValidate() {}

    public boolean evaluate(Object value, PropertyTable params) {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
        Approvable psm = (Approvable)params.getPropertyForKey("PrefSupplierMaintEform");
        Log.customer.debug("**%s**approvable got="+psm+"", thisclass);

        String subj = subjectForMessages(params);
        Log.customer.debug("**%s**subject =%s", thisclass, subj);

        if(value != null)
            Log.customer.debug("**%s**value received=%s", thisclass, value.toString());

        if(psm != null) {
            String maintType  = (String) psm.getFieldValue("MaintenanceType");
            Log.customer.debug("**%s**maintType=%s", thisclass,maintType);

            if(!StringUtil.nullOrEmptyOrBlankString(maintType)
                    && !maintType.equalsIgnoreCase("Delete")
                    && value == null) {
                return new ConditionResult(Fmt.Sil("cat.psm.eform","MissingValueMsg",subj));
            }
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


    private static final String thisclass = "CatPSLFieldsValidate";

    protected static final ValueInfo parameterInfo[];
    protected static final ValueInfo valueInfo;
    private static final String requiredParameterNames[] = { "PrefSupplierMaintEform" };

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("PrefSupplierMaintEform", 0, "ariba.core.PrefSupplierMaintEform")
        });
        valueInfo = new ValueInfo(0, Constants.ObjectType, IntType, LongType, DoubleType);
    }
}
