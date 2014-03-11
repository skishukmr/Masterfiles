// k.stanley

package config.java.hook;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Approver;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatRequisitionApproveHook implements ApprovableHook {

    private static final String classname = "CatReqApproveHook";
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String DelegateError = ResourceService.getString("cat.vcsv1","DelegationLimitError");
	private static final String param = "Application.Caterpillar.Procure.ApprovalLimitsFile";
    private static final String REASON = "Supervisor";

	public CatRequisitionApproveHook() {
		super();
	}

	public List run(Approvable approvable) {
		String message = null;
		if (approvable instanceof Requisition) {
			Requisition r = (Requisition)approvable;
//			User sessionuser = User.getEffectiveUser();
			User realuser = (User)Base.getSession().getRealUser();
			Partition part = r.getPartition();
		    Log.customer.debug("%s *** real user: %s", classname, realuser.getUniqueName());
			BaseVector approvals = CatCommonUtil.getAllApprovalRequests(approvable);
		    Log.customer.debug("CatReqApproveHook *** # of approvals: " + approvals.size());
			Iterator actors = realuser.getCanActAsIterator();
     		for (Iterator requests = approvals.iterator(); requests.hasNext();) {
     			ApprovalRequest ar = (ApprovalRequest)requests.next();
  				Log.customer.debug("%s *** Approver: %s", classname, ar.getApprover().getUniqueName());
 				Log.customer.debug("CatReqApproveHook *** state: " + ar.getState());
 				Log.customer.debug("%s *** reasonKey: %s", classname, ar.getReasonKey());

//  *** Only continue if approval is active (state=2) and reason is Supervisor or SupervisorChain
     			if (ar.getState() == 2 && ar.getApprover() != null && ar.getReasonKey().indexOf(REASON) > -1) {
     				Approver approver = ar.getApprover();
       			    while (actors.hasNext()) {
     			    	BaseId actor = (BaseId)actors.next();
 //   					Log.customer.debug("CatReqApproveHook *** approver == actor? " + (approver.getBaseId() == actor));
      					Log.customer.debug("CatReqApproveHook *** approver.equals(actor)? " + (approver.getBaseId().equals(actor)));
    					if (approver instanceof User && approver.getBaseId().equals(actor)) {
         					ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(realuser, part);
         					if (partuser != null) {
         						String appcode = (String)partuser.getFieldValue("ExpenseApprovalCode");
         			 	 		String fileparam = Base.getService().getParameter(part, param);
         			 			Log.customer.debug("%s *** appcode/filename: %s/%s", classname,appcode,fileparam);
     							try {
     								String lookup = CatCommonUtil.getHashValueFromFile(appcode, fileparam);
     								if (lookup != null) {
     									BigDecimal bdvalue = new BigDecimal(lookup);
     				 			 		Log.customer.debug("CatCommonUtil *** BD value: " + bdvalue.toString());
     				 					if (bdvalue != null) {
     				 						Money limit = new Money(bdvalue, Currency.getBaseCurrency());
     				 						Money total = r.getTotalCost();
     				 						if (limit != null && limit.compareTo(total) < 0 ) {
     				 							Log.customer.debug("%s *** Expense Limit: %s", classname, limit.getApproxAmountInBaseCurrency());
     				 							Log.customer.debug("%s *** Total Cost: %s", classname, total.getApproxAmountInBaseCurrency());
     				     						return ListUtil.list(Constants.getInteger(-1), DelegateError);
     				 						}
         								}
         							}
     							}
     							catch (Exception e) {
     								Log.customer.debug("%s *** Exception: %s", classname, e);
     							}
/*
         						catch (IOException e) {
         							Log.customer.debug("%s *** Exception: %s", classname, e);
     							}
     							catch (ParseException e) {
     								Log.customer.debug("%s *** Exception: %s", classname, e);
     							}
 */
     						}
    					}
/* *** Commented since not applicable to roles ***
    					else if (approver instanceof Role) {
     						Role role = (Role)approver;
     						List users = (List)role.getAllUsers();
     						for (int i=0; i < users.size(); i++) {
     							if (actor.equals(users.get(i))) {

     								*** IF USE, ADD APPLICABLE CODE FROM ABOVE - AFTER if (approver instanceof User) ***
     							}
     						}
    					}
*/
     			    }
         			Log.customer.debug("%s *** Finished INNER While Loop", classname);
     			}
 			}
     	}
		return NoErrorResult;
	}

}
