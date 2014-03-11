
package config.java.invoiceeform.vcsv3;

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

public class CatEZOInitializeLineAmounts extends Action {

    private static final String CLASSNAME = "CatEZOInitializeLineAmounts";

    public void fire(ValueSource valuesource, PropertyTable params) throws ActionExecutionException {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** valuesource: %s",CLASSNAME,valuesource);

        if (valuesource instanceof BaseObject) {
            BaseObject line = (BaseObject)valuesource;

            ClusterRoot eform = line.getClusterRoot();
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** eform: %s",CLASSNAME,eform);

            if (eform != null) {

            	Currency currency = null;
            	List lineItems = (List)eform.getFieldValue("LineItems");

	            if (!ListUtil.nullOrEmptyList(lineItems)) {
	            	BaseObject refLine = (BaseObject)lineItems.get(lineItems.size()-1);  // last line for reference
	            	try {
	            		currency = (Currency)((Money)refLine.getFieldValue("Price")).getCurrency();
	                    //if (Log.customer.debugOn)
	                        Log.customer.debug("%s *** Using currency from last line Price: %s",CLASSNAME, currency);
	            	}
	            	catch (Exception e) {
	            		Log.customer.debug("%s *** Exception when getting previous line currency: %s",CLASSNAME, e);
	            	}
	            }
	            else {
	            	try {
	            		currency = (Currency)eform.getDottedFieldValue("TotalInvoicedLessTax.Currency");
	                    //if (Log.customer.debugOn)
	                        Log.customer.debug("%s *** Using currency from Inv SubTotal: %s",CLASSNAME, currency);
	            	}
	            	catch (Exception e) {
	            		Log.customer.debug("%s *** Exception when getting header SubTotal currency: %s",CLASSNAME, e);
	            	}
	            }
	            if (currency != null) {
		    		Money zeroMoney = new Money(Constants.ZeroBigDecimal, currency);
		    		line.setDottedFieldValueWithoutTriggering("Price",zeroMoney);
		    		line.setDottedFieldValueWithoutTriggering("Amount",zeroMoney);
	            }
	            else {
	                //if (Log.customer.debugOn)
	                    Log.customer.debug("%s *** Not changing currency!",CLASSNAME);
	            }
	        }
        }
    }
}
