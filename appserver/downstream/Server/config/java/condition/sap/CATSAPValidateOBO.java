/************************************************************

 *   Requirement:

 *
 *

 *   Change History:

 *   Change By			Change Date 		Description

 *	--------------------------------------------------------------------------

 *  					Jan-12-2004		Created

 ************************************************************/

package config.java.condition.sap;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Log;
import ariba.purchasing.core.Requisition;
import ariba.user.core.Permission;
import ariba.util.core.PropertyTable;


public class CATSAPValidateOBO extends Condition {

	public boolean evaluate(Object value, PropertyTable params) {

			Log.customer.debug("CATSAPValidateOBO: Started validation");
			Requisition approvable = null;
			String companyCode = "";
			String valueToTest = (String) ((ClusterRoot) value).getFieldValue("UniqueName");
			BaseObject paramsObj = (BaseObject) params.getPropertyForKey(InputValueParam);

			String purchaseorg =null;
			BaseObject bo = (BaseObject) paramsObj;
		if (bo instanceof Requisition) {
			approvable = (Requisition) bo;

		/* Code changes: Ravindra Prabhu
		 * CR # 1049
		 * Requirement: Escaping the validation on OBO if the user having CatCrossCompanyBuying permission
		 */
			ariba.user.core.User sUser = approvable.getPreparer();
			if(sUser.hasPermission(Permission.getPermission(escValPermission))){
				Log.customer.debug("CATSAPValidateOBO: Preparer has CatCrossCompanyBuying permission, skipping the requester field validation.");
				return true;
			}
		/*
		 *End of CR # 1049
		 */
			ClusterRoot companycode = (ClusterRoot) (approvable.getDottedFieldValue("CompanyCode"));

			if(companycode!=null)
			{
			 companyCode = (String) (approvable.getDottedFieldValue("CompanyCode.UniqueName"));
			purchaseorg = (String) (approvable.getDottedFieldValue("CompanyCode.PurchaseOrgFiltering"));
			Log.customer.debug("CATSAPValidateOBO: purchaseorg =>" +purchaseorg);
			}
			else
			return true;
			}

			Log.customer.debug("CATSAPValidateOBO: approvable =>" +approvable);
			Log.customer.debug("CATSAPValidateOBO: companyCode =>" +companyCode);
			ariba.common.core.User puser = ariba.common.core.User.getPartitionedUser(approvable.getRequester(), approvable.getPartition());
			Log.customer.debug("CATSAPValidateOBO: puser =>" +puser);
			String rqstrcompanycode= (String)puser.getDottedFieldValue("CompanyCode.UniqueName");
			Log.customer.debug("CATSAPValidateOBO: rqstrcompanycode =>" +rqstrcompanycode);
			if(purchaseorg!=null && purchaseorg.equals("Y"))
			  {
				String porg = (String) (approvable.getDottedFieldValue("CustomCatalogPurchaseOrg.UniqueName"));
				Log.customer.debug("CATSAPValidateOBO: porg =>" +porg);
				String rqstrpurchaseorg= (String)puser.getDottedFieldValue("PurchaseOrg.UniqueName");
				Log.customer.debug("CATSAPValidateOBO: rqstrpurchaseorg =>" +rqstrpurchaseorg);
			if (companyCode != null && rqstrcompanycode!=null && porg!=null && rqstrpurchaseorg!=null )
				  {
					if(companyCode.equals(rqstrcompanycode) &&porg.equals(rqstrpurchaseorg))
				return true;
				  }
				}
			else
			{
			if (companyCode != null && rqstrcompanycode!=null &&(companyCode.equals(rqstrcompanycode)|| companyCode == rqstrcompanycode))

			{
				Log.customer.debug("CATSAPValidateOBO: end2 ");
				return true;
			}
			}
			Log.customer.debug("CATSAPValidateOBO: end1 ");
			return false;
	}

 public CATSAPValidateOBO()
    {
		super();
    }
    protected String[] getRequiredParameterNames() {
	return requiredParameterNames;
	}
    protected ValueInfo[] getParameterInfo() {
	return parameterInfo;
	}

		private static final String InputValueParam = "InputValue";
		private String escValPermission = "CatCrossCompanyBuying";
		/** Condition info */
		private static final ValueInfo[] parameterInfo = { new ValueInfo(InputValueParam, IsScalar, "ariba.approvable.core.Approvable"), };
		private static final String[] requiredParameterNames = { InputValueParam };
		public static final String ComponentStringTable = "aml.CatSAPRequisitionExt";
		private static final String oboViolationMsg = "RequesterinvalidMsg";

}