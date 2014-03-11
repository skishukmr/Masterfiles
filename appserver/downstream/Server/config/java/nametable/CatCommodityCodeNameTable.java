package config.java.nametable;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.Fmt;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatCommodityCodeNameTable extends AQLNameTable
{

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        super.addQueryConstraints(query, field, pattern, searchQuery);
        if(CatConstants.DEBUG)
            Log.customer.debug("%s *** SUPER's Query %s", "CatCommodityCodeNameTable", query);
        String conditionText = "CommodityCode IN (SELECT CommodityCode FROM %s PARTITION %s)";
        Partition part = Base.getSession().getPartition();
        conditionText = Fmt.S(conditionText, "ariba.common.core.CommodityExportMapEntry", part.getName());
        if(CatConstants.DEBUG)
            Log.customer.debug("%s *** conditionText: %s", "CatCommodityCodeNameTable", conditionText);
        query.and(AQLCondition.parseCondition(conditionText));
        if(CatConstants.DEBUG)
            Log.customer.debug("%s *** Final Query: %s", "CatCommodityCodeNameTable", query);
    }

    public CatCommodityCodeNameTable()
    {
    }

    private static final String THISCLASS = "CatCommodityCodeNameTable";
    private static final String CEMEclass = "ariba.common.core.CommodityExportMapEntry";
}
