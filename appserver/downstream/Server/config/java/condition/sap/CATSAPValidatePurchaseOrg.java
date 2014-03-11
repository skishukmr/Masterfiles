/************************************************************

 *   Requirement:

 *	To validate Purchase orgs based on the restricted Flag and companycode.

 *  If the PurchaseOrg is associated with the companycode and is not restricted, it is valid.

 *  If the purchaseorg  selected in the user profile is restricted , it will be valid.

 *

 *   Change History:

 *   Change By			Change Date 		Description

 *	--------------------------------------------------------------------------

 *  					Jan-12-2004		Created

 ************************************************************/

package config.java.condition.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.Log;
import ariba.purchasing.core.Requisition;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;

/**

 * This class is used to validate the PurchaseOrg field in the requisition and

 * masteragreement screen.

 */

public class CATSAPValidatePurchaseOrg extends Condition {

	public boolean evaluate(Object value, PropertyTable params) {

		try {

			Log.customer.debug("CATSAPValidatePurchaseOrg: Started validation");
			java.util.Vector porgs = new java.util.Vector();
			java.util.Vector porgsBools = new java.util.Vector();
			AQLQuery aqlQuery = null;
			AQLOptions options = null;
			Requisition approvable = null;
			AQLResultCollection results = null;
			ariba.base.core.Partition currentPartition = null;
			String companyCode = "";
			String sqlText = "";
			String valueToTest = (String) ((ClusterRoot) value).getFieldValue("UniqueName");
			BaseObject paramsObj = (BaseObject) params.getPropertyForKey(InputValueParam);
			boolean puchaseOrgMatchboolean = false;

			BaseObject bo = (BaseObject) paramsObj;

			if (bo instanceof Requisition) {

				approvable = (Requisition) bo;

				// get the companycode

				companyCode = (String) (approvable.getDottedFieldValue("CompanyCode.UniqueName"));

			}

			Log.customer.debug("CATSAPValidatePurchaseOrg: approvable =>" +approvable);

			Log.customer.debug("CATSAPValidatePurchaseOrg: companyCode =>" +companyCode);



			// Get the current partition

			currentPartition = (ariba.base.core.Partition) ariba.base.core.Base.getSession().getPartition();

			if (companyCode != null) {

				// sql query

				/*

				 * : Query has been performance tuned : START

				 */

				// sql query

				sqlText = " Select "

						+ " POrg.UniqueName as UniqueName, "

						+ " POrg.\"Restricted\" as restrictedporg "

						+ " from  "

						+ " ariba.core.PurchaseOrg AS POrg, "

						+ " cat.core.PorgCompanyCodeCombo AS PorgCompanyCodeCombo, "

						+ " ariba.core.CompanyCode AS CC "

						+ " where  "

						+ " POrg = PorgCompanyCodeCombo.PurchaseOrg "

						+ " and PorgCompanyCodeCombo.CompanyCode = CC "

						+ " and CC.UniqueName='" + companyCode + "' " +
						   " and PorgCompanyCodeCombo.PurchaseOrg.IsSAPPurchaseOrg = 'N'";

				/*

				 * Query has been performance tuned : END

				 */

				Log.customer.debug("CATSAPValidatePurchaseOrg: sqlText =>" +sqlText);

				aqlQuery = AQLQuery.parseQuery(sqlText);

				options = new AQLOptions(currentPartition);

				results = Base.getService().executeQuery(aqlQuery, options);

				Log.customer.debug("CATSAPValidatePurchaseOrg: sqlText =>" +results.getSize() );

				while (results.next()) {

					porgs.addElement(results.getString(0));

					if(results.getObject(1) == null)
					{
						Log.customer.debug("CATSAPValidatePurchaseOrg:  results.getString(1) =>" +results.getObject(1) );
						Boolean temp = new Boolean("false");
						porgsBools.addElement(temp);
					}

					else
					{
						Log.customer.debug("CATSAPValidatePurchaseOrg:  results.getString(1) is not null =>" +results.getObject(1) );
						porgsBools.addElement(results.getObject(1));
					}



				}

				Log.customer.debug("CATSAPValidatePurchaseOrg: porgs.elementAt(i)=>" +porgs );
				Log.customer.debug("CATSAPValidatePurchaseOrg: porgs.elementAt(i)=>" +porgsBools );

				for (int i = 0; i < porgs.size(); i++)

				{
					Log.customer.debug("CATSAPValidatePurchaseOrg: porgs.elementAt(i)=>" +porgs.elementAt(i) );
					Log.customer.debug("CATSAPValidatePurchaseOrg: Selected Purchase Org => " + valueToTest);


					if (porgs.elementAt(i).equals(valueToTest))

					{
						Log.customer.debug("CATSAPValidatePurchaseOrg: Selected Purchase Org is valid for the CompanyCode");
						Boolean tempBool = (Boolean) porgsBools.elementAt(i);

						Log.customer.debug("CATSAPValidatePurchaseOrg: tempBool" +tempBool);
						puchaseOrgMatchboolean = true;

						if(tempBool.booleanValue())

						{

							Log.customer.debug("CATSAPValidatePurchaseOrg: Purchase Org selected is restricted");

							aqlQuery = AQLQuery.parseQuery(sqlText);



							options = new AQLOptions(currentPartition);

							results = Base.getService().executeQuery(aqlQuery, options);

							String requester = (String) approvable.getDottedFieldValue("Requester.UniqueName");

							Log.customer.debug("CATSAPValidatePurchaseOrg: requester =>" +requester);

							sqlText = " Select from "

									+ " ariba.common.core.User "

									+ " where  "

									+ " UniqueName = '" + requester + "'";

							aqlQuery = AQLQuery.parseQuery(sqlText);

							options = new AQLOptions(currentPartition);

							results = Base.getService().executeQuery(aqlQuery, options);

							ariba.common.core.User requesterCommon = null;

							if(results.next())
							{

								requesterCommon = (ariba.common.core.User) ariba.base.core.Base.getSession().objectForWrite((BaseId)results.getBaseId(0));

								Log.customer.debug("CATSAPValidatePurchaseOrg: Got the partition user object : requesterCommon => "+requesterCommon);

							}

							if(requesterCommon!=null)

								puchaseOrgMatchboolean = requesterCommon.getDottedFieldValue("PurchaseOrg.UniqueName").equals(valueToTest);

							else

								puchaseOrgMatchboolean = false;
						}
						Log.customer.debug("CATSAPValidatePurchaseOrg: Got the partition user object : puchaseOrgMatchboolean => "+puchaseOrgMatchboolean);
						return puchaseOrgMatchboolean;
					}
				}
			}
		}
		catch (Exception e)
		{

			Log.customer.debug("CATSAPValidatePurchaseOrg : Exception : " + e);
		}
		return false;
	}

	/**
	public boolean evaluate(Object value, PropertyTable params) {

		 return evaluateImpl(value, params);

	}
	**/

	/**

	 * Tests for condition and returns error message if error occurs.

	 */

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {

		Log.customer.debug("CATSAPValidatePurchaseOrg: => within evaluateAndExplain  *** ");
		//if (!evaluate(value, params))
			// return new ConditionResult(ResourceService.getString(ComponentStringTable, PurchaseOrgViolationMsg));
		boolean testResult =  evaluate(value, params);
		Log.customer.debug("CATSAPValidatePurchaseOrg: => testResult =>" +testResult);

		if(!testResult){
			// For Debug purpose
			Log.customer.debug("CATSAPValidatePurchaseOrg: => Printing the Error message of evaluateAndExplain ");
			String PORViolationMsg = (String)ResourceService.getString(ComponentStringTable, "PurchaseOrgViolationMsg");
			Log.customer.debug("CATSAPValidatePurchaseOrg: =>PORViolationMsg => " +PORViolationMsg);
			ConditionResult testCondRes = new ConditionResult(ResourceService.getString(ComponentStringTable, PORViolationMsg));
			Log.customer.debug("CATSAPValidatePurchaseOrg: =>PORViolationMsg => " +testCondRes);
			String strError = testCondRes.getFirstError();
			Log.customer.debug("CATSAPValidatePurchaseOrg: =>strError => " +strError);
			return testCondRes;
		}
		else
			return null;

	} /**

	 * Return the required parameter names.

	 */

	protected String[] getRequiredParameterNames() {

		return requiredParameterNames;

	}

	/**

	 * Return the list of valid value types.

	 */

	protected ValueInfo[] getParameterInfo() {

		return parameterInfo;

	}

    public CATSAPValidatePurchaseOrg()
    {
		super();
    }
	// Parameter to passed in through AML/

	private static final String InputValueParam = "InputValue";

	/** Condition info */

	private static final ValueInfo[] parameterInfo = { new ValueInfo(InputValueParam, IsScalar, "ariba.approvable.core.Approvable"), };

	private static final String[] requiredParameterNames = { InputValueParam };

	public static final String ComponentStringTable = "aml.CatSAPRequisitionExt";

	private static final String PurchaseOrgViolationMsg = "PurchaseOrgViolationMsg";
}

