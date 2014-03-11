// Decompiled by Jad v1.5.8e. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(5) braces fieldsfirst noctor nonlb space lnc
// Source File Name:   ProcureLineItemCollection.java

package ariba.procure.core;

import ariba.approvable.core.*;
import ariba.approvable.core.mail.SupplierNotificationMessage;
import ariba.base.core.*;
import ariba.base.core.aql.AQLError;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Fields;
import ariba.base.fields.FieldsService;
import ariba.base.fields.ValueSourceUtil;
import ariba.base.server.EventQueueException;
import ariba.basic.core.Country;
import ariba.basic.core.Money;
import ariba.common.core.*;
import ariba.cxml.SupplierSyncUtil;
import ariba.cxml.core.CXMLUtil;
import ariba.cxml.service.DocumentService;
import ariba.server.workflowserver.WorkflowService;
import ariba.user.core.*;
import ariba.user.core.User;
import ariba.user.core.Approver;
import ariba.user.util.mail.Notification;
import ariba.util.core.*;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.formatter.DateFormatter;
import ariba.util.io.TempFile;
import ariba.util.log.Logger;
import ariba.util.messaging.CallCenter;
import ariba.util.messaging.MessagingException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import ariba.channel.file.EventContext;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.FileUtil;
import ariba.app.server.FileUploadHelper;
import ariba.integration.core.AMFConstants;

// Referenced classes of package ariba.procure.core:
//            ProcureLineItemCollectionBase, ServiceManager, ProcureLineItem, SourcingRequestCoreApprovable,
//            ContractSource, SimpleProcureRecord, CategoryTemplateDetailsSource, ExcelImportErrorHandler,
//            ExcelImportLog, ServiceProvider, Log, ProcureUtil,
//            SharedGlobalItemProperties, ProductDescription, CategoryUtil, CategoryTemplateDetails,
//            CategoryTemplateProperties, QualifiedMoney, Milestone, MilestoneWorksheet

public abstract class ProcureLineItemCollection extends ProcureLineItemCollectionBase
    implements ProjectsUpdatable, ServiceProvider {

            public static final String ChargesTab = "chg";
            public static final int ChargesTabWidth = 74;
            public static final String ChargesTabTip = "ChargesTabTip";
            public static final String PermissionNoNonCatalogItems = "NoNonCatalogItems";
            public static final String PermissionSourcingAuthorized = "SourcingAuthorized";
            public static final String PermissionCreateSourcingRequest = "CreateSourcingRequest";
            public static final int Unsourced = 1;
            public static final int Sourcing = 2;
            public static final int Sourced = 4;
            public static final int SourcingCanceling = 8;
            public static final int SourcingCanceled = 16;
            public static final int SourcingFailed = 32;
            public static final int ConfirmingSourcing = 64;
            public static final int SourcingCompleted = 128;
            protected static final String SourcingStateNames[] = {
/* 215*/        "Unsourced", "Sourcing", "Sourced", "SourcingCanceling", "SourcingCanceled", "SourcingFailed", "SourcingConfirming", "SourcingCompleted"
            };
            private static final String ParentKit = "ParentKit";
            private static final String KitInstanceId = "KitInstanceId";
            public static int Added = 1;
            public static int Updated = 2;
            public static int Deleted = 4;
            private boolean triggerFlag;
            private boolean saving;
            private static final String idPrefix = "_pAyLoAd_";
            private static Random randomGenerator = new Random(System.currentTimeMillis());
            private static final String SupplierManagerPermission = "SupplierManager";
            private static final String StringTable = "ariba.procure.core";
            private static final String SupplierUpdatedNotificationSubject = "SupplierUpdatedNotificationSubject";
            private static final String SupplierUpdatedNotificationBody = "SupplierUpdatedNotificationBody";
            private static final String CommonSupplierUpdatedNotificationSubject = "CommonSupplierUpdatedNotificationSubject";
            private static final String CommonSupplierUpdatedNotificationBody = "CommonSupplierUpdatedNotificationBody";
            private static final String SupplierOverdueNotificationSubject = "SupplierOverdueNotificationSubject";
            private static final String SupplierOverdueNotificationBody = "SupplierOverdueNotificationBody";
            private static final String UnknownSupplierIDNotificationSubjectKey = "UnknownSupplierIDNotificationSubject";
            private static final String UnknownSupplierIDNotificationBody1Key = "UnknownSupplierIDNotificationBody1";
            private static final String UnknownSupplierIDNotificationBody2Key = "UnknownSupplierIDNotificationBody2";
            private static final String UnknownSupplierIDNotificationBody3Key = "UnknownSupplierIDNotificationBody3";
            private static final String UnknownSupplierIDNotificationBody4Key = "UnknownSupplierIDNotificationBody4";
            private static final String UnknownSupplierUserNotificationSubjectKey = "UnknownSupplierUserNotificationSubject";
            private static final String UnknownSupplierUserNotificationBodyKey = "UnknownSupplierUserNotificationBody";
            private static final String ResubmitRequestURLKey = "ResubmitRequestURL";
            private static final String ViewRequestURLKey = "ViewRequestURL";
            public static final String FilenameKey = "Filename";
            public static final String PreparerKey = "Preparer";
            public static final String HeaderFileNameKey = "HeaderFileName";
            public static final String ExcelImportId = "XLSID";
            public static final String EventSourceKey = "EventSource";
            public static final String ErrorLogKey = "ErrorLog";
            public static final String RelaxDefaultOnRequesterChange = "Application.Procure.RelaxDefaultOnRequesterChange";
            private User oldRequester;
            private User newRequester;

            public ProcureLineItemCollection() {
/* 266*/        triggerFlag = false;
/* 276*/        saving = false;
/*2531*/        oldRequester = null;
/*2532*/        newRequester = null;
            }

            public void init(Partition partition, String title, User preparer, User requester) {
/* 358*/        super.init(partition, title, preparer, requester);
/* 359*/        setSourcingState(1);
/* 360*/        if (getClassName().equals("ariba.procure.core.ProcureLineItemCollection")) {
/* 361*/            fireTriggers("Create", null);
                }
/* 364*/        setServices(new ServiceManager(partition));
            }

            public void reconstitute() {
/* 369*/        super.reconstitute();
/* 370*/        refreshLineItemReferences();
            }

            protected void recomputeTotalCost() {
/* 375*/        Money total = null;
/* 377*/        if (ListUtil.nullOrEmptyList(getLineItems())) {
/* 378*/            total = new Money(0.0D, getReportingCurrency());
                } else {
/* 381*/            total = LineItem.sumLineItems(getLineItemsIterator(), getReportingCurrency());
                }
/* 386*/        Money oldTotal = getTotalCost();
/* 387*/        if (total == oldTotal || total.equals(oldTotal)) {
/* 388*/            return;
                } else {
/* 390*/            super.setTotalCost(total);
/* 391*/            return;
                }
            }

            public ProcureLineItem getDefaultProcureLineItem() {
/* 398*/        return (ProcureLineItem)getDefaultLineItem();
            }

            protected int getSubmitAccess(Approver approver) {
/* 413*/        if (approver != null && (approver instanceof User) && Base.getSession().getRealUser() != null && !approver.equals(Base.getSession().getRealUser())) {
/* 417*/            return Access.CantSubmit;
                }
/* 420*/        int access = super.getSubmitAccess(approver);
/* 421*/        if (access == Access.Now) {
/* 422*/            return access;
                }
/* 425*/        if (!getLineItemsIterator().hasNext()) {
/* 426*/            return Access.CantSubmitNoLineItems;
                } else {
/* 429*/            return access;
                }
            }

            protected int getComposeAccess(Approver approver) {
/* 434*/        if (approver != null && (approver instanceof User) && !approver.equals(Base.getSession().getRealUser())) {
/* 436*/            return Access.CantEditWhenDelegated;
                } else {
/* 439*/            return super.getComposeAccess(approver);
                }
            }

            protected int getCopyAccess(User user) {
/* 444*/        if (user != null && !user.equals(Base.getSession().getRealUser())) {
/* 446*/            return Access.CantCopyWhenDelegated;
                } else {
/* 449*/            return super.getCopyAccess(user);
                }
            }

            protected int getWithdrawAccess(Approver approver) {
/* 460*/        int access = super.getWithdrawAccess(approver);
/* 461*/        if (access != Access.Now) {
/* 462*/            return access;
                }
/* 465*/        if (hasNextVersion()) {
/* 466*/            return Access.CantWithdrawOldVersion;
                } else {
/* 469*/            return Access.Now;
                }
            }

            protected int getDeleteAccess(User user, Object args[]) {
/* 482*/        if (user != null && !user.equals(Base.getSession().getRealUser())) {
/* 484*/            return Access.CantDeleteWhenDelegated;
                }
/* 487*/        int access = super.getDeleteAccess(user, args);
/* 488*/        if (allowDeletionOfNonComposingApprovables() || access != Access.Now) {
/* 490*/            return access;
                }
/* 493*/        if (!isCreationInProgress() && hasFinalLineItems()) {
/* 494*/            return Access.CantDeleteRequestsWithOrderedItems;
                } else {
/* 497*/            return Access.Now;
                }
            }

            protected int getApproveAccess(Approver approver) {
/* 508*/        if (super.getApproveAccess(approver) != Access.Now) {
/* 509*/            return super.getApproveAccess(approver);
                }
/* 515*/        if (getHasItemsFromUnApprovedSupplier() && (approver instanceof User)) {
/* 517*/            User approverUser = (User)approver;
/* 518*/            if (approverUser.hasRole(Role.getRole("Professional Buyer"))) {
/* 519*/                return Access.CantApprovePendingSupplierApproval;
                    }
                }
/* 524*/        if (getSourcingState() == 2) {
/* 525*/            return Access.CantApproveOrDenySourcing;
                } else {
/* 528*/            return Access.Now;
                }
            }

            public boolean getHasItemsFromUnApprovedSupplier() {
/* 533*/        for (Iterator lineItems = getLineItemsIterator(); lineItems.hasNext();) {
/* 535*/            ProcureLineItem pli = (ProcureLineItem)lineItems.next();
/* 537*/            if (pli.getSupplier() != null && !pli.getSupplier().getIsSupplierApproved()) {
/* 539*/                return true;
                    }
                }

/* 543*/        return false;
            }

            protected int getDenyAccess(Approver approver) {
/* 554*/        if (super.getDenyAccess(approver) != Access.Now) {
/* 555*/            return super.getDenyAccess(approver);
                }
/* 560*/        if (getHasItemsFromUnApprovedSupplier() && (approver instanceof User)) {
/* 562*/            User approverUser = (User)approver;
/* 563*/            if (approverUser.hasRole(Role.getRole("Professional Buyer"))) {
/* 564*/                return Access.CantApprovePendingSupplierApproval;
                    }
                }
/* 569*/        if (getSourcingState() == 2) {
/* 570*/            return Access.CantApproveOrDenySourcing;
                } else {
/* 573*/            return Access.Now;
                }
            }

            public boolean allowInactivationWhenDeleting() {
/* 584*/        return super.allowInactivationWhenDeleting() && !hasFinalLineItems();
            }

            protected boolean hasFinalLineItems() {
/* 599*/        BaseVector items = getLineItems();
/* 600*/        for (int i = 0; i < ListUtil.getListSize(items); i++) {
/* 601*/            ProcureLineItem item = (ProcureLineItem)items.get(i);
/* 602*/            if (item.getIsFinalState()) {
/* 603*/                return true;
                    }
                }

/* 607*/        return false;
            }

            protected int getChangeAccess(User user) {
/* 620*/        if (hasNextVersion()) {
/* 621*/            return Access.CantChangeOldVersion;
                }
/* 625*/        if (getCheckedOut()) {
/* 626*/            return Access.CantCheckoutAlreadyCheckedout;
                }
/* 630*/        if (isCreationInProgress()) {
/* 631*/            return Access.CantCheckoutCreationInProgress;
                }
/* 635*/        if (getApprovedState() != 4) {
/* 636*/            return Access.CantChange;
                } else {
/* 639*/            return Access.Now;
                }
            }

            protected int getSubmitChangeAccess(User user) {
/* 652*/        if (isChanging() && isUserChanging(user)) {
/* 653*/            return Access.Now;
                } else {
/* 656*/            return Access.CantSubmitNotChanged;
                }
            }

            protected int getUndoChangeAccess(User user) {
/* 669*/        if ((isChanging() || isInvalid()) && isUserChanging(user)) {
/* 670*/            return Access.Now;
                } else {
/* 673*/            return Access.CantSubmitNotChanged;
                }
            }

       /* @aribaapi private */
    public void refreshLineItemReferences ()
    {
        Iterator lines = getAllLineItems();
        while (lines.hasNext()) {
            ProcureLineItem pli = (ProcureLineItem)lines.next();
            pli.refreshLineItemReferences();
                }

            }

            public void setSourcingState(int state) {
/* 715*/        Log.sourcing.debug("Setting Sourcing state for %s:%s", this, Constants.getInteger(state));
/* 717*/        super.setSourcingState(state);
/* 718*/        updateStatusString();
            }

            public static List getSourcingStatusStrings() {
/* 729*/        List statusStrings = ListUtil.list();
/* 730*/        for (int i = 0; i < SourcingStateNames.length; i++) {
/* 731*/            ListUtil.addElementIfAbsent(statusStrings, SourcingStateNames[i]);
                }

/* 734*/        return statusStrings;
            }

            public List getSourcingRequests() {
/* 739*/        List sourcingRequests = ListUtil.list();
/* 740*/        Iterator lineItems = getLineItemsIterator();
/* 741*/        do {
/* 741*/            if (!lineItems.hasNext()) {
/* 742*/                break;
                    }
/* 742*/            ProcureLineItem pli = (ProcureLineItem)lineItems.next();
/* 743*/            SourcingRequestCoreApprovable sreq = pli.getSourcingRequest();
/* 744*/            if (sreq != null) {
/* 745*/                ListUtil.addElementIfAbsent(sourcingRequests, sreq);
                    }
                } while (true);
/* 749*/        return sourcingRequests;
            }

            public List getSourcedItemsFromSRCA(SourcingRequestCoreApprovable srca) {
/* 754*/        List sourcedItems = ListUtil.list();
/* 755*/        Iterator lineItems = getLineItemsIterator();
/* 756*/        do {
/* 756*/            if (!lineItems.hasNext()) {
/* 757*/                break;
                    }
/* 757*/            ProcureLineItem pli = (ProcureLineItem)lineItems.next();
/* 758*/            SourcingRequestCoreApprovable sreq = pli.getSourcingRequest();
/* 759*/            if (sreq != null && sreq.equals(srca)) {
/* 760*/                ListUtil.addElementIfAbsent(sourcedItems, pli);
                    }
                } while (true);
/* 764*/        return sourcedItems;
            }

            public boolean isFullySourced() {
/* 769*/        boolean fullySourced = true;
/* 770*/        for (Iterator SourcingRequests = getSourcingRequests().iterator(); SourcingRequests.hasNext();) {
/* 773*/            SourcingRequestCoreApprovable sreq = (SourcingRequestCoreApprovable)SourcingRequests.next();
/* 775*/            if (sreq.getSourcingState() != 4) {
/* 776*/                return false;
                    }
                }

/* 779*/        return fullySourced;
            }

            public boolean isSourcingFailed() {
/* 784*/        for (Iterator sourcingRequests = getSourcingRequests().iterator(); sourcingRequests.hasNext();) {
/* 787*/            SourcingRequestCoreApprovable sreq = (SourcingRequestCoreApprovable)sourcingRequests.next();
/* 791*/            if (sreq.getSourcingState() != 32) {
/* 792*/                return false;
                    }
                }

/* 796*/        return true;
            }

            public void updateSourcingState() {
/* 803*/        if (isFullySourced()) {
/* 804*/            setSourcingState(4);
                } else
/* 808*/        if (isSourcingFailed()) {
/* 809*/            setSourcingState(32);
                }
            }

            public boolean isBeingSourced() {
/* 815*/        List sourcingRequests = getSourcingRequests();
/* 816*/        for (int i = 0; i < sourcingRequests.size(); i++) {
/* 817*/            SourcingRequestCoreApprovable srca = (SourcingRequestCoreApprovable)sourcingRequests.get(i);
/* 819*/            if (srca.getApprovedState() >= 2) {
/* 820*/                Log.sourcing.debug("%s has %s in %s state therefore being sourced", getUniqueName(), srca.getUniqueName(), lookupApprovedStateNameForId(srca.getApprovedState()));
/* 823*/                return true;
                    }
                }

/* 827*/        return false;
            }

            public boolean allowKits() {
/* 839*/        return false;
            }

            public boolean seeEForms() {
/* 850*/        return false;
            }

            public static String lookupSourcingStateNameForId(int state) {
/* 858*/        return getStateName(SourcingStateNames, state);
            }

            public static int lookupSourcingStateIdForName(String stateName) {
/* 868*/        return getStateWithName(SourcingStateNames, stateName);
            }

            public String getDerivedStatus() {
/* 884*/        boolean test = isDenied() && getSourcingState() != 32;
/* 890*/        if (getSourcingState() <= 1 && !getIsAddedToProjects() || (getApprovedState() == 1 || isDenied() || getApprovedState() == 2) && getSourcingState() != 32 && getSourcingState() != 16) {
/* 896*/            return super.getDerivedStatus();
                }
/* 898*/        if (!StringUtil.nullOrEmptyString(getSourcingStatusString()) && getIsAddedToProjects()) {
/* 900*/            return getSourcingStatusString();
                } else {
/* 903*/            return lookupSourcingStateNameForId(getSourcingState());
                }
            }

            public boolean save ()
			    {
			        if (saving) {
			            return true;
			        }

			        boolean result = true;
			        try {
			            saving = true;
			            List lineItems = getLineItems();
			            int size = lineItems.size();
			            for (int i = 0; i < size; i++) {
			                LineItem li = (LineItem)lineItems.get(i);
			                Form form = (Form)li.getForm();
			                if (form != null) {
			                    result &= form.save();
			                }
			            }
			            result &= super.save();
			        }
			        finally {
			            saving = false;
			        }

			        return result;
			    }


            public String getCustomFilterQuery() {
/* 951*/        return null;
            }

            public Clusterable duplicateClusterable(ClusterRoot clusterRoot, BaseObject parent, Map map) {
/* 961*/        ProcureLineItemCollection copy = (ProcureLineItemCollection)super.duplicateClusterable(clusterRoot, parent, map);
/* 965*/        User me = User.getEffectiveUser();
/* 966*/        Iterator e = copy.getLineItemsIterator();
/* 969*/        ProcureLineItemCollection org = (ProcureLineItemCollection)clusterRoot;
/* 971*/        Iterator eOrg = org.getLineItemsIterator();
/* 972*/        Map orgLiToNewLi = MapUtil.map();
/* 973*/        do {
/* 973*/            if (!e.hasNext()) {
/* 974*/                break;
                    }
/* 974*/            LineItem lineItem = (LineItem)e.next();
/* 976*/            Form form = lineItem.getForm();
/* 977*/            if (form != null) {
/* 978*/                form.setCreator(me);
/* 979*/                form.setApprovable(copy);
/* 980*/                form.save();
                    }
/* 984*/            if (eOrg.hasNext()) {
/* 985*/                Object lineItemOrg = eOrg.next();
/* 987*/                if ((lineItemOrg instanceof ProcureLineItem) && (lineItem instanceof ProcureLineItem)) {
/* 989*/                    ProcureLineItem pliOrg = (ProcureLineItem)lineItemOrg;
/* 990*/                    ProcureLineItem pli = (ProcureLineItem)lineItem;
/* 991*/                    pli.getChildren().clear();
/* 993*/                    orgLiToNewLi.put(pliOrg, pli);
/* 994*/                    pli.setKitRequiredItem(pliOrg.getKitRequiredItem());
/* 995*/                    pli.basePut("ParentKit", pliOrg.baseGet("ParentKit"));
/* 996*/                    pli.basePut("KitInstanceId", pliOrg.baseGet("KitInstanceId"));
                        }
                    }
                } while (true);
/*1003*/        orgLiToNewLi.put(org.getDefaultLineItem(), copy.getDefaultLineItem());
/*1005*/        if (!orgLiToNewLi.isEmpty()) {
/*1007*/            Iterator it = orgLiToNewLi.keySet().iterator();
/*1008*/            do {
/*1008*/                if (!it.hasNext()) {
/*1009*/                    break;
                        }
/*1009*/                ProcureLineItem pli = (ProcureLineItem)it.next();
/*1010*/                if (pli.getParent() != null) {
/*1011*/                    ProcureLineItem newParentPli = (ProcureLineItem)orgLiToNewLi.get(pli.getParent());
/*1013*/                    ProcureLineItem newChildPli = (ProcureLineItem)orgLiToNewLi.get(pli);
/*1015*/                    if (newParentPli != null && newChildPli != null) {
/*1018*/                        newChildPli.setClusterRoot(copy);
/*1019*/                        newParentPli.getChildren().add(new LineItemReference(newChildPli));
/*1021*/                        newChildPli.setParent(newParentPli);
/*1024*/                        newChildPli.setClusterRoot(null);
                            }
                        }
                    } while (true);
                }
/*1031*/        return copy;
            }

            public void addedLineItemsElement(LineItem lineItem) {
/*1044*/        ariba.approvable.core.LineItemCollection lic = lineItem.getLineItemCollection();
/*1045*/        if (lic == null) {
/*1046*/            Log.sourcing.debug("Re-init LineItem");
/*1047*/            lineItem.init(getPartition(), this);
                }
/*1054*/        if (getClusterRoot() == null) {
/*1055*/            setClusters(this);
                }
/*1057*/        super.addedLineItemsElement(lineItem);
            }

            public void clearAccountingData() {
/*1068*/        List lineItems = getLineItems();
/*1069*/        for (int i = 0; i < lineItems.size(); i++) {
/*1070*/            ProcureLineItem li = (ProcureLineItem)lineItems.get(i);
/*1071*/            li.setAccountings((SplitAccountingCollection)null);
                }

            }

            protected boolean isUserChanging(User user) {
/*1087*/        ProcureLineItemCollection previous = (ProcureLineItemCollection)getPreviousVersion();
/*1089*/        if (previous == null) {
/*1090*/            return false;
                }
/*1093*/        if (SystemUtil.equal(previous.getChangedBy(), user)) {
/*1094*/            return true;
                }
/*1097*/        if (SystemUtil.equal(previous.getRequester(), user)) {
/*1098*/            return true;
                }
/*1101*/        return SystemUtil.equal(previous.getPreparer(), user);
            }

            public List getLineItemsToReprice(ProcureLineItem pli) {
/*1120*/        if (pli instanceof ContractSource) {
/*1121*/            return ((ContractSource)pli).getMatchingLineItems(this);
                } else {
/*1124*/            return ListUtil.list();
                }
            }

            public boolean canReprice() {
/*1140*/        Assert.that(false, "This method should not be called.");
/*1141*/        return false;
            }

            /**
             * @deprecated Method generatePayloadID is deprecated
             */

            public static String generatePayloadID(Date date) {
/*1154*/        return CXMLUtil.documentService().payloadID();
            }

            protected static long getNextCounter() {
/*1159*/        long next = Base.getService().getNextNamedLong("_pAyLoAd_");
/*1160*/        if (next < 0L) {
/*1161*/            next = 1L;
/*1162*/            Base.getService().setNamedLong("_pAyLoAd_", next);
                }
/*1165*/        return next;
            }

            public void updateAmountField(String fieldName) {
/*1173*/        Assert.that(!StringUtil.nullOrEmptyOrBlankString(fieldName), "FieldName cannot be null");
/*1175*/        Money amount = null;
/*1176*/        if (ListUtil.nullOrEmptyList(getLineItems())) {
/*1177*/            amount = new Money(0.0D, getReportingCurrency());
                } else {
/*1180*/            amount = LineItem.sumLineItems(getLineItemsIterator(), getReportingCurrency(), fieldName);
                }
/*1186*/        Money oldAmount = (Money)getDottedFieldValue(fieldName);
/*1187*/        if (amount == oldAmount || amount.equals(oldAmount)) {
/*1188*/            return;
                } else {
/*1190*/            setDottedFieldValue(fieldName, amount);
/*1191*/            return;
                }
            }

            public List unknownCommonSupplierNotificationMsgs(OrganizationID supplierID, BaseId subObjectId, boolean hasProfile, String supplierName) {
/*1205*/        new SimpleProcureRecord(this, getPreparer(), null, "UnknownSupplierRecord");
/*1211*/        SupplierNotificationMessage msgToSupplierManager = unknownSupplierSMNotificationMsg(supplierID, subObjectId, hasProfile, true, supplierName);
/*1215*/        SupplierNotificationMessage msgToUser = unknownSupplierUserNotificationMsg(supplierID, supplierName);
/*1218*/        return ListUtil.list(msgToUser, msgToSupplierManager);
            }

            public List unknownSupplierNotificationMsgs(OrganizationID supplierID, Partition partition, CommonSupplier cs, BaseId subObjectId, boolean hasProfile, String supplierName) {
/*1231*/        new SimpleProcureRecord(this, getPreparer(), null, "UnknownSupplierRecord");
/*1236*/        SupplierNotificationMessage msgToSupplierManager = unknownSupplierSMNotificationMsg(supplierID, subObjectId, hasProfile, false, supplierName);
/*1243*/        SupplierNotificationMessage msgToUser = unknownSupplierUserNotificationMsg(supplierID, supplierName);
/*1246*/        return ListUtil.list(msgToUser, msgToSupplierManager);
            }

            public List supplierUpdatedNotificationMsgs(Supplier supplier, BaseId subObjectId) {
/*1254*/        new SimpleProcureRecord(this, getPreparer(), null, "UnknownSupplierApprovedRecord");
/*1262*/        List receivers = ListUtil.list(getPreparer().getBaseId());
/*1266*/        LocalizedString subject = new LocalizedString("ariba.procure.core", "SupplierUpdatedNotificationSubject");
/*1272*/        LocalizedString body = new LocalizedString("ariba.procure.core", "SupplierUpdatedNotificationBody", supplier.getSupplierID().getCacheKey(), DateFormatter.getStringValue(getCreateDate()), getName(), supplier.getName());
/*1281*/        Map receiverSpecificMsg = getUserSpecificMessage(receivers, "ResubmitRequestURL", false);
/*1285*/        SupplierNotificationMessage msgToUser = new SupplierNotificationMessage(receivers, subject, body, receiverSpecificMsg);
/*1289*/        return ListUtil.list(msgToUser);
            }

            public List commonSupplierUpdatedNotificationMsgs(CommonSupplier commonSupplier, BaseId subObjectId) {
/*1297*/        new SimpleProcureRecord(this, getPreparer(), null, "UnknownSupplierApprovedRecord");
/*1303*/        List receivers = ListUtil.list(getPreparer().getBaseId());
/*1307*/        LocalizedString subject = new LocalizedString("ariba.procure.core", "CommonSupplierUpdatedNotificationSubject");
/*1313*/        LocalizedString body = new LocalizedString("ariba.procure.core", "CommonSupplierUpdatedNotificationBody", commonSupplier.getOrganizationID().getCacheKey(), DateFormatter.getStringValue(getCreateDate()), getName(), commonSupplier.getNameStringForDefaultLanguage());
/*1323*/        Map receiverSpecificMsg = getUserSpecificMessage(receivers, "ResubmitRequestURL", false);
/*1327*/        SupplierNotificationMessage msgToUser = new SupplierNotificationMessage(receivers, subject, body, receiverSpecificMsg);
/*1330*/        return ListUtil.list(msgToUser);
            }

            public List supplierOverdueNotificationMsgs(OrganizationID supplierID, BaseId subObjectId) {
/*1338*/        List receivers = ListUtil.list(getPreparer().getBaseId());
/*1342*/        LocalizedString subject = new LocalizedString("ariba.procure.core", "SupplierOverdueNotificationSubject");
/*1348*/        LocalizedString body = new LocalizedString("ariba.procure.core", "SupplierOverdueNotificationBody", supplierID.getCacheKey(), DateFormatter.getStringValue(getCreateDate()), getName());
/*1357*/        return ListUtil.list(new SupplierNotificationMessage(receivers, subject, body));
            }

            private SupplierNotificationMessage unknownSupplierSMNotificationMsg(OrganizationID supplierID, BaseId subObjectId, boolean hasProfile, boolean isCommonSupplier, String supplierName) {
/*1369*/        List receivers = Permission.getPermission("SupplierManager").getAllUsers();
/*1372*/        LocalizedString subject = new LocalizedString("ariba.procure.core", "UnknownSupplierIDNotificationSubject");
/*1377*/        String bodyKey = "";
/*1378*/        if (isCommonSupplier) {
/*1379*/            bodyKey = hasProfile ? "UnknownSupplierIDNotificationBody1" : "UnknownSupplierIDNotificationBody2";
                } else {
/*1384*/            bodyKey = hasProfile ? "UnknownSupplierIDNotificationBody3" : "UnknownSupplierIDNotificationBody4";
                }
/*1391*/        LocalizedString body = new LocalizedString("ariba.procure.core", bodyKey, supplierName == null ? "" : ((Object) (supplierName)), supplierID.getCacheKey(), getPreparer().getMyName(), getPreparer().getEmailAddress(), getName(), getPartition().getLabel());
/*1402*/        Map receiverSpecificMsg = getUserSpecificMessage(receivers, "ViewRequestURL", true);
/*1405*/        SupplierNotificationMessage msgToSupplierManager = new SupplierNotificationMessage(receivers, subject, body, receiverSpecificMsg);
/*1409*/        if (hasProfile) {
/*1410*/            msgToSupplierManager.setAttachProfile(true);
                }
/*1412*/        return msgToSupplierManager;
            }

            private SupplierNotificationMessage unknownSupplierUserNotificationMsg(OrganizationID supplierID, String supplierName) {
/*1421*/        List receivers = ListUtil.list(getPreparer().getBaseId());
/*1425*/        LocalizedString subject = new LocalizedString("ariba.procure.core", "UnknownSupplierUserNotificationSubject");
/*1431*/        LocalizedString body = new LocalizedString("ariba.procure.core", "UnknownSupplierUserNotificationBody", supplierName == null ? "" : ((Object) (supplierName)), supplierID.getCacheKey(), getName(), SupplierSyncUtil.overdueTimeLimit(), getSupplierManagerContactInfo());
/*1442*/        return new SupplierNotificationMessage(receivers, subject, body);
            }

            private Map getUserSpecificMessage(List users, String urlMessageKey, boolean includeRecipientList) {
/*1450*/        Map result = MapUtil.map();
/*1451*/        int i = 0;
/*1451*/        for (int size = users.size(); i < size; i++) {
/*1452*/            BaseId id = (BaseId)users.get(i);
/*1453*/            if (id == null) {
/*1454*/                continue;
                    }
/*1454*/            User user = (User)id.get();
/*1455*/            String url = Notification.webJumperURL(user, this.id);
/*1459*/            String msg = Fmt.Sil(user.getLocale(), "ariba.procure.core", urlMessageKey, url);
/*1463*/            if (includeRecipientList) {
/*1464*/                String recipients = Fmt.Sil(user.getLocale(), "ariba.procure.core", "RecipientList", getRecipientList(users, user));
/*1468*/                msg = StringUtil.strcat(msg, recipients);
                    }
/*1470*/            result.put(user.getBaseId(), msg);
                }

/*1473*/        return result;
            }

            private String getRecipientList(List users, User userExclude) {
/*1478*/        FastStringBuffer buffer = new FastStringBuffer();
/*1479*/        int i = 0;
/*1479*/        for (int size = users.size(); i < size; i++) {
/*1480*/            BaseId id = (BaseId)users.get(i);
/*1481*/            if (id == null) {
/*1482*/                continue;
                    }
/*1482*/            User user = (User)id.get();
/*1483*/            if (!user.equals(userExclude)) {
/*1484*/                buffer.append(Fmt.S("%s (email:%s)\n", user.getMyName(), user.getEmailAddress()));
                    }
                }

/*1491*/        return buffer.toString();
            }

            private String getSupplierManagerContactInfo() {
/*1496*/        List supplierManagers = Permission.getPermission("SupplierManager").getAllUsers();
/*1498*/        FastStringBuffer buffer = new FastStringBuffer();
/*1499*/        for (int i = 0; i < supplierManagers.size(); i++) {
/*1500*/            BaseId id = (BaseId)supplierManagers.get(i);
/*1501*/            if (id != null) {
/*1502*/                User supplierManager = (User)id.get();
/*1503*/                buffer.append(Fmt.S("%s (email:%s)\n", supplierManager.getMyName(), supplierManager.getEmailAddress()));
                    }
                }

/*1508*/        return buffer.toString();
            }

            public void processSourcingFailed() {
/*1520*/        Assert.that(false, (new StringBuilder()).append("This method must be implemented by the subclass: ").append(getClass().getName()).toString());
            }

            public void processSourcingConfirmFailed() {
/*1534*/        Assert.that(false, (new StringBuilder()).append("This method must be implemented by the subclass: ").append(getClass().getName()).toString());
            }

            public void processSourcingCompleted() {
/*1549*/        Assert.that(false, (new StringBuilder()).append("This method must be implemented by the subclass: ").append(getClass().getName()).toString());
            }

            public void sendSourcingConfirmationMessage() {
/*1557*/        Assert.that(false, (new StringBuilder()).append("This method must be implemented by the subclass: ").append(getClass().getName()).toString());
            }

            public void sendCancelSourcingMessage() {
/*1565*/        Assert.that(false, (new StringBuilder()).append("This method must be implemented by the subclass: ").append(getClass().getName()).toString());
            }

            public int forceToState(Comment comment, int accessOp, String recordType, String toEvent) {
/*1585*/        User user = User.getEffectiveUser();
/*1587*/        int access = getAccess(accessOp, user);
/*1588*/        if (access != Access.Now) {
/*1589*/            return access;
                }
/*1593*/        Comment realComment = addComment(comment, user);
/*1596*/        SimpleProcureRecord record = new SimpleProcureRecord(this, user, realComment, recordType);
/*1607*/        try {
/*1607*/            WorkflowService.getService().processEventSync(this, toEvent);
                }
/*1610*/        catch (EventQueueException eqe) {
/*1612*/            Log.approvable.debug("PUT REAL ERROR HERE");
                }
/*1615*/        return Access.Now;
            }

            public Approvable createNextVersion(User authUser) {
/*1620*/        ProcureLineItemCollection next = (ProcureLineItemCollection)super.createNextVersion(authUser);
/*1623*/        next.setSourcingState(1);
/*1624*/        Map orgLiToNewLiMap = MapUtil.map();
                ProcureLineItem nextLineItem;
                ProcureLineItem prevLI;
/*1626*/        for (Iterator nextLineItems = next.getAllLineItems(); nextLineItems.hasNext(); orgLiToNewLiMap.put(prevLI, nextLineItem)) {
/*1628*/            nextLineItem = (ProcureLineItem)nextLineItems.next();
/*1629*/            nextLineItem.removeDeletedSplitAccountings();
/*1631*/            prevLI = (ProcureLineItem)nextLineItem.getOldLineItemValues();
                }

/*1635*/        List nextLines = next.getLineItems();
/*1636*/        for (Iterator i$ = nextLines.iterator(); i$.hasNext();) {
/*1636*/            ProcureLineItem nextLI = (ProcureLineItem)i$.next();
/*1637*/            ProcureLineItem prevLineItem = (ProcureLineItem)nextLI.getOldLineItemValues();
            if (prevLineItem != null && prevLineItem.hasChildren()) {
/*1639*/                nextLI.getChildren().clear();
/*1640*/                Iterator prevChildren = prevLineItem.getChildrenIterator();
/*1641*/                while (prevChildren.hasNext())  {
/*1642*/                    LineItemReference ref = (LineItemReference)prevChildren.next();
/*1643*/                    ProcureLineItem prevChildLine = (ProcureLineItem)ref.getLineItem();
/*1644*/                    ProcureLineItem nextChildLine = (ProcureLineItem)orgLiToNewLiMap.get(prevChildLine);
/*1645*/                    if (nextChildLine != null) {
/*1646*/                        ProcureUtil.linkParentAndChild(nextLI, nextChildLine);
                            }
                        }
                    }
                }

/*1651*/        orgLiToNewLiMap.clear();
/*1654*/        SharedGlobalItemProperties.copySharedGlobalProperties(this, next);
/*1666*/        if (this instanceof AttachmentSource) {
/*1667*/            List nextAttachments = next.getAttachments();
/*1668*/            for (int i = 0; i < nextAttachments.size(); i++) {
/*1669*/                AttachmentWrapper attachment = (AttachmentWrapper)nextAttachments.get(i);
/*1670*/                attachment.setAttachmentReference(null);
/*1671*/                AttachmentReference.addReference(attachment);
                    }

/*1673*/            AttachmentWrapper.fixupReferences(this, next);
                }
/*1676*/        return next;
            }

            protected List appendVectors(List target, List source) {
/*1691*/        if (ListUtil.nullOrEmptyList(source)) {
/*1692*/            return target;
                }
/*1695*/        if (target == null) {
/*1696*/            target = ListUtil.list();
                }
/*1699*/        for (int i = 0; i < source.size(); i++) {
/*1700*/            Object element = source.get(i);
/*1701*/            if (element instanceof BaseId) {
/*1702*/                element = ((BaseId)element).getIfAny();
                    }
/*1704*/            if (element != null) {
/*1705*/                target.add(element);
                    }
                }

/*1709*/        return target;
            }

            protected void setLineItemsFields(String fieldName, Object value) {
/*1717*/        List lineItemsVector = getLineItems();
/*1718*/        int numLineItems = lineItemsVector.size();
/*1719*/        for (int i = 0; i < numLineItems; i++) {
/*1720*/            LineItem li = (LineItem)lineItemsVector.get(i);
/*1721*/            li.setFieldValue(fieldName, value);
                }

            }

            public void addAmount(String fieldName, Money amount) {
/*1732*/        Object oldAmount = getDottedFieldValue(fieldName);
/*1734*/        if (oldAmount != null) {
/*1735*/            Assert.that(oldAmount instanceof Money, "Field %s must be of type %s", fieldName, "ariba.basic.core.Money");
/*1739*/            amount = ((Money)oldAmount).add(amount);
                }
/*1742*/        setDottedFieldValue(fieldName, amount);
            }

            public Money getAmountLessAmount(Money amount1, Money amount2) {
/*1750*/        if (amount1 == null) {
/*1751*/            return null;
                }
/*1753*/        if (amount2 == null) {
/*1754*/            return amount1;
                }
/*1757*/        Money less = Money.subtract(amount1, amount2);
/*1760*/        if (less.getAmount().signum() < 0) {
/*1761*/            return new Money(Constants.ZeroBigDecimal, amount1.getCurrency());
                } else {
/*1764*/            return less;
                }
            }

            public void addCommodityCodeToLine(ProcureLineItem pli) {
/*1769*/        Partition partition = getPartition();
/*1770*/        ProductDescription pd = pli.getDescription();
/*1771*/        ariba.basic.core.CommodityCode ccc = pd.getCommonCommodityCode();
/*1772*/        if (ccc == null) {
/*1773*/            Log.cxml.debug("CommonCommodityCode has not been set on %s, %s", this, pli);
/*1775*/            return;
                }
/*1778*/        List comExportMapEntries = pd.getCommodityExportMapChoices(getPartition());
/*1780*/        if (ListUtil.nullOrEmptyList(comExportMapEntries)) {
/*1781*/            Log.fixme.warning(1053, ccc, partition);
                } else {
/*1784*/            ariba.base.fields.FieldProperties fp = pli.getFieldProperties("CommodityCode");
/*1785*/            int numCEME = comExportMapEntries.size();
/*1786*/            boolean comExportMapEntrySet = false;
/*1787*/            CommodityExportMapEntry oldCEME = pli.getCommodityExportMapEntry();
/*1788*/            ariba.common.core.PartitionedCommodityCode oldCC = pli.getCommodityCode();
/*1789*/            int j = 0;
/*1789*/            do {
/*1789*/                if (j >= numCEME) {
/*1790*/                    break;
                        }
/*1790*/                CommodityExportMapEntry comExportMapEntry = (CommodityExportMapEntry)comExportMapEntries.get(j);
/*1793*/                pli.setCommodityExportMapEntry(comExportMapEntry);
/*1795*/                boolean isValid = ValueSourceUtil.evaluateConstraints(pli, "CommodityExportMapEntry", "__validity", null, true, null, null);
/*1804*/                if (isValid) {
/*1805*/                    Log.cxml.debug("Setting CommodityExportMapEntry %s on LineItem %s", comExportMapEntry, pli);
/*1809*/                    comExportMapEntrySet = true;
/*1810*/                    break;
                        }
/*1789*/                j++;
                    } while (true);
/*1813*/            if (!comExportMapEntrySet) {
/*1814*/                pli.setCommodityExportMapEntry(oldCEME);
/*1815*/                pli.setCommodityCode(oldCC);
/*1816*/                Log.cxml.debug("Could not find a valid CommodityExportMapEntry for CommonCommodityCode %s, partition %s", ccc, partition);
                    }
                }
            }

            public List getHeaderLevelExternalComments() {
/*1830*/        List comments = getComments();
/*1831*/        List headerExternalComments = ListUtil.list();
/*1832*/        for (int i = 0; i < comments.size(); i++) {
/*1833*/            Comment comment = (Comment)comments.get(i);
/*1834*/            if (comment.getExternalComment() && comment.getLineItem() == null) {
/*1836*/                headerExternalComments.add(comment);
                    }
                }

/*1840*/        return headerExternalComments;
            }

            public List getHeaderSubmitExternalComments() {
/*1850*/        List lstReturn = ListUtil.list();
/*1851*/        BaseVector allComments = getComments();
/*1852*/        Comment comment = null;
/*1853*/        for (int i = 0; i < allComments.size(); i++) {
/*1854*/            comment = (Comment)allComments.get(i);
/*1855*/            if (comment.getExternalComment() && (comment.getType() == 2 || comment.getType() == 0x400000 || comment.getType() == 1)) {
/*1861*/                lstReturn.add(comment);
                    }
                }

/*1864*/        return lstReturn;
            }

            public List getExternalCommentsForLI(LineItem argLI) {
/*1874*/        List lstReturn = ListUtil.list();
/*1876*/        BaseVector allComments = getComments();
/*1877*/        Comment comment = null;
/*1878*/        for (int i = 0; i < allComments.size(); i++) {
/*1879*/            comment = (Comment)allComments.get(i);
/*1880*/            if (comment.getExternalComment() && comment.getType() == 1 && comment.getLineItem() != null && comment.getLineItem().getNumberInCollection() == argLI.getNumberInCollection()) {
/*1887*/                lstReturn.add(comment);
                    }
                }

/*1891*/        return lstReturn;
            }

            public List getAuthoringAttachments() {
/*1906*/        List attachments = ListUtil.list();
/*1907*/        if (getExternalSourcingId() != null) {
/*1908*/            Iterator it = getAttachmentsIterator();
/*1909*/            do {
/*1909*/                if (!it.hasNext()) {
/*1910*/                    break;
                        }
/*1910*/                ariba.app.util.Attachment attachment = ((AttachmentWrapper)it.next()).getAttachment();
/*1911*/                if (attachment instanceof RemoteAttachment) {
/*1912*/                    attachments.add(attachment);
                        }
                    } while (true);
                }
/*1916*/        return attachments;
            }

            public ProcureLineItemCollection getTargetLineItemCollection() {
/*1932*/        return this;
            }

            public boolean supplierSetInAllLineItems() {
/*1943*/        boolean isSupplierNull = false;
/*1944*/        Iterator itr = getLineItems().iterator();
/*1945*/        do {
/*1945*/            if (!itr.hasNext()) {
/*1947*/                break;
                    }
/*1947*/            ProcureLineItem lineItem = (ProcureLineItem)itr.next();
/*1948*/            if (lineItem.getSupplier() != null && !CategoryUtil.isUnspecifiedSupplier(lineItem.getSupplier())) {
/*1950*/                continue;
                    }
/*1950*/            isSupplierNull = true;
/*1951*/            break;
                } while (true);
/*1954*/        return !isSupplierNull;
            }

            public boolean isTriggerFlagSet() {
/*1967*/        return triggerFlag;
            }

            public void setTriggerFlag(boolean trgrFlag) {
/*1972*/        triggerFlag = trgrFlag;
            }

            public void setTotalCost(Money total) {
/*1988*/        Money oldTotal = getTotalCost();
/*1989*/        if (ListUtil.nullOrEmptyList(getLineItems()) && (oldTotal == null || !oldTotal.isZero())) {
/*1991*/            oldTotal = new Money(0.0D, getReportingCurrency());
/*1992*/            super.setTotalCost(oldTotal);
                }
/*1998*/        Money blanketAmount = getBlanketAmountMaybe();
/*1999*/        boolean dontUpdate = false;
/*2000*/        if (blanketAmount != null && total.compareTo(blanketAmount) <= 0) {
/*2002*/            if (oldTotal.compareTo(blanketAmount) == 0) {
/*2003*/                dontUpdate = true;
                    } else {
/*2006*/                total = new Money(blanketAmount);
                    }
                }
/*2011*/        if (total == oldTotal || total != null && total.equals(oldTotal) || dontUpdate) {
/*2013*/            return;
                } else {
/*2015*/            Log.categoryProcurement.debug("********* Setting TotalCost to %s", total);
/*2016*/            super.setTotalCost(total);
/*2017*/            return;
                }
            }

            protected Money getBlanketAmountMaybe() {
/*2022*/        if ((this instanceof CategoryTemplateDetailsSource) && ((CategoryTemplateDetailsSource)this).getCategoryTemplateDetails() != null) {
/*2024*/            CategoryTemplateDetails chd = ((CategoryTemplateDetailsSource)this).getCategoryTemplateDetails();
/*2026*/            if (chd != null && chd.getCategoryTemplateProperties() != null) {
/*2027*/                boolean isBP = BooleanFormatter.getBooleanValue(chd.getCategoryTemplateProperties().getBlanketPurchasing());
/*2030*/                boolean useBlanketAmount = !BooleanFormatter.getBooleanValue(chd.getCategoryTemplateProperties().getBlanketPurchasingItemizeAllCosts());
/*2034*/                if (isBP && useBlanketAmount && chd.getCategoryTemplateProperties().getBlanketAmount() != null) {
/*2036*/                    Money value = chd.getCategoryTemplateProperties().getBlanketAmount().getValue();
/*2038*/                    Log.categoryProcurement.debug("***** Blanket Amount is %s", value);
/*2039*/                    return value;
                        }
                    }
                }
/*2043*/        return null;
            }

            public boolean allowMilestoneItemization() {
/*2053*/        return false;
            }

            public boolean useOldStyleMilestoneItemization(Milestone milestone) {
/*2060*/        MilestoneWorksheet mw = milestone.getMilestoneWorksheet();
/*2061*/        if (mw != null && mw.getMilestoneWorksheetItems() != null && !mw.getMilestoneWorksheetItems().isEmpty()) {
/*2064*/            return true;
                }
/*2068*/        return milestone.getItemization() == null;
            }

            public LineItem getRepresentativeLineItem() {
/*2087*/        if (super.getRepresentativeLineItem() != null) {
/*2088*/            return super.getRepresentativeLineItem();
                }
/*2090*/        if (CategoryTemplateDetails.isBlanketPurchasing(this)) {
/*2091*/            return ((CategoryTemplateDetailsSource)this).getCategoryTemplateLineItem();
                } else {
/*2093*/            return null;
                }
            }

            public static ProcureLineItemCollection create(AttachmentInfo attachmentInfo, String excelEvent) {
/*2115*/        return create(attachmentInfo, excelEvent, null);
            }

            public static ProcureLineItemCollection
			        create (AttachmentInfo attachmentInfo,
			                String excelEvent,
			                List errors)
			    {
			        Map userInfo = MapUtil.map(1);
			        ProcureLineItemCollection plic = null;
			        try {
			            CallCenter callCenter = CallCenter.defaultCenter();
			                // retrieve attachment info
			            String attachmentFileName = attachmentInfo.getFileName();
			            String mimeType = attachmentInfo.getMimeType();
			            InputStream input = attachmentInfo.getContents();

			            if (input == null) {
			                Log.excelImport.debug("No Content");
			                return null;
			            }
			                //write attachment contents to tmp file
			            File tmpDir = FileUtil.directory(SystemUtil.getLocalTempDirectory());

			            String fileType = "";
			            if (attachmentFileName.contains(FileUploadHelper.XLSXExtension)) {
			                fileType = "xlsx";
			            }
			            else if (attachmentFileName.contains(FileUploadHelper.XLSExtension)) {
			                fileType = "xls";
			            }
			            File tmpFile = TempFile.createTempFile(tmpDir, "excelimport", fileType);
			            OutputStream out = IOUtil.bufferedOutputStream(tmpFile);
			            IOUtil.inputStreamToOutputStream(input, out);
			            out.close();

			            Log.excelImport.debug(
			                "Saved File is %s", tmpFile.getAbsolutePath());
			            String fileName = tmpFile.getAbsolutePath();
			            Map userData = MapUtil.map(2);

			            String adapterSource =
			                getExcelAdapterSourceName(tmpFile.getName());
			            userData.put(EventSourceKey,
			                         adapterSource);

			            userInfo.put(AMFConstants.KeyPartition,
			                         Base.getSession().getPartition().getName());

			            User user = User.getEffectiveUser();
			            Log.excelImport.debug("Before event user is %s",
			                                  user);
			            userInfo.put(PreparerKey, user);

			            if (errors != null) {
			                ExcelImportErrorHandler handler =
			                    new ExcelImportErrorHandler(
			                        user,
			                        Fields.getService().getNow(),
			                        adapterSource,
			                        attachmentFileName,
			                        Base.getSession().getLocale()
			                        );
			                userInfo.put(ErrorLogKey, handler);
			            }

			            userData.put(FilenameKey, fileName);
			            userData.put(HeaderFileNameKey, fileName);
			            userData.put(EventContext.KeyLocale,
			                         Base.getSession().getLocale().toString());
			            callCenter.call(excelEvent, userData, userInfo);
			            plic = getCreatedObject(adapterSource);
			        }
			        catch (MessagingException me) {
			            Log.excelImport.debug(
			                "Messaging Exception while loading Excel file %s",
			                SystemUtil.stackTrace(me));
			            plic = null;

			            if (errors != null) {
			                    //add this error in the handler
			                ExcelImportErrorHandler handler =
			                    (ExcelImportErrorHandler)userInfo.get(ErrorLogKey);

			                handler.addError(null, null, null,
			                                 StringTable,
			                                 "ImportFatalErrorMsg",
			                                 null,
			                                 me);
			            }
			        }
			        catch (IOException ioe) {
			            if (errors != null) {
			                errors.add(ioe.getMessage());
			            }
			            Log.excelImport.debug(
			                "IOException while loading Excel file %s",
			                SystemUtil.stackTrace(ioe));
			            plic = null;
			        }
			        finally {
			                //has to save the log in the finally because ActionErrors
			                //are not caught as Exceptions.
			            if (errors != null) {
			                ExcelImportErrorHandler handler =
			                    (ExcelImportErrorHandler)userInfo.get(ErrorLogKey);

			                ExcelImportLog log = handler.saveLog();

			                if (!log.getMessages().isEmpty()) {
			                        //there is error, could be from ActionErrors
			                    errors.add((ExcelImportLog)log.getBaseId().get());
			                    if (plic != null) {
			                        plic.setImportLog(log);
			                    }
			                }
			                Base.getSession().transactionCommit();
			            }

			            return plic;
			        }
			    }

            public static String getExcelAdapterSourceName(String fileName) {
/*2259*/        long adapterSourceLong = Base.getService().getNextNamedLong("XLSID");
/*2261*/        return Fmt.S("%s:%s", Long.toString(adapterSourceLong), fileName);
            }

            public static ProcureLineItemCollection getCreatedObject(String adapterSource) {
/*2274*/        AQLQuery query = new AQLQuery("ariba.procure.core.ProcureLineItemCollection");
/*2275*/        query.addSelectElement("AdapterSource");
/*2276*/        query.andEqual("AdapterSource", adapterSource);
/*2277*/        AQLOptions options = new AQLOptions(Base.getService().getPartition());
/*2278*/        AQLResultCollection results = Base.getService().executeQuery(query, options);
/*2280*/        List resultsErrors = results.getErrors();
/*2281*/        if (!ListUtil.nullOrEmptyList(resultsErrors)) {
/*2282*/            for (int i = 0; i < resultsErrors.size(); i++) {
/*2283*/                AQLError error = (AQLError)resultsErrors.get(i);
/*2284*/                Log.excelImport.warning(7079, error);
                    }

/*2286*/            return null;
                }
/*2290*/        if (results.isEmpty()) {
/*2291*/            return null;
                }
/*2294*/        if (results.next()) {
/*2295*/            BaseId Id = results.getBaseId(0);
/*2296*/            return (ProcureLineItemCollection)Base.getSession().objectForWrite(Id);
                } else {
/*2298*/            return null;
                }
            }

            public void setProjectsAssociation() {
/*2313*/        setIsAddedToProjects(true);
            }

            public boolean maintainExternalCredentials() {
/*2328*/        return getSourceCredentials() != null;
            }

            public boolean supportsService(Class serviceInterface) {
/*2334*/        return serviceInterface.isAssignableFrom(getClass()) || getServices().supportsService(serviceInterface);
            }

            public ServiceManager getServices() {
/*2340*/        ServiceManager mgr = super.getServices();
/*2341*/        if (mgr == null) {
/*2342*/            setServices(new ServiceManager(getPartition()));
                }
/*2344*/        return super.getServices();
            }

            public Object getService(Class serviceInterface) {
/*2349*/        if (serviceInterface.isAssignableFrom(getClass())) {
/*2350*/            return this;
                } else {
/*2352*/            return getServices().getService(serviceInterface);
                }
            }

            public Object getServiceClient() {
/*2360*/        return null;
            }

            public void setServiceClient(Object obj) {
            }

            protected void setSplitAccountingChangedState() {
/*2378*/        if (hasPreviousVersion()) {
                    ProcureLineItem pli;
/*2379*/            for (Iterator lis = getAllLineItems(); lis.hasNext(); pli.setSplitAccountingChangedState()) {
/*2381*/                pli = (ProcureLineItem)lis.next();
                    }

                }
            }

            protected void renumberSplitLineItems(boolean forceRenumber) {
/*2389*/        if (!forceRenumber) {
/*2390*/            return;
                }
/*2392*/        List lineItems = getLineItems();
/*2393*/        int numLineItems = lineItems.size();
/*2394*/        for (int i = 0; i < numLineItems; i++) {
/*2395*/            ProcureLineItem pli = (ProcureLineItem)lineItems.get(i);
/*2396*/            SplitAccountingCollection sac = pli.getAccountings();
/*2397*/            if (sac != null) {
/*2398*/                sac.renumberSplits(forceRenumber);
                    }
                }

            }

            public void removedLineItemsElement(LineItem lineItem) {
/*2412*/        super.removedLineItemsElement(lineItem);
/*2414*/        ProcureLineItem pli = (ProcureLineItem)lineItem;
/*2415*/        ProcureLineItem parent = pli.getParent();
/*2421*/        if (parent == null) {
/*2423*/            parent = pli;
/*2424*/            BaseVector children = parent.getChildren();
/*2425*/            ProcureLineItem child = null;
/*2426*/            for (int i = 0; i < children.size(); i++) {
/*2427*/                child = (ProcureLineItem)((LineItemReference)children.get(i)).getLineItem();
/*2432*/                child.setParent(null);
/*2441*/                boolean savedGraphNode = getDeletedLineItems().graphNode;
/*2442*/                getDeletedLineItems().graphNode = true;
/*2443*/                getLineItems().remove(child);
/*2444*/                getDeletedLineItems().graphNode = savedGraphNode;
/*2445*/                child.setParent(parent);
                    }

                } else {
/*2451*/            BaseVector children = parent.getChildren();
/*2452*/            BaseId childBid = null;
/*2453*/            int i = 0;
/*2453*/            do {
/*2453*/                if (i >= children.size()) {
/*2455*/                    break;
                        }
/*2455*/                childBid = BaseId.parse(((LineItemReference)children.get(i)).getLineItemBaseId());
/*2457*/                if (childBid.equals(lineItem.getBaseId())) {
/*2458*/                    children.remove(i);
/*2459*/                    break;
                        }
/*2453*/                i++;
                    } while (true);
                }
            }

            public void requesterChanged(User r) {
/*2471*/        if (shouldDefaultOnRequesterChange()) {
/*2473*/            getDefaultLineItem().setDefaultsFromRequester(r);
/*2476*/            List lineItems = getLineItemsToDefault();
/*2477*/            int i = 0;
/*2477*/            for (int count = lineItems.size(); i < count; i++) {
/*2478*/                LineItem item = (LineItem)lineItems.get(i);
/*2479*/                item.setDefaultsFromApprovable(this);
                    }

                }
            }

            protected List getLineItemsToDefault() {
/*2492*/        return getLineItems();
            }

            public boolean shouldAccrue() {
/*2501*/        return true;
            }

            protected boolean shouldDefaultOnRequesterChange() {
/*2515*/        Partition partition = Base.getSession().getPartition();
/*2516*/        boolean relaxDefault = Base.getService().getBooleanParameter(partition, "Application.Procure.RelaxDefaultOnRequesterChange");
/*2519*/        boolean requesterFirstSet = oldRequester == null && newRequester != null;
/*2520*/        boolean requesterActuallyChanging = oldRequester != null && !oldRequester.equals(newRequester) || oldRequester == null && newRequester != null;
/*2523*/        return (!relaxDefault || requesterFirstSet) && requesterActuallyChanging;
            }

            public void setRequester(User u) {
/*2536*/        oldRequester = getRequester();
/*2537*/        newRequester = u;
/*2538*/        super.setRequester(newRequester);
/*2539*/        oldRequester = null;
/*2540*/        newRequester = null;
            }

            public Locale getSupplierLocale() {
/*2545*/        Iterator e = getLineItemsIterator();
/*2546*/        if (e.hasNext()) {
/*2547*/            ProcureLineItem procureLineItem = (ProcureLineItem)e.next();
/*2548*/            SupplierLocation location = procureLineItem.getSupplierLocation();
/*2549*/            if (location != null) {
/*2550*/                return Country.getLocale(location.getCountry());
                    }
                }
/*2553*/        return ResourceService.getService().getLocale();
            }

            public List getKitLineItemsList() {
/*2563*/        return ListUtil.list();
            }

            public List getLineItemsByKitInstanceId(String kitInstanceId) {
/*2575*/        return ListUtil.list();
            }

            public List getRootLineItems() {
/*2588*/        List rootNodes = ListUtil.list();
/*2591*/        Iterator iter = getLineItemsIterator();
/*2592*/label0:
/*2592*/        do {
/*2592*/            if (!iter.hasNext()) {
/*2593*/                break;
                    }
/*2593*/            ProcureLineItem pli = (ProcureLineItem)iter.next();
/*2594*/            if (pli.isInKit()) {
/*2598*/                String kitId = (String)pli.getDottedFieldValue("KitInstanceId");
/*2599*/                Iterator kitIter = getKitLineItemsList().iterator();
                        ProcureLineItem kitLine;
/*2600*/                do {
/*2600*/                    if (!kitIter.hasNext()) {
/*2601*/                        continue label0;
                            }
/*2601*/                    kitLine = (ProcureLineItem)kitIter.next();
                        } while (!kitId.equals((String)kitLine.getDottedFieldValue("KitInstanceId")) || rootNodes.contains(kitLine));
/*2605*/                rootNodes.add(kitLine);
                    } else
/*2612*/            if (pli.getParent() == null) {
/*2613*/                rootNodes.add(pli);
                    }
                } while (true);
/*2618*/        return rootNodes;
            }

            static  {
/* 332*/        Map spec = MapUtil.map();
/* 333*/        spec.put("name", "chg");
/* 334*/        spec.put("width", Constants.getInteger(74));
/* 335*/        spec.put("color", "#330066");
/* 336*/        spec.put("tip", "ChargesTabTip");
/* 337*/        spec.put("strings", "ariba.procure.core");
/* 338*/        tabs.put("chg", spec);
            }
}
