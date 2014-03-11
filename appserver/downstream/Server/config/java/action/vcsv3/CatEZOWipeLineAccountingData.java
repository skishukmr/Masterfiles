/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	11/01/2006
	Description: 	Trigger implementation to wipe out line accounting data
					when the Enter Accounting is unchecked on MAR Line Item on
					Nationally scoped Contract Request without Release.
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Iterator;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.PropertyTable;

public class CatEZOWipeLineAccountingData extends Action
{
	private static final String ClassName = "CatEZOWipeLineAccountingData";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof ContractRequestLineItem){
			ContractRequestLineItem marli = (ContractRequestLineItem) object;
			SplitAccountingCollection sac = marli.getAccountings();
			if (sac != null)
			{
				BaseVector splits = sac.getSplitAccountings();
				if (!splits.isEmpty())
				{
					for (Iterator itr = splits.iterator(); itr.hasNext();)
					{
						SplitAccounting sa = (SplitAccounting) itr.next();
						//sa.setDottedFieldValueWithoutTriggering("AccountingFacility", "");
						sa.setDottedFieldValue("Department", "");
						sa.setDottedFieldValue("Division", "");
						sa.setDottedFieldValue("Section", "");
						sa.setDottedFieldValue("ExpenseAccount", "");
						sa.setDottedFieldValue("Order", "");
						sa.setDottedFieldValue("Misc", "");
						sa.setDottedFieldValue("CompDivision", "");
						sa.setDottedFieldValue("CompSection", "");
						sa.setDottedFieldValue("CompExpenseAccount", "");
					}
				}
			}
		}
		return;
	}
	public CatEZOWipeLineAccountingData()
	{
		super();
	}
}