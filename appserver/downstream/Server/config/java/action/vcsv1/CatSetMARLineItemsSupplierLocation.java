package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasteAgreement to Contract
 */

public class CatSetMARLineItemsSupplierLocation extends Action
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof ContractRequest)
        {
        	ContractRequest mar = (ContractRequest)object;
            ariba.common.core.SupplierLocation sloc = mar.getSupplierLocation();
            BaseVector lines = mar.getLineItems();
            if(!lines.isEmpty())
            {
                int size = lines.size();
                for(int i = 0; i < size; i++)
                {
                	ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(i);
                    mali.setSupplierLocation(sloc);
                }

                Log.customer.debug("%s *** Finished setting SuplrLoc for all lines!", "CatSetMARLineItemsSupplierLocation");
            }
        }
    }

    public CatSetMARLineItemsSupplierLocation()
    {
    }

    private static final String THISCLASS = "CatSetMARLineItemsSupplierLocation";
}