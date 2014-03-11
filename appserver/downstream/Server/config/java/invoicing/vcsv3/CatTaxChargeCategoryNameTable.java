
package config.java.invoicing.vcsv3;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLScalarExpression;
// import ariba.procure.core.nametable.TaxChargeCategoryNameTable;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

/** @author kstanley
* 	Restricts returned values for TaxCategory to only Tax and VAT.
*/

public class CatTaxChargeCategoryNameTable extends AQLNameTable {

    private static final String ClassName = "CatTaxChargeCategoryNameTable";
    private static final String Exclusions = " 'SalesTaxCharge','ServiceUseTax' ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern){

        super.addQueryConstraints(query, field, pattern, null);

        query.andEqual("Category", AQLScalarExpression.buildLiteral(2));
        String conditionText = Fmt.S("ProcureLineType.UniqueName NOT IN (%s)", Exclusions);
        if (conditionText != null)
            query.and(AQLCondition.parseCondition(conditionText));

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** Final Query: %s", ClassName, query);
    }

    public boolean getUseQueryCache() {

        if (super.getUseQueryCache()) {
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Switching UseCache() to FALSE!",ClassName);
        }
        return false;
    }

    public CatTaxChargeCategoryNameTable() {
        super();
    }
}