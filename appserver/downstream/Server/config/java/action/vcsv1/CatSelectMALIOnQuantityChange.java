/* Created by KS on Dec 07, 2005
 * --------------------------------------------------------------
 * Override of fire() method to exclude additional charges
 */
package config.java.action.vcsv1;

import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.action.SelectContractLineItemOnQuantityChange;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.ClassUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

/*
 * AUL : Changed SelectMALineItemOnQuantityChange to SelectContractLineItemOnQuantityChange
 */

public class CatSelectMALIOnQuantityChange extends SelectContractLineItemOnQuantityChange
{
    private static final String THISCLASS = "CatSelectMALIOnQuantityChange";

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object != null && ClassUtil.instanceOf(object, "ariba.procure.core.ProcureLineItem"))
        {
            ProcureLineItem pli = (ProcureLineItem)object;
            Log.customer.debug("%s *** FIRING for PLI!",THISCLASS);
            if(pli.getSupplier() != null && !CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {
                Log.customer.debug("%s *** ATTEMPTING TO SET MALI FOR PLI!", THISCLASS);
                ContractLineItem.autoSelectMALineItem(pli);
            }
        }
    }
}