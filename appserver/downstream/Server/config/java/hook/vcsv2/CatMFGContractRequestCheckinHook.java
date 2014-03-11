/*
 * Created by KS on May 6, 2005
 * --------------------------------------------------------------
 * Used to validate contract request accounting
 */
package config.java.hook.vcsv2;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.contract.core.ContractRequest;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatMFGContractRequestCheckinHook implements ApprovableHook {

    private static final String THISCLASS = "CatMFGContractRequestCheckinHook";
    private static final List NOERROR = ListUtil.list(Constants.getInteger(0));
    private static boolean debug = CatConstants.DEBUG;


	public List run(Approvable approvable) {

        if (approvable instanceof ContractRequest) {
            Log.customer.debug("Calling SubmitHook from Checkinhook");
            CatMFGContractRequestSubmitHook submithook = new CatMFGContractRequestSubmitHook();
            return submithook.run(approvable);
        }
        return NOERROR;
	}


	public CatMFGContractRequestCheckinHook() {
		super();
	}


}
