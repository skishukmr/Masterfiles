/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Changing the ActionFlag Directorder

	ChangeLog:
	Date		Name		Description
	--------------------------------------------------------------------------------------------------------------
	7/10/2005 	Kingshuk	Changing the ActionFlag Directorder

*******************************************************************************************************************************************/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class EmergencyOverWritePO extends ScheduledTask
{
    private String inputFileName="EmergencyOverWritePO.data";

    private Partition partition;

    private BufferedReader br;

	private String query1, query2;
	private String line = null;

	private AQLQuery aqlquery;
	private AQLOptions options;
	private AQLResultCollection results;

    public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Beginning UserLoad program .....");

        partition = Base.getSession().getPartition();

        String fileNameI = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + inputFileName;
        File inputFile = new File(fileNameI);

        try
        {
        	br = new BufferedReader(new FileReader(inputFile));

			ariba.base.core.Log.customer.debug(line);

			while ((line = br.readLine()) != null)
			{
				String controlid = line;

				String controldt = getDateTime (controlid);
				ariba.base.core.Log.customer.debug("Line	" + line + "	Controlid	" + controlid);
				query1 = ("SELECT FROM ariba.purchasing.core.DirectOrder WHERE ControlIdentifier like '" + controlid + "' and ControlDate = Date('" + controldt + "')");
				ariba.base.core.Log.customer.debug(query1);

				aqlquery = AQLQuery.parseQuery(query1);
				options = new AQLOptions(partition);
				results = Base.getService().executeQuery(aqlquery, options);

				if(results.getErrors() != null)
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS in Results");

				while(results.next())
				{
					ClusterRoot cr = results.getBaseId(0).get();
					if (cr == null)
					{
						ariba.base.core.Log.customer.debug("CR is null ActionFlag could not be set....");
					}
					else
					{
						cr.setFieldValue("ActionFlag", new String("InProcess"));
					}
					ariba.base.core.Log.customer.debug("ActionFlag is set to \"InProcess\" for the object....");
				}

				Base.getSession().transactionCommit();
			} // end of while

			br.close();
			ariba.base.core.Log.customer.debug("Ending EmergencyOverwritePO program .....");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    String  getDateTime (String str)
    {
		String yy = str.substring(20,24);
		System.out.println(yy);
		String mm = str.substring(24,26);
		System.out.println(mm);
		String dd = str.substring(26,28);
		System.out.println(dd);
		String hh = str.substring(28,30);
		System.out.println(hh);
		String mn = str.substring(30,32);
		System.out.println(mn);
		String ss = str.substring(32);
		System.out.println(ss);

		str = new String (yy + "-" + mm + "-" + dd + " " + hh + ":" + mn + ":" + ss + " GMT");

		return str;
	}

    public EmergencyOverWritePO()
    {
    }

}

/*******************************************************************************************************************************************/
