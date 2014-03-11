
package config.java.invoicing.vcsv3;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatEZOSetInvoiceAccounting extends Action {

    private static final String ClassName = "CatEZOSetInvoiceAccounting";
    private static final String DEPT = "Application.Caterpillar.Procure.DepartmentForCapital";
    private static final String EXPACCT = "Application.Caterpillar.Procure.ExpenseForCapital";
    private static final String DIV = "Application.Caterpillar.Procure.DivisionForCapital";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof StatementCoreApprovableLineItem) {
			StatementCoreApprovableLineItem inli = (StatementCoreApprovableLineItem) object;
			ClusterRoot type = (ClusterRoot) inli.getFieldValue("AccountType");
			if (type != null) {
				Partition part = inli.getPartition();
				String atype = type.getUniqueName();
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** Account Type: %s", ClassName, type.getUniqueName());
				SplitAccountingCollection sac = inli.getAccountings();
				if (sac != null) {
					BaseVector splits = sac.getSplitAccountings();
					if (!splits.isEmpty()) {
						for (Iterator itr = splits.iterator(); itr.hasNext();) {
							SplitAccounting sa = (SplitAccounting) itr.next();
							if (atype.equals("Capital")) {
								String value = Base.getService().getParameter(part, DEPT);
								if (value != null)
									sa.setDottedFieldValue("Department", value);
								value = Base.getService().getParameter(part, EXPACCT);
								if (value != null)
								    sa.setDottedFieldValue("ExpenseAccount", value);
						/*		value = Base.getService().getParameter(part, DIV);
								if (value != null)
								    sa.setDottedFieldValue("Division", value);	*/
							}
						/*	else  {
								sa.setDottedFieldValue("Department", null);
						 	    sa.setDottedFieldValue("ExpenseAccount", null);
						 	    sa.setDottedFieldValue("Division", null);
							}	*/
							if (!atype.equals("Other")) {
							    sa.setDottedFieldValue("CompDivision", null);
							    sa.setDottedFieldValue("CompSection", null);
							    sa.setDottedFieldValue("CompExpenseAccount", null);
							}
						}
					}
				}
			}
		}
	}

}