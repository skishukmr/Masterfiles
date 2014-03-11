/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/22/2006
	Description: 	Trigger implemented to append the Emergency Buy title text
					to the Requisition title
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;

public class CatEZOSetTitleForEmergencyBuy extends Action
{
	private static final String ClassName = "CatSetTitleForEmergencyBuy";
	public static final String EmergencyText = ResourceService.getString("cat.java.common", "EmergencyBuyTitleText");

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof Requisition)
		{
			Requisition r = (Requisition) vs;
			Boolean eb = (Boolean) r.getFieldValue("EmergencyBuy");
			String title = r.getName();

			Log.customer.debug("%s ::: Req TITLE / Emergency Buy (before): %s / %s", ClassName, title, eb.toString());
    		int index = -1;
    		// AUL, sdey : Resolved null pointer exception
    		if(title!=null){
    			index = title.indexOf(EmergencyText);
    		}
			if ((eb == null || !eb.booleanValue()) && index > -1)
			{
				int length = EmergencyText.length();

				Log.customer.debug("%s ::: Emergency Buy text length: " + length, ClassName);
				title = title.substring(0, index) + title.substring(index + EmergencyText.length());
				r.setFieldValue("Name", title);
			}
			else
				if (eb != null && eb.booleanValue() && index < 0)
				{
					title = EmergencyText + title;
					r.setFieldValue("Name", title);
				}
				Log.customer.debug("%s ::: Req TITLE (after): %s", ClassName, title);
		}
	}
	public CatEZOSetTitleForEmergencyBuy()
	{
		super();
	}
}
