/**********
* Created By Nagendra
* writes data to Inclusion file as of now
* Rajat - Changes made to filter users by Payroll Facility in addition to Company Code
********/

package config.java.schedule.sap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.util.core.Fmt;
import ariba.util.core.IOUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;

public class CatSAPUserLoad extends ScheduledTask {

    private String classname = "CatSAPUserLoad";
    private String inclusionFileName = null;
    private String exclusionFileName = null;
    private String userLoadConfigFile = null;


    private Partition partition;
	private String ALL = "ALL";
	private String NONE = "NONE";

	AQLQuery query;
	AQLOptions options;
	AQLResultCollection results;

	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();
            if (key.equals("InclusionsFile")) {
                inclusionFileName  = (String)arguments.get(key);
            }
            if (key.equals("ExclusionsFile")) {
                  exclusionFileName= (String)arguments.get(key);
            }
            if (key.equals("UserConfigFile")) {
                userLoadConfigFile  = (String)arguments.get(key);
            }
		}//end of for loop
	 }

    public void run() throws ScheduledTaskException {

		Log.customer.debug("Beginning UserLoad program .....");
		String headerEncodeString = "Cp1252";
		/*String headerString = "UniqueName,PasswordAdapter,EmployeeNumber,"
					+ "VanillaDeliverTo,DeliverToPhone,DeliverToMailStop,"
					+ "ReceivingFacility,ExpenseApprovalCode,CapitalApprovalCode,"
					+ "Division,AccountingFacility,Department,Section,PayrollFacility,"
					+ "CSVCostCenter,CSVCompany";*/

         String headerString = "UniqueName,PasswordAdapter,ManagementLevel,"
		 					+ "VanillaDeliverTo,"
		 					+ "SAPCompanyCode,CostCenterText,SAPShipTo,SAPPurchaseOrg,SAPPlant,SAPExpenseApprovalCode";

	    PrintWriter inclusionWriter=null;
	    PrintWriter exclusionWriter = null;
	    PrintWriter logWriter = null;
	    BufferedReader br = null;

	    String line = null;
	    List inclusionQueries = ListUtil.list();
	    List exclusionQueries = ListUtil.list();

        partition = Base.getSession().getPartition();
        //boolean isForMFG1 = (partition.getName().equals("mfg1"));

        try {
        	inclusionWriter = new PrintWriter(IOUtil.bufferedOutputStream(new File(inclusionFileName)),true);
        	exclusionWriter = new PrintWriter(IOUtil.bufferedOutputStream(new File(exclusionFileName)),true);
        	logWriter = new PrintWriter(IOUtil.bufferedOutputStream(new File("config/variants/SAP/partitions/SAP/data/CatSAPUserLoad.log")),true);
        	br = new BufferedReader(new FileReader(userLoadConfigFile));

			inclusionWriter.println(headerEncodeString);
			inclusionWriter.println(headerString);
			exclusionWriter.println(headerEncodeString);
			exclusionWriter.println(headerString);

			String userQueryStr = "SELECT u FROM ariba.user.core.User u ";
			String ExcluserQueryStr = "SELECT u FROM ariba.user.core.User u include inactive where" + " \"Active\" = false";

			line = br.readLine(); // read off the header string from the userConfig file

			while ((line = br.readLine()) != null && !StringUtil.nullOrEmptyOrBlankString(line)) {

				Log.customer.debug("%s : UserConfig line read=%s", classname, line);

				StringTokenizer st = new StringTokenizer(line, "|");
				String part, sapcomcode , payfac;

				try {
					part = st.nextToken();
					sapcomcode = st.nextToken();
                    payfac = st.nextToken();

				} catch (Exception nse) {
					Log.customer.debug("%s : UserConfig ERROR while tokenizing line=%s", classname, line, nse.toString());
					throw new ScheduledTaskException("Error: " + nse.toString() + " * while reading userloadconfig line="+line, nse);
				}

				AQLQuery userInclQuery = AQLQuery.parseQuery(userQueryStr );
				boolean addIncQuery = false;
				List incCondList = ListUtil.list();

				if(!StringUtil.nullOrEmptyOrBlankString(sapcomcode) && !sapcomcode.equals(ALL)) {
					String payrollCond = Fmt.S(" SAPCompanyCode = %s", AQLScalarExpression.buildLiteral(sapcomcode.trim()));
					ListUtil.addElementIfAbsent(incCondList, AQLCondition.parseCondition(payrollCond));
				}

				if(!StringUtil.nullOrEmptyOrBlankString(payfac)) {
					String payrollfacilityCond = Fmt.S(" PayrollFacility  = %s", AQLScalarExpression.buildLiteral(payfac.trim()));
					ListUtil.addElementIfAbsent(incCondList, AQLCondition.parseCondition(payrollfacilityCond));
				}

                if(!ListUtil.nullOrEmptyList(incCondList)) {
					AQLCondition inclusionCond = AQLCondition.buildAnd(incCondList);
					userInclQuery.and(inclusionCond);
					addIncQuery = true;
				}


				//add query when atleast one condition is specified.
				if(addIncQuery) {
					ListUtil.addElementIfAbsent(inclusionQueries, userInclQuery);
					Log.customer.debug("%s : NEW Inclusion Query=%s", classname, userInclQuery.toString());
				}
				/********** This part creates the exclusion queries ***************/

								AQLQuery userExclQuery = AQLQuery.parseQuery(ExcluserQueryStr );
								boolean addExcQuery = false;
				            List excCondList = ListUtil.list();
				            //String payrollCond1 = Fmt.S(" Active = %s", false);
						   //ListUtil.addElementIfAbsent(incCondList, AQLCondition.parseCondition(payrollCond1));
                     Log.customer.debug(" NEW Exclusion Query=%s",userExclQuery.toString());
	                  if(!StringUtil.nullOrEmptyOrBlankString(sapcomcode) && !sapcomcode.equals(ALL)) {
						String ExpayrollCond = Fmt.S(" SAPCompanyCode = %s", AQLScalarExpression.buildLiteral(sapcomcode.trim()));
						ListUtil.addElementIfAbsent(excCondList, AQLCondition.parseCondition(ExpayrollCond));
                     Log.customer.debug("sapcodeIF");
                     }

                     if(!StringUtil.nullOrEmptyOrBlankString(payfac)) {
					 		String ExpayrollfacilityCond = Fmt.S(" PayrollFacility  = %s", AQLScalarExpression.buildLiteral(payfac.trim()));
					 		ListUtil.addElementIfAbsent(excCondList, AQLCondition.parseCondition(ExpayrollfacilityCond));
				     }


		              if(!ListUtil.nullOrEmptyList(excCondList)) {
							AQLCondition exclusionCond = AQLCondition.buildAnd(excCondList);
							userExclQuery.and(exclusionCond);
							addExcQuery = true;
							Log.customer.debug(" exccondlistif");
						}



						if(addExcQuery) {
							ListUtil.addElementIfAbsent(exclusionQueries, userExclQuery);
							Log.customer.debug("%s : NEW Exclusion Query=%s", classname, userExclQuery.toString());
						}

			}



			int cnt=0;
			//Write user records to Inclusions.csv
			for(Iterator in = inclusionQueries.iterator(); in.hasNext();) {
				AQLQuery query = (AQLQuery)in.next();

				options = new AQLOptions(partition);
				results = Base.getService().executeQuery(query, options);

				if (results.getErrors() != null) {
					Log.customer.debug("%s:ERROR Inclusion RESULTS :%s ", classname, results.getErrors());
					Log.customer.debug("%s:ERROR:",classname, results.getErrorStatementText());
					throw new ScheduledTaskException("Error in results= "+results.getErrorStatementText() );

				}
				Log.customer.debug("%s : Inclusion query =%s", classname, query.toString());
				Log.customer.debug("%s : Inclusion ResultsSize ="+results.getSize(), classname);

				logWriter.println("Inclusion query " +cnt+ " = " + query.toString());
				logWriter.println("Inclusion " +cnt+ " ResultSize =" + results.getSize());

				BaseId SelUserId = null;
				ariba.user.core.User SelUser = null;
				String incUserRow;

				while (results.next()) {
					for(int i = 0; i < results.getResultFieldCount(); i++) {

						SelUserId = results.getBaseId(0);
						SelUser = (ariba.user.core.User) SelUserId.get();

                        /* Check the AccessType, if accesstype is "No", do not
                         * create the partitioned user */
                        String mscAccessTypeStr = null;
                        ClusterRoot mscAccessObj = (ClusterRoot) SelUser.getFieldValue("MSCAccessType");

                        //Log.customer.debug("User="+SelUser+ " MSCAccessType="+mscAccessObj);

                        if (mscAccessObj != null) {
                        	mscAccessTypeStr = (String)mscAccessObj.getFieldValue("UniqueName");
                        } else {//is null
                                //If MSCAccessType is null, do not add the user
                                continue;
                        }
                        //If MSCAccessType is NO, do not add the user
                        if (!StringUtil.nullOrEmptyOrBlankString(mscAccessTypeStr)
                                        && mscAccessTypeStr.equals("No")) continue;


						incUserRow= SelUser.getUniqueName()+",\""
							+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""
							+(Integer)SelUser.getFieldValue("ManagementLevel")+"\",\""
							+ SelUser.getName().getPrimaryString()+"\","+"\""
							//+((StringUtil.nullOrEmptyOrBlankString((String)SelUser.getFieldValue("Telephone")))?"":((String)SelUser.getFieldValue("Telephone")))+"\",\""
							//+(String)SelUser.getFieldValue("ManagementLevel")+"\",\""
							//+(String)SelUser.getFieldValue("ReceivingFacilityCode")+"\",\""
							+(String)SelUser.getFieldValue("SAPCompanyCode")+"\",\""
							//+(String)SelUser.getFieldValue("CapitalApprovalCode")+"\",\""
							//+(String)SelUser.getFieldValue("Division")+"\",\""
							//+(String)SelUser.getFieldValue("AccountingFacility")+"\",\""
							//+(String)SelUser.getFieldValue("Department")+"\",\""
							//+(String)SelUser.getFieldValue("Section")+"\",\""
							//+(String)SelUser.getFieldValue("PayrollFacility")+"\",\""
							//+((isForMFG1)?(String)SelUser.getFieldValue("AccountingFacility"):"")
							//+(String)SelUser.getFieldValue("CostCenterString")+"\",\""
							+(String)SelUser.getFieldValue("SAPCostCenter")+"\",\""
						    +  "\",\""
						    + "\",\""
						    +"\",\""
						    +(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\"";

						inclusionWriter.println(incUserRow);

					}
				} // end of while for results loop
				results.close();
			} // end of FOR - inclusions
			String mscUserRow = "mscadmin,PasswordAdapter1,,mscadmin,,,,,,,,,,,,";

			inclusionWriter.println(mscUserRow);
			cnt = 0;
						//Write user records to Exclusions.csv
						for(Iterator ex = exclusionQueries.iterator(); ex.hasNext();) {
							AQLQuery query = (AQLQuery)ex.next();

							options = new AQLOptions(partition);
							results = Base.getService().executeQuery(query, options);

							if (results.getErrors() != null) {
								Log.customer.debug("%s:ERROR Exclusion RESULTS :%s ", classname, results.getErrors());
								Log.customer.debug("%s:ERROR:",classname, results.getErrorStatementText());
								throw new ScheduledTaskException("Error in results= "+results.getErrorStatementText() );

							}

							Log.customer.debug("%s : Exclusion query =%s", classname, query.toString());
							Log.customer.debug("%s : Exclusion ResultsSize ="+results.getSize(), classname);

							logWriter.println("Exclusion query " +cnt+ " = " + query.toString());
							logWriter.println("Exclusion " +cnt+ " ResultSize =" + results.getSize());

							BaseId SelUserId = null;
							ariba.user.core.User SelUser = null;
							String excUserRow;

							while (results.next()) {
								for(int i = 0; i < results.getResultFieldCount(); i++) {

									SelUserId = results.getBaseId(0);
									SelUser = (ariba.user.core.User) SelUserId.get();

									excUserRow = SelUser.getUniqueName()+",\""
										+(String)SelUser.getFieldValue("PasswordAdapter")+"\",\""
									+(Integer)SelUser.getFieldValue("ManagementLevel")+"\",\""
									+ SelUser.getName().getPrimaryString()+"\","+"\""
									//+((StringUtil.nullOrEmptyOrBlankString((String)SelUser.getFieldValue("Telephone")))?"":((String)SelUser.getFieldValue("Telephone")))+"\",\""
									//+(String)SelUser.getFieldValue("ManagementLevel")+"\",\""
									//+(String)SelUser.getFieldValue("ReceivingFacilityCode")+"\",\""
									+(String)SelUser.getFieldValue("SAPCompanyCode")+"\",\""
									//+(String)SelUser.getFieldValue("CapitalApprovalCode")+"\",\""
									//+(String)SelUser.getFieldValue("Division")+"\",\""
									//+(String)SelUser.getFieldValue("AccountingFacility")+"\",\""
									//+(String)SelUser.getFieldValue("Department")+"\",\""
									//+(String)SelUser.getFieldValue("Section")+"\",\""
									//+(String)SelUser.getFieldValue("PayrollFacility")+"\",\""
									//+((isForMFG1)?(String)SelUser.getFieldValue("AccountingFacility"):"")
									//+(String)SelUser.getFieldValue("CostCenterString")+"\",\""
									+(String)SelUser.getFieldValue("SAPCostCenter")+"\",\""
									+  "\",\""
									+ "\",\""
									+"\",\""
									+(String)SelUser.getFieldValue("ExpenseApprovalCode")+"\"";
									exclusionWriter.println(excUserRow);

								}
							} // end of while for results loop
							results.close();
			} // end of FOR - exclusions

			inclusionWriter.close();
			exclusionWriter.close();
			br.close();

			Log.customer.debug("Ending integration and exiting");
		}
		catch (Exception e) {
			try {
				br.close();
				inclusionWriter.close();
				//exclusionWriter.close();
			} catch (Exception io) {}
			//Log.customer.debug(e.toString());
			throw new ScheduledTaskException("Error while running CATUserLoad="+ e.toString(), e );
		}
    }

    public CatSAPUserLoad() {}

}
