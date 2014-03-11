
// Shaila :  March 4th 07 :  CR # 755 VATCode changes
package config.java.invoicing.vcsv2;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Log;
import ariba.basic.core.Money;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;

public class CatMFGDefaultAccountingOnAdditionalCharges
{

    public CatMFGDefaultAccountingOnAdditionalCharges()
    {
    }

    public static void defaultAccountingOnLines(InvoiceReconciliation ir)
    {
        Log.customer.debug(" ************ Entering defaultAccountingOnLines() and IR object is "+ir);
        if(!ir.getInvoice().isStandardInvoice())
            return;
        BaseVector irLineItems = ir.getLineItems();
        InvoiceReconciliationLineItem irLineItem = null;
        ProcureLineItem procureLineItem = null;
        for(int i = 0; i < irLineItems.size(); i++)
        {
            irLineItem = (InvoiceReconciliationLineItem)irLineItems.get(i);
            if(irLineItem.getLineType().getCategory() != 1)
            {
                procureLineItem = getProcureLineItem(irLineItem);
                if(procureLineItem != null)
                    if(irLineItem.getLineType().getCategory() == 2)
                    {
                        // CR # 755 VATCode changes: Adding accounting only if we have matched lineitem
                        if(!(ir.getInvoice().getIsTaxInLine()) && irLineItem.getMatchedLineItem() == null)
                        {
                           Log.customer.debug(" **************** Default Accounting for Header Tax because TaxInLine = "+ir.getInvoice().getIsTaxInLine());
                           defaultAccountingForHeaderLevelTax(irLineItem, procureLineItem);
                        }
                        else
                        {
                           Log.customer.debug(" **************** Default Accounting for Line Tax because TaxInLine = "+ir.getInvoice().getIsTaxInLine());
                           defaultAccountingForLineLevelTax(irLineItem, procureLineItem);
                        }
                    } else
                    {
                        defaultAccountingForOtherCharges(irLineItem, procureLineItem);
                    }
            }
        }

    }

    public static void defaultAccountingForOtherCharges(InvoiceReconciliationLineItem irLineItem, ProcureLineItem procureLineItem)
    {
        irLineItem.setDottedFieldValue("AccountType", procureLineItem.getDottedFieldValue("AccountType"));
        SplitAccounting materialLineSA = (SplitAccounting)procureLineItem.getAccountings().getSplitAccountings().get(0);
        SplitAccounting sa = (SplitAccounting)irLineItem.getAccountings().getSplitAccountings().get(0);
        sa.setDottedFieldValue("Facility", materialLineSA.getDottedFieldValue("Facility"));
        sa.setDottedFieldValue("CostCenter", materialLineSA.getDottedFieldValue("CostCenter"));
        sa.setDottedFieldValue("Account", materialLineSA.getDottedFieldValue("Account"));
        sa.setDottedFieldValue("SubAccount", materialLineSA.getDottedFieldValue("SubAccount"));
        sa.setDottedFieldValue("Project", materialLineSA.getDottedFieldValue("Project"));
        irLineItem.getAccountings().getSplitAccountings().set(0, sa);
    }

    public static void defaultAccountingForHeaderLevelTax(InvoiceReconciliationLineItem taxLineItem, ProcureLineItem procureLineItem)
    {
        Log.customer.debug("Processing Invoice Line = " + taxLineItem.getNumberInCollection() + " of Line Type " + taxLineItem.getDottedFieldValue("LineType.UniqueName"));
        String delimiter = "^";
        InvoiceReconciliation ir = (InvoiceReconciliation)taxLineItem.getLineItemCollection();
        Log.customer.debug("InvoiceReconciliation is " + ir.getUniqueName());
        BaseVector irLineItems = ir.getLineItems();
        ProcureLineItemCollection plic = (ProcureLineItemCollection)procureLineItem.getLineItemCollection();
        ariba.base.core.Partition partition = taxLineItem.getPartition();
        SplitAccounting materialLineSA = (SplitAccounting)procureLineItem.getAccountings().getSplitAccountings().get(0);
        String facility = (String)materialLineSA.getDottedFieldValue("Facility.UniqueName");
        if(facility == null)
            facility = " ";
        taxLineItem.setDottedFieldValueRespectingUserData("AccountType", procureLineItem.getDottedFieldValue("AccountType"));
        boolean zeroTax = false;
        if(taxLineItem.getAmount().getAmount().doubleValue() == 0.0D)
            zeroTax = true;
        Hashtable ht = new Hashtable();
        for(int i = 0; i < irLineItems.size(); i++)
        {
            InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)irLineItems.get(i);
            if(irli.getLineType().getCategory() == 2)
                continue;
            if(ir.getConsolidated())
            {
                ProcureLineItemCollection temp = irli.getOrder();
                if(temp == null)
                    temp = irli.getMasterAgreement();
                if(temp == null || temp != plic)
                    continue;
            }
            String subaccount = null;
            String project = null;
            boolean vatRecoverable = true;
            try
            {
                vatRecoverable = ((Boolean)irli.getDottedFieldValue("IsVATRecoverable")).booleanValue();
            }
            catch(NullPointerException ne) { }
            if(!vatRecoverable && !zeroTax)
            {
                subaccount = (String)materialLineSA.getDottedFieldValue("SubAccount.UniqueName");
                project = (String)materialLineSA.getDottedFieldValue("Project.UniqueName");
            }
            if(subaccount == null)
                subaccount = " ";
            if(project == null)
                project = " ";
            ClusterRoot _costcenter = getCostCenter(taxLineItem, irli, materialLineSA);
            ClusterRoot _account = getAccount(taxLineItem, irli, materialLineSA);
            String costcenter = null;
            String account = null;
            if(_costcenter == null)
                costcenter = " ";
            else
                costcenter = (String)_costcenter.getDottedFieldValue("UniqueName");
            if(_account == null)
                account = " ";
            else
                account = (String)_account.getDottedFieldValue("UniqueName");
            String key = facility + delimiter + costcenter + delimiter + account + delimiter + subaccount + delimiter + project;
            Money value = new Money(Constants.ZeroBigDecimal, taxLineItem.getAmount().getCurrency());
            if(irli.getTaxAmount() != null)
                value = new Money(irli.getTaxAmount());
            if(ht.containsKey(key))
            {
                Money previousValue = (Money)ht.get(key);
                value.addTo(previousValue);
            }
            Log.customer.debug("HT Key = " + key);
            Log.customer.debug("HT Value = " + value.getAmount().doubleValue());
            ht.put(key, value);
            if(zeroTax)
                break;
        }

        Log.customer.debug("HT Size = " + ht.size());
        if(ht.size() == 0)
        {
            defaultAccountingForOtherCharges(taxLineItem, procureLineItem);
            return;
        }
        SplitAccountingCollection sac = new SplitAccountingCollection(new SplitAccounting(partition));
        SplitAccounting sa = null;
        boolean firstEntry = true;
        for(Enumeration e = ht.keys(); e.hasMoreElements();)
        {
            String key = e.nextElement().toString();
            Money value = (Money)ht.get(key);
            Log.customer.debug("------------------------------------------------------------------");
            Log.customer.debug("Key = " + key);
            Log.customer.debug("Value = " + value.getAmount().doubleValue());
            if(ht.size() <= 1 || value.getAmount().doubleValue() != 0.0D)
            {
                StringTokenizer st = new StringTokenizer(key, delimiter);
                ClusterRoot _facility = Base.getService().objectMatchingUniqueName("cat.core.Facility", partition, st.nextToken());
                ClusterRoot _costcenter = Base.getService().objectMatchingUniqueName("ariba.core.CostCenter", partition, st.nextToken());
                ClusterRoot _account = Base.getService().objectMatchingUniqueName("ariba.core.Account", partition, st.nextToken());
                ClusterRoot _subaccount = Base.getService().objectMatchingUniqueName("ariba.core.SubAccount", partition, st.nextToken());
                ClusterRoot _project = Base.getService().objectMatchingUniqueName("cat.core.Project", partition, st.nextToken());
                if(firstEntry)
                    firstEntry = false;
                else
                    sac.addSplit();
                sa = (SplitAccounting)sac.getSplitAccountings().lastElement();
                sa.setFieldValue("Amount", value);
                sa.setFieldValue("Facility", _facility);
                sa.setFieldValue("CostCenter", _costcenter);
                sa.setFieldValue("Account", _account);
                sa.setFieldValue("SubAccount", _subaccount);
                sa.setFieldValue("Project", _project);
            }
        }
		Log.customer.debug("********** SA Amount before setting Accounting object - header tax"+sa.getAmount());
        taxLineItem.setDottedFieldValueRespectingUserData("Accountings", sac);
        Log.customer.debug("********** SA Amount after setting Accounting object - header tax"+sa.getAmount());
    }

    public static void defaultAccountingForLineLevelTax(InvoiceReconciliationLineItem taxLineItem, ProcureLineItem procureLineItem)
    {
        Log.customer.debug("defaultAccountingForLineLevelTax...");
        taxLineItem.setDottedFieldValueRespectingUserData("AccountType", procureLineItem.getDottedFieldValue("AccountType"));
        boolean zeroTax = false;
        if(taxLineItem.getAmount().getAmount().doubleValue() == 0.0D)
            zeroTax = true;
        SplitAccounting materialLineSA = (SplitAccounting)procureLineItem.getAccountings().getSplitAccountings().get(0);
        InvoiceReconciliationLineItem parentIRLine = (InvoiceReconciliationLineItem)taxLineItem.getParent();
        ClusterRoot _costCenter = getCostCenter(taxLineItem, parentIRLine, materialLineSA);
        ClusterRoot _account = getAccount(taxLineItem, parentIRLine, materialLineSA);
        if(_account == null)
            _account = (ClusterRoot)materialLineSA.getDottedFieldValue("Account");
        if(_costCenter == null)
            _costCenter = (ClusterRoot)materialLineSA.getDottedFieldValue("CostCenter");
        SplitAccountingCollection sac = (SplitAccountingCollection)taxLineItem.getAccountings().deepCopyAndStrip();
        SplitAccounting sa = (SplitAccounting)sac.getSplitAccountings().get(0);
        sa.setDottedFieldValue("Facility", materialLineSA.getDottedFieldValue("Facility"));
        sa.setDottedFieldValue("CostCenter", _costCenter);
        sa.setDottedFieldValue("Account", _account);
        boolean vatRecoverable = true;
        try
        {
            vatRecoverable = ((Boolean)parentIRLine.getDottedFieldValue("IsVATRecoverable")).booleanValue();
        }
        catch(NullPointerException ne) { }
        if(vatRecoverable)
        {
            sa.setDottedFieldValue("SubAccount", null);
            sa.setDottedFieldValue("Project", null);
        } else
        if(!zeroTax)
        {
            sa.setDottedFieldValue("SubAccount", materialLineSA.getDottedFieldValue("SubAccount"));
            sa.setDottedFieldValue("Project", materialLineSA.getDottedFieldValue("Project"));
        }
        sac.setDottedFieldValue("SplitAccountings", ListUtil.list(sa));
        Log.customer.debug("********** SA Amount after setting Accounting object - line tax "+sa.getAmount());
        taxLineItem.setDottedFieldValueRespectingUserData("Accountings", sac);
        Log.customer.debug("********** SA Amount after setting Accounting object - line tax "+sa.getAmount());
    }

    private static ClusterRoot getAccount(InvoiceReconciliationLineItem taxLineItem, InvoiceReconciliationLineItem irli, SplitAccounting materialLineSA)
    {
        String vatClassId = null;
        boolean vatRecoverable = true;
        Log.customer.debug("in method getAccount ...");
        try
        {
            vatClassId = (String)irli.getDottedFieldValue("VATClass.UniqueName");
            vatRecoverable = ((Boolean)irli.getDottedFieldValue("IsVATRecoverable")).booleanValue();
        }
        catch(NullPointerException ne)
        {
            return null;
        }
        if(vatClassId == null)
            return null;
        Log.customer.debug("VAT Class is " + vatClassId);
        String account = null;
        ClusterRoot _account = null;
        boolean zeroTax = false;
        if(taxLineItem.getAmount().getAmount().doubleValue() == 0.0D)
            zeroTax = true;
        if(vatRecoverable || zeroTax)
        {
            if(materialLineSA.getDottedFieldValue("Facility.UniqueName").toString().equals("DX"))
                account = "DX5164" + vatClassId;
            else
            if(vatClassId.equals("2"))
                account = "NA85350";
            else
                account = "NA85150";
            Log.customer.debug("account is " + account);
            _account = Base.getService().objectMatchingUniqueName("ariba.core.Account", irli.getPartition(), account);
        } else
        {
            Log.customer.debug("vat is not recoverable ...");
            _account = (ClusterRoot)materialLineSA.getDottedFieldValue("Account");
        }
        return _account;
    }

    private static ClusterRoot getCostCenter(InvoiceReconciliationLineItem taxLineItem, InvoiceReconciliationLineItem irli, SplitAccounting materialLineSA)
    {
        String vatClassId = null;
        boolean vatRecoverable = true;
        Log.customer.debug("in method getCostCenter ...");
        try
        {
            vatClassId = (String)irli.getDottedFieldValue("VATClass.UniqueName");
            vatRecoverable = ((Boolean)irli.getDottedFieldValue("IsVATRecoverable")).booleanValue();
        }
        catch(NullPointerException ne)
        {
            return null;
        }
        if(vatClassId == null)
            return null;
        Log.customer.debug("VAT Class is " + vatClassId);
        String costCenter = null;
        ClusterRoot _costCenter = null;
        boolean zeroTax = false;
        if(taxLineItem.getAmount().getAmount().doubleValue() == 0.0D)
            zeroTax = true;
        if(vatRecoverable || zeroTax)
        {
            if(materialLineSA.getDottedFieldValue("Facility.UniqueName").toString().equals("DX"))
            {
                costCenter = "DXN501";
            } else
            {
                if(vatClassId.equals("0"))
                    costCenter = "VC35";
                if(vatClassId.equals("1"))
                    costCenter = "VC34";
                if(vatClassId.equals("2"))
                    costCenter = "VC40";
                if(vatClassId.equals("3"))
                    costCenter = "VC40";
                if(vatClassId.equals("4"))
                    costCenter = "VC36";
                if(vatClassId.equals("5"))
                    costCenter = "VC38";
                if(vatClassId.equals("6"))
                    costCenter = "VC41";
                if(vatClassId.equals("7"))
                    costCenter = "VC41";
                if(vatClassId.equals("8"))
                    costCenter = "VC47";
                if(vatClassId.equals("9"))
                    costCenter = "VC37";
                costCenter = "NA" + costCenter;
            }
            Log.customer.debug("cost center is " + costCenter);
            _costCenter = Base.getService().objectMatchingUniqueName("ariba.core.CostCenter", irli.getPartition(), costCenter);
        } else
        {
            Log.customer.debug("vat is not recoverable ...");
            _costCenter = (ClusterRoot)materialLineSA.getDottedFieldValue("CostCenter");
        }
        return _costCenter;
    }

    public static ProcureLineItem getProcureLineItem(InvoiceReconciliationLineItem irLineItem)
    {
        ProcureLineItem procureLineItem = irLineItem.getOrderLineItem();
        if(procureLineItem == null)
            procureLineItem = irLineItem.getMALineItem();
        if(procureLineItem != null)
            return procureLineItem;
        ProcureLineItemCollection plic = irLineItem.getOrder();
        if(plic == null)
            plic = irLineItem.getMasterAgreement();
        if(plic != null)
            procureLineItem = (ProcureLineItem)plic.getLineItems().get(0);
        return procureLineItem;
    }

    public static final String ClassName = "config.java.invoicing.vcsv2.CatMFGDefaultAccountingOnAdditionalCharges";
    private static final String DX_FACILITY = "DX";
    private static final String NA_FACILITY = "NA";
}