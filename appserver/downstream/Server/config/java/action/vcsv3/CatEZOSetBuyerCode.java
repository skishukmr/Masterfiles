/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/25/2006
	Description: 	Trigger implementation to set the Buyer code on req lines
					to a default value specified in the Parameters.table for
					this partition (41EU).
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetBuyerCode extends Action
{
	private static final String ClassName = "CatEZOSetBuyerCode";
	private static final String defaultBuyerCode = "Application.Caterpillar.Procure.DefaultBuyerCode";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{

		Log.customer.debug("%s ::: OBJECT: %s", ClassName, object);
		if (object instanceof ReqLineItem)
		{
			ReqLineItem rli = (ReqLineItem) object;
			Requisition r = (Requisition) rli.getLineItemCollection();
			if (r != null)
			{
				setBuyerCode(rli);
			}
		}
		else
			if (object instanceof Requisition)
			{
				Requisition r = (Requisition) object;
				if (r.getPreviousVersion() == null)
				{
					BaseVector lines = r.getLineItems();
					if (lines != null && !lines.isEmpty())
					{
						int size = lines.size();
						for (int i = 0; i < size; i++)
						{
							ReqLineItem rli = (ReqLineItem) lines.get(i);
							setBuyerCode(rli);
						}
					}
				}
			}
	}

	public CatEZOSetBuyerCode()
	{
	}

	private void setBuyerCode(ReqLineItem rli)
	{
		Log.customer.debug("%s ::: OBJECT: %s", ClassName, rli);
		String param = Base.getService().getParameter(rli.getPartition(), defaultBuyerCode);

		Log.customer.debug("%s ::: param: %s", ClassName, param);
		Log.customer.debug("%s ::: defaultBuyerCode: %s", ClassName, defaultBuyerCode);

		if (!StringUtil.nullOrEmptyOrBlankString(param))
		{
			ClusterRoot defaultBuyerCodeObj = Base.getSession().objectFromName(param, "cat.core.BuyerCode", rli.getPartition());
			if (defaultBuyerCodeObj != null)
				Log.customer.debug("%s ::: BuyerCode/UniqueName: %s/%s", ClassName, defaultBuyerCodeObj, defaultBuyerCodeObj.getUniqueName());
			if (defaultBuyerCodeObj != null)
			{
				Log.customer.debug("%s ::: Setting BuyerCode on RLI", ClassName);
				rli.setFieldValue("BuyerCode", defaultBuyerCodeObj);
			}
			else
			{
				rli.setFieldValue("BuyerCode", null);
			}
		}
		else{
			rli.setFieldValue("BuyerCode", null);
		}
	}
}