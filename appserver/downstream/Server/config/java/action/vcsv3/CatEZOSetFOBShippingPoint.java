/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/04/2006
	Description: 	Trigger to set FOB Point (a.k.a Shipping Point in R4) to 
					Supplier Location / Plant address
-------------------------------------------------------------------------------
	Change Author: 	
	Date Created:  	
	Description: 	
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.SupplierLocation;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZOSetFOBShippingPoint extends Action
{
	private static final String ClassName = "CatEZOSetFOBShippingPoint";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		Log.customer.debug("%s ::: OBJECT: %s", ClassName, object);
		if (object instanceof ReqLineItem)
		{
			ReqLineItem rli = (ReqLineItem) object;
			Address source = null;
			SupplierLocation loc = rli.getSupplierLocation();
			if (loc != null)
			{
				source = (Address) loc.getFieldValue("FOBPoint");
				if (source == null)
					source = (Address) loc;
				rli.setFieldValue("FOBPoint", source);
			}
			Log.customer.debug("%s ::: address source: %s", ClassName, source);
		}
	}
	public CatEZOSetFOBShippingPoint()
	{
		super();
	}
}