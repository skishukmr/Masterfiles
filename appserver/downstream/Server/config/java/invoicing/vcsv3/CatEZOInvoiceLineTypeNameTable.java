
package config.java.invoicing.vcsv3;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;

/**
 * @author kstanley
 * Removes Discount (32) from available choices
 */

public class CatEZOInvoiceLineTypeNameTable extends AQLNameTable {

    public static final String ClassName = "CatEZOInvoiceLineTypeNameTable";

    public void init(String className, boolean classIsLeaf, String description)
    {
        super.init("ariba.procure.core.ProcureLineType", classIsLeaf, description);
    }

    public void setClassName(String className)
    {
        super.setClassName("ariba.procure.core.ProcureLineType");
    }

    public void addQueryConstraints(AQLQuery query, String field, String pattern)
    {
        super.addQueryConstraints(query, field, pattern, null);

 //       java.util.List categories = ListUtil.list(Constants.getInteger(16), Constants.getInteger(32));
        java.util.List categories = ListUtil.list(Constants.getInteger(16));
        query.andIn("Category", categories);

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** Final Query: %s", ClassName, query);
    }

    public CatEZOInvoiceLineTypeNameTable()
    {
    }


}
