/*
 * SetPreferredSupplierLocation.java
 * Created by Chandra on Aug 10, 2005
 *
 */

package config.java.psleform;

import java.util.List;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.util.core.PropertyTable;

/*
 * Trigger to set the PreferredSupplierLocation field when
 *  the PreferredSupplierToCreate, PreferredSupplierToUpdate field is set.
 */
public class SetPreferredSupplierLocation
                    extends Action {

    public void fire(ValueSource object, PropertyTable params) {
        String targetFieldName = params.stringPropertyForKey("Target");
        Supplier supplier = (Supplier)params.getPropertyForKey("Source");

        Log.customer.debug("SetPreferredSupplierLocation:***object="+object);
        //Log.customer.debug("SetPreferredSupplierLocation:***supplier="+supplier+"**targetFieldName=%s", targetFieldName);

        if(object == null)
            return;

        List locationsList = null;

        if(supplier != null)
            locationsList = supplier.getLocations();

        //Log.customer.debug("SetPreferredSupplierLocation:***Setting the Locations");


        BaseVector prefSuppLocVec = (BaseVector)object.getFieldValue("PreferredSupplierLocation");
        prefSuppLocVec.clear();
        prefSuppLocVec.updateElements(locationsList);

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

    public SetPreferredSupplierLocation() {}

    private static final String requiredParameterNames[] = {
        "Target", "Source"
    };
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("Target", true, ariba.base.fields.Behavior.IsVector, "ariba.common.core.SupplierLocation"),
        new ValueInfo("Source", 0, "ariba.common.core.Supplier")
    };
    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.core.PrefSupplierMaintEform");

}
