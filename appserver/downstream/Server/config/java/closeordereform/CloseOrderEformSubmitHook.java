/********************************************************************************************************************
												Revision History

1)  Amit 	09-25-2007		Changed the package from config.java.closeordereform.vcsv1 to config.java.closeordereform

*********************************************************************************************************************/


package config.java.closeordereform;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.common.core.Log;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.formatter.BooleanFormatter;
import ariba.util.formatter.DateFormatter;



public class CloseOrderEformSubmitHook implements ApprovableHook
{
	private static final String ClassName = "CloseOrderEformSubmitHook";
	private static final List NoErrorResult = ListUtil.list(Constants.getInteger(NoError));

	public List run(Approvable approvable)
	{

		Log.customer.debug("%s ::: Entering the Submit Hook Implementation run method", ClassName);
		Log.customer.debug("%s ::: Looking at COEF: %s", ClassName, approvable.getUniqueName());

		Approvable coef = approvable;
		Boolean CloseOrder = (Boolean) coef.getDottedFieldValue("CloseOrder");

		if (CloseOrder != null)
		{
			if (CloseOrder.booleanValue())	//If Close Order
			{
				processOrder(coef, true);
			}
			else							//If Reopening Order
			{
				processOrder(coef, false);
			}
		}
		return NoErrorResult;
	}

	void processOrder(Approvable coef, boolean IsCloseOrder)
	{
		BaseVector allorders = null;
		Log.customer.debug("%s ::: Inside the processOrder method", ClassName);
		if (IsCloseOrder)
		{
			Log.customer.debug("%s ::: ClosePO Vactor is... %s", ClassName, coef.getFieldValue("ClosePO"));
			allorders = (BaseVector)coef.getFieldValue("ClosePO");
		}
		else
		{
			Log.customer.debug("%s ::: OpenPO Vactor is... %s", ClassName, coef.getFieldValue("OpenPO"));
			allorders = (BaseVector)coef.getFieldValue("OpenPO");
		}

		Log.customer.debug("%s ::: Order Vector IS: %s...", ClassName, allorders);

		if (allorders != null)
		{
			Date date = Date.getNow();
			//Date.setTimeToMidnight(date);
			//date = date.makeCalendarDate();

			Log.customer.debug("%s ::: Vector Size is... %s", ClassName, allorders.size());
			Log.customer.debug("%s ::: date==%s", ClassName, date);

			for (int i=0; i< allorders.size(); i++)
			{
				Log.customer.debug("%s ::: # %s Item is...%s ", ClassName, i, allorders.get(i));
				BaseSession bs = Base.getSession();
				ClusterRoot dor = (ClusterRoot) ( bs.objectFromId( (BaseId)allorders.get(i) ) );
				Log.customer.debug("%s ::: # %s ---------- ", ClassName, dor);
				if (IsCloseOrder) {
					//closing orders selected, setting the closeorder date to current time
					Log.customer.debug("%s ::: Closing the PO: %s", ClassName, dor.getUniqueName());
					Log.customer.debug("%s ::: set date as: %s", ClassName, date.toString());
					dor.setFieldValue("CloseOrderDate", date);
				} else {
					//reopen orders selected, setting the closeorder date based on closeorder period
					//selected in eform as standard (from parm table) or custom date.

					Log.customer.debug("%s ::: Opening the PO: %s", ClassName, dor.getUniqueName());
					boolean closeOrderPeriod = BooleanFormatter.getBooleanValue(coef.getDottedFieldValue("CloseOrderPeriod"));
					//if true meaninfgStandard, use the period specified in Param table else use custom date
					if (closeOrderPeriod) {
						String closeorderafter = Base.getService().getParameter(null,"System.Base.CloseOrderAfter");
						Log.customer.debug("%s *** Date Before Adding %s Days: %s ",ClassName, closeorderafter, date);
						int idays = -1;
						if (closeorderafter!= null)
							idays = Integer.parseInt(closeorderafter);
						Log.customer.debug("%s *** After parsing the param value is %s Days: %s ",ClassName, idays);
						if (idays != -1) {
							Date.addDays(date, idays);
							Log.customer.debug("%s *** Date After Adding %s Days: %s ",ClassName, closeorderafter, date);
							dor.setFieldValue("CloseOrderDate", date);
							Log.customer.debug("%s ::: CloseDate for the PO set to: %s", ClassName, date.toString());
						}
					} else {
						date = DateFormatter.getDateValue(coef.getDottedFieldValue("CustomCloseOrderDate"));
						if( date!=null) dor.setFieldValue("CloseOrderDate", date);
						Log.customer.debug("%s ::: CloseDate for the PO set with custom date : %s", ClassName, date.toString());
					}
				}
				Log.customer.debug("%s *** CloseOrder flag on order set as : "+IsCloseOrder,ClassName);
				dor.setFieldValue("CloseOrder",new Boolean(IsCloseOrder));

			}

		}
	}
}
