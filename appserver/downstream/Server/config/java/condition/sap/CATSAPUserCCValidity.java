/*********************************************************************************************************************

Changed by : Dharshan BS
Date	   : 21/10/2010
1. Ashwini & Dharshan  10-11-2010 Issue 1203 Setting costcenter to uppercase in user profile
********************************************************************************************************************/

package config.java.condition.sap;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.common.core.Accounting;
import ariba.common.core.UserProfile;
import ariba.common.core.UserProfileDetails;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATSAPUserCCValidity extends Condition {

	public boolean evaluate(Object value, PropertyTable props)
			throws ConditionEvaluationException {
		// TODO Auto-generated method stub
		if (evaluateAndExplain(value, props)==null){
			return true;
		}
		return false;
	}

	//@Override
	public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
			throws ConditionEvaluationException {
		// TODO Auto-generated method stub
		String errorText = "Cost center should have exactly 10 characters for MX company codes";
		String errorTextcase = "Please enter cost center in UpperCase only !!!";
		boolean lowerFound = false;
		BaseObject upDetails = (BaseObject) value;

		if (upDetails == null) {
			Log.customer.debug(thisClass+"Accounting null, existing");
			return null;
		}
		ClusterRoot cl = upDetails.getClusterRoot();
		if(cl==null)return null;
		Log.customer.debug(thisClass+"Clusterroot not null");
		UserProfile up = null;
		BaseObject companyCode = null;
		if(cl instanceof ariba.common.core.UserProfile){
			Log.customer.debug(thisClass+"Clusterroot instance of user profile");
			up=(UserProfile)cl;
			Log.customer.debug(thisClass+"Clusterroot converted into user profile.");
			companyCode = (BaseObject) up.getDetails().getFieldValue("CompanyCode");
			if (companyCode==null) return null;
			Log.customer.debug(thisClass+"company code not null");
			String companyCodeString = (String) companyCode.getFieldValue("UniqueName");
			Log.customer.debug(thisClass+"Company code UniqueName: "+companyCodeString);
			if(companyCodeString.equalsIgnoreCase("8900")
					|| companyCodeString.equalsIgnoreCase("MC00")
							|| companyCodeString.equalsIgnoreCase("ME00")
									|| companyCodeString.equalsIgnoreCase("XY00")){
				ConditionResult conRes = new ConditionResult(errorText,true);
				ConditionResult conRes1 = new ConditionResult(errorText,false);
				ConditionResult conRes2 = new ConditionResult(errorTextcase,true);
				int ccTextLen = ((UserProfileDetails)upDetails).getAccounting().getFieldValue("CostCenterText").toString().length();
				 // 1203 Starts Setting costcenter to uppercase in user profile
				String ccTextcase = (String)up.getDetails().getDottedFieldValue("Accounting.CostCenterText");
								Log.customer.debug("CATSAPUserCCValidity ::: CostCentertextval %s",ccTextcase);
								char[] chars = ccTextcase.toCharArray();
								 for (int x = 0; x < chars.length; x++) {
								    char c = chars[x];
								   if ((c >= 'a') && (c <= 'z')) {
								   	return conRes2;

								   }
				 }

				 // 1203 Ends Setting costcenter to uppercase in user profile



				if(ccTextLen!=10)
				{
					 return conRes;
				}
				else
				{
					String CostCenter=(String)up.getDetails().getDottedFieldValue("Accounting.CostCenterText");
					ariba.common.core.User paruser=((ariba.common.core.UserProfile)cl).getUser();
					ariba.user.core.User user=paruser.getUser();
					user.setFieldValue("SAPCostCenter",CostCenter);
					return conRes1;
				}
			}
			Log.customer.debug(thisClass+"Not a MX company code, validation doesn't apply");
		}
		return null;
	}
	private String thisClass = this.getClass().getName();
}
