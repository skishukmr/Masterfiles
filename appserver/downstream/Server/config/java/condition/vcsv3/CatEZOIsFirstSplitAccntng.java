/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/06/2006
	Description: 	Condition implementation to check if the split accounting
					passed as parameter is the first split.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.condition.vcsv3;

import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;

public class CatEZOIsFirstSplitAccntng extends Condition
{
	private static final String ClassName = "CatEZOIsFirstSplitAccntng";
	private static String AccountingParam = "Accounting";
	private static String ErrorMsg = "This is not the first split on the line";
	private ValueInfo[] parameterInfo;
	private String[] requiredParameterNames;

	public boolean evaluate(Object value, PropertyTable params) {
		return (evaluateAndExplain(value, params) == null);
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
		SplitAccounting sa = (SplitAccounting) params.getPropertyForKey(AccountingParam);
		if (sa != null){
			ProcureLineItem li = (ProcureLineItem) sa.getLineItem();
			if (li != null){
				SplitAccountingCollection sac = li.getAccountings();
				if (sac != null){
					BaseVector sacV = sac.getSplitAccountings();

					BaseId saBid = sa.getBaseId();

					for (int i=0; i<sacV.size(); i++) {
						SplitAccounting firstSA = (SplitAccounting) sacV.get(0);
						BaseId firstSABid = firstSA.getBaseId();

						if (firstSABid.equals(saBid)){
							return null;
						}
						else{
							return new ConditionResult(ErrorMsg);
						}
					}
				}
			}
		}
		return null;
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
				new ValueInfo(AccountingParam, 0, "ariba.common.core.SplitAccounting")
			}
		);
		requiredParameterNames = (new String[] { AccountingParam });
	}

	public CatEZOIsFirstSplitAccntng() {
		initializeParams();
	}
}