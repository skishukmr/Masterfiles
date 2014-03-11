package config.java.cxml;

import java.math.BigDecimal;
import java.util.Locale;

import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.server.cxml.ProcureAXComponent;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;

public class CatMoneyComponent extends ProcureAXComponent
{

    public void setRoot(Object o)
    {
        super.setRoot(o);
        money = (Money)o;
        Currency baseCurrency = Currency.getBaseCurrency();
        Currency reportingCurrency = Currency.getReportingCurrency();
        Currency transactionCurrency = money.getCurrency();
        BigDecimal amt = money.getAmount();
        Log.customer.debug("**CatMoneyComponent** Money passed in here=" + money);
        amount = BigDecimalFormatter.getStringValue(amt, 5, Locale.US);
        currency = transactionCurrency.getUniqueName();
        if(!transactionCurrency.isEuro())
        {
            if(transactionCurrency.inEMU())
            {
                alternateCurrency = "EUR";
                alternateAmount = money.convertToCurrency(Currency.getCurrency("EUR")).getAmount().toString();
            }
        } else
        if(transactionCurrency.isEuro())
            if(baseCurrency != null && !baseCurrency.isEuro() && baseCurrency.inEMU())
            {
                alternateCurrency = baseCurrency.getUniqueName();
                alternateAmount = money.getApproxAmountInBaseCurrency().toString();
            } else
            if(reportingCurrency != null && !reportingCurrency.isEuro() && reportingCurrency.inEMU())
            {
                alternateCurrency = transactionCurrency.getUniqueName();
                alternateAmount = money.getAmountInReportingCurrency().toString();
            }
    }

    public CatMoneyComponent()
    {
    }

    private final int PRECISION = 5;
    private Money money;
    public String currency;
    public String amount;
    public String alternateCurrency;
    public String alternateAmount;
}