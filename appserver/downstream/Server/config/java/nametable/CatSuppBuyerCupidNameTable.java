/*
 * Created by Chandra on May 18, 2006
 * --------------------------------------------------------------
 * Nametable for BuyerCupid on SupplierLocation
 */
package config.java.nametable;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.log.Log;

public class CatSuppBuyerCupidNameTable extends AQLNameTable {

	private String THISCLASS="CatSuppBuyerCupidNameTable";

    public void addQueryConstraints(AQLQuery query, String field, String pattern) {
        super.addQueryConstraints(query, field, pattern, null);
		Log.customer.debug("%s *** query=%s=" , THISCLASS, query);

		String condition = "\"User\" IN (SELECT distinct BuyerCupid from ariba.common.core.SupplierLocation)";
		AQLCondition cond = AQLCondition.parseCondition(condition);
		query.and(cond);
		Log.customer.debug("%s *** Final query=%s=" , THISCLASS, query);
    }

    public CatSuppBuyerCupidNameTable() {}
}
