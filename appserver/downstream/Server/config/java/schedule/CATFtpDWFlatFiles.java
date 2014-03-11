/* Created Madhavan Chari
   Date 21 May 1982
   Description FTP of the files, once the DW ST completes the flat file creation
*/
package config.java.schedule;

import java.io.File;

import ariba.base.core.Log;
import ariba.util.core.Assert;
import ariba.util.core.Date;
import ariba.util.formatter.DateFormatter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATFtpDWFlatFiles extends ScheduledTask {
    private String classname = "CATFtpDWFlatFiles";
    private String tiggerFileName = "/msc/arb821/Server/config/mscreports/MSC_DW_INVOICE_PUSH_TRIGGER.txt";
    File tiggerfile = null;

    public void run() throws ScheduledTaskException	{
		createTriggerFile();
		ftpFlatFiles();
		boolean hasRenamed2 = tiggerfile.renameTo(new File(tiggerFileName + ".gz" + "." + DateFormatter.tocXML(Date.getNow())));
	    //Log.customer.debug("::class name"+classname+"::file renamed::"+hasRenamed2);

	}

		public void createTriggerFile(){
        	tiggerfile = new File(tiggerFileName);
        	try {
    			Log.customer.debug("%s::Tigger file name:%s",classname,tiggerfile);
    			if (!tiggerfile.exists()) {
        			Log.customer.debug("%s::if file does not exit then create 1",classname);
        			tiggerfile.createNewFile();
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
    			Process runDWFTP = Runtime.getRuntime().exec("/usr/bin/sh /msc/arb821/Server/config/java/schedule/DWFileFTP_All_QA.sh");
    			int exitval = runDWFTP.waitFor();
    			Log.customer.debug("%s::Process exits with =::%s",classname,exitval);
    			Log.customer.debug("%s:: FTP of flat files Finished:",classname);
	    	}
	    	catch(Exception e){
				Log.customer.debug(e.toString());
		    }
      	}
    }

