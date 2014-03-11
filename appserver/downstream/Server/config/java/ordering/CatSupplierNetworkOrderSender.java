/***
*  CatSupplierNetworkOrderSender.java
*  Overides the Sender method. For Orders from Paving facility, the order should not be
*  sent out to suppliers.
*
*
*/
// Source File Name:   SupplierNetworkOrderSender.java

package config.java.ordering;

import java.io.IOException;
import java.io.InputStream;

import ariba.procure.core.ApprovableLockGroup;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.OrderRecipient;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.ordering.SupplierNetworkOrderSender;


public class CatSupplierNetworkOrderSender extends SupplierNetworkOrderSender {

    public void send(OrderRecipient recipient, PurchaseOrder po, InputStream formattedOrder, String fileExtension)
        throws IOException {

		if(po.getPartition().getName().equals("pcsv1")) {
			POLineItem poli = (POLineItem) po.getLineItem(1);
			Log.customer.debug("%s send() poli  =" + poli, classname);
			Requisition req = (Requisition)poli.getRequisition();
			Log.customer.debug("%s send() req on poli  =" + req, classname);

			String accountingFac = null;

			if(req !=null) {
				accountingFac = (String)req.getRequester().getFieldValue("AccountingFacility");
				Log.customer.debug("%s send() po requesterAccFac =" + accountingFac, classname);
			} else {
				Log.customer.debug("%s send() po accountingFac IS NULL" , classname);
			}

			//If requester is from facility R8, do not send order,
			// update status as success and exit method
			if(accountingFac  != null && accountingFac.equals("R8")) {
				po = (PurchaseOrder)ApprovableLockGroup.lock(po.getBaseId());
				String orderMethod = recipient.getOrderingMethod();
				Log.customer.debug("%s send() orderMethod=%s" , classname,orderMethod);
				recipient.setOrderingMethod("Silent");
				Log.customer.debug("%s send() orderMethod=%s" , classname,orderMethod);
				success(po, formattedOrder, fileExtension, "text/html", recipient);
				return;
			}
		}
		super.send(recipient, po, formattedOrder, fileExtension);

    }

	private static String classname = "CatSupplierNetworkOrderSender: ";
    public CatSupplierNetworkOrderSender() { }

}
