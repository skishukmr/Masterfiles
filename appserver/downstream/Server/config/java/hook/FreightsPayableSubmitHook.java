/*
Changed by: Ashwini        10/02/2010        Issue 1035 :display of MSg from csv

*/

package config.java.hook;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Log;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;

public class FreightsPayableSubmitHook
    implements ApprovableHook


{
	private static final String Msg = ResourceService.getString("cat.java.common","HomePageMsg");
    public List run(Approvable approvable)
    {

        List NoErrorResult = ListUtil.list(Constants.getInteger(1), Msg.toString());
        Log.customer.debug("CatReceiveSubmitHook: 1: " + NoErrorResult.toString());
        return NoErrorResult;
    }

    public FreightsPayableSubmitHook()
    {
    }
}
