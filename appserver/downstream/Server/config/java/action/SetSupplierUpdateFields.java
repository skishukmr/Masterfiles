// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:10:06 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   SetSupplierUpdateFields.java

package config.java.action;

import ariba.approvable.core.Approvable;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class SetSupplierUpdateFields extends Action
{

    public SetSupplierUpdateFields()
    {
    }

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        Log.customer.debug("SetSupplierBasedOnInputField: firing..");
        Approvable approvable = (Approvable)object;
        Log.customer.debug("SetSupplierBasedOnInputField: Approvable = " + approvable);
        ClusterRoot supp = (ClusterRoot)approvable.getFieldValue("SupplierToUpdate2");
        Log.customer.debug("SetSupplierBasedOnInputField: SupplierToUpdate2 = " + supp);
        if(supp != null)
        {
            approvable.setFieldValue("ManufacturersForCreate", supp.getFieldValue("Manufacturers"));
            approvable.setFieldValue("CategoriesVector", supp.getFieldValue("Categories"));
            approvable.setFieldValue("EffectiveDateForCreate", supp.getFieldValue("EffectiveDate"));
            approvable.setFieldValue("ExpirationDateForCreate", supp.getFieldValue("ExpirationDate"));
            approvable.setFieldValue("ReviewNotificationDateForCreate", supp.getFieldValue("ReviewNotificationDate"));
            approvable.setFieldValue("URLInformationLink", supp.getFieldValue("URLInformationLink"));
        }
    }

    private static final String classname = "SetSupplierBasedOnInputField: ";
}