/* Created by KS on Oct 1, 2005 (slighty modified version of config.java.action class by same name)
 */
package config.java.action.sap;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;

 public class CatSetTitleForCriticalAssetDown extends Action {

 	private static final String THISCLASS = "CatSetTitleForCriticalAssetDown";
    public static final String EmergencyText = ResourceService.getString("cat.java.common","EmergencyBuyTitleText");

    public void fire(ValueSource vs, PropertyTable params)
            throws ActionExecutionException {

        if (vs instanceof Requisition) {
            Requisition r = (Requisition)vs;
        	Boolean stat = (Boolean)r.getFieldValue("CriticalAssetDown");
    		String title = r.getName();
    		Log.customer.debug("%s *** Req TITLE (before): %s",THISCLASS,title);
    		int index = -1;
    		// AUL, sdey : Resolved null pointer exception
    		if(title!=null){
    			index = title.indexOf(EmergencyText);
    		}
    		if ((stat == null || !stat.booleanValue()) && index>-1) {
    		    int length = EmergencyText.length();
    		    title = title.substring(0,index) + title.substring(index+EmergencyText.length());
    		    r.setFieldValue("Name",title);
    		}
    		else if (stat != null && stat.booleanValue() && index<0){
				title = EmergencyText + title;
				r.setFieldValue("Name",title);
			}
    		Log.customer.debug("%s *** Req TITLE (after): %s",THISCLASS,title);
        }
    }


    public CatSetTitleForCriticalAssetDown() {
        super();

    }

}
