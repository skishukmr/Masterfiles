/*
 * Created by KS on April 22, 2005
 * -------------------------------------------------------------------------------
 * Nametable that leverages generic vcsv2 address nametable to limit values of Billing Address
 * This version used in Procure (contains custom addresses created by the user)
 */
package config.java.nametable.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatShipToNameTable2 extends CatAddressNameTable {

    private static final String THISCLASS = "CatShipToAddressNameTable"; 
    private static final String KeyFile = "cat.java.vcsv2";
    private static final String KeyField = "AddressNameTableValues_ShipTo";
    
    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
        
        super.setConditionText(KeyFile, KeyField);
        query.or(AQLCondition.parseCondition(super.getConditionText()));      
        BaseId realuser = Base.getSession().getRealUserId();
        if (realuser != null) {
            String preText = Fmt.S("Creator = BaseId('%s')", realuser.toDBString());
            query.or(AQLCondition.parseCondition(preText)); 
        } 
        if(CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);        
    }
    
    public CatShipToNameTable2() {
        super();
    } 
}