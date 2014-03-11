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
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;

public class CatSAPPaymentTermsNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSAPPaymentTermsNameTable()
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
		Locale userLocale = (Locale)Base.getSession().getLocale();
		BaseObject CompanyCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatSAPPaymentTermsNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPPaymentTermsNameTable: CCSAPSource"+CCSAPSource);
		//AUL, sdey : changed the class name for PaymentTerms
		//qryString = "Select PaymentTerms, UniqueName, Description  from ariba.common.core.PaymentTerms where SAPSource = '"+ CCSAPSource +"'";
		qryString = "Select PaymentTerms, UniqueName, Description  from ariba.payment.core.PaymentTerms where SAPSource = '"+ CCSAPSource +"'";

		Log.customer.debug(" CatSAPPaymentTermsNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPPaymentTermsNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatSAPPaymentTermsNameTable: %s", qryString);
			AQLQuery query1 = AQLQuery.parseQuery(qryString);
			AQLOptions options = new AQLOptions(currentPartition);
			options.setRowLimit(140);
			options.setUserLocale(userLocale);
			options.setUserPartition(currentPartition);
			AQLResultCollection results = Base.getService().executeQuery(query1,options);
	       	Log.customer.debug("Results Statement= %s", results);
	       	return results.getRawResults();
	}

}
