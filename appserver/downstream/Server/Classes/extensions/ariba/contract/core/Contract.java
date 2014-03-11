
package ariba.contract.core;

import ariba.app.util.SystemParameters;
import ariba.approvable.core.Access;
import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.ApprovableUtil;
import ariba.approvable.core.AttachmentWrapper;
import ariba.approvable.core.ChangeRecord;
import ariba.approvable.core.ChangedLineItem;
import ariba.approvable.core.Comment;
import ariba.approvable.core.ExcelExportableApprovable;
import ariba.approvable.core.FullViewAccessControl;
import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.approvable.core.LineItemReference;
import ariba.approvable.core.Record;
import ariba.approvable.core.SimpleRecord;
import ariba.approvable.core.SupplierUserSearchable;
import ariba.approvable.protocol.Approvable.ApprovableServer;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseMeta;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ChangedField;
import ariba.base.core.ClusterRoot;
import ariba.base.core.ForcingVectorIterator;
import ariba.base.core.LocalizedString;
import ariba.base.core.LogEntry;
import ariba.base.core.NamedPair;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLClassExpression;
import ariba.base.core.aql.AQLClassSelect;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLClassSelect;
import ariba.base.core.aql.AQLClassUnion;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLError;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.ClassProperties;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.Fields;
import ariba.base.fields.Trigger;
import ariba.base.fields.Variant;
import ariba.base.meta.core.FieldMeta;
import ariba.base.server.EventQueueException;
import ariba.basic.core.Currency;
import ariba.basic.core.DateRange;
import ariba.basic.core.Money;
import ariba.basic.util.BaseUtil;
import ariba.catalog.admin.core.CatalogItem;
import ariba.catalog.admin.core.CatalogLoadAPI;
import ariba.catalog.admin.core.Subscription;
import ariba.common.core.CommonSupplier;
import ariba.common.core.CoreUtil;
import ariba.common.core.ExternalCommentApprovable;
import ariba.common.core.SourcingDestination;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.cxml.core.CXMLUtil;
import ariba.cxml.service.ServiceException;
import ariba.pricing.core.PricingTerms;
import ariba.procure.core.AccrualUtil;
import ariba.procure.core.CatalogTypeImpactReporter;
import ariba.procure.core.HierarchicalTerm;
import ariba.procure.core.Milestone;
import ariba.procure.core.OrderSource;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.procure.core.ProcureUtil;
import ariba.procure.core.ProductDescription;
import ariba.procure.core.SubscriptionTracker;
import ariba.procure.core.SubscriptionTrackerRecord;
import ariba.procure.core.SubscriptionTrackerSource;
import ariba.procure.core.category.CategoryPropertyCache;
import ariba.procure.core.category.ProcureCategoryUtil;
import ariba.procure.core.mail.Notifications;
import ariba.receiving.core.DirectMilestoneReceivable;
import ariba.receiving.core.DirectMilestoneReceivableUtil;
import ariba.receiving.core.DirectReceivableUtil;
import ariba.receiving.core.MilestoneTracker;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptCoreApprovable;
import ariba.receiving.core.ReceiptItem;
import ariba.receiving.core.ReceiptSource;
import ariba.receiving.core.ReceiptTracker;
import ariba.receiving.core.ReceivableLineItem;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.server.workflowserver.Workflow;
import ariba.server.workflowserver.WorkflowService;
import ariba.user.core.Approver;
import ariba.user.core.Group;
import ariba.user.core.MoneyFormatter;
import ariba.user.core.Organization;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.user.core.Role;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Compare;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.Sort;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.formatter.IntegerFormatter;
import ariba.util.messaging.MessageUtil;
import ariba.util.messaging.MessagingException;
import ariba.util.messaging.NotificationCenter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Contract extends ContractBase
    implements ExternalCommentApprovable,
        OrderSource,
        ReceiptSource,
        FullViewAccessControl,
        DirectMilestoneReceivable,
        SupplierUserSearchable,
        ExcelExportableApprovable,
        SubscriptionTrackerSource

{

    public static final int MAStateCreated =  1;


    public static final int MAStateProcessing  =  2;


    public static final int MAStateProcessed  =  4;


    public static final int MAStateOpen  =  8;


    public static final int MAStateClosed  = 16;


    public static final int MAStateSending = 32;


    public static final int MAStateProcessFailed = 64;


    public static final int MAStateOpening = 128;


    public static final int MAStateClosing = 256;


    public static final String[] MAStateNames = {
        "Created", "Processing", "Processed", "Open",
        "Closed", "Sending", "Process Failed", "Opening", "Closing"
    };


    public static final int MAClosedReasonUser = 1;


    public static final int MAClosedReasonExpired = 2;


    public static final int MAClosedReasonOverLimit = 4;


    public static final int MAClosedReasonOther = 8;


    public static final int AvailableBalanceStateSubmitted  = 1;
    public static final int AvailableBalanceStateApproved   = 2;
    public static final int AvailableBalanceStateOrdered    = 4;
    public static final int AvailableBalanceStateReceived   = 8;
    public static final int AvailableBalanceStateInvoiced   = 16;
    public static final int AvailableBalanceStateReconciled = 32;
    public static final int AvailableBalanceStatePaid       = 64;
    public static final int AvailableBalanceStatePurchased  = 128;

    public static final int NotExistInERP = 0;
    public static final int PendingInERP = 1;
    public static final int ProcessedInERP = 2;
    public static final int FailedInERP = 4;


    protected static final String[] AvailableBalanceStates =
    { "Submitted", "Approved", "Ordered", "Received",
      "Invoiced", "Reconciled", "Paid", "Purchased" };



    public static final int CumulativePricingStateSubmitted  = 1;
    public static final int CumulativePricingStateApproved   = 2;
    public static final int CumulativePricingStateOrdered    = 4;
    public static final int CumulativePricingStateReceived   = 8;
    public static final int CumulativePricingStateInvoiced   = 16;
    public static final int CumulativePricingStateReconciled = 32;
    public static final int CumulativePricingStatePaid       = 64;


    protected static final String[] CumulativePricingStates =
    { "Submitted", "Approved", "Ordered", "Received", "Reconciled", "Paid" };




    private static final String MARestrictedQueryAllKey = "MARestrictedQueryAll";
    private static final String RACString = "ReleaseAccessCriteria";
    private static final String MARACString = "MAReleaseAccessCriteria";

    public static final String ParameterPushEvent = ParamMA + "ContractPushEvent";
    public static final String ParameterERPSendMethod =
        ParamMA + "ContractERPSendMethodName";
    public static final String ParameterContractSendMethod =
        ParamMA + "ContractSendMethodName";



    public Contract ()
    {
    }


    public Contract (Partition partition)
    {
        Assert.that(false, "Constructor not supported");
    }

    public Contract (Partition partition,
                            String title,
                            User preparer,
                            User requester)
    {
        Assert.that(false, "Constructor not supported");
    }


    public Contract (ContractRequest mar)
    {

        this.init(mar.getPartition(),
                  mar.getName(),
                  mar.getPreparer(),
                  mar.getRequester(),
                  false);
        this.createMasterAgreementFromRequest(mar);
        fireTriggers(Trigger.Create, null);
    }


    public void init (Partition partition)
    {
        this.init(partition, null, null, null);
    }


    public void init (Partition partition,
                      String title,
                      User preparer,
                      User requester)
    {
        init(partition, title, preparer, requester, true);
        setupContract(false);

            // fire creation defaulting trigger
        if (getClassName().equals(Contract.ClassName)) {
            fireTriggers(Trigger.Create, null);
        }
    }


    public void init (Partition partition,
                      String title,
                      User preparer,
                      User requester,
                      boolean fireCreateTrigger)
    {
        super.init(partition, title, preparer, requester);
        setupContract(false);

            // fire creation defaulting trigger
        if (fireCreateTrigger &&
            getClassName().equals(Contract.ClassName)) {

            fireTriggers(Trigger.Create, null);
        }
    }


    public void initNewVersion ()
    {
        super.initNewVersion();
        setupContract(true);

            // fire creation defaulting trigger
        if (getClassName().equals(Contract.ClassName)) {
            fireTriggers(Trigger.Create, null);
        }
    }


    private void setupContract (boolean versioned)
    {
        setSubmitDate(Fields.getService().getNow());
        setApprovedDate(Fields.getService().getNow());
        setApprovedStateNoUpdate(StateSubmitted);
        setApprovedStateNoUpdate(StateApproved);



        setPayloadID(generatePayloadID(getCreateDate()));

        if (versioned == false) {
            setMAState(MAStateCreated);
        }
        else {
            Contract previousMA = (Contract)getPreviousVersion();
            Assert.that(previousMA != null,
                        "%s does not have a previous version", this);

            ContractRequest previousMAR = getMasterAgreementRequest();
            ContractRequest currentMAR  =
                (ContractRequest)previousMAR.getNextVersion();
            Assert.that(currentMAR != null,
                        "%s does not have a next version.", previousMAR);


            List records = (List)getRecords().clone();

            getComments().clear();
            copyFieldsFromMAR(currentMAR);


            copyFieldsFromPreviousMA();


            resetExpirationNotificationDateToNull();
            setPercentageOfBalanceNotificationDate(null);
            getRecords().clear();
            getRecords().addAll(records);

            Assert.that(previousMAR.getUseExternalIDForMA() ==
                    currentMAR.getUseExternalIDForMA(),
                    "MAChange: Cannot change boolean UseExternalIDForMA, %s to %s",
                    Constants.getBoolean(previousMAR.getUseExternalIDForMA()),
                    Constants.getBoolean(currentMAR.getUseExternalIDForMA()));

        }
        updateStatusString();
    }

    private void resetExpirationNotificationDateToNull ()
    {
        if ((getExpirationNotificationDate() != null) &&
            (getDaysLeft() > getNotificationDays())) {
            setExpirationNotificationDate(null);
        }
        return;
    }


    private static final int PaddingSize = 4;


    protected String makeAribaIdBody (String externalID)
    {


        String idBody = getAribaIdBody();


        if (!StringUtil.nullOrEmptyOrBlankString(idBody)) {


            int maxLen = 0;
            FieldProperties fp = getFieldProperties(KeyUniqueName);
            if (fp != null) {
                maxLen = IntegerFormatter.getIntValue(
                    fp.getPropertyForKey(FieldProperties.TextLength));
            }


            maxLen = maxLen - idBody.length() - PaddingSize;


            String idPrefix = makeAribaIdPrefix();
            if (!StringUtil.nullOrEmptyOrBlankString(idPrefix)) {
                maxLen = maxLen - idPrefix.length();
            }


            if (maxLen > 0) {

                if (externalID.length() > maxLen) {
                    externalID = externalID.substring(0, maxLen);
                    Log.contract.debug("%s Truncating externalID to '%s'",
                                              this, externalID);

                }


                idBody = Fmt.S("%s-%s", idBody, externalID );


                setAribaIdBody(idBody);
            }
            else {
                Log.contract.debug("%s No room for externalID %s",
                                          this, externalID);
            }
        }

        return idBody;
    }

    protected String makeAribaIdPrefix ()
    {
        if (getIsBlanketPurchaseOrder()) {

            return "BPO";
        }

        return super.makeAribaIdPrefix();
    }

    private void createMasterAgreementFromRequest (ContractRequest mar)
    {
        this.save();

        copyFieldsFromMAR(mar);


        if (mar.getUseExternalIDForMA()) {
            String externalID = mar.getExternalID();
            Assert.that(!StringUtil.nullOrEmptyOrBlankString(externalID),
                        "%s .ExternalID cannot be empty when useExternalID is set",
                        mar);

            makeAribaIdBody(externalID);
        }
        else {


            makeAribaId();
            setAribaIdBody(mar.getAribaIdBody());
            String aribaId = makeAribaId();
            String idPrefix = makeAribaIdPrefix();


            AQLOptions options = new AQLOptions(Partition.Any);
            String queryString = Fmt.S("SELECT UniqueName " +
                                       "FROM %s INCLUDE ACTIVE INCLUDE INACTIVE",
                                       ClassName);
            AQLQuery query = AQLQuery.parseQuery(queryString);
            query.addSelectElement("UniqueName");
            AQLResultCollection results = Base.getService().executeQuery(query, options);
            int resultFieldCount = results.getResultFieldCount();
            List contractUniqueNames = ListUtil.list();
            while (results.next()) {
                String contractUniqueName = results.getString(1);
                contractUniqueNames.add(contractUniqueName);
            }


            while (contractUniqueNames.contains(aribaId)) {
                String idBody = Long.toString(
                    Base.getService().getNextNamedLong(idPrefix));
                setAribaIdBody(idBody);
                aribaId = makeAribaId();
            }

            setUniqueName(aribaId);
        }

        getRecords().clear();

        Currency currency = getCurrency();
        setAmountSubmitted(new Money(Constants.ZeroBigDecimal, currency));
        setAmountApproved(new Money(Constants.ZeroBigDecimal, currency));
        setAmountOrdered(new Money(Constants.ZeroBigDecimal, currency));
        setAmountReceived(new Money(Constants.ZeroBigDecimal, currency));
        setAmountAccepted(new Money(Constants.ZeroBigDecimal, currency));
        setAmountRejected(new Money(Constants.ZeroBigDecimal, currency));
        setAmountInvoiced(new Money(Constants.ZeroBigDecimal, currency));
        setAmountReconciled(new Money(Constants.ZeroBigDecimal, currency));
        setAmountPaid(new Money(Constants.ZeroBigDecimal, currency));
        setAmountPurchased(new Money(Constants.ZeroBigDecimal, currency));


        rollUpPreloadAmount(getPreloadAmount());

        ContractAccumulators nonSpecificAccumulators =
            new ContractAccumulators(getPartition(), getCurrency());
        setNonTermsSubAccumulators(nonSpecificAccumulators);
        ContractAccumulators specificAccumulators =
            new ContractAccumulators(getPartition(), getCurrency());
        setTermsSubAccumulators(specificAccumulators);
    }

    private void copyFieldsFromMAR (ContractRequest mar)
    {
        mar.setMasterAgreement(this);
        this.setMasterAgreementRequest(mar);


        String originalId = getUniqueName();
        String initialUniqueName = getInitialUniqueName();


        setCurrency(mar.getCurrency());

        BaseObject.copyCommonFields(mar, this, false, true);

        setUniqueName(originalId);
        setInitialUniqueName(initialUniqueName);


        setType(ClassName);


        BaseVector subscriptionTrackers = (BaseVector)getSubscriptionTrackers();
        subscriptionTrackers.clear();

        BaseVector maLineItems = (BaseVector)getLineItems();
        BaseVector marLineItems = (BaseVector)mar.getLineItems();

        int numMALineItems = maLineItems.size();
        int numMARLineItems = marLineItems.size();
        Assert.that(numMALineItems <= numMARLineItems,
                    "%s has less items than %s, " +
                    "deleting line items is not allowed.",
                    this, mar);

        for (int i = 0; i < numMALineItems; i++) {

            ContractLineItem maLineItem = (ContractLineItem)maLineItems.get(i);

            ContractCoreApprovableLineItem marLineItem =
                (ContractCoreApprovableLineItem)marLineItems.get(i);
            BaseObject.copyCommonFields(marLineItem, maLineItem, false, true);
        }

        Currency currency = getCurrency();
        for (int i = numMALineItems; i < numMARLineItems; i++) {

            ContractLineItem maLineItem = new ContractLineItem(getPartition(), this);
            maLineItems.add(maLineItem);

            ContractCoreApprovableLineItem marLineItem =
                (ContractCoreApprovableLineItem)marLineItems.get(i);
            BaseObject.copyCommonFields(marLineItem, maLineItem, false, true);
            maLineItem.setSourcingRequestItem(
                marLineItem.getSourcingRequestItem());

            maLineItem.setAmountAccepted(
                new Money(Constants.ZeroBigDecimal, maLineItem.getReceivingCurrency()));
            maLineItem.setAmountRejected(
                new Money(Constants.ZeroBigDecimal, maLineItem.getReceivingCurrency()));
            maLineItem.setAmountInvoiced(new Money(Constants.ZeroBigDecimal, currency));
            maLineItem.setAmountReconciled(new Money(Constants.ZeroBigDecimal, currency));
            maLineItem.setNumberAccepted(Constants.ZeroBigDecimal);
            maLineItem.setNumberRejected(Constants.ZeroBigDecimal);
            maLineItem.setNumberInvoiced(Constants.ZeroBigDecimal);
            maLineItem.setNumberReconciled(Constants.ZeroBigDecimal);

            if (!mar.getIsReceivable()) {
                maLineItem.setReceivingType(ReceivableLineItem.NoReceipt);
            }
            else {

                Log.receiving.debug("*** Setting MALI receiving type");
                maLineItem.setReceivingType(maLineItem.findReceivingType());
            }

            maLineItem.copyPricingFiltersFromMACALI(marLineItem);
        }

        AttachmentWrapper.fixupReferences(mar, this);

        fixupLineItemReference(mar);

        updateStatusString();

    }

    private void fixupLineItemReference (LineItemCollection mar)
    {
        List marComments = mar.getComments();
        BaseVector marLineItems = mar.getLineItems();
        List maComments = this.getComments();

        if (!ListUtil.nullOrEmptyList(marComments)) {
            for (int i = 0 ; i < marComments.size(); i++) {
                Comment marComment = (Comment)marComments.get(i);
                LineItem marLineItem = marComment.getLineItem();
                if (marLineItem == null) {
                    continue;
                }

                int index = marLineItems.indexOf(marLineItem);
                LineItem maLineItem = (LineItem)this.getLineItems().get(index);

                if (maLineItem != null) {
                    Comment maComment = (Comment)maComments.get(i);
                    maComment.setLineItem(maLineItem);
                }
            }
        }
    }

    private void copyFieldsFromPreviousMA ()
    {
        Contract previousMA = (Contract)getPreviousVersion();
        if (previousMA == null) {
            return;
        }

        setInitialUniqueName(previousMA.getInitialUniqueName());


        copyAccumulatorsFromPreviousVersion(previousMA);

        copyMilestoneTrackersFromPreviousMA(previousMA);
    }

    private void copyMilestoneTrackersFromPreviousMA (Contract previousMA)
    {
        List previousMS = previousMA.getMilestones();
        List currentMS = getMilestones();
        int numItems = previousMS.size();
        for (int i = 0; i < numItems; i++) {
            List usableTrackers = ListUtil.list();

            Milestone prevMS = (Milestone)previousMS.get(i);
            ForcingVectorIterator trackers =
                new ForcingVectorIterator(prevMS.getMilestoneTrackers());
            while (trackers.hasNext()) {
                MilestoneTracker tracker = (MilestoneTracker)trackers.next();

                if (tracker.isReceived()) {
                    Log.receiving.debug("copy over tracker %s", tracker);
                    usableTrackers.add(tracker);
                }
            }

            Milestone currMS = (Milestone)currentMS.get(i);
            currMS.getMilestoneTrackers().updateElements(usableTrackers);
            if (!ListUtil.nullOrEmptyList(usableTrackers)) {
                currMS.setStatus(prevMS.getStatus());
            }
        }
    }

    protected boolean isDefaultingCreateTriggerFiredManually ()
    {
        return true;
    }

    public void setStatusString (String status)
    {
        super.setStatusString(status);
        if (SystemUtil.equal(status, lookupMAStateNameForId(MAStateClosed))) {
            if (isTermTypeCatalog()) {
                List lineItems = getLineItems();
                int numLineItems = ListUtil.getListSize(lineItems);
                List subscriptionTrackers = ListUtil.list(numLineItems);
                for (int i = 0; i < numLineItems; i++) {
                    ContractLineItem cli = (ContractLineItem)lineItems.get(i);
                    SubscriptionTracker st = cli.getSubscriptionTracker();
                    if (st != null) {
                        st.publishEvent(st.EventDeactivate);
                    }
                }
            }
        	else {

	            SubscriptionTracker mast = getCurrentSubscriptionTracker();
	            if (mast != null) {
	                mast.publishEvent(mast.EventDeactivate);
	            }
        	}
            deactivateMilestoneTrackers(this);
        } else if (SystemUtil.equal(status, lookupMAStateNameForId(MAStateOpen))) {
            activateMilestoneTrackers(this);
        }

    }


    protected void copyLineItemAccumulatorsFromPreviousVersion (
        ReceivableLineItemCollection previous)
    {
        Assert.that(previous != null, "Previous version must be specified");



        List currentItems = getLineItems();
        List previousItems = previous.getLineItems();
        int numItems = previousItems.size();
        Money amount = null;
        for (int i = 0; i < numItems; i++) {
            ContractLineItem currLI = (ContractLineItem)currentItems.get(i);
            ContractLineItem prevLI = (ContractLineItem)previousItems.get(i);

            if (prevLI != null) {
                currLI.copyAccumulatorsFromPreviousVersion(prevLI);
            }
        }
    }


    private Object m_numberOfReleasesLock = new Object();


    public int getNumberOfReleases ()
    {
        return getLastReleaseNumber();
    }


    public void incrementNumberOfReleases ()
    {
        synchronized (m_numberOfReleasesLock) {
            int numReleases = getLastReleaseNumber();
            numReleases++;
            super.setLastReleaseNumber(numReleases);
        }
    }

    public void setLastReleaseNumber (int lastReleaseNumber)
    {
        Assert.that(false,
                    "Not supported.  Call incrementNumberOfReleases() instead");
    }


    public void setMAState (int state)
    {
        super.setMAState(state);

        Log.contract.debug("Setting MAState to %s in %s",
                                  Constants.getInteger(state), this);
        if (state == MAStateOpen) {
            setMAOpenDate(Fields.getService().getNow());

            setMAClosedDate(null);
            notifyOnTopic(ContractUtil.appendRealm(TopicOpen));


            if (getIsInvoiceable()) {
                setInvoicedState(Invoicing);
            }


            if (getIsReceivable()) {
                setReceivedState(Receiving);
            }
        }

        else if (state == MAStateClosed) {
              m_closing = false;
              setMAClosedDate(Fields.getService().getNow());


            List records = getRecords();
            for (int i = records.size() - 1; i >= 0; i--) {
                Record record = (Record)records.get(i);
                if (!(record instanceof ContractRecord)) {
                    continue;
                }
                ContractRecord maRecord = (ContractRecord)record;
                int maClosedReason = maRecord.getMAClosedReason();
                String maRecordType = maRecord.getMARecordType();
                if (maRecordType.startsWith(ContractRecord.MAClosed)  &&
                    maClosedReason != ContractRecord.MAClosedReasonNotClosed) {
                    setMAClosedReason(maClosedReason);
                    break;
                }
            }
            notifyOnTopic(ContractUtil.appendRealm(TopicClose));
        }

        updateStatusString();
    }


    public void setReceivedState (int state)
    {
        int currentState = getReceivedState();
        if (currentState == state) {
            return;
        }
        super.setReceivedState(state);

        Log.contract.debug("Setting ReceivedState to %s in %s",
                                  Constants.getInteger(state), this);
        if ((state == Received) && (getMAState() != MAStateClosed)) {
            Log.contract.debug(
                "Closing %s because it is fully received", this);
            close(MAClosedReasonOverLimit);
        }
        updateStatusString();
    }


    public void setInvoicedState (int state)
    {
        int currentState = getInvoicedState();
        if (currentState == state) {
            return;
        }
        super.setInvoicedState(state);

        Log.contract.debug("Setting InvoicedState to %s in %s",
                                  Constants.getInteger(state), this);
        if ((state == Invoiced) && (getMAState() != MAStateClosed)) {
            Log.contract.debug(
                "Closing %s because it is fully invoiced", this);
            close(MAClosedReasonOverLimit);
        }
        updateStatusString();
    }

    protected void notifyOnTopic (String topic)
    {

        NotificationCenter center = NotificationCenter.defaultCenter();
        try {
            center.notifyOnTopic(topic, this, null);
        }
        catch (MessagingException e) {
            Assert.that(false,
                        "Error encountered while publishing %s on %s: %s",
                         topic, this, e);
        }
    }


    public static void notifyAllInterestedParties (Contract ma,
                String action,
                LocalizedString subject,
                LocalizedString message,
                String help,
                boolean emailApproval)
    {

        List emailUsers = ListUtil.list();

        if (ma.getRequester() != null) {
            emailUsers.add(ma.getRequester().getBaseId());
        }
        if ((ma.getPreparer() != null) &&
            (!ma.getPreparer().equals(ma.getRequester()))) {

            emailUsers.add(ma.getPreparer().getBaseId());
        }
        if (!ListUtil.nullOrEmptyList(ma.getEditList())) {
            ListUtil.addElementsIfAbsent(emailUsers, ma.getEditList());
        }
        if (!ListUtil.nullOrEmptyList(ma.getNotificationList())) {
            List notificationList = ma.getNotificationList();
            List usersNotificationList =
                    ContractUtil.userGroupRoleListToUserList(notificationList);
            ListUtil.addElementsIfAbsent(emailUsers, usersNotificationList);
        }

        ma.sendMail(emailUsers,
                action,
                subject,
                message,
                help,
                emailApproval);
    }


    public void setMAState (String stateName)
    {
        int stateId = lookupMAStateIdForName(stateName);

        if (stateId > 0) {
            Log.contract.debug("Setting MAState to %s in %s",
                                      stateName, this);
            setMAState(stateId);
        }
        else {
            Log.contract.error(5377, stateName, this);
        }
    }


    public static String lookupMAStateNameForId (int state)
    {
        return getStateName(MAStateNames, state);
    }


    public static int lookupMAStateIdForName (String stateName)
    {
        return getStateWithName(MAStateNames, stateName);
    }


    public boolean isCreated ()
    {
        return (getMAState() <= MAStateCreated);
    }


    public boolean isOpen ()
    {
        return (getMAState() == MAStateOpen);
    }


    public boolean canOpen ()
    {
        return (getOpenMAAccess() == Access.Now);
    }


    public boolean isClosed ()
    {
        return (getMAState() == MAStateClosed);
    }


    public boolean canClose ()
    {
        return (getCloseMAAccess() == Access.Now);
    }


    public String getDerivedStatus ()
    {
        if (getMAState() < MAStateCreated) {
            return super.getDerivedStatus();
        }


        if (getReleaseType() == ReleaseTypeUser) {
            return lookupMAStateNameForId(getMAState());
        }
        else {
            int maState = getMAState();
            if (maState <= MAStateOpen || maState == MAStateSending ) {
                return lookupMAStateNameForId(maState);
            }
            int receivedState = getReceivedState();
            int invoicedState = getInvoicedState();
            boolean isReceivable = getIsReceivable();
            boolean isInvoiceable = getIsInvoiceable();



            if (isReceivable && isInvoiceable) {

                if (receivedState == Received && invoicedState == Invoiced) {
                    return lookupMAStateNameForId(MAStateClosed);
                }

                else if (receivedState == Received && invoicedState != Invoiced) {
                    return lookupReceivedStateNameForId(Received);
                }

                else if (receivedState != Received && invoicedState == Invoiced) {
                    return lookupInvoicedStateNameForId(Invoiced);
                }

                else {
                    return InitialClose;
                }
            }
            else if (isReceivable && !isInvoiceable) {

                if (receivedState == Received) {
                    return lookupMAStateNameForId(MAStateClosed);
                }
                else {
                    return InitialClose;
                }
            }
            else if (!isReceivable && isInvoiceable) {

                if (invoicedState == Invoiced) {
                    return lookupMAStateNameForId(MAStateClosed);
                }
                else {
                    return InitialClose;
                }
            }
            else {

                return lookupMAStateNameForId(MAStateClosed);
            }
        }
    }

    private static final String InitialClose = "InitialClose";


    public static String lookupAvailableBalanceStateNameForId (int state)
    {
        return getStateName(AvailableBalanceStates, state);
    }


    public static int lookupAvailableBalanceStateIdForName (String stateName)
    {
        return getStateWithName(AvailableBalanceStates, stateName);
    }



    public static String lookupCumulativePricingStateNameForId (int state)
    {
        return getStateName(CumulativePricingStates, state);
    }


    public static int lookupCumulativePricingStateIdForName (String stateName)
    {
        return getStateWithName(CumulativePricingStates, stateName);
    }



    public Money getAvailableAmount ()
    {
        return getAvailableAmount(null);
    }


    public Money getReceivedAvailableAmount ()
    {
        return getAvailableAmount(AvailableBalanceStateReceived);
    }


    public Money getReconciledAvailableAmount ()
    {
        return getAvailableAmount(AvailableBalanceStateReconciled);
    }


    public Money getAvailableAmount (int state)
    {
        return getAvailableAmount(lookupAvailableBalanceStateNameForId(state));
    }


    public Money getAvailableAmount (String state)
    {
        Money maxAmount = getMaxAmount();
        if (maxAmount == null) {
            return null;
        }

        Money usedAmount = getUsedAmount(state);

        if (usedAmount == null) {
            return null;
        }
        Money availableAmount = Money.subtract(maxAmount, usedAmount);
        if (availableAmount.getSign() < 0) {
            return new Money(availableAmount, 0.0);
        }

        return availableAmount;
    }


    public Money getUsedAmount ()
    {
        return getUsedAmount(null);
    }


    public Money getUsedAmount (String state)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(state)) {
            if (!isAvailableBalanceStateValid(state)) {
                Log.contract.error(7081,
                                          state,
                                          getPartition());
                state = null;
            }
        }

        if (StringUtil.nullOrEmptyOrBlankString(state)) {
            state = getAvailableBalanceState();
        }
        if (StringUtil.nullOrEmptyOrBlankString(state)) {
            return new Money(Constants.ZeroBigDecimal, getCurrency());
        }

        String usedAmountField = Fmt.S("Amount%s", state);
        Money usedAmount =
            (Money)ContractPricingUtil.getAmountFieldValue(this, usedAmountField);
        if (usedAmount == null) {
            usedAmount = new Money(Constants.ZeroBigDecimal, getCurrency());
        }
        return addPreloadAmount(usedAmount);
    }



    public BigDecimal getAmountPercentLeft ()
    {
        return getAmountPercentLeft(null);
    }


    public BigDecimal getReceivedAmountPercentLeft ()
    {
        return getAmountPercentLeft(AvailableBalanceStateReceived);
    }


    public BigDecimal getReconciledAmountPercentLeft ()
    {
        return getAmountPercentLeft(AvailableBalanceStateReconciled);
    }


    public BigDecimal getAmountPercentLeft (int state)
    {
        return getAmountPercentLeft(lookupAvailableBalanceStateNameForId(state));
    }


    public BigDecimal getAmountPercentLeft (String state)
    {
        Money availableAmount = getAvailableAmount(state);
        if (availableAmount == null) {
            return null;
        }

        return new BigDecimal(Money.percentage(availableAmount, getMaxAmount()));
    }


    public int getDaysLeft ()
    {
        Date expireDate = getExpirationDate();
        if (expireDate == null) {
            return 0;
        }

        Date expire = new Date(expireDate);
        Date.setTimeToMidnight(expire);
        Date now = Fields.getService().getNow();
        Date.setTimeToMidnight(now);

        long expireTime = expire.getTime();
        long nowTime = now.getTime();
        if (nowTime > expireTime) {
            return 0;
        }

        return (int)((expireTime - nowTime) / Date.MillisPerDay);
    }


    public String getSubscriptionName ()
    {

        return getInitialUniqueName();
    }


    public List getAvailableLineItems ()
    {
        return getAvailableLineItems(null);
    }


    public List getAvailableLineItems (String state)
    {
        List results = ListUtil.list();
        List lineItems = getLineItems();
        int numLineItems = ListUtil.getListSize(lineItems);
        for (int i = 0; i < numLineItems; i++) {
            ContractLineItem maLineItem = (ContractLineItem)lineItems.get(i);
            if (maLineItem.hasValidAvailableBalance(state)) {
                results.add(maLineItem);
            }
        }

        return results;
    }


    public Money getMaxAmountLessInvoiced ()
    {
        return getAmountLessAmount(getInternalMaxAmount(), getAmountInvoiced());
    }


    public Money getMaxAmountLessReconciled ()
    {
        return getAmountLessAmount(getInternalMaxAmount(), getAmountReconciled());
    }


    public Money getTotalAmountOrdered ()
    {
        return addPreloadAmount(
                ContractPricingUtil.getAmountFieldValue(this, KeyAmountOrdered));
    }


    public Money getTotalAmountInvoiced ()
    {
        return addPreloadAmount(
                ContractPricingUtil.getAmountFieldValue(this, KeyAmountReconciled));
    }


    public BaseId getSubscriptionId ()
    {
        BaseId subscriptionId = super.getSubscriptionId();
        if (subscriptionId == null) {
            subscriptionId =
                CatalogLoadAPI.findSubscription(getCommonSupplier(),
                                                getSubscriptionName());
        }
        return subscriptionId;
    }


    public void createSubscriptionTracker ()
    {
        createSubscriptionTracker(false);
    }


    public void createMASubscriptionTracker ()
    {
        createSubscriptionTracker();
    }



    public void createSubscriptionTracker (boolean synchronous)
    {
        if (!shouldCreateMAST()) {

            SubscriptionTracker mast = getCurrentSubscriptionTracker();
            if (mast == null) {

                Contract previousContract = (Contract)getPreviousVersion();
                if (previousContract != null) {
                    mast = previousContract.getCurrentSubscriptionTracker();
                }
            }
            if ((mast != null) &&
                ((mast.isActivated() || mast.isActivateFailed()))) {

                mast.publishEvent(mast.EventDeactivate);
            }
            return;
        }

        if (synchronous) {
            this.realCreateSubscriptionTracker();
        }
        else {
            Log.contract.debug("Initiating creation of TL for MAST for %s",
                                      this);

            ContractSTCreatorTL.createDurableMASTTransactionListener(this);
        }
    }


    public List realCreateSubscriptionTracker ()
    {
        if (isTermTypeItem() || (isReleaseTypeNone() && (getSLA() != null))) {
            Log.contract.debug("realCreateSubscriptionTracker: %s", this);
            ContractSubscriptionTracker cst =
                new ContractSubscriptionTracker(getPartition(), this);
            getSubscriptionTrackers().add(cst);
            Log.contract.debug("SubscriptionTracker %s created for Contract %s",
                               cst, this);
            return ListUtil.list(cst);
        }

        else if (isTermTypeCatalog() && !hasPreviousVersion()) {
            Log.contract.debug("Creating SubscriptionTrackers for %s", this);
            List lineItems = getLineItems();
            int numLineItems = lineItems.size();
            for (int i = 0; i < numLineItems; i++) {
                ContractLineItem cli = (ContractLineItem)lineItems.get(i);
                Subscription subscription = cli.getCatalogSubscription();
                if (subscription != null) {
                    ContractSubscriptionTracker cst =
                        new ContractSubscriptionTracker(getPartition(), this);
                    cli.setSubscriptionTracker(cst);
                }
            }
        }
        return null;
    }


    private boolean shouldCreateMAST ()
    {
        if (isTermTypeItem()) {
            boolean createMASTParam = Base.getService().getBooleanParameter(
                this.getPartition(),
                ParamCreateMASubscription);
            if (!createMASTParam) {
                Log.contract.debug(
                    "%s = false, SubscriptionTracker not created for %s",
                    ParamCreateMASubscription, this);
                return false;
            }

            if (!getCreateSubscription()) {
                Log.contract.debug(
                    "Field %s = false, SubscriptionTracker not created for %s",
                    KeyCreateSubscription, this);
                return false;
            }
        }

        if (!hasItemsNeedingSubscription()) {
            Log.contract.debug(
                "%s does not contain any items needing subscription.  " +
                "SubscriptionTracker not created",
                this);
            return false;
        }

        if (getReleaseType() == ReleaseTypeNone) {
            if (!hasValidAvailableBalance(AvailableBalanceStateReconciled)) {
                Log.contract.debug(
                    "%s does not have a valid available balance.  " +
                    "SubscriptionTracker not created",
                    this);
                return false;
            }
        }
        else {
            if (!hasValidAvailableBalance()) {
                Log.contract.debug(
                    "%s does not have a valid available balance.  " +
                    "SubscriptionTracker not created",
                    this);
                return false;
            }
        }

        Date now = Fields.getService().getNow();
        Date expDate = getActualExpirationDate();
        if (expDate != null && expDate.before(now) &&
            getReleaseType() != ReleaseTypeNone) {
            Log.contract.debug(
                "%s expirationDate %s has passed.  SubscriptionTracker not created",
                this, expDate);
            return false;
        }

        return true;
    }


    public void activateSubscription ()
    {
        activateSubscriptions();
    }


    public void activateSubscriptions ()
    {
        if (isTermTypeCatalog()) {
            List lineItems = getLineItems();
            int numLineItems = ListUtil.getListSize(lineItems);
            List subscriptionTrackers = ListUtil.list(numLineItems);
            for (int i = 0; i < numLineItems; i++) {
                ContractLineItem cli = (ContractLineItem)lineItems.get(i);
                SubscriptionTracker st = cli.getSubscriptionTracker();
                if (st != null) {
                    if (cli.hasValidAvailableBalance()) {
                        st.publishEvent(st.EventActivate);
                    }
                    else {
                        st.publishEvent(st.EventDeactivate);
                    }
                }
            }
        }
        else  {
            SubscriptionTracker st = getCurrentSubscriptionTracker();
            if (st != null) {
                st.publishEvent(st.EventActivate);
            }
        }

    }


    public void deactivateSubscriptions ()
    {
        if (isTermTypeCatalog()) {
            List lineItems = getLineItems();
            int numLineItems = ListUtil.getListSize(lineItems);
            List subscriptionTrackers = ListUtil.list(numLineItems);
            for (int i = 0; i < numLineItems; i++) {
                ContractLineItem cli = (ContractLineItem)lineItems.get(i);
                SubscriptionTracker st = cli.getSubscriptionTracker();
                if (st != null) {
                    st.publishEvent(st.EventDeactivate);
                }
            }
        }
        else {
            SubscriptionTracker st = getCurrentSubscriptionTracker();
            if (st != null) {
                st.publishEvent(st.EventDeactivate);
            }
        }
    }


    public SubscriptionTracker getCurrentSubscriptionTracker ()
    {
        List subscriptions = getSubscriptionTrackers();

        if (ListUtil.nullOrEmptyList(subscriptions)) {
            return null;
        }

        int numSubscriptions = ListUtil.getListSize(subscriptions);
        long currentSubscriptionTime = 0;
        SubscriptionTracker currentMAST = null;
        for (int i = 0; i < numSubscriptions; i++) {
            BaseId mastId = (BaseId)subscriptions.get(i);
            SubscriptionTracker mast = (SubscriptionTracker)mastId.get();
            long mastCreateTime = mast.getCreateDate().getTime();
            if (mastCreateTime > currentSubscriptionTime) {
                currentMAST = mast;
                currentSubscriptionTime = mastCreateTime;
            }
        }
        return currentMAST;
    }


    public List getCurrentSubscriptionTrackers ()
    {
        if (isTermTypeItem()) {
            SubscriptionTracker st = getCurrentSubscriptionTracker();
            if (st != null) {
                return ListUtil.list(st);
            }
            else {
                return ListUtil.list();
            }
        }

        if (isTermTypeCatalog()) {
            List lineItems = getLineItems();
            int numLineItems = ListUtil.getListSize(lineItems);
            List subscriptionTrackers = ListUtil.list(numLineItems);
            for (int i = 0; i < numLineItems; i++) {
                ContractLineItem cli = (ContractLineItem)lineItems.get(i);
                SubscriptionTracker st = cli.getSubscriptionTracker();
                if (st != null) {
                    subscriptionTrackers.add(st);
                }
            }
            return subscriptionTrackers;
        }

        return ListUtil.list();
    }


    public ContractSubscriptionTracker getCurrentMASubscriptionTracker ()
    {
        return (ContractSubscriptionTracker)getCurrentSubscriptionTracker();
    }


    public CommonSupplier getCommonSupplier (SubscriptionTracker tracker)
    {
        return getCommonSupplier();
    }


    public String getSubscriptionName (SubscriptionTracker tracker)
    {
        return getSubscriptionName();
    }


    public BaseId getSubscriptionId (SubscriptionTracker tracker)
    {
        if (isTermTypeCatalog() && (tracker != null)) {
            ContractLineItem cli = findLineItemForTracker(tracker);
            Subscription subscription = cli.getCatalogSubscription();
            Assert.that(subscription != null,
                        "No subscription exists on %s",
                        cli);
            return subscription.getBaseId();
        }

        return getSubscriptionId();
    }

    private ContractLineItem findLineItemForTracker (SubscriptionTracker tracker)
    {
        if (!isTermTypeCatalog()) {
            return null;
        }

        List lineItems = getLineItems();
        int numLineItems = ListUtil.getListSize(lineItems);
        List subscriptionTrackers = ListUtil.list(numLineItems);
        for (int i = 0; i < numLineItems; i++) {
            ContractLineItem cli = (ContractLineItem)lineItems.get(i);
            if (SystemUtil.equal(tracker, cli.getSubscriptionTracker())) {
                return cli;
            }
        }

        return null;
    }


    public void setSubscriptionId (SubscriptionTracker tracker,
                                   BaseId subscriptionId)
    {
        if (isTermTypeItem()) {
            setSubscriptionId(subscriptionId);
        }
    }


    public List createCatalogItems (SubscriptionTracker tracker)
    {
        if (isTermTypeCatalog()) {
            return ListUtil.list();
        }

        ContractSubscriptionTracker mast = getCurrentMASubscriptionTracker();
        String aribaMAId = getSubscriptionName();
        String isOrderable = getReleaseType() == ReleaseTypeUser ? "true" : "false";
        List maLineItems = getLineItems();
        int numMALineItems = ListUtil.getListSize(maLineItems);
        List newItems = ListUtil.list(numMALineItems);

        Date effDate = getEffectiveDate();
        Date expDate = getExpirationDate();


        List mastLineItems = mast.getMALineItems();
        for (int i = 0; i < numMALineItems; i++) {
            ContractLineItem maLineItem = (ContractLineItem)maLineItems.get(i);
            CatalogItem catalogItem =
                maLineItem.createCatalogItem(aribaMAId, isOrderable, effDate, expDate);
            if (catalogItem != null) {
                newItems.add(catalogItem);
                mastLineItems.add(new LineItemReference(maLineItem));
            }
        }
        return newItems;
    }


    public List getItemsForSubscription ()
    {
        if (isTermTypeCatalog()) {
            return ListUtil.list();
        }

        return getLineItems();
    }



    public boolean canActivateSubscription (SubscriptionTracker tracker)
    {
        if (!isOpen() && getReleaseType() != ReleaseTypeNone) {
            Log.contract.debug(
                "%s is not Open.  SubscriptionTracker cannot be activated",
                this);
            return false;
        }

        if (isTermTypeItem()) {
            Log.contract.debug(
                "%s is Open.  SubscriptionTracker can be activated",
                this);
            return true;
        }

        ContractLineItem cli = findLineItemForTracker(tracker);
        if (cli.hasValidAvailableBalance()) {
            Log.contract.debug(
                "%s is Open and %s has an available balance.  " +
                "SubscriptionTracker can be activated",
                this, cli);
            return true;
        }
        else {
            Log.contract.debug(
                "%s is Open but %s does not have an available balance.  " +
                "SubscriptionTracker cannot be activated",
                this, cli);
            return false;
        }
    }


    public void addSubscriptionTrackerRecord (SubscriptionTracker tracker,
                                              String recordType)
    {
        User aribasystem = User.getAribaSystemUser(getPartition());
        SubscriptionTrackerRecord record = new SubscriptionTrackerRecord(
            this, tracker, aribasystem, null, recordType);
        save();
    }


    public void updateCatalogSubscriptions ()
    {
        if (!isTermTypeCatalog()) {
            return;
        }

        Iterator itr = getLineItemsIterator();
        while (itr.hasNext()) {
            ContractLineItem cli = (ContractLineItem)itr.next();
            Subscription subscription = cli.getCatalogSubscription();
            if (subscription != null) {
                cli.updateSubscription(subscription, this);
            }
        }
    }


    public void registerTypeImpact ()
    {
        Contract previous = (Contract)getPreviousVersion();
        if (previous != null) {
            previous.unregisterTypeImpact();
        }

        List consumers = getCatalogTypeConsumers();
        if (ListUtil.nullOrEmptyList(consumers)) {
            Log.contract.debug(
                "%s will not be registered for Type Impact Report since does not" +
                "contain Formula Pricing.",
                this);
            return;
        }

        unregisterTypeImpact();

        Log.contract.debug("Registering TypeImpact for %s", this);
        String eventId = getUniqueName();
        Iterator it = consumers.iterator();
        CatalogTypeImpactReporter rep = new CatalogTypeImpactReporter();
        while (it.hasNext()) {
            ContractLineItem maLineItem = (ContractLineItem)it.next();
            Log.contract.debug("Adding %s, type %s to Type Impact",
                                      maLineItem,
                                      maLineItem.getConsumedTypeName());
            rep.add(maLineItem);
        }

        rep.registerImpactEvent(eventId,
            Permission.getPermission(PermissionContractTypeImpactReport));
    }


    public void unregisterTypeImpact ()
    {
        Log.contract.debug("Unregistering TypeImpact for %s", this);
        CatalogTypeImpactReporter.removeImpactEvent(getUniqueName());
    }


    private List getCatalogTypeConsumers ()
    {

        if (getTermType() != TermTypeItem) {
            return ListUtil.list();
        }


        List items = ProcureLineItem.filterLineItemsByLineType(
            getLineItems(),
            ProcureLineType.CatalogItemType);

        if (ListUtil.nullOrEmptyList(items)) {
            return ListUtil.list();
        }

        List consumers = ListUtil.list();
        Iterator itemsItr = items.iterator();
        while (itemsItr.hasNext()) {
            ContractLineItem maLineItem = (ContractLineItem)itemsItr.next();
            if (maLineItem.hasFormulaPricing()) {
                consumers.add(maLineItem);
            }
        }
        return consumers;
    }

    protected int getSubmitAccess (Approver approver)
    {
        return Access.CantSubmitMasterAgreement;
    }

    protected int getWithdrawAccess (Approver approver)
    {
        return Access.CantWithdrawMasterAgreement;
    }

    protected int getOpenMAAccess (User user)
    {

        if (getIsAuthoringContract()) {
            return Access.NotApplicable;
        }

        int access = canUserChange(user);
        if (access != Access.Now) {
            return access;
        }

        return getOpenMAAccess();
    }



    public int getOpenMAAccess ()
    {
        int maState = getMAState();
        if (maState < MAStateProcessed ||
            maState == MAStateSending ||
            maState == MAStateProcessFailed) {
            if (Log.contract.isDebugEnabled()) {
                Log.contract.debug(
                    "MA %s is not processed.  MAState = %s.  Cannot open.",
                    this, Constants.getInteger(maState));
            }
            return Access.CantOpenNotProcessed;
        }
        if (maState == MAStateOpen) {
            Log.contract.debug(
                "MA %s is already open.  Cannot open.", this);
            return Access.CantOpenAlreadyOpened;
        }

        if (!hasValidAvailableBalance()) {
            Log.contract.debug(
                "MA %s does not have an available balance.  Cannot open.",
                this);
            return Access.CantOpenOverTolerance;
        }

        if (!currentDateIsValid()) {
            Log.contract.debug(
                "Current date is invalid for MA %s.  Cannot open.", this);
            return Access.CantOpenCurrentDateInvalid;
        }

        return Access.Now;
    }


    protected int getCloseMAAccess (User user)
    {

        if (getIsAuthoringContract()) {
            return Access.NotApplicable;
        }

        int access = canUserChange(user);
        if (access != Access.Now) {
            return access;
        }

        return getCloseMAAccess();
    }

    private int getCloseMAAccess ()
    {
        int maState = getMAState();
        if (maState < MAStateProcessed) {
            return Access.CantCloseNotProcessed;
        }
        if (maState == MAStateClosed) {
            return Access.CantCloseAlreadyClosed;
        }

        return Access.Now;
    }

  protected int getUncloseMAAccess (User user)
    {
        int access = canUserChange(user);
        if (access != Access.Now) {
            return access;
        }

        return getUncloseMAAccess();
    }

    private int getUncloseMAAccess ()
    {
        if (getMAState() != MAStateClosed) {
            Log.contract.debug(
                "%s is not closed.  Contract cannot be unclosed.", this);
            return Access.CantUncloseMA;
        }

        Date effDate = getEffectiveDate();
        if  (effDate != null && effDate.before(Fields.getService().getNow())) {
            Log.contract.debug(
                "EffectiveDate %s for %s is null or in the past.  " +
                "Contract cannot be unclosed.",
                effDate, this);
            return Access.CantUncloseMA;
        }

        return Access.Now;
    }

    private int getReceiveFromMAAccess ()
    {
        int releaseType = getReleaseType();
        if (releaseType != ReleaseTypeNone) {
            return Access.CantReceiveNotReleaseTypeNoneMA;
        }

        boolean receivable = getIsReceivable();
        if (!receivable) {
            return Access.CantReceiveNonReceivableMA;
        }

        boolean received = isReceived();
        if (received) {
            return Access.CantReceiveReceivedMA;
        }

        return Access.Now;
    }

    protected int getInvoiceFromMAAccess (User user)
    {

        if (!ContractUtil.isInvoicingEnabled()) {
            return Access.CantInvoiceWithInvoicingDisabled;
        }

        int access = getInvoiceFromMAAccess();
        if (access != Access.Now) {
            return access;
        }

        Organization org = user.getOrganization();
        if (org instanceof CommonSupplier && org.isExternalOrganization() ) {
            if (!SystemUtil.equal((CommonSupplier)org, getCommonSupplier())) {
                return Access.SupplierUserCantInvoiceOtherSupplierContract;
            }
        }
        if (!ApprovableType.canCreateApprovable(user,
                                               Base.getSession().getPartition(),
                                               "ariba.invoicing.core.Invoice")) {
            return Access.NoPermissionToCreateInvoice;
        }
        return Access.Now;
    }


    private int getInvoiceFromMAAccess ()
    {
        int releaseType = getReleaseType();
        if (releaseType != ReleaseTypeNone) {
            return Access.CantInvoiceNotReleaseTypeNoneMA;
        }

        boolean invoiceable = getIsInvoiceable();
        if (!invoiceable) {
            return Access.CantInvoiceNonInvoiceableMA;
        }

        boolean invoiced = isInvoiced();
        if (invoiced) {
            return Access.CantInvoiceInvoicedMA;
        }

        if (!getPartition().equals(Base.getSession().getPartition())) {
            Supplier s = getCommonSupplier().getPartitionSupplier();
            if (s == null) {
                return Access.CantInvoiceGlobalMAWithInvalidPartitionSupplier;
            }
        }


        return Access.Now;
    }

    protected int getChangeAccess (User user)
    {
        return getMasterAgreementRequest().getChangeAccess(user);
    }

    protected int getReceiveAccessFromOrderState ()
    {
        return Access.Now;
    }


    public boolean currentDateIsValid ()
    {
        Date now = Fields.getService().getNow();
        Date effDate = getEffectiveDate();
        Date expDate = getActualExpirationDate();
        if ((effDate != null) && (now.before(effDate))) {
            return false;
        }

        if ((expDate != null) && (expDate.before(now))) {
            return false;
        }
        return true;
    }

    public void updateAmountSubmitted ()
    {
        updateAmountField(KeyAmountSubmitted);
    }

    public void updateAmountApproved ()
    {
        updateAmountField(KeyAmountApproved);
    }

    public void updateAmountOrdered ()
    {
        updateAmountField(KeyAmountOrdered);
    }

    public void updateAmountReceived ()
    {
        updateAmountField(KeyAmountReceived);
    }

    public void updateAmountInvoiced ()
    {
        updateAmountField(KeyAmountInvoiced);
    }

    public void updateAmountReconciled ()
    {
        updateAmountField(KeyAmountReconciled);
    }

    public void updateAmountPaid ()
    {
        updateAmountField(KeyAmountPaid);
    }


    public void updateAllAmounts ()
    {
        updateAmountField(KeyAmountSubmitted);
        updateAmountField(KeyAmountApproved);
        updateAmountField(KeyAmountOrdered);
        updateAmountField(KeyAmountReceived);
        updateAmountField(KeyAmountReconciled);
        updateAmountField(KeyAmountPaid);
        updateAmountField(KeyAmountPurchased);
    }

    public void updateAmountField (String fieldName)
    {
        super.updateAmountField(fieldName);

        if (Log.contract.isDebugEnabled()) {
            Money amount = (Money)getDottedFieldValue(fieldName);
            Log.contract.debug("New Amount for field %s = %s on %s",
                                      fieldName, amount.asString(), this);
        }
    }

    public void checkIfAvailableBalanceIsValid (String state)
    {
        Assert.that(!StringUtil.nullOrEmptyOrBlankString(state),
                    "State must not be null");
        Log.contract.debug(
            "checkIfAvailableBalanceIsValid(): State = %s", state);

        if (getMAState() < MAStateProcessed) {
            Log.contract.debug(
                "MA %s has not been processed. Not checking available balance",
                this);
            return;
        }

        int releaseType = getReleaseType();
        if ((releaseType == ReleaseTypeNone) &&
            (state.equals(lookupAvailableBalanceStateNameForId(
                              AvailableBalanceStateReceived)))) {
            if (getIsReceivable() &&
                !hasValidAvailableBalance(state)) {
                Log.contract.debug(
                    "Setting MA %s ReceivedState = Received because it " +
                    "is over its limit",
                    this);
                setReceivedState(Received);
            }
        }
        else if ((releaseType == ReleaseTypeNone) &&
                 (state.equals(lookupAvailableBalanceStateNameForId(
                                   AvailableBalanceStateReconciled)))) {
            if (!hasValidAvailableBalance(state)) {
                Log.contract.debug(
                    "Setting MA %s InvoicedState = Invoiced because it " +
                    "is over its limit",
                    this);
                setInvoicedState(Invoiced);
            }
        }

        else if (state.equals(getAvailableBalanceState())) {

            if (isOpen() && !hasValidAvailableBalance(state)) {
                Log.contract.debug(
                    "Closing MA %s because it is over its limit", this);
                close(MAClosedReasonOverLimit);
            }

            else if ((getOpenMAAccess() == Access.Now) &&
                     (getMAClosedReason() == MAClosedReasonOverLimit)) {

                Log.contract.debug(
                    "Opening MA %s because it now has an available balance", this);
                open();
            }
        }
    }

    public boolean hasValidAvailableBalance ()
    {
        Log.contract.debug(
            "hasValidAvailableBalance(): %s", this);

        if (getReleaseType() == ReleaseTypeNone) {
            if (!getIsReceivable()) {
                return
                    hasValidAvailableBalance(AvailableBalanceStateReconciled);
            }
            return
                hasValidAvailableBalance(AvailableBalanceStateReceived) &&
                hasValidAvailableBalance(AvailableBalanceStateReconciled);
        }
        return hasValidAvailableBalance(null);
    }

    public boolean hasValidAvailableBalance (int state)
    {
        return hasValidAvailableBalance(
            lookupAvailableBalanceStateNameForId(state));
    }

    public boolean hasValidAvailableBalance (String state)
    {
        Log.contract.debug(
            "hasValidAvailableBalance(): %s, state = %s", this, state);
        if (!hasValidAvailableAmount(state)) {
            return false;
        }

        List maLineItems = getLineItems();
        int numLineItems = maLineItems.size();

        for (int i = 0; i < numLineItems; i++) {
            ContractLineItem maLineItem = (ContractLineItem)maLineItems.get(i);
            if (maLineItem.hasValidAvailableBalance(state)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasValidAvailableAmount ()
    {
        return hasValidAvailableAmount(null);
    }

    public boolean hasValidAvailableAmount (String state)
    {
        Log.contract.debug(
            "hasValidAvailableAmount(): %s,  state = %s",
            this, state);
        Money internalMaxAmount = null;
        int releaseType = getReleaseType();
        if (releaseType == ReleaseTypeNone) {
            internalMaxAmount = getMaxAmount();
        }
        else {
            internalMaxAmount = getInternalMaxAmount();
        }
        if (internalMaxAmount == null) {
            Log.contract.debug(
                "InternalMaxAmount = null, returning true");
            return true;
        }
        Log.contract.debug(
            "InternalMaxAmount = %s", internalMaxAmount);

        Money usedAmount = getUsedAmount(state);

        if (usedAmount == null) {
            Log.contract.debug(
                "UsedAmount = null, returning true");
            return true;
        }

        boolean returnValue =
            internalMaxAmount.approxCompareTo(usedAmount) > 0;
        if (Log.contract.isDebugEnabled()) {
            Log.contract.debug(
                "hasValidAvailableAmount() returning %s for %s",
                Constants.getBoolean(returnValue), this);
        }
        return returnValue;
    }

    public boolean isAboveMaxAmount (ProcureLineItem pli)
    {
        Money maxAmount = getMaxAmount();
        if (maxAmount == null) {
            return false;
        }

        Money usedAmount = getUsedAmount();
        if (usedAmount == null) {
            return false;
        }

        Money newAmount = Money.add(usedAmount, pli.getAmount());
        return maxAmount.approxCompareTo(newAmount) < 0;
    }

    public String getAvailableBalanceState ()
    {
        String availableBalanceState = null;
        Partition partition = getPartition();
        int releaseType = getReleaseType();
        Assert.that((releaseType == ReleaseTypeUser) ||
                    (releaseType == ReleaseTypeNone),
                    "ReleaseType %s not allowed",
                    Constants.getInteger(releaseType));

        if (releaseType == ReleaseTypeNone && getIsInvoiceable()) {
            availableBalanceState = getStateName(AvailableBalanceStates,
                                                 AvailableBalanceStateReconciled);
        }

        else if (parentHierarchyHasInvoiceableMAs() ||
                  subHierarchyHasInvoiceableMAs()) {
            availableBalanceState =
                getStateName(AvailableBalanceStates,
                             AvailableBalanceStateReconciled);
        }
        else if (releaseType == ReleaseTypeNone && getIsReceivable()) {
            availableBalanceState = getStateName(AvailableBalanceStates,
                                                 AvailableBalanceStateReceived);
        }
        else if (parentHierarchyHasReceivableMAs() ||
                 subHierarchyHasReceivableMAs()) {
            availableBalanceState =
                getStateName(AvailableBalanceStates,
                             AvailableBalanceStateReceived);
        }
        else if (releaseType == ReleaseTypeUser ||
                 parentHierarchyHasReleasableMAs() ||
                 subHierarchyHasReleasableMAs()) {
            boolean contentOnly = CoreUtil.isPermissionActiveInRealm(
                CoreUtil.AribaContentProfessionalFeaturePermission);
            if (contentOnly) {
                availableBalanceState =
                    getStateName(AvailableBalanceStates,
                                 AvailableBalanceStatePurchased);
            }
            else {
                availableBalanceState =
                    Base.getService().getParameter(
                        partition, ParamReleaseAvailableBalanceState);
            }

            if (StringUtil.nullOrEmptyOrBlankString(availableBalanceState)) {
                Log.contract.error(4759,
                                          ParamReleaseAvailableBalanceState);
            }
        }

        if (StringUtil.nullOrEmptyString(availableBalanceState)) {
            Log.contract.debug(
                "MA %s does not allow invoices or receipts.  " +
                "AvailableBalanceState set to Received",
                this);

            availableBalanceState =
                getStateName(AvailableBalanceStates,
                             AvailableBalanceStateReceived);
        }

        if (!isAvailableBalanceStateValid(availableBalanceState)) {
            Log.contract.error(4761,
                                      availableBalanceState,
                                      partition);
        }
        return availableBalanceState;
    }


    public String getCumulativePricingState ()
    {
        if (getIsInvoiceable() ||
            parentHierarchyHasInvoiceableMAs() ||
            subHierarchyHasInvoiceableMAs()) {
            return lookupAvailableBalanceStateNameForId(
                AvailableBalanceStateReconciled);
        }


        return getCumulativePricingState(getPartition());
    }


    public static String getCumulativePricingState (Partition partition)
    {
        String state = Base.getService().getParameter(partition,
                                                      ParamCumulativePricingState);

        Assert.that(isCumulativePricingStateValid(state),
                    "%s in partition %s is not a valid state for the " +
                    "cumulative pricing.",
                    state, partition);

        return state;
    }

    public boolean acceptChange (LogEntry log, ClusterRoot committedMA)
    {
        if (log.fieldName == null) {
            return false;
        }

        if (KeyAmountPaid.equals(log.fieldName)) {

            Object mergeObject = committedMA.find(log.changeeId);
            if (mergeObject instanceof ContractLineItem ||
                mergeObject instanceof Contract) {
                Log.contract.debug("MA:acceptChange: for %s", log.fieldName);
                return true;
            }
            else {
                Log.contract.debug(
                    "MA: not acceptChange for %s, because %s",
                    log.fieldName,
                    "mergeObject is not ContractLineItem nor Contract");
                return false;
            }
        }

        if (super.acceptChange(log, committedMA) ||
                (log.fieldName.equals(Contract.KeySubscriptionId)
                && log.old == null)) {

            Log.contract.debug("acceptChange: true");
            return true;
        }

        return false;
    }

    public void merge (LogEntry log)
    {
        if (Log.contract.isDebugEnabled()) {
            Log.contract.debug("MA.merge: [%s,%s,%s,%s,%s]",
                                      log.changeeId.toDBString(),
                                      Constants.getInteger(log.type),
                                      log.element,
                                      log.fieldName,
                                      log.old);
        }

        if (KeyAmountPaid.equals(log.fieldName)) {

            Object mergeObject = this.find(log.changeeId);
            Assert.that(mergeObject != null,
                        "MA.merge could not find object for %s",
                        log.changeeId);

            Money oldValue = (Money)log.old;
            Money newValue = (Money)log.element;
            Money amountToAdd = Money.subtract(newValue, oldValue);


            if (mergeObject instanceof ContractLineItem) {
                ContractLineItem li = (ContractLineItem)mergeObject;
                li.addAmountWithoutUpdateParentTrigger(log.fieldName, amountToAdd);
            }
            else if (mergeObject instanceof Contract) {
                Contract ma = (Contract)mergeObject;
                ma.addAmount(log.fieldName, amountToAdd);
            }

            if (Log.contract.isDebugEnabled()) {
                Log.contract.debug(
                    "MA:merge: adding amount %s on field %s of  mergeObject",
                    amountToAdd.asString(),
                    log.fieldName,
                    mergeObject);
            }
        }


        else if (log.fieldName.equals(Contract.KeySubscriptionId)) {

            if (Log.contract.isDebugEnabled()) {
                Log.contract.debug("MA.merge: [%s,%s,%s,%s,%s]",
                        log.changeeId.toDBString(),
                        Constants.getInteger(log.type),
                        log.element,
                        log.fieldName,
                        log.old);
            }

            Object mergeObject = this.find(log.changeeId);
            if (mergeObject == null) {
                Log.contract.debug("Did NOT find mergeObject ");
                Assert.that(false, "MA.merge: could NOT find mergeObject");
            }

            Contract ma = (Contract)mergeObject;
            ma.setFieldValue(log.fieldName, log.element);
            Log.contract.debug(
                "MA:merge: setting mergeObject %s %s to %s",
                mergeObject,
                log.fieldName,
                log.element );
        }
        else {
            super.merge(log);
        }
    }


    public static boolean isAvailableBalanceStateValid (String state)
    {
        if (StringUtil.nullOrEmptyOrBlankString(state)) {
            return false;
        }

        for (int i = 0; i < AvailableBalanceStates.length; i++) {
            if (state.equalsIgnoreCase(AvailableBalanceStates[i])) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCumulativePricingStateValid (String state)
    {
        if (StringUtil.nullOrEmptyOrBlankString(state)) {
            return false;
        }

        for (int i = 0; i < CumulativePricingStates.length; i++) {
            if (state.equalsIgnoreCase(CumulativePricingStates[i])) {
                return true;
            }
        }

        return false;
    }

    public static AQLQuery getUnionedQuery(List allSelects)
    {
        int numAllSelects = allSelects.size();
        AQLClassExpression union = (AQLClassExpression)ListUtil.firstElement(allSelects);
        for (int i = 1; i < numAllSelects; i++) {
            AQLClassExpression nextUnion = (AQLClassExpression)allSelects.get(i);
            union = new AQLClassUnion(union, AQLClassUnion.OpUnion, nextUnion);
        }

        return new AQLQuery(union);
    }

    public static List getMAsForUser (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        boolean              addERPConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return ListUtil.list();
        }
        return getMAsForUser(user,
                             stateField,
                             states,
                             releaseTypes,
                             plic,
                             procureLineItem,
                             null,
                             addERPConstraint);
    }

    public static List getMAsForUser (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           additionalConstraint,
        boolean               addERPConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return ListUtil.list();
        }
        Log.contract.debug(
            "Retrieving MAs for %s, plic: %s, ProcureLineItem: %s",
            user, plic, procureLineItem);


        if ( procureLineItem != null ) {
             List allSelects = buildQueryMAsForUser(user,
                                                    stateField,
                                                    states,
                                                    releaseTypes,
                                                    null,
                                                    plic,
                                                    procureLineItem,
                                                    additionalConstraint,
                                                    addERPConstraint);
             List clonedAllSelects = ListUtil.list();
             for ( int i = 0; i < allSelects.size(); i++ ) {
                 clonedAllSelects.add(((AQLClassExpression)allSelects.get(i)).clone());
             }

             AQLClassReference maCR = null;
             AQLClassReference cli = null;
             boolean isGLobal = canCreateGlobalContracts();
             if (isGLobal) {
                 maCR = new AQLClassReference(ClassName,
                                              false,
                                              Partition.AnyVector);
                 cli = new AQLClassReference(ContractLineItem.ClassName,
                                             false,
                                             Partition.AnyVector);
             }
             else {
                 maCR = new AQLClassReference(ClassName,
                                              false,
                                              plic.getPartition().vector());
                 cli = new AQLClassReference(ContractLineItem.ClassName,
                                             false,
                                             plic.getPartition().vector());
             }
             for ( int i = 0; i < allSelects.size(); i++ ) {
                 AQLCondition cond = AQLCondition.buildCondition(
                     maCR.buildField(KeyTermType), AQLCondition.OpNotEqual,
                     Constants.getInteger(Contract.TermTypeItem));
                 ((AQLClassExpression)allSelects.get(i)).and(cond);
             }
             List nonItemTypeContracts = getMAsUsingQuery(getUnionedQuery(allSelects));

             for ( int i = 0; i < clonedAllSelects.size(); i++ ) {
                 AQLClassReference cliClone = (AQLClassReference)cli.clone();
                 AQLCondition cond = AQLCondition.buildCondition(
                     maCR.buildField(KeyTermType), AQLCondition.OpEqual,
                     Constants.getInteger(Contract.TermTypeItem));
                 ((AQLClassExpression)clonedAllSelects.get(i)).and(cond);

                 ((AQLClassSelect)clonedAllSelects.get(i)).addClass(
                         cliClone, KeyLineItems, false);
                 AQLCondition supplierPartNumberCond = AQLCondition.buildEqual(
                     cliClone.buildField(ContractLineItem.KeyDescription + "." +
                         ProductDescription.KeySupplierPartNumber),
                         procureLineItem.getDescription().getSupplierPartNumber());
                 ((AQLClassExpression)clonedAllSelects.get(i)).and(
                                         supplierPartNumberCond);
                 ((AQLClassExpression)clonedAllSelects.get(i)).detach();
             }
             List itemTypeContracts = getMAsUsingQuery(getUnionedQuery(clonedAllSelects));
             if ( nonItemTypeContracts != null && !nonItemTypeContracts.isEmpty() ) {
                 itemTypeContracts.addAll(nonItemTypeContracts);
             }
             return itemTypeContracts;
        }
        else {
            List allSelects = buildQueryMAsForUser(user,
                    stateField,
                    states,
                    releaseTypes,
                    null,
                    plic,
                    procureLineItem,
                    additionalConstraint,
                    addERPConstraint);

            return getMAsUsingQuery(getUnionedQuery(allSelects));
        }
    }

    public static List getOpenMAsForUser (
        ariba.common.core.User user,
        List                 releaseTypes,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           additionalConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return ListUtil.list();
        }

        String stateField = KeyMAState;
        List states = ListUtil.list(Constants.getInteger(MAStateOpen));

        return getMAsForUser(user,
                             stateField,
                             states,
                             releaseTypes,
                             plic,
                             procureLineItem,
                             additionalConstraint,
                             true);
    }

      private static final List VectorUserReleaseType =
        ListUtil.list(
            Constants.getInteger(Contract.ReleaseTypeUser));

    public static List getOpenMASubscriptionIdsForUser (
        ariba.common.core.User user,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           addConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return ListUtil.list();
        }
        ContractSubscriptionIdCache maSubIdCache =
            Contracts.getService().getMASubscriptionIdCache();
        String cacheKey =
            getMASubscriptionIdCacheKey(user, plic, procureLineItem, addConstraint);
        Object cacheValue = maSubIdCache.getValue(cacheKey);
        if (cacheValue != null) {
            return (List)cacheValue;
        }

        Log.contract.debug(
            "Retrieving open MAs with catalogs for %s, plic: %s, ProcureLineItem: %s",
            user, plic, procureLineItem);

        Assert.that(user != null,
                    "User cannot be null");

        Partition partition = user.getPartition();
        AQLClassReference maCR = new AQLClassReference(ClassName,
                                                       false,
                                                       partition.vector());

        AQLCondition catalogCond =
            AQLCondition.buildOr(
                AQLCondition.buildIsNotNull(maCR.buildField(KeySubscriptionId)),
                AQLCondition.buildAnd(
                    AQLCondition.buildEqual(maCR.buildField(KeyTermType),
                                            Constants.getInteger(TermTypeCatalog)),
                    AQLCondition.buildEqual(maCR.buildField(KeyCatalogsVisibleFlag),
                                            Boolean.TRUE)));


        if (addConstraint != null) {
            catalogCond = catalogCond.and(addConstraint);
        }

        String stateField = KeyMAState;
        List states = ListUtil.list(Constants.getInteger(MAStateOpen));

        List allSelects = buildQueryMAsForUser(user,
                                              KeyMAState,
                                              states,
                                              VectorUserReleaseType,
                                              ListUtil.list(KeyInitialUniqueName),
                                              plic,
                                              procureLineItem,
                                              catalogCond,
                                              true);

        List maSubIdList = getMASubscriptionIdList(getUnionedQuery(allSelects));
        maSubIdCache.putValue(cacheKey, maSubIdList);
        return maSubIdList;
    }

    private static List getMASubscriptionIdList (AQLQuery query)
    {

        AQLOptions options = getOptions();

        AQLResultCollection results =
            Base.getService().executeQuery(query, options);

        List resultsErrors = results.getErrors();
        if (!ListUtil.nullOrEmptyList(resultsErrors)) {
            for (int i = 0; i < resultsErrors.size(); i++) {
                AQLError error = (AQLError)resultsErrors.get(i);
                Log.contract.warning(4969, error);
            }
            return ListUtil.list();
        }


        if (results.isEmpty()) {
            return ListUtil.list();
        }

        List maSubscriptionIdList = ListUtil.list(results.getSize());
        while (results.next()) {
            String subscriptionId = results.getString(1);
            maSubscriptionIdList.add(subscriptionId);
        }
        return maSubscriptionIdList;
    }

    private static String getMASubscriptionIdCacheKey (
        ariba.common.core.User user,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           addConstraint)
    {
        FastStringBuffer cacheKeyFSB = new FastStringBuffer();
        cacheKeyFSB.append(user.getBaseId());
        Partition partition = user.getPartition();


        CustomContract customMA =
            getCustomContract(partition, user, plic);
        if (customMA != null) {
            AQLCondition partitionConstraint =
                customMA.customMasterAgreementConstraint(partition,
                                                         user,
                                                         plic,
                                                         procureLineItem);
            if (partitionConstraint != null) {
                cacheKeyFSB.append(partitionConstraint);
            }

            AQLCondition globalConstraint =
                customMA.customGlobalMasterAgreementConstraint(partition,
                                                               user,
                                                               plic,
                                                               procureLineItem);
            if (globalConstraint != null) {
                cacheKeyFSB.append(partitionConstraint);
            }

        }
        if (addConstraint != null) {
            cacheKeyFSB.append(addConstraint);
        }
        return cacheKeyFSB.toString();
    }

    private static List getMAsUsingQuery (AQLQuery query)
    {
        ContractCache maCache = Contracts.getService().getMasterAgreementCache();
        String queryString = query.toString();
        Object cacheValue = maCache.getValue(queryString);
        if (cacheValue != null) {
            return (List)cacheValue;
        }


        AQLOptions options = getOptions();

        AQLResultCollection results =
            Base.getService().executeQuery(query, options);


        List resultsErrors = results.getErrors();
        if (!ListUtil.nullOrEmptyList(resultsErrors)) {
            for (int i = 0; i < resultsErrors.size(); i++) {
                AQLError error = (AQLError)resultsErrors.get(i);
                Log.contract.warning(4969, error);
            }
            return ListUtil.list();
        }

        if (Log.contract.isDebugEnabled()) {
            Log.contract.debug(
                "Retrieved: %s Contracts", Constants.getInteger(results.getSize()));
        }


        if (results.isEmpty()) {
            maCache.putValue(queryString, ListUtil.list());
            return ListUtil.list();
        }

        List maList = ListUtil.list(results.getSize());
        while (results.next()) {
            BaseId maId = results.getBaseId(0);
            maList.add(maId.getForRead());
        }
        maCache.putValue(queryString, maList);
        return maList;
    }

    public static List buildQueryMAsForUser (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        List                 fetchFields,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           additionalConstraint)
    {
        return buildQueryMAsForUser(user,
                                    stateField,
                                    states,
                                    releaseTypes,
                                    fetchFields,
                                    plic,
                                    procureLineItem,
                                    additionalConstraint,
                                    false);
    }

    public static List buildQueryMAsForUser (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        List                 fetchFields,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           additionalConstraint,
        boolean                addERPConstraint)
    {
        return buildQueryMAsForUser(user,
                                    stateField,
                                    states,
                                    releaseTypes,
                                    fetchFields,
                                    plic,
                                    procureLineItem,
                                    additionalConstraint,
                                    addERPConstraint,
                                    true);
    }


    public static List buildQueryMAsForUser (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        List                 fetchFields,
        ProcureLineItemCollection plic,
        ProcureLineItem        procureLineItem,
        AQLCondition           additionalConstraint,
        boolean                addERPConstraint,
        boolean                addCustomContractConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return null;
        }

        Assert.that(user != null,
                    "User cannot be null");

        Partition partition = null;
        if (plic != null) {
            partition = plic.getPartition();
        }
        else {
            partition = user.getPartition();
        }

        CustomContract customMA = getCustomContract(partition, user, plic);
        AQLCondition partitionConstraint = null;
        AQLCondition globalConstraint = null;

        if (addCustomContractConstraint && customMA != null) {
            partitionConstraint =
                customMA.customMasterAgreementConstraint(partition,
                                                         user,
                                                         plic,
                                                         procureLineItem);

            globalConstraint =
                customMA.customGlobalMasterAgreementConstraint(partition,
                                                               user,
                                                               plic,
                                                               procureLineItem);
        }

        if (additionalConstraint != null) {
            if (globalConstraint != null) {
                globalConstraint = additionalConstraint.and(globalConstraint);
            }
            else {
                globalConstraint = additionalConstraint;
            }

            if (partitionConstraint != null) {
                partitionConstraint = additionalConstraint.and(partitionConstraint);
            }
            else {
                partitionConstraint = additionalConstraint;
            }
        }

        return getMAForUserQuery(user,
                                 stateField,
                                 states,
                                 releaseTypes,
                                 fetchFields,
                                 partitionConstraint,
                                 globalConstraint,
                                 addERPConstraint);
    }


    private static CustomContract getCustomContract (
        Partition              partition,
        ariba.common.core.User user,
        ProcureLineItemCollection plic)
    {
        return (CustomContract)
            ApprovableUtil.getClassParameter(partition,
                                             ParamCustomMAImplementation,
                                             CustomContract.class);
    }

    private static AQLCondition getSupplierTermCondition (ProcureLineItem procureLineItem)
    {
        Assert.that(procureLineItem != null && procureLineItem.getSupplier() != null,
                    "Null ProcureLineItem or supplier: %s", procureLineItem);

        Partition partition = procureLineItem.getPartition();
        AQLClassReference maCR = new AQLClassReference(ClassName,
                                                       false,
                                                       partition.vector());

        AQLCondition supplierCondition =
            AQLCondition.buildEqual(maCR.buildField(KeyTermType),
                                    Constants.getInteger(TermTypeSupplier));
        return supplierCondition;
    }

    private static final String maliCCField =
        KeyLineItems + "." + ContractLineItem.KeyCommodityCode;

    private static final String maliCCCField =
        KeyLineItems + "." + ContractLineItem.KeyDescription + "." +
        ProductDescription.KeyCommonCommodityCode;

    private static final String maliPartNumberField =
        KeyLineItems + "." + ContractLineItem.KeyDescription + "." +
        ProductDescription.KeySupplierPartNumber;

    private static AQLCondition getItemTermCondition (ProcureLineItem procureLineItem)
    {
        Assert.that(procureLineItem != null && procureLineItem.getSupplier() != null,
                    "Null ProcureLineItem or supplier: %s", procureLineItem);

        Partition partition = procureLineItem.getPartition();
        AQLClassReference maCR = new AQLClassReference(ClassName,
                                                       false,
                                                       partition.vector());

        String supplierPartNumber =
            procureLineItem.getDescription().getSupplierPartNumber();
        if (StringUtil.nullOrEmptyOrBlankString(supplierPartNumber)) {
            return null;
        }

        AQLCondition itemCondition =
            AQLCondition.buildAnd(
                AQLCondition.buildEqual(maCR.buildField(KeyTermType),
                                        Constants.getInteger(TermTypeItem)),
                AQLCondition.buildEqual(maCR.buildField(maliPartNumberField),
                                        supplierPartNumber));
        return itemCondition;
    }


    public boolean isCommentRequired (int commentType)
    {
        return (commentType == Comment.TypeCloseMA);
    }


    public static boolean isMasterAgreementEnabledForRealm ()
    {

        if (BaseUtil.isSharedServicesMode()) {
            return isMasterAgreementEnabled() &&
               CoreUtil.isFeatureActiveInRealm(
                   ProcureUtil.AribaFeatureContractCompliance);
        }
        return isMasterAgreementEnabled();
    }


    public static boolean isMasterAgreementEnabled ()
    {
        boolean installed = ApprovableUtil.isModuleInstalled(
                                SystemParameters.ParameterModuleContract);
        return installed;
    }



    public static final int UpdateActionReqSubmitted              =    1;
    public static final int UpdateActionReqWithdrawn              =    2;
    public static final int UpdateActionReqDenied                 =    4;
    public static final int UpdateActionReqEdited                 =    8;
    public static final int UpdateActionReqApproved               =   16;
    public static final int UpdateActionReqCanceled               =   32;
    public static final int UpdateActionOrderOrdered              =   64;
    public static final int UpdateActionOrderCanceled             =  128;
    public static final int UpdateActionReceiptReceived           =  256;
    public static final int UpdateActionInvoiceInvoiced           =  512;
    public static final int UpdateActionInvoiceReconciled         = 1024;
    public static final int UpdateActionInvoiceReconciliationPaid = 2048;
    public static final int UpdateActionShoppingCartPurchased     = 4096;
    public static final int UpdateActionShoppingCartEdited        = 8192;

    public static final String[] UpdateActionKeys = {
        "Requisition:Submitted",
        "Requisition:Withdrawn",
        "Requisition:Denied",
        "Requisition:Edited",
        "Requisition:Approved",
        "Requisition:Canceled",
        "Order:Ordered",
        "Order:Canceled",
        "Receipt:Received",
        "Invoice:Invoiced",
        "Invoice:Reconciled",
        "InvoiceReconciliation:Paid",
        "ShoppingCart:Purchased",
        "ShoppingCart:Edited"
    };


    public static String lookupMAUpdateActionForId (int state)
    {
        Log.contract.debug("Lookup MAUpdateActionState for %s",
                                  Constants.getInteger(state));
        return getStateName(UpdateActionKeys, state);
    }


    public static int lookupMAUpdateActionIdForName (String stateName)
    {
        return getStateWithName(UpdateActionKeys, stateName);
    }

    public static void updateAccumulators (Approvable approvable,
                                           String actionString)
    {
        updateAccumulators(approvable, actionString, null);
    }

    public static void updateAccumulators (Approvable approvable,
                                           String actionString,
                                           List changes)
    {
        int action = getStateWithName(UpdateActionKeys, actionString);

        Assert.that(action != -1,
                    "Invalid action %s", actionString);
        updateAccumulators(approvable, action, changes);
    }

    public static void updateAccumulators (Approvable approvable,
                                           int action)
    {
        updateAccumulators(approvable, action, null);
    }

    public static void  updateAccumulators (Approvable approvable,
                                           int action,
                                           List changes)
    {
        int state;
        String actionString = getStateName(UpdateActionKeys, action);
        ProcureLineItemCollection plic = null;
        switch (action) {
          case UpdateActionReqSubmitted:
            state = AvailableBalanceStateSubmitted;
            plic = (ProcureLineItemCollection)approvable;
            if (approvable.hasPreviousVersion()) {
                if (changes == null) {
                    changes = getChanges(plic, true);
                }
                updateAccumulators(plic, state, true, changes);
            }
            else {
                updateAccumulators(plic, state, true);
            }
            return;

          case UpdateActionReqWithdrawn:
            state = AvailableBalanceStateSubmitted;
            plic = (ProcureLineItemCollection)approvable;
            if (approvable.hasPreviousVersion()) {
                updateAccumulators(plic, state, false, changes);
            }
            else {
                updateAccumulators(plic, state, false);
            }
            return;

          case UpdateActionReqDenied:
            state = AvailableBalanceStateSubmitted;
            plic = (ProcureLineItemCollection)approvable;
            if (approvable.hasPreviousVersion()) {
                updateAccumulators(plic, state, false, changes);
            }
            else {
                updateAccumulators(plic, state, false);
            }
            return;

          case UpdateActionReqEdited:
            if (ListUtil.nullOrEmptyList(changes)) {
                Log.contract.debug(
                    "No changes made for action %s, %s.  Accumulators not updated",
                    actionString, approvable);
                return;
            }
            state = AvailableBalanceStateSubmitted;
            plic = (ProcureLineItemCollection)approvable;
            updateAccumulators((ProcureLineItemCollection)approvable,
                               state,
                               true,
                               changes);
            return;

          case UpdateActionReqApproved:
            state = AvailableBalanceStateApproved;
            plic = (ProcureLineItemCollection)approvable;
            if (approvable.hasPreviousVersion()) {
                updateAccumulators(plic, state, true, changes);
            }
            else {
                updateAccumulators(plic, state, true);
            }
            return;

          case UpdateActionReqCanceled:
            plic = (ProcureLineItemCollection)approvable;
            state = AvailableBalanceStateSubmitted;
            updateAccumulators(plic, state, false);
            state = AvailableBalanceStateApproved;
            updateAccumulators(plic, state, false);
            return;

          case UpdateActionOrderOrdered:
            state = AvailableBalanceStateOrdered;
            plic = (ProcureLineItemCollection)approvable;
            updateAccumulators(plic, state, true, changes);
            return;

          case UpdateActionOrderCanceled:

            checkApprovableType(approvable,
                                "ariba.purchasing.core.PurchaseOrder",
                                actionString);
            state = AvailableBalanceStateOrdered;
            updateAccumulators((ProcureLineItemCollection)approvable, state, false);
            return;

          case UpdateActionReceiptReceived:
            Assert.that(approvable instanceof Receipt,
                        "Approvable %s must be a %s for action %s",
                        approvable, Receipt.ClassName, actionString);
            state = AvailableBalanceStateReceived;
            updateAccumulators((Receipt)approvable, state);
            return;

          case UpdateActionInvoiceReconciled:

            checkApprovableType(approvable,
                                "ariba.invoicing.core.Invoice",
                                actionString);
            state = AvailableBalanceStateReconciled;
            updateAccumulators((ProcureLineItemCollection)approvable, state, true);
            return;

          case UpdateActionInvoiceReconciliationPaid:

            checkApprovableType(approvable,
                                "ariba.invoicing.core.InvoiceReconciliation",
                                actionString);
            state = AvailableBalanceStatePaid;
            updateAccumulators((ProcureLineItemCollection)approvable, state, true);
            return;

          case UpdateActionShoppingCartPurchased:
            state = AvailableBalanceStatePurchased;
            updateAccumulators((ProcureLineItemCollection)approvable, state, true);
            return;

          case UpdateActionShoppingCartEdited:
            state = AvailableBalanceStatePurchased;
            updateAccumulators((ProcureLineItemCollection)approvable, state, false);
            return;

          default:
            Assert.that(false,
                        "Action %s is not supported",
                        Constants.getInteger(action));
            return;
        }
    }

    private static void checkApprovableType (Approvable approvable,
                                             String requiredApprovableType,
                                             String actionString)
    {
        Assert.that(ClassUtil.instanceOf(approvable, requiredApprovableType),
                    "Approvable %s must be a %s for action %s",
                    approvable, requiredApprovableType, actionString);
    }

    private static void updateAccumulators (ProcureLineItemCollection plic,
                                            int state,
                                            boolean add)
    {
        String stateString = getStateName(AvailableBalanceStates, state);
        String quantityField = Fmt.S("Quantity%s", stateString);
        String amountField = Fmt.S("Amount%s", stateString);

        Iterator lineItems = plic.getLineItemsIterator();
        while (lineItems.hasNext()) {
            ProcureLineItem pli = (ProcureLineItem)lineItems.next();
            updateAccumulators(pli, add, stateString, amountField, quantityField);
        }
    }

    public static int getAvailableBalanceStateForPreApprovalAction (int action)
    {
        switch (action) {
          case UpdateActionReqSubmitted:
            return AvailableBalanceStateSubmitted;
          case UpdateActionReqWithdrawn:
            return AvailableBalanceStateSubmitted;
          case UpdateActionReqDenied:
            return AvailableBalanceStateSubmitted;
          case UpdateActionReqEdited:
            return AvailableBalanceStateSubmitted;
          case UpdateActionReqApproved:
            return AvailableBalanceStateApproved;
          default:
            Assert.that(false,
                        "Action %s is not supported",
                        Constants.getInteger(action));
            return 0;
        }
    }

    public static void updateAccumulators (ProcureLineItem pli,
                                           int state,
                                           boolean add)
    {
        String stateString = getStateName(AvailableBalanceStates, state);
        String quantityField = Fmt.S("Quantity%s", stateString);
        String amountField = Fmt.S("Amount%s", stateString);
        updateAccumulators(pli, add, stateString, amountField, quantityField);
    }

    private static void updateAccumulators (ProcureLineItem pli,
                                            boolean add,
                                            String stateString,
                                            String amountField,
                                            String quantityField)
    {
        ContractLineItem mali = null;
        if (pli instanceof ContractLineItemSource) {
            mali = ((ContractLineItemSource)pli).getMALineItem();
        }
        if (mali == null) {
            return;
        }

        BigDecimal quantity = null;
        Money amount = null;
        if (add) {
            quantity = pli.getQuantity();
            amount = pli.getAmount();
        }
        else {
            quantity = pli.getQuantity().negate();
            amount = pli.getAmount().negate();
        }

        mali.addAmount(amountField, amount, pli);
        mali.addQuantity(quantityField, quantity, pli);

        printAccumulators(mali, stateString, amount, quantity);
    }

    private static void updateAccumulators (Receipt receipt,
                                            int state)
    {
        String stateString = getStateName(AvailableBalanceStates, state);
        String quantityField = Fmt.S("Quantity%s", stateString);
        String amountField = Fmt.S("Amount%s", stateString);

        Iterator lineItems = receipt.getReceiptItemsIterator();
        while (lineItems.hasNext()) {
            ReceiptItem receiptItem = (ReceiptItem)lineItems.next();


            ContractLineItem mali = null;
            ReceivableLineItem rli = receiptItem.getLineItem();
            if (rli instanceof ContractLineItem) {
                mali = (ContractLineItem)rli;
            }
            else if (rli instanceof ContractLineItemSource) {
                mali = ((ContractLineItemSource)rli).getMALineItem();
            }
            if (mali == null) {
                continue;
            }

            BigDecimal quantity = receiptItem.getNumberAccepted();

            Money amount = null;

            if (receiptItem.getLineItem() instanceof ContractLineItem) {
                amount = receiptItem.getAmountAccepted();
            }
            else {

                amount = Money.multiply(
                    receiptItem.getLineItem().getDescription().getPrice(),
                    quantity);
            }
            mali.addAmount(amountField, amount, mali);
            mali.addQuantity(quantityField, quantity, mali);
            printAccumulators(mali, stateString, amount, quantity);
        }
    }

    private static String PLIQuantityField = ProcureLineItem.KeyQuantity;
    private static String PLIUnitPriceField =
        ProcureLineItem.KeyDescription + "." + ProductDescription.KeyPrice;

    private static void updateAccumulators (ProcureLineItemCollection plic,
                                            int state,
                                            boolean addChanges,
                                            List changes)
    {
        updateAccumulators(plic, state, addChanges, changes, false);
    }


    private static void updateAccumulators (ProcureLineItemCollection plic,
                                            int state,
                                            boolean addChanges,
                                            List changes,
                                            boolean getAllChanges)
    {
        if (ListUtil.nullOrEmptyList(changes)) {

            changes = getChanges(plic, getAllChanges);
            if (ListUtil.nullOrEmptyList(changes)) {
                updateAccumulators(plic, state, addChanges);
                return;
            }
        }

        String stateString = getStateName(AvailableBalanceStates, state);
        String quantityField = Fmt.S("Quantity%s", stateString);
        String amountField = Fmt.S("Amount%s", stateString);

        int numChanges = changes.size();
        List lineItemsProcessed = ListUtil.list();
        for (int i = 0; i < numChanges; i++) {
            ChangedField changedField = (ChangedField)changes.get(i);
            Log.contract.debug(
                "updateAccumulators: processing changedField: %s",
                changedField);
            String boClass = changedField.getBaseObjectClass();

            if (StringUtil.nullOrEmptyString(boClass) ||
                (!boClass.equals("ariba.purchasing.core.ReqLineItem") &&
                !boClass.equals("ariba.purchasing.core.POLineItem"))) {
                continue;
            }


            if (changedField instanceof ChangedLineItem) {
                ChangedLineItem changedLI = (ChangedLineItem)changedField;
                boolean addedLineItem = true;
                if (changedField.getNewBaseObjectId() == null) {
                    addedLineItem = false;
                }
                updateChangedLineItem(changedLI,
                                      state,
                                      addedLineItem,
                                      addChanges,
                                      lineItemsProcessed);
                continue;
            }

            BaseId liBaseId = changedField.getNewBaseObjectId();

            if (lineItemsProcessed.indexOf(liBaseId) != -1) {
                continue;
            }
            lineItemsProcessed.add(liBaseId);

            ProcureLineItem pli = (ProcureLineItem)
                changedField.getNewClusterRoot().findComponentIfAny(liBaseId);


            if (pli == null) {
                Log.contract.debug(
                    "LineItem %s was not found in Collection %s",
                     liBaseId, changedField.getNewClusterRoot());
                continue;
            }

            ContractLineItem mali = null;
            if (pli instanceof ContractLineItemSource) {
                mali = ((ContractLineItemSource)pli).getMALineItem();
            }
            if (mali == null) {
                continue;
            }

            BigDecimal oldQuantity = null;
            BigDecimal newQuantity = null;
            Money oldUnitPrice = null;
            Money newUnitPrice = null;


            for (int j = i; j < numChanges; j++) {
                ChangedField cf = (ChangedField)changes.get(j);
                BaseId newBaseId = cf.getNewBaseObjectId();
                if ((newBaseId == null) || !liBaseId.equals(newBaseId)) {
                    continue;
                }

                String fieldName = cf.getFieldName();
                if (PLIQuantityField.equals(fieldName)) {
                    if (oldQuantity == null) {
                        oldQuantity = (BigDecimal)cf.getOldValue();
                        newQuantity = (BigDecimal)cf.getNewValue();
                    }
                    else {
                        newQuantity = (BigDecimal)cf.getNewValue();
                    }
                }
                else if (PLIUnitPriceField.equals(fieldName)) {
                    if (oldUnitPrice == null) {
                        oldUnitPrice = (Money)cf.getOldValue();
                        newUnitPrice = (Money)cf.getNewValue();
                    }
                    else {
                        newUnitPrice = (Money)cf.getNewValue();
                    }
                }
            }

            if (oldQuantity != null && newQuantity != null) {
                Log.contract.debug("OldQuantity = %s; NewQuantity = %s",
                                          oldQuantity, newQuantity);
            }

            if (oldUnitPrice != null && newUnitPrice != null) {
                Log.contract.debug("OldPrice = %s; NewPrice = %s",
                                          oldUnitPrice.asString(),
                                          newUnitPrice.asString());
            }


            Money oldAmount = null;
            Money newAmount = null;
            if ((newQuantity == null) && (newUnitPrice == null)) {
                continue;
            }
            else if ((newQuantity != null) && (newUnitPrice != null)) {
                oldAmount = Money.multiply(oldUnitPrice, oldQuantity);
                newAmount = Money.multiply(newUnitPrice, newQuantity);
            }
            else if (newQuantity == null) {
                BigDecimal quantity = pli.getQuantity();
                oldAmount = Money.multiply(oldUnitPrice, quantity);
                newAmount = Money.multiply(newUnitPrice, quantity);
            }
            else if (newUnitPrice == null) {
                Money unitPrice = pli.getDescription().getPrice();
                oldAmount = Money.multiply(unitPrice, oldQuantity);
                newAmount = Money.multiply(unitPrice, newQuantity);
            }


            BigDecimal diffQuantity = null;
            if (newQuantity != null) {
                if (addChanges) {
                    diffQuantity = newQuantity.subtract(oldQuantity);
                }
                else {
                    diffQuantity = oldQuantity.subtract(newQuantity);
                }
                mali.addQuantityWithoutUpdateParentTrigger(quantityField, diffQuantity);
            }

            Money diffAmount = null;
            if (addChanges) {
                diffAmount = Money.subtract(newAmount, oldAmount);
            }
            else {
                diffAmount = Money.subtract(oldAmount, newAmount);
            }
            mali.addAmount(amountField, diffAmount, pli);

            printAccumulators(mali, stateString, diffAmount, diffQuantity);

        }
    }


    private static void updateChangedLineItem (ChangedLineItem changedLI,
                                               int state,
                                               boolean addedLineItem,
                                               boolean addChanges,
                                               List lineItemsProcessed)
    {
        Log.contract.debug(
                "updateChangedLineItem: %s", changedLI);
        String stateString = getStateName(AvailableBalanceStates, state);
        String quantityField = Fmt.S("Quantity%s", stateString);
        String amountField = Fmt.S("Amount%s", stateString);

        ProcureLineItemCollection plic = null;
        BaseId pliBaseId = null;

        if (addedLineItem) {
            plic = (ProcureLineItemCollection)changedLI.getNewClusterRoot();
            Assert.that(plic != null,
                        "NewClusterRoot is null in %s", changedLI);
            pliBaseId = changedLI.getNewBaseObjectId();
            Assert.that(pliBaseId != null,
                        "NewBaseObjectId is null in %s", changedLI);
        }
        else {
            plic = (ProcureLineItemCollection)changedLI.getOldClusterRoot();
            Assert.that(plic != null,
                        "OldClusterRoot is null in %s", changedLI);
            pliBaseId = changedLI.getOldBaseObjectId();
            Assert.that(pliBaseId != null,
                        "OldBaseObjectId is null in %s", changedLI);
        }


        if (lineItemsProcessed.indexOf(pliBaseId) != -1) {
            return;
        }

        ProcureLineItem pli =
            (ProcureLineItem)plic.findComponentIfAny(pliBaseId);

        if (pli == null) {
            Log.contract.debug("LineItem %s was not found in Collection %s",
                                      pliBaseId, plic);
            return;
        }


        lineItemsProcessed.add(pliBaseId);
        if (!(pli instanceof ContractLineItemSource)) {
            return;
        }

        ContractLineItem mali =
            ((ContractLineItemSource)pli).getMALineItem();
        if (mali == null) {
            return;
        }

        BigDecimal quantity = pli.getQuantity();
        Money amount = pli.getAmount();
        if (addedLineItem ^ addChanges) {
            quantity = quantity.negate();
            amount = amount.negate();
        }

        mali.addQuantity(quantityField, quantity, pli);
        mali.addAmount(amountField, amount, pli);
        printAccumulators(mali, stateString, amount, quantity);
    }

    private static List getChanges (ProcureLineItemCollection plic,
                                      boolean getAllChanges)
    {
        List records = plic.getRecords();


        Log.contract.debug("getChanges(): it's about to sort change records");
        records = sort(records, RecordDateCompare);

        if (ListUtil.nullOrEmptyList(records)) {
            return records;
        }

        int numRecords = records.size();
        String uniqueName = plic.getUniqueName();

        if (getAllChanges) {
            List changes = ListUtil.list();
            for (int i = 0; i < numRecords; i++) {
                Record record = (Record)records.get(i);
                if ((record instanceof ChangeRecord) &&
                    uniqueName.equals(record.getApprovableUniqueName())) {

                    Log.contract.debug("Found ChangeRecord for %s",
                                              plic);
                    ListUtil.addElementsIfAbsent(changes,
                        ((ChangeRecord)record).getChanges());
                }
            }
            return changes;
        }
        else {

            for (int i = numRecords - 1; i >= 0; i--) {
                Record record = (Record)records.get(i);

                if ((record instanceof ChangeRecord) &&
                    uniqueName.equals(record.getApprovableUniqueName())) {

                    Log.contract.debug("Found ChangeRecord for %s",
                                              plic);
                    return ((ChangeRecord)record).getChanges();
                }
            }
        }

        return ListUtil.list();
    }

    public static List sort (List records, Compare c)
	{
        Log.contract.debug("sort(): sorting change records");
	    Object[] array = records.toArray();
	    Sort.objects(array, c);
	    return ListUtil.arrayToList(array);
	}

	private static final Compare RecordDateCompare = new Compare ()
	{

	    public int compare (Object o1, Object o2)
	    {
	        Date d1 = ((Record)o1).getDate();
	        Date d2 = ((Record)o2).getDate();
	        return d1.compareTo(d2);
	    }
    };

    private static void printAccumulators (ContractLineItem mali,
                                           String state,
                                           Money addedAmount,
                                           BigDecimal addedQuantity)
    {
        if (!Log.contract.isDebugEnabled()) {
            return;
        }

        Log.contract.debug("State = %s", state);
        Contract ma = (Contract)mali.getLineItemCollection();
        String amountField = Fmt.S("Amount%s", state);
        String quantityField = Fmt.S("Quantity%s", state);

        if (addedQuantity != null) {
            BigDecimal newQuantity =
                (BigDecimal)mali.getFieldValue(quantityField);
            Log.contract.debug(
                "%s, ContractLineItem %s, %s: added %s, new Quantity = %s",
                ma,
                mali.getBaseId(),
                quantityField,
                addedQuantity,
                newQuantity);
        }
        if (addedAmount != null) {
            Money newAmount = (Money)mali.getFieldValue(amountField);
            Log.contract.debug(
                "%s, ContractLineItem %s, %s: added %s, new Amount = %s",
                ma,
                mali.getBaseId(),
                amountField,
                addedAmount.asString(),
                newAmount.asString());
        }
    }



    public Money getTotalProcureLineItemCollectionRelease (
            ProcureLineItemCollection plic)
    {
        Money result = new Money(plic.getPartition());

        List lineItems = plic.getLineItems();
        int numLineItems = ListUtil.getListSize(lineItems);
        for (int i = 0; i < numLineItems; i++) {
            ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);
            if (pli instanceof ContractLineItemSource) {
                ContractLineItemSource clis = (ContractLineItemSource)pli;
                Contract contract = clis.getMasterAgreement();
                if (contract != null && contract.equals(this)) {
                    result = Money.add(result, pli.getAmount());
                }
            }
        }
        return result;
    }


    public static List repriceProcureLineItemCollectionWithReturn (
        ProcureLineItemCollection plic)
    {
        Contract.repriceProcureLineItemCollection(plic);



        return null;
    }


    public static void repriceProcureLineItemCollection (
        ProcureLineItemCollection plic)
    {
        if (!ApprovableUtil.isModuleInstalled(
            SystemParameters.ParameterModuleContract)) {

            return;
        }

        if (!plic.canReprice()) {
            Log.contract.debug("Cannot reprice %s", plic);
            return;
        }

        String cumulativePricingStateString =
            getCumulativePricingState(plic.getPartition());
        int cumulativePricingState =
            lookupCumulativePricingStateIdForName(
                cumulativePricingStateString);

        if ((cumulativePricingState == CumulativePricingStateSubmitted) &&
            (plic.getApprovedState() >= plic.StateSubmitted)) {
            return;
        }

        if ((cumulativePricingState == CumulativePricingStateApproved) &&
            (plic.getApprovedState() == plic.StateApproved)) {
            return;
        }

        boolean isPLICSubmitted = (plic.getApprovedState() >= plic.StateSubmitted);
        boolean isPLICApproved = (plic.getApprovedState() == plic.StateApproved);

        List lineItems = plic.getLineItems();
        int numLineItems = ListUtil.getListSize(lineItems);
        for (int i = 0; i < numLineItems; i++) {
            ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);
            Assert.that(pli instanceof ContractLineItemSource,
                        "%s is does not implement ContractLineItemSource", pli);
            ContractLineItem mali = ((ContractLineItemSource)pli).getMALineItem();
            if (mali == null) {
                continue;
            }

            PricingStructure pricingTerms = mali.getPricingStructure();

            if (!pricingTerms.hasTieredTerms()) {
                continue;
            }

            Money oldAmount = pli.getAmount();
            mali.establishAgreementPrice(pli);
            Money newAmount = pli.getAmount();
            if (oldAmount.approxCompareTo(newAmount) == 0) {
                continue;
            }

            Money diffAmount = Money.subtract(newAmount, oldAmount);
            if (isPLICSubmitted) {
                mali.addAmount(mali.KeyAmountSubmitted, diffAmount, pli);
            }

            if (isPLICApproved) {
                mali.addAmount(mali.KeyAmountApproved, diffAmount, pli);
            }
        }
        return;
    }


    public static void priceProcureLineItems (ProcureLineItem pli,
                                              boolean repriceThisReqLineItem)
    {
        Assert.that(pli != null, "pli cannot be null");
        Assert.that(pli instanceof ContractLineItemSource,
                    "%s is not an instance of ContractLineItemSource", pli);
        ContractLineItemSource maliSource = (ContractLineItemSource)pli;
        ContractLineItem mali = maliSource.getMALineItem();
        priceProcureLineItems(pli, mali, repriceThisReqLineItem);
    }

    public static void priceProcureLineItems (ProcureLineItem pli,
                                              ContractLineItem mali,
                                              boolean repriceThisReqLineItem)
    {
        ProcureLineItemCollection plic =
            (ProcureLineItemCollection)pli.getLineItemCollection();
        if (plic == null) {
            Log.pricing.debug(
                "LineItemCollection is not set for %s. Bypassing repricing " +
                "of other line items.",
                pli);
            return;
        }

        if (mali == null) {
            Log.pricing.debug(
                "MALI is null for %s. Bypassing EstablishPriceUsingMALI", pli);
            return;
        }

        Contract ma = (Contract)mali.getLineItemCollection();

        if (repriceThisReqLineItem) {
            Log.contract.debug(
                "Calling establish agreement price for rli: %s current price is %s",
                pli,
                MoneyFormatter.getStringValue(pli.getDescription().getPrice()));
            mali.establishAgreementPrice(pli);
        }



        if (!mali.getPricingStructure().hasTieredTerms()) {
            return;
        }

        List toReprice = plic.getLineItemsToReprice(pli);
        toReprice.remove(plic);
        Iterator enum_Itr = toReprice.iterator();

        while (enum_Itr.hasNext()) {
            ProcureLineItem tempPLI = (ProcureLineItem)enum_Itr.next();
            ContractLineItem tempMALI = ((ContractLineItemSource)tempPLI).getMALineItem();
            if (Log.pricing.isDebugEnabled()) {
                Log.pricing.debug(
                    "Calling establish agreement price for %s current price = %s",
                    tempPLI,
                    MoneyFormatter.getStringValue(tempPLI.getDescription().getPrice()));
            }
            tempMALI.establishAgreementPrice(tempPLI);
        }
    }

    public static List getMAForUserQuery (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        List                 fetchFields,
        AQLCondition           partitionConstraint,
        AQLCondition           globalConstraint,
        boolean               addERPConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return null;
        }
        Assert.that(user != null,
                    "Null user passed to getOpenMAQueryForUser");

        List partitionSelects = getMAForUserSelects(user,
                                                    stateField,
                                                    states,
                                                    releaseTypes,
                                                    fetchFields,
                                                    partitionConstraint,
                                                    false,
                                                    addERPConstraint);
        Assert.that(!partitionSelects.isEmpty(),
                    "Number of User Selects for a Partition should be at least 1");

        List allSelects = ListUtil.list();

        allSelects.addAll(partitionSelects);
        if (canCreateGlobalContracts()) {
            List globalSelects = getMAForUserSelects(user,
                                                 stateField,
                                                 states,
                                                 releaseTypes,
                                                 fetchFields,
                                                 globalConstraint,
                                                 true,
                                                 addERPConstraint);
            allSelects.addAll(globalSelects);
        }

        return allSelects;
    }


    public static boolean canCreateGlobalContracts ()
    {
        return !BaseUtil.isSharedServicesMode();
    }

    private static List getMAForUserSelects (
        ariba.common.core.User user,
        String                 stateField,
        List                 states,
        List                 releaseTypes,
        List                 fetchFields,
        AQLCondition           additionalConstraint,
        boolean                global,
        boolean              addERPConstraint)
    {
        Partition partition = user.getPartition();
        AQLQuery query =
            getMAQuery(partition,
                       stateField,
                       states,
                       releaseTypes,
                       fetchFields,
                       global,
                       addERPConstraint);
        if (additionalConstraint != null) {
            query.and(additionalConstraint);
        }

        if (global) {
            return getReleaseAccessSelects(Partition.Any, query, user);
        }
        else {
            return getReleaseAccessSelects(partition, query, user);
        }
    }


    public static AQLQuery getMAQuery (Partition partition,
                                       String stateField,
                                       List states,
                                       List releaseTypes,
                                       List fetchFields,
                                       boolean global,
                                       boolean addERPConstraint)
    {
        if (!isMasterAgreementEnabled()) {
            return null;
        }
        Assert.that(partition != null,
                    "Null partition passed to getOpenMasterAgreements");

        AQLClassReference maCR = null;

        if (global) {
            maCR = new AQLClassReference(ClassName,
                                         false,
                                         Partition.AnyVector);
        }
        else {
            maCR = new AQLClassReference(ClassName,
                                         false,
                                         partition.vector());
        }

        AQLQuery query = new AQLQuery(maCR);

        if (addERPConstraint && getIsERPPushEnabledInAnyPartition()) {
            AQLClassReference npCR = new AQLClassReference(NamedPair.ClassName,
                                                           false,
                                                           Partition.AnyVector);
            query.addClass(npCR, KeyERPContractNumbers, true);
            AQLCondition notPushedCondition = AQLCondition.buildIsNull(
                npCR.buildField(NamedPair.KeyName));
            AQLCondition releaseCondition =
                AQLCondition.buildNot(
                    AQLCondition.buildEqual(
                        npCR.buildField(NamedPair.KeyName),
                        partition.getName()));
            releaseCondition = releaseCondition.or(notPushedCondition);
            query.and(releaseCondition);
        }



        if (global) {
            query.and(
                AQLCondition.buildEqual(maCR.buildField(KeyGlobalReleaseFlag),
                                        Boolean.TRUE));
        }
        else {
            query.and(
                AQLCondition.buildEqual(maCR.buildField(KeyGlobalReleaseFlag),
                                        Boolean.FALSE));
        }


        query.setDistinct(true);


        if (!ListUtil.nullOrEmptyList(states)) {
            int numStates = states.size();
            if (numStates == 1) {
                query.andEqual(stateField, ListUtil.firstElement(states));
            }
            else {
                List stateConditions = ListUtil.list(numStates);
                for (int i = 0; i < numStates; i++) {
                    stateConditions.add(
                        AQLCondition.buildEqual(maCR.buildField(stateField),
                                                states.get(i)));
                }
                query.and(AQLCondition.buildOr(stateConditions));
            }
        }


        if (!ListUtil.nullOrEmptyList(releaseTypes)) {
            int numReleaseTypes = releaseTypes.size();
            if (numReleaseTypes == 1) {
                query.andEqual(KeyReleaseType, ListUtil.firstElement(releaseTypes));
            }
            else {
                List releaseTypeConditions = ListUtil.list(numReleaseTypes);
                for (int i = 0; i < numReleaseTypes; i++) {
                    releaseTypeConditions.add(
                        AQLCondition.buildEqual(maCR.buildField(KeyReleaseType),
                                                releaseTypes.get(i)));
                }
                query.and(AQLCondition.buildOr(releaseTypeConditions));
            }
        }


        if (global && CommonSupplier.isMaskMaintained(partition)) {
            String userPartitionMask = StringUtil.bit(partition.intValue());
            AQLCondition userPartitionMaskCondition =
                AQLCondition.buildLike(maCR.buildField(CSAvailableMaskField),
                                       userPartitionMask);

            String supplierDirectMask =
                StringUtil.bit(Supplier.getPartitionSupplierDirect().intValue());
            AQLCondition supplierDirectMaskCondition =
                AQLCondition.buildLike(maCR.buildField(CSAvailableMaskField),
                                       supplierDirectMask);

            query.and(userPartitionMaskCondition.or(supplierDirectMaskCondition));
        }

        if (!ListUtil.nullOrEmptyList(fetchFields)) {
            CoreUtil.addFetchFieldSelects(
                fetchFields, Contract.ClassName, query, partition, null);
        }

        return query;
    }

    private static final String CSAvailableMaskField =
        KeyCommonSupplier + "." + CommonSupplier.KeyAvailableMask;


    private static List getReleaseAccessSelects (Partition partition,
                                                   AQLQuery query,
                                                   ariba.common.core.User user)
    {
        List raSelects = ListUtil.list();


        AQLCondition unrestricted =
            AQLCondition.buildEqual(
                query.buildField(KeyRestrictReleaseAccessFlag),
                Boolean.FALSE);


        AQLQuery unrestrictedQuery = (AQLQuery)query.clone();
        unrestrictedQuery.and(unrestricted);
        raSelects.add(AQLQuery.buildSubquery(unrestrictedQuery));


        List releaseConditions =
            ContractReleaseAccessCriteria.getReleaseAccessConstraints(partition,
                                                                query,
                                                                user);
        AQLCondition restricted =
            AQLCondition.buildEqual(
                query.buildField(KeyRestrictReleaseAccessFlag),
                Boolean.TRUE);

        if (!ListUtil.nullOrEmptyList(releaseConditions)) {
            for (int i = 0; i < releaseConditions.size(); i++) {
                AQLQuery qry = (AQLQuery)query.clone();
                qry.and(restricted);
                qry.and((AQLCondition)releaseConditions.get(i));
                raSelects.add(AQLQuery.buildSubquery(qry));
            }
        }
        return raSelects;
    }

    public boolean delete ()
    {
        boolean result  = super.delete();

        List v = this.getLineItems();
        if (v==null || result==false) {
            return result;
        }

        Iterator enum_Itr  = v.iterator();
        while (enum_Itr.hasNext()) {
            ContractLineItem mali = (ContractLineItem)enum_Itr.next();
            PricingTerms pricing = mali.getPricingTerms();
            if (pricing != null) {
                pricing.delete();
            }
        }
        return result;

    }


    public void setActive (boolean b)
    {
        super.setActive(b);

        List v = this.getLineItems();
        if (v==null) {
            return;
        }

        Iterator enum_Itr  = v.iterator();
        while (enum_Itr.hasNext()) {
            ContractLineItem mali = (ContractLineItem)enum_Itr.next();
            PricingTerms pricing = mali.getPricingTerms();
            if (pricing!=null) {
                pricing.setActive(b);
            }
        }
    }


    public void setPreviousVersion (ClusterRoot previousVersion)
    {
        if ((previousVersion instanceof Contract)) {

            super.setPreviousVersion(previousVersion);
        }
    }


    public void setParentAgreement (Contract newParentAgreement)
    {
        super.setParentAgreement(newParentAgreement);

        Contract prev = (Contract)getPreviousVersion();
        if (prev != null) {
            Contract parent = prev.getParentAgreement();
            if (parent != null) {
                List parentSubAgreements = newParentAgreement.getSubAgreements();
                parentSubAgreements.remove(prev);
            }
        }

        if (newParentAgreement != null) {
            List parentSubAgreements = newParentAgreement.getSubAgreements();
            if (!parentSubAgreements.contains(this)) {
                parentSubAgreements.add(this);
            }
        }
    }


    public static Contract lookupByUniqueName (String uniqueName,
                                                      AQLCondition andCondition)
    {
        return lookupByUniqueName(uniqueName, andCondition, false);
    }


    public static Contract lookupByUniqueName (String uniqueName,
                                                      AQLCondition andCondition,
                                                      boolean includeInactive)
    {
        AQLClassReference maCR
            = new AQLClassReference(Contract.ClassName,
                                    false,
                                    Partition.AnyVector);

        maCR.setIncludeInactive(includeInactive);

        return (Contract)lookupByUniqueField(maCR,
                                                    KeyUniqueName,
                                                    uniqueName,
                                                    andCondition);
    }


    public static Contract lookupByPayloadID (String payloadID,
                                                     AQLCondition andCondition)
    {
        return lookupByPayloadID(payloadID, andCondition, false);
    }


    public static Contract lookupByPayloadID (String payloadID,
                                                     AQLCondition andCondition,
                                                     boolean includeInactive)
    {
        AQLClassReference maCR
            = new AQLClassReference(Contract.ClassName,
                                    false,
                                    Partition.AnyVector);

        maCR.setIncludeInactive(includeInactive);

        return (Contract)lookupByUniqueField(maCR,
                                                    KeyPayloadID,
                                                    payloadID,
                                                    andCondition);
    }


    public static Contract getParentMAForExternalId (
        String externalId, List sds)
    {
        return getParentMAForExternalId(externalId, sds, false);
    }

    public static Contract getParentMAForExternalId (
        String externalId, List sds, boolean useSDUniqueNameForQuery)
    {
        AQLCondition sdCondition = null;

        if (!ListUtil.nullOrEmptyList(sds)) {
            if (useSDUniqueNameForQuery) {
                sdCondition = getSDUniqueNamesCondition(sds);
            }
            else {
                sdCondition = getSourcingDestinationsCondition(sds);
            }
        }

        Map objectExtrinsics = MapUtil.map();
        objectExtrinsics.put(Contract.KeyExternalSourcingId,
                             externalId);

        Contract ma =
            (Contract)CXMLUtil.getObjectFromExtrinsicsAndCondition(
                Contract.ClassName,
                objectExtrinsics,
                sdCondition,
                false);
        Log.contract.debug("found parent contract %s for proxyID %s",
                                  ma, externalId);
        if (ma != null && !ma.isClosed() &&
            ma.getHierarchicalType() != HierarchicalTypeStandAlone) {
            return (Contract)ma.getLatestVersion();
        }
        return null;
    }


    private static final String KeySourcingDestinationUniqueName =
                        KeySourcingDestination + "." + KeyUniqueName;
    private static AQLCondition getSDUniqueNamesCondition (List sds)
    {
        List sdConditions = ListUtil.list();
        for (int i = 0; i < sds.size(); i++) {
            BaseId sdId = (BaseId)sds.get(i);
            SourcingDestination sd = (SourcingDestination)sdId.get();
            AQLCondition sdCondition =
                AQLCondition.buildEqual(
                    new AQLFieldExpression(KeySourcingDestinationUniqueName),
                        sd.getUniqueName());
            ListUtil.addElementIfAbsent(sdConditions, sdCondition);
        }

        return  AQLCondition.buildOr(sdConditions);
    }

    private static final String lineMAField =
        KeyLineItems + "." + "MasterAgreement";

    private static final String lineHierarchyMAField =
        KeyLineItems + "." + ProcureLineItem.KeyHierarchicalTerms +
        "." + HierarchicalTerm.KeyLineItemReference +
        "." + LineItemReference.KeyLineItemCollection;

    private static final String recipientState =
        KeyRecipients + "." + ContractRecipient.KeyState;


    public List getReceivableLineItemCollections (String classname,
                                                  List approvedStates,
                                                  List orderedStates,
                                                  List receivedStates)
    {
        List rlics = ListUtil.list();
        AQLClassReference rlicCR =
            new AQLClassReference(classname,
                                  false,
                                  Partition.AnyVector);

        AQLQuery query = new AQLQuery(rlicCR);

        if (ListUtil.getListSize(approvedStates) > 0) {
            Approvable.addApprovedStateConstraints(query, rlicCR, approvedStates);
        }

        if (ListUtil.getListSize(orderedStates) > 0) {
            ReceivableLineItemCollection.
                addOrderedStateConstraints(query, rlicCR, orderedStates);
        }

        if (ListUtil.getListSize(receivedStates) > 0) {
            ReceivableLineItemCollection.
                addReceivedStateConstraints(query, rlicCR, receivedStates);
        }

        query.andNotEqual (rlicCR.buildField(Approvable.KeyStatusString),
        		           Approvable.StateDeniedString);

        AQLCondition marCondition =
            AQLCondition.buildEqual(rlicCR.buildField(lineMAField), this);


        if (!hasSubAgreements()) {
            query.and(marCondition);
        }
        else {
            AQLCondition parentCondition =
                AQLCondition.buildEqual(
                    rlicCR.buildField(lineHierarchyMAField), this);
            AQLCondition parentOrMarCondition =
                AQLCondition.buildOr(parentCondition, marCondition);
            query.and(parentOrMarCondition);
        }


        AQLOptions options = getOptions();
        AQLResultCollection results =
            Base.getService().executeQuery(query, options);

        while (results.next()) {
            BaseId id = results.getBaseId(0);
            rlics.add(id.get());
        }

        return rlics;

    }


    private static final String POMAInitialUniqueNamePath =
        "MasterAgreement" + "." +
        KeyInitialUniqueName;

    public AQLQuery getProcureLineItemCollectionsWithSingleMAQuery (String classname)
    {
        List plics = ListUtil.list();


        AQLClassReference poCR
            = new AQLClassReference(classname,
                                    false,
                                    Partition.AnyVector);
        AQLClassReference maCR
            = new AQLClassReference(ClassName,
                                    false,
                                    Partition.AnyVector);

        maCR.setIncludeInactive(true);
        AQLQuery query = new AQLQuery(poCR);


        query.addClass(maCR, "MasterAgreement", false);


        query.andEqual(maCR.buildField(KeyInitialUniqueName),
                getInitialUniqueName());

        return query;
    }


    public List getProcureLineItemCollectionsWithSingleMA (String classname)
    {
        List plics = ListUtil.list();
        AQLQuery query = getProcureLineItemCollectionsWithSingleMAQuery(classname);

        AQLOptions options = getOptions();
        AQLResultCollection results =
                Base.getService().executeQuery(query, options);
        while (results.next()) {
            BaseId id = results.getBaseId(0);
            plics.add(id.get());
        }
        return plics;
    }

    public boolean hasProcureLineItemCollectionsWithSingleMA (String classname)
    {

        AQLQuery query = getProcureLineItemCollectionsWithSingleMAQuery(classname);
        AQLOptions options = getOptions();
        return Base.getService().objectExists(query, options);

    }



    public List getOrders ()
    {

        return getProcureLineItemCollectionsWithSingleMA(
            "ariba.purchasing.core.PurchaseOrder");
    }


    public List getInvoices ()
    {
        return getProcureLineItemCollectionsWithSingleMA(
            "ariba.invoicing.core.Invoice");
    }


    public boolean hasInvoice ()
    {
        return hasProcureLineItemCollectionsWithSingleMA(
            "ariba.invoicing.core.Invoice");
    }

    public List getAllReceiptApprovables ()
    {
        List allReceipts = ListUtil.list();
        allReceipts = appendVectors(allReceipts, getReceipts());
        allReceipts = appendVectors(allReceipts, getReceiptTrackers());
        allReceipts = appendVectors(allReceipts, getMilestoneTrackers());
        return allReceipts;
    }

    public static void autoSelectForProcureLineItemCollection (
        ProcureLineItemCollection plic)
    {
        if (!isMasterAgreementEnabled()) {
            return;
        }

        List lineItems = plic.getLineItems();
        int numItems = ListUtil.getListSize(lineItems);
        if (numItems == 0) {
            return;
        }

        ProcureLineItem firstItem = (ProcureLineItem)ListUtil.firstElement(lineItems);
        Assert.that(firstItem instanceof ContractLineItemSource,
                    "Item %s from %s is not a ContractLineItemSource");
        boolean callAutoSelect = true;

        for (int i = 0; i < numItems; i++) {
            ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);

            if (!((ContractLineItemSource)pli).isDirectRelease()) {
                ContractLineItem.autoSelectMALineItem(pli);
            }
            else {
                ContractLineItem mali = ((ContractLineItemSource)pli).getMALineItem();
                if (mali == null) {

                    Log.contract.warning(8137, pli);
                    continue;
                }
                mali.establishAgreementPrice(pli);
            }
        }
        return;
    }


    public String defaultHumanReadableExpression ()
    {
        ClassProperties cp = ClassProperties.getClassProperties(getClassName(),
                                                                getVariant());
        User effectiveUser = User.getEffectiveUser();
        if (ApprovableType.canQueryAll(effectiveUser,
                                       getPartition(),
                                       getClassName()))
        {
            return Fmt.Sil(Base.getSession().getLocale(),
                           StringTable,
                           MACAQueryAllKey,
                           cp.getUiName());
        }
        return Fmt.Sil(Base.getSession().getLocale(),
                       StringTable,
                       MARestrictedQueryAllKey,
                       cp.getUiName());
    }

    private static final String SupplierSearchGroup =
        "MasterAgreementSupplierSearchFields";

    public String getSupplierSearchGroup ()
    {
        return SupplierSearchGroup;
    }

    public List getSupplierSearchConstraints (AQLQuery query)
    {
        User effectiveUser = User.getEffectiveUser();
        Assert.that(
            effectiveUser.getOrganization() instanceof CommonSupplier,
            "getSupplierSearchConstraint can not be called for a none supplier user.");
        return supplierSearchConditions(effectiveUser);
    }

    public static List supplierSearchConditions (User user)
    {
        return supplierSearchConditions(user, true, null);
    }

    public static List supplierSearchConditions (User user,
                                                 boolean isInvoiceableOnly,
                                                 AQLClassReference classRef)
    {
        List conditions = ListUtil.list();

        AQLCondition cond = AQLCondition.buildEqual(
            classRef != null ?
                classRef.buildField(Contract.KeyReleaseType) :
                new AQLFieldExpression(KeyReleaseType),
            Constants.getInteger(ReleaseTypeNone));
        conditions.add(cond);


        if (isInvoiceableOnly) {
           cond = AQLCondition.buildEqual(
               classRef != null ?
                   classRef.buildField(Contract.KeyIsInvoiceable) :
                   new AQLFieldExpression(KeyIsInvoiceable), Boolean.TRUE);
            conditions.add(cond);
        }



        CommonSupplier cs = (CommonSupplier)user.getOrganization();
        cond = AQLCondition.buildEqual(
            classRef != null ?
                classRef.buildField(Contract.KeyCommonSupplier) :
                new AQLFieldExpression(KeyCommonSupplier), cs);
        conditions.add(cond);

        return conditions;
    }


    public List getUsersInReleaseAccessCriteriaForMA ()
    {
        List releaseUsers = ListUtil.list();
        Variant variant = this.getVariant();
        Partition partition = this.getPartition();
        AQLQuery queryUsers;
        String method = "getUsersInReleaseAccessCriteriaForMA";
        ContractReleaseAccessCriteria crac = this.getReleaseAccessCriteria();


        for (int i = crac.FirstLocalFieldIndex; i < crac.FieldNames.length; i++) {
            String fieldName = crac.FieldNames[i];
            Log.contract.debug("%s: handling intrinsic field: %s",
                                      method, fieldName);


            if (i == crac.getIndexForUsersField()) {
                ListUtil.addElementsIfAbsent(releaseUsers, crac.getUsers());
            }

            else if (i == crac.getIndexForSupervisorField()) {
                queryUsers = AQLQuery.parseQuery(
                        (Fmt.S("SELECT DISTINCT u FROM %s u, %s c " +
                               "WHERE c = BaseId('%s') AND c.%s.%s = u.%s",
                                User.ClassName, Contract.ClassName,
                                this.getBaseId().toDBString(),
                                KeyReleaseAccessCriteria, User.KeySupervisor,
                                User.KeySupervisor)));
                ListUtil.addElementsIfAbsent(releaseUsers,
                        BaseUtil.executeQuery(queryUsers, partition));
            }

            else if (i == crac.getIndexForRolesField()) {
                List roles = crac.getRoles();
                for (int k = 0, sz = roles.size(); k < sz; k++) {
                    BaseId roleBid = (BaseId)roles.get(k);
                    Role role = (Role)roleBid.get();
                    List roleUsers = role.getAllUsers();
                    ListUtil.addElementsIfAbsent(releaseUsers, roleUsers);
                }
            }

            else if (i == crac.getIndexForGroupsField()) {
                List groups = crac.getGroups();
                for (int k = 0, sz = groups.size(); k < sz; k++) {
                    BaseId groupBid = (BaseId)groups.get(k);
                    Group group = (Group)groupBid.get();
                    List groupUsers = group.getAllUsers();
                    ListUtil.addElementsIfAbsent(releaseUsers, groupUsers);
                }
            }
        }


        BaseMeta baseMeta = Base.getService().getBaseMeta(
            ContractReleaseAccessCriteria.ClassName, variant);
        FieldMeta[] extrinsicMetas = baseMeta.getExtrinsics();
        if (extrinsicMetas == null || extrinsicMetas.length == 0) {
            return releaseUsers;
        }

        for (int i = 0; i < extrinsicMetas.length; i++) {
            FieldMeta fieldMeta = extrinsicMetas[i];
            String fieldName = fieldMeta.name();
            Log.contract.debug(
                "%s: handling extrinsic field: %s",
                      method, fieldName);
            String fieldClassName = fieldMeta.type.className;
            BaseMeta fieldBaseMeta =
                Base.getService().getBaseMeta(fieldClassName, variant);
            if (partition.equals(Partition.Any) &&
                fieldBaseMeta.isPartitioned()) {

                Log.contract.debug(
                    "%s: Partitioned extrinsic field %s not added to Partition.Any query",
                         method, fieldName);
            }
            else {

                String userFieldName =
                    (fieldName.equals(ariba.common.core.User.KeyShipTo))?
                     Fmt.S("%s.%s", ariba.common.core.User.KeyUser, User.KeyShipTos):
                     crac.getFieldName(fieldName);

                queryUsers = AQLQuery.parseQuery(
                        (Fmt.S("SELECT DISTINCT u.%s FROM %s u, %s c " +
                               "WHERE c = BaseId('%s') AND c.%s.%s = u.%s",
                                ariba.common.core.User.KeyUser,
                                ariba.common.core.User.ClassName, Contract.ClassName,
                                this.getBaseId().toDBString(),
                                KeyReleaseAccessCriteria, fieldName,
                                new Object[]{userFieldName})));
                ListUtil.addElementsIfAbsent(releaseUsers,
                        BaseUtil.executeQuery(queryUsers, partition));

                Log.contract.debug(
                    "%s: Partitioned extrinsic field %s added to query",
                         method, fieldName);
            }
        }
        return releaseUsers;
    }


    public void attachMALineItem (ProcureLineItem pli)
    {
        Assert.that(pli instanceof ContractLineItemSource,
                    "%s does not implement ContractLineItemSource", pli);
        ContractLineItemSource maLineItemSource = (ContractLineItemSource)pli;
        Assert.that(maLineItemSource.isDirectRelease(),
                    "This method can only be called when manually attaching " +
                    "a line item to a ProcureLineItemCollection.");

        ProcureLineItemCollection plic =
            (ProcureLineItemCollection)pli.getLineItemCollection();
        Assert.that(plic instanceof ContractSelectable,
                    "%s does not implement ContractSelectable", plic);
        Contract selectedMA =
            ((ContractSelectable)plic).getSelectedMasterAgreement();
        Assert.that(SystemUtil.equal(selectedMA, this),
                    "Selected MA on %s does not equal current MA, %s",
                    selectedMA, this);

        if (selectedMA.shouldMAAttachedToNonCatalogItems(pli)) {
            List maliList = ContractLineItem.getMatchingMALIs(pli, ListUtil.list(this));
            int numMatches = ListUtil.getListSize(maliList);
            if (numMatches == 0) {
                Log.contract.warning(4981, this, pli, plic);
            }
            else {
                maLineItemSource.setMALineItem(
                    (ContractLineItem)ListUtil.firstElement(maliList));
            }
        }
    }


    protected boolean shouldMAAttachedToNonCatalogItems (ProcureLineItem pli)
    {
        ProcureCategoryUtil pcu = ProcureCategoryUtil.get(pli);
        if (pcu.isItemAdHocForContractPricing(pli) &&
            (this.isTermTypeSupplier() || this.isTermTypeCommodity()) &&
            !(this.getAllowSupAndCommTermsToNonCatalogItems() ||
              this.getIsNonCatalogItemsAccumulated())) {
                Log.contract.debug(
                    "attacheMALineItem Bypasing pricing as pli %s",
                    "is adhoc, term type is not item, and the adhoc not accumulates");
                return false;
        }
        else {
            return true;
        }
    }


    public static final String EventSend        = "MasterAgreement:Send";
    public static final String EventSent        = "MasterAgreement:Sent";
    public static final String EventCreated     = "MasterAgreement:Created";
    public static final String EventApproved    = "MasterAgreement:Approved";
    public static final String EventOpen        = "MasterAgreement:Open";
    public static final String EventClose       = "MasterAgreement:Close";
    public static final String EventUnclose     = "MasterAgreement:Unclose";
    public static final String EventReceive     = "MasterAgreement:Receive";
    public static final String EventProcess     = "MasterAgreement:Process";
    public static final String EventProcessed   = "MasterAgreement:Processed";
    public static final String EventProcessingCompleted =
        "MasterAgreement:ProcessingCompleted";
    public static final String EventPushFailed =
        "MasterAgreement:PushFailed";


    public static final String TopicOpen        =
        MessageUtil.topicWithPrefix(MessageUtil.GlobalPrefix, EventOpen);
    public static final String TopicClose       =
        MessageUtil.topicWithPrefix(MessageUtil.GlobalPrefix, EventClose);

    private static final int ValidateMAOkay = 0;
    private static final int ValidateMAAutoWithdrawHigherAmount = 1;
    private static final int ValidateMAAutoWithdrawMAClosed = 2;
    private static final int ValidateMACantSubmitMAClosed = 3;
    private static final int ValidateMAWarningHigherAmount = 4;
    private static final int ValidateMACantSubmitMAClosedErrorCode = -1;

    private static final String CantSubmitReqMAClosedKey =
        "MACantSubmitReqMAClosed";
    private static final String ReqWarningHigherAmountKey =
        "MAReqWarningHigherAmount";
    private static final String AutoWithdrawReqMAClosedKey =
        "MAAutoWithdrawReqMAClosed";
    private static final String AutoWithdrawReqHigherAmountKey =
        "MAAutoWithdrawReqHigherAmount";
    private static final String AutoWithdrawReqHeaderKey =
        "MAAutoWithdrawReqHeader";
    private static final String AutoWithdrawnReqSubjectKey =
        "MAAutoWithdrawnReqSubject";

    public void publishEvent (String event)
    {
        publishEvent(event, false);
    }

    public void publishEvent (String event, boolean synchronously)
    {
        Log.contract.debug("Adding Event %s for %s", event, this);

        if (synchronously) {
            try {
                WorkflowService.getService().processEventSync(this, event);
            }
            catch (EventQueueException eqe) {
                Log.contract.error(6827, event, this, eqe);
            }
        }
        else {
            WorkflowService.getService().fireWorkflowEvent(this, event);
        }
    }


    public int checkOpen ()
    {
        User user = User.getEffectiveUser();
        return getAccess(OpenMAAccess, user);
    }


    public boolean open ()
    {
        return open(null, false);
    }


    public boolean open (Comment comment)
    {
        return open(comment, false);
    }


    public boolean open (Comment comment, boolean synchronously)
    {
        Log.contract.debug("Open %s", this);
        if (Log.approvable.isDebugEnabled()) {
            printCluster(Log.approvable);
        }

        if (isOpen()) {
            Log.contract.debug("%s is already open",
                                      this.getUniqueName());
            return true;
        }

        User user = User.getEffectiveUser();
        if (user == null) {
            user = User.getAribaSystemUser(getPartition());
        }


        Comment realComment = addComment(comment, user);


        ContractRecord record =
            new ContractRecord(this, user, realComment, ContractRecord.MAOpened);


        publishEvent(EventOpen, synchronously);

        Base.getSession().save(this);
        return true;
    }


    public int checkClose ()
    {
        User user = User.getEffectiveUser();
        return getAccess(CloseMAAccess, user);
    }


    public boolean close (int reason)
    {
        return close(reason, null, false);
    }


    public boolean close (int reason, Comment comment)
    {
        return close(reason, comment, false);
    }


    private boolean m_closing = false;


    public boolean close (int reason, Comment comment, boolean synchronously)
    {
        Log.contract.debug("Close %s", this);
        if (Log.approvable.isDebugEnabled()) {
            printCluster(Log.approvable);
        }

        if (isClosed()) {
            Log.contract.debug("%s is already closed", this);
            return true;
        }

        if (m_closing) {
            Log.contract.debug("%s is in the process of closing", this);
            return true;
        }

        m_closing = true;
        User user = User.getEffectiveUser();
        if (user == null) {
            user = User.getAribaSystemUser(getPartition());
        }


        Comment realComment = addComment(comment, user);


        ContractRecord record =
            new ContractRecord(this, user, comment, ContractRecord.MAClosed, reason);


        publishEvent(EventClose, synchronously);

        Base.getSession().save(this);

        return true;
    }


    public void unclose (Comment comment, boolean synchronously)
    {
        Log.contract.debug("Unclose %s", this);
        if (Log.approvable.isDebugEnabled()) {
            printCluster(Log.approvable);
        }

        if (getUncloseMAAccess() != Access.Now) {
            return;
        }

        User user = User.getEffectiveUser();
        if (user == null) {
            user = User.getAribaSystemUser(getPartition());
        }


        Comment realComment = addComment(comment, user);


        ContractRecord record =
            new ContractRecord(this, user, comment, ContractRecord.MAUnclosed);


        publishEvent(EventUnclose, synchronously);

        Base.getSession().save(this);
    }


    public boolean addToOutBoxOnCreate ()
    {
        return false;
    }


    public Approvable createNextVersion (User authUser)
    {
        Contract nextVersion =
            (Contract)super.createNextVersion(authUser);
        List changes = nextVersion.diffPrevious();
        if (!ListUtil.nullOrEmptyList(changes)) {
            ChangeRecord record = new ChangeRecord(nextVersion,
                                                   this,
                                                   getRequester(),
                                                   null,
                                                   changes);
        }



        List newContractLineItems = nextVersion.getLineItems();
        int numNewContractLineItems = ListUtil.getListSize(newContractLineItems);
        for (int i = 0; i < numNewContractLineItems; i++) {

            ContractLineItem newCli =
                (ContractLineItem)newContractLineItems.get(i);
            if (!nextVersion.getIsReceivable()) {

                newCli.setReceivingType(ReceivableLineItem.NoReceipt);
            }
            else {



                newCli.setReceivingType(newCli.findReceivingType());
            }
        }


        List subAgreements = getSubAgreements();
        if (!ListUtil.nullOrEmptyList(subAgreements)) {
            Iterator subAgreementsIterator = subAgreements.iterator();
            while (subAgreementsIterator.hasNext()) {
                BaseId subId = (BaseId)subAgreementsIterator.next();
                Contract sub  = (Contract)subId.get();
                sub.setParentAgreement(nextVersion);
                ContractRequest subRequest = sub.getMasterAgreementRequest();
                subRequest.setDottedFieldValueWithoutTriggering(
                        KeyParentAgreement, nextVersion);
            }
        }


        nextVersion.setInvoicedState(getInvoicedState());


        nextVersion.deactivatePreviousVersion();

        return nextVersion;
    }

    public void deactivatePreviousVersion ()
    {
        Approvable previous = (Approvable)getPreviousVersion();
        if (previous == null) {
            return;
        }
        Log.contract.debug("Deactivate the previous version: %s",
                                  previous);

        deactivateMilestoneTrackers(previous);
        previous.setActive(false);
    }

    private void deactivateMilestoneTrackers (Approvable previous)
    {
        Assert.that(previous instanceof Contract,
                    "Expecting Contract, got " + previous.getClass().getName());

        List mts = ((Contract)previous).getMilestoneTrackers();


        for (int i = 0; i < mts.size(); i++) {
            BaseId mtId = (BaseId)mts.get(i);
            MilestoneTracker mt = (MilestoneTracker)mtId.get();
            mt.setActive(false);
        }
    }

    private void activateMilestoneTrackers (Approvable previous)
    {
        Assert.that(previous instanceof Contract,
                    "Expecting Contract, got " + previous.getClass().getName());

        List mts = ((Contract)previous).getMilestoneTrackers();


        for (int i = 0; i < mts.size(); i++) {
            BaseId mtId = (BaseId)mts.get(i);
            MilestoneTracker mt = (MilestoneTracker)mtId.get();
            mt.setActive(true);
        }
    }

    public void updateMatchedInvoicesForChangedMA ()
    {
        Contract previousMA = (Contract)getPreviousVersion();
        if (previousMA != null) {
            updateInvoicesWithNewVersion(previousMA);
            Base.getSession().sessionCommit();
            reconcileInvoices();
        }
    }

    public void updateMatchedInvoices ()
    {
        reconcileInvoices();
    }

    public void updateSubmittedRLICsForChangedMA (String classname)
    {
        Contract previousMA = (Contract)getPreviousVersion();
        if (previousMA != null) {
            previousMA.updateSubmittedRLICsForMA(classname);
        }
    }

    public void updateSubmittedRLICsForClosedMA (String classname)
    {
        updateSubmittedRLICsForMA(classname);
    }

    private List getSubmittedUnorderedRLICsForMA (String classname)
    {
        List approvedStates = ListUtil.list(Constants.getInteger(StateSubmitted),
                                            Constants.getInteger(StateApproved));
        List orderedStates = ListUtil.list(Constants.getInteger(Unordered));
        return getReceivableLineItemCollections(classname,
                                                approvedStates,
                                                orderedStates,
                                                null);
    }

    private void updateSubmittedRLICsForMA (String classname)
    {
        List rlics = getSubmittedUnorderedRLICsForMA(classname);
        int numRLICs = rlics.size();
        if (numRLICs == 0) {
            return;
        }

        User effectiveUser = User.getEffectiveUser();
        Log.contract.debug("Prev effective user = %s", effectiveUser);
        User aribaSystemUser = User.getAribaSystemUser(getPartition());


        Base.getSession().setEffectiveUser(aribaSystemUser.id);
        Log.contract.debug("Setting effective user to %s", aribaSystemUser);

        for (int i = 0; i < numRLICs; i++) {
            ReceivableLineItemCollection rlic =
                (ReceivableLineItemCollection)rlics.get(i);

            if (rlic.hasPreviousVersion()) {
                continue;
            }
            boolean withdrawRLIC = false;
            Approver prevLocker =
                forceCheckoutReceivableLineItemCollection(rlic, aribaSystemUser);

            List procureLineItems = rlic.getLineItems();
            checkIfProcureLineItemsAreContractLineItemSource(procureLineItems);
            int numProcureLineItems = procureLineItems.size();
            List results = ListUtil.list();
            for (int j = 0; j < numProcureLineItems; j++) {
                ProcureLineItem pli = (ProcureLineItem)procureLineItems.get(j);
                Contract pliMA = ((ContractLineItemSource)pli).getMasterAgreement();


                if (pliMA == null) {
                    continue;
                }

                Log.contract.debug(
                    "The current pli for rlic is priced by %s",
                    pliMA.getUniqueName());
                if (isPLIPricedFromMasterAgreement(this, pli)) {
                    Log.contract.debug(
                        "Processing RLIc's Item %s:%s",
                        pli.getLineItemCollection().getUniqueName(),
                        pli);
                    int result = validateMAOnProcureLineItem(pli, results, true);
                    if ((result == ValidateMAAutoWithdrawMAClosed) ||
                        (result == ValidateMAAutoWithdrawHigherAmount)) {
                        withdrawRLIC = true;
                    }
                }
            }

            if (withdrawRLIC) {
                if (rlic.isApproved()) {
                    removeReceivableLineItemCollectionFromWorkflow(rlic, results);
                }
                withdrawReceivableLineItemCollection(rlic, results);
            }
            else {
                if (rlic.isApproved()) {
                    if (rlic.getEditAccess() !=
                         Approvable.EditAllowedWithoutEffects) {
                        removeReceivableLineItemCollectionFromWorkflow(rlic, results);
                        checkinReceivableLineItemCollection(rlic, prevLocker);
                    }
                    else {
                        rlic.setLocker(null);
                    }
                }
                else {
                    checkinReceivableLineItemCollection(rlic, prevLocker);
                }
            }
        }


        if (effectiveUser != null) {
            Log.contract.debug("Setting effective user back to %s",
                                      aribaSystemUser);
            Base.getSession().setEffectiveUser(effectiveUser.id);
        }

        else {
            Log.contract.debug("Setting effective user back to %s",
                                      effectiveUser);
            Base.getSession().setEffectiveUser(null);
        }
    }

    private static void removeReceivableLineItemCollectionFromWorkflow (
        ReceivableLineItemCollection rlic,
        List results)
    {
        rlic.setApprovedState(rlic.StateSubmitted);
        Iterator workflows = Workflow.findAllForBO(rlic);

        if (workflows != null && workflows.hasNext()) {
            WorkflowService.getService().abortWorkflows(rlic);
        }
        else {

            withdrawReceivableLineItemCollection(rlic, results);
        }
    }

    private static void withdrawReceivableLineItemCollection (
        ReceivableLineItemCollection rlic,
        List results)
    {
        User user = User.getEffectiveUser();
        rlic.setLocker(null);

        Locale locale = rlic.getRequester().getLocale();

        FastStringBuffer body = new FastStringBuffer();
        body.append(Fmt.Sil(locale,
                            StringTable,
                            AutoWithdrawReqHeaderKey,
                            rlic.getUniqueName()));
        int numResults = results.size();
        for (int i = 0; i < numResults; i++) {
            Object result = results.get(i);
            if (!(result instanceof String)) {
                continue;
            }
            String message = (String)result;
            body.append(Fmt.S("\t%s\n", message));
        }

        Comment comment = new Comment(rlic.getPartition());
        comment.setType(Comment.TypeWithdraw);
        comment.setBody(body.toString());


        rlic.withdraw(null, true);

        String subjectAndMessage =
            Fmt.Sil(locale, StringTable, AutoWithdrawnReqSubjectKey, "");
        rlic.sendMail(rlic.getRequester(),
                     Notifications.Withdraw,
                     subjectAndMessage,
                     subjectAndMessage,
                     Approvable.WithdrawnHelpURL,
                     false);


        Comment realComment = rlic.addComment(comment, user);


        SimpleRecord r = new SimpleRecord(rlic,
                                          user,
                                          realComment,
                                          SimpleRecord.Withdraw);
    }

    private static Approver forceCheckoutReceivableLineItemCollection (
        ReceivableLineItemCollection rlic,
        Approver                     newLocker)
    {
        if (changeLocker(rlic, newLocker)) {
            return null;
        }

        Approver currentLocker = rlic.getLocker();
        Log.procurement.debug("Removing %s as locker of %s", currentLocker, rlic);
        rlic.setLocker(null);
        changeLocker(rlic, newLocker);
        return currentLocker;
    }

    private static void checkinReceivableLineItemCollection (
        ReceivableLineItemCollection rlic,
        Approver                     prevLocker)
    {
        ApprovableServer appServer = ApprovableUtil.getService().getApprovableServer();
        List changes = rlic.getChanges();
        if (ListUtil.nullOrEmptyList(changes)) {
            rlic.setLocker(null);
            changeLocker(rlic, prevLocker);
        }
        else {
            appServer.checkin(rlic, null);
        }
    }


    public static List validateMAsOnRLIC (ReceivableLineItemCollection rlic,
                                          boolean autoSelectIfNoMA)
    {

        if (!rlic.isUnordered()) {
            Log.contract.debug(
                "validateMAOnRLIC: RLIC %s is past the ordering state.  " +
                "Validation not performed.",
                rlic);
            return null;
        }

        return validateMAsOnPLIC (rlic,
                                  autoSelectIfNoMA,
                                  true);
    }


    public static List validateMAsOnPLIC (ProcureLineItemCollection rlic,
                                          boolean autoSelectIfNoMA,
                                          boolean accumulate)
    {


        if (!ApprovableUtil.isModuleInstalled(
            SystemParameters.ParameterModuleContract)) {

            return null;
        }

        List results = ListUtil.list();



        if (rlic.hasPreviousVersion()) {
            Log.contract.debug(
                "validateMAOnRLIC: RLIC %s has previous version.  " +
                "Validation not performed.",
                rlic);
            return null;
        }

        CategoryPropertyCache.usePropertyCache();
        try {

            List procureLineItems = rlic.getLineItems();
            checkIfProcureLineItemsAreContractLineItemSource(procureLineItems);
            int numProcureLineItems = procureLineItems.size();
            for (int i = 0; i < numProcureLineItems; i++) {
                ProcureLineItem pli = (ProcureLineItem)procureLineItems.get(i);
                Contract ma = ((ContractLineItemSource)pli).getMasterAgreement();
                if (ma != null) {
                    int lineItemResult = ma.validateMAOnProcureLineItem(
                        pli, results, accumulate);
                }
                else if (autoSelectIfNoMA) {
                    ContractLineItem.autoSelectMALineItem(pli);
                }
            }
            if (!results.isEmpty()) {
                return results;
            }

            return null;
        }
        finally {
            CategoryPropertyCache.clearPropertyCache();
        }
    }

    public int validateMAOnProcureLineItem (
        ProcureLineItem pli, List results, boolean accumulate)
    {
        checkIfProcureLineItemIsContractLineItemSource(pli);
        ContractLineItemSource maliSource = (ContractLineItemSource)pli;
        Contract ma = this;
        ContractLineItem mali = getMALineItemForMAOnPLI(pli);
        Assert.that(mali != null,
                    "%s is not priced by valid ContractLineItem ", pli);

        ProcureLineItemCollection plic =
            (ProcureLineItemCollection)pli.getLineItemCollection();
        boolean isPLICSubmitted = (plic.getApprovedState() >= plic.StateSubmitted);
        boolean maHasNextVersion = ma.hasNextVersion();

        Money originalAmount = pli.getAmount();
        boolean setNextMALI = true;

        if (maHasNextVersion) {
            ma = (Contract)ma.getLatestVersion();
            int maliNum = mali.getNumberInCollection();
            mali = (ContractLineItem)ma.getLineItems().get(maliNum - 1);
        }

        if (ma.isClosed() || !mali.hasValidAvailableBalance()) {
            String availableBalanceState =
                Base.getService().getParameter(ma.getPartition(),
                                               ma.ParamReleaseAvailableBalanceState);
            int availableBalanceStateId =
                ma.lookupAvailableBalanceStateIdForName(availableBalanceState);

            boolean maHasValidAvailableBalance = ma.hasValidAvailableBalance();
            boolean maliHasValidAvailableBalance = mali.hasValidAvailableBalance();

            if (isPLICSubmitted && accumulate) {

                if ((maHasValidAvailableBalance && maliHasValidAvailableBalance) ||

                    ((!maHasValidAvailableBalance ||
                      !maliHasValidAvailableBalance) &&
                     (availableBalanceStateId > ma.AvailableBalanceStateSubmitted) &&
                     !plic.isApproved()) ||

                    ((!maHasValidAvailableBalance ||
                      !maliHasValidAvailableBalance) &&
                     (availableBalanceStateId > ma.AvailableBalanceStateApproved) &&
                     plic.isApproved())) {

                    if (plic.isApproved()) {
                        mali.addAmount(ContractLineItem.KeyAmountApproved,
                                       pli.getAmount().negate(),
                                       pli);
                        mali.addQuantity(ContractLineItem.KeyQuantityApproved,
                                         pli.getQuantity().negate(),
                                         pli);
                    }

                    if (maliSource.isDirectRelease()) {
                        if (plic.isDenied()) {
                            results.add(
                                Constants.getInteger(
                                        ValidateMACantSubmitMAClosedErrorCode));
                            results.add(Fmt.Sil(
                                plic.getRequester().getLocale(),
                                StringTable,
                                CantSubmitReqMAClosedKey,
                                maliSource.getMasterAgreement().getUniqueName(),
                                Constants.getInteger(pli.getNumberInCollection())));
                            return ValidateMACantSubmitMAClosed;
                        }
                        else {

                            results.add(
                                Fmt.Sil(plic.getRequester().getLocale(),
                                        StringTable,
                                        AutoWithdrawReqMAClosedKey,
                                        maliSource.getMasterAgreement().getUniqueName(),
                                        Constants.getInteger(
                                            pli.getNumberInCollection())));
                            return ValidateMAAutoWithdrawMAClosed;
                        }
                    }
                    else {
                        mali.addAmount(ContractLineItem.KeyAmountSubmitted,
                                       pli.getAmount().negate(),
                                       pli);
                        mali.addQuantity(ContractLineItem.KeyQuantitySubmitted,
                                         pli.getQuantity().negate(),
                                         pli);
                        maliSource.setMALineItem(null);

                        ContractLineItem.autoSelectMALineItem(pli);
                        ContractLineItem newMALI = maliSource.getMALineItem();
                        if (newMALI != null) {
                            newMALI.addAmount(ContractLineItem.KeyAmountSubmitted,
                                              pli.getAmount(),
                                              pli);
                            newMALI.addQuantity(ContractLineItem.KeyQuantitySubmitted,
                                                pli.getQuantity(),
                                                pli);
                        }
                        setNextMALI = false;
                    }
                }
            }
            else {

                if (maliSource.isDirectRelease()) {
                    results.add(
                        Constants.getInteger(ValidateMACantSubmitMAClosedErrorCode));
                    results.add(
                        Fmt.Sil(plic.getRequester().getLocale(),
                                StringTable,
                                CantSubmitReqMAClosedKey,
                                maliSource.getMasterAgreement().getUniqueName(),
                                Constants.getInteger(pli.getNumberInCollection())));
                    return ValidateMACantSubmitMAClosed;
                }
                else {
                    maliSource.setMALineItem(null);
                    ContractLineItem.autoSelectMALineItem(pli);
                    setNextMALI = false;
                }
            }
        }

        if (maHasNextVersion &&
            setNextMALI &&
            isPLIPricedDirectlyByMA(pli)) {

            maliSource.setMALineItem(mali);
        }
        else {
            priceProcureLineItems(pli, true);
        }

        Money newAmount = pli.getAmount();
        int amountCompare = originalAmount.approxCompareTo(newAmount);


        boolean sysparam = Base.getService().getBooleanParameter(
                            this.getPartition(),
                            ParamAlwaysRegenerateApprovalGraph);

        if ((amountCompare == -1) || (sysparam && ma.isClosed())) {
            if (isPLICSubmitted) {
                if (maliSource.isDirectRelease()) {
                    results.add(
                        Fmt.Sil(plic.getRequester().getLocale(),
                                StringTable,
                                AutoWithdrawReqHigherAmountKey,
                                Constants.getInteger(pli.getNumberInCollection()),
                                originalAmount.asString(),
                                newAmount.asString()));
                    return ValidateMAAutoWithdrawHigherAmount;
                }
                else {
                    int access = plic.getEditAccess();
                    switch (access) {
                      case Approvable.EditRequiresResubmit:
                      case Approvable.EditRequiresReapprovals:
                        plic.runApprovalEngine();
                        break;


                      case Approvable.EditAllowedWithoutEffects:
                      default:
                        break;
                    }
                }
            }
            else {
                results.add(
                    Constants.getInteger(Access.CantSubmitReqAmountChanged));
                results.add(
                    Fmt.Sil(plic.getRequester().getLocale(),
                            StringTable,
                            ReqWarningHigherAmountKey,
                            Constants.getInteger(pli.getNumberInCollection()),
                            originalAmount.asString(),
                            newAmount.asString()));
                return ValidateMAWarningHigherAmount;
            }
        }
        return ValidateMAOkay;
    }

    private static void checkIfProcureLineItemsAreContractLineItemSource (
        List procureLineItems)
    {
        int numProcureLineItems = procureLineItems.size();
        if (numProcureLineItems > 0) {
            checkIfProcureLineItemIsContractLineItemSource(
                (ProcureLineItem)ListUtil.firstElement(procureLineItems));
        }
    }

    private static void checkIfProcureLineItemIsContractLineItemSource (
        ProcureLineItem pli)
    {
        Assert.that(pli instanceof ContractLineItemSource,
                    "%s does not implemenet ContractLineItemSource", pli);
    }



    protected boolean hasItemsNeedingSubscription ()
    {
        return hasItemsNeedingSubscription(true);
    }

    protected boolean hasItemsNeedingSubscription (boolean availableItemsOnly)
    {

        int termType = getTermType();
        if ((!isReleaseTypeNone() || (getSLA() == null)) &&
            (termType == TermTypeSupplier || termType == TermTypeCommodity)) {
            Log.contract.debug(
                "%s is a ReleaseTypeUser Supplier or Commodity Level without an SLA" +
                " so no subscription is needed.",
                this);
            return false;
        }


        List maLineItems = getLineItems();
        int numMALineItems = ListUtil.getListSize(maLineItems);
        for (int i = 0; i < numMALineItems; i++) {
            ContractLineItem maLineItem = (ContractLineItem)maLineItems.get(i);
            if (maLineItem.needsSubscription(availableItemsOnly)) {
                return true;
            }
        }

        return false;
    }


    protected boolean shouldPrintApprovalFlow ()
    {
        return false;
    }

    protected boolean shouldPrintComments ()
    {
        return false;
    }



    public boolean defaultFullViewAccess (User user)
    {

        return canUserChange(user) == Access.Now;
    }



    public boolean getIsSourced ()
    {
        Assert.that(
            getMasterAgreementRequest() != null,
            "MasterAgreement cannot have null Contract Request");

        return getMasterAgreementRequest().isBeingSourced();
    }



    public void receive ()
    {

    }

    public void received ()
    {

    }


    public void createReceipts ()
    {
        int receiveFromMA = getReceiveFromMAAccess();
        if (receiveFromMA != Access.Now) {
            if (Log.receiving.isDebugEnabled()) {
                Log.receiving.debug(
                    "ReceiveFromMAAccess = %s, " +
                    "receipts not created for %s",
                    Constants.getInteger(receiveFromMA), this);
            }

            if (hasPreviousVersion()) {
                getReceipts().clear();
            }
            return;
        }

        ContractRequest mar = getMasterAgreementRequest();
        DirectReceivableUtil.createReceipts(this, mar.getPreparer(), mar.getRequester());
    }

    public void submitReceipts ()
    {
        DirectReceivableUtil.submitReceipts(this);
    }

    public void updateReceipt (ReceiptCoreApprovable receipt)
    {
        boolean overLimit = false;

        Money totalAccepted = new Money(Constants.ZeroBigDecimal, getCurrency());
        Iterator receiptItems = receipt.getReceiptItemsIterator();
        while (receiptItems.hasNext()) {
            ReceiptItem receiptItem =
                (ReceiptItem)receiptItems.next();
            totalAccepted.addTo(receiptItem.getTotalAmountAccepted());
            if (receiptItem.isOverLimit()) {
                overLimit = true;
            }
        }

        if (!overLimit) {

            Money limit = getInternalMaxAmount();
            if (limit != null &&
                Money.subtract(limit, totalAccepted).getAmount().signum() < 0 ) {
                overLimit = true;
            }
        }



        if (overLimit) {

            Log.receiving.debug(
                "Receipt %s has became over MA limit after MA changed.", receipt);

        }

    }

    public void addReceiptCoreApprovable (ReceiptCoreApprovable rca)
    {
        if (rca instanceof Receipt) {
            addReceipt((Receipt)rca);
        }
        else if (rca instanceof ReceiptTracker) {
            addReceiptTracker((ReceiptTracker)rca);
        }
    }


    public void addReceipt (Receipt receipt)
    {
        getReceipts().add(receipt);
    }


    public void addReceiptTracker (ReceiptTracker receiptTracker)
    {
        getReceiptTrackers().add(receiptTracker);
    }

    public void checkForNewVersions (ReceiptCoreApprovable receipt)
    {

    }



    public DateRange getValidReceivingPeriod ()
    {
        return getValidPeriod();
    }

    public static final String MAReceivingTypeParameter =
        ParamMA + "MAReceivingTypeMethod";


    public static final String ParamERPPushEnabled =
        ParamMA + "ParamERPPushEnabled";

    protected String getReceivingTypeMethodName (Partition partition)
    {
        return Base.getService().getParameter(partition,
                                              MAReceivingTypeParameter);
    }


    public void createMilestoneTrackers ()
    {
        DirectMilestoneReceivableUtil.createMilestoneTrackers(this);
    }


    public List getMilestones ()
    {
        return DirectMilestoneReceivableUtil.getMilestones(this);
    }


    public List getMilestoneTrackers ()
    {
        return DirectMilestoneReceivableUtil.getMilestoneTrackers(this);
    }


    public void submitMilestoneTrackers ()
    {
        DirectMilestoneReceivableUtil.submitMilestoneTrackers(this);
    }


    public void updateReceivedMilestone ()
    {
        DirectMilestoneReceivableUtil.updateReceivedMilestone(this);
    }


    public boolean isHeaderFullyAccepted ()
    {
        Money maxAmount = getMaxAmount();
        if (maxAmount == null) {
            return false;
        }
        else {
            Money totalAccepted = getAmountAccepted();
            if (totalAccepted == null) {
                return false;
            }
            if (Log.receiving.isDebugEnabled()) {
                Log.receiving.debug(
                    "comparing total amountAccepted %s to amount limit %s",
                    totalAccepted.asString(), maxAmount.asString());
            }
            if (Money.subtract(
                maxAmount, totalAccepted).getAmount().signum() > 0) {
                return false;
            }
        }

        return true;
    }


    public DateRange getValidPeriod ()
    {
        DateRange range = null;
        Date from = null;
        Date to = null;

        from = getEffectiveDate();
        to =  getActualExpirationDate();


        Date closed = getMAClosedDate();
        if ((to == null) ||
            (closed != null && closed.before(to))) {
            to = closed;
        }


        if (from != null || to != null) {
            range = new DateRange(getPartition(), from, to);
        }


        return range;
    }


    public boolean isPLIPricedFromMasterAgreement (Contract ma,
                                                   ProcureLineItem pli)
    {

        if (!(pli instanceof ContractLineItemSource)) {
            return false;
        }

        ContractLineItemSource maliSource = (ContractLineItemSource)pli;

        if (maliSource.getMasterAgreement() != null &&
            maliSource.getMasterAgreement().equals(ma)) {
            return true;
        }


        if (!ListUtil.nullOrEmptyList(pli.getHierarchicalTerms())) {
            Iterator termsIterator = pli.getHierarchicalTermsIterator();
            while (termsIterator.hasNext()) {
                HierarchicalTerm ht = (HierarchicalTerm)termsIterator.next();
                ContractLineItem parentMALI =
                    ContractLineItem.getMALineItem(ht.getLineItemReference());
                if (parentMALI != null &&
                    parentMALI.getMasterAgreement().equals(ma)) {
                    return true;
                }
            }
        }


        return false;
    }


    protected ContractLineItem getMALineItemForMAOnPLI (ProcureLineItem pli)
    {

        if (pli instanceof ContractLineItem) {
            return null;
        }

        Assert.that(pli instanceof ContractLineItemSource,
                    "%s does not implemenet ContractLineItemSource", pli);

        ContractLineItemSource maliSource = (ContractLineItemSource)pli;
        ContractLineItem mali = maliSource.getMALineItem();

        if (mali.getMasterAgreement().equals(this)) {
            return mali;
        }

        Iterator terms = pli.getHierarchicalTermsIterator();
        while (terms.hasNext()) {
            HierarchicalTerm ht = (HierarchicalTerm)terms.next();
            LineItemReference lir = ht.getLineItemReference();
            if (lir == null) {
                continue;
            }
            Contract ma = (Contract)lir.getLineItemCollection();

            if (this.equals(ma)) {
                return (ContractLineItem)lir.getLineItem();
            }
        }


        return null;
    }


    protected boolean isPLIPricedDirectlyByMA (ProcureLineItem pli)
    {

        if (pli instanceof ContractLineItemSource) {
            ContractLineItemSource maliSource = (ContractLineItemSource)pli;
            ContractLineItem mali = maliSource.getMALineItem();

            if (mali.getMasterAgreement().equals(this)) {
                return true;
            }
        }
        return false;
    }

    public static String getNonTermsSubAccumulatorFieldName (
        String fieldName)
    {
        return Fmt.S(
            "%s.%s",
            Contract.KeyNonTermsSubAccumulators,
            fieldName);
    }


    public void updateHeaderParentAccumulators (String fieldName,
                                                Money amount)
    {
        Log.contract.debug("%s processing updateHeader %s for parent %s",
                                  this,
                                  fieldName,
                                  this.getParentAgreement());

        Contract parent = getParentAgreement();

        if (parent == null) {
            Log.contract.debug(
                "Null Parent Agreement, %s",
                "UpdateHeaderParentAccumulators not doing anything");
            return;
        }

        if (parent.getIncludeSubAgreementAccumulators() &&
            !parent.isClosed()) {
            Log.contract.debug("Adding Amount to Parent %s %s",
                                      parent.getUniqueName(),
                                      amount);
            parent.addAmount(
                Contract.getNonTermsSubAccumulatorFieldName(
                    fieldName), amount);
        }


        parent.updateHeaderParentAccumulators(fieldName,
                                              amount);
    }


    public void updateParentAccumulators (ContractLineItem mali,
                                          ProcureLineItem pli,
                                          String fieldName,
                                          Money amount,
                                          BigDecimal quantity)
    {

        if (mali.getRollUpAccumulatorsToParent()) {
            if (amount != null) {
                updateParentAmountField(fieldName,
                                        pli,
                                        mali,
                                        getParentAgreement(),
                                        amount);
            }
            else if (quantity != null) {
                updateParentQuantityField(fieldName,
                                          pli,
                                          mali,
                                          getParentAgreement(),
                                          quantity);
            }
        }
    }


    protected ContractLineItem getParentMALI (Contract parent,
                                        ProcureLineItem pli)
    {

        ContractLineItem parentMALI = parent.getMALineItemForMAOnPLI(pli);
        if (parentMALI == null) {

            ContractLineItem mali = null;
            if (pli instanceof ContractLineItem) {
                mali = (ContractLineItem)pli;
            }
            else if (pli instanceof ContractLineItemSource) {
                ContractLineItemSource maliSource = (ContractLineItemSource)pli;
                mali = maliSource.getMALineItem();
            }

            if (mali != null) {
                return mali.getNextParentTerm(pli, parent);
            }
        }

        return parentMALI;
    }


    protected void updateParentAmountField (
        String fieldName,
        ProcureLineItem pli,
        ContractLineItem cli,
        Contract parent,
        Money amount)
    {

        if (parent == null) {
            Log.contract.debug(
                "Null Parent Agreement, %s",
                "UpdateAmountAccumulatorField not doing anything");
            return;
        }

        ContractLineItem parentMALI = cli;


        if (parent.getIncludeSubAgreementAccumulators() &&
            !parent.isClosed()) {

            boolean pricedByParent = false;


            parentMALI = getParentMALI(parent, pli);


            if (parentMALI != null &&
                parentMALI.getMasterAgreement().equals(parent) &&
                cli.canCompoundWithParentTerm(parentMALI)) {
                pricedByParent = true;
            }


            if (pricedByParent) {
                parentMALI.addAmountWithoutUpdateParentTrigger(
                    ContractPricingUtil.getTermsSubAccumulatorFieldName(fieldName),
                    amount);
            }

            else {
                parent.addAmount(
                    Contract.getNonTermsSubAccumulatorFieldName(
                        fieldName),
                    amount);
            }
        }


        updateParentAmountField(fieldName,
                                pli,
                                parentMALI,
                                parent.getParentAgreement(),
                                amount);

    }


    protected void updateParentQuantityField (
        String fieldName,
        ProcureLineItem pli,
        ContractLineItem cli,
        Contract parent,
        BigDecimal quantity)
    {

        if (parent == null) {
            Log.contract.debug(
                "Null Parent Agreement, %s",
                "UpdateParentAccumulatorField not doing anything");
            return;
        }

        ContractLineItem parentMALI = cli;


        if (parent.getIncludeSubAgreementAccumulators() &&
            !parent.isClosed()) {

            parentMALI = getParentMALI(parent, pli);
            if (parentMALI != null &&
                parentMALI.getMasterAgreement().equals(parent) &&
                cli.canCompoundWithParentTerm(parentMALI)) {
                parentMALI.addQuantityWithoutUpdateParentTrigger(
                    ContractPricingUtil.getTermsSubAccumulatorFieldName(
                        fieldName),
                    quantity);
            }
        }


        updateParentQuantityField(fieldName,
                                  pli,
                                  parentMALI,
                                  parent.getParentAgreement(),
                                  quantity);
    }


    private static String Invoiceable = "Invoiceable";
    private static String Receivable = "Receivable";
    private static String Releasable = "Releasable";


    private boolean parentHierarchyHasMAsOfReleaseType (String type)
    {
        Contract parent = getParentAgreement();


        if (parent == null) {
            return false;
        }

        if (parent.IsOfReleaseType(type)) {
            return true;
        }


        return parent.parentHierarchyHasMAsOfReleaseType(type);
    }

    private boolean IsOfReleaseType (String type)
    {
        if (StringUtil.nullOrEmptyString(type)) {
            return false;
        }

        if (type.equals(Invoiceable)) {
            if (getIsInvoiceable()) {
                return true;
            }
        }

        else if (type.equals(Receivable)) {
            if (getIsReceivable()) {
                return true;
            }
        }

        else if (type.equals(Releasable)) {
            if (getReleaseType() == ReleaseTypeUser) {
                return true;
            }
        }

        return false;
    }

    private boolean parentHierarchyHasInvoiceableMAs ()
    {
        return parentHierarchyHasMAsOfReleaseType(Invoiceable);
    }

    private boolean parentHierarchyHasReleasableMAs ()
    {
        return parentHierarchyHasMAsOfReleaseType(Releasable);
    }

    private boolean parentHierarchyHasReceivableMAs ()
    {
        return parentHierarchyHasMAsOfReleaseType(Receivable);
    }


    private boolean subHierarchyHasMAsOfReleaseType (String type)
    {
        List subAgreements = getSubAgreements();


        if (ListUtil.nullOrEmptyList(subAgreements)) {
            return false;
        }

        Iterator subAgreementsIterator = getSubAgreementsIterator();
        while (subAgreementsIterator.hasNext()) {
            BaseId id = (BaseId)subAgreementsIterator.next();
            Contract subAgreement =
               (Contract)id.get();
            if (subAgreement.IsOfReleaseType(type)) {
                return true;
            }

            if (subAgreement.subHierarchyHasMAsOfReleaseType(type)) {
                return true;
            }
        }


        return false;
    }

    private boolean subHierarchyHasInvoiceableMAs ()
    {
        return subHierarchyHasMAsOfReleaseType(Invoiceable);
    }

    private boolean subHierarchyHasReleasableMAs ()
    {
        return subHierarchyHasMAsOfReleaseType(Releasable);
    }

    private boolean subHierarchyHasReceivableMAs ()
    {
        return subHierarchyHasMAsOfReleaseType(Receivable);
    }


    public boolean hasOpenSubAgreements ()
    {
        for (Iterator it = getSubAgreementsIterator();it.hasNext();) {
            Contract sub =
                (Contract)((BaseId)it.next()).get();
            if (sub.getActive() && sub.isOpen()) {
                return true;
            }
        }

        return false;
    }

    public Money addPreloadAmount (Money amount)
    {
        if (amount == null) {
            amount = new Money(Constants.ZeroBigDecimal, getCurrency());
        }

        Money preload = getPreloadAmount();
        if (preload != null) {
            amount = amount.add(preload);
        }

        Money subPreload = getSubPreloadAmount();
        if (subPreload != null) {
            amount = amount.add(subPreload);
        }
        return amount;
    }

    public void rollUpPreloadAmount (Money rollUpAmount)
    {

        if (rollUpAmount == null) {
            return;
        }

        Contract parent = getParentAgreement();
        if (parent == null) {
            return;
        }


        if (parent.getIncludeSubAgreementAccumulators() &&
            !parent.isClosed()) {
            Money subPreloadAmount = parent.getSubPreloadAmount();
            if (subPreloadAmount == null) {
                subPreloadAmount = new Money(Constants.ZeroBigDecimal, getCurrency());
            }
            parent.setSubPreloadAmount(subPreloadAmount.add(rollUpAmount));
        }

        parent.rollUpPreloadAmount(rollUpAmount);
    }


    public void addHeaderAmount (String fieldName,
                                 Money amount)
    {
        addHeaderAmountWithoutUpdateParentTrigger(fieldName,amount);
        Map params = MapUtil.map();
        params.put(ContractCoreApprovable.AmountParam, amount);
        params.put(ContractCoreApprovable.FieldNameParam, fieldName);
        fireTriggers("UpdateParentAccumulators", params);
    }

    private void addHeaderAmountWithoutUpdateParentTrigger (String fieldName,
                                                           Money amount)
    {
        Log.contract.debug("Calling Add Amount for %s.%s = %s",
                           getUniqueName(), fieldName, amount);
        addAmount(fieldName, amount);
        if (hasNextVersion()) {
            updateAmountInLaterVersions(fieldName, amount);
        }
    }

    private void updateAmountInLaterVersions (String fieldName, Money amount)
    {

        Contract nextMA = (Contract)getNextVersion();
        if (nextMA == null) {
            return;
        }

        nextMA.addHeaderAmountWithoutUpdateParentTrigger(fieldName, amount);
    }


    public boolean getIsAuthoringContract ()
    {
        ContractRequest r = getMasterAgreementRequest();
        return (r != null) ? r.getIsAuthoringContract() : false;
    }


    public boolean allowAddedToProjects ()
    {
        ContractRequest r = getMasterAgreementRequest();
        return (r != null) ? r.allowAddedToProjects() : true;
    }

    public static String getERPSendMethodForPartition (Partition partition)
    {
        Log.contract.debug("Getting SendMethod for %s", partition.getName());
        return null;

    }


    public static String getContractSendMethodForPartition (Partition partition)
    {
        Log.contract.debug("Getting SendMethod for %s", partition.getName());
        return Base.getService().getParameter(partition, ParameterContractSendMethod);
    }


    public boolean isContractPushEnabledForPartition (Partition partition)
    {
        String sendmethod = getERPSendMethodForPartition(partition);
        return !(StringUtil.nullOrEmptyString(sendmethod));
    }


    public void processContractForSending ()
    {
        if (hasPreviousVersion()) {

            setSplitAccountingChangedState();
        }
    }


    public void createContractRecipients ()
    {
        processContractForSending();

        if (!getGlobalReleaseFlag()) {

            createBPORecipient(getPartition());
            return;
        }

        Partition[] partitions =
            Base.getService().getPartitionRuntime().getAllDomainPartitions();

        for (int i=0; i<partitions.length; i++) {

            Partition partition = partitions[i];

            createBPORecipient(partition);
        }
    }



    public void createBPORecipient (Partition partition)
    {

        if (!getIsBlanketPurchaseOrder()) {
            return;
        }
        SupplierLocation sl = getSupplierLocation();

        String sendMethod = null;
        if (sl != null) {
            sendMethod = sl.getPreferredOrderingMethod();
            if (StringUtil.nullOrEmptyString(sendMethod)) {
                Log.contract.debug(
                    "Contract send to AribaNetwork not enabled for Parition %s:%s",
                    getUniqueName(),
                    partition);
                return;
            }
        }

        Log.contract.debug("Creating New Contract Recipient with %s",
                           sendMethod);


        int recipientType =
            hasPreviousVersion() ?
            ContractRecipient.RecipientTypeChange :
            ContractRecipient.RecipientTypeCreate;

        ContractRecipient crecp = null;

        if (sendBPOToAN()) {
            crecp =
                new ContractRecipient(getPartition(),
                                      this,
                                      SupplierLocation.URL,
                                      recipientType);
        }
        else {
            crecp =
                new ContractRecipient(getPartition(),
                                      this,
                                      SupplierLocation.Manual,
                                      recipientType);
        }

        crecp.setRoutingPartition(partition.getName());
        crecp.save();
        getRecipients().addElementIfAbsent(crecp);
    }


    private boolean sendBPOToAN ()
    {
        SupplierLocation sl = getSupplierLocation();

        if (sl == null) {
            return false;
        }


        String bpoSendMethod = sl.getPreferredOrderingMethod();
        if (this.isSupplierANEnabled() &&
            bpoSendMethod.equals(SupplierLocation.URL)) {
            if (peviousVersionNotANBPO()) {
                return false;
            }
            return true;
        }


        boolean useASN = Base.getService().getBooleanParameter(
            getPartition(), this.ASNBPOForPrivateSupplier);
        if (useASN &&

            (bpoSendMethod.equals(SupplierLocation.AribaFax) ||
                bpoSendMethod.equals(SupplierLocation.AribaEmail))) {
            if (peviousVersionNotANBPO()) {
                return false;
            }
            return true;
        }

        return false;
    }


    private boolean peviousVersionNotANBPO ()
    {
        if (this.hasPreviousVersion()) {
            Contract prevContract = (Contract)this.getPreviousVersion();
            if (!prevContract.isAribaNetworkBPO()) {
                return true;
            }
        }


        return false;
    }


    public boolean isAribaNetworkBPO ()
    {
        BaseSession session = Base.getSession();
        List v = getRecipients();
        for (int i=0; i < v.size(); i++) {
            BaseId baseId = (BaseId)v.get(i);
            ContractRecipient recipient =
                (ContractRecipient)session.objectFromId(baseId);
            if (hasNextVersion()) {
                if (recipient.getIsACSNSender() &&
                    (recipient.getRecipientType() ==
                         ContractRecipient.RecipientTypeChange ||
                     recipient.getRecipientType() ==
                         ContractRecipient.RecipientTypeCreate)) {
                    return true;
                }
            }
            else {
                if (recipient.getActive() && recipient.getIsACSNSender() &&
                    (recipient.getRecipientType() ==
                         ContractRecipient.RecipientTypeChange ||
                     recipient.getRecipientType() ==
                         ContractRecipient.RecipientTypeCreate)) {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isSupplierANEnabled ()
    {
        SupplierLocation sl = getSupplierLocation();
        if (sl == null) {
            return false;
        }

        String orderMethod = sl.getPreferredOrderingMethod();
        boolean supplierANEnabled = false;
        try {
            supplierANEnabled =
                ProcureUtil.getSupplierCredential(sl) == null ? false : true;
        }
        catch (ServiceException ex)
        {
            Log.contract.warning(9392, ex);
        }

        return supplierANEnabled;
    }


    public static boolean getIsERPPushEnabledInAnyPartition ()
    {
        Partition[] partitions =
            Base.getService().getPartitionRuntime().getAllDomainPartitions();

        for (int i=0; i<partitions.length; i++) {

            Partition partition = partitions[i];
            String sendMethod = getERPSendMethodForPartition(partition);
            if (!StringUtil.nullOrEmptyString(sendMethod)) {
                Log.contract.debug("Contract Push enabled for Parition %s",
                                   partition);
                return true;
            }
        }

        return false;
    }


    public void createERPRecipient (Partition partition)
    {
        String sendMethod = getERPSendMethodForPartition(partition);
        if (StringUtil.nullOrEmptyString(sendMethod)) {
            Log.contract.debug("Contract Push not enabled for Parition %s:%s",
                               getUniqueName(),
                               partition);
            return;
        }


        String pushEvent = ContractRecipient.getERPPushEventName(sendMethod);
        boolean isERPSender = !StringUtil.nullOrEmptyString(pushEvent);

        if (isERPSender && getTermType() != TermTypeItem) {
            Log.contract.debug("Contract %s is not Item Term. %s",
                               getUniqueName(),
                               "Cannot create Contract Recipients");
            return;
        }


        if (isERPSender && getReleaseType() == ReleaseTypeNone) {
            Log.contract.debug("Contract %s is not Release. %s",
                               getUniqueName(),
                               "Cannot create Contract Recipients");
            return;
        }

        Log.contract.debug("Creating New Contract Recipient with %s",
                           sendMethod);


        int recipientType =
            hasPreviousVersion() ?
            ContractRecipient.RecipientTypeChange :
            ContractRecipient.RecipientTypeCreate;


        ContractRecipient crecp =
            new ContractRecipient(getPartition(),
                                  this,
                                  sendMethod,
                                  recipientType);

        crecp.setRoutingPartition(partition.getName());
        crecp.save();
        getRecipients().addElementIfAbsent(crecp);
    }

    public boolean getIsContractSentToPartition (Partition partition)
    {
        Iterator numbers = getERPContractNumbersIterator();
        while (numbers.hasNext()) {
            NamedPair erpNumber = (NamedPair)numbers.next();
            if (partition.getName().equals(erpNumber.getName()) &&
                !StringUtil.nullOrEmptyString(erpNumber.getValue())) {
                return true;
            }
        }
        return false;
    }

    public boolean getAllRecipientsAreSent ()
    {
        Iterator recipients = getRecipientsIterator();
        while (recipients.hasNext()) {
            BaseId id = (BaseId)recipients.next();
            ContractRecipient recipient = (ContractRecipient)id.get();
            if (!recipient.isApprovableSent()) {
                return false;
            }
        }

        return true;
    }

    public String getCurrentPartitionERPNumber ()
    {
        Iterator iterator = getERPContractNumbersIterator();
        while (iterator.hasNext()) {
            NamedPair number = (NamedPair)iterator.next();
            if (number.getName().equals(Base.getSession().getPartition().getName())) {
                return number.getValue();
            }
        }

        return null;
    }

    public NamedPair getERPContractNumber ()
    {
        NamedPair erpNumber = super.getERPContractNumber();
        if (erpNumber != null) {
            return erpNumber;
        }

        Iterator iterator = getERPContractNumbersIterator();
        while (iterator.hasNext()) {
            NamedPair number = (NamedPair)iterator.next();
            if (number.getName().equals(Base.getSession().getPartition().getName())) {
                return number;
            }
        }

        return null;
    }

    public ContractRecipient getERPRecipientForPartition (Partition partition)
    {
        Iterator recipients = getRecipientsIterator();
        while (recipients.hasNext()) {
            BaseId recipientId = (BaseId)recipients.next();
            ContractRecipient cr = (ContractRecipient)recipientId.get();
            if (cr.isERPRecipientForPartition(partition.getName())) {
                return cr;
            }
        }

        return null;
    }


    public String getURLForExcelFile ()
    {
        ContractRequest cr = getMasterAgreementRequest();
        return cr.getURLForExcelFile();
    }



    public String getExportedExcel ()
    {
        ContractRequest cr = getMasterAgreementRequest();
        return cr.getExportedExcel();
    }

    public String getExportedExcelForLocale (String locale)
    {
        ContractRequest cr = getMasterAgreementRequest();
        return cr.getExportedExcelForLocale(locale);
    }


    public void preExcelExport ()
    {
    }

    public void preApplyChanges ()
    {
        super.preApplyChanges();

        try {
            if (AccrualUtil.shouldAccrue(this)) {
                AccrualUtil.accrue(this);
            }
        }
        catch (RuntimeException e) {

            Log.contract.error(9603, this.getUniqueName(), e.getMessage());
            throw e;
        }
    }

    public static boolean isInContentOnlyMode ()
    {
        BaseSession session = Base.getSession();
        if (session != null) {
            Permission p =
                Permission.getPermission("ContentContractManager", session.getRealm());
            if (p != null && p.getActive()) {
                return true;
            }
        }
        return false;
    }

    private static AQLOptions getOptions ()
    {
        AQLOptions o = new AQLOptions();
        o.setSimpleGenerateHook(new ContractSQLGenerateHook());
        return o;
    }



    public boolean isManualSupplierContract ()
    {
        Iterator recipients = getRecipients().iterator();
        ContractRecipient r = null;
        while (recipients.hasNext()) {
            r = (ContractRecipient)((BaseId)recipients.next()).get();
            String sendMethod = r.getSendMethod();
            if (sendMethod != null &&
                sendMethod.equals(SupplierLocation.Manual)) {
                return true;
            }
        }
        return false;
    }


    public boolean hasInfoMessage ()
    {
        if ( this.getIsBlanketPurchaseOrder() ) {
            if (this.isManualSupplierContract()) {
                return true;
            }
        }
        return false;
    }


    public String infoMessage ()
    {
        return ResourceService.getString(StringTable,
                                         "ManualSupplierContractInfoMessage",
                                         ResourceService.getService().getLocale());
    }
}
