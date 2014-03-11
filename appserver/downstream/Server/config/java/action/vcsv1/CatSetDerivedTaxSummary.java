/*
 * Created by KS on Sep 25, 2005
 * --------------------------------------------------------------
 * Used to set Tax Summary for display in LineItemSimpleGeneralFields group
 * Changes:
   Chandra 28-Nov-07   Issue 217, fixed the display of the Rate field to show 4 decimals.
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatSetDerivedTaxSummary extends Action {

    private static final String THISCLASS = "CatSetDerivedTaxSummary";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            StringBuffer taxsummary = new StringBuffer();

            ClusterRoot taxcode = (ClusterRoot)pli.getFieldValue("TaxCode");
            if (taxcode != null)
                taxsummary.append(taxcode.getUniqueName());
            else
                taxsummary.append("null");

            taxsummary.append(", ");
            ClusterRoot taxstate = (ClusterRoot)pli.getFieldValue("TaxState");
            if (taxstate != null)
                taxsummary.append(taxstate.getUniqueName());
            else
                taxsummary.append("null");

            taxsummary.append(", ");
            BigDecimal taxrate = (BigDecimal)pli.getFieldValue("TaxRate");
            if (taxrate != null) {
				taxrate = taxrate.setScale(4, 0);
                taxsummary.append(taxrate.toString()).append("%");
            }
            Log.customer.debug("%s **** taxsummary SB: %s",THISCLASS,taxsummary);
            pli.setFieldValue("DerivedTaxSummary",taxsummary.toString());
        }
    }

    public CatSetDerivedTaxSummary() {
        super();
    }


}
