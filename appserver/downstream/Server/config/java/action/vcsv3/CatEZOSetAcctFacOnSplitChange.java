/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/25/2006
	Description: 	Trigger implementation to set account facility from the 1st
					split onto the following splits.
-------------------------------------------------------------------------------
	Change Author: 	
	Date Created:  	
	Description: 	
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Iterator;

import ariba.base.core.BaseId;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

public class CatEZOSetAcctFacOnSplitChange extends Action
{
	private static final String ClassName = "CatEZOSetAcctFacOnSplitChange";
	private static String AccountingParam = "Accounting";
	private ValueInfo[] parameterInfo;
	private String[] requiredParameterNames;

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		String fac = null;

		SplitAccounting paramSA = (SplitAccounting) params.getPropertyForKey(AccountingParam);
		BaseId paramSABId = paramSA.getBaseId();
		if (paramSA != null){
			ProcureLineItem li = (ProcureLineItem) paramSA.getLineItem();
			if (li != null){
				SplitAccountingCollection sac = li.getAccountings();

				if (sac != null){
					BaseVector sacV = sac.getSplitAccountings();
					if (paramSA != null){
						fac = (String) paramSA.getFieldValue("AccountingFacility");
					}

					if (sacV != null && (li instanceof ReqLineItem) && !StringUtil.nullOrEmptyOrBlankString(fac)){
						if (sacV.size() > 1){
							for (Iterator itr = sacV.iterator(); itr.hasNext();)
							{
								SplitAccounting sa = (SplitAccounting) itr.next();
								BaseId saBId = sa.getBaseId();
								if (!saBId.equals(paramSABId))
									sa.setDottedFieldValue("AccountingFacility",fac);
							}
						}
					}
				}
			}
		}
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

	public CatEZOSetAcctFacOnSplitChange()
	{
		initializeParams();
	}
}