/********************************************************
*
* Create Missing Supervisors users for R1 - Avinash Rao - April 2005
* Reads the supervisors upto VP, and includes them in the output file.
* VPs are removed by the CATSupportUserDelete file.

***********************************************************/

package config.java.schedule.sap;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATSAPMissingSuperNew extends ScheduledTask {

	private String classname = "CATMissingSuperNew";
    private String missingSuperFileName = null;

	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();
            if (key.equals("MissingSupervisorFile")) {
                missingSuperFileName  = (String)arguments.get(key);
            }
		}//end of for loop
	 }
    public void run() throws ScheduledTaskException {

		PrintWriter pw = null;
		AQLQuery query;
		AQLOptions options;
		AQLResultCollection results;
		Log.customer.debug("%s Beginning MissingSuper program .....", classname);

        Partition partition = Base.getSession().getPartition();
        File outputFile = new File(missingSuperFileName);

        try {
        	Log.customer.debug("%s entering core", classname);
        	pw = new PrintWriter(IOUtil.bufferedOutputStream(outputFile),true);
			String headerEncodeString = "Cp1252";
			//Commented by Majid to have SAP partition data file format header.
			/* String headerString = "UniqueName,PasswordAdapter,EmployeeNumber,"
					+ "VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,"
					+ "ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,"
					+ "Division,AccountingFacility,Department,Section,PayrollFacility,"
					+ "CSVCostCenter,CSVCompany";
					*/

			String headerString = "UniqueName,PasswordAdapter,ManagementLevel,"
									+ "VanillaDeliverTo,SAPCompanyCode,CostCenterText,SAPShipTo,"
									+ "SAPPurchaseOrg,SAPPlant,SAPExpenseApprovalCode";

			pw.println(headerEncodeString);
			pw.println(headerString);
			// Query to get the missing Supervisor
			String sqlstring = "Select distinct(t1.Supervisor) from ariba.common.core.User t1 "
						+" where t1.Supervisor.UniqueName not in (select t2.UniqueName "
												+" from ariba.common.core.User t2)";

			query = AQLQuery.parseQuery(sqlstring);
			options = new AQLOptions(partition);

			results = Base.getService().executeQuery(query,options);

			if (results.getErrors() != null) {
				Log.customer.debug("ERROR GETTING RESULTS");
			}

			BaseId SupUserId = null;
			ariba.user.core.User SupUser = null;
			String temp;

			Log.customer.debug("%s Beginning result processing .....", classname);


			while(results.next()) {
				for(int i = 0; i < results.getResultFieldCount(); i++) {
					SupUserId = results.getBaseId(0);
					SupUser = (ariba.user.core.User) SupUserId.get();

					Log.customer.debug("%s " + SupUser.getFieldValue("UniqueName"), classname);

					/* File header
					"UniqueName,PasswordAdapter,ManagementLevel,"
					+ "VanillaDeliverTo,SAPCompanyCode,CostCenterText,SAPShipTo,"
					+ "SAPPurchaseOrg,SAPPlant,SAPExpenseApprovalCode";
					*/
					temp = SupUser.getUniqueName()+",\""
							+(String)SupUser.getFieldValue("PasswordAdapter")+"\",\""
							+(Integer)SupUser.getFieldValue("ManagementLevel") +"\",\""
							+SupUser.getName().getPrimaryString() +"\",\""
							+(String)SupUser.getFieldValue("SAPCompanyCode")+"\",\""
							+(String)SupUser.getFieldValue("SAPCostCenter")+"\",\""
						    + "\",\""
						    + "\",\""
						    +"\",\""
						    +(String)SupUser.getFieldValue("ExpenseApprovalCode")+"\"";
					Log.customer.debug(" writing data into the file for supervisor" + SupUser);
					Log.customer.debug(" writing data into the file for supervisor" + temp);
					pw.println(temp);

					ariba.common.core.User pSuper = null;

					while (true) {
						SupUser = SupUser.getSupervisor();

						if (SupUser == null) {
							//end of chain reached
							break;
						} else if (!SupUser.getActive()) {
							//if shared supervisor is inactive, skip it
							continue;
						} else {
							//write the supervisor if he/she is not already loaded in the partition
							pSuper = ariba.common.core.User.getPartitionedUser(SupUser, partition);
							if ((pSuper == null) || (!pSuper.getActive())) {
								temp = SupUser.getUniqueName()+",\""
								+(String)SupUser.getFieldValue("PasswordAdapter")+"\",\""
								+(Integer)SupUser.getFieldValue("ManagementLevel") +"\",\""
								+SupUser.getName().getPrimaryString() +"\",\""
								+(String)SupUser.getFieldValue("SAPCompanyCode")+"\",\""
								+(String)SupUser.getFieldValue("SAPCostCenter")+"\",\""
							    + "\",\""
							    + "\",\""
							    +"\",\""
							    +(String)SupUser.getFieldValue("ExpenseApprovalCode")+"\"";
								Log.customer.debug("%s writing " + SupUser.getUniqueName(), classname);
								Log.customer.debug(" writing data into the file for supervisor" + SupUser);
								Log.customer.debug(" writing data into the file " + temp);
								pw.println(temp);
							}
						}
					}
				}
			}
			pw.close();

		} catch (Exception e) {
			try {
				pw.close();
			} catch (Exception io) {}
			Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error: " + e.toString(), e);
		}
    }
    public CATSAPMissingSuperNew() {}

}