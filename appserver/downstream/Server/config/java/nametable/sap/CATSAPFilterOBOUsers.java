package config.java.nametable.sap;

import java.util.List;
import java.util.Locale;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItemCollection;

public class CATSAPFilterOBOUsers extends AQLNameTable {

	public CATSAPFilterOBOUsers()
	{
		super();
	}
	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{

		Log.customer.debug(" CATSAPFilterOBOUsers: Started *****");
		String companyCode = "";
		String purchaseOrg = "";
		String strPattern = new String(pattern);

		if (strPattern != null && !strPattern.trim().equals("")) {
			if (strPattern.length() == 1 && strPattern.equals("*")) {
				strPattern = "";
			} else {
				strPattern = strPattern.replace('*', ' ');
			}
		}
		strPattern = strPattern.trim();


		ValueSource context = super.getValueSourceContext();
		if (!(context instanceof ProcureLineItemCollection)){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		ProcureLineItemCollection lic = (ProcureLineItemCollection)context;
		Log.customer.debug(" CATSAPFilterOBOUsers: lic => " +lic);
		if(lic != null)
		{
			int numOfLines = lic.getLineItems().size();
			Log.customer.debug(" CATSAPFilterOBOUsers: numOfLines => " +numOfLines);
					if (numOfLines <= 0)
			{
					Log.customer.debug(" CATSAPFilterOBOUsers: numOfLines => " +numOfLines);
					return super.matchPattern(field, pattern,searchTermQuery);
			}
		}


		String queryText =null;
		String purchaseOrguniq =null;

		// get the companycode
		companyCode = (String) ((BaseObject) context).getDottedFieldValue("CompanyCode.UniqueName");
		Log.customer.debug(" CATSAPFilterOBOUsers: companyCode => "+companyCode);
		purchaseOrg = (String) ((BaseObject) context).getDottedFieldValue("CompanyCode.PurchaseOrgFiltering");
		 Log.customer.debug(" CATSAPFilterOBOUsers: purchaseOrg => "+purchaseOrg);
		 purchaseOrguniq  = (String) lic.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName");
		Log.customer.debug(" CATSAPFilterOBOUsers: purchaseOrgUniqueName  => "+purchaseOrguniq);
		if(companyCode!=null &&(purchaseOrg!=null &&purchaseOrg.equals("Y")))
		{
		 queryText = "SELECT  \"User\",  \"User\".Name,  \"User\".EmailAddress,  \"User\".LastName FROM ariba.\"user\".core.\"User\" AS \"User\", ariba.common.core.\"User\" AS bu WHERE bu.CompanyCode.UniqueName = '" + companyCode + "'  AND bu.PurchaseOrg.UniqueName = '"+purchaseOrguniq+"' AND bu.\"User\" =\"User\" ";
		}
		 else
		 {
			queryText = "SELECT  \"User\",  \"User\".Name,  \"User\".EmailAddress,  \"User\".LastName FROM ariba.\"user\".core.\"User\" AS \"User\", ariba.common.core.\"User\" AS bu WHERE bu.CompanyCode.UniqueName = '" + companyCode + "'   AND bu.\"User\" =  \"User\" ";
		 }
		Log.customer.debug(" CATSAPFilterOBOUsers: after adding CompanyCode condition to query => "+queryText);


		ariba.base.core.Partition currentPartition = (ariba.base.core.Partition) Base.getSession().getPartition();
		Log.customer.debug(" CATSAPFilterPurchaseOrg: currentPartition => "+currentPartition);



		// Get the Requester Purchase Org also

		//}

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			queryText = queryText + " AND \"User\"."+field+" like '%" + pattern1 + "%' ORDER BY  \"User\".LastName ASC";

		}

			Log.customer.debug(" CATSAPFilterPurchaseOrg: Final queryText => "+queryText);

			AQLQuery query1 = AQLQuery.parseQuery(queryText);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(100);
			Locale userLocale = (Locale)Base.getSession().getLocale();
			options.setUserLocale(userLocale);
			options.setUserPartition(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
			Log.customer.debug("Results Statement= %s", results);
			return results.getRawResults();
	}

}
