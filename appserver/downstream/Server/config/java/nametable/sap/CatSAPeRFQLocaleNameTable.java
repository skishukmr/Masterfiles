package config.java.nametable.sap;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

//Created By Nagendra

public class CatSAPeRFQLocaleNameTable extends AQLNameTable {

    private static final String ClassName = "CatSAPeRFQLocaleNameTable";

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        ValueSource vs = getValueSourceContext();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: ValueSource: %s", ClassName, vs);
		StringBuffer sb1 = new StringBuffer("LocaleNameTableValuesERFQ");
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: sb1: %s", ClassName, sb1.toString());
		String LocaleNames = ResourceService.getString("cat.java.sap",sb1.toString());
		if (LocaleNames != null) {
			String [] locales = StringUtil.delimitedStringToArray(LocaleNames,',');
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: locale array: %s", ClassName, locales);
			if (locales != null) {
				StringBuffer sb2 = new StringBuffer();
				int i = locales.length;
				while (i-1 >= 0){
					sb2.append("'").append(locales[i-1]).append("'");
					if (i-1>0)
						sb2.append(",");
					i--;
				}
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: sb2: %s", ClassName, sb2.toString());
				String conditionText = Fmt.S("UniqueName IN (%s)", sb2.toString());
				//if (Log.customer.debugOn)
					Log.customer.debug("%s ::: sb2: %s", ClassName, conditionText);
				if (conditionText != null)
					query.and(AQLCondition.parseCondition(conditionText));
			}
		}
		if (CatConstants.DEBUG)
			Log.customer.debug("%s ::: Final Query: %s", ClassName, query);
    }

    public CatSAPeRFQLocaleNameTable() {
        super();
    }
}