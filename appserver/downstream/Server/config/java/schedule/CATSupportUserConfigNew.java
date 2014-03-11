/************************************************************************************
*
* Loading Partition users for R1 - Avinash Rao - April 2005
* Reads the config file maintained to create the CATSupportUser and CATSupportUserDelete files.
*
* Change History:
* Chandra 09-26-06  Modified code for Error handling and performance
************************************************************************************/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATSupportUserConfigNew extends ScheduledTask {

	private String classname = "CATSupportUserConfigNew";

    private String suppUserAddFileName = null;
	private String suppUserDelFileName  = null;
    private String supportUserConfigFile = null;

    private Partition partition;
	private String line = null;
	private String delim= ",";

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;


	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();
            if (key.equals("SupportUserAddFile")) {
                suppUserAddFileName  = (String)arguments.get(key);
            }
            if (key.equals("SupportUserDeleteFile")) {
                  suppUserDelFileName= (String)arguments.get(key);
            }
            if (key.equals("SupportUserConfigFile")) {
                supportUserConfigFile  = (String)arguments.get(key);
            }
		}//end of for loop
	}

    public void run() throws ScheduledTaskException {

	    PrintWriter suppUserAddWriter = null;
	    PrintWriter suppUserDelWriter = null;
	    BufferedReader br = null;

        partition = Base.getSession().getPartition();
		boolean isForMFG1 = (partition.getName().equals("mfg1"));

		String headerEncodeString = "Cp1252";

		String headerString = "UniqueName,PasswordAdapter,EmployeeNumber,"
					+ "VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,"
					+ "ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,"
					+ "Division,AccountingFacility,Department,Section,PayrollFacility,"
					+ "CSVCostCenter,CSVCompany";

        try {

        	suppUserAddWriter = new PrintWriter(IOUtil.bufferedOutputStream(new File(suppUserAddFileName)),true);
        	suppUserDelWriter = new PrintWriter(IOUtil.bufferedOutputStream(new File(suppUserDelFileName)),true);
        	br = new BufferedReader(new FileReader(supportUserConfigFile));

			suppUserAddWriter.println(headerEncodeString);
			suppUserAddWriter.println(headerString);
			suppUserDelWriter.println(headerEncodeString);
			suppUserDelWriter.println(headerString);

	        String line = br.readLine(); //header line ignored

			while ((line = br.readLine()) != null && !StringUtil.nullOrEmptyOrBlankString(line)) {

				StringTokenizer st = new StringTokenizer(line, ",");

				String UniqueName, PasswordAdapter, Name, AD;

				try {
					UniqueName = st.nextToken();
					PasswordAdapter = st.nextToken();
					Name = st.nextToken();
					AD = st.nextToken();
				} catch (Exception nse) {
					Log.customer.debug("%s : CATSupportUserConfigNew ERROR while tokenizing line=%s", classname, line, nse.toString());
					throw new ScheduledTaskException("Error: " + nse.toString() + " * while reading supportuserloadconfig line="+line, nse);
				}

				BaseId userId = null;
				ariba.user.core.User SelUser = null;
				String userRow;

				String sqlstring = "select from ariba.user.core.User where UniqueName="+"\'"+UniqueName+"\'";
				Log.customer.debug(sqlstring);

				query = AQLQuery.parseQuery(sqlstring);
				options = new AQLOptions(partition);

				results = Base.getService().executeQuery(query,options);

				if (results.getErrors() != null) {
					Log.customer.debug("ERROR GETTING RESULTS for "+ UniqueName);
				}

				while (results.next()) {
					for(int i = 0; i < results.getResultFieldCount(); i++) {
						userId = results.getBaseId(0);
						SelUser = (ariba.user.core.User) userId.get();

						userRow= SelUser.getUniqueName()
							+",\""+(String)SelUser.getFieldValue("PasswordAdapter")
							+"\",\""+(String)SelUser.getFieldValue("EmployeeNumber")
							+"\",\""+SelUser.getName().getPrimaryString()
							+"\","+",\""+(String)SelUser.getFieldValue("DeliverToMailStop")
							+"\",\""+(String)SelUser.getFieldValue("ReceivingFacilityCode")
							+"\",\""+(String)SelUser.getFieldValue("ExpenseApprovalCode")
							+"\",\""+(String)SelUser.getFieldValue("CapitalApprovalCode")
							+"\",\""+(String)SelUser.getFieldValue("Division")
							+"\",\""+(String)SelUser.getFieldValue("AccountingFacility")
							+"\",\""+(String)SelUser.getFieldValue("Department")
							+"\",\""+(String)SelUser.getFieldValue("Section")
							+"\",\""+(String)SelUser.getFieldValue("PayrollFacility")
							+"\",\""+((isForMFG1)?(String)SelUser.getFieldValue("AccountingFacility"):"")
							+(String)SelUser.getFieldValue("CostCenterString")
							+"\",\""+(String)SelUser.getFieldValue("CompanyCode")+"\"";

						if (AD.equals("add")) {
							suppUserAddWriter.println(userRow);
						}

						if (AD.equals("delete")) {
							suppUserDelWriter.println(userRow);
						}
					}
				}

			} // end of while

			suppUserAddWriter.close();
			suppUserDelWriter.close();
			br.close();

			Log.customer.debug("Ending integration and exiting");

		} catch (Exception e) {
			try {
				br.close();
				suppUserAddWriter.close();
				suppUserDelWriter.close();
			} catch (Exception io) {}
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("CATSupportUserConfigNew Error: " + e.toString(), e);
		}
    }

    public CATSupportUserConfigNew() {}
}


