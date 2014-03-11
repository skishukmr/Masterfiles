package config.java.action;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;
import config.java.condition.vcsv1.CatValidReferenceLineNumber;

public class CatSetAdditionalChargeLineItemFields extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ProcureLineItem)
        {
            ProcureLineItem pli = (ProcureLineItem)object;
            if(CatAdditionalChargeLineItem.isAdditionalCharge(pli) && CatValidReferenceLineNumber.validReferenceLine(pli) == 0)
            {
                int refNum = ((Integer)pli.getFieldValue("ReferenceLineNumber")).intValue();
                ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
                if(plic != null)
                {
                    ProcureLineItem mpli = (ProcureLineItem)plic.getLineItem(refNum);
                    if(pli != null)
                    {
                        Log.customer.debug("%s *** SET AC LINE FIELDS NOW!", "CatSetAdditionalChargeLineItemFields");
                        setAdditionalChargeFields(mpli, pli);
                        Log.customer.debug("%s *** DONE WITH AC LINE FIELDS!", "CatSetAdditionalChargeLineItemFields");
                    }
                }
            }
        }
    }

    public static void setAdditionalChargeFields(ProcureLineItem matLine, ProcureLineItem acLine)
    {
        if(matLine != null && acLine != null)
        {
            acLine.setNeedBy(matLine.getNeedBy());
            acLine.setShipTo(matLine.getShipTo());
            acLine.setFieldValue("FOBPoint", matLine.getFieldValue("FOBPoint"));
            acLine.setDeliverTo(matLine.getDeliverTo());
            acLine.setFieldValue("DeliverToPhone", matLine.getFieldValue("DeliverToPhone"));
            acLine.setFieldValue("DeliverToMailStop", matLine.getFieldValue("DeliverToMailStop"));
            acLine.setBillingAddress(matLine.getBillingAddress());
            acLine.setFieldValue("SettlementCode", matLine.getFieldValue("SettlementCode"));
            acLine.setDottedFieldValueWithoutTriggering("Description.CommonCommodityCode", matLine.getDescription().getCommonCommodityCode());
            acLine.setDottedFieldValueWithoutTriggering("CommodityCode", matLine.getCommodityCode());
            acLine.setFieldValue("ProjectNumber", matLine.getFieldValue("ProjectNumber"));
            acLine.setFieldValue("TaxUse", matLine.getFieldValue("TaxUse"));
            acLine.setFieldValue("TaxQualifier", matLine.getFieldValue("TaxQualifier"));
            acLine.setDottedFieldValueWithoutTriggering("BuyerCode", matLine.getFieldValue("BuyerCode"));
            acLine.setDottedFieldValueWithoutTriggering("AccountType", matLine.getFieldValue("AccountType"));
            BaseVector acSAC = (BaseVector)acLine.getDottedFieldValue("Accountings.SplitAccountings");
            BaseVector matSAC = (BaseVector)matLine.getDottedFieldValue("Accountings.SplitAccountings");
            if(acSAC != null && !acSAC.isEmpty() && matSAC != null && !matSAC.isEmpty())
            {
                for(int count = acSAC.size() - 1; count > -1; count--)
                {
                    SplitAccounting matSplit0 = (SplitAccounting)matSAC.get(0);
                    SplitAccounting acSplit = (SplitAccounting)acSAC.get(count);
                    acSplit.setFieldValue("AccountingFacility", matSplit0.getFieldValue("AccountingFacility"));
                    acSplit.setFieldValue("Department", matSplit0.getFieldValue("Department"));
                    acSplit.setFieldValue("Division", matSplit0.getFieldValue("Division"));
                    acSplit.setFieldValue("Section", matSplit0.getFieldValue("Section"));
                    acSplit.setFieldValue("ExpenseAccount", matSplit0.getFieldValue("ExpenseAccount"));
                    acSplit.setFieldValue("Order", matSplit0.getFieldValue("Order"));
                    acSplit.setFieldValue("Misc", matSplit0.getFieldValue("Misc"));
                    acSplit.setFieldValue("DepartmentApprover", null);
                    acSplit.setFieldValue("ValidateAccountingMessage", null);
                }

            }
        }
    }

    public CatSetAdditionalChargeLineItemFields()
    {
    }

    private static final String THISCLASS = "CatSetAdditionalChargeLineItemFields";
}
