/*
 * CatHideComentFieldForForm.java
 * Created by Chandra on Aug 30, 2005
 *
 */
package config.java.psleform;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.Comment;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*
 * Condition: returns false for the Comment.field which this condition associates
 * for the approvable type set in param ApprovableType -- else returns true
 */
public class CatHideComentFieldForForm extends Condition {

    private static final String classname = "CatHideComentFieldForForm: ";
    private static final ValueInfo parameterInfo[] = {
        new ValueInfo("ApprovableType", IsScalar, "java.lang.String")};

    private static final String requiredParameterNames[] = { "ApprovableType" };

    public boolean evaluate(Object object, PropertyTable params)
                throws ConditionEvaluationException {

        String apprToHideField = (String)params.getPropertyForKey("ApprovableType");
        Log.customer.debug("%s**approvable to hide this comment field ==", classname,apprToHideField);


        if(object !=null ){
            Comment comment = (Comment)object;


            Approvable approvable = (Approvable)comment.getClusterRoot();
            if(approvable == null) {
                java.util.List approvables = comment.getFutureParents();
                if(approvables != null)
                    approvable = (Approvable)ListUtil.firstElement(approvables);


            }
            //Log.customer.debug("%s**approvable this comment belongs="+approvable+"", classname);

            if(approvable != null) {
                String apprTypeName = approvable.getTypeName();

                //Log.customer.debug("%s**approvable name=%s", classname, apprTypeName);

                if (apprTypeName.equals(apprToHideField)) {
                    //Log.customer.debug("%s**Returning false for =%s", classname, apprToHideField);
                    return false;
                }

            }

        }
        return true;

    }

    public CatHideComentFieldForForm() {
        super();
    }

    protected ValueInfo[] getParameterInfo() {
            return parameterInfo;
    }
    protected String[] getRequiredParameterNames()  {
        return requiredParameterNames;
    }

}