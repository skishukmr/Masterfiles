/*
 * Created by KS on Dec 09, 2004
 */
package config.java.nametable;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.log.Log;
import ariba.base.core.*;
import ariba.util.core.Fmt;

public class CatShipToSearchNameTable extends AQLNameTable {

	private static final String classname = "CatShipToNameTable";
	
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);        
        Log.customer.debug("%s *** Original Query: %s ", classname, query);
        
		String conditionText = "ReceivingFacility is not null";	      	
   		query.and(AQLCondition.parseCondition(conditionText));     
   		
        String userId = Base.getSession().getEffectiveUserId().toDBString();
        Log.customer.debug("%s *** User baseId: %s", classname, userId);     
        conditionText = Fmt.S("Creator = baseId('%s')",userId);
        query.or(AQLCondition.parseCondition(conditionText)); 
        
        Log.customer.debug("%s *** Final Query: %s", classname, query);     
    }
	
	public CatShipToSearchNameTable() {
		super();
	}
	
}
