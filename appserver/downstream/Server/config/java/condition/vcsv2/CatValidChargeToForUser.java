/*
 * Created by KS on April 24, 2005
 * ---------------------------------------------------------------------------------
 * Returns true if Requester has matching AccountingField or Designated Approver has matching Facility
 * Can be used for any Accounting Field (used for CostCenter in Release 2 - UK)
 * Takes 2 AML params - TestField (accounting field to test) and TestUser (Requester or Approver)
 */
package config.java.condition.vcsv2;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.util.core.StringUtil;
import config.java.common.CatConstants;

public class CatValidChargeToForUser extends Condition {

	private static final String THISCLASS = "CatValidAccountingForUser";
	private static final String MSGSOURCE = "cat.java.vcsv2";
	private static final String ERRORS[] = { "ErrorCCApprover_NoApprover","ErrorCCApprover_NotInPartition", 
											 "ErrorCCApprover_BadAcctngApprover","ErrorCCApprover_BadAcctngRequester",
											 "ErrorCCApprover_RequesterCannotApprove" };		
	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestUser", IsScalar, "java.lang.String"),
				  									  new ValueInfo("TestField", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestUser","TestField" };
	private static int reason;
	
	
    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {    
  	
        return isValidAccounting(object, params);
    }
    
    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException  {
		
        if(!evaluate(object, params)) {
			String errorMsg = ERRORS[reason-1];
			if (CatConstants.DEBUG)
			    Log.customer.debug("%s *** Error Message: %s", THISCLASS, errorMsg);
			return new ConditionResult(ResourceService.getString(MSGSOURCE, errorMsg));          
		}
		return null;
	}	    
    
    public boolean isValidAccounting(Object object, PropertyTable params) {
		
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Object: %s", THISCLASS, object);
		reason = 0;
		boolean result = true;
		if (object instanceof SplitAccounting){
			SplitAccounting sa = (SplitAccounting)object;
			String testuser = (String)params.getPropertyForKey("TestUser");
			String testfield = (String)params.getPropertyForKey("TestField");
		
			if (!StringUtil.nullOrEmptyOrBlankString(testfield) && 
			        (testuser.equalsIgnoreCase("Requester") || testuser.equalsIgnoreCase("Approver"))) {
			    ClusterRoot afield = (ClusterRoot)sa.getFieldValue(testfield);
		        if (CatConstants.DEBUG)
		            Log.customer.debug("%s *** sa acctng field: %s", THISCLASS, afield);
			    if (afield != null) {
			        LineItemCollection lic = sa.getLineItem().getLineItemCollection();
			        User user = testuser.equalsIgnoreCase("Requester") ? lic.getRequester() : 
			            testuser.equalsIgnoreCase("Approver") ? (User)sa.getFieldValue("CostCenterApprover") : null;
			        if (CatConstants.DEBUG)
			            Log.customer.debug("%s *** user: %s", THISCLASS, user);
				    if (user == null && testuser.equalsIgnoreCase("Approver")) {
				    	reason = 1;
				    	result = false;
				        if (CatConstants.DEBUG)
				            Log.customer.debug("%s *** (1)CC Approver is null!", THISCLASS);				    	   
				    } else {
				        if (testuser.equalsIgnoreCase("Approver") && user == lic.getRequester()) {
				            reason = 5;
				            result = false;
					        if (CatConstants.DEBUG)
					            Log.customer.debug("%s *** (5)CC Approver is Requester!", THISCLASS);
				        } else {
					        ariba.common.core.User puser = ariba.common.core.User.getPartitionedUser(user, lic.getPartition());
					        if (CatConstants.DEBUG)
					            Log.customer.debug("%s *** part user: %s", THISCLASS, puser);
					        if (puser == null && testuser.equalsIgnoreCase("Approver")) {
					            reason = 2;
					            result = false;
						        if (CatConstants.DEBUG)
						            Log.customer.debug("%s *** (2)Approver is not in partition!", THISCLASS);				            
					        } else {
					            if (puser != null) {
					                ClusterRoot ufield = (ClusterRoot)puser.getAccounting().getFieldValue(testfield);
					                ClusterRoot ufac = (ClusterRoot)puser.getDottedFieldValue("Accounting.Facility");
					                ClusterRoot afac = (ClusterRoot)sa.getFieldValue("Facility");
							        if (CatConstants.DEBUG)
							            Log.customer.debug("%s *** User Field/Facility & SA Facility: %s / %s / %s ",
							                    THISCLASS,ufield,ufac,afac);					                
					                if (testuser.equalsIgnoreCase("Approver") && ufac != afac) {
					                    reason = 3;
					                    result = false;
					                    if (CatConstants.DEBUG)
								            Log.customer.debug("%s *** (3)Approver Facility does not match Requester's!", THISCLASS);
					                } else {
					                    if (testuser.equalsIgnoreCase("Requester") && ufield != null && !ufield.equals(afield)) {
					                        reason = 4;
					                        result = false;
					                        if (CatConstants.DEBUG)
									            Log.customer.debug("%s *** (4)Accounting does not match Requester's!", THISCLASS);			                        
					                    }		                    
					                }		       				              
					            }
					        }
				        }					        
				    }
			    }
				if (result && testuser.equalsIgnoreCase("Requester"))			
					sa.setDottedFieldValue("CostCenterApprover", null);				    
			}		
		}
		return result;
    }
    
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}  
	
	public CatValidChargeToForUser() {
		super();
	}	

}
