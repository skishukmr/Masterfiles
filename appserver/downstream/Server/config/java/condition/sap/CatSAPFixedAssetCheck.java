/* Created by Sandeep for MACH1 2.5
 * --------------------------------------------------------------
 * Used to Determine if WBS should be visible for Account Type F only for MACH1 CompanyCodes.
 */
package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPFixedAssetCheck extends Condition {

        private static final String THISCLASS = "CatSAPFixedAssetCheck";
        
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
            return isMACH1FixedAssetAcc(pli);

        return false;
    }

    public static boolean isMACH1FixedAssetAcc(ProcureLineItem pli) {
   String AccType = "F";
   String sapSource ="MACH1"; 
    boolean isMACH1CC = false;
    
    String ACCType = (String)pli.getDottedFieldValue("AccountCategory.UniqueName");
    Log.customer.debug("%s *** Account Type on the Line Item: %s",THISCLASS, ACCType);
    String CCSource = (String)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.SAPSource");
    Log.customer.debug("%s *** SAP Source - Company Code is MACH1 or CBS: %s",THISCLASS, CCSource);
   if(CCSource != null && CCSource.equalsIgnoreCase("MACH1") )
  {
  Log.customer.debug("Source is MACH1");
    if( ACCType != null && ACCType.equalsIgnoreCase("F"))
{    
Log.customer.debug("Source is not null and Source is MACH1 and AccType is F");        
                    isMACH1CC = true;
                }
         
          } 
          Log.customer.debug("Return : " +isMACH1CC);  
          return  isMACH1CC;
                 }   

 public CatSAPFixedAssetCheck() {
                super();
        }

}

