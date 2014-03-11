package config.java.workflow;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

public class CatSetupAddChargesOnMARApproval extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        ContractRequest mar = (ContractRequest)object;
        Log.customer.debug("%s *** WORKFLOW FOR CONTRACT REQUEST (APPROVED): %s", "CatSetupAddChargesOnMARApproval", mar);
        if(mar.getReleaseType() == 1 && mar.getTermType() == 2)
        {
            BaseVector lines = mar.getLineItems();
            if(!lines.isEmpty())
            {
                int size = lines.size();
                Log.customer.debug("CatSetupAddChargesOnMARApproval *** Lines.size(): " + size);
                while(size > 0)
                {
                	ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(--size);
                    if(CatAdditionalChargeLineItem.isAdditionalCharge(mali))
                    {
                        String auxId = mali.getDescription().getSupplierPartAuxiliaryID();
                        Log.customer.debug("%s *** auxId (1): %s", "CatSetupAddChargesOnMARApproval", auxId);
                        auxId = mar.getUniqueName();
                        auxId = auxId.concat("-").concat(String.valueOf(mali.getNumberInCollection()));
                        Log.customer.debug("%s *** auxId (2): %s", "CatSetupAddChargesOnMARApproval", auxId);
                        mali.setDottedFieldValue("Description.SupplierPartAuxiliaryID", auxId);
                    }
                    Log.customer.debug("CatSetupAddChargesOnMARApproval *** size counter: " + size);
                }
            }
        }
    }

    protected ValueInfo getValueInfo()
    {
        return new ValueInfo(0, "CatSetupAddChargesOnMARApproval");
    }

    public CatSetupAddChargesOnMARApproval()
    {
    }

    private static final String THISCLASS = "CatSetupAddChargesOnMARApproval";
}
