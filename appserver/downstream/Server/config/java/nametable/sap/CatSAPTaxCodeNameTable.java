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
import ariba.contract.core.ContractRequest;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.Requisition;

public class CatSAPTaxCodeNameTable extends AQLNameTable{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

	public CatSAPTaxCodeNameTable()
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
			Log.customer.debug(" CatSAPTaxCodeNameTable: CompanyCode is null");
			return super.matchPattern(field, pattern,searchTermQuery);
		}

		Address shipto = (Address)li.getDottedFieldValue("ShipTo");
		Log.customer.debug(" CatSAPTaxCodeNameTable : shipto %s " , shipto);
		if(shipto == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		Country shiptocountry = (Country)shipto.getCountry();
		if(shiptocountry == null){
			return super.matchPattern(field, pattern,searchTermQuery);
		}
		String shiptoCountryUniqueName = (String)shiptocountry.getUniqueName();

		String CCSAPSource = (String)CompanyCode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" CatSAPTaxCodeNameTable: CCSAPSource"+CCSAPSource);
		/*
		 * CR 219: Changing the tax code filtering based on company code country
		 */
		String ccCountryStr="";
		ariba.common.core.Address ccAddress = (ariba.common.core.Address) CompanyCode.getFieldValue("RegisteredAddress");
		if (ccAddress != null) {
			Log.customer.debug("Company code has registered address");
			ariba.basic.core.PostalAddress ccPostalAddress = ccAddress
					.getPostalAddress();
			if (ccPostalAddress != null
					&& ccPostalAddress.getCountry() != null) {
				Log.customer.debug("Postal address and country not null");
				ccCountryStr = ccPostalAddress.getCountry()
						.getUniqueName();
				Log.customer.debug("Company code country-"+ccCountryStr);
			}
		}


		if(lic instanceof Requisition || lic instanceof ContractRequest){
			/*
			 *qryString = "Select TaxCode, UniqueName, Description from ariba.core.TaxCode where Country.UniqueName = '"+ shiptoCountryUniqueName +"' and SAPSource = '"+ CCSAPSource +"' and (IsOnlyValidForIR <> 'Y' or IsOnlyValidForIR is null)";
			 */
			//AUL,sdey : Changed the class name for TaxCode
			qryString = "Select TaxCode, UniqueName, Description from ariba.tax.core.TaxCode where Country.UniqueName = '"+ ccCountryStr +"' and SAPSource = '"+ CCSAPSource +"' and (IsOnlyValidForIR <> 'Y' or IsOnlyValidForIR is null)";
		}else{
			/*
			 *qryString = "Select TaxCode, UniqueName, Description from ariba.core.TaxCode where Country.UniqueName = '"+ shiptoCountryUniqueName +"' and SAPSource = '"+ CCSAPSource +"'";
			 */
			//AUL,sdey : Changed the class name for TaxCode
			qryString = "Select TaxCode, UniqueName, Description from ariba.tax.core.TaxCode where Country.UniqueName = '"+ ccCountryStr +"' and SAPSource = '"+ CCSAPSource +"'";
		}
		Log.customer.debug(" CatSAPTaxCodeNameTable: pattern"+pattern);
		Log.customer.debug(" CatSAPTaxCodeNameTable: field"+field);
		//Log.customer.debug(" CatNonCatalogSupplierLocNameTable: comStr"+comStr);

		if(pattern != null && (!pattern.equals("*")))
		{
			String pattern1 =  pattern.substring(1,pattern.length()-1);
			qryString = qryString + " AND "+field+" like '%" + pattern1 + "%'";

		}

			qryString = qryString +" order by UniqueName";

			Log.customer.debug("final query : CatSAPTaxCodeNameTable: %s", qryString);
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
