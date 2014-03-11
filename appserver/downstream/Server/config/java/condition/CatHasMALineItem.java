/*
 * Created by KS on Nov 30, 2004
 */
package config.java.condition;

import ariba.base.fields.*;
import ariba.util.core.*;
import ariba.purchasing.core.*;
import ariba.util.log.Log;

public class CatHasMALineItem extends Condition {

	private static final String classname = "CatHasMALineItem";
	private static final String MALI = "MALineItem";

    public boolean evaluate(Object object, PropertyTable params)
    	throws ConditionEvaluationException {    
    	
		Log.customer.debug("%s *** In evaluate", classname);    	
        return hasMALineItem(object);
    }
	
	public boolean hasMALineItem(Object object) {

	    if (object instanceof ReqLineItem) {
	     	ReqLineItem rli = (ReqLineItem)object;
	        Log.customer.debug("CatHasMALineItem *** MALineItem = " + rli.getFieldValue(MALI));
	     	return (rli.getFieldValue(MALI) != null);
		}
	    return false;
	}
	
	public CatHasMALineItem() {
		super();
	}

}
