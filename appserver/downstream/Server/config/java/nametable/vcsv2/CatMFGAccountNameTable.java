/*
 * Created by KS on Jun 7, 2005
 * -------------------------------------------------------------------------------
 * Filters Account clusterroots based on AccountType (applicable only for DX facility)
 */
package config.java.nametable.vcsv2;

import ariba.approvable.core.LineItem;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;


public class CatMFGAccountNameTable extends CatMFGAccountingNameTable {

    private static final String THISCLASS = "CatMFGAccountNameTable";
    private static final String CAPITAL = "Capital";
    private static String ACCTKEY = ResourceService.getString("cat.java.vcsv2","CapitalAccountNameTableValues");


    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){

        super.addQueryConstraints(query, field, pattern, searchQuery);
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** SUPER's Query %s", THISCLASS, query);
        ValueSource vs = getValueSourceContext();
        if (vs instanceof SplitAccounting) {
    	SplitAccounting sa = (SplitAccounting)vs;
        	LineItem li = sa.getLineItem();
    		if (li instanceof ProcureLineItem) {
    		    ProcureLineItem pli = (ProcureLineItem)li;
    		    ClusterRoot at = (ClusterRoot)pli.getFieldValue("AccountType");
   		        ClusterRoot facility = (ClusterRoot)sa.getFieldValue("Facility");
    		    if (CatConstants.DEBUG)
    		        Log.customer.debug("%s *** AccountType %s", THISCLASS, at);
    		    if (at != null && facility != null) {
    		        String facUN = facility.getUniqueName();
    		        if (facUN.equals("DX")) {
    		            StringBuffer sb = new StringBuffer();
    		            String [] keys = StringUtil.delimitedStringToArray(ACCTKEY,',');
                        int i = keys.length;
                        if(CAPITAL.equals(at.getUniqueName())) {
	                        while (i-1 >= 0){
	                            sb.append("UniqueName like 'DX").append(keys[i-1]).append("%'");
	                            if (i-1>0)
	                                sb.append(" OR ");
	                            i--;
	                        }
                        } else {
                            while (i-1 >= 0){
	                            sb.append("UniqueName not like 'DX").append(keys[i-1]).append("%'");
	                            if (i-1>0)
	                                sb.append(" AND ");
	                            i--;
	                        }
                        }
	    		        if (CatConstants.DEBUG)
	    		            Log.customer.debug("%s *** conditionText(sb): %s", THISCLASS, sb);
	    		        query.and(AQLCondition.parseCondition(sb.toString()));
    		        }
    		    }
		    }
		}
        if (CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", THISCLASS, query);
    }

    public CatMFGAccountNameTable() {
        super();
    }
}