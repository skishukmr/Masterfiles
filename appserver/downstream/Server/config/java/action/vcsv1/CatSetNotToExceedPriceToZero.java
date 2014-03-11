/* Created by KS on Nov 2, 2005
 * --------------------------------------------------------------
 * Used to zero out NTE price to avoid including prior amounts in logic (e.g, Edit Rule)
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatSetNotToExceedPriceToZero extends Action {

    private static final String THISCLASS = "CatSetNotToExceedPriceToZero";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof LineItemProductDescription) {
            LineItemProductDescription lipd = (LineItemProductDescription)object;
            ProcureLineItem pli = lipd.getLineItem();
            if (pli instanceof ReqLineItem) {
	            String reason = (String)lipd.getFieldValue("ReasonCode");
	            Log.customer.debug("%s *** ReasonCode: %s",THISCLASS, reason);
	            if (reason != null && reason.indexOf("xceed") < 0) { 
	                Log.customer.debug("%s *** ReasonCode changed, resetting NTE Price!",THISCLASS);
	                Currency curr = Currency.getBaseCurrency();
	                Money amount = lipd.getPrice();
	                if (amount != null) 
	                    curr = amount.getCurrency();
	                amount = new Money(new BigDecimal(0), curr);
	                Log.customer.debug("%s *** Set NTEPrice to Zero! %s",THISCLASS,amount);
	                lipd.setFieldValue("NotToExceedPrice",amount);
	                pli.setFieldValue("NTEAmount",amount);
	            }    
            }
        }     
    }
    
    public CatSetNotToExceedPriceToZero() {
        super();
    }


}
