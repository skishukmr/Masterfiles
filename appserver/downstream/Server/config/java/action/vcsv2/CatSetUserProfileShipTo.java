/*
 * Created by KS on April 14, 2005
 * --------------------------------------------------------------
 * Used to set ShipTo in UserProfileDetails based on SiteFacility
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.UserProfileDetails;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;

/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 */

public class CatSetUserProfileShipTo extends Action {

	private static final String THISCLASS = "CatSetUserProfileShipTo";
	private static final String ADDRESSCLASS = "ariba.common.core.Address";
	private static final String LOOKUP = "DefaultShipToForSite_";


    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        if (object instanceof UserProfileDetails) {

            UserProfileDetails upd = (UserProfileDetails)object;
            ClusterRoot site = (ClusterRoot)upd.getFieldValue("SiteFacility");
            if (site != null){
                String key = LOOKUP + site.getUniqueName();
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s **** lookup key: %s", THISCLASS, key);
                String uname = ResourceService.getString("cat.java.vcsv2",key);
                if (uname != null){
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s **** address UN: %s", THISCLASS, uname);
                    ClusterRoot address = Base.getService().objectMatchingUniqueName(ADDRESSCLASS,
                            Base.getSession().getPartition() ,uname);
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s **** address: %s", THISCLASS, address);
                    if (address != null) {
                        upd.setShipTo((Address) address);
                    }
                }

            }
        }

    }
    public CatSetUserProfileShipTo() {
        super();
    }

}
