/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1	Shaila Salimath  07/05/09  issue 957 Added Null check for LineType field.
2   Nikita sharma    20/09/10  issue 1179  fix Settlement Code null pointer exception on IR 
***************************************************************************************** */

package config.java.invoicing.vcsv1;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.Comment;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.Approver;
import ariba.user.core.Group;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Message;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;
import config.java.tax.CatTaxUtil;

public class CatCSVInvoiceReconciliationApproveHook implements ApprovableHook {
	private static final String ComponentStringTable = "aml.InvoiceEform";
	private static final String catComponentStringTable = "aml.cat.Invoice";
	private static final int ValidationError = -2;
	private static final int ValidationWarning = 1;
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));
	private static final String LindaAlwaysRequired = Fmt.Sil("aml.cat.Invoice", "LindaAlwaysRequired");

	private static final String ClassName = "CatCSVInvoiceReconciliationApproveHook";

	public List run(Approvable approvable) {
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering the Approve Hook Implementation run method", ClassName);
			Log.customer.debug("%s ::: Looking at the IR: %s", ClassName, approvable.getUniqueName());
		//}

		InvoiceReconciliation ir = (InvoiceReconciliation) approvable;
		BaseVector irLineItems = ir.getLineItems();
		InvoiceReconciliationLineItem irli = null;
		SplitAccountingCollection sac = null;
		SplitAccounting sa = null;
		Response response = null;
		String AccountingErrorMsg = "";
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		InvoiceReconciliationLineItem taxLine = null;
		InvoiceReconciliationLineItem irliFirstMatLine = null;

		ariba.user.core.User currUser = (ariba.user.core.User) Base.getSession().getEffectiveUser();
		String currUserString = currUser.getMyName();
		String currUserUNString = currUser.getUniqueName();
/*
		if (!LindaAlwaysRequired.equalsIgnoreCase("true") || !("Linda M Yates".equals(currUserString))) {
			String hasQuantException = hasQuantityException(irLineItems);
			if (hasQuantException != null) {
				Log.customer.debug("%s ::: This IR has PO/MA Received Quantity Exception", ClassName);
				Log.customer.debug("%s ::: This hasQuantException string returned: %s", ClassName, hasQuantException);
				if (hasQuantException.indexOf("Error") > -1){
					String[] errorMegSplit = hasQuantException.split(";");
					String errorString =
						"Cannot proceed with an invoice reconciliation with \""
							+ errorMegSplit[1]
							+ "\": Lines (" + errorMegSplit[0] + ") must be received before further processing the invoice reconciliation.";
					if (errorString != null) {
						return ListUtil.list(Constants.getInteger(ValidationError), errorString);
					}
				}
				if (hasQuantException.indexOf("Warning") > -1){
					String[] warnMsgSplit = hasQuantException.split(";");
					String warningString =
						"You have disputed the \""
							+ warnMsgSplit[1]
							+ "\" on lines (" + warnMsgSplit[0] + ").  Please inform purchasing if you do not intend to receive any more items against this purchase order.";
					if (warningString != null) {
						return ListUtil.list(Constants.getInteger(ValidationWarning), warningString);
					}
				}
			}
		}
		else {
			Log.customer.debug("%s ::: Initial go live letting linda pass thru the exception check", ClassName);
		}
*/

		if (!LindaAlwaysRequired.equalsIgnoreCase("true") || !("Linda M Yates".equals(currUserString))) {
			String hasQuantException = hasQuantityException(irLineItems);
			if (hasQuantException != null) {
				Log.customer.debug("%s ::: This IR has PO/MA Received Quantity Exception", ClassName);
				Log.customer.debug("%s ::: This hasQuantException string returned: %s", ClassName, hasQuantException);
				if (hasQuantException.indexOf("Error") > -1){
					String[] errorMegSplit = hasQuantException.split(";");
					String errorString =
						"Cannot proceed with an invoice reconciliation with \""
							+ errorMegSplit[1]
							+ "\": Lines (" + errorMegSplit[0] + ") must be received before further processing the invoice reconciliation.";
					if (errorString != null) {
						return ListUtil.list(Constants.getInteger(ValidationError), errorString);
					}
				}
				/*
				if (hasQuantException.indexOf("Warning") > -1){
					String[] warnMsgSplit = hasQuantException.split(";");
					String warningString =
						"You have disputed the \""
							+ warnMsgSplit[1]
							+ "\" on lines (" + warnMsgSplit[0] + ").  Please inform purchasing if you do not intend to receive any more items against this purchase order.";
					if (warningString != null) {
						return ListUtil.list(Constants.getInteger(ValidationWarning), warningString);
					}
				}
				*/
			}
		}
		else {
			Log.customer.debug("%s ::: Initial go live letting linda pass thru", ClassName);
		}

		if (!LindaAlwaysRequired.equalsIgnoreCase("true") || !("Linda M Yates".equals(currUserString))) {
			Boolean taxOverrideFlagB = (Boolean) ir.getFieldValue("TaxOverrideFlag");
			Boolean taxCallNotFailedFlagB = (Boolean) ir.getFieldValue("taxCallNotFailed");
			boolean taxOverrideFlag = false;
			boolean taxCallNotFailedFlag = true;
			if (taxOverrideFlagB != null){
				taxOverrideFlag = taxOverrideFlagB.booleanValue();
			}
			if (taxCallNotFailedFlagB != null){
				taxCallNotFailedFlag = taxCallNotFailedFlagB.booleanValue();
			}
			if (currUser.hasPermission("TaxManager") && taxOverrideFlag && taxCallNotFailedFlag && !checkIfTaxExceptionHandler(ir)) {
				String errorString = Fmt.Sil(catComponentStringTable, "TaxUserNeedsToCallTax");
				if (errorString != null) {
					return ListUtil.list(Constants.getInteger(ValidationError), errorString);
				}
			}
			if (currUser.hasPermission("TaxManager") && !checkIfTaxExceptionHandler(ir)){
				String returnMessage = CatTaxUtil.checkForNullRequiredFields(ir);
				if (returnMessage != null){
					String nullFieldsMessage = "Cannot proceed with approval as following tax related fields are null: " + returnMessage;
					return ListUtil.list(Constants.getInteger(ValidationError), nullFieldsMessage);
				}
			}
		}
		else {
			Log.customer.debug("%s ::: Initial go live letting linda pass thru the web-service call check", ClassName);
		}

		int totalSACount = 0;
		BigDecimal authTaxTotal = new BigDecimal("0.0000");
		BigDecimal sumOfAuthTaxLines = new BigDecimal("0.0000");

		for (int i = 0; i < irLineItems.size(); i++) {
			irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			if ((irli.getLineType() != null) && (irli.getLineType().getCategory() == ProcureLineType.TaxChargeCategory)){
				taxLine = irli;
			}

			if (irliFirstMatLine == null){
				ClusterRoot capsChargeCodeObj = (ClusterRoot) irli.getFieldValue("CapsChargeCode");
				String capsChargeCodeString = null;
				if (capsChargeCodeObj != null) {
					capsChargeCodeString = capsChargeCodeObj.getUniqueName();
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: CAPS Charge Code is: %s", ClassName, capsChargeCodeString);
				}
				else {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Encountered a null CAPS Charge Code", ClassName);
					capsChargeCodeString = "";
				}

				if (capsChargeCodeString.equals("001")) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Found the first material line: %s", ClassName, irli.toString());
					irliFirstMatLine = irli;
				}
			}

			if(irli.getFieldValue("TaxAmountAuth") != null){
				Log.customer.debug("%s ::: The auth. tax amount is %s", ClassName, irli.getFieldValue("TaxAmountAuth"));
				//BigDecimal irliAuthAmnt = new BigDecimal(irli.getFieldValue("TaxAmountAuth").toString());
				authTaxTotal = authTaxTotal.add((BigDecimal)irli.getFieldValue("TaxAmountAuth"));

				if ((new BigDecimal("0.00")).compareTo((BigDecimal)irli.getFieldValue("TaxAmountAuth")) < 0){
//					sumOfAuthTaxLines = sumOfAuthTaxLines.add(irli.getAmountAccepted().getAmount());
					sumOfAuthTaxLines = sumOfAuthTaxLines.add(irli.getAmountAccepted().getApproxAmountInBaseCurrency());
				}
			}

			sac = irli.getAccountings();
			if (sac != null) {
				List sacList = sac.getSplitAccountings();
				int sacSize = sacList.size();
				for (int j = 0; j < sacSize; j++) {
					totalSACount++;
					sa = (SplitAccounting) sacList.get(j);
					response = CatValidateInvAccountingString.validateAccounting(sa);

                        // ssato aul - fix for NPE
	                if (response != null) {
	                    Message msg = response.getMessage();
	                    if (msg != null) {
	    					sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
	    					sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
	    					if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
	    						AccountingErrorMsg = AccountingErrorMsg + "Line " + (i+1) + " Split " + (j+1) + ": Error - " + sbrtnMessage + ";\n";
	    					}
	                    }
	                }
				}
			}
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", ClassName, AccountingErrorMsg);
			return ListUtil.list(Constants.getInteger(ValidationError), AccountingErrorMsg);
		}
		/*
		int totalSACount = 0;
		for (int i = 0; i < irLineItems.size(); i++) {
			InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) irLineItems.get(i);
			SplitAccountingCollection sac = irli.getAccountings();
			for (int j = 0; j < sac.getAllSplitAccountings().size(); j++) {
				totalSACount++;
			}
		}
		*/
		if (totalSACount > 256) {
			String errorString = Fmt.Sil(catComponentStringTable, "SplitsOverTheLimit");
			if (errorString != null) {
				return ListUtil.list(Constants.getInteger(ValidationError), errorString);
			}
		}

//		if (!LindaAlwaysRequired.equalsIgnoreCase("true") || !("Linda M Yates".equals(currUserString))) {
			if (taxLine != null){
				if ((taxLine.getLineType() != null) && (!taxLine.getLineType().getUniqueName().equals("VATCharge"))){
					//if(Log.customer.debugOn)
						Log.customer.debug("%s ::: Tax Line encountered is NOT a VAT Line", ClassName);
//					BigDecimal taxedAmount = taxLine.getAmount().getAmount();
					BigDecimal taxedAmount = taxLine.getAmount().getApproxAmountInBaseCurrency();
					BigDecimal percentage = taxedAmount.multiply(new BigDecimal("100"));
					BigDecimal maxAllowed = new BigDecimal("15.00");
					if (sumOfAuthTaxLines.compareTo(new BigDecimal("0.00")) > 0){
						percentage = percentage.divide(sumOfAuthTaxLines, BigDecimal.ROUND_HALF_UP);
					}
					else{
						if (taxedAmount.compareTo(new BigDecimal("0.00")) > 0){
							percentage = new BigDecimal("16.00");
						}
						else{
							percentage = new BigDecimal("0.00");
						}
					}
					//if(Log.customer.debugOn)
						Log.customer.debug("%s ::: The percentage of tax charged is %s", ClassName, percentage.toString());
//					if ((taxedAmount.compareTo(new BigDecimal("500.00")) > 0) || (percentage.compareTo(maxAllowed) > 0)){
					if (currUser.hasPermission("TaxManager")
						&& !checkIfTaxExceptionHandler(ir)
						&& !(ir.getRequestedAction() == 2)
						&& !(("Rejecting".equals(ir.getStatusString())) || ("Rejected".equals(ir.getStatusString())))){
					if (percentage.compareTo(maxAllowed) > 0){
/*						List exceptions = taxLine.getExceptions();
						if (!exceptions.isEmpty()) {
							int size = exceptions.size();
							for (int j = 0; j < size; j++) {
								InvoiceException ie = (InvoiceException) exceptions.get(j);
								ariba.util.log.Log.customer.debug("%s ::: Exception: %s", ClassName, ie.getType().getUniqueName());
								//ariba.util.log.Log.customer.debug("%s ::: ReferTo: %s", ClassName, irli.getReferTo());
								ariba.util.log.Log.customer.debug("%s ::: ExceptionStatus: %s", ClassName, ie.getState());
								if (ie != null){
									if (ie.getType().getUniqueName().indexOf("OverTaxVariance") > -1){
										if (ie.getState() == InvoiceException.Disputed){
											if(Log.customer.debugOn)
												Log.customer.debug("%s ::: Not Returning error as tax > $500 OR taxRate > 15 AND Exception is disputed", ClassName);
										}
										else{
*/											//if(Log.customer.debugOn)
												Log.customer.debug("%s ::: Returning error as taxRate > 15", ClassName);
											String fmt = Fmt.Sil(ComponentStringTable, "SalesTaxResonablenessError");
											return ListUtil.list(Constants.getInteger(ValidationError), fmt);
/*										}
									}
								}
							}
						}
*/					}
					}
				}
				else{
					//if(Log.customer.debugOn)
						Log.customer.debug("%s ::: Tax Line encountered is a VAT Line", ClassName);
				}
			}
//		}
//		else {
//			Log.customer.debug("%s ::: Initial go live letting linda pass thru the TAX Reasonableness check", ClassName);
//		}

		//String settlementCodeReq = CatCSVIRApprovalRulesUtil.getRequester(ir).getUniqueName();
		String settlementCodeUser = null;
		if (ir.getOrder()!=null){
			settlementCodeUser = CatCSVIRApprovalRulesUtil.getPreparer(ir).getUniqueName();
		}
		else if (ir.getMasterAgreement()!=null){
			settlementCodeUser = CatCSVIRApprovalRulesUtil.getRequester(ir).getUniqueName();
		}

		Boolean finalPayIndB = (Boolean) ir.getFieldValue("FinalPaymentIndicator");
		ClusterRoot settlementCodeObj = (ClusterRoot) ir.getFieldValue("SettlementCode");
		String settlementCodeStr = null;
		Integer settlementCodeInteger = null;
		//issue 1179 starts
		if (settlementCodeObj != null){
			settlementCodeStr = settlementCodeObj.getUniqueName();
        if (settlementCodeStr != null) 
		if ((settlementCodeStr.compareTo("00") >= 0) || (settlementCodeStr.compareTo("99") <= 0)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Settlement Code is over 00 and under 99", ClassName);
			settlementCodeInteger = new java.lang.Integer(settlementCodeStr);
		}
		else {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Settlement Code is under 00 or over 99", ClassName);
			settlementCodeInteger = new java.lang.Integer(0);
		}
		}
		else {
			 String errorString = Fmt.Sil(catComponentStringTable, "Settlementcodenotnull");
			 if (errorString != null) {
			 return ListUtil.list(Constants.getInteger(ValidationError), errorString);
		     }
		}
	  //issue 1179 ends
		//if ((currUserUNString.equals(settlementCodeReq)) && (settlementCodeInteger.intValue() > 30) && (finalPayIndB == null)) {
		if ((currUserUNString.equals(settlementCodeUser)) && (settlementCodeInteger.intValue() > 30) && (finalPayIndB == null)) {
			String errorString = Fmt.Sil(catComponentStringTable, "FinalPaymntNotSelected");
			if (errorString != null) {
				return ListUtil.list(Constants.getInteger(ValidationError), errorString);
			}
		}

		boolean finalPaymntInd = false;
		if (finalPayIndB != null)
			finalPaymntInd = finalPayIndB.booleanValue();
		//if ((currUserUNString.equals(settlementCodeReq)) && (settlementCodeInteger.intValue() > 30) && (finalPaymntInd)) {
		if ((currUserUNString.equals(settlementCodeUser)) && (settlementCodeInteger.intValue() > 30) && (finalPaymntInd)) {
			addBuyerAfterRequester(ir);
		}

		CatCSVInvoiceReconciliationEngine.calculateDiscountedAmounts(ir);

		if(taxLine != null){
			boolean overTaxDisputed = false;
			List liExceptions = taxLine.getExceptions();
			if (!liExceptions.isEmpty()){
				for(int i=0; i<liExceptions.size(); i++){
					InvoiceException ie = (InvoiceException) liExceptions.get(i);
					if (ie != null){
						if (ie.getType().getUniqueName().indexOf("OverTaxVariance") > -1){
							if (ie.getState() == InvoiceException.Disputed){
								overTaxDisputed = true;
							}
						}
					}
				}
				//if(overTaxDisputed && currUser.hasRole(Role.getRole("Tax Manager")) && isLastApprover(ir)){
				if (overTaxDisputed && isLastApprover(ir)){
					String taxState = (String) taxLine.getDottedFieldValue("TaxState.UniqueName");
					BigDecimal taxRate = null;
					if (irliFirstMatLine != null){
						taxRate = (BigDecimal) irliFirstMatLine.getDottedFieldValue("TaxRate");
					}
					else{
						taxRate = (BigDecimal) taxLine.getDottedFieldValue("TaxRate");
					}
					if (taxRate != null){
						taxRate = taxRate.setScale(4,BigDecimal.ROUND_UP);
					}
					String taxMessage = null;
					if (irliFirstMatLine!=null){
						taxMessage = (String) irliFirstMatLine.getDottedFieldValue("TaxCodeMessage");
					}
					else{
						taxMessage = (String) taxLine.getDottedFieldValue("TaxCodeMessage");
					}
					String rejectionMessage = taxState + " Tax - " + taxRate + "% - " + taxMessage;
					LongString commentText = new LongString(rejectionMessage);
					//String commentTitle = "Reason For Tax Short Pay";
					String commentTitle = "Tax Comments";
					Date commentDate = new Date();
					User commentUser = User.getAribaSystemUser(ir.getPartition());

					List commentsOnIR = ir.getComments();
					boolean disputeCommentExisted = false;
					for (int i=0; i<commentsOnIR.size(); i++){
						Comment commentItem = (Comment) commentsOnIR.get(i);
						if (commentTitle.equals(commentItem.getTitle())){
							disputeCommentExisted = true;
							commentItem.setText(commentText);
							commentItem.setDate(commentDate);
						}
					}
					if (!disputeCommentExisted){
						CatTaxUtil.addCommentToIR(ir, commentText, commentTitle, commentDate, commentUser);
					}
				}
				if (!overTaxDisputed && isLastApprover(ir)){
					//String commentTitle = "Reason For Tax Short Pay";
					String commentTitle = "Tax Comments";
					String newCommentTitle = "Tax Paid in Full per Invoice";
					LongString newCommentText = new LongString("Tax paid as invoiced");
					Date newCommentDate = new Date();
					List commentsOnIR = ir.getComments();
					//List newCommentsOnIR = new BaseVector();
					//newCommentsOnIR.clear();
					boolean disputeCommentExisted = false;
					for (int i=0; i<commentsOnIR.size(); i++){
						Comment commentItem = (Comment) commentsOnIR.get(i);
						if (commentTitle.equals(commentItem.getTitle())){
							disputeCommentExisted = true;
							//commentItem.setTitle(newCommentTitle);
							commentItem.setText(newCommentText);
							commentItem.setDate(newCommentDate);
						}
						//else{
						//	newCommentsOnIR.add(commentItem);
						//}
					}
					//if (disputeCommentExisted){
					//	ir.setDottedFieldValue("Comments",null);
					//	ir.setDottedFieldValue("Comments",newCommentsOnIR);
					//}
				}
			}
		}

		//Added By KM
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
		Log.customer.debug("%s ::: HandleMustExceptions: Outside the loop ApprovalRequests %s", ClassName, currentar);

		//Checks the reason for the ApprovalRequest
		//ExceptionHandlerMustApprove, "Exception Handler must approve this Invoice Reconciliation"
		String strReason = (String)currentar.getFieldValue("Reason");
		String strReasonCheck = Fmt.Sil("aml.cat.Invoice", "ReasonCheck");
		//if (Log.customer.debugOn)
		{
			Log.customer.debug("%s ::: HandleMustExceptions: Checking for the string \"%s\"", ClassName, strReasonCheck);
			Log.customer.debug("%s ::: HandleMustExceptions: Checking inside the string \"%s\"", ClassName, strReason);
		}
		if ( isCurrentUserApprover && strReason.indexOf(strReasonCheck) != -1 )
		{
			String  MustHandleExceptions = Fmt.Sil("aml.cat.Invoice","MustHandleExceptions");
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
										Log.customer.debug("%s ::: As LineLevelException %s Matching With MustHandleException %s...Now checking if the approver is responsible for handling this...", ClassName, HandleException, ie.getType().getUniqueName());
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
		//End Of Addition By KM

		return NoErrorResult;
	}

	private String hasQuantityException(BaseVector lines) {
		//ClusterRoot CapsCC = null;
		//String CapsCCString = null;
		String errorMessage = null;
		String warnMessage = null;
		String exceptionName = null;

		if (lines != null) {
			int linecount = lines.size();
			for (int i = 0; i < linecount; i++) {
				InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem) lines.get(i);
				//CapsCC = (ClusterRoot) irli.getFieldValue("CapsChargeCode");
				//if (CapsCC != null){
				//	CapsCCString = CapsCC.getUniqueName();
				//}
				List exceptions = irli.getExceptions();
				if (!exceptions.isEmpty()) {
					int size = exceptions.size();
					for (int j = 0; j < size; j++) {
						InvoiceException ie = (InvoiceException) exceptions.get(j);
						ariba.util.log.Log.customer.debug("%s ::: Exception: %s", ClassName, ie.getType().getUniqueName());
						//ariba.util.log.Log.customer.debug("%s ::: ReferTo: %s", ClassName, irli.getReferTo());
						ariba.util.log.Log.customer.debug("%s ::: ExceptionStatus: %s", ClassName, ie.getState());
						if (ie != null){
							if (ie.getType().getUniqueName().indexOf("ReceivedQuantityVariance") > -1){
								if (exceptionName == null){
									exceptionName = ie.getType().getUniqueName();
								}
								if (ie.getState() == InvoiceException.Disputed){
									if (warnMessage == null){
										warnMessage = "" + (i+1);
									}
									else{
										warnMessage = warnMessage + (i+1);
									}
								}
								else{
									//if ((CapsCC != null && "001".equals(CapsCCString)) && (ie.getState() != InvoiceException.Cleared)){
									if (ie.getState() != InvoiceException.Cleared){
										if (errorMessage == null){
											errorMessage = "" + (i+1);
										}
										else{
											errorMessage = errorMessage + (i+1);
										}
									}
								}
								if (errorMessage != null && i<(linecount-1)){
									errorMessage = errorMessage + ", ";
								}
								if (warnMessage != null && i<(linecount-1)){
									warnMessage = warnMessage + ", ";
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
		/*
		if (warnMessage != null){
			warnMessage = warnMessage + ";" + exceptionName + ";" + "Warning";
			return warnMessage;
		}
		*/
		return null;
	}

	private void addBuyerAfterRequester(InvoiceReconciliation ir) {
		//User buyerUser = (User) CatCSVIRApprovalRulesUtil.getBuyer(ir);
		Approver buyerUser = (Approver) CatCSVIRApprovalRulesUtil.getBuyer(ir);
		User reqUser = null;
		if (ir.getOrder() != null){
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning preparer for Order %s", ClassName, ir.getOrder().getUniqueName());
			reqUser = (User) CatCSVIRApprovalRulesUtil.getPreparer(ir);
		}
		else{
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Returning Contract Contact for Contract %s", ClassName, ir.getMasterAgreement().getUniqueName());
			reqUser = (User) CatCSVIRApprovalRulesUtil.getRequester(ir);
		}

		boolean buyerInAppFlow = false;
		if (buyerUser instanceof User){
			buyerInAppFlow = ir.hasRequiredApproval((User)buyerUser);
		}

		if (!buyerInAppFlow && reqUser != null) {
			ApprovalRequest arBuyerUser =
				ApprovalRequest.create(ir, buyerUser, true, "cat.rulereasons", "SettlementCode_Approver_Buyer");
			BaseVector requests = ir.getApprovalRequests();

			ApprovalRequest arTaxWatcher = (ApprovalRequest) requests.get(0);
			BaseVector arbeforeTaxWatcher = (BaseVector) arTaxWatcher.getDependencies();

			BaseVector buyerDependencies = arBuyerUser.getDependencies();
			for (int i = 0; i < arbeforeTaxWatcher.size(); i++) {
				buyerDependencies.add(i, arbeforeTaxWatcher.get(i));
			}
			arBuyerUser.setFieldValue("Dependencies", buyerDependencies);

			arbeforeTaxWatcher.removeAllElements();
			arbeforeTaxWatcher.add(0, arBuyerUser);
			arTaxWatcher.setFieldValue("Dependencies", arbeforeTaxWatcher);
		}
	}

	private boolean isLastApprover(InvoiceReconciliation ir, User currUser){
		List arList = (List) ir.getFieldValue("ApprovalRequests");
		if (ListUtil.nullOrEmptyList(arList)){
			return true;
		}
		for(int i=0; i<arList.size(); i++){
			ApprovalRequest currentAR = (ApprovalRequest) arList.get(i);
			Approver approver = currentAR.getApprover();
			if (approver instanceof User){
				if (!approver.equals(currUser)) {
					ariba.util.log.Log.customer.debug("%s ::: Current user is not the approver ", ClassName);
					return false;
				}
			} else if(approver instanceof Role) {
				if (!currUser.hasRole((Role)approver)) {
					return false;
				}
			} else if(approver instanceof Group) {
				if (!currUser.hasGroup((Group)approver)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isLastApprover(Approvable approvable) {
		//get the current user
		User currentUser = (User) Base.getSession().getEffectiveUser();

		//if no required approver or no approvers then return true
		List arList = (List) approvable.getFieldValue("ApprovalRequests");
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: isLastApprover: Starting function with approvable: %s", ClassName, approvable.getUniqueName());

		if (ListUtil.nullOrEmptyList(arList) || !hasRequiredApprover(approvable)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: isLastApprover: Null ApprovalRequest Vector. or no required approver Is last Approver.", ClassName);
			return true;
		}

		//get all current required approvers
		Iterator arRequiredActiveIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StateActive, null, new Boolean(true));

		//get all pending required approvers
		Iterator arPendingRequiredIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StateNotActive, null, new Boolean(true));

		//make sure that there are no pending required approvers and that all the current required
		//approvers are not one in the same with the current users
		if (arPendingRequiredIterator.hasNext() == false) {
			while (arRequiredActiveIterator.hasNext()) {
				ApprovalRequest currentAR = (ApprovalRequest) arRequiredActiveIterator.next();
				Approver approver = currentAR.getApprover();
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: isLastApprover: Iterating through role: %s", ClassName, approver.getName());

				if (approver instanceof User) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: isLastApprover: Approver is a user", ClassName);
					if (!approver.equals(currentUser)) {
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: isLastApprover: Approver is not CurrentUser", ClassName);
						return false;
					}
				}
				else if(approver instanceof Role) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: isLastApprover: Approver is Role", ClassName);
					if (!currentUser.hasRole((Role) approver)) {
						//if (Log.customer.debugOn)
							Log.customer.debug("% ::: isLastApprover: CurrentUser does not have role", ClassName);
						return false;
					}
				}
				else if(approver instanceof Group) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: isLastApprover: Approver is Group", ClassName);
					if (!currentUser.hasGroup((Group) approver)) {
						//if (Log.customer.debugOn)
							Log.customer.debug("% ::: isLastApprover: CurrentUser does not have group", ClassName);
						return false;
					}
				}

			}

			//this means that there are no pending approvers
			//and all the active required approvers are associated with the current user.
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: isLastApprover: No pending approvers and all active approvers are associated with the current user", ClassName);
			return true;
		}

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: isLastApprover: Not fully approved", ClassName);
		return false;
	}

	public static boolean checkIfTaxExceptionHandler(Approvable approvable) {
		//get the current user
		User currentUser = (User) Base.getSession().getEffectiveUser();

		//if no required approver or no approvers then return true
		List arList = (List) approvable.getFieldValue("ApprovalRequests");
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: checkIfTaxExceptionHandler: Starting function with approvable: %s", ClassName, approvable.getUniqueName());

		if (ListUtil.nullOrEmptyList(arList) || !hasRequiredApprover(approvable)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: checkIfTaxExceptionHandler: Null ApprovalRequest Vector. or no required approver Is last Approver.", ClassName);
			return false;
		}

		//get all current required approvers
		Iterator arRequiredActiveIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StateActive, null, new Boolean(true));

		//get all pending required approvers
		Iterator arPendingRequiredIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StateNotActive, null, new Boolean(true));

		//make sure that there are no pending required approvers and that all the current required
		//approvers are not one in the same with the current users
		while (arRequiredActiveIterator.hasNext()) {
			ApprovalRequest currentAR = (ApprovalRequest) arRequiredActiveIterator.next();
			Approver approver = currentAR.getApprover();
			String reason = currentAR.getReason();
			String exceptionReason = "Exception Handler must approve this Invoice Reconciliation for Special Charge Variance";

			if (exceptionReason.equals(reason)){
				if(approver instanceof Role) {
					if (currentUser.hasRole((Role) approver)){
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: checkIfTaxExceptionHandler: (Role) Returning true as required approver acting as exception handler.", ClassName);
						return true;
					}
				} else if(approver instanceof Group) {
					if (currentUser.hasGroup((Group) approver)){
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: checkIfTaxExceptionHandler: (Group) Returning true as required approver acting as exception handler.", ClassName);
						return true;
					}
				}
			}
		}

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: checkIfTaxExceptionHandler: Returning false as required approver not acting as tax exception handler.", ClassName);
		return false;
	}

	public static boolean hasRequiredApprover(Approvable approvable) {
		//find all ars in any state, any role, and required
		Iterator arIterator = approvable.getApprovalRequestsIterator(ApprovalRequest.StatesAll, null, new Boolean(true));
		return arIterator.hasNext();
	}
}
