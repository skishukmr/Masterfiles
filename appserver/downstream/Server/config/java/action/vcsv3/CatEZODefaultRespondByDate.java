/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	01/09/2007
	Description: 	Trigger implemented to default the respond by date for eRFQ
					to 1 week from the creation date
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;

public class CatEZODefaultRespondByDate extends Action
{
	private static final String ClassName = "CatEZODefaultRespondByDate";

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof Requisition)
		{
			Requisition r = (Requisition) vs;
			Boolean eRFQB = (Boolean) r.getFieldValue("ISeRFQ");
			Log.customer.debug("%s ::: Req / ISeRFQ: %s / %s", ClassName, r, eRFQB);

			if (eRFQB != null && eRFQB.booleanValue()) {
				ariba.util.core.Date today = new ariba.util.core.Date();
				ariba.util.core.Date dateToUse = new ariba.util.core.Date();

				GregorianCalendar cal = new GregorianCalendar();
				cal.add(Calendar.DATE,7);

				Log.customer.debug("%s ::: today / cal: %s / %s", ClassName, today.toString(), cal.getTime().toString());

				Date oneWeekAfter = cal.getTime();
				dateToUse = new ariba.util.core.Date(oneWeekAfter.getTime());

				Log.customer.debug("%s ::: dateToUse: %s / %s", ClassName, dateToUse.toString());

				r.setDottedFieldValue("RespondByDate", dateToUse);
			}
			else {
				Log.customer.debug("%s ::: eRFQB is null or false when the action was triggered", ClassName);
			}
		}
		else {
			Log.customer.debug("%s ::: Requisition is null", ClassName);
		}
	}
	public CatEZODefaultRespondByDate()
	{
		super();
	}
}
