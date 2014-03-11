// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:14 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATPORcptAllFacility.java

package config.java.schedule;

import java.math.BigDecimal;
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
import ariba.receiving.core.ReceiptItem;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATPORcptAllFacility extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Setting up the PO object and the receipt object...");
        partition = Base.getSession().getPartition();
        try
        {
            query1 = "select * from ariba.purchasing.core.DirectOrder where ActionFlag like '%InProcess%'";
            Log.customer.debug(query1);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0.0D;
            controlid = new String("MFG-ProPOHeader_" + getDateTime(datetimezone));
            push(query1, "DirectOrder");
            if(isTransferred)
            {
                interfacename = new String("MFG-ProPOHeaderPush");
                sendControlObject(datetimezone, count, total);
                controlid = new String("MFG-ProPOLine_" + getDateTime(datetimezone));
                interfacename = new String("MFG-ProPOLinePush");
                sendControlObject(datetimezone, lineitemcount, lineitemtotal);
            }
            isTransferred = false;
            Log.customer.debug("10...ControlID....." + controlid);
            query1 = "select * from ariba.receiving.core.Receipt where ActionFlag like '%InProcess%'";
            Log.customer.debug(query1);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            lineitemcount = 0;
            lineitemtotal = 0.0D;
            controlid = new String("MFG-ProReceipt_" + getDateTime(datetimezone));
            Log.customer.debug("Before RCPT push....");
            push(query1, "Receipt");
            if(isTransferred)
            {
                interfacename = new String("MFG-ProReceiptPush");
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
                Log.customer.debug("In PO push....");
                obj = (DirectOrder)results.getBaseId("DirectOrder").get();
                Log.customer.debug("In push 11....");
                if(obj.getFieldValue("ExchangeRate") != null)
                    total += ((BigDecimal)obj.getFieldValue("ExchangeRate")).doubleValue();
                Log.customer.debug("In push 12....Total is " + total);
                topicname = new String("PurchaseOrderPush");
                Log.customer.debug("In push 13....");
                eventsource = new String("ibm_mfg_popush");
                Log.customer.debug("DO...." + obj.toString());
            }
            if(objectname.equals("Receipt"))
            {
                Receipt rcpt = null;
                Log.customer.debug("In RCPT push....");
                obj = (Receipt)results.getBaseId("Receipt").get();
                rcpt = (Receipt)results.getBaseId("Receipt").get();
                if(rcpt.getReceiptItems() != null)
                {
                    lineitemcount += rcpt.getReceiptItems().size();
                    Log.customer.debug("Size of RcptItems is...." + lineitemcount);
                    for(int k = 0; k < rcpt.getReceiptItems().size(); k++)
                    {
                        ReceiptItem rcptitem = (ReceiptItem)rcpt.getReceiptItems().get(k);
                        Log.customer.debug("NumberAccepted for RcptItem(" + k + ") is...." + (BigDecimal)rcptitem.getFieldValue("NumberAccepted"));
                        lineitemtotal += ((BigDecimal)rcptitem.getFieldValue("NumberAccepted")).doubleValue();
                    }

                } else
                {
                    Log.customer.debug("ReceiptItems is null");
                }
                Log.customer.debug("In push 12....Total is " + total);
                topicname = new String("ReceiptPush");
                eventsource = new String("ibm_mfg_receiptpush");
            }
            if(obj != null)
                unique = (String)obj.getFieldValue("UniqueName");
            try
            {
                Map userInfo = MapUtil.map(3);
                Map userData = MapUtil.map(3);
                CallCenter callCenter = CallCenter.defaultCenter();
                userInfo.put("Partition", "mfg1");
                Log.customer.debug("Partition...." + partition.getName());
                Log.customer.debug("In push 15....");
                Log.customer.debug("In push 16....");
                if(objectname.equals("DirectOrder"))
                    userData.put("DirectOrder", obj);
                if(objectname.equals("Receipt"))
                    userData.put("Receipt", obj);
                Log.customer.debug("In push 17....");
                tragetfacilityname = new String((String)obj.getFieldValue("FacilityFlag"));
                obj.setFieldValue("ControlIdentifier", controlid);
                obj.setFieldValue("ControlDate", datetimezone);
                Log.customer.debug("Object ControlId " + obj.getFieldValue("ControlIdentifier") + "Object ControlId " + obj.getFieldValue("ControlDate"));
                AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
                callCenter.callAsync(topicname, userData, userInfo, listener);
                Log.customer.debug("Called IBM IE...." + topicname);
                Base.getSession().transactionCommit();
                Log.customer.debug("Object Pushed....");
                count++;
                Log.customer.debug("DateTime is set for the object....");
                if(objectname.equals("DirectOrder"))
                    query1 = new String("select from ariba.purchasing.core.DirectOrder where UniqueName like '" + unique + "'");
                if(objectname.equals("Receipt"))
                    query1 = new String("select from ariba.receiving.core.Receipt where UniqueName like '" + unique + "'");
                aqlquery1 = AQLQuery.parseQuery(query1);
                Log.customer.debug("Query for fetching the object again...." + query1);
                options1 = new AQLOptions(partition);
                results1 = Base.getService().executeQuery(aqlquery1, options1);
                if(results1.getErrors() != null)
                    Log.customer.debug("ERROR GETTING RESULTS in Results1");
                while(results1.next())
                {
                    ClusterRoot cr = results1.getBaseId(0).get();
                    if(cr == null)
                    {
                        Log.customer.debug("CR is null ActionFlag could not be set....");
                    } else
                    {
                        cr.setFieldValue("ActionFlag", new String("Completed"));
                        Log.customer.debug("ActionFlag is set to \"Completed\" for the object....");
                    }
                }
                if(objectname.equals("DirectOrder"))
                {
                    query1 = new String("select UniqueName, count(LineItems) cnt, sum(LineItems.Description.Price.Amount) tot from ariba.purchasing.core.DirectOrder where UniqueName like '" + unique + "'  GROUP BY UniqueName");
                    aqlquery1 = AQLQuery.parseQuery(query1);
                    options1 = new AQLOptions(partition);
                    results1 = Base.getService().executeQuery(aqlquery1, options1);
                    if(results1.getErrors() != null)
                        Log.customer.debug("ERROR GETTING RESULTS in Results1");
                    for(; results1.next(); Log.customer.debug("LI Level Total is....." + lineitemtotal))
                    {
                        lineitemcount += results1.getInteger("cnt");
                        lineitemtotal += results1.getInteger("tot");
                        Log.customer.debug("LI Level Count is....." + lineitemcount);
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

    void sendControlObject(Date datetimezone, int count, double total)
    {
        String topicname = new String("ControlObjectPush");
        ClusterRoot cluster = null;
        Partition p = Base.getService().getPartition("None");
        Base.getSession().transactionBegin();
        cluster = (ClusterRoot)ClusterRoot.create("cat.core.ControlPullObject", p);
        cluster.setFieldValue("UniqueName", controlid);
        cluster.setFieldValue("ControlDate", datetimezone);
        cluster.setFieldValue("RecordCount", new Integer(count));
        cluster.setFieldValue("TotalAmount", new BigDecimal(total));
        cluster.setFieldValue("TargetFacility", tragetfacilityname);
        cluster.setFieldValue("InterfaceName", interfacename);
        Log.customer.debug("TargetFacility-----", tragetfacilityname);
        Log.customer.debug("InterfaceName-----", interfacename);
        cluster.save();
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
                if(cluster == null)
                    Log.customer.debug("Object is null after the push....");
            }
            catch(Exception e)
            {
                if(cluster == null)
                    Log.customer.debug("Clusteter is null after the push....");
                Log.customer.debug(e.toString());
                return;
            }
        Base.getSession().transactionCommit();
    }

    String getDateTime(Date datetime)
    {
        String yy = (new Integer(Date.getYear(datetime))).toString();
        String mm = (new Integer(Date.getMonth(datetime))).toString();
        String dd = (new Integer(Date.getDayOfMonth(datetime))).toString();
        String hh = (new Integer(Date.getHours(datetime))).toString();
        String mn = (new Integer(Date.getMinutes(datetime))).toString();
        String ss = (new Integer(Date.getSeconds(datetime))).toString();
        return yy + mm + dd + hh + mn + ss;
    }

    public CATPORcptAllFacility()
    {
        datetimezone = null;
        baseId = null;
        isTransferred = false;
        interfacename = null;
        tragetfacilityname = null;
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }

    private Partition partition;
    private String query1;
    private String controlid;
    private int count;
    private int lineitemcount;
    private double total;
    private double lineitemtotal;
    private Date datetimezone;
    private BaseId baseId;
    private boolean isTransferred;
    private String interfacename;
    private String tragetfacilityname;
}