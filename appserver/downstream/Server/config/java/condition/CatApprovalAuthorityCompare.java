package config.java.condition;

import java.math.BigDecimal;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.basic.core.Money;
import ariba.common.core.User;
import ariba.common.core.UserProfileDetails;
import ariba.user.core.Delegation;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatApprovalAuthorityCompare extends Condition
{

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        boolean visible = true;
        Log.customer.debug("CatApprovalAuthorityCompare *** Object: " + object);
        if(object instanceof UserProfileDetails)
        {
            UserProfileDetails upd = (UserProfileDetails)object;
            Delegation delegation = upd.getDelegation();
            Log.customer.debug("CatApprovalAuthorityCompare *** Delegation: " + delegation);
            if(delegation != null)
            {
                ariba.user.core.User delegator = delegation.getDelegator();
                ariba.user.core.User delegatee = delegation.getDelegatee();
                Log.customer.debug("CatApprovalAuthorityCompare *** Delegator: " + delegator);
                Log.customer.debug("CatApprovalAuthorityCompare *** Delegatee: " + delegatee);
                ariba.base.core.Partition partition = upd.getPartition();
                Log.customer.debug("CatApprovalAuthorityCompare *** Partition: " + partition);
                int partitionNo = partition.intValue();
                Log.customer.debug("CatApprovalAuthorityCompare *** Partition Number: " + partitionNo);
                if(delegator != null && delegatee != null)
                {
                    User newDelegator = User.getPartitionedUser(delegator, partition);
                    User newDelegatee = User.getPartitionedUser(delegatee, partition);
                    Log.customer.debug("CatApprovalAuthorityCompare *** newDelegator: " + newDelegator);
                    Log.customer.debug("CatApprovalAuthorityCompare *** newDelegatee: " + newDelegatee);
                    if(partitionNo == 5 || partitionNo == 6)
                    {
						/*
						 *	Changed by 	: ARajendren, Ariba, Inc.
						 *  Changes		: Added null check to bypass NullPointerException
						 *
						 */

                    	BaseObject delegatorElevel = newDelegator != null ? (BaseObject)newDelegator.getFieldValue("SAPExpenseApprovalCode") : null;
                    	BaseObject delegateeElevel = newDelegatee != null ? (BaseObject)newDelegatee.getFieldValue("SAPExpenseApprovalCode") : null;
                    	if (delegatorElevel != null && delegateeElevel != null)
                    	{
                    		Money delegatorEAmt = (Money)delegatorElevel.getFieldValue("Amount");
                    		Money delegateeEAmt = (Money)delegateeElevel.getFieldValue("Amount");
                    		if(delegatorEAmt == null || delegateeEAmt == null)
                    		{
                    			visible = false;
                    		}
                    		if(delegatorEAmt.compareTo(delegateeEAmt) > 0)
                    		visible = false;
                    	}
                    	else
                    	{
                    		visible = false;
                    	}
                    }
                    else
                    {
						/*
						 *	Changed by 	: ARajendren, Ariba, Inc.
						 *  Changes		: Added null check to bypass NullPointerException
						 *
						 */
                    	String delegatorEAC = newDelegator != null ? (String)newDelegator.getFieldValue("ExpenseApprovalCode") : null;
                   	 	String delegateeEAC = newDelegatee != null ? (String)newDelegatee.getFieldValue("ExpenseApprovalCode") : null;
						String fileparam = Base.getService().getParameter(partition, "Application.Caterpillar.Procure.ApprovalLimitsFile");
						Log.customer.debug("%s *** delegatorEAC/filename: %s/%s", "CatApprovalAuthorityCompare", delegatorEAC, fileparam);
						Log.customer.debug("%s *** delegateeEAC/filename: %s/%s", "CatApprovalAuthorityCompare", delegateeEAC, fileparam);
						String delegatorLookup = CatCommonUtil.getHashValueFromFile(delegatorEAC, fileparam);
						String delegateeLookup = CatCommonUtil.getHashValueFromFile(delegateeEAC, fileparam);
						if(delegatorLookup != null && delegateeLookup != null)
						{
							BigDecimal delegatorBD = BigDecimalFormatter.getBigDecimalValue(delegatorLookup);
							Log.customer.debug("**AR** " + delegatorBD);
							BigDecimal delegateeBD = BigDecimalFormatter.getBigDecimalValue(delegateeLookup);
							Log.customer.debug("**AR** " + delegateeBD);
							Log.customer.debug("**AR** " + delegatorBD.compareTo(delegateeBD));
							if(delegatorBD.compareTo(delegateeBD) > 0)
								visible = false;
						}
                	}
              	}
            }
            Log.customer.debug("CatApprovalAuthorityCompare*** Visible = " + visible);
        }
        return visible;
    }

    public CatApprovalAuthorityCompare()
    {
    }

    private static final String classname = "CatApprovalAuthorityCompare";
}
