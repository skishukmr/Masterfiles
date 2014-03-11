package config.java.nametable.sap;

import java.util.List;

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
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;

public class CatTradingPartnerNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatTradingPartnerNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		ProcureLineItem li = (ProcureLineItem)valSrc;
		LineItemCollection lic = li.getLineItemCollection();
		if(lic == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Partition currentPartition = Base.getSession().getPartition();
		
		BaseObject CompanyCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatTradingPartnerNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}
				
				String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
				Log.customer.debug(" CatTradingPartnerNameTable: CCSAPSource"+CCSAPSource);
				
				String TradingpartnerSAPSource = (String)(CCSAPSource + "TradingPartner");
				Log.customer.debug(" CatTradingPartnerNameTable: TradingpartnerSAPSource "+TradingpartnerSAPSource);
				
				qryString = "Select CompanyCode, UniqueName, Description from ariba.core.CompanyCode where SAPSource = '"+ TradingpartnerSAPSource +"'";


		Log.customer.debug(" CatTradingPartnerNameTable: pattern"+pattern);
		Log.customer.debug(" CatTradingPartnerNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";
			
		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatTradingPartnerNameTable: %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(140);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}
	
}
