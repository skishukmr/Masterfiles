/*
 * Created by KS on April 18, 2005
 * -------------------------------------------------------------------------------
 * Restricts AccountTypes displayed in chooser based on partition  
 * Needed since AccountTypes defined in Plain, but different values apply in each partition
 */
package config.java.nametable;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatAccountTypeSearchNameTable extends AQLNameTable {

    private static final String THISCLASS = "CatAccountTypeNameTable"; 
    
    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
      
        super.addQueryConstraints(query, field, pattern, searchQuery);
        StringBuffer sb1 = new StringBuffer("AccountTypeNameTableValues_");
        sb1.append(Base.getSession().getPartition().getName());
        if (CatConstants.DEBUG) 
            Log.customer.debug("%s *** sb1: %s", THISCLASS, sb1.toString());
        String AccountTypeValues = ResourceService.getString("cat.java.common",sb1.toString());
        if (AccountTypeValues != null) {
            String [] types = StringUtil.delimitedStringToArray(AccountTypeValues,',');
            if (types != null) {
                StringBuffer sb2 = new StringBuffer();
                int i = types.length;
                while (i-1 >= 0){
                    sb2.append("'").append(types[i-1]).append("'");
                    if (i-1>0)
                        sb2.append(",");
                    i--;
                }
                String conditionText = Fmt.S("UniqueName IN (%s)", sb2.toString());
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** sb2: %s", THISCLASS, conditionText);
                if (conditionText != null) 
                    query.and(AQLCondition.parseCondition(conditionText));
            }
        }
    if (CatConstants.DEBUG)
        Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }          	

    public CatAccountTypeSearchNameTable() {
        super();
    }
}