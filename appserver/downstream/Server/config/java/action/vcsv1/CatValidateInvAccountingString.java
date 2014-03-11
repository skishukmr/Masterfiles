package config.java.action.vcsv1;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.statement.core.StatementCoreApprovableLineItem;
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
//import config.java.common.BusinessWebService;
//import cat.cis.atm.business.ValidationStatus;

/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 * AUL : Changed InvoiceCoreApprovableLineItem to StatementCoreApprovableLineItem
 * AUL-TBD : Need to Test Custom Log
 */


public class CatValidateInvAccountingString extends Action {

	private static final String ClassName = "CatValidateInvAccountingString";
	private static final String ValidAccountingMsg = ResourceService.getString("cat.java.vcsv1", "AccountDistributionValid");
	private static final String InvalidAccountingMsg = ResourceService.getString("cat.java.vcsv1", "AccountDistributionNotValid");
	private static final String AdditionalMessage = ResourceService.getString("cat.java.vcsv1", "AccountingErrorGuidance");
	private static final String param_dept = "Application.Caterpillar.Procure.DepartmentForCapital";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
		//BaseSession session = Base.getSession();
		//session.transactionBegin();

		if (object instanceof SplitAccountingCollection){
			object = ((SplitAccountingCollection)object).getLineItem();
		}

		if (object instanceof StatementCoreApprovableLineItem) {
			Log.customer.debug("%s ::: Entering Validate Acctng fire for Invoice Reconciliations", ClassName);
			StatementCoreApprovableLineItem irli = (StatementCoreApprovableLineItem) object;
			SplitAccountingCollection sac = irli.getAccountings();
//			CATFS7200 catfs7200 = new CATFS7200();
//			Response response = null;
//			OrgControlKey retOCK = null;
//			AccountingDistributionKey retADK = null;
//			String sbrtnRtCode = null;
//			String sbrtnMessage = null;

			if (sac != null) {
				Iterator saci = sac.getAllSplitAccountingsIterator();
//				Response response = null;
//				String sbrtnRtCode = null;
//				String sbrtnMessage = null;

				while (saci.hasNext()) {
					SplitAccounting sa = (SplitAccounting) saci.next();
					sa.setFieldValue("ValidateAccountingMessage", null);
//					FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

//					response = validateAccounting(sa);

					validateAccounting(sa);
					Log.customer.debug("\n\n\nThe SA Message is %s \n\n\n", (String)sa.getFieldValue("ValidateAccountingMessage"));
//					sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
//					sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
//					sa.setDottedFieldValue("ValidateAccountingMessage", sbrtnMessage);
//					fp.setPropertyForKey("ValueStyle", "catRedTextSm");
				}
			}
			irli.setAccountings(sac);
		}
//		session.transactionCommit();
//		session.sessionCommit();
//		session.refreshSession();
	}

	public static Response validateAccounting(SplitAccounting sa) {
		CATFS7200 catfs7200 = new CATFS7200();
		Response response = null;
		OrgControlKey retOCK = null;
		AccountingDistributionKey retADK = null;
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
//		String errorField = null;

		sa.setFieldValue("ValidateAccountingMessage", null);
		FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

		String bookingFacility = (String) sa.getFieldValue("AccountingFacility");
		if (bookingFacility != null){
			bookingFacility = bookingFacility.toUpperCase();
		}
		ClusterRoot catBookingFacility = null;
		String newBookingFacility = "";

		if(!StringUtil.nullOrEmptyOrBlankString(bookingFacility)){
			bookingFacility = bookingFacility.toUpperCase();
			catBookingFacility = Base.getSession().objectFromName(bookingFacility, "cat.core.CATBookingFacility", sa.getPartition());
		}

		//Validate the booking facility for the accounting distribution
		if (catBookingFacility != null) {

				Log.customer.debug("%s ::: The catBookingFacility object found is: %s", ClassName, catBookingFacility);
				Log.customer.debug("%s ::: The catBookingFacility object found is: %s", ClassName, catBookingFacility.getUniqueName());

			if ((catBookingFacility.getFieldValue("BookingFacility") != null) && !((Boolean) catBookingFacility.getFieldValue("BookingFacility")).booleanValue()) {
				if (catBookingFacility.getFieldValue("ConvertToFacility") != null){
					newBookingFacility = (String) catBookingFacility.getFieldValue("ConvertToFacility");

						Log.customer.debug("%s ::: Found a valid convert to booking facility: %s", ClassName, newBookingFacility);

					sa.setDottedFieldValue("AccountingFacility", newBookingFacility);
				}
				else{
					//This case is where the system doesn't have a valid convert to booking facility
					newBookingFacility = bookingFacility;

						Log.customer.debug("%s ::: Found no valid convert to booking facility for %s", ClassName, newBookingFacility);

					Response custResponse = new Response();
					Message custMessage = new Message();
					custMessage.setSubroutineReturnCode("WS");
					custMessage.setSubroutineReturnMessage("Invalid Accounting: No valid convert to booking facility exists in MSC");
					custResponse.setMessage(custMessage);
					StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("No valid convert to booking facility exists in MSC.").append(AdditionalMessage);
					//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - WS -  No valid convert to booking facility exists in MSC");
					sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

				        // S. Sato - AUL - Added isImmutable check
					if (!fp.isImmutable()) {
					    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
					}
					sa.setDottedFieldValue("AccountingFacility", newBookingFacility);
					return custResponse;
				}
			}
			else{
				//Current facility is a valid booking facity
				newBookingFacility = bookingFacility;

					Log.customer.debug("%s ::: Current facility is a valid booking facility: %s", ClassName, newBookingFacility);

				sa.setDottedFieldValue("AccountingFacility", newBookingFacility);
			}
		}
		else {

				Log.customer.debug("%s ::: The catBookingFacility object found is null", ClassName);
				Log.customer.debug("%s ::: System Error: Cannot find a valid booking facility validation object", ClassName);

			newBookingFacility = bookingFacility;
			Response custResponse = new Response();
			Message custMessage = new Message();
			custMessage.setSubroutineReturnCode("WS");
			custMessage.setSubroutineReturnMessage("Invalid Accounting: Your booking facility is not a valid facility in MSC");
			custResponse.setMessage(custMessage);
			sa.setDottedFieldValue("AccountingFacility", newBookingFacility);
			StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Your booking facility is not a valid facility in MSC.").append(AdditionalMessage);
			//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - WS -  Your booking facility is not a valid facility in MSC");
			sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

		        // S. Sato - AUL - Added isImmutable check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
			}
			return custResponse;
		}

		//Check if Department number starts with J or j
		if (sa.getFieldValue("Department") != null
			&& (((String) sa.getFieldValue("Department")).startsWith("J") || ((String) sa.getFieldValue("Department")).startsWith("j"))) {
			Log.customer.debug("%s ::: FS7200 Validation using function call 11", ClassName);
			response = catfs7200.getResp11(getParamObj(sa, "11"), getOrgCntrlKeyObj(sa));
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
			retOCK = response.getOrgControlKey();
			sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
			sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

			Log.customer.debug("\n\n\n");
			Log.customer.debug("%s ::: OCK Response Object", ClassName);
			/* AUL-TBD : Need to test Custom Log */
			config.java.common.Log.customCATLog.debug("%s ::: getMsgText : %s", ClassName, response.getMessage().getMsgText());
			config.java.common.Log.customCATLog.debug("%s ::: getDb2SQLSubroutineReturnCode : %s", ClassName, response.getMessage().getDb2SQLSubroutineReturnCode());
			config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnCode : %s", ClassName, response.getMessage().getSubroutineReturnCode());
			config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnMessage : %s", ClassName, response.getMessage().getSubroutineReturnMessage());

			Log.customer.debug("%s ::: getDepartmentFacility : %s", ClassName, retOCK.getDepartmentFacility());
			Log.customer.debug("%s ::: getDepartmentInd : %s", ClassName, retOCK.getDepartmentInd());
			Log.customer.debug("%s ::: getDepartmentOrg : %s", ClassName, retOCK.getDepartmentOrg());
			Log.customer.debug("%s ::: getDepartmentOrgType : %s", ClassName, retOCK.getDepartmentOrgType());
			Log.customer.debug("%s ::: getFinancialOrgType : %s", ClassName, retOCK.getFinancialOrgType());
			Log.customer.debug("\n\n\n");
		}

		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
			//Stop! Critical Error
			Log.customer.debug("%s ::: Stop! Critical Error returned from Function 11 !!!!!", ClassName);
			if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
				sbrtnMessage = "Invalid Accounting Combination";
			}
			/*
			errorField = returnErrorFields(sbrtnRtCode);
			StringBuffer errorMsg;
			if (errorField != null){
				errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
			}
			else{
				errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
			}
			*/
			StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

			//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
			sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

		        // S. Sato - AUL - Added isImmutable check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
			}
			return response;
		}
		else {
			Log.customer.debug("%s ::: FS7200 Validation using function call 09", ClassName);
			//TODO: Remove this line when making the live WS Call
			//Log.customer.debug("%s ::: Calling the temporary function 09 local call", ClassName);
			//response = getResp0309Local(getParamObj(sa, "09"), getAccntDistKeyObj(sa));

			//TODO: Remove the following comment to activate live WS Call.
			Log.customer.debug("%s ::: Calling the live function 09 local call", ClassName);
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
			retADK = response.getAccountingDistributionKey();
			sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
			sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

			Log.customer.debug("\n\n\n");
			Log.customer.debug("%s ::: ADK 09 Response Object", ClassName);
			/* AUL-TBD : Need to test Custom Log */
			config.java.common.Log.customCATLog.debug("%s ::: getMsgText : %s", ClassName, response.getMessage().getMsgText());
			config.java.common.Log.customCATLog.debug("%s ::: getDb2SQLSubroutineReturnCode : %s", ClassName, response.getMessage().getDb2SQLSubroutineReturnCode());
			config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnCode : %s", ClassName, response.getMessage().getSubroutineReturnCode());
			config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnMessage : %s", ClassName, response.getMessage().getSubroutineReturnMessage());

			Log.customer.debug("%s ::: getAccountingDistributionQualifier : %s", ClassName, retADK.getAccountingDistributionQualifier());
			Log.customer.debug("%s ::: getAccountingNumberFacilityCode : %s", ClassName, retADK.getAccountingNumberFacilityCode());
			Log.customer.debug("%s ::: getAccountingOrderType : %s", ClassName, retADK.getAccountingOrderType());
			Log.customer.debug("%s ::: getControlAccountNumber : %s", ClassName, retADK.getControlAccountNumber());
			Log.customer.debug("%s ::: getExpenseAccountNumber : %s", ClassName, retADK.getExpenseAccountNumber());
			Log.customer.debug("%s ::: getSubAccount : %s", ClassName, retADK.getSubAccount());
			Log.customer.debug("%s ::: getSubSubAccount : %s", ClassName, retADK.getSubSubAccount());
			Log.customer.debug("\n\n\n");
		}

		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") == 0)) {
			//This means it is good accounting combination
			Log.customer.debug("%s ::: Valid Accounting Combination !!!!!", ClassName);
			sa.setDottedFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

		        // S. Sato - AUL - Added isImmutable check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
			}
			return response;
		}
		else {
			if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
				//Stop! Critical Error
				Log.customer.debug("%s ::: Stop! Critical Error returned from Function 09 !!!!!", ClassName);
				if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
					sbrtnMessage = "Invalid Accounting Combination";
				}
				/*
				errorField = returnErrorFields(sbrtnRtCode);
				StringBuffer errorMsg;
				if (errorField != null){
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				else{
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				*/
				StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
				sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

			        // S. Sato - AUL - Added isImmutable check
				if (!fp.isImmutable()) {
				    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
				}
				return response;
			}
			else {
				Log.customer.debug("%s ::: FS7200 Validation using function call 03", ClassName);
				response = catfs7200.getResp0309(getParamObj(sa, "03"), getAccntDistKeyObj(sa));
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
				retADK = response.getAccountingDistributionKey();
				sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
				sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

				Log.customer.debug("\n\n\n");
				Log.customer.debug("%s ::: ADK 03 Response Object", ClassName);
				/*AUL-TBD : Need to test Custom Log */
				config.java.common.Log.customCATLog.debug("%s ::: getMsgText : %s", ClassName, response.getMessage().getMsgText());
				config.java.common.Log.customCATLog.debug("%s ::: getDb2SQLSubroutineReturnCode : %s", ClassName, response.getMessage().getDb2SQLSubroutineReturnCode());
				config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnCode : %s", ClassName, response.getMessage().getSubroutineReturnCode());
				config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnMessage : %s", ClassName, response.getMessage().getSubroutineReturnMessage());

				Log.customer.debug("%s ::: getAccountingDistributionQualifier : %s", ClassName, retADK.getAccountingDistributionQualifier());
				Log.customer.debug("%s ::: getAccountingNumberFacilityCode : %s", ClassName, retADK.getAccountingNumberFacilityCode());
				Log.customer.debug("%s ::: getAccountingOrderType : %s", ClassName, retADK.getAccountingOrderType());
				Log.customer.debug("%s ::: getControlAccountNumber : %s", ClassName, retADK.getControlAccountNumber());
				Log.customer.debug("%s ::: getExpenseAccountNumber : %s", ClassName, retADK.getExpenseAccountNumber());
				Log.customer.debug("%s ::: getSubAccount : %s", ClassName, retADK.getSubAccount());
				Log.customer.debug("%s ::: getSubSubAccount : %s", ClassName, retADK.getSubSubAccount());
				Log.customer.debug("\n\n\n");
			}
		}

		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
			//Stop! Critical Error
			Log.customer.debug("%s ::: Stop! Critical Error returned from Function 03 !!!!!", ClassName);
			if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
				sbrtnMessage = "Invalid Accounting Combination";
			}
			/*
			errorField = returnErrorFields(sbrtnRtCode);
			StringBuffer errorMsg;
			if (errorField != null){
				errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
			}
			else{
				errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
			}
			*/
			StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

			//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
			sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

			    // S. Sato - added check for isImmutable
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
			}
			return response;
		}
		else {
			if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") == 0)) {
				Log.customer.debug("%s ::: Setting new accounting values from function call 03", ClassName);
				if (!"000".equals(retADK.getAccountingDistributionQualifier())){
					sa.setFieldValue("Misc", retADK.getAccountingDistributionQualifier());
				}
				if (!"00000".equals(retADK.getAccountingOrderNumber())){
					sa.setFieldValue("Order", retADK.getAccountingOrderNumber());
				}
				sa.setFieldValue("AccountingFacility", retADK.getAccountingNumberFacilityCode());
				sa.setFieldValue("Department", retADK.getControlAccountNumber());
				sa.setFieldValue("Division", retADK.getSubAccount());
				sa.setFieldValue("Section", retADK.getSubSubAccount());
				sa.setFieldValue("ExpenseAccount", retADK.getExpenseAccountNumber());

				Log.customer.debug("%s ::: FS7200 Validation using function call 09", ClassName);
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
				retADK = response.getAccountingDistributionKey();
				sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
				sbrtnMessage = response.getMessage().getSubroutineReturnMessage();

				Log.customer.debug("\n\n\n");
				Log.customer.debug("%s ::: ADK 09 Response Object", ClassName);
				/* AUL-TBD : Need to test Custom Log */
				config.java.common.Log.customCATLog.debug("%s ::: getMsgText : %s", ClassName, response.getMessage().getMsgText());
				config.java.common.Log.customCATLog.debug("%s ::: getDb2SQLSubroutineReturnCode : %s", ClassName, response.getMessage().getDb2SQLSubroutineReturnCode());
				config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnCode : %s", ClassName, response.getMessage().getSubroutineReturnCode());
				config.java.common.Log.customCATLog.debug("%s ::: getSubroutineReturnMessage : %s", ClassName, response.getMessage().getSubroutineReturnMessage());

				Log.customer.debug("%s ::: getAccountingDistributionQualifier : %s", ClassName, retADK.getAccountingDistributionQualifier());
				Log.customer.debug("%s ::: getAccountingNumberFacilityCode : %s", ClassName, retADK.getAccountingNumberFacilityCode());
				Log.customer.debug("%s ::: getAccountingOrderType : %s", ClassName, retADK.getAccountingOrderType());
				Log.customer.debug("%s ::: getControlAccountNumber : %s", ClassName, retADK.getControlAccountNumber());
				Log.customer.debug("%s ::: getExpenseAccountNumber : %s", ClassName, retADK.getExpenseAccountNumber());
				Log.customer.debug("%s ::: getSubAccount  : %s", ClassName, retADK.getSubAccount());
				Log.customer.debug("%s ::: getSubSubAccount : %s", ClassName, retADK.getSubSubAccount());
				Log.customer.debug("\n\n\n");
			}
			else {
				//Error occured on the validation
				Log.customer.debug("%s ::: Error returned from Function 09 !!!!!", ClassName);
				if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
					sbrtnMessage = "Invalid Accounting Combination";
				}
				/*
				errorField = returnErrorFields(sbrtnRtCode);
				StringBuffer errorMsg;
				if (errorField != null){
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				else{
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				*/
				StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

				//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
				sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

				    // S. Sato - AUL - Added isImmutable check
				if (!fp.isImmutable()) {
				    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
				}
				return response;
			}
		}

		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") == 0)) {
			//This means it is good accounting combination
			Log.customer.debug("%s ::: Valid Accounting Combination !!!!!", ClassName);
			sa.setDottedFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

		        // S. Sato - AUL - Added isImmutable check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
			}
			return response;
		}
		else {
			if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
				//Stop! Critical Error
				Log.customer.debug("%s ::: Stop! Critical Error returned from Function 09 !!!!!", ClassName);
				if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
					sbrtnMessage = "Invalid Accounting Combination";
				}
				/*
				errorField = returnErrorFields(sbrtnRtCode);
				StringBuffer errorMsg;
				if (errorField != null){
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				else{
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				*/
				StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

				//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
				sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

			        // S. Sato - AUL - Added isImmutable check
				if (!fp.isImmutable()) {
				    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
				}
				return response;
			}
			else {
				//Error occured on the validation
				Log.customer.debug("%s ::: Error returned from Function 09 !!!!!", ClassName);
				if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
					sbrtnMessage = "Invalid Accounting Combination";
				}
				/*
				errorField = returnErrorFields(sbrtnRtCode);
				StringBuffer errorMsg;
				if (errorField != null){
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append("Incorrect Field - " + errorField + " - " + sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				else{
					errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);
				}
				*/
				StringBuffer errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

				//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
				sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

			        // S. Sato - AUL - Added isImmutable check
				if (!fp.isImmutable()) {
				    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
				}
				return response;
			}
		}
	}

	public static Param getParamObj(SplitAccounting sa, String funcInd) {
		String capitalDept = Base.getService().getParameter(sa.getPartition(), param_dept);
		String fac = (String) sa.getFieldValue("AccountingFacility");
		String dept = (String) sa.getFieldValue("Department");
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");
		String exp = (String) sa.getFieldValue("ExpenseAccount");
		String order = (String) sa.getFieldValue("Order");
		String misc = (String) sa.getFieldValue("Misc");

		Param param = new Param();
		//param.setAccountingControlKeyIndicator(dept.substring(0,1));
		param.setFunctionIndicator(funcInd);
		/*
		if (capitalDept.equals(dept))
			param.setExpenseAccountType("Capital");
		else
			param.setExpenseAccountType("Expense");
		*/
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
//		if (!"000".equals(misc)){
			adk.setAccountingDistributionQualifier(misc);
//		}
		adk.setAccountingNumberFacilityCode(fac);
//		if (!"00000".equals(order)){
//			adk.setAccountingOrderType(order);
			adk.setAccountingOrderNumber(order);
//		}
		adk.setControlAccountNumber(dept);
//		if (!"0000".equals(exp)){
			adk.setExpenseAccountNumber(exp);
//		}
		adk.setSubAccount(div);
		adk.setSubSubAccount(sect);
		return adk;
	}

	public static OrgControlKey getOrgCntrlKeyObj(SplitAccounting sa) {
		String fac = (String) sa.getFieldValue("AccountingFacility");
		String dept = (String) sa.getFieldValue("Department");
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");
		String exp = (String) sa.getFieldValue("ExpenseAccount");
		String order = (String) sa.getFieldValue("Order");
		String misc = (String) sa.getFieldValue("Misc");

		OrgControlKey ock = new OrgControlKey();
		ock.setOrganization(div + sect);
		ock.setOrgFacility(fac);
		return ock;
	}

	public static Response getResp0309Local(Param param, AccountingDistributionKey adk) {
		Response response = new Response();
		Message message = new Message();
		message.setSubroutineReturnCode("00");
		message.setSubroutineReturnMessage("This is a valid accounting combination");
		response.setMessage(message);
		response.setAccountingDistributionKey(adk);
		return response;
	}

	public static String returnErrorFields(String sbrtnRtCode) {
		Integer sbrtnRtCodeInt = null;
		int sbrtnRtCodeIntInt;

		try {
			sbrtnRtCodeInt = new Integer(sbrtnRtCode);
		}
		catch (Exception e) {

		}
		if (sbrtnRtCodeInt != null) {
			sbrtnRtCodeIntInt = sbrtnRtCodeInt.intValue();
			if (sbrtnRtCodeIntInt == 10) {
				return "Facility";
			}
			else if (sbrtnRtCodeIntInt == 11) {
				return "Department";
			}
			else if (sbrtnRtCodeIntInt == 12) {
				return "Division";
			}
			else if (sbrtnRtCodeIntInt == 13) {
				return "Section";
			}
			else if (sbrtnRtCodeIntInt == 14
					|| sbrtnRtCodeIntInt == 18
					|| sbrtnRtCodeIntInt == 20) {
				return "Expense Account";
			}
			else if (sbrtnRtCodeIntInt == 15) {
				return "Order Number";
			}
			else if (
				sbrtnRtCodeIntInt == 17
					|| sbrtnRtCodeIntInt == 19
					|| sbrtnRtCodeIntInt == 21
					|| sbrtnRtCodeIntInt == 33
					|| sbrtnRtCodeIntInt == 35) {
				return "Division / Section";
			}
		}
		else{
			if ("WS".equals(sbrtnRtCode)){
				return "Accounting Webservice Unreachable";
			}
		}
		return null;
	}

	public CatValidateInvAccountingString() {
		super();
	}
}
