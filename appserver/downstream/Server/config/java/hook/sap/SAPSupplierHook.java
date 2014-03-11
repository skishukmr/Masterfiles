/*********************************************************************************************************************
Created :
	Date		Name		History
	--------------------------------------------------------------------------------------------------------------
	27/08/08   Nagendra 	Supplier Eform Hook for SAP partititon
    Shailaja Salimath   09/04/2009 CR177:Changing email notification subject line, adding SOE number and partition name
    Shailaja Salimath   14/07/09   Issue 951 Allowing Supplier admin to change PreferredOrderingMethod
    Lekshmi S  19/72010 Issue 1146 Spl Character handling for SupplierName
	Purush Kancharla 08/28/2012    Added  code chnages for NonMach1 email notification subject line
********************************************************************************************************************/
package config.java.hook.sap;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.ListIterator;

import mc_style.functions.soap.sap.document.sap_com.ZFIAP_RFC_VEND_DATA_FEEDSoapBindingStub;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.core.Partition;
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
import functions.rfc.sap.document.sap_com.Char10;
import functions.rfc.sap.document.sap_com.Char4;
import functions.rfc.sap.document.sap_com.ZMMPUR_GET_VENDOR_DETAILSBindingStub;
import functions.rfc.sap.document.sap_com._ZMMPUR_GET_VENDOR_DETAILS_IM_BUKRS;
import functions.rfc.sap.document.sap_com._ZMMPUR_GET_VENDOR_DETAILS_IM_EKORG;
import functions.rfc.sap.document.sap_com._ZMMPUR_GET_VENDOR_DETAILS_IM_LIFNR;
import functions.rfc.sap.document.sap_com.holders.Char3Holder;
import functions.rfc.sap.document.sap_com.holders.Char70Holder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSCODHolder;
import functions.rfc.sap.document.sap_com.holders._ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSTXTHolder;
import functions.rfc.sap.document.sap_com.ZWS_VEND_DATA_FEEDStub;
//import functions.rfc.sap.document.sap_com.ZWS_VEND_DATA_FEEDCallbackHandler;

 
public class SAPSupplierHook implements ApprovableHook
{
	private static final String Supplier_contact = ResourceService.getString("aml.cat.suppliereform","SupplierTeamContact");
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private AQLResultCollection results;
    private AQLQuery qry;
    private String query;
    private AQLOptions options;
    String prefOrdering;
    private static ZFIAP_RFC_VEND_DATA_FEEDSoapBindingStub stub;
    private Char4 companycode1;
    private Char4 purchaseorg1;
	private Char10 supplier1;
	private Char3Holder respCode = new Char3Holder();
    private Char70Holder iDoc = new Char70Holder();
	private Char70Holder resMsg = new Char70Holder();
    private ZWS_VEND_DATA_FEEDStub cgmstub;
    private ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEED request = null;
    private ZWS_VEND_DATA_FEEDStub.Char4 _companycodecgm = new ZWS_VEND_DATA_FEEDStub.Char4();
    private ZWS_VEND_DATA_FEEDStub.Char4 _purchaseorgcgm = new ZWS_VEND_DATA_FEEDStub.Char4();
	private ZWS_VEND_DATA_FEEDStub.Char12 _suppliercgm = new ZWS_VEND_DATA_FEEDStub.Char12();
	private ZWS_VEND_DATA_FEEDStub.Char3 _respCodecgm = new ZWS_VEND_DATA_FEEDStub.Char3();
	private ZWS_VEND_DATA_FEEDStub.Char70 _respMessagecgm = new ZWS_VEND_DATA_FEEDStub.Char70();
	private ZWS_VEND_DATA_FEEDStub.Char70 _respIdoccgm = new ZWS_VEND_DATA_FEEDStub.Char70();
	String respcode;
	String idoc;
	String resmsg;
	String partition_Name = "" ;
	//CBS  Declarations
	ZMMPUR_GET_VENDOR_DETAILSBindingStub cbsstub;
    public List run(Approvable approvable)
    {
			Log.customer.debug("*** entering eform submit core ***");
			ClusterRoot useform = (ClusterRoot)approvable;
			String s1,s2,s3,s41,s42,s5,s6,s7,s8,s9,s10,s11,s12,s13,s14,s15,s16,s17,s18,s19,s20,s21,s22,s23,s32,s24,s25,s26;
			String changes ="";
			String asnFlag ="";
			String ASNText ="";
			String source ="";
			String exceptionstr ="";
			Connection db2Conn = null;
			Statement stmt = null;
			ResultSet rs = null;
			String buildSQL;
			/* PK code changes start */
					
			int partitionNumber = useform.getPartitionNumber();
			if (partitionNumber == 5) {
                partition_Name = "SAP";  }
               if (partitionNumber == 6) {
               partition_Name = "LSAP"; }
               /* PK code changes End */
			
            Log.customer.debug("*** entering eform submit core ***");
			try
			{
              String uniqueName = (String)useform.getFieldValue("UniqueName");
              Log.customer.debug("**AR** uniqueName " + uniqueName);
              String tmp = useform.getFieldValue("SupplierCode").toString().toUpperCase();
              //if(tmp.length() == 6)
    			//{
					//tmp = tmp + "0";
				//}
    			if(useform.getDottedFieldValue("CompanyCode.UniqueName") == null)
				{
					s24 = "";
				}
				else
				{
					s24 = useform.getDottedFieldValue("CompanyCode.UniqueName").toString();
					
				}
				if(useform.getDottedFieldValue("CompanyCode.SAPSource") == null)
				{
					source = "";
				}
				else
				{
					source = useform.getDottedFieldValue("CompanyCode.SAPSource").toString();
				}
                try
                {
					// Updated the tm1 value to avoid and X7067H0 and X7067H0_P as X7067H and also to uppercase is not required
                	// String tmp1 = useform.getFieldValue("SupplierCode").toString().toUpperCase().substring(0,6);
                	String tmp1 = useform.getFieldValue("SupplierCode").toString();

					// Log.customer.debug("**AR** This is the 6 digit code: " + tmp1);
                	Log.customer.debug("**AR** This is the Complete Supplier code code: " + tmp1);

					// String query = "SELECT SUPPLIERID FROM IBM_SAP_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+tmp1+"%' and partition = 'SAP'";
        			/* PK code changes start */
                	String query = "SELECT SUPPLIERID FROM IBM_SAP_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+tmp1+"' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
        			/* PK code changes end */

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
					ASNText = "\nYou have selected ASN as Preffered Ordering Method on this eForm.  To begin the Supplier ASN Onboarding process you will find attached a Supplier Survey that you need to send to the supplier to be completed.  You will also find attached an excel spreadsheet that must be filled in using the completed survey once you receive it.  Please complete the fields in the spreadsheet that you have information for from the survey.  Once you complete the spreadsheet, please send to "+ Supplier_contact + " \n";
					s1 = useform.getFieldValue("SupplierCode").toString().toUpperCase();
					//if(s1.length() == 6)
					//{
						//	s1 = s1 + "0";
					//}
					s2 = partition_Name;
					s3 = "SAP";
					s41 = "+1.";
					s42 = "+0.";
					s5 = "";

			 // issue 951 setting s5 value based on POM
			    String newOrderingMethod = (String)useform.getFieldValue("PreferredOrderingMethod");
				  		if (newOrderingMethod != null)
				  		{
				  			s5 = newOrderingMethod;
		                }
				if(useform.getFieldValue("SupplierContactEmail") == null)
				{
					s6 = "";
				}
				else
				{
					s6 = useform.getFieldValue("SupplierContactEmail").toString();
				}
				if(useform.getFieldValue("SupplierWebSiteURL") == null)
				{
					s7 = "";
				}
				else
				{
					s7 = useform.getFieldValue("SupplierWebSiteURL").toString();
				}
				String name = useform.getFieldValue("SupplierName").toString();
				Log.customer.debug("SAPSupplierHook Supplier Name Initially "
						+ name);
				String replaceName;
				replaceName = replaceSpecialChar(name);
				useform.setFieldValue("SupplierName", replaceName);
				Log.customer.debug("SAPSupplierHook Supplier Name replaceName "
						+ replaceName);
				if(useform.getFieldValue("SupplierName") == null)
				{
					s8 = "";
				}
				else
				{
					s8 = useform.getFieldValue("SupplierName").toString();
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

				if(useform.getFieldValue("ASNAccountID") == null)
				{
					s14 = "";
				}
				else
				{
					s14 = useform.getFieldValue("ASNAccountID").toString();
				}
				if(useform.getFieldValue("AREmail") == null)
				{
					s23 = "";
				}
				else
				{
					s23 = useform.getFieldValue("AREmail").toString();
				}
				s19 = "";
				s20 = "";
				s21 = "";
				s22 = "";
            	//s23 = Base.getSession().getRealUser().getFieldValue("UniqueName").toString();
			/*if(useform.getDottedFieldValue("CompanyCode.UniqueName") == null)
    			{
					s24 = "";
				}
				else
				{
					s24 = useform.getDottedFieldValue("CompanyCode.UniqueName").toString();
				}*/
    			if(useform.getDottedFieldValue("PurchaseOrg.UniqueName") == null)
				{
					s25 = "";
				}
				else
				{
					s25 = useform.getDottedFieldValue("PurchaseOrg.UniqueName").toString();
                }
    			if(useform.getDottedFieldValue("CompanyCode.SAPSource") == null)
				{
					s26 = "";
				}
				else
				{
					s26 = useform.getDottedFieldValue("CompanyCode.SAPSource").toString();
				}
			  asnFlag = useform.getFieldValue("ASNOnboarding").toString();
    		  String resetFax = useform.getFieldValue("ResetToFax").toString();
			  Log.customer.debug("**AR** asn flag: " + asnFlag);
			  Log.customer.debug("**AR** reset to fax flag: " + resetFax);
              changes = "This is a new MSC Supplier.\n";
              if(rs.next())
    		   {
     			   if(rs.getString(1) != null)
    			   {
			      /*   Log.customer.debug(" *** this is an update ***");
			         //String buildSQL = "UPDATE ibm_sap_supplier_supplement set isnew = 0. WHERE SUPPLIERID = '" + tmp + "' and partition = 'SAP' AND COMPANYCODE LIKE  '" + s24 + "'";
    				  // Log.customer.debug(buildSQL);
					  // stmt.executeUpdate(buildSQL);
    				   String buildSQL = "select PREFERREDORDERINGMETHOD from ibm_sap_supplier_supplement WHERE SUPPLIERID = '" + tmp + "' and partition = 'SAP' AND COMPANYCODE LIKE  '" + s24 + "'";
					   Log.customer.debug(buildSQL);
					   ResultSet rs2 = stmt.executeQuery(buildSQL);
					   if(rs2.next())
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
					   changes = "\nChanges made in this transaction: \n";
					   Log.customer.debug(changes);
					   buildSQL = "UPDATE ibm_sap_supplier_supplement set PREFERREDORDERINGMETHOD = '" + prefOrdering + "' WHERE SUPPLIERID = '" + tmp + "' and partition = 'SAP' AND COMPANYCODE LIKE  '" + s24 + "'";
					   Log.customer.debug(buildSQL);
					   stmt.executeUpdate(buildSQL);
					   */

					   if(useform.getFieldValue("SupplierContactEmail") != null)
					   {
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set EMAILADDRESS = '" + s6 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes end */
						   Log.customer.debug(buildSQL);
						   stmt.executeUpdate(buildSQL);
						   changes = changes + "email address set to: " + s6 + " \n";
						   Log.customer.debug(changes);
					   }
					   if(useform.getFieldValue("SupplierWebSiteURL") != null)
					   {
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set URL = '" + s7 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes end */
						   Log.customer.debug(buildSQL);
						   stmt.executeUpdate(buildSQL);
						   changes = changes + "URL set to: " + s7 + " \n";
						   Log.customer.debug(changes);
					   }
					   if(useform.getFieldValue("ASNAccountID") != null)
					   {
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set ARIBANETWORKID = '" + s14 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes start */
						   Log.customer.debug(buildSQL);
						   stmt.executeUpdate(buildSQL);
						   changes = changes + "ASN ID set to: " + s14 + " \n";
						   Log.customer.debug(changes);
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes end */
						   Log.customer.debug(buildSQL);
						   stmt.executeUpdate(buildSQL);
						   changes = changes + "Ordering Method set to: " + s5 + " \n";
						   Log.customer.debug(changes);
					   }
                    if (useform.getFieldValue("ASNAccountID")==(null) && useform.getFieldValue("PreferredOrderingMethod")!=(null))
						{
            			/* PK code changes start */
							 buildSQL = "UPDATE ibm_sap_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
								/* PK code changes end */
							 Log.customer.debug(buildSQL);
							stmt.executeUpdate(buildSQL);
							changes = changes + " PREFERRED ORDERING METHOD set to : " + s5 + "\n";
							Log.customer.debug(changes);
					}
					   if(useform.getFieldValue("AREmail") != null)
					   {
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set SupplierAPEmailAddress = '" + s22 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes end */
						   Log.customer.debug(buildSQL);
						   stmt.executeUpdate(buildSQL);
						   changes = changes + "AP email set to: " + s23 + " \n";
							/* PK code changes start */
						   buildSQL = "UPDATE ibm_sap_supplier_supplement set PREFERREDORDERINGMETHOD = '" + s5 + "' WHERE SUPPLIERID = '" + tmp + "' and partition = '" + partition_Name + "'  AND COMPANYCODE LIKE  '" + s24 + "'";
							/* PK code changes end */
						   Log.customer.debug(buildSQL);

						   stmt.executeUpdate(buildSQL);

						   changes = changes + "Ordering Method set to: " + s5 + " \n";

						   Log.customer.debug(changes);
						  Log.customer.debug(changes);
					   }
					   Log.customer.debug("ran sql 1");
					   Log.customer.debug("Final changes: " + changes);
				   }
	             else
				   {
						Log.customer.debug(" *** this is an insert ***");
					   // Commented by Majid	- Changed IsNew Flag field from s42 to s41 for new supplier
					   // For New Supplier ISNew flag should be 1
					   //String buildSQL1 = "INSERT INTO ibm_sap_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,ARCEMAIL,SupplierAPEmailAddress,COMPANYCODE,PURCHASEORG,SAPSOURCE ) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s42 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + s23 + "','" + s24 + "','" + s25 + "','" + s26 + "')";
					 String buildSQL1 = "INSERT INTO ibm_sap_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,ARCEMAIL,SupplierAPEmailAddress,COMPANYCODE,PURCHASEORG,SAPSOURCE ) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s41 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + s23 + "','" + s24 + "','" + s25 + "','" + s26 + "')";
Log.customer.debug(buildSQL1);
stmt.executeUpdate(buildSQL1);
Log.customer.debug("ran sql 2");
								 //added for extension for companycode
								 /* String companyquery = "SELECT SUPPLIERID FROM IBM_SAP_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+tmp+"%' and partition = 'SAP'" ;
								 ResultSet rs12 = stmt.executeQuery(companyquery);
					   if(rs12.next())
     					{
						   buildSQL1 = "UPDATE ibm_sap_supplier_supplement set isnew = 0 WHERE SUPPLIERID = '" + tmp + "' and partition = 'SAP' AND COMPANYCODE LIKE  '" + s24 + "'";
    						Log.customer.debug(buildSQL1);
							stmt.executeUpdate(buildSQL1);
						}
*/
				  }
				   }
			   else
			   {
				    Log.customer.debug(" *** this is an insert ***");
				       //	Commented by Majid	- Changed IsNew Flag field from s42 to s41 for new supplier
					   // 	For New Supplier ISNew flag should be 1
				       //   String buildSQL = "INSERT INTO ibm_sap_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,ARCEMAIL,SupplierAPEmailAddress,COMPANYCODE,PURCHASEORG,SAPSOURCE ) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s42 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + s23 + "','" + s24 + "','" + s25 + "','" + s26 + "')";
					/* PK code changes start */
				    String companyquery = "SELECT SUPPLIERID FROM IBM_SAP_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+tmp+"%' and partition = '" + partition_Name + "' " ;
					/* PK code changes end */
				    ResultSet rs12 = stmt.executeQuery(companyquery);
					if(rs12.next())
					{
						s41 = "0";
					}
					buildSQL = "INSERT INTO ibm_sap_supplier_supplement (SUPPLIERID,PARTITION,VARIANT,ISNEW,PREFERREDORDERINGMETHOD,EMAILADDRESS,URL,SUPPLIERNAME,ACCOUNTMANAGER,PHONEAREACODE,PHONENUMBER,FAXAREACODE,FAXNUMBER,ARIBANETWORKID,AUTHORIZEDDX,AUTHORIZEDNA,AUTHORIZEDMX,AUTHORIZEDMY,VATREGNUMBER,VATCLASSNAME,PAYMENTTERMS,ARCEMAIL,SupplierAPEmailAddress,COMPANYCODE,PURCHASEORG,SAPSOURCE ) VALUES ('" + s1 + "','" + s2 + "','" + s3 + "'," + s41 + ",'" + s5 + "','" + s6 + "','" + s7 + "','" + s8 + "','" + s9 + "','" + s10 + "','" + s11 + "','" + s12 + "','" + s13 + "','" + s14 + "','" + s15 + "','" + s16 + "','" + s17 + "','" + s18 + "','" + s19 + "','" + s20 + "','" + s21 + "','" + s22 + "','" + s23 + "','" + s24 + "','" + s25 + "','" + s26 + "')";
					Log.customer.debug(buildSQL);
stmt.executeUpdate(buildSQL);

Log.customer.debug("ran sql 2");
 }
 Log.customer.debug("completed insert / update");
				String value = webServiceCall(tmp,s24,s25,s26);
                Log.customer.debug("value=>"+value);
                 //Added code for exceptions
    			   if(value.equals("sucess")|| value =="sucess")
					{
					   Log.customer.debug(" entered to if sucess");
					}
					else{
 						Log.customer.debug(" entered to DELQUERY");
						String delquery ="DELETE ibm_sap_supplier_supplement  WHERE SUPPLIERID LIKE '" + s1 + "' AND  COMPANYCODE  LIKE '" + s24 + "'";
      					 stmt.executeUpdate(delquery);
						   Log.customer.debug(delquery);
						  Log.customer.debug("DELQUERY",delquery );
						return ListUtil.list(Constants.getInteger(-1), "WEB service call is not sucessful Pls contact Administrator");
					}
    	           // Commented by Majid
	    			//createSupplierPorg(tmp,s24,s25);
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
			          /* Properties props = System.getProperties();
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
						message="A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the SAP partition.\nPlease do not reply to this mail ID as this is an automated message.";

			              /* message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
			               message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(eId));
			               message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(Base.getService().getParameter(null, "System.Base.CATEmailID")));
			               message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID1")));
			               message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID2")));
			               message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));
			               message.setSubject("Supplier eform: Request to Deactivate Supplier");
			               message.setText("A request has been raised in the MSC system to deactivate this supplier: " + tmp + ". \nThis request was raised by: " + eId + " in the SAP partition.\nPlease do not reply to this mail ID as this is an automated message.");
*/
			               Log.customer.debug("** mailing deactivation **");
							Log.customer.debug("** mailing deactivation **");
					List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.SAP","SupplierEformEmailIds",eId), ':'));
					for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
						String toAddress = (String)it.next();
				        Log.customer.debug("Values for toAddress = %s", toAddress);
					    CatEmailNotificationUtil.sendNotification(msgSubject, message, toAddress, null);
						}
					 } else {
            Log.customer.debug("** calling supplier download **");
            SupplierDownload sd = new SupplierDownload();
            sd.setSupplierCode(tmp);
            String status = sd.getResponse();
            Log.customer.debug(tmp);
            Log.customer.debug(useform.getFieldValue("Action").toString());
            /*message.setFrom(new InternetAddress("MSC_Supplier_eForm@cat.com"));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(eId));
            message.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(Base.getService().getParameter(null, "System.Base.CATEmailID")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID1")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID2")));
            message.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(Base.getService().getParameter(null, "System.Base.AMSEmailID3")));*/
            Log.customer.debug("source" + source);
			if(source.equals("CGM")) {
			Log.customer.debug("source" + source);
			msgSubject="In NonMach1SAP partition,Supplier eform " + uniqueName  +  " : " + tmp + " added on production";
			Log.customer.debug("msgSubject" + msgSubject);
			}
			else 
			{
			Log.customer.debug("source" + source);
			msgSubject="In SAP partition,Supplier eform " + uniqueName  +  " : " + tmp + " added on production";
			Log.customer.debug("msgSubject" + msgSubject);
			}
            if(asnFlag.equals("true"))
            changes = changes + ASNText;
            Log.customer.debug("Supplier eform tmp" + tmp);
            message=Fmt.Sil("cat.vcsv1", "SupplierEformMessage",tmp,status,changes);

            if(asnFlag.equals("true")) {
                Log.customer.debug("**AR** inside asnFlag **");
                reportFileZip = new File("/msc/arb821/Server/classes/extensions/Templates.zip");
                ListUtil.addElementIfAbsent(attachments,reportFileZip);
                Log.customer.debug("**AR** set multipart **");
            }
            Log.customer.debug("** mailing insert / update **");
            List toAddressList = ListUtil.arrayToList(StringUtil.delimitedStringToArray(Fmt.Sil("cat.vcsv1","SupplierEformEmailIds",eId), ':'));
			for(ListIterator it = toAddressList.listIterator(); it.hasNext();) {
				String toAddress = (String)it.next();
				Log.customer.debug("Values for toAddress = %s", toAddress);
			CatEmailNotificationUtil.sendNotification( msgSubject, message, toAddress, attachments);
	     }
        }

			           //Transport.send(message);
			           Log.customer.debug("*** exiting supplier eform submit ***");
			           return NoErrorResult;
			   	    }
			   		catch (Exception e)
			   		{
		   				Log.customer.debug(e.toString());
		   				return NoErrorResult;
			   		}
			       }
public  String webServiceCall(String suppliercode,String companycode,String purchaseorg,String sapsource)
{
try{
 if(sapsource.equals("MACH1"))
			{
				String mach1EndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/VendorDataFeed_Ariba_SAPMach1Web/sca/ZFIAP_RFC_VEND_DATA_FEED";
				String mach1EndPointStr = ResourceService.getString("cat.java.sap","VendorDownloadWSCalltheEndPointmach1");
				Log.customer.debug("Value Taken from resource String : "+ mach1EndPointStr);
				String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(mach1EndPointStr))?mach1EndPointStr:mach1EndPointDefault) ;
				Log.customer.debug("SAPSupplierLookup.fire theEndPoint:"+ theEndPoint);
				URL endpoint = new URL(theEndPoint);
				Log.customer.debug("SAPSupplierLookup.fire endpoint:"+ endpoint);
				stub = new ZFIAP_RFC_VEND_DATA_FEEDSoapBindingStub(endpoint, null);
				companycode1 = new Char4(companycode);
				Log.customer.debug("companycode1=>"+companycode1);
				purchaseorg1 = new Char4(purchaseorg);
				Log.customer.debug("companycode1=>"+purchaseorg1);
				supplier1 = new Char10(suppliercode);
				Log.customer.debug("supplier1=>"+supplier1);
				stub.zfiapRfcVendDataFeed(companycode1,purchaseorg1,supplier1,respCode,iDoc,resMsg);
				respcode = respCode.value.toString();
				Log.customer.debug("respcode=>"+respcode);
				resmsg = resMsg.value.toString();
				Log.customer.debug("resmsg=>"+resmsg);
				idoc = iDoc.value.toString();
				Log.customer.debug("idoc=>"+idoc);
			if(respcode!= null)
			  {
                if(respcode.equals("000"))
			 {
				return "sucess";
				}
			else
				  {
					return resmsg;
					}
			  }

			}
	/* PK code changes start */
	else if(sapsource.equals("CGM"))
		{
	    String cgmEndPointDefault = "http://172.16.51.242:8001/sap/bc/srt/rfc/sap/zws_vend_data_feed/140/zws_vend_data_feed/zws_vend_data_feed";
		String cgmEndPointStr = ResourceService.getString("cat.java.sap","VendorDownloadWSCalltheEndPointcgm");
		Log.customer.debug("Value Taken from resource String : "+ cgmEndPointDefault);
		String theEndPointcgm = ((!StringUtil.nullOrEmptyOrBlankString(cgmEndPointStr))?cgmEndPointStr:cgmEndPointDefault) ;
		Log.customer.debug("SAPSupplierLookup.fire theEndPoint:"+ theEndPointcgm);
		
        cgmstub = new ZWS_VEND_DATA_FEEDStub(null,theEndPointcgm);
        _companycodecgm.setChar4(companycode);
        _purchaseorgcgm.setChar4(purchaseorg);
        _suppliercgm.setChar12(suppliercode);
        
        request = new ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEED();
        request.setIM_BUKRS(_companycodecgm);
        request.setIM_EIKTO(_suppliercgm);
        request.setIM_EKORG(_purchaseorgcgm);

        
        ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEEDResponse response = cgmstub.zFIAP_RFC_VEND_DATA_FEED(request);
		String msgCodecgm ;
		String msgtxtcgm ;
	    String msgidoccgm ;
		
		_respCodecgm = response.getEX_MSG_CODE();
		msgCodecgm = _respCodecgm.toString();
		Log.customer.debug("msgCode=>"+msgCodecgm);
		_respMessagecgm = response.getEX_MSG_TXT();
		msgtxtcgm = _respMessagecgm.toString();
		Log.customer.debug("msgtxt=>"+msgtxtcgm);
		_respIdoccgm = response.getEX_MSG_IDOC();
		msgidoccgm = _respIdoccgm.toString();
		Log.customer.debug("msgidoc=>"+msgidoccgm);
		if(msgCodecgm!= null)
	  {
        if(msgCodecgm.equals("000"))
	 {

		return "sucess";
		}
	else
		  {
			return msgtxtcgm;
			}
		}

	}
	/* PK code changes end */
			else
			{
				if(sapsource.equals("CBS"))
				{
					String cbsEndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/VendorDataFeed_Ariba_CBSWeb/sca/ZMMPUR_GET_VENDOR_DETAILSPortType";
				String cbsEndPointStr = ResourceService.getString("cat.java.sap","VendorDownloadWSCalltheEndPointcbs");
				Log.customer.debug("Value Taken from resource String : "+ cbsEndPointDefault);
				String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(cbsEndPointStr))?cbsEndPointStr:cbsEndPointDefault) ;
				Log.customer.debug("SAPSupplierLookup.fire theEndPoint:"+ cbsEndPointStr);
				URL endpointcbs = new URL(theEndPoint);
				Log.customer.debug("SAPSupplierLookup.fire endpointcbs:"+ endpointcbs);
				cbsstub = new ZMMPUR_GET_VENDOR_DETAILSBindingStub(endpointcbs, null);
				_ZMMPUR_GET_VENDOR_DETAILS_IM_BUKRS companycodecbs= new _ZMMPUR_GET_VENDOR_DETAILS_IM_BUKRS(companycode);
				Log.customer.debug("companycodecbs=>"+companycodecbs);
				_ZMMPUR_GET_VENDOR_DETAILS_IM_EKORG purchaseorgcbs = new _ZMMPUR_GET_VENDOR_DETAILS_IM_EKORG(purchaseorg);
				Log.customer.debug("purchaseorgcbs=>"+purchaseorgcbs);
				_ZMMPUR_GET_VENDOR_DETAILS_IM_LIFNR suppliercbs = new _ZMMPUR_GET_VENDOR_DETAILS_IM_LIFNR(suppliercode);
				Log.customer.debug("suppliercbs=>"+suppliercbs);
				String msgCode ;
				String msgtxt ;
				_ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSCODHolder msdcodecbs = new _ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSCODHolder();
					_ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSTXTHolder msgtxtcbs = new _ZMMPUR_GET_VENDOR_DETAILSResponse_EX_MSTXTHolder();
				cbsstub.ZMMPUR_GET_VENDOR_DETAILS(companycodecbs,purchaseorgcbs,suppliercbs,msdcodecbs,msgtxtcbs);

				msgCode = msdcodecbs.value.toString();
				Log.customer.debug("msgCode=>"+msgCode);
				msgtxt = msgtxtcbs.value.toString();
				Log.customer.debug("msgtxt=>"+msgtxt);
				if(msgCode!= null)
			  {
                if(msgCode.equals("000"))
			 {

				return "sucess";
				}
			else
				  {
					return msgtxt;
					}
				}

			}
			}
return "sucess";

}
 catch(Exception e){
ariba.util.log.Log.customer.debug(e.toString());
Log.customer.debug("entered to catch block");
return e.toString();
}
}
public void createSupplierPorg(String Porg,String supplier)
{
	Partition partition = Base.getService().getPartition(partition_Name);
BaseObject supplierPorg = (BaseObject)BaseObject.create("ariba.core.PorgSupplierCombo",partition);
//invoiceLines.add(invoiceLI);
supplierPorg.setFieldValue("PurchaseOrg ", Porg);
supplierPorg.setFieldValue("Supplier ", supplier);
Log.customer.debug("supplierPorg=>"+supplierPorg);
}
//public void boolean isExisting(String Supplier)
	//{
	//String query = "SELECT SUPPLIERID FROM IBM_SAP_SUPPLIER_SUPPLEMENT WHERE SUPPLIERID like '"+Supplier+"%' and partition = 'SAP'" ;
	//}

/*
 * Issue 1146 : Replacing Spl Character by Space in Supplier Name
 * Author : Lekshmi IBM
 */
  String replaceSpecialChar(String name) {
	Log.customer.debug("Initial Value of Supplier name" + name);
	char symbol[] = { '\'', '\"', '.', '\\', '/', '-', '(', ')', ',' };
	// int size = Array.getLength(symbol);
	Log.customer.debug("Size of array" + symbol.length);
	for (int i = 0; i < symbol.length; i++) {
		Log.customer.debug("Char is " + symbol[i]);
		name = StringUtil.replaceCharByString(name, symbol[i], " ");
		Log.customer.debug("Value of name in loop: " + name
				+ " after replacing character " + symbol[i]);

	}

	Log.customer.debug("Final Value of Supplier name" + name);
	return name;
}
			       public SAPSupplierHook()
			       {
			   		        	super();
			       }
			   }