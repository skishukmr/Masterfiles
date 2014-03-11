/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1   Rajani      04/02/2008      Issue #785, read Email body from resource file and email API is change to CatEmailNotificationUtil.
2. Shailaja Salimath   09/04/2009 CR177:Changing email notification subject line, adding SOE number and partition name

***************************************************************************************** */


package config.java.hook;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import java.util.List;
import ariba.util.log.Log;
import ariba.util.core.Fmt;
import ariba.base.core.Base;
import ariba.base.core.aql.*;
import ariba.util.core.SystemUtil;
import java.sql.*;
import java.net.*;
import ariba.base.core.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import config.java.common.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import config.java.common.CatEmailNotificationUtil;
import java.io.*;
import ariba.util.core.StringUtil;
import java.util.ListIterator;
//import ariba.base.core.Log;

public class CatEZOSupplierHook implements ApprovableHook
    {
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private AQLResultCollection results;
    private AQLQuery qry ;
    private String query;
    private AQLOptions options;
    String prefOrdering;

    public List run(Approvable approvable)
        {
			Log.customer.debug("*** entering eform submit core ***");
			ClusterRoot useform = (ClusterRoot)approvable;
			String s1,s2,s3,s41,s42,s5,s6,s7,s8,s9,s10,s11,s12,s13,s14,s15,s16,s17,s18,s19,s20,s21,s22,s23,s32;//s33;
			String changes ="";
			String asnFlag ="";
			String ASNText ="";
			String exceptionstr ="";

			Connection db2Conn = null;
			Statement stmt = null;
			ResultSet rs = null;

			try
			{
				String uniqueName = (String)useform.getFieldValue("UniqueName");
                Log.customer.debug("**AR** uniqueName " + uniqueName);
				String tmp = useform.getFieldValue("SupplierCode").toString().toUpperCase();
				if(tmp.length() == 6)
				{
					tmp = tmp + "0";
				}

                try
                {
					String tmp1 = useform.getFieldValue("SupplierCode").toString().toUpperCase().substring(0,6);
					Log.customer.debug("**AR** This is the 6 digit code: " + tmp1);
					String query = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+tmp1+"%' and partition = 'ezopen'";
					Log.customer.debug("**AR** This is the new query: " + query);
					Class.forName ("COM.ibm.db2.jdbc.app.DB2Driver");

					String DBName = Base.getService().getParameter(null,"System.Base.DBName");
					DBName = "jdbc:db2:" + DBName;
					String DBUser = Base.getService().getParameter(null,"System.Base.DBUser");
					String DBPwd = Base.getService().getParameter(null,"System.Base.DBPwd");

					db2Conn = DriverManager.getConnection(DBName,DBUser,DBPwd);
					stmt = db2Conn.createStatement();
					rs = stmt.executeQuery (query);
					Log.customer.debug("*** ex 1 query ***");

					// Issue #785 start

					ASNText = Fmt.Sil("cat.vcsv3","ASNText");

					// Issue #785 end

					s1 = useform.getFieldValue("SupplierCode").toString().toUpperCase();

					if(s1.length() == 6)
					{
						s1 = s1 + "0";
					}

					s2 = "ezopen";
					s3 = "vcsv3";
					s41 = "+1.";
					s42 = "+0.";

					if (useform.getFieldValue("ASNAccountID")==(null))
					{
						if((useform.getFieldValue("PreferredOrderingMethod").toString()).equals("Fax"))
						s5 = "Fax";
						else
						s5 = "Email";
					}
					else
					{
						s5 = "URL";
						asnFlag = "true";
					}

					if (useform.getFieldValue("SupplierContactEmail")==(null))
					{
						s6 = "";
					}
					else
					{
						s6 = useform.getFieldValue("SupplierContactEmail").toString();
					}
					if (useform.getFieldValue("SupplierWebSiteURL")==(null))
					{
						s7 = "";
					}
					else
					{
						s7 = useform.getFieldValue("SupplierWebSiteURL").toString();
					}
					if (useform.getFieldValue("SupplierName")==(null))
					{
						s8 = "";
					}
					else
					{
						s8 = useform.getFieldValue("SupplierName").toString();

						//check s8 field and replace quotes with space
						if(s8.indexOf("\'")>0)

						{
							String patternStr = "\'";
							String replacementStr = " ";
							Pattern pattern = Pattern.compile(patternStr);

							// Replace all occurrences of '
							Matcher matcher = pattern.matcher(s8);
							s8 = matcher.replaceAll(replacementStr);
							Log.customer.debug("@@@@@ s8==="+s8);
						 }

						Log.customer.debug("###### s8==="+s8);



					}
					s9 = "";
					s10 = "";
					s11 = "";
					s12 = "";
					s13 = "";
					s15 = "";
					s16 = "";
					s17 = "";
					s18 = "";

					if (useform.getFieldValue("ASNAccountID")==(null))
					{
						s14 = "";
					}
					else
					{
						s14 = useform.getFieldValue("ASNAccountID").toString();
					}
					if (useform.getFieldValue("APEmail")==(null))
					{
						s22 = "";
					}
					else
					{
						s22 = useform.getFieldValue("APEmail").toString();
					}

					if (useform.getFieldValue("VATCode")==(null))
					{
						s32 = "";
					}
				 	else
					{
						s32 = useform.getFieldValue("VATCode").toString();
					}
				/*	if (useform.getFieldValue("SupplierAPEmailAddress")==(null))
					{
						s33 = "";
					}
					else
					{
						s33 = useform.getFieldValue("SupplierAPEmailAddress").toString();
					}*/
					s19 = "";
					s20 = "";
					s21 = "";

					s23 = Base.getSession().getRealUser().getFieldValue("UniqueName").toString();
					/*
					asnFlag = useform.getFieldValue("ASNOnboarding").toString();
					String resetFax = useform.getFieldValue("ResetToFax").toString();
					Log.customer.debug("**AR** asn flag: " + asnFlag);
					Log.customer.debug("**AR** reset to fax flag: " + resetFax);
					*/
					String buildSQL;
					String buildSQL3;
					changes = "This is a new MSC Supplier.\n";



					if(rs.next())
					{
						if (rs.getString(1) != null)
						{
							String query2 = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID = '"+tmp+"' and partition = 'ezopen'";
							Log.customer.debug("**AR** This is the subquery - check second location: " + query2);
							ResultSet rs1 = stmt.executeQuery (query2);
							if(rs1.next())
							{
								if (rs1.getString(1) != null)
								{
									Log.customer.debug(" *** this is an update ***");

									//remove isnew in all updates

									/*buildSQL = "UPDATE ibm_supplier_supplement set isnew = 0. WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
									Log.customer.debug(buildSQL);
									stmt.executeUpdate (buildSQL);
									*/

									/*
									buildSQL = "select PREFERREDORDERINGMETHOD from ibm_supplier_supplement WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
									Log.customer.debug(buildSQL);
									ResultSet rs2 = stmt.executeQuery (buildSQL);
									if(rs.next())
									{
										prefOrdering = rs.getString(1);
										Log.customer.debug(prefOrdering);
									}

									if (prefOrdering.equals("URL"))
									{
										if ((useform.getFieldValue("PreferredOrderingMethod").toString()).equals("Fax"))
										{
											prefOrdering = "Fax";
											changes = changes + "Ordering method reset to Fax \n";
										}
										else
										{
											prefOrdering = "Email";
											changes = changes + "Ordering method set to Email \n";
									    }
										s5 = prefOrdering;
									}

									changes = "\nChanges made in this transaction: \n";
									Log.customer.debug(changes);

									buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = \'" + prefOrdering + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
									Log.customer.debug(buildSQL);
									stmt.executeUpdate (buildSQL);
									*/

									if (useform.getFieldValue("SupplierContactEmail")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set EMAILADDRESS = \'" + s6 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "email address set to: " + s6 + " \n";
										Log.customer.debug(changes);
									}
									if (useform.getFieldValue("SupplierWebSiteURL")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set URL = \'" + s7 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "URL set to: " + s7 + " \n";
										Log.customer.debug(changes);
									}
									if (useform.getFieldValue("ASNAccountID")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set ARIBANETWORKID = \'" + s14 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "ASN ID set to: " + s14 + " \n";
										Log.customer.debug(changes);
										buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = \'" + s5 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "Ordering Method set to: " + s5 + " \n";
										Log.customer.debug(changes);
									}
									if (useform.getFieldValue("ASNAccountID")==(null) && useform.getFieldValue("PreferredOrderingMethod")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = \'" + s5 + "\' WHERE SUPPLIERID = \'" +tmp+ "' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate(buildSQL);
										changes = changes + " PREFERRED ORDERING METHOD set to : " + s5 + "\n";
										Log.customer.debug(changes);
									}

									if (useform.getFieldValue("APEmail")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set SUPPLIERAPEMAILADDRESS = \'" + s22 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "AR email set to: " + s22 + " \n";
										Log.customer.debug(changes);
									}
									if (useform.getFieldValue("VATCode")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set VATREGNUMBER = \'" + s32 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "VAT Code set to: " + s32 + " \n";
										Log.customer.debug(changes);
									}
								/*	if (useform.getFieldValue("SupplierAPEmailAddress")!=(null))
									{
										buildSQL = "UPDATE ibm_supplier_supplement set SUPPLIERAPEMAILADDRESS = \'" + s33 + "\' WHERE SUPPLIERID = \'" +tmp+"' and partition = 'ezopen'";
										Log.customer.debug(buildSQL);
										stmt.executeUpdate (buildSQL);
										changes = changes + "AP email set to: " + s33 + " \n";
										Log.customer.debug(changes);
									} */
	//							//	if (asnFlag == "true")
								//	Log.customer.debug("ran sql 1");
									Log.customer.debug("Final changes: " + changes);

									//Added by Sandeep to Update the Mastertable
																	    buildSQL = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='ezopenUpdate' WHERE LOCATION_ID= '" + tmp + "'";
																	    Log.customer.debug(buildSQL);
																	    Log.customer.debug("Set Update flag in Mastertable");
																	    stmt.executeUpdate(buildSQL);
						                        Log.customer.debug("ran sql");



								}


							}
							else
							{
								buildSQL3 = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SUPPLIERAPEMAILADDRESS,BUYERCUPID) VALUES (\'" + s1 + "\',\'" + s2 + "\',\'" +  s3 + "\'," + s42 + ",\'" + s5 + "\',\'" + s6 + "\',\'" + s7 + "\',\'" + s8 + "\',\'" + s9 + "\',\'" + s10 + "\',\'" + s11 + "\',\'" + s12 + "\',\'" + s13 + "\',\'" + s14 + "\',\'" + s15 + "\',\'" + s16 + "\',\'" + s17 + "\',\'" + s18 + "\',\'" + s32 + "\',\'" + s20 + "\',\'" + s21 + "\',\'" + s22 + "\',\'" + s23 + "\')";
								Log.customer.debug("**AR** This is the second location: " + buildSQL3);
								stmt.executeUpdate (buildSQL3);
								Log.customer.debug("ran buildSQL3");


								                    //Added by Sandeep to update the Master Table
								                    Log.customer.debug(" This is a second supplier location for  hence Update MasterTable as Load");

								                    String buildSQL4 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='ezopenLoad' where LOCATION_ID= '" + s1 + "'";
								                    stmt.executeUpdate(buildSQL4);
                    Log.customer.debug("ran buildSQL4");

							}

						}
					}
					else
					{
						Log.customer.debug(" *** this is an insert ***");
						buildSQL = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SUPPLIERAPEMAILADDRESS,BUYERCUPID) VALUES (\'" + s1 + "\',\'" + s2 + "\',\'" +  s3 + "\'," + s41 + ",\'" + s5 + "\',\'" + s6 + "\',\'" + s7 + "\',\'" + s8 + "\',\'" + s9 + "\',\'" + s10 + "\',\'" + s11 + "\',\'" + s12 + "\',\'" + s13 + "\',\'" + s14 + "\',\'" + s15 + "\',\'" + s16 + "\',\'" + s17 + "\',\'" + s18 + "\',\'" + s32 + "\',\'" + s20 + "\',\'" + s21 + "\',\'" + s22 + "\',\'" + s23 + "\')";
						Log.customer.debug(buildSQL);
						stmt.executeUpdate (buildSQL);
						Log.customer.debug("ran sql 2");

						 Log.customer.debug(" This is a new supplier location for  hence Update MasterTable as Load");
									  String buildSQL2 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='ezopenLoad' where LOCATION_ID= '" + s1 + "'";
									  stmt.executeUpdate(buildSQL2);
              Log.customer.debug("ran buildSQL2");

					}

					Log.customer.debug("completed insert / update");

					rs.close();
					stmt.close();
					db2Conn.close();
			    }

			    catch(Exception ex)
				{
					rs.close();
					stmt.close();
					db2Conn.close();
					exceptionstr = ex.getMessage();
				}

				String eId = Base.getSession().getRealUser().getFieldValue("EmailAddress").toString();
				Log.customer.debug(eId);
				String message,msgSubject;
				List attachments = ListUtil.list();
        		File reportFileZip = null;

        		// Issue #785 start

				/*Properties props = System.getProperties();
				props.setProperty("mail.transport.protocol", "smtp");
				String smtpurl = Base.getService().getParameter(null,"System.Base.SMTPServerName");
				props.put("mail.smtp.host", smtpurl);
				Session session = Session.getDefaultInstance(props, null);
				MimeMessage message = new MimeMessage(session);

				String sendFrom = Base.getService().getParameter(null,"System.Base.SuppliereFormID");
				Log.customer.debug("CatEZOSuppliereForm ::: Mail sent From :::" + sendFrom);
				message.setFrom(new InternetAddress(sendFrom) );
				message.addRecipient(Message.RecipientType.TO,new InternetAddress(eId));
				message.addRecipient(Message.RecipientType.CC,new InternetAddress(Base.getService().getParameter(null,"System.Base.CATEmailID")));
				message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID1")));
				message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID2")));
				message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID3")));*/

				if (((useform.getFieldValue("Action").toString()) == "Deactivate Supplier")|(useform.getFieldValue("Action").equals("Deactivate Supplier")))
				{
					Log.customer.debug(useform.getFieldValue("Action").toString());
					msgSubject="Supplier eform: Request to Deactivate Supplier";
					message="A request has been raised in the MSC system to deactivate this supplier: "+tmp+". \nThis request was raised by: "+eId+" in the ezopen partition.\nPlease do not reply to this mail ID as this is an automated message.";
					//message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
					//message.addRecipient(Message.RecipientType.TO,new InternetAddress(eId));
					//message.addRecipient(Message.RecipientType.CC,new InternetAddress(Base.getService().getParameter(null,"System.Base.CATEmailID")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID1")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID2")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID3")));
					//message.setSubject("Supplier eform: Request to Deactivate Supplier");
					//message.setText("A request has been raised in the MSC system to deactivate this supplier: "+tmp+". \nThis request was raised by: "+eId+" in the ezopen partition.\nPlease do not reply to this mail ID as this is an automated message.");
					Log.customer.debug("** mailing deactivation **");
					List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
							 for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
								   String toAddress = (String)it.next();
								   Log.customer.debug("Values for toAddress = %s", toAddress);
								CatEmailNotificationUtil.sendNotification(msgSubject, message, toAddress, null);
			    }
				}
				else
				{
				//	Log.customer.debug("** calling supplier download **");
				//	SupplierDownload sd = new SupplierDownload();
				//	sd.setSupplierCode(tmp);

				//	String status = sd.getResponse();
				//	String find = "YE0524I6";




					Log.customer.debug("@@@ Action @@@ " + useform.getFieldValue("Action").toString());
					//message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
					//message.addRecipient(Message.RecipientType.TO,new InternetAddress(eId));
					//message.addRecipient(Message.RecipientType.CC,new InternetAddress(Base.getService().getParameter(null,"System.Base.CATEmailID")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID1")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID2")));
					//message.addRecipient(Message.RecipientType.BCC,new InternetAddress(Base.getService().getParameter(null,"System.Base.AMSEmailID3")));
										//message.setSubject("Supplier eform: " + tmp + " added");
msgSubject="In Geneva partition Supplier eform " + uniqueName + " : " + tmp + " added on production";
//		msgSubject="Supplier eform: " + tmp + " added";
						Log.customer.debug("Supplier Found and added : Supplier ");



					if (asnFlag.equals("true"))
					{
						changes = changes + ASNText;
					}
				String status1= "Unsuccessful";

					/*BodyPart messageBodyPart = new MimeBodyPart();
					messageBodyPart.setText("Your request for the Supplier " + tmp + " was sent to LOGNET. Status of the request returned: " + status + " \nPlease contact 4-HELP for clarifications. \n\n"+ changes +"\nPlease do not reply to this mail ID as this is an automated message.");
					Multipart multipart = new MimeMultipart();
					multipart.addBodyPart(messageBodyPart);*/
                    message="Your request for the Supplier " + tmp + " was sent to LOGNET. Status of the request returned: " + status1 + " \nPlease contact 4-HELP for clarifications. \n\n"+ changes +"\nPlease do not reply to this mail ID as this is an automated message.";
					if (asnFlag.equals("true"))
					{
						Log.customer.debug("**AR** inside asnFlag **");
						reportFileZip = new File("/ariba9r1/appserver/downstream/Server/classes/extensions/Templates.zip");
						ListUtil.addElementIfAbsent(attachments,reportFileZip);
						/*messageBodyPart = new MimeBodyPart();
						DataSource source = new FileDataSource("/msc/arb821/Server/classes/extensions/Templates.zip");
						messageBodyPart.setDataHandler(new DataHandler(source));
						messageBodyPart.setFileName("Templates.zip");
						multipart.addBodyPart(messageBodyPart);*/
						Log.customer.debug("**AR** set multipart **");
					}

					//message.setContent(multipart);
					Log.customer.debug("** mailing insert / update **");
					 List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
								for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
									String toAddress = (String)it.next();
									Log.customer.debug("Values for toAddress = %s", toAddress);
			      CatEmailNotificationUtil.sendNotification( msgSubject, message, toAddress, attachments);
				    }
                 }
				//Transport.send(message);
				// Issue #785 end
				Log.customer.debug("*** exiting supplier eform submit ***");
        		return NoErrorResult;

			}

			catch (Exception e)
			{
				Log.customer.debug(e.toString());
				return NoErrorResult;
			}
        }

    public CatEZOSupplierHook()
        {
        	super();
        }
    }
