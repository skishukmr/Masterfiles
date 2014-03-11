// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:01 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATInvoicePushAllFacility.java

package config.java.schedule;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Vector;

import ariba.app.server.ObjectServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.ordering.AribaPOERPReplyListener;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATInvoicePushAllFacility extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Setting up the Invoice object...");
        partition = Base.getSession().getPartition();
        try
        {
            isTransferred = false;
            query = new String("select * from ariba.invoicing.core.InvoiceReconciliation where ActionFlag = 'InProcess'");
            Log.customer.debug(query);
            datetimezone = new Date();
            total = 0.0D;
            count = 0;
            controlid = new String("MFG-ProInvoiceHeader_" + getDateTime(datetimezone));
            push(query, "InvoiceReconciliation");
            if(isTransferred)
            {
                interfacename = new String("MFG-ProInvoiceHeaderPush");
                sendControlObject(datetimezone, count, total);
                controlid = new String("MFG-ProInvoiceLine_" + getDateTime(datetimezone));
                interfacename = new String("MFG-ProInvoiceDetailPush");
                sendControlObject(datetimezone, lineitemcount, lineitemtotal);
            }
            Log.customer.debug("Ending InvoiceProcess program .....");
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    void push(String query, String objectname)
    {
        InvoiceReconciliation obj = null;
        String topicname = null;
        String eventsource = null;
        AQLQuery aqlquery = null;
        AQLOptions options = null;
        AQLResultCollection results = null;
        aqlquery = AQLQuery.parseQuery(query);
        options = new AQLOptions(partition, true);
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
            if(objectname.equals("InvoiceReconciliation"))
            {
                Log.customer.debug("In Invoice push....");
                obj = (InvoiceReconciliation)results.getBaseId("InvoiceReconciliation").get();
                Log.customer.debug("In push 11....");
                topicname = new String("InvoiceReconciliationPush");
                Log.customer.debug("In push 13....");
                eventsource = new String("ibm_mfg_invoicereconpush");
                Log.customer.debug("IR...." + obj.toString());
                Log.customer.debug("IR...." + unique);
            }
            if(obj != null)
            {
                unique = (String)obj.getFieldValue("UniqueName");
                try
                {
                    Map userInfo = MapUtil.map(3);
                    Map userData = MapUtil.map(3);
                    CallCenter callCenter = CallCenter.defaultCenter();
                    userInfo.put("Partition", "mfg1");
                    Log.customer.debug("Partition...." + partition.getName());
                    if(objectname.equals("InvoiceReconciliation"))
                        userData.put("Invoice", obj);
                    tragetfacilityname = new String((String)obj.getFieldValue("FacilityFlag"));
                    obj.setFieldValue("ControlIdentifier", controlid);
                    obj.setFieldValue("ControlDate", datetimezone);
                    int invlinecount = obj.getLineItemsCount();
                    Vector tempVat = new Vector();
                    for(int i = 0; i < invlinecount; i++)
                    {
                        InvoiceReconciliationLineItem invreconli = null;
                        invreconli = (InvoiceReconciliationLineItem)obj.getLineItems().get(i);
                        if(invreconli != null)
                        {
                            invreconli.setFieldValue("ControlIdentifier", new String("MFG-ProInvoiceLine_" + getDateTime(datetimezone)));
                            invreconli.setFieldValue("ControlDate", datetimezone);
                        }
                    }

                    AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
                    callCenter.callAsync(topicname, userData, userInfo, listener);
                    Log.customer.debug("Called IBM IE...." + topicname);
                    Log.customer.debug("Object Pushed....");
                    Log.customer.debug("DateTime is set for the object....");
                    String invstat = (String)obj.getFieldValue("InvoiceStatus");
                    String actionflag = (String)obj.getFieldValue("ActionFlag");
                    if(invstat.equals("NonReconciled"))
                        obj.setFieldValue("ActionFlag", "Pushing");
                    else
                        obj.setFieldValue("ActionFlag", "Completed");
                    if(obj.getFieldValue("TotalCost") != null)
                        total += ((BigDecimal)((Money)obj.getFieldValue("TotalCost")).getFieldValue("Amount")).doubleValue();
                    count++;
                    lineitemcount += obj.getLineItemsCount();
                    for(int k = 0; k < obj.getLineItemsCount(); k++)
                    {
                        InvoiceReconciliationLineItem invreconli = null;
                        LineItemProductDescription desc = null;
                        Money price = null;
                        if(obj.getLineItems().get(k) != null)
                            invreconli = (InvoiceReconciliationLineItem)obj.getLineItems().get(k);
                        if(invreconli.getFieldValue("Description") != null)
                            desc = (LineItemProductDescription)invreconli.getFieldValue("Description");
                        if(desc.getFieldValue("Price") != null)
                            price = (Money)desc.getFieldValue("Price");
                        if(price.getFieldValue("Amount") != null)
                            lineitemtotal += ((BigDecimal)price.getFieldValue("Amount")).doubleValue();
                    }

                    Log.customer.debug("In push 12....Total is " + total + " Count is " + count);
                    Base.getSession().transactionCommit();
                }
                catch(Exception e)
                {
                    Log.customer.debug(e.toString());
                    return;
                }
                isTransferred = true;
            }
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

    ClusterRoot getVatClass(String vatunique)
    {
        AQLQuery vatquery = null;
        AQLOptions vatoptions = null;
        AQLResultCollection vatresults = null;
        vatquery = AQLQuery.parseQuery(new String("select from cat.core.VATClass where UniqueName like '" + vatunique + "'"));
        vatoptions = new AQLOptions(partition, true);
        vatresults = Base.getService().executeQuery(vatquery, vatoptions);
        if(vatresults.getErrors() != null)
        {
            Log.customer.debug("ERROR GETTING RESULTS in Results");
            return null;
        }
        for(; vatresults.next(); Log.customer.debug("VAT Object is....." + vatresults.getBaseId("VATClass").get()));
        return vatresults.getBaseId("VATClass").get();
    }

    public CATInvoicePushAllFacility()
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
    private String query;
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