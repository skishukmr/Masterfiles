/*
 * Created by KS on Dec 1, 2004
 */
package config.java.condition;

import ariba.approvable.core.*;
import ariba.base.fields.*;
import ariba.common.core.SplitAccounting;
import ariba.contract.core.*;
import ariba.purchasing.core.*;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatMasterAgreementWithRelease extends Condition {

	private static final String classname = "CatMasterAgreementWithRelease";

    public boolean evaluate(Object object, PropertyTable params)
 	throws ConditionEvaluationException {

		Log.customer.debug("%s *** In evaluate", classname);
        return isReleaseable(object);
    }

    public boolean isReleaseable(Object object) {

    	boolean result = false;
    	int release = 0;
		Log.customer.debug("%s *** Object: %s", classname, object);
		LineItem li = null;
		LineItemCollection lic = null;
		if (object instanceof SplitAccounting) {
		 	SplitAccounting sa = (SplitAccounting)object;
		 	li = sa.getLineItem();
		 	if (li != null)
		 		lic = li.getLineItemCollection();
		}
		if (object instanceof LineItem) {
			li = (LineItem)object;
			lic = li.getLineItemCollection();
		}
		if (object instanceof LineItemCollection)
			lic = (LineItemCollection)object;

		if (lic instanceof ContractCoreApprovable) {
			ContractCoreApprovable maca = (ContractCoreApprovable)lic;
		 	Log.customer.debug("%s ***  MA: %s", classname, maca);
		 	release = maca.getReleaseType();
		}
		if (lic instanceof Requisition){
			Requisition r = (Requisition)lic;
			Contract ma = (Contract)r.getFieldValue("MasterAgreement");
		 	Log.customer.debug("%s *** Master Agreement (REQ): %s", classname, ma);
			if (ma != null)
				release = ma.getReleaseType();
		}
	 	if (release == 1)
	 		result = true;
	 	Log.customer.debug("CatMasterAgreementWithRelease *** release = " + release);
		Log.customer.debug("CatMasterAgreementWithRelease *** result = " + result);
		return result;
    }

	public CatMasterAgreementWithRelease() {
		super();
	}

}
