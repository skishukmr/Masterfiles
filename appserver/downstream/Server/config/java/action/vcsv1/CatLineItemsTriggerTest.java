/* TESTING of LineItems trigger
 */
package config.java.action.vcsv1;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;

 public class CatLineItemsTriggerTest extends Action {

 	private static final String THISCLASS = "CatLineItemsTriggerTest";

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        if (object instanceof Requisition) {
            Log.customer.debug("%s *** LineItems trigger FIRING!",THISCLASS);
            Requisition r = (Requisition)object;
            String input = "HELPHELP";
            String message = (String)r.getFieldValue("ChargesAddedMessage");
            if (message == null)
                r.setFieldValue("ChargesAddedMessage",input);
            else
                r.setFieldValue("ChargesAddedMessage",null);
    		Log.customer.debug("%s *** Charges Message (after): %s",THISCLASS,r.getFieldValue("ChargesAddedMessage"));
        }
    }


    public CatLineItemsTriggerTest() {
        super();

    }

}
