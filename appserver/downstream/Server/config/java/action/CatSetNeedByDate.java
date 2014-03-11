/*
 * Created by KS on Nov 27, 2004
 Change History:
 	Change By	Change Date		Description
  =============================================================================================
  1 Kannan	    04/15/2008		Issue 770, Need by date set to null if Application.Caterpillar.Procure.NeedByAdder = 0
 */
package config.java.action;

import ariba.base.fields.*;
import ariba.base.core.Base;
import ariba.base.core.*;
import ariba.util.core.*;
import ariba.util.core.PropertyTable;
import ariba.purchasing.core.*;
import ariba.base.fields.Fields;
import ariba.util.log.Log;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import java.lang.Integer;


public class CatSetNeedByDate extends Action {

	private static final String classname = "CatSetNeedByDate";
	private static final String needByAdder = "Application.Caterpillar.Procure.NeedByAdder";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof Requisition) {
			Requisition r = (Requisition)object;
			BaseVector dlines = r.getDefaultLineItems();
			if (!ListUtil.nullOrEmptyList(dlines)) {
				ReqLineItem dli = (ReqLineItem)dlines.get(0);
				Date today = Date.getNow();
				Date today2 = Fields.getService().getNow();
				Log.customer.debug("CatSetNeedByDate *** Today:" + today);
				Log.customer.debug("CatSetNeedByDate *** Today2 :" + today2);

				String param = Base.getService().getParameter(r.getPartition(), needByAdder);
				Log.customer.debug("%s *** Adder param: %s", classname, param);
				if (!StringUtil.nullOrEmptyOrBlankString(param)) {
					int adder = Integer.parseInt(param);
					// Issue 770 start
					if (adder > 0) {
						Date.addDays(today, adder);
						Log.customer.debug("CatSetNeedByDate *** New Needby: " + today);
						dli.setDottedFieldValue("NeedBy", today);

					}
					if (adder == 0) {
						Log.customer.debug("CatSetNeedByDate *** New NeedByAdder from parameters.table: " + adder);
						dli.setDottedFieldValue("NeedBy", null);

					}
					// Issue 770 end
				}
			}
		}
	}

	public CatSetNeedByDate() {
		super();
	}

}
