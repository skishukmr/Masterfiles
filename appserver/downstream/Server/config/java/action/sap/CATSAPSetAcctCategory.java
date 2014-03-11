/*********************************************************************************************************************
Created by : James S Pagadala
********************************************************************************************************************/
package config.java.action.sap;
import ariba.procure.core.ProcureLineItem;
import ariba.util.log.Log;

	 public void fire(ValueSource object, PropertyTable params){
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

		   if(reqOrMastAggReq.getDottedFieldValue("AccountCategory") != null){
		   }
	       Log.customer.debug("CATSAPSetAcctCategory : fire : ****END****");