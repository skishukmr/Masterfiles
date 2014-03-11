/*
 * Created by KS on May 7, 2005
 * -------------------------------------------------------------------------------
 * Filters Facility clusterroots to include only Accounting Facilities
 * Used in places where the Accounting.Facility field can be edited
 */
package config.java.nametable.vcsv2;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatMFGFacilityNameTable extends AQLNameTable {

    private static final String THISCLASS = "CatMFGFacilityNameTable"; 
    private static final String FACILITIES = ResourceService.getString("cat.java.vcsv2","MFGFacilityNameTableValues");
 
    
    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
      
        super.addQueryConstraints(query, field, pattern, searchQuery);
        if (CatConstants.DEBUG) 
            Log.customer.debug("%s *** SUPER's Query %s", THISCLASS, query);        
        if (FACILITIES != null){
            String [] values = StringUtil.delimitedStringToArray(FACILITIES,',');
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** Facilities array: %s", THISCLASS, values);
            if (values != null) {
                StringBuffer sb = new StringBuffer();
                int i = values.length;
                while (i-1 >= 0){
                    sb.append("'").append(values[i-1]).append("'");
                    if (i-1>0)
                        sb.append(",");
                    i--;
                }  
                String conditionText = Fmt.S("UniqueName IN (%s)", sb.toString());    	            
    	        query.and(AQLCondition.parseCondition(conditionText));        	            
            }
        }
        
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }  

    public CatMFGFacilityNameTable() {
        super();
    }
}