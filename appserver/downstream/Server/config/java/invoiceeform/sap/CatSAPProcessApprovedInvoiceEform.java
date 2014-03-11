// Created by Majid to copy SAP specific fields from Invoice Eform to Invoice
// Source File Name:   CatSAPProcessApprovedInvoiceEform.java
//Nagendra added piece of code to copy CuSSUppLoc field value from EForm to Invoice
//Issue 988     08-17-2010     Added code to copy comments to invoice     Lekshmi and Darshan
// Aswini added piece of code to copy New fields from Eform to Invoice

package config.java.invoiceeform.sap;


import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Address;
import ariba.basic.core.Money;
import ariba.basic.core.PostalAddress;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.Log;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.payment.core.PaymentTerms;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementOrderInfo;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.formatter.IntegerFormatter;
import config.java.invoiceeform.InvoiceEformUtil;


public class CatSAPProcessApprovedInvoiceEform extends Action
{

    public CatSAPProcessApprovedInvoiceEform()
    {
    }

    public void fire (ValueSource object, PropertyTable parameters)
    throws ActionExecutionException
  {
      Approvable eform = (Approvable)object;
      Log.customer.debug("Processing InvoiceEform %s.", eform);

		// Create a new Ariba Invoice object
      Partition partition = eform.getPartition();
      Invoice invoice = (Invoice)BaseObject.create(Invoice.ClassName,partition);
      invoice.save();

		// Mark the Invoice as being loaded from an Eform
      invoice.setLoadedFrom(Invoice.LoadedFromEForm);

		// Copy the header fields onto the Invoice
      invoice.setName(
          (String)eform.getFieldValue("Name"));
      invoice.setPreparer(
          (User)eform.getFieldValue("Preparer"));
      invoice.setRequester(
          (User)eform.getFieldValue("Requester"));
      invoice.setInvoiceNumber(
          (String)eform.getFieldValue("InvoiceNumber"));
      invoice.setInvoiceDate(
          (Date)eform.getFieldValue("InvoiceDate"));
      invoice.setSupplier(
          (Supplier)eform.getFieldValue("Supplier"));
      invoice.setSupplierLocation(
          (SupplierLocation)eform.getFieldValue("SupplierLocation"));
      invoice.setRemitToAddress((Address)eform.getFieldValue("RemitToAddress"));

      //Code by lekshmi to copy coments fron Invoice Eform to Invoice
	          //Start

	          Log.customer
	  		.debug("CatCSVProcessApprovedInvoiceEform Adding Comment in Invoice");
	         List commentsOnApprovable = eform.getComments();
	     	   Log.customer
	  	          .debug("CatCSVProcessApprovedInvoiceEform Adding Comment in Invoice"+ commentsOnApprovable.size());
	         for (int i = 0; i < commentsOnApprovable.size(); i++) {
	      	   Log.customer
	     		.debug("CatCSVProcessApprovedInvoiceEform Entering for loop");
	  	         Comment commentItem = (Comment) commentsOnApprovable.get(i);
	  	         Log.customer
	  	 		.debug("CatCSVProcessApprovedInvoiceEform  Comment in Eform is "+commentItem);
	  	         invoice.addComment(commentItem);
	  	         Log.customer
	  		 		.debug("CatCSVProcessApprovedInvoiceEform  Comment in added to Invoice "+commentItem);
	         }

       //End

		//added by Nani on 4/19/2005
      invoice.setPaymentTerms((PaymentTerms)eform.getFieldValue("PaymentTerms"));
		//end-add

		// Added by Majid to copy field's value from Invoice Eform to Invoice -- Starts here

		// BlockStampDate
		// RelatedCatInvoice
		// CustomSuppLoc - TBD

			Log.customer.debug("Setting BlockStampDate value from Eform to Invoice Object");
		  	invoice.setFieldValue("BlockStampDate",(Date)eform.getFieldValue("BlockStampDate"));
			Log.customer.debug(" RelatedCatInvoice value => " + eform.getFieldValue("RelatedCatInvoice"));
			if(eform.getFieldValue("RelatedCatInvoice")!= null)
		  	{
				invoice.setFieldValue("RelatedCatInvoice",(String)eform.getFieldValue("RelatedCatInvoice"));
			}else
			{
				Log.customer.debug("RelatedCatInvoice value is null");
			}

			if(eform.getFieldValue("WithHoldTaxCode")!= null)
		  	{
				invoice.setFieldValue("WithHoldTaxCode",(ClusterRoot)eform.getFieldValue("WithHoldTaxCode"));
			}
			if(eform.getFieldValue("CurrencyExchangeRate")!= null)
			{
			Log.customer.debug(" CurrencyExchangeRate value => " + eform.getFieldValue("CurrencyExchangeRate"));
			invoice.setFieldValue("CurrencyExchangeRate",(String)eform.getFieldValue("CurrencyExchangeRate"));
			}
		// Added by Majid to copy field's value from Invoice Eform to Invoice -- Ends here

		// Get the totals from the Eform first
      Money totalInvoicedLessTax = (Money)
          eform.getFieldValue("TotalInvoicedLessTax");
      Money totalInvoiced = (Money)
          eform.getFieldValue("EnteredInvoiceAmount");
      Money totalTax = (Money)
          eform.getFieldValue("TotalTax");

		// And negate them if this is a credit memo
      boolean isCreditMemo =
          Invoice.PurposeCreditMemo.equals(
              (String)eform.getFieldValue("Purpose"));

      if (isCreditMemo) {
          totalInvoicedLessTax = totalInvoicedLessTax.negate();
          totalTax = totalTax.negate();
          totalInvoiced = totalInvoiced.negate();
      }

		// Then set the totals on the Invoice
      invoice.setTotalInvoicedLessTax(totalInvoicedLessTax);
      invoice.setTotalTax(totalTax);
      invoice.setTotalInvoiced(totalInvoiced);

		// Set the extrinsics on the Invoice
      invoice.setFieldValue("Eform", Boolean.TRUE);
      invoice.setFieldValue("InvoiceEform", eform);
      invoice.setFieldValue("Terms",
                            (String)eform.getFieldValue("Terms"));

		// Get the MasterAgreement number if it was set
      Contract ma = (Contract)eform.getFieldValue("MasterAgreement");
      String maNumber = null;
      if (ma != null) {
          maNumber = ma.getUniqueName();
      }

		// Now go through all of the Eform line items
      List eformLineItems = (List)eform.getFieldValue("LineItems");
      StatementOrderInfo prevInfo = null;
      boolean consolidated = false;
      int size = ListUtil.getListSize(eformLineItems);
      for (int i = 0; i < size; i++) {
          BaseObject eformLI = (BaseObject)eformLineItems.get(i);
            InvoiceLineItem invoiceLI = new InvoiceLineItem(partition, invoice);
            //Nagendra Start
		    if(eformLI.getFieldValue("CustomSuppLoc")!= null)
			invoiceLI.setFieldValue("CustomSuppLoc",(PostalAddress)eformLI.getFieldValue("CustomSuppLoc"));
		      Log.customer.debug(" CusSupplierlocation value => " + eformLI.getFieldValue("CustomSuppLoc"));
            //end Nagendra
			// Create a new Invoice Line Item

          invoiceLI.setDefaultsFromApprovable(invoice);

			// And copy fields from one line item to the other
          ProcureLineType lineType = (ProcureLineType)
              eformLI.getFieldValue("LineType");
          invoiceLI.setLineType(lineType);
          invoiceLI.setInvoiceLineNumber(
              IntegerFormatter.getIntValue(
                  eformLI.getFieldValue("InvoiceLineNumber")));
          invoiceLI.setOrderLineNumber(
              IntegerFormatter.getIntValue(
                  eformLI.getFieldValue("OrderLineNumber")));

		    // Set the ProductDescription field

          LineItemProductDescription pd = invoiceLI.getDescription();
          pd.setDescription(
              (String)eformLI.getFieldValue("Description"));
          if(eformLI.getFieldValue("SupplierPartNumber")!=null){
          pd.setSupplierPartNumber((String)eformLI.getFieldValue("SupplierPartNumber"));
			}
          //modified by Nani on 4/19/2005.
          Money mPrice = (Money)eformLI.getFieldValue("Price");
          if (isCreditMemo) {
				if (mPrice.getSign() > 0 &&	!ProcureLineType.isChargeCategory(lineType)) {
					mPrice = mPrice.negate();
				}
			}
			Log.customer.debug("price is " + mPrice.asString());
          pd.setPrice(mPrice);
          //end-modify

          if (ProcureLineType.isLineItemCategory(lineType)) {
              pd.setUnitOfMeasure(
                  (UnitOfMeasure)eformLI.getFieldValue("UnitOfMeasure"));
          }

          invoiceLI.setDottedFieldValue("ReferenceLineNumber", eformLI.getFieldValue("ReferenceLineNumber"));

			// Set the Supplier Order Info field
          StatementOrderInfo info = new StatementOrderInfo(partition);
          info.setOrderNumber(
              (String)eformLI.getFieldValue("OrderNumber"));
          info.setMANumber(maNumber);
          invoiceLI.setSupplierOrderInfo(info);

			// Compare Order Info to check whether consolidated invoice
          if (!consolidated && prevInfo != null) {
              consolidated = !prevInfo.equals(info);
          }
          prevInfo = info;

			// Now get the quantity and amount from the Eform
          BigDecimal qty = (BigDecimal)
              eformLI.getFieldValue("Quantity");
          Money amount = (Money)
              eformLI.getFieldValue("Amount");

			// And negate them if this is a credit memo
          if (isCreditMemo) {

				/*** comment out by Nani on 4/19/2005.
              // Negate non charges if qty is positive
              if (qty.signum() > 0 &&
                  !ProcureLineType.isChargeCategory(lineType)) {
                  qty = qty.negate();
              }
              ***/

				// if its a credit memo then
				// the amount should be negated
              if (amount.getSign() > 0) {
                  amount = amount.negate();
              }
          }

			// Then set the quantity and amount on the Invoice
          invoiceLI.setQuantity(qty);
          invoiceLI.setAmount(amount);

      		// Add TaxCode to the Invoice LineItem.
          if(eformLI.getFieldValue("TaxCode")!= null)
              invoiceLI.setFieldValue("TaxCode",(ClusterRoot)eformLI.getFieldValue("TaxCode"));

          if(eformLI.getFieldValue("TaxAmount")!= null)
              invoiceLI.setTaxAmount((Money)eformLI.getFieldValue("TaxAmount"));

			//Added by Aswini
           if(eformLI.getFieldValue("ShipFrom")!= null)		    
              invoiceLI.setFieldValue("ShipFrom",(ClusterRoot)eformLI.getFieldValue("ShipFrom"));   
			  Log.customer.debug(" ShipFrom value => " + eformLI.getFieldValue("ShipFrom")); 
		   if(eformLI.getFieldValue("TransportMode")!= null)		    
              invoiceLI.setFieldValue("TransportMode",(ClusterRoot)eformLI.getFieldValue("TransportMode"));   
			  Log.customer.debug(" TransportMode value => " + eformLI.getFieldValue("TransportMode"));
			if(eformLI.getFieldValue("TransactionNature")!= null)		    
              invoiceLI.setFieldValue("TransactionNature",(ClusterRoot)eformLI.getFieldValue("TransactionNature"));   
			  Log.customer.debug(" TransactionNature value => " + eformLI.getFieldValue("TransactionNature")); 
			if(eformLI.getFieldValue("NetWeight")!=null)
             invoiceLI.setFieldValue("NetWeight",(String)eformLI.getFieldValue("NetWeight"));
			 Log.customer.debug(" NetWeight value => " + eformLI.getFieldValue("NetWeight")); 
			// Add the new invoice line item
          invoice.getLineItems().add(invoiceLI);
      }

		// Set consolidated flag based on the POs
      invoice.setConsolidated(consolidated);
      if (!consolidated) {
          invoice.setSupplierOrderInfo(prevInfo);
      }

      InvoiceEformUtil.linkParentAndChildren(invoice);

		// Process the loaded invoice
      invoice.processLoadedInvoice();

		// Set the invoiceeform id to be the same as that of the invoice id
      FastStringBuffer invUniqueName =
          new FastStringBuffer(invoice.getUniqueName());
      invUniqueName.replace(invoiceIdentifier, inveformIdentifier);
      eform.setUniqueName(invUniqueName.toString());
  }

    protected ValueInfo getValueInfo()
    {
        return new ValueInfo(0, "config.java.invoiceeform.InvoiceEform");
    }

    public static final String ClassName = "CatSAPOProcessApprovedInvoiceEform";
    private static final String invoiceIdentifier = "INV";
    private static final String inveformIdentifier = "INEF";
}