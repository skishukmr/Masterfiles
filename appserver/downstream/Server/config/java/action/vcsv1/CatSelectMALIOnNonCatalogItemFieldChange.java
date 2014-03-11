/* Created by KS on Dec 07, 2005
 * --------------------------------------------------------------
 * Override of fire() method to exclude additional charges
 */
package config.java.action.vcsv1;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.ClassUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

public class CatSelectMALIOnNonCatalogItemFieldChange extends Action
{
    private static final String THISCLASS = "CatSelectMALIOnNonCatalogItemFieldChange";

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
            if (!CatAdditionalChargeLineItem.isAdditionalCharge(pli)) {
                Log.customer.debug("%s *** ATTEMPTING TO SET MALI FOR PLI!", THISCLASS);
                ContractLineItem.autoSelectMALineItem(pli);
            }
        }
    }
}