/*
 * Created by KS on Dec 1, 2004
 */
package config.java.condition.vcsv2;

import ariba.approvable.core.*;
import ariba.base.fields.*;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.*;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.*;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatUseMAAccountingOnRLI extends Condition {

	private static final String THISCLASS = "CatUseMAAccountingOnRLI";

    public boolean evaluate(Object object, PropertyTable params)
 	throws ConditionEvaluationException {

        return useMAAccounting(object);
    }

    public boolean useMAAccounting(Object object) {

    	if (CatConstants.DEBUG)
    	    Log.customer.debug("%s *** Object: %s", THISCLASS, object);

        boolean result = false;
    	Boolean useAccounting = null;
		LineItem li = null;

		if (object instanceof SplitAccounting) {
		 	SplitAccounting sa = (SplitAccounting)object;
		 	li = sa.getLineItem();
		}
		else if (object instanceof ProcureLineItem) {
		 	li = (ProcureLineItem)object;
		}
		if (li instanceof ContractCoreApprovableLineItem) {
			ContractCoreApprovableLineItem mali = (ContractCoreApprovableLineItem)li;
		 	useAccounting = (Boolean)mali.getFieldValue("UseAccountingOnRLI");
		}
		else if (li instanceof ReqLineItem){
			ReqLineItem rli = (ReqLineItem)li;
			if (rli.getMALineItem() != null)
			    useAccounting = (Boolean)rli.getDottedFieldValue("MALineItem.UseAccountingOnRLI");
		}
		if (useAccounting != null && useAccounting.booleanValue())
		    result = true;
	 	if (CatConstants.DEBUG) {
	 	    Log.customer.debug("CatUseMAAccountingOnRLI *** useAccounting = " + useAccounting);
	 	    Log.customer.debug("CatUseMAAccountingOnRLI *** result = " + result);
	 	    if (result)
			 	Log.customer.debug("CatUseMAAccountingOnRLI *** li.getClassName(): " + li.getClassName());
	 	}
		return result;
    }

	public CatUseMAAccountingOnRLI() {
		super();
	}

}
