// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:34 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 

package config.java.invoiceeform.vcsv1;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.*;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import java.util.List;

public class CatCSVValidateNonSummaryInvoice extends Condition
{

    public CatCSVValidateNonSummaryInvoice()
    {
    }

    public boolean evaluate(Object obj, PropertyTable propertytable)
    {
        return evaluateImpl(obj, propertytable);
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
    {
        boolean flag = evaluate(obj, propertytable);
        if(flag)
            return null;
        else
            return new ConditionResult(Fmt.Sil("aml.InvoiceEform", "SummaryInvoiceError", subjectForMessages(propertytable)));
    }

    private boolean evaluateImpl(Object obj, PropertyTable propertytable)
    {
        ClusterRoot clusterroot = (ClusterRoot)propertytable.getPropertyForKey("InvoiceEForm");
        List list = (List)clusterroot.getFieldValue("Orders");
        return list.size() <= 1;
    }

    public ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    public ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    public String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final ValueInfo valueInfo = new ValueInfo(0);
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("InvoiceEForm", 0, "config.java.invoiceeform.InvoiceEform")
    };
    private static final String requiredParameterNames[] = {
        "InvoiceEForm"
    };
    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String StringTable = "ariba.common.core.condition";
    private static final String ErrorMsgKey = "SummaryInvoiceError";

}