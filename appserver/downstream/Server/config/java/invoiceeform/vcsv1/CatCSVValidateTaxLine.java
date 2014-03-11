// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:45 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 

package config.java.invoiceeform.vcsv1;

import ariba.base.core.BaseObject;
import ariba.base.fields.*;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.*;
import ariba.util.formatter.IntegerFormatter;

public class CatCSVValidateTaxLine extends Condition
{

    public CatCSVValidateTaxLine()
    {
    }

    public boolean evaluate(Object obj, PropertyTable propertytable)
    {
        return evaluateImpl(obj, propertytable);
    }

    private boolean evaluateImpl(Object obj, PropertyTable propertytable)
    {
        BaseObject baseobject = (BaseObject)propertytable.getPropertyForKey("LineItem");
        ariba.base.core.ClusterRoot clusterroot = baseobject.getClusterRoot();
        Assert.that(clusterroot != null, "Cluster root not set");
        return validateLine(clusterroot, baseobject);
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
    {
        if(!evaluateImpl(obj, propertytable))
            return new ConditionResult(Fmt.Sil("aml.InvoiceEform", "InvalidTaxLineItem", subjectForMessages(propertytable)));
        else
            return null;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private boolean validateLine(BaseObject baseobject, BaseObject baseobject1)
    {
        ProcureLineType procurelinetype = (ProcureLineType)baseobject1.getFieldValue("LineType");
        if(procurelinetype.getCategory() == 2)
        {
            int i = IntegerFormatter.getIntValue(baseobject1.getFieldValue("OrderLineNumber"));
            if(i != 0)
                return false;
        }
        return true;
    }

    private static final String requiredParameterNames[] = {
        "LineItem"
    };
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem", 0, "config.java.invoiceeform.InvoiceEformLineItem")
    };
    private static final String ComponentStringTable = "aml.InvoiceEform";
    protected static final String InvalidLineItemKey = "InvalidTaxLineItem";

}