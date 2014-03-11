/*
*
*	Created by Kavitha
*
	Changes.
	Chandra - using the updateapprovals to clear stuck irs.
	Chandra - 18-01-08 Modifed the query to include IRs in reconciling.

*
*/
package config.java.schedule;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;


public class CatActivateApprovalsOnStuckIRs extends ScheduledTask {


	private String classname="CatActivateApprovalsOnStuckIRs";


	public AQLOptions baseOptions() {
	   AQLOptions options = new AQLOptions();
	   options.setRowLimit(0);
	   options.setUserLocale(Base.getSession().getLocale());
	   options.setUserPartition(Base.getSession().getPartition());
	   return options;
	}

	public void run() throws ScheduledTaskException {
		try {
			Log.customer.debug("%s: Getting the InvoiceReconciliation objects stuck in approving.....", classname);

            String irInApprovingQuery="SELECT  ir FROM ariba.invoicing.core.InvoiceReconciliation ir "
                                        +"where ir.ApprovedState=2 "
                                        +"and ir not in (SELECT distinct ir1 FROM ariba.invoicing.core.InvoiceReconciliation ir1 "
                                        +"JOIN ApprovalRequest as ir1ar USING ir1.ApprovalRequests "
	                                    +"WHERE ir1.ApprovedState=2 and ir1ar.State=2)";

			AQLQuery irInApproving = AQLQuery.parseQuery(irInApprovingQuery);
			AQLResultCollection results = Base.getService().executeQuery(irInApproving, baseOptions());
			Log.customer.debug("%s: the query is %s", classname, irInApproving);

			if(results.getErrors() != null) {
				Log.customer.debug("%s:ERROR RESULTS =:%s ", classname, results.getErrors());
				throw new ScheduledTaskException("Error In Results: " + results.getErrors());
		    }

			while (results.next()) {
				BaseId irid = results.getBaseId(0);
	            InvoiceReconciliation ir = (InvoiceReconciliation)irid.get();
				Log.customer.debug("%s: IR Object got from the query is "+ ir, classname);
				ir.updateApprovals();
				Base.getSession().transactionCommit();
		    }
			Log.customer.debug("%s:Done ", classname);
		} catch(Exception e) {
		     throw new ScheduledTaskException("Error : " + e.toString(), e);
		}
	}
}


