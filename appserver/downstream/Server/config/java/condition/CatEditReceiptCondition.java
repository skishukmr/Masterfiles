package config.java.condition;

import ariba.base.fields.Behavior;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Core;
import ariba.common.core.Log;
import ariba.common.core.User;
import ariba.receiving.core.Receipt;
import ariba.user.core.Permission;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;

public class CatEditReceiptCondition extends Condition
{

    public CatEditReceiptCondition()
    {
    }

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        Object recObj = params.getPropertyForKey("ReceiptObject");
        Log.customer.debug("**%s**Object got for ReceiptObject param=" + recObj + "", "CatEditReceiptCondition");
        String permName = params.stringPropertyForKey("PermissionName");
        Permission permission = Permission.getPermission(permName);
        if(recObj != null && (recObj instanceof Receipt))
        {
            Receipt receipt = (Receipt)recObj;
            int approvedState = IntegerFormatter.getIntValue(receipt.getFieldValue("ApprovedState"));
            int processedState = IntegerFormatter.getIntValue(receipt.getFieldValue("ProcessedState"));
            Log.customer.debug("**%s**approvedstate of receipt==" + approvedState + "", "CatEditReceiptCondition");
            Log.customer.debug("**%s**processedState of receipt==" + processedState + "", "CatEditReceiptCondition");
            if(approvedState == 4)
            {
                User user = Core.getService().getEffectiveUser();
                Log.customer.debug("**%s**User lookingup this receipt==" + user, "CatEditReceiptCondition");
                boolean userHasPermission = user.getUser().hasPermission(permission);
                Log.customer.debug("**%s**User has permission==" + userHasPermission, "CatEditReceiptCondition");
                if(userHasPermission)
                    return null;
                else
                    return new ConditionResult(Fmt.Sil("ariba.common.core.condition", "UserHasPermissionMsg1", ""));
            }
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

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String thisclass = "CatEditReceiptCondition";
    protected static final ValueInfo parameterInfo[];
    protected static final ValueInfo valueInfo;
    private static final String requiredParameterNames[] = {
        "PermissionName", "ReceiptObject"
    };

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("ReceiptObject", 0, "ariba.receiving.core.Receipt"), new ValueInfo("PermissionName", 0, Behavior.StringClass)
        });
        valueInfo = new ValueInfo(0, Constants.ObjectType, Behavior.IntType, Behavior.LongType, Behavior.DoubleType);
    }
}
