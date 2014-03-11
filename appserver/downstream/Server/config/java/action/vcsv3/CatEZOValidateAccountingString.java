/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/22/2006
	Description: 	Trigger to carry out FS7200 Accounting validation on Req
					Line Items.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  11/29/2006
	Description: 	Changed trigger to call Function 09 based on Heidi's e-mail
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:  01/12/2007
	Description: 	Added logic to skip validation for sa where AccountType is
					Other and the Order Number is populated
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Iterator;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.Message;
import cat.cis.fasd.ws.soap.OrgControlKey;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;
import config.java.common.CATFS7200;

public class CatEZOValidateAccountingString extends Action
{
	private static final String ClassName = "CatEZOValidateAccountingString";
	private static final String ValidAccountingMsg = ResourceService.getString("cat.java.vcsv3", "AccountDistributionValid");
	private static final String InvalidAccountingMsg = ResourceService.getString("cat.java.vcsv3", "AccountDistributionNotValid");
	private static final String AdditionalMessage = ResourceService.getString("cat.java.vcsv3", "AccountingErrorGuidance");
	private static final String AccntsToSkipValidation = ResourceService.getString("cat.java.vcsv3", "AccountTypeToSkipOrderValidation");

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		// Code Added For NP Exception
					validateResponse(object);
	}

	public  Response validateResponse(ValueSource object)
	{
		Response response = null;
		if (object instanceof ProcureLineItem)
				{
					Log.customer.debug("%s ::: Validate Acctng fire!", ClassName);

					CATFS7200 catfs7200 = new CATFS7200();
					OrgControlKey retOCK = null;
					AccountingDistributionKey retADK = null;
					String sbrtnRtCode = null;
					String sbrtnMessage = null;
					StringBuffer errorMsg = null;
					String accountType = "";

					ProcureLineItem pli = (ProcureLineItem) object;

					ClusterRoot accntType = (ClusterRoot) pli.getFieldValue("AccountType");
					if (accntType != null) {
						accountType = accntType.getUniqueName();
					}

					SplitAccountingCollection sac = pli.getAccountings();
					if (sac != null)
					{
						Iterator saci = sac.getAllSplitAccountingsIterator();
						while (saci.hasNext())
						{
							SplitAccounting sa = (SplitAccounting) saci.next();

							if (!shouldSkipValidation(accountType, sa)) {
								response = null;
								retOCK = null;
								retADK = null;
								sbrtnRtCode = null;
								sbrtnMessage = null;
								errorMsg = null;

								sa.setFieldValue("ValidateAccountingMessage", null);
								FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

								Log.customer.debug("%s ::: Calling the live function 09 local call", ClassName);
								// Live Call
								 response = catfs7200.getResp0309(getParamObj(sa, "09"), getAccntDistKeyObj(sa));
								 if (response == null)
								                         {
								 				sa.setDottedFieldValue("ValidateAccountingMessage", "Accounting Web Serivce Down!!!");
								                                 Log.customer.debug("%s *** Response Object is NULL");
								                                 return null;
								                         }
								                         else
								                         {
								                                 Log.customer.debug("%s *** Response Object is NOT NULL");
								                         }
                        		Log.customer.debug("%s *** After the NULL Check for Response Object");
								// Simulated Call
								// response = getResp0309Local(getParamObj(sa, "09"), getAccntDistKeyObj(sa));

								retADK = response.getAccountingDistributionKey();
								sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
								sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

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

								if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") == 0)) {
									//This means it is good accounting combination

									Log.customer.debug("%s ::: Valid Accounting Combination !!!!!", ClassName);
									sa.setDottedFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

            				            // S. Sato - AUL - Added isImmutable check
                    				if (!fp.isImmutable()) {
									    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
                    				}
								}
								else {
									if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
										//Stop! Critical Error
										Log.customer.debug("%s ::: Stop! Critical Error returned from Function 09 !!!!!", ClassName);
										if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
											sbrtnMessage = InvalidAccountingMsg;
										}
										errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnMessage + ".").append(AdditionalMessage);
										sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

                				            // S. Sato - AUL - Added isImmutable check
	                    				if (!fp.isImmutable()) {
										    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
	                    				}
									}
									else {
										// Otherwise, Error occured on the validation
										Log.customer.debug("%s ::: Error returned from Function 09 !!!!!", ClassName);
										if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
											sbrtnMessage = InvalidAccountingMsg;
										}
										errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
										sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

                				            // S. Sato - AUL - Added isImmutable check
	                    				if (!fp.isImmutable()) {
										    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
	                    				}
									}
								}
							}
							else {

								Log.customer.debug("%s ::: Account Type should be skipped for validation", ClassName);
								sa.setFieldValue("ValidateAccountingMessage", null);
								FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
								sa.setDottedFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

        				            // S. Sato - AUL - Added isImmutable check
                				if (!fp.isImmutable()) {
								    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
                				}
							}
						}
					}
		}
			return response;
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

	public static OrgControlKey getOrgCntrlKeyObj(SplitAccounting sa) {
		String fac = (String) sa.getFieldValue("AccountingFacility");
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");

		OrgControlKey ock = new OrgControlKey();
		ock.setOrganization(div + sect);
		ock.setOrgFacility(fac);
		return ock;
	}

	public static Response getResp0309Local(Param param, AccountingDistributionKey adk) {
		String expAccnt = adk.getExpenseAccountNumber();
		Response response = new Response();
		Message message = new Message();
		if ("1010".equals(expAccnt)) {
			message.setSubroutineReturnCode("11");
			message.setSubroutineReturnMessage("This is an invalid accounting combination");
		}
		else {
			message.setSubroutineReturnCode("00");
			message.setSubroutineReturnMessage("This is a valid accounting combination");
		}
		response.setMessage(message);
		response.setAccountingDistributionKey(adk);
		return response;
	}

	public boolean shouldSkipValidation(String accountType, SplitAccounting sa) {
		String order = (String) sa.getFieldValue("Order");
		boolean shouldSkip = false;

		String [] types = StringUtil.delimitedStringToArray(AccntsToSkipValidation,',');

		Log.customer.debug("%s ::: type array: %s", ClassName, types);

		if (types != null) {
			int i = types.length;

			Log.customer.debug("%s ::: type array length: " + i, ClassName);
			while (i-1 >= 0){
				String testAccntType = types[i-1];

				Log.customer.debug("%s ::: testAccntType: %s", ClassName, testAccntType);
				if ((!StringUtil.nullOrEmptyOrBlankString(accountType))
				&& (accountType.equals(testAccntType))
				&& (!StringUtil.nullOrEmptyOrBlankString(order))) {

					Log.customer.debug("%s ::: Account Type should be skipped: %s/%s", ClassName, testAccntType, accountType);
					shouldSkip = true;
				}
				i--;
			}
		}

		Log.customer.debug("%s ::: Returning shouldSkip: " + shouldSkip, ClassName);
		return shouldSkip;
	}

	public CatEZOValidateAccountingString()
	{
	}
}
