/*************************************************************************************************
*   Created by: Santanu Dey
*
*   Updated by Amit:
*
*26/10/2010 Amit : 1119-Remove the filter of CrossCompanyBuyEnabled for Master agreements
*************************************************************************************************/

package config.java.nametable.sap;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.User;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItemCollection;

public class CatCompanyCodeNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatCompanyCodeNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		ProcureLineItemCollection lic = (ProcureLineItemCollection)valSrc;
		if(lic == null )
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Partition currentPartition = Base.getSession().getPartition();

		BaseObject CompanyCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatCompanyCodeNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

				String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
				Log.customer.debug(" CatCompanyCodeNameTable: CCSAPSource"+CCSAPSource);
				User commonUser = (User)User.getPartitionedUser(lic.getRequester(),currentPartition);
				if(commonUser==null){
					return super.matchPattern(field, pattern,searchTermQuery);
				}
				String RequesterOpcoSAPSource = (String)commonUser.getDottedFieldValue("CompanyCode.SAPSource");
				Log.customer.debug(" CatCompanyCodeNameTable: RequesterOpcoSAPSource "+RequesterOpcoSAPSource);
				if(RequesterOpcoSAPSource==null){
					return super.matchPattern(field, pattern,searchTermQuery);
				}
				qryString = "Select CompanyCode, UniqueName, Description from ariba.core.CompanyCode where SAPSource = '"+ RequesterOpcoSAPSource +"' and CrossCompanyBuyEnabled = 'Y'";

			if(lic.instanceOf("ariba.invoicing.core.Invoice"))
			{
				qryString = "Select CompanyCode, UniqueName, Description from ariba.core.CompanyCode where SAPSource not like '%TradingPartner%'";
			}

		// 1119 :starts here: Code added by Amit Gupta for Contracts.Remove the filter of CrossCompanyBuyEnabled for Master agreements
			if((lic.instanceOf("ariba.contract.core.ContractRequest")) || (lic.instanceOf("ariba.contract.core.Contract")))
			{
					qryString = "Select CompanyCode, UniqueName, Description from ariba.core.CompanyCode where SAPSource = '"+ RequesterOpcoSAPSource +"'";

			}
		Log.customer.debug(" CatCompanyCodeNameTable: pattern"+pattern);
		Log.customer.debug(" CatCompanyCodeNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);
		// 1119 : Code added by Amit Gupta for Contracts. Ends.

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatCompanyCodeNameTable: %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(140);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}

}
