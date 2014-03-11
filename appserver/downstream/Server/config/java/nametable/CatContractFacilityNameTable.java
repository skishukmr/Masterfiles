package config.java.nametable;

import ariba.base.core.Base;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

public class CatContractFacilityNameTable extends AQLNameTable
{

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug("%s *** In CatContractFacilityNameTable", "CatContractAvailAreaNameTable");
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Query FROM Class: %s ", "CatContractFacilityNameTable", query.getFirstClassAlias());
        Log.customer.debug("%s *** Original Query: %s ", "CatContractFacilityNameTable", query);
        ValueSource vs = getValueSourceContext();
        ariba.base.core.Partition part = Base.getSession().getPartition();
        Log.customer.debug("%s *** ValueSource / Partition: %s / %s", "CatContractFacilityNameTable", vs, part);
        if(vs instanceof ContractRequest)
        {
            String rUnique = vs.getDottedFieldValue("ContractCountryCode.UniqueName").toString();
            String conditionText = Fmt.S("\"CTRY_CD\" = '%s'", rUnique);
            Log.customer.debug("%s *** Condition Text: %s", "CatContractFacilityNameTable", conditionText);
            if(conditionText != null)
                query.and(AQLCondition.parseCondition(conditionText));
            Log.customer.debug("%s *** Final Query: %s", "CatContractAvailAreaNameTable", query);
        }
    }

    public CatContractFacilityNameTable()
    {
    }

    private static final String classname = "CatContractFacilityNameTable";
}