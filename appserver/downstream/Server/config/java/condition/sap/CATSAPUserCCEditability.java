package config.java.condition.sap;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.common.core.UserProfile;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * A Temporary solution for Mexico company codes to allow users to select the
 * their cost centers by themselves. Company codes are hard coded in this code
 * instead of setting a company level flag, as this is only a stop-gap arrangement.
 */
public class CATSAPUserCCEditability extends Condition {

	// @Override
	public boolean evaluate(Object value, PropertyTable params)
			throws ConditionEvaluationException {
		// TODO Auto-generated method stub

		BaseObject accounting = (BaseObject) value;	

		if (accounting == null) {
			Log.customer.debug(thisClass+"Accounting null, existing");
			return false;
		}
		ClusterRoot cl = accounting.getClusterRoot();
		if(cl==null)return false;
		Log.customer.debug(thisClass+"Clusterroot not null");
		UserProfile up = null;
		BaseObject companyCode = null;
		if(cl instanceof ariba.common.core.UserProfile){
			Log.customer.debug(thisClass+"Clusterroot instance of user profile");
			up=(UserProfile)cl;
			Log.customer.debug(thisClass+"user profile created");
			companyCode = (BaseObject) up.getDetails().getFieldValue("CompanyCode");
			if (companyCode==null) return false;
			Log.customer.debug(thisClass+"company code not null");
			String companyCodeString = (String) companyCode.getFieldValue("UniqueName");
			Log.customer.debug(thisClass+"Company code UniqueName: "+companyCodeString);
			if(companyCodeString.equalsIgnoreCase("8900") 
					|| companyCodeString.equalsIgnoreCase("MC00")
							|| companyCodeString.equalsIgnoreCase("ME00")
									|| companyCodeString.equalsIgnoreCase("XY00")){
				Log.customer.debug(thisClass+"Cost center editable for MX company codes");
				return true;
			}
			Log.customer.debug(thisClass+"Not a MX company code, can not change cost center");
		}		
		return false;
	}
	public static String propNameAcc = "AccObject";
	private String thisClass = this.getClass().getName();
	//private static final ValueInfo parameterInfo[] = {new ValueInfo(propNameAcc, 0, "ariba.common.core.Accounting")};
}
