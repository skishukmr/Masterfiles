
package config.java.common;

import java.net.URL;

import ariba.base.core.Base;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.atm.service.SecureMySupplyCabinetServiceSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorWebServiceServiceLocator;


public class SupplierDownload
{

    public void setSupplierCode(String value)
    {
        stubSupplierCode = value;
    }

    public String getResponse()
    {
        try
        {
            //String theEndPoint = "https://atm.cat.com/atm/services/MySupplyCabinetService";
             String theEndPointDefault = "https://atm.cat.com/atm/services/MySupplyCabinetService";

            String theEndPointStr = ResourceService.getString("cat.ws.util","TaxWSCalltheEndPoint");
            Log.customer.debug("Value Taken from resource String : "+ theEndPointStr);

	        String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;
	        Log.customer.debug("SupplierDownload.getResponse endPoint used: "+ theEndPoint);

            URL endpoint = new URL(theEndPoint);
            SecureMySupplyCabinetServiceSoapBindingStub stub = new SecureMySupplyCabinetServiceSoapBindingStub(endpoint, null);
            AuthenticatorWebServiceServiceLocator authLocator = new AuthenticatorWebServiceServiceLocator();
            String urlString = "https://tufws.cat.com/TUFWebServices/services/authenticator";
            AuthenticatorSoapBindingStub webservice = new AuthenticatorSoapBindingStub(new URL(urlString), authLocator);
            String credential = webservice.authenticate("aribaws", "q37NrANaKc");
            Log.customer.debug(credential);
            stub.setCredential(credential);
            response = stub.requestSupplierDownload(stubFacilityCode, stubSupplierCode);
            Log.customer.debug(response);
            Log.customer.debug("out of core");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
        }
        return response;
    }

    public SupplierDownload()
    {
        stubSupplierCode = null;
        stubFacilityCode = Base.getService().getParameter(null, "System.Base.SupplierStubFacilityCode");
        Log.customer.debug("stubFacilityCode read from Parameter table " + stubFacilityCode);
        response = "Error connecting to webservice";
    }

    private String query;
    private AQLQuery qry;
    private AQLOptions options;
    private AQLResultCollection results;
    private Partition partition;
    private String stubSupplierCode;
    private String stubFacilityCode;
    private String response;
}
