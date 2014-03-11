
package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 * AUL : Remove all debugon if statements
 */

public class CatEZOAssignUserIRPermissions extends Action {

    private static final String ClassName = "CatEZOAssignUserIRPermissions";
	private static final String PostLoadIndicator = ResourceService.getString("cat.invoicejava.vcsv3", "Action_UserPostLoadIndicator");
    private static final String AddPermissions = ResourceService.getString("cat.invoicejava.vcsv3", "Action_UserIRPermissions_Add");
    private static final String RemovePermissions = ResourceService.getString("cat.invoicejava.vcsv3", "Action_UserIRPermissions_Remove");

    public void fire(ValueSource object, PropertyTable params)
    {
        if (!loadPermissions())
            return;

        ariba.common.core.User pUser = (ariba.common.core.User)object;
        User user = pUser.getUser();
        Log.customer.debug("%s *** pUser UniqueName: %s", ClassName, pUser.getUniqueName());

        Permission irPermission = null;

        // 1. Remove IR related permissions for R5 users
        if (!StringUtil.nullOrEmptyOrBlankString(RemovePermissions)) {
            String [] deletions = StringUtil.delimitedStringToArray(RemovePermissions,',');
	        if (deletions != null) {
	            BaseVector permissions = user.getPermissions();
	            /* if (Log.customer.debugOn)
	                Log.customer.debug("CatEZOAssignUserIRPermissions *** Permissions(before deletions)" +
	                        permissions.size());*/
	            int size = deletions.length;
	            while (size >0){
	                irPermission = (Permission)Base.getService().objectMatchingUniqueName(
	                        "ariba.user.core.Permission",Partition.None, deletions[--size]);
	                if (irPermission != null) {
	                    permissions.remove(irPermission);
                    	Log.customer.debug("%s *** Removing Permission: %s",ClassName,irPermission.getUniqueName());
	                }
	            }
	            /* if (Log.customer.debugOn)
	                Log.customer.debug("CatEZOAssignUserIRPermissions *** Permissions(after deletions)" +
	                        user.getPermissions().size()); */
	        }
        }
        else {
           	Log.customer.debug("%s *** No Permission Deletions required!",ClassName);
        }
        // 2. Add IR related permissions for R5 users
        if (!StringUtil.nullOrEmptyOrBlankString(AddPermissions)) {
	        String [] additions = StringUtil.delimitedStringToArray(AddPermissions,',');
	        if (additions != null) {
	            BaseVector permissions = user.getPermissions();
	            /* if (Log.customer.debugOn)
	                Log.customer.debug("CatEZOAssignUserIRPermissions *** Permissions(before additions)" +
	                        permissions.size()); */
	            int size = additions.length;
	            while (size >0){
	                irPermission = (Permission)Base.getService().objectMatchingUniqueName(
	                        "ariba.user.core.Permission",Partition.None, additions[--size]);
	                if (irPermission != null && !permissions.contains(irPermission)) {
	                    permissions.add(irPermission);
                    	Log.customer.debug("%s *** Adding Permission: %s",ClassName,irPermission.getUniqueName());
	                }
	            }
	            /* if (Log.customer.debugOn)
	                Log.customer.debug("CatEZOAssignUserIRPermissions *** Permissions(after additions)" +
	                        user.getPermissions().size()); */
	        }
        }
        else {
           	Log.customer.debug("%s *** No Permission Additions required!",ClassName);
        }
    }

	private static boolean loadPermissions() {

	    boolean isLoad = false;

	    if (!StringUtil.nullOrEmptyOrBlankString(PostLoadIndicator)) {
		    Integer loadKey = Integer.valueOf(PostLoadIndicator);
		    if (loadKey != null && loadKey.intValue() == 1)
		        isLoad = true;
		}
	    return isLoad;
	}
}
