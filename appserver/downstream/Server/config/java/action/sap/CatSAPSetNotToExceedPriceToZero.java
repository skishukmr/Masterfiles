/******************************************************************************
	Author: 		Nagendra
	Date Created:
	Description: 	Trigger implementation to zero the NTE price to prevent
					prior amounts being included in the logic (e.g. Edit rules)
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
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSetNotToExceedPriceToZero extends Action
{
	private static final String ClassName = "CatSAPSetNotToExceedPriceToZero";
	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof LineItemProductDescription)
		{
			LineItemProductDescription lipd = (LineItemProductDescription) object;
			ProcureLineItem pli = lipd.getLineItem();
			if (pli instanceof ReqLineItem)
			{
				String reason = (String) lipd.getFieldValue("ReasonCode");
				Log.customer.debug("%s ::: ReasonCode: %s", ClassName, reason);
				if (reason != null && reason.indexOf("xceed") < 0)
				{
					Log.customer.debug("%s ::: ReasonCode changed, resetting NTE Price!", ClassName);
					Currency curr = Currency.getBaseCurrency();
					Money amount = lipd.getPrice();
					if (amount != null)
						curr = amount.getCurrency();
					amount = new Money(new BigDecimal(0), curr);
					Log.customer.debug("%s ::: Set NTEPrice to Zero! %s", ClassName, amount);
					lipd.setFieldValue("NotToExceedPrice", amount);
					pli.setFieldValue("NTEAmount", amount);
				}
			}
		}
	}
	public CatSAPSetNotToExceedPriceToZero()
	{
		super();
	}
}
