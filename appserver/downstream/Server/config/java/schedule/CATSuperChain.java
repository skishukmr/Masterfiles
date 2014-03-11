// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 3:11:18 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CATSuperChain.java

package config.java.schedule;

import java.io.File;
import java.io.PrintWriter;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.user.core.User;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATSuperChain extends ScheduledTask
{

    public void run()
        throws ScheduledTaskException
    {
        partition = Base.getSession().getPartition();
        String fileName = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileName;
        File outputFile = new File(fileName);
        try
        {
            pw = new PrintWriter(IOUtil.bufferedOutputStream(outputFile), true);
        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            return;
        }
        /* AUL, sdey : Changed this query
         * because of null pointer exception when supervisor is null for any user
         */
        //String sqlstring = "Select Supervisor, t1 from ariba.common.core.User t1";
        String sqlstring = "Select Supervisor, t1 from ariba.common.core.User t1 where Supervisor is not null";
        String header = "UserId,Name,SupervisorId,Name";
        Log.customer.debug(header);
        pw.println(header);
        AQLQuery query = AQLQuery.parseQuery(sqlstring);
        AQLOptions options = new AQLOptions(partition);
        AQLResultCollection results = Base.getService().executeQuery(query, options);
        BaseId supervisorId = null;
        BaseId userId = null;
        User supervisor = null;
        ariba.common.core.User user = null;
        for(; results.next(); printSupervisoryChain(supervisor, user))
        {
            supervisorId = results.getBaseId(0);
            userId = results.getBaseId(1);
            supervisor = (User)supervisorId.get();
            user = (ariba.common.core.User)userId.get();
        }

        pw.close();
        Base.getSession().transactionCommit();
    }

    private void printSupervisoryChain(User supervisor, ariba.common.core.User user)
    {
        String output = user.getUniqueName() + "," + user.getName().getPrimaryString() + "," + supervisor.getUniqueName() + "," + supervisor.getName().getPrimaryString();
        Log.customer.debug(output);
        pw.println(output);
        ariba.common.core.User pSupervisor = null;
        do
        {
            supervisor = supervisor.getSupervisor();
            if(supervisor == null)
                break;
            if(supervisor.getActive())
            {
                pSupervisor = ariba.common.core.User.getPartitionedUser(supervisor, partition);
                if(pSupervisor == null || !pSupervisor.getActive())
                {
                    output = "in chain: " + supervisor.getUniqueName() + "," + supervisor.getName().getPrimaryString();
                    Log.customer.debug(output);
                    pw.println(output);
                }
            }
        } while(true);
    }

    public CATSuperChain()
    {
        outputFileName = "CATSuperChain";
        adapterSource = "PasswordAdapter1";
    }

    private String outputFileName;
    String adapterSource;
    private Partition partition;
    private PrintWriter pw;
}