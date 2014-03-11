/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/10/2006
	Description: 	Submit Hook initial implementation
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  11/08/2006
	Description: 	Additional logic for eRFQ submission.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  01/12/2007
	Description: 	Added logic to skip validation for sa where AccountType is
					Other and the Order Number is populated
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  01/24/2007
	Description: 	Modified the error messages to be returned as localized
					strings based on the user locale
******************************************************************************/

package config.java.hook.vcsv3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.ConditionResult;
import ariba.basic.core.LocaleID;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.condition.NeedByDate;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.OrgControlKey;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.CatValidateAccountingString;
import config.java.common.CATFS7200;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;


public class CatEZORequisitionSubmitHook implements ApprovableHook {

	private static final String ClassName = "CatEZORequisitionSubmitHook";
	private static final int ErrorCode = -1;
	private static final int NoErrorCode = 0;
	private static final List NoError = ListUtil.list(Constants.getInteger(0));
	private static final String ErrorInActiveApprover=ResourceService.getString("cat.vcsv1","ErrorInActiveApprover");
	private static final String ErrorApprover_PreparerCannotApproveRequestionAsDA= ResourceService.getString("cat.vcsv1","ErrorApprover_PreparerCannotApproveRequestionAsDA");

	// Commented out so as to initialize in the method as localized strings
	// private static final String SingleSplitError = Fmt.Sil("cat.vcsv3","AccountDistributionError_Single");
	// private static final String MultiSplitError = Fmt.Sil("cat.vcsv3","AccountDistributionError_Multiple");
	// private static final String CatalogItemsError = Fmt.Sil("cat.vcsv3","Error_RFQHasCatalogItems");
	// private static final String SuppSelectedError = Fmt.Sil("cat.vcsv3","Error_RFQHasSuppSelected");
	// private static final String NeedByFlag = Fmt.Sil("cat.vcsv3","NeedByFlag");
	// private static final String NeedByError = Fmt.Sil("cat.vcsv3","NeedByLeadTimeError");
	// private static final String ERFQ3rdPurchasingError = Fmt.Sil("cat.java.vcsv3","ERFQ3rdPurchasingError");
	// private static final String SupplierDetailsReq = Fmt.Sil("cat.java.vcsv3","SupplierDetailsReq");
	// private static final String InvalidAccountingMsg = Fmt.Sil("cat.java.vcsv3", "AccountDistributionNotValid");

	private static final String AccntsToSkipValidation = ResourceService.getString("cat.java.vcsv3", "AccountTypeToSkipOrderValidation");

	protected boolean isEdit = false;  // used since SubmitHook also called from CheckinHook

	public List run(Approvable approvable)
	{
		FastStringBuffer totalMsg = new FastStringBuffer ();
		boolean hasErrors = false;
		String error = "";

		if (approvable instanceof Requisition) {

			User sessionUser = (User) Base.getSession().getEffectiveUser();
			LocaleID userLocaleID = sessionUser.getLocaleID();
			String userLanguage = userLocaleID.getLanguage();
			Locale userLocale = null;
			if (!StringUtil.nullOrEmptyOrBlankString(userLanguage)) {
				userLocale = new Locale(userLanguage);
			}
			else {
				userLocale = Locale.US;
			}

			String CatalogItemsError = Fmt.Sil(userLocale,"cat.vcsv3","Error_RFQHasCatalogItems");
			if (StringUtil.nullOrEmptyOrBlankString(CatalogItemsError))
				CatalogItemsError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String SuppSelectedError = Fmt.Sil(userLocale,"cat.vcsv3","Error_RFQHasSuppSelected");
			if (StringUtil.nullOrEmptyOrBlankString(SuppSelectedError))
				SuppSelectedError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String NeedByFlag = Fmt.Sil(userLocale,"cat.vcsv3","NeedByFlag");
			if (StringUtil.nullOrEmptyOrBlankString(NeedByFlag))
				NeedByFlag = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String NeedByError = Fmt.Sil(userLocale,"cat.vcsv3","NeedByLeadTimeError");
			if (StringUtil.nullOrEmptyOrBlankString(NeedByError))
				NeedByError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String ERFQ3rdPurchasingError = Fmt.Sil(userLocale,"cat.java.vcsv3","ERFQ3rdPurchasingError");
			if (StringUtil.nullOrEmptyOrBlankString(ERFQ3rdPurchasingError))
				ERFQ3rdPurchasingError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			String SupplierDetailsReq = Fmt.Sil(userLocale,"cat.java.vcsv3","SupplierDetailsReq");
			if (StringUtil.nullOrEmptyOrBlankString(SupplierDetailsReq))
				SupplierDetailsReq = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

			Requisition r = (Requisition)approvable;
			Partition partition=r.getPartition();
			Boolean iSeRFQB = (Boolean) r.getDottedFieldValue("ISeRFQ");
			boolean iSeRFQ = false;
			if (iSeRFQB != null) {
				iSeRFQ = iSeRFQB.booleanValue();
			}

			BaseVector lines = r.getLineItems();

			boolean isPurchasing = false;
			int reqLinesSize = 0;
			User currUser = (User) Base.getSession().getEffectiveUser();
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Current User: %s", ClassName, currUser);
			if (currUser != null)
				isPurchasing = currUser.hasPermission("CatPurchasing");
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Current User isPurchasing: " + isPurchasing, ClassName);

			if (iSeRFQ && isPurchasing) {
				// This logic should run if a purchasing user is creating the eRFQ
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Setting eRFQBuyer to: %s", ClassName, currUser);
				r.setDottedFieldValue("eRFQBuyer",currUser);
			}

			if (lines != null) {
				reqLinesSize = lines.size();
			}
			if (iSeRFQ && isPurchasing && (reqLinesSize == 0)) {
				return ListUtil.list(Constants.getInteger(ErrorCode), ERFQ3rdPurchasingError);
			}

			for (int i = 0; i < lines.size(); i++) {
				int errorLine = i + 1;
				boolean hasBadAcctng = false;
				boolean hasBadNeedBy = false;
				boolean hasCatalogItems = false;
				boolean hasSuppSelected = false;
				ReqLineItem rli = (ReqLineItem)lines.get(i);

				// Test 1 - VALIDATE ACCOUNTING
 				if (!iSeRFQ) {
					// Live Call
					// TODO: Remove the commented live call for Production
					String lineresult = checkAccounting(rli, userLocale);
					// Substitute Call
					// String lineresult = checkAccountingLocal(rli, userLocale);
					if (!lineresult.equals("0")) {
						String formatLineError = Fmt.S(lineresult, errorLine);
						hasErrors = true;
						hasBadAcctng = true;
						totalMsg.append(formatLineError);
						Log.customer.debug("%s ::: Line Error Msg: %s", ClassName, formatLineError);
					}
				}

				// Test 2 - Ariba's OOB Need by check in Submit
				if (!iSeRFQ) {
					if (r.getPreviousVersion() == null) {
						Date needby = rli.getNeedBy();
						NeedByDate nbd = new NeedByDate();
						PropertyTable ptable = new PropertyTable(getPropertyMap(r, rli));
						Log.customer.debug("%s ::: ptable: %s", ClassName, ptable);
						ConditionResult cr = nbd.evaluateAndExplain(needby, ptable);
						Log.customer.debug("%s ::: CR: %s", ClassName, cr);
						if (cr != null) {
							hasErrors = true;
							String crmsg = null;
							if (cr.getWarningCount() > 0)
								crmsg = cr.getFirstWarning();
							if (cr.getErrorCount() > 0) {
								crmsg = cr.getFirstError();
							}
							Log.customer.debug("%s ::: CR Message: %s", ClassName, crmsg);
							if (crmsg != null) {
								hasBadNeedBy = true;
								crmsg = NeedByFlag + crmsg;
								if (!hasBadAcctng)
									crmsg = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + crmsg;
								totalMsg.append(crmsg);
							}
						}
					}
				}

				// Test for eRFQ Requisitions
				if (iSeRFQ) {
					String errMsg = null;
					// Test 1 - Test if eRFQ has any Catalog Items
					if (!rli.getIsAdHoc()) {
						hasCatalogItems = true;
					}
					// Test 1 - Test if eRFQ has any items with supplier/location specified
					if (rli.getSupplier() != null || rli.getSupplierLocation() != null) {
						hasSuppSelected = true;
					}

					if (hasCatalogItems){
						hasErrors = true;
						errMsg = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + CatalogItemsError;
					}
					else if (hasSuppSelected) {
						hasErrors = true;
						errMsg = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + SuppSelectedError;
					}
					totalMsg.append(errMsg);
				}

				if (!iSeRFQ && isEdit) {
					if (rli.getSupplier() == null || rli.getSupplierLocation() == null) {
						hasErrors = true;
						totalMsg.append(Fmt.S(" Line %s: ", String.valueOf(errorLine)) + SupplierDetailsReq);
					}
				}
				Log.customer.debug("%s ::: Finished Line# " + errorLine, ClassName);
			}
			Log.customer.debug("%s ::: Finished ALL lines and hasErrors is: " + hasErrors, ClassName);
			if (hasErrors) {
				Log.customer.debug("%s ::: Total Error Msg: %s", ClassName, totalMsg.toString());
				return ListUtil.list(Constants.getInteger(ErrorCode), totalMsg.toString());
			}
			if (!checkforactiveDA(lines,partition))
			    return ListUtil.list(Constants.getInteger(-2), ErrorInActiveApprover);
			if (!checkforPreparerasDA(lines,partition))
                return ListUtil.list(Constants.getInteger(-2), ErrorApprover_PreparerCannotApproveRequestionAsDA);
		}
		return NoError;
	}

	public CatEZORequisitionSubmitHook() {
		super();
	}

	protected String checkAccounting(ReqLineItem rli, Locale userLocale) {
		String lineErrorResult = "0";
		int lineErrors = 0;

		CATFS7200 catfs7200 = new CATFS7200();
		Response response = null;
		OrgControlKey retOCK = null;
		AccountingDistributionKey retADK = null;
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		StringBuffer errorMsg = null;
		String accountType = "";

		String InvalidAccountingMsg = Fmt.Sil(userLocale,"cat.java.vcsv3", "AccountDistributionNotValid");
		if (StringUtil.nullOrEmptyOrBlankString(InvalidAccountingMsg))
			InvalidAccountingMsg = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String SingleSplitError = Fmt.Sil(userLocale,"cat.vcsv3","AccountDistributionError_Single");
		if (StringUtil.nullOrEmptyOrBlankString(SingleSplitError))
			SingleSplitError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String MultiSplitError = Fmt.Sil(userLocale,"cat.vcsv3","AccountDistributionError_Multiple");
		if (StringUtil.nullOrEmptyOrBlankString(MultiSplitError))
			MultiSplitError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		ClusterRoot accntType = (ClusterRoot) rli.getFieldValue("AccountType");
		if (accntType != null) {
			accountType = accntType.getUniqueName();
		}

		FastStringBuffer lineMsg = new FastStringBuffer();
		SplitAccountingCollection sac = rli.getAccountings();
		if (sac != null) {
			BaseVector splits = sac.getSplitAccountings();
			for (int j = 0; j < splits.size(); j++) {
				int splitErrors = 0;
				int cfnErrors = 0;
				int errorSplit = j + 1;
				FastStringBuffer splitMsg = new FastStringBuffer();
				FastStringBuffer cfnSplitMsg = new FastStringBuffer();
				SplitAccounting sa = (SplitAccounting) splits.get(j);
				if (sa != null) {
					if (!shouldSkipValidation(accountType, sa)) {
						response = null;
						retOCK = null;
						retADK = null;
						sbrtnRtCode = null;
						sbrtnMessage = null;
						errorMsg = null;

						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Calling the live function 09 local call", ClassName);
						// Live Call
						response = catfs7200.getResp0309(getParamObj(sa, "09"), getAccntDistKeyObj(sa));
						// Simulated Call
						// response = getResp0309Local(getParamObj(sa, "09"), getAccntDistKeyObj(sa));

						/*
						 * AUL, sdey - comment out this code as lab has no connectivity.
						 */
						sbrtnRtCode = "00";

						/*
						retADK = response.getAccountingDistributionKey();
						sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
						sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
						*/

						/*
						 * AUL, sdey - comment out this code as lab has no connectivity.
						 */

						//if (Log.customer.debugOn) {

						/* AUL, sdey - comment out this code as lab has no connectivity.
							Log.customer.debug("\n\n\n");
							Log.customer.debug("%s ::: ADK 09 Response Object", ClassName);
							Log.customer.debug("%s ::: getMsgText : %s", ClassName, response.getMessage().getMsgText());
							Log.customer.debug("%s ::: getDb2SQLSubroutineReturnCode : %s", ClassName, response.getMessage().getDb2SQLSubroutineReturnCode());
							Log.customer.debug("%s ::: getSubroutineReturnCode : %s", ClassName, response.getMessage().getSubroutineReturnCode());
							Log.customer.debug("%s ::: getSubroutineReturnMessage : %s", ClassName, response.getMessage().getSubroutineReturnMessage());
							Log.customer.debug("%s ::: getAccountingDistributionQualifier : %s", ClassName, retADK.getAccountingDistributionQualifier());
							Log.customer.debug("%s ::: getAccountingNumberFacilityCode : %s", ClassName, retADK.getAccountingNumberFacilityCode());
							Log.customer.debug("%s ::: getAccountingOrderType : %s", ClassName, retADK.getAccountingOrderType());
							Log.customer.debug("%s ::: getControlAccountNumber : %s", ClassName, retADK.getControlAccountNumber());
							Log.customer.debug("%s ::: getExpenseAccountNumber : %s", ClassName, retADK.getExpenseAccountNumber());
							Log.customer.debug("%s ::: getSubAccount : %s", ClassName, retADK.getSubAccount());
							Log.customer.debug("%s ::: getSubSubAccount : %s", ClassName, retADK.getSubSubAccount());
							Log.customer.debug("\n\n\n");

						AUL, sdey - comment out this code as lab has no connectivity.
						*/

						//}

						if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") == 0)) {
							//This means it is good accounting combination
							//if (Log.customer.debugOn)
								Log.customer.debug("%s ::: Valid Accounting Combination !!!!!", ClassName);
							Log.customer.debug("%s ::: Accounting is valid, reset message to NULL!", ClassName);
							sa.setFieldValue("ValidateAccountingMessage", null);
						}
						else {
							if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
								//Stop! Critical Error
								Log.customer.debug("%s ::: Stop! Critical Error returned from Function 09 !!!!!", ClassName);
								if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
									errorMsg = new StringBuffer(InvalidAccountingMsg);
								}
								else {
									errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnMessage + ".");
								}
								splitErrors += 1;
								lineErrors += 1;
								splitMsg.append(errorMsg);
								Log.customer.debug("%s ::: Split#: " + errorSplit + " Error: " + errorMsg, ClassName);
							}
							else {
								// Otherwise, Error occured on the validation
								Log.customer.debug("%s ::: Error returned from Function 09 !!!!!", ClassName);
								if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
									errorMsg = new StringBuffer(InvalidAccountingMsg);
								}
								else {
									errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnMessage + ".");
								}
								splitErrors += 1;
								lineErrors += 1;
								splitMsg.append(errorMsg);
								Log.customer.debug("%s ::: Split#: " + errorSplit + " Error: " + errorMsg, ClassName);
							}
						}
					}
					else {
						//if (Log.customer.debugOn)
							Log.customer.debug("%s ::: Account Type should be skipped for validation", ClassName);
						sa.setFieldValue("ValidateAccountingMessage", null);
					}
				}
				if (splitErrors > 0) {
					String splitErrorResult = null;
					String formatSplitError = null;
					if (splits.size() > 0) {
						splitErrorResult = MultiSplitError + splitMsg.toString();
						formatSplitError = Fmt.S(splitErrorResult, errorSplit);
					}
					else {
						formatSplitError = SingleSplitError + splitMsg.toString();
					}
					lineMsg.append(formatSplitError);
				}
			}
		}
		Log.customer.debug("%s ::: LineErrors: " + lineErrors, ClassName);
		if (lineErrors > 0) {
			lineErrorResult = " Line %s:" + lineMsg.toString();
			Log.customer.debug("%s ::: Line Error Msg: %s", ClassName, lineErrorResult);
		}
		return lineErrorResult;
	}

	public boolean shouldSkipValidation(String accountType, SplitAccounting sa) {
		String order = (String) sa.getFieldValue("Order");
		boolean shouldSkip = false;

		String [] types = StringUtil.delimitedStringToArray(AccntsToSkipValidation,',');
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: type array: %s", ClassName, types);

		if (types != null) {
			int i = types.length;
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: type array length: " + i, ClassName);
			while (i-1 >= 0){
				String testAccntType = types[i-1];
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: testAccntType: %s", ClassName, testAccntType);
				if ((!StringUtil.nullOrEmptyOrBlankString(accountType))
				&& (accountType.equals(testAccntType))
				&& (!StringUtil.nullOrEmptyOrBlankString(order))) {
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: Account Type should be skipped: %s/%s", ClassName, testAccntType, accountType);
					shouldSkip = true;
				}
				i--;
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Returning shouldSkip: " + shouldSkip, ClassName);
		return shouldSkip;
	}

	protected HashMap getPropertyMap(Requisition req, ReqLineItem reqline) {
		HashMap map = new HashMap();
		map.put("ProcureLineItemCollection", req);
		map.put("ProcureLineItem", reqline);
		map.put("NotPastDate", "true");
		map.put("AllowNullDate", "true");
		return map;
	}

	protected String checkNeedByDate(ReqLineItem rli, Locale userLocale) {
		String NeedByError = Fmt.Sil(userLocale,"cat.vcsv3","NeedByLeadTimeError");
		if (StringUtil.nullOrEmptyOrBlankString(NeedByError))
			NeedByError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String lineErrorResult = "0";
		LineItemProductDescription lipd = rli.getDescription();
		if (lipd != null) {
			int leadtime = lipd.getLeadTime();
			if (leadtime > 0) {
				Date expected = Date.getNow();
				Date.addDays(expected, leadtime);
				Log.customer.debug("%s ::: Expected: %s", ClassName, expected);
				Date wanted = rli.getNeedBy();
				Log.customer.debug("%s ::: Wanted: %s", ClassName, wanted);
				if (wanted != null && !wanted.after(expected)) {
					lineErrorResult = Fmt.S(NeedByError, String.valueOf(leadtime));
				}
			}
		}
		return lineErrorResult;
	}

	protected String checkAccountingLocal(ReqLineItem rli, Locale userLocale) {
		String lineErrorResult = "0";
		int lineErrors = 0;

		String SingleSplitError = Fmt.Sil(userLocale,"cat.vcsv3","AccountDistributionError_Single");
		if (StringUtil.nullOrEmptyOrBlankString(SingleSplitError))
			SingleSplitError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String MultiSplitError = Fmt.Sil(userLocale,"cat.vcsv3","AccountDistributionError_Multiple");
		if (StringUtil.nullOrEmptyOrBlankString(MultiSplitError))
			MultiSplitError = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		FastStringBuffer lineMsg = new FastStringBuffer();
		SplitAccountingCollection sac = rli.getAccountings();
		if (sac != null) {
			BaseVector splits = sac.getSplitAccountings();
			for (int j = 0; j < splits.size(); j++) {
				int splitErrors = 0;
				int errorSplit = j + 1;
				FastStringBuffer splitMsg = new FastStringBuffer();
				SplitAccounting sa = (SplitAccounting) splits.get(j);
				if (sa != null) {
					CatAccountingCollector cac = CatValidateAccountingString.getCatAccounting(sa);
					if (cac != null) {
						CatAccountingValidator response = new CatAccountingValidator();
						Log.customer.debug("%s ::: The accounting facility is: ", ClassName, cac.getFacility());
						if (cac.getFacility().equals("36") && cac.getDepartment().equals("J0000")) {
							response.setValidationCode("99");
							response.setValidationMessage("Invalid Accounting for testing failure!");
						}
						else {
							response.setValidationCode("00");
						}
						if (!response.getResultCode().equals("00")) {
							splitErrors += 1;
							lineErrors += 1;
							splitMsg.append(response.getMessage());
							Log.customer.debug("%s ::: Split#: " + errorSplit + " Error: " + response.getMessage(), ClassName);
						}
						else {
							Log.customer.debug("%s ::: Accounting is valid, reset message to NULL!", ClassName);
							sa.setFieldValue("ValidateAccountingMessage", null);
						}
					}
				}
				if (splitErrors > 0) {
					String splitErrorResult = null;
					String formatSplitError = null;
					if (splits.size() > 1) {
						splitErrorResult = MultiSplitError + splitMsg.toString();
						formatSplitError = Fmt.S(splitErrorResult, errorSplit);
					}
					else {
						formatSplitError = SingleSplitError + splitMsg.toString();
					}
					lineMsg.append(formatSplitError);
				}
			}
		}
		Log.customer.debug("%s ::: LineErrors: " + lineErrors, ClassName);
		if (lineErrors > 0) {
			lineErrorResult = "  Line %s:" + lineMsg.toString();
			Log.customer.debug("%s ::: Line Error Msg: %s", ClassName, lineErrorResult);
		}
		return lineErrorResult;
	}

	public static Param getParamObj(SplitAccounting sa, String funcInd) {
		String fac = (String) sa.getFieldValue("AccountingFacility");
		String dept = (String) sa.getFieldValue("Department");
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");
		String exp = (String) sa.getFieldValue("ExpenseAccount");
		String order = (String) sa.getFieldValue("Order");
		String misc = (String) sa.getFieldValue("Misc");

		Param param = new Param();
		param.setFunctionIndicator(funcInd);
		return param;
	}

	public static AccountingDistributionKey getAccntDistKeyObj(SplitAccounting sa) {
		String fac = (String) sa.getFieldValue("AccountingFacility");
		String dept = (String) sa.getFieldValue("Department");
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");
		String exp = (String) sa.getFieldValue("ExpenseAccount");
		String order = (String) sa.getFieldValue("Order");
		String misc = (String) sa.getFieldValue("Misc");

		AccountingDistributionKey adk = new AccountingDistributionKey();
		adk.setAccountingDistributionQualifier(misc);
		adk.setAccountingNumberFacilityCode(fac);
		adk.setAccountingOrderNumber(order);
		adk.setControlAccountNumber(dept);
		adk.setExpenseAccountNumber(exp);
		adk.setSubAccount(div);
		adk.setSubSubAccount(sect);
		return adk;
	}
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
			       else{

			           Log.customer.debug("CatCSVReqSubmitHook***Designated approver Active"+partuser);
			           return true;
	                   }
				      }
			         }
			        }
			       }
			      }
    	  return true;
	  }
public static boolean checkforPreparerasDA(BaseVector lines, Partition partition){
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
     LineItemCollection lic = sa.getLineItem().getLineItemCollection();
     Log.customer.debug("CatCSVReqSubmitHook*** SplitAccounting"+sa);
     ariba.user.core.User approver = (ariba.user.core.User)sa.getFieldValue("DepartmentApprover");
     if(approver==lic.getPreparer()){
     return false;
    }
 else {
 Log.customer.debug("CatCSVReqSubmitHook*** Preparer is not DA");
 return true;
        }

       }
     }
    }
   }
    return true;
  }


}