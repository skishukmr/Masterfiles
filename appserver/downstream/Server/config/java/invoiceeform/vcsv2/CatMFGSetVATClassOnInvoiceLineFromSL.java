/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default the VATClass on the invoice line from Supplier Location.
*/

package config.java.invoiceeform.vcsv2;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;

public class CatMFGSetVATClassOnInvoiceLineFromSL extends Action
{
    public void fire (ValueSource object, PropertyTable params)
    {
		BaseObject invoiceLine = (BaseObject) object;
        ClusterRoot invoice = invoiceLine.getClusterRoot();

        if (invoice != null) {
			ariba.base.core.Log.customer.debug("Invoice = " + invoice.getUniqueName());

			ClusterRoot vatClass = (ClusterRoot) invoice.getDottedFieldValue("SupplierLocation.VATClass");

            Integer category = (Integer) invoiceLine.getDottedFieldValue("LineType.Category");

			if (category != null) {
				//if tax category, set VATClass to null else set from supplier location
				if (category.intValue() == 2) {
					invoiceLine.setDottedFieldValue("VATClass",null);
				} else {
					invoiceLine.setDottedFieldValue("VATClass",vatClass);
				}
			}
        }
    }
}
