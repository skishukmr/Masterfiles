package config.java.action;

import java.net.URL;

import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.atm.service.SecureMySupplyCabinetServiceSoapBindingStub;
import cat.cis.atm.service.SupplierInformation;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorWebServiceServiceLocator;

public class SupplierLookup extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        Log.customer.debug("Entering SupplierLookup core ...");
        ClusterRoot cluster = (ClusterRoot)object;
        /* AUL : get the partition from cluster itself */
        //partition = Base.getService().getPartition();
        partition = cluster.getPartition();

        String tmp = cluster.getFieldValue("SupplierCode").toString();
        try
        {
            cluster.setFieldValue("iserror", "no");
            cluster.setFieldValue("Validate", "valid");
            ariba.util.log.Log.customer.debug(tmp);
            //String theEndPoint = "https://atm.cat.com/atm/services/MySupplyCabinetService";
            String theEndPointDefault = "https://atm.cat.com/atm/services/MySupplyCabinetService";
            String theEndPointStr = ResourceService.getString("cat.ws.util","TaxWSCalltheEndPoint");
            Log.customer.debug("Value Taken from resource String : "+ theEndPointStr);
			String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;
			Log.customer.debug("SupplierLookup.fire theEndPoint:"+ theEndPoint);

            URL endpoint = new URL(theEndPoint);
            SecureMySupplyCabinetServiceSoapBindingStub stub = new SecureMySupplyCabinetServiceSoapBindingStub(endpoint, null);
            AuthenticatorWebServiceServiceLocator authLocator = new AuthenticatorWebServiceServiceLocator();
            String urlString = "https://tufws.cat.com/TUFWebServices/services/authenticator";
            AuthenticatorSoapBindingStub webservice = new AuthenticatorSoapBindingStub(new URL(urlString), authLocator);
            String credential = webservice.authenticate("aribaws", "q37NrANaKc");
            ariba.util.log.Log.customer.debug(credential);
            stub.setCredential(credential);
            SupplierInformation si = stub.getSupplierInformation(tmp);
            ariba.util.log.Log.customer.debug(si.getNameLine1());
            ariba.util.log.Log.customer.debug(si.getNameLine2());
            ariba.util.log.Log.customer.debug(si.getFaxNumber());
            cluster.setFieldValue("SupplierName", si.getNameLine1());
            cluster.setFieldValue("SupplierFaxNumber", si.getFaxNumber());
            cluster.setFieldValue("Address1", si.getAddress1());
            cluster.setFieldValue("Address2", si.getAddress2());
            cluster.setFieldValue("City", si.getCity());
            cluster.setFieldValue("State", si.getState());
            cluster.setFieldValue("Zip", si.getZip());
            ariba.util.log.Log.customer.debug(si.getErrorMessage());
            if(!(si.getErrorMessage().equals(null) | (si.getErrorMessage().toString().trim() == "") | (si.getErrorMessage() == null) | si.getErrorMessage().equals("")))
            {
                ariba.util.log.Log.customer.debug("in error");
                cluster.setFieldValue("iserror", "yes");
                cluster.setFieldValue("Validate", "invalid");
                cluster.setFieldValue("SupplierError", si.getErrorMessage());
            } else
            if(!(si.getNameLine2().equals(null) | (si.getNameLine2().toString().trim() == "") | (si.getNameLine2() == null) | si.getNameLine2().equals("")))
            {
                ariba.util.log.Log.customer.debug("in validate");
                cluster.setFieldValue("iserror", "no");
                cluster.setFieldValue("Validate", "invalid");
                cluster.setFieldValue("SupplierError", si.getNameLine2());
            } else
            {
                ariba.util.log.Log.customer.debug("in no errors");
                cluster.setFieldValue("iserror", "no");
                cluster.setFieldValue("Validate", "valid");
            }
            ariba.util.log.Log.customer.debug("out of core");
        }
        catch(Exception e)
        {
            ariba.util.log.Log.customer.debug(e.toString());
        }
    }

    public SupplierLookup()
    {
    }

    private String query;
    private AQLQuery qry;
    private AQLOptions options;
    private AQLResultCollection results;
    private Partition partition;
}
