/*
 * CatIntegrationPostLoadUserPerm.java
 *
 * Trigger that loads permissions when CatInclusionsPull is run.
 * Update LastName in sharedUser
 * Kannan PGS Nov 11, 2009
 * Issue # 984
 *
 */
package config.java.action;

import java.util.List;
import java.util.Map;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.integration.core.Message;
import ariba.user.core.Log;
import ariba.user.core.Permission;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;


public class CatIntegrationPostLoadUserPerm extends Action {

    public void fire(ValueSource object, PropertyTable params) {

        Assert.that(object instanceof ariba.common.core.User, "Tried to call a non ariba.common.core.User object in %s", ClassName);

        Message message = (Message)params.getPropertyForKey("Message");
        Map requestTable = message.getMessageConfiguration().getRequestMessageParameters();
        String loadPermissions = (String)requestTable.get("LoadPermissions");
        ariba.common.core.User user = (ariba.common.core.User)object;
		ariba.user.core.User sharedUser = user.getUser();

        if("true".equalsIgnoreCase(loadPermissions)) {



            List permissionNames = (List)requestTable.get("PermissionsToAdd");
			List permissions = getPermissions(permissionNames);

			if(ListUtil.nullOrEmptyList(permissions)) {
				Log.user.debug("%s : there's no permission to add", ClassName);
				return;
			}

			if(!StringUtil.nullOrEmptyOrBlankString(m_errorMessage)) {
				Log.user.debug("%s : missing permissions %s", ClassName, m_errorMessage);
				return;
			} else {

				Log.user.debug("Calling %s on ariba.common.core.User %s", ClassName, user.getUniqueName());
				ListUtil.addElementsIfAbsent(sharedUser.getPermissions(), permissions);
			}

        }

        else {

			Log.user.debug("%s : LoadPermissions is off", ClassName);
	}
       // Update LastName
       // issue 984
      String lastName = (String) sharedUser.getFieldValue("LastName");
      // if(StringUtil.nullOrEmptyOrBlankString(lastName)) {

           String fullname	= sharedUser.getDottedFieldValue("Name.PrimaryString").toString();
           lastName = fullname.substring(fullname.lastIndexOf(" ")+1);
           Log.user.debug("Calling %s Setting Last Name %s", ClassName, lastName);
           sharedUser.setFieldValue("LastName", lastName.toUpperCase());
	  // }
    }

    private final List getPermissions(List permissionNames) {
        if(ListUtil.nullOrEmptyList(permissionNames))
            return null;
        List permissions = ListUtil.list();
        int i = 0;
        for(int size = permissionNames.size(); i < size; i++) {
            String permissionName = (String)permissionNames.get(i);
            Permission permission = Permission.getPermission(permissionName);
            if(permission == null) {
                m_errorMessage = Fmt.S("%s, %s", m_errorMessage, permissionName);
                Log.user.debug("%s : can't permission %s", ClassName, permissionName);
            } else {
                Log.user.debug("%s: got %s permission", ClassName, permissionName);
                permissions.add(permission);
            }
        }
        return permissions;
    }

    public CatIntegrationPostLoadUserPerm() { m_errorMessage = ""; }

    public static final String ClassName = "CatIntegrationPostLoadUserPerm";
    private static final String PermissionsKey = "PermissionsToAdd";
    private static final String LoadPermissionsKey = "LoadPermissions";
    private String m_errorMessage;
}
