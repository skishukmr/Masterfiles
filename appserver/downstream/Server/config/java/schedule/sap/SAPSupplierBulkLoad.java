/**********************************************************************************

Creator: Nagendra

Description:

05/10/2012	Purush Kancharla	WI 319- Logic added for SAPSupplierBulkLoad for LSAP partition.
************************************************************************************/

package config.java.schedule.sap;


import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import mc_style.functions.soap.sap.document.sap_com.ZFIAP_RFC_VEND_DATA_FEEDSoapBindingStub;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.receiving.core.Receipt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
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
import functions.rfc.sap.document.sap_com.ZWS_VEND_DATA_FEEDCallbackHandler;
import functions.rfc.sap.document.sap_com.ZWS_VEND_DATA_FEEDStub;
import functions.rfc.sap.document.sap_com.*;





public class SAPSupplierBulkLoad extends ScheduledTask {

	private Partition partition;

	private String query = null;

	private String companycode = null;

	private String purchaseorg = null;

	private String supplier = null;

	boolean isHeader = false;

	private static final String thisclass = "SAPSupplierBulkLoad: ";

	private static ZFIAP_RFC_VEND_DATA_FEEDSoapBindingStub stub;

	private static ZWS_VEND_DATA_FEEDStub cstub;

    private Char4 companycode1;

    private Char4 purchaseorg1;

	private Char10 supplier1;

	private Char3Holder respCode = new Char3Holder();

	private Char70Holder iDoc = new Char70Holder();

	private Char70Holder resMsg = new Char70Holder();
/* CGM  Declarations start */

    private ZWS_VEND_DATA_FEEDStub cgmstub;
    private ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEED request = null;
    private ZWS_VEND_DATA_FEEDStub.Char4 _companycodecgm = new ZWS_VEND_DATA_FEEDStub.Char4();
    private ZWS_VEND_DATA_FEEDStub.Char4 _purchaseorgcgm = new ZWS_VEND_DATA_FEEDStub.Char4();
	private ZWS_VEND_DATA_FEEDStub.Char12 _suppliercgm = new ZWS_VEND_DATA_FEEDStub.Char12();
	private ZWS_VEND_DATA_FEEDStub.Char3 _respCodecgm = new ZWS_VEND_DATA_FEEDStub.Char3();
	private ZWS_VEND_DATA_FEEDStub.Char70 _respMessagecgm = new ZWS_VEND_DATA_FEEDStub.Char70();
	private ZWS_VEND_DATA_FEEDStub.Char70 _respIdoccgm = new ZWS_VEND_DATA_FEEDStub.Char70();
/* CGM  Declarations end */
	String respcode;

	String idoc;

	String resmsg;

    String theEndPoint;

	String noofrecords;

	//CBS  Declarations
	ZMMPUR_GET_VENDOR_DETAILSBindingStub cbsstub;



public void init(Scheduler scheduler, String scheduledTaskName, Map arguments)
{

			super.init(scheduler, scheduledTaskName, arguments);
			for(Iterator e = arguments.keySet().iterator(); e.hasNext();)
			{
				String key = (String)e.next();
				if (key.equals("queryST"))
				{
				query  = (String)arguments.get(key);
				Log.customer.debug("query");
				}
				else if(key.equals("NoOfRec"))
				{
				noofrecords  = (String)arguments.get(key);
				Log.customer.debug(" noofrecords=>"+noofrecords);
				}
			}

}


	public void run() throws ScheduledTaskException {

	         Log.customer.debug("beginning SAPSupplierBulkLoad...",thisclass);
	         partition = Base.getSession().getPartition();
	         try {
	        	 	Log.customer.debug("%s %s",query,thisclass);
		        	Receipt receipt = null;
		            AQLQuery aqlquery = null;
		            AQLQuery aqlquery1 = null;
		 			AQLOptions options = null;
		 			AQLOptions options1 = null;
		 			AQLResultCollection results = null;
		 			AQLResultCollection rs = null;
				    BaseId baseId = null;
				    aqlquery = AQLQuery.parseQuery(query);
					Log.customer.debug("aqlquery=>"+aqlquery);
					Log.customer.debug("partition=>"+partition);
					options = new AQLOptions(partition);
					results = Base.getService().executeQuery(aqlquery, options);
					Log.customer.debug("noofrecords=>"+noofrecords);
					int noofrecords1 = 25;
					noofrecords1 =Integer.parseInt(noofrecords);
					Log.customer.debug(" noofrecords1=>"+noofrecords1);
					Connection db2Conn = null;
					Statement stmt = null;
					String sapsource = null;
					int i= 1;
					if( (results != null) && (!results.isEmpty()))
					{
            		   while(results.next())
            		   {
            			   // Increment each time counter by 1

            			   Log.customer.debug(" Number of records in Process =>"+ i);
            			   if(i >= noofrecords1)
            			   {
            				   Log.customer.debug("Terminating the SUPPLIER BULK LOAD : job =>"+ i);
            				   break;

            			   }
            			   i++;

            			   String uniquename=(String) results.getString(0);
            			   Log.customer.debug(" companycode=>"+uniquename);
            			   companycode=(String) results.getString(1);
            			   Log.customer.debug(" companycode=>"+companycode);
            			   purchaseorg=(String) results.getString(2);
            			   Log.customer.debug(" purchaseorg1=>"+purchaseorg);
            			   supplier=uniquename;
            			   Log.customer.debug(" supplier=>"+supplier);
						    sapsource=(String) results.getString(3);
            			   Log.customer.debug(" sapsource=>"+sapsource);

            			   try
            			   {

								Class.forName ("COM.ibm.db2.jdbc.app.DB2Driver");
								String DBName = Base.getService().getParameter(null,"System.Base.DBName");
								DBName = "jdbc:db2:" + DBName;
								String DBUser = Base.getService().getParameter(null,"System.Base.DBUser");
								String DBPwd = Base.getService().getParameter(null,"System.Base.DBPwd");
								Log.customer.debug(" DBName=>"+DBName);
								Log.customer.debug(" DBUser=>"+DBUser);
								Log.customer.debug(" DBPwd=>"+DBPwd);
								db2Conn = DriverManager.getConnection(DBName,DBUser,DBPwd);
								stmt = db2Conn.createStatement();

								if(companycode!=null && uniquename!= null && purchaseorg!=null && sapsource!=null)
								{
									String query1 = "Update IBM_SAP_SUPPLIER_SUPPLEMENT set isNew =1 " +
									"where Supplierid = '"+uniquename+ "'  and COMPANYCODE = '"+companycode+"' " +
									"and PURCHASEORG = '"+purchaseorg+"'";

									Log.customer.debug("Updating staging table");
									Log.customer.debug("Update query => "+query1);
									stmt.executeUpdate(query1);
									webServiceCall(uniquename,companycode,purchaseorg,sapsource);

									// Assuming we will load supplier with IsNew flag as "2" through scripts
									// to enable it for first time
									// and to extend the same supplier for different company code
									// We will load them with IsNew Flag as "3"
									// after enabling the Vendor for first time, do a query with same Vendor id
									// and get the record which has IsNew flag as "3"
									// Set IsNew flag for those record as "0" and then call the webservice again to extend the Supplier
									// for additional company code.

									String extVendorQuery = " SELECT DISTINCT COMPANYCODE,PURCHASEORG FROM IBM_SAP_SUPPLIER_SUPPLEMENT " +
									"WHERE SUPPLIERID = '"+uniquename+"'AND ISNEW = 3";
									Log.customer.debug("SUPPLIER BULK LOAD : QUERY TO EXTEND THE SAME SUPPLIER FOR DIFF COMPANY CODE query => "+extVendorQuery);
									rs = Base.getService().executeQuery(extVendorQuery, options);


									if( (rs != null) && (!rs.isEmpty()))
									{
									  while(rs.next())
				            		   {
										  Log.customer.debug("Inside Loop : To extend vendors for different company code");

				            			   Log.customer.debug("For Vendor =>"+supplier);

				            			   String extCompanyCode=(String) rs.getString(0);
				            			   Log.customer.debug("For Company Code =>"+extCompanyCode);

				            			   String extPurchaseOrg=(String) rs.getString(1);
				            			   Log.customer.debug("For Purchase Org =>"+extPurchaseOrg);

				            			   if(extPurchaseOrg!=null && extCompanyCode!= null)
				            			   {
				            			   // Make sure we load them as IsNew Flag as "0" to get message from WBI as "Update"
				            			   	String queryupdate = "Update IBM_SAP_SUPPLIER_SUPPLEMENT set isNew =0 " +
											"where Supplierid = '"+uniquename+ "'  and COMPANYCODE = '"+extCompanyCode+"' " +
											"and PURCHASEORG = '"+extPurchaseOrg+"'";
				            			   	Log.customer.debug("Updating staging table for supplier which needs to be extended for Different company code");
											Log.customer.debug("Update query => "+queryupdate);
											stmt.executeUpdate(queryupdate);
											Log.customer.debug("Data updated");
											Log.customer.debug("Calling WebService for  Supplier = > "+supplier+" ComapnyCode =>  "+extCompanyCode+"  Purchase Org => "+ extPurchaseOrg);
											webServiceCall(uniquename,extCompanyCode,extPurchaseOrg,sapsource);
				            			   }
				            		   }
									}
									else
									{
										Log.customer.debug("OutSide Loop :: To extend vendors for different company code :: No of records found is Zero");
									}
								}

            			   } catch(Exception e) {
						 	Log.customer.debug("%s %s", e.toString(), thisclass);
            			   	}
            		   	}

					}
					else
					{
						Log.customer.debug("There is no such data to process the Load supplier");
					}

			}
	         catch(Exception e) {
				Log.customer.debug("%s %s", e.toString(), thisclass);
        		}

	         Log.customer.debug("SUPPLIER BULK LOAD Job has finished processing");
}



public void webServiceCall(String suppliercode,String companycode,String purchaseorg,String sapsource)

	{
             if(sapsource.equals("MACH1"))
		{

				try{

				String mach1EndPointDefault = "http://adwpst1.ecorp.cat.com:9080/VendorDataFeed_Ariba_SAPMach1Web/sca/ZFIAP_RFC_VEND_DATA_FEED";

				String mach1EndPointStr = ResourceService.getString("cat.java.sap","VendorDownloadWSCalltheEndPointmach1");

				Log.customer.debug("Value Taken from resource String : "+ mach1EndPointStr);

				theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(mach1EndPointStr))?mach1EndPointStr:mach1EndPointDefault) ;

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

				respcode = respCode.toString();

				Log.customer.debug("respcode=>"+respcode);

				idoc = iDoc.toString();

				Log.customer.debug("idoc=>"+idoc);

				resmsg = resMsg.toString();

				Log.customer.debug("resmsg=>"+resmsg);

				}
				catch(Exception e){
					ariba.util.log.Log.customer.debug(e.toString());

				}
		}
		if (sapsource.equals("CGM")) // PK code changes START 
			{
				try{
				Log.customer.debug("SAPSupplierBulkLoad: Enters the CGM Sapsource"+sapsource);
				String cgmEndPointDefault = "http://172.20.182.51:8001/sap/bc/srt/rfc/sap/zws_vend_data_feed/140/zws_vend_data_feed/zws_vend_data_feed";
				String cgmEndPointStr = ResourceService.getString("cat.java.sap","VendorDownloadWSCalltheEndPointcgm");
				Log.customer.debug("Value Taken from resource String : "+ cgmEndPointStr);
			//	String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(cgmEndPointStr))?cgmEndPointStr:cgmEndPointDefault) ;
				String theEndPointcgm = ((!StringUtil.nullOrEmptyOrBlankString(cgmEndPointStr))?cgmEndPointStr:cgmEndPointDefault) ;
				Log.customer.debug("SAPSupplierBulkLoad.fire theEndPointcgm:"+ theEndPointcgm);
				//URL cgmendpoint = new URL(theEndPoint);
				Log.customer.debug("SAPSupplierBulkLoad.fire cgmendpoint:"+ theEndPointcgm);
				cgmstub = new ZWS_VEND_DATA_FEEDStub(null,theEndPointcgm);
				Log.customer.debug("SAPSupplierBulkLoad.fire cgmstub:"+ cgmstub);
		_companycodecgm.setChar4(companycode);
		Log.customer.debug("SAPSupplierBulkLoad.fire companycode:"+ companycode);
        _purchaseorgcgm.setChar4(purchaseorg);
		Log.customer.debug("SAPSupplierBulkLoad.fire purchaseorg:"+ purchaseorg);
        _suppliercgm.setChar12(suppliercode);
		Log.customer.debug("SAPSupplierBulkLoad.fire suppliercode:"+ suppliercode);
		request = new ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEED();
		Log.customer.debug("SAPSupplierBulkLoad.fire request:"+ request);
        request.setIM_BUKRS(_companycodecgm);
		Log.customer.debug("SAPSupplierBulkLoad.fire _companycodecgm:"+ _companycodecgm);
        request.setIM_EIKTO(_suppliercgm);
		Log.customer.debug("SAPSupplierBulkLoad.fire _suppliercgm:"+ _suppliercgm);
        request.setIM_EKORG(_purchaseorgcgm);
		Log.customer.debug("SAPSupplierBulkLoad.fire _purchaseorgcgm:"+ _purchaseorgcgm);
		ZWS_VEND_DATA_FEEDStub.ZFIAP_RFC_VEND_DATA_FEEDResponse response = cgmstub.zFIAP_RFC_VEND_DATA_FEED(request);
		Log.customer.debug("SAPSupplierBulkLoad.fire response:"+ response);
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
				}
				catch(Exception e){
				ariba.util.log.Log.customer.debug(e.toString());

				}
			}// PK code changes END

			if(sapsource.equals("CBS"))
			{
				try{
				String cbsEndPointDefault = "http://adwpst1.ecorp.cat.com:9080/VendorDataFeed_Ariba_CBSWeb/sca/ZMMPUR_GET_VENDOR_DETAILSPortType";
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
				}
					catch(Exception e){
					ariba.util.log.Log.customer.debug(e.toString());

				}
			}
		
}

public void createSupplierPorg(String Porg,String supplier)
{
	//Partition partition = Base.getService().getPartition("SAP");
	Partition partition = Base.getService().getPartition();
	BaseObject supplierPorg = (BaseObject)BaseObject.create("ariba.core.PorgSupplierCombo",partition);
	supplierPorg.setFieldValue("PurchaseOrg ", Porg);
	supplierPorg.setFieldValue("Supplier ", supplier);
	Log.customer.debug("supplierPorg=>"+supplierPorg);
}
}
