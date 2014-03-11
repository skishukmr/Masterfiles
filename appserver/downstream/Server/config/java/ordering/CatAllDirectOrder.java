package config.java.ordering;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Comment;
import ariba.base.core.LongString;
import ariba.base.fields.Fields;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.pcard.ordering.PCardOrderMethodUtil;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.purchasing.ordering.AllDirectOrder;
import ariba.util.core.ResourceService;

public class CatAllDirectOrder extends AllDirectOrder
{

    public int canProcessLineItem(ReqLineItem lineItem)
        throws OrderMethodException
    {
        return 1;
    }

    public boolean canAggregateLineItems(ReqLineItem li1, POLineItem li2)
        throws OrderMethodException
    {
        Log.customer.debug("CatAllDirectOrder: In canAggregateLineItems Method");
        if(li1.getShipTo() != li2.getShipTo())
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Ship Tos.");
            return false;
        }
        if(li1.getAmount().getCurrency() != li2.getAmount().getCurrency())
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Currencies.");
            return false;
        }
        if(li1.getFieldValue("SettlementCode") != li2.getFieldValue("SettlementCode"))
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Settlement Codes.");
            return false;
        }
        if(li1.getFieldValue("TaxCode") != li2.getFieldValue("TaxCode"))
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Tax Codes.");
            return false;
        }
        if(!PCardOrderMethodUtil.lineItemsHaveSameAccountings(li1, li2) && !PCardOrderMethodUtil.lineItemsCorrespond(li1, li2))
        {
            SplitAccountingCollection sac = li1.getAccountings();
            if(sac != null)
            {
                String message;
                for(Iterator saci = sac.getAllSplitAccountingsIterator(); saci.hasNext(); Log.customer.debug("CatAllDirectOrder: ReqLineItem ValidateAccountingMessage = " + message))
                {
                    SplitAccounting sa = (SplitAccounting)saci.next();
                    message = (String)sa.getFieldValue("ValidateAccountingMessage");
                }

            }
            SplitAccountingCollection sac2 = li2.getAccountings();
            if(sac2 != null)
            {
                String message2;
                for(Iterator saci2 = sac2.getAllSplitAccountingsIterator(); saci2.hasNext(); Log.customer.debug("CatAllDirectOrder: POLineItem ValidateAccountingMessage = " + message2))
                {
                    SplitAccounting sa2 = (SplitAccounting)saci2.next();
                    message2 = (String)sa2.getFieldValue("ValidateAccountingMessage");
                }

            }
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different accountings", li1, li2);
            return false;
        }
        String rliPOPref = (String)li1.getFieldValue("LognetPOPrefix");
        String pliPOPref = (String)li2.getFieldValue("LognetPOPrefix");
        if(rliPOPref != null && !rliPOPref.equalsIgnoreCase(pliPOPref))
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Lognet PO Prefixes.");
            return false;
        }
        if (rliPOPref == null && pliPOPref != null)
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Lognet PO Prefixes.");
            return false;
        }
        if(li1.getFieldValue("BuyerCode") != li2.getFieldValue("BuyerCode"))
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Buyer Codes.");
            return false;
        }
        String rliTQ = (String)li1.getFieldValue("TaxQualifier");
        String pliTQ = (String)li2.getFieldValue("TaxQualifier");
        if(!rliTQ.equalsIgnoreCase(pliTQ))
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Tax Qualifiers.");
            return false;
        }
        String rliDC = (String)li1.getFieldValue("DockCode");
        String pliDC = (String)li2.getFieldValue("DockCode");
        if(rliDC != null && !rliDC.equalsIgnoreCase(pliDC))
        {
             Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Dock Codes.");
             return false;
        }
        if (rliDC == null && pliDC != null)
        {
            Log.customer.debug("CatAllDirectOrder: Cannot aggregate because line items have different Dock Codes.");
            return false;
        } else
        {
            Log.customer.debug("CatAllDirectOrder: Super's result = " + super.canAggregateLineItems(li1, li2));
            return super.canAggregateLineItems(li1, li2);
        }
    }

    public List endProcessingRequisition(Requisition req)
        throws OrderMethodException
    {
        Log.customer.debug("CatAllDirectOrder: endProcessingRequisition");
        Log.customer.debug("CatAllDirectOrder: Requisition = " + req);
        Object emergencyBuy = req.getFieldValue("EmergencyBuy");
        Log.customer.debug("CatAllDirectOrder: Emergency Buy = " + emergencyBuy);
        Log.customer.debug("CatAllDirectOrder: Requisition Size = " + req.getLineItems().size());
        for(int i = 0; i < req.getLineItems().size(); i++)
        {
            ReqLineItem rli = (ReqLineItem)req.getLineItems().get(i);
            Log.customer.debug("CatAllDirectOrder: ReqLineItem = " + rli);
            Object msdsNumber = rli.getFieldValue("MSDSNumber");
            Log.customer.debug("CatAllDirectOrder: MSDSNumber = " + msdsNumber);
            PurchaseOrder po = rli.getOrder();
            if(po != null)
            {
                po.setFieldValue("SettlementCode", rli.getFieldValue("SettlementCode"));
                po.setFieldValue("TaxCode", rli.getFieldValue("TaxCode"));
                po.setFieldValue("LognetPOPrefix", rli.getFieldValue("LognetPOPrefix"));
                po.setFieldValue("DockCode", rli.getFieldValue("DockCode"));
                po.setFieldValue("TaxQualifier", rli.getFieldValue("TaxQualifier"));
                po.setFieldValue("BuyerCode", rli.getFieldValue("BuyerCode"));
                po.setFieldValue("EmergencyBuy", emergencyBuy);
                if(msdsNumber != null)
                {
                    Log.customer.debug("CatAllDirectOrder: Creating new comment");
                    Comment newComment = new Comment(po.getPartition());
                    newComment.setType(1);
                    newComment.setDate(Fields.getService().getNow());
                    newComment.setUser(po.getRequester());
                    newComment.setText(new LongString(ResourceService.getService().getLocalizedString("aml.cat.ui", "MessageHazMatNoteCode", po.getRequester().getLocale())));
                    newComment.setTitle(ResourceService.getService().getLocalizedString("aml.cat.ui", "MessageHazMatNoteCodeTitle", po.getRequester().getLocale()));
                    newComment.setExternalComment(true);
                    newComment.setParent(po);
                    newComment.setCommentName(ResourceService.getService().getLocalizedString("aml.cat.ui", "MessageHazMatNoteCodeTitle", po.getRequester().getLocale()));
                    Log.customer.debug("CatAllDirectOrder: Adding new comment to Order");
                    po.getComments().add(newComment);
                }
            }
        }

        return super.endProcessingRequisition(req);
    }

    public CatAllDirectOrder()
    {
    }

    private static final String classname = "CatAllDirectOrder";
}
