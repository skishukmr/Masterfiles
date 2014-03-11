/*
 * Created by KS on Nov 20, 2005
 * --------------------------------------------------------------
 * Used to reinitialize TaxCodeOverride and TaxAllFieldsOverride for Change Orders
 * On Req V2+ overrides copy over from previous version - so must reset)
 * Need to ensure doesn't affect COPY (values should copy over)
 */
package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatInitializeTaxOverrides extends Action {

    private static final String THISCLASS = "CatInitializeTaxRateAndBase";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof Requisition) {
            Requisition r = (Requisition)object;   
            Integer version = r.getVersionNumber();
            Log.customer.debug("%s *** VersionNumber: %s",THISCLASS,version);
            if (version != null && version.intValue()>1 && r.getLineItemsCount() > 0) {
                BaseVector lines = r.getLineItems();
                int size = lines.size();
                for (int i=0;i<size;i++) {
                    ReqLineItem rli = (ReqLineItem)lines.get(i);
                    Log.customer.debug("%s *** TaxAllFieldsOverride (BEFORE): %s", THISCLASS,
    	                    rli.getFieldValue("TaxAllFieldsOverride"));
                    rli.setFieldValue("TaxCodeOverride",new Boolean(false));
                    rli.setFieldValue("TaxAllFieldsOverride",new Boolean(false));
    	            Log.customer.debug("%s *** TaxAllFieldsOverride (AFTER): %s", THISCLASS,
    	                    rli.getFieldValue("TaxAllFieldsOverride"));	
                }               
            }
        }       
    }
    
    public CatInitializeTaxOverrides() {
        super();
    }


}
