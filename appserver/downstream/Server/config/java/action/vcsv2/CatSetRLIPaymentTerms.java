/*
 * Created by KS on April 24, 2005
 * --------------------------------------------------------------
 * Used to set PaymentTerms on ReqLineItems from 3 sources in order of preference:
 * 1) MasterAgreement, 2) SupplierLoc or 3)partition parameter
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.payment.core.PaymentTerms;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatSetRLIPaymentTerms extends Action {

	private static final String THISCLASS = "CatSetRLIPaymentTerms";
	//AUL, sdey : changed the class name for PaymentTerms
	//private static final String PAYTERMSCLASS = "ariba.common.core.PaymentTerms";
	private static final String PAYTERMSCLASS = "ariba.payment.core.PaymentTerms";
	private static final String ptPARAM = "Application.Procure.DefaultPaymentTerms";

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        if (object instanceof ReqLineItem) {

            PaymentTerms pt = null;
            ReqLineItem rli = (ReqLineItem)object;
            Contract ma = rli.getMasterAgreement();
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** RLI.MasterAgreement: %s",THISCLASS, ma);
            if (ma != null) {
                pt = ma.getPaymentTerms();
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** (1)Set PayTerms from Contract: %s",THISCLASS, pt);
            } else {
                SupplierLocation sloc = rli.getSupplierLocation();
                if (sloc != null) {
                    pt = sloc.getPaymentTerms();
                    if (CatConstants.DEBUG)
                        Log.customer.debug("%s *** (2)Set PayTerms from SuplrLoc: %s",THISCLASS, pt);
                }
            }
            if (pt == null) {
                Partition part = rli.getPartition();
				String param = Base.getService().getParameter(part, ptPARAM);
				if (!StringUtil.nullOrEmptyOrBlankString(param)){
                    ClusterRoot cr = Base.getService().objectMatchingUniqueName(PAYTERMSCLASS,part,param);
                    if (cr != null) {
                        pt = (PaymentTerms)cr;
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (3)Set PayTerms from Param: %s",THISCLASS, pt);
                    }
				}
            }
            if (pt == null && CatConstants.DEBUG)
                Log.customer.debug("%s *** Out of options, PayTerms NOT set!: %s",THISCLASS, pt);
            rli.setFieldValue("PaymentTerms",pt);
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** AFTER setting RLI.PayTerms: %s",THISCLASS,rli.getFieldValue("PaymentTerms"));
        }
    }

    public CatSetRLIPaymentTerms() {
        super();
    }

}
