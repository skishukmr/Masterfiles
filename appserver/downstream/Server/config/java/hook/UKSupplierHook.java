/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1   Rajani      04/02/2008      Issue #785, read Email body from resource file and email API is change to CatEmailNotificationUtil.
2. Kannan       08/01/2008      Issue # 839 ASNID partitian changed pcsv1 to mfg1
3  Sudheer K Jain 27/06/2008   Issue  #832, Take out the supplier name from SuppliereForm and Replace special symbbol by blank space.
4. Shailaja Salimath   14/07/09   Issue 951 Allowing Supplier admin to change PreferredOrderingMethod
***************************************************************************************** */

package config.java.hook;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.ListIterator;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import config.java.common.CatEmailNotificationUtil;
import config.java.common.SupplierDownload;

public class UKSupplierHook
    implements ApprovableHook
{

    public List run(Approvable approvable)
    {
	  try
	   {
        ClusterRoot ukeform;
        Log.customer.debug("*** entering eform submit core ***");
        ukeform = approvable;
        String tmp = ukeform.getFieldValue("SupplierCode").toString().toUpperCase();
        String tmp1 = ukeform.getFieldValue("SupplierCode").toString().toUpperCase().substring(0, 6);
        Log.customer.debug("**AR** This is the 6 digit code: " + tmp1);
        String query = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '" + tmp1 + "%' and partition = 'mfg1'";
        Log.customer.debug("**AR** This is the new query: " + query);
        Class.forName("COM.ibm.db2.jdbc.app.DB2Driver");
        String DBName = Base.getService().getParameter(null, "System.Base.DBName");
        DBName = "jdbc:db2:" + DBName;
        String DBUser = Base.getService().getParameter(null, "System.Base.DBUser");
        String DBPwd = Base.getService().getParameter(null, "System.Base.DBPwd");
        Connection db2Conn = DriverManager.getConnection(DBName, DBUser, DBPwd);
        Statement stmt = db2Conn.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        Log.customer.debug("*** ex 1 query ***");
        String asnFlag = ukeform.getFieldValue("ASNOnboarding").toString();
        String resetFax = ukeform.getFieldValue("ResetToFax").toString();
        Log.customer.debug("**AR** asn flag: " + asnFlag);
        Log.customer.debug("**AR** reset to fax flag: " + resetFax);
        String s1 = ukeform.getFieldValue("SupplierCode").toString().toUpperCase();

        // Issue #785 start
        String ASNText = ResourceService.getString("cat.vcsv1","ASNText");
        // Issue #785 end
        if(s1.length() == 6)
            s1 = s1 + "0";
        String s2 = "mfg1";
        String s3 = "vcsv2";
        String s41 = "+1.";
        String s42 = "+0.";
        String s5 = "";
       /* if(ukeform.getFieldValue("ASNAccountID") == null)
            s5 = "Fax";
        else
            s5 = "URL"; */

            // issue 951 setting s5 value based on POM
	     // issue 951 setting s5 value based on POM
	        		String newOrderingMethod = (String)ukeform.getFieldValue("PreferredOrderingMethod");
	   		if (newOrderingMethod != null)
	   		{
	   			s5 = newOrderingMethod;
	   		}
		  Log.customer.debug("**** value for s5 : " + s5);
        String s6;
        if(ukeform.getFieldValue("SupplierContactEmail") == null)
            s6 = "";
        else
            s6 = ukeform.getFieldValue("SupplierContactEmail").toString();
        String s7;
        if(ukeform.getFieldValue("SupplierWebSiteURL") == null)
            s7 = "";
        else
            s7 = ukeform.getFieldValue("SupplierWebSiteURL").toString();
        String s8;
        if(ukeform.getFieldValue("SupplierName") == null)
            s8 = "";
        else
            s8 = ukeform.getFieldValue("SupplierName").toString();
        String s9 = "";
        String s10 = "";
        String s11 = "";
        String s12 = "";
        String s13 = "";
        String s15 = "";
        String s16 = "";
        String s17 = "";
        String s18 = "";
        String s22 = "";
        String s14;

        if(ukeform.getFieldValue("ASNAccountID") == null)
            s14 = "";
        else
            s14 = ukeform.getFieldValue("ASNAccountID").toString();
        String s19;
        if(ukeform.getFieldValue("VATRegistration") == null)
            s19 = "";
        else
            s19 = ukeform.getFieldValue("VATRegistration").toString();
        String s20;
        if(ukeform.getFieldValue("VATCode") == null)
            s20 = "";
        else
            s20 = ukeform.getFieldValue("VATCode").toString();
        String s21;
        if(ukeform.getDottedFieldValue("PaymentTerms.UniqueName") == null)
            s21 = "";
        else
            s21 = ukeform.getDottedFieldValue("PaymentTerms.UniqueName").toString();
        if(!ukeform.getFieldValue("Action").equals("Deactivate Supplier"))
        {
            Log.customer.debug(ukeform.getFieldValue("DX").toString());
            if(ukeform.getFieldValue("DX").toString().equals("All Locations"))
            {
                s15 = "1";
                s16 = "1";
                s17 = "1";
                s18 = "1";
            } else
            if(ukeform.getFieldValue("DX").toString().equals("NA"))
            {
                s15 = "0";
                s16 = "1";
                s17 = "0";
                s18 = "0";
            } else
            {
                s15 = "1";
                s16 = "0";
                s17 = "1";
                s18 = "1";
            }
        } else
        {
            s15 = "0";
            s16 = "0";
            s17 = "0";
            s18 = "0";
        }
        String changes = "This is a new MSC Supplier.\n";
         String buildSQL;
           String buildSQL3;
        if(rs.next())
        {
            if(rs.getString(1) != null)
            {
              String query2 = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                Log.customer.debug("**AR** This is the subquery - check second location: " + query2);
                ResultSet rs1 = stmt.executeQuery(query2);
                if(rs1.next())
                {
                  //  if(rs1.getString(1) != null)
                    {
//                        Log.customer.debug(" *** this is an update ***");
//                        buildSQL = "UPDATE ibm_supplier_supplement set isnew = 0. WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
//                        Log.customer.debug(buildSQL);
//                        stmt.executeUpdate(buildSQL);
//                        buildSQL = "select PREFERREDORDERINGMETHOD from ibm_supplier_supplement WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
//                        Log.customer.debug(buildSQL);
//                        ResultSet rs2 = stmt.executeQuery(buildSQL);
//                        changes = "\nChanges made in this transaction: \n";
 //                       Log.customer.debug(changes);
                      /* if(rs2.next())
                        {
                            prefOrdering = rs2.getString(1);
                            Log.customer.debug(prefOrdering);
                        }
                        if(prefOrdering.equals("URL"))
                        {
                            if(resetFax.equals("true"))
                            {
                                prefOrdering = "Fax";
                                changes = changes + "Ordering method reset to Fax \n";
                            }
                            s5 = prefOrdering;
                        }

                        buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                        Log.customer.debug(buildSQL);
                        stmt.executeUpdate(buildSQL);*/
                        if(ukeform.getFieldValue("SupplierContactEmail") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set EMAILADDRESS = '" + s6 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "email address set to: " + s6 + " \n";
                            Log.customer.debug(changes);
                             buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
							 Log.customer.debug(buildSQL);
							stmt.executeUpdate(buildSQL);
                            Log.customer.debug(changes);
                        }
                        if(ukeform.getFieldValue("SupplierWebSiteURL") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set URL = '" + s7 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "URL set to: " + s7 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(ukeform.getFieldValue("ASNAccountID") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set ARIBANETWORKID = '" + s14 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "ASN ID set to: " + s14 + " \n";
                            Log.customer.debug(changes);
                            buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "Ordering Method set to: " + s5 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(ukeform.getFieldValue("VATRegistration") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set VATREGNUMBER = '" + s19 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "VAT Regn. set to: " + s19 + " \n";
                            Log.customer.debug(changes);
                        }
                         if (ukeform.getFieldValue("ASNAccountID")==(null) && ukeform.getFieldValue("PreferredOrderingMethod")!=(null))
							{
								buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = \'" + s5 + "\' WHERE SUPPLIERID = \'" +tmp+ "' and partition = 'mfg1'";
								Log.customer.debug(buildSQL);
								stmt.executeUpdate(buildSQL);
								changes = changes + " PREFERRED ORDERING METHOD set to : " + s5 + "\n";
								Log.customer.debug(changes);
					     }
                        if(ukeform.getFieldValue("VATCode") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set VATCLASSNAME = '" + s20 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "VAT Classname set to: " + s20 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(ukeform.getDottedFieldValue("PaymentTerms.UniqueName") != null)
                        {
                            buildSQL = "UPDATE ibm_supplier_supplement set PAYMENTTERMS = '" + s21 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "Payment terms set to: " + s21 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(!ukeform.getFieldValue("Action").equals("Deactivate Supplier"))
                        {
                            if(ukeform.getFieldValue("DX").toString().equals("All Locations"))
                            {
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDDX = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMX = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMY = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDNA = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                changes = changes + "Facility set to: All Locations \n";
                                Log.customer.debug(changes);
                            }
                            if(ukeform.getFieldValue("DX").toString().equals("NA"))
                            {
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDDX = '0' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMX = '0' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMY = '0' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDNA = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                changes = changes + "Facility set to: NA \n";
                                Log.customer.debug(changes);
                            }
                            if(ukeform.getFieldValue("DX").toString().equals("DX/MX/MY"))
                            {
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDDX = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMX = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDMY = '1' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                buildSQL = "UPDATE ibm_supplier_supplement set AUTHORIZEDNA = '0' WHERE SUPPLIERID = '" + tmp + "' and partition = 'mfg1'";
                                Log.customer.debug(buildSQL);
                                stmt.executeUpdate(buildSQL);
                                changes = changes + "Facility set to: DX/MX/MY \n";
                                Log.customer.debug(changes);
                            }
                        }

                        //Added by Sandeep to Update the Mastertable
						                        buildSQL = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='mfg1Update' where LOCATION_ID= '" + tmp + "'";
						                        Log.customer.debug(buildSQL);
						                        Log.customer.debug("Set Update flag in Mastertable");
						                        stmt.executeUpdate(buildSQL);
						                        Log.customer.debug("ran sql");
                    }

                } else
                {
                    buildSQL3 = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SupplierAPEmailAddress) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s42 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "')";
                    Log.customer.debug("**AR** This is the second location: " + buildSQL3);
                    stmt.executeUpdate(buildSQL3);
                    Log.customer.debug("ran buildSQL3");

                    //Added by Sandeep to update the Master Table
					                    Log.customer.debug(" This is a second supplier location for  hence Update MasterTable as Load");

					                    String buildSQL4 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='mfg1Load' where  LOCATION_ID= '" + s1 + "'";
					                    stmt.executeUpdate(buildSQL4);
                    Log.customer.debug("ran buildSQL4");
                }
            }
        } else
        {
            Log.customer.debug(" *** this is an insert ***");
            buildSQL = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SUPPLIERAPEMAILADDRESS) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s41 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "')";
            Log.customer.debug(buildSQL);
            stmt.executeUpdate(buildSQL);
            Log.customer.debug("ran sql 2");

              Log.customer.debug(" This is a new supplier location for  hence Update MasterTable as Load");
						  String buildSQL2 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='mfg1Load' where LOCATION_ID= '" + s1 + "'";
						  stmt.executeUpdate(buildSQL2);
              Log.customer.debug("ran buildSQL2");
        }
        Log.customer.debug("completed insert / update");
        rs.close();
        stmt.close();
        db2Conn.close();
        String eId = Base.getSession().getRealUser().getFieldValue("EmailAddress").toString();
        Log.customer.debug(eId);
        // Issue #785 start

       /* Properties props = System.getProperties();
        props.setProperty("mail.transport.protocol", "smtp");
        String smtpurl = Base.getService().getParameter(null, "System.Base.SMTPServerName");
        props.put("mail.smtp.host", smtpurl);
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);*/
        String message,msgSubject;
		List attachments = ListUtil.list();
        File reportFileZip = null;
        if((ukeform.getFieldValue("Action").toString() == "Deactivate Supplier") | ukeform.getFieldValue("Action").equals("Deactivate Supplier"))
        {
            Log.customer.debug(ukeform.getFieldValue("Action").toString());
            msgSubject="Supplier eform: Request to Deactivate Supplier";
            message="A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the mfg1 partition.\nPlease do not reply to this mail ID as this is an automated message.";
            /*message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(eId));
            message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(Base.getService().getParameter(null, "System.Base.CATEmailID")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID1")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID2")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));
            message.setSubject("Supplier eform: Request to Deactivate Supplier");
            message.setText("A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the mfg1 partition.\nPlease do not reply to this mail ID as this is an automated message.");*/
            Log.customer.debug("** mailing deactivation **");
            List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
						         for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
									   String toAddress = (String)it.next();
									   Log.customer.debug("Values for toAddress = %s", toAddress);
									CatEmailNotificationUtil.sendNotification(msgSubject, message, toAddress, null);
			}
        } else
        {
           // Log.customer.debug("** calling supplier download **");
           // SupplierDownload sd = new SupplierDownload();
           // sd.setSupplierCode(tmp);
           // String status = sd.getResponse();
           // Log.customer.debug(tmp);
            Log.customer.debug(ukeform.getFieldValue("Action").toString());
           /* message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(eId));
            message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(Base.getService().getParameter(null, "System.Base.CATEmailID")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID1")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID2")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));*/
            //message.setSubject("Supplier eform: " + tmp + " added on production");
            msgSubject="Supplier eform: " + tmp + " added on production";
            if(asnFlag.equals("true"))
                changes = changes + ASNText;
           /* BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Your request for the Supplier " + tmp + " was sent to LOGNET. Status of the request returned: " + status + " \nPlease contact McMilleon_Bill_J@cat.com or krausel@us.ibm.com for clarifications. \n\n" + changes + "\nPlease do not reply to this mail ID as this is an automated message.");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);*/
            message=Fmt.Sil("cat.vcsv1", "SupplierEformMessage",tmp,changes);
            if(asnFlag.equals("true"))
            {
                Log.customer.debug("**AR** inside asnFlag **");
                reportFileZip = new File("/ariba9r1/appserver/downstream/Server/classes/extensions/Templates.zip");
                ListUtil.addElementIfAbsent(attachments,reportFileZip);
                /*messageBodyPart = new MimeBodyPart();
                javax.activation.DataSource source = new FileDataSource("/msc/arb821/Server/classes/extensions/Templates.zip");
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName("Templates.zip");
                multipart.addBodyPart(messageBodyPart);*/
                Log.customer.debug("**AR** set multipart **");
            }
           // message.setContent(multipart);
            Log.customer.debug("** mailing insert / update **");
            List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
						for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
							String toAddress = (String)it.next();
							Log.customer.debug("Values for toAddress = %s", toAddress);
						CatEmailNotificationUtil.sendNotification( msgSubject, message, toAddress, attachments);
	     }
        }
       // Transport.send(message);
       // Issue #785 end
        Log.customer.debug("*** exiting supplier eform submit ***");
        return NoErrorResult;
	}
      catch(Exception e)
      {
          Log.customer.debug(e.toString());
          return NoErrorResult;
     }
    }
    public UKSupplierHook()
    {
    }

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private AQLResultCollection results;
    private AQLQuery qry;
    private String query;
    private AQLOptions options;
    String prefOrdering;

}
