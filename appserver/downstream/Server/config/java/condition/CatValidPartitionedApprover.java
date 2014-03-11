/*
 * Created by KS on Feb 13, 2005 
 * Emergency workaround to Production nametable query issue (system hangs on partitioned user subquery) 
 */
package config.java.condition;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.common.core.SplitAccounting;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatValidPartitionedApprover extends Condition {
    
	private static final String classname = "CatValidPartitionedApprover";
	private static final String StringTable = "cat.vcsv1";	
	private static final String ERRORMSG[] = { "Error_Default", "Error_NonPartitionedApprover" };
																				 
	private static int reason;
	
    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {
 
    	reason = 0;	
        boolean result = true;
        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            Partition partition = Base.getSession().getPartition();
            User approver = (User)sa.getFieldValue("DepartmentApprover");
            if (approver == null) {
                result = false;
                reason = 1;
            } else {
                Log.customer.debug("%s *** approver: %s", classname, approver.getUniqueName());
                ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(approver,partition);
                Log.customer.debug("%s *** partuser: %s", classname, partuser);
                if (partuser == null) {
                    result = false;
                	reason = 2;
                }
            }    
        } 
        Log.customer.debug("CatValidPartitionedApprover *** result+reason: " + result + reason);
        return result;
    }
    
    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    throws ConditionEvaluationException  {
    	if(!evaluate(object, params)) {
			String errorMsg = reason == 2 ? ERRORMSG[1] : ERRORMSG[0];
			Log.customer.debug("%s *** Error Message: %s", classname, errorMsg);
			return new ConditionResult(ResourceService.getString(StringTable, errorMsg));           
    	}
        return null;
    }	
    
    public CatValidPartitionedApprover() {
        super();
    }
}
