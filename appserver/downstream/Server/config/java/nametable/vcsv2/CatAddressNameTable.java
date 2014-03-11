/*
 * Created by KS on April 22, 2005
 * -------------------------------------------------------------------------------
 * Generic address nametable class to be leveraged by field-specific nametables (e.g., ShipTo, BillTo)
 * containing only a small set of values (read in as String from CSV file)
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

public class CatAddressNameTable extends AQLNameTable {

    private static final String THISCLASS = "CatAddressNameTable";
    private String ValuesFile = null;
    private String ValuesKey = null;
    private String ConditionText = null;

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){

        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** OOB Query %s", THISCLASS, query);
        super.addQueryConstraints(query, field, pattern, searchQuery);
        if (CatConstants.DEBUG) {
            Log.customer.debug("%s *** SUPER's Query %s", THISCLASS, query);
            Log.customer.debug("%s *** values_key: %s", THISCLASS, ValuesKey);
        }
        if (ValuesFile != null && ValuesKey != null) {
            setConditionText(ValuesFile, ValuesKey);
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** conditionText: %s", THISCLASS, ConditionText);
            if (ConditionText != null)
                query.and(AQLCondition.parseCondition(ConditionText));
        }
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }

    protected void setConditionText(String filename, String keyname) {
        if (filename != null && keyname != null){
            String AddressValues = ResourceService.getString(filename, keyname);
            if (AddressValues != null) {
                String [] addresses = StringUtil.delimitedStringToArray(AddressValues,',');
                if (CatConstants.DEBUG)
                    Log.customer.debug("%s *** addresses array: %s", THISCLASS, addresses);
                if (addresses != null) {
                    StringBuffer sb = new StringBuffer();
                    int i = addresses.length;
                    while (i-1 >= 0){
                        sb.append("'").append(addresses[i-1]).append("'");
                        if (i-1>0)
                            sb.append(",");
                        i--;
                    }
                    ConditionText = Fmt.S("UniqueName IN (%s)", sb.toString());
                }
            }
        }
    }

    protected void setLookupValues(String filename, String keyname){
        ValuesFile = filename;
        ValuesKey = keyname;
    }

    public String getValuesKey(){
        return ValuesKey;
    }

    public String getConditionText() {
        return ConditionText;
    }

    public CatAddressNameTable() {
        super();
    }
}