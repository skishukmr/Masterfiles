package config.java.action.vcsv1;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.Invoice;
import ariba.purchasing.core.POLineItem;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
    AUL : Changed MasterAgreement to Contract

    AUL: Modification Log: ssato - Ariba, Inc.
         - Made formatting and indentation corrections
         - Added logic to set settlement code from order directly
           associated with the invoice.
*/
public class SetSettlementCodeFromOrderSelected extends Action
{
    public void fire (ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {

            // Invoice
        if (valuesource instanceof Invoice) {

                // get settlement code from the first line item of the associated
                // master agreement
            Log.customer.debug(
                    "%s ::: Setting settlement code on an invoice in case of UI entry",
                    "SetSettlementCodeFromOrderSelected");
            Invoice invoice = (Invoice) valuesource;

                // Purchase Orders
            List orders = invoice.getSelectedOrders();
            if (orders != null && !orders.isEmpty()) {
                Log.customer.debug("Setting settlement code from orders");
                setSettlementCodeFromOrders(invoice, orders);
                return;
            }

                // Master Agreement
            Contract masteragreement = invoice.getSelectedMasterAgreement();
            Log.customer.debug(
                    "%s ::: Invoice being worked on is %s",
                    "SetSettlementCodeFromOrderSelected",
                    invoice.getUniqueName());
            Log.customer.debug(
                    "%s ::: MasterAgreement selected is %s",
                    "SetSettlementCodeFromOrderSelected",
                    masteragreement.getUniqueName());

            if (masteragreement != null) {

                BaseVector basevector = masteragreement.getLineItems();
                if (basevector != null && !basevector.isEmpty()) {

                    ContractLineItem malineitem = (ContractLineItem) basevector.get(0);
                    if (malineitem != null) {
                        Log.customer.debug(
                                "%s ::: Setting the settlement code (%s) on the " +
                                "Invoice Entry Screen from the MA line item",
                                "SetSettlementCodeFromOrderSelected",
                                malineitem.getFieldValue("SettlementCode").toString());
                        invoice.setDottedFieldValue(
                                "SettlementCode",
                                (ClusterRoot) malineitem.getFieldValue("SettlementCode"));
                    }
                    else {
                        Log.customer.debug(
                                "%s ::: MasterAgreement Line is null",
                                "SetSettlementCodeFromOrderSelected");
                    }
                }
                else {
                    Log.customer.debug(
                            "%s ::: MasterAgreement Lines are empty or null",
                            "SetSettlementCodeFromOrderSelected");
                }
            }
        }

            // Invoice eForm (This is not needed and may be removed in 9r)
        else {

               // get settlement code from the first line item of the associated
                // purchase order
            Log.customer.debug(
                    "%s ::: Setting settlement code on an invoice in case of EForm entry",
                    "SetSettlementCodeFromOrderSelected");
            Approvable approvable = (Approvable) valuesource;
            List list = (List) approvable.getFieldValue("Orders");
            if (ListUtil.nullOrEmptyList(list)) {

                BaseVector basevector1 = (BaseVector) approvable.getFieldValue("LineItems");
                if(basevector1 == null || basevector1.isEmpty()) {
                    Log.customer.debug(
                            "%s ::: Returning without setting as the orders list size is zero",
                            "SetSettlementCodeFromOrderSelected");
                }
                else {
                    int i = 0;
                    do {
                        if (i >= basevector1.size()) {
                            break;
                        }
                        POLineItem polineitem =
                            (POLineItem) ((BaseObject) basevector1.get(i)).getFieldValue("OrderLineItem");

                        if (polineitem != null) {
                            Log.customer.debug(
                                    "%s ::: Setting the settlement code (%s) on the Invoice eform " +
                                    "Screen from the PO line item",
                                    "SetSettlementCodeFromOrderSelected",
                                    polineitem.getFieldValue("SettlementCode").toString());
                            approvable.setDottedFieldValue(
                                    "SettlementCode",
                                    (ClusterRoot) polineitem.getFieldValue("SettlementCode"));
                            break;
                        }
                        i++;
                    }
                    while(true);
                }
            }
            else {
                if(list.size() > 1) {
                    Log.customer.debug(
                            "%s ::: Returning without setting as the orders list " +
                            "has multiple orders", "SetSettlementCodeFromOrderSelected");
                    return;
                }
                List list1 = (List) approvable.getFieldValue("LineItems");
                int j = 0;
                do {
                    if (j >= list1.size()) {
                        break;
                    }
                    POLineItem polineitem1 =
                        (POLineItem) ((BaseObject) list1.get(j)).getFieldValue("OrderLineItem");
                    if (polineitem1 != null) {
                        Log.customer.debug(
                                "%s ::: Setting the settlement code (%s) on the Invoice " +
                                "eform Screen from the PO line item",
                                "SetSettlementCodeFromOrderSelected",
                                polineitem1.getFieldValue("SettlementCode").toString());
                        approvable.setDottedFieldValue(
                                "SettlementCode",
                                (ClusterRoot) polineitem1.getFieldValue("SettlementCode"));
                        break;
                    }
                    j++;
                }
                while (true);
            }
        }
    }

    /**
        Set the settlement code from the order (s) associated with this invoice

        @param invoice The invoice that has the settlement code
        @param orders  The list of orders
    */
    protected void setSettlementCodeFromOrders (Invoice invoice, List orders)
    {
        if (ListUtil.nullOrEmptyList (orders)) {

            BaseVector basevector1 = (BaseVector) invoice.getLineItems();
            if(basevector1 == null || basevector1.isEmpty()) {
                Log.customer.debug(
                        "%s ::: Returning without setting as the orders list size is zero",
                        "SetSettlementCodeFromOrderSelected");
            }
            else {
                int i = 0;
                do {
                    if (i >= basevector1.size()) {
                        break;
                    }
                    POLineItem polineitem =
                        (POLineItem) ((BaseObject) basevector1.get(i)).getFieldValue("OrderLineItem");

                    if (polineitem != null) {
                        Log.customer.debug(
                                "%s ::: Setting the settlement code (%s) on the Invoice eform " +
                                "Screen from the PO line item",
                                "SetSettlementCodeFromOrderSelected",
                                polineitem.getFieldValue("SettlementCode").toString());
                        invoice.setDottedFieldValue(
                                "SettlementCode",
                                (ClusterRoot) polineitem.getFieldValue("SettlementCode"));
                        break;
                    }
                    i++;
                }
                while(true);
            }
        }
        else {
            if(orders.size() > 1) {
                Log.customer.debug(
                        "%s ::: Returning without setting as the orders list " +
                        "has multiple orders", "SetSettlementCodeFromOrderSelected");
                return;
            }
            List list1 = (List) invoice.getLineItems();
            int j = 0;
            do {
                if (j >= list1.size()) {
                    break;
                }
                POLineItem polineitem1 =
                    (POLineItem) ((BaseObject) list1.get(j)).getFieldValue("OrderLineItem");
                if (polineitem1 != null) {
                    Log.customer.debug(
                            "%s ::: Setting the settlement code (%s) on the Invoice " +
                            "eform Screen from the PO line item",
                            "SetSettlementCodeFromOrderSelected",
                            polineitem1.getFieldValue("SettlementCode").toString());
                    invoice.setDottedFieldValue(
                            "SettlementCode",
                            (ClusterRoot) polineitem1.getFieldValue("SettlementCode"));
                    break;
                }
                j++;
            }
            while (true);
        }
    }

    private static final String ClassName = "SetSettlementCodeFromOrderSelected";
}
