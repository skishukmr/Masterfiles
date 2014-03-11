/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	09/17/2006
	Description: 	Created filter for the Department Approver Chooser to
					restrinct the selection to users in CSARL partition and 
					excluding the requisitioner from the results.
-------------------------------------------------------------------------------
	Change Author: 	
	Date Created:  	
	Description: 	
******************************************************************************/

package config.java.nametable.vcsv3;

import ariba.approvable.core.LineItem;
import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLClassReference;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZODeptApproverNameTable extends AQLNameTable
{
	private static final String classname = "CatCSARLDeptApproverNameTable";
	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
	{
		Log.customer.debug("%s ::: In DeptApproverNametable", classname);
		super.addQueryConstraints(query, field, pattern, searchQuery);
		Log.customer.debug("%s ::: Query FROM Class: %s ", classname, query.getFirstClassAlias());
		Log.customer.debug("%s ::: Original Query: %s ", classname, query);
		ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
		if (vs instanceof SplitAccounting)
		{
			// Must now add Requester exclusion to query condition (Requester can't approve own requisition)
			// Note: Production tests showed do not need pcsv1 subquery 
			//		All ready there on OOB query that restricts the query to partition users
			SplitAccounting sa = (SplitAccounting) vs;
			LineItem li = sa.getLineItem();
			if (li instanceof ProcureLineItem)
			{
				ProcureLineItemCollection plic = (ProcureLineItemCollection) li.getLineItemCollection();
				if (plic != null && plic.getRequester() != null)
				{
					String rUnique = plic.getRequester().getUniqueName();
					//Added common.core.User into the query so that non partiitoned user can not be added as an approver
					String buyerUserAlias = "cu";
					AQLClassReference buRef = new AQLClassReference("ariba.common.core.User", buyerUserAlias);
					query.addClass(buRef);
					AQLCondition cond = AQLCondition.parseCondition(StringUtil.strcat(buyerUserAlias, ".User=\"User\""));
					query.and(cond);
					//End Of Added common.core.User into the query so that non partiitoned user can not be added as an approver
					String conditionText = Fmt.S("\"User\".UniqueName <> '%s'", rUnique);
					Log.customer.debug("%s ::: Condition Text: %s", classname, conditionText);
					if (conditionText != null)
						query.and(AQLCondition.parseCondition(conditionText));
					Log.customer.debug("%s ::: Final Query: %s", classname, query);
				}
			}
		}
	}
	public CatEZODeptApproverNameTable()
	{
		super();
	}
}