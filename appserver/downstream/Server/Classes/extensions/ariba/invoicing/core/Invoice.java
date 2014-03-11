/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/release/invoicing/6.28.1+/ariba/invoicing/core/Invoice.java#17 $

    Responsible: adolgachev
*/

package ariba.invoicing.core;

import ariba.app.util.SystemParameters;
import ariba.approvable.core.Access;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.ApprovableUtil;
import ariba.approvable.core.Comment;
import ariba.approvable.core.LineItem;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Clusterable;
import ariba.base.core.ForcingVectorIterator;
import ariba.base.core.LogEntry;
import ariba.base.core.Partition;
import ariba.base.core.LocalizedString;
import ariba.base.core.aql.AQLClassExpression;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLFunctionCall;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Trigger;
import ariba.base.fields.Variant;
import ariba.basic.core.Money;
import ariba.basic.core.SearchExpression;
import ariba.common.core.CoreUtil;
import ariba.contract.core.ContractSelectable;
import ariba.payment.core.PaymentCoreApprovable;
import ariba.procure.core.CategoryLineItemDetails;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.Milestone;
import ariba.procure.core.MilestoneItemization;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.server.workflowserver.WorkflowService;
import ariba.statement.core.Cancelable;
import ariba.statement.core.SimpleSCARecord;
import ariba.statement.core.StatementOrderInfo;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.LRUHashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
    Instances of this class represent Invoice documents
*/
public class Invoice extends InvoiceBase implements ContractSelectable, Cancelable
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    /**
        Base string for all invoicing application parameters.

        @aribaapi documented
    */
    public static final String ParameterInvoicingBase =
        "Application.Invoicing.";

    public static final String ParameterInvoiceEntryAllowOverInvoicing =
        ParameterInvoicingBase + "InvoiceEntryAllowOverInvoicing";
    /**
        Base string for all invoicing system parameters.

        @aribaapi documented
    */
    public static final String ParameterSystemInvoicingBase =
        "System.Invoicing.";

    /**
        Parameter to indicate whether Invoicing is enabled on the partition.

        @aribaapi documented
    */
    public static final String ParameterInvoicingEnabled =
        ParameterInvoicingBase + "Enabled";

    public static String SendIRParameter =
        ParameterSystemInvoicingBase + "SendInvoiceReconciliationToNetwork";

    private static final String InvoicingModuleName =
        SystemParameters.ParameterModuleInvoicing;

    /**
        This permission allows the holder to "administrate" invoices.

        This provides the holder with full visibility into invoices and
        invoice reconciliations in any state.

        @aribaapi documented
    */
    public static final String PermissionAdministrator = "InvoiceAdministrator";

    /**
        This permission allows the holder to "manage" invoices.

        This provides the holder with special views on invoices and
        invoice reconciliations and the ability to edit invoice details.

        @aribaapi documented
    */
    public static final String PermissionManager = "InvoiceManager";


    /**
        This permission allows the holder to "create" invoices.

        This provides the holder with special the ability to create invoices.

        @aribaapi documented
    */
    public static final String PermissionCreate = "CreateInvoice";

    /**
        This permission allows the holder to force reconcile against an invoice, so that
        this invoice goes to reconciling state and IR got generated.

        @aribaapi documented
    */
    public static final String PermissionForceReconcile = "ForceReconcileInvoice";

    /**
        This permission allows the holder to see all invoices.

        @aribaapi private
     */
    public static final String PermissionQueryAllInvoice = "QueryAllInvoice";

    /**
        This permission allows the holder to see invoices.

        @aribaapi private
     */
    public static final String PermissionQueryMyInvoice = "QueryMyInvoice";

    /**
        Constant denoting that the purpose of the invoice is a
        standard billing statement from the supplier to the buyer.

        @aribaapi documented
    */
    public static final String PurposeStandard = "standard";

    /**
    Constant denoting that the purpose of the invoice is a
    credit memo for giving credit back to the buyer by the supplier.

    @aribaapi documented
    */
    public static final String PurposeCreditMemo = "creditMemo";

    /**
        Constant denoting that the purpose of the invoice is a line item level credit memo 
        for giving credit back to the buyer by the supplier.

        @aribaapi documented
    */
    public static final String PurposeLineLevelCreditMemo = "lineLevelCreditMemo";

    /**
       Constant denoting that the purpose of the invoice is a
        debit memo for billing a balance owed by the buyer to the supplier.

        @aribaapi documented
    */
    public static final String PurposeDebitMemo = "debitMemo";

    private static final String StringTable = "ariba.invoicing.core";

    public static final String EventCanceled       = "Invoice:Canceled";

    public static final String EventCCInvoiceSent = "Invoice:CCInvoiceSent";

    private static final String StateCancelingString = "Canceling";

    private static final String InvalidNumberOfLinesInInvoiceMsgKey =
                                        "InvalidNumberOfLinesInInvoiceMsg";

    private Boolean m_hasShippingServiceItem = null;
    private Boolean m_hasHandlingServiceItem = null;

    public static final String InvoicePDAAdditionalSubjectKey =
            "InvoicePDAAdditionalSubject";

    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Create a new Invoice.

        @aribaapi private
    */
    public Invoice ()
    {
    }

    /**
        Create a new invoice based on Partition

        @aribaapi private
    */
    public Invoice (Partition partition)
    {
        this.init(partition);
    }

    /**
        Initialize an Invoice by setting some defaults, marking it Loaded
        and firing the creation triggers.

        @aribaapi private
    */
    public void init (Partition partition)
    {
        super.init(partition);

            // Initialize the default values
        setConsolidated(true);
        setInvoicePurpose(PurposeStandard);

            // fire creation defaulting trigger
        if (getClassName().equals(ClassName)) {
            fireTriggers(Trigger.Create, null);
        }
    }

    protected Clusterable postCopy (Clusterable copy, boolean strip)
    {
        Invoice theCopy = (Invoice)copy;
        theCopy.m_hasShippingServiceItem = null;
        theCopy.m_hasHandlingServiceItem = null;
        return super.postCopy(copy, strip);
    }

    protected void postDuplicate (BaseObject original)
    {
        m_hasShippingServiceItem = null;
        m_hasHandlingServiceItem = null;
        super.postDuplicate(original);
    }

    public void initCopy ()
    {
        m_hasShippingServiceItem = null;
        m_hasHandlingServiceItem = null;
        super.initCopy();
    }

    /**
        Indicate that we are firing the Defaulting trigger

        @aribaapi private
    */
    protected boolean isDefaultingCreateTriggerFiredManually ()
    {
        return true;
    }

    /*-----------------------------------------------------------------------
        Overrides
      -----------------------------------------------------------------------*/

    public boolean setDefaultsFromMatchingCollection (ReceivableLineItemCollection oldLIC,
                                                      ReceivableLineItemCollection newLIC,
                                                      String fieldName)
    {
            // If no defaulting done by super, then do nothing here either
        if (!super.setDefaultsFromMatchingCollection(oldLIC, newLIC, fieldName)) {
            return false;
        }

            // Otherwise default the collections on the IRs
        ForcingVectorIterator irs
            = new ForcingVectorIterator(getInvoiceReconciliations());
        while (irs.hasNext()) {
            InvoiceReconciliation ir =
                (InvoiceReconciliation)irs.next();

                // Only update the IR if it is not already approved
            if (!ir.isApproved()) {
                ir.setFieldValue(fieldName, newLIC);
            }
        }

            // Then notify collection that an invoice was matched to it
        if (newLIC != null) {
            newLIC.onMatch(this);
        }

        return true;
    }

    /*-----------------------------------------------------------------------
        Query Methods
      -----------------------------------------------------------------------*/
    /**
        Looks up an invoice using the PayloadID field.

        @param payloadID payload id of invoice to be found.
        @param andCondition an extra AQLCondition to be AND'd with the query.
        @return the Invoice if one and only one exists, otherwise null.

        @aribaapi documented
    */
    public static Invoice lookupByPayloadID (String payloadID,
                                             AQLCondition andCondition)
    {
        AQLClassReference invoiceCR
            = new AQLClassReference(Invoice.ClassName,
                                    false,
                                    Partition.AnyVector);

        return (Invoice)lookupByUniqueField(invoiceCR,
                                            KeyPayloadID,
                                            payloadID,
                                            andCondition);
    }

    /*-----------------------------------------------------------------------
        Utility Methods
      -----------------------------------------------------------------------*/

    /**
        Returns true if invoicing is enabled.

        @return true if the invoicing module is enabled.

        @aribaapi private
    */
    public static boolean isInvoicingEnabled (Partition partition)
    {
            // If both P2P and SSI feature permissions are off -> no invoicing
        if (!CoreUtil.isPermissionActiveInRealm(CoreUtil.SSIFeaturePermission) &&
            !CoreUtil.isProcurementEnabledInRealm())
        {
            return false;
        }

            // Handle the CD scenario
        if (!CoreUtil.isPermissionActiveInRealm(PermissionCreate)) {
            return false;
        }

        return (ApprovableUtil.isModuleInstalled(InvoicingModuleName) &&
                Base.getService().getBooleanParameter(partition,
                                                      ParameterInvoicingEnabled));
    }

    /**
        Checks whether the specified user has the InvoiceManager permission.

        @param user the user to check.

        @return true if user has the InvoiceManager permission.

        @aribaapi documented
    */
    public static boolean isInvoiceManager (User user)
    {
            // Just return false if the user is null
        if (user == null) {
            return false;
        }

            // Get the permission and check whether the user has it
        return user.hasPermission(PermissionManager);
    }


    /**
        Checks whether the specified user has the InvoiceAdministrator permission.

        @param user the user to check.

        @return true if user has the InvoiceAdministrator permission.

        @aribaapi documented
    */
    public static boolean isInvoiceAdministrator (User user)
    {
            // Just return false if the user is null
        if (user == null) {
            return false;
        }

            // Get the permission and check whether the user has it
        return user.hasPermission(PermissionAdministrator);
    }

    protected String getReconciliationMethodParameter ()
    {
        return InvoiceReconciliation.ParameterReconciliationMethod;
    }


    /**
        Returns the derived status string. While the IR is in the submitted
        state, the status shown is is reconciling until it has been fully
        reconciled, and then approving.  After it is approved and starts
        processing, the status shown is the processed state.

        @aribaapi private
    */
    public String getDerivedStatus ()
    {
        if (isCanceling()) {
            return StateCancelingString;
        }

        return super.getDerivedStatus();
    }

    /**
        Returns true if the Invoice has non-approved CancelInvoiceRequest attached.

        @return whether Invoice has CancelInvoiceRequest attached.

        @aribaapi private
    */
    public boolean isCanceling ()
    {
        CancelInvoiceRequest cir = getCancelInvoiceRequest();
        return ( (cir != null) && (!cir.isApproved()) && (!cir.isDenied()));
    }

    /**
        Returns true if the Invoice can be canceled

        @return whether the Invoice can be canceled

        @aribaapi private
    */
    public boolean isCancelable ()
    {
        if (isCanceling() || isCanceled()) {
            Log.invoicing.warning(8434, this,
                    getCancelInvoiceRequest());
            return false;
        }

        if (!isReconciling()) {
            Log.invoicing.warning(8468, this);
            return false;
        }

        List recIds =  getInvoiceReconciliations();
        int recSize = ListUtil.getListSize(recIds);
        for (int i=0; i<recSize; i++) {
            BaseId irId = (BaseId)recIds.get(i);
            InvoiceReconciliation ir = (InvoiceReconciliation)(irId.getIfAny());
            if (ir.getApprovedState() >= InvoiceReconciliation.StateApproved) {
                Log.invoicing.warning(8352, ir, this);
                return false;
            }
        }
        Log.invoicing.debug(
                "%s: Invoice %s is cancellable.", ClassName, this);

        return true;
    }

    /**
        Returns true if all the Invoice Reconciliations have been approved

        @return whether all the Invoice Reconciliations for a given Invoice
        have been approved.

        @aribaapi private
    */
    public boolean areAllInvoiceReconciliationsApproved ()
    {
            // XXX AD - change to use the line item enumerator
        ForcingVectorIterator irs
            = new ForcingVectorIterator(getInvoiceReconciliations());

        while (irs.hasNext()) {
            InvoiceReconciliation ir =
                (InvoiceReconciliation)irs.next();
            if (!ir.isApproved()) {
                return false;
            }
        }

        return true;
    }

    /**
        @return the first InvoiceReconciliation for this Invoice,
        if it exists.

        @aribaapi documented
    */
    public InvoiceReconciliation getFirstInvoiceReconciliation ()
    {
        List ids = getInvoiceReconciliations();
        BaseId id = (BaseId)ListUtil.firstElement(ids);
        InvoiceReconciliation ir = null;

        if (id != null) {
            ir = (InvoiceReconciliation)id.getIfAny();
        }

        return ir;
    }

    /**
        Returns whether this is a standard invoice.

        @return whether this is a standard invoice.

        @aribaapi documented
    */
    public boolean isStandardInvoice ()
    {
        return PurposeStandard.equals(getInvoicePurpose());
    }

    public void setInvoicePurpose (String purpose)
    {
        super.setInvoicePurpose(purpose);
        if (PurposeCreditMemo.equals(purpose) && !hasPO() && !hasContract()) {
           setCategory(Invoice.CategoryNonAriba);
        }
    }

    /**
        Returns whether this is a credit memo.

        @return whether this is a credit memo.

        @aribaapi documented
    */
    public boolean isCreditMemo ()
    {
        return PurposeCreditMemo.equals(getInvoicePurpose());
    }

    public boolean isNonPO ()
    {
        return this.getIsNonPO();
    }

    /**
        Returns whether this is a debit memo.

        @return whether this is a debit memo.

        @aribaapi documented
    */
    public boolean isDebitMemo ()
    {
        return PurposeDebitMemo.equals(getInvoicePurpose());
    }

    /**
        Fires the Cancelled event
    */
    public void fireCanceledEvent ()
    {
        WorkflowService.getService().fireWorkflowEvent(this,
                                                       EventCanceled);
    }

    /**
        Fires the CCInvoiceSent event
     */
    public void fireCCInvoiceSentEvent ()
    {
        WorkflowService.getService().fireWorkflowEvent(this,
                                                       Invoice.EventCCInvoiceSent);
    }
    /*-----------------------------------------------------------------------
        Creation and Defaulting Methods
      -----------------------------------------------------------------------*/

    /**
        Create an Invoice, using the specified user as the requester and preparer.

        @param partition the partition to create the Invoice in.
        @param user The user to default the Invoice from, which must be non-null.

        @return the new Invoice

        @aribaapi documented
    */
    public static Invoice create (Partition partition,
                                  User user)
    {
        return create(partition, user, LoadedFromUnknownSource);
    }

    /**
        Create an Invoice, using the specified user as the requester and preparer.

        @param partition the partition to create the Invoice in.
        @param user The user to default the Invoice from, which must be non-null.
        @param loadedFrom Where the invoice is loaded from.

        @return the new Invoice

        @aribaapi documented
    */
    public static Invoice create (Partition partition,
                                  User user,
                                  int loadedFrom)
    {
        Assert.that(user != null,
                    "%s.create: user must be non-null",
                    ClassName);

        Invoice invoice = (Invoice)BaseObject.create(ClassName,
                                                     partition);

            // Default the preparer and requester
        invoice.setRequester(user);
        invoice.setPreparer(user);

            // Set the loaded from field (has to be set before save)
        invoice.setLoadedFrom(loadedFrom);

        invoice.save();

        return invoice;

    }

    /**
        Sets up payment requests when the invoice reconciliations are created.

        Called from the workflow through the setupPaymentRequests() action.

        @aribaapi documented
    */
    public void setupPaymentRequests ()
    {
        Log.invoicing.debug("%s.setupPaymentRequests called with %s",
                            ClassName,
                            getUniqueName());

        Iterator irs = getInvoiceReconciliationsIterator();
        while (irs.hasNext()) {
            BaseId irId = (BaseId)irs.next();
            InvoiceReconciliation ir = (InvoiceReconciliation)irId.get();
            ir.setupPaymentRequests();
        }
    }

    /**
        Return the Invoice for an uniqueName.

        @param uniqueName the uniquename to use for the lookup.

        @return the matching Invoice (if 1 and only 1 exists).

        @aribaapi documented
    */
    public static Invoice lookupByUniqueName (String uniqueName)
    {
        AQLClassReference ref = new AQLClassReference(Invoice.ClassName,
                                                      false,
                                                      Partition.AnyVector);
        AQLQuery query =
            new AQLQuery(ref);
        query.andEqual(Invoice.KeyUniqueName, uniqueName);

            // Get the matching CRs
        AQLOptions options = new AQLOptions();
        query.setDistinct(true);
        AQLResultCollection results =
            Base.getService().executeQuery(query, options);

        if (results.getSize() > 0) {
                // Return the invoice
            results.next();
            BaseId id = results.getBaseId(0);
            return (Invoice)Base.getSession().objectFromId(id);
        }
        return null;

    }


    // xxx AD Make this into non-static and move up
    public static void inactivateInvoice (Invoice invoice)
    {
            // cleanup
        Iterator irs = new ForcingVectorIterator(invoice.getInvoiceReconciliations());
        while (irs.hasNext()) {

            InvoiceReconciliation ir = (InvoiceReconciliation)irs.next();

                // revert all accumulators for the invoice
            ir.getAccumulator().resetAccumulatorsInvoiced(ir, null, true);

                // delete payment objects
            Iterator payments = new ForcingVectorIterator(ir.getPayments());
            while (payments.hasNext()) {

                PaymentCoreApprovable pmr = (PaymentCoreApprovable)payments.next();
                pmr.setActive(false);
            }

            ir.setActive(false);
        }
        invoice.setActive(false);
    }

    /**
        Reassign an invoice to a different partition

        @aribaapi ariba

        @param  invoice invoice which should be recreated in a new partition
        @param  newPartition the new partition
        @return new invoice
    */
    public static Invoice reassignToDifferentPartition (Invoice invoice,
                                                        Comment comment,
                                                        Partition newPartition)
    {
        Base.getSession().transactionBegin();
        Invoice newInvoice = null;
        Partition oldPartition = invoice.getPartition();
        try {
            newInvoice = InvoiceFactory.recreateInvoice(invoice, newPartition);
        }
        catch (InvalidInvoiceException iie) {
            Log.invoicing.warning(7985,
                                  invoice,
                                  newPartition,
                                  SystemUtil.stackTrace(iie));
        }

        if (newInvoice == null) {
            Log.invoicing.warning(7986,
                                  invoice,
                                  newPartition);
            Base.getSession().transactionRollback();
            return null;
        }

        Comment realComment = comment;
        if (comment != null) {
                // Create comment.
            realComment = newInvoice.addComment(comment,
                                                User.getEffectiveUser());
        }

        new SimpleSCARecord(newInvoice,
                      User.getEffectiveUser(),
                      realComment,
                      SimpleSCARecord.ChangePartitionRecord,
                      ListUtil.list(newInvoice.getUniqueName(),
                                    newPartition.getLabel(),
                                    invoice.getUniqueName(),
                                    oldPartition.getLabel()));

            // now process the new invoice
        newInvoice.processLoadedInvoice();
        newInvoice.setLoadedFrom(invoice.getLoadedFrom());
        BaseId newInvoiceId = newInvoice.getBaseId();

            // delete the old invoice
        inactivateInvoice(invoice);

        Base.getSession().transactionCommit();

        return (Invoice)newInvoiceId.get();
    }

    /*-----------------------------------------------------------------------
        XXX AD pulled from PO.java but should merge w/ above
      -----------------------------------------------------------------------*/

    private final static String InvoiceLineItemOrderField =
        Invoice.KeyLineItems + "." + InvoiceLineItem.KeyOrder;

    private final static String IROrderField =
        Invoice.KeyInvoiceReconciliations + "." +
        InvoiceReconciliation.KeyOrder;

    /**
        Return a query that will look up the invoices for this PO.
        This query will return the invoices that came in with this PO
        on them as well as the invoices that have been manually matched
        to this PO.

        @param po the PurchaseOrder to use for the lookup.

        @return the AQL query to look up invoices associated with this PO.

        @aribaapi documented
    */
    public static AQLQuery getQueryForInvoices (PurchaseOrder po)
    {
        /*
        AQLQuery query = new AQLQuery(Invoice.ClassName);
        query.setDistinct(true);

        AQLCondition invoicePO = AQLCondition.buildEqual(
            query.buildField(Invoice.KeyOrder), po.getBaseId());

        AQLCondition invoiceLineItemPOs = AQLCondition.buildIn(
            AQLScalarExpression.buildLiteral(po),
            query.buildField(InvoiceLineItemOrderField));

        AQLCondition irPO = AQLCondition.buildEqual(
            query.buildField(IROrderField), po.getBaseId());

        AQLCondition irLineItemPOs = getIRLineItemPOsCondition(query, po);

        AQLCondition matchPO = AQLCondition.buildOr(
            ListUtil.list(invoicePO, invoiceLineItemPOs, irPO, irLineItemPOs));

        query.and(matchPO);

        return query;
        */


    	/*
    	 * 	Changed by	:	Arasan Rajendren
    	 * 	Changed on	:	03/08/2011
    	 * 	Changes		:	Currently the Invoice Search Query uses an OR Clause which
    	 * 					makes DB2 to skip indexes and do a Full Table scan. Modified the
    	 * 					Query to use "UNION" instead of "OR"
    	 */

    	/*
    	 *  ORIGINAL QUERY
    	 *
	        SELECT DISTINCT Invoice
	        FROM ariba.invoicing.core.Invoice AS Invoice
	        JOIN ariba.invoicing.core.InvoiceLineItem SUBCLASS NONE USING LineItems
	        WHERE Invoice."Order" = BaseId('hdi9n5.3c')
	        OR LineItems."Order" = BaseId('hdi9n5.3c')
	     */

    	/*
    	 *  MODIFIED QUERY
    	 *
	        SELECT DISTINCT Invoice
	        FROM ariba.invoicing.core.Invoice AS Invoice
	        JOIN ariba.invoicing.core.InvoiceLineItem SUBCLASS NONE USING LineItems
	        WHERE Invoice."Order" = BaseId('hdi9n5.3c')
	        UNION
	        SELECT DISTINCT Invoice
	        FROM ariba.invoicing.core.Invoice AS Invoice
	        JOIN ariba.invoicing.core.InvoiceLineItem SUBCLASS NONE USING LineItems
	        WHERE LineItems."Order" = BaseId('hdi9n5.3c')
	     */

        String fmt = StringUtil.strcat(
            "SELECT DISTINCT %s \n",
            "FROM %s \n",
            "JOIN %s SUBCLASS NONE USING %s \n",
            "WHERE %s.\"%s\" = BaseId('%s') \n",
            "UNION \n",
            "SELECT DISTINCT %s \n",
            "FROM %s \n",
            "JOIN %s SUBCLASS NONE USING %s \n",
            "WHERE %s.\"%s\" = BaseId('%s') ");
        Object[] args1 = {
            ClassUtil.stripPackageFromClassName(Invoice.ClassName),
            Invoice.ClassName,
            InvoiceLineItem.ClassName,
            Invoice.KeyLineItems,
            ClassUtil.stripPackageFromClassName(Invoice.ClassName),
            Invoice.KeyOrder,
            po.getBaseId().toDBString(),
            ClassUtil.stripPackageFromClassName(Invoice.ClassName),
            Invoice.ClassName,
            InvoiceLineItem.ClassName,
            Invoice.KeyLineItems,
            Invoice.KeyLineItems,
            InvoiceLineItem.KeyOrder,
            po.getBaseId().toDBString()
        };
        String invoiceQuery = Fmt.S(fmt, args1);


        /*
         * 	ORIGINAL QUERY
         *
            SELECT DISTINCT Invoice
            FROM ariba.invoicing.core.InvoiceReconciliation AS ir
            JOIN ariba.invoicing.core.Invoice SUBCLASS NONE USING ir.Invoice
            WHERE ir."Order" = BaseId('hdi9n5.3c')
            OR ir.LineItems."Order" = BaseId('hdi9n5.3c')
        */

        /*
         * 	MODIFIED QUERY
         *
            SELECT DISTINCT Invoice
            FROM ariba.invoicing.core.InvoiceReconciliation AS ir
            JOIN ariba.invoicing.core.Invoice SUBCLASS NONE USING ir.Invoice
            WHERE ir."Order" = BaseId('hdi9n5.3c')
            UNION
            SELECT DISTINCT Invoice
            FROM ariba.invoicing.core.InvoiceReconciliation AS ir
            JOIN ariba.invoicing.core.Invoice SUBCLASS NONE USING ir.Invoice
            WHERE ir.LineItems."Order" = BaseId('hdi9n5.3c')
        */

        fmt = StringUtil.strcat(
            "SELECT DISTINCT %s \n",
            "FROM %s AS ir \n",
            "JOIN %s SUBCLASS NONE USING ir.%s \n",
            "WHERE ir.\"%s\" = BaseId('%s') \n",
            "UNION \n",
            "SELECT DISTINCT %s \n",
            "FROM %s AS ir \n",
            "JOIN %s SUBCLASS NONE USING ir.%s \n",
            "WHERE ir.%s.\"%s\" = BaseId('%s')");
        Object[] args2 = {
            ClassUtil.stripPackageFromClassName(Invoice.ClassName),
            InvoiceReconciliation.ClassName,
            Invoice.ClassName,
            InvoiceReconciliation.KeyInvoice,
            InvoiceReconciliation.KeyOrder,
            po.getBaseId().toDBString(),
            ClassUtil.stripPackageFromClassName(Invoice.ClassName),
            InvoiceReconciliation.ClassName,
            Invoice.ClassName,
            InvoiceReconciliation.KeyInvoice,
            InvoiceReconciliation.KeyLineItems,
            InvoiceReconciliationLineItem.KeyOrder,
            po.getBaseId().toDBString()
        };
        String irQuery = Fmt.S(fmt, args2);

        String queryString = Fmt.S("%s\nUNION\n%s", invoiceQuery, irQuery);
        AQLQuery query = AQLQuery.parseQuery(queryString);
        return query;
    }

    private static final String IRsAlias = "aIRs";
    private static final String IRsFieldPath =
                                Invoice.KeyInvoiceReconciliations;

    private static final String LineItemsAlias = "aLineItems";
    private static final String LineItemsFieldPath =
        Fmt.S("%s.%s", IRsAlias, InvoiceReconciliation.KeyLineItems);

    private static final String OrderAlias = "aOrder";
    private static final String OrderFieldPath =
        Fmt.S("%s.%s", LineItemsAlias, InvoiceReconciliationLineItem.KeyOrder);

    private static AQLCondition getIRLineItemPOsCondition (AQLQuery query,
                                                           Approvable approvable)
    {
        List partitionV = Base.getSession().getPartition().vector();

            // join Invoice.InvoiceReconciliations
        AQLClassReference crIRs =
            new AQLClassReference(
                InvoiceReconciliation.ClassName, IRsAlias, false, partitionV);
        query.addClass(crIRs, IRsFieldPath, false);

            // join InvoiceReconciliation.LineItems
        AQLClassReference crLineItems =
            new AQLClassReference(
                InvoiceReconciliationLineItem.ClassName,
                LineItemsAlias,
                false,
                partitionV);
        query.addClass(crLineItems, LineItemsFieldPath, true);

           // join LineItems.Order
        AQLClassReference crOrder =
            new AQLClassReference(
                PurchaseOrder.ClassName, OrderAlias, false, partitionV);
        query.addClass(crOrder, OrderFieldPath, false);

        AQLCondition irLineItemPOs = AQLCondition.buildEqual(
            new AQLFieldExpression(OrderAlias),
            approvable.getBaseId());

        return irLineItemPOs;
    }

    /**
        Overridden to only allow force reconcile when in the proper
        state and the user has the right permission.

        @aribaapi private
    */
    protected int getForceReconcileAccess (User user)
    {
            // Check that user is active
        user = getUserIfActive(user);
        if (user == null) {
            return Access.CantWorkOnApprovableNotActive;
        }
        if (getInvoiceState() == CCInvoiceToAN &&
            user.hasPermission(Permission.getPermission(PermissionForceReconcile))) {
            return Access.Now;
        }
        return Access.NotApplicable;
    }

    public List checkSubmit ()
    {
        InvoiceLineItem defaultLine = (InvoiceLineItem)getDefaultLineItem();
        int headerChargeCount = ListUtil.getListSize(defaultLine.getChildren());

            // check if the invoice has atleast one regular item
        if (getLineItems().size() == headerChargeCount) {

            List results = ListUtil.list();
            results.add(Constants.getInteger(-500));
            results.add(ResourceService.getString(
                    StringTable,
                    InvalidNumberOfLinesInInvoiceMsgKey,
                    getPreparer().getLocale()));
            return results;

        }
        return super.checkSubmit();
    }

    /*
        Over-ridden to get from Orders list if the non-persist field selectedOrders
        is null
    */
    public BaseVector getSelectedOrders ()
    {
        BaseVector selOrders = super.getSelectedOrders();

        if (selOrders.isEmpty()) {
            selOrders.addAll(getOrders());
        }

        return selOrders;
    }

    /**
     * Returns PO from supplierOrderInfo
     */
    public PurchaseOrder getValidPOFromSupplierOrderInfo (
                                                       StatementOrderInfo soi)
    {
        return StatementOrderInfo.getPurchaseOrder(soi, getSupplier());
    }


    /**
     * Method to check if there are any active invoices against an order
     * @aribaapi private
     */
    public static boolean hasActiveInvoicesForReceivable (
            ReceivableLineItemCollection rlic)
    {

    	/*
    	 * 	Changed by	:	Arasan Rajendren
    	 * 	Changed on	:	03/08/2011
    	 * 	Changes		:	Currently the Invoice Search Query uses an OR Clause which
    	 * 					makes DB2 to skip indexes and do a Full Table scan. Modified the
    	 * 					Query to use "UNION" instead of "OR"
    	 */

        /*
            Modified the Query to use "UNION" instead of "OR" Clause which may skip
            indexes and do a Full Table scan - 1-BAE4QA

            SELECT count(DISTINCT ir)
            FROM ariba.invoicing.core.InvoiceReconciliation AS ir
            WHERE ir."Order" = BaseId('hdi9n5.3c')
            AND ProcessedState NOT IN (8, 128)
            UNION
            SELECT count(DISTINCT ir)
            FROM ariba.invoicing.core.InvoiceReconciliation AS ir
            WHERE ir.LineItems."Order" = BaseId('hdi9n5.3c')
            AND ProcessedState NOT IN (8, 128)
        */
        String fmt = StringUtil.strcat(
            "SELECT count(DISTINCT ir)",
            "FROM %s AS ir \n",
            "WHERE ir.\"%s\" = BaseId('%s') \n",
            "AND ir.%s NOT IN (%s,%s) \n",
            "UNION \n",
            "SELECT count(DISTINCT ir)",
            "FROM %s AS ir \n",
            "WHERE ir.%s.\"%s\" = BaseId('%s') \n",
            "AND ir.%s NOT IN (%s,%s)");
        Object[] args = {
            InvoiceReconciliation.ClassName,
            InvoiceReconciliation.KeyOrder,
            rlic.getBaseId().toDBString(),
            InvoiceReconciliation.KeyProcessedState,
            Integer.toString(InvoiceReconciliation.Canceled),
            Integer.toString(InvoiceReconciliation.Rejected),
            InvoiceReconciliation.ClassName,
            InvoiceReconciliation.KeyLineItems,
            InvoiceReconciliationLineItem.KeyOrder,
            rlic.getBaseId().toDBString(),
            InvoiceReconciliation.KeyProcessedState,
            Integer.toString(InvoiceReconciliation.Canceled),
            Integer.toString(InvoiceReconciliation.Rejected)
        };
        String queryString = Fmt.S(fmt, args);

        AQLQuery query = AQLQuery.parseQuery(queryString);
        AQLOptions options = new AQLOptions(rlic.getPartition());
        AQLResultCollection results =
            Base.getService().executeQuery(query, options);

        try {
            if (results.next() && results.getInteger(0) > 0) {
                return true;
            }
            else {
                return false;
            }
        }
        finally {
            results.close();
        }

    }

    public boolean acceptChange (LogEntry log, ClusterRoot committedObject)
    {
        Object mergeObject = committedObject.find(log.changeeId);

        Log.invoicing.debug("Invoice.acceptChange entered: %s logEntry:%s",
                mergeObject, log);

        if (super.acceptChange(log, committedObject) ||
            log.fieldName.equals(KeyPayloadID)) {
            if (mergeObject instanceof Invoice ||
                mergeObject instanceof InvoiceLineItem) {
                Log.invoicing.debug(
                    "Invoice: acceptChange on %s: for %s", mergeObject, log.fieldName);
                return true;
            }
            else {
                Log.invoicing.debug(
                    "Invoice: did not acceptChange for %s, because %s", log.fieldName,
                    "mergeObject is not Invoice nor InvoiceLineItem");
                }
        }

        // Can't handle the change so return false
        return false;
    }

    public void merge (LogEntry log)
    {
        Log.invoicing.debug(
            "Invoice.merge: [%s,%s,%s,%s,%s,%s]", this, log.changeeId.toDBString(),
            Constants.getInteger(log.type), log.element,log.fieldName, log.old);

        Object mergeObject = this.find(log.changeeId);
        if (log.fieldName.equals(KeyPayloadID)) {
            ((BaseObject)mergeObject).basePut(log.fieldName , log.element );
            Log.invoicing.debug("Invoice:merge: setting mergeObject %s %s to %s",
                mergeObject, log.fieldName, log.element );
        }
        else {
            super.merge(log);
        }
    }

    public Clusterable findInternal (BaseId bid)
    {
        updateIfClusterRoot();

        Variant variant = getVariant();

            // Invoices that are beyond Loaded state cannot change. Hence the LineItem
            // indexes remain constant. We take advantage of this constraint and use a
            // cache of lineitem to index to speed up LineItem lookup.

        if (getInvoiceState() > Loaded &&
                InvoiceUtil.isBaseIdOfClassType(bid,
                                                variant,
                                                InvoiceLineItem.ClassName)) {

                // check the cache
            LineItem lineItem =
                InvoiceLineItemIndexCache.Instance.findLineItem(this, bid);

            if (lineItem != null) {
                return lineItem;
            }
        }

        return super.findInternal(bid);
    }


    /**
        Static cache map of Invoices and their lineitem  indexes. This is built only
        for large invoices which have 100 or more lineitems.
        Since Invoices don't change after they are loaded the indexes remain constant.

        @aribaapi ariba
    */
    public static class InvoiceLineItemIndexCache
    {
        public final static String ClassName = InvoiceLineItemIndexCache.class.getName();
        public final int MinSizeForInvoiceCache = 100;

        private static LRUHashtable invoiceCache = new LRUHashtable(ClassName,
                10, 30, 10, .75, null);

        private InvoiceLineItemIndexCache ()
        {}

        private Map getNewObjectMap ()
        {
            return new WeakHashMap();
        }

        public static InvoiceLineItemIndexCache Instance =
                new InvoiceLineItemIndexCache();

        public LineItem findLineItem (Invoice invoice, BaseId bid)
        {
                //bypass the cache for small invoices
            if (invoice.getLineItemsCount() < MinSizeForInvoiceCache) {
                return invoice.getLineItem(bid);
            }

            Map objectMap = (Map)invoiceCache.get(invoice.id);
            if (objectMap == null) {
                objectMap = getNewObjectMap();
                invoiceCache.put(invoice.id, objectMap);
            }

            return LineItemCacheUtil.findLineItem(invoice, bid,
                                 InvoiceLineItem.ClassName, objectMap);
        }



    }

    /**
     * Checks if this invoice has Shipping service item added to it.
     * @return true if Shipping service item is present in the line items vector
     * @aribaapi private
     */
    public boolean hasShippingServiceItem ()
    {
        checkShippingHandlingItems();
        return m_hasShippingServiceItem.booleanValue();
    }

    /**
     * Checks if this invoice has Special Handling service item added to it.
     * @return true if special handling service item is present in the line items vector
     * @aribaapi private
     */
    public boolean hasSpecialHandlingServiceItem ()
    {
        checkShippingHandlingItems();
        return m_hasHandlingServiceItem.booleanValue();
    }

    /**
     * Overridden method to default invoice info from the order which is added to
     * SelectedOrders vector.
     * If the order is not already present for that invoice, invoice information
     * gets defulted from that order
     * Note : Order information gets defaulted only if
     * 1. the invoice is in loaded state and it is not a credit memo and
     * 2. SupplierLocation on the order existing on invoice and that on this order match
     *
     * @param orderBaseId baseid of the order being added to the vector
     * @aribaapi private
     */
    public void addedSelectedOrdersElement (BaseId orderBaseId)
    {
        //make sure we only update invoice when they are in Loaded state.
        if (!isLoaded() || isCreditMemo()) {
            return;
        }

        PurchaseOrder po = (PurchaseOrder)Base.getSession().objectFromId(orderBaseId);

        // 1-8QTB2P: Order selected from RecentChoice could be Older version. We retrieve
        // lastest version and use that.

        PurchaseOrder latestOrder = (PurchaseOrder)po.getLatestVersion();
        List ordersInInvoice = getOrders();

        // how about shipping service line matched to in consolidated invoice
        if (ListUtil.nullOrEmptyList(ordersInInvoice) ||
            !ordersInInvoice.contains(latestOrder)) {

            // Invoice already have some order and along with that this order
            // is getting added, making the invoice as consolidated
            if (ordersInInvoice!= null && !ordersInInvoice.isEmpty()) {

                PurchaseOrder POInInvoice = (PurchaseOrder)ordersInInvoice.get(0);
                setConsolidated(true);
                if (!SystemUtil.equal(POInInvoice.getSupplierLocation(),
                                      latestOrder.getSupplierLocation())) {
                    Log.invoicing.debug("%s : Supplier locations on %s and %s " +
                                        "are different. Not Defaulting from PO",
                                        ClassName, POInInvoice, latestOrder, latestOrder );
                    return;
                }
            }
            defaultInvoiceInfoFromPO(latestOrder);
            // header taxes and charges are removed because, this wouldnt take care of
            // the newly added lines for this PO.
            removeHeaderTaxesAndChargesFromInvoice();
            updateInvoiceTotals();
        }
    }

    /**
     * Overridden method to remove all the invoice line items which are matched
     * to the order specified.
     * If any line item is deleted, header taxes and charges will be deleted as well
     * and the lines will be renumbered.
     *
     * Note : This method will not do any operation on the invoices which are
     * not in loaded state or if it is a credit memo
     *
     * @param orderBaseId baseid of the order deleted from the vector
     * @aribaapi private
     */
    public void removedSelectedOrdersElement (BaseId orderBaseId)
    {
        //make sure we only update invoice when they are in Loaded state.
        if (!isLoaded() || isCreditMemo()) {
            return;
        }

        PurchaseOrder po = (PurchaseOrder)Base.getSession().objectFromId(orderBaseId);
        List ordersInInvoice = getOrders();

        if (!ListUtil.nullOrEmptyList(ordersInInvoice) && ordersInInvoice.contains(po)) {

            int sizeOfOrdersInInvoice = ordersInInvoice.size();

            // Invoice is said to be consolidated if it has more than 1 order.
            // Since, one order will be deleted from the invoice, check is made
            // to verify if the size <3
            if (sizeOfOrdersInInvoice < 3 ) {
                PurchaseOrder lastOrder = null;
                int index = ordersInInvoice.indexOf(po);
                if (index == 0) {
                    if (sizeOfOrdersInInvoice != 1) {
                        lastOrder = (PurchaseOrder)ordersInInvoice.get(1);
                    }
                }
                else {
                    lastOrder = (PurchaseOrder)ordersInInvoice.get(0);
                }
                setOrderInfoOnInvoice(lastOrder != null ?
                                        (PurchaseOrder)lastOrder.getLatestVersion() :
                                        null);
                setConsolidated(false);
            }

            if (sizeOfOrdersInInvoice == 1) {
                //Clear off things if we are resettiing
                setOrder(null);
                setPaymentTerms(null);
                setSupplier(null);
                setSupplierLocation(null);
                getLineItems().clear();
            }
            else {
                removeILIsMatchedToPO(po);
                //header taxes and charges are removed because, those would have taken
                // into account, the deleted lines for this PO.
                removeHeaderTaxesAndChargesFromInvoice();
            }
            updateInvoiceTotals();
            renumberInvoiceLines();
        }
    }

    private void setOrderInfoOnInvoice (PurchaseOrder lastOrder)
    {
        setOrder(lastOrder);
        setSupplierOrderInfo(
                (lastOrder != null) ? new StatementOrderInfo(lastOrder) : null);
    }

    /**
     * Renumbers the invoice line items.
     * @aribaapi private
     */
    public void renumberInvoiceLines ()
    {
        int lineNumber = 1;
        Iterator lines = getLineItemsIterator();
        while (lines.hasNext()) {
            InvoiceLineItem ili = (InvoiceLineItem)lines.next();
            // set the line number only for level 2 line items
            if (ili.isLevel2LineItem()) {
                ili.setInvoiceLineNumber(lineNumber++);
            }
        }
    }

    /**
     * Defaults order information on the invoice
     * 1. Adds invoice lines corresponding to the PO lines.
     * 2. Sets supplier, supplierLocation, payment terms information from order
     * @param order
     * @aribaapi private
     */
    public void defaultInvoiceInfoFromPO (PurchaseOrder order)
    {
        List invoiceLines = getLineItems();
        int invoiceLineNumber = getLevel2LineItemsCount() + 1;
        setSupplier(order.getSupplier());
        setSupplierLocation(order.getSupplierLocation());

        // set order reference if non-consolidated
        if (!getConsolidated()) {
            setOrder(order);
            setSupplierOrderInfo(
                    (order != null) ? new StatementOrderInfo(order) : null);
        }
        else {
            setOrder(null);
            setSupplierOrderInfo(null);
        }
        setPaymentTerms((order != null) ? order.getPaymentTerms() : null);

        boolean allowInvoicedPOLineItems = Base.getService().getBooleanParameter(
                    getPartition(), ParameterInvoiceEntryAllowOverInvoicing);

        // Now go through each line on the order
        List poLines = order.getLineItems();
        int lines = ListUtil.getListSize(poLines);
        for (int j = 0; j < lines; j++) {
            POLineItem poLI = (POLineItem)poLines.get(j);
            // do not flip it if there is nothing to flip
            // or if it has been fully flipped
            if (!allowInvoicedPOLineItems &&
                (poLI.getAmountOrderedLessInvoiced() == null ||
                 poLI.getAmountOrderedLessInvoiced().isZero())) {
                continue;
            }
            // Create a new invoice line item and add it to the invoice
            InvoiceLineItem invoiceLI =
                    new InvoiceLineItem(getPartition(), this);
            invoiceLines.add(invoiceLI);

            // Get the LineType from the PO line and set it on the invoice line item
            ProcureLineType lineType = ProcureLineType.lookupByLineItem(poLI);
            invoiceLI.setLineType(lineType);
            invoiceLI.setSupplierOrderInfo(
                    (order != null) ? new StatementOrderInfo(order) : null);
            invoiceLI.setCategory(Invoice.CategoryPurchase);

            // Set the InvoiceLineNumber to the next number and increment
            invoiceLI.setInvoiceLineNumber(invoiceLineNumber++);

            invoiceLI.setReferenceDate(getInvoiceDate());

            // Set the order information
            invoiceLI.setOrder(order);
            invoiceLI.setOrderLineItem(poLI);
            invoiceLI.setOrderLineNumber(
                Constants.getInteger(poLI.getExternalLineNumber()).intValue());

            // Get the product description from the po line and copy that info over
            LineItemProductDescription pd = poLI.getDescription();
            invoiceLI.setDescription(pd);

            // make sure the slid not get lost
            CategoryLineItemDetails clid = poLI.getCategoryLineItemDetails();
            if (clid != null) {
                clid = (CategoryLineItemDetails)clid.deepCopyAndStrip();
                invoiceLI.setCategoryLineItemDetails(clid);
                clid.setLineItem(invoiceLI);
            }

            // make sure the milestone gets flipped
            Milestone ms = (poLI.getMilestone() != null) ?
                (Milestone)poLI.getMilestone().deepCopyAndStrip() : null;
            invoiceLI.setMilestone(ms);
            invoiceLI.setStartDate(poLI.getStartDate());
            invoiceLI.setMilestoneItemization(null);

            // Set the quantity and price based on the line type of order
            // If non-quantifiable or receiving by amount we set the
            // quantity to 1 and price to the amount.
            boolean isQuantifiable = ProcureLineType.isQuantifiable(lineType);
            boolean isReceivingByAmount = poLI.isAmountBasedReceiving();

            if (!isQuantifiable || isReceivingByAmount) {
                invoiceLI.setQuantity(Constants.OneBigDecimal);
                // Here even though we are setting the Price the price will
                //end up being 0 since in ProcureLineItem.updateAmount it
                //ignores the price passed in and sets the price to be equal
                //to amount which at this time is 0
                invoiceLI.getDescription().setPrice(
                    new Money(poLI.getAmountOrderedLessInvoiced()));
                invoiceLI.setAmount(poLI.getAmountOrderedLessInvoiced());
            }
            else {
                invoiceLI.setQuantity(poLI.getNumberOrderedLessInvoiced());
                invoiceLI.getDescription().setPrice(poLI.getDescription().getPrice());
                Money price = invoiceLI.getDescription().getPrice();
                if (poLI.hasCategoryLineItemDetails() && price.isZero()) {
                    // for acp items where the price is not set in the catalog,
                    // get the price from the po line item.
                    price = poLI.getDescription().getPrice();
                }
                //CR 1-6HX7AN - if quantifiable or receiving by quantity the amount
                //should be equal to quantity * unit price
                invoiceLI.setAmount(price.multiply(invoiceLI.getQuantity()));
            }


            // Set accounting info
            invoiceLI.updateAccountingInfoFromRLIC(poLI);
        }
    }

    /**
     * Deletes invoice line items on the invoice which are matched to the order specified
     * @param order
     * @aribaapi private
     */
    public void removeILIsMatchedToPO (PurchaseOrder order)
    {
        List invoiceLines = getLineItems();
        //loop through each invoiceline and delete if that is matched to the order
        if (!ListUtil.nullOrEmptyList(invoiceLines)) {
            for (int i = invoiceLines.size() - 1; i >= 0; i--) {
                InvoiceLineItem invoiceLine = (InvoiceLineItem)invoiceLines.get(i);
                PurchaseOrder orderOnILI = invoiceLine.getOrder();
                if (orderOnILI != null) {
                    if (order.equals(orderOnILI)) {
                        removeChild(invoiceLine);
                    }
                }
            }
        }

    }

    /**
     * Deletes header level taxes and charges from the invoice
     * @aribaapi private
     */
    public void removeHeaderTaxesAndChargesFromInvoice ()
    {
        InvoiceLineItem ili = (InvoiceLineItem)getDefaultLineItem();
        List headerTaxesNCharges = ili.getChargeChildren();

        for (int i = headerTaxesNCharges.size() - 1; i >= 0; i--) {
            InvoiceLineItem item = (InvoiceLineItem)headerTaxesNCharges.get(i);
            removeChild(item);
        }
    }

    /**
     * Overridden method to set the newly added
     * Shipping and SpecialHandling line items.
     */
    public void addedLineItemsElement (LineItem li)
    {
        super.addedLineItemsElement(li);
        InvoiceLineItem ili = (InvoiceLineItem)li;
        if (ili.getIsShippingServiceItem()) {
            m_hasShippingServiceItem = Boolean.TRUE;
        }
        else if (ili.getIsSpecialHandlingServiceItem()) {
            m_hasHandlingServiceItem = Boolean.TRUE;
        }
    }

    /**
     * Overridden method to reset the Shipping and SpecialHandling line items
     */
    public void removedLineItemsElement (LineItem li)
    {
        super.removedLineItemsElement(li);
        InvoiceLineItem ili = (InvoiceLineItem)li;
        if (ili.getIsShippingServiceItem()) {
            m_hasShippingServiceItem = Boolean.FALSE;
        }
        else if (ili.getIsSpecialHandlingServiceItem()) {
            m_hasHandlingServiceItem = Boolean.FALSE;
        }
    }

    /**
     * Initializes the Shipping and Special handling flags if not initialized.
     *
     * @aribaapi private
     */
    private void checkShippingHandlingItems ()
    {
        if (m_hasShippingServiceItem == null || m_hasHandlingServiceItem == null) {

            Iterator lines = getLineItemsIterator();
            while (lines.hasNext()) {
                InvoiceLineItem ili = (InvoiceLineItem)lines.next();

                if (ili.getIsShippingServiceItem()) {
                    m_hasShippingServiceItem = Boolean.TRUE;
                }

                else if (ili.getIsSpecialHandlingServiceItem()) {
                    m_hasHandlingServiceItem = Boolean.TRUE;
                }

                if (m_hasShippingServiceItem != null &&
                    m_hasHandlingServiceItem != null) {
                        return;
                }
            }
            if (m_hasShippingServiceItem == null) {
                m_hasShippingServiceItem = Boolean.FALSE;
            }
            if (m_hasHandlingServiceItem == null) {
                m_hasHandlingServiceItem = Boolean.FALSE;
            }
        }
    }

    /**
     * Remove DISTINCT from search if user can query all invoices.
     * Querying DISTINCT all invoices has a performance issue since
     * database needs to process all invoices to return unique invoices.
     *
     * @aribaapi private
     */
    public AQLQuery searchQuery (AQLQuery query, SearchExpression srchExp)
    {
        User effectiveUser = User.getEffectiveUser();
        Partition partition = Base.getSession().getPartition();
        if (ApprovableType.canQueryAll(effectiveUser,
                                        partition,
                                        Invoice.ClassName)) {
            query.setDistinct(false);
        }
        return super.searchQuery(query,srchExp);
    }

    public void setInvoiceNumber (String number)
    {
       super.setInvoiceNumber(number);
       super.setUpperCaseInvoiceNumber(number.toUpperCase());

    }

    /**
    *
    * @param query the query (if not null) will be and'd with the condition
    * @param invoice invoice class or subclass to use
    * @param states list of states
    * @return Invoice.InvoiceState IN ( <code>states</code> )
    */
    public static AQLCondition addInvoiceStateConstraints (AQLQuery query,
                                                           AQLClassExpression invoice,
                                                           List states) {

        AQLCondition condition =
            AQLCondition.buildIn(invoice.buildField(KeyInvoiceState), states);

        if (query != null) {
            query.and(condition);
        }

        return condition;
    }

    /**
    *
    * @param className the subclass to retrieve, or all if null
    * @param fieldName the name of the field to use for the matching
    * @param lic the collection against which the invoices are being looked up
    * @param states list of invoice states
    * @param partitions list of partitions used in the query
    * @return lineItem count of Invoices in the specified state(s) which are matched
    * to the specified RLIC at the header level
    *
    * @see InvoiceCoreApprovable#lookupByHeaderMatchingCollectionLineItemsCount(String, String, ReceivableLineItemCollection, List, List)
    *
    * @aribaapi private
    */
    public static int lookupByHeaderMatchingCollectionInvoiceStatesLineItemsCount (
            String fieldName,
            ReceivableLineItemCollection lic,
            List states,
            List partitions)
    {
        int count = 0;

        // SELECT count(Invoice.LineItems) FROM ariba.invoicing.core.Invoice AS Invoice
        // WHERE Invoice.InvoiceState IN (1, 2) AND Invoice."Order" = baseid('iwce.52')
        AQLClassReference inv = buildAQLClassReference(ClassName);
        AQLQuery query = new AQLQuery(inv);
        AQLOptions opts = new AQLOptions(partitions);
        List countLineItems = ListUtil.list(
                new AQLFunctionCall(new AQLFieldExpression(AQLFunctionCall.
                                                           FunctionAggregateCount),
                                    ListUtil.list(inv.buildField(KeyLineItems)),
                                    false,
                                    false));
        query.setSelectList(countLineItems);

        addInvoiceStateConstraints(query, inv, states);
        addHeaderMatchingConstraints(query, inv, fieldName, lic);

        AQLResultCollection results = Base.getService().executeQuery(query, opts);
        if (!results.isEmpty()) {
            results.next();
            count = results.getInteger(0);
        }

        return count;
    }

    /**
    *
    * @param className the subclass to retrieve, or all if null
    * @param fieldName the name of the field to use for the matching
    * @param lic the collection against which the invoices are being looked up
    * @param states list of invoice states
    * @param partitions list of partitions used in the query
    * @return lineItem count of Invoices in the specified state(s) which are matched
    * to the specified RLIC at the line level
    *
    * @see InvoiceCoreApprovable#lookupByLineMatchingCollectionLineItemsCount(String, String, ReceivableLineItemCollection, List, List)
    *
    * @aribaapi private
    */
    public static int lookupByLineMatchingCollectionInvoiceStatesLineItemsCount (
            String fieldName,
            ReceivableLineItemCollection lic,
            List states,
            List partitions)
    {
        int count = 0;

        // SELECT DISTINCT Invoice FROM ariba.invoicing.core.Invoice AS Invoice
        // WHERE Invoice.InvoiceState IN (1, 2) AND Invoice."Order" IS NULL AND Invoice.LineItems."Order" = baseid('kdna.52')
        AQLClassReference subInv = buildAQLClassReference(ClassName);
        AQLQuery subQuery = new AQLQuery(subInv);
        AQLCondition lineMatchCondition = AQLCondition.buildAnd(
                addInvoiceStateConstraints(null, subInv, states),
                addLineMatchingConstraints(null, subInv, fieldName, lic));
        subQuery.and(lineMatchCondition);
        subQuery.setDistinct(true);

        // SELECT count(Invoice.LineItems) FROM ariba.invoicing.core.Invoice AS Invoice
        // WHERE Invoice IN
        // (SELECT DISTINCT Invoice FROM ariba.invoicing.core.Invoice AS Invoice
        // WHERE Invoice.InvoiceState IN (1, 2) AND Invoice."Order" IS NULL AND Invoice.LineItems."Order" = baseid('kdna.52'))
        AQLClassReference inv = buildAQLClassReference(ClassName);
        AQLQuery query = new AQLQuery(inv);
        AQLOptions opts = new AQLOptions(partitions);
        List countLineItems = ListUtil.list(
                new AQLFunctionCall(new AQLFieldExpression(AQLFunctionCall.
                                                           FunctionAggregateCount),
                                    ListUtil.list(inv.buildField(KeyLineItems)),
                                    false,
                                    false));
        query.setSelectList(countLineItems);
        query.andIn(inv.buildField(), AQLQuery.buildSubquery(subQuery));

        AQLResultCollection results = Base.getService().executeQuery(query, opts);
        if (!results.isEmpty()) {
            results.next();
            count += results.getInteger(0);
        }

        return count;
    }

    /**
        Can the user query invoices.
        @aribaapi private
     */
    public static boolean canQueryInvoice ()
    {
        User user = User.getEffectiveUser();
        return (user.hasPermission(PermissionQueryAllInvoice) ||
                user.hasPermission(PermissionQueryMyInvoice));
    }

    /**
        This is to override the same function in approvable so to display a
        custom subject for invoice PDA emails. For Invoice, we will add the
        supplier name and the amount of the invoice to the start of the subject

        @see ariba.approvable.core.Approvable#customPDAEmailSubject(
                ariba.base.core.LocalizedString)
        @param action
        @return LocalizedString
     */
    @Override
    public LocalizedString customPDAEmailSubject (LocalizedString originalSubject)
    {
            // add the supplier name and amount to the subject
        return InvoiceUtil.getPDAEmailSubject(this, originalSubject);
    }
}
