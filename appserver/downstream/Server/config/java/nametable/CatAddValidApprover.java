/*
 * Created by Kingshuk on May 18, 2006
 * --------------------------------------------------------------
 * Nametable for Approvar chooser in Add Approver page.
 * Restricts only active partitioned Users to be displayed.
*/


package config.java.nametable;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.log.Log;

public class CatAddValidApprover extends AQLNameTable {

    protected AQLQuery buildQuery(AQLQuery query, String field, String pattern, SearchTermQuery searchTermQuery) {
		String queryText = "SELECT Approver, Approver.Name, Approver.PayrollFacility AS APPPayroll " +
		"FROM ariba.\"user\".core.Approver AS Approver SUBCLASS (ariba.user.core.Role) " +
		"UNION " +
		"SELECT uu AS Approver, uu.Name AS AppName, uu.PayrollFacility AS APPPayroll " +
		"FROM ariba.user.core.User uu, ariba.common.core.User cu " +
		"WHERE cu.\"User\"=uu AND uu.Creator IS NULL";

		Log.customer.debug(classname + " *** Field IS: " + field);
		Log.customer.debug(classname + " *** pattern IS: " + pattern);
		if (searchTermQuery != null)
			Log.customer.debug(classname + " *** searchTermQuery IS: " + searchTermQuery.toString());

		if ( field != null && pattern != null && !pattern.equals("*") )
		{
			String strWhere = field  + " like '" + pattern.replace('*', '%') + "'";
			queryText = "SELECT Approver, Approver.Name, Approver.PayrollFacility AS APPPayroll " +
			"FROM ariba.\"user\".core.Approver AS Approver SUBCLASS (ariba.user.core.Role) " +
			"WHERE " + strWhere + " UNION " +
			"SELECT uu AS Approver, uu.Name AS AppName, uu.PayrollFacility AS APPPayroll " +
			"FROM ariba.user.core.User uu, ariba.common.core.User cu " +
			"WHERE uu." + strWhere + " AND cu.\"User\"=uu AND uu.Creator IS NULL";
			Log.customer.debug(classname + " *** Query String IS: " + queryText);
		}

        query = AQLQuery.parseQuery(queryText);

        Log.customer.debug(classname + " *** CatAddValidApprover : Final Query = " + query);
        return query;
    }
    public CatAddValidApprover()
    {}
    private static final String classname = "CatAddValidApprover : ";
}
