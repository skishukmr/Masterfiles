/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushes IR to DW system

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	9/14/2005 	Kingshuk	--Initial Development Pushes IR to DW system

*******************************************************************************************************************************************/

package config.java.schedule.vcsv2;

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

public class DWMFGInvoicePush extends ScheduledTask
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
        Log.customer.debug("Pushing the IR objects into CAPS system...");
        p = Base.getSession().getPartition();
        try
        {
			isHeader = false;
			query = "select from ariba.invoicing.core.InvoiceReconciliation where DWInvoiceFlag = 'InProcess'";
            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("InvoiceReconciliationPush");
            String eventsource = new String("ibm_mfg_invoicereconpush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");
			while(results.next())
        	{
				obj = (ariba.invoicing.core.InvoiceReconciliation)results.getBaseId("InvoiceReconciliation").get();
				if(obj != null)
				try
				{
						Map userInfo = MapUtil.map(3);
						Map userData = MapUtil.map(3);
						CallCenter callCenter = CallCenter.defaultCenter();
						userInfo.put("Partition", p.getName() );
						userData.put("Invoice", obj);
						userData.put("EventSource", eventsource);

						AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
						callCenter.callAsync(topicname, userData, userInfo, listener);
						obj.setFieldValue("DWInvoiceFlag", "Completed");


				}
				catch(Exception e)
				{
					Log.customer.debug(e.toString());
					return;
        		}
        		Base.getSession().transactionCommit();
        	    Log.customer.debug("Ending MFGDWInvoicePush program .....");
        	}

    	}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			return;
		}
	}

    public DWMFGInvoicePush()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}