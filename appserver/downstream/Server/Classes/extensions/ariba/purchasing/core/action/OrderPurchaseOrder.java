/*
    Copyright (c) 1996-2004 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/customer/Caterpillar/Downstream/core_java/ariba/purchasing/core/action/OrderPurchaseOrder.java#2 $

    Responsible: avidyasagar
*/

package ariba.purchasing.core.action;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.purchasing.core.OrderRecipient;
import ariba.purchasing.core.PurchaseOrder;
import ariba.server.workflowserver.WorkflowService;
import ariba.util.core.PropertyTable;
import java.util.List;

/**
    Called to kick off the order recipients by starting their workflow.
    This should someday be moved to object creation

    OrderPurchaseOrder tries to send the order out by activating the order's
    OrderRecipients.

    @aribaapi private
*/
public class OrderPurchaseOrder extends Action
{
	// ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
	private static final String THISCLASS = "OrderPurchaseOrder";
    private static final String FaxSender = "System.Procure.SendMethods.Fax.Sender";
    private static final String FaxFormatter = "System.Procure.SendMethods.Fax.Formatter";
    // CAT - End of Core Code Hack

    public void fire (ValueSource object,
                      PropertyTable parameters)
      throws ActionExecutionException
    {

        PurchaseOrder order = (PurchaseOrder)object;
        List orderRecipients = order.getRecipients();
        if (orderRecipients.isEmpty()) {
            Log.fixme.warning(1244, order);
        }

        BaseSession session = Base.getSession();
        int numRecipients = orderRecipients.size();
        for (int i = 0; i < numRecipients; i++) {
            BaseId recipientId = (BaseId)orderRecipients.get(i);
            OrderRecipient recipient = (OrderRecipient)
                session.objectFromId(recipientId);

                //For InternalOrder, the OrderRecipient could be inactive.
            if (recipient.getActive()) {
            	// ARajendren Ariba, Inc.,
                // 9R1 Upgrade, Added CAT core code customizations.
                // CAT - Core Code Hack Starts
            	String sender = recipient.getSender();
                String formatter = recipient.getFormatter();
                Log.customer.debug("%s *** Sender/Formatter: %s, %s", "OrderPurchaseOrder", sender, formatter);
                PurchaseOrder order0 = (PurchaseOrder)order.getPreviousVersion();
                if(order0 != null && sender.indexOf("Network") > -1)
                {
                    String sender0 = (String)order0.getFieldValue("SendMethod");
                    Log.customer.debug("%s *** Prev Version SENDER: %s", "OrderPurchaseOrder", sender0);
                    if(sender0 != null && sender0.indexOf("Fax") > -1)
                    {
                        Log.customer.debug("%s *** Change bet. Versions, change Recipient to FAX!", "OrderPurchaseOrder");
                        String param1 = Base.getService().getParameter(null, "System.Procure.SendMethods.Fax.Formatter");
                        String param2 = Base.getService().getParameter(null, "System.Procure.SendMethods.Fax.Sender");
                        Log.customer.debug("%s *** Params 1/2: %s, %s!", "OrderPurchaseOrder", param1, param2);
                        if(param1 != null && param2 != null)
                        {
                            recipient.setFormatter(param1);
                            recipient.setSender(param2);
                            Log.customer.debug("%s *** SWITCHED TO FAX! Sender: %s, Formatter: %s", "OrderPurchaseOrder", recipient.getSender(), recipient.getFormatter());
                        }
                    }
                }
                order.setFieldValue("SendMethod", recipient.getSender());
                // CAT - End of Core Code Hack
                WorkflowService.getService().startWorkflow(recipient);
            }
        }
    }

    protected ValueInfo getValueInfo ()
    {
        return new ValueInfo(IsScalar,
                                      ariba.purchasing.core.PurchaseOrder.ClassName);
    }
}
