package config.java.nametable.sap;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

//Created By Nag for displaying Companycodes

public class CatSAPSUPCOMNameTable extends AQLNameTable {

    private static final String ClassName = "CatSAPSUPCOMNameTable";

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
					String conditionText = Fmt.S("SAPSource not like  %s","'%TradingPartner%'" );
					String conditionText1 = Fmt.S("SAPSource is not null" );
					//if (Log.customer.debugOn)
						Log.customer.debug("%s ::: sb2: %s", ClassName, conditionText);

                    if (conditionText != null)
						query.and(AQLCondition.parseCondition(conditionText));
					if (conditionText1 != null)
					     query.and(AQLCondition.parseCondition(conditionText1));
				Log.customer.debug("%s ::: Final Query: %s", ClassName, query);
    }

    public CatSAPSUPCOMNameTable() {
        super();
    }
}