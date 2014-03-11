/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushes IR object and the ControlObjects for each IR to the CAPS system

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	9/14/2005 	Kingshuk	--Initial Develipment Pushes IR object and the ControlObjects for each IR to the CAPS system

	30/01/2009  Dibya Prakash --  Additional null check to CapsLineNumber field.--Issue 865

*******************************************************************************************************************************************/

package config.java.schedule;

import java.math.BigDecimal;
import java.util.Iterator;
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
import ariba.common.core.SplitAccounting;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.MapUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import config.java.common.CatEmailNotificationUtil;
public class CAPSInvoiceReconciliationPush extends ScheduledTask
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
    private ariba.invoicing.core.InvoiceReconciliation  obj;
    private ariba.invoicing.core.InvoiceReconciliation  obj1;
    private ariba.common.core.SplitAccountingCollection sacol ;
    private ariba.invoicing.core.InvoiceReconciliationLineItem invreconli;
    private java.math.BigDecimal bdTotCost;
    private int iSpAcct = 0;

    private Integer iCAPSLineNo;
	private String sCAPSLineNo;
	private SplitAccounting splitAcc;
	private boolean IsForeign;
	private BigDecimal totamt,InvoiceSplitDiscountDollarAmount,TotalInvoiceAmountMinusDiscount;



    private FastStringBuffer message = null;
	private String mailSubject = null;
	private int resultCount, pushedCount;
    private String startTime, endTime;
    private String thisclass ="CAPSInvoiceReconciliationPush";

    public void run() throws ScheduledTaskException
    {
        Log.customer.debug("Pushing the IR objects into CAPS system...");
        p = Base.getSession().getPartition();
        message = new FastStringBuffer();
        mailSubject = "CAPSInvoiceReconciliationPush Task Completion Status - Completed Successfully";
        try
        {
			startTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			isHeader = false;
			query = "select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag ='InProcess'";
            Log.customer.debug(query);
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			String topicname = new String("InvoiceReconciliationPush");
            String eventsource = new String("ibm_caps_invoicereconpush");

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);

			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");

			resultCount = results.getSize();
			while(results.next())
        	{
				obj = (ariba.invoicing.core.InvoiceReconciliation )results.getBaseId("InvoiceReconciliation").get();
				if(obj != null)
				try
				{
					datetimezone = new Date();
					String invunique = (java.lang.String)obj.getFieldValue("UniqueName");
					String strunique = invunique ;
					Log.customer.debug("UniqueName Is..." + invunique);
					if (invunique.length() >= 18)
					{
						invunique = "pcsv1-" + invunique.substring(invunique.length() - 18);
					}
					else invunique = "pcsv1-" + invunique;

					Log.customer.debug("Last 24 Chars of the IR Is..." + invunique);
					controlid = new String (invunique);

					controlid = getDateTime(datetimezone) + controlid;
					Log.customer.debug("ControlIdentifier IS..." + controlid);
					obj.setFieldValue("ControlIdentifier",controlid);

					obj.setFieldValue( "ControlDate", datetimezone );

					if (obj.getDottedFieldValue("TotalCost.ApproxAmountInBaseCurrency") != null)
						bdTotCost = (java.math.BigDecimal)obj.getDottedFieldValue("TotalCost.ApproxAmountInBaseCurrency");

					iSpAcct = 0;
					for (int i=0; i<obj.getLineItemsCount();i++)
					{
						invreconli = (ariba.invoicing.core.InvoiceReconciliationLineItem)obj.getLineItems().get(i);

						if (invreconli.getFieldValue("Accountings") != null)
							sacol = (ariba.common.core.SplitAccountingCollection)invreconli.getFieldValue("Accountings");

						if (sacol != null && sacol.getSplitAccountings() != null)
							iSpAcct+= ((BaseVector)sacol.getSplitAccountings()).size();

							//Code added for issue-865

							for(Iterator s= sacol.getSplitAccountingsIterator(); s.hasNext();) {
																					  splitAcc = (SplitAccounting) s.next();
								   if (splitAcc != null) {
									   	Log.customer.debug("splitAcc IS..." + splitAcc);
									  String capsLineNumber = (String)splitAcc.getDottedFieldValue("CapsLineNumber");
									  if(capsLineNumber == null)
									  {
								                 	Log.customer.debug("CapsLineNumber is null..calling GenerateCapsLineNumber");
								              GenerateCapsLineNumber (obj);
									}
								}
							}//Code Ended for Issue 865
					}
					obj.setFieldValue("ActionFlag", "Completed");	//Sending Control Object to CAPS system

					boolean isPushed = false;

					try{
						Log.customer.debug("Before Push ActionFlag Is..." + (String)obj.getFieldValue("ActionFlag"));
						Map userInfo = MapUtil.map(3);
						Map userData = MapUtil.map(3);
						CallCenter callCenter = CallCenter.defaultCenter();
						userInfo.put("Partition", p.getName() );
						userData.put("Invoice", obj);
						//userData.put("Invoice", obj1);
						userData.put("EventSource", eventsource);

						AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
						//AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj1.getBaseId(), ObjectServer.objectServer());
						Log.customer.debug("Before Push...........");

						callCenter.callAsync(topicname, userData, userInfo, listener);
						isPushed = true;
						Log.customer.debug("After Push............");
					}
					catch(Exception ex)
					{
						isPushed = false;
						Log.customer.debug("In Catch..." + (String)obj.getFieldValue("ActionFlag"));
						obj.setFieldValue("ActionFlag", "InProcess");	//Sending Control Object to CAPS system
						Log.customer.debug("In Catch After resetting the ActionFlag..." + (String)obj.getFieldValue("ActionFlag"));
						Log.customer.debug(ex.toString());
						throw ex;
					}

					if (isPushed)
					{
						sendControlObject();
						pushedCount++;
					}
				}
				catch(Exception e)
				{
					Log.customer.debug(e.toString());
					throw e;
        		}
        	    Log.customer.debug("Ending CAPSIRPush program .....");


        	}


    	}
		catch(Exception e)
		{
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
			message.append("CAPSInvoiceReconciliationPush Failed - Exception details below");
			message.append("\n");
			message.append(e.toString());
			mailSubject = "CAPSInvoiceReconciliationPush Task Failed";
			Log.customer.debug("%s: Inside Exception message "+ message.toString() , thisclass);

			throw new ScheduledTaskException( e.toString());
		}

		finally {
			Log.customer.debug("%s: Inside Finally ", thisclass);
			message.append("Task start time : "+ startTime);
			Log.customer.debug("%s: Inside Finally added start time", thisclass);
			message.append("\n");
			endTime = DateFormatter.getStringValue(new ariba.util.core.Date(), "EEE MMM d hh:mm:ss a z yyyy", TimeZone.getTimeZone("CST"));
			message.append("Task end time : " + endTime);
			message.append("\n");
			message.append("Records to be pushed : "+ resultCount);
			message.append("\n");
			message.append("No. of records successfully pushed : "+ pushedCount);
			message.append("\n");
			Log.customer.debug("%s: Inside Finally message "+ message.toString() , thisclass);

			// Sending email
			CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CAPSIRPushNotify");
			message = null;
			pushedCount =0;
			resultCount =0;
		}
	}

	public void sendControlObject() throws Exception
	{
		Log.customer.debug("Inside Control Push...");
		String topicname1 = new String("ControlObjectPush");
		Log.customer.debug("0.1..........");
		Partition p1 = Base.getService().getPartition("None");
		Log.customer.debug("0.1..........");
		ClusterRoot cluster = null;
		Log.customer.debug("0.1..........");
		//Base.getSession().transactionBegin();
		cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p1);
		Log.customer.debug("1..........");
		cluster.setFieldValue("UniqueName", controlid);
		Log.customer.debug("2..........");
		cluster.setFieldValue("ControlDate", datetimezone);
		Log.customer.debug("3..........");
		cluster.setFieldValue("InterfaceName", "MSC_CAPS_INVOICES");
		Log.customer.debug("4..........");
		cluster.setFieldValue("SourceSystem", "Ariba_vcsv1_pcsv1");
		Log.customer.debug("5..........");
		cluster.setFieldValue("SourceFacility", "        ");	//8 Spaces
		Log.customer.debug("6..........");
		cluster.setFieldValue("TargetSystem", "CAPS");
		Log.customer.debug("7..........");
		cluster.setFieldValue("TargetFacility", "CAPS");
		Log.customer.debug("8..........");
		cluster.setFieldValue("RecordCount", new Integer(1));

		if (bdTotCost != null)
		{
			bdTotCost =  bdTotCost.setScale(2, java.math.BigDecimal.ROUND_HALF_UP);
			cluster.setFieldValue("TotalAmount", bdTotCost);
		}
		Log.customer.debug("9..........");
		cluster.setFieldValue("Area2", new Integer(iSpAcct));	//Sum of splitaccountings
		Log.customer.debug("10..........");
		cluster.setFieldValue("Area3", "                                             ");	//48 Spaces
		Log.customer.debug("11..........");
		cluster.save();

		if(cluster != null)
		try
		{
			Log.customer.debug("12..........");
			CallCenter callCenter = CallCenter.defaultCenter();
			Log.customer.debug("13..........");
			Map userInfo1 = MapUtil.map(3);
			Log.customer.debug("14..........");
			Map userData1 = MapUtil.map(3);
			Log.customer.debug("15..........");
			userInfo1.put("Partition", p1.getName());
			Log.customer.debug("16..........");
			userData1.put("EventSource", "ibm_mfg_controlpush");
			Log.customer.debug("17..........");
			userData1.put("ControlPullObject", cluster);
			Log.customer.debug("18..........");
			AribaPOERPReplyListener listener1 = new AribaPOERPReplyListener(cluster.getBaseId(), ObjectServer.objectServer());
			Log.customer.debug("19..........");
			callCenter.callAsync(topicname1, userData1, userInfo1, listener1);
			Log.customer.debug("Before Calling ControlObjectPush" + topicname1 + " " + userData1.toString() + " " + userInfo1.toString() + " " + listener1.toString());

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
			throw e;
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

    public CAPSInvoiceReconciliationPush()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }

    // Code Added for Issue-865

    void GenerateCapsLineNumber (InvoiceReconciliation inv) {
			InvoiceSplitDiscountDollarAmount = Constants.ZeroBigDecimal;
			TotalInvoiceAmountMinusDiscount = Constants.ZeroBigDecimal;
			int iLineNo = 1;
			int iLineType = 0;

			Log.customer.debug("IR Object: " + inv );

			for(Iterator i = inv.getLineItemsIterator(); i.hasNext();) {
				InvoiceReconciliationLineItem irLine = (InvoiceReconciliationLineItem)i.next();

				BigDecimal totSplitAmtToCompare = Constants.ZeroBigDecimal;

				if (irLine == null) continue;

				if (irLine.getFieldValue("LineType") != null)
					iLineType = ( (Integer)irLine.getDottedFieldValue("LineType.Category") ).intValue();

				Log.customer.debug(" IRLI #"+irLine.getNumberInCollection() +"LineType is. " + iLineType );

				for(Iterator s= irLine.getAccountings().getSplitAccountingsIterator(); s.hasNext();) {
					splitAcc = (SplitAccounting) s.next();

					if (splitAcc != null) {
						if (IsForeign) {
							Log.customer.debug("IsForeign is true " );
							BigDecimal splitAccBaseCurrValue = (BigDecimal) splitAcc.getAmount().getApproxAmountInBaseCurrency();

							totSplitAmtToCompare = totSplitAmtToCompare.add(splitAccBaseCurrValue);

							Log.customer.debug(" totSplitAmtToCompare"+totSplitAmtToCompare);
						}

						Log.customer.debug("Inside the Loop and in If");

						iCAPSLineNo = new Integer(iLineNo);
						sCAPSLineNo = new String ( iCAPSLineNo.toString() );
						Log.customer.debug("Setting CapsLineNumber as "+sCAPSLineNo);
						splitAcc.setFieldValue("CapsLineNumber", sCAPSLineNo );
					iLineNo ++;	 }
				}
			}
		}
}
