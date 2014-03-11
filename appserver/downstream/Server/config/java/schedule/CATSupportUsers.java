/*****************************************************************************************************************
 Author: Nani Venkatesan
   Date: 01/25/2005
Purpose: The purpose of this task to print out users who are not loaded in a specfic partition as a partitioned user
and for whom one or more partitioned users are either directly or indirectly reporting to.
*****************************************************************************************************************/
package config.java.schedule;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATSupportUsers extends ScheduledTask
{

    private String adapterSource, outputFileName;

    private Partition partition;

    private PrintWriter pw;

    public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
    {
        super.init(scheduler, scheduledTaskName, arguments);
        for(Iterator e = arguments.keySet().iterator(); e.hasNext();)
        {
            String key = (String)e.next();
            if(key.equals("AdapterSource")) {
                adapterSource = (String)arguments.get(key);
			}
            if(key.equals("OutputFileName")) {
                outputFileName = (String)arguments.get(key);
			}
        }

    }

    public void run() throws ScheduledTaskException
    {

        if(adapterSource == null)
        {
            ariba.base.core.Log.customer.debug("AdapterSource must be specified");
            return;
        }

        if(outputFileName == null)
        {
            ariba.base.core.Log.customer.debug("OutputFileName must be specified");
            return;
        }

        partition = Base.getService().getPartition();

        String fileName = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileName;
        File outputFile = new File(fileName);
        try
        {
        	pw = new PrintWriter(IOUtil.bufferedOutputStream(outputFile),true);
		} catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}

        String sqlstring = "Select t1.Supervisor, t1 from ariba.common.core.User t1 where t1.AdapterSource = "
                            + "'" + adapterSource + "'" + " and t1.Supervisor.UniqueName not in "
                            + "(select t2.UniqueName from ariba.common.core.User t2 where t2.AdapterSource = "
                            + "'" + adapterSource + "'" + ")" + " order by t1.UniqueName";

        String header = "UserId,Name,SupervisorId,Name";
        ariba.base.core.Log.customer.debug(header);
        pw.println(header);

		AQLQuery query = AQLQuery.parseQuery(sqlstring);
		AQLOptions options = new AQLOptions(partition,true);
		AQLResultCollection results = Base.getService().executeQuery(query,options);

		BaseId supervisorId = null, userId = null;
		ariba.user.core.User supervisor = null;
		ariba.common.core.User user = null;

		while (results.next())
		{
			supervisorId = results.getBaseId(0);
			userId       = results.getBaseId(1);
			supervisor = (ariba.user.core.User) supervisorId.get();
			user       = (ariba.common.core.User) userId.get();
			printSupervisoryChain(supervisor, user);

		}

		pw.close();

        Base.getSession().transactionCommit();

    }

    private void printSupervisoryChain(ariba.user.core.User supervisor, ariba.common.core.User user)
    {

		String output = user.getUniqueName() + "," + user.getName().getPrimaryString() + "," + supervisor.getUniqueName() + "," + supervisor.getName().getPrimaryString();

		ariba.base.core.Log.customer.debug(output);

		pw.println(output);

		ariba.common.core.User pSupervisor = null;

		while (true)
		{
			supervisor = supervisor.getSupervisor();

			if (supervisor == null)
			{
				//end of chain reached
				break;
			} else if (!supervisor.getActive())
			{
				//if shared supervisor is inactive, skip it
				continue;
			} else
			{
				//print the supervisor if he/she is not already loaded in the partition
				pSupervisor = ariba.common.core.User.getPartitionedUser(supervisor, partition);
				if ((pSupervisor == null) || (!pSupervisor.getActive()))
				{
					output = "," + "," + supervisor.getUniqueName() + "," + supervisor.getName().getPrimaryString();
					ariba.base.core.Log.customer.debug(output);
					pw.println(output);
				}
			}

		}


	}

    public CATSupportUsers()
    {
    }


}