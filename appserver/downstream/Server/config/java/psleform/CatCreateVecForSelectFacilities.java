/*
 * CatCreateVecForSelectFacilities.java
 * Created by Chandra on Jan 06, 2006
 *
 */

package config.java.psleform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*
 * Creates the facility-Category lines in PSL eform based on the
 * selection made by user in Facility and Categories field and clicking Trigger button.
 * For each facility, a category is mapped and a new record is created.
 */
 public class CatCreateVecForSelectFacilities extends Action {

    private static final ValueInfo valueInfo =
        new ValueInfo(0, Approvable.ClassName);

    private static final String thisclass = "CatCreateVecForSelectFacilities: ";

    public void fire (ValueSource object, PropertyTable params) {
        Approvable psleform = (Approvable)object;

        List facilityList = (List)psleform.getFieldValue("SupplierFacilitiesToAdd");
        List categoryList = (List)psleform.getFieldValue("SupplierCategoriesToAdd");
        List availCntryList = (List)psleform.getFieldValue("AvailableCountry");
        List manufList = (List)psleform.getFieldValue("SupplierManufacToAdd");

        if (ListUtil.nullOrEmptyList(facilityList) || ListUtil.nullOrEmptyList(categoryList)) {
            return;
        }


        // Get number of invoice lines to use for the invoice line number
        List supFacLinesList = (List)psleform.getFieldValue("SupplierFacilityLines");

        Log.customer.debug("Vec size before add="+ListUtil.getListSize(supFacLinesList) ,thisclass);


        int facListSize = facilityList.size();
        int categListSize = categoryList.size();

        for (int i = 0; i < facListSize; i++) {
            BaseId facBaseId = (BaseId)facilityList.get(i);
            ClusterRoot facCR = facBaseId.get();

            for (int j = 0; j < categListSize; j++) {
                BaseId categBaseId = (BaseId)categoryList.get(j);
                ClusterRoot categCR = categBaseId.get();

                Log.customer.debug("**%s**:- creating new psfm", thisclass);
                BaseObject psfmBO = (BaseObject)BaseObject.create("cat.core.CatPrefSupplierItemsForFacility", psleform.getPartition());

                psfmBO.setFieldValue("Facility", facCR);
                psfmBO.setFieldValue("Category", categCR);

                if(!ListUtil.nullOrEmptyList(manufList)) {
                    BaseVector psfmManufVec = (BaseVector)psfmBO.getFieldValue("ManufacturerList");
                    psfmManufVec.clear();
                    psfmManufVec.updateElements(manufList);
                }

                ListUtil.addElementIfAbsent(supFacLinesList, psfmBO);
            }
        }
        facilityList.clear();
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
