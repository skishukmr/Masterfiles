/******************************************************************************
	Author: 		Kieth Stanley
	Date Created:  	07/06/2005
	Description: 	Condition implementation to check if the split accounting
					passed as parameter is the first split.
-------------------------------------------------------------------------------
	Change Author:	Keith Stanley
	Date Created:	04/19/2006
	Description:	Changes required to print BillTo on HTML printed PO
					Added different paths for US vs. UK in format() method
-------------------------------------------------------------------------------
	Change Author:	Dharmang Shelat
	Date Created:	10/19/2006
	Description:	Added additional path for EU in format() method
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.ordering;

import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.PrintApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ordering.OrderFormatter;
import ariba.purchasing.core.ordering.OrderMethodException;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;
import config.java.print.vcsv1.CatCSVPurchaseOrder_Print;
import config.java.print.vcsv3.CatEZOPurchaseOrder_Print;

public class CatHTMLOrderFormatter implements OrderFormatter, PrintApprovableHook
{
	private static final String ClassName = "CatHTMLOrderFormatter";

	public String format(PurchaseOrder po, PrintWriter out) throws OrderMethodException {
		return format(po, out, true);
	}

	public String format(PurchaseOrder po, PrintWriter out, boolean referenceImages)
	{
		Log.customer.debug("%s ::: STEP 1: Initiate Order Formatter!", ClassName);

		// 04.17.06 (ks) use new CSV print methods if US order
		if (po.getPartitionNumber() == 2) {
			Log.customer.debug("%s ::: US order, use CatCSVPurchaseOrder_Print!", ClassName);
			CatCSVPurchaseOrder_Print cpop = new CatCSVPurchaseOrder_Print();
			cpop.printHTML(po, out, referenceImages);
		}
		// continue to use EU specific
		else if (po.getPartitionNumber() == 4) {
			Log.customer.debug("%s ::: EU order, use CatEZOPurchaseOrder_Print!", ClassName);
			CatEZOPurchaseOrder_Print cpopEU = new CatEZOPurchaseOrder_Print();
			cpopEU.printHTML(po, out, referenceImages);
		}
		// continue to use UK specific for non-US and non-EU
		else {
			Log.customer.debug("%s ::: UK/Other order, use CatPurchaseOrder_Print!", ClassName);
			CatPurchaseOrder_Print catPop = new CatPurchaseOrder_Print();
			catPop.printHTML(po, out, referenceImages);
		}
		return "htm";
	}

	public List run(Approvable approvable, PrintWriter out, Locale locale, boolean printForEmail)
	{
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: Initiate Order Formatter (PRINT HOOK)!", ClassName);
		ClusterRoot effectiveUser = Base.getSession().getEffectiveUser();
		// 1.19.06 Added by Chandra to suppress print since dumps the html code in email
		if (printForEmail) {
			return ListUtil.list(Constants.getInteger(0));
		}
		if (effectiveUser != null) {
			ariba.user.core.User newUser = (ariba.user.core.User) effectiveUser;
			locale = newUser.getLocale();
		}
		if (!(approvable instanceof PurchaseOrder)) {
			return ListUtil.list(Constants.getInteger(1), "Error: not a Purchase Order!");
		}
		CatPurchaseOrder_Print catPop = new CatPurchaseOrder_Print();
		catPop.printHTML((PurchaseOrder) approvable, out, null, true, locale);
		return ListUtil.list(Constants.getInteger(0));
	}

	public CatHTMLOrderFormatter() {
		super();
	}
}