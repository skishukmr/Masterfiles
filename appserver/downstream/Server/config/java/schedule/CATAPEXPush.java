/****************************************************************************************************************

Description: Push all Invoice Reconciliations where the ActionFlag = 'Completed' and APEXFlag is null.
             After send update the APEXFlag = 'Completed'


******************************************************************************************************************/

package config.java.schedule;

import java.util.Map;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATAPEXPush extends ScheduledTask
{
	private Partition partition;
	private String query;
	private ariba.util.core.Date datetimezone = null;
    private ariba.util.core.Date curdate;
    private ariba.util.core.Date startdate;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null;



    public void run() throws ScheduledTaskException {

        Log.customer.debug("Pushing the Receipt objects into Apex system...");
        partition = Base.getSession().getPartition();

        try {
			curdate = new Date();
			startdate = new Date();
			Date.addMonths(startdate , -1);
			Log.customer.debug("CurDate: " + curdate);
			Log.customer.debug("Startdate: " + startdate);

			String strstart = "Date('" + Date.getYear(startdate)
			+ "-" + Date.getMonth(startdate)
			+ "-" + Date.getDayOfMonth(startdate) + " 00:00:00 GMT')";

			String strcur = "Date('" + Date.getYear(curdate)
							+ "-" + (Date.getMonth(curdate) + 1)
			+ "-" + Date.getDayOfMonth(curdate) + " 00:00:00 GMT')";

			Log.customer.debug("CurDate: " + strstart);
			Log.customer.debug("Startdate: " + strcur);

			//query = "Select from ariba.invoicing.core.InvoiceReconciliation "
			//			+" where ActionFlag ='Completed' AND APEXActionFlag is null ";

            query = "Select from ariba.invoicing.core.InvoiceReconciliation "
						+" where ActionFlag ='Completed' AND APEXFlag is null ";


            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = AQLQuery.parseQuery(query);
			AQLOptions options = new AQLOptions(partition);
			AQLResultCollection results = null;

			String topicname = new String("InvoiceReconciliationPush");
			String topicnameStr = new String("APEXInvoiceReconciliationPush");
            String eventsource = new String("ibm_apex_invoicereconpush");

			results = Base.getService().executeQuery(aqlquery, options);

			if(results.getErrors() != null){
				Log.customer.debug("ERROR GETTING RESULTS in Results");
				throw new ScheduledTaskException("ERROR GETTING RESULTS="+results.getErrorStatementText());
			}

			int cnt = 0;
			int size = results.getSize();

			while(results.next()) {
				obj = (ClusterRoot)results.getBaseId("InvoiceReconciliation").get();

				if(obj != null)
				try	{
					cnt++;
					size--;
					obj.setFieldValue("TopicName", topicnameStr);
					Map userInfo = MapUtil.map(3);
					Map userData = MapUtil.map(3);
					obj.setFieldValue("APEXFlag", "Completed");
					CallCenter callCenter = CallCenter.defaultCenter();
					userInfo.put("Partition", partition.getName() );
					userData.put("Invoice", obj);
					userData.put("EventSource", eventsource);
					AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
					callCenter.callAsync(topicname, userData, userInfo, listener);
					Log.customer.debug( "Object Pushed...." + obj.getDottedFieldValue("UniqueName") );
				}
				catch(Exception e) {
					obj.setFieldValue("APEXFlag", null);
					Log.customer.debug(e.toString());
        		}
				//commit every 200 records and reset counter
				if(cnt == 200) {
					Log.customer.debug("RevertFlag: Commit and reset at 200 ");
					Log.customer.debug("Remaining records to process="+ size);
					Base.getSession().transactionCommit();
					cnt=0;
				}
        	    Log.customer.debug("Pushing IR inside the CATAPEXPush program .....");
        	}
    	}
		catch(Exception e) {
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error: "+ e.toString(), e);
		}
		Base.getSession().transactionCommit();
        Log.customer.debug("Ending CATAPEXPush program .....");
	}

    public CATAPEXPush() {
        datetimezone = null;
    }
}
