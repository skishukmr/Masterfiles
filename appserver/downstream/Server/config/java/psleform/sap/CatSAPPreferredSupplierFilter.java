/*
 * CatPreferredSupplierFilter.java
 * Created by Chandra on Aug 10, 2005
 * Change Log:
 * Date				Developer		 Issue#	Description
 * 18/05/2010		Vikram J Singh	 1101	Removal of bugs in Update and Delete functionalities in PSL eforms
 *
 */

package config.java.psleform.sap;

import ariba.base.core.aql.*;
import ariba.util.core.*;
import ariba.util.log.Log;

/*
 *  Nametable to select only preferred suppliers.
 */
public class CatSAPPreferredSupplierFilter extends AQLNameTable
{
    private static final String classname = "CatSAPPreferredSupplierFilter: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug(classname + "firing... ");

        super.addQueryConstraints(query, field, pattern, searchQuery);
        //Log.customer.debug(classname + "query = " + query);
        String conditionText = "";

         /*
		  *  ARajendren Ariba, Inc.,
		  *  Modified conditionText based on the query class
		  *
		  */

		Log.customer.debug(classname + "query.getFirstClass().getSimpleName() - " + query.getFirstClass().getSimpleName());

		if(query.getFirstClass().getSimpleName().equals("Supplier")) {
			conditionText = Fmt.S("Locations.PreferredSupplier = true");
		}else if(query.getFirstClass().getSimpleName().equals("SupplierLocation")) {
			conditionText = Fmt.S("PreferredSupplier = true");
        } else {
			Log.customer.debug(classname + "UNKNOWN query class, add no condition to existing query");
			return;
		}

        Log.customer.debug(classname + "Condition Text = " + conditionText);
        query.and(AQLCondition.parseCondition(conditionText));
        Log.customer.debug(classname + "Final Query = " + query);
    }

    public CatSAPPreferredSupplierFilter()
    {
        super();
    }
}
