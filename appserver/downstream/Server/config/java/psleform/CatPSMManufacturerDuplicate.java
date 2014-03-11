/*
 * CatPSMManufacturerDuplicate.java
 * Created by Chandra on Aug 10, 2005
 *
 */
package config.java.psleform;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Log;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;

/*
 *  Condition to validate the new Manufacturer being created from ui is not a duplicate
 */
public class CatPSMManufacturerDuplicate extends Condition
{


    public CatPSMManufacturerDuplicate() {}

    public boolean evaluate(Object value, PropertyTable params) {
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {

        if(value != null && !StringUtil.nullOrEmptyOrBlankString(value.toString())) {
            String inputvalue = value.toString().trim().toUpperCase();
            Log.customer.debug("**%s**value received=%s", thisclass, inputvalue);

            String qText = "SELECT upper(Name) from cat.core.Manufacturer "
                                +"WHERE Name like '" + inputvalue +"%' ";

            Log.customer.debug("**%s : query: %s", thisclass, qText);
            AQLQuery query = AQLQuery.parseQuery(qText);

            AQLOptions options = new AQLOptions();
            options.setRowLimit(0);
            options.setUseCache(false);
            options.setUserPartition(Base.getSession().getPartition());

            AQLResultCollection results = Base.getService().executeQuery(query, options);
            //Log.customer.debug("**%s : results: " + results, thisclass);

            if (results.getErrors() == null) {
                if (results.next()) {
                    //Log.customer.debug("**%s : results has next : ");
                    String manfName = results.getString(0);
                    Log.customer.debug("**%s : manfName in db: " + manfName);
                    return new ConditionResult(ResourceService.getString("cat.psm.eform","PSMManufacturerDuplicateMsg"));

                }
            } else {
                Log.customer.debug("**%s : ERROR: results has error: %s", results.getErrorStatementText());
            }
        }
        return null;
    }

    protected ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    protected ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    private static final String thisclass = "CatPSMManufacturerDuplicate";


    protected static final ValueInfo parameterInfo[];
    protected static final ValueInfo valueInfo;

    static
    {
        parameterInfo = (new ValueInfo[] {
            new ValueInfo("Manufacturer", 0, "cat.core.Manufacturer"),
        });
        valueInfo = new ValueInfo(0, Constants.ObjectType, IntType, LongType, DoubleType);
    }
}
