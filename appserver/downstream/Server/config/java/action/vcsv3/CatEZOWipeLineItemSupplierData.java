/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	11/14/2006
	Description: 	Trigger implemented to wipe line level supplier data for
					AdHoc lines when a Requisition is selected to be eRFQ.
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.BaseVector;
import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;

public class CatEZOWipeLineItemSupplierData extends Action
{
	private static final String ClassName = "CatEZOWipeLineItemSupplierData";

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof Requisition)
		{
			Requisition r = (Requisition) vs;
			Boolean eRFQB = (Boolean) r.getFieldValue("ISeRFQ");

			Log.customer.debug("%s ::: Req / ISeRFQ: %s / %s", ClassName, r, eRFQB);

			if (eRFQB != null && eRFQB.booleanValue()) {
				BaseVector lines = (BaseVector) r.getLineItems();
				int size = lines.size();
				for (int i=0; i<size; i++) {
					ReqLineItem rli = (ReqLineItem) lines.get(i);
					if (rli != null && rli.getIsAdHoc()) {

						Log.customer.debug("%s ::: rli #" + (i+1) + " is an AdHoc line", ClassName);
						Log.customer.debug("%s ::: Setting line supplier data to null as an eRFQ requisiiton", ClassName);

						rli.setSupplier(null);
						rli.setSupplierLocation(null);
					}
					else {
						Log.customer.debug("%s ::: rli #" + (i+1) + " is null or not an AdHoc line", ClassName);
						Log.customer.debug("%s ::: rli / isAdHoc: %s / " + rli.getIsAdHoc(), ClassName, rli);
					}
				}
			}
			else {
				Log.customer.debug("%s ::: eRFQB is null or false when the action was triggered", ClassName);
			}
		}
		else {
			Log.customer.debug("%s ::: Requisition is null", ClassName);
		}
	}
	public CatEZOWipeLineItemSupplierData()
	{
		super();
	}
}
