/*
 * Created by Chandra on Nov 03, 2006
 * --------------------------------------------------------------
 * Nametable for Approvar chooser in Add Approver page.
 * Restricts only active partitioned Users to be displayed.
*/


package config.java.nametable;

import ariba.base.core.aql.AQLClassUnion;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.log.Log;

public class CatAddValidApproverNew extends AQLNameTable {

    public AQLQuery buildQuery(String field, String pattern) {

		Log.customer.debug(classname + " *** Field IS: " + field);
		Log.customer.debug(classname + " *** pattern IS: " + pattern);

		String queryText = "SELECT uu FROM ariba.user.core.User uu, ariba.common.core.User cu "
							+ "WHERE cu.\"User\"=uu";

        AQLQuery userQuery = AQLQuery.parseQuery(queryText);
        userQuery = super.buildQuery(userQuery, field, pattern, null);
        Log.customer.debug(classname + " *** userQuery= " + userQuery);



        AQLQuery roleQuery = new AQLQuery("ariba.user.core.Role");
        roleQuery = super.buildQuery(roleQuery, field, pattern, null);

        ariba.base.core.aql.AQLClassExpression roleAndUserQueries = new AQLClassUnion(AQLQuery.buildSubquery(userQuery), 1, AQLQuery.buildSubquery(roleQuery));
        AQLQuery wholeQuery = new AQLQuery(roleAndUserQueries);

        Log.customer.debug(classname + " *** CatAddValidApproverNew : Final Query = " + wholeQuery);

        return wholeQuery;

    }

    public CatAddValidApproverNew() {}

    private static final String classname = "CatAddValidApproverNew : ";
}
