/********************************************************************************
*
*CATUserGroupRpt.java
*
*Date: 03/08/2006
*
*Purpose: The purpose of this CATUserGroupPpt.java is to create a text file which
*contains the cupid's and the NARSApplicaitonId associated with the users
*
********************************************************************************/

package config.java.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

import ariba.base.core.Base;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CATUserGroupRpt extends ScheduledTask {

	private static final String classname = "CATUserGroupRpt";
    private Partition partition = null;
    String outputFileName=null;



	/* This method Reads all the parameters from ScheduleTask entry
	   and also the path and the file name
	*/
	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)  {
		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();

			if(key.equals("OutputFile")) {
				try  {
					outputFileName = (String)arguments.get(key);
				} catch(Exception ioexception)  {
					outputFileName=null;
				}
			}
		}//end of for loop
	 }

	public void run() throws ScheduledTaskException  {

		Log.customer.debug("%s: Beginning CATUserGroupRpt program .....", classname);
		Log.customer.debug("%s: outputfile name =%s" , classname,outputFileName);
		if (outputFileName==null) {
			return;
		}

		// If there already exists a file then that file should be deleted
		File checkFile = new File(outputFileName);
		if (checkFile.exists())   {
			checkFile.delete();
			Log.customer.debug("%s: deleted already Existing  File  Successfully." , classname);
		}

		File outFile = new File(outputFileName);
		partition = Base.getSession().getPartition();
		Log.customer.debug("%s: partition Value    ::%s:", classname, partition);

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile, true));
			String sqlString = "select \"Group\".NARSApplicationID, Users.UniqueName"
					+" from ariba.user.core.Group join "
					+" ariba.user.core.\"User\" using Users "
					+" where Users.UniqueName <> 'mscadmin' order by Users.Name";

				AQLQuery aqlQuery = AQLQuery.parseQuery(sqlString);
				Log.customer.debug(aqlQuery.toString());
				AQLOptions options = new AQLOptions(Base.getSession().getPartition());
				Log.customer.debug("%s: aql query =%s" , classname, aqlQuery.toString());

				AQLResultCollection queryResults = Base.getService().executeQuery(aqlQuery, options);

				if(queryResults.getErrors() != null) {
					Log.customer.debug("%s:ERROR RESULTS for:%s ", classname, queryResults.getErrors());
					return;
				} else  {
					while (queryResults.next()) {

						String groupAppId = queryResults.getString(0);
						if(groupAppId == null) groupAppId = "";

						if(groupAppId.length() > 24) {
							groupAppId = groupAppId.substring(0,24);
						}

						//Log.customer.debug("%s: groupAppId lenght="+ groupAppId.length(), classname);
						int grpCurrLen = groupAppId.length();
						for(int i=0; i < 62 - grpCurrLen; i++) {
							groupAppId = groupAppId + " ";
						}

						//Log.customer.debug("%s: groupAppId lenght after space add="+ groupAppId.length(), classname);
						//Log.customer.debug("%s: groupAppId="+ groupAppId+"=", classname);
						String userCupid = queryResults.getString(1);
						if(userCupid == null) userCupid = "";

						if(userCupid.length() > 10) {
							userCupid = userCupid.substring(0,10);
						}
						int cupCurrLen = userCupid.length();
						for(int j=0; j < 10 - cupCurrLen; j++)
							userCupid = userCupid + " ";

						//Log.customer.debug("%s: userCupid lenght="+userCupid.length(), classname);

						//Log.customer.debug("%s: Data to write===%s===" , classname, groupAppId + userCupid);

						try {
							out.write( groupAppId + userCupid + "\n");
						} catch (Exception e) {
							Log.customer.debug("%s: ERROR = %s" , classname, e.toString());
							System.err.println(e);
						}
					}
				 }  //end of else
				 out.close();
			}

			catch (Exception e) {
				Log.customer.debug("%s: RUN ERROR = %s" , classname, e.toString());
				System.err.println(e);
			}
	}  // End of Run Method...

    public CATUserGroupRpt()  {}

    }
