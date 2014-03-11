/********************************************************
*
* Create Missing Supervisors users for R1 - Avinash Rao - April 2005
* Reads the supervisors upto VP, and includes them in the output file.
* VPs are removed by the CATSupportUserDelete file.

***********************************************************/

package config.java.schedule;

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

public class CATMissingSuperNew extends ScheduledTask {

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
			String headerString = "UniqueName,PasswordAdapter,EmployeeNumber,"
					+ "VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,"
					+ "ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,"
					+ "Division,AccountingFacility,Department,Section,PayrollFacility,"
					+ "CSVCostCenter,CSVCompany";

			pw.println(headerEncodeString);
			pw.println(headerString);

			String sqlstring = "Select distinct(t1.Supervisor) from ariba.common.core.User t1 "
						+" where t1.Supervisor.UniqueName not in (select t2.UniqueName "
												+" from ariba.common.core.User t2) ";

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
					temp = SupUser.getUniqueName()
							+",\""+(String)SupUser.getFieldValue("PasswordAdapter")
							+"\",\""+(String)SupUser.getFieldValue("EmployeeNumber")
							+"\",\""+SupUser.getName().getPrimaryString()
							+"\","+",\""+(String)SupUser.getFieldValue("DeliverToMailStop")
							+"\",\""+(String)SupUser.getFieldValue("ReceivingFacilityCode")
							+"\",\""+(String)SupUser.getFieldValue("ExpenseApprovalCode")
							+"\",\""+(String)SupUser.getFieldValue("CapitalApprovalCode")
							+"\",\""+(String)SupUser.getFieldValue("Division")
							+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")
							+"\",\""+(String)SupUser.getFieldValue("Department")
							+"\",\""+(String)SupUser.getFieldValue("Section")
							+"\",\""+(String)SupUser.getFieldValue("PayrollFacility")
							+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")
							+(String)SupUser.getFieldValue("CostCenterString")
							+"\",\""+(String)SupUser.getFieldValue("CompanyCode")+"\"";
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
								temp = SupUser.getUniqueName()
										+",\""+(String)SupUser.getFieldValue("PasswordAdapter")
										+"\",\""+(String)SupUser.getFieldValue("EmployeeNumber")
										+"\",\""+SupUser.getName().getPrimaryString()
										+"\","+",\""+(String)SupUser.getFieldValue("DeliverToMailStop")
										+"\",\""+(String)SupUser.getFieldValue("ReceivingFacilityCode")
										+"\",\""+(String)SupUser.getFieldValue("ExpenseApprovalCode")
										+"\",\""+(String)SupUser.getFieldValue("CapitalApprovalCode")
										+"\",\""+(String)SupUser.getFieldValue("Division")
										+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")
										+"\",\""+(String)SupUser.getFieldValue("Department")
										+"\",\""+(String)SupUser.getFieldValue("Section")
										+"\",\""+(String)SupUser.getFieldValue("PayrollFacility")
										+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")
										+(String)SupUser.getFieldValue("CostCenterString")
										+"\",\""+(String)SupUser.getFieldValue("CompanyCode")+"\"";
								Log.customer.debug("%s writing " + SupUser.getUniqueName(), classname);
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

    public CATMissingSuperNew() {}

}


