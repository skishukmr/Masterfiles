/*255-IBM AMS_Bijesh Kumar-Budget Check Logic for Account Type "F" and Company Code '1000'*/

package config.java.condition.sap;

import java.util.ArrayList;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;

public class CatSAPValidateAccountingCheck extends Condition {


	private static final String classname = "CatSAPValidateAccountingCheck : ";
	public static String LineItemParam = "LineItem";
	private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = { new ValueInfo(LineItemParam, 0, "ariba.procure.core.ProcureLineItem") };
	private String requiredParameterNames[];
	private PropertyTable params;

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames() {
		return requiredParameterNames;
	}

	// Fetching the details of company code and account category
	public boolean evaluate(Object value, PropertyTable params)
			throws ConditionEvaluationException{
		Log.customer.debug("Entering the method evaluate in class = %s ",classname);

		ProcureLineItem li = (ProcureLineItem) params.getPropertyForKey(LineItemParam);
		LineItemCollection lic = (LineItemCollection) li.getLineItemCollection();
		String acctCategory = (String) lic.getDottedFieldValue("AccountCategory");
		Log.customer.debug("AcctCategory from Line Item Collection:->"+ acctCategory);
		ClusterRoot companycode = (ClusterRoot) lic.getDottedFieldValue("CompanyCode");
		boolean flag = true;
		if (companycode != null && acctCategory != null) {
			String companyCodeUniqueName = (String) lic.getDottedFieldValue("CompanyCode.UniqueName");
			Log.customer.debug(" Companycode from Line Item:-> %s ",companycode);
			if (isAccountCompanyCodeChk(acctCategory, companyCodeUniqueName)) {
			Boolean chkValidateAcct = (Boolean) li.getFieldValue("CheckValidateAcctButton");
				if (chkValidateAcct!=null && !chkValidateAcct) {
					flag = false;
				}
			}
		}
		Log.customer.debug("Exit the method evaluate in class = %s ",classname);

		return flag;
	}

	// Verifying Account category and company code from approvable and file
	public boolean isAccountCompanyCodeChk(String acctCategory, String mach1CompanyCode) {
		Log.customer.debug("Entering the method isAccountCompanyCodeChk in class = %s ",classname);
		boolean temp = false;
		ArrayList<String> companyCodeFromFile = CatCommonUtil.readDataFromFile(CatCommonUtil.COMPANYCODE_FileName);
		Log.customer.debug("%s companyCode From File is : %s",companyCodeFromFile);
		ArrayList<String> accountCategoryFromFile = CatCommonUtil.readDataFromFile(CatCommonUtil.ACCOUNTTYPE_FileName);
		Log.customer.debug("%s accountCategory From File is : %s",accountCategoryFromFile);
		Boolean validCC = CatCommonUtil.checkValueIsAvailable(mach1CompanyCode,companyCodeFromFile);
        Log.customer.debug("validCC from the method checkValueIsAvailable in class = %s %s",classname,validCC);
        Boolean validAccountCategory = CatCommonUtil.checkValueIsAvailable(acctCategory,accountCategoryFromFile);
        Log.customer.debug("validAccountCategory from the method checkValueIsAvailable in class = %s %s ",classname,validAccountCategory);

			if (validCC && validAccountCategory)
				{
				Log.customer.debug("inside if after verifying validCC and validAccountCategory of class = %s ",classname);
				temp = true;
			}
			Log.customer.debug("outside if of of class = %s ",classname);
		Log.customer.debug("Exit the method isAccountCompanyCodeChk in class = %s ",classname);

		return temp;
	}

	// Displaying error message
	public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
			throws ConditionEvaluationException {
			Log.customer.debug("Entering the method evaluateAndExplain in class = %s ",classname);
			ConditionResult cr=null;
		if (!evaluate(value, params))
		{
			String errorMessage = ResourceService.getString(ComponentStringTable, "ClickAccounting");
			cr = new ConditionResult(errorMessage);
			}
		Log.customer.debug("Exit the method evaluateAndExplain in class = %s ",classname);

			return cr;
	}
	public static final String ComponentStringTable = "cat.java.sap";
}
