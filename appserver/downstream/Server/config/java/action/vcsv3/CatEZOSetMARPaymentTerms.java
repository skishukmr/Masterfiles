/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/27/2006
	Description: 	Trigger implementation to set the Payment Terms on MAR
					using the Default Partition Parameter.
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.action.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.payment.core.PaymentTerms;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetMARPaymentTerms extends Action
{
	private static final String ClassName = "CatEZOSetMARPaymentTerms";
	//AUL, sdey : changed the class name for PaymentTerms
	//private static final String PaymentTermsClass = "ariba.common.core.PaymentTerms";
	private static final String PaymentTermsClass = "ariba.payment.core.PaymentTerms";
	private static final String paymentTermsDefault = "Application.Procure.DefaultPaymentTerms";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof ContractRequest)
		{
			PaymentTerms pt = null;
			ContractRequest mar = (ContractRequest) object;
			Log.customer.debug("%s ::: MasterAgreementRequest: %s", ClassName, mar);
			if (mar != null)
			{
				Partition p = mar.getPartition();
				String param = Base.getService().getParameter(p, paymentTermsDefault);
				if (!StringUtil.nullOrEmptyOrBlankString(param))
				{
					ClusterRoot cr = Base.getService().objectMatchingUniqueName(PaymentTermsClass, p, param);
					if (cr != null)
					{
						pt = (PaymentTerms) cr;
						Log.customer.debug("%s ::: Set Payment Terms from Default Parameter: %s / %s", ClassName, pt, pt.getUniqueName());
					}
				}

				Log.customer.debug("%s ::: Set PayTerms on Contract: %s", ClassName, pt);
					if (pt != null){
						Log.customer.debug("%s ::: MasterAgreementRequest UniqueName / Payment Terms: %s / %s", ClassName, mar.getUniqueName(), pt.getUniqueName());
						mar.setPaymentTerms(pt);
					}
			}
			Log.customer.debug("%s ::: AFTER setting Payment Terms: %s", ClassName, mar.getPaymentTerms().getUniqueName());
		}
	}

	public CatEZOSetMARPaymentTerms()
	{
		super();
	}
}
