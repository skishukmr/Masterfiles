// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 11/1/2005 4:12:34 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)
// Source File Name:   CatContractAvailAreaNameTable.java

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

public class CatContractAvailAreaNameTable extends AQLNameTable
{

    public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug("%s *** In ContractAvailAreaNameTableNametable", "CatContractAvailAreaNameTable");
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Query FROM Class: %s ", "CatContractAvailAreaNameTable", query.getFirstClassAlias());
        Log.customer.debug("%s *** Original Query: %s ", "CatContractAvailAreaNameTable", query);
        ValueSource vs = getValueSourceContext();
        ariba.base.core.Partition part = Base.getSession().getPartition();
        Log.customer.debug("%s *** ValueSource / Partition: %s / %s", "CatContractAvailAreaNameTable", vs, part);
        if(vs instanceof ContractRequest)
        {
            String rUnique = vs.getDottedFieldValue("ContractCountry.UniqueName").toString();
            String conditionText = Fmt.S("\"National\" = '%s'", rUnique);
            Log.customer.debug("%s *** Condition Text: %s", "CatContractAvailAreaNameTable", conditionText);
            if(conditionText != null)
                query.and(AQLCondition.parseCondition(conditionText));
            Log.customer.debug("%s *** Final Query: %s", "CatContractAvailAreaNameTable", query);
        }
    }

    public CatContractAvailAreaNameTable()
    {
    }

    private static final String classname = "CatContractAvailAreaNameTable";
}