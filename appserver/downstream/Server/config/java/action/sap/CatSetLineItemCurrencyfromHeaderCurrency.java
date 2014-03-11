/****************************************************************************************
Change History
Change# Change By       Change Date     Description
==============================================================================================
1       Madhuri 	  15-11-08	Created : Trigger on MAR Change of Header Currency to default Line Item Currencies with Header Currency
**********************************************************************************************/

package config.java.action.sap;

import java.math.BigDecimal;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
 *
 * AUL : Changed MARLineItem to ContractRequestLineItem
 * Changed MasterAgreementRequest to ContractRequest
 *
 */

public class CatSetLineItemCurrencyfromHeaderCurrency extends Action{


	public void fire(ValueSource object, PropertyTable params){
		Log.customer.debug(" CatSetLineItemCurrencyfromHeaderCurrency : Start of the Trigger " +object);
		Money zeroAmount;
		Currency headerCurrency =null;
		if(object!=null && object instanceof LineItemProductDescription){
			Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Object is instance of object " +object);
			LineItemProductDescription liPD = (LineItemProductDescription)object;
			Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : productDescription " +liPD);
			if(liPD != null){
			BaseObject MARObj = (BaseObject)liPD.getDottedFieldValue("LineItem");
			Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MARObj " +MARObj);
			if (MARObj instanceof ContractRequestLineItem) {
				ContractRequestLineItem MARLI = (ContractRequestLineItem) MARObj;
				if( MARLI != null){
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MAR Line Item"+ MARLI);
				ContractRequest MAR = (ContractRequest)MARLI.getLineItemCollection();
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MAR "+ MAR);
				if(MAR != null){
				 headerCurrency = (Currency) MAR.getDottedFieldValue("Currency");
				 Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency  the header currency is :"+headerCurrency);
				}
				if(headerCurrency != null && liPD != null){
					Money ZERO_MONEY = new Money(new BigDecimal(0),headerCurrency);
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Setting Description price Currency ");
					String currencyUniqueName = (String)headerCurrency.getDottedFieldValue("UniqueName");
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : "+currencyUniqueName);
					zeroAmount = new Money(new BigDecimal(0.0D),headerCurrency);
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : "+zeroAmount);
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Setting Description price Currency ");
					Money moneyFromLiPDBeforeSetting = (Money) liPD.getDottedFieldValue("Price");
					if((moneyFromLiPDBeforeSetting == null) || ZERO_MONEY.compareTo(moneyFromLiPDBeforeSetting) == 0){
						liPD.setDottedFieldValue("Price",zeroAmount);
					}
					Money moneyFromLiPD = (Money) liPD.getDottedFieldValue("Price");
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency :"+moneyFromLiPD);
					Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : After Setting Description price Currency ");
				} // End of header Currency Check
			  }
			}
	  	  }
		} // End of MARLineItem instance Check
	} // End Of Fire Method

}// End Of class


	/*	// Check to make sure that object is of MARLineItem
		if(object!=null && object instanceof MARLineItem){
			Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Object is instance of MAR Line Item ");
			//LineItemProductDescription productDescription = (LineItemProductDescription)object;
			MARLineItem MARLI = (MARLineItem) object;
			Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Object is instance of MAR Line Item"+ MARLI);
			if(MARLI != null){
				//MARLineItem MARLI = (MARLineItem)productDescription.getDottedFieldValue("LineItem");
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MAR Line Item"+ MARLI);
				MasterAgreementRequest MAR = (MasterAgreementRequest)MARLI.getLineItemCollection();
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MAR "+ MAR);
				if(MAR != null){
				 headerCurrency = (Currency) MAR.getDottedFieldValue("Currency");
				 Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency  the header currency is :"+headerCurrency);
				}
			    Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : MAR is not null ");
				BaseObject liPD = (BaseObject)MARLI.getDottedFieldValue("Description");
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : line Item Description "+liPD);
				Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : line Item Description is not null ");
					if(headerCurrency != null && liPD != null){
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Setting Description price Currency ");
						String currencyUniqueName = (String)headerCurrency.getDottedFieldValue("UniqueName");
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : "+currencyUniqueName);
						zeroAmount = new Money(new BigDecimal(0.0D),headerCurrency);
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : "+zeroAmount);
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : Setting Description price Currency ");
						liPD.setDottedFieldValue("Price",zeroAmount);
						MARLI.setDottedFieldValue("Amount",zeroAmount);
						MARLI.setDottedFieldValue("BasePrice",zeroAmount);
						MARLI.setDottedFieldValue("OriginalPrice",zeroAmount);
						Money moneyFromLiPD = (Money) liPD.getDottedFieldValue("Price");
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency :"+moneyFromLiPD);
						Log.customer.debug("CatSetLineItemCurrencyfromHeaderCurrency : After Setting Description price Currency ");
					} // End of header Currency Check
				  } // End of MarLine Iten Not Null check
		// }// end of pd check */

