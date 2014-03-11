/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/procureui/11.28.1+/ariba/htmlui/procure/invoicing/components/ARCReconcileInvoiceComponent.java#6 $

    Responsible: ssamant
*/

package ariba.htmlui.procure.invoicing.components;

import java.util.List;

import ariba.approvable.core.Access;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableUtil;
import ariba.approvable.core.print.Approvable_Print;
import ariba.base.core.ClusterRoot;
import ariba.htmlui.approvableui.ARBPrintUtil;
import ariba.htmlui.approvableui.ARPMobilePrint;
import ariba.htmlui.baseui.BaseUISession;
import ariba.htmlui.coreui.Session;
import ariba.htmlui.fieldsui.ARPPage;
import ariba.htmlui.procure.Log;
import ariba.htmlui.procure.invoicing.ARPChangeInvoicePartition;
import ariba.htmlui.procure.invoicing.ARPDoneReconciling;
import ariba.htmlui.procure.invoicing.CommandDefinition;
import ariba.htmlui.procure.invoicing.InvoiceCommands;
import ariba.htmlui.procure.invoicing.wizards.ARBInvoicingContext;
import ariba.htmlui.procure.receiving.ARBReceiptOnClient;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceUtil;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.html.AWBaseImage;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import config.java.invoicing.sap.CatSapIRApprovalRulesUtil;

/**
    S. Sato AUL - Migrated functionality from Viking
    822 equivalent class ariba.htmlui.procure.invoicing.ARPInvoicingWizard

    This class has been modified to do the following:
    - Receipt button shows up in the invoice reconciliation screen when there
    is a receipt quantity variance and if the user is responsible for receiving
*/
public class ARCReconcileInvoiceComponent extends ARCIRBasicComponent
{

    /*-----------------------------------------------------------------------
        Constants
        -----------------------------------------------------------------------*/

    public static final String Name = "ARCReconcileInvoiceComponent";

    private static final String StringTable = "ariba.html.invoicing";
    private static final String IRLineItemWarningLimitKey = "IRLineItemWarningLimit";

    // The line item warning limit of 2000 was determined by the performance team. IR's
    // with more than 2000 line items will have performance problems.
    public static final int IRLineItemWarningLimit = 2000;

    public static final CommandDefinition SubmitButton =
        new CommandDefinition("submitAction",
                      "@ariba.html.invoicing/InvoiceSubmit",
                      "@ariba.html.invoicing.alt/InvoiceSubmitTip");

    protected static final CommandDefinition SaveButton =
        new CommandDefinition("saveAction",
                      "@ariba.html.invoicing/InvoiceSave",
                      "@ariba.html.invoicing.alt/InvoiceSaveTip");

    public static final CommandDefinition ApproveButton =
        new CommandDefinition(InvoiceCommands.ApproveKey,
                      "@ariba.html.invoicing/ApproveInvoice",
                      "@ariba.html.invoicing.alt/ApproveInvoiceTip");

    public static final CommandDefinition ApproveRejectButton =
        new CommandDefinition(InvoiceCommands.ApproveKey,
                      "@ariba.html.invoicing/ApproveInvoiceForRejection",
                      "@ariba.html.invoicing.alt/ApproveInvoiceForRejectionTip");

    private static final CommandDefinition RejectButton =
        new CommandDefinition(InvoiceCommands.RejectKey,
                      "@ariba.html.invoicing/RejectInvoice",
                      "@ariba.html.invoicing.alt/RejectInvoiceTip");

    private static final CommandDefinition CancelRejectButton =
        new CommandDefinition("cancelRejectAction",
                      "@ariba.html.invoicing/CancelRejectInvoice",
                      "@ariba.html.invoicing.alt/CancelRejectInvoiceTip");

    private static final CommandDefinition ManualMatchButton =
        new CommandDefinition("manualMatchAction",
                      "@ariba.html.invoicing/InvoiceMatchToPO",
                      "@ariba.html.invoicing.alt/InvoiceMatchToPOTip");

    private static final CommandDefinition ReferButton =
        new CommandDefinition("referAction",
                      "@ariba.html.invoicing/ReferInvoice",
                      "@ariba.html.invoicing.alt/ReferInvoiceTip");

    private static final CommandDefinition ChangePartitionButton =
        new CommandDefinition("changePartitionAction",
                      "@ariba.html.invoicing/ChangeInvoicePartition",
                      "@ariba.html.invoicing.alt/ChangeInvoicePartitionTip");

    private static final CommandDefinition CalculateTaxButton =
        new CommandDefinition("calculateTaxAction",
                      "@ariba.html.invoicing/CalculateTax",
                      "@ariba.html.invoicing.alt/CalculateTaxTip");

    public static final CommandDefinition PrintButton =
        new CommandDefinition(InvoiceCommands.PrintKey,
                "@ariba.html.commonui/ApprovableCommandsPrint",
                "@ariba.html.commonui.alt/ApprovableCommandsPrintTip");

    /**
        Receive Button - Caterpillar Customization
    */
    private static final CommandDefinition ReceiveButton =
        new CommandDefinition("receiveAction",
                "@config.html.invoicing/ReceiveInvoice",
                "@config.html.invoicing.alt/ReceiveInvoiceTip");



    /*-----------------------------------------------------------------------
        Variables
      -----------------------------------------------------------------------*/

    private List m_buttons;
    private String TaxCalculationFailedException =
            "TaxCalculationFailed";
    private String ReconcileInvoiceTaxCalculationFailed =
            "ReconcileInvoiceTaxCalculationFailed";
    private String m_largeInvoiceReconciliationMessage;
    private boolean showLargeIRWarningMessage = true;

    /*-----------------------------------------------------------------------
        Overrides
      -----------------------------------------------------------------------*/

    /**
        Override to reset PendingUserAction.
    */
    @Override
    public void appendToResponse (AWRequestContext requestContext,
                                  AWComponent component)
    {
        InvoiceReconciliation ir = reconciliation();
        ir.setPendingUserAction(null);
        Log.invoicingUI.debug("Cleared PendingUserAction");
        super.appendToResponse(requestContext, component);
    }

    /**
        Returns the list of button definitinos that should be
        rendered in the button row for this page.

        Overridden by subclasses to define a different set of buttons.
    */
    public List getCommandButtons ()
    {
        if (m_buttons != null) {
            return m_buttons;
        }

        InvoiceReconciliation ir = reconciliation();
        User user = User.getEffectiveUser();
        m_buttons = ListUtil.list();
        if (ir.getAccess(InvoiceReconciliation.ApproveAccess, user) ==
            Access.Now) {
            if (ir.isStatusReconciling()) {
                m_buttons.add(SubmitButton);
            }
            else if (ir.isForRejection()) {
                m_buttons.add(ApproveRejectButton);
            }
            else {
                m_buttons.add(ApproveButton);
            }
        }
        else if (ir.isEditable()) {
            m_buttons.add(SaveButton);
        }
        if (ir.getAccess(InvoiceReconciliation.RejectAccess, user) ==
            Access.Now) {
            m_buttons.add(RejectButton);
        }
        if (ir.getAccess(InvoiceReconciliation.CancelRejectAccess, user) ==
            Access.Now) {
            m_buttons.add(CancelRejectButton);
        }
        if (ir.getAccess(InvoiceReconciliation.ReferAccess, user) ==
            Access.Now) {
            m_buttons.add(ReferButton);
        }

            // always allow Manual Match for non-consolidated invoices,
            // even if there are no unmatched exceptions.  This allows
            // the user to override the automatic matching.
        int reconcileAccess = ir.getAccess(Approvable.ReconcileAccess, user);
        if (!ir.getConsolidated() && ir.isEditable() &&
            reconcileAccess == Access.Now) {
            m_buttons.add(ManualMatchButton);
        }

        if (ir.getAccess(Approvable.ChangePartitionAccess,user) ==
            Access.Now) {
            m_buttons.add(ChangePartitionButton);
        }

            // caterpillar specific modifications
        if (isReceivedQtyExp(ir, user)) {

            Log.customer.debug("%s : Adding receive button", "ARPInvoicingWizard");
            m_buttons.add(ReceiveButton);
            Log.customer.debug("%s : Adding receive button", "ARPInvoicingWizard");
        }
            // end of caterpillar specific modifications

            // Calculate Tax Button should only shows up when the user has
            // Tax Manager group and there is a Tax Calculation
            // Failed Exception
        if (reconcileAccess == Access.Now && ir.isEditable()) {
            if (canReconcileTaxCalculationFailedException() &&
                    hasTaxCalculationFailedException()) {
                m_buttons.add(CalculateTaxButton);
            }
        }

        m_buttons.add(PrintButton);

        return m_buttons;
    }

    /*-----------------------------------------------------------------------
        Bindings
      -----------------------------------------------------------------------*/

    public boolean isFullyReconciled ()
    {
        InvoiceReconciliation ir = reconciliation();
        return ir.isReconciled();
    }

    public boolean isForRejection ()
    {
        InvoiceReconciliation ir = reconciliation();
        return ir.isForRejection();
    }

    /**
        If the IR contains more line items that the specific limit
        <code>IRLineItemWarningLimit</code>, then show a one-time warning message.
     */
    public boolean showLargeInvoiceReconciliationWarningMessage ()
    {
        if (showLargeIRWarningMessage) {
            showLargeIRWarningMessage = false;
            InvoiceReconciliation ir = reconciliation();
            return ir.getLineItemsCount() > IRLineItemWarningLimit;
        }
        return false;
    }

    public String largeInvoiceReconciliationMessage ()
    {
        if (m_largeInvoiceReconciliationMessage == null) {
            String fmt = ResourceService.getString(StringTable,
                                                   IRLineItemWarningLimitKey,
                                                   preferredLocale());
            m_largeInvoiceReconciliationMessage = Fmt.Si(fmt, IRLineItemWarningLimit);
        }

        return m_largeInvoiceReconciliationMessage;
    }

    /*-----------------------------------------------------------------------
        Methods
      -----------------------------------------------------------------------*/

    private void setActionDoneMessage (String doneMsg)
    {
        ARBInvoicingContext context = (ARBInvoicingContext)getContext();
        context.setActionDoneMessage(doneMsg);
    }

    private boolean hasTaxCalculationFailedException ()
    {
        InvoiceReconciliation ir = reconciliation();
        List unreconciledExceptions = ir.getAllUnreconciledExceptions();
        for (int i=0; i<unreconciledExceptions.size(); i++) {
            InvoiceException exception =
                    (InvoiceException)unreconciledExceptions.get(i);
            if (exception.getType().getUniqueName()
                    .equals(TaxCalculationFailedException)) {
                return true;
            }
        }
        return false;
    }

    private boolean canReconcileTaxCalculationFailedException ()
    {
        User user = User.getEffectiveUser();
        boolean isTaxManager = InvoiceUtil.isTaxManager(user);
        boolean hasReconcileTCFPermission =
                user.hasPermission(ReconcileInvoiceTaxCalculationFailed);

        return (isTaxManager && hasReconcileTCFPermission);
    }

    /*-----------------------------------------------------------------------
        Actions
      -----------------------------------------------------------------------*/

    public Object saveAction ()
    {
        setActionDoneMessage(ARPDoneReconciling.DoneSaveIR);
        return controller().checkin();
    }

    public Object submitAction ()
    {
            // check for any cancel invoice request exists
        Object cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }

        setActionDoneMessage(ARPDoneReconciling.DoneSubmitIR);
        return controller().submitInvoiceAction();
    }

    public Object approveAction ()
    {
            // check for any cancel invoice request exists
        Object cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }

        setActionDoneMessage(ARPDoneReconciling.DoneApproveIR);
        return controller().approveAction();
    }

    public Object cancelRejectAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionCancelReject);
        Log.invoicingUI.debug("Setting PendingUserAction to CancelReject");
        setActionDoneMessage(ARPDoneReconciling.DoneCancelRejectIR);
        return controller().cancelRejectAction();
    }

    public Object receiveAction()
    {
        Log.customer.debug("%s : receiveAction called", "ARPInvoicingWizard");
        if(reconciliation().getOrder() != null)
        {
            reconciliation().setLocker(null);
            reconciliation().save();
            Log.customer.debug("%s : receiveAction called : Locker -", "ARPInvoicingWizard" + reconciliation().getLocker());

            return ARBReceiptOnClient.receive((BaseUISession)session(), reconciliation().getOrder(), null, false);
        } else
        {
            reconciliation().setLocker(null);
            reconciliation().save();
            Log.customer.debug("%s : receiveAction called : Locker -", "ARPInvoicingWizard" + reconciliation().getLocker());
            return ARBReceiptOnClient.receive((BaseUISession)session(), reconciliation().getMasterAgreement(), null, false);
        }
    }

    public Object rejectAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionReject);
        Log.invoicingUI.debug("Setting PendingUserAction to Reject");
        setActionDoneMessage(ARPDoneReconciling.DoneRejectIR);
        return controller().rejectAction();
    }

    public Object referAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionRefer);
        Log.invoicingUI.debug("Setting PendingUserAction to Refer");
        setActionDoneMessage(ARPDoneReconciling.DoneReferIR);
        return controller().referAction();
    }

    public AWComponent manualMatchAction ()
    {
        ARPPage curPage = (ARPPage)pageComponent();
        InvoiceReconciliation ir = reconciliation();
        if (ir.getMasterAgreement() != null) {
                // match to contract, show contract chooser
            return context().buildContractChooser(
                null, curPage, curPage, (BaseUISession)session());
        }
        else {
            return context().buildOrderChooser(
                null, curPage, curPage, (BaseUISession)session());
        }
    }

    public Object calculateTaxAction ()
    {
        InvoiceReconciliation ir = reconciliation();
            // Reconcile with calculate Tax set to true
        ir.reconcile(true);

            // refresh object
        ir = (InvoiceReconciliation)ir.getBaseId().get();
        context().setApprovable(ir);

        return controller().finishCalculateTaxAction();

    }

    /**
        This method checks if there is any CancelInvoiceRequest existing for the IR.
        If there is, return error page.
    */
    protected Object getCancelInvoiceRequestError ()
    {
        return controller().getCancelInvoiceRequestError();
        /*    checkCancelInvoiceRequest();
        Object result = ((ARBInvoicingContext)getContext()).getErrorOrWarning();
        if (result!=null) {
            ((ARBInvoicingContext)getContext()).undoCheckout();
            //clearPageCache("ARPInvoicingWizard: getCancelInvoiceRequestError()");
            ARPCancelInvoiceRequestError errorPage =
                    (ARPCancelInvoiceRequestError)getSession().pageWithName(
                            ARPCancelInvoiceRequestError.Name);
            errorPage.setupPage(result,
                                forceExitAction());
            return errorPage;
        }
        return null;*/
    }

    /**
    * Prints the InvoiceReconciliation in a separate window
    */
    public AWComponent printAction ()
    {
        InvoiceReconciliation ir = reconciliation();

        Session session = (Session)m_session;

        if (session.isMobile()) {
            ARPMobilePrint mcPrintPg = (ARPMobilePrint)session
                    .pageWithName(ARPMobilePrint.Name);
            mcPrintPg.setBackPage((ARPPage)pageComponent());
            return mcPrintPg;
        }
        else {
            // We need a UI session to lookup branded resources.
            // Currently, ResourceService does not handle branded resources.
            // The Approvable Print facility does not use AW, so we
            // have to do this lookup here ahead of time.
            String brandUrlForCompanyLogo = AWBaseImage.imageUrl((session
                    .requestContext()), null, Fmt.S("images/%s",
                            ApprovableUtil.CompanyLogo));
            Approvable_Print
                    .registerBrandedLogoImageUrl(brandUrlForCompanyLogo);

            String loc = ARBPrintUtil.print(ir);
            AWRedirect page = (AWRedirect)session
                    .pageWithName(AWRedirect.PageName);
            page.setUrl(loc);
            return page;
        }
    }

    public AWComponent changePartitionAction ()
    {
        ARPChangeInvoicePartition changePartitionPage =
                (ARPChangeInvoicePartition)
                session().pageWithName(ARPChangeInvoicePartition.Name);
        changePartitionPage.setApprovable(reconciliation());
        changePartitionPage.setBackPage((ARPPage)pageComponent());
        return changePartitionPage;
    }


    /*-----------------------------------------------------------------------
        Caterpillar Specific Customizations
      -----------------------------------------------------------------------*/

    /**
        Returns true if the following conditions are met:
        - There is a receipt quantity variance in the IR
        - The user is able to receive

        @param ir   the invoice reconciliation
        @param user the logged in user

        @return true if the above conditions are met, false otherwise
    */
    private boolean isReceivedQtyExp (InvoiceReconciliation ir, User user)
    {
        try {
            List exceptions = ir.getAllUnreconciledExceptions();
            ClusterRoot requester = CatSapIRApprovalRulesUtil.getRequester(ir);
            ClusterRoot preparer = CatSapIRApprovalRulesUtil.getPreparer(ir);
            Log.customer.debug(
                    "%s : Requester is : %s", Name, requester);
            if (user != null &&
                    (requester != null &&
                            user == (User)requester ||
                            requester != null && user == (User)preparer))
            {
                Log.customer.debug(
                        "%s : User is same as requester ",
                        Name);
                for (int i = 0; i < exceptions.size(); i++) {

                    InvoiceExceptionType POReceivedQuantityExcp =
                        InvoiceExceptionType.lookupByUniqueName(
                                "POReceivedQuantityVariance",
                                ir.getPartition());
                    InvoiceExceptionType MAReceivedQuantityExcp =
                        InvoiceExceptionType.lookupByUniqueName(
                                "MAReceivedQuantityVariance",
                                ir.getPartition());
                    Log.customer.debug(
                            "%s : POReceivedQuantityExcp : %s",
                            Name,
                            POReceivedQuantityExcp);

                    InvoiceException exception = (InvoiceException)exceptions.get(i);
                    Log.customer.debug(
                            "%s : IR exception is : %s", Name,
                            exception);

                    if (exception != null &&
                            POReceivedQuantityExcp.equals(exception.getType()) ||
                            MAReceivedQuantityExcp.equals(exception.getType()))
                    {
                        Log.customer.debug("%s Returning true", Name);
                        return true;
                    }
                }
                Log.customer.debug("%s : Returning false", Name);
                return false;
            }
            else {
                Log.customer.debug("%s : Returning false", Name);
                return false;
            }
        }
        catch (Exception e) {
            Log.customer.debug(
                    "%s : Returning false : Exception Found ", Name);
        }
        return false;
    }
}
