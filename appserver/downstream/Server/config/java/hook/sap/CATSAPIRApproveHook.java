/*******************************************************************************************************************
	Revision History

	13/08/2012   IBM AMS_Manoj       WI 318          Skipping Approvable Hook for IR's in Rejecting or Rejected Status.
	04/09/2012	 IBM AMS_Vikram	     WI 320	         IR call to validate accounting and pull in people to the approvable and stop the IR for wrong accounting
********************************************************************************************************************/

package config.java.hook.sap;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.contract.core.ContractCoreApprovable;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.Approver;
import ariba.user.core.Group;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.sap.CATSAPUtils;

public class CATSAPIRApproveHook  implements ApprovableHook
{

    FastStringBuffer totalMsg = new FastStringBuffer ();
	boolean hasErrors = false;
   private static final String ClassName = "CATSAPIRApproveHook";
   private static final int ValidationError = -2;
	private static final int ValidationWarning = 1;
    public List run(Approvable approvable)
    {
		Log.customer.debug("CATSAPIRApproveHook : run : ***START***");
        if(! (approvable instanceof InvoiceReconciliation))
        {
			Log.customer.debug("CATSAPIRApproveHook : run : approvable is not instance of InvoiceReconciliation " + approvable);
			return NoErrorResult;
		}

         //  ***** WI 318 Starts ********

        InvoiceReconciliation ir = (InvoiceReconciliation)approvable;

        // If action = 2 , IR is in rejected Status.

        if (ir.isForRejection() || (ir.getRequestedAction() == 2) || ir.isRejecting() || ir.isRejected()){

						    //if (Log.customer.debugOn)
								Log.customer.debug("%s ::: IR: %s is being rejected, skipping validation!", ClassName, ir.getUniqueName());
							return NoErrorResult;
		}

		//  ***** WI 318 Ends ********

        BaseVector irLineItems = ir.getLineItems();
        InvoiceReconciliationLineItem irli = null;
        ClusterRoot user = Base.getSession().getEffectiveUser();
        //Added by Sandeep in order to Check if the user is an Exceptional handler
        User approver = (User)user;
        String userUniqueID = user.getUniqueName();
        Log.customer.debug("CATSAPIRApproveHook - Check UniqueName of the Current User - Sandeep");

		Log.customer.debug("CATSAPIRApproveHook : run : approver : " + approver);

		Log.customer.debug("CATSAPIRApproveHook : run : requisition : " + ir);
		for(int i=0; i<ir.getLineItems().size(); i++){
			 irli = (InvoiceReconciliationLineItem)ir.getLineItems().get(i);
				if(getAccountDistExcpRequired(irli)){
					return ListUtil.list(Constants.getInteger(-1), "The invoice line's accounting distribution data is either missing or invalid.");
				}
		}
		//Added by Sandeep to Replicate US Exception Handling Process.
	boolean isCurrentUserApprover = false;
			User currentUser = (User) Base.getSession().getEffectiveUser();
			Approver curapprover = null;
			ApprovalRequest currentar = null;
			//Check whether the current user is among the current required approvers or not
			Iterator allActiveApproverIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StateActive, null, new Boolean(true));
		while (allActiveApproverIterator.hasNext())
		{
					currentar = (ApprovalRequest) allActiveApproverIterator.next();
					curapprover = currentar.getApprover();
					Log.customer.debug("%s ::: HandleMustExceptions: ApprovalRequests %s", ClassName, currentar);
					if (curapprover instanceof User)
					{
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: HandleMustExceptions: Approver is a user", ClassName);
						if (curapprover.equals(currentUser))
						{
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: HandleMustExceptions: CurrentUser is Approver", ClassName);
							isCurrentUserApprover = true;
							break;
						}
					}
					else if(curapprover instanceof Role)
					{
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: HandleMustExceptions: Checking Roles for the CurrentUser", ClassName);

						if (currentUser.hasRole((Role) curapprover))
						{
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: HandleMustExceptions: CurrentUser has required Role hence an Approver", ClassName);
							isCurrentUserApprover = true;
							break;
						}
					}
					else if(curapprover instanceof Group)
					{
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: HandleMustExceptions: Checking groups for the CurrentUser", ClassName);

						if (currentUser.hasGroup((Group) curapprover))
						{
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: HandleMustExceptions: CurrentUser has required Groups hence an Approver", ClassName);
							isCurrentUserApprover = true;
							break;
						}
					}
		}

		String strReason = (String)currentar.getFieldValue("Reason");
				String strReasonCheck = ResourceService.getString("cat.java.sap", "ReasonCheck");
				//if (Log.customer.debugOn)
				{
					Log.customer.debug("%s ::: HandleMustExceptions: Checking for the string \"%s\"", ClassName, strReasonCheck);
					Log.customer.debug("%s ::: HandleMustExceptions: Checking inside the string \"%s\"", ClassName, strReason);
				}
				if ( isCurrentUserApprover && strReason.indexOf(strReasonCheck) != -1 )
				{
					String  MustHandleExceptions = ResourceService.getString("cat.java.sap","MustHandleExceptions");
					String HandleExceptions[] = MustHandleExceptions.split(":");
					for (int i = 0; i < irLineItems.size(); i++)	//Checking the line exceptions
					{
						irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
						List liExceptions = irli.getExceptions();
						if (!liExceptions.isEmpty())
						{
							for(int m=0; m<liExceptions.size(); m++)
							{
								InvoiceException ie = (InvoiceException) liExceptions.get(m);
								if (ie != null)
								{
									Log.customer.debug("%s ::: %s th LineLevelException IS: %s", ClassName, m, ie.getType().getUniqueName());

									for (int k=0;k<HandleExceptions.length; k++)
									{
										String HandleException = HandleExceptions[k];
										Log.customer.debug("%s ::: %s th MustHandleException IS: %s", ClassName, k, HandleException);
										if ( ie.getType().getUniqueName().indexOf(HandleException) > -1 ) //Linelevel exception falls under the MustHandledException
										{
											if (ie.getState() == 1)	//Exception Not Handled
											{
												Log.customer.debug("As LineLevelException %s Matching With MustHandleException %s...Now checking if the approver is responsible for handling this...", HandleException, ie.getType().getUniqueName());
												String strExName = ( (ariba.base.core.MultiLingualString)ie.getType().getName() ).getPrimaryString();
												Log.customer.debug("%s ::: HandleMustExceptions: Mandatory ExceptionName IS \"%s\"", ClassName, strExName);
												Log.customer.debug("%s ::: HandleMustExceptions: ApprovalNode ReasonCode IS \"%s\"", ClassName, strReason);

												if ( strReason.indexOf(strExName) != -1 ) //If current approvalnode is responsible for this exception
												{
													Log.customer.debug("%s ::: As LineLevelException %s Matching With MustHandleException %s...Approver has to handle this...Can NOT Proceed...", ClassName, HandleException, ie.getType().getUniqueName());
													return ListUtil.list(Constants.getInteger(ValidationError), ((ariba.base.core.MultiLingualString)ie.getType().getName()).getPrimaryString() + " must be handled by the exception handler.");
												}
											}
										}
									}
								}
							}
						}
					}
					List hExceptions = ir.getExceptions(); //Checking the header exceptions
					if (!hExceptions.isEmpty())
					{
						for(int m=0; m<hExceptions.size(); m++)
						{
							InvoiceException ie = (InvoiceException) hExceptions.get(m);
							if (ie != null)
							{
								Log.customer.debug("%s ::: %s th HeaderLevelException IS: %s", ClassName, m, ie.getType().getUniqueName());
								for (int k=0;k<HandleExceptions.length; k++)
								{
									String HandleException = HandleExceptions[k];
									Log.customer.debug("%s ::: %s th MustHandleException IS: %s", ClassName, k, HandleException);
									if ( ie.getType().getUniqueName().indexOf(HandleException) > -1 ) //Headerlevel exception falls under the MustHandledException
									{
										if (ie.getState() == 1)	//Exception Not Handled
										{
											Log.customer.debug("%s ::: As HeaderLevelException %s Matching With MustHandleException %s...Can NOT Proceed...", ClassName, HandleException, ie.getType().getUniqueName());
											String strExName = ( (ariba.base.core.MultiLingualString)ie.getType().getName() ).getPrimaryString();
											Log.customer.debug("%s ::: HandleMustExceptions: Mandatory ExceptionName IS \"%s\"", ClassName, strExName);
											Log.customer.debug("%s ::: HandleMustExceptions: ApprovalNode ReasonCode IS \"%s\"", ClassName, strReason);
											if ( strReason.indexOf(strExName) != -1 ) //If current approvalnode is responsible for this exception
											{
												Log.customer.debug("%s ::: As HeaderLevelException %s Matching With MustHandleException %s...Approver has to handle this...Can NOT Proceed...", ClassName, HandleException, ie.getType().getUniqueName());
												return ListUtil.list(Constants.getInteger(ValidationError), ((ariba.base.core.MultiLingualString)ie.getType().getName()).getPrimaryString() + " must be handled by the exception handler.");
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

	private boolean getAccountDistExcpRequired(InvoiceReconciliationLineItem irli) {
		ProcureLineType procureLineType = (ProcureLineType)irli.getLineType();
		if(procureLineType==null || procureLineType.getCategory()!=ProcureLineType.LineItemCategory){
			return false;
		}
		//  ***** Vikram: WI 320 Starts ********
		/*if(irli.getMasterAgreement()==null || irli.getMasterAgreement().getReleaseType()!= ContractCoreApprovable.ReleaseTypeNone){
			return false;
		}*/
		//  ***** Vikram: WI 320 Ends ********
		if(!CATSAPUtils.validateIRLineAccounting(irli)){
			return true;
		}
		return false;
	}
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String THISCLASS = "CATSAPIRApproveHook";

	public CATSAPIRApproveHook() {
		super();
	}
}
