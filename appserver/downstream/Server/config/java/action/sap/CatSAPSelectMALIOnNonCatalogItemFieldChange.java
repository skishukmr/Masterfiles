package config.java.action.sap;


import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.ClassUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;

public class CatSAPSelectMALIOnNonCatalogItemFieldChange extends Action
{
    private static final String THISCLASS = "CatSAPSelectMALIOnNonCatalogItemFieldChange";

    public void fire(ValueSource object, PropertyTable params)
    {
        ProcureLineItem pli = null;
        if(object instanceof ProcureLineItem)
            pli = (ProcureLineItem)object;
        else
        if(object instanceof LineItemProductDescription)
        {
            LineItemProductDescription pd = (LineItemProductDescription)object;
            pli = pd.getLineItem();
        }
        if(ClassUtil.instanceOf(pli, "ariba.purchasing.core.ReqLineItem") && pli.getClusterRoot() != null
                && !pli.getIsFromCatalog())  {
            Log.customer.debug("%s *** FIRING for PLI!",THISCLASS);
            if (!CatSAPAdditionalChargeLineItem.isAdditionalCharge(pli)) {
                Log.customer.debug("%s *** ATTEMPTING TO SET MALI FOR PLI!", THISCLASS);
                ContractLineItem.autoSelectMALineItem(pli);
            }
        }
    }
}