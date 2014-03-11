/* Requirement:
Introduce tax logic in requisition to default the tax code in Catalog and Non-Catalog Item:

Create History:
*   Create By        Create Date
*   Santanu Dey   	 26th Nov 08
Modification History:
*  Soumya Mohanty	31st Oct 11 
Requirement: Added the Tax Code Determination Logic for Line Item on Req and Contract
* Soumya Mohanty 	21-02-2012
Requirement: Added additional null check in order to handle the nullability of TaxCode during Cross Company Buying found in MSC-Mach1 3.0 Production Move
********************************************************************************************************************/

package config.java.action.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import java.util.List;
import java.lang.Integer;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.ActionExecutionException;
import ariba.common.core.PartitionedCommodityCode;
import ariba.purchasing.core.Requisition;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.Contract;
import ariba.tax.core.TaxCode;
import ariba.basic.core.Country;
import ariba.base.core.aql.*;
import ariba.common.core.Address;
import ariba.base.core.*;
import ariba.util.core.StringUtil;


public class CatSetTaxCodeFromCatalog extends Action{

	private static final String StringTable = "cat.aml.picklistvalues1";

	public void fire(ValueSource Object, PropertyTable params)
	 {
	  Log.customer.debug("CatSetTaxCodeFromCatalog: Fire started with object " + Object);
	  try
	  {

		  	ProcureLineItem lineItem=null;
		  	if(Object instanceof ReqLineItem || Object instanceof ContractRequestLineItem)
		  	{
		  		lineItem = (ProcureLineItem)Object;
		  	}
		  	Log.customer.debug("CatSetTaxCodeFromCatalog:lineItems = " + lineItem);

		  	if(lineItem != null)
	         {
		  	LineItemProductDescription des = null;
		  	String taxrate=null;
		  	String compcode =null;
		  	BaseObject taxcodeformaterials =null;
		  	BaseObject taxcodeforservices = null;
		  	Address ra= null;
		  	String  country =null;
		  	String sapsource=null;
		  	Partition partition=null;
		  	ariba.common.core.User user=null;
	  		ProcureLineItemCollection req = (ProcureLineItemCollection)lineItem.getLineItemCollection();

		    if(req != null)
			    {
				if( isTaxCodeSet(Object) == true )
				{
					Log.customer.debug("CatSetTaxCodeFromCatalog: Tax Code is Set by TaxCode Determination Logic");
					
				}
				
				else
				{			
				Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Has Returned false, 92");
			    Log.customer.debug("CatSetTaxCodeFromCatalog: Requisition = " + req.getUniqueName());
				compcode = (String)req.getDottedFieldValue("CompanyCode.UniqueName");
			    Log.customer.debug("CatSetTaxCodeFromCatalog: CompanyCode = " + compcode);
			    taxcodeformaterials =(BaseObject)req.getDottedFieldValue("CompanyCode.DefaultTaxCodeForMaterial");
				Log.customer.debug("CatSetTaxCodeFromCatalog: DefaultTaxCodeForMaterial = " + taxcodeformaterials );
			    taxcodeforservices =(BaseObject)req.getDottedFieldValue("CompanyCode.DefaultTaxCodeForService");
				Log.customer.debug("CatSetTaxCodeFromCatalog: DefaultTaxCodeForService = " + taxcodeforservices);
				ra=(Address)req.getDottedFieldValue("CompanyCode.RegisteredAddress");
				Log.customer.debug("CatSetTaxCodeFromCatalog: RegisteredAddress= " + ra);
				if (ra != null)
				{
					country = (String)ra.getDottedFieldValue("Country.UniqueName");
			    	Log.customer.debug("CatSetTaxCodeFromCatalog: Country.UniqueName = " + country );
			    	sapsource =(String)req.getDottedFieldValue("CompanyCode.SAPSource");
					Log.customer.debug("CatSetTaxCodeFromCatalog:SAPSource =" + sapsource );
					partition= (Partition)req.getPartition();
					Log.customer.debug("CatSetTaxCodeFromCatalog: Partition = " + partition);
				}
				}
				}
				else
			    {
		       		User shareduser = User.getEffectiveUser();
		       		Log.customer.debug("CatSetTaxCodeFromCatalog : shareduser is " + shareduser);
					Partition userPartition = Base.getSession().getPartition();
					if (!userPartition.equals(Partition.None))
						user = ariba.common.core.User.getPartitionedUser(shareduser,Base.getSession().getPartition());
			    	Log.customer.debug("CatSetTaxCodeFromCatalog : user is " + user);
			    	if (user != null)
			    	{
			    	compcode = (String)user.getDottedFieldValue("CompanyCode.UniqueName");
				    Log.customer.debug("CatSetTaxCodeFromCatalog: CompanyCode = " + compcode);
				    taxcodeformaterials =(BaseObject)user.getDottedFieldValue("CompanyCode.DefaultTaxCodeForMaterial");
					Log.customer.debug("CatSetTaxCodeFromCatalog: DefaultTaxCodeForMaterial = " + taxcodeformaterials );
				    taxcodeforservices =(BaseObject)user.getDottedFieldValue("CompanyCode.DefaultTaxCodeForService");
					Log.customer.debug("CatSetTaxCodeFromCatalog: DefaultTaxCodeForService = " + taxcodeforservices);
					ra=(Address)user.getDottedFieldValue("CompanyCode.RegisteredAddress");
					Log.customer.debug("CatSetTaxCodeFromCatalog: RegisteredAddress= " + ra);
					if (ra != null)
					{
						country = (String)ra.getDottedFieldValue("Country.UniqueName");
				    	Log.customer.debug("CatSetTaxCodeFromCatalog: Country.UniqueName = " + country );
				    	sapsource =(String)user.getDottedFieldValue("CompanyCode.SAPSource");
						Log.customer.debug("CatSetTaxCodeFromCatalog:SAPSource =" + sapsource );
						partition= (Partition)user.getPartition();
						Log.customer.debug("CatSetTaxCodeFromCatalog: Partition = " + partition);
					}
					}
			    }
				des = (LineItemProductDescription)lineItem.getDottedFieldValue("Description");
				Log.customer.debug("LineItems: ProductDescription = " + des);
				if (des!=null)
				{
				taxrate=(String)des.getDottedFieldValue("ManURL");
				//String taxrate =(String)des.getFieldValue("Taxrate");
				Log.customer.debug("CatSetTaxCodeFromCatalog: Taxrate = " + taxrate);
				}
				if(StringUtil.nullOrEmptyOrBlankString(taxrate))
				{
					taxrate = null;
				}else{
					if(taxrate.toUpperCase().startsWith("TAX:")){
						taxrate = taxrate.substring(4).trim();
					}
				}
				AQLOptions options = new ariba.base.core.aql.AQLOptions(partition);
				//AUL, sdey : changed the class name for TaxCode
				String qrycatalogtaxrate="select UniqueName,this,TaxRate from ariba.tax.core.TaxCode where TaxRate ="+taxrate+" and Country.UniqueName like '"+country+"' and SAPSource like '"+sapsource+"' order by 1";
				String qrycatalogtaxcode="select UniqueName,this,TaxRate from ariba.tax.core.TaxCode where UniqueName like '" +  taxrate + "'and Country.UniqueName like '"+country+"' and SAPSource like '"+sapsource+"' order by 1";

				//Boolean isadhoc = false;
				Boolean isadhoc = (Boolean) lineItem.getDottedFieldValue("IsAdHoc");
				Log.customer.debug("CatSetTaxCodeFromCatalog:IsAdHoc = " +isadhoc);
				//Boolean isservice = (Boolean) lineItem.getDottedFieldValue("IsService");
				//Log.customer.debug("UnileverSetTaxCode:IsService = " +isservice);
				if(isadhoc !=null && isadhoc.booleanValue() == false)
				{
			         if(taxrate != null && country != null && sapsource != null)
			         {

					if( isAlphaNumeric(taxrate) == true )
					{
						Log.customer.debug("CatSetTaxCodeFromCatalog:  isAlphaNumeric : true ");
					AQLQuery query = null;
			        try
			        {

					 query = AQLQuery.parseQuery(qrycatalogtaxcode);
					 Log.customer.debug("CatSetTaxCodeFromCatalog:  Query : "+query);
			        }
			        catch ( Exception e)
			        {
						Log.customer.debug("CatSetTaxCodeFromCatalog:  AQLQuery.parseQuery(qrycatalogtaxcode)" + e);
			        }
				    AQLResultCollection results = (AQLResultCollection)Base.getService().executeQuery(query , options);
					Log.customer.debug("CatSetTaxCodeFromCatalog:  AQLResultCollection= " + results);
					while (results.next())
					         {
								BaseId  tc = (BaseId) results.getBaseId(1);
								Log.customer.debug("CatSetTaxCodeFromCatalog : BaseId = " + tc);
								BaseObject taxcode = Base.getSession().objectIfAny(tc);
								Log.customer.debug("CatSetTaxCodeFromCatalog : BaseObject= " + taxcode );
								if (taxcode!= null)
								{
									lineItem.setDottedFieldValue("TaxCode",taxcode);
									Log.customer.debug("CatSetTaxCodeFromCatalog:TaxCode = " +taxcode);
									return;
								}

			         }

					}
					else
					{
						AQLQuery query = null;
						try
						{
					       query = AQLQuery.parseQuery(qrycatalogtaxrate);
						}
						catch ( Exception e)
						{
							Log.customer.debug("CatSetTaxCodeFromCatalog:  AQLQuery.parseQuery(qrycatalogtaxrate)" + e);
						}
					AQLResultCollection results = (AQLResultCollection)Base.getService().executeQuery(query , options);
					Log.customer.debug("CatSetTaxCodeFromCatalog:  AQLResultCollection= " + results);
					while (results.next())
					         {
								BaseId  tc = (BaseId) results.getBaseId(1);
								Log.customer.debug("BaseId = " + tc);
								BaseObject taxcode = Base.getSession().objectIfAny(tc);
								Log.customer.debug("BaseObject= " + taxcode );
								if (taxcode!= null)
								{
									lineItem.setDottedFieldValueWithoutTriggering("TaxCode",taxcode);
									Log.customer.debug("CatSetTaxCodeFromCatalog:TaxCode = " +lineItem.getDottedFieldValue("TaxCode"));
									return;
								}

			         }

					}
			        }
					}
			// If not in catalog

				String lineType = (String) lineItem.getDottedFieldValue("LineItemType");

			    Log.customer.debug("CatSetTaxCodeFromCatalog:lineType = " +lineType);
			    String service = (String)ResourceService.getString(StringTable, "TaxQualifier2");

			    if(lineType != null && lineType.equals(service))
			   {
			    	if(taxcodeforservices != null)
			    	{
							lineItem.setDottedFieldValueWithoutTriggering("TaxCode",taxcodeforservices);
							Log.customer.debug("CatSetTaxCodeFromCatalog:TaxCode = " +taxcodeforservices);
							return;
			    	}
				}
			    else
			     {
			    	if(taxcodeformaterials != null)
			    	{
							lineItem.setDottedFieldValueWithoutTriggering("TaxCode",taxcodeformaterials);
							Log.customer.debug("CatSetTaxCodeFromCatalog:TaxCode = " +taxcodeformaterials);
							return;
			    	}
			     }
			 }
	         else
			 {
				 Log.customer.debug("CatSetTaxCodeFromCatalog: LineItem is null ");
				 return;
			 }


	  }catch(Exception e)
		{
	      Log.customer.debug("CatSetTaxCodeFromCatalog: Exception " + e.toString());
		}
}



	public boolean isAlphaNumeric(final String s) {
	  final char[] chars = s.toCharArray();
	  for (int x = 0; x < chars.length; x++) {
	    final char c = chars[x];
	    if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')))
	    return true;
	  }
	  return false;
	}
	
	public boolean isTaxCodeSet(ValueSource object) 
	{
	// Mdified By Soumya
		if ( object instanceof ReqLineItem )
		{
			Log.customer.debug(" CatMSCTaxCodeDetermination : Logic for ReqLineItem STARTED.");
			String companyCode = "";
			String companyCodeCountry = "";
			Country companyCodeCountryobj = null;
			String checkCallToVertex = "";
			String shipTo_state = "";
			String shipTo_country = "";
			String sapTaxCodeForLineItem = "";
			ReqLineItem rli = (ReqLineItem)object;
			Requisition requisition = (Requisition)rli.getLineItemCollection();
			if (rli !=null)
			{
				Log.customer.debug(" CatMSCTaxCodeDetermination : ReqLineItem object is not null.");
				if(requisition.getDottedFieldValue("CompanyCode.UniqueName") != null)
				{
					companyCode = (String) requisition.getDottedFieldValue("CompanyCode.UniqueName");
					Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION object Company Code Value =" + companyCode);
					if(requisition.getDottedFieldValue("CompanyCode.CallToVertexEnabled") != null)
					{
						checkCallToVertex = (String) requisition.getDottedFieldValue("CompanyCode.CallToVertexEnabled");
						Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION object Company Code CalltoVertexEnabled Value =" + checkCallToVertex);
						if(requisition.getDottedFieldValue("CompanyCode.RegisteredAddress.Country") != null)
						{
							companyCodeCountry = (String) requisition.getDottedFieldValue("CompanyCode.RegisteredAddress.Country.UniqueName");
							companyCodeCountryobj = (Country) requisition.getDottedFieldValue("CompanyCode.RegisteredAddress.Country");
						}
						else
						{
							companyCodeCountry = "";
						}
						
						Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION object Company Code Country Value =" + companyCodeCountry);				
						String formattedString = "";
						if(rli.getDottedFieldValue("ShipTo.State") != null)
						{
							 shipTo_state = (String) rli.getDottedFieldValue("ShipTo.State");
						}
						if(rli.getDottedFieldValue("ShipTo.Country.UniqueName") != null)
						{
							shipTo_country = (String) rli.getDottedFieldValue("ShipTo.Country.UniqueName");
						}
						Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION lineitem ShipTo State and Country "+ shipTo_state +";"+shipTo_country);
						if (rli.getDottedFieldValue("TaxCode.SAPTaxCode") != null)
						{
							sapTaxCodeForLineItem = (String) rli.getDottedFieldValue("TaxCode.SAPTaxCode");
						}
						Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: SAPTAXCODEFOR LINE ITEM = " + sapTaxCodeForLineItem);
						if(checkCallToVertex !=null)
						{
							if(checkCallToVertex.equals("PR") || checkCallToVertex.equals("IR") || checkCallToVertex.equals("PIB"))
							{
								// if(sapTaxCodeForLineItem !=null && !(sapTaxCodeForLineItem.equalsIgnoreCase("B5")))
								// if(sapTaxCodeForLineItem !=null)
								// Added by Soumya for checking Null as well as Blank SAPTaxCode For Line Item. Restricts bypassing of blank value.
								// Dt: 21-02-2012
								if(!StringUtil.nullOrEmptyOrBlankString(sapTaxCodeForLineItem))
								{
									if(rli.getFieldValue("MasterAgreement") != null && sapTaxCodeForLineItem.equalsIgnoreCase("B5"))
									{
										Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Return True, 323");
										return true; // As this will only be satisfied for Released Contract thats a fuel item. Hence no role of Tax Code Determination logic.
									}
									else if (companyCodeCountry.equalsIgnoreCase("US") && !(shipTo_country.equalsIgnoreCase("US")))
									{
										// if(!(sapTaxCodeForLineItem.equalsIgnoreCase("B5")))
										// {
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: companyCodeCountry is US and ShipTo Country is Non-US");
											Object[] taxCodelookupKeys = new Object[2];
											taxCodelookupKeys[0] = "B5";
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: TaxCode from the lookup "+taxCodelookupKeys[0]);
											
											taxCodelookupKeys[1] =  companyCodeCountryobj;
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Country " + shipTo_country);
											ClusterRoot taxcode = (ClusterRoot) Base.getSession().objectFromLookupKeys(taxCodelookupKeys, "ariba.tax.core.TaxCode", requisition.getPartition());					
											// Set the Value of LineItem.TaxCode.UniqueName = 'B5'
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Setting TaxCode to B5 as Foreign");
											rli.setDottedFieldValueWithoutTriggering("TaxCode", taxcode);
											// Set the Value of LineItem.TaxCode.SAPTaxCode = 'B5'
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Setting TaxCode to B5 due to Foreign ShipTo, COMPLETED");
											Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Return True, 343");
											return true;	
										// }
										// return true;
									}
									else
									{
										formattedString = companyCode + "_" + shipTo_state;
										String qryString = "Select TaxCode,UniqueName, SAPTaxCode from ariba.tax.core.TaxCode where UniqueName  = '"+ formattedString +"' and Country.UniqueName ='"+  rli.getDottedFieldValue("ShipTo.Country.UniqueName") + "'";
										Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: qryString "+qryString);
										
										AQLOptions queryOptions = new AQLOptions(requisition.getPartition());
										Log.customer.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage I");
										AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
										Log.customer.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage II- Query Executed");
										if(queryResults !=null)
										{
											Log.customer.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage III - Query Results not null");
											while(queryResults.next()) 
											{
												Log.customer.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage IV - Entering the DO of DO-WHILE");
												TaxCode taxfromLookupvalue = (TaxCode)queryResults.getBaseId(0).get();
												Log.customer.debug(" CatMSCTaxCodeDetermination: REQUISITION - Stage IV - Entering the DO of DO-WHILE"+ taxfromLookupvalue);
												
												Log.customer.debug(" taxfromLookupvalue"+taxfromLookupvalue);
												Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: TaxCodefromLookup" + taxfromLookupvalue);
												// Set the Value of LineItem.TaxCode.UniqueName = 'formattedString'
												Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Setting TaxCodefromLookup" + taxfromLookupvalue);
												// Additional null check performed as part of Prod Issue - For Cross Company Buying - Soumya
												// Dt: 21-02-2012
												if(rli.getFieldValue("TaxCode") != null && !(((String) rli.getDottedFieldValue("TaxCode.UniqueName")).equalsIgnoreCase((String) taxfromLookupvalue.getDottedFieldValue("TaxCode.UniqueName"))))
												{
													rli.setDottedFieldValueWithoutTriggering("TaxCode", taxfromLookupvalue);																		
													Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Applied " + taxfromLookupvalue);
													Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Applied Tax Code = " + (String) rli.getDottedFieldValue("TaxCode.UniqueName"));
													Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Return True, 375");
													return true;
												}
												
												else
												{
													Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Not Applied ");
												}
												Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Tax From Line Item = " + rli.getDottedFieldValue("TaxCode.UniqueName"));
											}							
										}
									}
								}
								Log.customer.debug(" CatMSCTaxCodeDetermination : REQUISITION: Return False, 388");
								return false;
							}
						}
					}	// END - CallToVertexEnabled null Check
				} 	// END - CompanyCode null Check
			} 	// END - rli null check
					
		}
		else if (object instanceof ContractRequestLineItem)
		{
			Log.customer.debug(" CatMSCTaxCodeDetermination : Logic for CONTRACT STARTED.");
			String companyCode = "";
			String companyCodeCountry = "";
			String checkCallToVertex = "";
			String shipTo_state = "";
			String shipTo_country = "";
			String sapTaxCodeForLineItem = "";
			// String cntrReqRelType = "";
			// Field to hold Header Level ShipTo - To default the Headerlevel ShipTo Value to line level
			Address headerShipTo = null;
			Country companyCodeCountryobj = null; 
			ContractRequestLineItem rli = (ContractRequestLineItem)object;
			ContractRequest cntrctrequest = null;
			if(rli !=null)
			{
				cntrctrequest = (ContractRequest)rli.getLineItemCollection();
				//String cntrReq = (String)cntrctrequest.getFieldValue("ReleaseType");
				// Integer.valueOf(cntrctrequest.getFieldValue("ReleaseType").toString());
				// Commented by Soumya to include both Release and Non-Release Type Contract under TaxCode Determination
				// Log.customer.debug(" CatMSCTaxCodeDetermination : ReleaseType = " + cntrctrequest.getFieldValue("ReleaseType"));
				// if(Integer.valueOf(cntrctrequest.getFieldValue("ReleaseType").toString()) != 1)	
				// {				
					Log.customer.debug(" CatMSCTaxCodeDetermination : ContractRequestLineItem object is not null.");
					if(cntrctrequest.getDottedFieldValue("CompanyCode.UniqueName") != null)
					{
						companyCode = (String) cntrctrequest.getDottedFieldValue("CompanyCode.UniqueName");
						Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT object Company Code Value =" + companyCode);
						if(cntrctrequest.getDottedFieldValue("CompanyCode.CallToVertexEnabled") != null)
						{
							checkCallToVertex = (String) cntrctrequest.getDottedFieldValue("CompanyCode.CallToVertexEnabled");
						}
						else
						{
							checkCallToVertex = "";
						}
						Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT object Company Code CalltoVertexEnabled Value =" + checkCallToVertex);
						if(cntrctrequest.getDottedFieldValue("CompanyCode.RegisteredAddress.Country") != null)
						{
							companyCodeCountry = (String) cntrctrequest.getDottedFieldValue("CompanyCode.RegisteredAddress.Country.UniqueName");
							Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT object Company Code Country Value =" + companyCodeCountry);
							companyCodeCountryobj = (Country) cntrctrequest.getDottedFieldValue("CompanyCode.RegisteredAddress.Country");		
						}
						// To default the Headerlevel ShipTo Value to line level for Contract
						if((Address)cntrctrequest.getFieldValue("ContractShipTo") != null)
						{
							headerShipTo = (Address)cntrctrequest.getFieldValue("ContractShipTo");
						}
						Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT object Header Shipto Value =" + headerShipTo);
						if (headerShipTo != null)
						{
							// To set the default Headerlevel ShipTo Value to line level for Contract
							rli.setFieldValue("ShipTo", headerShipTo);
							Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT object Header Shipto Value Applied on Line item = " + headerShipTo);
						}
						String formattedString = "";	
						if(rli.getDottedFieldValue("ShipTo.State") != null)
						{
							 shipTo_state = (String) rli.getDottedFieldValue("ShipTo.State");
						}
						if(rli.getDottedFieldValue("ShipTo.Country.UniqueName") != null)
						{
							shipTo_country = (String) rli.getDottedFieldValue("ShipTo.Country.UniqueName");
						}
						Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT lineitem ShipTo State and Country "+ shipTo_state +";"+shipTo_country);
						if (rli.getDottedFieldValue("TaxCode.SAPTaxCode") != null)
						{
							sapTaxCodeForLineItem = (String) rli.getDottedFieldValue("TaxCode.SAPTaxCode");
						}
						
						Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: SAPTAXCODEFOR LINE ITEM = " + sapTaxCodeForLineItem);
						if(checkCallToVertex !=null)
						{
							if(checkCallToVertex.equals("PR") || checkCallToVertex.equals("IR") || checkCallToVertex.equals("PIB"))
							{
								// if(sapTaxCodeForLineItem !=null && !(sapTaxCodeForLineItem.equalsIgnoreCase("B5")))
								// Added by Soumya for checking Null as well as Blank SAPTaxCode For Line Item. Restricts bypassing of blank value.
								// Dt: 21-02-2012
								if(!StringUtil.nullOrEmptyOrBlankString(sapTaxCodeForLineItem) && !(sapTaxCodeForLineItem.equalsIgnoreCase("B5")))
								{
									if (companyCodeCountry.equalsIgnoreCase("US") && !(shipTo_country.equalsIgnoreCase("US")))
									{
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: companyCodeCountry is US and ShipTo Country is Non-US");
										Object[] taxCodelookupKeys = new Object[2];
										taxCodelookupKeys[0] = "B5";
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: TaxCode from the lookup "+taxCodelookupKeys[0]);
										
										taxCodelookupKeys[1] =  companyCodeCountryobj;
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Country " + shipTo_country);
										ClusterRoot taxcode = (ClusterRoot) Base.getSession().objectFromLookupKeys(taxCodelookupKeys, "ariba.tax.core.TaxCode", cntrctrequest.getPartition());
									
										// Set the Value of LineItem.TaxCode.UniqueName = 'B5'
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Setting TaxCode to B5 as Foreign");
										rli.setDottedFieldValueWithoutTriggering("TaxCode", taxcode);
										// Set the Value of LineItem.TaxCode.SAPTaxCode = 'B5'
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Setting TaxCode to B5 due to Foreign ShipTo, COMPLETED");
										return true;						
									}
									else
									{
										formattedString = companyCode + "_" + shipTo_state;
										String qryString = "Select TaxCode,UniqueName, SAPTaxCode from ariba.tax.core.TaxCode where UniqueName  = '"+ formattedString +"' and Country.UniqueName ='"+ (String) rli.getDottedFieldValue("ShipTo.Country.UniqueName") + "'";
										Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: qryString "+qryString);

										AQLOptions queryOptions = new AQLOptions(cntrctrequest.getPartition());
										Log.customer.debug(" CatMSCTaxCodeDetermination: CONTRACT - Stage I");
										AQLResultCollection queryResults = Base.getService().executeQuery(qryString, queryOptions);
										Log.customer.debug(" CatMSCTaxCodeDetermination: CONTRACT - Stage II- Query Executed");
										boolean checkWhile = false;
										if(queryResults !=null)
										{
											Log.customer.debug(" CatMSCTaxCodeDetermination: CONTRACT - Stage III - Query Results not null");
											while(queryResults.next()) 
											{
												Log.customer.debug(" CatMSCTaxCodeDetermination: CONTRACT - Stage IV - Entering the DO of DO-WHILE");
												Log.customer.debug(" CatMSCTaxCodeDetermination: CONTRACT - Stage IV - Entering the DO of DO-WHILE"+queryResults.getBaseId(0).get());
												TaxCode taxfromLookupvalue = (TaxCode)queryResults.getBaseId(0).get();
												Log.customer.debug(" taxfromLookupvalue"+taxfromLookupvalue);
												Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: TaxCodefromLookup" + taxfromLookupvalue);
												// Set the Value of LineItem.TaxCode.UniqueName = 'formattedString'
												Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Setting TaxCodefromLookup" + taxfromLookupvalue);
																			
												// Set the Value of LineItem.TaxCode.UniqueName
												rli.setDottedFieldValueWithoutTriggering("TaxCode", taxfromLookupvalue);
																						
												Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Applied " + taxfromLookupvalue);
												Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: Tax From Line Item = " + rli.getDottedFieldValue("TaxCode.UniqueName"));
												checkWhile = true;
											}
											if(checkWhile)
											{
												Log.customer.debug(" CatMSCTaxCodeDetermination : CONTRACT: While Returned true");
												return true;								
											}
										}							
									}
								}
								return false;					
							}
							return false;
						}
						return false;
					}
					return false;
				// }
				// return false;
			}
			return false;									
		}
		return false;
		// Modified End By Soumya
	}
}
