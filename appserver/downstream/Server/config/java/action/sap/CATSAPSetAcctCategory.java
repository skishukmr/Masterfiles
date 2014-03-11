/*********************************************************************************************************************
Created by : James S PagadalaDate	   : Oct 06 2008
********************************************************************************************************************/
package config.java.action.sap;import ariba.base.fields.ValueSource;import ariba.util.core.PropertyTable;import ariba.base.fields.Action;import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.util.log.Log;
public class CATSAPSetAcctCategory extends Action {
	 public void fire(ValueSource object, PropertyTable params){	       Log.customer.debug("CATSAPSetAcctCategory : fire : ****START****");
	       if(object == null){
			   return;
		   }

	       if (!(object instanceof ProcureLineItem)){
			   return;
		   }

	       ProcureLineItem procLI = (ProcureLineItem)object;
	       if(procLI == null){
			   return;
		   }

	       String acctCategory = (String)procLI.getDottedFieldValue("AccountCategory.UniqueName");

	       if(acctCategory == null){
			   return;
		   }

		   acctCategory = acctCategory.trim();

		   String WBS_ACCT_CATEGORY = "P";

		   String IO_ACCT_CATEGORY = "F";

	       ReceivableLineItemCollection reqOrMastAggReq = (ReceivableLineItemCollection) procLI.getLineItemCollection();

	       if(reqOrMastAggReq == null){
			   return;
		   }

	       if(WBS_ACCT_CATEGORY.equals(acctCategory) || IO_ACCT_CATEGORY.equals(acctCategory)){
			   reqOrMastAggReq.setDottedFieldValue("AccountCategory",acctCategory);
			   return;
		   }

		   if(reqOrMastAggReq.getDottedFieldValue("AccountCategory") != null){			   reqOrMastAggReq.setDottedFieldValue("AccountCategory",null);
		   }
	       Log.customer.debug("CATSAPSetAcctCategory : fire : ****END****");	}}
