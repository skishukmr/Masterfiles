/* Created Madhavan Chari
   Date 21 May 1982
   Description FTP of the files, once the DW ST completes the flat file creation
*/
package config.java.schedule;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Log;
import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATFtpDW_FlatFiles extends ScheduledTask {
    private String classname = "CATFtpDW_FlatFiles";
    private String tiggerFileName = "/msc/arb821/Server/config/mscreports/MSC_DW_INVOICE_PUSH_TRIGGER.txt";
    private String dwftpCommand = "/usr/bin/sh /msc/arb821/Server/config/java/schedule/DWFileFTP_All_QA.sh";
    File tigger_file = null;

    /*
     * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
     * Reason		: Along with 9r Server path might get changed.
     */
	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for (Iterator e = arguments.keySet().iterator(); e.hasNext();) {
			String key = (String) e.next();
			if (key.equals("TiggerFileName")) {
				tiggerFileName = (String) arguments.get(key);
				Log.customer.debug("CATFtpDW_FlatFiles : tiggerFileName "+ tiggerFileName);
			} else if (key.equals("DWFTPCommand")) {
				dwftpCommand = (String) arguments.get(key);
				Log.customer.debug("CATFtpDW_FlatFiles : DWFTPCommand "
								+ dwftpCommand);
			}
		}
	}
    /*
     * AUL, sdey 	: Moved the hardcoded values to schedule task parameter.
     * Reason		: Along with 9r Server path might get changed.
     */

    public void run() throws ScheduledTaskException	{
		createTriggerFile();
		ftpFlatFiles();
		boolean hasRenamed2 = tigger_file.renameTo(new File(tiggerFileName + ".gz" + "." + DateFormatter.tocXML(Date.getNow())));
	    //Log.customer.debug("::class name"+classname+"::file renamed::"+hasRenamed2);

	}

		public void createTriggerFile(){
        	tigger_file = new File(tiggerFileName);
        	try {
    			Log.customer.debug("%s::Tigger file name:%s",classname,tigger_file);
    			if (!tigger_file.exists()) {
        			Log.customer.debug("%s::if file does not exit then create 1",classname);
        			tigger_file.createNewFile();
    			}
    	    }
    		catch(Exception e){
				Log.customer.debug(e.toString());
				Assert.that(false, "Error in creatinf Trigger File = "+ e.toString());
		    }
	    }
	    public void ftpFlatFiles(){
       		Log.customer.debug("%s:: FTP of flat files begins:",classname);
       		try {
    			Process runDWFTP = Runtime.getRuntime().exec(dwftpCommand);
    			int exitval = runDWFTP.waitFor();
    			Log.customer.debug("%s::Process exits with =::%s",classname,exitval);
    			Log.customer.debug("%s:: FTP of flat files Finished:",classname);
	    	}
	    	catch(Exception e){
				Log.customer.debug(e.toString());
		    }
      	}
    }

