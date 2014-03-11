/*
 * Created by KS on Dec 9, 2004
 */
 /******************************************************************************************
  Change History
  #	Change By	Change Date		Description
  ===============================================================================================
  1   Deepak 	08-01-2008		Order field is change to persist the value on change of commodity code.
  2. Shaila       09-10-2009        Adding check if  the adapter source is not like pcsv1:RequisitionImport.csv. Issue 850
*******************************************************************************************/
package config.java.action.vcsv1;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.base.core.*;
import ariba.purchasing.core.*;
import ariba.common.core.*;
import java.util.*;
import ariba.util.log.Log;
import ariba.common.core.User;
import ariba.purchasing.core.Requisition;

public class CatSetAccountingForAccountType extends Action {

	private static final String classname = "CatSetAccountingForAccountType";
	private static final String param_dept = "Application.Caterpillar.Procure.DepartmentForCapital";
    private static final String EXP_CAPITAL = "0000";
//  private static final String DEPT_CAPITAL = "E0290";

	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType **********", object);
        String fac = null;
		String dept = null;
        String div = null;
        String sect = null;
        String expAcct = null;
        String order = null;
		if (object instanceof ReqLineItem) {
			Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType 1**********");
             ReqLineItem rli = (ReqLineItem)object;
             Requisition req = (Requisition)rli.getLineItemCollection();
			  if (req != null)
			  Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType 1**********");
			  String source = (String)req.getDottedFieldValue("AdapterSource");
			  if(!(source.equals("pcsv1:RequisitionImport.csv")))
             {
             CommodityExportMapEntry ceme = rli.getCommodityExportMapEntry();
             ClusterRoot type = (ClusterRoot)rli.getFieldValue("AccountType");
             Partition part = rli.getPartition();
             Requisition r = (Requisition)rli.getLineItemCollection();
             ariba.user.core.User requester = r.getRequester();
             Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType before if loop 2**********");
             if (part == null || requester == null || type == null)  {
             	Log.customer.debug("%s *** PROBLEM: Key field is null!", classname);
             	return;
             }
     		 String atype = type.getUniqueName();
     		 Log.customer.debug("%s *** Account Type: %s", classname, atype);
           	 User user = User.getPartitionedUser(requester, part);
           	 Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType User **********",user);
             if (user != null) {
             	Accounting ua = user.getAccounting();
             	Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType  UA **********",ua);
             	if (ua != null) {
					Log.customer.debug("********Inside Trigger CatSetAccountingForAccountType Inside if **********");
                 	fac = (String)ua.getFieldValue("AccountingFacility");
                 	dept = (String)ua.getFieldValue("Department");
                 	div = (String)ua.getFieldValue("Division");
                 	sect = (String)ua.getFieldValue("Section");
                 	//Changed by Deepak to get the value of order field
                 	order = (String)ua.getFieldValue("Order");
             	}
             }
             if (ceme != null) {
             	expAcct = (String)ceme.getFieldValue("ExpenseAccount");
             }
     		// Log.customer.debug("%s *** Default Values: %s, Fac: %s, Div: %s, Sect: %s, ExpAcct: %s",
    		// 		classname, fac, dept, div, sect, expAcct);
             SplitAccountingCollection sac = rli.getAccountings();
             if (sac != null) {
             	BaseVector splits = sac.getSplitAccountings();
             	if (!splits.isEmpty()) {
            		for (Iterator itr = splits.iterator() ; itr.hasNext() ;)  {
            		    SplitAccounting sa = (SplitAccounting)itr.next();
            			if (atype.equals("Expense"))  {
							Log.customer.debug("Setting Accounting Elements");
            				sa.setDottedFieldValueRespectingUserData("AccountingFacility", fac);
            				sa.setDottedFieldValueRespectingUserData("Department", dept);
            				sa.setDottedFieldValueRespectingUserData("Division", div);
            				sa.setDottedFieldValueRespectingUserData("Section", sect);
            				sa.setDottedFieldValueRespectingUserData("ExpenseAccount", expAcct);
					//CR-23 Modification by KM
					//Changeed by Deepak to set the value of order field
					        Log.customer.debug("********Setting Order**********");
            				sa.setDottedFieldValueRespectingUserData("Order",order);
            				//Log.customer.debug("********After Setting(User data) Order**********",sa.getDottedFieldValueRespectingUserData("Order"));
            				Log.customer.debug("********After Setting(data) Order**********",sa.getDottedFieldValue("Order"));
            				sa.setDottedFieldValueRespectingUserData("Misc", null);
					//CR-23 End Of Modification by KM
            			}
            		    if (atype.equals("Capital"))  {
							Log.customer.debug("********Inside Capital**********");
            		    	dept = Base.getService().getParameter(r.getPartition(), param_dept);
            		    	sa.setDottedFieldValueRespectingUserData("AccountingFacility", fac);
            				if (dept != null)
            					sa.setDottedFieldValue("Department", dept);
            				sa.setDottedFieldValueRespectingUserData("Division", null);
            				sa.setDottedFieldValueRespectingUserData("Section", null);
            				sa.setDottedFieldValue("ExpenseAccount", EXP_CAPITAL);
            				sa.setDottedFieldValueRespectingUserData("Order", null);
            				sa.setDottedFieldValueRespectingUserData("Misc", null);
            			}
            			if (atype.equals("Other"))  {
							Log.customer.debug("********Inside others **********");
            		    	sa.setDottedFieldValueRespectingUserData("AccountingFacility", null);
            				sa.setDottedFieldValueRespectingUserData("Department", null);
            				sa.setDottedFieldValueRespectingUserData("Division", null);
            				sa.setDottedFieldValueRespectingUserData("Section", null);
            				sa.setDottedFieldValueRespectingUserData("ExpenseAccount", expAcct);
            				sa.setDottedFieldValueRespectingUserData("Order", null);
            				sa.setDottedFieldValueRespectingUserData("Misc", null);
            			}
            		}
             	}
             }
         }
	 }
    	return;
    }

	public CatSetAccountingForAccountType() {
		super();
	}
}
