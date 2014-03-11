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

public class SupplierLookUpCurrentPrefOrderMethodUS extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        Log.customer.debug("Entering SupplierLookup core ...");
        ClusterRoot cluster = (ClusterRoot)object;
        partition = Base.getSession().getPartition();
        String tmp = cluster.getFieldValue("SupplierCode").toString();
        try
        {
            cluster.setFieldValue("iserror", "no");
            cluster.setFieldValue("Validate", "valid");
            ariba.util.log.Log.customer.debug(tmp);

            if((cluster.getFieldValue("Action").toString()).equals("Update"))
	    	{
	           Log.customer.debug("Action is Update");
	           String retValue = DefaultPreferredOrderingMethod(tmp);
	           Log.customer.debug ( "Current Preferred Ordering Method is "+retValue);
	           cluster.setFieldValue("CurrentPreferredOrderingMethod",retValue);
	    	}
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
    			String prefOrdMethod = null;
    			if(results.getFirstError()!=null)
    			{
    				Log.customer.debug(" Error in result collection"+results.toString());
    			}
    			else
    			{
    				while(results.next())
    				{
    					prefOrdMethod = results.getString(0);
    					Log.customer.debug(" Preferred Ordering Method is " +prefOrdMethod);
    				}
    			}
    			Log.customer.debug(" Return to fire method");
    			return prefOrdMethod;
    	}

        public SupplierLookUpCurrentPrefOrderMethodUS()
        {
        }



        //private String query;
          private Partition partition;
    }
