package config.java.nametable.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLNameTable;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.SearchTermQuery;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.Log;

/*******************************************************************
 *   Requirement:
 *	 To filter Purchase orgs based on the restricted Flag and companycode.
 *   If the purchaseorg  selected in the user profile is restricted , it will be shown.
 *   If the purchaseorg  selected in the user profile is not restricted , it will not be shown.
 *   All other restricted PurchseOrgs  will not be shown.
 *
 *   Change History:
 *   Change By		Change Date 		Description
 *	--------------------------------------------------------------------------------
 *  				Dec-24-2003	  Created
 *
 **********************************************************************/

/**
 * This class is used restrict the selection of PurchaseOrgs based on the
 * Restricted Flag for the opCo.
 */

public class CATSAPFilterPurchaseOrg extends AQLNameTable {

	public CATSAPFilterPurchaseOrg()
	{
		super();
	}

	public AQLQuery buildQuery(AQLQuery query, String field,String pattern, SearchTermQuery searchtermquery) {

		Log.customer.debug(" CATSAPFilterPurchaseOrg: Started *****");
		String companyCode = "";
		String strPattern = new String(pattern);

		if (strPattern != null && !strPattern.trim().equals("")) {
			if (strPattern.length() == 1 && strPattern.equals("*")) {
				strPattern = "";
			} else {
				strPattern = strPattern.replace('*', ' ');
			}
		}
		strPattern = strPattern.trim();


		ValueSource context = super.getValueSourceContext();

		String queryText = "SELECT DISTINCT PurchaseOrg, PurchaseOrg.UniqueName, PurchaseOrg.Name " +
		"FROM ariba.core.PurchaseOrg AS PurchaseOrg PARTITION SAP, " +
		"cat.core.PorgCompanyCodeCombo AS PorgCompanyCodeCombo PARTITION SAP WHERE 1 = 2";


		// get the companycode
		companyCode = (String) ((BaseObject) context).getDottedFieldValue("CompanyCode.UniqueName");
		Log.customer.debug(" CATSAPFilterPurchaseOrg: companyCode => "+companyCode);

		if(companyCode!=null)
		{
		queryText = "SELECT DISTINCT POCCC.PurchaseOrg, POCCC.PurchaseOrg.UniqueName, POCCC.PurchaseOrg.Name " +
				"FROM cat.core.PorgCompanyCodeCombo AS POCCC " +
				"WHERE ( POCCC.PurchaseOrg.IsSAPPurchaseOrg = 'N' AND " +
				"POCCC.PurchaseOrg.Restricted = FALSE AND" +
				" POCCC.CompanyCode.UniqueName = '" + companyCode + "') ";

		Log.customer.debug(" CATSAPFilterPurchaseOrg: after adding CompanyCode condition to query => "+queryText);


		ariba.base.core.Partition currentPartition = (ariba.base.core.Partition) Base.getSession().getPartition();
		Log.customer.debug(" CATSAPFilterPurchaseOrg: currentPartition => "+currentPartition);



		// Get the Requester Purchase Org also
		ariba.user.core.User requester = (ariba.user.core.User) ((BaseObject) context).getDottedFieldValue("Requester");
		Log.customer.debug(" CATSAPFilterPurchaseOrg: requester => " + requester);
		ariba.common.core.User partUser = ariba.common.core.User.getPartitionedUser(requester,currentPartition);
		Log.customer.debug(" CATSAPFilterPurchaseOrg: partUser => " + partUser);

		String userPOrgUniqueName = null;
		if(partUser.getDottedFieldValue("PurchaseOrg")!=null)
		{
			userPOrgUniqueName = (String) partUser.getDottedFieldValue("PurchaseOrg.UniqueName");
			String userPOrgCond =" OR ( POCCC.PurchaseOrg.UniqueName =  '"+userPOrgUniqueName+"' " +
					" AND POCCC.CompanyCode.UniqueName = '" + companyCode +
					"' AND POCCC.PurchaseOrg.IsSAPPurchaseOrg = 'N' ) " ;

			Log.customer.debug(" CATSAPFilterPurchaseOrg: userPOrgCond => "+userPOrgCond);
			queryText = queryText.concat(userPOrgCond);
			Log.customer.debug(" CATSAPFilterPurchaseOrg: After adding user profile porg cond query => "+queryText);

		}
		Log.customer.debug(" CATSAPFilterPurchaseOrg: query => "+queryText);

		}
		query = AQLQuery.parseQuery(queryText);
		if (strPattern != null && !strPattern.trim().equals("")) {
			AQLCondition patterncond;
			patterncond = AQLCondition.parseCondition("PurchaseOrg." + field
					+ " like '%" + strPattern.trim() + "%'");
			query.and(patterncond);
		}
		Log.customer.debug(" CATSAPFilterPurchaseOrg: Final Query => "+query);
		return query;
	}
}
