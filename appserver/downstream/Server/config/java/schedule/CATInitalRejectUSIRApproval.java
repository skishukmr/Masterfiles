/*
Issue 452 - 1st Schedule Task to fetch all the intial reject IRs and send them for verification.
*/
package config.java.schedule;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import config.java.common.CatEmailNotificationUtil;

public class CATInitalRejectUSIRApproval extends ScheduledTask
{
 private String thisclass = "CATInitalRejectUSIRApproval";
 private String startDate = null;
 private String endDate = null;
 private String querytoExecute = null;
 private String filepathforIR = "/msc/arb821/Server/config/variants/vcsv1/data/irInitialReject.txt";
 private ariba.invoicing.core.InvoiceReconciliation invrecon;
 private boolean notification = false;
 private String mailSubject = "Initial Reject IRs to for review";


 public AQLOptions baseOptions() {
 	   AQLOptions options = new AQLOptions();
 	   options.setRowLimit(0);
 	   options.setUserLocale(Base.getSession().getLocale());
 	   options.setUserPartition(Base.getSession().getPartition());
 	   return options;
 }

 /*
  * AUL, sdey 	: Moved the hardcoded values of filepathforIR to schedule task parameter.
  * Reason		: Along with 9r Server path might get changed.
  */
 public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
	//Get the start and end date for the task from Scheduled task table
 	super.init(scheduler, scheduledTaskName, arguments);
        	for(Iterator it = arguments.keySet().iterator(); it.hasNext();)
	        {
	            String key = (String)it.next();
	            if (key.equals("StartPeriod")) {
	                startDate = (String)arguments.get(key);
	            }
	            if (key.equals("EndPeriod")) {
	            	endDate = (String)arguments.get(key);
	            }
	            if (key.equals("FilePathForIR")) {
	            	filepathforIR = (String)arguments.get(key);
	            }
	        }
	        ariba.base.core.Log.customer.debug("%s strStart IS...", thisclass, startDate);
	        ariba.base.core.Log.customer.debug("%s strEnd   IS...", thisclass, endDate);
     }

	public void run() throws ScheduledTaskException{

        FastStringBuffer message = new FastStringBuffer();
        // Query based on the start and end date
		if (!StringUtil.nullOrEmptyOrBlankString(startDate)&&!startDate.equals("None")&&!StringUtil.nullOrEmptyOrBlankString(endDate)&&!endDate.equals("None")){

			querytoExecute = "Select Distinct ir from ariba.invoicing.core.InvoiceReconciliation ir "
					 +"JOIN ariba.approvable.core.ApprovalRequest as ar USING ir.ApprovalRequests "
			         +"where ir.CreateDate between date('"+startDate+"') and date('"+endDate+"') "
					 +"and ir.StatusString = 'Approving' "
					 +"and ar.ApprovalRequired = true and ar.State = 2 "
			 		 +"and ir.TotalCost.Amount = 0.0000000000 "
			 		 +"and ir.Accumulated = false";
	 	}
		else if(!StringUtil.nullOrEmptyOrBlankString(startDate)&&endDate.equals("None")){

			querytoExecute = "Select Distinct ir from ariba.invoicing.core.InvoiceReconciliation ir "
					 +"JOIN ariba.approvable.core.ApprovalRequest as ar USING ir.ApprovalRequests "
					 +"where ir.CreateDate > date('"+startDate+"') "
					 +"and ir.StatusString = 'Approving' "
					 +"and ar.ApprovalRequired = true and ar.State = 2 "
					 +"and ir.TotalCost.Amount = 0.0000000000 "
					 +"and ir.Accumulated = false";

	    }
		else {

      			Assert.that(false, "Date range not specified in ScheduledTask.Table");

		}
                //Eexcute the query
                Log.customer.debug("%s:Query to be executed::%s",thisclass,querytoExecute);
                AQLQuery aqlquerytoExecute = AQLQuery.parseQuery(querytoExecute);
		        AQLResultCollection queryResultCollection = Base.getService().executeQuery(aqlquerytoExecute, baseOptions());

                if(queryResultCollection.getErrors() != null) {
	        		Log.customer.debug("%s:ERROR queryResultCollection for:%s ", thisclass, queryResultCollection.getErrors());
		    		throw new ScheduledTaskException("Error in Result= "+queryResultCollection.getErrorStatementText() );
                }
                else{
			    	message.append("Please verify the IRs in initial reject state \n\n");
			     	PrintWriter out = null;
			     	//Create the file to be written
			     	try {
				 		File irFile = new File(filepathforIR);
				 		if (!irFile.exists()) {
				 		irFile.createNewFile();
		   	    	}
		   	     	out = new PrintWriter(IOUtil.bufferedOutputStream(irFile), true);
		   	     	if(queryResultCollection.getSize() > 0){
						Log.customer.debug("%s:Result Collection Size:%s",thisclass,queryResultCollection.getSize());
						Log.customer.debug("%s:Notification would be sent",thisclass);
						notification = true;
					}
                 	while (queryResultCollection.next()){
                        //write the IR UniqueName from the result collection
                    	Log.customer.debug("%s:  out=" + out, thisclass);

                    	BaseId irobj = queryResultCollection.getBaseId(0);
                    	invrecon = (ariba.invoicing.core.InvoiceReconciliation)irobj.get();

                    	Log.customer.debug("%s:Invoice Reconciliation object::%s",thisclass,invrecon);
                    	String irUniqueName = null;
                    	if (invrecon!=null){
                    		irUniqueName = (String)invrecon.getUniqueName();
                    		if(irUniqueName!=null){
								Log.customer.debug("%s:Invoice Reconciliation UniqueName:%s",thisclass,irUniqueName);

						    	out.write(irUniqueName+"\n");
						    	message.append(irUniqueName+"\n");

							}

				    	}


					}//while
					/*if(out!=null){
						Log.customer.debug("%s: Inside Finally ", thisclass);
						try {
							out.flush();
							out.close();
						}
						catch (Exception io) {
							Log.customer.debug("%s: ERROR:io=%s  ", thisclass, io.toString());
					 	}
		        	} */
                 	}
               		catch(Exception ex) {
			   			Log.customer.debug("%s: ERROR:ex=%s ", thisclass, ex.toString());
			   			Assert.that(false, "Error in creating File = "+ ex.toString());
			    		Log.customer.debug("%s: ERROR:ex=%s ", thisclass, ex.toString());
			    		mailSubject = "Error in Task CATInitalRejectUSIRApproval";
			    		message=null;
			    		message.append(ex.toString());
			   			try {
							out.flush();
			   				out.close();
			   	 		}
			   	 		catch(Exception fwex){}
			   	      		throw new ScheduledTaskException("Error while running CATInitalRejectUSIRApproval: " + ex.toString(), ex);
               			}
               			finally {
			   		    	out.flush();
			   				out.close();
			   				//Send the notification
			   				if(notification){
			   			  		CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "InitalReject");
						    }
			   			 	message = null;
	           			}
               }

	 }

}

