/*
 * SetPreferredSupplierLocation.java
 * Created by Chandra on Aug 10, 2005
 *
 */

package config.java.psleform.sap;

import java.util.StringTokenizer;

import ariba.base.fields.*;
import ariba.common.core.Supplier;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;

/*
 * Trigger to set the PreferredSupplierLocation field when
 *  the PreferredSupplierToCreate, PreferredSupplierToUpdate field is set.
 */
public class SetSAPPreferredSupplierLocation extends Action {

    public void fire(ValueSource object, PropertyTable params) {
        String targetFieldName = params.stringPropertyForKey("Target");
        Supplier supplier = (Supplier)params.getPropertyForKey("Source");

        Log.customer.debug("SetSAPPreferredSupplierLocation:***object="+object);

        if(object == null)
            return;

       BaseVector locationsList = null;

        if(supplier != null)
        	 locationsList = supplier.getLocations();

        Log.customer.debug("SetSAPPreferredSupplierLocation:***object="+locationsList);
        BaseVector prefSuppLocVec = (BaseVector)object.getFieldValue("PreferredSupplierLocation");
        prefSuppLocVec.clear();

        // add all supplierlocations which has Location type other than OA and VN

        Log.customer.debug("SetSAPPreferredSupplierLocation:***prefSuppLocVec =>"+prefSuppLocVec);
        Log.customer.debug("SetSAPPreferredSupplierLocation:***locationsList =>"+locationsList);

        if(locationsList == null)
        {
        	Log.customer.debug("SetSAPPreferredSupplierLocation:***locationsList is null");
        	return;
        }


// Get the Valid Company Code

        if(object.getDottedFieldValue("CompanyCode")==null)
    	{
    		Log.customer.debug(" SetSAPPreferredSupplierLocation: CompanyCode is null");
    		return;
    	}

        String companyCode = (String)object.getDottedFieldValue("CompanyCode.UniqueName");
    	Log.customer.debug("SetSAPPreferredSupplierLocation: companyCode => "+ companyCode);

    	String CompanyDefaultPartnerFunction = (String) object.getDottedFieldValue("CompanyCode.ValidPartneringFunctionsOnReq");
		Log.customer.debug("SetSAPPreferredSupplierLocation: CompanyDefaultPartnerFunction => "+ CompanyDefaultPartnerFunction);
		String locTypeArr[] = new String[10];
		if (CompanyDefaultPartnerFunction != null && !CompanyDefaultPartnerFunction.trim().equals(""))
		{
			StringTokenizer st = new StringTokenizer(CompanyDefaultPartnerFunction,"|");
			Log.customer.debug("SetSAPPreferredSupplierLocation: st.countTokens() => "+ st.countTokens());
			//String locType[] = new String[st.countTokens()];

			int i = 0;
			while (st.hasMoreTokens())
			{
				String partnerFunc = st.nextToken();
				Log.customer.debug("SetSAPPreferredSupplierLocation: partnerFunc => "+ partnerFunc);
				locTypeArr[i++] = partnerFunc;
			}


		}
		Log.customer.debug("SetSAPPreferredSupplierLocation: First locType => "+ locTypeArr[0]);
		Log.customer.debug("SetSAPPreferredSupplierLocation: Second locType => "+ locTypeArr[1]);

        for(int i=0; i<locationsList.size();i++)
        {
        	Log.customer.debug("SetSAPPreferredSupplierLocation:***locationsList =>"+ i + " =>"+locationsList.get(i));

        	BaseId bid =  (BaseId) locationsList.get(i);
        	Log.customer.debug("SetSAPPreferredSupplierLocation:***bid =>"+bid);
        	ClusterRoot suppLoc = (ClusterRoot) Base.getSession().objectIfAny(bid);
        	Log.customer.debug("SetPreferredSupplierLocation:***suppLoc =>"+suppLoc);
        	String locType = (String) suppLoc.getDottedFieldValue("LocType");
        	Log.customer.debug("SetSAPPreferredSupplierLocation:***locType =>"+locType);
        	Boolean prefSupplier = (Boolean) suppLoc.getDottedFieldValue("PreferredSupplier");
        	Log.customer.debug("SetSAPPreferredSupplierLocation:***prefSupplier =>"+prefSupplier);
        	if(prefSupplier == null )
        		prefSupplier = new Boolean("false");
         	Log.customer.debug("SetSAPPreferredSupplierLocation:***prefSupplier =>"+prefSupplier);
        	if(locType!= null && (prefSupplier.booleanValue()) && locType.length() > 1 && ( locType.equals(locTypeArr[0])  ||locType.equalsIgnoreCase(locTypeArr[1])))
        	{
        		Log.customer.debug("SetSAPPreferredSupplierLocation:***Before Adding to prefSuppLocVec =>"+prefSuppLocVec);
        		prefSuppLocVec.addElementIfAbsent(locationsList.get(i));
        		Log.customer.debug("SetPreferredSuppliSetSAPPreferredSupplierLocationerLocation:***After Adding to prefSuppLocVec =>"+prefSuppLocVec);
        	}
        }
        //prefSuppLocVec.updateElements(locationsList);

        object.setDottedFieldValue(targetFieldName, prefSuppLocVec);
    }

    protected ValueInfo getValueInfo() {
        return valueInfo;
    }

    protected ValueInfo[] getParameterInfo() {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames() {
        return requiredParameterNames;
    }

    public SetSAPPreferredSupplierLocation() {}

    private static final String requiredParameterNames[] = {
        "Target", "Source"
    };
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("Target", true, ariba.base.fields.Behavior.IsVector, "ariba.common.core.SupplierLocation"),
        new ValueInfo("Source", 0, "ariba.common.core.Supplier")
    };
    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.core.PrefSupplierMaintEform");

}
