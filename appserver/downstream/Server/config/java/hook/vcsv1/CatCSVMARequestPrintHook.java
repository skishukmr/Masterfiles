/*
 * 17/01/2014  IBM Parita Shah	SpringRelease_RSD 111(FDD4.9,4.10/TDD1.9,1.10) New file created for MSC Tax Gaps Correct Legal Entity
 * -------------------------------------------------------------------------------------------------------------------------------------------------
 *
 */

package config.java.hook.vcsv1;

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


public class CatCSVMARequestPrintHook implements PrintApprovableHook {

    private static final String THISCLASS = "CatCSVMARequestPrintHook";

    public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail) {

        Log.customer.debug("%s *** 1. PrintHook RUN!", THISCLASS);

        if((approvable instanceof ContractRequest)||(approvable instanceof Contract)) {
        	Log.customer.debug("Approvable Master Agreement or Master Agreement");
        	ApprovableType aType = ApprovableType.getApprovableType(approvable.getTypeName(), approvable.getPartition());
        	boolean showLineItems = aType.getShowApprovableDetailsInEmail();
        	Log.customer.debug("CatMFGMARequestPrintHook *** printForEmail: " + printForEmail);
        	Log.customer.debug("CatMFGMARequestPrintHook *** showLineItems: " + showLineItems);

        	if (!printForEmail) {
            		CatCSVMARequest_Print crp = new CatCSVMARequest_Print();
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


    public CatCSVMARequestPrintHook() {
        super();
    }

}
