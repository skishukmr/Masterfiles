package config.java.receiving;

import ariba.receiving.core.ReceivableLineItem;
import ariba.receiving.core.ReceivingTypeMethod;

public class CatNoReceiptTypeMethod extends ReceivingTypeMethod
{
    public int findReceivingType(ReceivableLineItem lineItem)
    {
        return ReceivingTypeMethod.noReceipt;
    }

    public CatNoReceiptTypeMethod()
    {
    }
}