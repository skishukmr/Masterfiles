package config.java.customapprover;

import ariba.approvable.core.ApprovalRequest;
import ariba.approvable.core.CustomApproverDelegateAdapter;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.user.core.Role;
import ariba.user.core.User;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.tax.CatTaxUtil;

public class CatTaxCustomApproverInv extends CustomApproverDelegateAdapter
{

    public CatTaxCustomApproverInv()
    {
    }

    public void notifyApprovalRequired(ApprovalRequest approvalrequest, String s, boolean flag)
    {
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: IN notifyApprovalRequired() for InvoiceReconciliation", "CatTaxCustomApproverInv");
        ariba.approvable.core.Approvable approvable = approvalrequest.getApprovable();
        InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)approvable;
        User user = User.getAribaSystemUser(approvalrequest.getPartition());
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        TaxReason = null;
        String s1 = CatTaxUtil.evaluateTax(invoicereconciliation);
        if(s1 != null)
        {
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: Required fields are missing hence setting AssessTaxMessage", "CatTaxCustomApproverInv");
            invoicereconciliation.setFieldValue("AssessTaxMessage", s1.toString());
            invoicereconciliation.setFieldValue("taxCallNotFailed", new Boolean(false));
            TaxReason = "Error encountered while calling tax module as required fields were not populated in the invoice reconciliation";
            flag1 = true;
        } else
        {
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: assessTaxMessage was null, hence seeing if approvals required", "CatTaxCustomApproverInv");
            invoicereconciliation.setFieldValue("TaxOverrideFlag", new Boolean(false));
            invoicereconciliation.setFieldValue("taxCallNotFailed", new Boolean(true));
            invoicereconciliation.setFieldValue("AssessTaxMessage", null);
            String s2 = "";
            String s3 = "";
            BaseVector basevector = (BaseVector)invoicereconciliation.getFieldValue("LineItems");
            int i = basevector.size();
            for(int j = 0; j < i; j++)
            {
                InvoiceReconciliationLineItem invoicereconciliationlineitem = (InvoiceReconciliationLineItem)basevector.get(j);
                String s7 = (String)invoicereconciliationlineitem.getFieldValue("TaxApprovalCode");
                ClusterRoot clusterroot = (ClusterRoot)invoicereconciliationlineitem.getFieldValue("TaxCode");
                //if(Log.customer.debugOn)
                {
                    Log.customer.debug("%s ::: The Approval Code is: %s", "CatTaxCustomApproverInv", s7);
                    Log.customer.debug("%s ::: The Tax Code is: %s", "CatTaxCustomApproverInv", clusterroot);
                }
                if(s7 == null && clusterroot != null)
                    continue;
                if(clusterroot == null || s7.equalsIgnoreCase("A"))
                {
                    //if(Log.customer.debugOn)
                        Log.customer.debug("%s ::: Found workflow indicator = A", "CatTaxCustomApproverInv");
                    flag2 = true;
                    s2 = s2 + Integer.toString(invoicereconciliationlineitem.getNumberInCollection()) + ", ";
                    continue;
                }
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Found workflow indicator = W", "CatTaxCustomApproverInv");
                flag3 = true;
                s3 = s3 + Integer.toString(invoicereconciliationlineitem.getNumberInCollection()) + ", ";
            }

            if(flag2)
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Setting the TaxReason and approvalRequired to true", "CatTaxCustomApproverInv");
                flag1 = true;
                TaxReason = "Tax Manager approval required as they need to look at the following lines: " + s2;
            } else
            if(flag3)
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Setting the TaxReason", "CatTaxCustomApproverInv");
                TaxReason = "Tax Manager added as a watcher for the following lines: " + s3;
            }
            if(taxApprovalRequired.equalsIgnoreCase("true"))
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: Initial Go-live, hence always adding the tax approver, setting spprovalReq", "CatTaxCustomApproverInv");
                flag1 = true;
                TaxReason = "Tax Manager added as a required approver for the initial go-live";
            }
            if(taxApprovalRequiredEmergencyBuy.equalsIgnoreCase("true"))
            {
                Object obj1 = null;
                boolean flag5 = false;
                PurchaseOrder purchaseorder = invoicereconciliation.getOrder();
                if(purchaseorder != null)
                {
                    Boolean boolean1 = (Boolean)purchaseorder.getDottedFieldValue("EmergencyBuy");
                    if(boolean1 != null)
                        flag5 = boolean1.booleanValue();
                }
                if(flag5)
                {
                    //if(Log.customer.debugOn)
                        Log.customer.debug("%s ::: Tax Manager added as approver for all emergency buys", "CatTaxCustomApproverInv");
                    flag1 = true;
                    TaxReason = "Tax Manager added as a required approver for Emergency Buy";
                }
            }
        }
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: Calling the method to set the exceptions if any on the IR", "CatTaxCustomApproverInv");
        boolean flag4 = false;
        flag4 = CatTaxUtil.addExceptions(invoicereconciliation);
        if(flag4)
        {
            flag1 = flag4;
            TaxReason = "Tax Manager added to handle tax related exceptions";
        }
        if(!flag1)
        {
            String s4 = CatTaxUtil.checkForNullRequiredFields(invoicereconciliation);
            if(s4 != null)
            {
                flag1 = true;
                String s5 = "Tax Manager added as the following tax fields are null: " + s4;
                LongString longstring = new LongString(s5);
                String s6 = "Reason For Adding Tax Manager";
                Date date = new Date();
                User user1 = User.getAribaSystemUser(invoicereconciliation.getPartition());
                CatTaxUtil.addCommentToIR(invoicereconciliation, longstring, s6, date, user1);
                TaxReason = "Tax Manager added as the following required fields are null: " + s4;
                invoicereconciliation.setFieldValue("AssessTaxMessage", s5);
            }
        }
        if(TaxReason != null)
        {
            Object obj = Role.getRole(TaxRole);
            if(obj == null)
                obj = user;
            ApprovalRequest approvalrequest1 = ApprovalRequest.create(invoicereconciliation, ((ariba.user.core.Approver) (obj)), flag1, "RuleReasons", TaxReason);
            BaseVector basevector1 = invoicereconciliation.getApprovalRequests();
            BaseVector basevector2 = approvalrequest1.getDependencies();
            basevector2.add(0, approvalrequest);
            approvalrequest1.setFieldValue("Dependencies", basevector2);
            approvalrequest.setState(2);
            approvalrequest.updateLastModified();
            basevector1.removeAll(approvalrequest);
            basevector1.add(0, approvalrequest1);
            invoicereconciliation.setApprovalRequests(basevector1);
            java.util.List list = ListUtil.list();
            java.util.Map map = MapUtil.map();
            boolean flag6 = approvalrequest1.activate(list, map);
            //if(Log.customer.debugOn)
            {
                Log.customer.debug("%s ::: New TaxAR Activated - " + flag6, "CatTaxCustomApproverInv");
                Log.customer.debug("%s ::: State (AFTER): " + approvalrequest.getState(), "CatTaxCustomApproverInv");
                Log.customer.debug("%s ::: Approved By: %s", approvalrequest.getApprovedBy(), "CatTaxCustomApproverInv");
            }
        } else
        {
            approvalrequest.setState(2);
        }
    }

    public String getIcon(ApprovalRequest approvalrequest)
    {
        return super.getIcon(approvalrequest);
    }

    private static final String ClassName = "CatTaxCustomApproverInv";
    private static String TaxRole = "Tax Manager";
    private static String TaxReason = null;
    private static final String taxApprovalRequired = ResourceService.getString("aml.cat.Invoice", "TaxApprovalAlwaysRequired");
    private static final String taxApprovalRequiredEmergencyBuy = ResourceService.getString("aml.cat.Invoice", "TaxApprovalRequiredEmergencyBuy");

}
