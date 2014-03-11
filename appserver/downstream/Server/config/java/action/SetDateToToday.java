package config.java.action;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.util.core.Date;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class SetDateToToday extends Action
{

    public SetDateToToday()
    {
    }

    public void fire(ValueSource object, PropertyTable params)
    {
        Log.customer.debug("SetDateToToday *  Info   * Executing program version - V01.08.2004");
        BaseObject inputObject = (BaseObject)object;
        String target = (String)params.getPropertyForKey("Target");
        Date date = new Date();
        inputObject.setFieldValue(target, date);
    }

    protected ValueInfo[] getParameterInfo()
    {
        return (new ValueInfo[] {
            new ValueInfo("Target", 0, "java.lang.String")
        });
    }

    private static final String PgmClassName = "SetDateToToday";
    private static final String PgmVersion = "V01.08.2004";
    private static final String PgmWarning = " * Warning * ";
    private static final String PgmError = " *  Error  * ";
    private static final String PgmInfo = " *  Info   * ";
}
