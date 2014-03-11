/*
 * Created by KS on Dec 09, 2004
 */
package config.java.nametable;

import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.procure.core.*;
import ariba.common.core.*;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.base.fields.*;

public class CatFOBPointNameTable extends AQLNameTable {

	private static final String classname = "CatFOBPointNameTable";
	
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug("%s *** In FOBPoint Nametable", classname);
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Query FROM Class: %s ", classname, query.getFirstClassAlias());        
        Log.customer.debug("%s *** Original Query: %s ", classname, query);
        ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
        Log.customer.debug("%s *** ValueSource / Partition: %s / %s", classname, vs, part);	
        
        if (vs instanceof ProcureLineItem) {
        	ProcureLineItem pli = (ProcureLineItem)vs;
           	Address shipto = pli.getShipTo();        	
        	SupplierLocation loc = pli.getSupplierLocation();
        	String conditionText = null;
        	if (shipto != null) 
               	conditionText = Fmt.S("UniqueName = '%s'", shipto.getUniqueName());         
        	if (loc != null) {
        		Address fob = (Address)loc.getFieldValue("FOBPoint");
        		if (fob != null) {
                  	conditionText = Fmt.S("UniqueName = '%s'", fob.getUniqueName());   			
                  	if (shipto != null)
                  		conditionText = Fmt.S("UniqueName IN ('%s','%s')", fob.getUniqueName(), shipto.getUniqueName());
        		}  	
        	}		      	
        	Log.customer.debug("%s *** Condition Text: %s", classname, conditionText);
        	if (conditionText != null)
        		query.and(AQLCondition.parseCondition(conditionText));     
            Log.customer.debug("%s *** Final Query: %s", classname, query);     
        }
    }
	
	public CatFOBPointNameTable() {
		super();
	}
	
	public CatFOBPointNameTable(String arg0, boolean arg1) {
		super(arg0, arg1);
	}	
	
}
