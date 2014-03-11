/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/25/2006
	Description: 	Trigger implementation to set bill to address from account
					facility on the 1st split.
-------------------------------------------------------------------------------
	Author: 		Dharmang J. Shelat
	Date Modified:	10/27/2006
	Description: 	Added logic to default Bill To on Master Agreement Request
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueInfo;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

public class CatEZODefaultBillToForAccntngFacility extends Action
{
	private static final String ClassName = "CatEZOSetAcctFacOnSplitChange";
	private static String AccountingParam = "Accounting";
	private static String billToConstant = "_BillTo";
	private ValueInfo[] parameterInfo;
	private String[] requiredParameterNames;

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		String fac = null;

		SplitAccounting paramSA = (SplitAccounting) params.getPropertyForKey(AccountingParam);
		ProcureLineItem li = (ProcureLineItem) paramSA.getLineItem();

		if (paramSA != null){
			fac = (String) paramSA.getFieldValue("AccountingFacility");
		}

		if ((li instanceof ReqLineItem || li instanceof ContractRequestLineItem) && !StringUtil.nullOrEmptyOrBlankString(fac)){
			String billToUN = fac + billToConstant;
			Address billTo = (Address) Base.getSession().objectFromName(billToUN, "ariba.common.core.Address", Base.getSession().getPartition());
			if (billTo != null)
			{
				li.setFieldValue("BillingAddress", billTo);
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

	public CatEZODefaultBillToForAccntngFacility()
	{
		initializeParams();
	}
}