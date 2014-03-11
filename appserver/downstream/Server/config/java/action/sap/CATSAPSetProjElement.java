/*********************************************************************************************************************
Created by : James S Pagadala
********************************************************************************************************************/
package config.java.action.sap;
public class CATSAPSetProjElement extends Action {
	 public void fire(ValueSource object, PropertyTable params){
	       if (!(object instanceof SplitAccounting)){
			   return;
		   }

	       SplitAccounting splitAccounting = (SplitAccounting)object;

	       String acctCategory = (String)procLI.getDottedFieldValue("AccountCategory.UniqueName");

	       if(acctCategory == null){
			   return;
		   }

		   acctCategory = acctCategory.trim();

		   String WBS_ACCT_CATEGORY = "P";

		   String IO_ACCT_CATEGORY = "F";

	       ReceivableLineItemCollection reqOrMastAggReq = (ReceivableLineItemCollection) procLI.getLineItemCollection();

	       if(WBS_ACCT_CATEGORY.equals(acctCategory)){

			   reqOrMastAggReq.setDottedFieldValue("WBSElementText",splitAccounting.getDottedFieldValue("WBSElementText"));

			   return;
		   }

	       if(IO_ACCT_CATEGORY.equals(acctCategory)){

			   reqOrMastAggReq.setDottedFieldValue("InternalOrderText",splitAccounting.getDottedFieldValue("InternalOrderText"));

			   return;
		   }

	       Log.customer.debug("CATSAPSetProjElement : fire : ****END****");