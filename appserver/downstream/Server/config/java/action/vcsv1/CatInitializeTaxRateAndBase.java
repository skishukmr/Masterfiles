/*
 * Created by KS on Nov 1, 2005
 * --------------------------------------------------------------
 * Used to initialize TaxRate and TaxBase prior to first use 
 * (needed to ensure TaxAllOverrideField reflects true overrides)
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatInitializeTaxRateAndBase extends Action {

    private static final String THISCLASS = "CatInitializeTaxRateAndBase";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            BigDecimal rate = (BigDecimal)pli.getFieldValue("TaxRate");
            BigDecimal base = (BigDecimal)pli.getFieldValue("TaxBase");
            Log.customer.debug("%s *** TaxRate, TaxBase (BEFORE): %s, %s", THISCLASS, rate, base);
            if (rate == null) {
                rate = new BigDecimal(0.0000);
                pli.setFieldValue("TaxRate",rate);
            }
            if (base == null)
                pli.setFieldValue("TaxBase",rate);
            Log.customer.debug("%s *** TaxRate, TaxBase (AFTER): %s, %s", THISCLASS, 
                    pli.getFieldValue("TaxRate"), pli.getFieldValue("TaxBase"));
        }     
    }
    
    public CatInitializeTaxRateAndBase() {
        super();
    }


}
