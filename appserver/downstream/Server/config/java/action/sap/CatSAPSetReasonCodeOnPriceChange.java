/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/02/2006
	Description: 	Trigger implementation to reset reason code to (no value)
					when price changed to > $0.00
-------------------------------------------------------------------------------
	Change Author: 	Nagendra
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

public class CatSAPSetReasonCodeOnPriceChange extends Action
{
	private static final String ClassName = "CatSAPSetReasonCodeOnPriceChange";
	public static final Money ZERO_MONEY = new Money(new BigDecimal(0), Currency.getBaseCurrency());
	public static final String NO_VALUE = "(no value)";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof LineItemProductDescription)
		{
			LineItemProductDescription lipd = (LineItemProductDescription) object;
			ProcureLineItem pli = lipd.getLineItem();
			if (pli instanceof ReqLineItem)
			{
				Log.customer.debug("%s ::: Testing: Price == ZERO_MONEY?", ClassName);
				Money price = lipd.getPrice();
				if (ZERO_MONEY.compareTo(price) < 0)
				{
					Log.customer.debug("%s ::: RESET ReasonCode to (no value)!", ClassName);
					lipd.setFieldValue("ReasonCode", NO_VALUE);
				}
			}
		}
	}
	public CatSAPSetReasonCodeOnPriceChange()
	{
		super();
	}
}
