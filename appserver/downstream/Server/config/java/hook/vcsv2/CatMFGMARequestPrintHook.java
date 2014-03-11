/*
 * 05-10-2007     Amit Kumar    Overridden OOB method to exclude company name Caterpillar Inc.and
 *								instead print Perkins Engines Company on contract print for mfg1.
 * -------------------------------------------------------------------------------------------
 *
 */

package config.java.hook.vcsv2;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableType;
import ariba.approvable.core.PrintApprovableHook;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractRequest;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
//import config.java.contract.CatProcureLineItemMA_Print;


public class CatMFGMARequestPrintHook implements PrintApprovableHook {

    private static final String THISCLASS = "CatMFGMARequestPrintHook";

    public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail) {

        Log.customer.debug("%s *** 1. PrintHook RUN!", THISCLASS);

        if((approvable instanceof ContractRequest)||(approvable instanceof Contract)) {
        	Log.customer.debug("Approvable Master Agreement or Master Agreement");
        	ApprovableType aType = ApprovableType.getApprovableType(approvable.getTypeName(), approvable.getPartition());
        	boolean showLineItems = aType.getShowApprovableDetailsInEmail();
        	Log.customer.debug("CatMFGMARequestPrintHook *** printForEmail: " + printForEmail);
        	Log.customer.debug("CatMFGMARequestPrintHook *** showLineItems: " + showLineItems);

        	if (!printForEmail) {
            		CatMFGMARequest_Print crp = new CatMFGMARequest_Print();
					Log.customer.debug("Master Agreement Request Print or Master Agreement Print");
        			if(approvable instanceof Contract) {
        				Log.customer.debug(" Master Agreement Print");
        				crp.printHTML((Contract)approvable, out, null, true, locale);
					}
					else {
						Log.customer.debug("Master Agreement Request Print");
						crp.printHTML((ContractRequest)approvable, out, null, true, locale);
					}
          	}
		}
		else {
			Log.customer.debug("Approvable not a MA or MAR");
			return ListUtil.list(Constants.getInteger(1), "Error: not a ContractRequest or Contract - not printing!");
        }

        return ListUtil.list(Constants.getInteger(0));
    }


    public CatMFGMARequestPrintHook() {
        super();
    }

}
