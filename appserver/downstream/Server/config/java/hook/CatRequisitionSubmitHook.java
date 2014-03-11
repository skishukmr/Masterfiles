/*
 * Created by KS on Dec 22, 2004
 * Madhavan Chari- 07-01-2008  Issue 724 Modified the EmergencyText to Critical Asset Down instead of Emergency Buy.
 */
package config.java.hook;

import java.util.HashMap;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseVector;
import ariba.base.fields.ConditionResult;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.condition.NeedByDate;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.action.CatValidateAccountingString;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;
import config.java.integration.ws.AccountValidator;

/*   This hook performs accounting validation on each split accounting object for each line item and
 *   returns error messages for all invalid splits specified by line. If the split accounting is valid,
 *   no message is returned.
*/

public class CatRequisitionSubmitHook implements ApprovableHook {

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
    private static final String SingleSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Single");
    private static final String MultiSplitError = ResourceService.getString("cat.vcsv1","AccountDistributionError_Multiple");
    private static final String classname = "CatReqSubmitHook";
    private static final String NeedByFlag = ResourceService.getString("cat.vcsv1","NeedByFlag");
    private static final String EmergencyText = ResourceService.getString("cat.java.common","EmergencyBuyTitleText");
    protected static final String returnCode = ResourceService.getString("cat.vcsv1","RequisitionSubmitHookReturnCode");

	public List run(Approvable approvable) {

        FastStringBuffer totalMsg = new FastStringBuffer ();
        boolean hasErrors = false;
        String error = "";

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
      	        boolean hasBadAcctng = false;
      	      	ReqLineItem rli = (ReqLineItem)lines.get(i);
      	      	String lineresult = checkAccounting(rli);
      	      	if (!lineresult.equals("0")) {
    		     	String formatLineError = Fmt.S(lineresult, errorLine);
    		     	hasErrors = true;
    		     	hasBadAcctng = true;
    		     	totalMsg.append(formatLineError);
    		      	Log.customer.debug("%s *** Line Error Msg: %s", classname, formatLineError);
      	      	}
      	      	Date needby = rli.getNeedBy();
      	      	NeedByDate nbd = new NeedByDate();
      	      	PropertyTable ptable = new PropertyTable(getPropertyMap(r, rli));
      	      	Log.customer.debug("%s *** ptable: %s", classname, ptable);
      	      	ConditionResult cr = nbd.evaluateAndExplain(needby, ptable);
      	      	Log.customer.debug("%s *** CR: %s", classname, cr);
      	      	if (cr != null) {
      	      		hasErrors = true;
      	      		String crmsg = null;
      	      		if (cr.getWarningCount() > 0)
      	      			crmsg = cr.getFirstWarning();
      	      		if (cr.getErrorCount() > 0) {
      	      			crmsg = cr.getFirstError();
  //    	      		if (crmsg.indexOf("Value") > -1)
  //    	      			crmsg.replaceFirst("Value","Need-By date");
      	      		}
      	      		Log.customer.debug("%s *** CR Message: %s", classname, crmsg);
      	      		if (crmsg != null) {
      	      			crmsg = NeedByFlag + crmsg;
      	      			if (!hasBadAcctng)
      	      				crmsg = Fmt.S(" Line %s: ", String.valueOf(errorLine)) + crmsg;
      	      			totalMsg.append(crmsg);
      	      		}
      	      	}
      	      	Log.customer.debug("CatReqSubmitHook *** Finished Line#: " + errorLine);
        	}
  	      	Log.customer.debug("CatReqSubmitHook *** Finished ALL lines and hasErrors? " + hasErrors);
  	      	if (hasErrors) {
		      	int code = 1;
		      	if (returnCode.equals("1") || returnCode.equals("-1") || returnCode.equals("0"))
		      		code = Integer.parseInt(returnCode);
  	      		Log.customer.debug("%s *** Total Error Msg: %s", classname, totalMsg.toString());
  	      		return ListUtil.list(Constants.getInteger(code), totalMsg.toString());
  	      	}
        }
		return NoErrorResult;
	}

	public CatRequisitionSubmitHook() {
		super();
	}

	protected static String checkAccounting(ReqLineItem rli) {

		String lineErrorResult = "0";
		int lineErrors = 0;
      	FastStringBuffer lineMsg = new FastStringBuffer();
      	SplitAccountingCollection sac = rli.getAccountings();
      	if (sac != null) {
      		BaseVector splits = sac.getSplitAccountings();
  	       	for (int j = 0; j < splits.size(); j++) {
      			int splitErrors = 0;
      			int errorSplit = j + 1;
      			FastStringBuffer splitMsg = new FastStringBuffer();
      			SplitAccounting sa = (SplitAccounting)splits.get(j);
      			if (sa != null) {
      				CatAccountingCollector cac = CatValidateAccountingString.getCatAccounting(sa);
      				if (cac != null) {
						CatAccountingValidator response = null;
						try {
							response = AccountValidator.validateAccount(cac);
							Log.customer.debug("%s *** CatAccountingValidator: %s", classname, response);
							if (response != null) {
								Log.customer.debug("%s *** ResultCode: %s", classname, response.getResultCode());
								Log.customer.debug("%s *** Message: %s", classname, response.getMessage());
								if (!response.getResultCode().equals("00")) {
									splitErrors += 1;
									lineErrors += 1;
						       		splitMsg.append(response.getMessage() + ". ");
						       		Log.customer.debug("CatReqSubmitHook *** Split#: " +
						       				errorSplit + " Error: " + response.getMessage());
								}
							}
						}
						catch (Exception e) {
							Log.customer.debug("%s *** Exception: %s", classname, e);
						}
      				}
      			}
      			if (splitErrors > 0) {
      				String splitErrorResult = null;
      				String formatSplitError = null;
// Make "if (splits.size() > 1)" to leave out Acct Distribution split # in message
      				if (splits.size() > 0) {
      					splitErrorResult = MultiSplitError + splitMsg.toString();
      					formatSplitError = Fmt.S(splitErrorResult, errorSplit);
      				} else {
      					formatSplitError = SingleSplitError + splitMsg.toString();
      				}
      				lineMsg.append(formatSplitError);
      			}
      		}
      	}
      	Log.customer.debug("CatReqSubmitHook *** LineErrors: " + lineErrors);
     	if (lineErrors > 0) {
	     	lineErrorResult = " Line %s:" + lineMsg.toString();
	      	Log.customer.debug("%s *** Line Error Msg: %s", classname, lineErrorResult);
     	}
		return lineErrorResult;
	}

	protected static HashMap getPropertyMap(Requisition req, ReqLineItem reqline)
	{
		HashMap map = new HashMap();
			map.put("ProcureLineItemCollection",req);
			map.put("ProcureLineItem",reqline);
			map.put("NotPastDate","true");
			map.put("AllowNullDate","true");
		return map;
	}

}
