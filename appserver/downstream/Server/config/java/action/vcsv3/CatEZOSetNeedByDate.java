/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/26/2006
	Description: 	Trigger implemented to set the need-by date on requisitions
					The number of days are obtained from a parameter set in the
					Parameters.table file.
-------------------------------------------------------------------------------

 Change History:
	Change By	Change Date		Description
 =============================================================================================
  1 Kannan	    04/15/2008		Issue 770, Need by date set to null if Application.Caterpillar.Procure.NeedByAdder = 0
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.Fields;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetNeedByDate extends Action
{
	private static final String ClassName = "CatEZOSetNeedByDate";
	private static final String needByAdder = "Application.Caterpillar.Procure.NeedByAdder";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof Requisition)
		{
			Requisition r = (Requisition) object;
			if (r.getPreviousVersion() != null)
				return;
			BaseVector dlines = r.getDefaultLineItems();
			if (!ListUtil.nullOrEmptyList(dlines))
			{
				ReqLineItem dli = (ReqLineItem) dlines.get(0);
				Date today1 = Date.getNow();
				Date today2 = Fields.getService().getNow();
				Log.customer.debug("%s ::: Today1:" + today1, ClassName);
				Log.customer.debug("%s ::: Today2 :" + today2, ClassName);
				String param = Base.getService().getParameter(r.getPartition(), needByAdder);
				Log.customer.debug("%s ::: Adder param: %s", ClassName, param);
				if (!StringUtil.nullOrEmptyOrBlankString(param))
				{
					int adder = Integer.parseInt(param);
					// Issue 770 start
					if (adder > 0)
					{
						Date.addDays(today1, adder);
						Log.customer.debug("%s ::: New Needby: " + today1, ClassName);
						dli.setDottedFieldValue("NeedBy", today1);
					}
					if (adder == 0)
					{

						Log.customer.debug("%s ::: New Needby from Parameter table: " + today1, ClassName);
						dli.setDottedFieldValue("NeedBy", null);
					}
					// Issue 770 end


				}
			}
		}
	}

	public CatEZOSetNeedByDate()
	{
	}
}