/*****************************************************************
Creator: Kingshuk Mazumdar
Description: Pushes Hazmat receipts to CAPS.
ChangeLog:
Date		Name		Description
09-29-06    Kannan      Receipts for contract is added in Select query Ref R4-CR59
09-29-06    Kannan      ScheduledTaskException are handled.

*******************************************************************/

package config.java.schedule.sap;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptItem;
import ariba.util.core.MapUtil;
import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

/*
 * AUL-sdey : This code need to test at onsite
 * 			  Need to revisit
 */

public class CAPSHazmatPush extends ScheduledTask {
	private Partition partition;
	private String query = null;
	private String query1 = null;
	boolean isHeader = false;
	private static final String thisclass = "CAPSHazmatPush: ";


	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
					super.init(scheduler, scheduledTaskName, arguments);
					for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
						String key = (String)e.next();
						if (key.equals("queryST")) {
						Log.customer.debug("queryST");
							query  = (String)arguments.get(key);
			}
		}
	 }

    public void run() throws ScheduledTaskException {
        Log.customer.debug("beginning CAPSHazmatPush...",thisclass);
        partition = Base.getSession().getPartition();
        try {

            Log.customer.debug("%s %s",query,thisclass);
            Receipt receipt = null;
            AQLQuery aqlquery = null;
            AQLQuery aqlquery1 = null;
			AQLOptions options = null;
			AQLOptions options1 = null;
			AQLResultCollection results = null;
			AQLResultCollection results1 = null;
			BaseId baseId = null;
			String topicname = new String("ReceiptPush");
            String eventsource = new String("ibm_epoc_hazmatpush");

			aqlquery = AQLQuery.parseQuery(query);
			Log.customer.debug("aqlquery=>"+aqlquery);
			Log.customer.debug("partition=>"+partition);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(aqlquery, options);

			if( (results != null) && (!results.isEmpty()))
			{
			while (results.next()) {
				query1=(String) results.getString(0);
				Log.customer.debug("query1 from staging table=>"+query1);
			}
			}

			// Parsing the staging query for receipt process
			aqlquery1 = AQLQuery.parseQuery(query1);
			Log.customer.debug("aqlquery1=>"+aqlquery1);
			options1 = new AQLOptions(partition);
			results1 = Base.getService().executeQuery(aqlquery1, options1);

			if( (results1 != null) && (!results1.isEmpty()))
			{
			while (results1.next()) {
				Log.customer.debug("Inside loop : Processing receipts one by one, Total number of receipts =>"+ results1.getSize());
				// Getting Receipt from the result set
				receipt = (Receipt) results1.getBaseId(0).get();
				Log.customer.debug("Receipt => "+ receipt);
				if(receipt != null) {
					if ((receipt.getFieldValue("HazmatFlag") == null) &&
					    (receipt.getFieldValue("StatusString").toString().equals("Approved"))) {
						try	{
	ariba.base.core.BaseVector bv = (BaseVector)receipt.getDottedFieldValue("ReceiptItems");
	for (Iterator it=bv.iterator(); it.hasNext(); )	{
			ReceiptItem receiptItem = (ReceiptItem)it.next();
		if (receiptItem.getDottedFieldValue("LineItem.HazmatWeight") != null) {
			BigDecimal hazmatweight = (BigDecimal)receiptItem.getDottedFieldValue("LineItem.HazmatWeight");
			receiptItem.setFieldValue("HazmatWeight", hazmatweight);
		}
	}
	//receipt.setFieldValue("ActionFlag", "Completed");
	//receipt.setFieldValue("HazmatFlag", "Completed");

	Map userInfo = MapUtil.map(3);
	Map userData = MapUtil.map(3);
	CallCenter callCenter = CallCenter.defaultCenter();
	userInfo.put("Partition", partition.getName() );
	userData.put("Receipt", receipt);
	receipt.setFieldValue("TopicName", "HazmatPush");
	AribaPOERPReplyListener listener = new AribaPOERPReplyListener(receipt.getBaseId(),ObjectServer.objectServer());
	Log.customer.debug("Listener = > "+listener);
	//Commented out by Garima because WBI is not ready.Put it when WBI is ready.
	//callCenter.callAsync(topicname, userData, userInfo, listener);
	Log.customer.debug("topicname = > "+topicname);
	Log.customer.debug("userData = > "+userData);
	Log.customer.debug("userInfo = > "+userInfo);
	Log.customer.debug("listener = > "+listener);
	callCenter.callAsync(topicname, userData, userInfo, listener);
	Log.customer.debug("%s %s", receipt.getFieldValue("UniqueName"),thisclass);
	Log.customer.debug(receipt.getDottedFieldValue("ReceiptItems.HazmatWeight"));
	receipt.setFieldValue("ActionFlag", "Completed");
	receipt.setFieldValue("HazmatFlag", "Completed");
	}catch(Exception e) {
	receipt.setFieldValue("HazmatFlag", null);
	receipt.setFieldValue("ActionFlag", null);
	Log.customer.debug("%s %s", e.toString(), thisclass);
	throw e;
	}
					}
				}
        	}
        	Log.customer.debug("%s %s", "Ending CAPSHazmatPush .....", thisclass);
    	}
 }
		catch(Exception e) {
			Log.customer.debug("%s %s", e.toString(), thisclass);
			throw new ScheduledTaskException("Error : " + e.toString(), e);
		}
	}
 public CAPSHazmatPush() {
    }
}

