/************************************************************************************
*
* Loading Partition users for R1 - Avinash Rao - April 2005
* Reads the config file maintained to create the CATSupportUser and CATSupportUserDelete files.
*
* Change History:
* Chandra 09-26-06  Modified code for Error handling and performance
************************************************************************************/

package config.java.schedule.sap;

import ariba.base.core.*;
import ariba.util.core.*;

import ariba.util.scheduler.*;
import java.io.*;
import java.util.*;
import ariba.base.core.aql.*;
import ariba.util.log.Log;

public class CATSAPSupportUserConfigNew extends ScheduledTask {

	private String classname = "CATSupportUserConfigNew";

    private String suppUserAddFileName = null;
	private String suppUserDelFileName  = null;
    private String supportUserConfigFile = null;

    private Partition partition;
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
		//boolean isForMFG1 = (partition.getName().equals("mfg1"));

		String headerEncodeString = "Cp1252";
		/* Commented for SAP Partition
		String headerString = "UniqueName,PasswordAdapter,EmployeeNumber,"
					+ "VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,"
					+ "ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,"
					+ "Division,AccountingFacility,Department,Section,PayrollFacility,"
					+ "CSVCostCenter,CSVCompany";
					*/

		String headerString = "UniqueName,PasswordAdapter,ManagementLevel,"
			+ "VanillaDeliverTo,SAPCompanyCode,CostCenterText,SAPShipTo,"
			+ "SAPPurchaseOrg,SAPPlant,SAPExpenseApprovalCode";

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
						/* Commented - for SAP partition
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

							*/
						userRow= SelUser.getUniqueName()+",\""
						+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""
						+(Integer)SelUser.getFieldValue("ManagementLevel") +"\",\""
						+SelUser.getName().getPrimaryString() +"\",\""
						+(String)SelUser.getFieldValue("SAPCompanyCode")+"\",\""
						+(String)SelUser.getFieldValue("SAPCostCenter")+"\",\""
					    + "\",\""
					    + "\",\""
					    +"\",\""
					    +(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\"";

						Log.customer.debug("\n Composed record  "+ userRow);

						if (AD.equals("add")) {
							Log.customer.debug("\n record to write for Add  "+ userRow);
							suppUserAddWriter.println(userRow);
						}

						if (AD.equals("delete")) {
							Log.customer.debug("\n record to write for delete  "+ userRow);
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

    public CATSAPSupportUserConfigNew() {}
}
