/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/04/2006
	Description: 	Action implementation to default the DeliverToDept from
					the Accounting Dept in case if it is not populated.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.User;
import ariba.common.core.UserProfileDetails;
import ariba.procure.core.Log;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

public class CatEZOSetDeliverToDeptFromAccntngDept extends Action
{
	private static final String ClassName = "CatEZOSetDeliverToDeptFromAccntngDept";

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof UserProfileDetails){
			UserProfileDetails upd = (UserProfileDetails) vs;
			if (upd.getName() != null && upd.getAccounting() != null){
				String userName = (String) upd.getName().getPrimaryString();
				User profileUser = (User) getUserFromName(userName, upd.getPartition());

				String department = (String) profileUser.getDottedFieldValue("Accounting.Department");
				String deliverToDept = (String) profileUser.getFieldValue("DeliverToDept");
				Log.customer.debug("%s ::: The current Accounting Department is: %s", ClassName, department);
				Log.customer.debug("%s ::: The current DeliverTo is: %s", ClassName, deliverToDept);
				if (StringUtil.nullOrEmptyOrBlankString(deliverToDept)){
					Log.customer.debug("%s ::: Setting DeliverToDept to: %s", ClassName, department);
					upd.setDottedFieldValueRespectingUserData("DeliverToDept",department);
				}
			}
			else{
				Log.customer.debug("%s ::: Name or Accounting is NULL: %s/%s", ClassName, upd.getName(), upd.getAccounting());
			}
		}
		else{
			Log.customer.debug("%s ::: The ValueSource is not UPD but: %s/%s", ClassName, vs.getClass().toString(),vs);
		}
	}

	public static ClusterRoot getUserFromName(String userName, Partition p) {
		AQLQuery query =
		AQLQuery.parseQuery(Fmt.S("SELECT \"User\" " + "FROM ariba.common.core.\"User\" " + "WHERE Name.PrimaryString = '" + userName + "'"));
		Log.customer.debug("%s ::: The query ran for finding user by the name is: \n%s", ClassName, query.toString());
		AQLOptions options = new AQLOptions(p);
		AQLResultCollection results = Base.getService().executeQuery(query, options);
		Log.customer.debug("%s ::: Size of the result collection/isEmpty: " + results.getSize() + "/" + results.isEmpty(), ClassName);
		if (results.next()) {
			Log.customer.debug("%s ::: ResultCollection class: %s", ClassName, results.getObject(0).getClass().toString());
			BaseId userBID = (BaseId) results.getObject(0);
			User partUserForProfile = (User) userBID.getIfAny();
			//ariba.common.core.User partUserForProfile = ariba.common.core.User.getPartitionedUser(unpartUserForProfile, p);
			Log.customer.debug("%s ::: Returning user: %s", ClassName, partUserForProfile.getName());
			return partUserForProfile;
		}
		return null;
	}

	public CatEZOSetDeliverToDeptFromAccntngDept()
	{
		super();
	}
}