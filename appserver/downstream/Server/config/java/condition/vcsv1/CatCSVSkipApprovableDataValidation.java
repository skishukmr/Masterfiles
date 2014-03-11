package config.java.condition.vcsv1;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.statement.core.StatementCoreApprovable;
import ariba.statement.core.StatementCoreApprovableLineItem;

public class CatCSVSkipApprovableDataValidation extends Condition
{

    public CatCSVSkipApprovableDataValidation()
    {
    }

    public boolean evaluate(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {
        Log.customer.debug("%s ::: Entering the evaluate method", "CatCSVSkipApprovableDataValidation");
        Log.customer.debug("%s ::: The passed in object is: %s", "CatCSVSkipApprovableDataValidation", obj);

        StatementCoreApprovableLineItem invoicecoreapprovablelineitem = (StatementCoreApprovableLineItem)obj;
        StatementCoreApprovable invoicecoreapprovable = (StatementCoreApprovable)invoicecoreapprovablelineitem.getLineItemCollection();
        if(invoicecoreapprovable.getStatusString().equals("Approved") || invoicecoreapprovable.getStatusString().equals("Reconciled") || invoicecoreapprovable.getStatusString().equals("Rejected") || invoicecoreapprovable.getStatusString().equals("Rejecting"))
        {
            Log.customer.debug("%s ::: Skipping validation", "CatCSVSkipApprovableDataValidation");
            return true;
        } else
        {
            return false;
        }
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {
        Log.customer.debug("%s ::: Entering the evaluateAndExplain method", "CatCSVSkipApprovableDataValidation");
        Log.customer.debug("%s ::: The passed in object is: %s", "CatCSVSkipApprovableDataValidation", obj);

        if(!evaluate(obj, propertytable))
            return new ConditionResult("");

        Log.customer.debug("%s ::: Skipping validation", "CatCSVSkipApprovableDataValidation");
        return null;
    }

    private static final String ClassName = "CatCSVSkipApprovableDataValidation";
}
