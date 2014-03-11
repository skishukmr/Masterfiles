//created by KS on Nov 24, 2004

package config.java.condition;

import java.math.BigDecimal;

import ariba.base.fields.*;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.basic.core.Money;
import ariba.base.fields.condition.NonZeroField;

public class CatNonZeroMoney extends NonZeroField {

	private static final String classname = "CatNonZeroMoney";
 	private static final ValueInfo parameterInfo[] = {new ValueInfo("Message", IsScalar, "java.lang.String")};
 	private static final String requiredParameterNames[] = { "Message" };

    public boolean evaluate(Object value, PropertyTable params)
    {
		Log.customer.debug("%s *** In evaluate", classname);    	
        return testValue(value, params);
    }
    
	protected boolean testValue(Object value, PropertyTable params) {
		Log.customer.debug("%s *** In testValue", classname);
		boolean result = false;
		if(value instanceof Money) {
   		    if (((Money)value).getAmount().compareTo(new BigDecimal(0.00)) > 0)
				result = true;
		}
		Log.customer.debug("CatNonZeroMoney *** result: " + result);    		
		return result;
	}
	
    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
		Log.customer.debug("%s *** In evaluateAndExplain", classname);
    	if(!testValue(value, params)) {
    		Log.customer.debug("%s *** String(message): %s", classname, (String)params.getPropertyForKey("Message"));    		
    		Log.customer.debug("%s *** Fmt.S(message): %s", classname, Fmt.S((String)params.getPropertyForKey("Message")));    		
            return new ConditionResult(Fmt.S((String)params.getPropertyForKey("Message")));
    	} else 
            return null;
    }	
	
	public CatNonZeroMoney() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}	

}

