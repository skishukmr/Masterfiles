package config.java.action.sap;

import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.action.SelectContractLineItemOnQuantityChange;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.ClassUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;

/**
 *
 * AUL : Changed SelectMALIOnQuantityChange to SelectContractLineItemOnQuantityChange
 *
 */

public class CatSAPSelectMALIOnQuantityChange extends SelectContractLineItemOnQuantityChange
{
    private static final String THISCLASS = "CatSAPSelectMALIOnQuantityChange";

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object != null && ClassUtil.instanceOf(object, "ariba.procure.core.ProcureLineItem"))
        {
            ProcureLineItem pli = (ProcureLineItem)object;
            Log.customer.debug("%s *** FIRING for PLI!",THISCLASS);
            if(pli.getSupplier() != null && !CatSAPAdditionalChargeLineItem.isAdditionalCharge(pli)) {
                Log.customer.debug("%s *** ATTEMPTING TO SET MALI FOR PLI!", THISCLASS);
                ContractLineItem.autoSelectMALineItem(pli);
            }
        }
    }
}