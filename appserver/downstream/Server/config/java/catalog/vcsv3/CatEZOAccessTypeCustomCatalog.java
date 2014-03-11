/******************************************************************************
	Author: 		Dharmang J. Shelat
	Date Created:  	10/07/2006
	Description: 	Custom Catalog implementation to filter catalogs based on
					the Access Level from the user profile (from LOGNET).
					AccessTypes are:
					- "Full"
						"Full Access to MSC"
						"By default all users have full access"
					- "No"
						"No Access to MSC"
						"Restricted user can not access system"
					- "Partial"
						"Partial Access"
						"User able to see catalogs only - Can NOT see Contracts"
					- "Limited"
						"Limited Access"
						"User only able to create Non-Catalog item,
						 can not see catalogs or contracts"
-------------------------------------------------------------------------------
	Change Author:	Dharmang J. Shelat
	Date Modified:	11/06/2006
	Description:	Added filter constraint for eRFQ requisitions to prevent
					selection of catalog items on eRFQ requisition.
-------------------------------------------------------------------------------
	Change Author:	Dharmang J. Shelat
	Date Modified:	12/13/2006
	Description:	Changed the filter to use TerritoryAvailable instead of the
					language field.
******************************************************************************/

package config.java.catalog.vcsv3;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.basic.core.LocaleID;
import ariba.common.core.CommonSupplier;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.server.CustomCatalog;
import ariba.purchasing.core.Requisition;
import ariba.util.log.Log;

public class CatEZOAccessTypeCustomCatalog implements CustomCatalog
{
	private static final String ClassName = "CatEZOAccessTypeCustomCatalog";
	private static String unspecifiedSupplierID = null;

	public void initialize() {
		CommonSupplier cs = CommonSupplier.tryGetPartialItemCommonSupplier();
		if (cs != null) {
			unspecifiedSupplierID = cs.getUniqueID();
		}
	}

	public AQLCondition customCatalogConstraint(AQLQuery catalogQuery,Partition partition,ValueSource user,ValueSource plic) {
		String conditionText = null;

		Log.customer.debug("%s ::: customCatalogConstraint catalogQuery: " + catalogQuery, ClassName);
		Log.customer.debug("%s ::: customCatalogConstraint plic: " + plic, ClassName);
		Log.customer.debug("%s ::: customCatalogConstraint user: " + user, ClassName);

		/*
		 * AUL, sdey - corrected class cast excp
		 */
		ariba.common.core.User userObj = null;
		if (user instanceof ariba.user.core.User) {
			BaseId userBid = (BaseId) user.getFieldValue("BaseId");
			ariba.user.core.User userCoreObj = (ariba.user.core.User) Base
					.getSession().objectFromId(userBid);

			Log.customer.debug(
					"%s ::: customCatalogConstraint userCoreObj is: %s",
					ClassName, userCoreObj);
			userObj = ariba.common.core.User.getPartitionedUser(userCoreObj,
					partition);
		} else if (user instanceof ariba.common.core.User) {
			userObj = (ariba.common.core.User) user;
			user = (ariba.user.core.User) userObj.getUser();
		}
		/*
		 * AUL, sdey - corrected class cast excp
		 */

		Log.customer.debug("%s ::: customCatalogConstraint userObj is: %s",ClassName, userObj);
		if (userObj != null){
			LocaleID userLoc = userObj.getLocaleID();
			ClusterRoot fac = (ClusterRoot) userObj.getFieldValue("AccountingFacility");
			/*
			if (userLoc != null){
				String userLang = userLoc.getLanguage();
				String userCountry = userLoc.getCountry();
				if ("en".equals(userLang)){
					conditionText = "CatalogEntry.Language = 'en_US'";
				}
				else {
					conditionText = "CatalogEntry.Language = '" + userLang + "'";
				}
			}
			*/
			if (fac != null){
				String userFac = fac.getUniqueName();
				if ("36".equals(userFac)){
					conditionText = "(CatalogEntry.TerritoryAvailable = 'CH' OR CatalogEntry.TerritoryAvailable is null)";
				}
				else if ("NF".equals(userFac) || "NG".equals(userFac)) {
					conditionText = "(CatalogEntry.TerritoryAvailable = 'ES' OR CatalogEntry.TerritoryAvailable is null)";
				}
			}
		}

		// AUL, sdey : check for plic to correct null pointer excp
		ProcureLineItemCollection plicObj = null;
		if(plic != null){
			BaseId plicBid = (BaseId) plic.getFieldValue("BaseId");
			plicObj = (ProcureLineItemCollection) Base.getSession().objectFromId(plicBid);
			Log.customer.debug("%s ::: customCatalogConstraint plicObj is: %s", ClassName, plicObj);
		}

		String accessType = userAccessType(user, plic);
		boolean iseRFQ = isRequisitionRFQ(plicObj);

		Log.customer.debug("%s ::: customCatalogConstraint accessType is: %s", ClassName, accessType);
		if (accessType == null || (accessType != null && (accessType.equalsIgnoreCase("FULL") || accessType.equalsIgnoreCase("PARTIAL")))) {

			Log.customer.debug("%s ::: access type is FULL/PARTIAL or null, return condition NULL!", ClassName);
			if (iseRFQ) {
				conditionText = conditionText + " AND CatalogEntry.Description.CommonSupplier = NULL";
			}
			//conditionText = "CatalogEntry.EULocale = 'M'";
			//conditionText = "CatalogEntry.Language = 'fr'";
			//return null;
		}
		else {

			Log.customer.debug("%s ::: customCatalogConstraint access type is: %s", ClassName, accessType);
			if (accessType.equalsIgnoreCase("Limited") || iseRFQ) {
				conditionText = conditionText + " AND CatalogEntry.Description.CommonSupplier = NULL";
			}
		}


		Log.customer.debug("%s ::: customCatalogConstraint conditionText: %s", ClassName, conditionText);
		AQLCondition returnCond = AQLCondition.parseCondition(conditionText);

		Log.customer.debug("%s ::: customCatalogConstraint AQLCondition Text: %s", ClassName, returnCond.toString());
		return returnCond;
	}

	public Object customCatalogHash(Partition partition, ValueSource user, ValueSource plic) {
		return null;
	}

	protected String userAccessType(ValueSource user, ValueSource plic) {
		String mscAccessTypeStr = null;
		if (user != null && plic instanceof Requisition) {

			/*
			 * AUL, sdey - Corrected class cast excp
			 */
			if (user instanceof ariba.common.core.User) {
				ariba.common.core.User commonUser = (ariba.common.core.User) user;
				user = (ariba.user.core.User) commonUser.getUser();
			}
			/*
			 * AUL, sdey - Corrected class cast excp
			 */

			ClusterRoot mscAccessObj = (ClusterRoot) user.getFieldValue("MSCAccessType");
			if (mscAccessObj != null)
				mscAccessTypeStr = (String) mscAccessObj.getFieldValue("UniqueName");
		}
		return mscAccessTypeStr;
	}

	protected boolean isRequisitionRFQ(ProcureLineItemCollection plic) {
		boolean iSeRFQ = false;
		if (plic instanceof Requisition) {
			Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");
			Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");
			if ((iSeRFQB != null) && iSeRFQB.booleanValue()) {
				iSeRFQ = true;
			}
			if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {
				iSeRFQ = true;
			}
		}
		return iSeRFQ;
	}

	public CatEZOAccessTypeCustomCatalog() {
		super();
	}
}
