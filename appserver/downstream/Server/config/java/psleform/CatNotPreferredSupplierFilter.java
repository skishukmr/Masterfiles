/*
 * CatNotPreferredSupplierFilter.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 *  Nametable to select suppliers that are not preferred.
 */
public class CatNotPreferredSupplierFilter extends AQLNameTable
{
    private static final String classname = "CatNotPreferredSupplierFilter: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug(classname + "firing... ");
        //String partition = Base.getSession().getPartition().getName();
        //Log.customer.debug(classname + "partition = %s", partition);

        super.addQueryConstraints(query, field, pattern, searchQuery);
        //Log.customer.debug(classname + "query = " + query);

        String conditionText = Fmt.S("PreferredSupplier = false OR PreferredSupplier IS NULL");
        Log.customer.debug(classname + "Condition Text = " + conditionText);

        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug(classname + "Final Query = " + query);
    }

    public CatNotPreferredSupplierFilter()
    {
        super();
    }
}
