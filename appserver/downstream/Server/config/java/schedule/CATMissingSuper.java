/*******************************************************************************************************************************************/

// Create Missing Supervisors users for R1 - Avinash Rao - April 2005
// Reads the supervisors upto VP, and includes them in the output file. VPs are removed by the CATSupportUserDelete file.

/*******************************************************************************************************************************************/

package config.java.schedule;

import java.io.File;
import java.io.PrintWriter;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATMissingSuper extends ScheduledTask
{

    private Partition partition;
	private String sqlstring;

	private String outputFileName="CATMissingSuper.csv";
	private PrintWriter pw;

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;

    public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Beginning MissingSuper program .....");

        partition = Base.getSession().getPartition();

        String fileName = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileName;
        File outputFile = new File(fileName);

        try
        {
        	ariba.base.core.Log.customer.debug("entering core");

        	pw = new PrintWriter(IOUtil.bufferedOutputStream(outputFile),true);

			pw.println("Cp1252");
			pw.println("UniqueName,PasswordAdapter,EmployeeNumber,VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,Division,AccountingFacility,Department,Section,PayrollFacility,CSVCostCenter,CSVCompany");
			sqlstring = "Select distinct(t1.Supervisor) from ariba.common.core.User t1  where t1.Supervisor.UniqueName not in (select t2.UniqueName from ariba.common.core.User t2)";

			query = AQLQuery.parseQuery(sqlstring);
			options = new AQLOptions(partition);

			results = Base.getService().executeQuery(query,options);

			if (results.getErrors() != null)
			{
				ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
			}

			BaseId SupUserId = null;
			ariba.user.core.User SupUser = null;
			String temp;

			ariba.base.core.Log.customer.debug("Beginning result processing .....");

			while(results.next())
			{
				for(int i = 0; i < results.getResultFieldCount(); i++)
					{

						SupUserId = results.getBaseId(0);
						SupUser = (ariba.user.core.User) SupUserId.get();

						ariba.base.core.Log.customer.debug(SupUser.getFieldValue("UniqueName"));
						temp= SupUser.getUniqueName()+",\""+(String)SupUser.getFieldValue("PasswordAdapter")+"\",\""+(String)SupUser.getFieldValue("EmployeeNumber")+"\",\""+SupUser.getName().getPrimaryString()+"\","+",\""+(String)SupUser.getFieldValue("DeliverToMailStop")+"\",\""+(String)SupUser.getFieldValue("ReceivingFacilityCode")+"\",\""+(String)SupUser.getFieldValue("ExpenseApprovalCode")+"\",\""+(String)SupUser.getFieldValue("CapitalApprovalCode")+"\",\""+(String)SupUser.getFieldValue("Division")+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")+"\",\""+(String)SupUser.getFieldValue("Department")+"\",\""+(String)SupUser.getFieldValue("Section")+"\",\""+(String)SupUser.getFieldValue("PayrollFacility")+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")+(String)SupUser.getFieldValue("CostCenterString")+"\",\""+(String)SupUser.getFieldValue("CompanyCode")+"\"";
						pw.println(temp);

						ariba.common.core.User pSuper = null;

						while (true)
						{
							SupUser = SupUser.getSupervisor();

							if (SupUser == null)
							{
								//end of chain reached
								break;
							} else if (!SupUser.getActive())
							{
								//if shared supervisor is inactive, skip it
								continue;
							} else
							{
								//write the supervisor if he/she is not already loaded in the partition
								pSuper = ariba.common.core.User.getPartitionedUser(SupUser, partition);
								if ((pSuper == null) || (!pSuper.getActive()))
								{
									temp= SupUser.getUniqueName()+",\""+(String)SupUser.getFieldValue("PasswordAdapter")+"\",\""+(String)SupUser.getFieldValue("EmployeeNumber")+"\",\""+SupUser.getName().getPrimaryString()+"\","+",\""+(String)SupUser.getFieldValue("DeliverToMailStop")+"\",\""+(String)SupUser.getFieldValue("ReceivingFacilityCode")+"\",\""+(String)SupUser.getFieldValue("ExpenseApprovalCode")+"\",\""+(String)SupUser.getFieldValue("CapitalApprovalCode")+"\",\""+(String)SupUser.getFieldValue("Division")+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")+"\",\""+(String)SupUser.getFieldValue("Department")+"\",\""+(String)SupUser.getFieldValue("Section")+"\",\""+(String)SupUser.getFieldValue("PayrollFacility")+"\",\""+(String)SupUser.getFieldValue("AccountingFacility")+(String)SupUser.getFieldValue("CostCenterString")+"\",\""+(String)SupUser.getFieldValue("CompanyCode")+"\"";
									ariba.base.core.Log.customer.debug("writing " + SupUser.getUniqueName());
									pw.println(temp);
								}
							}

						}

					}
			}
			pw.close();
			Base.getSession().transactionCommit();

			// uncomment after steady state
			ariba.base.core.Log.customer.debug("Ending MissingSuper program ..... ");
	/*		ariba.base.core.Log.customer.debug("Ending MissingSuper program ..... beginning integration");
			CallCenter callCenter = CallCenter.defaultCenter();
			Map userInfo = MapUtil.map();
            Map userData = MapUtil.map();

			userInfo.put("Partition", partition.getName());
			userData.put("EventSource", "PasswordAdapter1");
			callCenter.call("CATSuperPull",userData,userInfo);
			Base.getSession().transactionCommit();
			ariba.base.core.Log.customer.debug("Ending integration and exiting"); */

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public CATMissingSuper()
    {
    }

}

/*******************************************************************************************************************************************/
