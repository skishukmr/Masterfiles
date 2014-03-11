/*
 * Created by Chandra on Aug 21, 2006
 * --------------------------------------------------------------
 * This is a new Scheduled Task to move the approvables from one user to the other user.
 * Preparer, Requester and approver is considered.
 *
 */
/** Usage:
        CatMigrateApprovablesToNewUser = {
                ScheduledTaskClassName = "config.java.schedule.CATMigrateApprovable";
                LogFile = "logs/changeuseronapp/CatUserChangeLog.txt";
    };

    CHANGE HISTORY

    Changed by Kavitha Udayasankar on Dec 07 2007
    ------------------------------------------------------------
    1.Modified the Query to include limited set of approvables(Requisition,Contract,ContractRequest,Receipts,InvoiceReconciliaiton) for migration
    2.Getting the fromUser and toUser object to clear the cache error

    Changed by Amit kumar on 22-04-08
	    ------------------------------------------------------------
	1.Added transaction commit to avoid SMTP warning while sending email.

	Changed by Ashwini on 23-06-08
	----------------------------------------------------------
	1)Issue 825 :if condition for dummy record
	2)Issue 822 :Added replace function after the cupid migration.

	Changed by Ashwini on 12-02-09
		----------------------------------------------------------
	1)899 - Hazmat approvables null pointer fix


**/

package config.java.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.ApproverList;
import ariba.approvable.core.Comment;
import ariba.approvable.core.Folder;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.fields.Fields;
import ariba.common.core.UserProfile;
import ariba.user.core.Principal;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.SystemUtil;
import ariba.util.formatter.DateFormatter;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import config.java.common.CatCommonUtil;
import config.java.common.CatEmailNotificationUtil;



public class CATMigrateApprovable extends ScheduledTask {

	private String classname="CATMigrateApprovable";
    private String logFilePath = null;
    private List preparerAppr = null;
    private List requesterAppr = null;
    private List approverAppr = null;
    private FastStringBuffer message = null;
    private int migrateApprObjCnt = 0;
    private String mailSubject = null;
    String validApprovables = ResourceService.getString("cat.vcsv1","MigrateApprovables");


	public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)  {

		super.init(scheduler, scheduledTaskName, arguments);
		for(Iterator e = arguments.keySet().iterator(); e.hasNext();)  {
			String key = (String)e.next();

			if(key.equals("LogFile")) {
				try  {
					logFilePath = (String)arguments.get(key);
				} catch(Exception ioexception) {
					Log.customer.debug("%s ***ERROR-- " , classname, ioexception.toString());
				}
			}
		}//end of for loop
	 }

    // define the basic AQL options globally
    public AQLOptions baseOptions() {
       AQLOptions options = new AQLOptions();
       options.setRowLimit(0);
       options.setUserLocale(Base.getSession().getLocale());
       options.setUserPartition(Base.getSession().getPartition());
       return options;
    }

    public void run() throws ScheduledTaskException {

		mailSubject = "Migrate Approvables Task Completion Status - Completed Successfully";
		Log.customer.debug("%s: Beginning CATMigrateApprovable program .....", classname);
		//dump the details in a log file
		File cwdFile = SystemUtil.getCwdFile();
		BufferedWriter out = null;
		message = new FastStringBuffer();

		try {
			File logFile = new File(cwdFile, logFilePath);
			if (!logFile.exists()){
				logFile.createNewFile();
			}
			out = new BufferedWriter(new FileWriter(logFile, true));

			out.write("\n");
			message.append("\n");
			out.write("\n"+ Fmt.S("%s : %s ",currentTime(), "Running  CATMigrateApprovable" ));
			message.append("\n");
			message.append(currentTime() + "  CATMigrateApprovable Started...");
			AQLQuery usrquery = new AQLQuery("cat.core.CatMigrateApprovable");

			AQLResultCollection userResults = Base.getService().executeQuery(usrquery, baseOptions());

			if(userResults.getErrors() != null) {
				Log.customer.debug("%s:ERROR RESULTS for:%s ", classname, userResults.getErrors());
				out.write("\n"+ Fmt.S("%s : ERROR: %s (Transactions rolledBack)",currentTime(), userResults.getErrorStatementText() ));

				message.append("\n"+ Fmt.S("%s : ERROR: %s (Transactions rolledBack)",currentTime(), userResults.getErrorStatementText() ));
				throw new ScheduledTaskException("Error in results= "+userResults.getErrorStatementText() );
			} else  {
				while (userResults.next()) {
					migrateApprObjCnt++;
					BaseId ucbid = userResults.getBaseId(0);
					BaseObject uc = (BaseObject)ucbid.get();

					String oldUserStr = (String)uc.getFieldValue("UniqueName");
					String newUserStr = (String)uc.getFieldValue("NewUserId");

					//used to reset catmigrateapprovable objects - one dummy record is present - no action required.

					// ****************ISSUE 825 *********************
					if(oldUserStr.equalsIgnoreCase("dummyrecord") || newUserStr.equalsIgnoreCase("dummyrecord")) continue;
					// ****************ISSUE 825 *********************
					Partition partition = Base.getSession().getPartition();
					User fromUser = null;
					User toUser = null;

					// Get the user objects for these user ids
					String queryText = "SELECT uu "
						+ "FROM ariba.user.core.User uu INCLUDE INACTIVE, "
						+ "ariba.common.core.User cu INCLUDE INACTIVE "
						+ "WHERE cu.\"User\"=uu "
						+ "AND uu.UniqueName = '%s' ";

					//FROM User obj
			        AQLResultCollection results = Base.getService().executeQuery(Fmt.S(queryText, oldUserStr), baseOptions());
					if (results.getErrors() != null) {
						throw new ScheduledTaskException("(FROM USER fetch) Error in results= " + results.getErrorStatementText() );
					}

			        if(results.next()){
						fromUser = (ariba.user.core.User) results.getBaseId(0).get();
					}

					results = null;
					//TO User obj
			        results = Base.getService().executeQuery(Fmt.S(queryText, newUserStr), baseOptions());
					if (results.getErrors() != null) {
						throw new ScheduledTaskException("(TO USER fetch) Error in results= " + results.getErrorStatementText() );
					}

			        if(results.next()) {
						toUser = (ariba.user.core.User) results.getBaseId(0).get();
					}

					Log.customer.debug("%s: FROM user=" + fromUser + " TO user=" +toUser, classname);

					boolean resetActiveFlagOnUser = false;

					if (fromUser != null && toUser != null) {

						if (!fromUser.getActive()) {
							resetActiveFlagOnUser = true;
							fromUser.setActive(true);
							Base.getSession().transactionCommit();
						}


						//Getting the fromUser and toUser object to clear the cache error
						BaseId fromUserId=(BaseId)fromUser.getBaseId();
						BaseId toUserId=(BaseId)toUser.getBaseId();
						fromUser = (ariba.user.core.User)Base.getSession().objectFromId(fromUserId);
						toUser = (ariba.user.core.User)Base.getSession().objectFromId(toUserId);

						out.write("\n"+ Fmt.S("%s : From %s (%s) - To %s (%s)",currentTime(),
													fromUser.getMyName(), fromUser.getUniqueName(),
													toUser.getMyName(), toUser.getUniqueName() ));

						message.append("\n"+ Fmt.S("%s : From %s (%s) - To %s (%s)",currentTime(),
													fromUser.getMyName(), fromUser.getUniqueName(),
													toUser.getMyName(), toUser.getUniqueName() ));

						//moving the Approvables Preparers from old user to new user
						moveAppPrep(fromUser,toUser);
						//moving the Approvables Requester from old user to new user
						moveAppRequester(fromUser,toUser);
						//moving the Approvers in a requisition
						moveApp(fromUser,toUser);


						//Write to the file the Approvable #s if the above calls are successful
						out.write("\n"+ Fmt.S("%s : Preparer changed on %s",currentTime(),
											ListUtil.listToCSVString(preparerAppr)));
			            message.append("\n"+ Fmt.S("%s : Preparer changed on %s",currentTime(),
											ListUtil.listToCSVString(preparerAppr)));

						out.write("\n"+ Fmt.S("%s : Requester changed on %s",currentTime(),
											ListUtil.listToCSVString(requesterAppr)));
					    message.append("\n"+ Fmt.S("%s : Requester changed on %s",currentTime(),
											ListUtil.listToCSVString(requesterAppr)));

						out.write("\n"+ Fmt.S("%s : Active Approvers changed on %s",currentTime(),
											ListUtil.listToCSVString(approverAppr)));
						message.append("\n"+ Fmt.S("%s : Active Approvers changed on %s",currentTime(),
											ListUtil.listToCSVString(approverAppr)));

						out.write("\n"+ Fmt.S("%s : %s ",currentTime(), "Finished Running CATMigrateApprovable" ));
						message.append("\n"+ Fmt.S("%s : %s ",currentTime(), "Finished Running CATMigrateApprovable" ));
						out.newLine();
						message.append("\n");

						if(resetActiveFlagOnUser) {
							fromUser.setActive(false);
							resetActiveFlagOnUser = false;
						}




					} else {
						Log.customer.debug("%s: ONE of the user is null ====== FROM user=" + fromUser + " TO user=" +toUser, classname);
						message.append("\n"+ Fmt.S("%s : %s ",currentTime(), "ONE of the user is null, User not found.."+fromUser+ " and "+ toUser+".  TASK ABORTED" ));
					}
        		}  // end of while



        	}
		} catch (Exception ex) {
			Log.customer.debug("%s: ERROR:ex=%s ", classname, ex.toString());
			try {
				out.write("\n"+ Fmt.S("%s : ERROR: %s (Transactions rolledBack)\n",currentTime(), ex.toString() ));
				message.append("\n"+ Fmt.S("%s : ERROR: %s (Transactions rolledBack)\n",currentTime(), ex.toString() ));
				mailSubject = "Migrate Approvables Task FAILED";
			}catch(Exception fwex){}
			throw new ScheduledTaskException("Error while running CATMigrateApprovables", ex);
		} finally {
			Log.customer.debug("%s: Inside Finally ", classname);
			try {
				out.flush();
            	out.close();

			} catch (Exception io) {
				Log.customer.debug("%s: ERROR:io=%s  ", classname, io.toString());
			}
			// Sending email
			if(migrateApprObjCnt > 1)
				CatEmailNotificationUtil.sendEmailNotification(mailSubject, message.toString(), "cat.java.emails", "CUPIDMigration");
			Base.getSession().transactionCommit();
			message = null;
			mailSubject = null;
			migrateApprObjCnt = 0;
		}
    } //end

	private String currentTime() {
		return DateFormatter.toPaddedConciseDateTimeString(Date.getNow());
	}

	// to migrate the preparers for the Approvables (Requisition,Contract,ContractRequest,Receipts,InvoiceReconciliaiton)
    private void moveAppPrep(User fromUser, User toUser) {

		String olduserid = (String)fromUser.getUniqueName();

		String apprQryStr = "Select a from ariba.approvable.core.Approvable a subclass ("+validApprovables+") where Preparer";

		apprQryStr = Fmt.S("%s = %s", apprQryStr, AQLScalarExpression.buildLiteral(fromUser.getBaseId()));
		AQLQuery apprQry = AQLQuery.parseQuery(apprQryStr);
		preparerAppr = ListUtil.list();

		Log.customer.debug("%s:apprQry=%s", classname,apprQry.toString());
        AQLResultCollection result = Base.getService().executeQuery(apprQry, baseOptions());

		if(result.getErrors() != null) {
			Log.customer.debug("%s:ERROR RESULTS for:%s ", classname, result.getErrors());
			return;
		} else  {
			while (result.next()) {
            	BaseId appbid = result.getBaseId(0);
            	Approvable app = (Approvable)appbid.get();

				Log.customer.debug("%s:apprv=%s", classname,app.getUniqueName());


            	if (app instanceof UserProfile) {
					Log.customer.debug("%s:INFO: UserProfile skipped", classname);
                	continue;
            	} else {
	                // get the old and new user names for adding their names in the Internal Comments
	                String oldusername = (String)fromUser.getDottedFieldValue("Name.PrimaryString");
                	String newusername = (String)toUser.getDottedFieldValue("Name.PrimaryString");

					Log.customer.debug("%s: oldusername=%s", classname, oldusername);
					Log.customer.debug("%s: newusername=%s", classname, newusername);

                    app.setPreparer(toUser);

                    // adding comments
                    String note = " Ownership of this approvable was transferred from "
                    				+ oldusername + " to " + newusername
                    				+ " based on a request received by MSC Support.";
                    String title = "MSC Administrator";

                    addComment(app, note, title);
                    app.setLastModified(Fields.getService().getNow());
                    Log.customer.debug("%s: set the preparer, comment, modifieddate and save", classname);
                    app.save();
                    addApprovableToFolder(3,toUser, app);
                    ListUtil.addElementIfAbsent(preparerAppr, app.getUniqueName());
	            }
       		} //end of while
		}
	} //end of void

	// to migrate the requesters for the Approvables (Requisition,Contract,ContractRequest,Receipts,InvoiceReconciliaiton)
    private void moveAppRequester(User fromUser, User toUser) {

		String olduserid = (String)fromUser.getUniqueName();
		String apprQryStr = "Select a from ariba.approvable.core.Approvable a subclass ("+validApprovables+") where Requester";
		apprQryStr = Fmt.S("%s = %s", apprQryStr, AQLScalarExpression.buildLiteral(fromUser.getBaseId()));
		AQLQuery apprQry = AQLQuery.parseQuery(apprQryStr);
		requesterAppr = ListUtil.list();

		Log.customer.debug("%s:apprQry=%s", classname,apprQry.toString());
        AQLResultCollection result = Base.getService().executeQuery(apprQry, baseOptions());

		if(result.getErrors() != null) {
			Log.customer.debug("%s:ERROR RESULTS for:%s ", classname, result.getErrors());
			return;
		} else  {
			while (result.next()) {
            	BaseId appbid = result.getBaseId(0);
            	Approvable app = (Approvable)appbid.get();

				Log.customer.debug("%s:apprv=%s", classname,app.getUniqueName());

            	if (app instanceof UserProfile) {
					Log.customer.debug("%s:INFO: UserProfile skipped", classname);
                	continue;
            	} else {
	                // get the old and new user names for adding their names in the Internal Comments
	                String oldusername = (String)fromUser.getDottedFieldValue("Name.PrimaryString");
                	String newusername = (String)toUser.getDottedFieldValue("Name.PrimaryString");

					Log.customer.debug("%s: oldusername=%s", classname, oldusername);
					Log.customer.debug("%s: newusername=%s", classname, newusername);

                    app.setRequester(toUser);

                    // adding comments
                    String note = " Ownership of this approvable was transferred from "
                    				+ oldusername + " to " + newusername
                    				+ " based on a request received by MSC Support.";
                    String title = "MSC Administrator";

					//chk if preparer is not the same as requester (avoid multiple comments)
					if(app.getPreparer() != app.getRequester())
                    	addComment(app, note, title);

                    app.setLastModified(Fields.getService().getNow());
                    Log.customer.debug("%s: set the preparer, comment, modifieddate and save", classname);
                    app.save();
                    ListUtil.addElementIfAbsent(requesterAppr, app.getUniqueName());
	            }
       		} //end of while
		}
	} //end of void


    // To move the approvers from old user to new user
    private void moveApp(User fromUser, User toUser) {

		Log.customer.debug("%s: In Move App", classname);
		String apprQryStr = "SELECT Approvable FROM ariba.approvable.core.Approvable AS Approvable "
						+"	JOIN ariba.approvable.core.ApprovalRequest USING ApprovalRequests "
						+" LEFT OUTER JOIN ariba.user.core.Approver AS ApproverRole USING ApprovalRequest.Approver "
						+" where ApproverRole = %s "
						+" AND Approvable.StatusString <> 'Denied' "
						+" AND Approvable.ApprovalRequests.State in (2, 1)";

		AQLQuery apprQry = AQLQuery.parseQuery(Fmt.S(apprQryStr, AQLScalarExpression.buildLiteral(fromUser.getBaseId())));
		Log.customer.debug("%s: apprQry =%s", classname, apprQry.toString());
		approverAppr = ListUtil.list();

        AQLResultCollection result = Base.getService().executeQuery(apprQry,baseOptions());

		if(result.getErrors() != null) {
			Log.customer.debug("%s:ERROR RESULTS =:%s ", classname, result.getErrors());
			return;
		} else  {
			while (result.next()) {

	            BaseId appbid = result.getBaseId(0);
	            Approvable app = (Approvable)appbid.get();

	            String appid = (String)app.getUniqueName();
	            String oldusername = (String)fromUser.getDottedFieldValue("Name.PrimaryString");
	            String newusername = (String)toUser.getDottedFieldValue("Name.PrimaryString");

			    String oldusrid = (String)fromUser.getUniqueName();
			    String newusrid = (String)toUser.getUniqueName();

				Log.customer.debug("%s:apprv with Approver on flow =%s", classname, appid);

				BaseVector approvals = CatCommonUtil.getAllApprovalRequests(app);
			    Log.customer.debug("%s # of approvals: " + approvals.size(),classname );

	     		for (Iterator ars = approvals.iterator(); ars.hasNext();) {
	     			ApprovalRequest ar = (ApprovalRequest)ars.next();
	  				Log.customer.debug("%s Approver: %s", classname, ar.getApprover().getUniqueName());
	 				Log.customer.debug("%s state: " + ar.getState(), classname);

					Log.customer.debug("%s: approvalrequest=" + ar , classname);

                    if((ar.getState() == 1 || ar.getState() == 2)
                    		&& ar.getApprover()!= null && ar.getApprover() instanceof User) {
						User approver = (User)ar.getApprover();

						Log.customer.debug("%s:approver=" + approver + " and fromuser = " +fromUser, classname);

						if(approver == fromUser) {
							ar.setApprover(toUser);

                            // adding the internal comments
                            String note = " Approver " + oldusername + " was changed to approver "
                            		+ newusername + " based on a request received by MSC Support. ";
                            String title = "MSC Administrator";

                            addComment(app, note, title);
                            //save the approvable before saving and set the LastModifiedDate
                            app.setLastModified(Fields.getService().getNow());

                            app.save();
                            ListUtil.addElementIfAbsent(approverAppr, app.getUniqueName());

                            if (ar.getState() == ApprovalRequest.StateActive ) {
								addApprovableToFolder(2, toUser, app);
                            }
                        }
					}
				}
			}
		}



		String apprLiQryStr = " SELECT Approvable "
							+"	FROM ariba.approvable.core.Approvable AS Approvable "
							+"	JOIN ariba.approvable.core.ApprovalRequest USING ApprovalRequests "
							+"	LEFT OUTER JOIN ariba.user.core.Approver AS ApproverRole USING ApprovalRequest.Approver "
							+"	where "
							+"	ApproverRole in (select al "
							+"	from ariba.approvable.core.ApproverList al "
							+"	LEFT OUTER JOIN ariba.user.core.Approver AS ApproverUser using Approvers "
							+"	where ApproverUser = %s) "
							+"	AND Approvable.StatusString <> 'Denied' "
							+"	AND Approvable.ApprovalRequests.State in (2, 1) ";

		AQLQuery apprLiQry = AQLQuery.parseQuery(Fmt.S(apprLiQryStr, AQLScalarExpression.buildLiteral(fromUser.getBaseId())));
		Log.customer.debug("%s: apprLiQry =%s", classname, apprLiQry.toString());
        AQLResultCollection res = Base.getService().executeQuery(apprLiQry,baseOptions());

		if(res.getErrors() != null) {
			Log.customer.debug("%s:ERROR RESULTS =:%s ", classname, result.getErrors());
			return;
		} else  {
			while (res.next()) {

	            BaseId appbid = res.getBaseId(0);
	            Approvable app = (Approvable)appbid.get();

	            String appid = (String)app.getUniqueName();
				String apprType1 = "ariba.receiving.core.Receipt";
				Log.customer.debug("%s:******ApprovalTttttYpe=%s", classname,apprType1);
	            String apprType = app.getType();
				Log.customer.debug("%s:ApprovalTYpe=%s", classname,apprType);
	            String oldusername = (String)fromUser.getDottedFieldValue("Name.PrimaryString");
	            String newusername = (String)toUser.getDottedFieldValue("Name.PrimaryString");

			    String oldusrid = (String)fromUser.getUniqueName();
			    String newusrid = (String)toUser.getUniqueName();

			    Log.customer.debug("%s:apprv2=%s", classname, appid);

				BaseVector approvals = CatCommonUtil.getAllApprovalRequests(app);
			    Log.customer.debug("%s # of approvals2: " + approvals.size(),classname );

	     		for (Iterator ars = approvals.iterator(); ars.hasNext();) {
	     			ApprovalRequest arq = (ApprovalRequest)ars.next();
	  				Log.customer.debug("%s Approver2: %s", classname, arq.getApprover().getUniqueName());
	 				Log.customer.debug("%s state2: " + arq.getState(), classname);

				//get all the active approvers and check whether any approver belongs to this old user
                    if((arq.getState() == 2 || arq.getState() == 1)
                    		&& arq.getApprover()!= null && arq.getApprover() instanceof ApproverList) {

						ApproverList approverList = (ApproverList)arq.getApprover();

								String approverListName = (String)approverList.getDottedFieldValue("Name.PrimaryString");
								Log.customer.debug("%s: Original approverListName ="+approverListName, classname);

								String fromUsrUniqueName = (String)fromUser.getFieldValue("UniqueName");
								Log.customer.debug("%s: fromUsrUniqueName ="+fromUsrUniqueName, classname);

								String fromUsrName = (String)fromUser.getDottedFieldValue("Name.PrimaryString");
								Log.customer.debug("%s: fromUsrName ="+fromUsrName, classname);


								boolean approverChanged = false;
									 for(Iterator arl = approverList.getApproversIterator(); arl.hasNext();) {
										    BaseId ar = (BaseId)arl.next();
										    if(ar.equals(fromUser.getBaseId())) {
												     BaseVector approvers = (BaseVector) approverList.getApprovers();
												     boolean addStat = approvers.add(toUser.getBaseId());
												     Log.customer.debug("%s: New Approver added to approaval List ="+addStat, classname);

												     Log.customer.debug("%s: approvers in AL ="+ approvers, classname);
												     approvers.removeAll(fromUser.getBaseId());
												     Log.customer.debug("%s: approvers in AL after removing ="+ approvers, classname);
												     approverChanged = true;
												     break;
											}

										}

										if (apprType1.equals(apprType) && (approverListName != null) ) // Issue 899 - Hazmat null pointer
										{
											Log.customer.debug ("******Equals *****");


									//***********ISSUE 822 ***********************
							       if(approverChanged){
											Log.customer.debug ("******INSIDE REPLACEALL *****");
								             String ChangedApproverListName = approverListName.replaceAll(fromUsrUniqueName,newusrid);
								             Log.customer.debug("%s:******************* approverListName after replace***********="+ChangedApproverListName, classname);

											 approverList.setDottedFieldValue("Name.PrimaryString",ChangedApproverListName);
											 Log.customer.debug("%s:******************* approverListName after setting***********="+approverList.getName().getPrimaryString(), classname);
                              		}
								}
                              	//*************** ISSUE 822 *************************
					}
                    // adding the internal comments
                    String note = " Approver "+ newusername + " was added to the approver List based on a request received by MSC Support. ";
                    String title = "MSC Administrator";

                    addComment(app, note, title);
                    //save the approvable before saving and set the LastModifiedDate
                    app.setLastModified(Fields.getService().getNow());

                    app.save();
                    ListUtil.addElementIfAbsent(approverAppr, app.getUniqueName());

                    if (arq.getState() == ApprovalRequest.StateActive ) {
						addApprovableToFolder(2, toUser, app);
					}
               }
			}
		}
	} //end of while req_results

    //Adding Internal Comments
    private void addComment(Approvable app, String note, String title) {
		Log.customer.debug("%s: addComment=%s", classname, note);
		Comment comment = new Comment (app.getPartition());
		comment.setType(1);
		comment.setDate(Fields.getService().getNow());
		comment.setUser(User.getAribaSystemUser(app.getPartition()));
		comment.setText(new LongString(note));
		comment.setTitle(title);
		comment.setExternalComment(false);
		comment.setParent((BaseObject)app);
		app.getComments().add(comment);
    }

    //Adding items to Folder
	private void addApprovableToFolder(int folderCategory, User toUser, Approvable approvable) {
		Log.customer.debug("%s: addApprovableToFolder approvable=", classname, approvable.getUniqueName());
		Folder.addItem(folderCategory, (Principal)toUser, approvable);
    } //end of void additems to folder

}
