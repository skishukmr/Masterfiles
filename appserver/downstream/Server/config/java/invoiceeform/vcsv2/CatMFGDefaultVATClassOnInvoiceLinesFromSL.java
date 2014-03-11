/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default the VATClass on all invoice lines from Supplier Location.
*/

package config.java.invoiceeform.vcsv2;

import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

public class CatMFGDefaultVATClassOnInvoiceLinesFromSL extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
        ClusterRoot invoice = ((BaseObject)object).getClusterRoot();

        ariba.base.core.Log.customer.debug("Invoice = " + invoice.getUniqueName());

        if (invoice != null) {

			ClusterRoot vatClass = (ClusterRoot) invoice.getDottedFieldValue("SupplierLocation.VATClass");
			BaseObject invoiceLineItem = null;

            List lineItems = (List)invoice.getFieldValue("LineItems");

            int size = ListUtil.getListSize(lineItems);
            for (int i = 0; i < size; i++) {
				invoiceLineItem = (BaseObject) lineItems.get(i);
				CatMFGSetVATClassOnInvoiceLineFromSL var = new CatMFGSetVATClassOnInvoiceLineFromSL();
				var.fire(invoiceLineItem,null);
            }
        }
    }
}
