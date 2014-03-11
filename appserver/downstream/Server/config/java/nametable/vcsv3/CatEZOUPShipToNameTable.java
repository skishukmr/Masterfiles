/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/17/2006
	Description: 	Filter for the shipTo field on the User Profile Page.  The
					nametable, filters and allows only system defined addresses
					in the field.
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
import ariba.common.core.UserProfileDetails;
import ariba.util.log.Log;

public class CatEZOUPShipToNameTable extends AQLNameTable
{
	private static final String classname = "CatEZOUPShipToNameTable";
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		Log.customer.debug("%s ::: In DeptApproverNametable", classname);
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s ::: Query FROM Class: %s ", classname, query.getFirstClassAlias());
		Log.customer.debug("%s ::: Original Query: %s ", classname, query);
		ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
		if (vs instanceof UserProfileDetails){
			//AQLCondition cond = AQLCondition.parseCondition("AdapterSource = 'ezopen:Address.csv'");
			//query.and(cond);
			//String conditionText = "(ReceivingFacility is not null or Creator is not null)";
			String conditionText = "(ReceivingFacility is not null AND ReceivingFacility <> '' AND Creator is null)";
			query.and(AQLCondition.parseCondition(conditionText));
			Log.customer.debug("%s ::: Final Query: %s", classname, query);
		}
	}
	public CatEZOUPShipToNameTable()
	{
		super();
	}
}