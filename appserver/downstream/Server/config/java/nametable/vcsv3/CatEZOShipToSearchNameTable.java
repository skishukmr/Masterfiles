/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/02/2006
	Description: 	Filter for the shipTo field on Search Page.  The nametable,
					filters and allows system defined addresses and custom
					addresses created by the user logged in.
-------------------------------------------------------------------------------
	Change Author:	Dharmang J. Shelat
	Date Modified:	12/05/2006
	Description:	Added a condition to compensate for the ReceivingFacility
					being blank.
******************************************************************************/

package config.java.nametable.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.util.core.Fmt;
import ariba.util.log.Log;

public class CatEZOShipToSearchNameTable extends AQLNameTable
{
	private static final String ClassName = "CatEZOShipToSearchNameTable";
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		Log.customer.debug("%s ::: In DeptApproverNametable", ClassName);
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s ::: Query FROM Class: %s ", ClassName, query.getFirstClassAlias());
		Log.customer.debug("%s ::: Original Query: %s ", ClassName, query);
		ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();

		String conditionText = "ReceivingFacility is not null AND ReceivingFacility <> ''";
		query.and(AQLCondition.parseCondition(conditionText));

		String userId = Base.getSession().getEffectiveUserId().toDBString();
		Log.customer.debug("%s ::: User baseId: %s", ClassName, userId);
		conditionText = Fmt.S("Creator = baseId('%s')",userId);
		query.or(AQLCondition.parseCondition(conditionText));

		Log.customer.debug("%s ::: Final Query: %s", ClassName, query);
	}
	public CatEZOShipToSearchNameTable()
	{
		super();
	}
}