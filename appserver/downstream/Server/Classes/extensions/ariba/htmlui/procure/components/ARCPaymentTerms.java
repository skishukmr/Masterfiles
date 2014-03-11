/*
    Copyright (c) 1996-2004 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/procureui/11.28.1+/ariba/htmlui/procure/components/ARCPaymentTerms.java#1 $

    Responsible: kgu
*/

package ariba.htmlui.procure.components;

import ariba.approvable.core.Approvable;
import ariba.base.core.aql.AQLNameTable;
import ariba.common.core.Log;
import ariba.payment.core.PaymentTerms;
import ariba.htmlui.baseui.ARPDefaultNamedChooser;
import ariba.htmlui.fieldsui.ARPPage;
import ariba.htmlui.fieldsui.components.ARCComponent;
import ariba.htmlui.procure.ARBBindingNames;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.PurchaseOrder;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ClassUtil;

/**
    This component renders the contents of an invoice
    tab.

    Required bindings:
        approvable            in   The approvable to render
        groupEditable         in   Indicates whether the payment terms
                                   are editable
*/
public class ARCPaymentTerms extends ARCComponent
{
    /*-----------------------------------------------------------------------
        Constants
     -----------------------------------------------------------------------*/

    public static final String Name = "ARCPaymentTerms";
    public static final String Group = "PaymentTermsDetail";
    public static final String ClassesWithPaymentTerms [] =
                               {PurchaseOrder.ClassName,
                                InvoiceReconciliation.ClassName};

    /*-----------------------------------------------------------------------
        Memebers
     -----------------------------------------------------------------------*/
    private Approvable m_approvable;

    /*-----------------------------------------------------------------------
        overrided method
     -----------------------------------------------------------------------*/
    public void init ()
    {
        super.init();
        m_approvable = null;
    }

    protected void sleep ()
    {
        m_approvable = null;
        super.sleep();
    }

    /*-----------------------------------------------------------------------
        methods
     -----------------------------------------------------------------------*/

    public Approvable approvable ()
    {
        if (m_approvable == null) {
            if (hasBinding(ARBBindingNames.ValueSource)) {
                m_approvable =
                    (Approvable)valueForBinding(ARBBindingNames.ValueSource);
            }
        }

        return m_approvable;
    }

    /**
        Determine if payment terms should be displayed for this
        approvable object.  If apprvable object is an PO or invoice,
        then display payment terms.
    */
    public boolean isDisplayPaymentTerms ()
    {
        boolean displayPaymentTerms = false;
        // ARajendren Ariba, Inc.,
        // 9R1 Upgrade, Added CAT core code customizations.
        // CAT - Core Code Hack Starts
        /*if (approvable() != null) {
            for (int i = 0; i < ClassesWithPaymentTerms.length; i++) {
                if (ClassUtil.classForName(
                        ClassesWithPaymentTerms[i]).isInstance(approvable()))
                {
                    displayPaymentTerms = true;
                    break;
                }
            }
        }*/
        Approvable approvable = approvable();
        if (approvable != null) {
        	if("pcsv1".equals(approvable.getPartition().getName()) && (approvable instanceof ariba.statement.core.StatementCoreApprovable))
            {
                Log.customer.debug("ARCPaymentTerms ::: Successfully returned a false for StatementCoreApprovable");
                return false;
            }
            for (int i = 0; i < ClassesWithPaymentTerms.length; i++) {
                if (ClassUtil.classForName(
                        ClassesWithPaymentTerms[i]).isInstance(approvable()))
                {
                    displayPaymentTerms = true;
                    break;
                }
            }
        }
        return displayPaymentTerms;
        // CAT - End of Core Code Hack
    }

    public PaymentTerms getPaymentTerms ()
    {
        return ((ProcureLineItemCollection)approvable()).getPaymentTerms();
    }

    /**
        Determine if payment terms is null or not.
    */
    public boolean isPaymentTermsExists ()
    {
        boolean isPaymentTermExists = false;
        if (getPaymentTerms() == null) {
            isPaymentTermExists = false ;
        }
        else {
            isPaymentTermExists = true;
        }

        return isPaymentTermExists;
    }

    public String group ()
    {
        return Group;
    }

    public boolean groupEditable ()
    {
        return booleanValueForBinding(ARBBindingNames.GroupEditable, true);
    }

    /*-----------------------------------------------------------------------
        Actions
      -----------------------------------------------------------------------*/

    public AWComponent selectPaymentTermsAction ()
    {
        ARPDefaultNamedChooser page =
            (ARPDefaultNamedChooser)pageWithName(ARPDefaultNamedChooser.Name);

        AQLNameTable nameTable =
            new AQLNameTable(PaymentTerms.ClassName, false);

        page.initialize(  nameTable,
                          "PaymentTermsChooser",   /* groupForTable*/
                          "UniqueName",            /* tableField */
                          null,  /* customObjectClass*/
                          false,                   /* allowNullChoice*/
                          null, /* nullValueName */
                          false,                   /* showDetails*/
                          false,                   /* preload */
                          "PaymentTerms",          /* fieldName*/
                          approvable(), /* ValueSource*/
                          (ARPPage)pageComponent());         /* returnPage*/

        return page;
    }

    public AWComponent removePaymentTermsAction ()
    {
        ((ProcureLineItemCollection)approvable()).setPaymentTerms(null);
        return null;
    }

}
