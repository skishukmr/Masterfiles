/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/28/2006
	Description: 	Trigger implementation to set the Payment Terms on Req Line
					from 2 sources based on order of preference -
					- MasterAgreement
					- Partition Parameter
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
import ariba.contract.core.Contract;
import ariba.payment.core.PaymentTerms;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetRLIPaymentTerms extends Action
{
	private static final String ClassName = "CatEZOSetRLIPaymentTerms";
	//AUL, sdey : changed the class name for PaymentTerms
	//private static final String PaymentTermsClass = "ariba.common.core.PaymentTerms";
	private static final String PaymentTermsClass = "ariba.payment.core.PaymentTerms";
	private static final String paymentTermsDefault = "Application.Procure.DefaultPaymentTerms";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException
	{
		if (object instanceof ReqLineItem)
		{
			PaymentTerms pt = null;
			ReqLineItem rli = (ReqLineItem) object;
			Contract ma = rli.getMasterAgreement();

			Log.customer.debug("%s ::: MasterAgreement: %s", ClassName, ma);
			if (ma != null)
			{
				pt = ma.getPaymentTerms();

					Log.customer.debug("%s ::: Set PayTerms from Contract: %s", ClassName, pt);
					if (pt != null)
						Log.customer.debug("%s ::: MasterAgreement UniqueName / Payment Terms: %s / %s", ClassName, ma.getUniqueName(), pt.getUniqueName());
			}
			if (pt == null)
			{
				Partition p = rli.getPartition();
				String param = Base.getService().getParameter(p, paymentTermsDefault);
				if (!StringUtil.nullOrEmptyOrBlankString(param))
				{
					ClusterRoot cr = Base.getService().objectMatchingUniqueName(PaymentTermsClass, p, param);
					if (cr != null)
					{
						pt = (PaymentTerms) cr;
						Log.customer.debug("%s ::: Set Payment Terms from Default Parameter: %s / %s", ClassName, pt, pt.getUniqueName());
						rli.setFieldValue("PaymentTerms", pt);
					}
				}
			}
			if (pt == null ){
				Log.customer.debug("%s ::: Out of options, Payment Terms NOT set: %s", ClassName, pt);
				rli.setFieldValue("PaymentTerms", pt);
			}
				Log.customer.debug("%s ::: AFTER setting Payment Terms: %s", ClassName, rli.getFieldValue("PaymentTerms"));
		}
	}

	public CatEZOSetRLIPaymentTerms()
	{
		super();
	}
}
