package config.java.condition;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.fields.Behavior;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.Log;
import ariba.base.fields.Type;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.Variant;
import ariba.common.core.Core;
import ariba.common.core.User;
import ariba.user.core.Permission;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;

public class CatUserHasPermission extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        Log.customer.debug("CatUserHasPermission..");
        String permissionNames = params.stringPropertyForKey("PermissionName");
        List permissionsList = getPermissionsToEvaluate(permissionNames);
        if(permissionsList == null || permissionsList.isEmpty())
        {
            Log.constraints.warning(453, permissionNames);
            Log.customer.debug("permission list is empty..");
            return new ConditionResult(ResourceService.getString("ariba.common.core.condition", "PermissionsNonExistent"));
        }
        User user = Core.getService().getEffectiveUser();
        if(user == null)
        {
            Log.customer.debug("user is null....");
            return new ConditionResult(ResourceService.getString("ariba.common.core.condition", "UserNotSpecified"));
        }
        List l = user.getUser().getAllPermissions();
        Log.customer.debug("user is " + user.getUniqueName());
        for(int i = 0; i < l.size(); i++)
        {
            Permission p = (Permission)Base.getSession().objectFromId((BaseId)l.get(i));
            Log.customer.debug("permission is " + p.getUniqueName());
        }

        for(int i = 0; i < permissionsList.size(); i++)
        {
            Permission p = (Permission)Base.getSession().objectFromId((BaseId)permissionsList.get(i));
            Log.customer.debug("passed permission is " + p.getUniqueName());
            if(l.contains((BaseId)permissionsList.get(i)))
                return null;
        }

        Log.customer.debug("user does not have passed permission!!!...");
        String subject = subjectForMessages(params);
        return new ConditionResult(Fmt.Sil("ariba.common.core.condition", "UserHasPermissionMsg1", subject));
    }

    public List getPermissionsToEvaluate(String permissionNames)
    {
        char comma = ',';
        List permissionsList = ListUtil.list();
        String permNames[] = StringUtil.delimitedStringToArray(permissionNames, comma);
        for(int i = 0; i < permNames.length; i++)
        {
            Permission permission = Permission.getPermission(permNames[i]);
            if(permission != null)
                permissionsList.add(permission.getBaseId());
        }

        return permissionsList;
    }

    public StringArray checkConfigParameters(Type valueType, PropertyTable staticParams, PropertyTable dynParamTypes, PropertyTable outParamTypes, Variant variant)
    {
        StringArray errors = super.checkConfigParameters(valueType, staticParams, dynParamTypes, outParamTypes, variant);
        return errors;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    public CatUserHasPermission()
    {
    }

    public static final String PermissionNameParam = "PermissionName";
    private static final String requiredParameterNames[] = {
        "PermissionName"
    };
    private static final String UserHasPermissionMsg1 = "UserHasPermissionMsg1";
    private static final String UnknownPermissionMsg = "UnknownPermissionMsg";
    private static final String UserNotSpecified = "UserNotSpecified";
    private static final String PermissionsNonExistent = "PermissionsNonExistent";
    private static ValueInfo parameterInfo[];

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("PermissionName", 0, Behavior.StringClass)
        });
    }
}
