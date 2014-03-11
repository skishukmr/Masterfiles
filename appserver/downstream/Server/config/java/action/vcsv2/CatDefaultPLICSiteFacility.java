/*
 * Created by KS on April 14, 2005
 * -------------------------------------------------------------------------------
 * Used to set SiteFacility in PLIC header to value set by user in User Profile 
 */
package config.java.action.vcsv2;

import config.java.common.CatConstants;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.User;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatDefaultPLICSiteFacility extends Action {

	private static final String THISCLASS = "CatDefaultPLICSiteFacility";

	
    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {
 
        if (object instanceof ProcureLineItemCollection) {
            
            ProcureLineItemCollection plic = (ProcureLineItemCollection)object;
            ariba.user.core.User requester = plic.getRequester();
            Partition part = plic.getPartition();
            if (requester != null && part != null) {
                User partuser = User.getPartitionedUser(requester, part);
                if (partuser != null) {
                    ClusterRoot site = (ClusterRoot)partuser.getFieldValue("SiteFacility");
                    if (CatConstants.DEBUG)
                        Log.customer.debug(" %s **** SiteFaclity: %s", THISCLASS, site);
                    plic.setFieldValue("SiteFacility",site);
                }
            }
        }

    }
    public CatDefaultPLICSiteFacility() {
        super();
    }

}
