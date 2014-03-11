package config.java.action;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetHoldTillDate extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof Requisition)
        {
            Requisition r = (Requisition)object;
            if(r.getPreviousVersion() != null)
            {
                Log.customer.debug("CatSetHoldTillDate *** Setting theHoldTillDate to null");
                r.setFieldValue("HoldTillDate", null);
            }
        }
    }

    public CatSetHoldTillDate()
    {
    }
}
