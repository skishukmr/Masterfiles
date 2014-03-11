// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 2:09:52 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoicing.vcsv1;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.common.core.Log;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;

public class CatCSVInvoiceReconciliationSubmitHook
    implements ApprovableHook
{

    public CatCSVInvoiceReconciliationSubmitHook()
    {
    }

    public List run(Approvable approvable)
    {
        //if(Log.customer.debugOn)
        //{
            Log.customer.debug("%s ::: Entering the Submit Hook Implementation run method", "CatCSVInvoiceReconciliationSubmitHook");
            Log.customer.debug("%s ::: Looking at the IR: %s", "CatCSVInvoiceReconciliationSubmitHook", approvable.getUniqueName());
        //}
        InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)approvable;
        String s = invoicereconciliation.getStatusString();
        if(s.equals("Reconciling") || s.equals("Approving") || s.equals("Rejected") || s.equals("Rejecting"))
        {
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: Cannot resubmit a reconciling, approving, rejecting or rejected IR", "CatCSVInvoiceReconciliationSubmitHook");
            String s1 = "Cannot resubmit an IR in Reconciling, Approving, Rejecting or Rejected Status";
            return ListUtil.list(Constants.getInteger(-2), s1);
        } else
        {
            return NoErrorResult;
        }
    }

    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String catComponentStringTable = "aml.cat.Invoice";
    private static final int ValidationError = -2;
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String ClassName = "CatCSVInvoiceReconciliationSubmitHook";

}