/*
    Copyright (c) 1996-2004 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/procureui/11.28.1+/ariba/htmlui/procure/invoicing/wizards/ARPInvoicingWizard.java#1 $

    Responsible: lmcconnell
*/

package ariba.htmlui.procure.invoicing.wizards;

import ariba.approvable.core.Access;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.LineItem;
import ariba.base.core.BaseObject;
import ariba.htmlui.approvableui.ARPError;
import ariba.htmlui.approvableui.wizards.ARPApprovableWizard;
import ariba.htmlui.approvableui.wizards.ARPApprovableWizardExit;
import ariba.htmlui.baseui.ARBConfirmPageSource;
import ariba.htmlui.baseui.BaseUISession;
import ariba.htmlui.fieldsui.ARPPage;
import ariba.htmlui.fieldsui.FieldsUISession;
import ariba.htmlui.fieldsui.wizards.ARPWizardFrame;
import ariba.htmlui.procure.Log;
import ariba.htmlui.procure.invoicing.ARPChangeInvoicePartition;
import ariba.htmlui.procure.invoicing.ARPDoneReconciling;
import ariba.htmlui.procure.invoicing.CommandDefinition;
import ariba.htmlui.procure.invoicing.InvoiceCommands;
import ariba.htmlui.procure.invoicing.ARPReconcileInvoices;
import ariba.htmlui.procure.invoicing.ARPCancelInvoiceRequestError;
import ariba.htmlui.procure.receiving.ARBReceiptOnClient;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import java.util.List;
import config.java.invoicing.sap.CatSapIRApprovalRulesUtil;

/**
    Superclass for all modal pages in the invoice wizard that are rendered
    when a user performs an action such as rejecting or forwarding
    the invoice.

    @aribaapi private
*/
public class ARPInvoicingWizard extends ARPApprovableWizard
  implements ARBConfirmPageSource
{

    protected static final String StringTable =
        "ariba.html.invoicing";
    protected static final String StringHintTable =
        "ariba.procureui.hint";
    protected static final String StringHelpTable =
        "ariba.procureui.help";

    private static final String MultipleItemsSelectedKey =
        "MultipleItemsSelected";
    private static final String MustSelectItemKey =
        "MustSelectItem";
    private static final String ExitPageHintKey =
        "WizardConfirmExitHint";
    private static final String ExitPageHowToKey =
        "WizardConfirmExitHowTo";
    private static final String ExitPageScreenDetailsKey =
        "WizardConfirmExitScreenDetails";
    private static final String ExitPageMessageKey =
        "WizardConfirmExitMessage";
    private static final String ExitPageMessageNoTitleKey =
        "WizardConfirmExitMessageNoTitle";
    private static final String ExitPageHelpURL =
        "@ariba.procureui.help/ARPInvoicingWizard_Help";

    public static final CommandDefinition SubmitButton =
        new CommandDefinition("submitAction",
                      "@ariba.html.invoicing/InvoiceSubmit",
                      "@ariba.html.invoicing.alt/InvoiceSubmitTip");

    protected static final CommandDefinition SaveButton =
        new CommandDefinition("saveAction",
                      "@ariba.html.invoicing/InvoiceSave",
                      "@ariba.html.invoicing.alt/InvoiceSaveTip");

    public static final CommandDefinition ApproveButton =
        new CommandDefinition("approveAction",
                      "@ariba.html.invoicing/ApproveInvoice",
                      "@ariba.html.invoicing.alt/ApproveInvoiceTip");

    private static final CommandDefinition RejectButton =
        new CommandDefinition("rejectAction",
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

    private static final CommandDefinition ExitButton =
        new CommandDefinition("exitAction",
                      "@ariba.html.invoicing/InvoiceCancel",
                      "@ariba.html.invoicing.alt/InvoiceCancelTip");

    private static final CommandDefinition DoneButton =
        new CommandDefinition("exitAction",
                      "@ariba.html.invoicing/InvoiceWizardDone",
                      "@ariba.html.invoicing.alt/InvoiceWizardDoneTip");

    private static final CommandDefinition ChangePartitionButton =
        new CommandDefinition("changePartitionAction",
                      "@ariba.html.invoicing/ChangeInvoicePartition",
                      "@ariba.html.invoicing.alt/ChangeInvoicePartitionTip");

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    private static final CommandDefinition ReceiveButton =
    	new CommandDefinition("receiveAction",
    				  "@config.html.invoicing/ReceiveInvoice",
    				  "@config.html.invoicing.alt/ReceiveInvoiceTip");
    // CAT - End of Core Code Hack

    /*-----------------------------------------------------------------------
        Fields
     -----------------------------------------------------------------------*/

    private List m_itemsWithExceptions;
    private List m_headerExceptions;

    private InvoiceReconciliation m_reconciliation;
    private List                m_selection;
    private InvoiceCommands       m_listener;
    private List                m_buttons;

    private ARPPage               m_backPage;
    private List                m_unreconciledExceptions;
    private List                m_items;


    /*-----------------------------------------------------------------------
        Initialization
     -----------------------------------------------------------------------*/

    /**
        Initialize the page and create an invoicing context if
        necessary.
    */
    public void setupPage (List items,
                           ARPPage backPage)
    {
        m_items = items;
        m_unreconciledExceptions = null;
        m_backPage = backPage;
    }

    /**
        Initialize the page and create an invoicing context if
        necessary.
    */
    public void setupPage (List unreconciledExceptions,
                           List lineItems,
                           ARPPage backPage)
    {
        setupPage(lineItems, backPage);
        m_unreconciledExceptions = unreconciledExceptions;
    }

    /**
        Re-fetches the invoice reconciliation and its items from the
        server cache.  This should be called whenever we create/end/cancel
        a nested session.

        @aribaapi private
    */
    protected void refreshObjects ()
    {
        refreshObjects(true);
    }

    protected void refreshObjects (boolean fullRefresh)
    {
        InvoiceReconciliation reconciliation = reconciliation();
        reconciliation = (InvoiceReconciliation)reconciliation.getBaseId().get();
        getInvoicingContext().setApprovable(reconciliation);
        m_itemsWithExceptions = null;
        m_headerExceptions = null;

        if (fullRefresh) {
            List items = items();
            int count = ListUtil.getListSize(items);
            for (int i = 0; i < count; i++) {
                BaseObject obj = (BaseObject)items.get(i);
                obj = reconciliation.findComponent(obj.getBaseId());
                items.set(i, obj);
            }
        }
    }


    /*-----------------------------------------------------------------------
        Overrides of AWComponent
     -----------------------------------------------------------------------*/

    public void sleep ()
    {
        m_itemsWithExceptions = null;
        m_headerExceptions = null;
        super.sleep();
    }

    /**
        Overridden to clear the cached exceptions vector.
    */
    public void appendToResponse (AWRequestContext requestContext,
                                  AWComponent component)
    {
            // Recompute the command button list
        m_buttons = null;
        commandListener().setButtons(getCommandButtons());
        getInvoicingContext().setCurrWizardPage(this);

        super.appendToResponse(requestContext, component);
        getInvoicingContext().setErrorOrWarning(null);
    }

    public ARPPage newPage (FieldsUISession session)
    {
        ARPInvoicingWizard page = (ARPInvoicingWizard)super.newPage(session);
        page.setContext(getContext());
        page.setController(getController());
        page.setupPage(m_unreconciledExceptions, m_items, m_backPage);
        return page;
    }


    /*-----------------------------------------------------------------------
        ARPWizardFrame overrides
      -----------------------------------------------------------------------*/

    public InvoiceCommands commandListener ()
    {
        if (m_listener == null) {
            m_listener = new InvoiceCommands(this);
            m_listener.setButtons(getCommandButtons());
            InvoiceReconciliation ir = reconciliation();
            m_listener.setTarget(ir);
        }

        return m_listener;
    }

    /**
        Overridden to always return the ARPApprovableWizardExit
        page.  We need to do this because there's no ARBWizardFrame
        object associated with this page.
    */
    public String exitPageName ()
    {
        return ARPApprovableWizardExit.Name;
    }

    /**
        Need to override this because there is no ARBWizardFrame in
        the invoicing wizard.
    */
    public String pageHelp ()
    {
            // This will steer ARCTopBanner to retrieve
            // the help URL from the Help binding.
        return null;
    }

    /*-----------------------------------------------------------------------
        Binding names
     -----------------------------------------------------------------------*/

    /**
        Returns the list of button definitinos that should be
        rendered in the button row for this page.

        Overridden by subclasses to define a different set of buttons.
    */
    protected List getCommandButtons ()
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
        if (!ir.getConsolidated() && ir.isEditable()) {
            m_buttons.add(ManualMatchButton);
        }

        if (ir.getAccess(Approvable.ChangePartitionAccess,user) ==
            Access.Now) {
            m_buttons.add(ChangePartitionButton);
        }

        // ARajendren Ariba, Inc.,
        // 9R1 Upgrade, Added CAT core code customizations.
        // CAT - Core Code Hack Starts
        if(isReceivedQtyExp(ir, user))
        {
            Log.customer.debug("%s : Adding receive button", "ARPInvoicingWizard");
            m_buttons.add(ReceiveButton);
            Log.customer.debug("%s : Adding receive button", "ARPInvoicingWizard");
        }
        // CAT - End of Core Code Hack

        if (ir.isEditable()) {
            m_buttons.add(ExitButton);
        }
        else {
            m_buttons.add(DoneButton);
        }

        return m_buttons;
    }

    public ARPPage backPage ()
    {
        return m_backPage;
    }

    /**
        Returns true if we should prompt the user before exiting the
        reconciliation UI.
    */
    public boolean confirmExit ()
    {
            // xxx jpasalis only return true if there's a diff
        return true;
    }

    public List items ()
    {
        return m_items;
    }

    /**
        Returns true if the current set of invoice reconciliation
        line items is non-empty.
    */
    public boolean hasItems ()
    {
        return !ListUtil.nullOrEmptyList(m_items);
    }

    public List unreconciledExceptions ()
    {
        return m_unreconciledExceptions;
    }

    public InvoiceReconciliation reconciliation ()
    {
        return getInvoicingContext().getReconciliation();
    }

    public boolean hasUnreconciledExceptions ()
    {
        return !ListUtil.nullOrEmptyList(unreconciledExceptions());
    }

    public List headerExceptions ()
    {
        if (m_headerExceptions == null) {
            InvoiceReconciliation ir = reconciliation();
            m_headerExceptions = ir.getHeaderExceptionsForCurrentUser();
        }
        return m_headerExceptions;
    }

    /**
        Returns true if we should display header exceptions in a separate table.
    */
    public boolean hasHeaderExceptions ()
    {
        return !ListUtil.nullOrEmptyList(headerExceptions());
    }

    public List itemsWithExceptions ()
    {
        if (m_itemsWithExceptions == null) {
            InvoiceReconciliation ir = reconciliation();
            User user = User.getEffectiveUser();
            m_itemsWithExceptions =
                ir.getItemsWithExceptions(
                    user, InvoiceException.AnyStates);
        }
        return m_itemsWithExceptions;
    }

    public String pageHint ()
    {
        return frame().pageHint();
    }

    public String pageHowTo ()
    {
        return frame().howTo();
    }

    public String pageScreenDetails ()
    {
        return frame().screenDetails();
    }

    public List selection ()
    {
        return m_selection;
    }

    public void setSelection (List selection)
    {
        m_selection = selection;
    }

    /**
        Returns the currently selected line item.  Asserts if there's either
        zero or more than one selected.  If the selected item is not a
        LineItem, then we return null.
    */
    protected LineItem selectedLineItem ()
    {
        List selection = selection();
        Assert.that(selection != null,
                    "Button action invoked with a null selection");
        Assert.that(selection.size() == 1,
                    "More than one item selected for adding a comment");

        Object item = ListUtil.firstElement(selection);
        if (item instanceof LineItem) {
            return (LineItem)item;
        }

        return null;
    }

    /**
        Checks if there is one and only one item in the selection and,
        if not, sets an error message into the context.

        Returns true if the selection is valid.
    */
    protected boolean verifySingleSelection (List selection)
    {
        if (!verifySelection(selection)) {
            return false;
        }

        boolean justOne = (selection.size() == 1);
        if (!justOne) {
            setErrorMessage(MultipleItemsSelectedKey);
        }

        return justOne;
    }

    /**
        Checks if the selection vector is non-empty and, if not, sets
        an error message into the context.

        Returns true if the selection is valid.
    */
    protected boolean verifySelection (List selection)
    {
        if (ListUtil.nullOrEmptyList(selection)) {
            setErrorMessage(MustSelectItemKey);
            return false;
        }

        return true;
    }

    /**
        Sets the context's error message to be the localized
        string represented by the 'key' string id.
    */
    public void setErrorMessage (String key)
    {
        getInvoicingContext().setErrorOrWarning(
            ResourceService.getString(StringTable, key));
    }

    protected ARBInvoicingContext getInvoicingContext ()
    {
        return (ARBInvoicingContext)getContext();
    }

    public ARBInvoicingController getInvoicingController ()
    {
        return (ARBInvoicingController)getController();
    }


    /*-----------------------------------------------------------------------
        Actions
     -----------------------------------------------------------------------*/

    public AWComponent saveAction ()
    {
        setActionDoneMessage(ARPDoneReconciling.DoneSaveIR);
        Object nextPage = getInvoicingController().checkin();
        return processNextPage(nextPage);
    }

    public AWComponent submitAction ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }

        setActionDoneMessage(ARPDoneReconciling.DoneSubmitIR);
        return processNextPage(
                getInvoicingController().submitInvoiceAction());
    }

    public AWComponent approveAction ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }

        setActionDoneMessage(ARPDoneReconciling.DoneApproveIR);
        return processNextPage(
                getInvoicingController().approveAction());
    }

    public AWComponent cancelRejectAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionCancelReject);
        Log.invoicingUI.debug("Setting PendingUserAction to CancelReject");
        setActionDoneMessage(ARPDoneReconciling.DoneCancelRejectIR);
        return processNextPage(
                getInvoicingController().cancelRejectAction());
    }

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    public AWComponent receiveAction()
    {
        Log.customer.debug("%s : receiveAction called", "ARPInvoicingWizard");
        if(reconciliation().getOrder() != null)
        {
            reconciliation().setLocker(null);
            reconciliation().save();
            Log.customer.debug("%s : receiveAction called : Locker -", "ARPInvoicingWizard" + reconciliation().getLocker());
            return ARBReceiptOnClient.receive((BaseUISession)session(), reconciliation().getOrder(), backPage(), false);
        } else
        {
            reconciliation().setLocker(null);
            reconciliation().save();
            Log.customer.debug("%s : receiveAction called : Locker -", "ARPInvoicingWizard" + reconciliation().getLocker());
            return ARBReceiptOnClient.receive((BaseUISession)session(), reconciliation().getMasterAgreement(), backPage(), false);
        }
    }
    // CAT - End of Core Code Hack

    public AWComponent rejectAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionReject);
        Log.invoicingUI.debug("Setting PendingUserAction to Reject");
        setActionDoneMessage(ARPDoneReconciling.DoneRejectIR);
        return processNextPage(
                getInvoicingController().rejectAction());
    }

    public AWComponent referAction ()
    {
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionRefer);
        Log.invoicingUI.debug("Setting PendingUserAction to Refer");
        setActionDoneMessage(ARPDoneReconciling.DoneReferIR);
        return processNextPage(
                getInvoicingController().referAction());
    }

    public AWComponent manualMatchAction ()
    {
        ARPPage curPage = (ARPPage)pageComponent();
        InvoiceReconciliation ir = reconciliation();
        if (ir.getMasterAgreement() != null) {
                // match to contract, show contract chooser
            return getInvoicingContext().buildContractChooser(
                null, curPage, curPage, (BaseUISession)session());
        }
        else {
            return getInvoicingContext().buildOrderChooser(
                null, curPage, curPage, (BaseUISession)session());
        }
    }

    /**
        Returns the exit page if the user has modified the reconciliation.
        Otherwise it just cleans up and returns the back page.
    */
    public AWComponent exitAction ()
    {
        if (!confirmExit()) {
            getContext().cleanup(false);
            return pageAction(getInvoicingController().getCancelPage());
        }

        AWComponent page = processNextPage(exitPageName());
        Assert.that(page instanceof ARPApprovableWizardExit,
                    "Confirm exit page must be of type ARPApprovableWizardExit");

        ARPApprovableWizardExit exitPage = (ARPApprovableWizardExit)page;
        exitPage.setBackPage(this);
        exitPage.setConfirmMessage(exitPageConfirmMessage());
        exitPage.setHintMessage(exitPageHintMessage());
        exitPage.setHowToMessage(exitPageHowToMessage());
        exitPage.setScreenDetailsMessage(exitPageScreenDetailsMessage());
        exitPage.setHelpPage(ExitPageHelpURL);
        return exitPage;
    }

    public AWComponent forceExitAction ()
    {
        return (ARPPage)session().pageWithName(ARPReconcileInvoices.Name);
    }

    public AWComponent changePartitionAction ()
   {
        ARPChangeInvoicePartition changePartitionPage =
                (ARPChangeInvoicePartition)
                session().pageWithName(ARPChangeInvoicePartition.Name);
        changePartitionPage.setApprovable(reconciliation());
        changePartitionPage.setBackPage(this);
        return changePartitionPage;
    }

    private String exitPageHintMessage ()
    {
        String hint = ResourceService.getString(StringHintTable,
                                                ExitPageHintKey,
                                                preferredLocale());
        return hint;
    }

    private String exitPageHowToMessage ()
    {
        String howTo = ResourceService.getString(StringHelpTable,
                                                ExitPageHowToKey,
                                                preferredLocale());

        return howTo;
    }


    private String exitPageScreenDetailsMessage ()
    {
        String screenDetails = ResourceService.getString(StringHelpTable,
                                                ExitPageScreenDetailsKey,
                                                preferredLocale());

        return screenDetails;
    }


    private String exitPageConfirmMessage ()
    {
        InvoiceReconciliation reconciliation = reconciliation();
        String id = reconciliation.getUniqueId();
        String title = reconciliation.getName();
        String key = (title == null)
            ?  ExitPageMessageNoTitleKey : ExitPageMessageKey;
        String msg = Fmt.Sil(preferredLocale(), StringTable, key, id, title);

        return msg;
    }


    public AWComponent saveAndApprove ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }
        setActionDoneMessage(ARPDoneReconciling.DoneSaveAndApproveIR);
        Object nextPage = null;

        nextPage = getInvoicingController().approveOrSave(
                !reconciliation().isForRejection());

        return processNextPage(nextPage);
    }

    public AWComponent saveAndReject ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError = getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionReject);
        Log.invoicingUI.debug("Setting PendingUserAction to Reject");
        setActionDoneMessage(ARPDoneReconciling.DoneSaveAndRejectIR);
        Object nextPage = getInvoicingController().saveAndReject();
        return processNextPage(nextPage);
    }

    public AWComponent saveAndCancelReject ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionCancelReject);
        Log.invoicingUI.debug("Setting PendingUserAction to CancelReject");
        setActionDoneMessage(ARPDoneReconciling.DoneSaveAndCancelRejectIR);
        Object nextPage = getInvoicingController().saveAndCancelReject();
        return processNextPage(nextPage);
    }

    public AWComponent saveAndRefer ()
    {
            // check for any cancel invoice request exists
        AWComponent cancelInvoiceRequestError =
                    getCancelInvoiceRequestError();
        if (cancelInvoiceRequestError != null) {
            return cancelInvoiceRequestError;
        }
        reconciliation().setPendingUserAction(
            InvoiceReconciliation.UserActionRefer);
        Log.invoicingUI.debug("Setting PendingUserAction to Refer");
        setActionDoneMessage(ARPDoneReconciling.DoneSaveAndReferIR);
        Object nextPage = getInvoicingController().saveAndRefer();
        return processNextPage(nextPage);
    }

    /**
        Returns either the error page or the done page
        depending upon whether the nextPage parameter is
        an error page or null.

        This quirky method is needed since we're not really
        in a wizard with a tree of frames that we're navigating
        around...
    */
    protected AWComponent processNextPage (Object nextPage)
    {
            // We return our own error page since we're not really in a wizard
            // with an error frame.
        if (isErrorPage(nextPage)) {
            return showErrorPage();
        }

            // This case typically handles invoking confirmation pages
            // for submit, reject, etc.
        else if (nextPage instanceof String) {
            AWComponent page = pageWithName((String)nextPage);
            if (page instanceof ARPInvoicingWizard) {
                ((ARPInvoicingWizard)page).setupPage(null, this);
            }
            return page;
        }

            // When we want to go back to a previous page like
            // the "main page"...
        else if (nextPage instanceof AWComponent) {
            return (AWComponent)nextPage;
        }

            // If there is no next page, we assume we're done...
        else if (nextPage == null) {
            return showDonePage();
        }

        Assert.that(false, "Next page is invalid: %s",
                    nextPage);
        return null;
    }

    /**
        Returns the done page.
    */
    protected AWComponent showDonePage ()
    {
            // We need to do a pageWithName from the session object since this
            // might be called via an ARPError page callback and this page may
            // no longer be in the cache.
        Object endPage = endPage();
        ARPDoneReconciling page;
        if (endPage instanceof String) {
            BaseUISession session = (BaseUISession)session();
            page = (ARPDoneReconciling)session.pageWithName((String)endPage);
        }
        else {
            Assert.that(endPage instanceof ARPDoneReconciling,
                        "Invoicing done page must subclass from ARPDoneReconciling");
            page = (ARPDoneReconciling)endPage;
        }

        ARBInvoicingContext context = (ARBInvoicingContext)getContext();
        InvoiceReconciliation ir = reconciliation();
        page.setApprovable(ir);
        page.setEditing(context.isEditing());
        page.setIsApproving(!ir.isStatusReconciling());
        return page;
    }

    private void setActionDoneMessage (String doneMsg)
    {
        ARBInvoicingContext context = (ARBInvoicingContext)getContext();
        context.setActionDoneMessage(doneMsg);
    }

    /**
        Returns the ARPError page populated with the given
        error or warning object.
    */
    protected AWComponent showErrorPage ()
    {
        ARPError errorPage = (ARPError)getSession().pageWithName(ARPError.Name);
        ARBInvoicingContext context = (ARBInvoicingContext)getContext();

            // Showing invalid fields will prevent the user from continuing,
            // so we don't want to display invalid fields if we're
            // only invoking this page to display a warning.
            // xxx jpasalis This isn't robust.  We should really have a state
            // in the wizard context that tells us to ignore validation errors.
        List invalidFields = null;
        if (context.hasError() || !context.hasWarning()) {
            invalidFields = context.getInvalidFieldsList();
        }
        errorPage.setupPage(context.getErrorOrWarning(), invalidFields,
                            this, null);
        return errorPage;
    }

    /**
        Returns true if the given nextPage object is the
        error page.
    */
    protected boolean isErrorPage (Object page)
    {
        return (page != null &&
                getInvoicingController().getErrorFrame().equals(page));
    }

    /**
        Marks the exceptions on this page as CannotResolve and
        refers them to the specified role (if one was provided by
        the user).
    */
    protected void markAndReferExceptions ()
    {
            // Refer all header-level exceptions to the
            // specified role
        ARBInvoicingContext context = getInvoicingContext();
        context.markAndReferExceptions(unreconciledExceptions(), items());
    }

    /**
        This method check if there is any CancelInvoiceRequest exist for the IR.
        If there is, return error page.
    */
    protected AWComponent getCancelInvoiceRequestError ()
    {
        getInvoicingController().checkCancelInvoiceRequest();
        Object result = ((ARBInvoicingContext)getContext()).getErrorOrWarning();
        if (result!=null) {
            ((ARBInvoicingContext)getContext()).undoCheckout();
            clearPageCache("ARPInvoicingWizard: getCancelInvoiceRequestError()");
            ARPCancelInvoiceRequestError errorPage =
                    (ARPCancelInvoiceRequestError)getSession().pageWithName(
                            ARPCancelInvoiceRequestError.Name);
            errorPage.setupPage(result,
                                forceExitAction());
            return errorPage;
        }
        return null;
    }
    /*-----------------------------------------------------------------------
        Implementation of ARBConfirmPageSource (used by error page)
     -----------------------------------------------------------------------*/

    /**
        Asks the controller to handle going forward another step with
        the current user action.
    */
    public AWComponent confirmOKAction (Object object,
                                        String source,
                                        BaseUISession session)
    {
        Object nextPage = getInvoicingController().ignoreWarning();
        return processNextPage(nextPage);
    }

    /**
        Returns the user back to this page and cancels the action.
    */
    public AWComponent confirmCancelAction (Object object,
                                            String source,
                                            BaseUISession session)
    {
            // Make sure we are still in an editing state upon
            // returning from the error page.
        getInvoicingContext().editIR();
        return this;
    }

    // ARajendren Ariba, Inc.,
    // 9R1 Upgrade, Added CAT core code customizations.
    // CAT - Core Code Hack Starts
    private boolean isReceivedQtyExp(InvoiceReconciliation ir, User user)
    {
        try
        {
            List exceptions = ir.getAllUnreconciledExceptions();
            ariba.base.core.ClusterRoot requester = CatSapIRApprovalRulesUtil.getRequester(ir);
            ariba.base.core.ClusterRoot preparer = CatSapIRApprovalRulesUtil.getPreparer(ir);
            Log.customer.debug("%s : Requester is : %s", "ARPInvoicingWizard", requester);
            if(user != null && (requester != null && user == (User)requester || requester != null && user == (User)preparer))
            {
                Log.customer.debug("%s : User is same as requester ", "ARPInvoicingWizard");
                for(int i = 0; i < exceptions.size(); i++)
                {
                    InvoiceExceptionType POReceivedQuantityExcp = InvoiceExceptionType.lookupByUniqueName("POReceivedQuantityVariance", ir.getPartition());
                    InvoiceExceptionType MAReceivedQuantityExcp = InvoiceExceptionType.lookupByUniqueName("MAReceivedQuantityVariance", ir.getPartition());
                    Log.customer.debug("%s : POReceivedQuantityExcp : %s", "ARPInvoicingWizard", POReceivedQuantityExcp);
                    InvoiceException exception = (InvoiceException)exceptions.get(i);
                    Log.customer.debug("%s : IR exception is : %s", "ARPInvoicingWizard", exception);
                    if(exception != null && POReceivedQuantityExcp.equals(exception.getType()) || MAReceivedQuantityExcp.equals(exception.getType()))
                    {
                        Log.customer.debug("%s Returning true", "ARPInvoicingWizard");
                        return true;
                    }
                }
                Log.customer.debug("%s : Returning false", "ARPInvoicingWizard");
                return false;
            } else
            {
                Log.customer.debug("%s : Returning false", "ARPInvoicingWizard");
                return false;
            }
        }
        catch(Exception e)
        {
            Log.customer.debug("%s : Returning false : Exception Found ", "ARPInvoicingWizard");
        }
        return false;
    }
    // CAT - End of Core Code Hack
}
