/* Created by KS on Oct 21, 2005
 * Renames Requisition (title) on COPY to ensure Contract Add. Charge defaulting will work (for a copy)
 */
package config.java.action.vcsv1;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;

/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 */


 public class CatSetTitleOnCopy extends Action {

 	private static final String THISCLASS = "CatSetTitleOnCopy";
    public static final String COPY_TEXT = ResourceService.getString("cat.java.vcsv1","CopyTitleText_Ariba");
    public static final String NEW_TEXT = ResourceService.getString("cat.java.vcsv1","CopyTitleText_Cat");

    public void fire(ValueSource vs, PropertyTable params)
            throws ActionExecutionException {

        if (vs instanceof Requisition) {
            Requisition r = (Requisition)vs;
            String title = r.getName();
			Log.customer.debug("%s *** Req TITLE (before): %s",THISCLASS,title);
			if (title.startsWith(COPY_TEXT)) {
			    title = title.replaceFirst(COPY_TEXT, NEW_TEXT);
			    r.setFieldValue("Name",title);
			}
			Log.customer.debug("%s *** Req TITLE (after): %s",THISCLASS,r.getName());
        }
    }


    public CatSetTitleOnCopy() {
        super();

    }

}
