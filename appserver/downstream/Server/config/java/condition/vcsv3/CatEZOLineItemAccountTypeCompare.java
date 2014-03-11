/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/06/2006
	Description: 	Condition implementation to check for the Line Item Account
					type and return true if "Other".
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.condition.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.condition.BoundedDate;
import ariba.common.core.SplitAccounting;
import ariba.util.core.PropertyTable;

public class CatEZOLineItemAccountTypeCompare extends Condition
{
	private static final String ClassName = "CatEZOLineItemAccountTypeCompare";
	private static String AccountingParam = "Accounting";
	private static String ValueParam = "ValueToTest";
	private static String ErrorMsg = "The Account type selected on the line is not Other or null";
	private ValueInfo[] parameterInfo;
	private String[] requiredParameterNames;

	public boolean evaluate(Object value, PropertyTable params) {
		return (evaluateAndExplain(value, params) == null);
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
		SplitAccounting sa = (SplitAccounting) params.getPropertyForKey(AccountingParam);
		LineItem li = sa.getLineItem();
		ClusterRoot accountType = (ClusterRoot) li.getFieldValue("AccountType");
		if (accountType != null) {
			String accountTypeName = accountType.getUniqueName();
			if ("Other".equals(accountTypeName)) {
				return null;
			}
			else {
				return new ConditionResult(ErrorMsg);
			}
		}
		return new ConditionResult(ErrorMsg);
	}

	protected ValueInfo[] getParameterInfo() {
		ValueInfo[] parentInfo = super.getParameterInfo();
		return ValueInfo.concatInfoArrays(parentInfo, parameterInfo);
	}

	protected String[] getRequiredParameterNames() {
		String[] parentReqs = super.getRequiredParameterNames();
		return ValueInfo.concatStringArrays(parentReqs, requiredParameterNames);
	}

	private final void initializeParams() {
		parameterInfo =
		(
			new ValueInfo[] {
				new ValueInfo(AccountingParam, 0, "ariba.common.core.SplitAccounting"),
				new ValueInfo(ValueParam, 0, "java.lang.String")
			}
		);
		requiredParameterNames = (new String[] { AccountingParam, ValueParam});
	}

	public CatEZOLineItemAccountTypeCompare() {
		initializeParams();
	}
}