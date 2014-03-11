/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/07/2006
	Description: 	Trigger implementation to set Accounting Summary String to
					display in the LineItemSimpleGeneralFields group.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetDerivedAccountDistribution extends Action
{
    private static final String THISCLASS = "CatEZOSetDerivedAccountDistribution";
    private static final String Separator = "-";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {
		if (object instanceof SplitAccounting) {
			SplitAccounting sa = (SplitAccounting) object;
			LineItem li = sa.getLineItem();
			if (sa.getNumberInCollection() == 1 && li instanceof ReqLineItem) {

				StringBuffer dist = new StringBuffer();

				ClusterRoot type = (ClusterRoot) li.getFieldValue("AccountType");
				if (type != null)
					dist.append(type.getUniqueName()).append("  ");

				String aField = (String) sa.getFieldValue("AccountingFacility");
				if (!StringUtil.nullOrEmptyOrBlankString(aField))
					dist.append(aField);
				aField = (String) sa.getFieldValue("Department");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}
				aField = (String) sa.getFieldValue("Division");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}
				aField = (String) sa.getFieldValue("Section");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}
				aField = (String) sa.getFieldValue("ExpenseAccount");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}
				aField = (String) sa.getFieldValue("Order");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}
				aField = (String) sa.getFieldValue("Misc");
				if (!StringUtil.nullOrEmptyOrBlankString(aField)) {
					dist.append(Separator);
					dist.append(aField);
				}

				Log.customer.debug("%s ::: Summary acct dist is: %s",THISCLASS,dist.toString());
				li.setFieldValue("DerivedAccountDistribution", dist.toString());
			}
		}
	}

    public CatEZOSetDerivedAccountDistribution() {
        super();
    }
}