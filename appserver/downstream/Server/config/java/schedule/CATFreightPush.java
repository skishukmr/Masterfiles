/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the FreightPayble Object if the ActionFlag of the object IS NULL and the set the ActionFlag as Completed

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	8/01/2005 	Kingshuk	Pushing the FreightPayble object depending upon the ActionFlag of the object
	06/28/2007  Amit Kumar  Removed references to the field PO Number
*******************************************************************************************************************************************/

package config.java.schedule;

import java.util.Map;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATFreightPush extends ScheduledTask
{
	private Partition p;
	private String query;
	private String controlid, policontrolid;
	private int count, lineitemcount;
	private double total, lineitemtotal;
    private ariba.util.core.Date datetimezone = null;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;


    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Setting up the PO object and the receipt object...");
        p = Base.getSession().getPartition();
        try
        {
			isHeader = false;
			query = "select FreightsPayableEform from ariba.core.FreightsPayableEform where ActionFlag IS NULL";
            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("CATFreightsPayableEformPush");
            String eventsource = new String("ibm_traffic_freightspayablepush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");
			while(results.next())
        	{
				obj = (ClusterRoot)results.getBaseId("FreightsPayableEform").get();
				if(obj != null)
				try
				{
					obj.setFieldValue("TopicName","CATFreightsPayableEformPush");
					//if ( results.getString("PONumber") != null )
					//{
						Map userInfo = MapUtil.map(3);
						Map userData = MapUtil.map(3);
						CallCenter callCenter = CallCenter.defaultCenter();
						userInfo.put("Partition", p.getName() );
						userData.put("FreightsPayableEform", obj);

						AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
						callCenter.callAsync(topicname, userData, userInfo, listener);
						//Log.customer.debug( "Object Pushed....PONumber...." + results.getString("PONumber") );
						obj.setFieldValue("ActionFlag", "Completed");

						Base.getSession().transactionCommit();
					//}
				}
				catch(Exception e)
				{
					Log.customer.debug(e.toString());
					return;
        		}
        	    Log.customer.debug("Ending FreightPush program .....");
        	}

    	}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			return;
		}
	}

    public CATFreightPush()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}
