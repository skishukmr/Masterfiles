/* Created by KS on Mar 03, 2006
 * ----------------------------------------------------
 * Allows sub-contract to be tied to any other contract (regardless of owner)
 */
package config.java.nametable.vcsv1;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.util.log.Log;

public class CatParentContractNameTable extends AQLNameTable {

	private static final String classname = "CatParentContractNameTable";

	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Original Query: %s ", classname, query);

        ValueSource vs = getValueSourceContext();
        Log.customer.debug("%s *** ValueSource: %s ", classname, vs);

        if (vs instanceof ContractRequest) {

        	ContractRequest mar = (ContractRequest)vs;
            query.and(AQLCondition.parseCondition("HierarchicalType <> 0"));
            query.and(AQLCondition.parseCondition("StatusString IN ('Open','Processed')"));

            String conditionText = null;
            int type = mar.getTermType();
            if (type == 0)
                conditionText = "TermType = 0";
            else if (type == 1)
                conditionText = "TermType IN (0,1)";

            if (conditionText != null)
                query.and(AQLCondition.parseCondition(conditionText));

            Log.customer.debug("%s *** Final Query: %s", classname, query);
        }
    }

	public CatParentContractNameTable() {
		super();
	}

}
