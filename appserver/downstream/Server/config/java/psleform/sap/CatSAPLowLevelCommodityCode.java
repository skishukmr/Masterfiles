/*
 * CatSAPLowLevelCommodityCode.java
 Change Log:
 Date			Developer		 Issue#	Description
 18/05/2010		Vikram J Singh	 1115	Configuration to create a psl eform for 2-digit category ID at line level
 *
 */

package config.java.psleform.sap;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.basic.core.CommodityCode;
import ariba.util.core.*;
import ariba.util.log.Log;

public class CatSAPLowLevelCommodityCode extends Condition {
	public boolean evaluate(Object value, PropertyTable params) {
		Log.customer.debug("CatSAPLowLevelCommodityCode : Within evaluate ");
		return testCommodity(value, params);
	}
	protected boolean testCommodity(Object value, PropertyTable params) {
		boolean result = true;
		Log.customer.debug("CatSAPLowLevelCommodityCode : value =>" +value);
		if (value instanceof BaseObject)
		{
			BaseObject bo = (BaseObject) value;
			Log.customer.debug("CatSAPLowLevelCommodityCode : bo =>" +bo);
			if(bo.getDottedFieldValue("Category") !=null)
			{
				CommodityCode cc = (CommodityCode) bo.getDottedFieldValue("Category");
				Log.customer.debug("CatSAPLowLevelCommodityCode : cc =>" +cc);

				if(cc!=null && cc.getDescendents().size() > 1 )
				{
					String ccUniqueName = cc.getUniqueName();
					Log.customer.debug("CatSAPLowLevelCommodityCode : ccUniqueName =>" +ccUniqueName);
					if (!StringUtil.nullOrEmptyString(ccUniqueName)&& ccUniqueName.length() < 2)
					{
						Log.customer.debug("CatSAPLowLevelCommodityCode : ccUniqueName.length() =>" +ccUniqueName.length());
						result = false;
					}
				}
			}
		}
		Log.customer.debug("CatSAPLowLevelCommodityCode : result =>" +result);
		return result;
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
		if (!testCommodity(value, params)) {
			Log.customer.debug("%s *** evaluateAndExplain error: %s","CatLowLevelCommodityCode", errorMsg);
			return new ConditionResult(errorMsg);
		} else {
			return null;
		}
	}
	public CatSAPLowLevelCommodityCode() {
	}
	private static final String errorMsg = Fmt.Sil("cat.vcsv1","LowLevelCommodityError");
}