package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.LineItemProductDescription;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetReasonCodeOnPriceChange extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof LineItemProductDescription)
        {
            LineItemProductDescription lipd = (LineItemProductDescription)object;
            ariba.procure.core.ProcureLineItem pli = lipd.getLineItem();
            if(pli instanceof ReqLineItem)
            {
                Log.customer.debug("%s *** Testing: Price == ZERO_MONEY?", "CatSetReasonCodeOnPriceChange");
                Money price = lipd.getPrice();
                if(ZERO_MONEY.compareTo(price) < 0)
                {
                    Log.customer.debug("%s *** RESET ReasonCode to (no value)!", "CatSetReasonCodeOnPriceChange");
                    lipd.setFieldValue("ReasonCode", "(no value)");
                }
            }
        }
    }

    public CatSetReasonCodeOnPriceChange()
    {
    }

    private static final String THISCLASS = "CatSetReasonCodeOnPriceChange";
    public static final Money ZERO_MONEY = new Money(new BigDecimal(0.0D), Currency.getBaseCurrency());
    public static final String NO_VALUE = "(no value)";

}
