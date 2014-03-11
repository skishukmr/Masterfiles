/*******************************************************************************************************************************************
ChangeLog :
//updated by Aswini on 12/07/2011 for PCL logic added UniqueName in the query string
Date          Issue       Name           Description 

30/08/2012     322        Manoj.R        Increase the requisition Ship-to search result set limit from 100 entries to 500
******************************************************************************************************************************************/
package config.java.nametable.sap;

import java.util.List;
import java.util.Locale;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractRequest;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;

public class CatSAPShipToNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSAPShipToNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{

		ValueSource valSrc = getValueSourceContext();
		Log.customer.debug(" CatSAPShipToNameTable: valSrc " +valSrc);
		LineItemCollection lic = null;
		ProcureLineItem li = null;


		if(valSrc instanceof ContractRequest)
		{
			Log.customer.debug(" CatSAPShipToNameTable: Within MasterAgreementRequest " +valSrc);
			lic = (ContractRequest)valSrc;
		}
		else if( valSrc instanceof Contract)
		{
			Log.customer.debug(" CatSAPShipToNameTable: Within MasterAgreement " +valSrc);
			lic = (Contract)valSrc;
		}
		else
		{
			try{
		 		li = (ProcureLineItem)valSrc;
		 		lic = li.getLineItemCollection();
			}
			catch(Exception e){
				return super.matchPattern(field, pattern,searchTermQuery);
			}
		}

		if(lic == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		Partition currentPartition = Base.getSession().getPartition();
		Locale userLocale = (Locale)Base.getSession().getLocale();

		BaseObject CompanyCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");


		if (CompanyCode == null)
		{
			Log.customer.debug(" CatSAPShipToNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}


		String CompanyCodeID = (String) CompanyCode.getDottedFieldValue("UniqueName");

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPShipToNameTable: CCSAPSource"+CCSAPSource);
        
		// Added A.UniqueName in the query for PCL		
		qryString = "Select A, A.UniqueName, A.Name, A.PostalAddress.Lines, A.CompanyCode.UniqueName  from ariba.common.core.Address as A  " +
				"where A.CompanyCode.UniqueName  = '"+ CompanyCodeID +"' and A.Creator is null " ;
		Log.customer.debug(" CatSAPShipToNameTable: qryString =>"+qryString);


		String shrdUserUniqueName = null;
		String preparedAddress = null;
		String shrdUserBaseID = null;
		String shrdUserBaseIDStr= null;
		ariba.user.core.User shrdUser = (ariba.user.core.User) lic.getPreparer();




		if(shrdUser!=null){
			Log.customer.debug(" CatSAPShipToNameTable: shrdUser => "+shrdUser);
			shrdUserUniqueName = shrdUser.getUniqueName();
			Log.customer.debug(" CatSAPShipToNameTable: shrdUserUniqueName => "+shrdUserUniqueName);
			shrdUserBaseID = (String) shrdUser.getDottedFieldValue("BaseId").toString() ;
			Log.customer.debug(" CatSAPShipToNameTable: shrdUserBaseID => "+shrdUserBaseID);
			if(shrdUserBaseID.lastIndexOf(" ") > 0)
			{
				// AUL, sdey : changed the method as shrdUserBaseIDStr was returning just empty string.
				//shrdUserBaseIDStr = (shrdUserBaseID.substring(shrdUserBaseID.lastIndexOf(" "),(shrdUserBaseID.length()-1))).trim();
				shrdUserBaseIDStr = (String) shrdUser.getBaseId().toDBString();
				Log.customer.debug(" CatSAPShipToNameTable: shrdUserBaseIDStr => "+shrdUserBaseIDStr);
			}

			Log.customer.debug(" CatSAPShipToNameTable: shrdUserBaseIDStr => "+shrdUserBaseIDStr);

			preparedAddress = " OR ( A.Creator = BaseID('" + shrdUserBaseIDStr + "')  AND A.CompanyCode.UniqueName = '" +
			CompanyCodeID+ "' )";

			qryString = qryString + preparedAddress;
			Log.customer.debug(" CatSAPShipToNameTable: qryString =>"+qryString);
		}



		Log.customer.debug(" CatSAPShipToNameTable: qryString"+qryString);

		Log.customer.debug(" CatSAPShipToNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPShipToNameTable: field"+field);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by Name";

			Log.customer.debug("CatSAPShipToNameTable: Final qryString =>"+qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(500); // WI 322 Query set limit changed to 500 from 100.
			options.setUserLocale(userLocale);
			options.setUserPartition(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}

}