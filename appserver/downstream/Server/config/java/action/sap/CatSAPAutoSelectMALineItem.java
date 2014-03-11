package config.java.action.sap;

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
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;

/**
    AUL: Changed ContractLineItem to ContractLineItem
    AUL: Changed ContractLineItemSource to ContractLineItemSource

*/
public class CatSAPAutoSelectMALineItem extends Action
{
    private static final String THISCLASS = "CatSAPAutoSelectMALineItem";

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
        if((pli instanceof ContractLineItemSource) && pli.getSupplier() != null && !CatSAPAdditionalChargeLineItem.isAdditionalCharge(pli)) {
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

    public CatSAPAutoSelectMALineItem()
    {
    }

    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.procure.core.ProcureLineItem");

}