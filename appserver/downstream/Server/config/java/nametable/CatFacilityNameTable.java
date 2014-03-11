/*
 * CatFacilityNameTable.java
 * Created by Chandra on Aug 30, 2005
 *
 */
package config.java.nametable;

import java.util.List;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLFieldExpression;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLScalarExpression;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.util.core.ListUtil;
import ariba.util.log.Log;

/*
 *  Nametable to select Caterpillar facilities based on the countries
 * selected by user.
 */
public class CatFacilityNameTable extends AQLNameTable
{
    private static final String classname = "CatFacilityNameTable: ";

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug(classname + "firing... ");
        super.addQueryConstraints(query, field, pattern, searchQuery);
        ValueSource valuesrc = getValueSourceContext();
        //Log.customer.debug("%s : valuesource is "+ valuesrc, classname);
        //Log.customer.debug("%s : query in here=%s", classname, query.toString());

        if (valuesrc != null ) {

            List availCntry  = ListUtil.list();
            String vsTypeName = valuesrc.getTypeName();
            Log.customer.debug("%s : ValueSource type name=%s", classname, vsTypeName);

            //Since the same functionality is required in both these forms - using the foll.approach
            if(vsTypeName.equals("ariba.core.PrefSupplierMaintEform")){
                availCntry = (List) valuesrc.getFieldValue("AvailableCountry");
            } else if(vsTypeName.equals("ariba.contract.core.ContractRequest")) {
                availCntry = (List) valuesrc.getFieldValue("ContractCountry");
            }

            if(! ListUtil.nullOrEmptyList(availCntry)) {

                List  scaleExprList = AQLScalarExpression.buildScalarExpressionList(availCntry);

                AQLFieldExpression aqlFieldExpression = new AQLFieldExpression("CatFacility.Country");
                //Log.customer.debug("%s : aqlFieldExpression=%s", classname, aqlFieldExpression.toString());

                AQLCondition aqlCond = AQLCondition.buildIn(aqlFieldExpression, scaleExprList);
                Log.customer.debug("%s : after format conditionTxt=%s", classname, aqlCond.toString());
                query.and(aqlCond);
            }
        }


        Log.customer.debug("**%s : Final Query = %s", classname, query.toString());
    }

    public CatFacilityNameTable()
    {
        super();
    }
}
