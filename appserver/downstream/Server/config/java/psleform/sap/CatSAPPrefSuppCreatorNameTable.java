/*
 * CatPrefSuppCreatorNameTable.java
 * Created by Chandra on Aug 20, 2005
 *
 */
package config.java.psleform.sap;


import ariba.base.core.aql.*;
import ariba.util.core.*;
import ariba.util.log.Log;


/*
 *  Nametable to select preferred supplier creators. Used in Reports to select owner.
 */
public class CatSAPPrefSuppCreatorNameTable
                            extends AQLNameTable {

    private static final String classname = "CatPrefSuppCreatorNameTable: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery) {
        //Log.customer.debug(classname + "query = " + query);

        String condition = Fmt.S("\"User\" IN (SELECT DISTINCT PrefSuppCreator FROM ariba.common.core.SupplierLocation AS SupplierLocation)");
        Log.customer.debug("**%s : Condition Text = %s", classname, condition);

        query.and(AQLCondition.parseCondition(condition));
        Log.customer.debug("%s Final Query = %s" ,classname, query.toString());
    }

    public CatSAPPrefSuppCreatorNameTable() {
        super();
    }
}
