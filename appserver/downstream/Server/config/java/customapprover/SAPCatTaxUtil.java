/*
     This class forms the REQUEST XML that is sent to VERTEX Web Service and parses the response XML received from the VERTEX and
	 does the required tax calculations. The SAP Tax watcher Node will initiate this code
	 The Entry point for this code is : SAPCatTaxCustomApproverInv.java

   Author: Divya
   Change History
	#	Change By	Change Date		Description
	=============================================================================================
		Divya		02/02/2012		Put a fix to compare the Vertex Tax Amount > 0
									instead of != 0
		Divya		10/02/2012		Credit Memos are having TaxAmount as NULL in which case, Null check at TaxAmount.Amount field is changed
									to have NULL check at TaxAmount Field.
		IBM_AMS_Manoj.R    04/07/2012           (WI-308) Usage field has been replaced by UsageClass as Tax Calculation is based on UsageClass.
		IBM Niraj Kumar  05/21/2013     Mach15.5 Rel(FRD5.1/TD5.1)  Code added to handle empty tags in vertex call
		IBM Niraj Kumar  05/21/2013     Mach15.5 Rel(FRD3.3/TD3.3)  Code modified to include three fields VATRegistration, Established and Plafond in Vertex call


*/
package config.java.customapprover;


import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.common.core.SupplierLocation;
import ariba.util.core.ResourceService;
import org.apache.axis.AxisFault;
import ariba.approvable.core.*;
import ariba.base.core.LongString;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import config.java.tax.CatTaxUtil;
import ariba.base.core.BaseVector;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.procure.core.ProcureLineItem;
import ariba.tax.core.TaxCode;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.invoicing.core.InvoiceReconciliation;
//Start: Mach15.5 Rel(FRD5.1/TD5.1)
import ariba.invoicing.core.Invoice;
import ariba.util.core.ListUtil;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
//End: Mach15.5 Rel(FRD5.1/TD5.1)
import ariba.util.log.Log;
import ariba.base.core.aql.*;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.Address;
import ariba.basic.core.Money;
import config.java.common.CatCommonUtil;

import java.util.concurrent.TimeoutException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.util.Date;
import javax.xml.parsers.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
//Start: Mach15.5 Rel(FRD3.3/TD3.3)
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import ariba.util.core.StringUtil;
//End: Mach15.5 Rel(FRD3.3/TD3.3)
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import config.java.tax.CatTaxUtil;


import org.xml.sax.SAXException;

public class SAPCatTaxUtil {
	/*
	Method Name : createRequestFile
	Input Parameters: (ApprovalRequest ar,ValueSource valuesource
	Return Type: None

	This method creates the request XML and loads the file in a physical path and makes a call to the VERTEX

	*/

	public static void createRequestFile(ApprovalRequest ar,ValueSource valuesource)  {

		String className = "SAPCatTaxUtil";
		Log.customer.debug("%s :: Checking the source of trigger : Tax Manager or Custom Approver?:: ",className);

		Approvable lic;
		//InvoiceReconciliation invObj = (InvoiceReconciliation)valuesource;
		lic = ar.getApprovable();
		Partition partition = null;
		partition =  ar.getPartition();
		String strDivisionValue = "";
		String headerLevelTaxAmount= "0";
		User aribasys = User.getAribaSystemUser(ar.getPartition());


		SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");
		if (lic instanceof ProcureLineItemCollection) {
			Log.customer.debug("%s :: Inside ProcureLineItem Collection :: %s",className, lic);
			ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
			BaseVector lineItems = plic.getLineItems();
			int intCountOfLineItems = lineItems.size();
			Log.customer.debug("%s :: Total Number of line items ::", intCountOfLineItems);
			try{

				String root = "VertexEnvelope";
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				documentBuilderFactory.setNamespaceAware(true); // To make Namespace aware
				DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
				Document document = documentBuilder.newDocument();
				Element rootElement = document.createElement(root);

				// Added both the namespaces
				String rootElementNS = ResourceService.getString("cat.ws.util","rootElementNS");
				rootElement.setAttributeNS(rootElementNS,"xmlns", "urn:vertexinc:o-series:tps:6:0");
				rootElement.setAttributeNS(rootElementNS,"xmlns:xsi","http://www.w3.org/2001/XMLSchema-instance");
				document.appendChild(rootElement);

				String userName = (String)lic.getDottedFieldValue("CompanyCode.VertexLoginUID");
				userName = (userName == null)? "" : userName;
				Log.customer.debug("Custom Approver userName : " + userName);
				String passWord = (String)lic.getDottedFieldValue("CompanyCode.VertexLoginPWD");
				passWord = (passWord == null)? "" : passWord;
				Log.customer.debug("Custom Approver passWord :" + passWord);

				//Login element

				Log.customer.debug("%s :: Start of Login Element :: %s",className  );

				String loginText = "Login"; // For Login
				Element login = document.createElement(loginText);
				rootElement.appendChild(login);

				//User Name
				String element = "UserName"; // Login User Name
				String data = userName;
				Element em = document.createElement(element);
				em.appendChild(document.createTextNode(data));
				login.appendChild(em);

				//Password
				String element1 = "Password"; // Login Password
				String data1 = passWord;
				Element em1 = document.createElement(element1);
				em1.appendChild(document.createTextNode(data1));
				login.appendChild(em1);

				Log.customer.debug("%s :: End of Login Element :: %s",className  );

				//End of Login Element


				//Invoice Reconcilation Request Element

				Log.customer.debug("%s :: Start  of Invoice Reconciliation Element :: %s",className  );
				String strIRRequest = "InvoiceVerificationRequest";
				Element strIRRequestElement = document.createElement(strIRRequest );
				rootElement.appendChild(strIRRequestElement);


				//Document Number
				String strDocumentNumber = "documentNumber";
				String strDocumentNumberValue = (String) lic.getDottedFieldValue("UniqueName");
				strDocumentNumberValue = (strDocumentNumberValue == null)? "" : strDocumentNumberValue;
				Log.customer.debug("%s :: Document Number :: %s", className, strDocumentNumberValue );
				Attr attrDocumentNumber  = document.createAttribute(strDocumentNumber);
				attrDocumentNumber.setNodeValue(strDocumentNumberValue);
				strIRRequestElement.setAttributeNode(attrDocumentNumber);


				String spiltDocumentNumber = strDocumentNumberValue;
				String requestFileDocNumber = "";

				{

					Log.customer.debug("%s :: Document Number to be appended in the vertex request file name  :: %s", className, requestFileDocNumber );
					requestFileDocNumber = spiltDocumentNumber.replaceAll(" ","_");
				}

				String ccodeMattaxvalue = (String)lic.getDottedFieldValue("CompanyCode.DefaultTaxCodeForMaterial.SAPTaxCode");
				ccodeMattaxvalue = (ccodeMattaxvalue == null)? "" : ccodeMattaxvalue;
				Log.customer.debug("Custom Approver Material tax in companycode" + ccodeMattaxvalue);
				String ccodeServicetaxvalue = (String)lic.getDottedFieldValue("CompanyCode.DefaultTaxCodeForService.SAPTaxCode");
				ccodeServicetaxvalue = (ccodeServicetaxvalue == null)? "" : ccodeServicetaxvalue;
				Log.customer.debug("Custom Approver Serverice tax in companycode" + ccodeServicetaxvalue);


				//Document Date
				String strDocumentDate = "documentDate";
				String strDocumentDateValue = dateformatYYYYMMDD.format( lic.getSubmitDate()) ;
				Log.customer.debug("%s :: Document Date :: %s",className, strDocumentDateValue );
				Attr attrDocumetDate = document.createAttribute(strDocumentDate);
				attrDocumetDate.setNodeValue(strDocumentDateValue);
				strIRRequestElement.setAttributeNode(attrDocumetDate);


				//Transaction Type
				String strTransactionType = "transactionType";
				String strTransactionTypeValue = "PURCHASE";
				Log.customer.debug("%s :: Transaction Type:: %s",className, strTransactionTypeValue);
				Attr attrTransactiontype = document.createAttribute(strTransactionType);
				attrTransactiontype.setNodeValue(strTransactionTypeValue);
				strIRRequestElement.setAttributeNode(attrTransactiontype);


				//returnAssistedParametersIndicator
				String strReqturnAssistedParametersIndicator = "returnAssistedParametersIndicator";
				String strReqturnAssistedParametersIndicatorValue = "true";
				Log.customer.debug("%s :: Assisted Parameters Indicator:: %s",className, strReqturnAssistedParametersIndicatorValue );
				Attr attrReqturnAssistedParamIndicator = document.createAttribute(strReqturnAssistedParametersIndicator);
				attrReqturnAssistedParamIndicator.setNodeValue(strReqturnAssistedParametersIndicatorValue);
				strIRRequestElement.setAttributeNode(attrReqturnAssistedParamIndicator);

				Log.customer.debug("%s :: End  of Invoice Reconciliation Element :: %s",className  );

				//End of Invoice Reconcilation Request Element

				String strBuyerOutSideLineItemLevel = "Buyer";
				Element strBuyerElementOutSideLineItemLevel = document.createElement(strBuyerOutSideLineItemLevel );
				strIRRequestElement.appendChild(strBuyerElementOutSideLineItemLevel );


				//Company Attribute
				String strCompany = "Company";
				Element strCompanyElement = document.createElement(strCompany);
				String strCompanyCodeValue = "0001";
				Log.customer.debug("%s :: Company Code:: %s",className, strCompanyCodeValue );
				strCompanyElement.appendChild(document.createTextNode(strCompanyCodeValue));
				strBuyerElementOutSideLineItemLevel.appendChild(strCompanyElement);

				// Division Attribute
				String strDivision = "Division";
				Element strDivisionElement= document.createElement(strDivision);
				strDivisionValue = (String) lic.getDottedFieldValue("CompanyCode.UniqueName");
				strDivisionValue = (strDivisionValue == null)? "" : strDivisionValue;
				Log.customer.debug("%s :: Division Code :: %s",className, strDivisionValue );
				strDivisionElement.appendChild(document.createTextNode(strDivisionValue));
				strBuyerElementOutSideLineItemLevel.appendChild(strDivisionElement);


				//Tax Amount value required for tolerance calculation after receiving response


				if(lic.getFieldValue("TaxAmount")!=null)
				{
					headerLevelTaxAmount= ((BigDecimal)lic.getDottedFieldValue("TaxAmount.Amount")).toString();


					Log.customer.debug("%s :: :: Header Level Tax Amount  :: :: "+headerLevelTaxAmount);

				}


				//End of Currency Element

				Log.customer.debug("%s :: Start  of Line Item Element :: %s",className  );

				String accountName="";
				String costCenter ="";
				String taxCodeAmount = "0";



				ProcureLineItem pli = null;

				for (int j = 0; j < intCountOfLineItems; j++) {
					// Procure Line Item object at line level
					pli = (ProcureLineItem) lineItems.get(j);
					Log.customer.debug("%s :: Procure Line Item Object :: %s",className);
					//Check if LineType.Category == 2 -- if its tax then do not send those line items

					int lineTypeCategory = Integer.valueOf(pli.getDottedFieldValue("LineType.Category").toString());
					Log.customer.debug("%s :: lineTypeCategory :: "+lineTypeCategory);

					//Get the flexibleCodeField2 value .. if this is

					String flexibleCodeField2 = (String) pli.getDottedFieldValue("TaxCode.SAPTaxCode");
					flexibleCodeField2 = (flexibleCodeField2 == null)? "" : flexibleCodeField2;
					Log.customer.debug(" :: flexibleCodeField2 :: TaxCode.SAPTaxCode :: "	+ flexibleCodeField2);

					if ( lineTypeCategory != 2)
					{

						if( flexibleCodeField2.equals(ccodeMattaxvalue) || flexibleCodeField2.equals(ccodeServicetaxvalue) )
						{

							Log.customer.debug("%s :: :: LineType.Category is not equal to 2..hence adding line item in vertex Request :: :: ");



							// Printing data from lineitem

							String strLineItem = "LineItem";
							Element strLineItemElement = document.createElement(strLineItem);
							strIRRequestElement.appendChild(strLineItemElement);


							String strNumberInCollection = Integer.toString(pli.getNumberInCollection());
							Log.customer.debug("%s :: Number in Collection Object:: %s",className , strNumberInCollection );

							//Line Item Number

							String strLineItemNumber = "lineItemNumber";
							Attr attrLineItemNumber = document.createAttribute(strLineItemNumber );
							attrLineItemNumber.setNodeValue(strNumberInCollection);
							strLineItemElement.setAttributeNode(attrLineItemNumber);

							SplitAccountingCollection splitAccountingCollection = (SplitAccountingCollection) pli.getDottedFieldValue("Accountings");

							List splitAccountingList = (List) splitAccountingCollection.getDottedFieldValue("SplitAccountings");

							if (splitAccountingList != null) {
								SplitAccounting  firstElement = (SplitAccounting) splitAccountingList.get(0);
								costCenter = (String)firstElement.getDottedFieldValue("CostCenterText");
								costCenter = (costCenter == null)? "" : costCenter;
								Log.customer.debug(":: CostCenter Text ::  " + firstElement);
								accountName = (String)firstElement.getDottedFieldValue("GeneralLedgerText");
								accountName = (accountName == null)? "" : accountName;
								Log.customer.debug(":: General Ledger Text :: "+ accountName);
							}

							// START OF BUYER TAG WITHIN PROCURE LINE ITEM LEVEL

						//Start: Mach15.5 Rel(FRD3.3/TD3.3)
							if(!StringUtil.nullOrEmptyOrBlankString(userName) && userName.equalsIgnoreCase("MSC_VAT"))
							{
							Log.customer.debug("SAPCATTaxUtil: userName - CompanyCode.VertexLoginUID is MSC_VAT - Plafond");
							String strPlafondCode = "Volume";
							String strPlafondCodeValue = "";
							Element strPlafondCodeElement = document.createElement(strPlafondCode);

						if(strDivisionValue != null && (String) pli.getDottedFieldValue("SupplierLocation.PlafondInd") != null)
						  {
		   					Log.customer.debug("strDivisionValue ========== 111 "+ strDivisionValue);
							String plafondCombo = (String) pli.getDottedFieldValue("SupplierLocation.PlafondInd");
							if(!StringUtil.nullOrEmptyOrBlankString(plafondCombo)){
							Log.customer.debug("plafondCombo ============== 222 "+ plafondCombo);
							String[] arr = plafondCombo.split("\\*");
							Log.customer.debug("arr ================== 333 "+ arr);
                             if (Arrays.asList(arr).contains(strDivisionValue)){
								 strPlafondCodeValue = "1";
								 Log.customer.debug(" :: strPlafondCodeValue value ::" + strPlafondCodeValue);
								 }
								 else{
									 strPlafondCodeValue = "0";
									 }

							Log.customer.debug(" :: Plafond value ::" + strPlafondCodeValue);
							strPlafondCodeElement .appendChild(document.createTextNode(strPlafondCodeValue));
							strLineItemElement.appendChild(strPlafondCodeElement);
						    }
						}
					}
					//End: Mach15.5 Rel(FRD3.3/TD3.3)
							Log.customer.debug("%s :: Start of Buyer Tag :: %s",className );

							String strBuyer = "Buyer";
							Element strBuyerElement = document.createElement(strBuyer );
							strLineItemElement.appendChild(strBuyerElement);


							// Destination Attribute

							String strDestination = "Destination";
							Element strDestinationElement = document.createElement(strDestination );
							strBuyerElement.appendChild(strDestinationElement);

							//Ship to contents

							Address shipToCotentTags = (Address) pli.getDottedFieldValue("ShipTo");
							Log.customer.debug(" :: Ship To Values are ::" + shipToCotentTags);

							String shipToCityForBuyerTag = (String) shipToCotentTags.getFieldValue("City");
							Log.customer.debug(" :: Ship To City Value is ::" + shipToCityForBuyerTag);

							String shipToStateForBuyerTag = (String) pli.getDottedFieldValue("ShipTo.State");
							shipToStateForBuyerTag = (shipToStateForBuyerTag == "none")? "" : shipToStateForBuyerTag;
							Log.customer.debug(" :: Shipt To State :: " + shipToStateForBuyerTag);

							String shipToPostalCodeForBuyerTag = (String) pli.getDottedFieldValue("ShipTo.PostalCode");

							shipToPostalCodeForBuyerTag = (shipToPostalCodeForBuyerTag == null)? "" : shipToPostalCodeForBuyerTag;

							shipToPostalCodeForBuyerTag = shipToPostalCodeForBuyerTag.replaceAll("-","");
							Log.customer.debug(" :: Ship To Postal Code :: "+ shipToPostalCodeForBuyerTag);

							String shipToCountryForBuyerTag = (String) pli.getDottedFieldValue("ShipTo.Country.UniqueName");
							shipToCountryForBuyerTag = (shipToCountryForBuyerTag == null)? "" : shipToCountryForBuyerTag;
							Log.customer.debug(" :: Ship to Country :: " + shipToCountryForBuyerTag);

							//City Sub Attribute
							String strCity = "City";
							Element strCityElement= document.createElement(strCity);
							String strCityValue = shipToCityForBuyerTag;
							//Start: Mach15.5 Rel(FRD5.1/TD5.1)
							if(!StringUtil.nullOrEmptyOrBlankString(strCityValue)) {
							Log.customer.debug("%s :: Buyer Tag City attribute :: %s",className, strDivisionValue );
							strCityElement.appendChild(document.createTextNode(strCityValue));
							strDestinationElement.appendChild(strCityElement);
						    }
							//End: Mach15.5 Rel(FRD5.1/TD5.1)
							//Main Division Sub Attribute
							String strMainDivision = "MainDivision";
							Element strMainDivisionElement= document.createElement(strMainDivision);
							String strMainDivisionValue = shipToStateForBuyerTag;
							//Start: Mach15.5 Rel(FRD5.1/TD5.1)
							if(!StringUtil.nullOrEmptyOrBlankString(strMainDivisionValue)) {
							Log.customer.debug("%s :: Buyer Tag Main Division :: %s",className, strMainDivisionValue );
							strMainDivisionElement.appendChild(document.createTextNode(strMainDivisionValue));
							strDestinationElement.appendChild(strMainDivisionElement);
							}
							//End: Mach15.5 Rel(FRD5.1/TD5.1)
							//Postal Code Sub Attribute
							String strPostalCode = "PostalCode";
							Element strPostalCodeElement= document.createElement(strPostalCode);
							String strPostalCodeValue = shipToPostalCodeForBuyerTag;
							Log.customer.debug("%s :: Buyer Tag Postal Code  %s",className, strPostalCodeValue );
							strPostalCodeElement.appendChild(document.createTextNode(strPostalCodeValue));
							strDestinationElement.appendChild(strPostalCodeElement);

							//Country Sub Attribute
							String strCountry = "Country";
							Element strCountryElement= document.createElement(strCountry );
							String strCountryValue = shipToCountryForBuyerTag;
							Log.customer.debug("%s :: Buyer Tag For Country  :: %s",className, strCountryValue );
							strCountryElement.appendChild(document.createTextNode(strCountryValue ));
							strDestinationElement.appendChild(strCountryElement);

							Log.customer.debug("%s :: End of Buyer Tag :: %s",className );

							//END OF BUYER TAG


							//Cost Centre
							String strCostCenter = "costCenter";
							String strCostCenterValue = costCenter;
							Attr attrCostCenter = document.createAttribute(strCostCenter);
							attrCostCenter.setNodeValue(strCostCenterValue);
							Log.customer.debug(" :: Cost Center :: " + strCostCenterValue );
							strLineItemElement.setAttributeNode(attrCostCenter);

							//generalLedgerAccount

							String strGeneralLedger = "generalLedgerAccount";
							String strGeneralLedgerValue = accountName;
							Attr attrGeneralLedger = document.createAttribute(strGeneralLedger);
							attrGeneralLedger.setNodeValue(strGeneralLedgerValue);
							Log.customer.debug(" :: General Ledger Text ::" + strGeneralLedgerValue);
							strLineItemElement.setAttributeNode(attrGeneralLedger);

							//Delivery Term
							String incoTerms1 = (String) pli.getDottedFieldValue("IncoTerms1.UniqueName");

							if (incoTerms1 == null) {
								incoTerms1 = "";
							} else {
								incoTerms1 = incoTerms1;
							}
							Log.customer.debug("%s :: Incoterms value :: %s",className, incoTerms1);

							String strDeliverTerm = "deliveryTerm";
							Attr attrDeliveryTerm = document.createAttribute(strDeliverTerm);
							attrDeliveryTerm .setNodeValue(incoTerms1);
							strLineItemElement.setAttributeNode(attrDeliveryTerm );


							//Usage
							String strMannerOfUse = (String)pli.getDottedFieldValue("TaxUse.UniqueName");
							strMannerOfUse = (strMannerOfUse == null)? "UC001 - Office Functions" : strMannerOfUse;
							Log.customer.debug(" :: Manner of Use :: "+ strMannerOfUse );

							// WI-308 Starts
							//String strUsage = "usage";
							String strUsage = "usageClass";
							// WI-308 End
							Attr attrUsage = document.createAttribute(strUsage);
							attrUsage.setNodeValue(strMannerOfUse);
							strLineItemElement.setAttributeNode(attrUsage);


							//Start of Vendor TAG

							Log.customer.debug(" :: Start of Vendor Element within Procure Line Item level  ::" );

							String strVendor = "Vendor";
							Element strVendorElement = document.createElement(strVendor);
							strLineItemElement.appendChild(strVendorElement);


							//Vendor Code

							String strVendorCode = "VendorCode";
							Element strVendorCodeElement = document.createElement(strVendorCode);
							String strVendorCodeValue = (String) pli.getDottedFieldValue("Supplier.UniqueName");
							strVendorCodeValue = (strVendorCodeValue == null)? "" : strVendorCodeValue;
							Log.customer.debug(" :: Vendor Code alias supplier unique name ::" + strVendorCodeValue);
							strVendorCodeElement .appendChild(document.createTextNode(strVendorCodeValue));
							strVendorElement.appendChild(strVendorCodeElement);
							//Start: Mach15.5 Rel(FRD3.3/TD3.3)
							if(!StringUtil.nullOrEmptyOrBlankString(userName) && userName.equalsIgnoreCase("MSC_VAT"))
							{
							Log.customer.debug("SAPCATTaxUtil: userName - CompanyCode.VertexLoginUID is MSC_VAT");
							String taxReg = "TaxRegistration";
							Element strTaxRegElement = document.createElement(taxReg);
							String strContCode = "isoCountryCode";
							String strContCodeValue = shipToCountryForBuyerTag;
							if(!StringUtil.nullOrEmptyOrBlankString(strContCodeValue)){
								Attr attrContCode = document.createAttribute(strContCode);
								attrContCode.setNodeValue(strContCodeValue);
								Log.customer.debug(" :: General strContCodeValue Text ::" + strContCodeValue);
								strTaxRegElement.setAttributeNode(attrContCode);
							}
							String strEstablishedCodeValue="";
							String strEstablished = (String) pli.getDottedFieldValue("SupplierLocation.Established");
							Log.customer.debug(" :: strEstablished value is ::" + strEstablished);
							if(!StringUtil.nullOrEmptyOrBlankString(strEstablished))
							{
							  	strEstablishedCodeValue = (String) pli.getDottedFieldValue("SupplierLocation.Established");
								Log.customer.debug(" :: Established value ::" + strEstablishedCodeValue);
								if (strEstablishedCodeValue != null && strEstablishedCodeValue.equalsIgnoreCase("T")){
									String strEstablishedCode = "hasPhysicalPresenceIndicator";
									String hasPhysicalPresenceIndicatorValue="true";
										Attr attrEstablishedCode = document.createAttribute(strEstablishedCode);
										attrEstablishedCode.setNodeValue(hasPhysicalPresenceIndicatorValue);
										Log.customer.debug(" :: strEstablishedCodeValue ::" + strEstablishedCodeValue);
										strTaxRegElement.setAttributeNode(attrEstablishedCode);
								}
							}
														String strVATRegCode = "TaxRegistrationNumber";
														Log.customer.debug(" :: Starting VATRegistration code=====2 ::");
														Element strVATRegCodeElement = document.createElement(strVATRegCode);
														Log.customer.debug(" :: Starting VATRegistration code=====3 ::");
							                            String strVATRegCodeValue = "";
							                            Log.customer.debug(" :: Starting VATRegistration code=====4 ::");
								                       if ((String) pli.getDottedFieldValue("Statement.VATRegistration") != null)
								                       {
															Log.customer.debug(" :: Starting VATRegistration code=====5 ::" + (String) pli.getDottedFieldValue("Statement.VATRegistration"));
															strVATRegCodeValue = (String) pli.getDottedFieldValue("Statement.VATRegistration");
															Log.customer.debug("strVATRegCodeValue not null "+ strVATRegCodeValue);
															strVATRegCodeElement .appendChild(document.createTextNode(strVATRegCodeValue));
															strTaxRegElement.appendChild(strVATRegCodeElement);
											}
							strVendorElement.appendChild(strTaxRegElement);
							}
                            //End: Mach15.5 Rel(FRD3.3/TD3.3)
							//Administrative Origin

							String strAdministrativeOrigin = "PhysicalOrigin";
							Element strAdministrativeOriginElement = document.createElement(strAdministrativeOrigin);
							strVendorElement.appendChild(strAdministrativeOriginElement);

							//Ship From Address from Invoice - If not documented, please use Supplier Location Ship From

							String strVendorCityValue = "";
							String strVendorMainDivisionValue = "";
							String strVendorPostalCodeValue = "";
							String strVendorCountryValue = "";
							if((Address) pli.getDottedFieldValue("StatementLineItem.ShipFrom")!=null)
							{
								if((String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.City")!=null)
									strVendorCityValue = (String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.City");
								if((String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.State")!=null)
									strVendorMainDivisionValue = (String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.State");
								if((String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.PostalCode")!=null)
									strVendorPostalCodeValue = (String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.PostalCode");
								if((String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.Country.UniqueName")!=null)
									strVendorCountryValue = (String) pli.getDottedFieldValue("StatementLineItem.ShipFrom.Country.UniqueName");
							}

							else
							{
								Log.customer.debug(" :: Ship from Not documented on Invoice hence proceeding to get Invoice Supplier Location Details .Thank you ::::" );

								if((String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.City")!=null)
									strVendorCityValue = (String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.City");
								if((String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.State")!=null)
									strVendorMainDivisionValue = (String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.State");
								if((String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.PostalCode")!=null)
									strVendorPostalCodeValue = (String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.PostalCode");
								if((String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.Country.UniqueName")!=null)
									strVendorCountryValue = (String) pli.getDottedFieldValue("StatementLineItem.SupplierLocation.Country.UniqueName");
							}

							//City

							String strVendorCity = "City";
							Element strVendorCityElement = document.createElement(strVendorCity);
							Log.customer.debug(" :: supplier city ::" + strVendorCityValue);
							//Start: Mach15.5 Rel(FRD5.1/TD5.1)
							if(!StringUtil.nullOrEmptyOrBlankString(strVendorCityValue)) {
								Log.customer.debug(" :: supplier city is null  ::" + strVendorCityValue);
							strVendorCityElement.appendChild(document.createTextNode(strVendorCityValue));
							strAdministrativeOriginElement.appendChild(strVendorCityElement);
						    }
							//End: Mach15.5 Rel(FRD5.1/TD5.1)


							//MainDivision

							String strVendorMainDivision = "MainDivision";
							Element strVendorMainDivisionElement = document.createElement(strVendorMainDivision);
							Log.customer.debug( " :: Main Division Value :: " + strVendorMainDivisionValue);
							//Start: Mach15.5 Rel(FRD5.1/TD5.1)
							if(!StringUtil.nullOrEmptyOrBlankString(strVendorMainDivisionValue)) {
								Log.customer.debug( " :: Main Division Value is null :: " + strVendorMainDivisionValue);
							strVendorMainDivisionElement.appendChild(document.createTextNode(strVendorMainDivisionValue));
							strAdministrativeOriginElement.appendChild(strVendorMainDivisionElement);
								}
							//End: Mach15.5 Rel(FRD5.1/TD5.1)
							//PostalCode

							String strVendorPostalCode = "PostalCode";
							Element strVendorPostalCodeElement = document.createElement(strVendorPostalCode);
							strVendorPostalCodeValue = strVendorPostalCodeValue.replaceAll("-","");
							Log.customer.debug(" :: Vendor Postal Code Value :: " + strVendorPostalCodeValue);
							strVendorPostalCodeElement.appendChild(document.createTextNode(strVendorPostalCodeValue));
							strAdministrativeOriginElement.appendChild(strVendorPostalCodeElement);

							//Country

							String strVednorCountry = "Country";
							Element strVendorCountryElement = document.createElement(strVednorCountry);
							Log.customer.debug(" :: Vendor Country Value ::" + strVendorCountryValue);
							strVendorCountryElement.appendChild(document.createTextNode(strVendorCountryValue));
							strAdministrativeOriginElement.appendChild(strVendorCountryElement);

							Log.customer.debug(" :: End of Vendor Element ::" );



							//END of Vendor Tag

							//Charged Tax

							String strChargedTax = "ChargedTax";
							Element chargedTaxElement = document.createElement(strChargedTax);
							chargedTaxElement.appendChild(document.createTextNode("0"));
							strLineItemElement.appendChild(chargedTaxElement);


							//Purchase
							String commodityCode ="";
							if(pli.getDottedFieldValue("Description.CommonCommodityCode.UniqueName")!=null)
								commodityCode = (String) pli.getDottedFieldValue("Description.CommonCommodityCode.UniqueName");
							Log.customer.debug(" :: COMMON commodity Code  ::" + commodityCode );


							String strPurchaseText = "Purchase";
							Element strPurchaseElement = document.createElement(strPurchaseText);
							strPurchaseElement.appendChild(document.createTextNode(commodityCode));
							strLineItemElement.appendChild(strPurchaseElement);

							//Quantity
							String quantity = "";
							if(pli.getDottedFieldValue("Quantity").toString()!=null)
								quantity = pli.getDottedFieldValue("Quantity").toString();
							Log.customer.debug(" :: quantity ::" + quantity );

							String strQuantity = "Quantity";
							Element strQuantityElement = document.createElement(strQuantity);
							strQuantityElement.appendChild(document.createTextNode(quantity));
							strLineItemElement.appendChild(strQuantityElement);

							//Extended Price
							String amount = "";
							if(pli.getDottedFieldValue("Amount.Amount").toString()!=null)
								amount = ((BigDecimal)pli.getDottedFieldValue("Amount.Amount")).toString();
							Log.customer.debug(" :: amount ::" + amount );


							String strExtendedPrice = "ExtendedPrice";
							Element strExtendedPriceElement = document.createElement(strExtendedPrice);
							strExtendedPriceElement.appendChild(document.createTextNode(amount));
							strLineItemElement.appendChild(strExtendedPriceElement);



							// FlexibleFields Tag
							String FlexibleFieldsText = "FlexibleFields";
							Element FlexibleFields = document.createElement(FlexibleFieldsText);
							strLineItemElement.appendChild(FlexibleFields);


							String flexibleCodeField1 = accountName; // change as per mapping doc
							Log.customer.debug(":: flexibleCodeField1 :: accountName alias the GL Text :: "	+ flexibleCodeField1);

							String flexibleCodeField3 = (String) pli.getDottedFieldValue("LineItemType");
							flexibleCodeField3 = (flexibleCodeField3 == null)? "" : flexibleCodeField3;
							Log.customer.debug(" :: flexibleCodeField3  alias the Line Item Type ::"+ flexibleCodeField3);

							if(flexibleCodeField3.contains("TQM")){
								flexibleCodeField3 = "M";
							}
							if(flexibleCodeField3.contains("TQB")){
								flexibleCodeField3 = "B";
							}
							if(flexibleCodeField3.contains("TQC")){
								flexibleCodeField3 = "C";
							}
							if(flexibleCodeField3.contains("TQS")){
								flexibleCodeField3 = "S";
							}
							Log.customer.debug(" :: Line Item Type value going as request is :: "+ flexibleCodeField3);


							//String flexibleCodeField5 = strVendorMainDivisionValue;//Check this - this state value or supplier loc state value
							String flexibleCodeField5 = shipToStateForBuyerTag;
							Log.customer.debug(" :: flexibleCodeField5 alias Vendor Main Division or State Value :: "+ flexibleCodeField5);

							String flexibleCodeField25 = strVendorCountryValue;
							Log.customer.debug(" :: flexibleCodeField25 alias the vendor country value ::"	+ flexibleCodeField25);

							String FlexibleCodeFieldText1 = "FlexibleCodeField";
							String flexcodeFieldAttribute1 = "fieldId";
							String flexcodeFieldAttributeData1 = "1";
							Attr flexcodeFieldAttr1 = document.createAttribute(flexcodeFieldAttribute1);
							flexcodeFieldAttr1.setNodeValue(flexcodeFieldAttributeData1);
							Element FlexibleCodeField1 = document.createElement(FlexibleCodeFieldText1);
							FlexibleCodeField1.setAttributeNode(flexcodeFieldAttr1);
							String FlexibleCodeFielddata1 = flexibleCodeField1;
							FlexibleCodeField1.appendChild(document.createTextNode(FlexibleCodeFielddata1));
							FlexibleFields.appendChild(FlexibleCodeField1);


							String FlexibleCodeFieldText2 = "FlexibleCodeField";
							String flexcodeFieldAttribute2 = "fieldId";
							String flexcodeFieldAttributeData2 = "2";
							Attr flexcodeFieldAttr2 = document.createAttribute(flexcodeFieldAttribute2);
							flexcodeFieldAttr2.setNodeValue(flexcodeFieldAttributeData2);
							Element FlexibleCodeField2 = document.createElement(FlexibleCodeFieldText2);
							FlexibleCodeField2.setAttributeNode(flexcodeFieldAttr2);
							String FlexibleCodeFielddata2 = flexibleCodeField2;
							FlexibleCodeField2.appendChild(document.createTextNode(FlexibleCodeFielddata2));
							FlexibleFields.appendChild(FlexibleCodeField2);


							String FlexibleCodeFieldText3 = "FlexibleCodeField";
							String flexcodeFieldAttribute3 = "fieldId";
							String flexcodeFieldAttributeData3 = "3";
							Attr flexcodeFieldAttr3 = document.createAttribute(flexcodeFieldAttribute3);
							flexcodeFieldAttr3.setNodeValue(flexcodeFieldAttributeData3);
							Element FlexibleCodeField3 = document.createElement(FlexibleCodeFieldText3);
							FlexibleCodeField3.setAttributeNode(flexcodeFieldAttr3);
							String FlexibleCodeFielddata3 = flexibleCodeField3;
							FlexibleCodeField3.appendChild(document.createTextNode(FlexibleCodeFielddata3));
							FlexibleFields.appendChild(FlexibleCodeField3);

							String FlexibleCodeFieldText5 = "FlexibleCodeField";
							String flexcodeFieldAttribute5 = "fieldId";
							String flexcodeFieldAttributeData5 = "5";
							Attr flexcodeFieldAttr5 = document.createAttribute(flexcodeFieldAttribute5);
							flexcodeFieldAttr5.setNodeValue(flexcodeFieldAttributeData5);
							Element FlexibleCodeField5 = document.createElement(FlexibleCodeFieldText5);
							FlexibleCodeField5.setAttributeNode(flexcodeFieldAttr5);
							String FlexibleCodeFielddata5 = flexibleCodeField5;
							//Start: Mach15.5 Rel(FRD5.1/TD5.1)
							Log.customer.debug(" :: flexibleCodeField5 alias FlexibleCodeFielddata5 or State Value :: "+ flexibleCodeField5);
							if(!StringUtil.nullOrEmptyOrBlankString(flexibleCodeField5)) {
								Log.customer.debug(" :: Inside loop FlexibleCodeFielddata5 or State Value :: "+ flexibleCodeField5);
							    FlexibleCodeField5.appendChild(document.createTextNode(FlexibleCodeFielddata5));
							    FlexibleFields.appendChild(FlexibleCodeField5);
							       }
							//End: Mach15.5 Rel(FRD5.1/TD5.1)
							String FlexibleCodeFieldText25 = "FlexibleCodeField";
							String flexcodeFieldAttribute25 = "fieldId";
							String flexcodeFieldAttributeData25 = "25";
							Attr flexcodeFieldAttr25 = document.createAttribute(flexcodeFieldAttribute25);
							flexcodeFieldAttr25.setNodeValue(flexcodeFieldAttributeData25);
							Element FlexibleCodeField25 = document.createElement(FlexibleCodeFieldText25);
							FlexibleCodeField25.setAttributeNode(flexcodeFieldAttr25);
							String FlexibleCodeFielddata25 = flexibleCodeField25;
							FlexibleCodeField25.appendChild(document.createTextNode(FlexibleCodeFielddata25));
							FlexibleFields.appendChild(FlexibleCodeField25);


							strIRRequestElement.appendChild(strLineItemElement);

							Log.customer.debug(" :: End of Line item Tag :: ");


						}//end of If loop for filtering out the line items that are taxable
					}//End of if loop to check for SAPTAXCODE B1
				}// End of procure line item Level



				Date date = new Date();
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(document);
				String filePath = ResourceService.getString("cat.ws.util","VertexFilePath");
				File requestFile = new File(filePath + "VertexIRRequest_" +requestFileDocNumber+ "_" + date.getTime() +".xml");

				StreamResult result = new StreamResult(requestFile );
				Log.customer.debug(" :: Vertex IR Request File Content :: " + requestFile );
				Log.customer.debug(":: Vertex IR Request File Content :: RESULT AND SOURCE " + result + "," + source);
				Log.customer.debug(" :: Absolute Path of the File ::"+ requestFile.getAbsolutePath());
				try{

					transformer.transform(source, result);

					Log.customer.debug(":: :: Generated the vertex IR request file Successfully :: :: ");


					VertexTaxIRWSCall wscall = new VertexTaxIRWSCall();
					Log.customer.debug(" :: Before calling the vertex web service for IR :: ", className);
					String responseString;


					responseString = wscall.getVertexTaxResponse(requestFile,requestFileDocNumber);
					Log.customer.debug(" :: After calling getVertexTaxResponse() ::  %s",className, "");


					InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)lic;

					Object[] isTaxManagerRequired = parseXMLResult(headerLevelTaxAmount,strDivisionValue ,responseString ,pli,ar,lineItems,invoicereconciliation  );
					if(isTaxManagerRequired != null)
					{
						String approvalRequiredFlag = isTaxManagerRequired[0].toString();
						String approvalReason = isTaxManagerRequired[1].toString();
						if(approvalRequiredFlag.equals("true"))
						{


							User user = User.getAribaSystemUser(partition);
							boolean flag1 = true;
							String TaxReason= "Tax Reason";
							String role = "TM";

							invoicereconciliation.setDottedFieldValue("TaxOverrideFlag",true);

							Log.customer.debug("%s ::: addTaxManagerToIR - " + lic);

							Object obj = CatCommonUtil.getRoleforSplitterRuleForVertex(lic,role);
							invoicereconciliation.setDottedFieldValue("TaxOverrideFlag",true);
							ApprovalRequest approvalrequest1 = ApprovalRequest.create(invoicereconciliation, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
							Log.customer.debug("%s ::: approvalrequest1 got activated- " );

							BaseVector basevector1 = invoicereconciliation.getApprovalRequests();
							Log.customer.debug("%s ::: basevector1 got activated- " );

							BaseVector basevector2 = approvalrequest1.getDependencies();
							Log.customer.debug("%s ::: basevector2 got activated- " );

							basevector2.add(0, ar);
							Log.customer.debug("%s ::: ar added to basevector2 " );

							approvalrequest1.setFieldValue("Dependencies", basevector2);
							ar.setState(2);
							Log.customer.debug("%s ::: ar.setState- " );

							ar.updateLastModified();
							Log.customer.debug("%s ::: ar.updatelastmodified- " );

							basevector1.removeAll(ar);
							Log.customer.debug("%s ::: basevecotr1 .removeall " );

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
							invoicereconciliation.setDottedFieldValue("TaxOverrideFlag",false);

					}



					Log.customer.debug("After Parsing response...");
				}



				catch (AxisFault f) {
					// Determine type of fault (Client or Server)
					Log.customer.debug(" In Catch ......");
					String faultCode = f.getFaultCode().getLocalPart();
					String faultDetails = f.getFaultString();
					String faultString = f.getFaultString();
					String failReason = "Reason For Failure: ";
					Log.customer.debug("%s In Catch ...... faultString:- "+ faultString);
					Log.customer.debug("%s In Catch ...... faultCode:- "+ faultCode);
					BaseVector comments = plic.getComments();
					int count1 = comments.size();
					Log.customer.debug("%s **** Multiple comments CatTaxCustomApprover setting up comments",count1);

					if (faultCode.equalsIgnoreCase("server")) {
						/*		 This indicates that the Web Service is not in a valid
						  state Processing should stop until the problem is
						  resolved. A
						  "Server" fault is generated when a
						  VertexSystemException is thrown on the Server.
						  */
						Log.customer.debug("web-service is not in valid state. ****************  Server");
						faultString = faultString.concat("\n web-service is not in valid state.");
						Log.customer.debug("%s fault String :- " + faultString);
						Log.customer.debug(faultDetails.toString());
						Log.customer.debug("%s Reason :- " + f.getFaultReason());
						Log.customer.debug("%s message :- " + f.getMessage());
						lic.setFieldValue("ProjectID","F");
						// Update comments if Web Service fails ............
						LongString longstring = new LongString(faultString);
						addCommentToIR(ar,longstring,failReason,aribasys);
						addVertexManagerToIR(ar,lic);

					} else if (faultCode.equalsIgnoreCase("client")) {
						/* A "Client" fault would indicate that the request is
						  flawed but processing of additional requests could
						 continue. A "Client"
						  fault is generated when a VertexApplicationException
						  is thrown on the Server.*/


						Log.customer.debug("The XML request is invalid. Fix the request and resend.******** client");
						faultString = faultString.concat("\n The XML request is invalid. Fix the request and resend.");

						Log.customer.debug("%s fault String :- " + faultString);
						Log.customer.debug(faultDetails.toString());
						Log.customer.debug("%s Reason :- " + f.getFaultReason());
						Log.customer.debug("%s message :- " + f.getMessage());
						Log.customer.debug(" Before setting lic objcet ******** client" + lic.toString());
						//  Update comments if XML file is not valid............
						LongString longstring = new LongString(faultString);
						addCommentToIR(ar,longstring,failReason,aribasys);

						if(faultString.contains("User login failed"))
						  addMSCAdminToIR(ar,lic);
						  else
						// Addition of tax manager................
						addTaxManagerToIR(ar,lic);


					}else{
						Log.customer.debug("web-service is not in valid state. ****************  other");
						faultString = faultString.concat("\n web-service is not in valid state.");
						Log.customer.debug("%s fault String :- " + faultString);
						Log.customer.debug(faultDetails.toString());
						Log.customer.debug("%s Reason :- " + f.getFaultReason());
						Log.customer.debug("%s message :- " + f.getMessage());
						/// Update comments if Web Service fails ............
						LongString longstring = new LongString(faultString);
						addCommentToIR(ar,longstring,failReason,aribasys);
						addVertexManagerToIR(ar,lic);
					}

					Log.customer.debug("%s ::  Completed Axis Fault : - ");
				}//End Brace of Axis Fault Catch

				/*catch (IOException e) {
					// TODO Auto-generated catch block
					Log.customer.debug(" :: In IO Exception");
					Log.customer.debug(" :: in Exception" + e.getMessage());
					e.printStackTrace();
				}*/catch(NullPointerException e){
					Log.customer.debug(" :: In Null Exception");
					Log.customer.debug(" :: in Exception" + e.getMessage());
				}/*catch(TimeoutException a){
					Log.customer.debug(" :: In TimeoutException . Fix the request and resend.");
					Log.customer.debug(a.getCause());
					Log.customer.debug(a.getMessage());
				}*/catch(Exception a){
					Log.customer.debug(" :: Inside Exception . Fix the request and resend.");
					Log.customer.debug(a.getCause());
					Log.customer.debug(a.getMessage());
				}

			}//End of try for send Request method

			catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				Log.customer.debug("... Error In ParserConfigurationException ...");
				Log.customer.debug("... Error Message ..." + e.getMessage());
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				Log.customer.debug("... Error In Transforming ...");
				Log.customer.debug("... Error Message ..." + e.getMessage());
				e.printStackTrace();
			} catch(NullPointerException e){
				Log.customer.debug("In Null Exception");
				Log.customer.debug("in Exception" + e.getMessage());
			}catch(Exception a){
				Log.customer.debug("Inside Exception . Fix the request and resend.");
				Log.customer.debug(a.getCause());
				Log.customer.debug(a.getMessage());
			}
			/* catch (IOException e) {
				// TODO Auto-generated catch block
				Log.customer.debug("In IO Exception");
				Log.customer.debug("in Exception" + e.getMessage());
				e.printStackTrace();
			} */


		} // End Brace of sendRequest Method

	}//End brace of if instance of procure line item

	/*
	Method Name : parseXMLResult
	Input Parameters: String headerLevelTaxAmount,String divisionNode,String responseString,ProcureLineItem pli,
					   ApprovalRequest ar,BaseVector lineItems,InvoiceReconciliation invoicereconciliation
	Return Type: Object[2]

	This method parses the response that is received from VERTEX. The main functionality includes setting the tax amount at line level
	from the tax values received from vertex ; setting the tax code wherever necessary and checking the tolerance value,adding the tax manager
	if the tolerance exceeds 1%

	*/
	private static Object[] parseXMLResult(String headerLevelTaxAmount,String divisionNode,String responseString,ProcureLineItem pli,ApprovalRequest ar,BaseVector lineItems,InvoiceReconciliation invoicereconciliation  ) {
		Object[] isTaxManagerRequired =  new Object[2];
		LongString longstring = new LongString("");
		isTaxManagerRequired[0]=  "false";
		isTaxManagerRequired[1] = longstring ;
		Partition partition = ar.getPartition();
		Money authPOTaxAmount  = new Money();

		//set precision level for calculating tolerance for BigDecimal values..currently set to the 5th difit
		BigDecimal amountFromMSC = new BigDecimal(headerLevelTaxAmount).setScale(5, 5);

		if(amountFromMSC.compareTo(BigDecimal.ZERO)==0) amountFromMSC =  new BigDecimal("0");

		String taxCodeAmount = "0";
		BigDecimal taxAmountSum = new BigDecimal(0.0).setScale(5, 5);





		try {


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
			String headerLevelTaxAmtFromVertex = "";
			double totalTaxinInteger = 0;
			double sumOfLineItemTaxes = 0;
			String taxToleranceExceededException = "";

			Approvable lic;
			lic = ar.getApprovable();




			//Considering the header from the response - line item level

			Log.customer.debug("Root element "+ doc.getDocumentElement().getNodeName());
			NodeList nodeList = doc.getElementsByTagName("LineItem");

			Log.customer.debug("Information of all Line Item");

			for (int s = 0; s < nodeList.getLength(); s++)
			{
				//Get the Line Item Number
				Node fstNode = nodeList.item(s);
				Element fstElmntlnm = (Element) fstNode;
				String lineItemNumber = fstElmntlnm.getAttribute("lineItemNumber");


				int index = Integer.parseInt(lineItemNumber);
				Log.customer.debug(":: ::  lineItemNumber outside loop  after :: :: "+ index);

				int plinumber = index - 1;
				Log.customer.debug(":: :: lineItemNumber plinumber  after:: :: "+ plinumber);
				pli = (ProcureLineItem) lineItems.get(plinumber);
				Log.customer.debug("Node list ::"+nodeList.getLength());

				if (fstNode.getNodeType() == Node.ELEMENT_NODE)
				{

					Element fstElmnt = (Element) fstNode;
					NodeList countryElmntLst = fstElmnt.getElementsByTagName("Country");
					Element lstNmElmnt = (Element) countryElmntLst.item(0);
					NodeList lstNm = lstNmElmnt.getChildNodes();
					Log.customer.debug("::  Country : "	+ ((Node) lstNm.item(0)).getNodeValue());
					//To Calculate total tax by summing the total tax at line level

					NodeList totaltaxElmntLst = fstElmnt.getElementsByTagName("TotalTax");
					Element lstNmElmnt1 = (Element) totaltaxElmntLst.item(0);
					NodeList lstNm1 = lstNmElmnt1.getChildNodes();
					Log.customer.debug(":: TotalTax after population :: "+ ((Node) lstNm1.item(0)).getNodeValue());
					totalTax = ((Node) lstNm1.item(0)).getNodeValue();

					BigDecimal	taxAmount= new BigDecimal(totalTax);
					Money taxTotal = new Money(taxAmount, pli.getAmount().getCurrency());
					pli.setFieldValue("TaxAmount", taxTotal);
					Log.customer.debug(":: Tax Amount that is being set :: " + totalTax);


					totalTaxinInteger= Double.valueOf(totalTax);
					//Summing the total tax for each line item
					sumOfLineItemTaxes = sumOfLineItemTaxes+totalTaxinInteger;
					taxAmountSum= new BigDecimal(sumOfLineItemTaxes);

					//Setting scale of the Big Decimal value of total tax to 5
					taxAmountSum = taxAmountSum.setScale(5,5);
					authPOTaxAmount = new Money(taxAmountSum , pli.getAmount().getCurrency());


					headerLevelTaxAmtFromVertex = taxAmountSum.toString();
					Log.customer.debug(":: headerLevelTaxAmtFromVertex ::"+headerLevelTaxAmtFromVertex);


					//getting the tax code from the response
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



				}




				//Change tax code to CC_B2 based on invoice amount(if=0),vertex amount(if>0) and tax code(if=B2)


				Log.customer.debug(":: :: INVOICE ONLY tax amount in buyer  :: ::"+amountFromMSC );
				Log.customer.debug(":: :: Vertex Tax Code Evaluation: taxcode  :: ::"+taxCode );
				Log.customer.debug(":: :: Sum of line Tax Amounts from Vertex Response  :: :: "+headerLevelTaxAmtFromVertex);
				//Test the condition


				Log.customer.debug(":: :: is INVOICE ONLY tax amount from MSC EQUAL TO 0?:: :: "+amountFromMSC.compareTo(BigDecimal.ZERO) );
				Log.customer.debug(":: :: is tax amount from vertex greater than 0?:: :: "+taxAmountSum.compareTo(BigDecimal.ZERO) );
				if(amountFromMSC.compareTo(BigDecimal.ZERO)==0  && taxAmountSum.compareTo(BigDecimal.ZERO)==1 && taxCode.equals("B1"))

				{
					Log.customer.debug(" Change Tax Code to CC_B2 as Invoice Amount=0,Vertex Amount>0 and Tax Code=B1");
					String IRTaxCode = "B2";
					Log.customer.debug(" SAPCatTaxCustomApproverInv : TaxCode revised for IR Line Item "+IRTaxCode);
					Log.customer.debug(" SAPCatTaxCustomApproverInv : tax code that is now going to be revised ");
					Log.customer.debug("CC_B2 is not set..hence setting...");
					String qryString = "Select TaxCode,UniqueName, SAPTaxCode from ariba.tax.core.TaxCode where UniqueName  = '"+divisionNode+"_B2'";
					Log.customer.debug(":: :: qryString    :: :: "+qryString );
					AQLOptions queryOptions = new AQLOptions(partition);
					AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
					if(queryResults !=null)
					{
						Log.customer.debug(" :: ::  Query Results not null :: ::");
						while(queryResults.next())
						{

							TaxCode taxCodeObject = (TaxCode)queryResults.getBaseId(0).get();
							Log.customer.debug(" :: ::  tax code object being set  :: ::"+taxCodeObject );

							pli.setFieldValue("TaxCode", taxCodeObject);

						}
					}


				}//End of If for CC_B2

				else{

					BigDecimal HUNDRED = new BigDecimal("100");
					BigDecimal ONE = new BigDecimal("1");
					taxToleranceExceededException ="Tolerance exceeded. Total Tax Amount from Vertex exceeds the actual tax by 1%";


					//Negating the negative values for CM Fix
					BigDecimal amountFromVertex = new BigDecimal(headerLevelTaxAmtFromVertex);
					if(amountFromVertex.compareTo(BigDecimal.ZERO)==-1)  amountFromVertex = amountFromVertex.multiply(new BigDecimal(-1));
					//Negating the negative values for CM Fix
					if(amountFromMSC.compareTo(BigDecimal.ZERO)==-1)  amountFromMSC = amountFromMSC.multiply(new BigDecimal(-1));

					BigDecimal amountDifference = amountFromVertex.subtract(amountFromMSC);
					if(amountDifference.compareTo(BigDecimal.ZERO)==-1)  amountDifference = amountDifference.multiply(new BigDecimal(-1));
					Log.customer.debug(":: :: Different between tax amounts of request and response :: :: "+amountDifference );
					if(amountDifference.compareTo(BigDecimal.ZERO)==0 )
					{

						isTaxManagerRequired[0]=  "false";
						isTaxManagerRequired[1] = longstring ;


					}else

					{

						BigDecimal percent = ONE.divide(HUNDRED);
						Log.customer.debug(":: :: tolerance percent to be set :: :: "+percent);

						BigDecimal upperToleranceLimit = amountFromVertex.add(amountFromVertex.multiply(percent)).setScale(5,5);
						Log.customer.debug(":: :: UPPER tolerance Limit :: :: "+upperToleranceLimit);

						BigDecimal lowerToleranceLimit = amountFromVertex.subtract(amountFromVertex.multiply(percent)).setScale(5,5);
						Log.customer.debug(":: :: LOWER tolerance Limit :: :: "+lowerToleranceLimit);



						Log.customer.debug(":: ::  is MSC Tax amount greater than lower tolerance limit :: :: "+(lowerToleranceLimit.compareTo(amountFromMSC) == -1));
						Log.customer.debug(":: :: AND is Tax MSC amount lesser than upper tolerance limit  :: :: "+(upperToleranceLimit.compareTo(amountFromMSC) == 1));


						if ((lowerToleranceLimit.compareTo(amountFromMSC) == -1 && upperToleranceLimit.compareTo(amountFromMSC) == 1)||(lowerToleranceLimit.compareTo(amountFromMSC) == 0 && upperToleranceLimit.compareTo(amountFromMSC) == 1)||
								(lowerToleranceLimit.compareTo(amountFromMSC) == -1 && upperToleranceLimit.compareTo(amountFromMSC) == 0)) {


							isTaxManagerRequired[0]=  "false";
							isTaxManagerRequired[1] = longstring ;
						}
						else
						{
							Log.customer.debug(":: Setting AUTH PO TAX AMOUNT  ::"+taxAmountSum);
							lic.setFieldValue("AuthPOTaxAmt",authPOTaxAmount  );

							Log.customer.debug(":: :: Adding relevant tax exceptoins :: ::");
							isTaxManagerRequired[0]=  "true";
							isTaxManagerRequired[1] = longstring ;



						}
					}
				}//End of Else OF CC_B2
			}//End of the line item loop
			if(isTaxManagerRequired[0].toString().equals("true"))
			{
							Log.customer.debug(":: :: Under or Over Tolerance exception occurred!! Please add the Tax Manager !!  :: :: ");
							InvoiceExceptionType excType = InvoiceExceptionType.lookupByUniqueName("CATTaxCalculationFailed", partition);
							Log.customer.debug("%s ::: The created exception is: " + excType.getUniqueName());
							BaseVector exceptions = (invoicereconciliation).getExceptions();
							Log.customer.debug(":: :: Creating BaseVector of Exceptions object :: ::");
							InvoiceException newInvExc = InvoiceException.createFromTypeAndParent(excType, invoicereconciliation);
							Log.customer.debug(":: :: New Invoice Exception being created:: ::");
							exceptions.add(newInvExc);
							String taxReason = "Reason For Adding Tax Manager";
							longstring = new LongString(taxToleranceExceededException);
							User user1 = User.getAribaSystemUser(partition);
							addCommentToIR(ar, longstring, taxReason,  user1);



			}


			//Log.customer.debug("::Verification of update of AUTH PO TAX AMOUNT  ::"+lic.getFieldValue("AuthPOTaxAmount.Amount"));




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
	Method Name : addMSCAdminToIR
	Input Parameters: ApprovalRequest ar,Approvable lic
	Return Type: None

	This method adds the MSC Admin when there is vertex Login issue(Purely for login issues-incorrect user id/pwd

	*/


	public static void addMSCAdminToIR(ApprovalRequest ar,Approvable lic){
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
		basevector2.add(0, ar);
		Log.customer.debug("%s ::: ar added to basevector2 " );

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		ar.setState(2);
		Log.customer.debug("%s ::: ar.setState- " );

		ar.updateLastModified();
		Log.customer.debug("%s ::: ar.updatelastmodified- " );

		basevector1.removeAll(ar);
		Log.customer.debug("%s ::: basevecotr1 .removeall " );

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
	Input Parameters: ApprovalRequest ar,Approvable lic
	Return Type: None

	This method adds the Tax Manager when tolerance exceeds or when there is a data error

	*/


	public static void addTaxManagerToIR(ApprovalRequest ar,Approvable lic){
		Log.customer.debug("%s ::: addTaxManagerToIR - " + lic);
		String role = "TM";
		String TaxReason= "Tax Reason";

		boolean flag1 = true;

		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		Object obj = CatCommonUtil.getRoleforSplitterRuleForVertex(lic,role);

		Log.customer.debug("%s ::: isTaxManagerRequired - plic bfore create " + plic.toString());
		ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
		Log.customer.debug("%s ::: approvalrequest1 got activated- " );
		BaseVector basevector1 = plic.getApprovalRequests();
		Log.customer.debug("%s ::: basevector1 got activated- " );

		BaseVector basevector2 = approvalrequest1.getDependencies();
		Log.customer.debug("%s ::: basevector2 got activated- " );
		basevector2.add(0, ar);
		Log.customer.debug("%s ::: ar added to basevector2 " );

		approvalrequest1.setFieldValue("Dependencies", basevector2);
		ar.setState(2);
		Log.customer.debug("%s ::: ar.setState- " );

		ar.updateLastModified();
		Log.customer.debug("%s ::: ar.updatelastmodified- " );

		basevector1.removeAll(ar);
		Log.customer.debug("%s ::: basevecotr1 .removeall " );

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
	Method Name : addVertexManagerToIR
	Input Parameters: ApprovalRequest ar,Approvable lic
	Return Type: None

	This method adds the Vertex Manager Manager when there is vertex error or when vertex is down

	*/
	public static void addVertexManagerToIR(ApprovalRequest ar,Approvable lic){
		Log.customer.debug("%s ::: addVertexManagerToIR - " + lic);
		Object[] isTaxManagerRequired =  new Object[1];
		isTaxManagerRequired[0]=  "true";
		ProcureLineItemCollection plic = (ProcureLineItemCollection) lic;
		if(isTaxManagerRequired != null)
		{
			Log.customer.debug("%s ::: isTaxManagerRequired - " + isTaxManagerRequired);
			String approvalRequiredFlag = isTaxManagerRequired[0].toString();
			if(approvalRequiredFlag.equals("true"))
			{
				User user = User.getAribaSystemUser(ar.getPartition());
				String TaxRole = "VertexManager";

				String TaxReason= "Tax Reason";
				boolean flag1 = true;
				Object obj = Role.getRole(TaxRole);

				Log.customer.debug("%s ::: isTaxManagerRequired - plic bfore create " + plic.toString());
				ApprovalRequest approvalrequest1 = ApprovalRequest.create(plic, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
				Log.customer.debug("%s ::: approvalrequest1 got activated- " );

				BaseVector basevector1 = plic.getApprovalRequests();
				Log.customer.debug("%s ::: basevector1 got activated- " );

				BaseVector basevector2 = approvalrequest1.getDependencies();
				Log.customer.debug("%s ::: basevector2 got activated- " );

				basevector2.add(0, ar);
				Log.customer.debug("%s ::: ar added to basevector2 " );

				approvalrequest1.setFieldValue("Dependencies", basevector2);
				ar.setState(2);
				Log.customer.debug("%s ::: ar.setState- " );

				ar.updateLastModified();
				Log.customer.debug("%s ::: ar.updatelastmodified- " );

				basevector1.removeAll(ar);
				Log.customer.debug("%s ::: basevecotr1 .removeall " );

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


		}
	}

	/*
	Method Name : addCommentToIR
	Input Parameters: ApprovalRequest ar, LongString commentText, String commentTitle,  User commentUser
	Return Type: None

	This method adds the relevant comments in the comments section of the IR

	*/
	public static void addCommentToIR(ApprovalRequest ar, LongString commentText, String commentTitle, User commentUser){
		Comment comment = new Comment(ar.getPartition());
		comment.setType(Comment.TypeGeneral);
		comment.setText(commentText);
		comment.setTitle(commentTitle);
		comment.setDate(new ariba.util.core.Date());
		comment.setUser(commentUser);
		comment.setExternalComment(true);

		comment.setParent(ar);
		ar.getApprovable().getComments().add(comment);

	}

	/*
	 * public String getIcon(ApprovalRequest ar) { return super.getIcon(ar); }
	 */

	public SAPCatTaxUtil() {
	}

}





