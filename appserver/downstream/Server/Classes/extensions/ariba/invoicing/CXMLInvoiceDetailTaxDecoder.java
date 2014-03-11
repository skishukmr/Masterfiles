/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/invoicing/6.28.1+/ariba/invoicing/CXMLInvoiceDetailTaxDecoder.java#3 $

    Responsible: mpoolu
*/
package ariba.invoicing;

import ariba.base.core.Partition;
import ariba.basic.core.Money;
import ariba.encoder.xml.AXComponent;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceUtil;
import ariba.invoicing.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.procure.core.TaxTypeMapEntry;
import ariba.statement.core.StatementCoreApprovable;
import ariba.tax.core.TaxDetail;
import ariba.tax.core.TaxInfo;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import java.math.BigDecimal;


/**
    CXML decoding component for creating invoice lines from Tax elements within
    invoice detail request message.

    @aribaapi documented
*/
public class CXMLInvoiceDetailTaxDecoder extends AXComponent

{
    public static final String ClassName = CXMLInvoiceDetailTaxDecoder.class.getName();

    public String taxDescription;
    public String taxPurpose;
    public String taxType;
    public BigDecimal taxRate;
    public Money taxableAmount;
    private Money taxAmount;
    public Money altTaxAmount;
    public Money amount;
    public String taxLocation;

    public String triangularTransactionLawReference;
    public String isTriangularTransaction;

    public boolean created;
    public InvoiceLineItem parentLineItem;
    public TaxDetail taxDetail;
    public java.util.Vector taxDetails;
    public java.util.Hashtable request;

    protected CXMLInvoiceLineItemCreator creator = new CXMLInvoiceLineItemCreator();

    /**
        getter accessor for tax amount. This is parsed from the Money tag under the Tax tag.
        It is $11 in this specific example.
        <pre>
        <InvoiceDetailSummary>
            <SubtotalAmount>
               <Money currency="USD">100</Money>
            </SubtotalAmount>
            <Tax>
                <Money currency="USD">11.00</Money>
                <Description xml:lang="en-US" />
        </pre>
    */
    public Money getTaxAmount ()
    {
        return taxAmount;
    }

    /**
        @see #getTaxAmount
        sets the totalTax on the invoice from the total tax specified in the
        cXML of InvoiceDetailsSummary
    */
    public void setTaxAmount (Money taxAmount)
    {
        this.taxAmount = taxAmount;
        if (booleanValueForBinding("saveTaxOnInvoice")) {
            creator.invoice.setTotalTax(taxAmount);
        }
    }
    /**
       The awake method is used to reinitialize any variables which were nulled
       out when this instance of this class was put to sleep. It should always
       invoke the super to make sure anything the super depends on is also woken.
    */
    public void awake ()
    {
        super.awake();
        creator = new CXMLInvoiceLineItemCreator();
    }

    /**
       The sleep method is used to null out any instance variables. This makes
       sure that when the object is reused it will not inherit any data from
       the last run. This null state is enforced by the AW layer. In addition
       error reporting on a slept object is likely to cause some form of
       object out date exception. It is not sufficient to simply clear lists,
       tables, etc, they actually need to be nulled out.
    */
    protected void sleep ()
    {
        super.sleep();
        taxDescription = null;
        taxPurpose = null;
        taxType = null;
        taxRate = null;
        taxableAmount = null;
        taxAmount = null;
        amount = null;
        taxLocation = null;
        triangularTransactionLawReference = null;
        isTriangularTransaction = null;
        created = false;
        parentLineItem = null;
        taxDetail = null;
        taxDetails = null;
        request = null;
        creator = null;

    }

    public void initVars ()
    {
        Log.invoiceLoading.debug("%s.initVars", ClassName);
        creator.initVars();
        taxDescription = null;
        taxPurpose = null;
        taxType = null;
        taxRate = null;
        taxableAmount = null;
        taxAmount = null;
        amount = null;
        taxLocation = null;
        triangularTransactionLawReference = null;
        isTriangularTransaction = null;

        parentLineItem = null;
        taxDetail = null;
        taxDetails = null;
        request = null;
        created = false;
    }

    public void setInvoice (Invoice invoice)
    {
        creator.invoice = invoice;
    }

    public void createLineItem ()
    {

        Log.invoiceLoading.debug("Creating tax line item");
        Partition partition = parentLineItem.getPartition();

        ProcureLineType taxType;
        Money tax;
        String taxDesc;
        if (taxDetail != null) {
            taxType =
                lookupTaxType(taxDetail, partition);
            tax = taxDetail.getTaxAmount();
            taxDesc = taxDetail.getDescription();
        }
        else {
            taxDetail = new TaxDetail(partition);
            taxType = getDefaultTaxLineType();
            tax = taxAmount;
            if (StringUtil.nullOrEmptyOrBlankString(taxDescription)) {
                taxDesc = StatementCoreApprovable.getDefaultTaxDescriptionString();
            }
            else {
                taxDesc = taxDescription;
            }
        }

        String taxCat = taxDetail.getCategory();
        boolean ignoreZeroTax = InvoiceUtil.ignoreZeroHeaderTax(partition);

        if (ignoreZeroTax &&
            !(creator.invoice.getIsTaxInLine()) &&
            !"vat".equalsIgnoreCase(taxCat) &&
            tax.isApproxZero()) {

            Log.invoiceLoading.debug("Ignoring zero tax. Did not create tax line item.");
            return;
        }

            // AUL S. Sato Cat Tax Related Custom logic
        if (taxDetail.getTaxPointDate() == null &&
                creator.invoice != null) {

                // set tax point date to invoice date if tax point date is null
            Log.customer.debug("setting tax point date");
            taxDetail.setTaxPointDate(creator.invoice.getInvoiceDate());
        }
            // AUL S. Sato End of customization

        creator.reference = parentLineItem.getSupplierOrderInfo();

        InvoiceLineItem invoiceLineItem =
            creator.createLineItem(taxType,
                                   taxDesc,
                                   parentLineItem.getInvoiceLineNumber(),
                                   Constants.OneBigDecimal,
                                   tax,
                                   request);

        if (invoiceLineItem == null) {

                // ensure that this is the case.. typically for
                // tax lines
            return;
        }

        //generate and set the tax detail object on the line item
        invoiceLineItem.setTaxDetail(taxDetail);

        TaxInfo taxInfo = new TaxInfo(partition);
        invoiceLineItem.setExpectedTax(taxInfo);

        //do the parent-child linking here now
        creator.linkParentAndChild(parentLineItem, invoiceLineItem);
        created = true;
        Log.invoiceLoading.debug("Created tax line item: %s", invoiceLineItem);
    }


    public void initTaxDetail ()
    {
        taxDetail.init(parentLineItem.getPartition());
        if (StringUtil.nullOrEmptyOrBlankString(taxDescription)) {
            taxDescription = StatementCoreApprovable.getDefaultTaxDescriptionString();
        }
        taxDetail.setDescription(taxDescription);
    }

    public void processTaxDetail ()
    {
        taxDetail.setIsTriangularTransaction(
            isTriangularTransaction != null &&
            isTriangularTransaction.equalsIgnoreCase("yes"));
    }


    /**
        Lookup the tax type from the tax details. If a tax type cannot be looked up
        from the TaxDetail, then the default tax type will be returned

        @aribaapi documented

        @param  taxDetail taxdetail data
        @param  partition the partition
        @return taxtype the tax type

        @see #getDefaultTaxLineType
    */
    protected ProcureLineType lookupTaxType (TaxDetail taxDetail,
                                             Partition partition)
    {
        ProcureLineType type = null;
        String taxTypeName = Constants.EmptyString;
        if (taxDetail != null) {

            taxTypeName = taxDetail.getCategory();
            type = TaxTypeMapEntry.lookupTaxLineTypeByCommonName(taxTypeName, partition);
        }

        if (type == null) {
            type = getDefaultTaxLineType();
            Log.invoiceLoading.warning(8384, this, taxTypeName,
                                       type == null ? Constants.EmptyString :
                                       type.getUniqueName());
        }
        return type;
    }

    /**
        Return the default tax line type. This is used when the tax type cannot be
        figured out from the TaxDetail.category attribute. This attribute should map
        to the common tax type names specified by the TaxTypeMapEntry table. If there
        is no match, then we use the default tax type returned from this method.

        @aribaapi documented

        @return the default tax type
    */
    protected ProcureLineType getDefaultTaxLineType ()
    {
        ProcureLineType taxLineType =
            (ProcureLineType)request.get("taxLineType");
        return taxLineType;
    }


}
