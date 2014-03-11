/*******************************************************************************************************************************************

	Creator: B S Dharshan
	Description: Setting Active field of durable Email to false

	ChangeLog:
	Date		Name		       History
	--------------------------------------------------------------------------------------------------------------
	03/30/2010 	B S Dharshan	--Initial Version
	11/29/2010  ARajendren, 	  Changed the package name of DurableEmail from ariba.approvable.core.DurableEmail to ariba.app.util.DurableEmail
	 	        Ariba Inc.,	 	  for 9R1 upgrade.

*******************************************************************************************************************************************/

package config.java.schedule;

/* Importing the files */

import ariba.approvable.core.Log;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

/* Extending Schedule Task to create CATDurableEmailMaint */

public class CATDurableEmailMaint extends ScheduledTask {
	private Partition p;
	private String query;


    public void run() throws ScheduledTaskException  {
        Log.customer.debug("Making Duable Email Active = 'false'...");
        p = Base.getSession().getPartition();

        try {

            /* Extracting field from the Durable Email */

			query ="select from ariba.app.util.DurableEmail where " + " \"Active\" = true";
            Log.customer.debug(query);

            ClusterRoot obj = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);


			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");

			/* Creating Loop to update the Active Field */

			while(results.next()) {

				obj = (ClusterRoot)results.getBaseId("DurableEmail").get();
				if(obj == null) continue;
				obj.setActive(false);
				Log.customer.debug("DurableEmail is set to false..." + (String)obj.getFieldValue("UniqueName"));
        		Base.getSession().transactionCommit();

        	}

    	}
    	/* Catching if there are anu exception */

		catch(Exception e) {
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("DurableEmail Error: "+ e.toString(), e);
		}
		Log.customer.debug("Ending DurableEmail Active Status .....");
	}

    public CATDurableEmailMaint() {
    }
}
