/*
 * Created by PGS  on Nov 03, 2006
 * --------------------------------------------------------------
 * Nametable for User chooser in Add User for CUPID Migration.
 *
*/


package config.java.migrateeform;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.log.Log;

public class CatLookupFromUserNameTable extends AQLNameTable {


    public AQLQuery buildQuery(String field, String pattern) {

		AQLQuery userQuery;

		Log.customer.debug(classname + " *** Field IS: " + field);
		Log.customer.debug(classname + " *** pattern IS: " + pattern);


		String queryText = "SELECT uu "
					+ "FROM ariba.user.core.User uu INCLUDE INACTIVE, "
					+ "ariba.common.core.User cu INCLUDE INACTIVE "
					+ "WHERE cu.\"User\"=uu ";

        userQuery = AQLQuery.parseQuery(queryText);
        userQuery = super.buildQuery(userQuery, field, pattern, null);
        Log.customer.debug(classname + " *** userQuery= " + userQuery);

        Log.customer.debug(classname + " *** CatLookupFromUserNameTable : Final Query = " + userQuery);

        return userQuery;

    }

    public CatLookupFromUserNameTable() {}

    private static final String classname = "CatLookupFromUserNameTable : ";
}
