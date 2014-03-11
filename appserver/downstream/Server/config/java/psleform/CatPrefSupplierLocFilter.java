/*
 * CatPrefSupplierLocFilter.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.Supplier;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/*
 *  Nametable to select only the locations of the supplier selected.
 */
public class CatPrefSupplierLocFilter extends AQLNameTable
{
    private static final String classname = "CatPrefSupplierLocFilter: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug(classname + "firing... ");

        ValueSource valuesrc = getValueSourceContext();
        //Log.customer.debug("**%s : valuesource is "+ valuesrc, classname);

        if (valuesrc != null ) {
            Supplier supplier = null;
            String maintType = (String)valuesrc.getFieldValue("MaintenanceType");
            //Log.customer.debug("**%s : maintenance type is ", classname, maintType);
            if (maintType.equals("Create")) {
                supplier= (Supplier)valuesrc.getFieldValue("PreferredSupplierToCreate");
            } else if (maintType.equals("Update")) {
                supplier = (Supplier)valuesrc.getFieldValue("PreferredSupplierToUpdate");
            }
            //Log.customer.debug("**%s : supplier got =" +supplier, classname);

            if(supplier != null) {
                BaseId supBaseid= supplier.getBaseId();
                super.addQueryConstraints(query, field, pattern, searchQuery);
                //Log.customer.debug("**%s : query = %s", classname, query.toString());
                String conditionText = Fmt.S("Supplier = %s", AQLScalarExpression.buildLiteral(supBaseid));

                Log.customer.debug("**%s : Condition Text = %s", classname, conditionText);
                query.and(AQLCondition.parseCondition(conditionText));
            }
        }
        Log.customer.debug("**%s : Final Query = %s", classname, query.toString());
    }

    public CatPrefSupplierLocFilter()
    {
        super();
    }
}
