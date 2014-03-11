// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3)
// Source File Name:   CatIsInternalSupplier.java
/**
    Samir Sato (Ariba Upgrade Lab)
    - This class was decompiled as the source file was missing in the instance
    received.
*/
package config.java.condition;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.*;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatIsInternalSupplier extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
    {
        String uniqueName = null;
        ClusterRoot supplier = null;
        String subj = subjectForMessages(params);
        Log.customer.debug("%s *** %s", "CatIsInternalSupplier", object);
        if(object != null)
        {
            ClusterRoot cr = (ClusterRoot)object;
            uniqueName = (String)cr.getDottedFieldValue("Supplier.UniqueName");
        }
        if(uniqueName != null)
        {
            Log.customer.debug("%s *** SupplierName is %s *** Internal Supplier String IS %s CHECK *** %s", "CatIsInternalSupplier", uniqueName, Key_InvalidSupplier, new Boolean(uniqueName.startsWith(Key_InvalidSupplier)));
            if(uniqueName.startsWith(Key_InvalidSupplier))
                return new ConditionResult(Fmt.Sil("aml.cat.Invoice", "InternalSupplierMsg", subj));
        }
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

    public CatIsInternalSupplier()
    {
    }

    private static final String Key_InvalidSupplier = Fmt.Sil("cat.java.vcsv1", "ErrorKey_InvalidSupplier");
    private static final String classname = "CatIsInternalSupplier";
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("TestField", 0, "java.lang.String")
    };
    private static final String requiredParameterNames[] = {
        "TestField"
    };

}
