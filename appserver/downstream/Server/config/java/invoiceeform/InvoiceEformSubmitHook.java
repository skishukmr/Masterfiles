/*
    Copyright (c) 1996-2003 Ariba, Inc.
    All rights reserved. Patents pending.


    Responsible: ariba
*/

package config.java.invoiceeform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;

/**
    Checks that the sum of the lineitems matches the invoice header amount.
*/
public class InvoiceEformSubmitHook implements ApprovableHook
{
    private static final String ComponentStringTable = "aml.InvoiceEform";

    private static final int ValidationError = -2;
    private static final List NoErrorResult =
        ListUtil.list(Constants.getInteger(NoError));

    public List run (Approvable approvable)
    {
        ClusterRoot cr = (ClusterRoot)approvable;

        Money invSubTot = (Money)cr.getFieldValue("TotalInvoicedLessTax");
        Money invTotTax = (Money)cr.getFieldValue("TotalTax");
        Money invTotShip = (Money)cr.getFieldValue("TotalShipping");

        if (invSubTot == null) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                               ResourceService.getString(ComponentStringTable,
                                       "InvalidInvoiceHeaderSubtotalAmount"));
        }

        if (invTotTax == null) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                               ResourceService.getString(ComponentStringTable,
                                       "InvalidInvoiceHeaderTaxAmount"));
        }

        if (invTotShip == null) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                               ResourceService.getString(ComponentStringTable,
                                       "InvalidInvoiceHeaderShippingAmount"));
        }

            // Sum the header amounts
        Money invTotAmt = Money.add(invSubTot, invTotTax);
        invTotAmt = Money.add(invTotAmt, invTotShip);

            /// Verify that there are line items on the invoice
        List lineItems = (List)cr.getFieldValue("LineItems");
        int size = ListUtil.getListSize(lineItems);
        if (size == 0) {
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     ResourceService.getString(ComponentStringTable,
                                             "EmptyInvoice"));
        }

            // Sum the line amounts
        Money invTotLnAmt = new Money(Constants.ZeroBigDecimal,
                                      invTotAmt.getCurrency());
        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);
            Money invLnAmt = (Money)lineItem.getFieldValue("Amount");

            if (invLnAmt != null) {
                invTotLnAmt = Money.add(invTotLnAmt,invLnAmt);
            }
        }

            // And then compare the totals (use approx. for currencies)
        if (invTotLnAmt.approxCompareTo(invTotAmt) != 0) {
            String fmt =  Fmt.Sil(ComponentStringTable,
                                  "NonMatchingTotal",
                                  invTotAmt.asString(),
                                  invTotLnAmt.asString());
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     fmt);
        }

        return NoErrorResult;
    }
}
