/*********** Scheduled Partition User Load **********/

package config.java.schedule;

import java.io.IOException;

import ariba.base.core.Partition;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATPartitionUserLoad extends ScheduledTask
{

    private String adapterSource, outputFileName;

    private Partition partition;

    private java.io.BufferedWriter output;
    private java.io.BufferedReader input;
	private String query1, query2;
	private java.util.Vector vec1 = new java.util.Vector();
	private java.util.Vector vec2 = new java.util.Vector();
	private String line = null;
	private String delim= "|";
	private String all = "ALL";
	private String none = "NONE";


    public void run() throws ScheduledTaskException
    {
		try
		{

			//input = new BufferedReader(new FileReader("UserLoadConfig.txt"));
			output = new java.io.BufferedWriter(new java.io.FileWriter("UserInclusions.csv"));

			/*line = input.readLine();

			while (( line = input.readLine()) != null)
			{

				StringTokenizer st = new StringTokenizer(line, "|");

				String Partition=st.nextToken();
				String PFCI=st.nextToken();
				String ADI=st.nextToken();
				String DI=st.nextToken();
				String PFCE=st.nextToken();
				String ADE=st.nextToken();
				String DE=st.nextToken();*/

				output.write("here");
				ariba.base.core.Log.customer.debug("1string1");

		//	}
			ariba.base.core.Log.customer.debug("2string2");
			output.close();
		}
		catch (IOException e)
		{
			ariba.base.core.Log.customer.debug("estringe");
			ariba.base.core.Log.customer.debug(e.toString());
		}
	}

	 public CATPartitionUserLoad()
    {
    }


}



/*


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
        */




