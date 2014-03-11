/*
 * CatSetPSLOwner.java
 * Created by Chandra on Aug 10, 2005
 *
 */

package config.java.psleform;

import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;

/*
 * Trigger to set the Preparer as PSLOwner
 */
public class CatSetPSLOwner
                    extends Action {

    public void fire(ValueSource object, PropertyTable params) {

        Log.customer.debug("CatSetPSLOwner:***object="+object);

        if(object == null)
            return;

        Log.customer.debug("CatSetPSLOwner:***preparer="+object.getFieldValue("Preparer"));
        object.setDottedFieldValue("PSLOwner", object.getFieldValue("Preparer"));
    }

    public CatSetPSLOwner() {}


}
