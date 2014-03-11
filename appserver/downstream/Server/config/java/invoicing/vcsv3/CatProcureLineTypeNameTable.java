
package config.java.invoicing.vcsv3;

import ariba.base.core.BaseObject;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

/**  Author: KS.
Restricts returned values to only those not in string read/parsed from resource CSV file.
*/

public class CatProcureLineTypeNameTable extends AQLNameTable {

    private static final String ClassName = "CatProcureLineTypeNameTable";
    private static final String Exclusions = Fmt.Sil("cat.invoicejava.vcsv3","NameTable_ProcureLineTypeExclusions");

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){

        super.addQueryConstraints(query, field, pattern, searchQuery);
        ValueSource vs = getValueSourceContext();
        if (vs instanceof BaseObject) {
            BaseObject bo = (BaseObject)vs;

            String [] exclusions = StringUtil.delimitedStringToArray(Exclusions,';');
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** types array: %s", ClassName, exclusions);
            if (exclusions != null) {
                StringBuffer sb = new StringBuffer();
                int i = exclusions.length;
                while (i-1 >= 0){
                    sb.append("'").append(exclusions[i-1]).append("'");
                    if (i-1>0)
                        sb.append(",");
                    i--;
                }
                String conditionText = Fmt.S("UniqueName NOT IN (%s)", sb.toString());
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** conditionText: %s", ClassName, conditionText);
                if (conditionText != null)
                    query.and(AQLCondition.parseCondition(conditionText));
            }
    	}
        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** Final Query: %s", ClassName, query);
    }

    public CatProcureLineTypeNameTable() {
        super();
    }
}