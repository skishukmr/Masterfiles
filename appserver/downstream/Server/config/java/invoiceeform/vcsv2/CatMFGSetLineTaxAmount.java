/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to set the tax amount in the invoice line.

Shaila: Feb 26th 08 :  CR # 755 Line no: 30: Added code to check instance of invoice line item

Sudheer : Adding Null check for the VAT Class Issue # 970
*/

package config.java.invoiceeform.vcsv2;

import java.math.BigDecimal;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;


public class CatMFGSetLineTaxAmount extends Action
{

    public void fire (ValueSource object, PropertyTable params)
    {
		BaseObject invoiceLine = (BaseObject) object;
		// CR 755
	if(!(invoiceLine instanceof InvoiceReconciliationLineItem))
		{
		Log.customer.debug("CatMFGSetLineTaxAmount: invoiceLine = %s" , invoiceLine);
		Integer category = (Integer) invoiceLine.getDottedFieldValue("LineType.Category");

		ClusterRoot vatClass = (ClusterRoot) invoiceLine.getDottedFieldValue("VATClass");
    //    BigDecimal NewRate = null;
		invoiceLine.setDottedFieldValue("TaxAmount", null);

		if (category != null && vatClass != null) {
			Log.customer.debug("CatMFGSetLineTaxAmount: category = %s" , category);
			if (category.intValue() != ProcureLineType.TaxChargeCategory && vatClass != null) {
				Log.customer.debug("CatMFGSetLineTaxAmount: category " , category);
				Money amount = (Money) invoiceLine.getDottedFieldValue("Amount");
				if (amount != null) {
					BigDecimal vatRate = (BigDecimal) invoiceLine.getDottedFieldValue("VATClass.RateInPercentage");
                    String NewRate = (String)invoiceLine.getDottedFieldValue("NewRateInPercentage");
                    if (NewRate != null)
                      {
						  Log.customer.debug("CatMFGSetLineTaxAmount: NewRate" +NewRate);
						  BigDecimal bigDecimal1 = new BigDecimal(NewRate);
						  Log.customer.debug("CatMFGSetLineTaxAmount: bigDecimal1" +bigDecimal1);
						  vatRate = bigDecimal1;
					  }
					   Log.customer.debug("CatMFGSetLineTaxAmount: vatRate" +vatRate);
                    vatRate = vatRate.divide(new BigDecimal(100.0), 3,BigDecimal.ROUND_HALF_UP);
                    Log.customer.debug("CatMFGSetLineTaxAmount: vatRate converted " +vatRate);
					Money taxAmount = Money.multiply(amount, vatRate);
					  Log.customer.debug("CatMFGSetLineTaxAmount: taxAmount " +taxAmount);
					taxAmount.setAmount(taxAmount.getAmount().setScale(2,BigDecimal.ROUND_HALF_UP));
					invoiceLine.setDottedFieldValue("TaxAmount", taxAmount);
						  Log.customer.debug("CatMFGSetLineTaxAmount: taxAmount is set properly " );

				}

			}
		}
    }

}
}
