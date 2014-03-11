/*******************************************************************************************************************************************/

// Loading Partition users for R1 - Avinash Rao - April 2005
// Reads the config file maintained to create the CATSupportUser and CATSupportUserDelete files.

/*******************************************************************************************************************************************/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.core.MapUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class CATSupportUserConfig extends ScheduledTask
{

    private String outputFileNameI="CATSupportUser.csv";
    private String outputFileNameO="CATSupportUserDelete.csv";
    private String inputFileName="CATSupportUserConfig.csv";
    private Partition partition;
    private PrintWriter pw1,pw2;
    private BufferedReader br;
	private String line = null;
	private String delim= ",";

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;

    public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Beginning Converter program .....");

        partition = Base.getSession().getPartition();

        String fileNameI = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileNameI;
        File outputFileI = new File(fileNameI);
        String fileNameO = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + outputFileNameO;
        File outputFileO = new File(fileNameO);
        String fileName2 = "config/variants/" + partition.getVariant().getName() + "/partitions/" + partition.getName() + "/data/" + inputFileName;
        File inputFile = new File(fileName2);

        try
        {
        	pw1 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileI),true);
        	pw2 = new PrintWriter(IOUtil.bufferedOutputStream(outputFileO),true);
        	br = new BufferedReader(new FileReader(inputFile));

			pw1.println("Cp1252");
			pw1.println("UniqueName,PasswordAdapter,EmployeeNumber,VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,Division,AccountingFacility,Department,Section,PayrollFacility,CSVCostCenter,CSVCompany");
			pw2.println("Cp1252");
			pw2.println("UniqueName,PasswordAdapter,EmployeeNumber,VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,Division,AccountingFacility,Department,Section,PayrollFacility,CSVCostCenter,CSVCompany");

	        String line = br.readLine();

			ariba.base.core.Log.customer.debug(line);

			while ((line = br.readLine()) != null)
			{

				StringTokenizer st = new StringTokenizer(line, ",");

				String UniqueName=st.nextToken();
				String PasswordAdapter=st.nextToken();
				String Name=st.nextToken();
				String AD=st.nextToken();

				BaseId userId = null;
				ariba.user.core.User SelUser = null;
				String temp;

				String sqlstring = "select from ariba.user.core.User where UniqueName="+"\'"+UniqueName+"\'";
				ariba.base.core.Log.customer.debug(sqlstring);
				query = AQLQuery.parseQuery(sqlstring);
				options = new AQLOptions(partition);
				results = Base.getService().executeQuery(query,options);
				if (results.getErrors() != null)
				{
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS for "+ UniqueName);
				}

				while (results.next())
				{
					for(int i = 0; i < results.getResultFieldCount(); i++)
					{
						userId = results.getBaseId(0);
						SelUser = (ariba.user.core.User) userId.get();

						temp= SelUser.getUniqueName()+",\""+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""+(String)SelUser.getFieldValue("EmployeeNumber")+"\",\""+SelUser.getName().getPrimaryString()+"\","+",\""+(String)SelUser.getFieldValue("DeliverToMailStop")+"\",\""+(String)SelUser.getFieldValue("ReceivingFacilityCode")+"\",\""+(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\",\""+(String)SelUser.getFieldValue("CapitalApprovalCode")+"\",\""+(String)SelUser.getFieldValue("Division")+"\",\""+(String)SelUser.getFieldValue("AccountingFacility")+"\",\""+(String)SelUser.getFieldValue("Department")+"\",\""+(String)SelUser.getFieldValue("Section")+"\",\""+(String)SelUser.getFieldValue("PayrollFacility")+"\",\""+(String)SelUser.getFieldValue("AccountingFacility")+(String)SelUser.getFieldValue("CostCenterString")+"\",\""+(String)SelUser.getFieldValue("CompanyCode")+"\"";

						if (AD.equals("add"))
						{
							pw1.println(temp);
						}

						if (AD.equals("delete"))
						{
							pw2.println(temp);
						}
					}
				}

			} // end of while

			pw1.close();
			pw2.close();
			br.close();

			Base.getSession().transactionCommit();
			ariba.base.core.Log.customer.debug("Ending config program ..... beginning integration");
			CallCenter callCenter = CallCenter.defaultCenter();
			Map userInfo = MapUtil.map();
            Map userData = MapUtil.map();

			userInfo.put("Partition", partition.getName());
			userData.put("EventSource", "PasswordAdapter1");
			callCenter.call("CATSupportUserPull",userData,userInfo);
			Base.getSession().transactionCommit();

			Map userInfo1 = MapUtil.map();
			Map userData1 = MapUtil.map();

			userInfo1.put("Partition", partition.getName());
			userData1.put("EventSource", "PasswordAdapter1");
			callCenter.call("CATSupportUserDeletePull",userData1,userInfo1);
			Base.getSession().transactionCommit();

			ariba.base.core.Log.customer.debug("Ending integration and exiting");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public CATSupportUserConfig()
    {
    }

}

/*******************************************************************************************************************************************/
