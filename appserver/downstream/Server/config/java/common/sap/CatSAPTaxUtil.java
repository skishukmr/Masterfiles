/*************************************************************************************************
 *    Mounika.k - Work Item 148 - 25th Sept 2011 - Rounding Issue while calculating Tax for order
 *
 *
 *************************************************************************************************/

package config.java.common.sap;

import java.math.BigDecimal;

import ariba.base.core.BaseObject;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import java.math.RoundingMode;

public class CatSAPTaxUtil {
	private static String ClassName = "CatSAPTaxUtil : ";
	public static boolean compareTax(InvoiceReconciliation ir) {
		try
		{
		Money actualTaxAmount = (Money)ir.getInvoice().getTotalTax();
		BigDecimal actualTaxAmountAmt = (BigDecimal)ir.getInvoice().getTotalTax().getAmount();
		Currency currency = actualTaxAmount.getCurrency();
		//Money calculatedTaxAmount = new Money(Constants.ZeroBigDecimal, currency);;
		Money calculatedTaxAmounttemp = new Money(Constants.ZeroBigDecimal, currency);;
		BigDecimal calculatedTaxAmount = new BigDecimal (0.00);
		BigDecimal calculatedFinalTaxAmount = new BigDecimal (0.00);
		//Money lineTaxAmount = new Money(Constants.ZeroBigDecimal, currency);;
		Money lineTaxAmounttemp = new Money(Constants.ZeroBigDecimal, currency);;
        BigDecimal lineTaxAmount = new BigDecimal (0.00);
		Invoice invoice = (Invoice)ir.getInvoice();
		for(int i=0; i<invoice.getLineItemsCount(); i++){
			BaseObject invoiceLI = (BaseObject)invoice.getLineItems().get(i);
	        Log.customer.debug(" %s *** invoice %s",ClassName ,invoiceLI);
			//Money lineAmount = (Money)invoiceLI.getFieldValue("Amount");
			Money lineAmounttemp = (Money)invoiceLI.getFieldValue("Amount");
			BigDecimal lineAmount = (BigDecimal)invoiceLI.getDottedFieldValue("Amount.Amount");

			ProcureLineType lineType = (ProcureLineType)
            invoiceLI.getFieldValue("LineType");
			Log.customer.debug(" %s *** lineType %s",ClassName ,lineType);

			int lineCategory =
            (lineType != null ? lineType.getCategory() :
             ProcureLineType.LineItemCategory);
			Log.customer.debug(" %s *** lineCategory %s",ClassName ,lineCategory);


			if (lineCategory == ProcureLineType.LineItemCategory) {

			PurchaseOrder  order = (PurchaseOrder)invoiceLI.getFieldValue("Order");
			Contract ma = (Contract)invoiceLI.getFieldValue("MasterAgreement");
			BigDecimal taxRateInPercent = (BigDecimal)invoiceLI.getDottedFieldValue("TaxCode.TaxRate");

			if(order !=null){
				Log.customer.debug(" %s *** ProcureLineType.LineItemCategory %s",ClassName ,ProcureLineType.LineItemCategory);

					Integer orderLine = (Integer)invoiceLI.getFieldValue("OrderLineNumber");
					Log.customer.debug(" %s *** orderLine %s",ClassName ,orderLine);
					ProcureLineItem orderLI = (ProcureLineItem)invoiceLI.getFieldValue("OrderLineItem");
					if(orderLI == null){
						orderLI = (ProcureLineItem)order.getLineItems().get(orderLine.intValue()-1);
					}
					BigDecimal taxRateInOrderLine = (BigDecimal)orderLI.getDottedFieldValue("TaxCode.TaxRate");
					if(taxRateInOrderLine == null){
						taxRateInPercent = new BigDecimal (0.00);
					}else if (taxRateInOrderLine.compareTo(taxRateInPercent) != 0){
						taxRateInPercent = taxRateInOrderLine;

				}
			}else if(ma != null){
				Log.customer.debug(" %s *** ProcureLineType.LineItemCategory %s",ClassName ,ProcureLineType.LineItemCategory);

				ProcureLineItem maLI = (ProcureLineItem)invoiceLI.getFieldValue("MALineItem");
				if(maLI != null)
				{
					BigDecimal taxRateInMALine = (BigDecimal)maLI.getDottedFieldValue("TaxCode.TaxRate");

					if(taxRateInMALine == null){
						taxRateInPercent = new BigDecimal (0.00);
					}else if (taxRateInMALine.compareTo(taxRateInPercent) != 0){
						taxRateInPercent = taxRateInMALine;
					}
				}

			}
			BigDecimal taxRate = new BigDecimal (0.00);
			BigDecimal hundred = new BigDecimal (100.00);
			BigDecimal quantity = (BigDecimal)invoiceLI.getFieldValue("Quantity");
				if(taxRateInPercent !=null)
				{
				taxRate = taxRateInPercent.divide(hundred,8,BigDecimal.ROUND_HALF_UP);
				Log.customer.debug("CatSAPTaxUtil: Tax Rate "+taxRate);
				}
				if(taxRate==null)
				{
					taxRate = new BigDecimal(0);
				}
				if(quantity==null)
				{
					quantity = new BigDecimal(0);
				}
				if(lineAmount!=null && taxRate!=null && quantity!=null)
				{
					Log.customer.debug("CatSAPTaxUtil: BasePrice "+lineAmount+" TaxRate "+taxRate);
					//lineTaxAmount = new Money(lineAmount.multiply(taxRate));
					lineTaxAmounttemp = new Money(lineAmounttemp.multiply(taxRate));
					lineTaxAmount = lineAmount.multiply(taxRate);
					Log.customer.debug("CatSAPTaxUtil: LineTaxAmount : "+lineTaxAmount);
					Log.customer.debug("CatSAPTaxUtil: lineItem Amount: "+invoiceLI.getFieldValue("Amount")+" LineItem tax: "+invoiceLI.getFieldValue("TaxAmount"));
				}
				//calculatedTaxAmount = Money.add(calculatedTaxAmount,lineTaxAmount);
				calculatedTaxAmount = calculatedTaxAmount.add(lineTaxAmount);
				calculatedTaxAmounttemp = Money.add(calculatedTaxAmounttemp,lineTaxAmounttemp);
				calculatedTaxAmounttemp = calculatedTaxAmounttemp.round();
				Log.customer.debug("CatSAPTaxUtil: calculatedTaxAmount without round : "+calculatedTaxAmount);
				}
			}
			calculatedFinalTaxAmount = calculatedTaxAmount.setScale(2, BigDecimal.ROUND_UP);
			Log.customer.debug("CatSAPTaxUtil: calculatedFinalTaxAmount after round : "+calculatedFinalTaxAmount);
			if(calculatedFinalTaxAmount.compareTo(actualTaxAmountAmt) != 0){
			    ir.setFieldValue("AuthPOTaxAmt",calculatedTaxAmounttemp);
				ir.setDottedFieldValue("AuthPOTaxAmt.Amount",calculatedFinalTaxAmount);
				return true;
			}
			else{
			    ir.setFieldValue("AuthPOTaxAmt",calculatedTaxAmounttemp);
				ir.setDottedFieldValue("AuthPOTaxAmt.Amount",calculatedFinalTaxAmount);
				return false;
			}
		}
		catch(Exception exp)
		{
			Log.customer.debug("CatSAPTaxUtil: Exception occured "+exp);
		}

		return false;
	}
}
