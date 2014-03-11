package config.java.condition;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.basic.core.CommodityCode;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatLowLevelCommodityCode extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
        Log.customer.debug("%s *** In evaluate", "CatLowLevelCommodityCode");
        return testCommodity(value, params);
    }

    protected boolean testCommodity(Object value, PropertyTable params)
    {
        boolean result = true;
        if(value instanceof ProcureLineItem)
        {
            Log.customer.debug("%s *** Instance of ProcureLineItem", "CatLowLevelCommodityCode");
            ProcureLineItem rli = (ProcureLineItem)value;
            if(rli.getDescription() != null && rli.getIsAdHoc() && !rli.getIsFromCatalog())
            {
                Log.customer.debug("%s *** Is AdHoc LIPD!", "CatLowLevelCommodityCode");
                CommodityCode cc = rli.getDescription().getCommonCommodityCode();
                if(cc != null && cc.getDescendents().size() > 1)
                {
                    Log.customer.debug("CatLowLevelCommodityCode *** CC Descendents = " + cc.getDescendents().size());
                    String unique = cc.getUniqueName();
                    Log.customer.debug("%s *** CC UniqueName: %s", "CatLowLevelCommodityCode", unique);
                    if(!StringUtil.nullOrEmptyString(unique) && unique.length() < 6)
                    {
                        Log.customer.debug("CatLowLevelCommodityCode *** UN length ? " + unique.length());
                        result = false;
                    }
                }
            }
        }
        Log.customer.debug("CatLowLevelCommodityCode *** result: " + result);
        return result;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
        if(!testCommodity(value, params))
        {
            Log.customer.debug("%s *** evaluateAndExplain error: %s", "CatLowLevelCommodityCode", errorMsg);
            return new ConditionResult(errorMsg);
        } else
        {
            return null;
        }
    }

    public CatLowLevelCommodityCode()
    {
    }

    private static final String classname = "CatLowLevelCommodityCode";
    private static final String errorMsg = ResourceService.getString("cat.vcsv1", "LowLevelCommodityError");

}
