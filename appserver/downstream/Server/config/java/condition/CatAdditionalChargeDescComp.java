/* Created by Mounika on April 29, 2013 - WI 193
 * --------------------------------------------------------------
 * Used to determine if Description begins with Additional Charge
 */
package config.java.condition;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.util.core.StringUtil;

public class CatAdditionalChargeDescComp extends Condition {

	private static final String THISCLASS = "CatAdditionalChargeDescComp";

    public boolean evaluate(Object object, PropertyTable params)
 	throws ConditionEvaluationException {

        Log.customer.debug("Entering Class : %s ",THISCLASS);
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
            return isAdditionalChargeComp(pli);

        return false;
    }

    public static boolean isAdditionalChargeComp(ProcureLineItem pli) {

    boolean isAC = false;
    try {
        String descpli = (String)pli.getDescription().getFieldValue("Description");
        Log.customer.debug("%s *** Description: %s",THISCLASS, descpli);
        Boolean parli = (Boolean)pli.getDottedFieldValue("Description.IsPartialItem");
        if(parli) {
            Log.customer.debug("%s *** IsPartialItem : %s",THISCLASS, parli);
            if (descpli != null && descpli.startsWith("Additional Charge -")) {
            Log.customer.debug("%s *** Description contains Additional Charge -",THISCLASS);
	        isAC = true;
            }
            else {
            Log.customer.debug("%s *** Description does not contain Additional Charge -",THISCLASS);
            isAC = false;
            }
        }
        else {
            Log.customer.debug("%s *** IsPartialItem : %s",THISCLASS, parli);
            isAC = false;
        }
    }
    catch (Exception exp)
    {
       	Log.customer.debug("CatAdditionalChargeDescComp: Exception occured "+ exp);
	}

	    return isAC;
    }


	public CatAdditionalChargeDescComp() {
	super();
	}

}
