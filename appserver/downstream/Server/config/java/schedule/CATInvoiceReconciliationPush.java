// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:04 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATInvoiceReconciliationPush.java

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
/*
 * To be tested onsite  Need to revisit
 *
 * S. Sato - This class calls IE: CAPSInvoiceReconciliationPush which is defined in the IBM
 * channel. Will need to be tested onsite
 */
public class CATInvoiceReconciliationPush extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Pushing the IR objects into CAPS system...");
        p = Base.getSession().getPartition();
        AQLResultCollection results;
        String topicname;
        isHeader = false;
        query = "select * from ariba.invoicing.core.InvoiceReconciliation where CreateDate > Date ('2005-01-01 00:00:00 GMT')";
        Log.customer.debug(query);
        ClusterRoot obj = null;
        AQLQuery aqlquery = null;
        AQLOptions options = null;
        results = null;
        topicname = new String("CAPSInvoiceReconciliationPush");
        String eventsource = new String("ibm_caps_invoicereconpush");
        aqlquery = AQLQuery.parseQuery(query);
        options = new AQLOptions(p);
        results = Base.getService().executeQuery(aqlquery, options);

        if(results.getErrors() != null)
            Log.customer.debug("ERROR GETTING RESULTS in Results");
        else if(results.next()){
            obj = results.getBaseId("InvoiceReconciliation").get();
            if(obj != null)
                try
                {
                    Map userInfo = MapUtil.map(3);
                    Map userData = MapUtil.map(3);
                    CallCenter callCenter = CallCenter.defaultCenter();
                    userInfo.put("Partition", p.getName());
                    userData.put("InvoiceReconciliation", obj);
                    AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
                    callCenter.callAsync(topicname, userData, userInfo, listener);
                    Log.customer.debug("Object Pushed....IR #...." + results.getString("UniqueName"));
                    obj.setFieldValue("ActionFlag", "Completed");
                }
                catch(Exception e)
                {
                    Log.customer.debug(e.toString());
                    return;
                }
            Log.customer.debug("Ending FreightPush program .....");
        }
        else{
        	Log.customer.debug("NO records returned .....");
        }
    }

    public CATInvoiceReconciliationPush()
    {
        datetimezone = null;
        baseId = null;
        isTransferred = false;
        interfacename = null;
        tragetfacilityname = null;
        isHeader = false;
        datetimezone = null;
        count = 0;
        total = 0.0D;
    }

    private Partition p;
    private String query;
    private String controlid;
    private String policontrolid;
    private int count;
    private int lineitemcount;
    private double total;
    private double lineitemtotal;
    private Date datetimezone;
    private BaseId baseId;
    private boolean isTransferred;
    private String interfacename;
    private String tragetfacilityname;
    private String strarea2;
    private String strarea3;
    private boolean isHeader;
}