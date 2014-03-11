/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/24/2006
	Description: 	Trigger implementation to set PricePrecisionString field on
					MARLineItem from Description.Price.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;

public class CatEZOSetPricePrecisionString extends Action
{
	private static final String ClassName = "CatEZOSetPricePrecisionString";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof LineItemProductDescription) {
			LineItemProductDescription lipd = (LineItemProductDescription) object;
			Object li = lipd.getLineItem();
			if (li instanceof ContractRequestLineItem) {
				ContractRequestLineItem mali = (ContractRequestLineItem) li;
				Money price = lipd.getPrice();
				if (price != null) {
					StringBuffer sb = new StringBuffer();
					Currency curr = price.getCurrency();
					String string = curr.getPrefix();

					Log.customer.debug("%s ::: string (prefix): %s", ClassName, string);
					if (!StringUtil.nullOrEmptyOrBlankString(string))
						sb.append(string);
					BigDecimal amount = price.getAmount();
					string = amount.toString();

					Log.customer.debug("%s ::: string (Before rounding): %s", ClassName, string);
					amount = BigDecimalFormatter.round(amount, 5);
					string = amount.toString();

					Log.customer.debug("%s ::: string (After rounding): %s", ClassName, string);
					sb.append(string).append(" ");
					sb.append(curr.getSuffix());
					Log.customer.debug("%s ::: sb: %s", ClassName, sb);

					mali.setFieldValue("PricePrecisionString", sb.toString());
				}
			}
		}
	}

	public CatEZOSetPricePrecisionString()
	{
		super();
	}
}