/*******
* Loading Partition users for R1 - Avinash Rao - April 2005
* Also auto-loads the inclusions file.
* IMPORTANT : USE FOR THE PERKINS PARTITION ONLY
*
* Change:
* Chandra 06-01-06  Exclude users who have MSCAccessType set to "No" or have null access type
*
********/

package config.java.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.IOUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.messaging.CallCenter;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;

public class UserLoadR2 extends ScheduledTask
{

    private String outputFileNameI="Inclusions.csv";
    private String outputFileNameO="Exclusions.csv";
    private String inputFileName="UserLoadConfigR2.txt";

    private Partition partition;

    private PrintWriter pw1,pw2;
    private BufferedReader br;

	private String query1, query2;
	private java.util.Vector vec1 = new java.util.Vector();
	private java.util.Vector vec2 = new java.util.Vector();
	private String line = null;
	private String delim= "|";
	private String all = "ALL";
	private String none = "NONE";

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;

    public void run() throws ScheduledTaskException
    {
		ariba.base.core.Log.customer.debug("Beginning UserLoadR2 program .....");

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

	        line = br.readLine();

			ariba.base.core.Log.customer.debug(line);

			while ((line = br.readLine()) != null)
			{

				StringTokenizer st = new StringTokenizer(line, "|");

				String Partition=st.nextToken();
				String PFCI=st.nextToken();
				String ADI=st.nextToken();
				String DI=st.nextToken();
				String PFCE=st.nextToken();
				String ADE=st.nextToken();
				String DE=st.nextToken();
				String EM=st.nextToken();

				Base.getSession().transactionCommit();

				// ********** This part is the Inclusion queries ***************//

				if (PFCI.equals(all) && ADI.equals(all) && DI.equals(all))
				{
					query1="No Query";
				}

				else if(PFCI.equals(all))
				{
					if (ADI.equals(all))
					{
						query1 = ("SELECT FROM ariba.user.core.User WHERE Division like ("+DI+")");
						ariba.base.core.Log.customer.debug(query1);
					}
					else
					{
						if (DI.equals(all))
						{
							query1 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADI+")");
							ariba.base.core.Log.customer.debug(query1);
						}
						else
						{
							query1 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADI+") and Division like ("+DI+")");
							ariba.base.core.Log.customer.debug(query1);
						}
					}
				}

				else if (ADI.equals(all))
				{
					if (DI.equals(all))
					{
						query1 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCI+")");
						ariba.base.core.Log.customer.debug(query1);
					}
					else
					{
						query1 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCI+") and Division like ("+DI+")");
						ariba.base.core.Log.customer.debug(query1);
					}
				}

				else if (DI.equals(all))
				{
					query1 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCI+") and Department like ("+ADI+")");
					ariba.base.core.Log.customer.debug(query1);
				}

				else
				{
					query1 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCI+") and Department like ("+ADI+") and Division like ("+DI+")");
					ariba.base.core.Log.customer.debug(query1);
				}

				if (!vec1.contains(query1))
				{
					if(!(query1=="No Query"))
					{
					vec1.addElement(query1);
					}
				}

				ariba.base.core.Log.customer.debug(query1);

				// ********** This part creates the exclusion queries ***************//

				if (PFCE.equals(none) && ADE.equals(none) && DE.equals(none) && EM.equals(none))
				{
					query2="No Query";
				}

				else if(PFCE.equals(none))
				{
					if (ADE.equals(none))
					{
						if (DE.equals(none))
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE EmailAddress like ("+EM+")");
							ariba.base.core.Log.customer.debug(query2);
						}
						else
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE Division like ("+DE+") and EmailAddress like ("+EM+")");
							ariba.base.core.Log.customer.debug(query2);
						}
					}
					else
					{
						if (DE.equals(none))
						{
							if (EM.equals(none))
							{
								query2 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADE+")");
								ariba.base.core.Log.customer.debug(query2);
							}
							else
							{
								query2 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADE+") and EmailAddress like ("+EM+")");
								ariba.base.core.Log.customer.debug(query2);
							}
						}
						else
						{
							if (EM.equals(none))
							{
								query2 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADE+") and Division like ("+DE+")");
								ariba.base.core.Log.customer.debug(query2);
							}
							else
							{
								query2 = ("SELECT FROM ariba.user.core.User WHERE Department like ("+ADE+") and Division like ("+DE+") and EmailAddress like ("+EM+")");
								ariba.base.core.Log.customer.debug(query2);
							}
						}
					}
				}

				else if (ADE.equals(none))
				{
					if (DE.equals(none))
					{
						if (EM.equals(none))
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+")");
							ariba.base.core.Log.customer.debug(query2);
						}
						else
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and EmailAddress like ("+EM+")");
							ariba.base.core.Log.customer.debug(query2);
						}
					}
					else
					{
						if (EM.equals(none))
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Division like ("+DE+")");
							ariba.base.core.Log.customer.debug(query2);
						}
						else
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Division like ("+DE+") and EmailAddress like ("+EM+")");
							ariba.base.core.Log.customer.debug(query2);
						}
					}
				}

				else if (DE.equals(none))
				{
					if (EM.equals(none))
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Department like ("+ADE+")");
							ariba.base.core.Log.customer.debug(query2);
						}
					else
						{
							query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Department like ("+ADE+") and EmailAddress like ("+EM+")");
							ariba.base.core.Log.customer.debug(query2);
						}
				}

				else if (EM.equals(none))
				{
					query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Department like ("+ADE+") and Division like ("+DE+")");
					ariba.base.core.Log.customer.debug(query2);
				}

				else
				{
					query2 = ("SELECT FROM ariba.user.core.User WHERE PayrollFacility like ("+PFCE+") and Department like ("+ADE+") and Division like ("+DE+") and EmailAddress like ("+EM+")");
					ariba.base.core.Log.customer.debug(query2);
				}

				if (!vec2.contains(query2))
				{
					if(!(query2=="No Query"))
					{
						vec2.addElement(query2);
					}
				}

				ariba.base.core.Log.customer.debug(query2);

			// ********** End of query generation ***************//

			} // end of while

			for (Enumeration e1 = vec1.elements() ; e1.hasMoreElements() ;)
			{
				String sqlstring = (e1.nextElement()).toString();
				query = AQLQuery.parseQuery(sqlstring);
				options = new AQLOptions(partition);
				results = Base.getService().executeQuery(query,options);

				if (results.getErrors() != null)
				{
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
				}

				BaseId SelUserId = null;
				ariba.user.core.User SelUser = null;
				String temp1;

				while (results.next())
				{
					for(int i = 0; i < results.getResultFieldCount(); i++)
					{

						SelUserId = results.getBaseId(0);
						SelUser = (ariba.user.core.User) SelUserId.get();

						/* Check the AccessType, if accesstype is "No", do not
						 * create the partitioned user */
						String mscAccessTypeStr = null;
			            ClusterRoot mscAccessObj = (ClusterRoot) SelUser.getFieldValue("MSCAccessType");

			            ariba.base.core.Log.customer.debug("User="+SelUser+ " MSCAccessType="+mscAccessObj);

        				if (mscAccessObj != null) {
        				    mscAccessTypeStr = (String)mscAccessObj.getFieldValue("UniqueName");
        				} else {//is null
							//If MSCAccessType is null, do not add the user
							continue;
						}
        				//If MSCAccessType is NO, do not add the user
        				if (!StringUtil.nullOrEmptyOrBlankString(mscAccessTypeStr)
        							&& mscAccessTypeStr.equals("No")) continue;

						ariba.base.core.Log.customer.debug("User="+SelUser+ " MSCAccessTypeStr="+mscAccessTypeStr);


						temp1= SelUser.getUniqueName()+",\""
						+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""
						+(String)SelUser.getFieldValue("EmployeeNumber")+"\",\""
						+SelUser.getName().getPrimaryString()+"\","+",\""
						+(String)SelUser.getFieldValue("DeliverToMailStop")+"\",\""
						+(String)SelUser.getFieldValue("ReceivingFacilityCode")+"\",\""
						+(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\",\""
						+(String)SelUser.getFieldValue("CapitalApprovalCode")+"\",\""
						+(String)SelUser.getFieldValue("Division")+"\",\""
						+(String)SelUser.getFieldValue("AccountingFacility")+"\",\""
						+(String)SelUser.getFieldValue("Department")+"\",\""
						+(String)SelUser.getFieldValue("Section")+"\",\""
						+(String)SelUser.getFieldValue("PayrollFacility")+"\",\""
						+(String)SelUser.getFieldValue("AccountingFacility")
						+(String)SelUser.getFieldValue("CostCenterString")+"\",\""
						+(String)SelUser.getFieldValue("CompanyCode")+"\"";

//						ariba.base.core.Log.customer.debug(temp1);

						pw1.println(temp1);

					}
				} // end of while for results loop
			} // end of FOR - vec1

			// ********** vec2 result handling ***************//

			for (Enumeration e = vec2.elements() ; e.hasMoreElements() ;)
			{
				String sqlstring = (e.nextElement()).toString();
				query = AQLQuery.parseQuery(sqlstring);
				options = new AQLOptions(partition);
				results = Base.getService().executeQuery(query,options);

				if (results.getErrors() != null)
				{
					ariba.base.core.Log.customer.debug("ERROR GETTING RESULTS");
				}

				BaseId SelUserId = null;
				ariba.user.core.User SelUser = null;
				String temp2;

				while (results.next())
				{
					for(int i = 0; i < results.getResultFieldCount(); i++)
					{

						SelUserId = results.getBaseId(0);
						SelUser = (ariba.user.core.User) SelUserId.get();

						temp2= SelUser.getUniqueName()+",\""
						+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""
						+(String)SelUser.getFieldValue("EmployeeNumber")+"\",\""
						+SelUser.getName().getPrimaryString()+"\","+",\""
						+(String)SelUser.getFieldValue("DeliverToMailStop")+"\",\""
						+(String)SelUser.getFieldValue("ReceivingFacilityCode")+"\",\""
						+(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\",\""
						+(String)SelUser.getFieldValue("CapitalApprovalCode")+"\",\""
						+(String)SelUser.getFieldValue("Division")+"\",\""
						+(String)SelUser.getFieldValue("AccountingFacility")+"\",\""
						+(String)SelUser.getFieldValue("Department")+"\",\""
						+(String)SelUser.getFieldValue("Section")+"\",\""
						+(String)SelUser.getFieldValue("PayrollFacility")+"\",\""
						+(String)SelUser.getFieldValue("AccountingFacility")
						+(String)SelUser.getFieldValue("CostCenterString")+"\",\""
						+(String)SelUser.getFieldValue("CompanyCode")+"\"";

//						ariba.base.core.Log.customer.debug(temp2);

						pw2.println(temp2);

					}
				} // end of while for results loop
			} // end of FOR - vec2

			String temp3 = "mscadmin,PasswordAdapter1,,mscadmin,,,,,,,,,,,,";
	//		String temp3 = "aribasystem,PasswordAdapter1,,aribasystem,,,,,,,,,,,,";
			pw1.println(temp3);

			pw1.close();
			pw2.close();
			br.close();

			ariba.base.core.Log.customer.debug("Ending UserLoad program ..... calling integration");

			CallCenter callCenter = CallCenter.defaultCenter();
    		Map userInfo = MapUtil.map();
            Map userData1 = MapUtil.map();

			userInfo.put("Partition", partition.getName());
			userData1.put("EventSource", "PasswordAdapter1");
            userData1.put("Filename", "Inclusions.csv");

            callCenter.call("CATInclusionsPull",userData1,userInfo);
			ariba.base.core.Log.customer.debug("called inclusions ...");
			Base.getSession().transactionCommit();
			ariba.base.core.Log.customer.debug("Ending integration and exiting");

		} // end of try

		catch (Exception e)
		{
			ariba.base.core.Log.customer.debug(e.toString());
			return;
		}
    }

    public UserLoadR2()
    {
    }

}

/*******************************************************************************************************************************************/
