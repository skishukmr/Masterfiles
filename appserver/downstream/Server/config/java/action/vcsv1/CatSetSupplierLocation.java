package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.action.SetSupplierLocation;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetSupplierLocation extends SetSupplierLocation
{

    public void fire(ValueSource object, PropertyTable params)
    {
        if(object instanceof ReqLineItem)
        {
            ReqLineItem rli = (ReqLineItem)object;
            Supplier supplier = rli.getSupplier();
            if(supplier != null && rli.getIsAdHoc() && rli.getMasterAgreement() == null)
            {
                BaseVector locations = supplier.getLocations();
                if(locations != null && locations.size() == 1)
                {
                    Log.customer.debug("%s *** SupplierLoc being set since only 1 exists!", "CatSetSupplierLocation");
                    ariba.base.core.ClusterRoot loc = Base.getSession().objectFromId((BaseId)locations.get(0));
                    Log.customer.debug("%s **** locations(0): %s", "CatSetSupplierLocation", loc);
                    if(loc instanceof SupplierLocation)
                    {
                        rli.setSupplierLocation((SupplierLocation)loc);
                        return;
                    }
                } else
                {
                    Log.customer.debug("%s *** Supplier has 0 or Muliple locations, SupplierLoc not set!", "CatSetSupplierLocation");
                    rli.setSupplierLocation(null);
                    return;
                }
            }
            super.fire(object, params);
        }
    }

    public CatSetSupplierLocation()
    {
    }

    private static final String THISCLASS = "CatSetSupplierLocation";
}
