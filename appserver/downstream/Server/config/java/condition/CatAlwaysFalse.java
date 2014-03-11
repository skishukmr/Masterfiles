package config.java.condition;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.common.core.Log;
import ariba.util.core.PropertyTable;

public class CatAlwaysFalse extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
        Log.customer.debug("**CatAlwaysFalse**returning false");
        return false;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        return null;
    }

    public CatAlwaysFalse()
    {
    }
}
