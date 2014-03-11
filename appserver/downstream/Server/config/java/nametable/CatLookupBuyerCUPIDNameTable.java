/*
 * Created by PGS  on Aug 31, 2006
 * --------------------------------------------------------------
 * Nametable for buyer chooser in Add buyer CUPID in Supplier Eform.
 *
*/

package config.java.nametable;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.log.Log;

public class CatLookupBuyerCUPIDNameTable extends AQLNameTable {

AQLQuery buyerQuery;

    public AQLQuery buildQuery(String field, String pattern) {

		Log.customer.debug(classname + " *** Field IS: " + field);
		Log.customer.debug(classname + " *** pattern IS: " + pattern);

		String queryText = "Select bc from cat.core.BuyerCode bc where AdapterSource = 'ibm_buyercode'";


        buyerQuery = AQLQuery.parseQuery(queryText);
        buyerQuery = super.buildQuery(buyerQuery, field, pattern, null);
        Log.customer.debug(classname + " *** buyerQuery= " + buyerQuery);

        Log.customer.debug(classname + " *** CatLookupBuyerCUPIDNameTable : Final Query = " + buyerQuery);

        return buyerQuery;

    }

    public CatLookupBuyerCUPIDNameTable() {}

    private static final String classname = "CatLookupBuyerCUPIDNameTable : ";
}
