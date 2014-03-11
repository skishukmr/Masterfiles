/* Created by KS on May 18, 2006
 * ---------------------------------------------------------------------------------
 * Issue#406 - Sets PricePrecisionString field on ContractRequestLineItem from Description.Price
 */
package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.formatter.BigDecimalFormatter;
import ariba.util.log.Log;
/*
 * AUL : Changed MARLineItem to ContractRequestLineItem
 */

public class CatSetPricePrecisionString extends Action {

	private static final String THISCLASS = "CatSetPricePrecisionString";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof LineItemProductDescription){
            LineItemProductDescription lipd = (LineItemProductDescription)object;
            Object li = lipd.getLineItem();
            if (li instanceof ContractRequestLineItem) {
                ContractRequestLineItem mali = (ContractRequestLineItem)li;
                Money price = lipd.getPrice();
                if (price != null) {
                    StringBuffer sb = new StringBuffer();
                    Currency curr = price.getCurrency();
                    String string = curr.getPrefix();
                    Log.customer.debug("%s *** string (prefix): %s",THISCLASS,string);
                    if (!StringUtil.nullOrEmptyOrBlankString(string))
                        sb.append(string);
                    BigDecimal amount = price.getAmount();
                    string = amount.toString();
 //                   Log.customer.debug("%s *** string (amount BEFORE): %s",THISCLASS,string);
                    amount = BigDecimalFormatter.round(amount,5);
                    string = amount.toString();
//                    Log.customer.debug("%s *** string (amount AFTER): %s",THISCLASS,string);
                    sb.append(string).append(" ");
                    sb.append(curr.getSuffix());
                    Log.customer.debug("%s *** sb (final): %s",THISCLASS,sb);

                    mali.setFieldValue("PricePrecisionString",sb.toString());
                }
            }
        }
    }

    public CatSetPricePrecisionString() {
        super();
    }


}
