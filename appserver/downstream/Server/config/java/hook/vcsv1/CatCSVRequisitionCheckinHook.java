/* Rewritten for R4 by KS on 5 Oct 2005
 * --------------------------------------------------------------
 * Just like SubmitHook (so uses SubmitHook run()) plus 1 additional Tax override check
 */
package config.java.hook.vcsv1;

import java.util.HashMap;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.common.core.condition.UserHasPermission;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatCSVRequisitionCheckinHook implements ApprovableHook {

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String THISCLASS = "CatCSVRequisitionCheckinHook";
    private static final String TaxOverrideError = ResourceService.getString("cat.java.vcsv1","Error_TaxOverrideMustReassess");

	public List run(Approvable approvable) {

        if (approvable instanceof Requisition) {
        	Requisition r = (Requisition)approvable;

		    /* 1st - Check if Tax User with Override but AssessTax button not retriggered */
	  	 	 User editor = (User)Base.getSession().getEffectiveUser();
	  	 	 UserHasPermission uhp = new UserHasPermission();
	  	 	 PropertyTable ptable = new PropertyTable(getPropertyMap("CatTax"));
	  	 	 Log.customer.debug("%s **** ptable: %s",THISCLASS,ptable);
	  	 	 if (uhp.evaluate(r,ptable)) {
	  	 	     Log.customer.debug("%s **** USER IS CATTAX! ",THISCLASS);
	  	 	     Boolean isOverride = (Boolean)r.getFieldValue("TaxOverrideFlag");
	  	 	     if (isOverride != null && isOverride.booleanValue()){
	  	 	         Log.customer.debug("%s **** OVERRIDE IS TRUE SO ERROR! ",THISCLASS);
	  	 	         return ListUtil.list(Constants.getInteger(-1),TaxOverrideError);
	  	 	     }
	  	 	 }

			 /* 2nd - Run SubmitHook Checks */
	  	 	 CatCSVRequisitionSubmitHook submithook = new CatCSVRequisitionSubmitHook();
	  	 	 if (submithook != null)  {
	  	 	     submithook.isEdit = true;
	  	 	     return submithook.run(r);
	  	 	 }
	  	 	 // garbage truck just pulled up
	  	 	 submithook = null;
        }
		return NoErrorResult;
	}

	public CatCSVRequisitionCheckinHook() {
		super();
	}

	private HashMap getPropertyMap(String permission)
	{
		HashMap map = new HashMap();
			map.put("PermissionName",permission);
		return map;
	}

}
