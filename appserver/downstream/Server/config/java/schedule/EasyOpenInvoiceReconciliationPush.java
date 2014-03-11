/*********************************************************************************************************************


	Description: Pushes IR object and the ControlObjects for each IR to the EasyOpen system

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	--Initial Develipment Pushes IR object and the ControlObjects for each IR to the EasyOpen system

	08/04/08	Ashwini.M	changed the mailsubject to EasyOpenInvoiceReconciliationPush(Issue 792)

********************************************************************************************************************/

package config.java.schedule;

import java.util.Map;
import java.util.TimeZone;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;


public class EasyOpenInvoiceReconciliationPush extends ScheduledTask {

	private Partition partition;
	private String query;
	private String controlid, policontrolid;
	private int count, lineitemcount;
	private double total, lineitemtotal;
    private ariba.util.core.Date datetimezone = null;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;
    private InvoiceReconciliation  obj;
    private InvoiceReconciliation  obj1;
    private SplitAccountingCollection sacol ;
    private ariba.invoicing.core.InvoiceReconciliationLineItem invreconli;
    private java.math.BigDecimal bdTotCost;
    private int iSpAcct = 0;

    private FastStringBuffer message = null;
	String mailSubject = null;
	private int resultCount, pushedCount;
	private String startTime, endTime;

    String thisclass = "EasyOpenInvoiceReconciliationPush";

    public void run() throws ScheduledTaskException {

        Log.customer.debug("Pushing the IR objects into EasyOpen system...");

        message = new FastStringBuffer();
		mailSubject = "EasyOpenInvoiceReconciliationPush Task Status - Completed Successfully";


        partition = Base.getSession().getPartition();

        try {
			startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			isHeader = false;
			query = "select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag ='InProcess'";
            Log.customer.debug(query);
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("InvoiceReconciliationPush");
			String topicnameStr = new String("EasyOpenInvoiceReconciliationPush");

			//String topicname = new String("EasyOpenInvoiceReconciliationPush");
            String eventsource = new String("ibm_easyopen_invoicereconpush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(aqlquery, options);

			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");

            resultCount = results.getSize();
			while(results.next()) {

				obj = (ariba.invoicing.core.InvoiceReconciliation )results.getBaseId("InvoiceReconciliation").get();
				if(obj != null)

				try	{

					datetimezone = new Date();
					String invunique = (java.lang.String)obj.getFieldValue("UniqueName");
					String strunique = invunique ;
					Log.customer.debug("UniqueName Is..." + invunique);

					if (invunique.length() >= 25) {
						invunique = "ezo-" + invunique.substring(invunique.length() - 25);
					}
					else invunique = "ezo-" + invunique;

					Log.customer.debug("Last 25 Chars of the IR Is..." + invunique);
					controlid = new String (invunique);

					controlid = getDateTime(datetimezone) + controlid;

					Log.customer.debug("ControlIdentifier IS..." + controlid);
					obj.setFieldValue("ControlIdentifier",controlid);

					obj.setFieldValue( "ControlDate", datetimezone );

					//if (obj.getDottedFieldValue("TotalCost.ApproxAmountInBaseCurrency") != null)
					//	bdTotCost = (java.math.BigDecimal)obj.getDottedFieldValue("TotalCost.ApproxAmountInBaseCurrency");

					if (obj.getDottedFieldValue("TotalCost.Amount") != null)
	                 bdTotCost = (java.math.BigDecimal)obj.getDottedFieldValue("TotalCost.Amount");

					iSpAcct = 0;
					for (int i=0; i<obj.getLineItemsCount();i++) {

						invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)obj.getLineItems().get(i);

						if (invreconli.getFieldValue("Accountings") != null)
							sacol = (ariba.common.core.SplitAccountingCollection)invreconli.getFieldValue("Accountings");

						if (sacol != null && sacol.getSplitAccountings() != null)
							iSpAcct+= ((BaseVector)sacol.getSplitAccountings()).size();
					}
					obj.setFieldValue("ActionFlag", "Completed");	//Sending Control Object to EasyOpen system

					boolean isPushed = false;

					try {

						Log.customer.debug("Before Push ActionFlag Is..." + (String)obj.getFieldValue("ActionFlag"));
						obj.setFieldValue("TopicName", topicnameStr);
						Map userInfo = MapUtil.map(3);
						Map userData = MapUtil.map(3);
						CallCenter callCenter = CallCenter.defaultCenter();
						userInfo.put("Partition", partition.getName() );
						userData.put("Invoice", obj);
						userData.put("EventSource", eventsource);

						AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
						Log.customer.debug("Before Push...........");
						callCenter.callAsync(topicname, userData, userInfo, listener);
						isPushed = true;
						Log.customer.debug("After Push............");
					}
					catch(Exception ex) {
						isPushed = false;
						Log.customer.debug("In Catch..." + (String)obj.getFieldValue("ActionFlag"));
						obj.setFieldValue("ActionFlag", "InProcess");
						Log.customer.debug("In Catch After resetting the ActionFlag..." + (String)obj.getFieldValue("ActionFlag"));
						Log.customer.debug(ex.toString());
						throw ex;
					}

					if (isPushed) {
						//Sending Control Object to EasyOpen system
						sendControlObject();
						pushedCount++;
					}
				}
				catch(Exception e) {
					Log.customer.debug(e.toString());
					throw e;
        		}
        	    Log.customer.debug("Ending EasyOpenInvoiceReconciliationPush program .....");

        	}

    	}
		catch(Exception e) {
			Log.customer.debug(e.toString());
			//add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("EZOPENIRPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "EasyOpenInvoiceReconciliationPush Task Status - Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);
			new ScheduledTaskException("Error : " + e.toString(), e);
		}

		finally {
			Log.customer.debug("%s: Inside Finally ", thisclass);
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			message.append("\n");
			endTime = DateFormatter.getStringValue(new Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("Records to be pushed : "+ resultCount);
			message.append("\n");
			message.append("No. of records successfully pushed : "+ pushedCount);
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "EasyOpenIRPushNotify");
			message = null;
			pushedCount =0;
			resultCount =0;
	  }
	}

	public void sendControlObject() throws Exception
	{
		Log.customer.debug("Calling sendControlObject()...");
		String topicname1 = new String("ControlObjectPush");
		Partition p1 = Base.getService().getPartition("None");
		ClusterRoot cluster = null;
		Log.customer.debug("Create ControlPullObject... ");
		//Base.getSession().transactionBegin();
		cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p1);
		Log.customer.debug("Setting controlid ==> " + controlid);
		cluster.setFieldValue("UniqueName", controlid);
		Log.customer.debug("Setting ControlDate ==> " + datetimezone);
		cluster.setFieldValue("ControlDate", datetimezone);
		Log.customer.debug("Setting InterfaceName ...");
		cluster.setFieldValue("InterfaceName", "MSC_EZOPEN_INVOICES");
		Log.customer.debug("Setting SourceSystem ..." );
		cluster.setFieldValue("SourceSystem", "Ariba_vcsv3_ezopen");
		Log.customer.debug("Setting SourceFacility ..." );
		cluster.setFieldValue("SourceFacility", "        ");	//8 Spaces
		Log.customer.debug("Setting TargetSystem ..." );
		cluster.setFieldValue("TargetSystem", "EZOPEN");
		Log.customer.debug("Setting TargetFacility ..." );
		cluster.setFieldValue("TargetFacility", "EZOPEN");
		Log.customer.debug("Setting RecordCount ..." );
		cluster.setFieldValue("RecordCount", new Integer(1));

		if (bdTotCost != null)
		{
			bdTotCost =  bdTotCost.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
			cluster.setFieldValue("TotalAmount", bdTotCost);
			Log.customer.debug("After setFieldValue TotalAmount ..." );
		}
		//Log.customer.debug("9..........");
		cluster.setFieldValue("Area2", new Integer(iSpAcct));	//Sum of splitaccountings
		//Log.customer.debug("10..........");
		cluster.setFieldValue("Area3", "                                             ");	//48 Spaces
		//Log.customer.debug("11..........");
		cluster.save();

		if(cluster != null)
		try
		{
			//Log.customer.debug("12..........");
			CallCenter callCenter = CallCenter.defaultCenter();
			//Log.customer.debug("13..........");
			Map userInfo1 = MapUtil.map(3);
			//Log.customer.debug("14..........");
			Map userData1 = MapUtil.map(3);
			//Log.customer.debug("15..........");
			userInfo1.put("Partition", p1.getName());
			//Log.customer.debug("16..........");

			userData1.put("EventSource", "ibm_easyopen_controlpush");
			//Log.customer.debug("17..........");
			userData1.put("ControlPullObject", cluster);
			//Log.customer.debug("18..........");
			AribaPOERPReplyListener listener1 = new AribaPOERPReplyListener(cluster.getBaseId(),
			                                                         ObjectServer.objectServer());
			//Log.customer.debug("19..........");

			Log.customer.debug("Before Calling ControlObjectPush" + topicname1 + " " +
			                         userData1.toString() + " " + userInfo1.toString() + " " +
			                         listener1.toString());
			callCenter.callAsync(topicname1, userData1, userInfo1, listener1);

			Log.customer.debug("Called IBM IE....ControlObjectPush");
			//Base.getSession().transactionCommit();
			Log.customer.debug("Object Pushed....");
			if (cluster == null)
					Log.customer.debug("Object is null after the push....");
		}
		catch(Exception e)
		{
			if (cluster == null)
					Log.customer.debug("Cluster is null after the push....");
			Log.customer.debug(e.toString());
			throw e ;
		}

		Base.getSession().transactionCommit();
	}

	String getDateTime(Date datetime)
	{
		int yy = (new Integer(Date.getYear(datetime))).intValue();
		int mm = (new Integer(Date.getMonth(datetime))).intValue();
		int dd = (new Integer(Date.getDayOfMonth(datetime))).intValue();
		int hh = (new Integer(Date.getHours(datetime))).intValue();
		int mn = (new Integer(Date.getMinutes(datetime))).intValue();
		int ss = (new Integer(Date.getSeconds(datetime))).intValue();
		mm++;
		String retstr = new String ("");
		retstr = retstr + yy;

		if ( mm/10 == 0)	retstr = retstr + "0" + mm;
		else	retstr = retstr + mm;

		if ( dd/10 == 0)	retstr = retstr + "0" + dd;
		else	retstr = retstr + dd;

		if ( hh/10 == 0)	retstr = retstr + "0" + hh;
		else	retstr = retstr + hh;

		if ( mn/10 == 0)	retstr = retstr + "0" + mn;
		else	retstr = retstr + mn;

		if ( ss/10 == 0)	retstr = retstr + "0" + ss;
		else	retstr = retstr + ss;

		return retstr;
    }

    public EasyOpenInvoiceReconciliationPush()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}
