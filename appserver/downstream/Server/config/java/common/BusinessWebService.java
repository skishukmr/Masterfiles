package config.java.common;

import java.net.URL;

import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.atm.business.ValidationStatus;
import cat.cis.atm.service.SecureMySupplyCabinetServiceSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorWebServiceServiceLocator;

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.

    Set the default parameters to "". It was previously pointing to prod params
*/
public class BusinessWebService
{

    public BusinessWebService()
    {
    }

	// String theEndPointDefault = "https://atm.cat.com/atm/services/MySupplyCabinetService";
    String theEndPointDefault = "";
	String theEndPointStr = ResourceService.getString("cat.ws.util","TaxWSCalltheEndPoint");
	String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;


    public ValidationStatus isValidInvoice(String facilityCode, String invoiceDate)
    {
        ValidationStatus vs = null;
        try
        {
            Log.customer.debug("%s ::: Entering isValidInvoice() method", ClassName);
            //String theEndPoint = "https://atm.cat.com/atm/services/MySupplyCabinetService";
			Log.customer.debug("isValidInvoice:theEndPoint used: "+ theEndPoint);

            URL endpoint = new URL(theEndPoint);
            SecureMySupplyCabinetServiceSoapBindingStub stub = new SecureMySupplyCabinetServiceSoapBindingStub(endpoint, null);
            AuthenticatorWebServiceServiceLocator authLocator = new AuthenticatorWebServiceServiceLocator();
            // String urlString = "https://tufws.cat.com/TUFWebServices/services/authenticator";
            // String urlString = "https://tufwsqax.ecorp.cat.com/TUFWebServices/services/authenticator";
            String urlString = "";

                // S. Sato - AUL (Adding code to look up this URL from a resource. This is the only one which
                //                is hardcoded)
            String preferredURLString = ResourceService.getString("cat.ws.util","TufWSAuthenticator");
            if (!StringUtil.nullOrEmptyOrBlankString(preferredURLString)) {
                urlString = preferredURLString;
            }
            AuthenticatorSoapBindingStub webservice = new AuthenticatorSoapBindingStub(new URL(urlString), authLocator);
            String credential = webservice.authenticate("aribaws", "q37NrANaKc");
            stub.setCredential(credential);
            Log.customer.debug("%s ::: The credential string is: %s", ClassName, credential);
            vs = stub.validateInvoice(facilityCode, invoiceDate);
            Log.customer.debug("%s ::: The ValidationStatus is: %s", ClassName, vs);
            if(vs != null)
            {
                Log.customer.debug("%s ::: The isValid flag from ValidationStatus is: %s", ClassName, (new Boolean(vs.isValid())).toString());
                Log.customer.debug("%s ::: The ReasonCode from ValidationStatus is: %s", ClassName, vs.getReasonCode());
            }
        }
        catch(Exception e)
        {
            Log.customer.debug("%s ::: Webservice call failed due to the following exceptions: " + e.toString());
        }
        return vs;
    }

    public ValidationStatus isValidContractNumber(String contractFileNumber, String date)
    {
        ValidationStatus vs = null;
        try
        {
            Log.customer.debug("%s ::: Entering isValidContractNumber() method", ClassName);
            //String theEndPoint = "https://atm.cat.com/atm/services/MySupplyCabinetService";
            Log.customer.debug("isValidContractNumber:theEndPoint used: "+ theEndPoint);
            URL endpoint = new URL(theEndPoint);
            SecureMySupplyCabinetServiceSoapBindingStub stub = new SecureMySupplyCabinetServiceSoapBindingStub(endpoint, null);
            AuthenticatorWebServiceServiceLocator authLocator = new AuthenticatorWebServiceServiceLocator();
            // String urlString = "https://tufws.cat.com/TUFWebServices/services/authenticator";
            // String urlString = "https://tufwsqax.ecorp.cat.com/TUFWebServices/services/authenticator";
            String urlString = "";

                // S. Sato - AUL (Adding code to look up this URL from a resource. This is the only one which
                //                is hardcoded)
            String preferredURLString = ResourceService.getString("cat.ws.util","TufWSAuthenticator");
            if (!StringUtil.nullOrEmptyOrBlankString(preferredURLString)) {
                urlString = preferredURLString;
            }
            AuthenticatorSoapBindingStub webservice = new AuthenticatorSoapBindingStub(new URL(urlString), authLocator);
            String credential = webservice.authenticate("aribaws", "q37NrANaKc");
            stub.setCredential(credential);
            Log.customer.debug("%s ::: The credential string is: %s", ClassName, credential);
            vs = stub.validateContractFileNumber(contractFileNumber, date);
            Log.customer.debug("%s ::: The ValidationStatus is: %s", ClassName, vs);
            if(vs != null)
            {
                Log.customer.debug("%s ::: The isValid flag from ValidationStatus is: %s", ClassName, (new Boolean(vs.isValid())).toString());
                Log.customer.debug("%s ::: The ReasonCode from ValidationStatus is: %s", ClassName, vs.getReasonCode());
            }
        }
        catch(Exception e)
        {
            Log.customer.debug("%s ::: Webservice call failed due to the following exceptions: " + e.toString());
        }
        return vs;
    }

    public static String ClassName = "BusinessWebService";

}
