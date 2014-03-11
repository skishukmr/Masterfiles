/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/25/2006
	Description: 	Trigger implemented to set the currency for Price and
					Amounton the MAR default line so as to default new lines to
					that currency.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */

public class CatEZODefaultLineCurrencies extends Action
{
	private static final String ClassName = "CatEZODefaultLineCurrencies";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) object;
			Currency marCurrency = (Currency) mar.getFieldValue("Currency");
			if (marCurrency != null){
				Log.customer.debug("%s ::: MAR Currency is: %s", ClassName, marCurrency);
				LineItem defaultLine = mar.getDefaultLineItem();
				if (defaultLine != null){
					Log.customer.debug("%s ::: MAR Default Line is: %s", ClassName, defaultLine);
					Money linePrice = (Money) defaultLine.getDottedFieldValue("Description.Price");
					Money lineAmount = defaultLine.getAmount();
					Money basePrice = (Money) defaultLine.getFieldValue("BasePrice");
					Money orgPrice = (Money) defaultLine.getFieldValue("OriginalPrice");
					if (linePrice != null){
						Log.customer.debug("%s ::: MAR Default Line Price is: %s", ClassName, linePrice);
						Log.customer.debug("%s ::: Setting Default Line Price Currency to: %s", ClassName, marCurrency.getName().getPrimaryString());
						linePrice.setCurrency(marCurrency);
					}
					if (lineAmount != null){
						Log.customer.debug("%s ::: MAR Default Line Amount is: %s", ClassName, lineAmount);
						Log.customer.debug("%s ::: Setting Default Line Amount Currency to: %s", ClassName, marCurrency.getName().getPrimaryString());
						lineAmount.setCurrency(marCurrency);
					}
					if (basePrice != null){
						Log.customer.debug("%s ::: MAR Default Line Amount is: %s", ClassName, basePrice);
						Log.customer.debug("%s ::: Setting Default Line Amount Currency to: %s", ClassName, marCurrency.getName().getPrimaryString());
						basePrice.setCurrency(marCurrency);
					}
					if (orgPrice != null){
						Log.customer.debug("%s ::: MAR Default Line Amount is: %s", ClassName, orgPrice);
						Log.customer.debug("%s ::: Setting Default Line Amount Currency to: %s", ClassName, marCurrency.getName().getPrimaryString());
						orgPrice.setCurrency(marCurrency);
					}
				}
			}
		}
	}

	public CatEZODefaultLineCurrencies()
	{
	}
}