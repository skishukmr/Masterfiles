/**************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the VoucherEform Object if the ActionFlag of the object IS NULL and Status is Approved
				 and the set the ActionFlag as Completed

	ChangeLog:
	Date		Name		History
	-----------------------------------------------------------------------------------------------------------
	4/18/2005 	Kingshuk	Pushing the VoucherEform object depending upon the ActionFlag and the StatusString of
							the object

****************************************************************************************************************/

package config.java.schedule;

import java.util.Map;
import java.util.TimeZone;

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
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;


public class CATVoucherPush extends ScheduledTask {
	private Partition p;
	private String query;
	private String controlid, policontrolid;
    private ariba.util.core.Date datetimezone = null;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;
    private java.math.BigDecimal bdTotCost;
    private String vsupplier = null;
    private String vefobj = "";
    private static final String thisclass = "CATVoucherPush: ";
    private FastStringBuffer message = null;
    String mailSubject = null;
    private int resultCount, pushedCount;
    private String startTime, endTime;

    public void run() throws ScheduledTaskException {
        Log.customer.debug("%s Starting to push Voucher object...",thisclass);
        p = Base.getSession().getPartition();
        ClusterRoot obj = null;

        message = new FastStringBuffer();
        mailSubject = "CATVoucherPush Task Completion Status - Completed Successfully";

        try {
			startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			isHeader = false;
			query = "select from config.java.vcsv1.vouchereform.VoucherEform " +
			                     " where StatusString = 'Approved' and ActionFlag IS NULL";
            Log.customer.debug("%s " + query,thisclass);


            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("CAPSVoucherPush");
            String eventsource = new String("ibm_caps_voucherpush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			if(results.getErrors() != null)
				Log.customer.debug("%s ERROR GETTING RESULTS in Results",thisclass);

			resultCount = results.getSize();

			while(results.next()) {
				obj = (ClusterRoot)results.getBaseId("VoucherEform").get();

				if(obj != null) {
					vefobj = obj.getUniqueName();
					boolean isPushed = false;
					datetimezone = new Date();
					obj.setFieldValue( "ControlDate", datetimezone );

					if (obj.getDottedFieldValue("TotalInvoiced.ApproxAmountInBaseCurrency") != null) {
						//bdTotCost = (java.math.BigDecimal)obj.getDottedFieldValue("TotalInvoiced.ApproxAmountInBaseCurrency");
						bdTotCost = (java.math.BigDecimal)obj.getDottedFieldValue("TotalInvoiced.Amount");
					}

					if (obj.getDottedFieldValue("VoucherSupplier.UniqueName") != null ) {
						vsupplier = (String)obj.getDottedFieldValue("VoucherSupplier.UniqueName");
				    }

					if (obj.getFieldValue("InvoiceNumber") != null) {
						String invunique = (java.lang.String)obj.getFieldValue("InvoiceNumber");
						Log.customer.debug("%s InvoiceNumber Is..." + invunique,thisclass);

						if (invunique.length() > 24) {
							//invunique = "pcsv1-" + invunique.substring(invunique.length() - 18);
							invunique = invunique.substring(0,24);
						}
						//else invunique = "pcsv1-" + invunique;
						else invunique = invunique;

						Log.customer.debug("%s Last 18 Chars of the IR Is..." + invunique,thisclass);

						invunique = getDateTime(datetimezone) + invunique;
						controlid = new String (invunique);
						Log.customer.debug("%s ControlIdentifier IS..." + controlid, thisclass);
						obj.setFieldValue("ControlIdentifier", controlid);
					}

					if ( ( obj.getFieldValue("ControlIdentifier") != null ) &&
					                 ( obj.getFieldValue( "ControlDate") != null ) ) {

						try	{
							String vname = (String)obj.getFieldValue("UniqueName");
							//obj.setFieldValue("TopicName",topicname);
							obj.setFieldValue("ActionFlag", "Completed");
							Map userInfo = MapUtil.map(3);
							Map userData = MapUtil.map(3);
							CallCenter callCenter = CallCenter.defaultCenter();
							userInfo.put("Partition", p.getName() );
							userData.put("EventSource", eventsource);
							userData.put("Voucher", obj);

							AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(),
							                                                                ObjectServer.objectServer());
							callCenter.callAsync(topicname, userData, userInfo, listener);
							Log.customer.debug("%s Voucher Object Pushed...." + vname , thisclass);


							Base.getSession().transactionCommit();
							isPushed = true;
						}
						catch(Exception e) {
							obj.setFieldValue("ActionFlag", null);
							Log.customer.debug("%s %s",e.toString(), thisclass);
							throw e;

						}
					}

					if (isPushed) {
						sendControlObject();
						pushedCount++;

					}
				}
        	}
        	Log.customer.debug("%s Ending VoucherPush program .....", thisclass);


    	}
		catch(Exception e) {
			Log.customer.debug("%s %s", e.toString() , thisclass);
			//add message

			message.append("Task start time : "+ startTime);
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("No of records pushed : "+ pushedCount);
			message.append("\n");
			message.append("No of records queued  :"+ (resultCount - pushedCount));
			message.append("\n");
			message.append("VoucherPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CATVoucherPush Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);

			throw new ScheduledTaskException("Error : VoucherEform="+ vefobj +" ***If Requisition field has FatalAssertionException, set to null and Repush***" +obj.toString() + e.toString(), e);
		}

		finally {
			Log.customer.debug("%s: Inside Finally ", thisclass);
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("\n");
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("Records to be pushed : "+ resultCount);
			message.append("\n");
			message.append("No. of records successfully pushed : "+ pushedCount);
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CATVoucherPushNotify");
			message = null;
			pushedCount =0;
			resultCount =0;
		}

	}

	public void sendControlObject()	throws Exception{
		Log.customer.debug("%s Inside Control Push...", thisclass);
		String topicname1 = new String("ControlObjectPush");
		Partition p1 = Base.getService().getPartition("None");
		Log.customer.debug("%s  0.1..........", thisclass);
		ClusterRoot cluster = null;
		Log.customer.debug("%s 0.1..........", thisclass);
		cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p1);
		cluster.setFieldValue("UniqueName", controlid);
		cluster.setFieldValue("ControlDate", datetimezone);
		cluster.setFieldValue("InterfaceName", "MSC_CAPS_MnBM_VOUCHERS");
		Log.customer.debug("%s 4..........", thisclass);
		cluster.setFieldValue("SourceSystem", "Ariba_vcsv1_pcsv1");
		Log.customer.debug("%s 5..........", thisclass);
		cluster.setFieldValue("SourceFacility", "        ");	//8 Spaces
		Log.customer.debug("%s 6..........", thisclass);
		cluster.setFieldValue("TargetSystem", "CAPS");
		Log.customer.debug("%s 7..........", thisclass);
		cluster.setFieldValue("TargetFacility", "CAPS");
		Log.customer.debug("%s 8..........", thisclass);
		cluster.setFieldValue("RecordCount", new Integer(1));

		if (bdTotCost != null) {
			bdTotCost =  bdTotCost.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
			cluster.setFieldValue("TotalAmount", bdTotCost);
		}
		Log.customer.debug("%s 9..........", thisclass);
		cluster.setFieldValue("Area2", new Integer(1));	//Sum of splitaccountings
		Log.customer.debug("10..........", thisclass);
		if (vsupplier != null)
			cluster.setFieldValue("Area3", vsupplier);	//48 Spaces
		Log.customer.debug("%s 11..........", thisclass);
		cluster.save();

		if(cluster != null)
		try {
			CallCenter callCenter = CallCenter.defaultCenter();
			Map userInfo1 = MapUtil.map(3);
			Map userData1 = MapUtil.map(3);
			userInfo1.put("Partition", p1.getName());
			userData1.put("EventSource", "ibm_mfg_controlpush");
			userData1.put("ControlPullObject", cluster);
			AribaPOERPReplyListener listener1 = new AribaPOERPReplyListener(cluster.getBaseId(),
			                                                                   ObjectServer.objectServer());
			callCenter.callAsync(topicname1, userData1, userInfo1, listener1);
			Log.customer.debug("%s Before Calling ControlObjectPush" + topicname1 + " " +
			                      userData1.toString() + " " + userInfo1.toString() + " " + listener1.toString(), thisclass);

			Log.customer.debug("%s Called IBM IE....ControlObjectPush", thisclass);
			//Base.getSession().transactionCommit();
			Log.customer.debug("%s Object Pushed....", thisclass);
		}
		catch(Exception e) {
			Log.customer.debug("%s %s",e.toString(), thisclass);
			throw e;
		}

		Base.getSession().transactionCommit();
	}

	String getDateTime(Date datetime) {
		int yy = (new Integer(Date.getYear(datetime))).intValue();
		int mm = (new Integer(Date.getMonth(datetime))).intValue();
		int dd = (new Integer(Date.getDayOfMonth(datetime))).intValue();
		int hh = (new Integer(Date.getHours(datetime))).intValue();
		int mn = (new Integer(Date.getMinutes(datetime))).intValue();
		int ss = (new Integer(Date.getSeconds(datetime))).intValue();
		mm++;
		String retstr = new String ("");
		retstr = retstr + yy;

		if ( mm/10 == 0)
		   retstr = retstr + "0" + mm;
		else
		   retstr = retstr + mm;

		if ( dd/10 == 0)
		   retstr = retstr + "0" + dd;
		else
		   retstr = retstr + dd;

		if ( hh/10 == 0)
		   retstr = retstr + "0" + hh;
		else
		   retstr = retstr + hh;

		if ( mn/10 == 0)
		   retstr = retstr + "0" + mn;
		else
		   retstr = retstr + mn;

		if ( ss/10 == 0)
		   retstr = retstr + "0" + ss;
		else
		   retstr = retstr + ss;

		return retstr;
    }

    public CATVoucherPush() {
        datetimezone = null;
    }
}
