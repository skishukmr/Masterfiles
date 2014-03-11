/*
 * Created by KS on Dec 09, 2004
 * Modified by Kingshuk 06-June-2006	//Added common.core.User into the query
 										so that non partiitoned user can not be added as an approver
 */
package config.java.nametable;

import ariba.approvable.core.LineItem;
import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.common.core.*;
import ariba.procure.core.*;
import ariba.util.core.*;
import ariba.util.log.Log;
import ariba.base.fields.*;

public class CatDeptApproverNameTable extends AQLNameTable {

	private static final String classname = "CatDeptApproverNameTable";

	public void addQueryConstraints(AQLQuery query, String field, String pattern, SearchTermQuery searchQuery)
    {
        Log.customer.debug("%s *** In DeptApproverNametable", classname);
        super.addQueryConstraints(query, field, pattern, searchQuery);
        Log.customer.debug("%s *** Query FROM Class: %s ", classname, query.getFirstClassAlias());
 //     Log.customer.debug("%s *** Original Query: %s ", classname, query);
        ValueSource vs = getValueSourceContext();
		Partition part = Base.getSession().getPartition();
 //     Log.customer.debug("%s *** ValueSource / Partition: %s / %s", classname, vs, part);

        if (vs instanceof SplitAccounting) {

//	**** Must now add Requester exclusion to query condition (Requester can't approve own requisition)
//	**** Note: Production tests showed do not need pcsv1 subquery (all ready there on OOB query)
            SplitAccounting sa = (SplitAccounting)vs;
            LineItem li = sa.getLineItem();
            if (li instanceof ProcureLineItem) {
                ProcureLineItemCollection plic = (ProcureLineItemCollection)li.getLineItemCollection();
                if (plic != null && plic.getRequester() != null) {
                	String rUnique = plic.getRequester().getUniqueName();

                	//Added common.core.User into the query so that non partiitoned user can not be added as an approver
                	String buyerUserAlias = "cu";
                	AQLClassReference buRef = new AQLClassReference("ariba.common.core.User", buyerUserAlias);
					query.addClass(buRef);
					AQLCondition cond = AQLCondition.parseCondition(StringUtil.strcat(buyerUserAlias, ".User=\"User\""));
					query.and(cond);
					//End Of Added common.core.User into the query so that non partiitoned user can not be added as an approver

                    String conditionText = Fmt.S("\"User\".UniqueName <> '%s'",rUnique);
                    Log.customer.debug("%s *** Condition Text: %s", classname, conditionText);
                    if (conditionText != null)
                    		query.and(AQLCondition.parseCondition(conditionText));
                     Log.customer.debug("%s *** Final Query: %s", classname, query);
                }
            }
        }
    }

	public CatDeptApproverNameTable() {
		super();
	}

}
