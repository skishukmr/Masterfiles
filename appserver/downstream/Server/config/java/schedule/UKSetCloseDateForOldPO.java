/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: For setting the CloseOrderDate for the Old POs which created before the ClosePO CR

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------
	05/11/2007 	Kingshuk	For setting the CloseOrderDate for the Old POs which created before the ClosePO CR
	10/01/2010           Ashwini	Included this for UK partition

*******************************************************************************************************************************************/

package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Date;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class UKSetCloseDateForOldPO extends ScheduledTask {

    private Partition partition;
	private String query, query1, query2;
	private AQLQuery qry, qry1, qry2;
	private AQLOptions options, options1, options2;
	private AQLResultCollection results, results1, results2;
	private ariba.purchasing.core.DirectOrder order;
    private int numofdays = 365;
    private static final String THISCLASS = "UKSetCloseDateForOldPO:";

	public void run() throws ScheduledTaskException {

        partition = ariba.base.core.Base.getSession().getPartition();

		//Processing POs which already created with CloseOrderDate IS NULL
		query = new String ("select DirectOrder from ariba.purchasing.core.DirectOrder where OrderedState=4 and UniqueName like 'PE%'" );

		Log.customer.debug("%s %s", THISCLASS, query);
		qry = AQLQuery.parseQuery(query);
		options = new AQLOptions(partition);
		results = Base.getService().executeQuery(qry,options);
		if (results.getErrors() != null)
		{
			Log.customer.debug("%s ERROR GETTING RESULTS in Results1", THISCLASS);
		}

		int cnt = 0;
		int size = results.getSize();
        Date currentDate = Date.getNow();
		Date dt = null;
		while(results.next()) {
			try {
				cnt++;
				size--;

				order = (ariba.purchasing.core.DirectOrder)(results.getBaseId("DirectOrder").get());
				if (order == null) continue;
				Log.customer.debug("%s Setting for PO# %s", THISCLASS, order.getUniqueId());

				dt = new Date((Date)order.getFieldValue("OrderedDate"));


				//Log.customer.debug("%s OrderedDate IS:: %s", THISCLASS, dt);

				//numofDays is 365
				Date.addDays(dt, 365);
				//Log.customer.debug("%s CloseOrderDate IS:: %s", THISCLASS, dt);

				order.setFieldValue("CloseOrderDate", dt);
				Log.customer.debug("%s \nOrderedDate = %s \nCloseOrderDate= %s", THISCLASS, order.getFieldValue("OrderedDate") ,order.getFieldValue("CloseOrderDate"));

				//where you check if currentdate > closeorderdate
				//then set closeorder to true
				//else to false
				if (dt.before(currentDate))
				   order.setFieldValue("CloseOrder",new Boolean(true));
				else
				   order.setFieldValue("CloseOrder",new Boolean(false));


				//commit every 50 records and reset counter
				//resultsize - print number left to complete
				if(cnt == 50) {
					Log.customer.debug("%s Remaining records to process= "+ size, THISCLASS);
					Base.getSession().transactionCommit();
					cnt=0;
				}
			} catch (Exception ex) {
				Log.customer.debug("%s ", ex.toString());
				throw new ScheduledTaskException("%s Close Order ST Error: "+ ex.toString(), ex);

			}

		}
	}

    public UKSetCloseDateForOldPO(){}
}

