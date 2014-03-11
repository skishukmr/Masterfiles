/***************************************************************************************************

 12-03-2007  Amit Kumar  This class is used to populate the current preferred Ordering method of the
 						 supplier onto the supplier Eform from the supplier location object.


****************************************************************************************************/



package config.java.action;

import ariba.base.core.Base;
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

public class SupplierLookupPrefOrderMethod extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        Log.customer.debug("Entering SupplierLookupPrefOrderMethod core ...");
        String currPrefOrderMethod;
        ClusterRoot cluster = (ClusterRoot)object;
        partition = Base.getSession().getPartition();
        String tmp = cluster.getFieldValue("SupplierCode").toString();
        try
        {
            //cluster.setFieldValue("iserror", "no");
            //cluster.setFieldValue("Validate", "valid");
            ariba.util.log.Log.customer.debug(tmp);
			/*
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
			*/

			// Default the Preferred Ordering Method Field to Email if adding the supplier
			if((cluster.getFieldValue("Action").toString()).equals("Add Supplier"))
			{
				Log.customer.debug("Action is Add SupplierLookUpPrefOrderMethod");
			  	cluster.setFieldValue("PreferredOrderingMethod","Email");
		 	}

			// Set the value for Current Preferred Ordering method field
			// using Supplier Location object's Preferred Ordering Method
			else
            if((cluster.getFieldValue("Action").toString()).equals("Update"))
            {
            	Log.customer.debug("Action is Update SupplierLookUpPrefOrderMethod");
            	currPrefOrderMethod = DefaultPreferredOrderingMethod(tmp);
            	Log.customer.debug ( "Current Preferred Ordering Method is "+currPrefOrderMethod);
            	if(currPrefOrderMethod != null)
            		cluster.setFieldValue("CurrentPreferredOrderingMethod",currPrefOrderMethod);
            	else
            	{
					// Supplier object not in MSC ( new supplier) . Set Current PrefOrdMethod to empty string and default PrefOrdMethod to Email
            		cluster.setFieldValue("CurrentPreferredOrderingMethod","");
            		cluster.setFieldValue("PreferredOrderingMethod","Email");
				}


				// Default the Preferred Ordering Method field to the Current Preferred Ordering Method Value
			   if(currPrefOrderMethod.equalsIgnoreCase("URL"))
				   	cluster.setFieldValue("PreferredOrderingMethod","ASN");
			   else
			   if(currPrefOrderMethod.equalsIgnoreCase("Fax"))
				   	cluster.setFieldValue("PreferredOrderingMethod","Fax");
			   else
				   	cluster.setFieldValue("PreferredOrderingMethod","Email");

	    	}

                cluster.setFieldValue("iserror", "no");
                cluster.setFieldValue("Validate", "valid");

            ariba.util.log.Log.customer.debug("out of core");
        }
        catch(Exception e)
        {
            ariba.util.log.Log.customer.debug(e.toString());
        }
    }


   public String DefaultPreferredOrderingMethod(String id)
	{
			Log.customer.debug("Entering DefaultPreferredOrderingMethod");
			AQLQuery query=AQLQuery.parseQuery("select PreferredOrderingMethod from ariba.common.core.SupplierLocation where UniqueName like '%"+id+"%'");
			Log.customer.debug(query);
			AQLOptions options = new AQLOptions(partition);
			AQLResultCollection results=Base.getService().executeQuery(query,options);
			String prefOrderMethod = null;
			if(results.getFirstError()!=null)
			{
				// The supplier object is not in MSC . Hence PrefOrderMethod is null
				Log.customer.debug(" The Query to retrieve PrefOrdMethod returned null as supplier object is not in MSC");
				Log.customer.debug(" Error in result collection"+results.toString());
			}
			else
			{
				while(results.next())
				{
					// The Supplier is already in MSC and hence get the PrefOrdMethod from the supplier location object
					Log.customer.debug("Query to retrieve PrefOrdMethod returned the result :");
					prefOrderMethod = results.getString(0);
					Log.customer.debug(" Preferred Ordering Method is " +prefOrderMethod);
				}
			}
			Log.customer.debug(" Return to fire method");
			return prefOrderMethod;
	}

    public SupplierLookupPrefOrderMethod()
    {
    }



    //private String query;
      private Partition partition;
}
