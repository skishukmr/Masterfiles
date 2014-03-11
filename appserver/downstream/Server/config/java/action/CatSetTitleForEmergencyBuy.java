/*
 * Created by KS on Jun 30, 2005
 * -------------------------------------------------------------------------------
 * Updates title (Requisition Name) field to include !!! Emergency Buy !!! to ensure
 * these requisition sort to the top of the approval table
 */
package config.java.action;

import ariba.base.core.Base;
import ariba.base.core.Log;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import config.java.common.CatConstants;
/*
 * AUL : Changed Fmt.sil to ResourceService.getString
 */
 public class CatSetTitleForEmergencyBuy extends Action {

 	private static final String THISCLASS = "CatSetTitleForEmergencyBuy";
    String EmergencyText = ResourceService.getString("cat.java.common","EmergencyBuyTitleText");;

    public void fire(ValueSource vs, PropertyTable params)
            throws ActionExecutionException {

		Partition p = Base.getSession().getPartition();
		if(p.getName().equals("mfg1"))
			EmergencyText = ResourceService.getString("cat.java.common","MFGEmergencyBuyTitleText");

		Log.customer.debug("Emergency Buy Text is "+EmergencyText);

        if (vs instanceof Requisition) {
            Requisition r = (Requisition)vs;
        	Boolean stat = (Boolean)r.getFieldValue("EmergencyBuy");
        	if (stat != null) {
        		String title = r.getName();
        		int index = -1;
        		// AUL, sdey : Resolved null pointer exception
        		if(title!=null){
        			index = title.indexOf(EmergencyText);
        		}
        		if (CatConstants.DEBUG)
        		    Log.customer.debug("%s *** Title (BEFORE): %s",THISCLASS,title);
        		if (stat.booleanValue() && index<0){
        			title = EmergencyText + title;
        			r.setFieldValue("Name",title);
        		}
        		else if (!stat.booleanValue() && index>-1) {
        		    int length = EmergencyText.length();
        		    title = title.substring(index+EmergencyText.length());
 //       			title = title.substring(EmergencyText.length());
        			r.setFieldValue("Name",title);
        		}
        		if (CatConstants.DEBUG)
        		    Log.customer.debug("%s *** Title (AFTER): %s",THISCLASS,title);
        	}
        }


    }
    public CatSetTitleForEmergencyBuy() {
        super();

    }

}
