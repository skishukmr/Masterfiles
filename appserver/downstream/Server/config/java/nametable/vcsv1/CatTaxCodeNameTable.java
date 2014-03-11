/* Created by KS on Nov 30, 2005
 * -----------------------------
 * Filters out DFLT TaxCode which is only used by Tax interface logic
 */
package config.java.nametable.vcsv1;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatTaxCodeNameTable extends AQLNameTable {

	private static final String classname = "CatTaxCodeNameTable";
	private static String DefaultTaxCode = ResourceService.getString("cat.java.vcsv1","Tax_DefaultTaxCode");

	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        StringBuffer fsb = new StringBuffer();
        fsb.append("UniqueName <> '");
        fsb.append(DefaultTaxCode);
        fsb.append("'");

   		query.and(AQLCondition.parseCondition(fsb.toString()));
        Log.customer.debug("%s *** Final Query: %s", classname, query);
    }

	public CatTaxCodeNameTable() {
		super();
	}

}
