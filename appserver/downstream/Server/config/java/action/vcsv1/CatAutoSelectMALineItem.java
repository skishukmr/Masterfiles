/* Created by A. Kirkpatrick on Dec 01, 2005
 * --------------------------------------------------------------
 * Override of fire() method to exclude additional charges (was causing a problem in manually added AC lines)
 */
package config.java.action.vcsv1;

import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractLineItemSource;
import ariba.contract.core.Log;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

public class CatAutoSelectMALineItem extends Action
{
    private static final String THISCLASS = "CatAutoSelectMALineItem";

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object == null)
            return;
        ProcureLineItem pli = null;
        if(object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;
        else
        if(object instanceof LineItemProductDescription)
        {
            pli = ((LineItemProductDescription)object).getLineItem();
        } else
        {
            Log.contract.warning(7967, object);
            return;
        }
        if(pli == null)
        {
        	Log.contract.debug("Could not get ProcureLineItem from %s", object);
            return;
        }
        if((pli instanceof ContractLineItemSource) && pli.getSupplier() != null && !CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {
            Log.customer.debug("%s *** AUTO-SELECTING MALI for PLI!",THISCLASS);
            ContractLineItem.autoSelectMALineItem(pli);
            if (pli instanceof ReqLineItem) {
                ReqLineItem rli = (ReqLineItem)pli;
                Log.customer.debug("%s *** MALI set for PLI: %s",THISCLASS,rli.getMALineItem());
            }
        } else {
            Log.customer.debug("%s *** SKIPPING AUTO-SELECT MALI!",THISCLASS);
        }
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    public CatAutoSelectMALineItem()
    {
    }

    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.procure.core.ProcureLineItem");

}