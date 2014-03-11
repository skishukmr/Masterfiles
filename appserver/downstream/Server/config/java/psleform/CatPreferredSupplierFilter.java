/*
 * CatPreferredSupplierFilter.java
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
 *  Nametable to select only preferred suppliers.
 */
public class CatPreferredSupplierFilter extends AQLNameTable
{
    private static final String classname = "CatPreferredSupplierFilter: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug(classname + "firing... ");

        super.addQueryConstraints(query, field, pattern, searchQuery);
        //Log.customer.debug(classname + "query = " + query);
        String conditionText = "";

        conditionText = Fmt.S("PreferredSupplier = true");
        Log.customer.debug(classname + "Condition Text = " + conditionText);

        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug(classname + "Final Query = " + query);
    }

    public CatPreferredSupplierFilter()
    {
        super();
    }
}
