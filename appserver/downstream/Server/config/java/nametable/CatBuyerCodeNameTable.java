/*
 * Created by KS for R1
 * Updated on Feb 3, 2005 to restrict BuyerCodes to certain facilities (parameter)
 */
package config.java.nametable;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatBuyerCodeNameTable extends AQLNameTable {

    private static final String classname = "CatBuyerCodeNameTable";
    private static final String FACILITY_LIST = "Application.Caterpillar.Procure.FilteredBuyerCodeFacilities";    
    
    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery){
      
        Log.customer.debug("%s *** IN BUYERCODE NAMETABLE",classname);
        super.addQueryConstraints(query, field, pattern, searchQuery);
        ValueSource vs = getValueSourceContext();
//      Log.customer.debug("%s *** ValueSource: %s", classname, vs);	       
        if (vs instanceof ReqLineItem) {
            ReqLineItem rli = (ReqLineItem)vs;
			String param = Base.getService().getParameter(rli.getPartition(), FACILITY_LIST);
			Log.customer.debug("%s *** param: %s", classname, param);
	        if (!StringUtil.nullOrEmptyOrBlankString(param)) {
          		String [] facilities = StringUtil.delimitedStringToArray(param,',');
         		Log.customer.debug("CatBuyerCodeNameTable *** string array: " + facilities);          		
          		if (facilities != null){
        			StringBuffer sb = new StringBuffer();
              		int size = facilities.length;
              		for (int i = size-1; i>=0;i--) {
              		    sb.append("'").append(facilities[i]).append("'");
              		    if (i>0)
              		        sb.append(",");
               		}
  //           		Log.customer.debug("CatBuyerCodeNameTable *** final sb: " + sb);
    	            String conditionText = Fmt.S("AccountingFacility IN (%s)", sb.toString());
              		Log.customer.debug("%s *** Condition Text: %s", classname, conditionText);    
                	if (conditionText != null)
                		query.and(AQLCondition.parseCondition(conditionText));     
  //                Log.customer.debug("%s *** Final Query: %s", classname, query);        		    
          		}       		
    		}  	
    	}		      	
    }

    public CatBuyerCodeNameTable() {
        super();
    }

}
