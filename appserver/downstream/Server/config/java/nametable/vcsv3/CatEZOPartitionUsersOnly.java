/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	11/29/2006
	Description: 	Created filter for MileStone Verifier so as to only return
					Partitioned Users in VCSV3.
-------------------------------------------------------------------------------
	Change Author:
	Date Modified:
	Description:
******************************************************************************/

package config.java.nametable.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOPartitionUsersOnly extends AQLNameTable
{
	private static final String classname = "CatEZOPartitionUsersOnly";
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		Log.customer.debug("%s ::: In DeptApproverNametable", classname);
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s ::: Query FROM Class: %s ", classname, query.getFirstClassAlias());
		Log.customer.debug("%s ::: Original Query: %s ", classname, query);
		Partition part = Base.getSession().getPartition();

		//Added common.core.User into the query so that non partiitoned user can not be added as an approver
		String buyerUserAlias = "cu";
		AQLClassReference buRef = new AQLClassReference("ariba.common.core.User", buyerUserAlias);
		query.addClass(buRef);
		AQLCondition cond = AQLCondition.parseCondition(StringUtil.strcat(buyerUserAlias, ".User=\"User\""));
		query.and(cond);
		//End Of Added common.core.User into the query so that non partiitoned user can not be added as an approver
	}

	public CatEZOPartitionUsersOnly()
	{
		super();
	}
}