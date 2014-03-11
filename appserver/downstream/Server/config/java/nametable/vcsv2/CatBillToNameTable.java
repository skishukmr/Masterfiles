/*
 * Created by KS on April 22, 2005
 * -------------------------------------------------------------------------------
 * Nametable that leverages generic vcsv2 address nametable to limit values of Billing Address
 */
package config.java.nametable.vcsv2;

import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatBillToNameTable extends CatAddressNameTable {

    private static final String THISCLASS = "CatBillToAddressNameTable"; 
    private static final String KeyFile = "cat.java.vcsv2";
    private static final String KeyField = "AddressNameTableValues_BillTo";
    
    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
        
        if(CatConstants.DEBUG)
            Log.customer.debug("%s *** In NAMETABLE!", THISCLASS);
        super.setLookupValues(KeyFile, KeyField);
        super.addQueryConstraints(query, field, pattern, searchQuery);
    }
    
    public CatBillToNameTable() {
        super();
    }
    
}


