/*
 * Created by KS on April 25, 2005
 * -------------------------------------------------------------------------------
 * Generic approver nametable that adds AND condition to exclude the PLIC Requester
 */
package config.java.nametable;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatDesignatedApproverNameTable extends AQLNameTable {

    private static final String THISCLASS = "CatDesignatedApproverNameTable"; 
    
    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
       
//        super.addQueryConstraints(query, field, pattern, searchQuery);
        if (CatConstants.DEBUG) 
            Log.customer.debug("%s *** OOB Query %s", THISCLASS, query);        
        ValueSource object = getValueSourceContext();
        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            LineItemCollection lic = sa.getLineItem().getLineItemCollection();
            if (lic != null) {
                User requester = lic.getRequester();
                if (requester != null) {
                    String ConditionText = Fmt.S("UniqueName <> '%s'", requester.getUniqueName());
                    if (ConditionText != null) 
                        query.and(AQLCondition.parseCondition(ConditionText));  
                }
            }
        }
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }   

    public CatDesignatedApproverNameTable() {
        super();
    }
}