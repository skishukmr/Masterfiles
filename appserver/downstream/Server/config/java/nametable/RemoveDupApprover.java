package config.java.nametable;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

public class RemoveDupApprover extends AQLNameTable
{

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        ariba.base.fields.ValueSource vs = getValueSourceContext();
        Log.customer.debug("RemoveDupApprover: firing... ");
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("RemoveDupApprover: query = " + query);
        String conditionText = Fmt.S("AdapterSource != 'None:Group.csv'");
        Log.customer.debug("RemoveDupApprover: Condition Text = " + conditionText);
        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug("RemoveDupApprover: Final Query = " + query);
    }

    public RemoveDupApprover()
    {
    }

    private static final String classname = "RemoveDupApprover: ";
}
