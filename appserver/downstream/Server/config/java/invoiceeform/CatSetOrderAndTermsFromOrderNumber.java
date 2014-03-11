/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to set the Order based on the order number entered on the invoice
         eform line item. This class also defaults the payment terms on the eform header from the order.
*/

package config.java.invoiceeform;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.Behavior;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetOrderAndTermsFromOrderNumber extends Action
{

    public void fire (ValueSource object, PropertyTable params)
    {
        Log.customer.debug("%s - Object = %s, class name = %s", this.getClass().getName(), object, object.getTypeName());

        if(!object.getTypeName().equals("config.java.invoiceeform.InvoiceEformLineItem")) {
			Log.customer.debug("Not config.java.invoiceeform.InvoiceEformLineItem. Returning...");
            return;
        }

        BaseObject invoiceEformLineItem = (BaseObject)object;
        ClusterRoot invoiceEform = invoiceEformLineItem.getClusterRoot();

        String orderId = (String) invoiceEformLineItem.getDottedFieldValue("OrderNumber");

        Log.customer.debug("SetOrderFromOrderNumber - ORDERID: %s, ", orderId);

        if (orderId != null) {
			orderId = (String)orderId.toUpperCase();
            PurchaseOrder po = PurchaseOrder.lookupByUniqueName(orderId, invoiceEformLineItem.getPartition());
			if (po != null) {
                Log.customer.debug("SetOrderFromOrderNumber: %s, UniqueName: %s", po, po.getUniqueName());
                ClusterRoot order = (ClusterRoot) invoiceEformLineItem.getDottedFieldValue("Order");
				invoiceEformLineItem.setDottedFieldValue("Order",po);

				ClusterRoot paymentTerms = (ClusterRoot) invoiceEform.getDottedFieldValue("PaymentTerms");
				if (paymentTerms == null) {
					invoiceEform.setDottedFieldValue("PaymentTerms", po.getPaymentTerms(po.getPartition()));
				}

			} else {
				invoiceEformLineItem.setDottedFieldValue("Order",null);
			}
		} else {
			invoiceEformLineItem.setDottedFieldValue("Order",null);
		}

    }

    private static final ValueInfo valueInfo =
        new ValueInfo(Behavior.IsScalar, Approvable.ClassName);

    /**
        Returns the list of valid value types.

        @return the list of valid value types.
    */
    protected ValueInfo getValueInfo ()
    {
        return valueInfo;
    }

}
