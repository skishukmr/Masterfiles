//updated by Aswini on 12/07/2011 for PCL logic added UniqueName in the query string
package config.java.nametable.sap;

import java.util.List;
import java.util.Locale;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
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

public class CatSAPBillToNameTable extends AQLNameTable{
	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;
	public CatSAPBillToNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		Log.customer.debug(" CatSAPBillToNameTable: valSrc " +valSrc);
		LineItemCollection lic = null;
		ProcureLineItem li = null;

		if(valSrc instanceof ContractRequest)
		{
			Log.customer.debug(" CatSAPBillToNameTable: Within MasterAgreementRequest " +valSrc);
			lic = (ContractRequest)valSrc;
		}
		else if( valSrc instanceof Contract)
		{
			Log.customer.debug(" CatSAPBillToNameTable: Within MasterAgreement " +valSrc);
			lic = (Contract)valSrc;
		}
		else
		{
		 li = (ProcureLineItem)valSrc;
		 lic = li.getLineItemCollection();
		}

		Log.customer.debug(" CatSAPBillToNameTable: li =>" +li);
		Log.customer.debug(" CatSAPBillToNameTable: lic =>" +lic);

		if(lic == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		Partition currentPartition = Base.getSession().getPartition();
		Locale userLocale = (Locale)Base.getSession().getLocale();
		ClusterRoot companyCode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" CatSAPBillToNameTable: companyCode =>" +companyCode);
		if (companyCode == null)
		{
			Log.customer.debug("CatSAPBillToNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		String companyCodeID = (String) companyCode.getDottedFieldValue("UniqueName");
		Log.customer.debug(" CatSAPBillToNameTable: companyCodeID =>" +companyCodeID);

		ClusterRoot defaultBillToAddress = (ClusterRoot) lic.getDottedFieldValue("CompanyCode.DefaultBillToAddress");
		Log.customer.debug(" CatSAPBillToNameTable: defaultBillToAddress =>" +defaultBillToAddress);
		if(defaultBillToAddress == null)
		{
			Log.customer.debug("CatSAPBillToNameTable: defaultBillToAddress for Company Code is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		String addUniqueID = (String) defaultBillToAddress.getDottedFieldValue("UniqueName");
		Log.customer.debug(" CatSAPBillToNameTable: addUniqueID =>" +addUniqueID);
        
         // Added A.UniqueName in the query for PCL		
		qryString = "Select A, A.UniqueName, A.Name, A.PostalAddress.Lines, A.CompanyCode.UniqueName  from ariba.common.core.Address as A  " +
		"where A.UniqueName = '"+ addUniqueID +"' and A.CompanyCode.UniqueName = '"+ companyCodeID +"' and A.Creator is null " ;

		Log.customer.debug(" CatSAPBillToNameTable: qryString"+qryString);
		Log.customer.debug(" CatSAPBillToNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPBillToNameTable: field"+field);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}
			qryString = qryString +" order by Name";
			Log.customer.debug("CatSAPBillToNameTable: Final qryString =>"+qryString);
			AQLQuery query = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(100);
			options.setUserLocale(userLocale);
			options.setUserPartition(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}

}