/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/17/2006
	Description: 	Allows sub-contract to be tied to any other contract
					(regardless of owner).
-------------------------------------------------------------------------------
	Change Author:
	Date Created:
	Description:
******************************************************************************/

package config.java.nametable.vcsv3;

import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.util.log.Log;

public class CatEZOParentContractNameTable extends AQLNameTable {

	private static final String ClassName = "CatEZOParentContractNameTable";

	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s *** Original Query: %s ", ClassName, query);

		ValueSource vs = getValueSourceContext();
		Log.customer.debug("%s *** ValueSource: %s ", ClassName, vs);

		if (vs instanceof ContractRequest) {

			ContractRequest mar = (ContractRequest) vs;
			query.and(AQLCondition.parseCondition("HierarchicalType <> 0"));
			query.and(AQLCondition.parseCondition("StatusString IN ('Open','Processed')"));

			String conditionText = null;
			int type = mar.getTermType();
			if (type == 0)
				conditionText = "TermType = 0";
			else
				if (type == 1)
					conditionText = "TermType IN (0,1)";

			if (conditionText != null)
				query.and(AQLCondition.parseCondition(conditionText));

			Log.customer.debug("%s *** Final Query: %s", ClassName, query);
		}
	}

	public CatEZOParentContractNameTable()
	{
		super();
	}
}