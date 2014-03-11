/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Pushing the VoucherEform Object if the ActionFlag of the object IS NULL and Status is Approved and the set the ActionFlag as Completed

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	4/18/2005 	Kingshuk	Pushing the VoucherEform object depending upon the ActionFlag and the StatusString of the object

*******************************************************************************************************************************************/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class RevertFlag extends ScheduledTask
{
	private Partition p;
	private String query;
	private BaseId baseId = null;
    private String objtype = null;
    private String flagtype = null;
    private String flagval = null;
    private String fieldname = null;

    public void run() throws ScheduledTaskException
    {
        Log.customer.debug("Starting RevertFlag program...");
        p = Base.getSession().getPartition();
        try
        {
			String fileNameI = "config/variants/" + p.getVariant().getName() + "/partitions/" + p.getName() + "/data/RevertFlag.txt";
			File inputFile = new File(fileNameI);
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = br.readLine();
			if (line != null)
			{
				StringTokenizer st = new StringTokenizer(line, "|");
				objtype=st.nextToken();
				flagtype=st.nextToken();
				flagval=st.nextToken();
				fieldname=st.nextToken();
			}

			Log.customer.debug("ObjectType IS..." + objtype);
			Log.customer.debug("FlagName IS..." + flagtype);
			Log.customer.debug("FlagVal IS..." + flagval);
			Log.customer.debug("FieldName IS..." + fieldname);

			query = "select from " + objtype +" where " + flagtype + "= '" + flagval + "'";
            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;

			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			if(results.getErrors() != null) {
				Log.customer.debug("ERROR GETTING RESULTS in Results");
				throw new ScheduledTaskException("Error in results= " + results.getErrorStatementText());
			}

			Log.customer.debug("Value changed");
			Log.customer.debug("** RevertFlag result size ="+results.getSize() );
			int cnt = 0;
			int size = results.getSize();

			while(results.next())
        	{
				obj = (ClusterRoot)results.getBaseId(fieldname).get();
				if(obj != null)
				{
					cnt++;
					size--;
					//Log.customer.debug("Changing the value for %s FROM %s TO NULL: FieldName IS %s", (String)obj.getFieldValue("UniqueName"), flagval, fieldname);
					obj.setFieldValue(flagtype, null);
					//Log.customer.debug("Value changed");
				}
				//commit every 200 records and reset counter
				if(cnt == 200) {
					Log.customer.debug("RevertFlag: Commit and reset");
					Log.customer.debug("Remaining records to process="+ size);
					Base.getSession().transactionCommit();
					cnt=0;
				}
        	}
        	Base.getSession().transactionCommit();
        	Log.customer.debug("Ending RevertFlag program .....");

    	}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error: "+ e.toString(), e);
		}
	}
}
