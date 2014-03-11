/*
    Author: Shaila Salimath
    Purpose: CR # 755 The purpose of this class is to validate invoice during the submit.

    Amit - Added lines 93,94,95 - 3rd March 2008

    Ariba Upgrade Lab: 23rd Nov 2010

        - Migrated Invoice eForm submit hook logic (MFG)
        - Made changes as per the 9r API
        - Indentation and formatting changes
*/
package config.java.invoicing.vcsv2;

import java.math.BigDecimal;
import java.util.List;
import java.util.ListIterator;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.PurchaseOrder;
import ariba.tax.core.TaxID;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;

public class CatMFGInvSubmitHook implements ApprovableHook
{


    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Class Name
    */
    public static final String ClassName = CatMFGInvSubmitHook.class.getName();

    /**
        The abbreviated class name (used for logging purposes)
    */
    public static final String cn = "CatMFGInvSubmitHook";

    /**
        Resource String Table
    */
    protected static final String InvoiceEformStringTable = "aml.InvoiceEform";
    protected static final String InvoiceStringTable = "aml.cat.Invoice";

    /**
        Validation Constants
    */
    protected static final int ValidationError = -2;
    protected static final int ValidationWarning = 1;
    protected static final List NoErrorResult =
        ListUtil.list(Constants.getInteger(NoError));


    /*-----------------------------------------------------------------------
        Overridden Methods
      -----------------------------------------------------------------------*/

    public List run (Approvable approvable)
    {
        List list = ListUtil.list();
        Invoice inv = (Invoice) approvable;
        int category = inv.getCategory();

            // no need to check if it is UI based since
            // we are using a submit hook only for
            // UI based invoices
        if (category == Invoice.CategoryOrder) {
            list = processPOInvoice(inv);
        }
        else {
            list = processNonPOInvoice(inv);
        }
        return list;
    }


    /*-----------------------------------------------------------------------
        Helper Methods - PO Based Invoice - Migrated from Invoice eForm
      -----------------------------------------------------------------------*/

    /**
        Process PO Based Invoice - Logic is migrated from invoice eForm in
        Viking.

        @param approvable the invoice
        @list  list of error codes
    */
    public List processPOInvoice (Invoice inv)
    {
        List list = validateCurrency(inv);
        Integer integer = (Integer) list.get(0);
        if (integer != null) {
            if (integer.intValue() != NoError) {
                return list;
            }
        }

        Money totalTaxAmount = inv.getTotalTax();

        TaxID supplierTaxID = inv.getSupplierTaxID();
        Money computedTaxAmount =
            new Money(
                    Constants.ZeroBigDecimal,
                    totalTaxAmount.getCurrency());

        boolean nonTaxLineFound = false;
        boolean taxLineFound = false;

        List invLineItems = inv.getLineItems();

            // suppress summary invoice (KM)
        List orders = ListUtil.list();
        for (int j = 0; j < invLineItems.size(); j++) {

            InvoiceLineItem ili = (InvoiceLineItem) invLineItems.get(j);
            Object order = ili.getOrder();
            if (order != null) {
                ListUtil.addElementIfAbsent(orders, order);
            }
        }

            // summary invoice
        if(orders.size() > 1) {
            String s =
                ResourceService.getString(
                        InvoiceEformStringTable,
                        "MFGSummaryInvoiceError");
            return ListUtil.list(Constants.getInteger(-2), s);
        }

        int numberOfTaxLines = 0;
        int numberOfNonTaxLines = 0;
        PurchaseOrder po;
        List poList = ListUtil.list();

        for (int i = 0; i < invLineItems.size(); i++) {

            InvoiceLineItem ili = (InvoiceLineItem) invLineItems.get(i);
            String lineTypeName =
                (String) ili.getDottedFieldValue("LineType.Name.PrimaryString");

            try {
                    // make sure that SupplierTaxID is entered when VAT is present
                if (lineTypeName.equals("VAT")) {
                    taxLineFound = true;
                    if (supplierTaxID == null) {
                        return ListUtil.list(Constants.getInteger(ValidationError),
                                             ResourceService.getString(
                                                     InvoiceStringTable,
                                                     "EnterSupplierTaxID"));
                    }
                    numberOfTaxLines = numberOfTaxLines + 1;
                }
                else {
                    nonTaxLineFound = true;
                    numberOfNonTaxLines = numberOfNonTaxLines + 1;
                    po = ili.getOrder();
                    ListUtil.addElementIfAbsent(poList, po);
                }

                Money taxAmount = ili.getTaxAmount();

                    // change scale
                taxAmount.setAmount(
                        taxAmount.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));

                    // calculate the tax amount - sum of all VAT lines
                if (taxAmount != null) {
                    computedTaxAmount = Money.add(computedTaxAmount, taxAmount);
                }

            } catch (NullPointerException ne) {}
        }

        if (nonTaxLineFound) {

                // If an invoice contains one or more non-tax line(s),
                // make sure that there is a tax line.
            if (!taxLineFound) {
                String fmt =
                    ResourceService.getString(
                            InvoiceStringTable,
                            "EnterTaxLine");
                return ListUtil.list(
                        Constants.getInteger(ValidationError), fmt);
            }

                // If summary invoice, make sure that the user enters one tax line
                // per distinct order.
            int numberOfOrders = poList.size();

                // we may not need this section as summary invoices are already
                // supressed
            if (numberOfOrders > 1) {
                if ((numberOfTaxLines < numberOfOrders) ||
                        (numberOfTaxLines > numberOfOrders &&
                                numberOfTaxLines != numberOfNonTaxLines)) {

                    // if there is not a tax line for every distinct order OR if the user
                    // has not entered as many tax lines as there are number of non tax
                    // lines (line level VAT), throw error
                    String fmt =
                        Fmt.Sil(InvoiceStringTable,
                                "EnterOneTaxLinePerDistinctOrder",
                                new Integer(numberOfOrders));
                    return ListUtil.list(Constants.getInteger(ValidationError), fmt);
                }
            }

                // If an invoice contains non tax lines, make sure that the computed tax
                // amount matches exactly with entered total tax amount ignoring any round
                // off errors. If it does not, return error. If an invoice contains only
                // tax line(s), do not do this validation.
            if ((totalTaxAmount.approxCompareTo(computedTaxAmount) != 0)) {
                String fmt = Fmt.Sil(InvoiceStringTable,
                                      "NonMatchingTotalTax",
                                      totalTaxAmount.asString(),
                                      computedTaxAmount.asString());
                return ListUtil.list(Constants.getInteger(ValidationError),
                                         fmt);
            }
        }
        return NoErrorResult;
    }

    /**
        Validate the invoice currency with the currency selected by the user in
        the currency selection page.

        @param approvable the invoice
        @list  error list
    */
    protected List validateCurrency (Approvable approvable)
    {
        String mn = cn + ".validateCurrency(): ";
        ClusterRoot cr = (ClusterRoot) approvable;

        Money enteredInvoiceAmount =
            (Money) cr.getFieldValue("EnteredInvoiceAmount");

        if (enteredInvoiceAmount == null) {
            return ListUtil.list(
                    Constants.getInteger(ValidationError),
                    ResourceService.getString(
                            InvoiceStringTable,
                            "InvalidEnteredInvoiceAmount"));
        }

            // Verify that there are line items on the invoice
        List lineItems = (List)cr.getFieldValue("LineItems");
        int size = ListUtil.getListSize(lineItems);
        if (size == 0) {
            return ListUtil.list(
                    Constants.getInteger(ValidationError),
                    ResourceService.getString(
                            InvoiceEformStringTable,
                            "EmptyInvoice"));
        }

        Money invTotLnAmt =
            new Money(
                    Constants.ZeroBigDecimal,
                    enteredInvoiceAmount.getCurrency());

        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);
            try {
                Money invLnAmt = (Money)lineItem.getFieldValue("Amount");

                    // make sure that header ccy and the lines ccy match
                if (invLnAmt.getCurrency() != enteredInvoiceAmount.getCurrency()) {
                    return ListUtil.list(
                            Constants.getInteger(ValidationError),
                            ResourceService.getString(
                                    InvoiceStringTable,
                                    "InvalidCurrency"));
                }

                    // make sure that po ccy and invoice ccy match
                PurchaseOrder po = (PurchaseOrder)lineItem.getFieldValue("Order");
                if (po != null) {
                    if (po.getTotalCost().getCurrency() != enteredInvoiceAmount.getCurrency()) {
                        return ListUtil.list(
                                Constants.getInteger(ValidationError),
                                ResourceService.getString(
                                        InvoiceStringTable,
                                        "CurrencyMismatch"));
                    }
                }

                invTotLnAmt = Money.add(invTotLnAmt,invLnAmt);

            } catch (NullPointerException ne) {}
        }

            // And then compare the totals (use approx. for currencies)
        if (invTotLnAmt.approxCompareTo(enteredInvoiceAmount) != 0) {
            String fmt = Fmt.Sil(InvoiceStringTable,
                                  "NonMatchingTotal",
                                  enteredInvoiceAmount.asString(),
                                  invTotLnAmt.asString());
            return ListUtil.list(Constants.getInteger(ValidationError),
                                     fmt);
        }
        return NoErrorResult;
    }


   /*-----------------------------------------------------------------------
       Helper Methods - Non PO Based Invoice - UI Based Invoices 822
    -----------------------------------------------------------------------*/

    /**
        Process Non PO Based Invoice - This is the existing logic for UI based
        invoices in 822

        @param inv the invoice
        @list      list of error codes
    */
    public List processNonPOInvoice (Invoice inv)
    {
        String mn = cn + ".processNonPOInvoice(): ";
        String errorMsg = "";
        String warningMsg = "";
        String e1 = "";
        String e2 = "";

        e1 = validateINVAmount(inv);

        if (!e1.equals("")) {
            errorMsg = new String(e1);
        }
        if(!errorMsg.equals("")) {
            return ListUtil.list(Constants.getInteger(-2), errorMsg);
        }
        else {
            processCreditInvoice(inv);
        }
        return NoErrorResult;
    }

    /**
        Validate the invoice amount

        @param inv the invoice
        @list      error list
    */
    protected String validateINVAmount (Invoice inv)
    {
        String mn = cn + ".validateINVAmount(): ";
        String fmt = "";
        Money totalTaxAmount = inv.getTotalTax();
        Money computedTaxAmount =
            new Money(Constants.ZeroBigDecimal, totalTaxAmount.getCurrency());
        boolean nonTaxLineFound = false;
        boolean taxLineFound = false;
        List invLineItems = inv.getLineItems();
        int numberOfTaxLines = 0;
        int numberOfNonTaxLines = 0;
        PurchaseOrder po;
        List poList = ListUtil.list();
        List taxLineItems = ListUtil.list();
        List nonTaxLineItems = ListUtil.list();
        BigDecimal zero = new BigDecimal("0.000000000");
        BigDecimal vatRate = new BigDecimal("0.0");

        for (int i = 0; i < invLineItems.size(); i++) {

            InvoiceLineItem ili = (InvoiceLineItem) invLineItems.get(i);
            Log.customer.debug("%s Line Item: %s", mn, ili);
            try {

                String lineTypeName =
                    (String) ili.getDottedFieldValue("LineType.Name.PrimaryString");
                vatRate =
                    (BigDecimal) ili.getDottedFieldValue(
                            "VATClass.RateInPercentage");

                    // VAT (Tax) line items.
                    // We won't allow header level tax
                if (!(lineTypeName == null) && (lineTypeName.equals("VAT"))) {

                    Log.customer.debug(
                            "%s Line Item %s is a VAT line item.",
                            mn,
                            ili);
                    taxLineFound = true;
                    Money liAmount = (Money) ili.getFieldValue("Amount");
                    numberOfTaxLines = numberOfTaxLines + 1;
                    ListUtil.addElementIfAbsent(taxLineItems, ili);

                    Log.customer.debug(
                            "%s Setting the IsTaxInLine field on the Invoice",
                            mn);

                        // S. Sato - AUL
                        // In 9r the header and line level tax works in the following manner
                        // - Header (the parent is the default line item)
                        // - Line (the parent is a material line item)
                    /*
                    if(ili.getMatchedLineItem() != null) {
                        Log.customer.debug(
                                "%s Matched line item is present - " +
                                "implying line level tax",
                                mn);
                        Invoice invObj = (Invoice) ili.getLineItemCollection();
                        Log.customer.debug("%s Invoice Object: %s", mn, invObj);
                        invObj.setIsTaxInLine(true);
                    }
                    else {
                        Log.customer.debug(
                                "%s Matching line item not found - " +
                                "Header level tax added",
                                mn);
                        String result =
                            ResourceService.getString(
                                    InvoiceStringTable,
                                    "HeaderTaxNotAllowed");
                        fmt = Fmt.S(result,(i+1));
                        return fmt;
                    }
                    */

                        // new logic 9r for determining header level tax
                    ProcureLineItem parentLi = ili.getParent();
                    if (parentLi != null) {
                        int parentLineNo = parentLi.getNumberInCollection();

                            // header level tax points to default line item
                        if (parentLineNo == 0) {
                            Log.customer.debug(
                                    "%s Matching line item not found - " +
                                    "Header level tax added",
                                    mn);
                            String result =
                                ResourceService.getString(
                                        InvoiceStringTable,
                                        "HeaderTaxNotAllowed");
                            fmt = Fmt.S(result,(i+1));
                            return fmt;
                        }
                        else {
                            Log.customer.debug(
                                    "%s Matched line item is present - " +
                                    "implying line level tax",
                                    mn);
                            Invoice invObj = (Invoice) ili.getLineItemCollection();
                            Log.customer.debug("%s Invoice Object: %s", mn, invObj);
                            invObj.setIsTaxInLine(true);
                        }
                    }

                        // computing the total tax amount = sum of all VAT line amounts
                    if(liAmount != null) {
                        computedTaxAmount =
                            Money.add(computedTaxAmount, liAmount);
                    }
                }

                    // non tax line item
                else if (!(vatRate.compareTo(zero) == 0)) {

                    Log.customer.debug(
                            "%s Line Item: %s is a non tax line item.", mn, ili);
                    nonTaxLineFound = true;
                    numberOfNonTaxLines = numberOfNonTaxLines + 1;
                    ListUtil.addElementIfAbsent(nonTaxLineItems, ili);
                }
            }
            catch (Exception e) {
                Log.customer.debug("%s Exception Occurred: %s", mn, e.toString());
            }
        }

            // If an invoice contains one or more non-tax line(s),
            // make sure that there is a tax line.
        ContractLineItem maLine = null;
        ContractLineItem matchedLine = null;
        InvoiceLineItem nonTaxLine = null;
        InvoiceLineItem taxLine = null;

            // there are no tax lines
        if (((vatRate != null) &&
                !(vatRate.compareTo(zero) == 0)) &&
                (taxLineItems.size() <= 0))
        {
            Log.customer.debug(
                    "%s Validation Error - tax line is not entered.", mn);
            fmt = ResourceService.getString(InvoiceStringTable, "EnterTaxLine");
            return fmt;
        }

            // check if no of material line matches with no of tax lines for all VAT
            // classes other than VAT Rate 0
        if (((vatRate != null) &&
                !(vatRate.compareTo(zero) == 0)) &&
                (nonTaxLineItems.size() != taxLineItems.size()))
        {
            Log.customer.debug(
                    "%s Validation Error - The non tax line does not " +
                    "have a corresponding tax line.",
                    mn);
            fmt = ResourceService.getString(InvoiceStringTable, "EnterTaxLine");
            return fmt;
        }

            // check if there exists tax lines and it matches with material lines
        for (ListIterator outIt = nonTaxLineItems.listIterator(); outIt.hasNext();) {

            nonTaxLine = (InvoiceLineItem) outIt.next();
            maLine = (ContractLineItem) nonTaxLine.getMALineItem();
            Log.customer.debug(
                    "%s Non Tax Line: Associated MA Line: %s",
                    mn,
                    maLine);
        }

        for (ListIterator inIt = taxLineItems.listIterator(); inIt.hasNext();) {

            taxLine = (InvoiceLineItem) inIt.next();
            matchedLine = (ContractLineItem) taxLine.getMatchedLineItem();
            Log.customer.debug(
                    "%s Non Tax Line: MA Line: %s",
                    mn,
                    matchedLine);
        }
        if (maLine != null && matchedLine != null) {
            BaseId maLineBaseId = maLine.getBaseId();
            BaseId matchedLineBaseId = matchedLine.getBaseId();
            Log.customer.debug("%s MA Line Base ID: %s", mn, maLineBaseId);
            Log.customer.debug("%s Matched Line Base ID: %s", mn, matchedLineBaseId);

            if (!(maLineBaseId.equals(matchedLineBaseId))) {

                Log.customer.debug(
                        "%s Validation Error - The non tax line is not " +
                        "associated with a tax Line");
                String errorResult =
                    ResourceService.getString(
                            InvoiceStringTable,
                            "TaxLineMissing");
                fmt = Fmt.S(errorResult,nonTaxLine.getNumberInCollection());
                return fmt;
            }
        }

            // If an invoice contains non tax lines, make sure that the computed
            // tax amount matches exactly with entered total tax amount ignoring
            // any round off errors. If it does not, return error. If an invoice
            // contains only tax line(s), do not do this validation.
        Money totalInvoicedLessTax = inv.getTotalInvoicedLessTax();
        totalInvoicedLessTax.setAmount(
                totalInvoicedLessTax.getAmount().setScale(2, 4));
        Log.customer.debug("%s Total %s", mn, totalInvoicedLessTax);

        Money enteredInvAmount = (Money) inv.getFieldValue("EnteredInvoiceAmount");
        Money computedAmount = Money.add(computedTaxAmount, totalInvoicedLessTax);
        if (enteredInvAmount != null) {
            Log.customer.debug(
                    "%s Entered Invoice Amount: %s", mn, enteredInvAmount);
        }
        if (computedAmount.approxCompareTo(enteredInvAmount) != 0) {

            Log.customer.debug("%s Computed Tax Amount: %s", mn, computedTaxAmount);
            String nonMatchingInvAmtMsg =
                ResourceService.getString(
                        InvoiceStringTable,
                        "NonMatchingInvoiceAmount");
            fmt = Fmt.S(
                    nonMatchingInvAmtMsg,
                    enteredInvAmount.asString(),
                    computedAmount.asString());
            return fmt;
        }
        return fmt;
    }

    /**
        Process a credit memo

        @param cr the invoice clusterroot
        @list  error list
    */
    protected void processCreditInvoice (ClusterRoot cr)
    {
        String mn = cn + ".catMFGSetCreditInvoice(): ";
        Invoice inv = (Invoice) cr;
        Log.customer.debug("%s Invoice: %s", mn, inv);

        Money totalInvoicedLessTax = inv.getTotalInvoicedLessTax();
        Money totalInvoiced = (Money) inv.getFieldValue("EnteredInvoiceAmount");
        Money totalTax = inv.getTotalTax();
        Log.customer.debug(
                "%s Total Invoiced Less Tax: %s",
                mn,
                totalInvoicedLessTax);
        Log.customer.debug("%s Total Invoiced Less Tax: %s", mn, totalInvoiced);
        Log.customer.debug("%s Total Tax: %s", mn, totalTax);

        boolean isCreditMemo = inv.isCreditMemo();
        if (isCreditMemo) {

                 // negate the amounts
            Log.customer.debug(
                    "%s Invoice: %s is a credit memo",
                    mn,
                    inv.getUniqueName());
            totalInvoicedLessTax = totalInvoicedLessTax.negate();
            totalTax = totalTax.negate();
            totalInvoiced = totalInvoiced.negate();
            inv.setTotalInvoicedLessTax(totalInvoicedLessTax);
            inv.setTotalTax(totalTax);
        }
        inv.setTotalInvoiced(totalInvoiced);

        Log.customer.debug(
                "%s New Total Invoiced Less Tax: %s", mn, totalInvoicedLessTax);
        Log.customer.debug("%s New Total Tax: %s", mn, totalTax);
        Log.customer.debug("%s New Total Invoiced: %s", mn, totalInvoiced);

             // line items
        List invLineItems = inv.getLineItems();
        for (int i = 0; i < invLineItems.size(); i++) {

            InvoiceLineItem ili = (InvoiceLineItem) invLineItems.get(i);
            Log.customer.debug("%s Invoice Line Item: %s", ili);
            LineItemProductDescription pd = ili.getDescription();
            ProcureLineType lineType =
                (ProcureLineType) ili.getFieldValue("LineType");
            String procureLineTypeName =
                (String) ili.getDottedFieldValue("LineType.Name.PrimaryString");
            Money price = pd.getPrice();

            if (isCreditMemo) {
                if (price.getSign() > 0 &&
                        !ProcureLineType.isChargeCategory(lineType) &&
                        (procureLineTypeName != "Shiping"))
                {
                    price = price.negate();
                    pd.setPrice(price);
                }
            }
            Log.customer.debug("%s Price: %s", mn, price.asString());
            Money amount = ili.getAmount();
            Money taxAmount = ili.getTaxAmount();
            if (isCreditMemo) {

                if (amount.getSign() > 0) {
                    amount = amount.negate();
                    ili.setAmount(amount);
                }
                try {
                    if (taxAmount.getSign() > 0) {
                        taxAmount = taxAmount.negate();
                        ili.setTaxAmount(taxAmount);
                    }
                }
                catch(NullPointerException ne) {}
            }
        }
    }
}