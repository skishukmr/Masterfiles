package config.java.action.sap;

import ariba.base.core.BaseVector;
import ariba.base.fields.*;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.*;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;
import config.java.condition.sap.CatSAPValidReferenceLineNumber;

public class CatSAPSetAdditionalChargeLineItemFields extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ProcureLineItem)
        {
            ProcureLineItem pli = (ProcureLineItem)object;
            Log.customer.debug("%s ProcureLineItem %s", "CatSAPSetAdditionalChargeLineItemFields", pli);
            if(CatSAPAdditionalChargeLineItem.isAdditionalCharge(pli) && (CatSAPValidReferenceLineNumber.validReferenceLine(pli) == 0 || CatSAPValidReferenceLineNumber.validReferenceLine(pli) == 5))
            {
                int refNum = ((Integer)pli.getFieldValue("ReferenceLineNumber")).intValue();
                Log.customer.debug("%s Inside SECOND IF BLOCK -- RefNum %s", "CatSAPSetAdditionalChargeLineItemFields", pli.getFieldValue("ReferenceLineNumber"));
                ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
                Log.customer.debug("%s ProcureLineItemCollection %s", "CatSAPSetAdditionalChargeLineItemFields", plic);
                if(plic != null)
                {
                    ProcureLineItem mpli = (ProcureLineItem)plic.getLineItem(refNum);
                    if(pli != null)
                    {
                        Log.customer.debug("%s *** SET AC LINE FIELDS NOW!", "CatSAPSetAdditionalChargeLineItemFields");
                        setAdditionalChargeFields(mpli, pli);
                        Log.customer.debug("%s *** DONE WITH AC LINE FIELDS!", "CatSAPSetAdditionalChargeLineItemFields");
                    }
                }
            }
        }
    }

    public static void setAdditionalChargeFields(ProcureLineItem matLine, ProcureLineItem acLine)
    {
        if(matLine != null && acLine != null)
        {
            acLine.setSupplier(matLine.getSupplier());
            acLine.setSupplierLocation(matLine.getSupplierLocation());
            acLine.setNeedBy(matLine.getNeedBy());
            acLine.setShipTo(matLine.getShipTo());
            acLine.setDottedFieldValueWithoutTriggering("PurchaseOrg", matLine.getFieldValue("PurchaseOrg"));
            acLine.setDeliverTo(matLine.getDeliverTo());
            acLine.setFieldValue("DeliverToPhone", matLine.getFieldValue("DeliverToPhone"));
            acLine.setFieldValue("MailDrop", matLine.getFieldValue("MailDrop"));
            acLine.setBillingAddress(matLine.getBillingAddress());
            acLine.setDottedFieldValueWithoutTriggering("ItemCategory", matLine.getFieldValue("ItemCategory"));
            acLine.setDottedFieldValueWithoutTriggering("Description.CommonCommodityCode", matLine.getDescription().getCommonCommodityCode());
            acLine.setDottedFieldValueWithoutTriggering("CommodityCode", matLine.getCommodityCode());
            acLine.setDottedFieldValueWithoutTriggering("CommodityExportMapEntry", matLine.getCommodityExportMapEntry());
            acLine.setFieldValue("LineItemType", matLine.getFieldValue("LineItemType"));
            acLine.setDottedFieldValueWithoutTriggering("TaxCode", matLine.getFieldValue("TaxCode"));
            acLine.setFieldValue("PaymentTerms", matLine.getFieldValue("PaymentTerms"));
            acLine.setDottedFieldValueWithoutTriggering("BuyerCode", matLine.getFieldValue("BuyerCode"));
            acLine.setDottedFieldValueWithoutTriggering("AccountCategory", matLine.getFieldValue("AccountCategory"));
            acLine.setDottedFieldValueWithoutTriggering("TradingPartner", matLine.getFieldValue("TradingPartner"));
            acLine.setDottedFieldValueWithoutTriggering("IncoTerms1", matLine.getFieldValue("IncoTerms1"));
            acLine.setDottedFieldValueWithoutTriggering("IncoTerms2", matLine.getFieldValue("IncoTerms2"));
            BaseVector acSAC = (BaseVector)acLine.getDottedFieldValue("Accountings.SplitAccountings");
            BaseVector matSAC = (BaseVector)matLine.getDottedFieldValue("Accountings.SplitAccountings");
            if(acSAC != null && !acSAC.isEmpty() && matSAC != null && !matSAC.isEmpty())
            {
                for(int count = acSAC.size() - 1; count > -1; count--)
                {
                    SplitAccounting matSplit0 = (SplitAccounting)matSAC.get(0);
                    SplitAccounting acSplit = (SplitAccounting)acSAC.get(count);
                    
                    if(matSplit0.getFieldValue("CostCenterText")!=null)
                    	{
                    		acSplit.setFieldValue("CostCenterText", matSplit0.getFieldValue("CostCenterText"));
                    	}
                    if(matSplit0.getFieldValue("GeneralLedgerText")!=null)
                	{
                		acSplit.setFieldValue("GeneralLedgerText", matSplit0.getFieldValue("GeneralLedgerText"));
                	}
                    if(matSplit0.getFieldValue("WBSElementText")!=null)
                	{
                		acSplit.setFieldValue("WBSElementText", matSplit0.getFieldValue("WBSElementText"));
                	}
                    if(matSplit0.getFieldValue("AssetText")!=null)
                	{
                		acSplit.setFieldValue("AssetText", matSplit0.getFieldValue("AssetText"));
                	}
                    if(matSplit0.getFieldValue("InternalOrderText")!=null)
                	{
                		acSplit.setFieldValue("InternalOrderText", matSplit0.getFieldValue("InternalOrderText"));
                	}

                    acSplit.setFieldValue("CostCenterApprover", null);
                    acSplit.setFieldValue("ValidateAccountingMessage", null);               
                
                }

            }
        }
    }

    public CatSAPSetAdditionalChargeLineItemFields()
    {
    }

}
