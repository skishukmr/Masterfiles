/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the DirectOrder and Receipt object depending upon the ActionFlag of the object

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------------------------------------------------
	5/31/2005 	Kingshuk	Pushing the DirectOrder and Receipt object depending upon the ActionFlag of the object
	9/13/2005 	Kingshuk	Introducing new field isPO for differentiating the push object. If isPO is true then
							push w/ 9 decimal places otherwise push w/ 2 decimal places
10/13/2008              Shaila         Is isPO is false then push 5 decimal places
        05/05/2009  Shailaja Salimath  Issue 864 Receipts failed during MFG push due to issues with rounding.   WBI rounds to 5 positions, MSC to 2 positions, will open up rounding in MSC to 5 positions.
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
import ariba.purchasing.core.DirectOrder;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.receiving.core.Receipt;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATPOCompleteProcess extends ScheduledTask
{
	private Partition partition;
	private String query1;
	private String controlid, policontrolid;
	private int count, lineitemcount;
	private double total, lineitemtotal;
    private ariba.util.core.Date datetimezone = null;
    private BaseId baseId = null;
    private boolean isTransferred = false;
    private String interfacename = null, tragetfacilityname = null, strarea2, strarea3;
    private boolean isHeader = false;
    private boolean isPO = false;

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Setting up the PO object and the receipt object...");
        partition = Base.getSession().getPartition();
        strarea2 = new String("/mfgpro/extract/msc/import/");
        try
        {
			isHeader = false;
			isPO = true;
			query1 = "select * from ariba.purchasing.core.DirectOrder where ActionFlag like '%InProcess%' and FacilityFlag like 'DX'";
            Log.customer.debug(query1);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0;
            controlid = new String("MFG-ProPOHeader-----" + getDateTime(datetimezone));
            policontrolid = new String("MFG-ProPOLine-------" + getDateTime(datetimezone));
            push(query1, "DirectOrder");
            if (isTransferred )
            {
				interfacename = new String ("MFG-ProPOHeaderPush");
				strarea3 = new String ("dxmscdatapohead." + getDateTime(datetimezone) + ".ariba");
				isHeader = true;
				if (count != 0 && total !=0)
					sendControlObject(datetimezone, count, total);
				isHeader = false;

				controlid = new String("MFG-ProPOLine-------" + getDateTime(datetimezone));
				interfacename = new String ("MFG-ProPOLinePush");
				strarea3 = new String ("dxmscdatapoline." + getDateTime(datetimezone) + ".ariba");
				if (lineitemcount != 0 && lineitemtotal !=0)
					sendControlObject(datetimezone, lineitemcount, lineitemtotal);
			}

            isTransferred = false;
            query1 = "select * from ariba.purchasing.core.DirectOrder where ActionFlag like '%InProcess%' and FacilityFlag like 'NA'";
            Log.customer.debug(query1);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0;
            controlid = new String("MFG-ProPOHeader-----" + getDateTime(datetimezone));
            policontrolid = new String("MFG-ProPOLine-------" + getDateTime(datetimezone));
            push(query1, "DirectOrder");
            if (isTransferred )
			{
				interfacename = new String ("MFG-ProPOHeaderPush");
				strarea3 = new String ("namscdatapohead." + getDateTime(datetimezone) + ".ariba");
				isHeader = true;
				if (count != 0 && total !=0)
					sendControlObject(datetimezone, count, total);
				isHeader = false;

				controlid = new String("MFG-ProPOLine-------" + getDateTime(datetimezone));
				interfacename = new String ("MFG-ProPOLinePush");
				strarea3 = new String ("namscdatapoline." + getDateTime(datetimezone) + ".ariba");
				if (lineitemcount != 0 && lineitemtotal !=0)
					sendControlObject(datetimezone, lineitemcount, lineitemtotal);
			}

            isTransferred = false;
            isPO = false;
            query1 = "select * from ariba.receiving.core.Receipt where ActionFlag like '%InProcess%' and FacilityFlag like 'DX'";
            Log.customer.debug(query1);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0;
            controlid = new String("MFG-ProReceipt------" + getDateTime(datetimezone));
            Log.customer.debug("Before RCPT push....");
            push(query1, "Receipt");
            if (isTransferred )
			{
				interfacename = new String ("MFG-ProReceiptPush");
				strarea3 = new String ("dxmscdatareceipt." + getDateTime(datetimezone) + ".ariba");
				if (lineitemcount != 0 && lineitemtotal !=0)
					sendControlObject(datetimezone, lineitemcount, lineitemtotal);
			}

            isTransferred =false;
            query1 = "select * from ariba.receiving.core.Receipt where ActionFlag like '%InProcess%' and FacilityFlag like 'NA'";
			Log.customer.debug(query1);
			datetimezone = new Date();
			total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0;
            controlid = new String("MFG-ProReceipt------" + getDateTime(datetimezone));
			Log.customer.debug("Before RCPT push....");
			push(query1, "Receipt");
			if (isTransferred )
			{
				interfacename = new String ("MFG-ProReceiptPush");
				strarea3 = new String ("namscdatareceipt." + getDateTime(datetimezone) + ".ariba");
				if (lineitemcount != 0 && lineitemtotal !=0)
					sendControlObject(datetimezone, lineitemcount, lineitemtotal);
			}

            Log.customer.debug("Ending POProcess program .....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    void push(String query, String objectname)
    {
        ClusterRoot obj = null;
        String topicname = null;
        String eventsource = null;
        AQLQuery aqlquery = null;
        AQLOptions options = null;
        AQLResultCollection results = null;
        aqlquery = AQLQuery.parseQuery(query);
        options = new AQLOptions(partition);
        results = Base.getService().executeQuery(aqlquery, options);
        if(results.getErrors() != null)
            Log.customer.debug("ERROR GETTING RESULTS in Results");
        while(results.next())
        {
			AQLQuery aqlquery1 = null;
		    AQLOptions options1 = null;
        	AQLResultCollection results1 = null;
        	String query1 = null;
        	String unique = null;

            if(objectname.equals("DirectOrder"))
            {
                obj = (DirectOrder)results.getBaseId("DirectOrder").get();
                //total += ((BigDecimal)((Money)obj.getFieldValue("TotalCost")).getFieldValue("Amount")).doubleValue();
                if (obj.getFieldValue("ExchangeRate") != null)
                {
                	//total += ((java.math.BigDecimal)obj.getFieldValue("ExchangeRate")).doubleValue();
                	Log.customer.debug("B4 Adding....Total is " + total + "ExRate is...." + ( (java.math.BigDecimal)obj.getFieldValue("ExchangeRate") ).doubleValue() );
                	java.math.BigDecimal bd = ((java.math.BigDecimal)obj.getFieldValue("ExchangeRate"));
                	bd = bd.setScale(9, java.math.BigDecimal.ROUND_DOWN);
                	total += bd.doubleValue();
				}
                Log.customer.debug("In push 12....Total is " + total);
                topicname = new String("PurchaseOrderPush");
                eventsource = new String("ibm_mfg_popush");
            }
            if(objectname.equals("Receipt"))
            {
				Receipt rcpt = null;
                Log.customer.debug("In RCPT push....");
                obj = (Receipt)results.getBaseId("Receipt").get();
                rcpt = (Receipt)results.getBaseId("Receipt").get();

                if (rcpt.getReceiptItems() != null)
                {
					//lineitemcount += rcpt.getReceiptItems().size();
					Log.customer.debug("Size of RcptItems is...." + lineitemcount);
					for (int k=0; k< rcpt.getReceiptItems().size(); k++)
					{
						ariba.receiving.core.ReceiptItem rcptitem = (ariba.receiving.core.ReceiptItem)rcpt.getReceiptItems().get(k);
						Log.customer.debug("NumberAccepted for RcptItem(" + k + ") is...." + (java.math.BigDecimal)rcptitem.getFieldValue("NumberAccepted"));
						lineitemtotal += ( (java.math.BigDecimal)rcptitem.getFieldValue("NumberAccepted") ).doubleValue();

						if ( ( ( (java.math.BigDecimal)rcptitem.getFieldValue("NumberAccepted") ).doubleValue() ) != 0 )	lineitemcount++;
					}
				}
				else
				{
					Log.customer.debug("ReceiptItems is null");
				}

                Log.customer.debug("In push 12....Total is " + total);
                topicname = new String("ReceiptPush");
                eventsource = new String("ibm_mfg_receiptpush");
            }
            if(obj != null)
				unique = (java.lang.String)obj.getFieldValue("UniqueName");
                try
                {
                    Map userInfo = MapUtil.map(3);
                    Map userData = MapUtil.map(3);
                    CallCenter callCenter = CallCenter.defaultCenter();
                    userInfo.put("Partition", "mfg1");
                    Log.customer.debug("Partition...." + partition.getName());
                    if(objectname.equals("DirectOrder"))
                        userData.put("DirectOrder", obj);
                    if(objectname.equals("Receipt"))
                        userData.put("Receipt", obj);

                    tragetfacilityname = new String ((java.lang.String)obj.getFieldValue("FacilityFlag"));
                    obj.setFieldValue("ControlIdentifier", controlid);
                    obj.setFieldValue("ControlDate", datetimezone);
                    Log.customer.debug("Object ControlId " + obj.getFieldValue("ControlIdentifier") + "Object ControlId " + obj.getFieldValue("ControlDate"));

                    //Setting the controldate and controlidentifier for LIs
                    if (objectname.equals("DirectOrder"))
					{
						ariba.purchasing.core.DirectOrder dorder = (DirectOrder)obj;
						ariba.approvable.core.LineItem li = null;
						int polisize = ( (ariba.base.core.BaseVector)dorder.getLineItems() ).size();
						for (int z =0; z <polisize; z++)
						{
							li = (ariba.approvable.core.LineItem)( (ariba.base.core.BaseVector)dorder.getLineItems() ).get(z);

							if (li != null)
							{
								/*ariba.procure.core.LineItemProductDescription desc = (ariba.procure.core.LineItemProductDescription) li.getFieldValue("Description");

								if (desc != null)
								{
									java.lang.String strdesc = (java.lang.String)desc.getFieldValue("Description");
									if (strdesc != null)
									{
										char[] inArray = strdesc.toCharArray();
										StringBuffer out = new StringBuffer (inArray.length);
										for (int lstr = 0; lstr < inArray.length; lstr++)
										{
											char c = inArray[lstr];
											if (c == '\n' || c == '\r')
												;
											else
												out.append(c);
										}

										strdesc = out.toString();
										desc.setFieldValue("Description", strdesc);
									}
								}*/

								li.setFieldValue("ControlIdentifier",policontrolid);
								li.setFieldValue("ControlDate", datetimezone);
							}
						}
					}


                    AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
                    callCenter.callAsync(topicname, userData, userInfo, listener);
                    Log.customer.debug("Called IBM IE...." + topicname);
                    Base.getSession().transactionCommit();
                    Log.customer.debug("Object Pushed....");
                    count++;
                    Log.customer.debug("DateTime is set for the object....");

	                if(objectname.equals("DirectOrder"))
					{
						query1 = new String ("select from ariba.purchasing.core.DirectOrder where UniqueName like '" + unique + "'");
					}
                    if(objectname.equals("Receipt"))
					{
						query1 = new String ("select from ariba.receiving.core.Receipt where UniqueName like '" + unique + "'");
					}

					//Setting the flag of the object
                    aqlquery1 = AQLQuery.parseQuery(query1);
                    Log.customer.debug("Query for fetching the object again...." + query1);
					options1 = new AQLOptions(partition, true);
					results1 = Base.getService().executeQuery(aqlquery1, options1);
					if(results1.getErrors() != null)
			            Log.customer.debug("ERROR GETTING RESULTS in Results1");

	                while(results1.next())
	                {
						ClusterRoot cr = results1.getBaseId(0).get();
						if (cr == null)
						{
							Log.customer.debug("CR is null ActionFlag could not be set....");
						}
						else
						{
							cr.setFieldValue("ActionFlag", new String("Completed"));

							//Setting the controldate and controlidentifier for LIs
							/*if (objectname.equals("DirectOrder"))
							{
								ariba.purchasing.core.DirectOrder dorder = (DirectOrder)results1.getBaseId(0).get();
								ariba.approvable.core.LineItem li = null;
								int polisize = ( (ariba.base.core.BaseVector)dorder.getLineItems() ).size();
								for (int z =0; z <polisize; z++)
								{
									li = (ariba.approvable.core.LineItem)( (ariba.base.core.BaseVector)dorder.getLineItems() ).get(z);

									if (li != null)
									{
										li.setFieldValue("ControlIdentifier",policontrolid);
										li.setFieldValue("ControlDate", datetimezone);
									}
								}
							}*/

							Log.customer.debug("ActionFlag is set to \"Completed\" for the object....");
						}
					}

					//Calculating total & count for lineitem control object push
					if(objectname.equals("DirectOrder"))
					{
						query1 = new String ("select UniqueName, count(LineItems) cnt, sum(LineItems.Description.Price.Amount) tot from ariba.purchasing.core.DirectOrder where UniqueName like '" + unique + "'  GROUP BY UniqueName");
						aqlquery1 = AQLQuery.parseQuery(query1);
						options1 = new AQLOptions(partition);
						results1 = Base.getService().executeQuery(aqlquery1, options1);
						if(results1.getErrors() != null)
							Log.customer.debug("ERROR GETTING RESULTS in Results1");

						while(results1.next())
	                	{
							lineitemcount += results1.getInteger("cnt");
							lineitemtotal += results1.getDouble("tot");
							Log.customer.debug("LI Level Count is....." + lineitemcount);
							Log.customer.debug("LI Level Total is....." + lineitemtotal);
						}
					}

                }
                catch(Exception e)
                {
                    Log.customer.debug(e.toString());
                    return;
                }
                isTransferred = true;
        }
        Base.getSession().transactionCommit();
    }

    void sendControlObject(ariba.util.core.Date datetimezone, int count, double total)
    {
        String topicname = new String("ControlObjectPush");
        ClusterRoot cluster = null;
        Partition p = Base.getService().getPartition("None");
        Base.getSession().transactionBegin();
        cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p);
        cluster.setFieldValue("UniqueName", controlid);
        cluster.setFieldValue("ControlDate", datetimezone);
        cluster.setFieldValue("RecordCount", new Integer(count));
        java.math.BigDecimal controltot = new java.math.BigDecimal(total);

        if (!isPO)
        {
        	//controltot = controltot.setScale(5, java.math.BigDecimal.ROUND_HALF_UP);
        	//controltot = controltot.setScale(5,java.math.BigDecimal.ROUND_UNNECESSARY);
        	controltot = controltot;

		}
        else
        {
        	controltot = controltot.setScale(9, java.math.BigDecimal.ROUND_HALF_UP);
		}
        Log.customer.debug("CONTROLTOTAL IS........................................" + controltot);
        cluster.setFieldValue("TotalAmount", controltot);
        cluster.setFieldValue("TargetFacility", tragetfacilityname);
        cluster.setFieldValue("InterfaceName", interfacename);
        cluster.setFieldValue("Area2", strarea2);
		cluster.setFieldValue("Area3", strarea3);
        cluster.save();
        isHeader = false;
        if(cluster != null)
            try
            {
                CallCenter callCenter = CallCenter.defaultCenter();
                Map userInfo = MapUtil.map(3);
                Map userData = MapUtil.map(3);
                userInfo.put("Partition", p.getName());
                userData.put("EventSource", "ibm_mfg_controlpush");
                userData.put("ControlPullObject", cluster);

                AribaPOERPReplyListener listener = new AribaPOERPReplyListener(cluster.getBaseId(), ObjectServer.objectServer());
                callCenter.callAsync(topicname, userData, userInfo, listener);
                Log.customer.debug("Before Called IBM IE....ControlObjectPush" + topicname + userData.toString() + userInfo.toString() + listener.toString());

                Log.customer.debug("Called IBM IE....ControlObjectPush");
                Base.getSession().transactionCommit();
                Log.customer.debug("Object Pushed....");
                if (cluster == null)
	                    Log.customer.debug("Object is null after the push....");
            }
            catch(Exception e)
            {
                if (cluster == null)
	                    Log.customer.debug("Clusteter is null after the push....");
                Log.customer.debug(e.toString());
                return;
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

    public CATPOCompleteProcess()
    {
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }
}
