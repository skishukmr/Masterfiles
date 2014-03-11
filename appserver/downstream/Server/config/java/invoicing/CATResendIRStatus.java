/*******************************************************************************************************************************************

	Creator: 	 James S Pagadala
	Description: To resend the status to ASN for the rejected invoices

	ChangeLog:
	Date		Name		Description

*******************************************************************************************************************************************/

package config.java.invoicing;

import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceStatusUpdateRequester;
import ariba.invoicing.core.InvoiceWorkFailure;
import ariba.util.core.Vector;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.Scheduler;

public class CATResendIRStatus extends ScheduledTask
{

	private String ApprovableIDs = "ApprovableIDs";
	private	String approvableNamesArgument = null;
	private String status = "reconciled";

	public void init(Scheduler scheduler, String scheduledTaskName,	Map arguments) {

		Log.customer.debug(" CATResendIRStatus : init : ****START**** " );

		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();
			Log.customer.debug(" CATResendIRStatus : init : key : " + key);
			if (key.equals("ApprovableIDs")) {
				approvableNamesArgument = (String) arguments.get(key);
				Log.customer.debug(" CATResendIRStatus : init : approvableNamesArgument : " + approvableNamesArgument);
			}
			if (key.equals("Status")) {
				status = (String) arguments.get(key);
				Log.customer.debug(" CATResendIRStatus : init : status : " + status);
			}

		}

		Log.customer.debug(" CATResendIRStatus : init : ****END**** " );
    }

    public void run ()
    {

		Log.customer.debug(" CATResendIRStatus : run : ****START**** " );

		Partition partition = Base.getService().getPartition();

		Log.customer.debug(" CATResendIRStatus : run : partition " + partition);

		if(approvableNamesArgument == null){

			Log.customer.debug(" CATResendIRStatus : run : approvableNamesArgument is null ");

			return;
		}

		Log.customer.debug(" CATResendIRStatus : run : approvableNamesArgument : " + approvableNamesArgument);

		Iterator listOfIRs = getIdentifiersFromArgument(approvableNamesArgument);

		int counter = 0;

		while(listOfIRs.hasNext()){

			Log.customer.debug(" CATResendIRStatus : run : looping thru " + counter++);

			String IRUniqueName = (String) listOfIRs.next();

			Log.customer.debug(" CATResendIRStatus : run : IRUniqueName " + IRUniqueName);

			InvoiceReconciliation ir = (InvoiceReconciliation)Base.getService().objectMatchingUniqueName("ariba.invoicing.core.InvoiceReconciliation",partition,IRUniqueName);

			if(ir == null){
				Log.customer.debug(" CATResendIRStatus : run : IRUniqueName " + IRUniqueName);
				continue;
			}

			Log.customer.debug(" CATResendIRStatus : run : ir " + ir);

			/**
                S. Sato - AUL -
                This scheduled task will need to be tested onsite. Need to revisit
			*/
			try {
				InvoiceStatusUpdateRequester.sendRequest(ir, status);
			} catch (InvoiceWorkFailure e) {
				Log.customer.debug(" CATResendIRStatus : InvoiceWorkFailure : "
						+ e.toString());
			}

			Log.customer.debug(" CATResendIRStatus : run : Processing complete for IR : " +  IRUniqueName);
		}

		Log.customer.debug(" CATResendIRStatus : run : ****END****" );
	}

    private Iterator getIdentifiersFromArgument (String argument) {

		Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : ****START*****");

        Vector identifiers = new Vector();

        if(argument == null){
			Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : argument is null");
			return identifiers.iterator();
		}

        int end = 0;
        int begin = 0;
        int i = 0;
		String identifier = null;

		while(end != -1) {
			end = argument.indexOf(",", begin);
			if (end != -1) {
				identifier = argument.substring(begin, end);
				Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : identifier : " + identifier);
				begin = end+1;
			}
			else {
				identifier = argument.substring(begin,	argument.length());
				Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : last identifier : " + identifier);
			}
			identifier = identifier.trim();
			Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : identifier after trim: " + identifier);
			identifiers.addElement(identifier);
		}

		Log.customer.debug(" CATResendIRStatus : getIdentifiersFromArgument : ****END*****");
        return identifiers.iterator();
    }

}
