/*
 * Changed by Ashwini on 28-04-09
 * --------------------------------------------------------------
 *Issue 869 : Remove the this validation for ASN invoices which fires during IR approval
 *Issue 163 : UAT Defect Ariba 9r Upgrade
 *            S. Sato - Added fix so that system looks at 'Accepted' state as well along with
 *            'Cleared' before proceeding further in the IR workflow.
 *
 */
package config.java.invoicing.vcsv2;

import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.Contract;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import config.java.action.vcsv2.CatValidateMFGAccounting;

// Referenced classes of package config.java.invoicing.vcsv2:
//            CatMFGDefaultAccountingOnAdditionalCharges

public class CatMFGInvoiceReconciliationSubmitHook
    implements ApprovableHook
{

    public CatMFGInvoiceReconciliationSubmitHook()
    {
    }

    public List run(Approvable approvable)
    {
        Log.customer.debug("config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook");
        InvoiceReconciliation ir = (InvoiceReconciliation)approvable;
        String errorMsg = "";
        String warningMsg = "";
        String e1 = "";
        String e2 = "";
        if(hasQuantityException(ir.getLineItems()))
        {
            ariba.util.log.Log.customer.debug("%s *** hasPOQuantityException!!", "config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook");
            String e0 = ResourceService.getString("cat.java.vcsv2", "ErrorPOQExceptionOnIR");
            if(e0 != null)
                return ListUtil.list(Constants.getInteger(-2), e0);
        }
        User effectiveUser = (User)Base.getSession().getEffectiveUser();
//        if(effectiveUser.hasPermission(Permission.getPermission("InvoiceManager")))
 //           e1 = validateTotalTaxAmount(ir);
        if(effectiveUser.hasPermission(Permission.getPermission("InvoiceManager")))
            defaultAccountingForTaxLines(ir);
        e2 = validateAccounting(ir);
        if(!e1.equals(""))
            errorMsg = new String(e1);
        if(!e2.equals(""))
            if(errorMsg.equals(""))
                errorMsg = new String(e2);
            else
                errorMsg = errorMsg + " ; " + new String(e2);
        if(!errorMsg.equals(""))
            return ListUtil.list(Constants.getInteger(-2), errorMsg);
        warningMsg = getCreditInvoiceReminder(ir);
        if(!warningMsg.equals(""))
            return ListUtil.list(Constants.getInteger(1), warningMsg);
        else
            return NoErrorResult;
    }

    private boolean hasQuantityException(BaseVector lines)
    {
        if(lines != null)
        {
            int linecount = lines.size();
            for(int i = 0; i < linecount; i++)
            {
                InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lines.get(i);
                List exceptions = irli.getExceptions();
                if(!exceptions.isEmpty())
                {
                    int size = exceptions.size();
                    for(int j = 0; j < size; j++)
                    {
                        InvoiceException ie = (InvoiceException)exceptions.get(j);

                            // S. Sato - 14th Mar 2011 - Added State 'Accepted' as an additional state for clearing receipt quantity variance
                            // as the system could be in either of these two states if the exception is cleared
                        ariba.util.log.Log.customer.debug("%s *** Exception: %s", "config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook", ie);
                        if(ie != null && ie.getType().getUniqueName().indexOf("QuantityVariance") > -1 && (ie.getState() != 16 && ie.getState() != 2))
                            return true;
                    }

                }
            }

        }
        return false;
    }

    private String validateAccounting(InvoiceReconciliation ir)
    {
        FastStringBuffer acctngMsg = new FastStringBuffer();
        BaseVector lines = ir.getLineItems();
        for(int i = 0; i < lines.size(); i++)
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lines.get(i);
            String validateError = validateAccounting(irli);
            if(validateError != null)
                acctngMsg.append(validateError);
        }

        return acctngMsg.toString().trim();
    }

    private String validateAccounting(InvoiceReconciliationLineItem irli)
    {
        String result = null;
        FastStringBuffer lineMsg = new FastStringBuffer();
        for(Iterator itr = irli.getAccountings().getSplitAccountingsIterator(); itr.hasNext();)
        {
            SplitAccounting sa = (SplitAccounting)itr.next();
            int indicator = CatValidateMFGAccounting.validateAccounting(sa);
            if(indicator > 0)
            {
                String lineError = CatValidateMFGAccounting.getValidationMessage(indicator);
                if(lineError != null)
                {
                    int errorLine = irli.getLineItemNumber();
                    lineMsg.append(Fmt.S(LINEMARKER, String.valueOf(errorLine)));
                    lineMsg.append(lineError);
                    result = lineMsg.toString();
                }
            }
        }

        return result;
    }
// Start of Issue 869 --commenting validation to avoid tax amount rounding error
/*   private String validateTotalTaxAmount(InvoiceReconciliation ir)
    {
        String fmt = "";
        Money totalTaxAmount = ir.getTotalTax();
        Log.customer.debug("validateTotalTaxAmount: totalTaxAmount = %s", totalTaxAmount);
        Money computedTaxAmount = new Money(Constants.ZeroBigDecimal, totalTaxAmount.getCurrency());
        List lineItems = ir.getLineItems();
        int size = ListUtil.getListSize(lineItems);
          Log.customer.debug("validateTotalTaxAmount: size = %s", size);
        boolean nonTaxLineFound = false;
        for(int i = 0; i < size; i++)
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)lineItems.get(i);
            Log.customer.debug("validateTotalTaxAmount: irli = %s", irli);
            if(irli.getLineType().getCategory() != 2)
            {
                 Log.customer.debug("validateTotalTaxAmount: material line = %s");
                nonTaxLineFound = true;
                Money taxAmount = irli.getTaxAmount();
                Log.customer.debug("validateTotalTaxAmount: taxAmount = %s", taxAmount.getAmount());
                taxAmount.setAmount(taxAmount.getAmount().setScale(2, 4));
                if(taxAmount != null)
                 Log.customer.debug("validateTotalTaxAmount: taxAmount after rounding = %s", taxAmount.getAmount());
                    computedTaxAmount = Money.add(computedTaxAmount, taxAmount);
                    Log.customer.debug("validateTotalTaxAmount: computedTaxAmount after rounding = %s", computedTaxAmount);
            }
        }

        if(nonTaxLineFound && totalTaxAmount.approxCompareTo(computedTaxAmount) != 0)
            fmt = Fmt.Sil("aml.cat.Invoice", "NonMatchingTotalTax", totalTaxAmount.asString(), computedTaxAmount.asString());
        return fmt;
    }
*/

// End of issue 869
    private String getCreditInvoiceReminder(InvoiceReconciliation ir)
    {
        String fmt = "";
        if(ir.getTotalCost().getSign() < 0)
        {
            Log.customer.debug("config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook credit invoice...");
            PurchaseOrder po = null;
            Contract ma = null;
            List orders = ir.getOrders();
            List contracts = ir.getMasterAgreements();
            String pos = "";
            String mas = "";
            if(orders.size() > 0)
            {
                Log.customer.debug("config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook orders size > 0");
                po = (PurchaseOrder)orders.get(0);
                pos = pos + po.getUniqueName();
                for(int i = 1; i < orders.size(); i++)
                {
                    po = (PurchaseOrder)orders.get(i);
                    pos = pos + ", " + po.getUniqueName();
                }

                fmt = Fmt.Sil("aml.cat.Invoice", "CreditInvoiceMsg", "Purchase Order(s)", pos);
            } else
            if(contracts.size() > 0)
            {
                Log.customer.debug("config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook contracts size > 0");
                ma = (Contract)contracts.get(0);
                mas = mas + ma.getUniqueName();
                for(int i = 1; i < contracts.size(); i++)
                {
                    ma = (Contract)contracts.get(i);
                    mas = mas + ", " + ma.getUniqueName();
                }

                fmt = Fmt.Sil("aml.cat.Invoice", "CreditInvoiceMsg", "Master Agreement(s)", mas);
            }
        }
        return fmt;
    }

    private void defaultAccountingForTaxLines(InvoiceReconciliation ir)
    {
        if(!ir.getInvoice().isStandardInvoice())
            return;
        BaseVector irLineItems = ir.getLineItems();
        InvoiceReconciliationLineItem irLineItem = null;
        ariba.procure.core.ProcureLineItem procureLineItem = null;
        for(int i = 0; i < irLineItems.size(); i++)
        {
            irLineItem = (InvoiceReconciliationLineItem)irLineItems.get(i);
            if(irLineItem.getLineType().getCategory() == 2)
            {
                procureLineItem = CatMFGDefaultAccountingOnAdditionalCharges.getProcureLineItem(irLineItem);
                if(procureLineItem != null)
                    if(!ir.getInvoice().getIsTaxInLine())
                        CatMFGDefaultAccountingOnAdditionalCharges.defaultAccountingForHeaderLevelTax(irLineItem, procureLineItem);
                    else
                        CatMFGDefaultAccountingOnAdditionalCharges.defaultAccountingForLineLevelTax(irLineItem, procureLineItem);
            }
        }

    }

    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String catComponentStringTable = "aml.cat.Invoice";
    private static final String xType = "QuantityVariance";
    private static final int ValidationError = -2;
    private static final int ValidationWarning = 1;
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String ClassName = "config.java.invoicing.vcsv2.CatMFGInvoiceReconciliationSubmitHook";
    private static final String EMPTY_STRING = "";
    private static final String LINEMARKER = ResourceService.getString("cat.java.vcsv2", "SubmitHookLineMarker_Default");

}
