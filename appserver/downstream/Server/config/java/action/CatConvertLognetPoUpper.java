// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:09:44 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatConvertLognetPoUpper.java

package config.java.action;

import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.base.fields.action.SetField;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;

public class CatConvertLognetPoUpper extends SetField
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof ReqLineItem)
        {
            ReqLineItem rli = (ReqLineItem)object;
            Log.customer.debug("CatConvertLognetPoUpper ***  :" + rli);
            String lognetPoprefix = null;
            lognetPoprefix = (String)rli.getFieldValue("LognetPOPrefix");
            lognetPoprefix.toUpperCase();
            rli.setFieldValue("LognetPOPrefix", lognetPoprefix);
        }
        Log.customer.debug("Exiting CatConvertLognetPoUpper ** trigger");
    }

    public CatConvertLognetPoUpper()
    {
    }
}