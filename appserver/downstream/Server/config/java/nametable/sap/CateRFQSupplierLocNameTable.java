/*************************************************************************************************
*   Created by: Nagendra
*
*   Requirement:
*   For eRFQ  Req, SupplierLocations available to the users for selection should be based on the
*   requestors CompanyCode that is part of the CommonSupplier Domain-Value pair.  The Domain of the
*   CommonSupplier should be the CompanyCode so that when selecting the list of Suppliers the Domain
*   can be matched to the requestor's CompanyCode and Location ContactID should be companycode
*
*************************************************************************************************/

package config.java.nametable.sap;

import java.util.List;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.FastStringBuffer;
import ariba.util.log.Log;

public class CateRFQSupplierLocNameTable extends AQLNameTable
{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CateRFQSupplierLocNameTable ()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		Requisition req = (Requisition)valSrc;
		FastStringBuffer partFunc = new FastStringBuffer ();
		if(req == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Partition currentPartition = Base.getSession().getPartition();
		String comStr=null;
		String CompanyCodeFilteringStr = "Y";
		String PurchaseOrgFilteringStr = "N";
		String CompanyCodeFiltering = (String)req.getDottedFieldValue("CompanyCode.CompanyCodeFiltering");
		String PurchaseOrgFiltering = (String)req.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering");

		Log.customer.debug(" CatNonCatalogSupplierLocNameTable: PurchaseOrgFiltering : %s", PurchaseOrgFiltering);

		String CompanyCode = (String)req.getDottedFieldValue("CompanyCode.UniqueName");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		if(CompanyCodeFiltering != null){
			CompanyCodeFilteringStr = CompanyCodeFiltering;
		}

		if(PurchaseOrgFiltering != null){
			PurchaseOrgFilteringStr = PurchaseOrgFiltering;
		}

		String PurchaseOrg =(String)req.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");
		if (PurchaseOrgFilteringStr.equalsIgnoreCase("Y") && PurchaseOrg == null)
		{
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: PurchaseOrg is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		String CompanyDefaultPartnerFunction = (String)req.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
		if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
		{
			StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
			while (st.hasMoreTokens())
			{
				String partnerFunc = st.nextToken();
				Log.customer.debug(" CatNonCatalogSupplierLocNameTable: partnerFunc is %s",partnerFunc);
				partFunc.append(",'"+ partnerFunc +"'");
			}
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: partnerFunc is %s",partFunc.toString());
			partFunc.removeCharAt(0);
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: partnerFunc is %s",partFunc.toString());
		}

		if (CompanyCodeFilteringStr.equalsIgnoreCase("Y"))
		{
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: CompanyCodeFilteringStr is Y ");
			if(PurchaseOrgFilteringStr.equalsIgnoreCase("Y")){

				Log.customer.debug(" CatNonCatalogSupplierLocNameTable: PurchaseOrgFilteringStr is Y ");

				qryString = "Select PorgSupplierCombo.Supplier.Locations, PorgSupplierCombo.Supplier.Locations.Name, PorgSupplierCombo.Supplier.Locations.UniqueName," +
					"PorgSupplierCombo.Supplier.Locations.PostalAddress.Lines,PorgSupplierCombo.Supplier.Locations.PostalAddress.City," +
					"PorgSupplierCombo.Supplier.Locations.PostalAddress.State,PorgSupplierCombo.Supplier.Locations.PostalAddress.Country.UniqueName " +
					"from ariba.core.PorgSupplierCombo ,ariba.common.core.SupplierLocation AS SL " +
					"where PorgSupplierCombo.Supplier.Locations = SL AND PorgSupplierCombo.PurchaseOrg.UniqueName = " + "'" + PurchaseOrg + "' " +
					"AND PorgSupplierCombo.Supplier.CommonSupplier.OrganizationID.Ids.\"Domain\" = " + "'" + CompanyCode.toLowerCase() + "' " +
					"and (PorgSupplierCombo.Supplier.Locations.ContactID = '"+ CompanyCode +"') ";

				comStr = "PorgSupplierCombo.Supplier.Locations";

			Log.customer.debug("CompanyCodeFilteringStr : Y PurchaseOrgFilteringStr : Y : Query is : CatNonCatalogSupplierLocNameTable: %s", qryString);

			}else
			{
				qryString = "Select Locations,Locations.Name,Locations.UniqueName," +
						"Locations.PostalAddress.Lines,Locations.PostalAddress.City,Locations.PostalAddress.State," +
						"Locations.PostalAddress.Country.UniqueName from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
						"where Locations = SupplierLocation AND CommonSupplier.OrganizationID.Ids.\"Domain\" = " + "'" + CompanyCode.toLowerCase() + "' " +
						"and Locations.ContactID = '" + CompanyCode + "'";
				comStr = "Locations";

				Log.customer.debug("CompanyCodeFilteringStr : Y PurchaseOrgFilteringStr : N : Query is : CatNonCatalogSupplierLocNameTable: %s", qryString);

			}

		}
		else
		{
			Log.customer.debug(" CatNonCatalogSupplierLocNameTable: CompanyCodeFilteringStr is N");
			if(PurchaseOrgFilteringStr.equalsIgnoreCase("Y")){

				Log.customer.debug(" CatNonCatalogSupplierLocNameTable: PurchaseOrgFilteringStr is Y ");

				qryString = "Select PorgSupplierCombo.Supplier.Locations, PorgSupplierCombo.Supplier.Locations.Name, PorgSupplierCombo.Supplier.Locations.UniqueName," +
					"PorgSupplierCombo.Supplier.Locations.PostalAddress.Lines,PorgSupplierCombo.Supplier.Locations.PostalAddress.City," +
					"PorgSupplierCombo.Supplier.Locations.PostalAddress.State,PorgSupplierCombo.Supplier.Locations.PostalAddress.Country.UniqueName " +
					"from ariba.core.PorgSupplierCombo ,ariba.common.core.SupplierLocation AS SL " +
					"where PorgSupplierCombo.Supplier.Locations = SL AND PorgSupplierCombo.PurchaseOrg.UniqueName = " + "'" + PurchaseOrg + "' " ;
					//"AND PorgSupplierCombo.Supplier.CommonSupplier.OrganizationID.Ids.Domain = " + "'" + CompanyCode + "' " +
					//"and (PorgSupplierCombo.Supplier.Locations.ContactID = '"+ CompanyCode +"') ";

			comStr = "PorgSupplierCombo.Supplier.Locations";

			Log.customer.debug("CompanyCodeFilteringStr : N PurchaseOrgFilteringStr : Y : Query is : CatNonCatalogSupplierLocNameTable: %s", qryString);

			}else
			{
				qryString = "Select Locations,Locations.Name,Locations.UniqueName," +
						"Locations.PostalAddress.Lines,Locations.PostalAddress.City,Locations.PostalAddress.State," +
						"Locations.PostalAddress.Country.UniqueName from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
						"where Locations = SupplierLocation " ;
						//"AND CommonSupplier.OrganizationID.Ids.Domain = " + "'" + CompanyCode + "' " +
						//"and Locations.ContactID = '" + CompanyCode + "'";
				comStr = "Locations";

				Log.customer.debug("CompanyCodeFilteringStr : N PurchaseOrgFilteringStr : N : Query is : CatNonCatalogSupplierLocNameTable: %s", qryString);

			}

		}

		if(partFunc.length()>0){
			qryString = qryString + " AND "+comStr+".LocType in (" + partFunc.toString()+ ")";
		}

		Log.customer.debug(" CatNonCatalogSupplierLocNameTable: pattern"+pattern);
		Log.customer.debug(" CatNonCatalogSupplierLocNameTable: field"+field);
		Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
			{
				String pattern1 =  pattern.substring(1,pattern.length()-1);
				/*
				if (field.equals("Name"))
				{
					qryString = qryString + " AND "+comStr+".Name like '%" + pattern1 + "%'";
				}
				else if(field.equals("UniqueName"))
				{
					if(comStr.startsWith("Supplier") || comStr.startsWith("Locations"))
						qryString = qryString + " AND Supplier.UniqueName like '%" + pattern1 + "%'";
					else if(comStr.startsWith("PorgSupplierCombo"))
						qryString = qryString + " AND PorgSupplierCombo.Supplier.UniqueName like '%" + pattern1 + "%'";
					else
						qryString = qryString + " AND Location.UniqueName like '%" + pattern1 + "%'";
				}
				else if(field.equals("PreferredOrderingMethod"))
				{
					qryString = qryString + " AND "+comStr+".PreferredOrderingMethod like '%" + pattern1 + "%'";
				}
				else {
				*/
					qryString = qryString + " AND "+comStr+"."+field+" like '%" + pattern1 + "%'";
				//}

			}
			qryString = qryString +" order by "+comStr+".Name";

			Log.customer.debug("final query : CatNonCatalogSupplierLocNameTable: %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(140);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}
}