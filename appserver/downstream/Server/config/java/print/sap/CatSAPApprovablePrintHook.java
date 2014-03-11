/* Created by KS/Chandra on Apr 17, 2006
 * -------------------------------------------------------------------------------
 * Required to call special print methods (for precision control and Material/AC ordered printing)
 */
package config.java.print.sap;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import config.java.print.sap.CatSAPRequisition_Print;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.PrintApprovableHook;
import ariba.approvable.core.ApprovableType;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.Requisition;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;


public class CatSAPApprovablePrintHook implements PrintApprovableHook {

    private static final String THISCLASS = "CatSAPApprovablePrintHook";

    public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail) {

        Log.customer.debug("%s *** 1. PrintHook RUN!", THISCLASS);
        if (!(approvable instanceof ProcureLineItemCollection)) {
            return ListUtil.list(Constants.getInteger(1), "Error: not PLIC - not printing!");
        }
        ApprovableType aType = ApprovableType.getApprovableType(approvable.getTypeName(), approvable.getPartition());
        boolean showLineItems = aType.getShowApprovableDetailsInEmail();
 //       Log.customer.debug("CatSAPApprovablePrintHook *** printForEmail: " + printForEmail);
 //       Log.customer.debug("CatSAPApprovablePrintHook *** showLineItems: " + showLineItems);
        if (!printForEmail) {

            ClusterRoot effectiveUser = Base.getSession().getEffectiveUser();
            if (effectiveUser != null)
            {
                ariba.user.core.User newUser = (ariba.user.core.User)effectiveUser;
                locale = newUser.getLocale();
            }
            if (approvable instanceof Requisition) {
	            CatSAPRequisition_Print crp = new CatSAPRequisition_Print();
	        	crp.printHTML((Requisition) approvable, out, null, true, locale);
            }
            else if (approvable instanceof PurchaseOrder) {
                CatSAPPurchaseOrder_Print cpop = new CatSAPPurchaseOrder_Print();
	        	cpop.printHTML((PurchaseOrder) approvable, out, null, true, locale);
            }

            else if (approvable instanceof Invoice) {
				CatSAPInvoice_Print cip = new CatSAPInvoice_Print();
				cip.printHTML((Invoice) approvable, out, null, true, locale);
            }
            else if (approvable instanceof InvoiceReconciliation) {
				CatSAPInvoiceReconciliation_Print cirp = new CatSAPInvoiceReconciliation_Print();
				cirp.printHTML((InvoiceReconciliation) approvable, out, null, true, locale);
            }


        }
        return ListUtil.list(Constants.getInteger(0));
    }


    public CatSAPApprovablePrintHook() {
        super();
    }

}
