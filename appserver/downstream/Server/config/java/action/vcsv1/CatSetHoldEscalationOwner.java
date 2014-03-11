/* Created by KS on Oct 23, 2005
 * Sets HoldEscalationOwner field to name of User who set HoldEscalation = TRUE
 */
package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;


 public class CatSetHoldEscalationOwner extends Action {

 	private static final String THISCLASS = "CatSetHoldEscalationOwner";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof Requisition) {
            Requisition r = (Requisition)object;
            Boolean hold = (Boolean)r.getFieldValue("HoldEscalation");
            String owner = (String)r.getFieldValue("HoldEscalationOwner");
			Log.customer.debug("%s *** HOLD/OWNER (before): %s / %s",THISCLASS,hold,owner);
			if (Boolean.TRUE.equals(hold)) {
			    ClusterRoot actor = Base.getSession().getRealUser();
	//		    Log.customer.debug("%s *** ACTOR: %s",THISCLASS,actor);
			    if (actor instanceof User) {
			        User user = (User)actor;
			        actor = null;
			        owner = user.getName()== null ? user.getUniqueName() : user.getName().getPrimaryString();
			        Log.customer.debug("%s *** HoldEscalation is TRUE, setting OWNER to: %s",THISCLASS,owner);
			        r.setFieldValue("HoldEscalationOwner",owner);
	//		        Log.customer.debug("%s *** OWNER (after): %s",THISCLASS,r.getFieldValue("HoldEscalationOwner"));
			    }
			} 
			else if (Boolean.FALSE.equals(hold)) { // means user unchecked box 
			    Log.customer.debug("%s *** HoldEscalation is FALSE, resetting Owner to null!",THISCLASS);
			    r.setFieldValue("HoldEscalationOwner",null);
			}
        }
    }


    public CatSetHoldEscalationOwner() {
        super();

    }

}
