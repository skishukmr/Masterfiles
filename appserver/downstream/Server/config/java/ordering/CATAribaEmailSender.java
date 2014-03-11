/*******************************************************************************************************************************************
	Creator: 	 James S Pagadala
	Description: To send email orders if the requestor belongs non-CPPI company code.

	ChangeLog:
	Date		Name		Description

*******************************************************************************************************************************************/

package config.java.ordering;

import java.io.IOException;
import java.io.InputStream;

import ariba.base.core.Partition;
import ariba.procure.core.ApprovableLockGroup;
import ariba.purchasing.core.OrderRecipient;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.ordering.AribaEmailSender;
import ariba.util.log.Log;


public class CATAribaEmailSender extends AribaEmailSender
{

    public void send(OrderRecipient recipient, PurchaseOrder po, InputStream formattedOrder, String fileExtension)
        throws IOException
    {

		Log.customer.debug("CATAribaEmailSender : send : ****START****");

        po = (PurchaseOrder)ApprovableLockGroup.lock(po.getBaseId());

        Partition part = po.getPartition();
        String partName = part.getName();
        Log.customer.debug("CATAribaEmailSender: send : partition name : " + partName);

		//if paving requesters, do not send order
		if(partName.equals("pcsv1")) {
			POLineItem poli = (POLineItem) po.getLineItem(1);
			Requisition req = (Requisition)poli.getRequisition();

			String requesterAccFac = (String)req.getRequester().getFieldValue("AccountingFacility");
        	Log.customer.debug("CATAribaEmailSender: send : requesterAccFac : " + requesterAccFac);

	    	//If requester is from facility R8, do not send order,
	    	// update status as success and exit method
	    	if(requesterAccFac != null && requesterAccFac.equals("R8")) {
        		Log.customer.debug("CATAribaEmailSender: send : CPPI User" );
        		po = (PurchaseOrder)ApprovableLockGroup.lock(po.getBaseId());
        		super.success(po, formattedOrder, fileExtension, "text/html", recipient);
        		return;
			}
		}//end paving

       	Log.customer.debug("CATAribaEmailSender: send : Calling the out of box java for non CPPI users" );
		super.send(recipient, po, formattedOrder, fileExtension);

		Log.customer.debug("CATAribaEmailSender : send : ****END****");
    }


}