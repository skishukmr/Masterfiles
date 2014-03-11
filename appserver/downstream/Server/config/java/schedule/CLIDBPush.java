/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushes PO to the CLIDB system for the US

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------------------------------------------------
	9/14/2005 	Kingshuk	Pushes PO to the CLIDB system for the US

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
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CLIDBPush extends ScheduledTask
{
	private Partition p;
	private String query;
	private String controlid, policontrolid;
	private int count, lineitemcount;
	private double total, lineitemtotal;
    private ariba.util.core.Date datetimezone = null;
    private ariba.util.core.Date curdate;
    private ariba.util.core.Date startdate;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;
    private Object requesterName = null;


    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Pushing the Receipt objects into CAPS system...");
        p = Base.getSession().getPartition();
        try
        {
			isHeader = false;
			curdate = new Date();
			startdate = new Date();
			Date.addMonths(startdate , -1);
			Log.customer.debug("CurDate: " + curdate);
			Log.customer.debug("Startdate: " + startdate);
			String strstart = "Date('" + Date.getYear(startdate) + "-" + Date.getMonth(startdate) + "-" + Date.getDayOfMonth(startdate) + " 00:00:00 GMT')";
			String strcur = "Date('" + Date.getYear(curdate) + "-" + (Date.getMonth(curdate) + 1) + "-" + Date.getDayOfMonth(curdate) + " 00:00:00 GMT')";
			Log.customer.debug("CurDate: " + strstart);
			Log.customer.debug("Startdate: " + strcur);
			//query = "select from ariba.purchasing.core.DirectOrder where CLIDBPOFlag IS NULL and CreateDate > " + strstart + " and CreateDate < " + strcur;
			//query = "select from ariba.purchasing.core.DirectOrder where StatusString <> 'Receiving' AND StatusString <> 'Received' AND StatusString <> 'Canceling' AND StatusString <> 'Canceled'";
			query = "select from ariba.purchasing.core.DirectOrder where StatusString = 'Receiving' OR StatusString = 'Ordered'";
            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("PurchaseOrderPush");
            String eventsource = new String("ibm_dw_popush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");
			while(results.next())
        	{
				obj = (ClusterRoot)results.getBaseId("DirectOrder").get();
				if(obj != null)
				try
				{
					obj.setFieldValue("TopicName", "CLIDBPush");

					if ( obj.getFieldValue("RequesterName") == null) {
						requesterName = obj.getDottedFieldValue("LineItems[0].Requisition.Requester.Name.PrimaryString");
						obj.setFieldValue("RequesterName",requesterName.toString());
					}


					Map userInfo = MapUtil.map(3);
					Map userData = MapUtil.map(3);
					obj.setFieldValue("CLIDBPOFlag", "Completed");

					CallCenter callCenter = CallCenter.defaultCenter();
					userInfo.put("Partition", p.getName() );
					userData.put("DirectOrder", obj);

					AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
					callCenter.callAsync(topicname, userData, userInfo, listener);
					Log.customer.debug( "Object Pushed...." + obj.getDottedFieldValue("UniqueName") );
				}
				catch(Exception e)
				{
					obj.setFieldValue("CLIDBPOFlag", null);
					Log.customer.debug(e.toString());
					throw new Exception("Error CLIDBPush:", e);
        		}
			Base.getSession().transactionCommit();
        	    Log.customer.debug("Pushing POs inside the CLIDBPush program .....");
        	}

    	}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error CLIDBPush:", e);
		}
        	    Log.customer.debug("Ending CLIDBPush program .....");
	}

    public CLIDBPush()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}
