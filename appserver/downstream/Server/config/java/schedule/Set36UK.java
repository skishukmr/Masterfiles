/*******************************************************************************************************************************************/

// Set Accounting to NA if cost center is 36XX for this partition - Avinash Rao - April 2005
// Run first before User loads
// IMPORTANT : USE FOR THE PERKINS PARTITION ONLY

/*******************************************************************************************************************************************/

package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class Set36UK extends ScheduledTask
{
    private Partition partition;

	private String query1;
	private java.util.Vector vec1 = new java.util.Vector();
	private String line = null;

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;

    public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("*** Beginning Set36UK program ***");
        partition = Base.getSession().getPartition();
        try
        {
        	/* AUL, sdey : Changed the query as it is going thru
        	 * all the users which is not required, and taking lot of time to complete the ST.
        	 */
			//query1 = "select from ariba.user.core.User";
        	query1 = "select from ariba.user.core.User where CostCenterString like '36%' and AccountingFacility <> 'NA'";
			query = AQLQuery.parseQuery(query1);
			options = new AQLOptions(partition);
			results = Base.getService().executeQuery(query,options);
			if (results.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
			}

			BaseId SelUserId = null;
			ariba.user.core.User SelUser = null;

			String temp1;
			ariba.base.core.Log.customer.debug("*** till while ***");
			while (results.next())
			{
				for(int i = 0; i < results.getResultFieldCount(); i++)
				{
					SelUserId = results.getBaseId(0);
					SelUser = (ariba.user.core.User) SelUserId.get();
//					ariba.base.core.Log.customer.debug("*** inside results ***");
					if (!(SelUser.getFieldValue("CostCenterString")==(null)))
					{
						if (SelUser.getFieldValue("CostCenterString").toString().trim().length() > 2)
						{
							ariba.base.core.Log.customer.debug("***Non null CCS***");
							ariba.base.core.Log.customer.debug(SelUser.getFieldValue("CostCenterString").toString());
							if ((SelUser.getFieldValue("CostCenterString").toString()).substring(0,2).equals("36"))
							{
								ariba.base.core.Log.customer.debug("**Found 36XX**");
								ariba.base.core.Log.customer.debug(SelUser.getFieldValue("UniqueName"));
								SelUser.setFieldValue("AccountingFacility","NA");
							}
						}
					}

				}
			}

			ariba.base.core.Log.customer.debug("** Ending & exiting **");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public Set36UK()
    {
    }

}

/*******************************************************************************************************************************************/
