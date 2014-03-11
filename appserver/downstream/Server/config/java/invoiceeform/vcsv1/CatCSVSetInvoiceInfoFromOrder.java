// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:30 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoiceeform.vcsv1;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

public class CatCSVSetInvoiceInfoFromOrder extends Action
{

    public CatCSVSetInvoiceInfoFromOrder()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
    {
        Approvable approvable = (Approvable)valuesource;
        List list = (List)approvable.getFieldValue("Orders");
        if(ListUtil.nullOrEmptyList(list))
            return;
        List list1 = (List)approvable.getFieldValue("LineItems");
        int i = ListUtil.getListSize(list1) + 1;
        int j = list.size();
        for(int k = 0; k < j; k++)
        {
            BaseId baseid = (BaseId)list.get(k);
            PurchaseOrder purchaseorder = (PurchaseOrder)Base.getSession().objectFromId(baseid);
            ariba.base.core.BaseVector basevector = purchaseorder.getLineItems();
            if(approvable.getFieldValue("Supplier") == null)
                approvable.setFieldValue("Supplier", purchaseorder.getSupplier());
            if(approvable.getFieldValue("SupplierLocation") == null)
                approvable.setFieldValue("SupplierLocation", purchaseorder.getSupplierLocation());
            int l = ListUtil.getListSize(basevector);
            for(int i1 = 0; i1 < l; i1++)
            {
                POLineItem polineitem = (POLineItem)basevector.get(i1);
                BaseObject baseobject = (BaseObject)BaseObject.create("config.java.invoiceeform.InvoiceEformLineItem", approvable.getPartition());
                list1.add(baseobject);
                ProcureLineType procurelinetype = ProcureLineType.lookupByLineItem(polineitem);
                baseobject.setFieldValue("LineType", procurelinetype);
                baseobject.setFieldValue("InvoiceLineNumber", Constants.getInteger(i));
                i++;
                baseobject.setFieldValue("Order", purchaseorder);
                baseobject.setFieldValue("OrderNumber", purchaseorder.getOrderID());
                baseobject.setFieldValue("OrderLineItem", polineitem);
                baseobject.setFieldValue("OrderLineNumber", Constants.getInteger(polineitem.getExternalLineNumber()));
                LineItemProductDescription lineitemproductdescription = polineitem.getDescription();
                Object obj = lineitemproductdescription.getDottedFieldValue("CAPSChargeCode");
                baseobject.setFieldValue("CapsChargeCode", obj);
                baseobject.setFieldValue("ReferenceLineNumber", (Integer)polineitem.getDottedFieldValue("ReferenceLineNumber"));
                baseobject.setFieldValue("Price", lineitemproductdescription.getPrice());
                baseobject.setFieldValue("UnitOfMeasure", lineitemproductdescription.getUnitOfMeasure());
                baseobject.setFieldValue("Description", lineitemproductdescription.getDescription());
                baseobject.setFieldValue("SupplierPartNumber", lineitemproductdescription.getSupplierPartNumber());
                baseobject.setFieldValue("Quantity", polineitem.getQuantity());
                baseobject.setFieldValue("Amount", polineitem.getAmount());
            }

        }

        list.clear();
        approvable.save();
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    private static final ValueInfo valueInfo = new ValueInfo(0, "ariba.approvable.core.Approvable");

}