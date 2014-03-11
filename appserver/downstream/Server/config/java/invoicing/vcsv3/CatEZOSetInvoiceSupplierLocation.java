package config.java.invoicing.vcsv3;


import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.invoicing.core.Invoice;
import ariba.procure.core.action.SetSupplierLocation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CatEZOSetInvoiceSupplierLocation extends SetSupplierLocation {

    private static final String ClassName = "CatEZOSetInvoiceSupplierLocation";

    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        if(valuesource instanceof Invoice)
        {
            Invoice inv = (Invoice)valuesource;
            Supplier supplier = inv.getSupplier();
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Supplier: %s",ClassName, supplier);
            if(supplier != null && inv.getMasterAgreement() == null)
            {
                BaseVector locations = supplier.getLocations();
                if(locations != null && locations.size() == 1)
                {
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s ::: SupplierLoc being set since only 1 exists!", ClassName);
                    ariba.base.core.ClusterRoot cr = Base.getSession().objectFromId((BaseId)locations.get(0));
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s :::* locations(0): %s", "CatEZOSetSupplierLocation", cr);
                    if(cr instanceof SupplierLocation)
                    {
                        inv.setSupplierLocation((SupplierLocation)cr);
                        return;
                    }
                } else
                {
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s ::: Supplier has 0 or Muliple locations, SupplierLoc not set!", ClassName);
                    inv.setSupplierLocation(null);
                    return;
                }
            }
            super.fire(valuesource, propertytable);
        }
    }
}