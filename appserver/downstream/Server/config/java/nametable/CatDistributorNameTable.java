package config.java.nametable;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

public class CatDistributorNameTable extends AQLNameTable
{

    protected void init(String className, boolean classIsLeaf)
    {
        super.init(className, classIsLeaf);
        setIncludesUserObjects(true);
    }

    protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery)
    {
        String queryText = Fmt.S("SELECT Distributor, Distributor.Name FROM cat.core.Distributor AS Distributor");
        query = AQLQuery.parseQuery(queryText);
        Log.customer.debug("%s *** in buildquery query returned=%s", "CatDistributorNameTable", query.toString());
        query = super.buildQuery(query, field, pattern, searchTermQuery);
        Log.customer.debug("%s *** in buildquery after super returned=%s", "CatDistributorNameTable", query.toString());
        return query;
    }

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Final Query: %s", "CatDistributorNameTable", query);
    }

    public CatDistributorNameTable()
    {
    }

    private static final String classname = "CatDistributorNameTable";
}
