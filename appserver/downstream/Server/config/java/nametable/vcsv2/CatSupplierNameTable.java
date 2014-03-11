/*
 * Created by KS on Dec 09, 2004
 */
package config.java.nametable.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatSupplierNameTable extends AQLNameTable {

	private static final String THISCLASS = "CatSupplierNameTable";
	private static final String SUPLCLASS = "ariba.common.core.Supplier";
	private static final String [] DXarray = { "DX","MX","MY"}; 
	private static final String KEYFIELD = "Locations.Authorized";
	
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);  
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** SUPER Query: %s ", THISCLASS, query);
        ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** ValueSource: %s ", THISCLASS, vs);
        
        if (vs instanceof ReqLineItem) {
        	ReqLineItem rli = (ReqLineItem)vs;	
        	Requisition r = (Requisition)rli.getLineItemCollection();
        	if (r != null) {
        	    User requester = r.getRequester();
        	    String afac = (String)requester.getFieldValue("AccountingFacility");
        	    if (afac != null){
        	        StringBuffer conditionText = null;
        	        if (afac.equals("NA")) {
        	            conditionText = new StringBuffer(KEYFIELD);
        	            conditionText.append("NA = true");     	            
        	        }       	        
        	        else if (afac.equals("DX")) {
        	            conditionText = new StringBuffer();
        	            int length = DXarray.length;
        	            for (int i=0; i<length; i++) {
        	                conditionText.append(KEYFIELD);
        	                conditionText.append(DXarray[i]);
        	                conditionText.append(" = true");
        	                if (i < length-1)
        	                    conditionText.append(" OR ");
        	            }
        	        }        	        
        	        if (conditionText != null) {
           	            if (CatConstants.DEBUG)
        	                Log.customer.debug("%s *** SB Text: %s ", THISCLASS, conditionText.toString());        	            
        	            query.and(AQLCondition.parseCondition(conditionText.toString())); 
        	            if (CatConstants.DEBUG)
        	                Log.customer.debug("%s *** Final query: %s ", THISCLASS, query);        	            
        	        }
        	    }   
        	}
        }
    }
	
	public CatSupplierNameTable() {
		super();
	}
	
	
}
