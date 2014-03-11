/*
 * CatSAPNotPreferredSupplierFilter.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform.sap;

import java.util.StringTokenizer;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.*;
import ariba.util.core.FastStringBuffer;
import ariba.util.log.Log;
import ariba.base.fields.*;
import java.util.List;

/*
 *  Nametable to select suppliers that are not preferred.
 */
public class CatSAPNotPreferredSupplierFilter extends AQLNameTable
{

	String qryString;
	AQLQuery query;
	AQLOptions queryOptions;
	AQLResultCollection queryResults;

    public List matchPattern(String field, String strPattern,SearchTermQuery searchTermQuery)
    {
    	Log.customer.debug("CatSAPNotPreferredSupplierFilter: Started *****");
    	ValueSource valSrc = getValueSourceContext();
    	Log.customer.debug("CatSAPNotPreferredSupplierFilter: valSrc => "+ valSrc);
    	Approvable appr = (Approvable)valSrc;
    	Log.customer.debug("CatSAPNotPreferredSupplierFilter: appr => "+ appr);
    	FastStringBuffer partFunc = new FastStringBuffer ();
    	Partition currentPartition = Base.getSession().getPartition();
    	Log.customer.debug("CatSAPNotPreferredSupplierFilter: currentPartition => "+ currentPartition);

    	if(appr.getDottedFieldValue("CompanyCode")==null)
    	{
    		Log.customer.debug(" CatSupplierNameTable: CompanyCode is null");
     		return super.matchPattern(field, strPattern,searchTermQuery);
    	}

    	String companyCode = (String)appr.getDottedFieldValue("CompanyCode.UniqueName");
    	Log.customer.debug("CatSAPNotPreferredSupplierFilter: companyCode => "+ companyCode);


		String CompanyDefaultPartnerFunction = (String) appr.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
		Log.customer.debug("CatSAPNotPreferredSupplierFilter: CompanyDefaultPartnerFunction => "+ CompanyDefaultPartnerFunction);

		if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
		{
			StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
			while (st.hasMoreTokens())
			{
				String partnerFunc = st.nextToken();
				Log.customer.debug("CatSAPNotPreferredSupplierFilter: partnerFunc => "+ partnerFunc);
				partFunc.append(",'"+ partnerFunc +"'");
			}
			Log.customer.debug("CatSAPNotPreferredSupplierFilter: partFunc.toString() => "+ partFunc.toString());
			partFunc.removeCharAt(0);
			Log.customer.debug("CatSAPNotPreferredSupplierFilter: partFunc.toString() => "+ partFunc.toString());
		}

		String qryString = "Select Supplier,Supplier.Name " +
					" from ariba.common.core.Supplier , ariba.common.core.SupplierLocation " +
					" where Locations = SupplierLocation " +
					" and Locations.ContactID = '" + companyCode + "'" +
					" and Locations.LocType in (" + partFunc.toString()+ ") " +
					" and ( Locations.PreferredSupplier = false OR Locations.PreferredSupplier is null )";

		Log.customer.debug("CatSAPNotPreferredSupplierFilter: qryString => "+ qryString);
		Log.customer.debug(" CatSAPNotPreferredSupplierFilter: pattern"+strPattern);
		Log.customer.debug(" CatSAPNotPreferredSupplierFilter: field"+field);

		if (strPattern != null && !strPattern.trim().equals("")) {
			if (strPattern.length() == 1 && strPattern.equals("*")) {
				strPattern = "";
			} else {
				strPattern = strPattern.replace('*', ' ');
			}
		}
		strPattern = strPattern.trim();


		if (strPattern != null && !strPattern.trim().equals("")) {
			qryString = qryString + " AND Supplier."+field+" like '%" + strPattern + "%'";
		}

		Log.customer.debug("CatSAPNotPreferredSupplierFilter: Final qryString => "+ qryString);

		AQLQuery query1 = AQLQuery.parseQuery(qryString);
		query1.setDistinct(true);
		Log.customer.debug("CatSAPNotPreferredSupplierFilter: Fianl AQL  query1 => "+ query1);
		AQLOptions options = new AQLOptions(currentPartition);
		options.setRowLimit(100);
		AQLResultCollection results = Base.getService().executeQuery(query1,options);
       	Log.customer.debug("CatSAPNotPreferredSupplierFilter Results Statement=> +", results);
       	return results.getRawResults();
    }

    public CatSAPNotPreferredSupplierFilter()
    {
        super();
    }
}
