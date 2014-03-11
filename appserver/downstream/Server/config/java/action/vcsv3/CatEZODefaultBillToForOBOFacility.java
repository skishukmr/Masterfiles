/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/12/2006
	Description: 	Trigger implementation to set bill to address on Req Line
					Create using the OBO facility on the profile.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZODefaultBillToForOBOFacility extends Action
{
	private static final String ClassName = "CatDefaultBillToForFacility";
	private static final String ADDRESSCLASS = "ariba.common.core.Address";
	private static String billToConstant = "_BillTo";
	private static final String LOOKUP = "DefaultBillToForFacility_";
	private static final String FACILITY = "AccountingFacility";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof ProcureLineItem) {
			Log.customer.debug("%s ::: Entering method to default Bill To", ClassName);
			ProcureLineItem pli = (ProcureLineItem) object;
			ProcureLineItemCollection plic = (ProcureLineItemCollection) pli.getLineItemCollection();
			if (plic != null) {
				User requester = plic.getRequester();
				String facility = (String) requester.getFieldValue(FACILITY);
				if (facility != null) {
					Log.customer.debug("%s ::: Facility on the ariba.user.core.user Object is: %s", ClassName, facility);
					String billToUN = facility + billToConstant;
					Log.customer.debug("%s ::: Default Bill To UniqueName for Facility %s is: %s", ClassName, facility, billToUN);
					Address billTo = (Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());
					if (billTo != null)
					{
						pli.setFieldValue("BillingAddress", billTo);
					}
					Log.customer.debug("%s ::: Default Bill To set on the Line is: %s", ClassName, pli.getBillingAddress());
				}
			}
		}
	}

	public CatEZODefaultBillToForOBOFacility() {
		super();
	}
}
