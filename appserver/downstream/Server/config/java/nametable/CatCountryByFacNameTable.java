/*
 * CatCountryByFacNameTable.java
 * Created by Chandra on Sep 02, 2005
 *
 */
package config.java.nametable;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 *  Nametable to select Country for available Cat Facilities.
 */
public class CatCountryByFacNameTable extends AQLNameTable {

    private static final String classname = "CatCountryByFacNameTable";

    protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery) {
        //Log.customer.debug("%s *** in buildquery = query=%s",classname, query.toString());


        String queryText = Fmt.S("SELECT DISTINCT Country, Country.Name "
                                +"FROM ariba.basic.core.Country AS Country "
                                +"WHERE Country IN (Select DISTINCT CatFacility.Country from cat.core.CatFacility) ");

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


    public CatCountryByFacNameTable() {
        super();
    }

}
