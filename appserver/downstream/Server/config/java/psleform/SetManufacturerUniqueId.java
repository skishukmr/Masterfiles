/*
 * SetManufacturerUniqueId.java
 * Created by Chandra on Aug 10, 2005
 *
 */

package config.java.psleform;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

/*
 * Generates a uniqueId for Manufacturer
 */
public class SetManufacturerUniqueId extends Action
{
    private static final String classname = "SetManufacturerUniqueId: ";

    public void fire(ValueSource object, PropertyTable params) {
        Log.customer.debug("SetManufacturerUniqueId firing.. ");

        if (object != null) {
            Log.customer.debug("SetManufacturerUniqueId valuesource: " + object);

            ClusterRoot cr = (ClusterRoot)object;
            //Log.customer.debug(classname + "cr = " + cr);
            String idPrefix = "DI";
            //Log.customer.debug(classname + "idPrefix = " + idPrefix);
            long next = Base.getService().getNextNamedLong(idPrefix);
            //Log.customer.debug(classname + "next = " + next);
            String unique = StringUtil.strcat(idPrefix, Long.toString(next));
            //Log.customer.debug(classname + "unique = " + unique );
            cr.setFieldValue("UniqueName",unique);
            //Log.customer.debug(classname + "cr unique = " + cr.getUniqueName());
        }//end if

    }

    protected String[] getRequiredParameterNames() {
        return null;
    }
}
