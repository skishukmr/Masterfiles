/********************************************************************************************
 Change History
 	#	Change By	Change Date		Description
	===========================================================================================
	   Divya Nair   05/30/10        Issue # 1123- WithHolding TaxCode appearing in Invoice for Service Contract Items without release
********************************************************************************************/
package config.java.nametable.sap;

import java.util.List;
import java.util.Locale;

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
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.Log;
import ariba.purchasing.core.PurchaseOrder;
public class CatSAPWithHoldTaxCodeNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSAPWithHoldTaxCodeNameTable()
	{
		super();
	}

   	public List matchPattern(String field, String pattern,SearchTermQuery searchTermQuery)
	{
		ValueSource valSrc = getValueSourceContext();
		BaseObject invoiceEform = (BaseObject)valSrc;
		Partition currentPartition=null;
		Locale userLocale=null;
		Log.customer.debug("WithHoldTaxNameTable InvoiceEform :",invoiceEform);
		if(invoiceEform == null)
		{
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		if(invoiceEform instanceof Invoice)
		{
			Log.customer.debug("WithHoldTaxNameTable : Instance of Invoice");
			currentPartition = Base.getSession().getPartition();
		    userLocale = (Locale)Base.getSession().getLocale();
		//Fix for withholding tax field display when user enters Invoice for Contract (CWOR) start
            Contract contract = null;
		    List InvoiceLines = (List)invoiceEform.getFieldValue("LineItems");
		    if(InvoiceLines==null || (InvoiceLines!=null && InvoiceLines.size()==0)){
			return super.matchPattern(field, pattern,searchTermQuery);
		    }
		    else{
			   BaseObject lineitem = (BaseObject)InvoiceLines.get(0);
			   contract = (Contract)lineitem.getFieldValue("MasterAgreement");
		   }
		   BaseObject CompanyCode = (BaseObject)contract.getDottedFieldValue("CompanyCode");
		   if (CompanyCode == null)
		   {
			   Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: CompanyCode is null");
			   return super.matchPattern(field, pattern,searchTermQuery);
		   }
			BaseObject orderline = (BaseObject) contract.getLineItems().get(0);
      //Fix for withholding tax field display when user enters Invoice for Contract (CWOR) end
		if (orderline==null){
			Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: orderline is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Address shipto = (Address)orderline.getDottedFieldValue("ShipTo");
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable : shipto %s " , shipto);
		if(shipto == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Country shiptocountry = (Country)shipto.getCountry();
		if(shiptocountry == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		String shiptoCountryUniqueName = (String)shiptocountry.getUniqueName();

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: CCSAPSource"+CCSAPSource);
		//AUL,sdey : Changed the class name for TaxCode
		qryString = "Select TaxCode, UniqueName, Description from ariba.tax.core.TaxCode where Country.UniqueName = '"+ shiptoCountryUniqueName +"' and SAPSource = '"+ CCSAPSource +"' and IsWithHoldingTax = 'Y'";

		}
		else
		{
			Log.customer.debug("WithHoldTaxNameTable : Not an Instance of Invoice");


		 currentPartition = Base.getSession().getPartition();
		userLocale = (Locale)Base.getSession().getLocale();

		PurchaseOrder order = null;
		List InvoiceLines = (List)invoiceEform.getFieldValue("LineItems");
		if(InvoiceLines==null || (InvoiceLines!=null && InvoiceLines.size()==0)){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		else{
			BaseObject lineitem = (BaseObject)InvoiceLines.get(0);
			order = (PurchaseOrder)lineitem.getFieldValue("Order");
		}
		BaseObject CompanyCode = (BaseObject)order.getDottedFieldValue("CompanyCode");
		if (CompanyCode == null)
		{
			Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		BaseObject orderline = (BaseObject) order.getLineItems().get(0);
		if (orderline==null){
			Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: orderline is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Address shipto = (Address)orderline.getDottedFieldValue("ShipTo");
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable : shipto %s " , shipto);
		if(shipto == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Country shiptocountry = (Country)shipto.getCountry();
		if(shiptocountry == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		String shiptoCountryUniqueName = (String)shiptocountry.getUniqueName();

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: CCSAPSource"+CCSAPSource);

		//AUL,sdey : Changed the class name for TaxCode
		qryString = "Select TaxCode, UniqueName, Description from ariba.tax.core.TaxCode where Country.UniqueName = '"+ shiptoCountryUniqueName +"' and SAPSource = '"+ CCSAPSource +"' and IsWithHoldingTax = 'Y'";
		}
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPWithHoldTaxCodeNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatSAPWithHoldTaxCodeNameTable: %s", qryString);
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
