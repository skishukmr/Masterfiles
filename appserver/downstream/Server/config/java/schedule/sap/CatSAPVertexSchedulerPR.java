/*
	Author: Archana
   	This scheduler runs every two hours and gethers the information about all PR's which has 
	    Vertex Manager and makes call to webservice..
		
	Change History
	#	Change By	Change Date		Description
	=============================================================================================

 */
package config.java.schedule.sap;

import ariba.approvable.core.Approvable;
import config.java.common.CatCommonUtil;
import ariba.purchasing.core.ReqLineItem;
import ariba.approvable.core.ApprovalRequest;
import ariba.util.core.ResourceService;
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

public class CatSAPVertexSchedulerPR extends ScheduledTask {

	private static final String THISCLASS = "CatSAPVertexSchedulerPR";
	private String appPart = "";
	private int partition;
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
			// partition = Base.getSession().getPartition().intValue();
			if (checkExternalApproveRequest()) {
				Log.customer.debug("%s : Finished", THISCLASS);
			} else {
				Log.customer.debug("%s : Failed", THISCLASS);
			}

		} catch (Exception e) {
			Log.customer.debug("%s error : " + SystemUtil.stackTrace(e),
					THISCLASS);
		}
	}
	/*
	 * Method Name : checkExternalApproveRequest 
	 * Input Parameters: None
	 * Return Type: None
	 * 
	 * This method will be getting triggered when the scheduler runs and search for the latest file 
	 */
	private boolean checkExternalApproveRequest() throws SQLException {
		String qryStringrc = "SELECT UniqueName FROM ariba.purchasing.core.Requisition where ProjectID = 'F'";
		Log.customer.debug("%s qryString " + qryStringrc);
		Log.customer.debug("%s Partiotion :  "
				+ Base.getSession().getPartition());
		AQLOptions queryOptionsrc = new AQLOptions(Base.getSession()
				.getPartition());
		AQLResultCollection queryResultsrc = Base.getService().executeQuery(
				qryStringrc, queryOptionsrc);
		Log.customer.debug("%s after result n bfor while qryString "
				+ qryStringrc);
		String filePath = ResourceService.getString("cat.ws.util",
				"VertexFilePath");
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
						String name = "VertexRequest_" + uniqueName;
						FilenameFilter select = new FileListFilter(name, "xml");
						File[] contents = folder.listFiles(select);
						if (contents != null) {
							Log.customer.debug("\nThe " + contents.length
									+ " matching items in the directory, "
									+ folder.getName() + ", are:");
							File file = null;
							for (int i = 0; i < contents.length; i++) {
								file = contents[i];
								Log.customer.debug(file + " is a "
										+ (file.lastModified()));
							}
						} else {
							Log.customer.debug(folder.getName()
									+ " is not a directory");
						}
						File lastModifiedFile = contents[0];
						for (int i = 1; i < contents.length; i++) {
							if (lastModifiedFile.lastModified() < contents[i]
									.lastModified()) {
								lastModifiedFile = contents[i];
								Log.customer.debug(lastModifiedFile + " is a "
										+ (lastModifiedFile.lastModified()));
							}

						}
						System.out
								.println("outside loop : " + lastModifiedFile);
						Comment comment = new Comment(receipt.getPartition());
						comment.setType(Comment.TypeGeneral);
						comment.setTitle("Failuire Reason");
						comment.setDate(new Date());
						comment.setUser(erpreceiptuser);
						comment.setExternalComment(false);
						comment.setParent(receipt);
						try {
							VertexTaxWSCall wscall = new VertexTaxWSCall();
							Log.customer
									.debug(
											"%s Before calling getVertexTaxResponse()...: %s",
											"");
							String respFile = wscall.getVertexTaxResponse(
									lastModifiedFile, uniqueName);
							String comments = " - approve";
							Log.customer.debug("%s :comment 1:%s", THISCLASS,
									comments);
							Comment commentObj = new Comment(receipt
									.getPartition());
							commentObj.setDate(Date.getNow());
							commentObj.setUser(erpreceiptuser);
							commentObj.setBody(comments);
							commentObj.setType(Comment.TypeApprove);
							receipt.setFieldValue("ProjectID", "T");
							// parseXMLResult(respFile,uniqueName);
							if (respFile != null) {
								parseXMLResult(respFile, uniqueName);
							} else {
								String failString = "There is no Response from vertex Please contact Administrator";
								String failReason = "Reason For Failure: ";
								LongString longstring = new LongString(
										failString);
								comment.setText(longstring);
								// addCommentToPR(ar,longstring,failReason,date,aribasys);
								Log.customer.debug("inside else");
								return false;
							}
							receipt.approve(erpreceiptuser, erpreceiptuser,
									commentObj);
							Log.customer.debug("%s :approved %s", THISCLASS,
									uniqueName);
							receipt.save();
							Log.customer.debug("%s : receipt status saved",
									THISCLASS);

						}
						/***************************************************************************************/
						catch (AxisFault f) {
							// Determine type of fault (Client or Server)
							Log.customer.debug(" In Catch ......");
							String faultCode = f.getFaultCode().getLocalPart();
							String faultDetails = f.getFaultString();
							String faultString = f.getFaultString();
							String failReason = "Reason For Failure: ";
							Log.customer
									.debug("%s In Catch ...... faultString:- "
											+ faultString);
							Log.customer
									.debug("%s In Catch ...... faultCode:- "
											+ faultCode);

							if (faultCode.equalsIgnoreCase("server")) {
								// This indicates that the Web Service is not in
								// a valid state Processing should stop until
								// the
								// problem is resolved. A "Server" fault is
								// generated when a VertexSystemException is
								// thrown on the Server.
								Log.customer
										.debug("web-service is not in valid state. ****************  Server");
								faultString = faultString
										.concat("\n web-service is not in valid state.");
								Log.customer.debug("%s fault String :- "
										+ faultString);
								Log.customer.debug(faultDetails.toString());
								LongString commnetText = new LongString(
										faultString);
								comment.setText(commnetText);
								Log.customer.debug("%s Reason :- "
										+ f.getFaultReason());
								Log.customer.debug("%s message :- "
										+ f.getMessage());
								// receipt.setFieldValue("ProjectID","F");
								// addVertexManagerToPR(receipt);
							} else if (faultCode.equalsIgnoreCase("client")) {
								// A "Client" fault would indicate that the
								// request is flawed but processing of
								// additional requests
								// could continue. A "Client" fault is generated
								// when a VertexApplicationException is thrown
								// on the Server.
								Log.customer
										.debug("The XML request is invalid. Fix the request and resend.******** client");
								faultString = faultString
										.concat("\n The XML request is invalid. Fix the request and resend.");
								Log.customer.debug("%s fault String :- "
										+ faultString);
								LongString commnetText = new LongString(
										faultString);
								comment.setText(commnetText);
								Log.customer.debug(faultDetails.toString());
								Log.customer.debug("%s Reason :- "
										+ f.getFaultReason());
								Log.customer.debug("%s message :- "
										+ f.getMessage());
								receipt.setFieldValue("ProjectID", "T");
								// addTaxManagerToPR(receipt);
								if (faultString.contains("User login failed:")) {
									addMSCAdminToPR(receipt);
								} else {

									// ReqLineItem rli = (ReqLineItem)receipt;
									addIPToPR(receipt);
								}
							}
							plic.getComments().add(comment);
							Log.customer
									.debug("%s ****  Completed Comments : - ");
							Log.customer
									.debug("%s ****  Completed Axis Fault : - ");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Log.customer.debug("In IO Exception");
							Log.customer.debug("in Exception" + e.getMessage());
							e.printStackTrace();
						} catch (NullPointerException e) {
							Log.customer.debug("In Null Exception");
							Log.customer.debug("in Exception" + e.getMessage());
						} catch (TimeoutException a) {
							Log.customer
									.debug("Inside TimeoutException . Fix the request and resend.");
							Log.customer.debug(a.getCause());
							Log.customer.debug(a.getMessage());
						} catch (Exception a) {
							Log.customer
									.debug("Inside Exception . Fix the request and resend.");
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
	 * Method Name : parseXMLResult 
	 * Input Parameters: String respFile,String uniqueName
	 * Return Type: None
	 * 
	 * This method Parses the Response file generated by vertex webservice.
	 */
	private void parseXMLResult(String respFile, String uniqueName)
			throws SAXException, ParserConfigurationException, IOException {
		Log.customer.debug("After calling getVertexTaxResponse()...: %s",
				"CatTaxCustomApprover response file before parsing : -  "
						+ respFile);
		// Parsing XML and populating field in Ariba.....
		Approvable lic = getReceiptFromUniqueName(uniqueName);
		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		BaseVector lineItems = plic.getLineItems();
		int count = lineItems.size();
		ProcureLineItem pli = null;
		Log.customer.debug("%s **** Multiple Line Items ", THISCLASS, count);
		Log.customer.debug(" Parsing XML file ...........: %s");
		File file1 = new File(respFile);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file1);
		if (respFile != null) {
			doc.getDocumentElement().normalize();
			Log.customer.debug("%s Root element "
					+ doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("LineItem");
			Log.customer.debug("%s Information of all Line Item nodeList "
					+ nodeList.getLength());

			for (int s = 0; s < nodeList.getLength(); s++) {
				Log.customer.debug("s : in parse method : - " + s
						+ " nodeList " + nodeList.getLength());
				Node fstNode = nodeList.item(s);
				Log.customer.debug("*** lineItemNumber before : ");
				Element fstElmntlnm = (Element) fstNode;
				String lineItemNumber = fstElmntlnm
						.getAttribute("lineItemNumber");
				Log.customer
						.debug("%s *** lineItemNumber outside loop  after: "
								+ lineItemNumber);
				int index = Integer.parseInt(lineItemNumber);
				Log.customer
						.debug("%s *** lineItemNumber outside loop  after: "
								+ index);
				try {
					int plinumber = index - 1;
					Log.customer
							.debug("%s *** lineItemNumber plinumber  after: "
									+ plinumber);
					pli = (ProcureLineItem) lineItems.get(plinumber);
				} catch (Exception e) {
					Log.customer.debug("%s *** in catch of pli : "
							+ lineItemNumber + " ******** " + e.toString());
					Log.customer.debug(pli.toString());
					Log.customer.debug(e.getClass());
				}
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {

					Element fstElmnt = (Element) fstNode;
					NodeList countryElmntLst = fstElmnt
							.getElementsByTagName("Country");
					Element lstNmElmnt = (Element) countryElmntLst.item(0);
					NodeList lstNm = lstNmElmnt.getChildNodes();
					Log.customer.debug("%s *** Country : "
							+ ((Node) lstNm.item(0)).getNodeValue());

					// Total Tax
					NodeList totaltaxElmntLst = fstElmnt
							.getElementsByTagName("TotalTax");
					Element lstNmElmnt1 = (Element) totaltaxElmntLst.item(0);
					NodeList lstNm1 = lstNmElmnt1.getChildNodes();
					Log.customer.debug("%s *** TotalTax after population : "
							+ ((Node) lstNm1.item(0)).getNodeValue());
					String totalTax = ((Node) lstNm1.item(0)).getNodeValue();
					BigDecimal taxAmount = new BigDecimal(totalTax);
					Money taxTotal = new Money(taxAmount, pli.getAmount()
							.getCurrency());
					pli.setFieldValue("TaxAmount", taxTotal);
					Log.customer.debug("%s *** Tax Amount : " + totalTax);

					// Reason code
					Element fstElmntRC = (Element) fstNode;
					NodeList lstNmElmntLstRC = fstElmntRC
							.getElementsByTagName("AssistedParameter");
					String ReasonCode = "";
					Log.customer.debug("%s *** lstNmElmntLstRC.getLength() : "
							+ lstNmElmntLstRC.getLength());
					for (int b = 0; b < lstNmElmntLstRC.getLength(); b++) {
						Node fstNodeRC = lstNmElmntLstRC.item(b);
						if (fstNodeRC.getNodeType() == Node.ELEMENT_NODE) {
							Element fstElmntRC1 = (Element) fstNodeRC;
							String fieldIdRC = fstElmntRC1
									.getAttribute("phase");
							Log.customer.debug("%s *** ReasonCode in loop : "
									+ fieldIdRC);
							if ("POST".equalsIgnoreCase(fieldIdRC)) {
								Log.customer.debug("*** in IF  : " + fieldIdRC);
								try {
									Element lstNmElmntRC = (Element) lstNmElmntLstRC
											.item(0);
									if (lstNmElmntRC.equals(null)
											|| lstNmElmntRC.equals("")) {
										ReasonCode = "";
										Log.customer
												.debug("%s *** ReasonCode in if : "
														+ ReasonCode);
									} else {
										NodeList lstNmRC = lstNmElmntRC
												.getChildNodes();
										ReasonCode = ((Node) lstNmRC.item(0))
												.getNodeValue();
										Log.customer
												.debug("%s *** ReasonCode in else : "
														+ ReasonCode);
									}

								} catch (NullPointerException e) {
									Log.customer
											.debug("%s *** inside exception : ");
									e.printStackTrace();
								}

							} else {
								Log.customer.debug("inside else");
								break;
							}
						}
						Log.customer.debug("inside loop still....");
					}
					Log.customer.debug("outside loop .....");
					// *********************************************************************************

					// TaxAmount = 0 and Reason code = Null then exempt Reason
					// code is E0.
					if (("0.0".equals(totalTax) && ((ReasonCode == null) || (""
							.equals(ReasonCode))))) {
						ReasonCode = "E0";
						Log.customer.debug("*** ReasonCode in condition : %s",
								THISCLASS, ReasonCode);
					} else if (("0.0".equals(totalTax) && ((ReasonCode != null) || (!""
							.equals(ReasonCode))))) {
						/** Fetching Description from Table. */
						Log.customer.debug("*** ReasonCode after : "
								+ ReasonCode);
						String taxCodeForLookup = ReasonCode; // Please Replace
																// with the
																// Actual value
																// from Web
																// Service
																// Response.
						String qryStringrc = "Select TaxExemptDescription from cat.core.TaxExemptReasonCode where TaxExemptUniqueName  = '"
								+ taxCodeForLookup + "'";
						Log.customer
								.debug("%s TaxExemptReasonCode : qryString "
										+ qryStringrc);
						// Replace the cntrctrequest - Invoice Reconciliation
						// Object
						AQLOptions queryOptionsrc = new AQLOptions(Base
								.getSession().getPartition());
						AQLResultCollection queryResultsrc = Base.getService()
								.executeQuery(qryStringrc, queryOptionsrc);
						if (queryResultsrc != null) {
							Log.customer
									.debug(" TaxExemptReasonCode: Query Results not null");
							while (queryResultsrc.next()) {
								String taxdescfromLookupvalue = (String) queryResultsrc
										.getString(0);
								Log.customer
										.debug(" TaxExemptReasonCode: taxdescfromLookupvalue ="
												+ taxdescfromLookupvalue);
								// Change the rli to appropriate Carrier Holding
								// object, i.e. IR Line object
								if ("".equals(taxdescfromLookupvalue)
										|| taxdescfromLookupvalue == null
										|| "null"
												.equals(taxdescfromLookupvalue)) {
									// if(taxdescfromLookupvalue.equals("")||taxdescfromLookupvalue
									// ==
									// null||taxdescfromLookupvalue.equals("null")
									// ){
									taxdescfromLookupvalue = "";
								}
								pli.setFieldValue("Carrier",
										taxdescfromLookupvalue);
								Log.customer
										.debug(" TaxExemptReasonCode Applied on Carrier:  "
												+ taxdescfromLookupvalue);
							}
						}
					}
					// *****************************************************************************//*
					// tax code logic ...
					if (totalTax.equals("0.0")) {
						String companyCode = (String) lic
								.getDottedFieldValue("CompanyCode.UniqueName");
						String state = (String) pli
								.getDottedFieldValue("ShipTo.State");
						String formattedString = companyCode + "_" + state
								+ "_" + "B0";
						Log.customer.debug("***formattedString : "
								+ formattedString);
						String qryString = "Select TaxCode,UniqueName, SAPTaxCode from ariba.tax.core.TaxCode where UniqueName  = '"
								+ formattedString
								+ "' and Country.UniqueName ='"
								+ pli
										.getDottedFieldValue("ShipTo.Country.UniqueName")
								+ "'";
						Log.customer
								.debug(" CatMSCTaxCodeDetermination : REQUISITION: qryString "
										+ qryString);
						AQLOptions queryOptions = new AQLOptions(receipt
								.getPartition());
						Log.customer
								.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage I");
						AQLResultCollection queryResults = Base.getService()
								.executeQuery(qryString, queryOptions);
						Log.customer
								.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage II- Query Executed");
						if (queryResults != null) {
							Log.customer
									.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage III - Query Results not null");
							while (queryResults.next()) {
								Log.customer
										.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage IV - Entering the DO of DO-WHILE");
								Log.customer
										.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage IV - Entering the DO of DO-WHILE"
												+ queryResults.getBaseId(0)
														.get());
								TaxCode taxfromLookupvalue = (TaxCode) queryResults
										.getBaseId(0).get();
								Log.customer.debug(" taxfromLookupvalue"
										+ taxfromLookupvalue);
								Log.customer
										.debug(" CatMSCTaxCodeDetermination : REQUISITION: TaxCodefromLookup"
												+ taxfromLookupvalue);
								// Set the Value of LineItem.TaxCode.UniqueName
								// = 'formattedString'
								Log.customer
										.debug(" CatMSCTaxCodeDetermination : REQUISITION: Setting TaxCodefromLookup"
												+ taxfromLookupvalue);
								pli
										.setFieldValue("TaxCode",
												taxfromLookupvalue);
								Log.customer
										.debug(" CatMSCTaxCodeDetermination : REQUISITION: Applied "
												+ taxfromLookupvalue);
							}
						}
					}
					// end Tax code...
					Log.customer.debug("*** After loop Tax code  : ");
				}
			}
		}
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
	Method Name : addTaxManagerToIR
	Input Parameters: Approvable lic
	Return Type: None
	
	This method adds the Tax Manager when tolerance exceeds or when there is a data error

	*/
	public void addTaxManagerToPR(Approvable lic) {
		Log.customer.debug("%s ::: addTaxManagerToPR - " + lic);
		Object[] isTaxManagerRequired = new Object[1];
		isTaxManagerRequired[0] = "true";
		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		if (isTaxManagerRequired != null) {
			Log.customer.debug("%s ::: isTaxManagerRequired - "
					+ isTaxManagerRequired);
			String approvalRequiredFlag = isTaxManagerRequired[0].toString();
			if (approvalRequiredFlag.equals("true")) {
				// User user =
				// User.getAribaSystemUser(Base.getSession().getPartition());
				// String TaxRole = "Vertex Manager";
				String TaxRole = "Tax Manager";
				String TaxReason = "Tax Reason";
				boolean flag1 = true;
				Object obj = Role.getRole(TaxRole);
				// plic.setFieldValue("ProjectID","F");
				Log.customer
						.debug("%s ::: isTaxManagerRequired - plic bfore create "
								+ plic.toString());
				ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic,
						((ariba.user.core.Approver) (obj)), flag1,
						"RuleReasons", TaxReason);
				Log.customer.debug("%s ::: approvalrequest1 got activated- ");

				BaseVector basevector1 = plic.getApprovalRequests();
				Log.customer.debug("%s ::: basevector1 got activated- ");

				BaseVector basevector2 = approvalrequest1.getDependencies();
				Log.customer.debug("%s ::: basevector2 got activated- ");

				Log.customer.debug("%s ::: Before loop - ");
				// trying to get AR object
				for (int i = 0; i < basevector1.size(); i++) {
					Log.customer.debug("%s ::: inside loop - ");
					ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
					basevector2.add(0, ar1);
					Log.customer.debug("%s ::: ar added to basevector2 ");
					ar1.setState(2);
					Log.customer.debug("%s ::: ar.setState- ");
					ar1.updateLastModified();
					Log.customer.debug("%s ::: ar.updatelastmodified- ");
					basevector1.removeAll(ar1);
					Log.customer.debug("%s ::: basevecotr1 .removeall ");
				}
				Log.customer.debug("%s ::: after loop - ");

				// basevector2.add(0, ar);
				Log.customer.debug("%s ::: ar added to basevector2 ");

				approvalrequest1.setFieldValue("Dependencies", basevector2);
				// ar.setState(2); // need to change
				// Log.customer.debug("%s ::: ar.setState- " );

				// ar.updateLastModified();
				Log.customer.debug("%s ::: ar.updatelastmodified- ");

				// basevector1.removeAll(ar);
				Log.customer.debug("%s ::: basevecotr1 .removeall ");

				basevector1.add(0, approvalrequest1);
				Log.customer.debug("%s ::: basevector1 .add- ");

				plic.setApprovalRequests(basevector1);
				Log.customer
						.debug("%s ::: ir .setApprovalRequests got activated- ");

				java.util.List list = ListUtil.list();
				java.util.Map map = MapUtil.map();
				boolean flag6 = approvalrequest1.activate(list, map);

				Log.customer.debug("%s ::: New TaxAR Activated - ");
				Log.customer.debug("%s ::: State (AFTER): ");
				Log.customer.debug("%s ::: Approved By: %s");
			}
			// else
			// plic.setFieldValue("ProjectID","true");

		}
	}
	/*
	 * Method Name : addIPToPR 
	 * Input Parameters: Approvable r 
	 * Return Type: None
	 * 
	 * This method adds the IP team when there is data issue or processing
	 * request file with wrong data.
	 */
	public void addIPToPR(Approvable r) {

		ProcureLineItemCollection plic = (ProcureLineItemCollection) r;
		String var = "IP";
		/*
		 * ProcureLineItem pli = null; BaseVector lineItems = null; if(plic !=
		 * null){ lineItems = plic.getLineItems(); } int count =
		 * lineItems.size(); User user =
		 * User.getAribaSystemUser(Base.getSession().getPartition());
		 */
		String TaxReason = "Tax Reason";
		boolean flag1 = true;
		Object obj = CatCommonUtil.getRoleforSplitterRuleForVertex(r, var);
		Log.customer.debug("%s ::: isIPTeam - plic bfore create %s", THISCLASS,
				plic.toString());

		BaseVector basevector1 = plic.getApprovalRequests();
		Log.customer.debug("%s ::: basevector1 got activated- %s", THISCLASS);

		// Log.customer.debug("%s ::: Before loop - " );
		ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic,
				((ariba.user.core.Approver) (obj)), flag1, "RuleReasons",
				TaxReason);
		Log.customer.debug("%s ::: approvalrequest1 got activated- %s",
				THISCLASS);

		BaseVector basevector2 = approvalrequest1.getDependencies();
		Log.customer.debug("%s ::: basevector2 got activated- %s", THISCLASS);

		Log.customer.debug("%s ::: Before loop - ");
		// trying to get AR object
		for (int i = 0; i < basevector1.size(); i++) {

			Log.customer.debug("%s ::: inside loop 2 - ");
			ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
			basevector2.add(0, ar1);
			Log.customer.debug("%s ::: ar added to basevector2 ");
			ar1.setState(2);
			Log.customer.debug("%s ::: ar.setState- ");
			ar1.updateLastModified();
			Log.customer.debug("%s ::: ar.updatelastmodified- ");
			basevector1.removeAll(ar1);
			Log.customer.debug("%s ::: basevecotr1 .removeall ");
		}
		Log.customer.debug("%s ::: after loop - ");

		// basevector2.add(0, ar);
		Log.customer.debug("%s ::: ar added to basevector2 %s", THISCLASS);

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		// ar.setState(2);
		// Log.customer.debug("%s ::: ar.setState- %s",THISCLASS );

		// ar.updateLastModified();
		// Log.customer.debug("%s ::: ar.updatelastmodified- %s",THISCLASS);

		// basevector1.removeAll(ar);
		// Log.customer.debug("%s ::: basevecotr1 .removeall %s",THISCLASS );

		basevector1.add(0, approvalrequest1);
		Log.customer.debug("%s ::: basevector1 .add- %s", THISCLASS);

		plic.setApprovalRequests(basevector1);
		Log.customer.debug("%s ::: ir .setApprovalRequests got activated- %s",
				THISCLASS);

		java.util.List list = ListUtil.list();
		java.util.Map map = MapUtil.map();
		boolean flag6 = approvalrequest1.activate(list, map);

		Log.customer.debug("%s ::: New TaxAR Activated - %s", THISCLASS);
		Log.customer.debug("%s ::: State (AFTER): %s", THISCLASS);
		Log.customer.debug("%s ::: Approved By: %s", THISCLASS);
	}

	/*
	 * Method Name : addMSCAdminToIR 
	 * Input Parameters: Approvable lic 
	 * Return Type: None
	 * 
	 * This method adds the MSC Admin when there is vertex Login issue(Purely
	 * for login issues-incorrect user id/pwd
	 */
	public void addMSCAdminToPR(Approvable lic) {
		Log.customer.debug("%s ::: addTaxManagerToPR - %s", THISCLASS, lic);
		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		// User user =
		// User.getAribaSystemUser(Base.getSession().getPartition());
		String TaxRole = "MSC Administrator";
		String TaxReason = "Tax Reason";
		boolean flag1 = true;
		Object obj = Role.getRole(TaxRole);
		// plic.setFieldValue("ProjectID","F");
		Log.customer.debug("%s ::: isMSCAdminRequired - plic bfore create %s",
				THISCLASS, plic.toString());
		ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic,
				((ariba.user.core.Approver) (obj)), flag1, "RuleReasons",
				TaxReason);
		Log.customer.debug("%s ::: approvalrequest1 got activated- %s",
				THISCLASS);

		BaseVector basevector1 = plic.getApprovalRequests();
		Log.customer.debug("%s ::: basevector1 got activated- %s", THISCLASS);

		BaseVector basevector2 = approvalrequest1.getDependencies();
		Log.customer.debug("%s ::: basevector2 got activated- %s", THISCLASS);

		Log.customer.debug("%s ::: Before loop - ");
		// trying to get AR object
		for (int i = 0; i < basevector1.size(); i++) {
			Log.customer.debug("%s ::: inside loop - ");
			ApprovalRequest ar1 = (ApprovalRequest) basevector1.get(i);
			basevector2.add(0, ar1);
			Log.customer.debug("%s ::: ar added to basevector2 ");
			ar1.setState(2);
			Log.customer.debug("%s ::: ar.setState- ");
			ar1.updateLastModified();
			Log.customer.debug("%s ::: ar.updatelastmodified- ");
			basevector1.removeAll(ar1);
			Log.customer.debug("%s ::: basevecotr1 .removeall ");
		}
		Log.customer.debug("%s ::: after loop - ");

		// basevector2.add(0, ar);
		// Log.customer.debug("%s ::: ar added to basevector2 %s",THISCLASS );

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		// ar.setState(2);
		// Log.customer.debug("%s ::: ar.setState- %s",THISCLASS );

		// ar.updateLastModified();
		// Log.customer.debug("%s ::: ar.updatelastmodified- %s",THISCLASS);

		// basevector1.removeAll(ar);
		// Log.customer.debug("%s ::: basevecotr1 .removeall %s",THISCLASS );

		basevector1.add(0, approvalrequest1);
		Log.customer.debug("%s ::: basevector1 .add- %s", THISCLASS);

		plic.setApprovalRequests(basevector1);
		Log.customer.debug("%s ::: ir .setApprovalRequests got activated- %s",
				THISCLASS);

		java.util.List list = ListUtil.list();
		java.util.Map map = MapUtil.map();
		boolean flag6 = approvalrequest1.activate(list, map);

		Log.customer.debug("%s ::: New TaxAR Activated - %s", THISCLASS);
		Log.customer.debug("%s ::: State (AFTER): %s", THISCLASS);
		Log.customer.debug("%s ::: Approved By: %s", THISCLASS);
		// }
		// }
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
