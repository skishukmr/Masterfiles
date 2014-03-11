package config.java.invoiceeform.sap;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import java.util.List;

public class CatSAPUpdateTotal extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        Log.customer.debug("UpdateTotal: begin\n");
        Assert.that(object instanceof BaseObject, "BaseObject expected");
        if (!(object instanceof ClusterRoot)) {
            object = ((BaseObject)object).getClusterRoot();
            Assert.that(object != null, "Cluster root not set");
        }

        updateTotals(object);
    }


    private static void updateTotals (ValueSource object)
    {
        List lineItems = (List)object.getFieldValue("LineItems");
        Money enteredAmount = (Money)object.getFieldValue("EnteredInvoiceAmount");

        if (enteredAmount == null) {
			return;
		}

        Log.customer.debug("UpdateTotal: EnteredInvoiceAmount = %s", enteredAmount.asString());

        Assert.that(lineItems != null,
                    "LineItems field cannot be null");

        // Zero out the subtotals
        Currency currency = enteredAmount.getCurrency();
        Money subTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money taxTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money shippingTotal = new Money(Constants.ZeroBigDecimal, currency);
        Money newTotal = new Money(Constants.ZeroBigDecimal, currency);
        
        
        int size = lineItems.size();
        for (int i=0; i < size; i++) {
            BaseObject lineItem = (BaseObject)
                lineItems.get(i);
            Money lineAmount = (Money)
                lineItem.getFieldValue("Amount");

			if (lineAmount == null) {
				continue;
			}

            // Get the line type and category
            ProcureLineType lineType = (ProcureLineType)
                lineItem.getFieldValue("LineType");
            int lineCategory =
                (lineType != null ? lineType.getCategory() :
                 ProcureLineType.LineItemCategory);
            
            // For additional line item (Shipping or Tax), the default curency code is same as header currency
            if(lineAmount!=null && lineAmount.getAmount()==Constants.ZeroBigDecimal)
            {
            	lineItem.setDottedFieldValueWithoutTriggering("Price",new Money(Constants.ZeroBigDecimal,currency));
            	lineItem.setDottedFieldValueWithoutTriggering("Amount",new Money(Constants.ZeroBigDecimal,currency));
            	lineItem.setDottedFieldValueWithoutTriggering("TaxAmount",new Money(Constants.ZeroBigDecimal,currency));
            	lineItem.setDottedFieldValueWithoutTriggering("TotalAmount",new Money(Constants.ZeroBigDecimal,currency));            	
            }

            if (lineCategory == ProcureLineType.TaxChargeCategory) {
                taxTotal = taxTotal.add(lineAmount);
            }
            else if (lineCategory == ProcureLineType.FreightChargeCategory) {
                shippingTotal = shippingTotal.add(lineAmount);
            }
            else {
                subTotal = subTotal.add(lineAmount);
            }
            
            /* Santanu : Not required now since the design changed
            //Additional code for SAP
            Money lineTaxAmount = (Money)
            lineItem.getFieldValue("TaxAmount");

            if (lineTaxAmount != null && lineCategory != ProcureLineType.TaxChargeCategory) {
            	taxTotal = taxTotal.add(lineTaxAmount);
            }
            //Additional code for SAP
            */
        }

        subTotal = Money.add(subTotal, shippingTotal);

        Log.customer.debug("UpdateTotal: shippingTotal = %s", shippingTotal.asString());
        Log.customer.debug("UpdateTotal: taxTotal = %s", taxTotal.asString());
        Log.customer.debug("UpdateTotal: subTotal = %s", subTotal.asString());

        object.setDottedFieldValue("TotalInvoicedLessTax", subTotal);
        object.setDottedFieldValue("TotalTax", taxTotal);
        object.setDottedFieldValue("TotalShipping", shippingTotal);

    }
    
}
