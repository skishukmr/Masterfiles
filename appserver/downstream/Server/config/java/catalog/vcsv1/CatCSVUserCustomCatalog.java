/*
 * Created by Chandra on May 17, 2006
 * --------------------------------------------------------------
 * Catalog filter for vcsv1-US partition
 *
 * AccessType:
 *"Full","Full Access to MSC","By default all users have full access"
 *"No","No Access to MSC","Restricted user can not access system"
 *"Partial","Partial Access","User able to see catalogs only - Can NOT see Contracts"
 *"Limited","Limited Access","User only able to create Non-Catalog item, can not see catalogs or contracts"
 *
 ChangeLog:
 	Date		Name		Description
 	--------------------------------------------------------------------------------------------------------------------------------------------------
 	5/22/2008 	Depak   Issue 819	Handle null in  conditionText

 */
package config.java.catalog.vcsv1;

import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommonSupplier;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.server.CustomCatalog;
import ariba.purchasing.core.Requisition;
import ariba.util.log.Log;

public class CatCSVUserCustomCatalog implements CustomCatalog {

    private static final String THISCLASS = "CatCSVUserCustomCatalog";
	private static String unspecifiedSupplierID = null;

    public void initialize() {
        CommonSupplier cs = CommonSupplier.tryGetPartialItemCommonSupplier();
        if(cs != null) {
            unspecifiedSupplierID = cs.getUniqueID();
		}
	}

    public AQLCondition customCatalogConstraint(AQLQuery catalogQuery, Partition partition,
		            ValueSource user, ValueSource plic) {

		Log.customer.debug("%s *** catalogQuery - " + catalogQuery, THISCLASS);
		Log.customer.debug("%s ***customCatalogConstraint  plic=" + plic, THISCLASS);
		Log.customer.debug("%s ***customCatalogConstraint user=" + user, THISCLASS);

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

		// AUL, sdey : check for plic to correct null pointer excp
		if(plic != null){
			BaseId plicBid = (BaseId)plic.getFieldValue("BaseId");
			ProcureLineItemCollection plicObj = (ProcureLineItemCollection)Base.getSession().objectFromId(plicBid);
			Log.customer.debug("%s *** plicObj is  =%s", THISCLASS, plicObj);
		}
        String conditionText = null;
        String accessType = userAccessType(user, plic);
        if (accessType == null || (accessType != null
        		&& (accessType.equalsIgnoreCase("FULL") || accessType.equalsIgnoreCase("PARTIAL"))) ) {
			Log.customer.debug("%s *** access type is FULL/PARTIAL or null, return condition NULL!", THISCLASS);
            return null;
        }
        else {
			Log.customer.debug("%s *** access type is =%s",THISCLASS, accessType);
			/* //Currently only limited access is implemented.
			if (accessType.equalsIgnoreCase("Partial")) {

		        //    If access type is Partial, show catalogs only and NO contracts,
		        //    set the field constraints to exclude MA suppliers for this user.
		        //    Note: If suppliers have catalog and contract items - both dont show

		        List maIds = MasterAgreement.getOpenMASubscriptionIdsForUser(userObj, plicObj, null, null);
		        Log.customer.debug("%s *** : maIds for User %s are: %s",THISCLASS, userObj, maIds.size());

		        List aqlExprList = AQLScalarExpression.buildScalarExpressionList(maIds);
		        Log.customer.debug("%s *** List of aqlExprList=%s",THISCLASS, ListUtil.listToCSVString(aqlExprList));

				//conditionText = "CatalogEntry.Description.CAPSChargeCodeID = '001'";
            	//conditionText = Fmt.S(conditionText, partition.getName(), ListUtil.listToCSVString(aqlExprList));
            	conditionText = "CatalogEntry.Description.CommonSupplier IS NOT NULL";
            } else
            */
			if(accessType.equalsIgnoreCase("Limited")) {
		        /*
		            If access type is Limited, no catalogs or contracts, only non-catalog
		            set the field constraints so that no records will be returned.
		        */
            	conditionText = "CatalogEntry.Description.CommonSupplier = NULL ";
			}
        }
        Log.customer.debug("%s *** custom catalog conditionText=%s",THISCLASS, conditionText);

        // start Issue 819

        if (conditionText !=null) {
        return AQLCondition.parseCondition(conditionText);
        }
        else
        return null;
      // end  Issue 819
    }


    public Object customCatalogHash(Partition partition, ValueSource user, ValueSource plic) {
        return null;
    }

	/*
	* Returns the User Access Type for the user
	*/
    protected String userAccessType(ValueSource user, ValueSource plic) {
        String mscAccessTypeStr = null;
        if(user != null && plic instanceof Requisition) {

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
        	    mscAccessTypeStr = (String)mscAccessObj.getFieldValue("UniqueName");
        }
        return mscAccessTypeStr;
    }


    public CatCSVUserCustomCatalog() {
        super();
    }

}
