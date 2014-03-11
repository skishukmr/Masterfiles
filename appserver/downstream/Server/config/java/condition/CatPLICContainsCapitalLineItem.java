/*
 * Created by KS on April 15, 2005
 * -------------------------------------------------------------------------------
 * Returns TRUE if PLIC contains a line item with AccountType = "Capital" 
 */
package config.java.condition;

import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.*;

public class CatPLICContainsCapitalLineItem extends Condition {

	private static final String THISCLASS = "CatPLICContainsCapitalLineItem";
	private static final String CAPITAL = "Capital";

    public boolean evaluate(Object object, PropertyTable params)
    { 	
        return containsCapitalLine(object);
    }
       
	public static boolean containsCapitalLine(Object object) {
		
		boolean result = false;
		if(object instanceof ProcureLineItemCollection) {
    		ProcureLineItemCollection plic = (ProcureLineItemCollection)object;
    		
    		BaseVector lines = plic.getLineItems();
    		if (lines != null) {
    		    int i = 0;
    		    while (i < lines.size()) {
    		        ProcureLineItem pli = (ProcureLineItem)lines.get(i);
    		        ClusterRoot accounttype = (ClusterRoot)pli.getFieldValue("AccountType");
    		        if (accounttype != null) {
    		            String uname = accounttype.getUniqueName();
    		            if (CatConstants.DEBUG)    		            
    		                Log.customer.debug("%s **** AccountType: %s", THISCLASS, uname);       		            
    		            if (uname.equalsIgnoreCase(CAPITAL)) {
    		                result = true;
    		            }
    		        }
    		    i++;
    		    }
    		}
		}
		if (CatConstants.DEBUG)
		    Log.customer.debug("CatPLICContainsCapitalLineItem *** result: " + result);    		
		return result;
	}
	
    
	public CatPLICContainsCapitalLineItem() {
		super();
	}

}

