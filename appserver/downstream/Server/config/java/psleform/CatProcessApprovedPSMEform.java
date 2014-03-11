/*
 * CatProcessApprovedPSMEform.java
 * Created by Chandra on Aug 10, 2005
 *
 */


package config.java.psleform;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;


/*
 * Post-Approval Action class for Preferred Supplier Maintenance Eform
 */
public class CatProcessApprovedPSMEform extends Action
{
    private static final String thisclass = "CatProcessApprovedPSMEform";


    public void fire (ValueSource object, PropertyTable parameters)
                throws ActionExecutionException {

        Approvable psm = (Approvable)object;
        Supplier supplier = null;
        Log.customer.debug("**%s**Processing %s.", thisclass, psm.getUniqueName());

        //If the approvable is approved
        if (psm.getApprovedState() == Approvable.StateApproved) {

            //Log.customer.debug("**%s**:%s in Approved State - updating Supplier objects.", thisclass, psm.getUniqueName());

            String partition = psm.getPartition().getName();

            //set the fields from PSM Form to Supplier object

            SupplierLocation supplierLocation = null;

            String maintType = (String)psm.getFieldValue("MaintenanceType");
            Log.customer.debug("**%s**:%s maintType ==%s", thisclass, psm.getUniqueName(), maintType);

            if (maintType.equals("Create") || maintType.equals("Update")) {

                if (maintType.equals("Create")) {
                    supplier = (Supplier)psm.getFieldValue("PreferredSupplierToCreate");
                }else if(maintType.equals("Update")) {
                    supplier = (Supplier)psm.getFieldValue("PreferredSupplierToUpdate");
                }

                if (supplier == null)
                    throw new ActionExecutionException("Supplier not selected in PSM="+psm.getUniqueName());

                List availFacList = (List)psm.getFieldValue("SupplierFacilityLines");

                if(!ListUtil.nullOrEmptyList(availFacList)) {

/*********************
                    //clear the vec being held in supplier
                    BaseVector suppFacility = (BaseVector)supplier.getFieldValue("AvailableFacilityMap");
                    suppFacility.clear();
                    Log.customer.debug("**%s**:- cleared AvailableFacilityMap in supplier", thisclass);

                    List psfmNewList = (List)supplier.getFieldValue("AvailableFacilityMap");
                    Log.customer.debug("**%s**:- AvailableFacilityMap list size =" +ListUtil.getListSize(psfmNewList), thisclass);

                    //add the new vec elements to the list that will be added to supplier
                    for(int i = 0; i < availFacList.size(); i++) {
                        BaseObject bo = (BaseObject)availFacList.get(i);
                        BaseObject boForSupplier = bo.deepCopyAndStrip();
                        psfmNewList.add(boForSupplier);
                    }

                    Log.customer.debug("**%s**:- AvailableFacilityMap before update =" +ListUtil.getListSize(psfmNewList), thisclass);
                    suppFacility.updateElements(psfmNewList);
                    Log.customer.debug("**%s**:- after setting in supplier onjedt =" , thisclass);

*********************/

                    setAllSupplierDataInactive(supplier);

                    String objMatchQuery ="Select CatPreferredSupplierData "
                                          + "FROM cat.core.CatPreferredSupplierData "
                                          + "AS CatPreferredSupplierData include inactive "
                                          + "WHERE PreferredSupplier=%s "
                                          + "AND Facility=%s "
                                          + "AND Category=%s ";
                    AQLOptions options = new AQLOptions(Base.getSession().getPartition());
                    for(int i = 0; i < availFacList.size(); i++) {
                        BaseObject bo = (BaseObject)availFacList.get(i);
                        ClusterRoot facilityCR = (ClusterRoot)bo.getFieldValue("Facility");
                        ClusterRoot categoryCR = (ClusterRoot)bo.getFieldValue("Category");
                        List manufList = (List)bo.getFieldValue("ManufacturerList");

                        BaseId bidPrefSuppData = Base.getService().objectMatching(
                                        Fmt.S(objMatchQuery,
                                            AQLScalarExpression.buildLiteral(supplier.getBaseId()),
                                            AQLScalarExpression.buildLiteral(facilityCR.getBaseId()),
                                            AQLScalarExpression.buildLiteral(categoryCR.getBaseId())),
                                        options);

                        if(bidPrefSuppData != null) {
                            //Log.customer.debug("**%s**:objectmatching="+bidPrefSuppData, thisclass);
                            ClusterRoot prefSuppDataCR = (ClusterRoot)bidPrefSuppData.get();
                            prefSuppDataCR.setFieldValue("Facility", facilityCR);
                            prefSuppDataCR.setFieldValue("Category", categoryCR);
                            BaseVector manufVec = (BaseVector)prefSuppDataCR.getFieldValue("ManufacturerList");
                            manufVec.updateElements(manufList);
                            prefSuppDataCR.setActive(true);
                            prefSuppDataCR.save();
                        } else {
                            ClusterRoot prefSuppDataCR = (ClusterRoot)
                                            BaseObject.create("cat.core.CatPreferredSupplierData", Base.getSession().getPartition());

                            prefSuppDataCR.setFieldValue("PreferredSupplier", supplier);
                            prefSuppDataCR.setFieldValue("Facility", facilityCR);
                            prefSuppDataCR.setFieldValue("Category", categoryCR);
                            BaseVector manufVec = (BaseVector)prefSuppDataCR.getFieldValue("ManufacturerList");
                            manufVec.updateElements(manufList);
                            prefSuppDataCR.save();
                            //Log.customer.debug("**%s**:newObject="+prefSuppDataCR, thisclass);
                        }
                    }
                }

                BaseVector availCntyVec = (BaseVector)supplier.getFieldValue("AvailableCountry");
                availCntyVec.clear();
                List availCntryList = (List)psm.getFieldValue("AvailableCountry");
                availCntyVec.updateElements(availCntryList);

                User pslOwner = (User)psm.getFieldValue("PSLOwner");
                if (pslOwner == null) pslOwner = (User) psm.getFieldValue("Preparer");

                //Set the supplier as preferred, updating values from PSM form
                Log.customer.debug("**%s**:%s updating Supplier objects fields.", thisclass, psm.getUniqueName());
                supplier.setFieldValue("PreferredSupplier", new Boolean(true));
                supplier.setFieldValue("EffectiveDate",psm.getFieldValue("EffectiveDate"));
                supplier.setFieldValue("ExpirationDate",psm.getFieldValue("ExpirationDate"));
                supplier.setFieldValue("ReviewNotifDate",psm.getFieldValue("ReviewNotifDate"));
                supplier.setFieldValue("DistributorURL",psm.getFieldValue("DistributorURL"));
                supplier.setFieldValue("PrefSuppCreator",pslOwner);
                supplier.setFieldValue("AvailableCountry",availCntyVec);

                //Update the selected supplier locations as preferred
                //Need to set locations to false when not in update list
                List supplierLocList = (List)psm.getFieldValue("PreferredSupplierLocation");
                BaseId baseid = null;

                List locationsList = supplier.getLocations();
                boolean exists = false;
                for(Iterator e = supplier.getLocationsObjects(); e.hasNext();) {
                    SupplierLocation location = (SupplierLocation)e.next();
                    exists = false;

                    for (Iterator sl = supplierLocList.iterator(); sl.hasNext();) {
                        baseid = (BaseId) sl.next();
                        supplierLocation = (SupplierLocation)baseid.get();
                        if(location.equals(supplierLocation)) {
                            exists=true;
                            break;
                        }
                    }
                    location.setFieldValue("PreferredSupplierLocation", new Boolean(exists));
                }
                Log.customer.debug("**%s**:- Done updating Supplier objects fields.", thisclass);
            } else if (maintType.equals("Delete")) {
                //Mark the supplier as not preferred
                //Log.customer.debug("**%s**: Delete maintType to be done.", thisclass );
                supplier = (Supplier)psm.getFieldValue("PreferredSupplierToDelete");

                Log.customer.debug("**%s**: mark Supplier"+supplier+"= with uniquename="+supplier.getUniqueName()+" as not preferred.", thisclass);

                supplier.setFieldValue("PreferredSupplier", new Boolean(false));
                setAllSupplierDataInactive(supplier);
                //BaseVector suppFacility = (BaseVector)supplier.getFieldValue("AvailableFacilityMap");
                //suppFacility.clear();

                List locationsList = supplier.getLocations();

                for(Iterator e = supplier.getLocationsObjects(); e.hasNext();) {
                    SupplierLocation location = (SupplierLocation)e.next();
                    location.setFieldValue("PreferredSupplierLocation", new Boolean(false));
                }

            }//end if(create, update, delete)
        }//state approved

        //supplier.save();
		Base.getSession().transactionCommit();

    }

    private void setAllSupplierDataInactive(Supplier supp) {
        Log.customer.debug("**%s**:setAllSupplierDataInactive="+supp+" data to inactive", thisclass);
        String queryTxt ="Select CatPreferredSupplierData "
                              + "FROM cat.core.CatPreferredSupplierData "
                              + "AS CatPreferredSupplierData "
                              + "WHERE PreferredSupplier=%s ";
        AQLQuery aql = AQLQuery.parseQuery(Fmt.S(queryTxt,
                                            AQLScalarExpression.buildLiteral(supp.getBaseId())));
        AQLOptions options = new AQLOptions(Base.getSession().getPartition());

        AQLResultCollection results = Base.getService().executeQuery(aql, options);
        Log.customer.debug(thisclass + "Size = " + results.getSize());

        while (results.next()) {
            BaseId bid = results.getBaseId(0);
            ClusterRoot cr = (ClusterRoot)bid.get();
            //Log.customer.debug("**%s**: cr = " + cr, thisclass);

            cr.setActive(false);
        }
        Log.customer.debug("**%s**:set the data to inactive", thisclass);

    }

    protected ValueInfo getValueInfo () {
        return new ValueInfo(IsScalar, Approvable.ClassName);
    }
}
