package config.java.invoiceeform.vcsv3;


import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatEZOSetInvEformSupplierLocation extends Action {
    
    private static final String ClassName = "CatEZOSetInvEformSupplierLocation";

    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        if(valuesource instanceof BaseObject)
        {
            BaseObject eform = (BaseObject)valuesource;
            Supplier supplier = (Supplier)eform.getFieldValue("Supplier");
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Supplier: %s",ClassName, supplier);     
            if(supplier != null)
            {
                BaseVector locations = supplier.getLocations();
                if(locations != null && locations.size() == 1)
                {
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s ::: SupplierLoc being set since only 1 exists!", ClassName);
                    ariba.base.core.ClusterRoot cr = Base.getSession().objectFromId((BaseId)locations.get(0));
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s :::* locations(0): %s", ClassName, cr);
                    if(cr instanceof SupplierLocation)
                    {
                        eform.setFieldValue("SupplierLocation",(SupplierLocation)cr);
                        return;
                    }
                } else
                {
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s ::: Supplier has 0 or Muliple locations, SupplierLoc not set!", ClassName);
                    eform.setFieldValue("SupplierLocation",null);
                    return;
                }
            } 
        }
    }
}