/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/23/2006
	Description: 	Trigger implemented to set the Supplier Location on a
					change to the supplier field.  If no location could be set
					it will default null, thus preventing the old location
					from being used.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/
package config.java.action.vcsv3;

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

public class CatEZOSetMARSupplierLocation extends SetSupplierLocation
{
	private static final String ClassName = "CatEZOSetMARSupplierLocation";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) object;
			Supplier supplier = mar.getSupplier();
			if (supplier != null) {
				BaseVector locations = supplier.getLocations();
				if (locations != null && locations.size() == 1) {
					Log.customer.debug("%s ::: Supplier Location being set", ClassName);
					ariba.base.core.ClusterRoot loc = Base.getSession().objectFromId((BaseId) locations.get(0));
					Log.customer.debug("%s ::: Location being set to: %s", ClassName, loc);
					if (loc instanceof SupplierLocation) {
						mar.setSupplierLocation((SupplierLocation) loc);
						return;
					}
				}
				else {
					Log.customer.debug(
						"%s ::: Supplier has 0 or Muliple locations, SupplierLoc set to NULL!",
						ClassName);
					mar.setSupplierLocation(null);
					return;
				}
			}
			Log.customer.debug("%s ::: Supplier Loc trigger calling SUPER()!", ClassName);
			super.fire(object, params);
		}
	}

	public CatEZOSetMARSupplierLocation()
	{
	}
}