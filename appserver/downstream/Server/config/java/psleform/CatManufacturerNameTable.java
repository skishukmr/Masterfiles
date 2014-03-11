/*
 * CatManufacturerNameTable.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 *  Nametable to select Manufacturer created by the user.
 */
public class CatManufacturerNameTable extends AQLNameTable {

    private static final String classname = "CatManufacturerNameTable";

    protected void init(String className, boolean classIsLeaf) {
        super.init(className, classIsLeaf);
        setIncludesUserObjects(true);
    }

    protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery) {
        //Log.customer.debug("%s *** in buildquery = query=%s",classname, query.toString());


        String queryText = Fmt.S("SELECT Manufacturer, Manufacturer.Name "
                                +"FROM cat.core.Manufacturer AS Manufacturer");

        query = AQLQuery.parseQuery(queryText);
        Log.customer.debug("%s *** in buildquery query returned=%s",classname, query.toString());

        query = super.buildQuery(query, field, pattern, searchTermQuery);
        Log.customer.debug("%s *** in buildquery after super returned=%s",classname, query.toString());

        return query;

    }

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery) {

        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Final Query: %s", classname, query);
    }


    public CatManufacturerNameTable() {
        super();
    }

}
