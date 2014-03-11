/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/22/2006
	Description: 	Trigger implemented to set Supplier Location for Supplier
					when only 1 location exists, for multiple user needs to
					identify which one
-------------------------------------------------------------------------------
	Change Author: 	
	Date Created:  	
	Description: 	
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.action.SetSupplierLocation;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZOSetSupplierLocation extends SetSupplierLocation
{
	private static final String ClassName = "CatEZOSetSupplierLocation";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ReqLineItem)
		{
			ReqLineItem rli = (ReqLineItem) object;
			Supplier supplier = rli.getSupplier();
			if (supplier != null)
			{
				// restrict custom defaulting to only non-catalog items
				if (rli.getIsAdHoc() && rli.getMasterAgreement() == null)
				{
					BaseVector locations = supplier.getLocations();
					// only set SupplierLocation if Supplier has ONLY 1 location
					if (locations != null && locations.size() == 1)
					{
						Log.customer.debug("%s ::: SupplierLoc being set since only 1 exists!", ClassName);
						ClusterRoot loc = Base.getSession().objectFromId((BaseId) locations.get(0));
						Log.customer.debug("%s :::* locations(0): %s", ClassName, loc);
						if (loc instanceof SupplierLocation)
						{
							rli.setSupplierLocation((SupplierLocation) loc);
							return;
						}
					}
					else
					{
						Log.customer.debug("%s ::: Supplier has 0 or Muliple locations, SupplierLoc not set!", ClassName);
						rli.setSupplierLocation(null);
						return;
					}
				}
				/*
				else {  // means not non-catalog item, so use Ariba's super to set
					super.fire(object,params);
				}
				*/
			} // 2.03.06 - Added to reset SL to null when Supplier is null
			super.fire(object, params);
		}
	}
	public CatEZOSetSupplierLocation()
	{
		super();
	}
}
