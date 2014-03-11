/*
 * CatUpdateFieldsOnPSM.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

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
import ariba.base.fields.ValueSource;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*
 * On selection of Update as Maintenance type and when supplier is
 * is selected, the values from the Supplier fields are copied to the eform
 */
public class CatUpdateFieldsOnPSM extends Action {


    private static final String classname = "CatUpdateFieldsOnPSM: ";

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {
        Log.customer.debug(classname + "firing..");
        Approvable appr = (Approvable)object;
        //Log.customer.debug(classname + "Approvable = " + appr);
        ClusterRoot supp = (ClusterRoot)appr.getFieldValue("PreferredSupplierToUpdate");
        //Log.customer.debug(classname + "PreferredSupplierToUpdate = " + supp);

        if (supp != null) {
/****************
            List suppAvailFacMapList = (List)supp.getFieldValue("AvailableFacilityMap");

            if(! ListUtil.nullOrEmptyList(suppAvailFacMapList)) {
                Log.customer.debug(classname + " updating facility on psl");
                BaseVector pslFacility = (BaseVector)appr.getFieldValue("SupplierFacilityLines");
                pslFacility.clear();
                List psfmNewList = ListUtil.list();

                for(int i = 0; i < suppAvailFacMapList.size(); i++) {
                    BaseObject bo = (BaseObject)suppAvailFacMapList.get(i);
                    BaseObject boForPSL = bo.deepCopyAndStrip();
                    psfmNewList.add(boForPSL);
                }
                pslFacility.updateElements(psfmNewList);
            }
****************/

            BaseVector pslFacility = (BaseVector)appr.getFieldValue("SupplierFacilityLines");
            pslFacility.clear();
            List psfmNewList = ListUtil.list();

            String query = "Select CatPreferredSupplierData "
                          + "FROM cat.core.CatPreferredSupplierData "
                          + "AS CatPreferredSupplierData "
                          + "WHERE PreferredSupplier=%s";

            AQLQuery aql = AQLQuery.parseQuery(Fmt.S(query,
                                                AQLScalarExpression.buildLiteral(supp.getBaseId())));
            AQLOptions options = new AQLOptions(Base.getSession().getPartition());

            Log.customer.debug("%s: aql query =%s" , classname, aql.toString());

            AQLResultCollection results = Base.getService().executeQuery(aql, options);

            if(!ListUtil.nullOrEmptyList(results.getErrors())) {
                String err = results.getErrorStatementText();
                Log.customer.debug("%s: ERROR: %s",classname, err);
            }

            Log.customer.debug("%s: the size of results =" +results.getSize(), classname);

            while (results.next()) {
                BaseId bid = results.getBaseId(0);
                ClusterRoot cr = (ClusterRoot)bid.get();
                Log.customer.debug("**%s**: cr = " + cr, classname);
                BaseObject boForPSL = (BaseObject)BaseObject.create("cat.core.CatPrefSupplierItemsForFacility", Base.getSession().getPartition());

                boForPSL.setFieldValue("Facility", (ClusterRoot)cr.getFieldValue("Facility"));
                boForPSL.setFieldValue("Category", (ClusterRoot)cr.getFieldValue("Category"));
                BaseVector manufVec = (BaseVector)boForPSL.getFieldValue("ManufacturerList");
                manufVec.updateElements((List)cr.getFieldValue("ManufacturerList"));
                psfmNewList.add(boForPSL);

            }
            pslFacility.updateElements(psfmNewList);

            Log.customer.debug(classname + " Updating availcntry and dates .. " );
            BaseVector availCntyVec = (BaseVector)appr.getFieldValue("AvailableCountry");
            availCntyVec.clear();
            List availCntryList = (List)supp.getFieldValue("AvailableCountry");
            availCntyVec.updateElements(availCntryList);

            appr.setFieldValue("AvailableCountry", availCntyVec);
            appr.setFieldValue("EffectiveDate", supp.getFieldValue("EffectiveDate"));
            appr.setFieldValue("ExpirationDate", supp.getFieldValue("ExpirationDate"));
            appr.setFieldValue("ReviewNotifDate", supp.getFieldValue("ReviewNotifDate"));
            appr.setFieldValue("DistributorURL", supp.getFieldValue("DistributorURL"));

            User pslOwner = (User)supp.getFieldValue("PrefSuppCreator");
            appr.setFieldValue("PSLOwner",pslOwner);
        }
    }

    public CatUpdateFieldsOnPSM() {
        super();
    }

}
