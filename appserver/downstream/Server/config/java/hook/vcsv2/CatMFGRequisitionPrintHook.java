/*
 * Created by KS on Jul 18, 2005
 * -------------------------------------------------------------------------------
 * Changes required to remove Caterpillar Inc. company name from Requisition print
 */
package config.java.hook.vcsv2;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.PrintApprovableHook;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;



public class CatMFGRequisitionPrintHook implements PrintApprovableHook {

    private static final String THISCLASS = "CatRequisitionPrintHook";

    public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail) {

        Log.customer.debug("%s *** 1. PrintHook RUN!", THISCLASS);
        if (!(approvable instanceof Requisition)) {
            return ListUtil.list(Constants.getInteger(1), "Error: not a Requisition - not printing!");
        }
        ApprovableType aType = ApprovableType.getApprovableType(approvable.getTypeName(), approvable.getPartition());
        boolean showLineItems = aType.getShowApprovableDetailsInEmail();
        Log.customer.debug("CatRequisitionPrintHook *** printForEmail: " + printForEmail);
        Log.customer.debug("CatRequisitionPrintHook *** showLineItems: " + showLineItems);
        if (!printForEmail) {
            CatMFGRequisition_Print crp = new CatMFGRequisition_Print();
        	crp.printHTML((Requisition) approvable, out, null, true, locale);
        }
        return ListUtil.list(Constants.getInteger(0));
    }


    public CatMFGRequisitionPrintHook() {
        super();
    }

}
