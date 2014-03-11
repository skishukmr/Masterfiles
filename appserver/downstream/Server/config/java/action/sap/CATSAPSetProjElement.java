/*********************************************************************************************************************
Created by : James S PagadalaDate	   : Oct 06 2008
********************************************************************************************************************/
package config.java.action.sap;import ariba.base.fields.Action;import ariba.base.fields.ValueSource;import ariba.common.core.SplitAccounting;import ariba.procure.core.ProcureLineItem;import ariba.receiving.core.ReceivableLineItemCollection;import ariba.util.core.PropertyTable;import ariba.util.log.Log;
public class CATSAPSetProjElement extends Action {
	 public void fire(ValueSource object, PropertyTable params){		 		 try{	       Log.customer.debug("CATSAPSetProjElement : fire : ****START****");
	       if (!(object instanceof SplitAccounting)){
			   return;
		   }

	       SplitAccounting splitAccounting = (SplitAccounting)object;	       	       ProcureLineItem procLI = (ProcureLineItem)splitAccounting.getLineItem();

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

	       Log.customer.debug("CATSAPSetProjElement : fire : ****END****");		 }		 catch(Exception e){			 Log.customer.debug("CATSAPSetProjElement : Exception : ****"+ e.toString() +"****");			 return;		 }	 }	 }