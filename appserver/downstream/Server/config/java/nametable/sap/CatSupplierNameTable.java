/*************************************************************************************************
*   Created by: Santanu Dey
*
*   Requirement:
*	Filter supplier based on the companycode.
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
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.FastStringBuffer;
import ariba.util.log.Log;

public class CatSupplierNameTable extends AQLNameTable {
	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSupplierNameTable ()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();

		/*
		 * SDey Ariba, Inc : Do not cast SearchTerm to ProcureLineItemCollection.
		 */
		if(valSrc instanceof ariba.search.core.SearchTerm) {
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		/*
		 * SDey Ariba, Inc : Do not cast SearchTerm to ProcureLineItemCollection.
		 */

		ProcureLineItemCollection lic = (ProcureLineItemCollection)valSrc;
		FastStringBuffer partFunc = new FastStringBuffer ();
		if(lic == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Partition currentPartition = Base.getSession().getPartition();
		String comStr=null;
		String CompanyCodeFilteringStr = "Y";
		String PurchaseOrgFilteringStr = "N";
		String CompanyCodeFiltering = (String)lic.getDottedFieldValue("CompanyCode.CompanyCodeFiltering");
		String PurchaseOrgFiltering = (String)lic.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering");

		Log.customer.debug(" CatSupplierNameTable: PurchaseOrgFiltering : %s", PurchaseOrgFiltering);

		String CompanyCode = (String)lic.getDottedFieldValue("CompanyCode.UniqueName");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatSupplierNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		if(CompanyCodeFiltering != null){
			CompanyCodeFilteringStr = CompanyCodeFiltering;
		}

		if(PurchaseOrgFiltering != null){
			PurchaseOrgFilteringStr = PurchaseOrgFiltering;
		}

		String PurchaseOrg =(String)lic.getDottedFieldValue("PurchaseOrg.UniqueName");
		if (PurchaseOrgFilteringStr.equalsIgnoreCase("Y") && PurchaseOrg == null)
		{
			Log.customer.debug(" CatSupplierNameTable: PurchaseOrg is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		String CompanyDefaultPartnerFunction = (String)lic.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
		if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
		{
			StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
			while (st.hasMoreTokens())
			{
				String partnerFunc = st.nextToken();
				Log.customer.debug(" CatSupplierNameTable: partnerFunc is %s",partnerFunc);
				partFunc.append(",'"+ partnerFunc +"'");
			}
			Log.customer.debug(" CatSupplierNameTable: partnerFunc is %s",partFunc.toString());
			partFunc.removeCharAt(0);
			Log.customer.debug(" CatSupplierNameTable: partnerFunc is %s",partFunc.toString());
		}

		if (CompanyCodeFilteringStr.equalsIgnoreCase("Y"))
		{
			Log.customer.debug(" CatSupplierNameTable: CompanyCodeFilteringStr is Y ");
			if(PurchaseOrgFilteringStr.equalsIgnoreCase("Y")){

				Log.customer.debug(" CatSupplierNameTable: PurchaseOrgFilteringStr is Y ");

				qryString = "Select PorgSupplierCombo.Supplier, PorgSupplierCombo.Supplier.UniqueName, PorgSupplierCombo.Supplier.Name " +
					"from ariba.core.PorgSupplierCombo ,ariba.common.core.SupplierLocation AS SL " +
					"where PorgSupplierCombo.Supplier.Locations = SL AND PorgSupplierCombo.PurchaseOrg.UniqueName = " + "'" + PurchaseOrg + "' " +
					"AND PorgSupplierCombo.Supplier.CommonSupplier.OrganizationID.Ids.\"Domain\" = " + "'" + CompanyCode.toLowerCase() + "' " +
					"and (PorgSupplierCombo.Supplier.Locations.ContactID = '"+ CompanyCode +"') ";

				comStr = "PorgSupplierCombo.Supplier.Locations";

			Log.customer.debug("CompanyCodeFilteringStr : Y PurchaseOrgFilteringStr : Y : Query is : CatSupplierNameTable: %s", qryString);

			}else
			{
				qryString = "Select Supplier,Supplier.UniqueName,Supplier.Name " +
						" from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
						"where Locations = SupplierLocation AND CommonSupplier.OrganizationID.Ids.\"Domain\" = " + "'" + CompanyCode.toLowerCase() + "' " +
						"and Locations.ContactID = '" + CompanyCode + "'";
				comStr = "Locations";

				Log.customer.debug("CompanyCodeFilteringStr : Y PurchaseOrgFilteringStr : N : Query is : CatSupplierNameTable: %s", qryString);

			}

		}
		else
		{
			Log.customer.debug(" CatSupplierNameTable: CompanyCodeFilteringStr is N");
			if(PurchaseOrgFilteringStr.equalsIgnoreCase("Y")){

				Log.customer.debug(" CatSupplierNameTable: PurchaseOrgFilteringStr is Y ");

				qryString = "Select PorgSupplierCombo.Supplier, PorgSupplierCombo.Supplier.UniqueName, PorgSupplierCombo.Supplier.Name " +
					"from ariba.core.PorgSupplierCombo ,ariba.common.core.SupplierLocation AS SL " +
					"where PorgSupplierCombo.Supplier.Locations = SL AND PorgSupplierCombo.PurchaseOrg.UniqueName = " + "'" + PurchaseOrg + "' " ;
					//"AND PorgSupplierCombo.Supplier.CommonSupplier.OrganizationID.Ids.Domain = " + "'" + CompanyCode + "' " +
					//"and (PorgSupplierCombo.Supplier.Locations.ContactID = '"+ CompanyCode +"') ";

			comStr = "PorgSupplierCombo.Supplier.Locations";

			Log.customer.debug("CompanyCodeFilteringStr : N PurchaseOrgFilteringStr : Y : Query is : CatSupplierNameTable: %s", qryString);

			}else
			{
				qryString = "Select Supplier,Supplier.UniqueName,Supplier.Name " +
						"from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
						"where Locations = SupplierLocation " ;
						//"AND CommonSupplier.OrganizationID.Ids.Domain = " + "'" + CompanyCode + "' " +
						//"and Locations.ContactID = '" + CompanyCode + "'";
				comStr = "Locations";

				Log.customer.debug("CompanyCodeFilteringStr : N PurchaseOrgFilteringStr : N : Query is : CatSupplierNameTable: %s", qryString);

			}

		}

		if(lic.instanceOf("ariba.invoicing.core.Invoice")){

			qryString = "Select Supplier,Supplier.Name,Supplier.UniqueName " +
					" from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
					"where Locations = SupplierLocation AND CommonSupplier.OrganizationID.Ids.\"Domain\" = " + "'" + CompanyCode.toLowerCase() + "' " +
					"and Locations.ContactID = '" + CompanyCode + "'";
			comStr = "Locations";

			Log.customer.debug("lic instanceof ariba.invoicing.core.Invoice : Query is : CatSupplierNameTable: %s", qryString);
		}

		if(partFunc.length()>0){
			qryString = qryString + " AND "+comStr+".LocType in (" + partFunc.toString()+ ")";
		}

		Log.customer.debug(" CatSupplierNameTable: pattern"+pattern);
		Log.customer.debug(" CatSupplierNameTable: field"+field);
		Log.customer.debug(" CatSupplierNameTable: comStr"+comStr);

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

			Log.customer.debug("final query : CatSupplierNameTable: %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			query1.setDistinct(true);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(140);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}

}
