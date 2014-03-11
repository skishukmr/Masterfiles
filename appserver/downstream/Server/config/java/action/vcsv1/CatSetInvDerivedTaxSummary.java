package config.java.action.vcsv1;

import java.math.BigDecimal;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSetInvDerivedTaxSummary extends Action
{

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        if(valuesource instanceof ProcureLineItem)
        {
            ProcureLineItem procurelineitem = (ProcureLineItem)valuesource;
            StringBuffer stringbuffer = new StringBuffer();
            ClusterRoot clusterroot = (ClusterRoot)procurelineitem.getFieldValue("TaxCode");
            if(clusterroot != null)
                stringbuffer.append(clusterroot.getUniqueName());
            else
                stringbuffer.append("null");
            stringbuffer.append(", ");
            ClusterRoot clusterroot1 = (ClusterRoot)procurelineitem.getFieldValue("TaxState");
            if(clusterroot1 != null)
                stringbuffer.append(clusterroot1.getUniqueName());
            else
                stringbuffer.append("null");
            stringbuffer.append(", ");
            BigDecimal bigdecimal = (BigDecimal)procurelineitem.getFieldValue("TaxRate");
            if(bigdecimal != null)
            {
                bigdecimal = bigdecimal.setScale(4, 0);
                stringbuffer.append(bigdecimal.toString()).append("%");
            } else
            {
                stringbuffer.append("null");
            }
            stringbuffer.append(", ");
            String s = (String)procurelineitem.getFieldValue("ERPTaxCode");
            if(s != null)
                stringbuffer.append(s);
            else
                stringbuffer.append("null");
            stringbuffer.append(", ");
            BigDecimal bigdecimal1 = (BigDecimal)procurelineitem.getFieldValue("TaxAmountAuth");
            if(bigdecimal1 != null)
            {
                bigdecimal1 = bigdecimal1.setScale(4, 0);
                stringbuffer.append("$").append(bigdecimal1.toString());
            } else
            {
                stringbuffer.append("null");
            }
            Log.customer.debug("%s ::: taxsummary string is: %s", "CatSetInvDerivedTaxSummary", stringbuffer);
            procurelineitem.setFieldValue("DerivedTaxSummary", stringbuffer.toString());
        }
    }

    public CatSetInvDerivedTaxSummary()
    {
    }

    private static final String ClassName = "CatSetInvDerivedTaxSummary";
}
