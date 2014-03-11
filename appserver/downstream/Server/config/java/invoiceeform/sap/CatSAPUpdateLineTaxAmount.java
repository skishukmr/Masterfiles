/*
20/01/2012 		Aswini 		Vertex 		

  Change History
	#	Change By	Change Date		Description
	=============================================================================================
		Aswini		20/01/2012		Change for vertex fix  to bypass the taxamount with rate multiplication in the E form for vertex companycodes	
										This would implement the condition to check the CalculateTaxes field before calculation
		Divya		13/02/2012		Including check for Contracts(MasterAgreement) in order to get the CalculateTaxes Value at Companycode level
		IBM Niraj   1/10/2013   Mach1 Rel 5.5 (FRD10.2/TD10.2) Set Vat Registration in Invoice Object
*/
package config.java.invoiceeform.sap;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;

public class CatSAPUpdateLineTaxAmount extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
    	try
    	{
    	if(object!=null && object instanceof Invoice)
    	{
    		Log.customer.debug(" %s *** invoice %s",ClassName ,object);
    		Invoice invoice = (Invoice)object;
    		Log.customer.debug("CatSAPUpdateLineTaxAmount: invoice.getLoadedFrom() : " +invoice.getLoadedFrom());

    		if (invoice.getLoadedFrom()!=Invoice.LoadedFromEForm){
    			Money taxTotal = (Money)getHeaderTaxAmt(invoice);
				if (taxTotal!=null)
					setInvoiceAmt(invoice, taxTotal);
    			return;
    		}

		//Start: Mach1 Rel 5.5 (FRD10.2/TD10.2)
    		        Log.customer.debug(" CatSAPUpdateLineTaxAmount :: ********set vatReg*** ::");
	       if ((String) invoice.getDottedFieldValue("InvoiceEform.VATRegistration") != null){
	    		 String vatReg =(String) invoice.getDottedFieldValue("InvoiceEform.VATRegistration");
				 Log.customer.debug(" :: ********set vatReg********* ::" + vatReg);
				 invoice.setFieldValue("VATRegistration",vatReg);
				}
		//End: Mach1 Rel 5.5 (FRD10.2/TD10.2)
    	}

    	if(object!=null && object instanceof LineItemProductDescription)
		{
    		Log.customer.debug(" %s *** LineItemProductDescription %s",ClassName ,object);
			LineItemProductDescription lipd = (LineItemProductDescription)object;
			if(lipd.getLineItem() !=null && (lipd.getLineItem() instanceof InvoiceLineItem))
				{
				InvoiceLineItem invoiceLineItem = (InvoiceLineItem)lipd.getLineItem();
				Log.customer.debug("CatSAPUpdateLineTaxAmount: InvoiceLineItem : "+invoiceLineItem);
				Invoice invoice = (Invoice)invoiceLineItem.getLineItemCollection();
				if (invoice.getLoadedFrom()!=Invoice.LoadedFromEForm){
					Log.customer.debug("CatSAPUpdateLineTaxAmount: invoice.getLoadedFrom() : " +invoice.getLoadedFrom());
					Money taxTotal = (Money)getHeaderTaxAmt(invoice);
					if (taxTotal!=null)
						setInvoiceAmt(invoice, taxTotal);
				}
				return;
				}
		}

        BaseObject invoiceLI = (BaseObject) object;
        Log.customer.debug(" %s *** invoice %s",ClassName ,invoiceLI);
		Money lineamt = (Money)invoiceLI.getFieldValue("Amount");
		BigDecimal taxRateInPercent = (BigDecimal)invoiceLI.getDottedFieldValue("TaxCode.TaxRate");
		BigDecimal taxRate = new BigDecimal (0.00);
		BigDecimal hundred = new BigDecimal (100.00);
		BigDecimal quantity = (BigDecimal)invoiceLI.getFieldValue("Quantity");
			if(taxRateInPercent !=null)
			{
			taxRate = taxRateInPercent.divide(hundred,8,BigDecimal.ROUND_HALF_UP);
			Log.customer.debug("CatSAPUpdateLineTaxAmount: Tax Rate "+taxRate);
			}
			if(taxRate==null)
			{
				taxRate = new BigDecimal(0);
			}
			if(quantity==null)
			{
				quantity = new BigDecimal(0);
			}
			if(lineamt!=null && taxRate!=null && quantity!=null)
			{
			    Log.customer.debug("CatSAPUpdateLineTaxAmount: BasePrice "+lineamt+" TaxRate "+taxRate);		
				Money taxAmount = new Money(lineamt.multiply(taxRate));	
				//Aswini: Change for vertex to bypass the taxamount with rate multiplication for vertex companycodes	
				String taxCal = null;
				Log.customer.debug("CatSetTaxAmountLine: getting the Calculate Taxes Value..");
				if(invoiceLI.getFieldValue("Order") != null)
				{
				 taxCal  = (String)invoiceLI.getDottedFieldValue("Order.CompanyCode.CalculateTaxes");
				Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired taxCal for Order: "+taxCal);
				}
				
				else if(invoiceLI.getFieldValue("MasterAgreement") != null)
				{
				 taxCal = (String)invoiceLI.getDottedFieldValue("MasterAgreement.CompanyCode.CalculateTaxes");
				Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired taxCal for  Contract: "+taxCal);
				}
				Log.customer.debug("CatSetTaxAmountLine: isTaxCalculationRequired taxCal value: "+taxCal);
				if(taxCal!=null)
				if(taxCal.equalsIgnoreCase("Y")) 
				//End of change for vertex
				invoiceLI.setDottedFieldValue("TaxAmount",taxAmount);
				Log.customer.debug("CatSAPUpdateLineTaxAmount: lineItem Amount: "+invoiceLI.getFieldValue("Amount")+" LineItem tax: "+invoiceLI.getFieldValue("TaxAmount"));
				if(invoiceLI instanceof InvoiceLineItem ){
					InvoiceLineItem invoiceLineItem = (InvoiceLineItem)invoiceLI;
					Log.customer.debug("CatSAPUpdateLineTaxAmount: InvoiceLineItem : "+invoiceLineItem);
					Invoice invoice = (Invoice)invoiceLineItem.getLineItemCollection();
					if (invoice.getLoadedFrom()!=Invoice.LoadedFromEForm){
						Log.customer.debug("CatSAPUpdateLineTaxAmount: invoice.getLoadedFrom() : " +invoice.getLoadedFrom());
						Money taxTotal = (Money)getHeaderTaxAmt(invoice);
						if (taxTotal!=null)
							setInvoiceAmt(invoice, taxTotal);
						}
				}

				if(invoiceLI instanceof InvoiceReconciliationLineItem){
					InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)invoiceLI;
					Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : irli "+irli);
					InvoiceReconciliation ir = (InvoiceReconciliation)irli.getLineItemCollection();
					updateIRTaxAmount(ir);
				}

			
			}
       }
		catch(Exception exp)
		{
			Log.customer.debug("CatSAPUpdateLineTaxAmount: Exception occured "+exp);
		}

    }

    public void updateIRTaxAmount(InvoiceReconciliation ir)
    {
    	/* Santanu : Not Required after the logic change
    	Invoice inv = (Invoice)ir.getInvoice();
    	Money invoiceTotalTax = (Money)inv.getTotalTax();
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : invoiceTotalTax "+invoiceTotalTax);

    	Money sumInvLineTax = (Money)getSumOfLineTax(inv);
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : sumInvLineTax "+sumInvLineTax);

    	Money diff = new Money(new BigDecimal(0),ir.getTotalCost().getCurrency());

    	if(sumInvLineTax!=null && invoiceTotalTax != null){
    	diff = Money.subtract(invoiceTotalTax, sumInvLineTax);
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : diff "+diff);
		}
    	Money sumIRLineTax = (Money)getSumOfLineTax(ir);
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : sumIRLineTax "+sumIRLineTax);
		*/
    	Money IRTaxAmount = sumIRTaxLines(ir);
		Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : IRTaxAmount "+IRTaxAmount);

    	if(IRTaxAmount!=null){
    		ir.setTaxAmount(IRTaxAmount);
    		Log.customer.debug("CatSAPUpdateLineTaxAmount: updateIRTaxAmount : ir.getTaxAmount() "+ir.getTaxAmount());

    	}
    }

    public Money getSumOfLineTax(LineItemCollection lic)
    {
    	Money taxAmtAtLic = new Money(new BigDecimal(0),lic.getTotalCost().getCurrency());
    	List lineitems = (List)lic.getLineItems();
    	for(int i=0;i<lineitems.size();i++){
    		StatementCoreApprovableLineItem li = (StatementCoreApprovableLineItem)lineitems.get(i);
    		Money lineTaxAmt = (Money)li.getTaxAmount();
    		Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : lineTaxAmt "+lineTaxAmt);
    		if(lineTaxAmt!=null && li.getLineType()!=null && li.getLineType().getCategory()!= ProcureLineType.TaxChargeCategory){
    			taxAmtAtLic = taxAmtAtLic.add(lineTaxAmt);
    		}
    	}
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : taxAmtAtLic "+taxAmtAtLic);
    	return taxAmtAtLic;
    }
    // This method only consider the tax lines, not the tax amount of procure lines.
    public Money sumIRTaxLines(LineItemCollection lic)
    {
    	Money taxAmtAtLic = new Money(new BigDecimal(0),lic.getTotalCost().getCurrency());
    	List lineitems = (List)lic.getLineItems();
    	for(int i=0;i<lineitems.size();i++){
    		StatementCoreApprovableLineItem li = (StatementCoreApprovableLineItem)lineitems.get(i);
    		Money lineAmt = (Money)li.getAmount();
    		Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : lineAmt "+lineAmt);
    		if(lineAmt!=null && li.getLineType()!=null && li.getLineType().getCategory()== ProcureLineType.TaxChargeCategory){
    			taxAmtAtLic = taxAmtAtLic.add(lineAmt);
    		}
    	}
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : taxAmtAtLic "+taxAmtAtLic);
    	return taxAmtAtLic;
    }

	public Money getHeaderTaxAmt(Invoice invoice){
		List lineItems = (List)invoice.getLineItems();
		Money taxTotal = new Money(Constants.ZeroBigDecimal, invoice.getTotalCost().getCurrency());
		for (int i = 0; i<lineItems.size();i++){
			InvoiceLineItem lineItem = (InvoiceLineItem)lineItems.get(i);
            Money lineAmount = (Money)lineItem.getFieldValue("Amount");
            if (lineAmount == null) {
            	continue;
            }
            Money lineTaxAmount = (Money)lineItem.getFieldValue("TaxAmount");
            Log.customer.debug("CatSAPUpdateLineTaxAmount: updateHeaderTaxAmt : lineTaxAmount "+lineTaxAmount);

			ProcureLineType lineType = (ProcureLineType)lineItem.getFieldValue("LineType");
			int lineCategory =(lineType != null ? lineType.getCategory() : ProcureLineType.LineItemCategory);

			if (lineAmount!=null && lineCategory == ProcureLineType.TaxChargeCategory) {
				Log.customer.debug("CatSAPUpdateLineTaxAmount: updateHeaderTaxAmt : lineAmount for tax line : "+lineAmount);
				taxTotal = taxTotal.add(lineAmount);
			}
			/* Santanu : Not required since the desing changed.
			if (lineTaxAmount != null && lineCategory != ProcureLineType.TaxChargeCategory) {
				Log.customer.debug("CatSAPUpdateLineTaxAmount: updateHeaderTaxAmt : lineTaxAmount "+lineTaxAmount);
				taxTotal = taxTotal.add(lineTaxAmount);
			}
			*/

		}
		Log.customer.debug("CatSAPUpdateLineTaxAmount: updateHeaderTaxAmt "+taxTotal);
		return taxTotal;

	}


	public void setInvoiceAmt(Invoice invoice, Money taxTotal)
	{
		invoice.setDottedFieldValueWithoutTriggering("TotalTax",taxTotal);
		invoice.setDottedFieldValueWithoutTriggering("TaxAmount",taxTotal);
		Money totalInvoicedLessTax = (Money)invoice.getDottedFieldValue("TotalInvoicedLessTax");
		if(totalInvoicedLessTax!=null && taxTotal!=null)
		invoice.setTotalInvoiced(totalInvoicedLessTax.add(taxTotal));
		Log.customer.debug("CatSAPUpdateLineTaxAmount: updateInvoiceTaxAmount : invoice.getTaxAmount() "+invoice.getTaxAmount()+" TotalTax "+invoice.getDottedFieldValue("TotalTax"));
	}

}
