/*
 * Created by KS on May 8, 2005
 * --------------------------------------------------------------
 * Catalog filter for mfg1 partition based on if supplier is authorized for the
 * (a) SiteFacility on the Requisition OR if null, then (b) User's AccountingFacility
 */
package config.java.catalog.vcsv2;

import config.java.common.CatConstants;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLCondition;
import ariba.base.core.aql.AQLQuery;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommonSupplier;
import ariba.procure.server.CustomCatalog;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Fmt;
import ariba.util.log.Log;


public class CatMFGCustomCatalog implements CustomCatalog {

    private static final String THISCLASS = "CATMFGCustomCatalog";
    private static final String AUTHORIZED = "Authorized";
    private static String unspecifiedSupplierID = null;

    public void initialize() {

        CommonSupplier cs = CommonSupplier.tryGetPartialItemCommonSupplier();
        if(cs != null)
            unspecifiedSupplierID = cs.getUniqueID();
    }

    public AQLCondition customCatalogConstraint(AQLQuery catalogQuery, Partition partition,
            ValueSource user, ValueSource plic) {

        String conditionText = null;
        String facility = bestFacility(user, plic);
        if (facility == null) {
        	if (CatConstants.DEBUG)
        	    Log.customer.debug("%s *** Facility is null, return NULL!",THISCLASS);
            return null;
        }
        else {
            String authorizedKey = AUTHORIZED + facility;
        	if (CatConstants.DEBUG)
        	    Log.customer.debug("%s *** Authorized Key: %s",THISCLASS,authorizedKey);
            conditionText = "CatalogEntry.Description.CommonSupplier IN (SELECT Distinct(Supplier.CommonSupplier) " +
            		"FROM ariba.common.core.Supplier Supplier PARTITION \"%s\" SUBCLASS NONE, " +
            		"ariba.common.core.SupplierLocation Location PARTITION \"%s\" SUBCLASS NONE " +
            		"WHERE Location.Supplier = Supplier AND Location.%s = TRUE)";
            conditionText = Fmt.S(conditionText, partition.getName(), partition.getName(), authorizedKey);
        	if (CatConstants.DEBUG)
        	    Log.customer.debug("%s *** conditionText: %s",THISCLASS,conditionText);
        }
        return AQLCondition.parseCondition(conditionText);
    }


    public Object customCatalogHash(Partition partition, ValueSource user, ValueSource plic) {

        return bestFacility(user,plic);
    }


    protected String bestFacility(ValueSource user, ValueSource plic)
    {
        String fac = null;
        if(fac == null && plic instanceof Requisition) {
            ClusterRoot facObj = (ClusterRoot)plic.getFieldValue("SiteFacility");
        	if (CatConstants.DEBUG)
        	    Log.customer.debug("%s *** Facility set from Requisition SiteFacility: %s",THISCLASS,fac);
        	if (facObj != null)
        	    fac = facObj.getUniqueName();
        }
        if(fac == null && user != null) {

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

    		fac = (String)user.getFieldValue("AccountingFacility");
        	if (CatConstants.DEBUG)
        	    Log.customer.debug("%s *** Facility set from User AccountingFacility: %s",THISCLASS,fac);
        }
        return fac;
    }

    public CatMFGCustomCatalog() {
        super();
    }

}
