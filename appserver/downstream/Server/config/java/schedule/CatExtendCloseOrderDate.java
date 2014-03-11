/**************************************************************************************************************************
 Creator: Manoj.R
 Description: Extending the CloseOrderDate for Reopened PO

 ChangeLog:
 Date		Name		Issue #      History
 --------------------------------------------------------------------
 17/09/2012 	IBM_AMS_Manoj.R          323         Extending the CloseOrderDate for Reopened PO

*****************************************************************************************************************************/

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
import ariba.util.formatter.DateFormatter;
import ariba.util.core.Date;

public class CatExtendCloseOrderDate extends ScheduledTask {
	private Partition p;
	private String query;
    private static final String THISCLASS = "CatExtendCloseOrderDate:";

    public void run() throws ScheduledTaskException  {
        Log.customer.debug(THISCLASS +"Entering  the CloseOrderedDate for Re-Opened PO's");
        p = Base.getSession().getPartition();
     	query ="select from ariba.purchasing.core.DirectOrder where CloseOrder=True and Closed = 1";
		Log.customer.debug(query);
		DirectOrder order = null;
		String closeOrderReopen = Base.getService().getParameter(null,"System.Base.CloseOrderReopen");
		AQLQuery aqlquery = null;
		AQLOptions options = null;
		AQLResultCollection results = null;
		aqlquery = AQLQuery.parseQuery(query);
		options = new AQLOptions(p);
		results = Base.getService().executeQuery(aqlquery, options);
		int size = results.getSize();

		Log.customer.debug(THISCLASS +"  Orders to be closed  : "+size );

		if(results.getErrors() != null)
		{
					Log.customer.debug("%s ERROR GETTING RESULTS in Results1", THISCLASS);
		}
		int cnt = 0;
		while(results.next())
		{
		  try
		 {
		   cnt++;
           size--;
                   Date currentDate = Date.getNow();
                   Log.customer.debug(THISCLASS +"*** Current Date :%s ",currentDate);
		   order = (DirectOrder) results.getBaseId("DirectOrder").get();
                   Date closeOrderDate = DateFormatter.getDateValue(order.getFieldValue("CloseOrderDate"));
		  if(order == null) continue;
		  {
                    Log.customer.debug(THISCLASS +"*** Current PO UniqueName : %s",order.getUniqueName());
		    int idays = -1;
		   	if (closeOrderReopen!= null)
		     {
			 idays = Integer.parseInt(closeOrderReopen);
			 Log.customer.debug(THISCLASS +"*** After parsing the param value Days: %s ",idays);
			 Log.customer.debug(THISCLASS +"*** Date Before Adding %s Days: %s ",closeOrderReopen, closeOrderDate);
			 if (idays != -1)
			 {
				 Date.addDays(currentDate, idays);
				 order.setFieldValue("CloseOrderDate", currentDate);
				 Log.customer.debug(THISCLASS +"CloseDate for the ReOpenPO would be set to: %s",currentDate);
				 order.setFieldValue("CloseOrder", new Boolean("false"));
			 }
		    if(cnt == 50)
		    {
				Log.customer.debug(THISCLASS + "%s Remaining records to process= "+ size);
				Base.getSession().transactionCommit();
				cnt=0;
			 }

		   }

		  }
	    }
	    catch(Exception e) {
		Log.customer.debug(e.toString());
		throw new ScheduledTaskException("ClosePO Error: "+ e.toString(), e);
		}

	   }

		Log.customer.debug("Ending CatExtendCloseOrderDate program .....");
	}

    public CatExtendCloseOrderDate() {
    }
}
