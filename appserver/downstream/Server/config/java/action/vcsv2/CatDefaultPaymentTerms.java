package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.Contract;
import ariba.payment.core.PaymentTerms;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatDefaultPaymentTerms extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ReqLineItem)
        {
            PaymentTerms pt = null;
            ReqLineItem rli = (ReqLineItem)object;
            Contract ma = rli.getMasterAgreement();
            if(ma != null)
            {
                pt = ma.getPaymentTerms();
                if(CatConstants.DEBUG)
                    Log.customer.debug("%s *** (1)Set PayTerms from Contract: %s", "CatDefaultPaymentTerms", pt);
            } else
            {
                SupplierLocation sloc = rli.getSupplierLocation();
                if(sloc != null)
                {
                    pt = sloc.getPaymentTerms();
                    if(CatConstants.DEBUG)
                        Log.customer.debug("%s *** (2)Set PayTerms from SuplrLoc: %s", "CatDefaultPaymentTerms", pt);
                }
            }
            if(pt == null)
            {
                ariba.base.core.Partition part = rli.getPartition();
                String param = Base.getService().getParameter(part, "Application.Procure.DefaultPaymentTerms");
                if(!StringUtil.nullOrEmptyOrBlankString(param))
                {
                	//AUL, sdey : changed the class name for PaymentTerms
                    //ariba.base.core.ClusterRoot cr = Base.getService().objectMatchingUniqueName("ariba.common.core.PaymentTerms", part, param);
                	ariba.base.core.ClusterRoot cr = Base.getService().objectMatchingUniqueName("ariba.payment.core.PaymentTerms", part, param);
                	if(cr != null)
                    {
                        pt = (PaymentTerms)cr;
                        if(CatConstants.DEBUG)
                            Log.customer.debug("%s *** (3)Set PayTerms from Param: %s", "CatDefaultPaymentTerms", pt);
                    }
                }
            }
            if(pt == null && CatConstants.DEBUG)
                Log.customer.debug("%s *** Out of options, PayTerms NOT set!: %s", "CatDefaultPaymentTerms", pt);
        }
    }

    public CatDefaultPaymentTerms()
    {
    }

    private static final String THISCLASS = "CatDefaultPaymentTerms";
    //AUL, sdey : changed the class name for PaymentTerms
    //private static final String PAYTERMSCLASS = "ariba.common.core.PaymentTerms";
    private static final String PAYTERMSCLASS = "ariba.payment.core.PaymentTerms";
    private static final String ptPARAM = "Application.Procure.DefaultPaymentTerms";
}
