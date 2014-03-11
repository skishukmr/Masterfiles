/*
 * CatPrefSuppCreatorNameTable.java
 * Created by Chandra on Aug 20, 2005
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
 *  Nametable to select preferred supplier creators. Used in Reports to select owner.
 */
public class CatPrefSuppCreatorNameTable
                            extends AQLNameTable {

    private static final String classname = "CatPrefSuppCreatorNameTable: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery) {
        //Log.customer.debug(classname + "query = " + query);

        String condition = Fmt.S("\"User\" IN (SELECT DISTINCT PrefSuppCreator FROM ariba.common.core.Supplier AS Supplier)");
        Log.customer.debug("**%s : Condition Text = %s", classname, condition);

        query.and(AQLCondition.parseCondition(condition));
        Log.customer.debug("%s Final Query = %s" ,classname, query.toString());
    }

    public CatPrefSuppCreatorNameTable() {
        super();
    }
}
