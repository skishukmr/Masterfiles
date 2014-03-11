/*
 * Created by KS on Nov 26, 2004
 */
package config.java.condition;

import ariba.base.fields.*;
import ariba.common.core.*;
import ariba.user.core.User;
import ariba.approvable.core.*;
import ariba.util.core.*;
import ariba.purchasing.core.*;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatValidDepartmentForUser extends Condition {

	private static final String classname = "CatValidDepartmentForUser";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestUser", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestUser" };
	private static final String StringTable = "cat.vcsv1";
    private static final String DEPT_CAPITAL = ResourceService.getString("cat.vcsv1","CaptialDepartmentValue");
	private static final String ErrorMessages[] = { "Error_Default", "Error_NoApproverForDept", 
													"Error_InvalidApproverForDept" };
	private static int reason;

	public boolean evaluate(Object object, PropertyTable params)
	throws ConditionEvaluationException {
		
		reason = 0;
		boolean result = true;
		if (object instanceof SplitAccounting){
			SplitAccounting sa = (SplitAccounting)object;
			String testuser = (String)params.getPropertyForKey("TestUser");
			String dept = (String)sa.getFieldValue("Department");
			String div = (String)sa.getFieldValue("Division");
			String sect = (String)sa.getFieldValue("Section");
			LineItem li = (LineItem)sa.getFieldValue("LineItem");
			if (li instanceof ReqLineItem && dept != null && div != null && sect != null) {
				ReqLineItem rli = (ReqLineItem)li;
				LineItemCollection lic = rli.getLineItemCollection();
//				String combo = dept + div + sect;	
				StringBuffer sbcombo = new StringBuffer(dept.toUpperCase()).append(div.toUpperCase()).append(sect.toUpperCase());
				Log.customer.debug("%s *** Combo: %s", classname, sbcombo.toString());
				if (testuser != null && !dept.toUpperCase().equals(DEPT_CAPITAL)) {
					User user = testuser.equalsIgnoreCase("Requester") ? lic.getRequester() : 
						   		testuser.equalsIgnoreCase("Approver") ? (User)sa.getFieldValue("DepartmentApprover") : null;
				    if (user == null && testuser.equalsIgnoreCase("Approver")) {
				    	Log.customer.debug("%s *** Department Approver is null", classname);
				    	reason = 1;
				    	result = false;
				    }
				    else {
				    	String ucombo = CatCommonUtil.getUserDepartmentAcctng(user, lic.getPartition());
						Log.customer.debug("%s *** User Combo: %s", classname, ucombo);
				    	if (ucombo == null || !sbcombo.toString().equals(ucombo)) {
				    		result = false;
				    		if (testuser.equalsIgnoreCase("Approver"))
				    			reason = 2;
				    	}
				    }
					if (result && testuser.equalsIgnoreCase("Requester"))			
						sa.setDottedFieldValue("DepartmentApprover", null);					    
				}
			}		
			Log.customer.debug("%s *** Final Dept Approver: %s", classname, sa.getFieldValue("DepartmentApprover"));			
		}
		Log.customer.debug("CatValidDepartmentForUser *** Result: " + result);					
		Log.customer.debug("CatValidDepartmentForUser *** Reason: " + reason);	
		return result;
	}
	
    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    throws ConditionEvaluationException  {
    	if(!evaluate(object, params)) {
			String errorMsg = reason == 1 ? ErrorMessages[1] : reason == 2 ? ErrorMessages[2] : ErrorMessages[0];
			Log.customer.debug("%s *** Error Message: %s", classname, errorMsg);
			return new ConditionResult(ResourceService.getString(StringTable, errorMsg));          
    	}
        return null;
    }	
	
	public CatValidDepartmentForUser() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
