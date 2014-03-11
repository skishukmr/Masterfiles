package config.java.condition.vcsv1;

import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.common.core.Accounting;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatCSVIsVisibleToUser extends Condition
{

    private static final String ClassName = "CatCSVIsVisibleToUser";

    public CatCSVIsVisibleToUser()
    {
    }

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        //boolean debug = Log.customer.debugOn;

    	Log.customer.debug("%s ::: Entering the evaluate method", "CatCSVIsVisibleToUser");
        User currUser = (User)Base.getSession().getEffectiveUser();
		String testvalue = "R8";
		if(currUser != null){
			String userAcctFacility = (String)currUser.getFieldValue("AccountingFacility");
			if (testvalue.equalsIgnoreCase(userAcctFacility))
			{
				Log.customer.debug("CatCSVIsVisibleToUser: returning true");
				return true;
			}
		}
		if(object instanceof Accounting){
			Accounting acct = (Accounting)object;
			String facility = (String)acct.getDottedFieldValue("Facility.UniqueName");
			if(facility != null && testvalue.equalsIgnoreCase(facility)){
				return true;
			}
		}
		if(object instanceof ReqLineItem){
			ReqLineItem rli = (ReqLineItem)object;
			SplitAccountingCollection sac = rli.getAccountings();
			if(sac != null)
			{
				Iterator saci = sac.getAllSplitAccountingsIterator();
				while(saci.hasNext())
				{
					SplitAccounting sa = (SplitAccounting)saci.next();
					if (sa != null) {
						String facility = (String)sa.getDottedFieldValue("Facility.UniqueName");
						if(facility != null && testvalue.equalsIgnoreCase(facility)){
							return true;
						}
						break;
					}
				}
			}
		}
		return false;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
        throws ConditionEvaluationException
    {
        Log.customer.debug("%s ::: Entering the evaluateAndExplain method", "CatCSVIsVisibleToUser");
        if(!evaluate(object, params))
        {
            return new ConditionResult("");
        }
        Log.customer.debug("%s ::: Skipping validation", "CatCSVIsVisibleToUser");
        return null;
    }
}
