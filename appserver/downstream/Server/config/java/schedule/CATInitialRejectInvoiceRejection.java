/*
Issue 452 - 2nd Schedule task to make the initial IRs to reject state.
*/
package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.Assert;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;


public class CATInitialRejectInvoiceRejection extends ScheduledTask{

  private ClusterRoot irObj = null;
  private ariba.invoicing.core.InvoiceReconciliation invrecon;
  private String thisclass = "CATInitialRejectInvoiceRejection";
  protected Locale locale;
  protected String line = null;
  private String fileToRead = "/msc/arb821/Server/config/variants/vcsv1/data/irInitialReject.txt";

  /*
   * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
   * Reason		: Along with 9r Server path might get changed.
   */
	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();
			if (key.equals("FileToRead")) {
				fileToRead = (String) arguments.get(key);
				Log.customer.debug("CATInitialRejectInvoiceRejection : FileToRead "+ fileToRead);
			}
		}
	}
  /*
   * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
   * Reason		: Along with 9r Server path might get changed.
   */

  public void run() throws ScheduledTaskException {

  	try {

  		File inputFile = new File(fileToRead);
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        //Read the irInitialReject.txt
		while ((line=reader.readLine()) != null){

            locale = Base.getSession().getLocale();
			Log.customer.debug("%s:Reader line:%s",thisclass,line);
			//Conver the String from the irInitialReject.txt to InvoiceReconciliation object
            ariba.invoicing.core.InvoiceReconciliation irObject = (ariba.invoicing.core.InvoiceReconciliation) Base.getSession().objectFromName(line, "ariba.invoicing.core.InvoiceReconciliation", Base.getSession().getPartition());
            Log.customer.debug("%s:Object from file:%s",thisclass,irObject);

		    if (irObject instanceof InvoiceReconciliation){
	       		invrecon  = (InvoiceReconciliation)irObject;
		       	Log.customer.debug ("%s:IR to be rejected:%s",thisclass,invrecon.getUniqueName());
		       	//Clear the Approval Request BaseVector
		       	BaseVector approvalRequest = invrecon.getApprovalRequests();
		       	if (approvalRequest!=null && approvalRequest.size()>0){

					approvalRequest.clear();
				}
                //Change the Approved State
                int approvedState = invrecon.getApprovedState();
                Log.customer.debug("%s:Approved State before Change:%s",thisclass,approvedState);
                if (approvedState!=4){

					invrecon.setApprovedState(4);

				}

				Log.customer.debug("%s:Approved State After Change:%s",thisclass,invrecon.getApprovedState());
                //Change the Processed State
				int processedState = invrecon.getProcessedState();
				Log.customer.debug("%s:Processed State before Change:%s",thisclass,processedState);
				if (processedState!=8){

                    Integer newProcesssedState = new java.lang.Integer(8);
					invrecon.setFieldValue("ProcessedState",newProcesssedState);

				}
				Log.customer.debug("%s:Processed State After Change:%s",thisclass,invrecon.getProcessedState());
                //Finally Reject the IR (Though the Procseed State change will make the IR to rejected, but added this to make sure IR gets Rejected)
		        invrecon.reject();
			}

		}
		//Close the buffer reader
		reader.close();

	}
	catch(Exception ex) {
		Log.customer.debug("%s: ERROR:ex=%s ", thisclass, ex.toString());
		Assert.that(false, "Error in Reading file = "+ ex.toString());
	}

  }

}