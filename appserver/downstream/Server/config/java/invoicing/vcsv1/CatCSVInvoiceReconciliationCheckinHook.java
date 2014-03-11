// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 2:09:48 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoicing.vcsv1;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Message;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;

public class CatCSVInvoiceReconciliationCheckinHook
    implements ApprovableHook
{

    public CatCSVInvoiceReconciliationCheckinHook()
    {
    }

    public List run(Approvable approvable)
    {
        //if(Log.customer.debugOn)
        //{
            Log.customer.debug("%s ::: Entering the CheckIn Hook Implementation run method", "CatCSVInvoiceReconciliationCheckinHook");
            Log.customer.debug("%s ::: Looking at the IR: %s", "CatCSVInvoiceReconciliationCheckinHook", approvable.getUniqueName());
        //}
        InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)approvable;
        BaseVector basevector = invoicereconciliation.getLineItems();
        Object obj = null;
        Object obj1 = null;
        Object obj2 = null;
        Object obj3 = null;
        String s = "";
        Object obj4 = null;
        Object obj5 = null;
        User user = (User)Base.getSession().getEffectiveUser();
        String s3 = user.getMyName();
        for(int i = 0; i < basevector.size(); i++)
        {
            InvoiceReconciliationLineItem invoicereconciliationlineitem = (InvoiceReconciliationLineItem)basevector.get(i);
            SplitAccountingCollection splitaccountingcollection = invoicereconciliationlineitem.getAccountings();
            if(splitaccountingcollection == null)
                continue;
            BaseVector basevector1 = splitaccountingcollection.getSplitAccountings();
            int j = basevector1.size();
            for(int k = 0; k < j; k++)
            {
                SplitAccounting splitaccounting = (SplitAccounting)basevector1.get(k);
                Response response = CatValidateInvAccountingString.validateAccounting(splitaccounting);

                    // ssato aul - fix for NPE
                if (response != null) {
                    Message msg = response.getMessage();
                    if (msg != null) {
                        String s1 = response.getMessage().getSubroutineReturnCode();
                        String s2 = response.getMessage().getSubroutineReturnMessage();
                        if(s1 != null && s1.compareTo("00") != 0) {
                            s = s + "Line " + (i + 1) + " Split " + (k + 1) + ": Error - " + s2 + ";\n";
                        }
                    }
                }

            }

        }

        if(!StringUtil.nullOrEmptyOrBlankString(s))
        {
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", "CatCSVInvoiceReconciliationCheckinHook", s);
            return ListUtil.list(Constants.getInteger(-2), s);
        } else
        {
            return NoErrorResult;
        }
    }

    private String hasQuantityException(BaseVector basevector)
    {
        String s = null;
        String s1 = null;
        String s2 = null;
        if(basevector != null)
        {
            int i = basevector.size();
            for(int j = 0; j < i; j++)
            {
                InvoiceReconciliationLineItem invoicereconciliationlineitem = (InvoiceReconciliationLineItem)basevector.get(j);
                BaseVector basevector1 = invoicereconciliationlineitem.getExceptions();
                if(basevector1.isEmpty())
                    continue;
                int k = basevector1.size();
                for(int l = 0; l < k; l++)
                {
                    InvoiceException invoiceexception = (InvoiceException)basevector1.get(l);
                    ariba.util.log.Log.customer.debug("%s ::: Exception: %s", "CatCSVInvoiceReconciliationCheckinHook", invoiceexception.getType().getUniqueName());
                    ariba.util.log.Log.customer.debug("%s ::: ExceptionStatus: %s", "CatCSVInvoiceReconciliationCheckinHook", invoiceexception.getState());
                    if(invoiceexception == null || invoiceexception.getType().getUniqueName().indexOf("ReceivedQuantityVariance") <= -1)
                        continue;
                    if(s2 == null)
                        s2 = invoiceexception.getType().getUniqueName();
                    if(invoiceexception.getState() == 4)
                    {
                        if(s1 == null)
                            s1 = "" + (j + 1);
                        else
                            s1 = s1 + (j + 1);
                    } else
                    if(invoiceexception.getState() != 16)
                        if(s == null)
                            s = "" + (j + 1);
                        else
                            s = s + (j + 1);
                    if(s != null && j < i - 1)
                        s = s + ", ";
                    if(s1 != null && j < i - 1)
                        s1 = s1 + ", ";
                }

            }

        }
        if(s != null)
        {
            s = s + ";" + s2 + ";" + "Error";
            return s;
        } else
        {
            return null;
        }
    }

    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String catComponentStringTable = "aml.cat.Invoice";
    private static final int ValidationError = -2;
    private static final int ValidationWarning = 1;
    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String LindaAlwaysRequired = Fmt.Sil("aml.cat.Invoice", "LindaAlwaysRequired");
    private static final String ClassName = "CatCSVInvoiceReconciliationCheckinHook";

}