package config.java.action.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;

/*
 * AUL : Changed MasterAgreement to Contract
 */


public class CatSetRefLineNumberForAllLines extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ProcureLineItemCollection)
        {
            ProcureLineItemCollection plic = (ProcureLineItemCollection)object;
            int refNum = 0;
            BaseVector lines = plic.getLineItems();
            int size = lines.size();
            for(int i = 0; i < size; i++)
            {
                ProcureLineItem pli = (ProcureLineItem)lines.get(i);
                if(CatAdditionalChargeLineItem.isAdditionalCharge(pli))
                {
                    if(pli instanceof ReqLineItem)
                    {
                        ReqLineItem rli = (ReqLineItem)pli;
                        ContractLineItem mali = rli.getMALineItem();
                        Log.customer.debug("%s *** MALI for RLI %s", "CatSetRefLineNumberForAllLines", mali);
                        if(mali != null)
                        {
                            Log.customer.debug("%s *** MALI is Additional Charge!", "CatSetRefLineNumberForAllLines");
                            Contract ma = mali.getMasterAgreement();
                            Integer maRefNumInt = (Integer)mali.getFieldValue("ReferenceLineNumber");
                            Log.customer.debug("%s *** MALI refNum (Integer): %s", "CatSetRefLineNumberForAllLines", maRefNumInt);
                            if(maRefNumInt != null)
                            {
                                mali = (ContractLineItem)ma.getLineItem(maRefNumInt.intValue());
                                for(int j = 0; j < size; j++)
                                {
                                    rli = (ReqLineItem)lines.get(j);
                                    if(rli.getMALineItem() != mali)
                                        continue;
                                    refNum = rli.getNumberInCollection();
                                    break;
                                }

                            }
                        } else
                        {
                            refNum = ((Integer)rli.getFieldValue("ReferenceLineNumber")).intValue();
                        }
                    }
                } else
                {
                    refNum = pli.getNumberInCollection();
                }
                Log.customer.debug("CatSetReferenceLineNumber *** refNum: " + refNum);
                pli.setFieldValue("ReferenceLineNumber", new Integer(refNum));
            }

        }
    }

    public CatSetRefLineNumberForAllLines()
    {
    }

    private static final String THISCLASS = "CatSetRefLineNumberForAllLines";
}
