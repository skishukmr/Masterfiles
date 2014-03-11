/*********************************************************************************************************************

Created by : Dharshan BS
Date	   : 07/10/2010

********************************************************************************************************************/

package config.java.action.sap;

import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATSAPCCtoupper extends Action {

	 public void fire(ValueSource object, PropertyTable params){

		 try{
	       Log.customer.debug("CATSAPCCtoupper : fire : ****START****");

	       if (!(object instanceof SplitAccounting)){
			   return;
		   }

	       SplitAccounting splitAccounting = (SplitAccounting)object;

	       ProcureLineItem procLI = (ProcureLineItem)splitAccounting.getLineItem();

	       String acctCategory = (String)procLI.getDottedFieldValue("AccountCategory.UniqueName");

	       if(acctCategory == null){
			   return;
		   }
		   else{

		  String Costtextvalue = (String)splitAccounting.getDottedFieldValue("CostCenterText");
		  Log.customer.debug("CATSAPCCtoupper ::: CostCentertextval %s",Costtextvalue);
		  String convCosttext = Costtextvalue.toUpperCase();
		  Log.customer.debug("CATSAPCCtoupper ::: Converted Value*** %s",convCosttext);
		  splitAccounting.setDottedFieldValue("CostCenterText",convCosttext);
		   return;
	   }


	      // Log.customer.debug("CATSAPCCtoupper : fire : ****END****");
		 }
		 catch(Exception e){
			 Log.customer.debug("CATSAPCCtoupper : Exception : ****"+ e.toString() +"****");
			 return;
		 }
	 }

}