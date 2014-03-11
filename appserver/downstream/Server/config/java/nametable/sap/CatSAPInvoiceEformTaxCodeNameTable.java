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
import ariba.basic.core.Country;
import ariba.common.core.Address;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.PurchaseOrder;

public class CatSAPInvoiceEformTaxCodeNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSAPInvoiceEformTaxCodeNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		BaseObject Invli = (BaseObject)valSrc;
		if(Invli==null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		Integer i = (Integer)Invli.getFieldValue("OrderLineNumber");
		PurchaseOrder order = (PurchaseOrder)Invli.getFieldValue("Order");
		if (order==null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}else if( i==null || (i.intValue() <= 0) || i.intValue() > order.getLineItems().size() ){
			i = new Integer(1);
		}
		ProcureLineItem li =(ProcureLineItem)order.getLineItems().get(i.intValue()-1);

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
			Log.customer.debug(" CatSAPInvoiceEformTaxCodeNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		Address shipto = (Address)li.getDottedFieldValue("ShipTo");
		Log.customer.debug(" CatSAPInvoiceEformTaxCodeNameTable : shipto %s " , shipto);
		if(shipto == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Country shiptocountry = (Country)shipto.getCountry();
		if(shiptocountry == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		String shiptoCountryUniqueName = (String)shiptocountry.getUniqueName();

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPInvoiceEformTaxCodeNameTable: CCSAPSource"+CCSAPSource);
		//AUL,sdey : Changed the class name for TaxCode
		qryString = "Select TaxCode, UniqueName, Description from ariba.tax.core.TaxCode where Country.UniqueName = '"+ shiptoCountryUniqueName +"' and SAPSource = '"+ CCSAPSource +"'";

		Log.customer.debug(" CatSAPInvoiceEformTaxCodeNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPInvoiceEformTaxCodeNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatSAPInvoiceEformTaxCodeNameTable: %s", qryString);
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
