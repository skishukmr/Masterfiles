/* Created by Soumya on Jan 17, 2012
 * --------------------------------------------------------------
 * Used to determine if ProcureLineItem is an additional charge and make the TaxCode Field as Editable for the TaxManager and the CatPurchasing
 */
package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.base.core.ClusterRoot;
import ariba.user.core.User;
import ariba.base.core.Base;

public class CatSAPAdditionalChargeLineItemTaxCodeEdit extends Condition {

        private static final String THISCLASS = "CatSAPAdditionalChargeLineItemTaxCodeEdit";
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
            return isAdditionalChargeTaxCode(pli);

        return false;
    }

    public static boolean isAdditionalChargeTaxCode(ProcureLineItem pli) 
	{

        boolean isAC = false;
		boolean isSetVertex = false;
        if (pli != null && pli.getIsInternalPartId()) 
		{
            String chargecode = (String)pli.getDescription().getFieldValue("CAPSChargeCodeID");
            Log.customer.debug("%s *** chargecode: %s",THISCLASS, chargecode);
            String additionalChargeEnabled = (String)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.AdditionalChargeEnabled");
			String checkCallToVertex = (String)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.CallToVertexEnabled");
			if (chargecode != null && !materialCC.equals(chargecode) && additionalChargeEnabled!=null && additionalChargeEnabled.equalsIgnoreCase("Y")) 
			{
				if(checkCallToVertex !=null)
				{
					ClusterRoot cr = Base.getSession().getEffectiveUser();
					Log.customer.debug(" %s : ** Clusterroot of the login User is %s",THISCLASS, cr);	
					User userName1 = (User) cr;
					Log.customer.debug(" %s : ** UserID of the login User is %s",THISCLASS, userName1);	
					
					if(checkCallToVertex.equals("PR") || checkCallToVertex.equals("IR") || checkCallToVertex.equals("PIB"))
					{
						if(userName1.hasPermission("CatPurchasing") || userName1.hasPermission("TaxManager"))
						{
							isAC = false;
							isSetVertex = true;
						}
					}
				}
				if(!isSetVertex)
				{
					isAC = true;
				}
			}			                
		}
		return isAC;
	}


        public CatSAPAdditionalChargeLineItemTaxCodeEdit() {
                super();
        }

}