/*
	Author: Archana
    This scheduler runs every two hours and gethers the information about all IR's which has 
	    Vertex Manager and makes call to webservice..
		
		Change History
	#	Change By	Change Date		Description
	=============================================================================================

*/
package config.java.schedule.sap;
import config.java.customapprover.VertexTaxIRWSCall;
import ariba.approvable.core.Approvable;
import config.java.common.CatCommonUtil;
import ariba.util.core.ResourceService;
import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseObject;
import ariba.base.core.Base;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.tax.core.TaxCode;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.user.core.Role;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.receiving.core.Receipt;
import ariba.user.core.User;
import ariba.util.core.SystemUtil;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.scheduler.ScheduledTask;
import ariba.util.scheduler.ScheduledTaskException;
import ariba.util.scheduler.Scheduler;
import ariba.approvable.core.ApprovableUtil;
import ariba.basic.core.Money;
import ariba.basic.core.Currency;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ariba.base.core.*;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import config.java.customapprover.sap.VertexTaxWSCall;
import ariba.util.core.ResourceService;
import ariba.user.core.Approver;
import ariba.util.core.StringUtil;
import ariba.approvable.core.CustomApprover;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;

public class CatSAPVertexSchedulerIR extends ScheduledTask {

	private static final String THISCLASS = "CatSAPVertexSchedulerIR";
	private String appPart = "";
	private int partition ;
	User erpreceiptuser = null;
	User erpreceiptuser1 = null;
	Approvable receipt;
	
	public void init(Scheduler scheduler, String scheduledTaskName,
			Map arguments) {
		super.init(scheduler, scheduledTaskName, arguments);
	}

	public void run() throws ScheduledTaskException {
		Log.customer.debug("%s : Begin. SCHEDULER...", THISCLASS);
		erpreceiptuser = getERPReceptApproverUser();
		try {
	//		partition = Base.getSession().getPartition().intValue();
			if (checkExternalApproveRequest()) {
				Log.customer.debug("%s : Finished", THISCLASS);
			} else {
				Log.customer.debug("%s : Failed", THISCLASS);
			}
		
		} catch (Exception e) {
			Log.customer.debug("%s error : " + SystemUtil.stackTrace(e),THISCLASS);
		}
	}
	/*
	 * Method Name : checkExternalApproveRequest 
	 * Input Parameters: None
	 * Return Type: None
	 * 
	 * This method will be getting triggered when the scheduler runs and search for the latest file 
	 */
	private boolean checkExternalApproveRequest()
			throws SQLException {
		String qryStringrc = "SELECT UniqueName FROM ariba.invoicing.core.InvoiceReconciliation where ProjectID = 'F'";
		Log.customer.debug("%s qryString "+ qryStringrc);
		Log.customer.debug("%s Partiotion :  " + Base.getSession().getPartition());
		AQLOptions queryOptionsrc = new AQLOptions(Base.getSession().getPartition());
		AQLResultCollection queryResultsrc = Base.getService().executeQuery(qryStringrc,queryOptionsrc);
		Log.customer.debug("%s after result n bfor while qryString "+ qryStringrc);
		String filePath = ResourceService.getString("cat.ws.util","VertexFilePath");
		while (queryResultsrc.next()) {
			String uniqueName = queryResultsrc.getString("UniqueName");
			Log.customer.debug("%s :returnMessage: %s ", THISCLASS, uniqueName);
			if (uniqueName != null) {
				 receipt = getReceiptFromUniqueName(uniqueName);
				 ProcureLineItemCollection plic = (ProcureLineItemCollection) receipt;
				Log.customer.debug("%s :  receipt : " + receipt, THISCLASS);
				if (receipt != null) {
					// Make Vertex Call....
					File folder = new File(filePath);
					if (folder.isDirectory()) {
						String name = "VertexIRRequest_" + uniqueName;
						FilenameFilter select = new FileListFilter(name, "xml");
						File[] contents = folder.listFiles(select);						
						if (contents != null) {
							Log.customer.debug("\nThe " + contents.length + " matching items in the directory, " + folder.getName() + ", are:");
							File file = null;
							for (int i = 0; i < contents.length; i++) {
								file = contents[i];
								Log.customer.debug(file + " is a " + (file.lastModified()));
							}
						} else {
							Log.customer.debug(folder.getName()	+ " is not a directory");
						}
						File lastModifiedFile = contents[0];
						for (int i = 1; i < contents.length; i++) {
							if (lastModifiedFile.lastModified() < contents[i].lastModified()) {
								lastModifiedFile = contents[i];
								Log.customer.debug(lastModifiedFile + " is a "	+ (lastModifiedFile.lastModified()));
							}

						}
						 System.out.println("outside loop : " + lastModifiedFile);
						 Comment comment = new Comment(receipt.getPartition());
						 comment.setType(Comment.TypeGeneral);
						 comment.setTitle("Failuire Reason");
						 comment.setDate(new Date());
						 comment.setUser(erpreceiptuser);
						 comment.setExternalComment(false);
						 comment.setParent(receipt);
						try {
							VertexTaxIRWSCall wscall = new VertexTaxIRWSCall();
							Log.customer.debug("%s Before calling getVertexTaxIRWSCall()...: %s","");
							String respFile = wscall.getVertexTaxResponse(lastModifiedFile, uniqueName);
							String comments = " - approve";
							Log.customer.debug("%s :comment 1:%s", THISCLASS,comments);
							Comment commentObj = new Comment(receipt.getPartition());
							commentObj.setDate(Date.getNow());
							commentObj.setUser(erpreceiptuser);
							commentObj.setBody(comments);
							commentObj.setType(Comment.TypeApprove);
							receipt.setFieldValue("ProjectID","T");
					//		parseXMLResult(respFile,uniqueName);
							if(respFile != null){
								Object[] isTaxManagerRequired = parseXMLResult(respFile ,uniqueName,receipt);
								InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)receipt;
								if(isTaxManagerRequired != null)
								{
									String approvalRequiredFlag = isTaxManagerRequired[0].toString();
									String approvalReason = isTaxManagerRequired[1].toString();
									if(approvalRequiredFlag.equals("true"))
									{
									   User user = User.getAribaSystemUser(Base.getSession().getPartition());
									   String TaxRole = "Tax Manager";
									   String TaxReason= "Tax Reason";
									   boolean flag1 = true;
									   Object obj = Role.getRole(TaxRole);
									   invoicereconciliation.setFieldValue("TaxOverrideFlag","true");
			          				   ApprovalRequest approvalrequest1 = ApprovalRequest.create(invoicereconciliation, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
									   Log.customer.debug("%s ::: approvalrequest1 got activated- " );
									   
					 				   BaseVector basevector1 = invoicereconciliation.getApprovalRequests();
									   Log.customer.debug("%s ::: basevector1 got activated- " );

			               			   BaseVector basevector2 = approvalrequest1.getDependencies();
									   Log.customer.debug("%s ::: basevector2 got activated- " );

			           				   /*basevector2.add(0, ar);
									   Log.customer.debug("%s ::: ar added to basevector2 " );

			           				   approvalrequest1.setFieldValue("Dependencies", basevector2);
			           				   ar.setState(2);
									   Log.customer.debug("%s ::: ar.setState- " );

			           					ar.updateLastModified();
									    Log.customer.debug("%s ::: ar.updatelastmodified- " );

			            				basevector1.removeAll(ar);
									    Log.customer.debug("%s ::: basevecotr1 .removeall " );
*/
			           				   
			           				  for (int i = 0; i < basevector1.size();i++){
			     				    	 Log.customer.debug("%s ::: inside loop - " );
			     				    	 ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
			     				    	 basevector2.add(0, ar1);
			     				    	 Log.customer.debug("%s ::: ar added to basevector2 " );
			     				    	 ar1.setState(2); 
			     				    	 Log.customer.debug("%s ::: ar.setState- " );
			     				    	 ar1.updateLastModified();
			     				    	 Log.customer.debug("%s ::: ar.updatelastmodified- " );
			     				    	 basevector1.removeAll(ar1);
			     				    	 Log.customer.debug("%s ::: basevecotr1 .removeall " );
			     				    }
			            				basevector1.add(0, approvalrequest1);
									    Log.customer.debug("%s ::: basevector1 .add- " );

			            			    invoicereconciliation.setApprovalRequests(basevector1);
									    Log.customer.debug("%s ::: ir .setApprovalRequests got activated- " );

			           					java.util.List list = ListUtil.list();
			           					java.util.Map map = MapUtil.map();
			             				boolean flag6 = approvalrequest1.activate(list, map);
			             				
									    Log.customer.debug("%s ::: New TaxAR Activated - " );
									    Log.customer.debug("%s ::: State (AFTER): " );
									    Log.customer.debug("%s ::: Approved By: %s" );
									    }
									    else
									    invoicereconciliation.setFieldValue("TaxOverrideFlag","false");
									}
							}else{
								String failString = "There is no Response from vertex Please contact Administrator" ;
								String failReason = "Reason For Failure: ";
								LongString longstring = new LongString(failString);
								comment.setText(longstring);
						//		addCommentToPR(ar,longstring,failReason,date,aribasys);
								Log.customer.debug("inside else");
								return false;
							}
							receipt.approve(erpreceiptuser, erpreceiptuser,	commentObj);
							Log.customer.debug("%s :approved %s", THISCLASS,uniqueName);
    						receipt.save();
							Log.customer.debug("%s : receipt status saved",THISCLASS);						
						}
						/***************************************************************************************/
						catch (AxisFault f) {
							// Determine type of fault (Client or Server)
							Log.customer.debug(" In Catch ......");
							String faultCode = f.getFaultCode().getLocalPart();
							String faultDetails = f.getFaultString();
							String faultString = f.getFaultString();
							String failReason = "Reason For Failure: ";
							Log.customer.debug("%s In Catch ...... faultString:- "	+ faultString);
							Log.customer.debug("%s In Catch ...... faultCode:- " + faultCode);							
							
							 
							if (faultCode.equalsIgnoreCase("server")) {
								// This indicates that the Web Service is not in a valid state Processing should stop until the
								// problem is resolved. A "Server" fault is generated when a VertexSystemException is thrown on the Server.								
								Log.customer.debug("web-service is not in valid state. ****************  Server");
								faultString = faultString.concat("\n web-service is not in valid state.");
								Log.customer.debug("%s fault String :- "+ faultString);
								Log.customer.debug(faultDetails.toString());
								Log.customer.debug("%s Reason :- "	+ f.getFaultReason());
								Log.customer.debug("%s message :- "	+ f.getMessage());
							
							} else if (faultCode.equalsIgnoreCase("client")) {
								// A "Client" fault would indicate that the request is flawed but processing of additional requests
								// could continue. A "Client" fault is generated when a VertexApplicationException is thrown on the Server.
								Log.customer.debug("The XML request is invalid. Fix the request and resend.******** client");
								faultString = faultString.concat("\n The XML request is invalid. Fix the request and resend.");
								Log.customer.debug("%s fault String :- "+ faultString);
								Log.customer.debug(faultDetails.toString());
								Log.customer.debug("%s Reason :- " 	+ f.getFaultReason());
								Log.customer.debug("%s message :- "	+ f.getMessage());
							if(faultString.contains("User login failed"))						 
									  addMSCAdminToIR(receipt);
									  else					
									// Addition of tax manager................
									addTaxManagerToIR(receipt);	
							 }
							Log.customer.debug("%s ****  Completed Comments : - ");
							Log.customer.debug("%s ****  Completed Axis Fault : - ");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Log.customer.debug("In IO Exception");
							Log.customer.debug("in Exception" + e.getMessage());
							e.printStackTrace();
						} catch (NullPointerException e) {
							Log.customer.debug("In Null Exception");
							Log.customer.debug("in Exception" + e.getMessage());
						} /*catch (TimeoutException a) {
							Log.customer.debug("Inside TimeoutException . Fix the request and resend.");
							Log.customer.debug(a.getCause());
							Log.customer.debug(a.getMessage());
						}*/ catch (Exception a) {
							Log.customer.debug("Inside Exception . Fix the request and resend.");
							Log.customer.debug(a.getCause());
							Log.customer.debug(a.getMessage());
						}
					}
				 }
				// .... End vertex Call...
			}
		}

		Log.customer.debug("%s : checkExternalApproveRequest finished",
				THISCLASS);
		return true;
	}
	/*
	Method Name : addMSCAdminToIR
	Input Parameters: Approvable lic
	Return Type: None
	
	This method adds the MSC Admin when there is vertex Login issue(Purely for login issues-incorrect user id/pwd

	*/


	public static void addMSCAdminToIR(Approvable lic){
		Log.customer.debug("%s ::: addMSCAdminToIR - " + lic);
		
		String TaxRole = "MSC Administrator";
		String TaxReason= "Tax Reason";
		boolean flag1 = true;
		Object obj = Role.getRole(TaxRole);

		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
		Log.customer.debug("%s ::: approvalrequest1 got activated- " );
		BaseVector basevector1 = plic.getApprovalRequests();
		Log.customer.debug("%s ::: basevector1 got activated- " );

		BaseVector basevector2 = approvalrequest1.getDependencies();
		Log.customer.debug("%s ::: basevector2 got activated- " );
		
		Log.customer.debug("%s ::: Before loop - " );
	    // trying to get AR object 
	    for (int i = 0; i < basevector1.size();i++){
	    	
	    	 Log.customer.debug("%s ::: inside loop 2 - " );
	    	 ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
	    	 basevector2.add(0, ar1);
	    	 Log.customer.debug("%s ::: ar added to basevector2 " );
	    	 ar1.setState(2); 
	    	 Log.customer.debug("%s ::: ar.setState- " );
	    	 ar1.updateLastModified();
	    	 Log.customer.debug("%s ::: ar.updatelastmodified- " );
	    	 basevector1.removeAll(ar1);
	    	 Log.customer.debug("%s ::: basevecotr1 .removeall " );
	    }
	    Log.customer.debug("%s ::: after loop - " );	    

		/*basevector2.add(0, ar);
		Log.customer.debug("%s ::: ar added to basevector2 " );

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		ar.setState(2);
		Log.customer.debug("%s ::: ar.setState- " );

		ar.updateLastModified();
		Log.customer.debug("%s ::: ar.updatelastmodified- " );

		basevector1.removeAll(ar);
		Log.customer.debug("%s ::: basevecotr1 .removeall " );
*/
		basevector1.add(0, approvalrequest1);
		Log.customer.debug("%s ::: basevector1 .add- " );

		plic.setApprovalRequests(basevector1);
		Log.customer.debug("%s ::: ir .setApprovalRequests got activated- " );

		java.util.List list = ListUtil.list();
		java.util.Map map = MapUtil.map();
		boolean flag6 = approvalrequest1.activate(list, map);

		Log.customer.debug("%s ::: New MSCAdmin Activated - " );
		Log.customer.debug("%s ::: State (AFTER): " );
		Log.customer.debug("%s ::: Approved By: %s" );
	}

	/*
	Method Name : addTaxManagerToIR
	Input Parameters:Approvable lic
	Return Type: None
	
	This method adds the Tax Manager when tolerance exceeds or when there is a data error

	*/


	public static void addTaxManagerToIR(Approvable lic){
	//	Log.customer.debug("%s ::: addTaxManagerToIR - " + lic);
		String role = "TM";
		String TaxReason= "Tax Reason";
		boolean flag1 = true;							
		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		Object obj = CatCommonUtil.getRoleforSplitterRuleForVertex(lic,role);
		ProcureLineItem pli = null;
		BaseVector lineItems = null;
		if(plic != null){
			lineItems = plic.getLineItems();
		}			
		int count = lineItems.size();

		Log.customer.debug("%s ::: isTaxManagerRequired - plic bfore create " + plic.toString());
		ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
		Log.customer.debug("%s ::: approvalrequest1 got activated- " );
		BaseVector basevector1 = plic.getApprovalRequests();
		Log.customer.debug("%s ::: basevector1 got activated- " );

		BaseVector basevector2 = approvalrequest1.getDependencies();
		Log.customer.debug("%s ::: basevector2 got activated- " );
		
		Log.customer.debug("%s ::: Before loop - " );
	    // trying to get AR object 
	    for (int i = 0; i < basevector1.size();i++){
	    	
	    	 Log.customer.debug("%s ::: inside loop 2 - " );
	    	 ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
	    	 basevector2.add(0, ar1);
	    	 Log.customer.debug("%s ::: ar added to basevector2 " );
	    	 ar1.setState(2); 
	    	 Log.customer.debug("%s ::: ar.setState- " );
	    	 ar1.updateLastModified();
	    	 Log.customer.debug("%s ::: ar.updatelastmodified- " );
	    	 basevector1.removeAll(ar1);
	    	 Log.customer.debug("%s ::: basevecotr1 .removeall " );
	    }
	    Log.customer.debug("%s ::: after loop - " );	    

		/*
		basevector2.add(0, ar);
		Log.customer.debug("%s ::: ar added to basevector2 " );

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		ar.setState(2);
		Log.customer.debug("%s ::: ar.setState- " );

		ar.updateLastModified();
		Log.customer.debug("%s ::: ar.updatelastmodified- " );

		basevector1.removeAll(ar);
		Log.customer.debug("%s ::: basevecotr1 .removeall " );
*/
		basevector1.add(0, approvalrequest1);
		Log.customer.debug("%s ::: basevector1 .add- " );

		plic.setApprovalRequests(basevector1);
		Log.customer.debug("%s ::: ir .setApprovalRequests got activated- " );

		java.util.List list = ListUtil.list();
		java.util.Map map = MapUtil.map();
		boolean flag6 = approvalrequest1.activate(list, map);

		Log.customer.debug("%s ::: New TaxAR Activated - " );
		Log.customer.debug("%s ::: State (AFTER): " );
		Log.customer.debug("%s ::: Approved By: %s" );
	}
	/*
	 * Method Name : parseXMLResult 
	 * Input Parameters: String respFile,String uniqueName
	 * Return Type: None
	 * 
	 * This method Parses the Response file generated by vertex webservice.
	 */
	public Object[] parseXMLResult(String responseString,String uniqueName,Approvable receipt) {

		Object[] isTaxManagerRequired =  new Object[2];
		Partition partition = Base.getSession().getPartition();
			
		try {

			receipt = getReceiptFromUniqueName(uniqueName);
			ProcureLineItemCollection plic = (ProcureLineItemCollection) receipt;
			BaseVector lineItems = plic.getLineItems();
			int count = lineItems.size();
			ProcureLineItem pli = null;
			
			File responseFile = new File(responseString);
			
			//Partition partition = ar.getPartition();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(responseFile);
			doc.getDocumentElement().normalize();
			String buyerAmount="";
			String totalAmt = "";
			String totalTax = "";
			String taxCode = "";
			//Log.customer.debug("Amount from MSC is ***"+strAmount);
			NodeList strDivision = doc.getElementsByTagName("Division");
			Log.customer.debug("Information of Division" + strDivision);
			Log.customer.debug("Root element "+ doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("LineItem");
			Log.customer.debug("Information of all Line Item");
			for (int s = 0; s < nodeList.getLength(); s++) 
				{
					Node fstNode = nodeList.item(s);
					Element fstElmntlnm = (Element) fstNode;
					String lineItemNumber = fstElmntlnm.getAttribute("lineItemNumber");
					Log.customer.debug(":: Line Item Number :: " + lineItemNumber); 		
					pli = (ProcureLineItem) lineItems.get(s);
					if(pli.getDottedFieldValue("Amount.Amount").toString()!=null)
					buyerAmount= ((BigDecimal)pli.getDottedFieldValue("Amount.Amount")).toString();
					Log.customer.debug("buyerAmount::"+buyerAmount);
					Log.customer.debug("Line item Number ::"+s);
					Log.customer.debug("Node list ::"+nodeList.getLength());

					if (fstNode.getNodeType() == Node.ELEMENT_NODE)
					{
						Element fstElmnt = (Element) fstNode;
						NodeList countryElmntLst = fstElmnt.getElementsByTagName("Country");
						Element lstNmElmnt = (Element) countryElmntLst.item(0);
						NodeList lstNm = lstNmElmnt.getChildNodes();
						Log.customer.debug("::  Country : "	+ ((Node) lstNm.item(0)).getNodeValue());

					
					/*****************************************************************************/
						/** Fetching Description from Table.*/
						/*String taxCodeForLookup = "429"; // Please Replace with the Actual value from Web Service Response.
						String qryString = "Select TaxExemptDescription from cat.core.TaxExemptReasonCode where TaxExemptUniqueName  = '"+ taxCodeForLookup + "'";
						Log.customer.debug(" :: TaxExemptReasonCode : qryString ::"+qryString);
						// Replace the cntrctrequest - Invoice Reconciliation Object
						AQLOptions queryOptions = new AQLOptions(partition);
						AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
						if(queryResults !=null)
						{
							Log.customer.debug(" :: TaxExemptReasonCode: Query Results not null ::");
							while(queryResults.next()) 
							{
								String taxdescfromLookupvalue = (String)queryResults.getString(0);
								Log.customer.debug(" :: TaxExemptReasonCode: taxdescfromLookupvalue ::"+taxdescfromLookupvalue);
								// Change the rli to appropriate Carrier Holding object, i.e. IR Line object
								pli.setFieldValue("Carrier", taxdescfromLookupvalue);
								Log.customer.debug(" :: TaxExemptReasonCode Applied on Carrier::  " + taxdescfromLookupvalue);
							}
						}*/
						
						
						//*****************************************************************************//*


				NodeList totaltaxElmntLst = fstElmnt.getElementsByTagName("TotalTax");
						Element lstNmElmnt1 = (Element) totaltaxElmntLst.item(0);
						NodeList lstNm1 = lstNmElmnt1.getChildNodes();
						Log.customer.debug(":: TotalTax after population :: "+ ((Node) lstNm1.item(0)).getNodeValue());
						totalTax = ((Node) lstNm1.item(0)).getNodeValue();
						BigDecimal	taxAmount= new BigDecimal(totalTax);
						Money taxTotal = new Money(taxAmount, pli.getAmount().getCurrency());
						pli.setFieldValue("TaxAmount", taxTotal);						
						Log.customer.debug(":: Tax Amount that is being set :: " + totalTax);			
						Log.customer.debug(":: New Tax Amount that is being got from lineitem :: " + pli.getFieldValue("TaxAmount"));	

						// Tax code
						NodeList lstNmElmntLst2 = fstElmnt.getElementsByTagName("FlexibleCodeField");
						for (int a = 0; a < lstNmElmntLst2.getLength(); a++) 
							{
							Node fstNode1 = lstNmElmntLst2.item(a);
							if (fstNode1.getNodeType() == Node.ELEMENT_NODE)
								{
									Element fstElmnt1 = (Element) fstNode1;
									String fieldId2 = fstElmnt1.getAttribute("fieldId");
									Log.customer.debug(":: fieldId2 :: " + fieldId2);
									if ("2".equalsIgnoreCase(fieldId2))
										{
											Log.customer.debug(":: in IF  :: " + fieldId2);
											Element lstNmElmnt21 = (Element) lstNmElmntLst2.item(a);
											NodeList lstNm21 = lstNmElmnt21.getChildNodes();
											taxCode = ((Node) lstNm21.item(0)).getNodeValue();
											Log.customer.debug(":: Tax code :: " + taxCode);
										}
								}
								
							}
					NodeList vertexTaxAmountLst = fstElmnt.getElementsByTagName("ExtendedPrice");
					Element VertexTaxAmtElement = (Element) vertexTaxAmountLst.item(0);
					NodeList lstVerTaxAmt = VertexTaxAmtElement .getChildNodes();
					totalAmt = ((Node) lstVerTaxAmt .item(0)).getNodeValue();
					Log.customer.debug(":: Total Amount from Vertex Response is :: "+totalAmt);
	
				}
			 	//Change tax code to CC_B2 based on invoice amount(if=0),vertex amount(if>0) and tax code(if=B2)

					Log.customer.debug(":: :: Vertex Tax Code Evaluation: amount   :: :: "+totalAmt);
					Log.customer.debug(":: :: Vertex Tax Code Evaluation: totalTax :: ::"+totalTax );
					Log.customer.debug(":: :: Vertex Tax Code Evaluation: taxcode  :: ::"+taxCode );
					Log.customer.debug(":: :: Buyer Amount Evaluation: amount   :: :: "+buyerAmount);
					//Test the condition
					//buyerAmount ="0";
					//taxCode = "B1";
					if (buyerAmount=="0" && totalAmt!="0" && taxCode=="B1")
							{
								Log.customer.debug(" Change Tax Code to CC_B2 as Invoice Amount=0,Vertex Amount>0 and Tax Code=B1");
								String IRTaxCode = "B2";
								Log.customer.debug(" SAPCatTaxCustomApproverInv : TaxCode revised for IR Line Item "+IRTaxCode);
								Log.customer.debug(" SAPCatTaxCustomApproverInv : tax code that is now going to be revised ");
								Log.customer.debug("CC_B2 is not set");
  							    String qryString = "Select TaxCode,UniqueName, SAPTaxCode from ariba.tax.core.TaxCode where UniqueName  = '"+strDivision+"_B2'"; 
								Log.customer.debug(":: :: qryString    :: :: "+qryString );
								
								AQLOptions queryOptions = new AQLOptions(partition);
								AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
								if(queryResults !=null)
									{
										Log.customer.debug(" :: ::  Query Results not null :: ::");
										while(queryResults.next()) 
										{

										TaxCode taxCodeObject = (TaxCode)queryResults.getBaseId(0).get();
										Log.customer.debug(" :: ::  taxCodeObject  :: ::"+taxCodeObject );

										pli.setFieldValue("TaxCode", taxCodeObject);
								
										}
								  }

							}
			isTaxManagerRequired = new Object[2];
			BigDecimal HUNDRED = new BigDecimal("100");
			BigDecimal percentageDifference  = new BigDecimal("0");

			BigDecimal ONE = new BigDecimal("1");
			String taxToleranceExceededException ="Tax Manager to verify the tax variation > 1% tolerance as received from Vertex during the current tax invoice calculation for line item : "+lineItemNumber;			
			LongString longstring = new LongString(taxToleranceExceededException);


			BigDecimal amountFromVertex = new BigDecimal(totalAmt);
		//	amountFromVertex = new BigDecimal(2999);
			BigDecimal amountFromMSC = new BigDecimal(buyerAmount);
		//	amountFromMSC = new BigDecimal(2999.03);

			BigDecimal amountDifference = amountFromVertex.subtract(amountFromMSC);
			Log.customer.debug(":: :: amountDifference :: :: "+amountDifference );
			if(amountDifference.compareTo(BigDecimal.ZERO)==0 ) 
			{
			percentageDifference =new BigDecimal("0");	
			isTaxManagerRequired[0]=  "false"; 
			isTaxManagerRequired[1] = longstring ;
			
			
			}else
			
			{
				if (percentageDifference.compareTo(ONE.divide(HUNDRED)) > 0 || percentageDifference.compareTo(ONE.divide(HUNDRED)) < 0) {

					percentageDifference  = amountDifference.divide(HUNDRED);
					//percentageDifference = (percentageDifference<0)?(-1 * percentageDifference):percentageDifference;
					Log.customer.debug(":: :: percentageDifference :: :: "+percentageDifference);

					isTaxManagerRequired[0]=  "true"; 
					isTaxManagerRequired[1] = longstring ;

					String taxReason = "Reason For Adding Tax Manager";
					 Date date = new Date();
					 User user1 = erpreceiptuser;
					 addCommentToIR(longstring, taxReason, user1,receipt);
				}
				else
				{
					isTaxManagerRequired[0]=  "false"; 
					isTaxManagerRequired[1] = longstring ;
				}
			}
				// invoicereconciliation.setFieldValue("AssessTaxMessage", s5);
			 }
			//return isTaxManagerRequired;	
		}
		
 		catch (IOException e) {
		// TODO Auto-generated catch block
		Log.customer.debug("In IO Exception");
		Log.customer.debug("in Exception" + e.getMessage());
		e.printStackTrace();
		}catch(SAXException e){
		Log.customer.debug("In SAX Exception");
		Log.customer.debug("in Exception" + e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
		}		
		return isTaxManagerRequired;	
	} 
	/*
	 * Method Name : getReceiptFromUniqueName 
	 * Input Parameters: String uniqueName 
	 * Return Type: Approvable
	 * 
	 * This method retrive the details of the Approvable for the provided UniqueName.
	 */
	public Approvable getReceiptFromUniqueName(String uniqueName) {
		String queryString = "SELECT Approvable FROM ariba.approvable.core.Approvable AS Approvable "
				+ "WHERE Approvable.UniqueName = '" + uniqueName + "'";

		Log.customer.debug("%s : queryString : %s", THISCLASS, queryString);
		AQLQuery query = AQLQuery.parseQuery(queryString);
		AQLOptions options = new AQLOptions(Partition.Any);
		AQLResultCollection results = Base.getService().executeQuery(query,
				options);

		if (results.getSize() != 1) {
			Log.customer.debug("%s : No Approvable found for UniqueName : %s",
					THISCLASS, uniqueName);
			return null;
		} else {
			results.next();
			BaseId id = results.getBaseId(0);

			Log.customer.debug("%s :  BaseId : " + id, THISCLASS);
			return (Approvable) Base.getSession().objectFromId(id);
		}
	}
	private User getERPReceptApproverUser() {
		return User.getUser("erpreceiptapprover", Base.getService()
				.getParameter(Base.getSession().getPartition(),
						"Application.Authentication.PasswordAdapter"));

	}
	/*
	 * Method Name : addCommentToIR 
	 * Input Parameters: ApprovalRequest ar, LongString commentText, String commentTitle, Date commentDate, User commentUser 
	 * Return Type: None
	 * 
	 * This method adds the comments to the IR - if there is any issue with
	 * generating the response or request file.
	 */
	public static void addCommentToIR(LongString commentText, String commentTitle, User commentUser,Approvable receipt){
		Comment comment = new Comment(Base.getSession().getPartition());
		comment.setType(Comment.TypeGeneral);
		comment.setText(commentText);
		comment.setTitle(commentTitle);
		//comment.setDate(commentDate);
		comment.setUser(commentUser);
		comment.setExternalComment(true);
		
		comment.setParent(receipt);
		receipt.getComments().add(comment);

	}
}
class FileListFilter implements FilenameFilter {
	private String name;
	private String extension;

	public FileListFilter(String name, String extension) {
		this.name = name;
		this.extension = extension;
	}

	public boolean accept(File directory, String filename) {
		boolean fileOK = true;

		if (name != null) {
			fileOK &= filename.startsWith(name);
		}

		if (extension != null) {
			fileOK &= filename.endsWith('.' + extension);
		}
		return fileOK;
	}
}
