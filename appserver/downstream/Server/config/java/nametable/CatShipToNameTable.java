/*
 * Created by KS on Dec 09, 2004
 */
package config.java.nametable;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.log.Log;

public class CatShipToNameTable extends AQLNameTable {

	private static final String classname = "CatShipToNameTable";
	
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug("%s *** In FOBPoint Nametable", classname);
        super.addQueryConstraints(query, field, pattern, searchQuery);        
        Log.customer.debug("%s *** Original Query: %s ", classname, query);
        
		String conditionText = "(ReceivingFacility is not null or Creator is not null)";	      	
   		query.and(AQLCondition.parseCondition(conditionText));     
        Log.customer.debug("%s *** Final Query: %s", classname, query);     
    }
	
	public CatShipToNameTable() {
		super();
	}
	
}
