/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/22/2006
	Description: 	Action implementation to set/clear the HoldEscalationOwner
					field.
					- When user selects to hold their name/uniquename is
					  populated in HoldEscalationOwner field.
					- As soon as the hold escalation flag is unchecked it
					  clears the HoldEscalationOwner field.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;

public class CatEZOSetHoldEscalationOwner extends Action
{
	private static final String ClassName = "CatEZOSetHoldEscalationOwner";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof Requisition)
		{
			Requisition r = (Requisition) object;
			Boolean hold = (Boolean) r.getFieldValue("HoldEscalation");
			String owner = (String) r.getFieldValue("HoldEscalationOwner");
			Log.customer.debug("%s ::: HOLD/OWNER (before): %s / %s", ClassName, hold, owner);
			if (Boolean.TRUE.equals(hold))
			{
				ClusterRoot activeUser = Base.getSession().getRealUser();
				Log.customer.debug("%s ::: USER: %s",ClassName,activeUser);
				if (activeUser != null && activeUser instanceof User)
				{
					User user = (User) activeUser;
					activeUser = null;
					if (user.getName() != null){
						owner = user.getName().getPrimaryString();
					}
					else{
						owner = user.getUniqueName();
					}
					Log.customer.debug("%s ::: HoldEscalation is TRUE, setting OWNER to: %s", ClassName, owner);
					r.setFieldValue("HoldEscalationOwner", owner);
					Log.customer.debug("%s ::: OWNER (after): %s",ClassName,r.getFieldValue("HoldEscalationOwner"));
				}
			}
			else
				if (Boolean.FALSE.equals(hold))
				{
					Log.customer.debug("%s *** HoldEscalation is FALSE, resetting Owner to null!", ClassName);
					r.setFieldValue("HoldEscalationOwner", null);
				}
		}
	}

	public CatEZOSetHoldEscalationOwner()
	{
		super();
	}
}
