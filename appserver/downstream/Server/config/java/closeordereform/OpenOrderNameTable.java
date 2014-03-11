/********************************************************************************************************************
												Revision History

1)  Amit 	09-25-2007		Changed the package from config.java.closeordereform.vcsv1 to config.java.closeordereform

*********************************************************************************************************************/



package config.java.closeordereform;


import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 * To select orders to open
 */
public class OpenOrderNameTable
                            extends AQLNameTable {

    private static final String classname = "OpenOrderNameTable: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery) {
        //Log.customer.debug(classname + "query = " + query);
		super.addQueryConstraints(query, field, pattern, searchQuery);
        String condition = Fmt.S("CloseOrder = true");
        Log.customer.debug("**%s : Condition Text = %s", classname, condition);

        query.and(AQLCondition.parseCondition(condition));
        Log.customer.debug("%s Final Query = %s" ,classname, query.toString());

    }

    public OpenOrderNameTable() {
        super();
    }
}


