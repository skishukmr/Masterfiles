/*
 * Created by KS on April 26, 2005
 * ---------------------------------------------------------------------------------
 * Used to filter Accounting Field (only Account, CostCenter or Project) based on FacilityCode
 */
package config.java.nametable.vcsv2;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.Accounting;
import ariba.common.core.SplitAccounting;
import ariba.user.core.User;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatMFGAccountingNameTable extends AQLNameTable {

	private static final String THISCLASS = "CatMFGAccountingNameTable";
	private static final String CONDITION = "FacilityCode = ";
	
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);  
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** SUPER Query: %s ", THISCLASS, query);
        ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** ValueSource: %s ", THISCLASS, vs);
        
        if (vs instanceof SplitAccounting) {
        	SplitAccounting sa = (SplitAccounting)vs;
        	ClusterRoot facility = (ClusterRoot)sa.getFieldValue("Facility");
            if (CatConstants.DEBUG)
                Log.customer.debug("%s *** Facility (from SplitAcctng): %s ", THISCLASS, facility);
        	if (facility == null) {
            	LineItem li = sa.getLineItem();
        		if (li != null) {
        		    LineItemCollection lic = li.getLineItemCollection();
        		    if (lic != null) {
        		        User requester = lic.getRequester();
        		        ariba.common.core.User puser = ariba.common.core.User.getPartitionedUser(requester, lic.getPartition());
        		        if (puser != null) {
        		            Accounting ua = puser.getAccounting();
        		            if (ua != null) {
        		                facility = (ClusterRoot)ua.getFieldValue("Facility");
        		                if (CatConstants.DEBUG)
        		                    Log.customer.debug("%s *** Facility (from User Acctng): %s ", THISCLASS, facility);        		                
        		            }
        		        }
        		    }
        		}
        	}
        	if (facility != null) {
        	    StringBuffer conditionText = new StringBuffer(CONDITION);
        	    conditionText.append("'");
        	    conditionText.append(facility.getUniqueName());
        	    conditionText.append("'");
	            query.and(AQLCondition.parseCondition(conditionText.toString())); 
	            if (CatConstants.DEBUG)
	                Log.customer.debug("%s *** Final query: %s ", THISCLASS, query); 
        	} else {
	            if (CatConstants.DEBUG)
	                Log.customer.debug("%s *** UNABLE TO SET FACILITY!", THISCLASS); 
        	}
        }
    }
	
	public CatMFGAccountingNameTable() {
		super();
	}
	
	
}
