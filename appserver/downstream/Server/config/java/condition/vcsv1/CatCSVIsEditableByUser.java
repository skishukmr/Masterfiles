package config.java.condition.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.invoicing.vcsv1.CatCSVIRApprovalRulesUtil;

public class CatCSVIsEditableByUser extends Condition {

	private static final String ClassName = "CatCSVIsEditableByUser";

	public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {
			Log.customer.debug("%s ::: Entering the evaluate method", ClassName);
			Log.customer.debug("%s ::: The passed in object is: %s", ClassName, object);

		InvoiceReconciliation ir = (InvoiceReconciliation) object;

		ariba.user.core.User currUser = (ariba.user.core.User) Base.getSession().getEffectiveUser();
		String currUserString = currUser.getMyName();
		String currUserUNString = currUser.getUniqueName();
		//String settlementCodeReq = CatCSVIRApprovalRulesUtil.getRequester(ir).getUniqueName();
		String settlementCodeUser = null;
		if (ir.getOrder()!=null){
			settlementCodeUser = CatCSVIRApprovalRulesUtil.getPreparer(ir).getUniqueName();
		}
		else if (ir.getMasterAgreement()!=null){
			settlementCodeUser = CatCSVIRApprovalRulesUtil.getRequester(ir).getUniqueName();
		}

		ClusterRoot settlementCodeObj = (ClusterRoot) ir.getFieldValue("SettlementCode");
		String settlementCodeStr = null;
		Integer settlementCodeInteger = null;
		if (settlementCodeObj != null)
			settlementCodeStr = settlementCodeObj.getUniqueName();

		if (settlementCodeStr != null) {
			//if (("00".compareTo(settlementCodeStr) <= 0) || ("99".compareTo(settlementCodeStr) >= 0)) {
			if ((settlementCodeStr.compareTo("00") >= 0) || (settlementCodeStr.compareTo("99") <= 0)) {
				Log.customer.debug("%s ::: Settlement Code is over 00 and under 99", ClassName);
				settlementCodeInteger = new java.lang.Integer(settlementCodeStr);
			}
			else {
				Log.customer.debug("%s ::: Settlement Code is under 00 or over 99", ClassName);
				settlementCodeInteger = new java.lang.Integer(0);
			}
		}
		else {
			Log.customer.debug("%s ::: Settlement Code is null hence integer value used is 0", ClassName);
			settlementCodeInteger = new java.lang.Integer(0);
		}

		Log.customer.debug("%s ::: Settlement Code Int Value: " + settlementCodeInteger.intValue(), ClassName);
		Log.customer.debug("%s ::: currUserUNString: %s", ClassName, currUserUNString);
		Log.customer.debug("%s ::: settlementCodeReq: %s", ClassName, settlementCodeUser);

		if ((settlementCodeInteger.intValue() > 30) && (currUserUNString.equals(settlementCodeUser))) {
				Log.customer.debug("%s ::: Returning true as current user is the requester of the PO/MA and SC>30", ClassName);
			return true;
		}
		Log.customer.debug("%s ::: Returning false as either current user is not the requester of the PO/MA or SC<30", ClassName);
		return false;
	}

	public ConditionResult evaluateAndExplain(Object object, PropertyTable params) throws ConditionEvaluationException {
		Log.customer.debug("%s ::: Entering the evaluateAndExplain method", ClassName);
		Log.customer.debug("%s ::: The passed in object is: %s", ClassName, object);

		if (!evaluate(object, params)) {
			return new ConditionResult("");
		}
		Log.customer.debug("%s ::: Skipping validation", ClassName);
		return null;
	}
}