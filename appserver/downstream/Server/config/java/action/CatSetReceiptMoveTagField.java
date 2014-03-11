package config.java.action;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.Log;
import ariba.base.fields.ValueSource;
import ariba.receiving.core.ReceiptCoreApprovable;
import ariba.receiving.core.ReceiptItem;
import ariba.util.core.PropertyTable;

public class CatSetReceiptMoveTagField extends Action
{

    public CatSetReceiptMoveTagField()
    {
    }

    public void fire(ValueSource object, PropertyTable params)
    {
        Log.customer.debug("CatSetReceiptMoveTagField firing.. ");
        if(object != null)
        {
            Log.customer.debug("CatSetReceiptMoveTagField valuesource: " + object);
            ReceiptItem receiptItem = (ReceiptItem)object;
            Log.customer.debug("CatSetReceiptMoveTagField: receiptItem = " + receiptItem);
            int thisReceiptItemNumber = receiptItem.getNumberInCollection();
            Log.customer.debug("%s *** The recipt item number passed to trigger =" + thisReceiptItemNumber, "CatSetReceiptMoveTagField: ");
            ReceiptCoreApprovable receipt = receiptItem.getReceipt();
            Log.customer.debug("CatSetReceiptMoveTagField: receipt = " + receipt);
            for(Iterator i = receipt.getReceiptItemsIterator(); i.hasNext();)
            {
                ReceiptItem receiptItemCol = (ReceiptItem)i.next();
                int receiptNumber = receiptItemCol.getNumberInCollection();
                Log.customer.debug("%s *** The recipt item number  =" + receiptNumber, "CatSetReceiptMoveTagField: ");
                if(receiptNumber != thisReceiptItemNumber)
                {
                    Log.customer.debug("%s *** about to set receipt " + receiptNumber + " MoveTag to false", "CatSetReceiptMoveTagField: ");
                    Log.customer.debug("%s *** setting receipt " + receiptNumber + " MoveTag to false", "CatSetReceiptMoveTagField: ");
                }
            }

        }
    }

    protected String[] getRequiredParameterNames()
    {
        return null;
    }

    private static final String classname = "CatSetReceiptMoveTagField: ";
}
