package config.java.invoicing.vcsv2;

import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import java.util.List;
import ariba.util.core.ListUtil;
/*
 *    SDey, Ariba,Inc - 9r1 OOTB Code shows only DiscountCategory.
 *    This nametable is to show SpecialChargeCategory along with DiscountCategory.
 */
public class CatMFGInvoiceLineTypeNameTable extends AQLNameTable
{

    public static final String ClassName =
        "config.java.invoicing.vcsv2.CatMFGInvoiceLineTypeNameTable";


    public void init (String className, boolean classIsLeaf, String description)
    {
        super.init(ProcureLineType.ClassName, classIsLeaf, description);
    }


    public void setClassName (String className)
    {
        super.setClassName(ProcureLineType.ClassName);
    }


    public void addQueryConstraints (AQLQuery query, String field, String pattern)
    {
            // Add the default field constraints for the pattern
        super.addQueryConstraints(query, field, pattern);

            // Only include categories relevant to statement adjustments
        List categories =
            ListUtil.list(Constants.getInteger(ProcureLineType.SpecialChargeCategory),
                        Constants.getInteger(ProcureLineType.DiscountCategory));
        query.andIn(ProcureLineType.KeyCategory, categories);
    }
}

