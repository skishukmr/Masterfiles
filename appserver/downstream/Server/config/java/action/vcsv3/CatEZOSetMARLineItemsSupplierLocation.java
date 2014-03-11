/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/23/2006
	Description: 	Trigger implemented to set the Supplier Location on the
					line items using the location from the header.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequestLineItem;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZOSetMARLineItemsSupplierLocation extends Action
{
	private static final String ClassName = "CatEZOSetMARLineItemsSupplierLocation";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) object;
			ariba.common.core.SupplierLocation sloc = mar.getSupplierLocation();
			BaseVector lines = mar.getLineItems();
			if (!lines.isEmpty()) {
				int size = lines.size();
				for (int i = 0; i < size; i++) {
					ContractRequestLineItem mali = (ContractRequestLineItem) lines.get(i);
					mali.setSupplierLocation(sloc);
				}

				Log.customer.debug("%s ::: Done setting Supplier Location on the lines", ClassName);
			}
		}
	}

	public CatEZOSetMARLineItemsSupplierLocation()
	{
	}
}