
package config.java.invoiceeform.vcsv3;

import java.math.BigDecimal;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  Author: KS.  
	Simple trigger to set TaxAmount (VAT) on line item using VATRate and Amount.
*/

public class CatEZOSetInvLineVATAmount extends Action {

    private static final String CLASSNAME = "CatEZOSetInvLineVATAmount";
    private static final BigDecimal div100 = new BigDecimal(".01");

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** object: %s",CLASSNAME,object);
        BaseObject bo = null;
        if (object instanceof BaseObject) {
            bo = (BaseObject)object;
            ProcureLineType plt = (ProcureLineType)bo.getFieldValue("LineType");
            
            // if LineType is not Tax, update TaxAmount
            if (plt != null && plt.getCategory() != 2) {
	            Money lineAmt = (Money)bo.getFieldValue("Amount");
	            if (lineAmt != null) {
	                BigDecimal rate = (BigDecimal)bo.getFieldValue("VATRate");
	                if (rate != null) {
	                    Money vatAmt = lineAmt.multiply(rate).multiply(div100);
	                    //if (Log.customer.debugOn)
	                        Log.customer.debug("%s *** vatAmt: %s",CLASSNAME,vatAmt);
	                    bo.setFieldValue("TaxAmount",vatAmt);
	                }
	            }
            }
        }
    }

    public CatEZOSetInvLineVATAmount() {
        super();
    }


}
