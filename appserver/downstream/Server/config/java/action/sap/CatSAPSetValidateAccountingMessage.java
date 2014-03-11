package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.contract.core.ContractRequestLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSetValidateAccountingMessage extends Action{



	public void fire(ValueSource object, PropertyTable params){
		Log.customer.debug(" CatSAPSetValidateAccountingMessage : Start of the Trigger ");
		LineItem li = (LineItem)object;
		Log.customer.debug("CatSAPSetValidateAccountingMessage : li " + li);


		if(object!=null && (object instanceof ContractRequestLineItem || object instanceof ReqLineItem)){
			Log.customer.debug(" CatSAPSetValidateAccountingMessage : li " + li);

			LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
			Log.customer.debug(" CatSAPSetValidateAccountingMessage : lic " + lic);
			if(lic!=null){


			SplitAccountingCollection  sac = (SplitAccountingCollection)li.getDottedFieldValue("Accountings");

			if (sac==null){
				return;
			}

			setCCinAllSplitAcc(sac);
			}

		}
	}


public void setCCinAllSplitAcc(SplitAccountingCollection sac){
		// Reading splits in to list
		List splitAccountings = (List)sac.getDottedFieldValue("SplitAccountings");
		Log.customer.debug(" CatSAPSetValidateAccountingMessage : splitAccountings " + splitAccountings);
		// iterating through splits and set the ValidateAccountingMessage value
		if(splitAccountings!=null ){
			for(int i=0;i<splitAccountings.size();i++){
				SplitAccounting sa = (SplitAccounting)splitAccountings.get(i);
				Log.customer.debug(" CatSAPSetValidateAccountingMessage : SplitAccounting sa " + sa);
				sa.setDottedFieldValue("ValidateAccountingMessage",null);
				sa.getLineItem().getLineItemCollection().save();
			}
		}
	}// end of setCCinAllSplitAcc Method

}
