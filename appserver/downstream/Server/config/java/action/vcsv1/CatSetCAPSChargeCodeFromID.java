/*
 * Created by KS on Sep 14, 2005
 * --------------------------------------------------------------
 * Used to set CAPSChargeCode (object) from CAPSChargeCodeID string
 */
package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProductDescription;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetCAPSChargeCodeFromID extends Action {

    private static final String THISCLASS = "CatSetCAPSChargeCodeFromID";
    private static String CAPSClass = "cat.core.CAPSChargeCode";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        ProductDescription pd = null;
        if (object instanceof ProcureLineItem) {
            ProcureLineItem pli = (ProcureLineItem)object;
            pd = pli.getDescription();
        }      
        else if (object instanceof ProductDescription)         
            pd = (ProductDescription)object;    
        
        if (pd != null) {
            String ccodeID = (String)pd.getFieldValue("CAPSChargeCodeID");
            Log.customer.debug("%s *** caps ID: %s",THISCLASS, ccodeID); 
            if (ccodeID != null) {
                ClusterRoot caps = Base.getService().objectMatchingUniqueName(CAPSClass,
                        Partition.None, ccodeID);
                Log.customer.debug("%s *** caps Object: %s",THISCLASS, caps); 
                if (caps != null)
                    pd.setFieldValue("CAPSChargeCode", caps);
            }        
 //           Log.customer.debug("%s *** caps object (after): %s",THISCLASS, pd.getFieldValue("CAPSChargeCode"));                
        }     
    }
    
    public CatSetCAPSChargeCodeFromID() {
        super();
    }


}
