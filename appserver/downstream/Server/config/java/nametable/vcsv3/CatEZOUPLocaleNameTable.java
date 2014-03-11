package config.java.nametable.vcsv3;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatEZOUPLocaleNameTable extends AQLNameTable {

    private static final String ClassName = "CatEZOUPLocaleNameTable";

    protected void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        ValueSource vs = getValueSourceContext();
		//if (Log.customer.debugOn)
			Log.customer.debug("%s ::: ValueSource: %s", ClassName, vs);
		if (vs instanceof User){
			StringBuffer sb1 = new StringBuffer("LocaleNameTableValues");
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: sb1: %s", ClassName, sb1.toString());
			String LocaleNames = ResourceService.getString("cat.java.vcsv3",sb1.toString());
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
		else
			//if (Log.customer.debugOn)
				Log.customer.debug("%s ::: ValueSource not instance Of: %s", ClassName, "ariba.user.core.User");
    }

    public CatEZOUPLocaleNameTable() {
        super();
    }
}