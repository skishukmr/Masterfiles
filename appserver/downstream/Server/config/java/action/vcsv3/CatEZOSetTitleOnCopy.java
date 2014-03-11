/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/22/2006
	Description: 	To maintain consistency with R4 rename a copy title from
					"Copy of" (OOB Ariba) to "Copy -"
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

 public class CatEZOSetTitleOnCopy extends Action {

 	private static final String ClassName = "CatSetTitleOnCopy";
    public static final String COPY_TEXT = ResourceService.getString("cat.java.vcsv3","CopyTitleText_Ariba");
    public static final String NEW_TEXT = ResourceService.getString("cat.java.vcsv3","CopyTitleText_Cat");

	public void fire(ValueSource vs, PropertyTable params) throws ActionExecutionException
	{
		if (vs instanceof Requisition)
		{
			Requisition r = (Requisition) vs;
			String title = r.getName();
			Log.customer.debug("%s ::: Req TITLE (before): %s", ClassName, title);
			if (title.startsWith(COPY_TEXT))
			{
				title = title.replaceFirst(COPY_TEXT, NEW_TEXT);
				r.setFieldValue("Name", title);
			}

			Log.customer.debug("%s ::: Req TITLE (after): %s", ClassName, r.getName());
		}
	}

    public CatEZOSetTitleOnCopy() {
        super();

    }
}
