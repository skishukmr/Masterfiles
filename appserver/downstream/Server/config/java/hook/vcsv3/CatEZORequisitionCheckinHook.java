/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/05/2006
	Description: 	CheckIn Hook initial implementation.  Like submit hook,
					hence uses Submit Hook implementation.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.hook.vcsv3;

import java.util.HashMap;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;

public class CatEZORequisitionCheckinHook implements ApprovableHook {

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String ClassName = "CatEZORequisitionCheckinHook";

	public List run(Approvable approvable) {

        if (approvable instanceof Requisition) {
        	Requisition r = (Requisition)approvable;

			 /* Run SubmitHook Checks */
	  	 	 CatEZORequisitionSubmitHook submithook = new CatEZORequisitionSubmitHook();
	  	 	 if (submithook != null)  {
	  	 	     submithook.isEdit = true;
	  	 	     return submithook.run(r);
	  	 	 }
	  	 	 submithook = null;
        }
		return NoErrorResult;
	}

	public CatEZORequisitionCheckinHook() {
		super();
	}

	private HashMap getPropertyMap(String permission)
	{
		HashMap map = new HashMap();
			map.put("PermissionName",permission);
		return map;
	}

}
