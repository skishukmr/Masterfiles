/*
 * Created on Nov 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package config.java.integration.ws;

import java.net.URL;

import config.java.common.CatCommonUtil;

import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.fasd.ws.soap.AccountValidationServiceSoapBindingStub;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.Message;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;
/**
 * @author nunna
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.

    Set the default parameters to "". It was previously pointing to prod params
*/

public class ServiceInitiator
{

    // String theEndPointDefault = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
	String theEndPointDefault = "";
    String theEndPointStr = ResourceService.getString("cat.ws.util","CATFS7200theEndPoint");
	String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;

    public ServiceInitiator()
    {
        _accountingOrderType = null;
        _subSubAccount = null;
        _subAccount = null;
        _accountingOrderNumber = null;
        _string = null;
        _controlAccountNumber = null;
        _accountingDistributionQualifier = null;
        _expenseAccountNumber = null;
        _accountingOrderPrefix = null;
        _accountingNumberFacilityCode = null;
        _accountingOrderFacilityCode = null;
        _resp = null;
    }

    public void setWSInput(String input[])
    {
        _accountingOrderType = input[0];
        _subSubAccount = input[1];
        _subAccount = input[2];
        _accountingOrderNumber = input[3];
        _string = input[4];
        _controlAccountNumber = input[5];
        _accountingDistributionQualifier = input[6];
        _expenseAccountNumber = input[7];
        _accountingOrderPrefix = input[8];
        _accountingNumberFacilityCode = input[9];
        _accountingOrderFacilityCode = input[10];
    }

    public void setWSParams(String as[])
    {
    }

    public void setWSInput(String AccountingNumberFacilityCode, String ControlAccountNumber, String SubAccount, String SubSubAccount, String ExpenseAccountNumber, String AccountingOrderNumber, String AcctgDist)
    {
        _accountingNumberFacilityCode = AccountingNumberFacilityCode;
        _controlAccountNumber = ControlAccountNumber;
        _subAccount = SubAccount;
        _subSubAccount = SubSubAccount;
        _expenseAccountNumber = ExpenseAccountNumber;
        _accountingOrderNumber = AccountingOrderNumber;
        _accountingDistributionQualifier = AcctgDist;
    }

    public AccountingDistributionKey createAccountingDistributionKey()
    {
        AccountingDistributionKey input = new AccountingDistributionKey();
        input.setControlAccountNumber(_controlAccountNumber);
        input.setAccountingDistributionQualifier(_accountingDistributionQualifier);
        input.setAccountingNumberFacilityCode(_accountingNumberFacilityCode);
        input.setAccountingOrderFacilityCode(_accountingOrderFacilityCode);
        input.setAccountingOrderNumber(_accountingOrderNumber);
        input.setAccountingOrderPrefix(_accountingOrderPrefix);
        input.setAccountingOrderType(_accountingOrderType);
        input.setExpenseAccountNumber(_expenseAccountNumber);
        input.setSubAccount(_subAccount);
        input.setFormattedString(_string);
        input.setSubSubAccount(_subSubAccount);
        return input;
    }

    public Param createParam()
    {
        Param param = new Param();
        param.setFunctionIndicator("01");
        param.setMessageType("M");
        return param;
    }

    public String[] validateAccount()
        throws Exception
    {
        String respCode = null;
        String respMessage = null;
        Log.customer.debug("**AR** creating param" + createParam().toString());
        Log.customer.debug("**AR** creating adk" + createAccountingDistributionKey().toString());

            // S. Sato - AUL - Skip web call if disable integration parameter is set
            // Note: This parameter is not really required except in the lab environment
            // when the integration needs to be disabled. If this parameter is not mentioned
            // or set in Parameters.table, the system just goes ahead and integrates
        if (CatCommonUtil.DisableIntegration) {

            Log.customer.debug(
                "Parameter: %s has been set to true. The system will not make the  " +
                "account validation call and will return a response code 00",
                CatCommonUtil.DisableIntegrationParam);
                respCode = "00";
                respMessage = "Skip the web call as disable integration has been set.";
            return (new String[] {
                    respCode, respMessage
            });
        }

        //theEndPoint = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";

		Log.customer.debug("validateAccount:theEndPoint used : "+ theEndPoint);


        endpoint = new URL(theEndPoint);
        stub = new AccountValidationServiceSoapBindingStub(endpoint, null);
        if(stub.validate(createParam(), createAccountingDistributionKey()) != null)
        {
            _resp = stub.validate(createParam(), createAccountingDistributionKey());
            Log.customer.debug("**AR** ok, not null - stub validate");
        } else
        {
            Log.customer.debug("**AR** is null - stub validate");
        }
        Log.customer.debug("**AR** getting _resp: stub . validate");
        respCode = _resp.getMessage().getSubroutineReturnCode();
        Log.customer.debug("**AR** got resp code: " + respCode.toString());
        respMessage = _resp.getMessage().getSubroutineReturnMessage();
        Log.customer.debug("**AR** got resp msg: " + respMessage.toString());
        return (new String[] {
            respCode, respMessage
        });
    }

    public static void main(String args[])
        throws Exception
    {
        String data[] = {
            null, "00", "634", "75817", null, "J0945", null, "1022", null, "06",
            null
        };
        ServiceInitiator init = new ServiceInitiator();
        init.setWSInput(data);
        Param param = init.createParam();
        AccountingDistributionKey input = init.createAccountingDistributionKey();
        //theEndPoint = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
	    // String theEndPointDefaultTest = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
	    String theEndPointDefaultTest = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
	    String theEndPointStrTest = ResourceService.getString("cat.ws.util","CATFS7200theEndPoint");
		String theEndPointTest = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStrTest))?theEndPointStrTest:theEndPointDefaultTest) ;

        endpoint = new URL(theEndPointTest);
        stub = new AccountValidationServiceSoapBindingStub(endpoint, null);
        Response resp = stub.validate(param, input);
        Message msg = resp.getMessage();
        System.out.println(msg.getSubroutineReturnCode());
        System.out.println(msg.getMsgText());
        System.out.println(resp.getAccountingControlKey());
    }

    private String _accountingOrderType;
    private String _subSubAccount;
    private String _subAccount;
    private String _accountingOrderNumber;
    private String _string;
    private String _controlAccountNumber;
    private String _accountingDistributionQualifier;
    private String _expenseAccountNumber;
    private String _accountingOrderPrefix;
    private String _accountingNumberFacilityCode;
    private String _accountingOrderFacilityCode;
    private static URL endpoint;
    private static AccountValidationServiceSoapBindingStub stub;
    private Response _resp;
}
