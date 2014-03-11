
package config.java.invoiceeform;

import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  @author kstanley  Mar 06, 2007
		Initializes Price & Amount using Currency of last line item on Eform
*/

public class CatDefaultOrderNumberAndCurrency extends Action {

    private static final String CLASSNAME = "CatDefaultOrderNumberAndCurrency";
    private static final String Order_Number = "OrderNumber";
    private static final String Currency_Path = "Price.Currency";

    public void fire(ValueSource valuesource, PropertyTable params) throws ActionExecutionException {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** valuesource: %s",CLASSNAME,valuesource);

        if (valuesource instanceof BaseObject) {

            ClusterRoot eform = ((BaseObject)valuesource).getClusterRoot();
            if (eform != null) {

            	List lineItems = (List)eform.getFieldValue("LineItems");
            	int size = ListUtil.getListSize(lineItems);

	            if (size>1) {
	            	// get source values
	            	BaseObject line = (BaseObject)lineItems.get(size-2);  // previous line for reference
	            	String orderNum = (String)line.getFieldValue(Order_Number);
	            	Currency currency = (Currency)line.getDottedFieldValue(Currency_Path);

	            	// update new line values
	            	line = (BaseObject)lineItems.get(size-1);
	            	if ((String)line.getFieldValue(Order_Number) == null) { // only null when manual ADD used
	            		line.setFieldValue(Order_Number, orderNum);
		            	if (currency != null) {
		            		Money zeroMoney = new Money(Constants.ZeroBigDecimal, currency);
				    		line.setDottedFieldValueWithoutTriggering("Price",zeroMoney);
				    		line.setDottedFieldValueWithoutTriggering("Amount",zeroMoney);
		            	}
	            	}
	            }
	        }
        }
    }
}
