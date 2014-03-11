/*
 * CatSAPUpdateFieldsOnPSM.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform.sap;

import ariba.base.fields.*;
import ariba.approvable.core.*;
import ariba.base.core.*;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.common.core.SupplierLocation;
import ariba.util.core.ListUtil;
import java.util.List;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.base.core.aql.*;

/*
 * On selection of Update as Maintenance type and when supplier is
 * is selected, the values from the Supplier fields are copied to the eform
 */
public class CatSAPUpdateFieldsOnPSM extends Action {

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

    	Log.customer.debug("CatSAPUpdateFieldsOnPSM Started ...");
    	Log.customer.debug("CatSAPUpdateFieldsOnPSM object =>" +object);
        Approvable appr = (Approvable)object;
        Log.customer.debug("CatSAPUpdateFieldsOnPSM appr =>" +appr);
        ClusterRoot supp = (ClusterRoot)appr.getFieldValue("PreferredSupplierToUpdate");
        Log.customer.debug("CatSAPUpdateFieldsOnPSM supp =>" +supp);
        BaseVector supplierLocUpdateList = (BaseVector)appr.getFieldValue("PreferredSupplierLocation");
        Log.customer.debug("CatSAPUpdateFieldsOnPSM supplierLocUpdateList size =>" +supplierLocUpdateList.size() );
        Log.customer.debug("CatSAPUpdateFieldsOnPSM supplierLocUpdateList =>" +supplierLocUpdateList);
        if (supp != null && supplierLocUpdateList.size() > 0 ) {
        	Log.customer.debug("CatSAPUpdateFieldsOnPSM Both Supplier and Supplier Lcoation are not null ");

        	// Get the existing base object and clear the earlier record and prepare a temporary list to gather all the base object
            BaseVector pslFacility = (BaseVector)appr.getFieldValue("SAPSupplierFacilityLines");
            pslFacility.clear();
            List psfmNewList = ListUtil.list();

            // Prepare the Object matching Query
            String query = "Select CatSAPPreferredSupplierData "
                + "FROM cat.core.CatSAPPreferredSupplierData "
                + "AS CatSAPPreferredSupplierData "
                + "WHERE PreferredSupplier=%s";

            Log.customer.debug("CatSAPUpdateFieldsOnPSM: Object matching query =>"+query);
            AQLOptions options = new AQLOptions(Base.getSession().getPartition());
            // Start processing each of the supplier Location and fidn the cluster root
            for(int i = 0; i < supplierLocUpdateList.size(); i++)
        	{
                    BaseId bid =  (BaseId) supplierLocUpdateList.get(i);
		        	Log.customer.debug("CatSAPUpdateFieldsOnPSM:***bid =>"+bid);
		        	SupplierLocation suppLoc = (SupplierLocation) Base.getSession().objectIfAny(bid);
		        	Log.customer.debug("CatSAPUpdateFieldsOnPSM:***suppLoc =>"+suppLoc);
		        	AQLQuery aql = AQLQuery.parseQuery(Fmt.S(query,	AQLScalarExpression.buildLiteral(suppLoc.getBaseId())));
		        	Log.customer.debug("CatSAPUpdateFieldsOnPSM: Object matching aql => "+aql);
	    			AQLResultCollection results = Base.getService().executeQuery(aql, options);
	    			if(!ListUtil.nullOrEmptyList(results.getErrors()))
	    			{
	    				String err = results.getErrorStatementText();
	    				Log.customer.debug("CatSAPUpdateFieldsOnPSM: Encountered error in result set for suppLoc => "+suppLoc);
	    			}

	    			Log.customer.debug("CatSAPUpdateFieldsOnPSM: results size => "+results.getSize());
	    			 while (results.next())
	    			 	{
	    	                BaseId bidcr = results.getBaseId(0);
	    	                ClusterRoot cr = (ClusterRoot)bidcr.get();
	    	                Log.customer.debug("CatSAPUpdateFieldsOnPSM: cr => "+cr);
	    	                BaseObject boForPSL = (BaseObject)BaseObject.create("cat.core.CatSAPPrefSupplierItemsForFacility", Base.getSession().getPartition());

	    	                boForPSL.setFieldValue("SupplierLocation", (ClusterRoot)cr.getFieldValue("PreferredSupplier"));
	    	                boForPSL.setFieldValue("Category", (ClusterRoot)cr.getFieldValue("Category"));
	    	                BaseVector manufVec = (BaseVector)boForPSL.getFieldValue("ManufacturerList");
	    	                manufVec.updateElements((List)cr.getFieldValue("ManufacturerList"));
	    	                psfmNewList.add(boForPSL);
	    	                Log.customer.debug("CatSAPUpdateFieldsOnPSM: psfmNewList => "+psfmNewList);
	    	            }
	    			 	Log.customer.debug("CatSAPUpdateFieldsOnPSM: Created the CatSAPPrefSupplierItemsForFacility for the supplier Lcoation => "+suppLoc);
	    			 	Log.customer.debug("CatSAPUpdateFieldsOnPSM: Starting setting back other data on eForm => ");
	    	            appr.setFieldValue("EffectiveDate", suppLoc.getFieldValue("EffectiveDate"));
	    	            appr.setFieldValue("ExpirationDate", suppLoc.getFieldValue("ExpirationDate"));
	    	            appr.setFieldValue("ReviewNotifDate", suppLoc.getFieldValue("ReviewNotifDate"));
	    	            appr.setFieldValue("DistributorURL", suppLoc.getFieldValue("DistributorURL"));
	    	            User pslOwner = (User)suppLoc.getFieldValue("PrefSuppCreator");
	    	            appr.setFieldValue("PSLOwner",pslOwner);
	    	            Log.customer.debug("CatSAPUpdateFieldsOnPSM: psfmNewList => "+psfmNewList);
	    	} // End of For Loop for Supplier Location

            Log.customer.debug("CatSAPUpdateFieldsOnPSM: Updating pslFacility using psfmNewList");
            Log.customer.debug("CatSAPUpdateFieldsOnPSM: psfmNewList =>" + psfmNewList);
            pslFacility.updateElements(psfmNewList);
            Log.customer.debug("CatSAPUpdateFieldsOnPSM: psfmNewList =>" + pslFacility);
            }
    }

    public CatSAPUpdateFieldsOnPSM() {
        super();
    }

}