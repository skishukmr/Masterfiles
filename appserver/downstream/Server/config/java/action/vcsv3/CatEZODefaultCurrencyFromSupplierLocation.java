/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/25/2006
	Description: 	Trigger implemented to default currency on contract request
					from supplier location default currency.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */
public class CatEZODefaultCurrencyFromSupplierLocation extends Action
{
	private static final String ClassName = "CatEZODefaultCurrencyFromSupplierLocation";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) object;
			ariba.common.core.SupplierLocation sloc = mar.getSupplierLocation();
			if (sloc != null){
				Log.customer.debug("%s ::: Supplier Location is: %s", ClassName, sloc);
				Currency suppCurrency = (Currency) sloc.getFieldValue("Currency");
				if (suppCurrency != null){
					Log.customer.debug("%s ::: Supplier Location Currency: %s", ClassName, suppCurrency.getName().getPrimaryString());
					mar.setCurrency(suppCurrency);
					//mar.setReportingCurrency(suppCurrency);
				}
			}
		}
	}

	public CatEZODefaultCurrencyFromSupplierLocation()
	{
	}
}