/*
 * Created by KS on Sep 19
 * --------------------------------------------------------------
 * Used to set US Address fields from cat.core.TrafficCityStateCode
 */
package config.java.action.vcsv1;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetAddressFieldsFromCityStateCode extends Action {

    private static final String THISCLASS = "CatSetAddressFieldsFromCityStateCode";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        Address address = null;

        if (object instanceof Address)
            address = (Address)object;

        else if (object instanceof ProcureLineItem)  {
            ProcureLineItem pli = (ProcureLineItem)object;
            address = pli.getShipTo();
        }
        if (address != null) {
            ClusterRoot citystate = (ClusterRoot)address.getFieldValue("TrafficCityStateCode");
            ClusterRoot creator = address.getCreator();
            Log.customer.debug("%s *** creator: %s,  citystate: %s",THISCLASS,creator,citystate);
            if (creator != null && citystate != null) {
                // Set the corresponding Address fields
                address.setFieldValue("CityStateCode",citystate.getFieldValue("CityStateCode"));
                address.setDottedFieldValue("PostalAddress.City",citystate.getFieldValue("CityName"));
                address.setDottedFieldValue("PostalAddress.State",citystate.getFieldValue("StateName"));
                address.setDottedFieldValue("PostalAddress.PostalCode",citystate.getFieldValue("PostalCode"));
                Log.customer.debug("%s *** CityStateCode (after): %s",THISCLASS, address.getFieldValue("CityStateCode"));
                Log.customer.debug("%s *** City (after): %s",THISCLASS, address.getDottedFieldValue("PostalAddress.City"));
                address.save();
  //              ariba.base.core.Base.getSession().transactionCommit();
            }

        }

    }

    public CatSetAddressFieldsFromCityStateCode() {
        super();
    }


}
