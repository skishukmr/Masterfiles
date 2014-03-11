/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	04/15/2006
	Description: 	Submit Hook for the voucher eForm.  Checks for the
					following conditions -
					1)	Misc identified exists on the JET$ project
					2)	Amount invoiced is greater than $0.00
					3)	Fast Pay is not selected for Foreign Currency
					4)	Accounting identified is valid using FS7200 webservice
-------------------------------------------------------------------------------
	Change Author: Ashwini
	Date Created: 05/10/2009
	Description: Added null check for the misc value . (Issue 1002)
******************************************************************************/

package config.java.vouchereform.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.common.core.SplitAccounting;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import cat.cis.fasd.ws.soap.Response;
import config.java.action.vcsv1.CatValidateInvAccountingString;

public class VoucherEformSubmitHook implements ApprovableHook {
	private static final String ClassName = "CatCSVInvoiceReconciliationSubmitHook";
	private static final String ComponentStringTable = "aml.cat.VoucherEForm";
	private static final int ValidationError = -2;
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));
	private static final String FastPayError = ResourceService.getString(ComponentStringTable, "FastPayCurrencyMisMatch");
	private static final String InvAmntError = ResourceService.getString(ComponentStringTable, "InvoiceAmountNotSpecified");
	private static final String InvAmntOutOfRangeError = ResourceService.getString(ComponentStringTable, "InvAmntOutOfRangeError");
	private static final String InvalidAccountingMisc = ResourceService.getString(ComponentStringTable, "InvalidAccountingMisc");

	public List run(Approvable approvable) {
		//if (Log.customer.debugOn) {
			Log.customer.debug("%s ::: Entering the Submit Hook Implementation run method", ClassName);
			Log.customer.debug("%s ::: Looking at VCEF: %s", ClassName, approvable.getUniqueName());
		//}

		Approvable vcef = approvable;
		Boolean fastPay = (Boolean) vcef.getDottedFieldValue("FastPay");
		String sbrtnRtCode = null;
		String sbrtnMessage = null;
		Response response = null;
		String AccountingErrorMsg = "";
		Money invAmount = null;
		Currency cur = null;
		boolean miscValid = false;
		String vcefMisc = (String) vcef.getFieldValue("Misc");

		List reqLineItems = (List) vcef.getFieldValue("ReqLineItems");
		for (int i=0; i<reqLineItems.size(); i++){
			String misc = (String) ((BaseObject)reqLineItems.get(i)).getFieldValue("Misc");
			if ((misc != null) && (vcefMisc != null) ){
			if (misc.equals(vcefMisc)){
				miscValid = true;
			}
			}
		}
		if (!miscValid){
			return ListUtil.list(Constants.getInteger(-1), InvalidAccountingMisc);
		}

		if (vcef.getDottedFieldValue("TotalInvoiced") != null){
			invAmount = (Money) vcef.getDottedFieldValue("TotalInvoiced");
			cur = ((Money)vcef.getDottedFieldValue("TotalInvoiced")).getCurrency();
			if (fastPay != null && cur != null){
				if (fastPay.booleanValue() && !"USD".equals(cur.getUniqueName())){
					return ListUtil.list(Constants.getInteger(-1), FastPayError);
				}
			}
			if (invAmount != null){
				BigDecimal invAmountBD = invAmount.getAmount();
				BigDecimal zeroBD = new BigDecimal("0.00");
				if (zeroBD.compareTo(invAmountBD) >= 0){
					return ListUtil.list(Constants.getInteger(-1), InvAmntOutOfRangeError);
				}
			}
		}
		else{
			return ListUtil.list(Constants.getInteger(-1), InvAmntError);
		}

		SplitAccounting sa = new SplitAccounting(Base.getSession().getPartition());
		sa.setDottedFieldValueWithoutTriggering("AccountingFacility",vcef.getFieldValue("AccountingFacility"));
		sa.setDottedFieldValueWithoutTriggering("Department",vcef.getFieldValue("Department"));
		sa.setDottedFieldValueWithoutTriggering("Division",vcef.getFieldValue("Division"));
		sa.setDottedFieldValueWithoutTriggering("Section",vcef.getFieldValue("Section"));
		sa.setDottedFieldValueWithoutTriggering("ExpenseAccount",vcef.getFieldValue("ExpenseAccount"));
		sa.setDottedFieldValueWithoutTriggering("Order",vcef.getFieldValue("Order"));
		sa.setDottedFieldValueWithoutTriggering("Misc",vcef.getFieldValue("Misc"));

		if (sa != null){
			response = CatValidateInvAccountingString.validateAccounting(sa);
		}

		if (response != null){
			sbrtnRtCode = response.getMessage().getSubroutineReturnCode();
			sbrtnMessage = response.getMessage().getSubroutineReturnMessage();
		}
		if (sbrtnRtCode != null && (sbrtnRtCode.compareTo("00") != 0)) {
			AccountingErrorMsg = "Account Distribution is not valid in CAPS: Error - " + sbrtnMessage + "\n";
		}

		if (!StringUtil.nullOrEmptyOrBlankString(AccountingErrorMsg)) {
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: Error Message returned from the Accounting Validation: \n%s", ClassName, AccountingErrorMsg);
			return ListUtil.list(Constants.getInteger(ValidationError), AccountingErrorMsg);
		}

		return NoErrorResult;
	}
}