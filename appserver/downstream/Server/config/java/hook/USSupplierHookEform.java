/* ****************************************************************************************
Change History
	Change By	Change Date		Description
=============================================================================================
1	PGSKannan	08/29/2007		CR R4-CR76, Removed Submitter as Buyer, and user needs to select the supplier buyer contact
                                functionality added.
2   Rajani      04/02/2008      Issue #785, read Email body from resource file and email API is change to CatEmailNotificationUtil.

3   Sudheer K Jain 27/06/2008   Issue #832, Take out the supplier name from SuppliereForm and Replace special symbbol by blank space.

4.  PGS Kannan    08/05/08      Issue 800 Using new SupplierContactEmail and AREmail length has been changed.

6.  Shailaja Salimath   09/04/2009 CR177:Changing email notification subject line, adding SOE number and partition name
7.  Shailaja Salimath   14/07/09   Issue 951 Allowing Supplier admin to change PreferredOrderingMethod
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
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import config.java.common.CatEmailNotificationUtil;
import config.java.common.SupplierDownload;

public class USSupplierHookEform extends Action
{

    public void fire (ValueSource object, PropertyTable parameters)
                throws ActionExecutionException {

				Approvable psm = (Approvable)object;
				if (psm.getApprovedState() == Approvable.StateApproved){
		try {

        ClusterRoot useform;
        Log.customer.debug("*** entering eform submit core ***");
        useform = psm;
        String uniqueName = (String)useform.getFieldValue("UniqueName");
        Log.customer.debug("**AR** uniqueName " + uniqueName);
        String eId = (String)useform.getDottedFieldValue("Requester.EmailAddress");
        String tmp = useform.getFieldValue("SupplierCode").toString().toUpperCase();

        if(tmp.length() == 6)
            tmp = tmp + "0";

        String tmp1 = useform.getFieldValue("SupplierCode").toString().toUpperCase().substring(0, 6);
        Log.customer.debug("**AR** This is the 6 digit code: " + tmp1);

        String query = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '" + tmp1 + "%' and partition = 'pcsv1'";
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

        // Issue #785 start

        String ASNText = "\n"+ResourceService.getString("cat.vcsv1","ASNText")+"\n";

        // Issue #785 end

        String s1 = useform.getFieldValue("SupplierCode").toString().toUpperCase();
        if(s1.length() == 6)
            s1 = s1 + "0";

        String s2 = "pcsv1";
        String s3 = "vcsv1";
        String s41 = "+1.";
        String s42 = "+0.";
        String s5 = "";
        String buyerCupid ;

      /*  if(useform.getFieldValue("ASNAccountID") == null)
            s5 = "Fax";
        else
            s5 = "URL"; */
        String s6;

       // issue 951 setting s5 value based on POM
     		String newOrderingMethod = (String)useform.getFieldValue("PreferredOrderingMethod");
		if (newOrderingMethod != null)
		{
			s5 = newOrderingMethod;
		}
		  Log.customer.debug("**** value for s5 : " + s5);
        if(useform.getFieldValue("BuyerCupid") == null)
		      buyerCupid = "";
		else {
			//Added by Sandeep as a part of 9r1
              Log.customer.debug("Getting Buyer Cupid");
              buyerCupid = useform.getDottedFieldValue("BuyerCupid.UserID.UniqueName").toString();

              if(buyerCupid == null)
              {
				  buyerCupid = "";
			  }

              Log.customer.debug("**** buyerCupid from eform : " + buyerCupid);
		     }

        if(useform.getFieldValue("SupplierContactEmail75") == null)
            s6 = "";
        else
            s6 = useform.getFieldValue("SupplierContactEmail75").toString();
        String s7;
        if(useform.getFieldValue("SupplierWebSiteURL") == null)
            s7 = "";
        else
            s7 = useform.getFieldValue("SupplierWebSiteURL").toString();

       //   Sudheer K JAIN Code started
            String name = useform.getFieldValue("SupplierName").toString();
            String replaceName;
            replaceName = replaceSpecialChar(name);
            useform.setFieldValue("SupplierName",replaceName);

      // End of Sudheer K JAIN Code


       String s8;
        if(useform.getFieldValue("SupplierName") == null)
            s8 = "";
        else
            s8 = useform.getFieldValue("SupplierName").toString();

          String s9 = "";
        String s10 = "";
        String s11 = "";
        String s12 = "";
        String s13 = "";
        String s15 = "";
        String s16 = "";
        String s17 = "";
        String s18 = "";
        String s14;

        if(useform.getFieldValue("ASNAccountID") == null)
            s14 = "";
        else
            s14 = useform.getFieldValue("ASNAccountID").toString();

        String s22;
        if(useform.getFieldValue("AREmail75") == null)
            s22 = "";
        else
            s22 = useform.getFieldValue("AREmail75").toString();

        String s19 = "";
        String s20 = "";
        String s21 = "";

        //  removed for CR R4-CR76
        //String s23 = Base.getSession().getRealUser().getFieldValue("UniqueName").toString();
String buildSQL = null;
        String asnFlag = useform.getFieldValue("ASNOnboarding").toString();
        String resetFax = useform.getFieldValue("ResetToFax").toString();
        Log.customer.debug("**AR** asn flag: " + asnFlag);
        Log.customer.debug("**AR** reset to fax flag: " + resetFax);
        String changes = "This is a new MSC Supplier.\n";
        if(rs.next()) {

            if(rs.getString(1) != null) {
                String query2 = "SELECT SUPPLIERID FROM IBM_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                Log.customer.debug("**AR** This is the subquery - check second location: " + query2);
                ResultSet rs1 = stmt.executeQuery(query2);
                if(rs1.next()) {
                    if(rs1.getString(1) != null) {
// issue 951 commented code so that POM will not get updated
                     /*   Log.customer.debug(" *** this is an update ***");
                        String buildSQL = "UPDATE ibm_supplier_supplement set isnew = 0. WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                        Log.customer.debug(buildSQL);
                        stmt.executeUpdate(buildSQL);

                        buildSQL = "select PREFERREDORDERINGMETHOD from ibm_supplier_supplement WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                        Log.customer.debug(buildSQL);
                        ResultSet rs2 = stmt.executeQuery(buildSQL);

                        if(rs.next()) {
                            prefOrdering = rs.getString(1);
                            Log.customer.debug(prefOrdering);
                        }
                        if(prefOrdering.equals("URL")) {
                            if(resetFax.equals("true")) {
                                prefOrdering = "Fax";
                                changes = changes + "Ordering method reset to Fax \n";
                            }
                            s5 = prefOrdering;
                        }
                        changes = "\nChanges made in this transaction: \n";
                        Log.customer.debug(changes);
                        buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + prefOrdering + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                        Log.customer.debug(buildSQL);
                        stmt.executeUpdate(buildSQL);
*/
                        if(useform.getFieldValue("BuyerCupid") != null) {
							buildSQL = "UPDATE ibm_supplier_supplement set BUYERCUPID = '" + buyerCupid + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
							Log.customer.debug(buildSQL);
							stmt.executeUpdate(buildSQL);
							changes = changes + "BUYERCUPID set to: " + buyerCupid + " \n";
							Log.customer.debug(changes);
                        }


                        if(useform.getFieldValue("SupplierContactEmail75") != null) {
                            buildSQL = "UPDATE ibm_supplier_supplement set EMAILADDRESS = '" + s6 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "email address set to: " + s6 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(useform.getFieldValue("SupplierWebSiteURL") != null) {
                            buildSQL = "UPDATE ibm_supplier_supplement set URL = '" + s7 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "URL set to: " + s7 + " \n";
                            Log.customer.debug(changes);
                        }
                        if(useform.getFieldValue("ASNAccountID") != null) {
                            buildSQL = "UPDATE ibm_supplier_supplement set ARIBANETWORKID = '" + s14 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "ASN ID set to: " + s14 + " \n";
                            Log.customer.debug(changes);
                            buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "Ordering Method set to: " + s5 + " \n";
                            Log.customer.debug(changes);
                        }
                        if (useform.getFieldValue("ASNAccountID")==(null) && useform.getFieldValue("PreferredOrderingMethod")!=(null))
						{
							buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = \'" + s5 + "\' WHERE SUPPLIERID = \'" +tmp+ "' and partition = 'pcsv1'";
							Log.customer.debug(buildSQL);
							stmt.executeUpdate(buildSQL);
							changes = changes + " PREFERRED ORDERING METHOD set to : " + s5 + "\n";
							Log.customer.debug(changes);
					}
                        if(useform.getFieldValue("AREmail75") != null) {
                            buildSQL = "UPDATE ibm_supplier_supplement set SupplierAPEmailAddress = '" + s22 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
                            Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            changes = changes + "AP email set to: " + s22 + " \n";
                             buildSQL = "UPDATE ibm_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'pcsv1'";
							 Log.customer.debug(buildSQL);
                            stmt.executeUpdate(buildSQL);
                            Log.customer.debug(changes);
                        }

                        //Added by Sandeep to Update the Mastertable
                        buildSQL = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='pcsv1Update' where LOCATION_ID= '" + tmp + "'";
                        Log.customer.debug(buildSQL);
                        Log.customer.debug("Set Update flag in Mastertable");
                        stmt.executeUpdate(buildSQL);
                        Log.customer.debug("ran sql 1");

                    }
                } else {
                    String buildSQL3 = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SupplierAPEmailAddress,BUYERCUPID) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s42 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + buyerCupid + "')";
                    Log.customer.debug("**AR** This is the second location: " + buildSQL3);
                    stmt.executeUpdate(buildSQL3);
                    Log.customer.debug("ran buildSQL3");

                    //Added by Sandeep to update the Master Table
                    Log.customer.debug(" This is a second supplier location for  hence Update MasterTable as Load");

                    String buildSQL4 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='pcsv1Load' where LOCATION_ID= '" + s1 + "'";
                    stmt.executeUpdate(buildSQL4);
                    Log.customer.debug("ran buildSQL4");

                }
            }
        } else {
            Log.customer.debug(" *** this is an insert ***");
            buildSQL = "INSERT INTO ibm_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,SupplierAPEmailAddress,BUYERCUPID) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s41 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + buyerCupid + "')";
            Log.customer.debug(buildSQL);
            stmt.executeUpdate(buildSQL);
            Log.customer.debug("ran sql 2");

               Log.customer.debug(" This is a new supplier location for  hence Update MasterTable as Load");
			  String buildSQL2 = "UPDATE msc_ibm_supplier_supplement set MSCACCESS_PARTITION='pcsv1Load' where LOCATION_ID= '" + s1 + "'";
			  stmt.executeUpdate(buildSQL2);
              Log.customer.debug("ran buildSQL2");
        }
        Log.customer.debug("completed insert / update");
        rs.close();
        stmt.close();
        db2Conn.close();

     //   String eId = Base.getSession().getRealUser().getFieldValue("EmailAddress").toString();
     if (eId != null)
       Log.customer.debug(eId);

        // Issue #785 start

        /*Properties props = System.getProperties();
        props.setProperty("mail.transport.protocol", "smtp");
        String smtpurl = Base.getService().getParameter(null, "System.Base.SMTPServerName");
        props.put("mail.smtp.host", smtpurl);
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);*/
        String message,msgSubject;
        List attachments = ListUtil.list();
        File reportFileZip = null;
        if((useform.getFieldValue("Action").toString() == "Deactivate Supplier") | useform.getFieldValue("Action").equals("Deactivate Supplier"))
        {
            Log.customer.debug(useform.getFieldValue("Action").toString());
            msgSubject="Supplier eform: Request to Deactivate Supplier";
           message="A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the pcsv1 partition.\nPlease do not reply to this mail ID as this is an automated message.";

           /* message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(eId));
            message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(Base.getService().getParameter(null, "System.Base.CATEmailID")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID1")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID2")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));
            message.setSubject("Supplier eform: Request to Deactivate Supplier");
            message.setText("A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the pcsv1 partition.\nPlease do not reply to this mail ID as this is an automated message.");*/
            Log.customer.debug("** mailing deactivation **");
            List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
			         for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
						   String toAddress = (String)it.next();
						   Log.customer.debug("Values for toAddress = %s", toAddress);
						CatEmailNotificationUtil.sendNotification(msgSubject, message, toAddress, null);
			}
        } else {
			//Removed by Sandeep as a part of 9r1
            //Log.customer.debug("** calling supplier download **");
            //SupplierDownload sd = new SupplierDownload();
            //sd.setSupplierCode(tmp);
            //String status = sd.getResponse();
            //Log.customer.debug(tmp);
            Log.customer.debug(useform.getFieldValue("Action").toString());
             msgSubject="In US partition,Supplier eform " + uniqueName  +  " : " + tmp + " added on production";

            if(asnFlag.equals("true"))
                changes = changes + ASNText;
                Log.customer.debug("Supplier eform tmp" + tmp);
            message=Fmt.Sil("cat.vcsv1", "SupplierEformMessage",tmp,changes);

            if(asnFlag.equals("true")) {
                Log.customer.debug("**AR** inside asnFlag **");
                reportFileZip = new File("/ariba9r1/appserver/downstream/Server/classes/extensions/Templates.zip");
                ListUtil.addElementIfAbsent(attachments,reportFileZip);
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
       // return NoErrorResult;
	   }
      catch (Exception e) {
        Log.customer.debug(e.toString());
      //  return NoErrorResult;
	}
	}


    }

    // Started Sudheer K Jain code Issue 832

         String replaceSpecialChar(String name)
           {
			    Log.customer.debug("Initial Value of name"+name);
			   char symbol[] = {'\'','\"','.','\\','/','-','(',')',','};
          //     int size = Array.getLength(symbol);
                 Log.customer.debug("Size of array"+symbol.length);
			   for(int i=0;i<symbol.length;i++)
			      {
					  Log.customer.debug("Char is "+symbol[i]);
					  name = StringUtil.replaceCharByString(name,symbol[i]," ");
					  Log.customer.debug("Value of name in loop: "+name+" after replacing character "+symbol[i]);

				    }


			/*	    name.replace('-','$');
				    Log.customer.debug("Final Value of name"+name);
				    name = StringUtil.replaceCharByString(name,'a',"s");     */
				    Log.customer.debug("Final Value of name"+name);
				    return name;
		   }


    //Ended Sudheer K Jain Code Issue 832

    public USSupplierHookEform() {
    }

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private AQLResultCollection results;
    private AQLQuery qry;
    private String query;
    private AQLOptions options;
    String prefOrdering;
}
