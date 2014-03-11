/****************************************************************************/
/*						Change History

 *Change#		Change By		Change Date    Description
 *================================================================================================================================
 * Issue 961 	Vikram J Singh  25-06-2009     Issue Description: Adding Close PO functionality by Close Order eForm.
											   Enabling Close Order Date for Orders.
											   IR Header Level Exceptions - ClosePOVariance and CancelPOVariance.
											   Indirect Buyer, Requisitioner to handle ClosePOVariance, CancelPOVariance resp.

/**********************************************************************************************************************************/

package config.java.invoicing.vcsv2;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.basic.core.Money;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.Log;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.Role;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

public final class CatMFGIRApprovalRulesUtil
{

    public CatMFGIRApprovalRulesUtil()
    {
    }

    public static ClusterRoot getExceptionHandler(InvoiceException exc)
    {
        ClusterRoot approver = null;
        InvoiceExceptionType excType = (InvoiceExceptionType)exc.getType();
        String excTypeName = excType.getUniqueName();
        boolean reqAllowed = excType.getRequesterAllowed();
        BaseObject parent = exc.getParent();
        Log.customer.debug("%s:", "config.java.invoicing.CatIRApprovalRulesUtil");
        Log.customer.debug("%s: ExceptionType passed is %s", "config.java.invoicing.CatIRApprovalRulesUtil", excTypeName);
        Log.customer.debug("%s: Parent is %s", "config.java.invoicing.CatIRApprovalRulesUtil", parent);
        Log.customer.debug("config.java.invoicing.CatIRApprovalRulesUtil: RequesterAllowed is " + reqAllowed);
        if(reqAllowed)
        {
            if(parent instanceof InvoiceReconciliation)
                approver = getRequester((InvoiceReconciliation)parent);
            else
                approver = getLineRequester((InvoiceReconciliationLineItem)parent);
        } else
        {
            approver = getNonRequesterApprover(exc);
        }
        Log.customer.debug("%s: Returning %s", "config.java.invoicing.CatIRApprovalRulesUtil", approver.getUniqueName());
        return approver;
    }

    public static ClusterRoot getExceptionHandler(BaseObject bo)
    {
        InvoiceException exc = null;
        try
        {
            exc = (InvoiceException)bo;
        }
        catch(ClassCastException cce)
        {
            Log.customer.debug("Invalid parameter passed ...");
            return null;
        }
        return getExceptionHandler(exc);
    }

    public static ClusterRoot getRequester(InvoiceReconciliation ir)
    {
        Log.customer.debug("%s: getRequester", "config.java.invoicing.CatIRApprovalRulesUtil");
        List orders = ir.getOrders();
        if(orders.size() > 0)
            if(orders.size() > 1)
            {
                return getAP(ir);
            } else
            {
                PurchaseOrder po = (PurchaseOrder)orders.get(0);
                POLineItem poli = (POLineItem)po.getLineItems().get(0);
                return poli.getRequisition().getRequester();
            }
        List mas = ir.getMasterAgreements();
        if(mas.size() > 0)
        {
            if(mas.size() > 1)
            {
                return getAP(ir);
            } else
            {
                Contract ma = (Contract)mas.get(0);
                return ma.getRequester();
            }
        } else
        {
            return getAP(ir);
        }
    }

    public static ClusterRoot getLineRequester(InvoiceReconciliationLineItem irli)
    {
        Log.customer.debug("%s: getLineRequester", "config.java.invoicing.CatIRApprovalRulesUtil");
        POLineItem poli = irli.getOrderLineItem();
        if(poli != null)
            return poli.getRequisition().getRequester();
        ContractLineItem mali = irli.getMALineItem();
        if(mali != null)
        {
            return mali.getLineItemCollection().getRequester();
        } else
        {
            InvoiceReconciliation ir = (InvoiceReconciliation)irli.getLineItemCollection();
            return getRequester(ir);
        }
    }

	// If exception is ClosePOVariance, then add Indirect Buyer to handle the exception -- Issue 961
    public static ClusterRoot getNonRequesterApprover(InvoiceException exc)
    {
        Log.customer.debug("%s: getNonRequesterApprover", "config.java.invoicing.CatIRApprovalRulesUtil");
        String excTypeName = exc.getType().getUniqueName();
        if(excTypeName.equals("POCatalogPriceVariance") || excTypeName.equals("ClosePOVariance"))
            return getBuyer(exc);
        else
            return getAP(exc);
    }

    public static ClusterRoot getBuyer(InvoiceReconciliation ir)
    {
        Log.customer.debug("%s: getBuyer(ir)", "config.java.invoicing.CatIRApprovalRulesUtil");
        List orders = ir.getOrders();
        if(orders.size() > 0)
        {
            PurchaseOrder po = (PurchaseOrder)orders.get(0);
            POLineItem poli = (POLineItem)po.getLineItems().get(0);
            String facility = (String)poli.getRequisition().getRequester().getFieldValue("AccountingFacility");
            if(facility != null)
            {
                facility = facility.trim().toUpperCase();
                if(facility.equals("DX"))
                    return Role.getRole("Indirect Buyer (mfg1)");
                if(facility.equals("NA"))
                    return Role.getRole("Indirect Buyer (mfg1)");
            }
        }
        return Role.getRole("Indirect Buyer");
    }

    public static ClusterRoot getBuyer(InvoiceException exc)
    {
        Log.customer.debug("%s: getBuyer(excType)", "config.java.invoicing.CatIRApprovalRulesUtil");
        BaseObject parent = exc.getParent();
        InvoiceReconciliation ir = null;
        if(parent instanceof InvoiceReconciliation)
        {
            ir = (InvoiceReconciliation)parent;
        } else
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)parent;
            ir = (InvoiceReconciliation)irli.getLineItemCollection();
        }
        return getBuyer(ir);
    }

    public static ClusterRoot getAP(InvoiceReconciliation ir)
    {
        Log.customer.debug("%s: getAP(ir)", "config.java.invoicing.CatIRApprovalRulesUtil");
        List orders = ir.getOrders();
        if(orders.size() > 0)
        {
            PurchaseOrder po = (PurchaseOrder)orders.get(0);
            POLineItem poli = (POLineItem)po.getLineItems().get(0);
            String facility = (String)poli.getRequisition().getRequester().getFieldValue("AccountingFacility");
            if(facility != null)
            {
                facility = facility.trim().toUpperCase();
                if(facility.equals("DX"))
                    return Role.getRole("Accounts Payable (mfg1-DX)");
                if(facility.equals("NA"))
                    return Role.getRole("Accounts Payable (mfg1-NA)");
            }
        }
        return Role.getRole("Accounts Payable");
    }

    public static ClusterRoot getAP(InvoiceException exc)
    {
        Log.customer.debug("%s: getAP(excType)", "config.java.invoicing.CatIRApprovalRulesUtil");
        BaseObject parent = exc.getParent();
        InvoiceReconciliation ir = null;
        if(parent instanceof InvoiceReconciliation)
        {
            ir = (InvoiceReconciliation)parent;
        } else
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)parent;
            ir = (InvoiceReconciliation)irli.getLineItemCollection();
        }
        return getAP(ir);
    }

    public static boolean isTaxMismatch(InvoiceReconciliation ir)
    {
        boolean taxMismatch = false;
        Money totalTaxAmount = ir.getTotalTax();
        Money computedTaxAmount = new Money(Constants.ZeroBigDecimal, totalTaxAmount.getCurrency());
        List lineItems = ir.getLineItems();
        int size = ListUtil.getListSize(lineItems);
        boolean nonTaxLineFound = false;
        for(int i = 0; i < size; i++)
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lineItems.get(i);
            if(irli.getLineType().getCategory() != 2)
            {
                nonTaxLineFound = true;
                Money taxAmount = irli.getTaxAmount();
                if(taxAmount != null)
                {
                	taxAmount.setAmount(taxAmount.getAmount().setScale(2, 4));
                    computedTaxAmount = Money.add(computedTaxAmount, taxAmount);
				}
            }
        }

        if(nonTaxLineFound && totalTaxAmount.approxCompareTo(computedTaxAmount) != 0)
            taxMismatch = true;
        return taxMismatch;
    }

    public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir, boolean getCumulativeTax)
    {
        if(ir.getConsolidated())
            return null;
        BaseObject bo = ir.getOrder();
        if(bo == null)
            bo = ir.getMasterAgreement();
        if(bo == null)
            return null;
        else
            return getCumulativeAmount(ir, bo, getCumulativeTax, true);
    }

    public static BigDecimal getCumulativeAmount(InvoiceReconciliation ir, BaseObject bo, boolean getCumulativeTax, boolean inBaseCCY)
    {
        AQLQuery query = null;
        String status = "Reject%";
        String fieldName = null;
        if(bo instanceof PurchaseOrder)
            fieldName = "LineItems.\"Order\"";
        else
        if(bo instanceof Contract)
            fieldName = "LineItems.MasterAgreement";
        else
        if(bo instanceof POLineItem)
            fieldName = "LineItems.OrderLineItem";
        else
        if(bo instanceof ContractLineItem)
            fieldName = "LineItems.MALineItem";
        else
            return null;
        String baseId = bo.getBaseId().toDBString();
        String irBaseId = ir.getBaseId().toDBString();
        if(getCumulativeTax)
        {
            if(inBaseCCY)
                query = AQLQuery.parseQuery(Fmt.S("SELECT SUM(LineItems.TaxAmount.ApproxAmountInBaseCurrency) FROM ariba.invoicing.core.InvoiceReconciliation t1 WHERE StatusString not like '%s' AND t1 <> baseid('%s')AND %s = baseid('%s')", status, irBaseId, fieldName, baseId));
            else
                query = AQLQuery.parseQuery(Fmt.S("SELECT SUM(LineItems.TaxAmount.Amount) FROM ariba.invoicing.core.InvoiceReconciliation t1 WHERE StatusString not like '%s' AND t1 <> baseid('%s')AND %s = baseid('%s')", status, irBaseId, fieldName, baseId));
        } else
        if(inBaseCCY)
            query = AQLQuery.parseQuery(Fmt.S("SELECT SUM(LineItems.Amount.ApproxAmountInBaseCurrency) FROM ariba.invoicing.core.InvoiceReconciliation t1 WHERE StatusString not like '%s' AND t1 <> baseid('%s')AND %s = baseid('%s')", status, irBaseId, fieldName, baseId));
        else
            query = AQLQuery.parseQuery(Fmt.S("SELECT SUM(LineItems.Amount.Amount) FROM ariba.invoicing.core.InvoiceReconciliation t1 WHERE StatusString not like '%s' AND t1 <> baseid('%s')AND %s = baseid('%s')", status, irBaseId, fieldName, baseId));
        AQLOptions options = new AQLOptions(bo.getPartition());
        AQLResultCollection results = Base.getService().executeQuery(query, options);
        BigDecimal totalAmount;
        for(totalAmount = null; results.next();)
        {
            totalAmount = results.getBigDecimal(0);
            break;
        }

        if(ir.isRejecting() || ir.isRejected())
            return totalAmount;
        if(totalAmount == null)
            totalAmount = new BigDecimal(0.0D);
        if(getCumulativeTax)
        {
            Money taxAmount = ir.getInvoice().getTotalTax();
            if(inBaseCCY)
                totalAmount = totalAmount.add(taxAmount.getApproxAmountInBaseCurrency());
            else
                totalAmount = totalAmount.add(taxAmount.getAmount());
        } else
        {
            Money invoiceAmount = ir.getInvoice().getTotalInvoiced();
            if(inBaseCCY)
                totalAmount = totalAmount.add(invoiceAmount.getApproxAmountInBaseCurrency());
            else
                totalAmount = totalAmount.add(invoiceAmount.getAmount());
        }
        return totalAmount;
    }

    public static final String ClassName = "config.java.invoicing.CatIRApprovalRulesUtil";
    public static final String BUYER_ROLE = "Indirect Buyer";
    public static final String BUYER_ROLE_DX = "Indirect Buyer (mfg1)";
    public static final String BUYER_ROLE_NA = "Indirect Buyer (mfg1)";
    public static final String AP_ROLE = "Accounts Payable";
    public static final String AP_ROLE_DX = "Accounts Payable (mfg1-DX)";
    public static final String AP_ROLE_NA = "Accounts Payable (mfg1-NA)";
}