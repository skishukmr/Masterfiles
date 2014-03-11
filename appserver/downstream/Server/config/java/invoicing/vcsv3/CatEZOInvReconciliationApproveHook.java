
package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Response;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;
import config.java.common.CatCommonUtil;

/**
 * @author kstanley
 * 1. Returns error if unhandled ReceivingQtyVariance exception.
 * 2. Returns error if any unhandled exception.
 * 3. Returns error if invalid accounting.
 * 4. Sets IRHoldEscalation to FALSE if no error returned.
 */


public class CatEZOInvReconciliationApproveHook implements ApprovableHook {

	private static final String ClassName = "CatEZOInvReconciliationApproveHook";
	private static final String Role_AP = ResourceService.getString("cat.rulereasons.vcsv3", "Role_AP");
	private static final String Role_Purchasing = ResourceService.getString("cat.rulereasons.vcsv3","Role_TransCtr");
	private static final String Name_VAT = ResourceService.getString("cat.rulereasons.vcsv3", "VATCustomApprover_Name");
    private static final String ValidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_Valid");
	private static final String InvalidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_NotValid");
//	private static final String AdditionalMessageIR = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_ErrorGuidanceIR");
	private static final String AdditionalMessage = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_ErrorGuidance");
	private static final String AccountingFacilityError = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_AcctFacMismatch");
	private static final String ReceivedQuantityError = ResourceService.getString("cat.invoicejava.vcsv3", "Hook_IRReceivingQtyError");
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));


	public List run(Approvable approvable) {

		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		Response response = null;
		String AccountingErrorMsg = "";
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		StringBuffer sb = null;
		String acctFac = null;
		int materialLineCounter = 0;

		if (ir.isForRejection() || (ir.getRequestedAction() == 2) || ir.isRejecting() || ir.isRejected()){

		    //if (Log.customer.debugOn)
				Log.customer.debug("%s ::: IR: %s is being rejected, skipping validation!", ClassName, approvable.getUniqueName());
			return NoErrorResult;
		}

		User user = null;
		ClusterRoot actor = Base.getSession().getEffectiveUser();
		if (actor instanceof User)
		    user = (User)actor;

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Effective User: %s", ClassName, user);

		int userPower = irPowerUser(user);
		boolean isPostVAT = isPostVAT(ir);
		boolean isFinalApprover = isFinalApprover(ir);

		// 1. Validate no uncleared ReceivingQty exceptions
		if (userPower==0 || isPostVAT) {

			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: !!!!!!!!! 1. VERIFYING NOT RECEIVEDQTY EXCEPTION BYPASS!!!!!!!!!!!!!", ClassName);

		    StringBuffer qtyError = findUnclearedQtyExceptions(ir);
		    if (qtyError != null)
		        return ListUtil.list(Constants.getInteger(-1), Fmt.S(ReceivedQuantityError,qtyError.toString()));
		}

		// If final user or AP/IP after VAT assignement
		if (isFinalApprover || (userPower>0 && isPostVAT)) {  // Then validate accounting on all lines (not just material)

			// 2. Ensure no unhandled exceptions (in case AP/IP did not handle post-VAT)
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: !!!!!!!!! 2. VERIFYING NO UNHANDLED EXCEPTIONS!!!!!!!!!!!!!", ClassName);

			StringBuffer unhandledErrorMsg = CatVATCustomApprover.checkUnhandledExceptions(ir);
			if (unhandledErrorMsg != null) {

			    // 02.08.07 Per Heidi, if AP exempt from error (warning instead)
			    if (user.hasRole(Role.getRole(Role_AP)))
			        return ListUtil.list(Constants.getInteger(1), unhandledErrorMsg.toString());
			    else
			        return ListUtil.list(Constants.getInteger(-1), unhandledErrorMsg.toString());
			}

			// 3. Validate all accounting lines
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: !!!!!!!!! 3. VALIDATING ACCOUNTING!!!!!!!!!!!!!", ClassName);

			boolean isSimulation = CatEZOInvoiceAccountingValidation.getIsSimulation();
			if (isSimulation)
			    sb = new StringBuffer(InvalidAccountingMsg).append("Facility is not valid for this Department.");

			for (int i = 0; i < irLineItems.size(); i++) {

			    irli = (InvoiceReconciliationLineItem) irLineItems.get(i);

			    // 01.17.06  Added to capture AccountType for later accounting validation
			    ClusterRoot acctType = null;
			    if (!StringUtil.nullOrEmptyOrBlankString(CatEZOInvoiceAccountingValidation.skipOtherOrder)
			            && CatEZOInvoiceAccountingValidation.skipOtherOrder.startsWith("Y")) {
			    	acctType =(ClusterRoot)irli.getFieldValue("AccountType");
			    }

				sac = irli.getAccountings();
				if (sac != null) {
					List sacList = sac.getSplitAccountings();
					int sacSize = sacList.size();
					for (int j = 0; j < sacSize; j++) {
						sa = (SplitAccounting) sacList.get(j);

						// 1. Ensure all AcctngFac are the same (compare 1st splits only)
						if (i==0 && j==0)
						    acctFac = (String)sa.getFieldValue("AccountingFacility");
						else {
						    String facility = (String)sa.getFieldValue("AccountingFacility");
						    if (facility != null && !facility.equals(acctFac))
						        return ListUtil.list(Constants.getInteger(-1), AccountingFacilityError);
						}

						sbrtnRtCode = null;
						sbrtnMessage = null;

						 // 01.17.06  Added skip for AccountType = Other && non-Null Order#
					    if (acctType == null || !acctType.getUniqueName().
					            equals(CatEZOInvoiceAccountingValidation.skipAcctType)) {

							// FS7200 validation - first verify is not simulation
							if (!isSimulation) {
								response = CatEZOInvoiceAccountingValidation.validateAccounting(sa);
								if (response != null) {
									sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
									sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
								}
							}
							else { // use simulation
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Using Acctng Validation SIMULATION!", ClassName);

								CatAccountingCollector cac = CatEZOInvoiceAccountingValidation.getCatAccounting(sa);
						        try {
							        CatAccountingValidator validator =
							            CatEZOInvoiceAccountingValidation.callFS7200Placeholder(cac);
								    sbrtnRtCode = validator.getResultCode();
								    if (!sbrtnRtCode.equals("00"))
								        sbrtnMessage = sb.toString();

						        } catch (Exception e) {
									Log.customer.debug("%s *** SIMULATION Exception: %s", ClassName, e);
						        }
							}
					    }
					    else { // 01.17.06  Added temporary branch - skip handling for AccountType = Other

					        //if (Log.customer.debugOn)
							    Log.customer.debug("\n %s ::: TEMP logic branch - Acct Type = Other!", ClassName);

							String order = (String)sa.getFieldValue("Order");

							if (StringUtil.nullOrEmptyOrBlankString(order)) {
							    if (!isSimulation) {
									response = CatEZOInvoiceAccountingValidation.validateAccounting(sa);
									if (response != null) {
										sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
										sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
									}
								}
							    else {
								    CatAccountingCollector cac = CatEZOInvoiceAccountingValidation.getCatAccounting(sa);
							        try {
								        CatAccountingValidator validator =
								            CatEZOInvoiceAccountingValidation.callFS7200Placeholder(cac);

									    sbrtnRtCode = validator.getResultCode();
									    if (!sbrtnRtCode.equals("00"))
									        sbrtnMessage = sb.toString();

							        } catch (Exception e) {
										Log.customer.debug("%s *** SIMULATION Exception: %s", ClassName, e);
							        }
							    }
							}
							else {
						        //if (Log.customer.debugOn)
								    Log.customer.debug("\n %s ::: Order Number populated, skipping Validation!", ClassName);
							}
					    }

						if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
							AccountingErrorMsg = AccountingErrorMsg + "Line " + (i+1) + " Split " + (j+1) +
								": Error - " + sbrtnMessage + ";\n";
						}
					}
				}
			}
		}
		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			return ListUtil.list(Constants.getInteger(-1), AccountingErrorMsg);
		}

		// 4. If final approval, the ensure HoldIREscalation is turned off
		if (isFinalApprover) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Final Approver, Set HoldIREscaltion = FALSE!", ClassName);
		    ir.setFieldValue("HoldIREscalation",Boolean.FALSE);

		}
		return NoErrorResult;
	}

	public static int irPowerUser (User subject) {

	    if (subject != null) {
		    Role role = Role.getRole(Role_Purchasing);
		    if (role != null && subject.hasRole(role)) {
				return 1;
		    }
		    role = Role.getRole(Role_AP);
		    if (role != null && subject.hasRole(role)) {
				return 2;
		    }
	    }
	    return 0;
	}

	private static boolean isPostVAT(InvoiceReconciliation ir) {

		BaseVector requests = CatCommonUtil.getAllApprovalRequests(ir);
	    int size = requests.size();
	    for (int i=0;i<size;i++) {
	        ApprovalRequest ar = (ApprovalRequest)requests.get(i);
	        if (ar.getApprover().getUniqueName().equals(Name_VAT)) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Found VAT CustomApprover!", ClassName);
				if (ar.isApproved()) { // VAT approval has occured
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: VAT CustomApprover APPROVED!", ClassName);
					return true;
				}
				if (ar.getState()==8) { // VAT approval has occured
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: VAT CustomApprover *APPROVED*!", ClassName);
					return true;
				}
	        }
	    }
		return false;
	}

	private static boolean isFinalApprover(InvoiceReconciliation ir) {

	    BaseVector requests = ir.getApprovalRequests();
	    int size = requests.size();
		//if (Log.customer.debugOn)
			Log.customer.debug("CatEZOInvReconciliationApproveHook ::: FINAL ApprovalRequests SIZE() " + size);
	    for (int i=0;i<size;i++) {
	        ApprovalRequest ar = (ApprovalRequest)requests.get(i);
	        if (ar.isApproved())
	            continue;
	        if (!ar.isActive()) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Final AR(s) - NOT Approved or Active!", ClassName);
	            return false;
	        }
	    }
	    return true;
	}

	private static StringBuffer findUnclearedQtyExceptions(InvoiceReconciliation ir) {

	    StringBuffer lineExceptionMsg = null;
	    List allExceptions = ir.getAllExceptions();
	    int size = allExceptions.size();
        for (int i=0; i<size; i++) {
            InvoiceException ie = (InvoiceException)allExceptions.get(i);
             if (ie.getType().getUniqueName().indexOf("ReceivedQuantityVariance") > -1 &&
                     ie.getState() != InvoiceException.Cleared) {

                 //if (Log.customer.debugOn)
	    	            Log.customer.debug("%s *** Found UNcleared ReceivedQtyVariance exception!",ClassName);

                 BaseObject parent = ie.getParent();
                 if (parent instanceof InvoiceReconciliationLineItem) {

                     int lineNum = ((InvoiceReconciliationLineItem)parent).getNumberInCollection();
                     if (lineExceptionMsg == null)
                         lineExceptionMsg = new StringBuffer(String.valueOf(lineNum));
                     else
                         lineExceptionMsg.append(", ").append(String.valueOf(lineNum));

                     //if (Log.customer.debugOn)
	    	            Log.customer.debug("CatEZOInvReconciliationApproveHook *** Unhandled Exception at Line# " + lineNum);
                 }
             }
        }
	    return lineExceptionMsg;
	}

	private String hasQuantityException(BaseVector lines) {

		String errorMessage = null;
		String warnMessage = null;
		String exceptionName = null;

		if (lines != null) {
			int linecount = lines.size();
			for (int i = 0; i < linecount; i++) {
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) lines.get(i);

				List exceptions = irli.getExceptions();
				if (!exceptions.isEmpty()) {
					int size = exceptions.size();
					for (int j = 0; j < size; j++) {
						InvoiceException ie = (InvoiceException) exceptions.get(j);
						ariba.util.log.Log.customer.debug("%s ::: Exception: %s", ClassName, ie.getType().getUniqueName());
						ariba.util.log.Log.customer.debug("%s ::: ExceptionStatus: %s", ClassName, ie.getState());
						if (ie != null){
							if (ie.getType().getUniqueName().indexOf("ReceivedQuantityVariance") > -1){
								if (exceptionName == null){
									exceptionName = ie.getType().getUniqueName();
								}
								if (ie.getState() != InvoiceException.Cleared){
									if (errorMessage == null){
										errorMessage = "" + (i+1);
									}
									else{
										errorMessage = errorMessage + (i+1);
									}
								}
								if (errorMessage != null && i<(linecount-1)){
									errorMessage = errorMessage + ", ";
								}
							}
						}
					}
				}
			}
		}
		if (errorMessage != null){
			errorMessage = errorMessage + ";" + exceptionName + ";" + "Error";
			return errorMessage;
		}
		return null;
	}


}
