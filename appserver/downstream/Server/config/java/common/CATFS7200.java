package config.java.common;

import java.net.URL;

import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.AccountValidationServiceSoapBindingStub;
import cat.cis.fasd.ws.soap.AccountingDistributionKey;
import cat.cis.fasd.ws.soap.OrgControlKey;
import cat.cis.fasd.ws.soap.Param;
import cat.cis.fasd.ws.soap.Response;


public class CATFS7200
{

    Response resp;
    String theEndPoint;
    URL endpoint;

    public CATFS7200()
    {
        resp = null;
        //theEndPoint = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
        String theEndPointDefault = "http://globalsweb.ecorp.cat.com/fasd/ws/services/AccountValidationService";
        String theEndPointStr = ResourceService.getString("cat.ws.util","CATFS7200theEndPoint");
        Log.customer.debug("CATFS7200: Endpoint used from Resource File: " + theEndPointStr);
		theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;
    }

    public Response getResp0309(Param param, AccountingDistributionKey ADK)
    {
        try
        {
			Log.customer.debug("**CATFS7200** Endpoint used: " + theEndPoint);
            URL endpoint = new URL(theEndPoint);
            AccountValidationServiceSoapBindingStub stub = new AccountValidationServiceSoapBindingStub(endpoint, null);
            resp = stub.validate(param, ADK);
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
        return resp;
    }

    public Response getResp11(Param param, OrgControlKey orgcontrolkey)
    {
        try
        {
			Log.customer.debug("**CATFS7200** Endpoint used: " + theEndPoint);
            URL endpoint = new URL(theEndPoint);
            AccountValidationServiceSoapBindingStub stub = new AccountValidationServiceSoapBindingStub(endpoint, null);
            resp = stub.validateOrgControlKey(param, orgcontrolkey);
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
        return resp;
    }

}
