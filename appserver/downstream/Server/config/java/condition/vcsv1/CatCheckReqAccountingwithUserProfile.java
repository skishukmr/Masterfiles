/*******************************************************************************************************************************************
        Creator: Manoj.R
		Description: Validation on Accouting Reason Code field based on Accounting Distribution.
		Issue Number:R4-CR263
		ChangeLog: Intial Version
		Date		Name		History
		--------------------------------------------------------------------------------------------------------------
	   12/10/2011 	Manoj.R     Initial Version
*******************************************************************************************************************************************/
package config.java.condition.vcsv1;

import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.purchasing.core.*;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.common.core.User;
import ariba.base.core.Partition;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.Accounting;
import ariba.common.core.CommodityExportMapEntry;
import java.util.Iterator;
import ariba.base.core.ClusterRoot;

public class CatCheckReqAccountingwithUserProfile extends Condition

{

  private static final String classname = "CatCheckReqAccountingwithUserProfile";



  public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException

  {

	String fac = null;
	String dept = null;
	String div = null;
	String sect = null;
	String expAcct = null;
	String facRli = null;
	String deptRli = null;
	String divRli = null;
	String sectRli = null;
	String noValue = "(no value)";

    if(object instanceof Requisition)
    {
	  Log.customer.debug("******Inside Requisition", classname);
      Requisition req = (Requisition)object;
      if(req != null)
      {
		  String AcctReason =(String)req.getFieldValue("AcctReasonCode") ;
		  Log.customer.debug("********AccountingReason: %s", AcctReason);
		  Partition part = req.getPartition();
	      ariba.user.core.User requester = req.getRequester();

	       // Getting User Accounting Distribution
      if (part != null && requester != null)
      {
	  User user = User.getPartitionedUser(requester, part);
	  if (user != null)
	  {
		Log.customer.debug("******Into User", classname);
	    Accounting ua = user.getAccounting();
		if (ua != null)
		{
			Log.customer.debug("********Into User Account Details", classname);
			fac = (String)ua.getFieldValue("AccountingFacility");
			dept = (String)ua.getFieldValue("Department");
			div = (String)ua.getFieldValue("Division");
			sect = (String)ua.getFieldValue("Section");
		}
	 Log.customer.debug("%s *** Default Values: %s, Fac: %s, Div: %s, Sect: %s",
		classname, fac, dept, div, sect, expAcct);
	  }
     }
       // Getting LineItem Accounting Distribution
		BaseVector reqLi = req.getLineItems();
		ReqLineItem rli = null;
		int LineCount = reqLi.size();
		for (int i = 0; i < LineCount; i++)
		{
		  Log.customer.debug("********Into ReqLine details", classname);
		  rli = (ReqLineItem) reqLi.get(i);
		  CommodityExportMapEntry ceme = rli.getCommodityExportMapEntry();
                 if ( ceme != null)
                 {
		  ClusterRoot type = (ClusterRoot)rli.getFieldValue("AccountType");
                  if ( type != null)
                    {
		  String atype = type.getUniqueName();
		  if (atype.equals("Expense"))
		  {
			 Log.customer.debug("******Inside Expense", classname);
			SplitAccountingCollection sac = rli.getAccountings();
			   if (sac != null)
			   {
				 BaseVector splits = sac.getSplitAccountings();
				 if (!splits.isEmpty())
				 {
				   for (Iterator itr = splits.iterator() ; itr.hasNext() ;)
				   {
					   Log.customer.debug("*******Inside ReqLineAccountDetails", classname);
					   SplitAccounting sa = (SplitAccounting)itr.next();
						facRli = (String)sa.getFieldValue("AccountingFacility");
						deptRli = (String)sa.getFieldValue("Department");
						divRli = (String)sa.getFieldValue("Division");
						sectRli = (String)sa.getFieldValue("Section");
						 Log.customer.debug("%s *** Default Values: %s, Facli: %s, Divli: %s, Sectli: %s",
		classname, facRli, deptRli, divRli, sectRli, atype);
				   }
				 }

			   }
			 // Validation Starts

			 if (fac != null && fac.equals(facRli))
			 {
				Log.customer.debug("*******Inside validation 1", classname);
				if ( dept != null && dept.equals(deptRli))
				 {
				   Log.customer.debug("*******Inside validation 2", classname);
                   if (div != null && div.equals(divRli))
                   {
					    Log.customer.debug("*******Inside validation 3", classname);
					   if (sect != null && sect.equals(sectRli))
					    {
						    Log.customer.debug("*******Inside validation 4", classname);
                            continue ;
					    }
                        else
                        {
							return false ;
					    }
			        }
			       else
				   {
					return false ;
					}
				 }
				else
				{
					return false ;
				}
		   }

		   else
			{
				return false ;
	        }

		 }
               }
             }
     }
     req.setFieldValue("AcctReasonCode", noValue);
     Log.customer.debug("*******Set AcctReasonField : %s",AcctReason);
     return true;
}

}
Log.customer.debug("****** Object not instance of requisition", classname);
return false ;
}

}
