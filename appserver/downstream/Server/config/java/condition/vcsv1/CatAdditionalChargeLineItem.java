/* Created by KS on Sep 14, 2005
 * --------------------------------------------------------------
 * Used to determine if ProcureLineItem is an additional charge
 */
package config.java.condition.vcsv1;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatAdditionalChargeLineItem extends Condition {

	private static final String THISCLASS = "CatAdditionalChargeLineItem";
	private static String materialCC = "001";
	
    public boolean evaluate(Object object, PropertyTable params)
 	throws ConditionEvaluationException {    
        
        ProcureLineItem pli = null;
        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            LineItem li = sa.getLineItem();
            if (li instanceof ProcureLineItem) 
                pli = (ProcureLineItem)li;           
        }
        else if (object instanceof ProcureLineItem) 
            pli = (ProcureLineItem)object;
   
        if (pli != null) 
            return isAdditionalCharge(pli);
        
        return false;
    }
    
    public static boolean isAdditionalCharge(ProcureLineItem pli) {
		
        boolean isAC = false;
        if (pli != null && pli.getIsInternalPartId()) {     	
            String chargecode = (String)pli.getDescription().getFieldValue("CAPSChargeCodeID"); 
            Log.customer.debug("%s *** chargecode: %s",THISCLASS, chargecode); 
            if (chargecode != null && !materialCC.equals(chargecode)) {
                isAC = true;
            }
        } 
		return isAC;
    }
    
    
	public CatAdditionalChargeLineItem() {
		super();
	}

}
