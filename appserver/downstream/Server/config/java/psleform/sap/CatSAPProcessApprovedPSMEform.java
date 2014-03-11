/*
 * CatProcessApprovedPSMEform.java
 * Created by Chandra on Aug 10, 2005
 *
 */


package config.java.psleform.sap;

import ariba.approvable.core.Approvable;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.util.core.PropertyTable;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.base.core.*;
import ariba.util.core.ListUtil;
import java.util.List;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.base.core.aql.*;


/*
 * Post-Approval Action class for Preferred Supplier Maintenance Eform
 */

public class CatSAPProcessApprovedPSMEform extends Action
{
        public void fire (ValueSource object, PropertyTable parameters) throws ActionExecutionException {
        Log.customer.debug("CatSAPProcessApprovedPSMEform action Firing ...");
        Log.customer.debug("CatSAPProcessApprovedPSMEform object => "+object);
        Approvable psm = (Approvable)object;
        Log.customer.debug("CatSAPProcessApprovedPSMEform psm => "+psm.getUniqueName());


        Supplier supplier = null;
        	//If the approvable is approved
        	if (psm.getApprovedState() == Approvable.StateApproved) {
        		Log.customer.debug("CatSAPProcessApprovedPSMEform psm is approved psm => "+psm.getUniqueName());
                String partition = psm.getPartition().getName();
                SupplierLocation supplierLocation = null;
                String maintType = (String)psm.getFieldValue("MaintenanceType");
                Log.customer.debug("CatSAPProcessApprovedPSMEform MaintenanceType => "+maintType);
                if (maintType.equals("Create") || maintType.equals("Update")) {

                    if (maintType.equals("Create")) {
                        supplier = (Supplier)psm.getFieldValue("PreferredSupplierToCreate");
                        Log.customer.debug("CatSAPProcessApprovedPSMEform Create supplier => "+supplier);
                    }else if(maintType.equals("Update")) {
              	        supplier = (Supplier)psm.getFieldValue("PreferredSupplierToUpdate");
                        Log.customer.debug("CatSAPProcessApprovedPSMEform Update supplier => "+supplier);
                    }
                    if (supplier == null)
                    {
                    	Log.customer.debug("CatSAPProcessApprovedPSMEform supplier is null=> "+supplier);
                        throw new ActionExecutionException("Supplier not selected in PSM="+psm.getUniqueName());
                    }

                    List supplierFacilityLines = (List)psm.getFieldValue("SAPSupplierFacilityLines");
                    Log.customer.debug("CatSAPProcessApprovedPSMEform supplierFacilityLines=> "+supplierFacilityLines);

                    if(!ListUtil.nullOrEmptyList(supplierFacilityLines)) {

                    	// Get the Company Code
                    	ClusterRoot companyCode = (ClusterRoot) psm.getFieldValue("CompanyCode");
                    	Log.customer.debug("CatSAPProcessApprovedPSMEform companyCode => "+companyCode);

                    	User pslOwner = (User)psm.getFieldValue("PSLOwner");
                        if (pslOwner == null) pslOwner = (User) psm.getFieldValue("Preparer");
                        Log.customer.debug("CatSAPProcessApprovedPSMEform pslOwner => "+pslOwner);

                    	// Get each supplier Locations one by one
                    	String objMatchQuery ="Select CatSAPPreferredSupplierData "
                            + "FROM cat.core.CatSAPPreferredSupplierData "
                            + "AS CatSAPPreferredSupplierData include inactive "
                            + "WHERE PreferredSupplier=%s "
                            + "AND CompanyCode=%s "
                            + "AND Category=%s ";

                    	 Log.customer.debug("CatSAPProcessApprovedPSMEform objMatchQuery => "+objMatchQuery);
                    	 AQLOptions options = new AQLOptions(Base.getSession().getPartition());

                    	for(int i=0; i< supplierFacilityLines.size();i++ )
                    	{
                    		BaseObject bo = (BaseObject)supplierFacilityLines.get(i);
                    		Log.customer.debug("CatSAPProcessApprovedPSMEform bo => "+bo);
                    		SupplierLocation suppLocation = (SupplierLocation)bo.getFieldValue("SupplierLocation");
                            Log.customer.debug("CatSAPProcessApprovedPSMEform suppLocation => "+suppLocation);
                            // setAllSupplierDataInactive(supplier);
                            setAllSupplierLocDataInactive(suppLocation);
                            ClusterRoot categoryCR = (ClusterRoot)bo.getFieldValue("Category");
                            Log.customer.debug("CatSAPProcessApprovedPSMEform categoryCR => "+categoryCR);
                            List manufList = (List)bo.getFieldValue("ManufacturerList");
                            Log.customer.debug("CatSAPProcessApprovedPSMEform manufList => "+manufList);

                            BaseId bidPrefSuppData = Base.getService().objectMatching(
                                            Fmt.S(objMatchQuery,
                                                AQLScalarExpression.buildLiteral(suppLocation.getBaseId()),
                                                AQLScalarExpression.buildLiteral(companyCode.getBaseId()),
                                                AQLScalarExpression.buildLiteral(categoryCR.getBaseId())),
                                            options);

                            if(bidPrefSuppData != null)
                            {
	                            	Log.customer.debug("CatSAPProcessApprovedPSMEform bidPrefSuppData Found=> "+bidPrefSuppData);
	                            	ClusterRoot prefSuppDataCR = (ClusterRoot)bidPrefSuppData.get();
	                                prefSuppDataCR.setFieldValue("CompanyCode", companyCode);
	                                prefSuppDataCR.setFieldValue("Category", categoryCR);
	                                prefSuppDataCR.setFieldValue("PreferredSupplier", suppLocation);
	                                BaseVector manufVec = (BaseVector)prefSuppDataCR.getFieldValue("ManufacturerList");
	                                manufVec.updateElements(manufList);
	                                prefSuppDataCR.setActive(true);
	                                prefSuppDataCR.save();
	                                Log.customer.debug("CatSAPProcessApprovedPSMEform : Updated the existing cluster root");
                            } else {
                            	Log.customer.debug("CatSAPProcessApprovedPSMEform bidPrefSuppData not Found=> "+bidPrefSuppData);
                                ClusterRoot prefSuppDataCR = (ClusterRoot) BaseObject.create("cat.core.CatSAPPreferredSupplierData", Base.getSession().getPartition());
                                prefSuppDataCR.setFieldValue("CompanyCode", companyCode);
                                prefSuppDataCR.setFieldValue("Category", categoryCR);
                                prefSuppDataCR.setFieldValue("PreferredSupplier", suppLocation);
                                BaseVector manufVec = (BaseVector)prefSuppDataCR.getFieldValue("ManufacturerList");
                                manufVec.updateElements(manufList);
                                prefSuppDataCR.save();
                                Log.customer.debug("CatSAPProcessApprovedPSMEform bidPrefSuppData prefSuppDataCR has been created");

                            }
                            // Set the supplier as preferred, updating values from PSM form
                            Log.customer.debug("CatSAPProcessApprovedPSMEform started updating the SupplierLocation=> " +suppLocation);
                            suppLocation.setFieldValue("PreferredSupplier", new Boolean(true));
                            suppLocation.setFieldValue("EffectiveDate",psm.getFieldValue("EffectiveDate"));
                            suppLocation.setFieldValue("ExpirationDate",psm.getFieldValue("ExpirationDate"));
                            suppLocation.setFieldValue("ReviewNotifDate",psm.getFieldValue("ReviewNotifDate"));
                            suppLocation.setFieldValue("DistributorURL",psm.getFieldValue("DistributorURL"));
                            suppLocation.setFieldValue("PrefSuppCreator",pslOwner);
                            Log.customer.debug("CatSAPProcessApprovedPSMEform finished updating the SupplierLocation=> " +suppLocation);

                        }
                    }

                } else if (maintType.equals("Delete"))
                {
                    //Mark the supplier as not preferred
                	BaseVector suppLocDeleteList = (BaseVector) psm.getDottedFieldValue("PreferredSupplierLocation");
                	Log.customer.debug("CatSAPProcessApprovedPSMEform suppLocListDelete=> "+suppLocDeleteList);
                	for(int i = 0; i < suppLocDeleteList.size(); i++)
                	{
                            BaseId bid =  (BaseId) suppLocDeleteList.get(i);
        		        	Log.customer.debug("CatSAPProcessApprovedPSMEform:***bid =>"+bid);
        		        	SupplierLocation suppLoc = (SupplierLocation) Base.getSession().objectIfAny(bid);
        		        	Log.customer.debug("CatSAPProcessApprovedPSMEform:***suppLoc =>"+suppLoc);
        		        	suppLoc.setFieldValue("PreferredSupplier", new Boolean(false));
        		        	setAllSupplierLocDataInactive(suppLoc);
        		        	Log.customer.debug("CatSAPProcessApprovedPSMEform:***suppLoc is deleted=>"+suppLoc);
                	}

                }//end if(create, update, delete)
            }//state approved

        //supplier.save();
		//Base.getSession().transactionCommit();
    }

    private void setAllSupplierLocDataInactive(SupplierLocation suppLoc)
    {
        String queryTxt ="Select CatSAPPreferredSupplierData "
                              + "FROM cat.core.CatSAPPreferredSupplierData "
                              + "AS CatSAPPreferredSupplierData "
                              + "WHERE PreferredSupplier=%s ";
        AQLQuery aql = AQLQuery.parseQuery(Fmt.S(queryTxt,
                                            AQLScalarExpression.buildLiteral(suppLoc.getBaseId())));
        AQLOptions options = new AQLOptions(Base.getSession().getPartition());

        AQLResultCollection results = Base.getService().executeQuery(aql, options);
        Log.customer.debug("Results Size => " + results.getSize());

        while (results.next()) {
            BaseId bid = results.getBaseId(0);
            ClusterRoot cr = (ClusterRoot)bid.get();
            cr.setActive(false);
        }
    }

    protected ValueInfo getValueInfo () {
        return new ValueInfo(IsScalar, Approvable.ClassName);
    }
}
