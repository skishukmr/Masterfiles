/*
 * CatPrefSupplierLocFilter.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform.sap;

import java.util.StringTokenizer;
import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.base.fields.*;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.common.core.Supplier;

/*
 *  Nametable to select only the locations of the supplier selected.
 */
public class CatSAPPrefSupplierLocFilter extends AQLNameTable
{
    private static final String classname = "CatSAPPrefSupplierLocFilter: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
    	Log.customer.debug("CatSAPPrefSupplierLocFilter: Started *****");
    	ValueSource valuesrc = getValueSourceContext();
    	Log.customer.debug("CatSAPPrefSupplierLocFilter: valSrc => "+ valuesrc);

    	ClusterRoot appr = null ;

		appr = ((BaseObject)valuesrc).getClusterRoot();
		Log.customer.debug("CatSAPPrefSupplierLocFilter: appr => "+ appr);

    	FastStringBuffer partFunc = new FastStringBuffer ();
    	Partition currentPartition = Base.getSession().getPartition();
    	Log.customer.debug("CatSAPPrefSupplierLocFilter: currentPartition => "+ currentPartition);

    		Supplier supplier = null;
    		String prefSuppCondForDelete = "XXX";
            String maintType = (String)appr.getFieldValue("MaintenanceType");

            if (maintType.equals("Create")) {
                supplier= (Supplier)appr.getFieldValue("PreferredSupplierToCreate");
            } else if (maintType.equals("Update")) {
                supplier = (Supplier)appr.getFieldValue("PreferredSupplierToUpdate");
            } else if (maintType.equals("Delete")) {
                supplier = (Supplier)appr.getFieldValue("PreferredSupplierToDelete");
                prefSuppCondForDelete = "SupplierLocation.PreferredSupplier = true";
                Log.customer.debug("CatSAPPrefSupplierLocFilter: prefSuppCondForDelete =>" +prefSuppCondForDelete);
            }
            if(appr.getDottedFieldValue("CompanyCode")==null)
        	{
        		Log.customer.debug("CatSAPPrefSupplierLocFilter: CompanyCode is null");
        	}

        	String companyCode = (String)appr.getDottedFieldValue("CompanyCode.UniqueName");
        	Log.customer.debug("CatSAPPrefSupplierLocFilter: companyCode => "+ companyCode);


    		String CompanyDefaultPartnerFunction = (String) appr.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
    		Log.customer.debug("CatSAPPrefSupplierLocFilter: CompanyDefaultPartnerFunction => "+ CompanyDefaultPartnerFunction);

    		if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
    		{
    			StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
    			while (st.hasMoreTokens())
    			{
    				String partnerFunc = st.nextToken();
    				Log.customer.debug("CatSAPPreferredSupplierFilter: partnerFunc => "+ partnerFunc);
    				partFunc.append(",'"+ partnerFunc +"'");
    			}
    			Log.customer.debug("CatSAPPreferredSupplierFilter: partFunc.toString() => "+ partFunc.toString());
    			partFunc.removeCharAt(0);
    			Log.customer.debug("CatSAPPreferredSupplierFilter: partFunc.toString() => "+ partFunc.toString());
    		}

            if(supplier != null) {
                BaseId supBaseid= supplier.getBaseId();

                String conditionText = Fmt.S("Supplier = %s", AQLScalarExpression.buildLiteral(supBaseid));
                Log.customer.debug("**%s : Condition Text = %s", classname, conditionText);
                query.and(AQLCondition.parseCondition(conditionText));

                String validOrderingLocType = "SupplierLocation.LocType in (" + partFunc.toString()+ ")";
                Log.customer.debug("CatSAPPrefSupplierLocFilter **%s : validOrderingLocType = %s", classname, validOrderingLocType);
                query.and(AQLCondition.parseCondition(validOrderingLocType));

                String validContactID = "SupplierLocation.ContactID = '"+companyCode+"'";
                Log.customer.debug("CatSAPPrefSupplierLocFilter **%s : validContactID = %s", classname, validOrderingLocType);
                query.and(AQLCondition.parseCondition(validContactID));

                if(prefSuppCondForDelete.length() > 3)
                {
                    Log.customer.debug("CatSAPPrefSupplierLocFilter **%s : prefSuppCondForDelete = %s", classname, prefSuppCondForDelete);
                    query.and(AQLCondition.parseCondition(prefSuppCondForDelete));
                }

            }
        Log.customer.debug("CatSAPPrefSupplierLocFilter **%s : Final Query = %s", classname, query.toString());
    }

    public CatSAPPrefSupplierLocFilter()
    {
        super();
    }
}
