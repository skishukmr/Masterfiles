/*
 * Created by Santanu on July 28, 2008
 * Ashwini		  22-09-10 	Issue 1172 -Commented the code for Designated Approver for Critical Asset Down!!!
 */
package config.java.condition;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Partition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Accounting;
import ariba.common.core.SplitAccounting;
import ariba.purchasing.core.ReqLineItem;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
//import config.java.common.CatCommonUtil;

public class CatCostCenterApproverVisibility extends Condition {

	private static final String classname = "CatCostCenterApproverVisibility";
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestUser", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestUser" };
	//private static final String StringTable = "cat.SAP";
	//private static final String ErrorMessages[] = { "Error_Default", "Error_NoApproverForCC", "Error_InvalidApproverForCC" };
	private static int reason;

	public boolean evaluate(Object object, PropertyTable params)
	throws ConditionEvaluationException {

		reason = 0;
		boolean result = false;
		if (object instanceof SplitAccounting){
			SplitAccounting sa = (SplitAccounting)object;
			String testuser = (String)params.getPropertyForKey("TestUser");
			String costcenter = (String)sa.getFieldValue("CostCenterText");
			LineItem li = (LineItem)sa.getFieldValue("LineItem");
			if (li instanceof ReqLineItem && costcenter != null) {
				ReqLineItem rli = (ReqLineItem)li;
				LineItemCollection lic = rli.getLineItemCollection();
				Log.customer.debug("%s *** SplitAccounting CostCenter: %s", classname, costcenter.toString());
				if (testuser != null) {
					User requester = (User)lic.getRequester();
					ariba.common.core.User CCapprover = (ariba.common.core.User)sa.getFieldValue("CostCenterApprover");

					if(requester == null){
						result = true;
					}

				/*	Issue 1172 - Commented the code for CAD to get designated approver
				Boolean isCriticalAssetDown = (Boolean)lic.getFieldValue("CriticalAssetDown");
					Log.customer.debug("%s **** isCriticalAssetDown : %s",classname  ,isCriticalAssetDown);
					if(isCriticalAssetDown!=null && isCriticalAssetDown.booleanValue())
					{
					  Log.customer.debug("%s **** This is for CriticalAssetDown : " ,classname);
					  return false;

					}*/

					Log.customer.debug("%s *** CostCenter approver is %s", classname, CCapprover);
				    String ucostcenter = getUserCostCenterAcctng(requester, lic.getPartition());
					Log.customer.debug("%s *** User CostCenter: %s", classname, ucostcenter);
				    if (ucostcenter == null || !costcenter.toString().toUpperCase().equals(ucostcenter)) {
				    	result = true;
				    		//reason = 1;
				    }
				}
			}
			Log.customer.debug("%s *** Final CostCenter Approver: %s", classname, sa.getFieldValue("CostCenterApprover"));
		}
		Log.customer.debug("CatValidCostCenterForUser *** Result: " + result);
		//Log.customer.debug("CatValidCostCenterForUser *** Reason: " + reason);
		return result;
	}

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    throws ConditionEvaluationException  {
    	/*
    	if(!evaluate(object, params)) {
			String errorMsg = reason == 1 ? ErrorMessages[1] : reason == 2 ? ErrorMessages[2] : ErrorMessages[0];
			Log.customer.debug("%s *** Error Message: %s", classname, errorMsg);
			return new ConditionResult(ResourceService.getString(StringTable, errorMsg));
    	}
    	*/
        return null;
    }

	public CatCostCenterApproverVisibility() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

	public static String getUserCostCenterAcctng(ariba.user.core.User user, Partition part) {
		String costcenter = null;
		if (user != null) {
			ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(user, part);
			if (partuser != null) {
				Accounting uacct = partuser.getAccounting();
				String ucostcenter = (String)uacct.getFieldValue("CostCenterText");
				if (ucostcenter != null)
				costcenter = ucostcenter.toUpperCase();
			}
		}
		return costcenter;
	}

}
