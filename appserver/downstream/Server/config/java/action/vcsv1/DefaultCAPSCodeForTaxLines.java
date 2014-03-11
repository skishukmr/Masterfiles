package config.java.action.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.Partition;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Remove all if statement for debugon
 */

public class DefaultCAPSCodeForTaxLines extends Action
{

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        Log.customer.debug("%s ::: Entering the fire method", "DefaultCAPSCodeForTaxLines");
        BaseObject baseobject = (BaseObject)valuesource;
        ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
        if(procurelinetype != null)
        {
            if("SalesTaxCharge".equals(procurelinetype.getUniqueName()))
            {
                Log.customer.debug("%s ::: Setting Charge Code for Sales Tax", "DefaultCAPSCodeForTaxLines");
                ariba.base.core.ClusterRoot clusterroot = Base.getService().objectMatchingUniqueName(CAPSChargeCodeClass, Partition.None, "002");
                baseobject.setDottedFieldValue("CapsChargeCode", clusterroot);
            }
            if("ServiceUseTax".equals(procurelinetype.getUniqueName()))
            {
                Log.customer.debug("%s ::: Setting Charge Code for Service Use Tax", "DefaultCAPSCodeForTaxLines");
                ariba.base.core.ClusterRoot clusterroot1 = Base.getService().objectMatchingUniqueName(CAPSChargeCodeClass, Partition.None, "003");
                baseobject.setDottedFieldValue("CapsChargeCode", clusterroot1);
            }
            if("VATCharge".equals(procurelinetype.getUniqueName()))
            {
                Log.customer.debug("%s ::: Setting Charge Code for VAT Tax", "DefaultCAPSCodeForTaxLines");
                ariba.base.core.ClusterRoot clusterroot2 = Base.getService().objectMatchingUniqueName(CAPSChargeCodeClass, Partition.None, "096");
                baseobject.setDottedFieldValue("CapsChargeCode", clusterroot2);
            }
            if("SpecialCharge".equals(procurelinetype.getUniqueName()))
            {
                Log.customer.debug("%s ::: Setting Charge Code for Special Charge", "DefaultCAPSCodeForTaxLines");
                ariba.base.core.ClusterRoot clusterroot3 = Base.getService().objectMatchingUniqueName(CAPSChargeCodeClass, Partition.None, "007");
                baseobject.setDottedFieldValue("CapsChargeCode", clusterroot3);
            }
        } else
        	Log.customer.debug("%s ::: Encountered a null Procure Line Type", "DefaultCAPSCodeForTaxLines");
    }

    public DefaultCAPSCodeForTaxLines()
    {
    }

    private static final String ClassName = "DefaultCAPSCodeForTaxLines";
    private static String CAPSChargeCodeClass = "cat.core.CAPSChargeCode";

}
