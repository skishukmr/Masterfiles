/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	11/01/2006
	Description: 	Trigger implemented to set the Accounting Facility on the
					MAR lines the header facility.
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import java.util.Iterator;

import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZODefaultLineSplitAccountingFac extends Action
{
	private static final String ClassName = "CatEZODefaultLineSplitAccountingFac";

	public void fire(ValueSource object, PropertyTable params)
	{
		if (object instanceof ContractRequest) {
			ContractRequest mar = (ContractRequest) object;
			ClusterRoot marAccntFac = (ClusterRoot) mar.getFieldValue("AccountingFacility");
			//String contractAvailability = (String) mar.getFieldValue("ContractAvailability"); //Global or National
			//int withRelease = mar.getReleaseType(); //0=NoRelease 1=Release
			if (marAccntFac != null){
				Log.customer.debug("%s ::: MAR Accnt Fac is: %s", ClassName, marAccntFac.getUniqueName());
				BaseVector marLines = mar.getLineItems();
				if (!marLines.isEmpty()) {
					int size = marLines.size();
					Log.customer.debug("%s ::: MAR Line size is: " + size, ClassName);
					for (int i = 0; i < size; i++) {
						ContractRequestLineItem mali = (ContractRequestLineItem) marLines.get(i);
						Log.customer.debug("%s ::: MAR Line item is: %s", ClassName, mali);
						if (mali != null){
							SplitAccountingCollection sac = mali.getAccountings();
							if (sac != null)
							{
								BaseVector splits = sac.getSplitAccountings();
								if (!splits.isEmpty())
								{
									for (Iterator itr = splits.iterator(); itr.hasNext();)
									{
										SplitAccounting sa = (SplitAccounting) itr.next();
										sa.setDottedFieldValue("AccountingFacility", marAccntFac.getUniqueName());
									}
								}
								else {
									Log.customer.debug("%s ::: Split Accounting is empty", ClassName);
								}
							}
						}
						else{
							Log.customer.debug("%s ::: Mali is null", ClassName);
						}
					}
				}
				else{
					Log.customer.debug("%s ::: MAR Lines Vector is Empty", ClassName);
				}
			}
		}
	}

	public CatEZODefaultLineSplitAccountingFac()
	{
	}
}