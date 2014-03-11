// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:29 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATUSReceiptNotifyIsOff.java

package config.java.schedule;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.User;
import ariba.receiving.core.ReceiptNotificationPreferences;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATUSReceiptNotifyIsOff extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        Log.customer.debug("Beginning CATUSReceiptNotifyIsOff program .....");
        Boolean flse = new Boolean(false);
        Integer int0 = new Integer(0);
        partition = Base.getSession().getPartition();
        try
        {
            String sqlstring = "SELECT FROM ariba.common.core.User";
            query = AQLQuery.parseQuery(sqlstring);
            options = new AQLOptions(partition);
            results = Base.getService().executeQuery(query, options);
            if(results.getErrors() != null)
                Log.customer.debug("ERROR GETTING RESULTS");
            BaseId SelUserId = null;
            User SelUser = null;
            while(results.next())
            {
                for(int i = 0; i < results.getResultFieldCount(); i++)
                {
                    SelUserId = results.getBaseId(0);
                    SelUser = (User)SelUserId.get();
                    Log.customer.debug("** AR ** returning users **: " + SelUser.getFieldValue("UniqueName").toString());
                    vec = (BaseVector)SelUser.getFieldValue("NotificationPreferences");
                    for(Iterator it = vec.iterator(); it.hasNext();)
                    {
                        Object o = ((BaseId)it.next()).get();
                        if(o instanceof ReceiptNotificationPreferences)
                        {
                            ReceiptNotificationPreferences rnp = (ReceiptNotificationPreferences)o;
                            rnp.setFieldValue("OfApproval", flse);
                            rnp.setFieldValue("OfFullApproval", flse);
                            rnp.setFieldValue("OfWaitingApprovable", int0);
                            rnp.setFieldValue("OfWatchingApprovable", int0);
                            rnp.setFieldValue("OfOverdueApprovable", int0);
                            Log.customer.debug(" ** AR ** values set to false" + SelUser.getFieldValue("UniqueName").toString());
                        }
                    }

                }

            }
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
    }

    public CATUSReceiptNotifyIsOff()
    {
        vec = new BaseVector();
    }

    private Partition partition;
    private BaseVector vec;
    AQLQuery query;
    AQLOptions options;
    AQLResultCollection results;
}