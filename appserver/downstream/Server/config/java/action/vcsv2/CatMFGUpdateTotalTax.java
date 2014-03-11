/*
 Author: shaila salimath
   Date; 11/13/2007
Purpose: CR # 755 The purpose of this class is to update the Invoice Header totals based on the line items.

Amit - 5th March 2008 - Added (baseobject1.getMatchedLineItem() != null) in line 94
						to set the vat amount on tax line only for line level tax.
*/

package config.java.action.vcsv2;

import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;

public class CatMFGUpdateTotalTax extends Action
{

   public void fire (ValueSource object, PropertyTable params)
       {
           Log.customer.debug("CatMFGUpdateTotalTax: begin\n");
           Assert.that(object instanceof BaseObject, "BaseObject expected");
           if (!(object instanceof ClusterRoot)) {
               object = ((BaseObject)object).getClusterRoot();
               Assert.that(object != null, "Cluster root not set");
           }

           updateTotals(object);
    }

 private static void updateTotals(ValueSource valuesource)
    {
        boolean isMaterialLine = false;
        boolean isTaxLineFound = false;
        if(valuesource instanceof Invoice)
         {
			List lineitem = (List)valuesource.getFieldValue("LineItems");
			Money money = (Money)valuesource.getFieldValue("EnteredInvoiceAmount");
			Money enteredInvAmt = (Money)valuesource.getFieldValue("EnteredInvoiceAmount");
			if(enteredInvAmt == null)
				return;
				Log.customer.debug(" CatMFGUpdateTotalTax UpdateTotal: EnteredInvoiceAmount = %s", enteredInvAmt.asString());
				Assert.that(lineitem != null, "LineItems field cannot be null");
				ariba.basic.core.Currency currency = enteredInvAmt.getCurrency();

				Money taxAmt = new Money(Constants.ZeroBigDecimal, currency);
				Money subTotal = (Money)valuesource.getFieldValue("TotalInvoicedLessTax");
				Money taxTotal = (Money)valuesource.getFieldValue("TotalTax");
                Money tax = new Money(Constants.ZeroBigDecimal, currency);

				int i = -1;
				int j = lineitem.size();
				// iterate through each line item
				for(int k = 0; k < j; k++)
				{
					BaseObject baseobject1 = (BaseObject)lineitem.get(k);
					Money li_amt = (Money)baseobject1.getFieldValue("Amount");
					if(li_amt == null)
						continue;
					ProcureLineType procurelinetype = (ProcureLineType)baseobject1.getFieldValue("LineType");
				    if((procurelinetype != null) && ((ProcureLineType)baseobject1.getFieldValue("LineType")).getCategory() == 2)
					taxTotal = taxTotal.add(li_amt);
					else
					  subTotal = subTotal.add(li_amt);
					Log.customer.debug("CatMFGUpdateTotalTax .LI #: " + k + " LI Size: " + j);
					Log.customer.debug("CatMFGUpdateTotalTax...lineCategory: " );

                  // check whether the line item is a meterial line
				  	if((procurelinetype != null) && ((ProcureLineType)baseobject1.getFieldValue("LineType")).getCategory() != 2)
					{
					   taxAmt = (Money)baseobject1.getFieldValue("TaxAmount");
					   if (taxAmt != null)
					   {
					   	Log.customer.debug(" CatMFGUpdateTotalTaxUpdateTotal: baseobject1 taxAmt %s", taxAmt);
					   	isMaterialLine= true;
					   	Log.customer.debug(" CatMFGUpdateTotalTax LITotal: LITotal = %s", taxAmt.asString());
				       		tax = tax.add(taxAmt);
                       Log.customer.debug("CatMFGUpdateTotalTax UpdateTotal: tax = %s", tax.asString());
					   }
					 }
					// check whether the line item is a taxline
					if((procurelinetype != null) && ((ProcureLineType)baseobject1.getFieldValue("LineType")).getCategory() == 2)
					{
						i =k;
						isTaxLineFound=true;
					}

					// if tax line then set TaxAmount from material line as Line item Amount
					InvoiceLineItem invLine = (InvoiceLineItem)baseobject1;
					if((isTaxLineFound) && (i != -1) && (invLine.getMatchedLineItem() != null))
					{
						BaseObject baseobject2 = (BaseObject)lineitem.get(i);
						Log.customer.debug(" CatMFGUpdateTotalTaxUpdateTotal: baseobject2 = %s", baseobject2);
						i=-1;
						baseobject2.setDottedFieldValue("Amount", taxAmt);
						Money li_amt2 = (Money)baseobject2.getFieldValue("Amount");
						Log.customer.debug(" CatMFGUpdateTotalTaxUpdateTotal: baseobject2 li_amt2 %s", li_amt2);
					    //taxAmt = new Money(Constants.ZeroBigDecimal, currency);
            		}
	  }

     taxTotal = Money.add(taxTotal,tax);
     subTotal = Money.add(subTotal, taxTotal);
     Log.customer.debug("CatMFGUpdateTotalTax UpdateTotal: shippingTotal = %s", tax.asString());
     Log.customer.debug("CatMFGUpdateTotalTax UpdateTotal: taxTotal = %s", taxTotal.asString());
     Log.customer.debug("CatMFGUpdateTotalTax UpdateTotal: subTotal = %s", subTotal.asString());
     valuesource.setDottedFieldValue("TotalTax", taxTotal);
    }
}

}// end
