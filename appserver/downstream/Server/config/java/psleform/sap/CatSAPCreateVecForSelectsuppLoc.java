/*
 * CatSAPCreateVecForSelectFacilities.java
 * Created by Chandra on Jan 06, 2006
 *
 */

package config.java.psleform.sap;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import java.util.List;
import ariba.base.core.ClusterRoot;
import ariba.util.log.Log;
import ariba.base.core.BaseVector;

/*
 * Creates the supplierLocation-Category lines in PSL eform based on the
 * selection made by user in SupplierLocation and Categories fields and clicking Trigger button.
 * For each SupplierLocation, a category is mapped and a new record is created.
 */
 public class CatSAPCreateVecForSelectsuppLoc extends Action {

    private static final ValueInfo valueInfo =
        new ValueInfo(0, Approvable.ClassName);

    private static final String thisclass = "CatSAPCreateVecForSelectsuppLoc: ";

    public void fire (ValueSource object, PropertyTable params) {
        Approvable psleform = (Approvable)object;
        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc *** Started creating BaseObject :CatPrefSupplierItemsForFacility");
        List suppLocList = (List)psleform.getDottedFieldValue("PreferredSupplierLocation");
        List categoryList = (List)psleform.getDottedFieldValue("SupplierCategoriesToAdd");
        List manufList = (List)psleform.getDottedFieldValue("SupplierManufacToAdd");

        if (ListUtil.nullOrEmptyList(suppLocList) || ListUtil.nullOrEmptyList(categoryList)) {
        	 Log.customer.debug("CatSAPCreateVecForSelectsuppLoc: PreferredSupplierLocation or SupplierCategoriesToAdd are empty");
            return;
        }

        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :suppLocList "+suppLocList);
        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :categoryList "+categoryList);


        List supFacLinesList = (List)psleform.getFieldValue("SAPSupplierFacilityLines");
        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :Vec size before add=> "+ListUtil.getListSize(supFacLinesList) ,thisclass);

        int suppLocListSize = suppLocList.size();
        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :suppLocListSize "+suppLocListSize);
        int categListSize = categoryList.size();
        Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :categListSize "+categListSize);

        for (int i = 0; i < suppLocListSize; i++) {
            BaseId suppLocBaseId = (BaseId)suppLocList.get(i);
            ClusterRoot supLoc = suppLocBaseId.get();
            Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :supLoc "+supLoc);
            String contactID = (String) supLoc.getDottedFieldValue("ContactID");

            Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :contactID "+contactID);
            ClusterRoot compCode =Base.getService().objectMatchingUniqueName("ariba.core.CompanyCode",Base.getService().getPartition(),contactID);
            for (int j = 0; j < categListSize; j++) {

                BaseId categBaseId = (BaseId)categoryList.get(j);
                ClusterRoot categCR = categBaseId.get();

                Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :categCR "+categCR);
                Log.customer.debug("CatSAPCreateVecForSelectsuppLoc :creating  Base Object");
                BaseObject psfmBO = (BaseObject)BaseObject.create("cat.core.CatSAPPrefSupplierItemsForFacility", psleform.getPartition());
                //psfmBO.setFieldValue("CompanyCode", compCode);
                psfmBO.setFieldValue("SupplierLocation", supLoc);
                psfmBO.setFieldValue("Category", categCR);

                if(!ListUtil.nullOrEmptyList(manufList)) {
                    BaseVector psfmManufVec = (BaseVector)psfmBO.getFieldValue("ManufacturerList");
                    psfmManufVec.clear();
                    psfmManufVec.updateElements(manufList);
                }

                ListUtil.addElementIfAbsent(supFacLinesList, psfmBO);
            }
        }
        //suppLocList.clear();
        categoryList.clear();
        manufList.clear();
    }
    /**
        Returns the list of valid value types.

        @return the list of valid value types.
    */
    protected ValueInfo getValueInfo ()
    {
        return valueInfo;
    }

}
