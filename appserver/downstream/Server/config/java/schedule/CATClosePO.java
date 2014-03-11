/*******************************************************************************************************************************************

	Creator: Kingshuk Mazumdar
	Description: Closes the PO for the US partition

	ChangeLog:
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	12/06/2006 	Kingshuk	--Initial Version
	09/25/2007  Amit		Changed the package from config.java.schedule.vcsv1 to config.java.schedule (global)
							and changed class name to CATClosePO from ClosePO
	02/24/2011  Samir		Updated the ootb field 'Closed' to 'ClosedForAll'
        08/31/2012      Manoj.R         WI 315 - Disabling Receipt Notifications On Closure Of Order
*******************************************************************************************************************************************/

package config.java.schedule;

import ariba.approvable.core.Log;
import ariba.base.core.Base;
import ariba.purchasing.core.DirectOrder;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.purchasing.core.DirectOrder;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATClosePO extends ScheduledTask {
	private Partition p;
	private String query;


    public void run() throws ScheduledTaskException  {
        Log.customer.debug("Closing the PO for the US partition...");
        p = Base.getSession().getPartition();

        try {

			query ="select from ariba.purchasing.core.DirectOrder where currentDate() - CloseOrderDate > 0 and CloseOrder=false";
            Log.customer.debug(query);

            DirectOrder order = null;
            AQLQuery aqlquery = null;
			AQLOptions options = null;
			AQLResultCollection results = null;
			aqlquery = AQLQuery.parseQuery(query);
			options = new AQLOptions(p);
			results = Base.getService().executeQuery(aqlquery, options);
			int size = results.getSize();

			Log.customer.debug("Orders to be closed  : "+size );

			if(results.getErrors() != null)
				Log.customer.debug("ERROR GETTING RESULTS in Results");

			while(results.next()) {

				order = (DirectOrder) results.getBaseId("DirectOrder").get();
				if(order == null) continue;

				order.setFieldValue("CloseOrder", new Boolean("true"));

				    // S. Sato - Ariba - Setting the ootb field to 'ClosedForAll'
				order.setClosed(DirectOrder.ClosedForAll);

                    // close the order. the order was previously closed by setting the
                    // flag.. We need to close the order as it is done in 9r
                   // *** WI 315 Starts ***
                   //order.closeOrder(null);
                   // *** WI 315 Ends ****
				Log.customer.debug("PO# Is Closed..." + order.getUniqueName());
				Log.customer.debug("Remaining Orderd to close ="+ size--);
        		Base.getSession().transactionCommit();

        	}

    	}
		catch(Exception e) {
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("ClosePO Error: "+ e.toString(), e);
		}
		Log.customer.debug("Ending POClose program .....");
	}

    public CATClosePO() {
    }
}
