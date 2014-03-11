/****************************************************************************************
Change History
Change# Change By       Change Date     Description
==============================================================================================
1       Madhuri 	  03-11-08	Created : Trigger on MALineItem Creation to default CostCenterText from User Profile.
2       S. Sato       03-16-11  Updated logic for excel upload contracts.. don't want to save the contract at this
                                point especially if the contract is loaded via excel
**********************************************************************************************/

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
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetCostCenterFromUserProfile extends Action{


	public void fire(ValueSource object, PropertyTable params){
		Log.customer.debug(" CatSetCostCenterFromUserProfile : Start of the Trigger ");
		LineItem li = (LineItem)object;
		Log.customer.debug("CatSetCostCenterFromUserProfile : li " + li);

		// Check to make sure that object is of MARLineItem
		if(object!=null && (object instanceof ContractRequestLineItem || object instanceof ReqLineItem)){
			Log.customer.debug(" CatSetCostCenterFromUserProfile : li " + li);
			// Getting Line Item Collection
			LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
			Log.customer.debug(" CatSetCostCenterFromUserProfile : lic " + lic);
			if(lic!=null){

			// Getting requester as sharedUser
			User requester = lic.getRequester();
			if(requester == null){
				return;
			}
			// Getting Partitioned User from Shared User
			ariba.common.core.User puser = ariba.common.core.User.getPartitionedUser(requester, lic.getPartition());

			// Getting Split Account object from MARLineItem
			SplitAccountingCollection  sac = (SplitAccountingCollection)li.getDottedFieldValue("Accountings");
			//null check
			if (sac==null){
				return;
			}
			//null check
			if(puser == null){
				return;
			}
            //Getting CostCenterText value from User's accounting
			String DefaultCostCenterText = (String)puser.getDottedFieldValue("Accounting.CostCenterText");
			if(!StringUtil.nullOrEmptyOrBlankString(DefaultCostCenterText)){
				Log.customer.debug(" CatSetCostCenterFromUserProfile : DefaultCostcenterText " + DefaultCostCenterText);
				// Calling method to set CostCenter Text value to every Split
				setCCinAllSplitAcc(sac,DefaultCostCenterText);
			}

		}// end of Line Collection null check
	}// End of MARLineItem Check
}// End Of Fire Method

//Method to set CostCenterText value on every split account
public void setCCinAllSplitAcc(SplitAccountingCollection sac,String CCValue){
		// Reading splits in to list
		List splitAccountings = (List)sac.getDottedFieldValue("SplitAccountings");
		Log.customer.debug(" CatSetCostCenterFromUserProfile : splitAccountings " + splitAccountings);
		Log.customer.debug(" CatSetCostCenterFromUserProfile : CCValue " + CCValue);
		// iterating through splits and set the CostCenterText value

		LineItemCollection lic = null;

		if(splitAccountings!=null && CCValue!=null){
			for(int i=0;i<splitAccountings.size();i++){
				SplitAccounting sa = (SplitAccounting)splitAccountings.get(i);
				Log.customer.debug(" CatSetCostCenterFromUserProfile : SplitAccounting sa " + sa);
				sa.setDottedFieldValue("CostCenterText",CCValue);
				Log.customer.debug(" CatSetCostCenterFromUserProfile : CostcenterText has been set to " + sa.getDottedFieldValue("GeneralLedgerText"));

				    // S. Sato - we'll just get this value once.
				if (lic == null) {
					lic = sa.getLineItem().getLineItemCollection();
				}
			}

                // S. Sato - save the contract request only if it is not loaded via excel
            if (lic != null) {
                String adapterSource =
                    (String) lic.getFieldValue("AdapterSource");
                if (StringUtil.nullOrEmptyOrBlankString(adapterSource)) {
                    lic.save();
                }
            }
		}
	}// end of setCCinAllSplitAcc Method

}// End of class
