/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/buyer/procure/ariba/procure/core/action/IntegrationPostLoadRequisition.java#1 $

    Responsible: avidyasagar
*/

package ariba.purchasing.core.action;


import ariba.approvable.core.ApprovableUtil;
import ariba.approvable.core.SimpleRecord;
import ariba.base.core.Base;
import ariba.base.core.BaseSession;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommonSupplier;
import ariba.common.core.Supplier;
import ariba.cxml.SupplierSyncUtil;
import ariba.integration.base.ObjectAdapterInfo;
import ariba.integration.base.TriggerConstants;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.SimpleProcureRecord;
import ariba.procure.core.action.IntegrationPostLoadPLIC;
import ariba.procure.core.mail.Notifications;
import ariba.purchasing.core.Log;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.OrganizationID;
import ariba.user.core.User;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
    @aribaapi private
*/
public class IntegrationPostLoadRequisition extends IntegrationPostLoadPLIC
{
    public static final String ClassName =
        "ariba.purchasing.core.action.IntegrationPostLoadRequisition";

    public static final String RequisitionImportErrorEmail =
        "RequisitionImportErrorEmail";

    private static final String StringTable = "ariba.procure.server";
    private static final String RequisitionImportError_Header
        = "RequisitionImportError_Header";
    private static final String RequisitionImportError_NoLineItems
        = "RequisitionImportError_NoLineItems";
    private static final String RequisitionImportError_MissingFields
        = "RequisitionImportError_MissingFields";
    private static final String RequisitionImportLineItems = "LineItems";
    private static final String RequisitionImportDeliverTo = "DeliverTo";

    protected static final String SubmitParam = "Submit";

    private static final String[] requiredParameterNames = { SubmitParam };

        // parameter type info
    private static final ValueInfo[] parameterInfo = {
        new ValueInfo(SubmitParam, IsScalar, BooleanType)
    };

        // allowed types for the value
    private static final ValueInfo valueInfo =
        new ValueInfo(IsScalar);

    private static int level1Indent  = 18;
    private static int level2Indent  = 46;
    private static int lineLength    = 80;
    private ObjectAdapterInfo log = null;

    private static final BigDecimal DefaultPrice = new BigDecimal("-999999999");

    public void fire (ValueSource object, PropertyTable params)
    {
        Assert.that(object instanceof Requisition,
                    "Tried to call %s without a requisition clusterRoot",
                    ClassName);

        BaseSession session = Base.getSession();

        Requisition req = (Requisition)object;

            // set the requisition param for later use
        Map userInfo = (Map)
            params.getPropertyForKey(TriggerConstants.UserInfo);
        userInfo.put("Requisition", req);
        Log.contract.debug("USER INFO IN POST LOAD TRIGGER %s", userInfo);
        Map reqMap = MapUtil.immutableMap(
            (Map)params.getPropertyForKey(TriggerConstants.ObjectData));
        log = (ObjectAdapterInfo)params.getPropertyForKey(
            TriggerConstants.ActionError);

        /*
            S. Sato - Migrated 8.2.2 Core Modification
            This section should be commented for JETS requisition as we don't
            want the system to update PR number for imported reqs.

            // reset unique name to be set by system, cannot allow
            // user generated uniquenames.
        Map objectParams = (Map)params.getPropertyForKey(
            TriggerConstants.TriggerParams);
        if (objectParams != null) {
            String uniqueName =
                (String)objectParams.get(Requisition.KeyUniqueName);
            if (!StringUtil.nullOrEmptyString(uniqueName) &&
                !StringUtil.nullOrEmptyString(req.getUniqueName()) &&
                !uniqueName.equals(req.getUniqueName())) {
                req.setUniqueName(uniqueName);
            }
        }
        */

            // check if this is an update to an existing object.
            // if so then don't change the preparer information.
        Boolean update = (Boolean)params.getPropertyForKey(
            TriggerConstants.ObjectUpdated);
        boolean isUpdate = update.booleanValue();
        if (!isUpdate) {
            updatePreparer(req, userInfo,reqMap, StringTable, log);
        }
        User preparer = req.getPreparer();

            // we must set the effective user in order to get through the
            // submittion process.
        session.setEffectiveUser(preparer.getBaseId());
            // XXX Chak: Fix this in the next label, to not to set the partition
            // and to verify why the 'usePartition' variable is needed
            // in one of the variantized RequisitionExt.aml files.
        session.setPartition(req.getPartition());


            // Save the requisition before attempting to validate the
            // requisition so the clusterRoot on the lineItems is set
        req.save();

        updateLineItems(req);

        boolean submitFlag = params.booleanPropertyForKey(SubmitParam);
        if (submitFlag == true) {
            Log.ordering.debug("Submit param = true");
        }
        else {
            Log.ordering.debug("Submit param = false");
        }
        if (StringUtil.nullOrEmptyString(req.getOriginatingSystem())) {
            req.setOriginatingSystem(Requisition.OrigSystemExternalforImportedReq);
        }
        new SimpleRecord(req,
                         User.getCustomerSupportAdmin(),
                         null,
                         SimpleProcureRecord.Import);
        if (req.getAdapterSource().toLowerCase().endsWith(".csv") &&
                (validate(req) == true) && (submitFlag == true)) {
            boolean defaultRecordFlag = true;

           req.submit(null, defaultRecordFlag);
        }
        session.setEffectiveUser(null);
        req.updateLineItemsAfterImport();
        req.save();
    }

    private void updateLineItems (Requisition req)
    {
        List lineItems = req.getLineItems();
        int size = lineItems.size();

        for (int i = 0; i < size; i++) {
            ReqLineItem li = (ReqLineItem)lineItems.get(i);
                // When loading from the IntegrationAPI the amount
                // does not get set because
                // LineItemProductDescription.setPrice() never gets
                // called.  Therefore, we need to update the LineItem
                // to calculate the amount before saving the object.
            li.updateLineItem(req);
            if (li.getSupplier() == null) {
                updateSupplierInfoFromSupplierIDs(req, li);
            }

            // If no price was present in the imported file, we set the price
            // to null so it will be defaulted from the catalog.
            if (li.getDescription().getPrice().getAmount().equals(DefaultPrice)) {
                li.getDescription().setPrice(null);
            }

            updateCatalogInfo(li);

            // if this is a non-catalog item and there was no price specified, the price
            // will still be null. We set the price back to zero (the old default).
            if (li.getDescription().getPrice() == null) {
                li.getDescription().setPrice(ApprovableUtil.createZeroMoneyObject(req.getPartition()));
            }

            //
            // If the CommonCommodityCode was invalid, it will be set to null by the
            // integration. However, we want to default to the code derived from the
            // PartitionedCommodityCode. Since this is done by
            // ProcureLineItem.setCommodityCode(), we call it again with the same code.

            if (li.getCommodityCode() != null &&
                    li.getDescription().getCommonCommodityCode() == null) {
                li.setCommodityCode(li.getCommodityCode());
            }


            updateCommodityInfo(li,
                                StringTable,
                                log);

            if (li.getUploadedShipTo() != null) {
                li.setShipTo(li.getUploadedShipTo());
            }

                // update accountings from mapping of commodity code
            updateCEMEFromCCC(li);

            if (!StringUtil.nullOrEmptyOrBlankString(li.getImportedDeliverToStaging())) {
                li.setDeliverTo(li.getImportedDeliverToStaging());
            }

            if (li.getImportedAccountingsStaging() != null &&
                !li.getImportedAccountingsStaging().getAllSplitAccountings().isEmpty()) {
                updateSplitAccounting(li);
            }
        }
    }


    protected void updateSplitAccounting (ReqLineItem li)
    {
        li.getImportedAccountingsStaging().updateSplits(li.getAmount(),li.getQuantity());
        li.setAccountings(li.getImportedAccountingsStaging());
        li.setImportedAccountingsStaging(null);
    }

    /**
        Update accounting fields of the line item from the CommodityCode map
    */
    private void updateCEMEFromCCC (ProcureLineItem lineItem)
    {
            // Now that all fields in the req has filled out, refire the trigger of
            // CommodityCode change to fire off SetCEMEFromCCC action
        LineItemProductDescription lipd = lineItem.getDescription();
        lipd.fireTriggersForDottedFieldChange(
                LineItemProductDescription.KeyCommonCommodityCode);
    }

    private void updateSupplierInfoFromSupplierIDs (Requisition req,
                                                    ReqLineItem li)
    {
        List supplierIDs =  li.getSupplierIDs();

        OrganizationID supplierID = getSupplierInfo(supplierIDs,
                                                    StringTable,
                                                    log);

        if (supplierID == null) {
            li.setSupplier(null);
            li.setSupplierLocation(null);
            return;
        }

        CommonSupplier commonSupplier =
            CommonSupplier.lookupFirst(supplierID);

        if (commonSupplier == null) {
            Log.requisitioning.debug("Common Supplier (%s) is unknown for %s",
                                     supplierID.getCacheKey(),
                                     req.getUniqueName());

            li.setSupplier(null);
            li.setSupplierLocation(null);
            SupplierSyncUtil.registerUnknownCommonSupplier(
                supplierID,
                req,
                li,
                null,
                    // XXX : Add url info here.
                null,
                true);
            return;
        }

        Log.requisitioning.debug("Found CommonSupplier %s for %s",
                                 commonSupplier,
                                 req.getUniqueName());

            // then make sure the supplier on partition is valid
        Supplier supplier =
            commonSupplier.findSupplier(supplierID, req.getPartition());
        if (supplier == null) {
            Log.requisitioning.debug(
                "Supplier (%s) on Partition (%s) is unknwon for %s",
                supplierID.getCacheKey(),
                req.getPartition().getLabel(),
                req.getUniqueName());

            li.setSupplier(null);
            li.setSupplierLocation(null);

            SupplierSyncUtil.registerUnknownSupplier(
                supplierID,
                commonSupplier,
                req,
                li,
                null,
                    // XXX : Add URL for supplier sync
                null,
                true);
            return;
        }

        Log.requisitioning.debug("Found Partition Supplier %s for %s",
                                 supplier,
                                 req.getUniqueName());
            // then set the supplier
        li.setSupplier(supplier);
        li.setSupplierLocation(supplier.getBestLocation());
    }

    private boolean validate (Requisition req)
    {
        ResourceService rs = ResourceService.getService();
        Map hs;

        /*
            Validate in all possible ways !

            1. See if the requisition has valid number of line items
            2. See if the checkSubmit() returns any errors.
            3. See if Invalid Fields is set to true.
        */

            // Start with the assumption, that nothing is validated,
            // and return true, only when everything is validated.
        boolean nullVectorCheckFlag      = false;
        boolean submitHookCheckFlag      = false;
        boolean validateFieldsCheckFlag  = false;

        if (ListUtil.nullOrEmptyList(req.getLineItems())) {

                // See if the requisition has some line items, if not,
                // set it to composing state and send a message! informing
                // that fact to the submitter.

            String errHeader =
                rs.getLocalizedString(StringTable,
                                      RequisitionImportError_Header,
                                      req.getPreparer().getLocale());

            String errNoLineItemMsg =
                rs.getLocalizedString(StringTable,
                                      RequisitionImportError_NoLineItems,
                                      req.getPreparer().getLocale());
                // Fill with proper context parameters !!
            Log.ordering.warning(4274, ClassName, req);

            Notifications.sendMail(
                ListUtil.list(req.getPreparer().getBaseId()),
                req,
                Notifications.RequisitionImportError,
                errHeader,
                errNoLineItemMsg,
                "Content/procure/notifications/requisition-import-error.htm");

        }
        else {
            nullVectorCheckFlag = true;
        }

        List results = req.checkSubmit();
        int code = 0;

        if (results != null) {

            String errHeader =
                rs.getLocalizedString(StringTable,
                                      RequisitionImportError_Header,
                                      req.getPreparer().getLocale());

            code = ((Integer)ListUtil.firstElement(results)).intValue();

            if (code < 0) {
                String error = (String)results.get(1);
                Log.ordering.warning(4291, ClassName, error, req);

                Notifications.sendMail(
                    ListUtil.list(req.getPreparer().getBaseId()),
                    req,
                    Notifications.RequisitionImportError,
                    errHeader,
                    error,
                    "Content/procure/notifications/requisition-import-error.htm");

            }
            else if (code > 0) {
                String warning = (String)results.get(1);
                Log.ordering.warning(4292, ClassName, warning, req);

                Notifications.sendMail(
                    ListUtil.list(req.getPreparer().getBaseId()),
                    req,
                    Notifications.RequisitionImportError,
                    errHeader,
                    warning,
                    "Content/procure/notifications/requisition-import-error.htm");

            }
            else {
                submitHookCheckFlag = true;
            }
        }
        else {
            submitHookCheckFlag = true;
        }

            // the && condition was added to avoid the error message being
            // created with  empty error fields when the actual error was
            // missing line items!

        if ((hs = req.getInvalidFields(null, req)) != null &&
            nullVectorCheckFlag == true ) {
                // Has some errors, create the message, with all the invalid values.
            String errHeader =
                rs.getLocalizedString(StringTable,
                                      RequisitionImportError_Header,
                                      req.getPreparer().getLocale());

            String errMsg =
                rs.getLocalizedString(StringTable,
                                      RequisitionImportError_MissingFields,
                                      req.getPreparer().getLocale());

                // Fill with proper context parameters !!
                // Check where to initiate this permissions list ?
                // We do not need to send to the whole group, about
                // But the guy, who is importing the requisition.

            String hsoutput = invalidFieldOutput(hs, true, false);

            String errDetailHeader = "Requisition :" + req + "\n\nLineItem # \t" +
                "Error Fields \n\n";

            String formattedFields = formatOutput(hsoutput);

            String fieldsInError = errHeader + errMsg + errDetailHeader + formattedFields;

            Log.ordering.warning(4275, ClassName , fieldsInError);

            Notifications.sendMail(
                ListUtil.list(req.getPreparer().getBaseId()),
                req,
                Notifications.RequisitionImportError,
                errHeader,
                errMsg + errDetailHeader + hsoutput,
                "Content/procure/notifications/requisition-import-error.htm");

                // Keep the requisition is the composing state!!
                // which means, do not call submit.
        }
        else {
            validateFieldsCheckFlag = true;
        }

        if (nullVectorCheckFlag == false
            || submitHookCheckFlag == false
            || validateFieldsCheckFlag == false) {
            return false;
        }
        return true;
    }

/**
    This method is very fine tuned to print the hash table to
    a user friendly output for Requisition import report.

    @aribaapi private
*/
private String invalidFieldOutput (Map hs,
                                       boolean toplevel,
                                       boolean lineItemLevel)
    {
        String retVal = "";

        List keysVector = MapUtil.keysList(hs);
        List elemsVector = MapUtil.elementsList(hs);

        if (keysVector.size() != elemsVector.size()) {
            // Some thing, that should not happen,
            // just default to the hash table to
            // give the string form of its values.
            retVal = hs.toString();
        }
        else {
            int len = keysVector.size();

            Object p;

            for (int i = 0; i < len; i++) {

                boolean lineItemsEntry = false;

                    // Add the Key: If possible, format the key.

                p = keysVector.get(i);
                if (p instanceof String) {
                       // Do not print a tab, if the key says "Line Items"

                    if (((String) p).compareTo("LineItems") != 0) {
                        retVal += (String)p.toString();
                        if (toplevel == true || lineItemLevel == true) {
                                // Indent to the next field level;
                            retVal += "\t\t";
                        }

                        if (toplevel == true || lineItemLevel == false) {
                            retVal += ".";
                        }
                    }
                    else {
                        lineItemsEntry = true;
                    }

                }

                    // Can a key anytime be a hash table?
                if (p instanceof Map) {
                   retVal += invalidFieldOutput((Map)p,
                                                false, lineItemsEntry);
                }

                    // Add the value
                p = elemsVector.get(i);

                if (p instanceof String) {
                    retVal += (String)p.toString();
                }
                if (p instanceof Map) {
                    retVal += invalidFieldOutput((Map)p, false, lineItemsEntry);
                }

                retVal += "\n";

            }

        }

        return retVal;
    }

    /**
        A method, which formats the error description information String
        to improve readability.

        @aribaapi private
    */


    private String formatOutput ( String val)
    {

        try {
            String retval = "";
            StringTokenizer st = new StringTokenizer(val, "\n");
            while (st.hasMoreTokens()) {
                String p = st.nextToken();

                if ( p != " " ) {
                    int strlen = p.length();
                    char endChar = p.charAt(strlen -1);

                    if (endChar == '.') {
                        p = p.substring(0 , strlen -1);
                    }

                    String substr = p.substring(0, 1);
                    char ch = substr.charAt(0);

                    if (Character.isDigit(substr.charAt(0))) {
                        for (int i = 0; i <  80; i++) {
                            retval += "-";
                        }

                        retval += "\n";
                        retval +=p;
                        retval += "\n\n";
                    }
                    else {
                        retval += "\t\t";
                        retval +=p;
                        retval += "\n\n";
                    }
                }
            }
            return retval;
        }
        catch (RuntimeException e) {
                // Processing buffer failed ! Just return the
                // original string.
            return val;
        }
    }


    /**
        Return the list of valid value types.
    */
    protected ValueInfo getValueInfo ()
    {
        return valueInfo;
    }

    /**
        Return the parameter names.
    */
    protected ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    /**
        Return the required parameter names.
    */
    protected String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }
}

