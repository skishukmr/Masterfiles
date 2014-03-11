/*
    Copyright (c) 1996-2010 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/config/java/invoicing/CATInvoiceCreator.java#2 $

    Responsible: ssato
*/
package config.java.invoicing;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.common.core.SupplierLocation;
import ariba.invoicing.AribaInvoiceCreator;
import ariba.invoicing.core.InvalidInvoiceException;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.Log;
import ariba.purchasing.core.PurchaseOrder;
import ariba.statement.core.StatementOrderInfo;
import ariba.tax.core.TaxID;
import ariba.util.core.StringUtil;

/**
    Extends AribaInvoiceCreator to do the following:

    - Override processPaymentTerms to ensure that payment terms are not
    created for ASN invoices.

    - Override processInvoicePartnerData to null out SupplierTaxID for
    non ezopen invoices.

    see ariba.invoicing.AribaInvoiceCreator
*/
public class CATInvoiceCreator extends AribaInvoiceCreator
{


    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Class Name
    */
    public static final String ClassName = CATInvoiceCreator.class.getName();

    /**
        Short class name - Used for logging purposes
    */
    public static final String cn = "CATInvoiceCreator";

    /**
        EZ Open Partition Name
    */
    public static final String EZOpen = "ezopen";


    /*-----------------------------------------------------------------------
        Overridden Methods
      -----------------------------------------------------------------------*/

    /**
        Not creating Payment Terms for AN Loaded Invoices

        @see ariba.invoicing.AribaInvoiceCreator
    */
    public void processPaymentTerms ()
    {
        String mn = cn + ".processPaymentTerms(): ";
        Log.customer.debug(
                "%s Not Creating Payment Terms for AN Loaded Invoices.",
                mn);
        return;
    }

    /**
        Clear supplier tax id if the partition is non ezopen. This is to be
        in sync with 822 customization where the supplier tax id is set for
        ezopen invoices loaded via ASN. The trigger CXMLCopyInvoice was
        used in 822

        822 Customization - File CatEZOInvoiceEntryExt.aml
        --------------------------------------------------

        1. Use CXMLCopyInvoice trigger to populate supplier tax id from the
        invoice xml.

        9r Customization
        ----------------

        1. Update the parameter Application.Invoicing.TaxIDDomains to include
        'supplierTaxID'
        2. Override processInvoicePartnerData to null out 'SupplierTaxID' in
        the invoice it the partition is not ezopen
    */
    public void processInvoicePartnerData ()
    {
        String mn = cn + ".processInvoicePartnerData(): ";
        super.processInvoicePartnerData();

            // delete supplier tax id if it the partition is not ezopen
            // and if the domain is supplierTaxID
        if (invoice != null) {
            Partition partition = invoice.getPartition();
            if (partition != null) {

                String partitionName = partition.getName();
                Log.customer.debug(
                        "%s Partition Name: %s", mn, partitionName);

                TaxID supplierTaxID = invoice.getSupplierTaxID();
                if (supplierTaxID != null) {
                    if (!EZOpen.equals(partitionName)) {

                        String domain = supplierTaxID.getDomain();
                        if ("supplierTaxID".equals(domain)) {

                            Log.customer.debug(
                                    "%s The Supplier Tax ID has " +
                                    "domain: %s. Setting it to null",
                                    mn,
                                    domain);
                            invoice.setSupplierTaxID(null);
                        }
                    }

                        // ezopen invoices
                        // functionality migrated from trigger 'CXMLInvoicePostCreateTrigger'
                        // refer to this trigger in CatEZOInvoiceEntryExt.aml
                    else {

                        String domain = supplierTaxID.getDomain();
                        String id = supplierTaxID.getID();

                        Log.customer.debug(
                                "%s Setting Values - SupplierTaxID Domain: %s, " +
                                "VATRegistrationNumber: %s",
                                mn,
                                domain,
                                id);
                        if (!"VAT".equals(domain)) {
                            supplierTaxID.setDomain("VAT");
                        }
                        invoice.setFieldValue(
                                "VATRegistrationNumber",
                                id);
                    }
                }
            }
        }
    }

    /*
     * 	Changed by :	Arasan Rajendren
     *  Changed on :    07/21/2011
     *  Changes    :	Override CreateInvoice method to populate SupplierLocation object
     *                  before validating Invoice.
     *
     */

    public Invoice createInvoice () throws InvalidInvoiceException
	{
      try {

    	  if (supplierLocation == null)
    	  	setSupplierLocation();

    	  validate();

      } catch (InvalidInvoiceException e) {
            // delete this invoice.
          if (invoice != null) {
              invoice.delete();
          }
          throw e;
      }
      return invoice;
	}

	private void setSupplierLocation()
    {
        StatementOrderInfo orderInfo = invoice.getSupplierOrderInfo();
        if (orderInfo != null) {
            String payloadId = orderInfo.getOrderPayloadID();
            String orderNumber = orderInfo.getOrderNumber();
            if (!StringUtil.nullOrEmptyOrBlankString(payloadId) && !StringUtil.nullOrEmptyOrBlankString(orderNumber)) {
                AQLQuery query = new AQLQuery(PurchaseOrder.ClassName);
                query.andEqual(PurchaseOrder.KeyUniqueName, orderNumber);
                query.andEqual(PurchaseOrder.KeyPayloadID, payloadId);
                query.addSelectElement("SupplierLocation");
                AQLOptions options = new AQLOptions(partition);
                AQLResultCollection result = null;
                try {
                    result = Base.getService().executeQuery(query, options);
                    if (result.next()) {
                    	supplierLocation = (SupplierLocation) result.getBaseId(1).get();
                    }
                }
                finally {
                    if (result != null) {
                        result.close();
                    }
                }
            }
        }
    }
}