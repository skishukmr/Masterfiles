// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:46 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   DWMFGPOPush.java

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

public class DWMFGPOPush extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Pushing the PO objects into DW for MFG system...");
        p = Base.getSession().getPartition();
        AQLResultCollection results;
        String topicname;
        isHeader = false;
        query = "select from ariba.purchasing.core.DirectOrder where DWPOFlag = 'InProcess'";
        Log.customer.debug(query);
        ClusterRoot obj = null;
        AQLQuery aqlquery = null;
        AQLOptions options = null;
        results = null;
        topicname = new String("PurchaseOrderPush");
        String eventsource = new String("ibm_dw_popush");
        aqlquery = AQLQuery.parseQuery(query);
        options = new AQLOptions(p);
        results = Base.getService().executeQuery(aqlquery, options);
        if(results.getErrors() != null)
            Log.customer.debug("ERROR GETTING RESULTS in Results");

        if(results.next()){
        	obj = results.getBaseId("DirectOrder").get();
	        if(obj != null)
	            try
	            {
	                Map userInfo = MapUtil.map(3);
	                Map userData = MapUtil.map(3);
	                CallCenter callCenter = CallCenter.defaultCenter();
	                userInfo.put("Partition", p.getName());
	                userData.put("DirectOrder", obj);
	                AribaPOERPReplyListener listener = new AribaPOERPReplyListener(obj.getBaseId(), ObjectServer.objectServer());
	                callCenter.callAsync(topicname, userData, userInfo, listener);
	                obj.setFieldValue("DWPOFlag", "Completed");
	            }
	            catch(Exception e)
	            {
	                Log.customer.debug(e.toString());
	                return;
	            }
	        Base.getSession().transactionCommit();
	        Log.customer.debug("Ending DWMFGPOPush program .....");
        }else{
        	Log.customer.debug("No Records found .....");
        }
    }

    public DWMFGPOPush()
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