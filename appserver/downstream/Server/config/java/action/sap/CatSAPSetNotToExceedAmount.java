/******************************************************************************
	Author: 		Nagendra
	Date Created:
	Description: 	Trigger implementation to update the NTE Amount on Req Line
					when the NTE price changes on LIPD.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.sap;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSetNotToExceedAmount extends Action
{
	private static final String ClassName = "CatSAPSetNotToExceedAmount";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		LineItemProductDescription lipd = null;
		ReqLineItem rli = null;
		if (object instanceof ReqLineItem)
		{
			rli = (ReqLineItem) object;
			lipd = rli.getDescription();
		}
		else
			if (object instanceof LineItemProductDescription)
			{
				lipd = (LineItemProductDescription) object;
				try
				{
					rli = (ReqLineItem) lipd.getLineItem();
				}
				catch (ClassCastException cce)
				{
					Log.customer.debug("%s ::: NOT ReqLineItem - ClassCastException - STOP!", ClassName);
					return;
				}
			}
		if (rli != null && lipd != null)
		{
			Money nte = (Money) lipd.getFieldValue("NotToExceedPrice");
			Log.customer.debug("%s ::: NTE PRICE: %s", ClassName, nte);
			BigDecimal value = new BigDecimal(0);
			if (nte != null && nte.getAmount().compareTo(value) > 0)
			{
				value = rli.getQuantity();
				Log.customer.debug("%s ::: NTE QTY: %s", ClassName, value);
				nte = nte.multiply(value);
				Log.customer.debug("%s ::: NTE AMOUNT: %s", ClassName, nte);
				rli.setFieldValue("NTEAmount", nte);
			}
		}
	}
	public CatSAPSetNotToExceedAmount()
	{
		super();
	}
}