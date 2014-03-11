// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:25:53 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   CatHideComentFieldForForm.java

package config.java.condition;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.fields.*;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;

public class CatHideComentFieldForForm extends Condition
{

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        String apprToHideField = (String)params.getPropertyForKey("ApprovableType");
        if(object != null)
        {
            Comment comment = (Comment)object;
            Approvable approvable = (Approvable)comment.getClusterRoot();
            if(approvable == null)
            {
                java.util.List approvables = comment.getFutureParents();
                if(approvables != null)
                    approvable = (Approvable)ListUtil.firstElement(approvables);
            }
            if(approvable != null)
            {
                String apprTypeName = approvable.getTypeName();
                if(apprTypeName.equals(apprToHideField))
                    return false;
            }
        }
        return true;
    }

    public CatHideComentFieldForForm()
    {
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final String classname = "CatHideComentFieldForForm: ";
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("ApprovableType", 0, "java.lang.String")
    };
    private static final String requiredParameterNames[] = {
        "ApprovableType"
    };

}