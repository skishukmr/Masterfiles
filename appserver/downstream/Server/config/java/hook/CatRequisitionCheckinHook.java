/*
 * Created by KS on Dec 22, 2004
 * Modified by RR on Feb 21, 2005 - Very minor modification for Emergency Buy title
 * Madhavan Chari- 07-01-2008  Issue 724 Modified the EmergencyText to Critical Asset Down instead of Emergency Buy.
 */
package config.java.hook;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseVector;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

/*   This hook performs accounting validation on each split accounting object for each line item AND
 *   LeadTime & NeedBy validation and returns error messages for all invalid splits specified by line.
 *   If the split accounting is valid,no message is returned.
*/

public class CatRequisitionCheckinHook implements ApprovableHook {

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String SingleSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Single");
    private static final String MultiSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Multiple");
    private static final String NeedByError = ResourceService.getString("cat.vcsv1","NeedByLeadTimeError");
    private static final String classname = "CatRequisitionCheckinHook";
    private static final String EmergencyText = ResourceService.getString("cat.java.common","EmergencyBuyTitleText");
    private static final String ReturnCode_Acct = ResourceService.getString("cat.vcsv1","RequisitionCheckinHookReturnCode_Accounting");
    private static final String ReturnCode_NeedBy = ResourceService.getString("cat.vcsv1","RequisitionCheckinHookReturnCode_NeedBy");

	public List run(Approvable approvable) {

        FastStringBuffer totalMsg = new FastStringBuffer ();
        String formatLineError = null;
        boolean hasBadAccounting = false;
        boolean hasBadNeedBy = false;
        int returncode = 0;

        if (approvable instanceof Requisition) {
        	Requisition r = (Requisition)approvable;
        	Boolean stat = (Boolean)r.getFieldValue("EmergencyBuy");
        	if (stat != null) {
        		String title = r.getName();
        		if (stat.booleanValue() && !title.startsWith(EmergencyText)){
        			title = EmergencyText + title;
        			r.setFieldValue("Name",title);
        		}
        		else if (!stat.booleanValue() && title.startsWith(EmergencyText)) {
        			title = title.substring(EmergencyText.length());
        			r.setFieldValue("Name",title);
        		}
        	}
        	BaseVector lines = r.getLineItems();
        	for (int i = 0; i < lines.size(); i++) {
      	      	int errorLine = i + 1;
      	      	ReqLineItem rli = (ReqLineItem)lines.get(i);
      	      	String resultAcct = CatRequisitionSubmitHook.checkAccounting(rli);
      	      	String resultNeedBy = checkNeedByDate(rli);
      	      	if (!resultAcct.equals("0")) {
      	      		hasBadAccounting = true;
      	      		formatLineError = Fmt.S(resultAcct, errorLine);
    		     	totalMsg.append(formatLineError);
    		      	Log.customer.debug("%s *** Accounting Error Msg: %s", classname, formatLineError);
    		      	if (rli.getIsAdHoc() && !resultNeedBy.equals("0")) {
    		      		hasBadNeedBy = true;
    		      		totalMsg.append(resultNeedBy);
    		          	Log.customer.debug("%s *** NeedBy Error Msg: %s", classname, resultNeedBy);
    		      	}
      	      	} else if (rli.getIsAdHoc() && !resultNeedBy.equals("0")) {
      	      		hasBadNeedBy = true;
      		     	String soloError = " Line %s:" + resultNeedBy;
      		     	formatLineError = Fmt.S(soloError, errorLine);
      		     	totalMsg.append(formatLineError);
		          	Log.customer.debug("%s *** NeedBy Error Msg: %s", classname, formatLineError);
      	      	}
      	      	Log.customer.debug("CatReqCheckinHook *** Finished Line#: " + errorLine);
      	      	Log.customer.debug("%s *** CUMULATIVE Msg: %s", classname, totalMsg.toString());
        	}
  	      	Log.customer.debug("CatReqCheckinHook *** Finished ALL lines and hasBadAccounting? " + hasBadAccounting);
  	      	if (hasBadAccounting) {
		      	if (ReturnCode_Acct.equals("1") || ReturnCode_Acct.equals("-1"))
		      		returncode = Integer.parseInt(ReturnCode_Acct);
  	      	}
  	      	if (hasBadNeedBy) {
		      	if (returncode > -1 && (ReturnCode_NeedBy.equals("1") || ReturnCode_NeedBy.equals("-1")))
		      		returncode = Integer.parseInt(ReturnCode_NeedBy);
  	      	}
	      	Log.customer.debug("CatReqCheckinHook *** hook return code: " + returncode);
  	      	return ListUtil.list(Constants.getInteger(returncode), totalMsg.toString());
        }
		return NoErrorResult;
	}

	public CatRequisitionCheckinHook() {
		super();
	}

	protected static String checkNeedByDate (ReqLineItem rli) {

		String lineErrorResult = "0";
		LineItemProductDescription lipd = rli.getDescription();
		if (lipd != null) {
			int leadtime = lipd.getLeadTime();
			if (leadtime > 0) {
				Date expected = Date.getNow();
				Date.addDays(expected, leadtime);
		      	Log.customer.debug("%s *** Expected: %s", classname, expected);
				Date wanted = rli.getNeedBy();
		      	Log.customer.debug("%s *** Wanted: %s", classname, wanted);
				if (wanted != null && !wanted.after(expected)) {
					lineErrorResult = Fmt.S(NeedByError, String.valueOf(leadtime));
				}
			}
		}
		return lineErrorResult;
	}

}
