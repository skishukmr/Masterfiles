/*
 * Created by PGS  on Nov 03, 2006
 * --------------------------------------------------------------
 * Nametable for User chooser in Add User for CUPID Migration.
 *
*/


package config.java.nametable;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.log.Log;

public class CatLookupToUserNameTable extends AQLNameTable {

AQLQuery userQuery;

    public AQLQuery buildQuery(String field, String pattern) {

		Log.customer.debug(classname + " *** Field IS: " + field);
		Log.customer.debug(classname + " *** pattern IS: " + pattern);

		String queryText = "SELECT uu FROM ariba.user.core.User uu, ariba.common.core.User cu "	+ "WHERE cu.\"User\"=uu";
		//String queryText = "SELECT uu FROM ariba.user.core.User uu";

        /**
		//Added common.core.User into the query so that non partiitoned user can not be added
		String buyerUserAlias = "cu";
		AQLClassReference buRef = new AQLClassReference("ariba.common.core.User", buyerUserAlias);
		userQuery.addClass(buRef);
		AQLCondition cond = AQLCondition.parseCondition(StringUtil.strcat(buyerUserAlias, ".User=\"User\""));
		userQuery.and(cond);
		//End Of Added common.core.User into the query so that non partiitoned user can not be added
        */

        userQuery = AQLQuery.parseQuery(queryText);
        userQuery = super.buildQuery(userQuery, field, pattern, null);
        Log.customer.debug(classname + " *** userQuery= " + userQuery);

        Log.customer.debug(classname + " *** CatLookupToUserNameTable : Final Query = " + userQuery);

        return userQuery;

    }

    public CatLookupToUserNameTable() {}

    private static final String classname = "CatLookupToUserNameTable : ";
}
