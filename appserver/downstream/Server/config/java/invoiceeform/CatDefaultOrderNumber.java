/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to default the order number on the current invoice eform line item
         from the previous line.
*/

package config.java.invoiceeform;

import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

public class CatDefaultOrderNumber extends Action
{

	private static final String ORDER_NUMBER = "OrderNumber";

    public void fire (ValueSource object, PropertyTable params)
    {
        ClusterRoot invoice = ((BaseObject)object).getClusterRoot();

        if (invoice != null) {
            List lineItems = (List)invoice.getFieldValue("LineItems");

            int size = ListUtil.getListSize(lineItems);

            if (size < 2) {
				return;
			}

			BaseObject lastLine = (BaseObject) lineItems.get(size - 1);
			BaseObject lineBeforeLastLine = (BaseObject) lineItems.get(size-2);

			String orderNumber = (String) lastLine.getDottedFieldValue(ORDER_NUMBER);

			if (orderNumber != null)  {
				return;
			}

			lastLine.setDottedFieldValue(ORDER_NUMBER, lineBeforeLastLine.getDottedFieldValue("OrderNumber"));

        }
    }
}
