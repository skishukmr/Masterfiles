/*
 * CatCreateVecForCntryFacilities.java
 * Created by Chandra on Jan 06, 2006
 *
 */

package config.java.psleform;

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
import ariba.base.core.aql.*;
import ariba.base.core.Partition;
import ariba.base.core.BaseVector;
/*
 * Creates the facility-Category lines in PSL eform based on the
 * selection made by user in Facility and Categories field and clicking Trigger button.
 * For each facility, a category is mapped and a new record is created.
 */
 public class CatCreateVecForCntryFacilities extends Action {

    private static final ValueInfo valueInfo =
        new ValueInfo(0, Approvable.ClassName);

    private static final String thisclass = "CatCreateVecForCntryFacilities: ";

    public void fire (ValueSource object, PropertyTable params) {
        Approvable psleform = (Approvable)object;

        List availCntryList = (List)psleform.getFieldValue("AvailableCountry");
        List categoryList = (List)psleform.getFieldValue("SupplierCategoriesToAdd");
        List manufList = (List)psleform.getFieldValue("SupplierManufacToAdd");

        if (ListUtil.nullOrEmptyList(availCntryList) || ListUtil.nullOrEmptyList(categoryList)) {
            return;
        }

        // Get number of invoice lines to use for the invoice line number
        List supFacLinesList = (List)psleform.getFieldValue("SupplierFacilityLines");

        Log.customer.debug("Vec size before add="+ListUtil.getListSize(supFacLinesList) ,thisclass);

        String queryTxt = "SELECT CatFacility FROM cat.core.CatFacility AS CatFacility";
        AQLQuery query = AQLQuery.parseQuery(queryTxt);

        if(! ListUtil.nullOrEmptyList(availCntryList)) {

            List scaleExprList = AQLScalarExpression.buildScalarExpressionList(availCntryList);

            AQLFieldExpression aqlFieldExpression = new AQLFieldExpression("CatFacility.Country");

            AQLCondition aqlCond = AQLCondition.buildIn(aqlFieldExpression, scaleExprList);
            Log.customer.debug("%s : after format conditionTxt=%s", thisclass, aqlCond.toString());
            query.and(aqlCond);
        }

        AQLOptions options =  new AQLOptions(Partition.None);

        Log.customer.debug("%s : query in here=%s", thisclass, query.toString());
        AQLResultCollection res = Base.getService().executeQuery(query, options);

        if(!ListUtil.nullOrEmptyList(res.getErrors())) {
            String err = res.getErrorStatementText();
            Log.customer.debug("%s: ERROR: %s",thisclass, err);
        }

        Log.customer.debug("%s: the size of results =" +res.getSize(), thisclass);

        int categListSize = categoryList.size();

        while (res.next()) {
            Log.customer.debug("%s: the object got =" +res.getObject(0), thisclass);
            BaseId facBaseId = (BaseId)res.getObject(0);
            ClusterRoot facilityCR = (ClusterRoot)facBaseId.get();

            for (int j = 0; j < categListSize; j++) {
                BaseId categBaseId = (BaseId)categoryList.get(j);
                ClusterRoot categCR = categBaseId.get();

                Log.customer.debug("**%s**:- creating new psfm", thisclass);
                BaseObject psfmBO = (BaseObject)BaseObject.create("cat.core.CatPrefSupplierItemsForFacility", psleform.getPartition());

                //psfmBO.setFieldValue("FacilityName", "FacilityName (CD)");
                psfmBO.setFieldValue("Facility", facilityCR);
                psfmBO.setFieldValue("Category", categCR);

                if(!ListUtil.nullOrEmptyList(manufList)) {
                    BaseVector psfmManufVec = (BaseVector)psfmBO.getFieldValue("ManufacturerList");
                    psfmManufVec.clear();
                    psfmManufVec.updateElements(manufList);
                }

                ListUtil.addElementIfAbsent(supFacLinesList, psfmBO);
            }
        }
        availCntryList.clear();
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
