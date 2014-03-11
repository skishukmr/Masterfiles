/*******************************************************************************************************************************************

	Creator: PGS Kannan
	Description: Revert the DWInvoiceFlag and DWPOFlag for the given create date range.

ariba.invoicing.core.InvoiceReconciliation|DWInvoiceFlag|Completed|InvoiceReconciliation|2007-07-01|2007-08-01|InProcess

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
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.core.DirectOrder;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class RevertDWFlag extends ScheduledTask
{
	private Partition p;
	private String query;
	private BaseId baseId = null;
    private String objtype = null;
    private String flagtype = null;
    private String fromFlagval = null;
    private String fieldname = null;
    private String iRCreateStartDate = null;
    private String iRCreateEndDate = null;
    private String toFlagval = null;
    private DirectOrder order;
    private InvoiceReconciliationLineItem doinvreconli;

    public void run() throws ScheduledTaskException
    {
        Log.customer.debug("Starting RevertDWFlag program...");
        p = Base.getSession().getPartition();
        try
        {
			String fileNameI = "config/variants/" + p.getVariant().getName() + "/partitions/" + p.getName() + "/data/RevertDWFlag.txt";
			File inputFile = new File(fileNameI);
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			String line = br.readLine();
			if (line != null)
			{
				StringTokenizer st = new StringTokenizer(line, "|");
				objtype=st.nextToken();
				flagtype=st.nextToken();
				fromFlagval=st.nextToken();
				fieldname=st.nextToken();
				iRCreateStartDate=st.nextToken();
				iRCreateEndDate=st.nextToken();
				toFlagval=st.nextToken();
			}

			Log.customer.debug("ObjectType IS..." + objtype);
			Log.customer.debug("FlagName IS..." + flagtype);
			Log.customer.debug("FromFlagvale IS..." + fromFlagval);
			Log.customer.debug("FieldName IS..." + fieldname);
			Log.customer.debug("IRCreateStartDate IS..." + iRCreateStartDate);
			Log.customer.debug("IRCreateEndDate IS..." + iRCreateEndDate);
			Log.customer.debug("ToFlagvale IS..." + toFlagval);


			//query = "select from " + objtype +" where " + flagtype + "='" + fromFlagval + "' and CreateDate > Date( '" + iRCreateStartDate +" 00:00:00 GMT')" +
			//                                                                             "and CreateDate < Date('"  + iRCreateEndDate  +" 00:00:00 GMT')";

             query = "select from " + objtype +" where  CreateDate > Date( '" + iRCreateStartDate +" 00:00:00 GMT')" +
                                                  " and CreateDate < Date('"  + iRCreateEndDate  +" 00:00:00 GMT')" +
                                                  " and "+ flagtype + " = '"  + fromFlagval + "'";
                                                                     ;





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
			Log.customer.debug("** RevertDWFlag result size ="+results.getSize() );
			int cnt = 0;
			int size = results.getSize();
            Log.customer.debug("** RevertDWFlag before while loop" );
			while(results.next())
        	{
				obj = (ClusterRoot)results.getBaseId(fieldname).get();
				if(obj != null)
				{
					cnt++;
					size--;
					// IR Flags change

					//Log.customer.debug("** RevertDWFlag IR Flag setting" );
					obj.setFieldValue(flagtype, toFlagval);
					//Log.customer.debug("Value changed");


				    //Po Flag chnage

				    if ( obj instanceof InvoiceReconciliation ) {
						//Log.customer.debug("** RevertDWFlag inside obj instanceof InvoiceReconciliation" );
						InvoiceReconciliation objIR = (InvoiceReconciliation)obj;
						//InvoiceReconciliationLineItem doinvreconli = (InvoiceReconciliationLineItem)objIR.getLineItems().get(0);

						for(int i = 0; i < objIR.getLineItemsCount(); i++)  {
							//Log.customer.debug("** RevertDWFlag inside for loop" );
						   doinvreconli = (InvoiceReconciliationLineItem)objIR.getLineItems().get(i);
						   if(doinvreconli != null && doinvreconli.getFieldValue("Order") != null) {
							   order = (DirectOrder)doinvreconli.getFieldValue("Order");
							   //Log.customer.debug("** RevertDWFlag setting DWPOFlag " );
							   order.setFieldValue("DWPOFlag", toFlagval);
						   }
                          }

					}

				}
				//commit every 200 records and reset counter
				if(cnt == 200) {
					Log.customer.debug("RevertDWFlag: Commit and reset");
					Log.customer.debug("Remaining records to process="+ size);
					Base.getSession().transactionCommit();
					cnt=0;
				}
        	}
        	Base.getSession().transactionCommit();
        	Log.customer.debug("Ending RevertDWFlag program .....");

    	}
		catch(Exception e)
		{
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error: "+ e.toString(), e);
		}
	}
}
