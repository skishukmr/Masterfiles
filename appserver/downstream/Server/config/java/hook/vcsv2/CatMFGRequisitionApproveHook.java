/*
 * Created by KS on May 8, 2005
 * --------------------------------------------------------------
 * Slightly modified version of config.java.hook.CatRequisitionSubmitHook (vcsv1) used
 * to ensure compilant delegation (i.e, approver must have sufficient authority for Supervisory approvals)
 * ******************* CHANGE HISTORY ***************************
 * Jun 6, 2005 (KS) - Added check to warn Indirect Purchasing user if SupplierLocation is not set
 * Aug 26 2005 (KS) - Bug fix - use ExpenseApprovalCode for realuser not the approver. Also switched to use GBP
 * 					  instead of BaseCurrency() when comparing approval authority to total cost.
 * Feb 11 2008 (MC) - Issue-723 Commenting out the check for aproval limit for real user and approver.
 */
package config.java.hook.vcsv2;

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
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;
import config.java.common.CatConstants;

public class CatMFGRequisitionApproveHook implements ApprovableHook {

    private static final String THISCLASS = "CatMFGRequisitionApproveHook";
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static String DelegateError = ResourceService.getString("cat.java.vcsv2","ErrorDelegationLimit");
	private static String param = "Application.Caterpillar.Procure.ApprovalLimitsFile";
    private static final String REASON = "Supervisor";
    private static final String REASON2 = "NonCatalog";
    private static String TransCtrPermission = "CatTransactionCtr";
    private static String nsThreshold = ResourceService.getString("cat.java.vcsv2","ApproveHookNoSupplierThreshold");
    private static String currencyParam = "Application.Base.Data.DefaultCurrency";
    private static StringBuffer NoSupplierLines;
    private static BigDecimal adhocAmount;


	public List run(Approvable approvable) {
		String message = null;
		if (approvable instanceof Requisition) {
			Requisition r = (Requisition)approvable;
			User realuser = (User)Base.getSession().getRealUser();
			Partition part = r.getPartition();
			if (CatConstants.DEBUG)
			    Log.customer.debug("%s *** real user: %s", THISCLASS, realuser.getUniqueName());
			BaseVector approvals = CatCommonUtil.getAllApprovalRequests(approvable);
			if (CatConstants.DEBUG)
			    Log.customer.debug("CatReqApproveHook *** # of approvals: " + approvals.size());
			Iterator actors = realuser.getCanActAsIterator();
     		for (Iterator requests = approvals.iterator(); requests.hasNext();) {
     			ApprovalRequest ar = (ApprovalRequest)requests.next();
     			if (CatConstants.DEBUG) {
	  				Log.customer.debug("%s *** Approver: %s", THISCLASS, ar.getApprover().getUniqueName());
	 				Log.customer.debug("CatRequisitionApproveHook *** state: " + ar.getState());
	 				Log.customer.debug("%s *** reasonKey: %s", THISCLASS, ar.getReasonKey());
     			}
     		/*	if (ar.getState() == 2 && ar.getApprover() != null) {
// *** TEST 1: Only continue if reason is Supervisor or SupervisorChain
     			    if (ar.getReasonKey().indexOf(REASON) > -1) {
	     				Approver approver = ar.getApprover();
	       			    while (actors.hasNext()) {
	     			    	BaseId actor = (BaseId)actors.next();
	     			    	if (CatConstants.DEBUG)
	     			    	    Log.customer.debug("CatReqApproveHook *** approver.equals(actor)? " + (approver.getBaseId().equals(actor)));
	     			    	if (approver instanceof User && approver.getBaseId().equals(actor)) {
		    	    // Swtiched to use realuser vs. approver
	     			    	    String appcode = (String)realuser.getFieldValue("ExpenseApprovalCode");
	     			 	 		String fileparam = Base.getService().getParameter(part, param);
	     			 	 		if (CatConstants.DEBUG)
	     			 	 		    Log.customer.debug("%s *** appcode/filename: %s/%s", THISCLASS,appcode,fileparam);
	 							try {
	 								String lookup = CatCommonUtil.readHashValueFromFile(appcode, fileparam);
	 								if (lookup != null) {
	 									BigDecimal bdvalue = new BigDecimal(lookup);
	 									if (CatConstants.DEBUG)
	 									    Log.customer.debug("CatCommonUtil *** BD value: " + bdvalue.toString());
	 				 					if (bdvalue != null) {
		 					    //	Switched to used GBP vs. getBaseCurrency()
	 				 					    // Money limit = new Money(bdvalue, Currency.getCurrency("GBP"));
	 				 					    String mfg1Currency = Base.getService().getParameter(part, currencyParam);
	 				 						if (CatConstants.DEBUG)
	 				 							Log.customer.debug("%s *** MFG1 Default Currency: %s", THISCLASS,
	 				 							        mfg1Currency);
	 				 					    Money limit = new Money(bdvalue, Currency.getCurrency(mfg1Currency));
	 				 						if (CatConstants.DEBUG) {
	 				 							Log.customer.debug("%s *** Expense Limit: %s", THISCLASS,
	 				 							        limit.getApproxAmountInBaseCurrency());
	 				 							Log.customer.debug("%s *** Total Cost: %s", THISCLASS,
	 				 							        r.getTotalCost().getApproxAmountInBaseCurrency());
	 				 						}
	 				 						if (limit != null && limit.compareTo(r.getTotalCost()) < 0 ) {
	 				     						return ListUtil.list(Constants.getInteger(-1), DelegateError);
	 				 						}
	     								}
	     							}
	 							}
	 							catch (Exception e) {
	 								Log.customer.debug("%s *** Exception: %s", THISCLASS, e);
	 							}
	    					}
	     			    }
     			    }
     			} */
//  *** TEST 2: Only continue if No Supplier condition exists and user is Indirect Purchasing
     			User effectiveUser = (User)Base.getSession().getEffectiveUser();
     			NoSupplierLines = null;
     			if (effectiveUser != null && ar.getReasonKey().indexOf(REASON2) > -1) {
     			   adhocAmount = new BigDecimal("0");
     			   if (CatConstants.DEBUG)
     			       Log.customer.debug("%s **** BEFORE: NoSupplierLines: %s; adhocAmount: %s",
     			               THISCLASS,NoSupplierLines,adhocAmount);
     			   setNoSupplierAndAdhocValues(r);
     			   if (CatConstants.DEBUG)
     			       Log.customer.debug("%s **** AFTER: NoSupplierLines: %s; adhocAmount: %s",
     			               THISCLASS,NoSupplierLines,adhocAmount);
     			    if (NoSupplierLines != null && adhocAmount.doubleValue() > 0.00) {
	     			    List permissions = effectiveUser.getAllPermissions();
	     			    boolean isTransCtr = false;
	     			    if (!permissions.isEmpty()){
	     			        int size = permissions.size();
	     			        Permission tcp = Permission.getPermission(TransCtrPermission);
//					        Log.customer.debug("%s **** tcp: Permission(TransCtr): %s",THISCLASS,tcp);
	     			        if (tcp != null) {
//				     		Log.customer.debug("%s **** tcp BaseId: %s",THISCLASS,tcp.getBaseId());
		     			        for (int i=0;i<size;i++) {
		     			            BaseId permission = (BaseId)permissions.get(i);
//					     			Log.customer.debug("%s **** Permission(i): %s",THISCLASS,permission);
		     			            if (permission.equals(tcp.getBaseId())) {
		     			                isTransCtr = true;
		     			                break;
		     			            }
		     			        }
	     			        }
	     			    }
	     			    if (CatConstants.DEBUG)
	     			        Log.customer.debug("CatMFGRequisitionApproveHook **** isTransCtr? " + isTransCtr);
	     			    if (isTransCtr) {
		     			   NoSupplierLines.append(")");
	     			       String [] inputs = StringUtil.delimitedStringToArray(nsThreshold,',');
		     			   if (inputs.length > 1) {
		     			       Currency tCurrency = Currency.getCurrency(inputs[1]);
		     			       if (CatConstants.DEBUG)
			     			        Log.customer.debug("%s **** tCurrency: %s",THISCLASS,tCurrency);
		     			       if (tCurrency != null) {
		     			           	Money threshold = new Money(new BigDecimal((String)inputs[0]),tCurrency);
		   	     			    	if (CatConstants.DEBUG)
		   	     			    	    Log.customer.debug("%s **** threshold: %s",THISCLASS,threshold);
		   	     			    	if (threshold != null) {
		   	     			    	    String NoSupplierMessage = null;
		   	     			    	    Money adhocTotal = new Money(adhocAmount,tCurrency);
		   	     			    	    if (CatConstants.DEBUG)
		   	     			    	        Log.customer.debug("%s **** adhocTotal: %s",THISCLASS,adhocTotal);
		   	     			    	    if (threshold.compareTo(adhocTotal) < 0) {
		   	     			    	        NoSupplierMessage = ResourceService.getString("cat.java.vcsv2","ApproveHookNoSupplierWarningAbove");
		   	     			    	    } else {
		   	     			    	        NoSupplierMessage = ResourceService.getString("cat.java.vcsv2","ApproveHookNoSupplierWarningBelow");
		   	     			    	    }
		   	     			    	    if (NoSupplierMessage != null)
		   	     			    	        NoSupplierMessage += NoSupplierLines.toString();
		   	     			    	        return ListUtil.list(Constants.getInteger(1), NoSupplierMessage);
		     			           }
		     			       }
		     			   }
		     			}
     			    }
     			}
 			}
     	}
		return NoErrorResult;
	}

	public static void setNoSupplierAndAdhocValues (ProcureLineItemCollection plic) {

	    if (plic != null)  {
	        BaseVector lines = plic.getLineItems();
	        if (!lines.isEmpty()) {
	            int warnings = 0;
		        int size = lines.size();
		        for (int i = 0; i<size; i++) {
		            ProcureLineItem pli = (ProcureLineItem)lines.get(i);
		            if (pli.getIsAdHoc()) {
		                adhocAmount = adhocAmount.add(pli.getAmount().getApproxAmountInBaseCurrency());
			            if (pli.getSupplierLocation() == null) {
			                warnings += 1;
			                if (warnings == 1)
			                    NoSupplierLines = new StringBuffer("(Line Item: ");
			                else
			                    NoSupplierLines.append(", ");
			                NoSupplierLines.append(pli.getLineItemNumber());
			            }
			        }
		        }
		    }
	    }
	}

	public CatMFGRequisitionApproveHook() {
		super();
	}
}
