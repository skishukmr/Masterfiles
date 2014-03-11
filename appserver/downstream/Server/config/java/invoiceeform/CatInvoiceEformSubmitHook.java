/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to validate invoice eform during the submit.
*/

package config.java.invoiceeform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;

public class CatInvoiceEformSubmitHook implements ApprovableHook
{
    protected static final String ComponentStringTable = "aml.InvoiceEform";
	protected static final String catComponentStringTable = "aml.cat.Invoice";

    protected static final int ValidationError = -2;
    protected static final int ValidationWarning = 1;
    protected static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));

    public List run (Approvable approvable)
    {
        ClusterRoot cr = (ClusterRoot)approvable;

        Money enteredInvoiceAmount 	= (Money) cr.getFieldValue("EnteredInvoiceAmount");

        if (enteredInvoiceAmount == null) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                               ResourceService.getString(catComponentStringTable,
                                       "InvalidEnteredInvoiceAmount"));
        }

        /// Verify that there are line items on the invoice
        List lineItems = (List)cr.getFieldValue("LineItems");
        int size = ListUtil.getListSize(lineItems);
        if (size == 0) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     ResourceService.getString(ComponentStringTable,
                                             "EmptyInvoice"));
        }

        Money invTotLnAmt 		= new Money(Constants.ZeroBigDecimal, enteredInvoiceAmount.getCurrency());

        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);

            try {

				Money invLnAmt = (Money)lineItem.getFieldValue("Amount");

				//make sure that header ccy and the lines ccy match
				if (invLnAmt.getCurrency() != enteredInvoiceAmount.getCurrency()) {
					return ListUtil.list(Constants.getInteger(ValidationError),
										 ResourceService.getString(catComponentStringTable,
												 "InvalidCurrency"));
				}

				//make sure that po ccy and invoice ccy match
				PurchaseOrder po = (PurchaseOrder)lineItem.getFieldValue("Order");
				if (po != null) {
					if (po.getTotalCost().getCurrency() != enteredInvoiceAmount.getCurrency()) {
						return ListUtil.list(Constants.getInteger(ValidationError),
											 ResourceService.getString(catComponentStringTable,
													 "CurrencyMismatch"));
					}
				}

                invTotLnAmt = Money.add(invTotLnAmt,invLnAmt);

			} catch (NullPointerException ne) {}

        }

        // And then compare the totals (use approx. for currencies)
        if (invTotLnAmt.approxCompareTo(enteredInvoiceAmount) != 0) {
            String fmt =  Fmt.Sil(catComponentStringTable,
                                  "NonMatchingTotal",
                                  enteredInvoiceAmount.asString(),
                                  invTotLnAmt.asString());
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     fmt);
        }

        return NoErrorResult;
    }
}
