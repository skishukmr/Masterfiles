/*
 * Created by KS on Nov 30, 2004
 */
package config.java.condition;

import ariba.base.fields.*;
import ariba.util.core.*;
import ariba.purchasing.core.*;
import ariba.contract.core.*;
import ariba.util.log.Log;

public class CatHasNonReleaseMALineItem extends CatHasMALineItem {

	private static final String classname = "CatHasNonReleaseMALineItem";

    public boolean evaluate(Object object, PropertyTable params)
	 	throws ConditionEvaluationException {    
	
    	if (!super.hasMALineItem(object))
    		return false;
    	else 
    		return isNonReleaseType((ReqLineItem)object, params);
}

    public boolean isNonReleaseType(ReqLineItem rli, PropertyTable params) {

    	boolean result = false;
    	int release = 0;
    	if (rli != null) {
			Contract ma = (Contract)rli.getFieldValue("MasterAgreement");				
			if (ma != null) {
				release = ma.getReleaseType();
		   		Log.customer.debug("CatHasNonReleaseMALineItem *** MA Release Type: " + release);		   			
			}
    	}
    	Log.customer.debug("CatHasNonReleaseMALineItem *** Release = " + release);
    	Log.customer.debug("CatHasNonReleaseMALineItem *** Result = " + result);  
    	if (release != 1)
    		result = true;
     	return result;
	}
	
	public CatHasNonReleaseMALineItem() {
		super();
	}
	
}
