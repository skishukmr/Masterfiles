/* Created by KS on Nov 2, 2005
 * ---------------------------------------------------------------------------------
 * Sets NTE Amount field on ReqLineItem based on Quantity * NTE Price (LIPD) 
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetNotToExceedAmount extends Action {

	private static final String THISCLASS = "CatSetNotToExceedAmount";
    
    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
        
        LineItemProductDescription lipd =  null;
        ReqLineItem rli = null;
        if (object instanceof ReqLineItem) {
            rli = (ReqLineItem)object;
            lipd = rli.getDescription();
        }
        else if (object instanceof LineItemProductDescription){
            lipd = (LineItemProductDescription)object;
            try {
                rli = (ReqLineItem)lipd.getLineItem();
            } 
            catch (ClassCastException cce) {
                Log.customer.debug("%s *** NOT ReqLineItem - ClassCastException - STOP!",THISCLASS);
                return;
            }
        }
        if (rli != null && lipd != null) {
            Money nte = (Money)lipd.getFieldValue("NotToExceedPrice");
            Log.customer.debug("%s *** NTE PRICE: %s",THISCLASS,nte);
            BigDecimal value = new BigDecimal(0);
            if (nte != null && nte.getAmount().compareTo(value) > 0) {
                value = rli.getQuantity();
                Log.customer.debug("%s *** NTE QTY: %s",THISCLASS,value);
                nte = nte.multiply(value);
                Log.customer.debug("%s *** NTE AMOUNT: %s",THISCLASS,nte);
                rli.setFieldValue("NTEAmount",nte);
            }
        }
    }

    public CatSetNotToExceedAmount() {
        super();
    }


}
