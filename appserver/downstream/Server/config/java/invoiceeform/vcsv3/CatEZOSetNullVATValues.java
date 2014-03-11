
package config.java.invoiceeform.vcsv3;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  Author: KS.  
	Simple trigger to reset VAT values for a VAT line type
*/

public class CatEZOSetNullVATValues extends Action {

    private static final String CLASSNAME = "CatEZOSetNullVATValues";
    
    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** object: %s",CLASSNAME,object);

        if (object instanceof BaseObject) {
            BaseObject bo = (BaseObject)object;
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Setting VATRate & TaxAmout to NULL!",CLASSNAME);
            bo.setFieldValue("TaxAmount",null);
            bo.setFieldValue("VATRate",null);
            bo.setFieldValue("IsVATRecoverable",Boolean.FALSE);
        }
    }

    public CatEZOSetNullVATValues() {
        super();
    }


}
