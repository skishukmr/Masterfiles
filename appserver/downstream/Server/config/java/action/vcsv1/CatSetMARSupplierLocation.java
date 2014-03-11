package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.ContractRequest;
import ariba.procure.core.action.SetSupplierLocation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*
 * AUL : Changed MasterAgreement to Contract
 */

public class CatSetMARSupplierLocation extends SetSupplierLocation
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof ContractRequest)
        {
            ContractRequest mar = (ContractRequest)object;
            Supplier supplier = mar.getSupplier();
            if(supplier != null)
            {
                BaseVector locations = supplier.getLocations();
                if(locations != null && locations.size() == 1)
                {
                    Log.customer.debug("%s *** SupplierLoc being set since only 1 exists!", "CatSetMARSupplierLocation");
                    ariba.base.core.ClusterRoot loc = Base.getSession().objectFromId((BaseId)locations.get(0));
                    Log.customer.debug("%s **** locations(0): %s", "CatSetMARSupplierLocation", loc);
                    if(loc instanceof SupplierLocation)
                    {
                        mar.setSupplierLocation((SupplierLocation)loc);
                        return;
                    }
                } else
                {
                    Log.customer.debug("%s *** Supplier has 0 or Muliple locations, SupplierLoc set to NULL!", "CatSetMARSupplierLocation");
                    mar.setSupplierLocation(null);
                    return;
                }
            }
            Log.customer.debug("%s *** Supplier Loc trigger calling SUPER()!", "CatSetMARSupplierLocation");
            super.fire(object, params);
        }
    }

    public CatSetMARSupplierLocation()
    {
    }

    private static final String THISCLASS = "CatSetMARSupplierLocation";
}