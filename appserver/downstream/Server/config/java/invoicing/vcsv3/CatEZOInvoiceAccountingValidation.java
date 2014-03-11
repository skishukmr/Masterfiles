package config.java.invoicing.vcsv3;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.FieldProperties;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.Message;
import cat.cis.fasd.ws.soap.OrgControlKey;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;
import config.java.common.CATFS7200;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;


public class CatEZOInvoiceAccountingValidation {

	private static final String ClassName = "CatEZOInvoiceAccountingValidation";
	private static final String Live7200Indicator = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_LiveIndicator");
	private static final String SimulationErrorKey = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_SimulationErrorKey");
	protected static final String ValidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_Valid");
	private static final String InvalidAccountingMsg = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_NotValid");
	private static final String AdditionalMessage = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_ErrorGuidance");
	private static final String SimulationErrorMsg = "PROBLEM SIMULATING ACCOUNTING VALIDATION!";
	private static final String param_dept = "Application.Caterpillar.Procure.DepartmentForCapital";
	protected static final String skipOtherOrder = ResourceService.getString("cat.invoicejava.vcsv3", "AcctngValidation_ExceptionOtherOrder");
	protected static final String skipAcctType = "Other";


	public static Response validateAccounting(SplitAccounting sa) {
		CATFS7200 catfs7200 = new CATFS7200();
		Response response = null;
		OrgControlKey retOCK = null;
		AccountingDistributionKey retADK = null;
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		StringBuffer errorMsg = null;

		sa.setFieldValue("ValidateAccountingMessage", null);
		FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

		//if (Log.customer.debugOn)
		    Log.customer.debug("%s ::: Calling the live function 09 local call", ClassName);
		response = catfs7200.getResp0309(getParamObj(sa, "09"), getAccntDistKeyObj(sa));

            // S. Sato AUL - Added null check in scenarios where web service is not enabled
        if (response == null) {
            Log.customer.debug(
                    "%s ::: No Response.. Web Service may be disabled. Doing nothing..",
                    ClassName);
            return null;
        }

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
			sa.setDottedFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

			    // S. Sato - Added IsImmutable property check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
			}
			return response;
		}

		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("89") > 0)) {
			//Stop! Critical Error
			Log.customer.debug("%s ::: Stop! Critical Error returned from Function 09 !!!!!", ClassName);
			if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
				sbrtnMessage = InvalidAccountingMsg;
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
			errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnMessage + ".").append(AdditionalMessage);
			sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

			    // S. Sato AUL - Added isImmutable fp check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
			}
			return response;
		}

		// Otherwise, Error occured on the validation
		Log.customer.debug("%s ::: Error returned from Function 09 !!!!!", ClassName);
		if (StringUtil.nullOrEmptyOrBlankString(sbrtnMessage)) {
			sbrtnMessage = InvalidAccountingMsg;
		}

		errorMsg = (new StringBuffer(InvalidAccountingMsg)).append(sbrtnRtCode + " - " + sbrtnMessage + ".").append(AdditionalMessage);

		//sa.setDottedFieldValue("ValidateAccountingMessage", "Error Encountered - " + sbrtnRtCode + " - " + sbrtnMessage);
		sa.setDottedFieldValue("ValidateAccountingMessage", errorMsg.toString());

	        // S. Sato AUL - Added isImmutable fp check
		if (!fp.isImmutable()) {
		    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
		}
		return response;
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
		String div = (String) sa.getFieldValue("Division");
		String sect = (String) sa.getFieldValue("Section");

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

	//	 method used for local instance & temporary testing
	public static void simulateValidateAccounting(SplitAccounting sa) {

		FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

	        // S. Sato AUL - Added isImmutable fp check
		if (!fp.isImmutable()) {
		    fp.setPropertyForKey("ValueStyle","brandVeryDkText");
		}
		CatAccountingCollector cac = getCatAccounting(sa);
        try {
	        CatAccountingValidator validator = callFS7200Placeholder(cac);
			if (validator != null) {
			    //if (Log.customer.debugOn) {
					Log.customer.debug("%s *** SIMULATION ResultCode: %s", ClassName, validator.getResultCode());
					Log.customer.debug("%s *** SIMULATION Message: %s", ClassName, validator.getMessage());
			    //}
				if (validator.getResultCode().equals("00")) {
					sa.setFieldValue("ValidateAccountingMessage",ValidAccountingMsg);
				}
				else {
				    StringBuffer sb = new StringBuffer(InvalidAccountingMsg).append(validator.getMessage()).
				    						append(AdditionalMessage);

				        // S. Sato AUL - Added isImmutable fp check
					if (!fp.isImmutable()) {
				        fp.setPropertyForKey("ValueStyle","catRedTextSm");
					}
				    sa.setFieldValue("ValidateAccountingMessage", sb.toString());
				}
			}
        } catch (Exception e){

			Log.customer.debug("%s *** SIMULATION Exception: %s", ClassName, e);

		        // S. Sato AUL - Added isImmutable fp check
			if (!fp.isImmutable()) {
			    fp.setPropertyForKey("ValueStyle", "catRedTextSm");
			}
            sa.setDottedFieldValue("ValidateAccountingMessage", SimulationErrorMsg);
        }
	}

	// method used for local instance & temporary testing
	public static CatAccountingCollector getCatAccounting(SplitAccounting sa) {

		String fac = (String)sa.getFieldValue("AccountingFacility");
		String dept = (String)sa.getFieldValue("Department");
		String div = (String)sa.getFieldValue("Division");
		String sect = (String)sa.getFieldValue("Section");
		String exp = (String)sa.getFieldValue("ExpenseAccount");
		String order = (String)sa.getFieldValue("Order");
		String misc = (String)sa.getFieldValue("Misc");

		return new CatAccountingCollector(fac, dept, div, sect, exp, order, misc);
	}

//	 method used for local instance & temporary testing
	public static CatAccountingValidator callFS7200Placeholder (CatAccountingCollector collector) throws Exception {

		String result = "00";
		String message = null;

		if (collector != null) {
			String facility = collector.getFacility();
			if (facility != null && facility.equals(SimulationErrorKey)) {
				result = "005";
				message = "Facility is not valid for this Department.";
			}
		}
		return new CatAccountingValidator (result, message);
	}

	public static boolean getIsSimulation() {

	    boolean isSimulation = false;

	    if (!StringUtil.nullOrEmptyOrBlankString(Live7200Indicator)) {
		    Integer liveKey = Integer.valueOf(Live7200Indicator);
	        Log.customer.debug("%s ::: Live 7200 Indicator: %s", ClassName, liveKey);
		    if (liveKey != null && liveKey.intValue() == 0)
		    	isSimulation = true;
		}
	    return isSimulation;
	}

	public static boolean validateIRLineAccounting(InvoiceReconciliationLineItem irli) {

		//if (Log.customer.debugOn)
		    Log.customer.debug("\n %s ::: ENTERING validateIRLineAccounting()! \n", ClassName);

	    boolean isValid = true;
	    String acctMsg = "";

	    // 01.17.06  Added skip for AccountType = Other && non-Null Order#
	    ClusterRoot acctType = null;
	    if (!StringUtil.nullOrEmptyOrBlankString(skipOtherOrder) && skipOtherOrder.startsWith("Y"))
	    	acctType =(ClusterRoot)irli.getFieldValue("AccountType");

        SplitAccountingCollection sac = irli.getAccountings();
		if (sac != null) {
			Iterator saci = sac.getAllSplitAccountingsIterator();

			// 01.17.06  Added skip for AccountType = Other && non-Null Order#
		    if (acctType == null || !acctType.getUniqueName().equals(skipAcctType)) {
				if (!getIsSimulation()) {
					while (saci.hasNext()) {
						SplitAccounting sa = (SplitAccounting) saci.next();
						sa.setFieldValue("ValidateAccountingMessage", null);
						validateAccounting(sa);
						acctMsg = (String)sa.getFieldValue("ValidateAccountingMessage");
						//if (Log.customer.debugOn)
						    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n", acctMsg);
						if (acctMsg != null && !acctMsg.equals(ValidAccountingMsg))
						    isValid = false;
					}
				}
				else { // use local simulation (AcctngFacilty NG is invalid, all others valid)
					while (saci.hasNext()) {
						SplitAccounting sa = (SplitAccounting) saci.next();
						sa.setFieldValue("ValidateAccountingMessage", null);
						simulateValidateAccounting(sa);
						acctMsg = (String)sa.getFieldValue("ValidateAccountingMessage");
						//if (Log.customer.debugOn)
						    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n", acctMsg);
						if (acctMsg != null && !acctMsg.equals(ValidAccountingMsg))
						    isValid = false;
					}
				}
		    }
		    else { // 01.17.06  Added temporary branch - skip handling for AccountType = Other

		        //if (Log.customer.debugOn)
				    Log.customer.debug("\n %s ::: TEMP logic branch - Acct Type = Other!", ClassName);
				while (saci.hasNext()) {
					SplitAccounting sa = (SplitAccounting) saci.next();
					sa.setFieldValue("ValidateAccountingMessage", null);
					String order = (String)sa.getFieldValue("Order");
					if (StringUtil.nullOrEmptyOrBlankString(order)) {
						sa.setFieldValue("ValidateAccountingMessage", null);
						validateAccounting(sa);
						acctMsg = (String)sa.getFieldValue("ValidateAccountingMessage");
						//if (Log.customer.debugOn)
						    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n", acctMsg);
						if (acctMsg != null && !acctMsg.equals(ValidAccountingMsg))
						    isValid = false;
					}
					else {
				        //if (Log.customer.debugOn)
						    Log.customer.debug("\n %s ::: Order Number populated, skipping Validation!", ClassName);
					}
				}
		    }
			irli.setAccountings(sac);
		}

		//if (Log.customer.debugOn)
		    Log.customer.debug("\n ::: EXITING validateIRLineAccounting()! isValid? " + isValid);
	    return isValid;
	}

	public CatEZOInvoiceAccountingValidation() {
		super();
	}
}
