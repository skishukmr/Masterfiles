/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/25/2006
	Description: 	Trigger implementation to set accounting data based on
					account type.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	10/28/2006
	Description: 	Added condition for defaulting on MARLineItems also
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	10/29/2006
	Description: 	Added logic to default Division for Capital Accounting.
					Added logic to populate Accounting Fac. for all Account
					types.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	10/09/2006
	Description: 	Added logic to default accounting on MAR when a release not
					required MAR as well as only when the line level field
					"EnterAccounting" is marked true.
-------------------------------------------------------------------------------
	Change Author: 	Dharmang J. Shelat
	Date Modified:	12/13/2006
	Description: 	Added logic to null out the complementary accounting fields
					when Account Type is changed to Expense or Capital.
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Iterator;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Accounting;
import ariba.common.core.CommodityExportMapEntry;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.User;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed MasterAgreement to Contract
 */
public class CatEZOSetAccountingForAccountType extends Action
{
	private static final String ClassName = "CatEZOSetAccountingForAccountType";
	private static final String param_dept = "Application.Caterpillar.Procure.DepartmentForCapital";
	private static final String param_exp = "Application.Caterpillar.Procure.ExpenseForCapital";
	private static final String param_div = "Application.Caterpillar.Procure.DivisionForCapital";
	private static final String EXP_CAPITAL = "0000";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		String fac = null;
		String dept = null;
		String div = null;
		String sect = null;
		String expAcct = null;
		if (object instanceof ReqLineItem || object instanceof ContractRequestLineItem){
			ProcureLineItem rli = (ProcureLineItem) object;
			Approvable lic = rli.getLineItemCollection();
			String contractAvailability = null;
			int withRelease = 100;
			Boolean enterAccountingB = null;
			boolean enterAccounting = true;
			if (object instanceof ContractRequestLineItem){
				contractAvailability = (String) lic.getFieldValue("ContractAvailability"); //Global or National
				withRelease = ((ContractRequest)lic).getReleaseType(); //0=NoRelease 1=Release
				enterAccountingB = (Boolean) rli.getDottedFieldValue("EnterAccounting");
				if (enterAccountingB != null) {
					enterAccounting = enterAccountingB.booleanValue();
				}
			}

			CommodityExportMapEntry ceme = rli.getCommodityExportMapEntry();
			ClusterRoot type = (ClusterRoot) rli.getFieldValue("AccountType");
			Partition part = rli.getPartition();
			Approvable r = (Approvable) rli.getLineItemCollection();
			ariba.user.core.User requester = r.getRequester();

			if (part == null || requester == null || type == null)
			{
				Log.customer.debug("%s ::: PROBLEM: Key field is null!", ClassName);
				return;
			}
			String atype = type.getUniqueName();
			Log.customer.debug("%s ::: Account Type: %s", ClassName, atype);
			User user = User.getPartitionedUser(requester, part);
			if (user != null)
			{
				Accounting ua = user.getAccounting();
				if (ua != null)
				{
					fac = (String) ua.getFieldValue("AccountingFacility");
					dept = (String) ua.getFieldValue("Department");
					div = (String) ua.getFieldValue("Division");
					sect = (String) ua.getFieldValue("Section");
				}
			}
			if (ceme != null)
			{
				expAcct = (String) ceme.getFieldValue("ExpenseAccount");
			}
			if (object instanceof ContractRequestLineItem)
			{
				ClusterRoot accntFacOnMAR = (ClusterRoot) rli.getLineItemCollection().getFieldValue("AccountingFacility");
				if (accntFacOnMAR != null){
					fac = accntFacOnMAR.getUniqueName();
				}
				else{
					fac = "";
				}
			}
			Log.customer.debug("%s ::: Default Values: Fac: %s, Dept: %s, Div: %s, Sect: %s, ExpAcct: %s", ClassName, fac, dept, div, sect, expAcct);
			SplitAccountingCollection sac = rli.getAccountings();
			if (sac != null)
			{
				BaseVector splits = sac.getSplitAccountings();
				if (!splits.isEmpty())
				{
					for (Iterator itr = splits.iterator(); itr.hasNext();)
					{
						SplitAccounting sa = (SplitAccounting) itr.next();
						if (rli instanceof ContractRequestLineItem){
							sa.setDottedFieldValue("AccountingFacility", fac);
						}
						if (atype.equals("Expense") && !"Global".equals(contractAvailability) && (withRelease != 1) && enterAccounting)
						{
							sa.setDottedFieldValueRespectingUserData("AccountingFacility", fac);
							sa.setDottedFieldValueRespectingUserData("Department", dept);
							sa.setDottedFieldValueRespectingUserData("Division", div);
							sa.setDottedFieldValueRespectingUserData("Section", sect);
							sa.setDottedFieldValueRespectingUserData("ExpenseAccount", expAcct);
							sa.setDottedFieldValue("Order", null);
							sa.setDottedFieldValue("Misc", null);
							sa.setDottedFieldValue("CompDivision",null);
							sa.setDottedFieldValue("CompSection",null);
							sa.setDottedFieldValue("CompExpenseAccount",null);
						}
						else
						{
							if (atype.equals("Capital") && !"Global".equals(contractAvailability) && (withRelease != 1) && enterAccounting)
							{
								dept = Base.getService().getParameter(r.getPartition(), param_dept);
								expAcct = Base.getService().getParameter(r.getPartition(), param_exp);
								div = Base.getService().getParameter(r.getPartition(), param_div);
								sa.setDottedFieldValueRespectingUserData("AccountingFacility", fac);
								if (dept != null)
									sa.setDottedFieldValue("Department", dept);
								if (div != null)
									sa.setDottedFieldValueRespectingUserData("Division", div);
								sa.setDottedFieldValueRespectingUserData("Section", null);
								if (expAcct != null)
									sa.setDottedFieldValue("ExpenseAccount", expAcct);
								sa.setDottedFieldValueRespectingUserData("Order", null);
								sa.setDottedFieldValueRespectingUserData("Misc", null);
								sa.setDottedFieldValue("CompDivision",null);
								sa.setDottedFieldValue("CompSection",null);
								sa.setDottedFieldValue("CompExpenseAccount",null);
							}
						}
					}
				}
			}
		}
		/*
		if (object instanceof InvoiceCoreApprovableLineItem)
		{
			InvoiceCoreApprovableLineItem inli = (InvoiceCoreApprovableLineItem) object;
			ClusterRoot type = (ClusterRoot) inli.getFieldValue("AccountType");
			if (type != null)
			{
				Partition part = inli.getPartition();
				String atype = type.getUniqueName();
				Log.customer.debug("%s ::: Account Type: %s", ClassName, atype);
				SplitAccountingCollection sac = inli.getAccountings();
				if (sac != null)
				{
					BaseVector splits = sac.getSplitAccountings();
					if (!splits.isEmpty())
					{
						for (Iterator itr = splits.iterator(); itr.hasNext();)
						{
							SplitAccounting sa = (SplitAccounting) itr.next();
							if (atype.equals("Capital"))
							{
								dept = Base.getService().getParameter(part, param_dept);
								expAcct = Base.getService().getParameter(inli.getPartition(), param_exp);
								if (dept != null)
									sa.setDottedFieldValue("Department", dept);
								if (expAcct != null)
									sa.setDottedFieldValue("ExpenseAccount", expAcct);
							}
						}
					}
				}
			}
		}
		*/
		return;
	}
	public CatEZOSetAccountingForAccountType()
	{
		super();
	}
}