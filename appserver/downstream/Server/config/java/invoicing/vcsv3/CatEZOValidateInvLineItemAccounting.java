package config.java.invoicing.vcsv3;

import java.util.Iterator;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CatEZOValidateInvLineItemAccounting extends Action {

	private static final String ClassName = "CatEZOValidateInvLineItemAccounting";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof SplitAccountingCollection){
			object = ((SplitAccountingCollection)object).getLineItem();
		}

		if (object instanceof StatementCoreApprovableLineItem) {
			Log.customer.debug("%s ::: Entering Validate Acctng fire for Invoice Reconciliations", ClassName);
			StatementCoreApprovableLineItem ili = (StatementCoreApprovableLineItem) object;
			SplitAccountingCollection sac = ili.getAccountings();

		    // 01.17.06  Added skip for AccountType = Other && non-Null Order#
		    ClusterRoot acctType = null;
		    if (!StringUtil.nullOrEmptyOrBlankString(CatEZOInvoiceAccountingValidation.skipOtherOrder)
		            && CatEZOInvoiceAccountingValidation.skipOtherOrder.startsWith("Y"))
		    	acctType =(ClusterRoot)ili.getFieldValue("AccountType");


			if (sac != null) {
				Iterator saci = sac.getAllSplitAccountingsIterator();

				// 01.17.06  Added skip for AccountType = Other && non-Null Order#
			    if (acctType == null || !acctType.getUniqueName().
			            equals(CatEZOInvoiceAccountingValidation.skipAcctType)) {

					if (!CatEZOInvoiceAccountingValidation.getIsSimulation()) {
						while (saci.hasNext()) {
							SplitAccounting sa = (SplitAccounting) saci.next();
							sa.setFieldValue("ValidateAccountingMessage", null);
							CatEZOInvoiceAccountingValidation.validateAccounting(sa);
							//if (Log.customer.debugOn)
							    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n",
							            (String)sa.getFieldValue("ValidateAccountingMessage"));
						}
					}
					else { // use local simulation
						while (saci.hasNext()) {
							SplitAccounting sa = (SplitAccounting) saci.next();
							sa.setFieldValue("ValidateAccountingMessage", null);
							CatEZOInvoiceAccountingValidation.simulateValidateAccounting(sa);
							//if (Log.customer.debugOn)
							    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n",
							            (String)sa.getFieldValue("ValidateAccountingMessage"));
						}
					}
			    }
			    else { // 01.17.06  Added temporary branch - skip handling for AccountType = Other

			        //if (Log.customer.debugOn)
					    Log.customer.debug("\n %s ::: TEMP logic branch - Acct Type = Other!", ClassName);
					while (saci.hasNext()) {
						SplitAccounting sa = (SplitAccounting) saci.next();
						sa.setFieldValue("ValidateAccountingMessage", null);
						String order = (String)sa.getFieldValue("Order");
						if (StringUtil.nullOrEmptyOrBlankString(order)) {
							CatEZOInvoiceAccountingValidation.validateAccounting(sa);
							//if (Log.customer.debugOn)
							    Log.customer.debug("\n\n Validate Acctng Msg: %s \n\n",
							            (String)sa.getFieldValue("ValidateAccountingMessage"));
						}
						else {
					        //if (Log.customer.debugOn)
							    Log.customer.debug("\n %s ::: Order Number populated, skipping Validation!", ClassName);

					        FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
					        sa.setDottedFieldValue("ValidateAccountingMessage",
							        CatEZOInvoiceAccountingValidation.ValidAccountingMsg);

						        // S. Sato AUL - Added isImmutable fp check
							if (!fp.isImmutable()) {
							    fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
							}
						}
					}
			    }
			}
			ili.setAccountings(sac);
		}
	}

	public CatEZOValidateInvLineItemAccounting() {
		super();
	}
}
