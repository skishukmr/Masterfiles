/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/17/2006
	Description: 	Trigger implementation to set Account Type based on
					CEME and if not then default to expense.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	10/28/2006
	Description: 	Added condition for defaulting on MARLineItems also
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommodityExportMapEntry;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */
public class CatEZOSetAccountTypeFromCEME extends Action
{
	private static final String ClassName = "CatEZOSetAccountTypeFromCEME";
	private static final String param_dept = "Application.Caterpillar.Procure.DepartmentForCapital";
	private static final String param_exp = "Application.Caterpillar.Procure.ExpenseForCapital";
	private static final String EXP_CAPITAL = "0000";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		ClusterRoot accountType = null;
		if (object instanceof ReqLineItem || object instanceof ContractRequestLineItem){
			ProcureLineItem rli = (ProcureLineItem) object;
			ClusterRoot type = (ClusterRoot) rli.getFieldValue("AccountType");
			Partition part = rli.getPartition();

			if (type == null){
				CommodityExportMapEntry ceme = rli.getCommodityExportMapEntry();
				if (ceme != null){
					accountType = ceme.getAccountType();
					if (accountType != null){
						Log.customer.debug("%s ::: Account Type Obj is: %s", ClassName, accountType);
						Log.customer.debug("%s ::: Account Type is: %s", ClassName, accountType.getUniqueName());
						rli.setFieldValue("AccountType",accountType);
					}
					else {
						accountType = Base.getService().objectMatchingUniqueName("ariba.common.core.AccountType",part,"Expense");
						if (accountType != null){
							Log.customer.debug("%s ::: Setting Account Type to Expense: %s", ClassName, accountType);
							Log.customer.debug("%s ::: Account Type is: %s", ClassName, accountType.getUniqueName());
						}
						rli.setFieldValue("AccountType",accountType);
					}
				}
				else {
					Log.customer.debug("%s ::: PROBLEM: Key field (CEME) is null!", ClassName);
					accountType = Base.getService().objectMatchingUniqueName("ariba.common.core.AccountType",part,"Expense");
					if (accountType != null){
						Log.customer.debug("%s ::: Setting Account Type to Expense: %s", ClassName, accountType);
						Log.customer.debug("%s ::: Account Type is: %s", ClassName, accountType.getUniqueName());
					}
					rli.setFieldValue("AccountType",accountType);
				}
			}
			else {
				Log.customer.debug("%s ::: PROBLEM: Account Type already set hence not over-writting", ClassName);
			}
		}
		return;
	}
	public CatEZOSetAccountTypeFromCEME()
	{
		super();
	}
}