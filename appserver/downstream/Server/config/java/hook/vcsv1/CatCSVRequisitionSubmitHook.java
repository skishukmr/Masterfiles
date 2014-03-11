/* Created by KS for R1 - Dec 22, 2004
 * Revised by KS for R4 - Sep 30, 2005
 * ---------------------------------------------
 * 02.13.06 (chandra) - remove NeedBy validation for V2+ requisitions - #273 *
 * 03.23.06 (ks) - Fixed Issue# with line item comments disappearing during line reordering
 * 04.11.06 (ks) - Commented out ALL line resequencing code; added AC/Mat line resyncing separately
 * 04.14.06 (ds) - Added code to prevent submission of original JET$ imported requisitions
 * 04.25.06 (chandra) - Included contract file number WS validation
 * 06.29.06 (ks) - CR52 - Added comment Attachment check to set Requisition.AttachmentIndicator
 * 06.11.13 (pgskannan) issue 576 - Added to restrict users from changing orders which are in receiving
 *                                    or invoicing states
 *07.08.07 (Madhavan)Issue 617-Added code to prevent submission of PR if designated approver is inactive and
 *                     if preparer adds himself/herself as Designated approver.Method names-checkforactiveDA,checkforPreparerasDA
 *07.01.08 (Kingshuk/Chandra)Issue 725 - commented code fix put for issue 576
 *31.07.08 (Ashwini) Issue 831-Checking for FOBPoint And setting the value
 *04.11.08 (Deepak) Added Far / DFar Condition for the requisition submit.
 *15/01/2014     IBM Parita Shah	SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8) MSC Tax Gaps Correct Legal Entity
 -------------------------------------------------------------------------------------------------------------------

  	Change Author: 	Deepak
  	Date Modified:  04/11/2008
  	Description: 	Added Far / DFar Condition for the requisition submit for CR 159


  ---------------------------------------------------------------------------------------------------------------------


 */
package config.java.hook.vcsv1;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.Comment;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.fields.ConditionResult;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.condition.NeedByDate;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
// Starts SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
import ariba.base.core.ClusterRoot;
// Ends SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.atm.business.ValidationStatus;
import config.java.action.CatValidateAccountingString;
import config.java.action.vcsv1.CatSetAdditionalChargeLineItemFields;
import config.java.action.vcsv1.CatValidateCSVAccounting;
import config.java.common.BusinessWebService;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;
import config.java.integration.ws.AccountValidator;
import config.java.ordering.vcsv1.CatCSVAllDirectOrder;

public class CatCSVRequisitionSubmitHook implements ApprovableHook {

	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
	private static final String SingleSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Single");
	private static final String MultiSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Multiple");
    private static final String SingleSplitCFNError = ResourceService.getString("cat.vcsv1","ContractFileNumberError_Single");
    private static final String MultiSplitCFNError = ResourceService.getString("cat.vcsv1","ContractFileNumberError_Multiple");
    private static final String CFNWebServiceDown = ResourceService.getString("cat.vcsv1","CFNWebServiceDown");
	private static final String THISCLASS = "CatCSVReqSubmitHook";
	private static final String NeedByFlag = ResourceService.getString("cat.vcsv1","NeedByFlag");
	private static final String ReturnCode = ResourceService.getString("cat.vcsv1","RequisitionSubmitHookReturnCode");
	private static final String NeedByError = ResourceService.getString("cat.vcsv1","NeedByLeadTimeError");
	private static final String PreR4ChangeError = ResourceService.getString("cat.vcsv1","ChangeOrderPreR4Error");
	private static final String JETSOriginalReq = ResourceService.getString("cat.vcsv1", "JETSImportedProjectError");
	private static final String JETSAdapterSource = ResourceService.getString("cat.vcsv1", "JETSAdapterSource");
	private static final String ErrorInActiveApprover=ResourceService.getString("cat.vcsv1","ErrorInActiveApprover");
	private static final String ErrorApprover_PreparerCannotApproveRequestionAsDA=ResourceService.getString("cat.vcsv1","ErrorApprover_PreparerCannotApproveRequestionAsDA");

	private static final String ErrAccountingWebSerivceDown=ResourceService.getString("cat.vcsv1","ErrAccountingWebSerivceDown"); //Code Added For Web Server down Issue

	private static final String FOB_TEXT_HAZMAT = ResourceService.getString("cat.aml.picklistvalues1","FOBPointValue8");
        private static final String FOB_TEXT = ResourceService.getString("cat.aml.picklistvalues1","FOBPointValue1");
    private static final String dept1 = ResourceService.getString("cat.vcsv1","FARDFARDepartment1");
	private static final String dept2 = ResourceService.getString("cat.vcsv1","FARDFARDepartment2");
	private static final String dept3 = ResourceService.getString("cat.vcsv1", "FARDFARDepartment3");
	private static final String FARDFARError =ResourceService.getString("cat.vcsv1","FARDFARError");


	protected boolean isEdit = false;  // used since SubmitHook also called from CheckinHook

	public List run(Approvable approvable) {

		FastStringBuffer totalMsg = new FastStringBuffer ();
		boolean hasErrors = false;
		String error = "";

		if (approvable instanceof Requisition) {
			Requisition r = (Requisition)approvable;
			Partition partition = r. getPartition();

			// Always set flag to true (used for Requisition AC defaulting from contract)
			r.setFieldValue("IsSubmitting",new Boolean(true));
			Log.customer.debug("%s *** IsSubmitting (entry): %s", THISCLASS, r.getFieldValue("IsSubmitting"));

			// DJS - BEGIN: Error logic added for preventing imported JET$ requisitions from being submitted
			Integer isImportObj = (Integer) r.getDottedFieldValue("isImport");
			int isImport = 0;
			if (isImportObj != null) {
				isImport = isImportObj.intValue();
			}
			String adapterSource = r.getAdapterSource();
			if (JETSAdapterSource.equals(adapterSource) && isImport == 1) {
				return ListUtil.list(Constants.getInteger(-1), JETSOriginalReq);
			}
			// DJS - END: Error logic

			// *R4 Remove* - removed adding "Emergency Buy" to title (now handled via UI trigger)

	 /* 0th Test - NO REVISIONS ON PRE-R4 ORDERS (added 12/06/05) */

			Requisition preVersion = (Requisition)r.getPreviousVersion();
			if (!isEdit && preVersion != null) { // test 1st line as indicator for all lines
				BaseVector preLines = preVersion.getLineItems();
				if (!preLines.isEmpty()) {
					PurchaseOrder po = ((ReqLineItem)preLines.get(0)).getOrder();
					if (po != null && po.getUniqueName().startsWith("P")) {  // P used instead of D prior to R4
						Log.customer.debug("%s *** FOUND PRE-R4 PO, REVISION NOT ALLOWED!", THISCLASS);
						return ListUtil.list(Constants.getInteger(-1), PreR4ChangeError);
					 }
				}
			}
			// commented the Issue 576 per Jon's request to check the native change PO functionality
			//Added to restrict users from changing orders which are in receiving or invoicing states

			//Issue 725 - restrict users from changing or cancelling if it is not allowed.
			if ( preVersion != null ) {
				Log.customer.debug("%s :  Checking this change req if Change/Cancel is allowed on this" ,THISCLASS );
				//instance of order method
				CatCSVAllDirectOrder orderMethod = new CatCSVAllDirectOrder();

				//for each line on req, check if the new change req is allowed
				for(Iterator itrli = r.getLineItemsIterator();itrli.hasNext();) {
					ReqLineItem rli = (ReqLineItem) itrli.next();
					//get the latest order of this req in the previous version
					PurchaseOrder latestOrder = rli.getLatestOrder();
					Log.customer.debug("%s :  rli=" + rli+ " latestorder="+ latestOrder,THISCLASS );
					if (latestOrder != null) {
						//get the corrosponding line on the order for this reqline item to compare
						POLineItem  poli = (POLineItem)latestOrder.getLineItem(rli.getNumberOnPO());
						//compare the two lines if it will aggregate or cancel and create a new order on this
						//latest change req.
						boolean canAggregate = false;
						try {
							canAggregate = orderMethod.canAggregateLineItems(rli, poli);
						}catch (OrderMethodException ome) {
							Log.customer.debug("%s: ERROR OrderMethodException caught. continue with canAggregate="+canAggregate, THISCLASS);
						}
						if(canAggregate) { //if true, the lines will aggregate
							Log.customer.debug("%s :  canAggregate=" + canAggregate, THISCLASS );
				            if(latestOrder.isReceived()) {
                				Log.customer.debug("%s: Change not allowed as it's previous version was received", THISCLASS);
                				return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnChangeableReceivingError"));
            				}
            				if(latestOrder.isInvoicedLineItems()) {
                				Log.customer.debug("%s: Change not allowed as it's prev version was invoiced", THISCLASS);
                				return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnChangeableInvoicingError"));
							}
						} else { //cannot aggregate
							Log.customer.debug("%s :  canAggregate=" + canAggregate, THISCLASS );
							//the new rli will cause a change order
				            if(latestOrder.isReceiving() || latestOrder.isReceived()) {
                				Log.customer.debug("%s: Cancel not allowed as it's previous version was receiving/received", THISCLASS);
                				return ListUtil.list(Constants.getInteger(-1),
                							Fmt.Sil("cat.vcsv1", "PrevOrderIsUnCancellableReceivingError",
                									new Integer(rli.getNumberInCollection()),
                									latestOrder.getOrderID()));
            				}
            				if(latestOrder.isInvoicingLineItems() || latestOrder.isInvoicedLineItems()) {
                				Log.customer.debug("%s: Cancel not allowed as it's prev version was invoicing/invoiced", THISCLASS);
                				return ListUtil.list(Constants.getInteger(-1),
                							Fmt.Sil("cat.vcsv1", "PrevOrderIsUnCResourceService.getStringbleInvoicingError",
                									new Integer(rli.getNumberInCollection()),
                									latestOrder.getOrderID()));
							}
						}
					}
				}//end reqline iterate
				for(Iterator itdrli = r.getDeletedLineItemsIterator();itdrli.hasNext();) {
					ReqLineItem drli = (ReqLineItem) itdrli.next();
					PurchaseOrder dlatestOrder = drli.getLatestOrder();
					Log.customer.debug("%s :  drli=" + drli+ " dlatestorder="+ dlatestOrder, THISCLASS);
					if (dlatestOrder != null) {
						if(dlatestOrder.isReceiving() || dlatestOrder.isReceived()) {
							Log.customer.debug("%s: Cancel not allowed as it's previous version was receiving/received", THISCLASS);
							return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnCancellableReceivingDLError"));
						}
						if(dlatestOrder.isInvoicingLineItems() || dlatestOrder.isInvoicedLineItems()) {
							Log.customer.debug("%s: Cancel not allowed as it's prev version was invoicing/invoiced", THISCLASS);
							return ListUtil.list(Constants.getInteger(-1), ResourceService.getString("cat.vcsv1", "PrevOrderIsUnCancellableInvoicingDLError"));
						}
					}
				}
				orderMethod = null;
			}
			//issue 725 end
			// issue 576 - end



			// Starts SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)
			Log.customer.debug("CatCSVRequisitionSubmitHook AccountingFacilityName RSD111 ");
			if (!r.getLineItems().isEmpty())
			{
				ReqLineItem reqlifirst = (ReqLineItem)r.getLineItem(1);

				Log.customer.debug("CatCSVRequisitionSubmitHook Requisition line item is:",+reqlifirst.getNumberInCollection());

				if(reqlifirst != null)
				{
					Log.customer.debug("CatCSVRequisitionSubmitHook AccountingFacilityName RSD111 ");
					SplitAccounting reqlisa = (SplitAccounting)reqlifirst.getAccountings().getSplitAccountings().get(0);
					if (reqlisa != null)
					{
						String accfac = (String)reqlisa.getDottedFieldValue("AccountingFacility");
						if (!StringUtil.nullOrEmptyOrBlankString(accfac))
						{
							Log.customer.debug("CatCSVRequisitionSubmitHook AccountingFacility is:",accfac );

							ClusterRoot Accfacility = Base.getService().objectMatchingUniqueName("cat.core.Facility", Base.getSession().getPartition(), accfac);
							if(Accfacility != null)
							{
								Log.customer.debug("CatCSVRequisitionSubmitHook: Facility Object:", Accfacility.getUniqueName());

								String Acclegalentity = (String)Accfacility.getDottedFieldValue("Name");
								Log.customer.debug("CatCSVRequisitionSubmitHook: Facility Name is:", Acclegalentity);
								if(!StringUtil.nullOrEmptyOrBlankString(Acclegalentity))
								{
									r.setDottedFieldValue("AccountingFacilityName",Acclegalentity);
								}


							}

						}

					}
				}
			}

			// Starts SpringRelease_RSD 111(FDD4.7,4.8/TDD1.7,1.8)

            //Log.customer.debug("CatCSVReqSubmitHook*** starting of the check for active Designated approver");
			BaseVector lines = r.getLineItems();
			//List reqli=r.getLineItems();
	/* Ashwini: FOB check */
           // setFOBPoint(reqli);
			for (int i = 0; i < lines.size(); i++) {
				int errorLine = i + 1;
				boolean hasBadAcctng = false;
				boolean hasBadNeedBy = false;
				ReqLineItem rli = (ReqLineItem)lines.get(i);


	  /* 1st Test - VALIDATE ACCOUNTING */
				// *R4 ADD* - do not validate accounting for AC lines (since set below from associated material line)

 /* live */     String lineresult = CatAdditionalChargeLineItem.isAdditionalCharge(rli) ? "0" : checkAccounting(rli);
 //*local */    	String lineresult = CatAdditionalChargeLineItem.isAdditionalCharge(rli) ? "0" : checkAccountingLocal(rli);
				if (!lineresult.equals("0")) {
					String formatLineError = Fmt.S(lineresult, errorLine);
					hasErrors = true;
					hasBadAcctng = true;
					totalMsg.append(formatLineError);
					Log.customer.debug("%s *** Line Error Msg: %s", THISCLASS, formatLineError);
				}
	/* 2nd Test - ARIBA'S OOB NEEDBY CHECK */

				//remove NeedBy validation for V2+ requisitions - #273
				if (r.getPreviousVersion() == null) {
					Date needby = rli.getNeedBy();
					NeedByDate nbd = new NeedByDate();
					PropertyTable ptable = new PropertyTable(getPropertyMap(r, rli));
					Log.customer.debug("%s *** ptable: %s", THISCLASS, ptable);
					ConditionResult cr = nbd.evaluateAndExplain(needby, ptable);
					Log.customer.debug("%s *** CR: %s", THISCLASS, cr);
					if (cr != null) {
						hasErrors = true;
						String crmsg = null;
						if (cr.getWarningCount() > 0)
							crmsg = cr.getFirstWarning();
						if (cr.getErrorCount() > 0) {
							crmsg = cr.getFirstError();
						}
						Log.customer.debug("%s *** CR Message: %s", THISCLASS, crmsg);
						if (crmsg != null) {
							hasBadNeedBy = true;
							crmsg = NeedByFlag + crmsg;
							if (!hasBadAcctng)
								crmsg = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + crmsg;
							totalMsg.append(crmsg);
						}
					}
				}
	/* 3rd Test - NON-CATALOG NEED BY CHECK (COMPARE TO LEAD TIME WHEN SET BY USER/PURCHASING */
		 // NO LONGER NEEDED - ARIBA's OOB ALSO COVERS NON-CATALOG AT SUBMIT
			 /*    	if (rli.getIsAdHoc()) {
					String resultNeedBy = checkNeedByDate(rli);
					if (!resultNeedBy.equals("0")) {
						hasErrors = true;
						if (!hasBadAcctng && !hasBadNeedBy)
							resultNeedBy = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + resultNeedBy;
						totalMsg.append(resultNeedBy);
					}
				}
			*/
				Log.customer.debug("CatCSVReqSubmitHook *** Finished Line#: " + errorLine);
			}
//Added by Deepak for 847
			String errorFARDFARAttachment = checkFARDFARAttachment(r);
			if(errorFARDFARAttachment.equals("0"))
			{
			    hasErrors = true;
			    //totalMsg.append("Requisition can not be submitted w/o attachment since special departments are used(FAR/DFAR)");
			    totalMsg.append(FARDFARError);
			}

			Log.customer.debug("CatCSVReqSubmitHook *** Finished ALL lines and hasErrors? " + hasErrors);
			if (hasErrors) {
				int code = 1;
				if (ReturnCode.equals("1") || ReturnCode.equals("-1") || ReturnCode.equals("0"))
					code = Integer.parseInt(ReturnCode);
				Log.customer.debug("%s *** Total Error Msg: %s", THISCLASS, totalMsg.toString());
				// always reset SubmitHook flag to FALSE before returning
				r.setFieldValue("TaxOverrideFlag",new Boolean(false));
				Log.customer.debug("%s *** SubmitHook Flag (exit): %s", THISCLASS, r.getFieldValue("TaxOverrideFlag"));
				return ListUtil.list(Constants.getInteger(code), totalMsg.toString());
			}

  // 03.10.06 (KS)#375/400 - since no longer reordering, must resync AC lines with Material Lines in case changes
			else {
				resyncAddChargeLines(lines);

  	      	    // 06.29.06 (KS) CR52 - update Attachment indicator on Req header
  	      	    // must rerun checks everytime since attachment could have been deleted by user
  	      	    boolean hasAttachment = false;
  	      	    BaseVector comments = r.getComments();
  	      	    int cSize = comments.size();
  	      	    for (int i=0;i<cSize;i++) {
  	      	        BaseVector attchs = ((Comment)comments.get(i)).getAttachments();
  	      	        if (!attchs.isEmpty()) {
	  	      	        hasAttachment = true;
	  	      	        r.setFieldValue("AttachmentIndicator",Boolean.TRUE);
	  	      	        break;
  	      	        }

  	      	    }
  	      	    if (!hasAttachment)
  	      	        r.setFieldValue("AttachmentIndicator",Boolean.FALSE);
			}

  // 03.10.06 (KS)#375/400 - pulled out all reordering code due to issues with History corruption, etc.

			/* *R4 NEW* If no errors, then 1) reorder lines, 2) reset references and 3) update AC line fields */

  /*      	else {
				if (preVersion == null) {
					Log.customer.debug("%s *** ORIGINAL SUBMISSION, reordering Lines!",THISCLASS);

				 // 03.23.06 (ks) updated to relink line item comments to new ReqLineItem BaseObjects
				 //   Must use new BaseVector - unable to set LineItems with OrderedLines list

					List orderedLines = CatTaxCollector.reorderLineItems(lines,true);
					BaseVector newlines = new BaseVector();
					newlines.addAll(orderedLines);
					Log.customer.debug("CatCSVReqSubmitHook *** Lines returned from reordering: " + orderedLines.size());
					List comments = r.getCommentsOfType(1);
					int cCount = comments.size();
					int lCount = orderedLines.size();
					for (int x=0;x<lCount;x++) {
						ReqLineItem rli = (ReqLineItem)orderedLines.get(x);
						Log.customer.debug("%s *** rli: %s",THISCLASS, rli);
						for (int y=0;y<cCount;y++) {
							Comment comment = (Comment)comments.get(y);
							ReqLineItem rli_c = (ReqLineItem)comment.getLineItem();
							Log.customer.debug("%s *** rli_c: %s",THISCLASS, rli_c);
							if (rli_c == rli) {
								comment.setLineItem((ReqLineItem)newlines.get(x));
								break;
							}
						}
					}
					r.setFieldValue("LineItems",newlines);
				}
				//  +++ If change order, do NOT reorder previous lines - replacing line items vector will result in
				//        Ariba treating as deletes & additions - VERY BAD! +++
				else {  // must be a change order revision
					if (!isEdit) {  // only reorder lines if Submit (not Checkin)
						BaseVector comments = r.getComments();
						int beforeLines = ((Requisition)r.getPreviousVersion()).getLineItemsCount();
						int afterLines = r.getLineItemsCount();
						BaseVector deletions = r.getDeletedLineItems();
						Log.customer.debug("%s *** REVISION SUBMISSION (CO), Lines (Before/After): " + beforeLines + afterLines);
						if (!deletions.isEmpty()) {
							int deletedLines = deletions.size();
							Log.customer.debug("CatCSVReqSubmitHook *** Total Deleted Lines: " + deletedLines);
							if (afterLines > (beforeLines - deletedLines)) {
								Log.customer.debug("%s *** Deletions AND Additions, reordering Lines!");
								// only reorder new lines added on revision
								reorderLineItemsOnRevision(lines,comments,false,(beforeLines-deletedLines),deletedLines);
								r.refreshLineItemReferences();
							}
						}
						else if (afterLines > beforeLines) {
							Log.customer.debug("%s *** Additions only, reordering Lines!");
							// only reorder new lines added on revision
							reorderLineItemsOnRevision(lines,comments,false,beforeLines,0);
							r.refreshLineItemReferences();
						}
						// MUST now refresh all AC lines from material line (in case material line modified in new version)
						lines = r.getLineItems();
						int size = lines.size();
						while (size>0) {
							ReqLineItem acLine = (ReqLineItem)lines.get(size-1);
							int refNum = ((Integer)acLine.getFieldValue("ReferenceLineNumber")).intValue();
							if (refNum != acLine.getNumberInCollection()) { // AC quick test
								ReqLineItem matLine = (ReqLineItem)r.getLineItem(refNum);
								Log.customer.debug("CatCSVReqSubmitHook *** FOUND AC Line, update AC line#: " + (size));
								CatSetAdditionalChargeLineItemFields.setAdditionalChargeFields(matLine,acLine);
							}
							size--;
						}
					}
				}
			}
*/
			// always reset SubmitHook flag to FALSE before returning
			r.setFieldValue("IsSubmitting",new Boolean(false));
			Log.customer.debug("%s *** IsSubmitting (exit): %s", THISCLASS, r.getFieldValue("IsSubmitting"));
			//BaseVector lines = r.getLineItems();
		    if (!checkforactiveDA(lines,partition))
			    return ListUtil.list(Constants.getInteger(-2), ErrorInActiveApprover);
			if (!checkforPreparerasDA(lines,partition))
                return ListUtil.list(Constants.getInteger(-2), ErrorApprover_PreparerCannotApproveRequestionAsDA);
		}
		return NoErrorResult;

	}


	public CatCSVRequisitionSubmitHook() {
		super();
	}

	protected String checkAccounting(ReqLineItem rli) {

		String lineErrorResult = "0";
		int lineErrors = 0;
		FastStringBuffer lineMsg = new FastStringBuffer();
		SplitAccountingCollection sac = rli.getAccountings();
        User currUser = (User)Base.getSession().getEffectiveUser();
		String testvalue = "R8";
		String userAcctFacility = (String)currUser.getFieldValue("AccountingFacility");
		if (sac != null) {
			BaseVector splits = sac.getSplitAccountings();
			for (int j = 0; j < splits.size(); j++) {
				int splitErrors = 0;
      			int cfnErrors = 0;
				int errorSplit = j + 1;
				FastStringBuffer splitMsg = new FastStringBuffer();
      			FastStringBuffer cfnSplitMsg = new FastStringBuffer();
				SplitAccounting sa = (SplitAccounting)splits.get(j);
				if (sa != null && !testvalue.equalsIgnoreCase(userAcctFacility)) {
					CatAccountingCollector cac = CatValidateAccountingString.getCatAccounting(sa);
					if (cac != null) {
						CatAccountingValidator response = null;
						try {
	/* LIVE	*/				response = AccountValidator.validateAccount(cac);
							Log.customer.debug("%s *** CatAccountingValidator: %s", THISCLASS, response);
							if (response != null) {
								Log.customer.debug("%s *** ResultCode: %s", THISCLASS, response.getResultCode());
								Log.customer.debug("%s *** Message: %s", THISCLASS, response.getMessage());
								if (!response.getResultCode().equals("00")) {
									splitErrors += 1;
									lineErrors += 1;
									splitMsg.append(response.getMessage() + ". ");
									Log.customer.debug("CatCSVReqSubmitHook *** Split#: " +
											errorSplit + " Error: " + response.getMessage());
								}
								//	*R4 Add* - Reset line Acctng Validation Message to null if no acctng error
								else {
									Log.customer.debug("%s *** Accounting is valid, reset message to NULL!", THISCLASS);
									sa.setFieldValue("ValidateAccountingMessage",null);
								}
							}

							//Code added for issue--837
							else
							{
								//String msg=ListUtil.list(Constants.getInteger(-2), ErrAccountingWebSerivceDown)).toString();
								//return (ListUtil.list(Constants.getInteger(-2), ErrAccountingWebSerivceDown)).toString();
								String errMsg="Accounting Web Serivce Down";
								return  errMsg;
							}

							//Validate the contract file number
							String contractFileNumber = (String) sa.getFieldValue("ContractFileNumber");
							Log.customer.debug("%s *** contractfile#=%s",THISCLASS, contractFileNumber);
							if (!StringUtil.nullOrEmptyOrBlankString(contractFileNumber)) {
								/* Reason Codes for isValidContractNumber() -
									-	"Bad Contract File Number."
									-	"No Such Contract File Found"
									-   "Valid Contract File But Obsolete Date"
									-   "Bad Contract File Date"
									-	"Exception while validating."
								*/

								Date today = Date.getNow();
								String yearMonthDate = today.toYearMonthDate();
								String currDateString = yearMonthDate.substring(4,8) + yearMonthDate.substring(0,4);

								BusinessWebService bws = new BusinessWebService();
								ValidationStatus vs = bws.isValidContractNumber(contractFileNumber, currDateString);

								Log.customer.debug("%s *** ValidationStatus: %s", THISCLASS, vs);
								if (vs != null && !vs.isValid()) {
									Log.customer.debug("%s *** ResultCode: %s", THISCLASS, vs.getReasonCode());
									cfnErrors += 1;
									lineErrors += 1;
									cfnSplitMsg.append(vs.getReasonCode() + ". ");
									Log.customer.debug("CatCSVReqSubmitHook *** Split#: " +
													errorSplit + " Error: " + vs.getReasonCode());
								}
								if ( vs == null) {
									Log.customer.debug("%s *** VS Null - webservice down", THISCLASS);
									lineErrors += 1;
									cfnErrors += 1;
									cfnSplitMsg.append( CFNWebServiceDown + ". ");
									Log.customer.debug("CatCSVReqSubmitHook ***WEBSERVICE DOWN Split#: " +
													errorSplit + " Error: " + CFNWebServiceDown);

								}
							}
							//end validation of contract file number
						}
						catch (Exception e) {
							Log.customer.debug("%s *** Exception: %s", THISCLASS, e);
						}
					}
				}
				if (sa != null && testvalue.equalsIgnoreCase(userAcctFacility)) {
					int error = CatValidateCSVAccounting.validateAccounting(sa);
					if (error > 0) {
						String errorMsg = CatValidateCSVAccounting.getValidationMessage(error);
						splitErrors += 1;
						lineErrors += 1;
						splitMsg.append(errorMsg + ". ");
					}
				}
				if (splitErrors > 0) {
					String splitErrorResult = null;
					String formatSplitError = null;
// Make "if (splits.size() > 1)" to leave out Acct Distribution split # in message
					if (splits.size() > 0) {
						splitErrorResult = MultiSplitError + splitMsg.toString();
						formatSplitError = Fmt.S(splitErrorResult, errorSplit);
					} else {
						formatSplitError = SingleSplitError + splitMsg.toString();
					}
					lineMsg.append(formatSplitError);
      			}
      			//build contract filenumber error msg
      			if (cfnErrors > 0) {
      				String cfnErrorResult = null;
      				String formatcfnSplitError = null;
      				if (splits.size() > 1) {
      					cfnErrorResult = MultiSplitCFNError + cfnSplitMsg.toString();
      					formatcfnSplitError = Fmt.S(cfnErrorResult, errorSplit);
      				} else {
      					formatcfnSplitError = SingleSplitCFNError + cfnSplitMsg.toString();
      				}
      				lineMsg.append(formatcfnSplitError);
				}
			}
		}
		Log.customer.debug("CatCSVReqSubmitHook *** LineErrors: " + lineErrors);
		if (lineErrors > 0) {
			lineErrorResult = " Line %s:" + lineMsg.toString();
			Log.customer.debug("%s *** Line Error Msg: %s", THISCLASS, lineErrorResult);
		}
		return lineErrorResult;
	}

	protected HashMap getPropertyMap(Requisition req, ReqLineItem reqline)
	{
		HashMap map = new HashMap();
			map.put("ProcureLineItemCollection",req);
			map.put("ProcureLineItem",reqline);
			map.put("NotPastDate","true");
			map.put("AllowNullDate","true");
		return map;
	}

	protected String checkNeedByDate (ReqLineItem rli) {

		String lineErrorResult = "0";
		LineItemProductDescription lipd = rli.getDescription();
		if (lipd != null) {
			int leadtime = lipd.getLeadTime();
			if (leadtime > 0) {
				Date expected = Date.getNow();
				Date.addDays(expected, leadtime);
				Log.customer.debug("%s *** Expected: %s", THISCLASS, expected);
				Date wanted = rli.getNeedBy();
				Log.customer.debug("%s *** Wanted: %s", THISCLASS, wanted);
				if (wanted != null && !wanted.after(expected)) {
					lineErrorResult = Fmt.S(NeedByError, String.valueOf(leadtime));
				}
			}
		}
		return lineErrorResult;
	}

	protected String checkAccountingLocal(ReqLineItem rli) {

		String lineErrorResult = "0";
		int lineErrors = 0;
		FastStringBuffer lineMsg = new FastStringBuffer();
		SplitAccountingCollection sac = rli.getAccountings();
		if (sac != null) {
			BaseVector splits = sac.getSplitAccountings();
			for (int j = 0; j < splits.size(); j++) {
				int splitErrors = 0;
				int errorSplit = j + 1;
				FastStringBuffer splitMsg = new FastStringBuffer();
				SplitAccounting sa = (SplitAccounting)splits.get(j);
				if (sa != null) {
					CatAccountingCollector cac = CatValidateAccountingString.getCatAccounting(sa);
					if (cac != null) {
						CatAccountingValidator response = new CatAccountingValidator();
						if (!cac.getFacility().equals("22")) {
							response.setValidationCode("00");
						} else {
							response.setValidationCode("something else");
							response.setValidationMessage("Houston, we got trouble!");
						}
						if (!response.getResultCode().equals("00")) {
							splitErrors += 1;
							lineErrors += 1;
							splitMsg.append(response.getMessage());
							Log.customer.debug("CatCSVReqSubmitHook *** Split#: " +
									errorSplit + " Error: " + response.getMessage());
						}
						//	*R4 Add* - Reset line Acctng Validation Message to null if no acctng error
						else {
							Log.customer.debug("%s *** Accounting is valid, reset message to NULL!", THISCLASS);
							sa.setFieldValue("ValidateAccountingMessage",null);
						}
					}
				}
				if (splitErrors > 0) {
					String splitErrorResult = null;
					String formatSplitError = null;
					if (splits.size() > 1) {
						splitErrorResult = MultiSplitError + splitMsg.toString();
						formatSplitError = Fmt.S(splitErrorResult, errorSplit);
					} else {
						formatSplitError = SingleSplitError + splitMsg.toString();
					}
					lineMsg.append(formatSplitError);
				}
			}
		}
		Log.customer.debug("CatCSVReqSubmitHook *** LineErrors: " + lineErrors);
		if (lineErrors > 0) {
			lineErrorResult = "  Line %s:" + lineMsg.toString();
			Log.customer.debug("%s *** Line Error Msg: %s", THISCLASS, lineErrorResult);
		}
		return lineErrorResult;
	}

	 // 03.10.06 (KS)#375/400 - new method to resync AC lines w/Mat line (since removed reordering)

	 public static void resyncAddChargeLines (BaseVector lines) {

		 int size = lines.size();
		 if (size > 1) { // no need to proceed unless multiple lines

			 // first determine if AC lines exist (only proceed when required)
			 boolean hasAddCharges = false;
			 for (int i=size-1;i>-1;i--) {
				 ReqLineItem rli = (ReqLineItem)lines.get(i);
				 Integer refNum = (Integer)rli.getFieldValue("ReferenceLineNumber");
				 if (refNum != null && refNum.intValue() != rli.getNumberInCollection()) {
					 hasAddCharges = true;
					 Log.customer.debug("%s *** Found AC, must resync!", THISCLASS);
					 break;
				 }
			 }
			 if (hasAddCharges) { // only proceed if AC lines exist on Req
				 for (int j=0;j<size;j++) {
					 ReqLineItem rli = (ReqLineItem)lines.get(j);
					 Integer refNum = (Integer)rli.getFieldValue("ReferenceLineNumber");
					 int nic = rli.getNumberInCollection();
					 if (refNum != null && refNum.intValue() == nic) { // material line
						 for (int k=size-1;k>-1;k--){ // update associated ACs
							 ReqLineItem rli2 = (ReqLineItem)lines.get(k);
							 if (rli2 != rli) {
								 refNum = (Integer)rli2.getFieldValue("ReferenceLineNumber");
								 if (refNum != null && refNum.intValue() == nic) { //found AC line
									 CatSetAdditionalChargeLineItemFields.setAdditionalChargeFields(rli, rli2);
								 }
							 }
						 }
					 }
				 }
			 }
		 }
	 }
//Check for InActive Approver
    public static boolean checkforactiveDA(BaseVector lines, Partition partition){
		for(int i=0;i<lines.size();i++)
		{
		  Log.customer.debug("CatCSVReqSubmitHook***Loop for getting the Requisition LineItem");
		  ReqLineItem rli1 = (ReqLineItem)lines.get(i);
		       if( rli1!=null){
		           SplitAccountingCollection sac = rli1.getAccountings();
		              if (sac != null){
		                  Log.customer.debug("CatCSVReqSubmitHook*** SplitAccountingCollection"+sac);
		                  BaseVector splits = sac.getSplitAccountings();
		                  SplitAccounting sa = (SplitAccounting)splits.get(0);
		                       if(sa!=null){
		                          Log.customer.debug("CatCSVReqSubmitHook*** SplitAccounting"+sa);
		                           ariba.user.core.User approver = (ariba.user.core.User)sa.getFieldValue("DepartmentApprover");
		                        if (approver != null){
		                            ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(approver,partition);
		                            Log.customer.debug("CatCSVReqSubmitHook***Common.Core.UserDA"+partuser);
                                    Log.customer.debug(partuser.getFieldValue("Active"));
                                    Log.customer.debug(partuser);
                                if (partuser.getFieldValue("Active").toString().equalsIgnoreCase("false")){
		                           Log.customer.debug("CatCSVReqSubmitHook***Designated Approver is not active"+partuser);
		                           return false;
		                           }

		                      /*else{

		                          Log.customer.debug("CatCSVReqSubmitHook***Designated approver Active"+partuser);
		                           return true;
                                }*/
			                 }
		                   }
		                }
		             }
		         }
    	  return true;
}
//Check for Preparer as Designated Approver
 public static boolean checkforPreparerasDA(BaseVector lines, Partition partition){
  for(int i=0;i<lines.size();i++)
  {
   Log.customer.debug("CatCSVReqSubmitHook***Loop for getting the Requisition LineItem");
   ReqLineItem rli1 = (ReqLineItem)lines.get(i);

              try{
              if( rli1!=null){
					//ISSUE 831
				   String test=rli1.getFieldValue("IsHazmat").toString();
				   Log.customer.debug("*******req:*****:%s","CatCSVRequisitionSubmitHook",test);
				   Requisition req1 = (Requisition)rli1.getLineItemCollection();
				if(test.equals("true"))
							{
								Log.customer.debug("%s **********IFLOOP***********","CatCSVRequisitionSubmitHook");
								//req1.setFieldValue("FOBPoint",FOB_TEXT_HAZMAT);
								//String fob= (String)req1.getFieldValue("FOBPoint");
								String fob= (String)req1.getFieldValue("FOBPoint");
								req1.setFieldValue("FOBPoint",fob);
								Log.customer.debug("%s **********SETTING FOB*********** %s","CatCSVRequisitionSubmitHook",fob);
							}
							else
							{
								String fob1= (String)req1.getFieldValue("FOBPoint");
								req1.setFieldValue("FOBPoint",fob1);
								}

					//ISSUE 831
				  SplitAccountingCollection sac = rli1.getAccountings();
                  if (sac != null){
                     Log.customer.debug("CatCSVReqSubmitHook*** SplitAccountingCollection"+sac);
                     BaseVector splits = sac.getSplitAccountings();
                     SplitAccounting sa = (SplitAccounting)splits.get(0);
                     if(sa!=null){
                        LineItemCollection lic = sa.getLineItem().getLineItemCollection();
                        Log.customer.debug("CatCSVReqSubmitHook*** SplitAccounting"+sa);
                        ariba.user.core.User approver = (ariba.user.core.User)sa.getFieldValue("DepartmentApprover");
                        if(approver==lic.getPreparer()){
                        return false;
                       }
                  /*else {
                        Log.customer.debug("CatCSVReqSubmitHook*** Preparer is not DA");
                        return true;
                         }*/

                         }
                       }
                     }
                     }catch(Exception e ){
							Log.customer.debug("The error is",e);
						}
                  }
             return true;
           }


         protected String checkFARDFARAttachment(Requisition req)
          {

			  {
				              String errorString = "0";
				              String NoErrorString = "1";
				              boolean checkdept = false;
			  				 //Requisition req = (Requisition)commentObj.getFieldValue("Parent");
			  				// if (req != null){
			  				 Log.customer.debug("********Inside method checkFARDFARAttachment 2********** %s",req);

			  				 String dept = null;
			  				 String deptName1 = "j0561";

			  				 BaseVector rliList = (BaseVector)req.getFieldValue("LineItems");
			  				 Log.customer.debug("CatSetFARDFARAttachmentfromDept ********Setting External Comment Field********** %s",rliList);
			  				 if(!rliList.isEmpty()){
			  					 Log.customer.debug("CatSetFARDFARAttachmentfromDept ********inside if ********** %s",rliList);
			  				 for (Iterator itr = rliList.iterator() ; itr.hasNext() ;)  {
			  					 Log.customer.debug("CatSetFARDFARAttachmentfromDept ********inside for********** ");
			  				 ReqLineItem rli = (ReqLineItem)itr.next();
			  				 Log.customer.debug("CatSetFARDFARAttachmentfromDept ********ReqLineItem********** %s",rli);
			  				 SplitAccountingCollection sac = rli.getAccountings();
			  				 if (sac != null) {
			  					Log.customer.debug("CatSetFARDFARAttachmentfromDept ********inside sac********** %s",sac);
			  					BaseVector splits = sac.getSplitAccountings();
			  					Log.customer.debug("CatSetFARDFARAttachmentfromDept ********inside sa vector********** %s",splits);
			  					if (!splits.isEmpty()) {
			  						for (Iterator itr1 = splits.iterator() ; itr1.hasNext() ;)  {
			  							SplitAccounting sa = (SplitAccounting)itr1.next();
			  							Log.customer.debug("CatSetFARDFARAttachmentfromDept ********inside sa vector********** %s",sa);
			  							dept = (String)sa.getDottedFieldValue("Department");
			  							Log.customer.debug("********Department**********%s",dept);
			  							if(dept != null){
			  							if((dept == dept1) || (dept.equalsIgnoreCase(dept1)) || (dept == dept2) || (dept.equalsIgnoreCase(dept2)) || (dept == dept3) || (dept.equalsIgnoreCase(dept3)))
							           {
			  								Log.customer.debug("********Setting External Comment Field**********%s",dept);
			  								checkdept = true;
			  								//req.setDottedFieldValue("Comments.ExternalComment",true);

			  							}
			  						}

			  					}

			  				 }
			  			 }
			  		 }
			  	 }


			  	 boolean hasAttachment = false;
			  	 //boolean externalComment = false;
			  	BaseVector comments = req.getComments();
				 Boolean FDFARAttachmentIndicator  = (Boolean)req.getFieldValue("FDFARAttachmentIndicator");
				  Log.customer.debug("@@@@ Setting External Comment Fields @@@@");

						int cSize = comments.size();
						for (int i=0;i<cSize;i++) {
							BaseVector attchs = ((Comment)comments.get(i)).getAttachments();
							Comment commtts = (Comment)comments.get(i);
							/*Boolean extncmmts =  (Boolean)commtts.getFieldValue("ExternalComment");
							if(extncmmts != null)
							{
								if(extncmmts.booleanValue())
									externalComment = true;
							}
							*/
							if(checkdept && FDFARAttachmentIndicator.booleanValue()) {
								Log.customer.debug("@@@@ Inside checkdept && FDFARAttachmentIndicator.booleanValue @@@@");

								commtts.setFieldValue("ExternalComment",Boolean.TRUE);

						    }




							if (!attchs.isEmpty()) {
	  	      	            hasAttachment = true;
					      }
					  }

			     if(checkdept && FDFARAttachmentIndicator.booleanValue())
			       {
			        if(hasAttachment)
			           return NoErrorString;
			        else
			           return errorString;
			        }
                 else
                     return NoErrorString;
 //}

		  }
	  }


   // Check isHazMat and set FOB point

  /* public static void setFOBPoint(List reqli)
   {

		 Log.customer.debug("**********SET FOB POINT***********");
		 int size1 = reqli.size();
		 if (size1 > 1) { // no need to proceed unless multiple lines

			//boolean IsHazmat = false;
			// for (int m=size1-1;m>-1;m--) {
				for (int i=0;i<reqli.size();i++){
					//if (reqli != null){
						 ReqLineItem LI = (ReqLineItem)reqli.get(i);
                 		 Log.customer.debug("**********if looooooop***********");
                  		 //boolean IsHazmat = BooleanFormatter.getBooleanValue(LI.getFieldValue("IsHazmat"));
                  		 String test=LI.getFieldValue("IsHazmat").toString();
                    	 Requisition req1 = (Requisition)LI.getLineItemCollection();
                   		Log.customer.debug("*******req:*****:%s","CatCSVRequisitionSubmitHook",test);
                 					if(test.equals("true"))
											{
												Log.customer.debug("%s **********IFLOOP***********","CatCSVRequisitionSubmitHook");
												req1.setFieldValue("FOBPoint",FOB_TEXT_HAZMAT);
												String fob= (String)req1.getFieldValue("FOBPoint");
												Log.customer.debug("%s **********SETTING FOB*********** %s","CatCSVRequisitionSubmitHook",fob);
												return;
											}
											else
											{
												req1.setFieldValue("FOBPoint",FOB_TEXT);
												}
									//req1.setFieldValue("FOBPoint",FOB_TEXT);
							//}
                                }

                        }
                        Log.customer.debug("%s **** Req is NOT Hazmat!!", "CatHazmatCheckAction");

        }


	 // 03.10.06 (KS)#375/400 - pull out all reordering code due to issues with History corruption, etc.

/*	 public static void reorderLineItemsOnRevision(BaseVector lines, BaseVector comments, boolean updateACLines,
			 int position, int deletes) {

		 Integer refNumInt = null;
		 if (lines != null && !lines.isEmpty()) {
			 List allLines = ListUtil.collectionToList(lines);
			 int size = allLines.size();
			 Log.customer.debug("CatCSVReqSubmitHook *** 0) allLines List size: " + size);
			 ArrayList matLines = new ArrayList(size-position);
			 ArrayList acLines = new ArrayList(size-position);
			 ArrayList skipLines = new ArrayList(size-position);
			 int safety = 0;
			 for (int i=position;i<size;i++) {
				 ProcureLineItem pli = (ProcureLineItem)lines.get(i);
				 refNumInt = (Integer)pli.getFieldValue("ReferenceLineNumber");
				 int nic = pli.getNumberInCollection();
				 Log.customer.debug("CatCSVReqSubmitHook *** 1) NIC/RefNum): " + nic + refNumInt);
				 if (refNumInt != null && refNumInt.intValue() == nic) {
					 matLines.add(pli);
				 }
				 else if (refNumInt != null && refNumInt.intValue() > position) {
					 acLines.add(pli);
				 }
				 else {
					 // skip AC lines referencing pre-revision material lines (these should be at top of additions)
					 Log.customer.debug("CatCSVReqSubmitHook *** SKIP (references pre-Revision line)! ");
					 skipLines.add(pli);
				 }
			 }
			 int matSize = matLines.size();
			 int acSize = acLines.size();
			 int skipSize = skipLines.size();
			 Log.customer.debug("CatCSVReqSubmitHook *** 2) New Lines Check (Matl/AC/Skips): " + matSize + "/"
					 + acSize + "/" + skipSize);

			 if (matSize > 0 && acSize > 0) {
				 List orderedLines = new ArrayList();

				 // first add all the skips at top (those that reference other lines
				 Log.customer.debug("CatCSVReqSubmitHook *** adding Skip Line!");
				 orderedLines.addAll(skipLines);

				// second add material lines one at a time
				 int counter = 0;
				 for (int j=0;j<matSize;j++) {
					int a_counter = 0;
					ProcureLineItem mLine = (ProcureLineItem)matLines.get(j);
					int nic = mLine.getNumberInCollection();
					Log.customer.debug("CatCSVReqSubmitHook *** adding Material Line#: " + nic);
					orderedLines.add(mLine);
					counter++;

					// third add all AC lines for each material line
					for (int k=0;k<acSize;k++) {
						ProcureLineItem acLine = (ProcureLineItem)acLines.get(k);
						if (acLine != null) {
							refNumInt = (Integer)acLine.getFieldValue("ReferenceLineNumber");
							if (refNumInt != null && refNumInt.intValue() == nic) {
								Log.customer.debug("CatCSVReqSubmitHook *** adding AC Line#: " + acLine.getNumberInCollection());
								acLine.setFieldValue("ReferenceLineNumber",new Integer(position+deletes+skipSize+counter));
								if (updateACLines) {
									Log.customer.debug("%s *** Setting AdditionalChargeFields()!",THISCLASS);
									CatSetAdditionalChargeLineItemFields.setAdditionalChargeFields(mLine, acLine);
								}
								orderedLines.add(acLine);
								acLines.remove(acLine);
								acLines.add(k,null);
								a_counter++;
							}
						}
					}
					counter += a_counter;
				 }
				 Log.customer.debug("CatCSVReqSubmitHook *** orderedLines size (after reorder): " + orderedLines.size());

				 // fourth (04.07.06 - added change to relink commments for new line items)
				  *
   *****  NOTE ***** if turn this V2+ reordering back on must: ******************************************
					 1) update Comments Line BaseIds
					 2) switch to use new BV for reordered additions before reinserting in full list to
					 ensure lines do not get duplicated (vs. swapped for new reordered lines)
   *******************************************************************************************************

				 // fifth replace orginal additions with reordered additions
				 lines.removeRange(position,size);
				 Log.customer.debug("CatCSVSubmitHook *** lines size (after removal): " + lines.size());
				 lines.addAll(position,orderedLines);
				 Log.customer.debug("CatCSVSubmitHook *** lines size (after remerge): " + lines.size());


				 // six update line NICs (not done automatically by Ariba due to lines remove/add)
				 for (;position<lines.size();position++) {
					 ProcureLineItem pli = (ProcureLineItem)lines.get(position);
					 pli.setNumberInCollection(position+deletes+1);
				 }
			 }
		 }
	 }
*/

}