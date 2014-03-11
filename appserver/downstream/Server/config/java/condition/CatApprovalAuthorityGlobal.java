/*
 * Created by KS on May 8, 2005
 * --------------------------------------------------------------
 * Mofified version of R1 condition written to provide warning message during
 * UP appproval authority delegation if delegate has less authority than delegator.
 * ++ Uses global user field ExpenseApprovalCode) not partitioned user ++
 * Madhavan Chari Issue666- 19-09-2007
 * Issue666 Changed the loogic to Same as CATApprovalAuthorityCompare
 */

package config.java.condition;

import java.math.BigDecimal;
import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.User;
import ariba.common.core.UserProfileDetails;
import ariba.user.core.Delegation;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatApprovalAuthorityGlobal extends Condition
{
    private static final String THISCLASS = "CatApprovalAuthorityGlobal";
    private static final String param = "Application.Caterpillar.Procure.ApprovalLimitsFile";

    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException
    {
        boolean visible = true;
		Log.customer.debug("CatApprovalAuthorityGlobal *** Object: " + object);
		if(object instanceof UserProfileDetails)
		{
			UserProfileDetails upd = (UserProfileDetails)object;
			Delegation delegation = upd.getDelegation();
			Log.customer.debug("CatApprovalAuthorityGlobal *** Delegation: " + delegation);
			if(delegation != null)
			{
				ariba.user.core.User delegator = delegation.getDelegator();
				ariba.user.core.User delegatee = delegation.getDelegatee();
				Log.customer.debug("CatApprovalAuthorityGlobal *** Delegator: " + delegator);
				Log.customer.debug("CatApprovalAuthorityGlobal *** Delegatee: " + delegatee);
				ariba.base.core.Partition partition = upd.getPartition();
				Log.customer.debug("CatApprovalAuthorityGlobal *** Partition: " + partition);
				if(delegator != null && delegatee != null)
				{
					try
					{
						User newDelegator = User.getPartitionedUser(delegator, partition);
						User newDelegatee = User.getPartitionedUser(delegatee, partition);
						Log.customer.debug("CatApprovalAuthorityGlobal *** newDelegator: " + newDelegator);
						Log.customer.debug("CatApprovalAuthorityGlobal *** newDelegatee: " + newDelegatee);
						String delegatorEAC = (String)newDelegator.getFieldValue("ExpenseApprovalCode");
						String delegateeEAC = (String)newDelegatee.getFieldValue("ExpenseApprovalCode");
						String fileparam = Base.getService().getParameter(partition, "Application.Caterpillar.Procure.ApprovalLimitsFile");
						Log.customer.debug("%s *** delegatorEAC/filename: %s/%s", "CatApprovalAuthorityGlobal", delegatorEAC, fileparam);
						Log.customer.debug("%s *** delegateeEAC/filename: %s/%s", "CatApprovalAuthorityGlobal", delegateeEAC, fileparam);

						/*
						 *	Changed by 	: ARajendren, Ariba, Inc.
						 *  Changes		: Changed lookup method from readHashValueFromFile to getHashValueFromFile
						 *
						 */

						//String delegatorLookup = CatCommonUtil.readHashValueFromFile(delegatorEAC, fileparam);
						//String delegateeLookup = CatCommonUtil.readHashValueFromFile(delegateeEAC, fileparam);
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

					} catch (Exception e) {
						   Log.customer.debug(e.toString());
					}
				}

			}

			Log.customer.debug("CatApprovalAuthorityGlobal*** Visible = " + visible);
		}
		return visible;
	}

    public CatApprovalAuthorityGlobal()
    {
        //super();
    }
}