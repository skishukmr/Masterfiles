/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/27/2006
	Description: 	Submit hook for MasterAgreement Request. Current checks are
					- Currency Consistency on all lines
					- Accounting Facility Consistency on all lines
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	11/05/2006
	Description: 	Added account validation logic.
					Changed the error check logic so as to run the for loop on
					the line items only once.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	11/29/2006
	Description: 	Added additional logic to check for the Enter Accounting
					boolean to identify if the FS7200 validation should be run.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	01/10/2007
	Description: 	Added logic to ensure the currency of min and max amount is
					checked.
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

-------------------------------------------------------------------------------
    Change Author: Madhavan Chari
    Date Modified: 10/01/2007
    Description:   Issue_627-Added Logic to ensure the Milestone Curency is same as
                   header Currency.

******************************************************************************/

package config.java.hook.vcsv3;

import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Currency;
import ariba.basic.core.LocaleID;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.Milestone;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.OrgControlKey;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;
import config.java.common.CATFS7200;

public class CatEZOMARSubmitHook implements ApprovableHook {
	private static final String ClassName = "CatEZOMARSubmitHook";
	private static final int ErrorCode = -1;
	private static final int NoErrorCode = 0;
	private static final List NoError = ListUtil.list(Constants.getInteger(0));

	// Commented out so as to initialize in the method as localized strings
	// private static final String SingleSplitError = Fmt.Sil("cat.vcsv3","AccountDistributionError_Single");
	// private static final String MultiSplitError = Fmt.Sil("cat.vcsv3","AccountDistributionError_Multiple");
	// private static final String InvalidAccountingMsg = Fmt.Sil("cat.java.vcsv3", "AccountDistributionNotValid");
	// private static String CurrencyErrorMsg = Fmt.Sil("cat.java.vcsv3", "ErrorCurrencyMismatch");
	// private static String HeaderCurrencyErrorMsg = Fmt.Sil("cat.java.vcsv3", "ErrorHCurrencyMismatch");
	// private static String AccntFacErrorMsg = Fmt.Sil("cat.java.vcsv3", "ErrorAccntFacMismatch");

	private static final String AccntsToSkipValidation = ResourceService.getString("cat.java.vcsv3", "AccountTypeToSkipOrderValidation");

	private static String firstCurrency = null;
	private static String firstAccntFac = null;

	public List run(Approvable approvable)
	{
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

		String CurrencyErrorMsg = Fmt.Sil(userLocale,"cat.java.vcsv3", "ErrorCurrencyMismatch");
		if (StringUtil.nullOrEmptyOrBlankString(CurrencyErrorMsg))
			CurrencyErrorMsg = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String HeaderCurrencyErrorMsg = Fmt.Sil(userLocale,"cat.java.vcsv3", "ErrorHCurrencyMismatch");
		if (StringUtil.nullOrEmptyOrBlankString(HeaderCurrencyErrorMsg))
			HeaderCurrencyErrorMsg = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String AccntFacErrorMsg = Fmt.Sil(userLocale,"cat.java.vcsv3", "ErrorAccntFacMismatch");
		if (StringUtil.nullOrEmptyOrBlankString(AccntFacErrorMsg))
			AccntFacErrorMsg = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		String AccntsToSkipValidation = Fmt.Sil(userLocale,"cat.java.vcsv3", "AccountTypeToSkipOrderValidation");
		if (StringUtil.nullOrEmptyOrBlankString(AccntsToSkipValidation))
			AccntsToSkipValidation = Fmt.Sil(Locale.US,"cat.vcsv3","Error_RFQHasCatalogItems");

		if (approvable instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) approvable;
			List lines = (List) mar.getFieldValue("LineItems");
			int size = ListUtil.getListSize(lines);

			firstCurrency = null;
			firstAccntFac = null;

			boolean hasErrors = false;
			boolean accntingError = false;
			FastStringBuffer totalMsg = new FastStringBuffer();

			if (mar != null){
				String maMinAmountCurr = "";
				String maMaxAmountCurr = "";
				String maPLAmountCurr = "";
				String headerCurrency = "";
				Money maMinAmount = mar.getMinAmount();
				Money maMaxAmount = mar.getMaxAmount();
				Money maPLAmount = mar.getPreloadAmount();
				Currency headerCurr = (Currency) mar.getFieldValue("Currency");
				if (maMinAmount != null)
					maMinAmountCurr = maMinAmount.getCurrency().getUniqueName();
				if (maMaxAmount != null)
					maMaxAmountCurr = maMaxAmount.getCurrency().getUniqueName();
				if (maPLAmount != null)
					maPLAmountCurr = maPLAmount.getCurrency().getUniqueName();
				if (headerCurr != null)
					headerCurrency = headerCurr.getUniqueName();

				if (size > 0){
					for (int i = 0; i < size && !hasErrors; i++) {
						ContractRequestLineItem line = (ContractRequestLineItem) lines.get(i);
						boolean enterAccntng = false;
						Boolean lineEnterAccntng = (Boolean) line.getDottedFieldValue("EnterAccounting");
						if (lineEnterAccntng != null) {
							enterAccntng = lineEnterAccntng.booleanValue();
						}
						if (enterAccntng) {
							String lineresult = accountingCheck(line, userLocale);
							if (!lineresult.equals("0")) {
								String formatLineError = Fmt.S(lineresult, i+1);
								hasErrors = true;
								accntingError = true;
								totalMsg.append(formatLineError);
								//if (Log.customer.debugOn)
									Log.customer.debug("%s ::: Line Error Msg: %s", ClassName, formatLineError);
							}
						}

						if (!currencyConsistencyCheck(line, i)){
							hasErrors = true;
							String currMsg = CurrencyErrorMsg;
							if (!accntingError)
								currMsg = Fmt.S(" Line %s: ", String.valueOf(i+1)) + CurrencyErrorMsg;
							totalMsg.append(currMsg);
						}

						if (!accntFacConsistencyCheck(line, i)) {
							hasErrors = true;
							String afacMsg = AccntFacErrorMsg;
							if (!accntingError)
								afacMsg = Fmt.S(" Line %s: ", String.valueOf(i+1)) + AccntFacErrorMsg;
							totalMsg.append(afacMsg);
						}
					}
				}

				if (!StringUtil.nullOrEmptyOrBlankString(firstCurrency)
					&& (!StringUtil.nullOrEmptyOrBlankString(maMinAmountCurr)
					|| !StringUtil.nullOrEmptyOrBlankString(maMaxAmountCurr)
					|| !StringUtil.nullOrEmptyOrBlankString(headerCurrency)
					|| !StringUtil.nullOrEmptyOrBlankString(maPLAmountCurr))) {
					if ((!StringUtil.nullOrEmptyOrBlankString(maMinAmountCurr)) && (!firstCurrency.equals(maMinAmountCurr))) {
						String currMsg = Fmt.S(" Header: %s", HeaderCurrencyErrorMsg);
						if (totalMsg.indexOf(currMsg) < 0){
							totalMsg.append(currMsg);
						}
						hasErrors = true;
					}
					if ((!StringUtil.nullOrEmptyOrBlankString(maMaxAmountCurr)) && (!firstCurrency.equals(maMaxAmountCurr))) {
						String currMsg = Fmt.S(" Header: %s", HeaderCurrencyErrorMsg);
						if (totalMsg.indexOf(currMsg) < 0){
							totalMsg.append(currMsg);
						}
						hasErrors = true;
					}
					if ((!StringUtil.nullOrEmptyOrBlankString(maPLAmountCurr)) && (!firstCurrency.equals(maPLAmountCurr))) {
						String currMsg = Fmt.S(" Header: %s", HeaderCurrencyErrorMsg);
						if (totalMsg.indexOf(currMsg) < 0){
							totalMsg.append(currMsg);
						}
						hasErrors = true;
					}
					if ((!StringUtil.nullOrEmptyOrBlankString(headerCurrency)) && (!firstCurrency.equals(headerCurrency))) {
						String currMsg = Fmt.S(" Header: %s", HeaderCurrencyErrorMsg);
						if (totalMsg.indexOf(currMsg) < 0){
							totalMsg.append(currMsg);
						}
						hasErrors = true;
					}
				}
			}

			if (hasErrors) {
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: Total Error Msg: %s", ClassName, totalMsg.toString());
				return ListUtil.list(Constants.getInteger(ErrorCode), totalMsg.toString());
			}
		}
		return NoError;
	}

	public static boolean currencyConsistencyCheck(ContractRequestLineItem marli, int i)
	{
		boolean isMatch = true;
		String currCurrency = "";
		String minCurr = "";
		String maxCurr = "";
		String milestonecurr = "";

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering currencyConsistencyCheck", ClassName);

		if (marli != null) {



				Log.customer.debug ("Getting the Milestone");
				Milestone milestone = marli.getMilestone();
				if (milestone != null){
			    Money milestonemaxamount = milestone.getMaxAmount();
			    Log.customer.debug ("Milestone MAxAmount::"+milestonemaxamount);
			    if (milestonemaxamount != null)
				    milestonecurr = milestonemaxamount.getCurrency().getUniqueName();
			    if (!StringUtil.nullOrEmptyOrBlankString(firstCurrency) && !StringUtil.nullOrEmptyOrBlankString(milestonecurr)) {
			    if ((!StringUtil.nullOrEmptyOrBlankString(milestonecurr)) && (!firstCurrency.equals(milestonecurr))) {
					 isMatch = false;
				    }
			     }
		     }

			if (i == 0) {
				LineItemProductDescription lipd = marli.getDescription();
				if (lipd != null) {
					Money price = lipd.getPrice();
					if (price != null && !price.isApproxZero()) {
						firstCurrency = price.getCurrency().getUniqueName();
					}
				}



				Money minAmount = marli.getMinAmount();
				Money maxAmount = marli.getMaxAmount();
				if (minAmount != null)
					minCurr = minAmount.getCurrency().getUniqueName();
				if (maxAmount != null)
					maxCurr = maxAmount.getCurrency().getUniqueName();

				if (!StringUtil.nullOrEmptyOrBlankString(firstCurrency)
					&& (!StringUtil.nullOrEmptyOrBlankString(minCurr)
					|| !StringUtil.nullOrEmptyOrBlankString(maxCurr))) {
					if ((!StringUtil.nullOrEmptyOrBlankString(minCurr)) && (!firstCurrency.equals(minCurr))) {
						isMatch = false;
					}
					if ((!StringUtil.nullOrEmptyOrBlankString(maxCurr)) && (!firstCurrency.equals(maxCurr))) {
						isMatch = false;
					}

				}

				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: isMatch for i=0: " + isMatch, ClassName);
				return isMatch;
			}
			LineItemProductDescription lipd = marli.getDescription();
			if (lipd != null) {
				Money price = lipd.getPrice();
				if (price != null && !price.isApproxZero()) {
					currCurrency = price.getCurrency().getUniqueName();
				}
				Money minAmount = marli.getMinAmount();
				Money maxAmount = marli.getMaxAmount();
				if (minAmount != null)
					minCurr = minAmount.getCurrency().getUniqueName();
				if (maxAmount != null)
					maxCurr = maxAmount.getCurrency().getUniqueName();
				if (!StringUtil.nullOrEmptyOrBlankString(firstCurrency)
					&& (!StringUtil.nullOrEmptyOrBlankString(minCurr)
					|| !StringUtil.nullOrEmptyOrBlankString(maxCurr))) {
					if ((!StringUtil.nullOrEmptyOrBlankString(minCurr)) && (!firstCurrency.equals(minCurr))) {
						isMatch = false;
					}
					if ((!StringUtil.nullOrEmptyOrBlankString(maxCurr)) && (!firstCurrency.equals(maxCurr))) {
						isMatch = false;
					}
				}
				if (!StringUtil.nullOrEmptyOrBlankString(firstCurrency)
					&& !StringUtil.nullOrEmptyOrBlankString(currCurrency)) {
					if (!currCurrency.equals(firstCurrency)) {
						isMatch = false;
					}
				}
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: isMatch for i!=0: " + isMatch, ClassName);
		return isMatch;
	}

	public static boolean accntFacConsistencyCheck(ContractRequestLineItem marli, int i)
	{
		boolean isMatch = true;
		String currAccntFac = "";

		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Entering accntFacConsistencyCheck", ClassName);

		if (marli != null) {
			if (i == 0) {
				SplitAccountingCollection sac = marli.getAccountings();
				if (sac != null) {
					BaseVector splits = sac.getSplitAccountings();
					SplitAccounting firstSA = (SplitAccounting)splits.get(0);
					if (firstSA != null) {
						firstAccntFac = (String) firstSA.getDottedFieldValue("AccountingFacility");
					}
				}
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: isMatch for i=0: " + isMatch, ClassName);
				return isMatch;
			}
			SplitAccountingCollection sac = marli.getAccountings();
			if (sac != null) {
				BaseVector splits = sac.getSplitAccountings();
				SplitAccounting firstSA = (SplitAccounting)splits.get(0);
				if (firstSA != null) {
					currAccntFac = (String) firstSA.getDottedFieldValue("AccountingFacility");
				}
				if (!StringUtil.nullOrEmptyOrBlankString(firstAccntFac)
					&& !StringUtil.nullOrEmptyOrBlankString(currAccntFac)) {
					if (!currAccntFac.equals(firstAccntFac)) {
						isMatch = false;
					}
				}
			}
		}
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: isMatch for i!=0: " + isMatch, ClassName);
		return isMatch;
	}

	protected String accountingCheck(ContractRequestLineItem marli, Locale userLocale) {
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

		ClusterRoot accntType = (ClusterRoot) marli.getFieldValue("AccountType");
		if (accntType != null) {
			accountType = accntType.getUniqueName();
		}

		FastStringBuffer lineMsg = new FastStringBuffer();
		SplitAccountingCollection sac = marli.getAccountings();
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

                            // S. Sato - AUL - putting a null check for response..
						if (response == null) {
							Log.customer.debug(
									"%s ::: Response to web service call was null - " +
									"returning w/o validating", ClassName);
							return lineErrorResult;
						}
						    // S. Sato - AUL - end of null check

						retADK = response.getAccountingDistributionKey();
						sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
						sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

						//if (Log.customer.debugOn) {
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

	public CatEZOMARSubmitHook()
	{
	}
}