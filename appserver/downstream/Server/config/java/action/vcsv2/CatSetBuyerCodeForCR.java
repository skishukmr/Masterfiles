/*
 * Created by SHaila on sept 18 2008
 * ---------------------------------------------------------------------------------
 * Used to set BuyerCode based PLIC.SiteFacility and CEME.BuyerCodePrefix
 */
package config.java.action.vcsv2;

import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommodityExportMapEntry;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatConstants;
import ariba.contract.core.ContractRequestLineItem;
import ariba.user.core.User;

public class CatSetBuyerCodeForCR extends Action {

	private static final String THISCLASS = "CatSetBuyerCodeForCR";
	private static final String BC_CLASS = "cat.core.BuyerCode";
	private static final String CapitalBC = "82";
	private static final String NA_BC = CapitalBC;
	private static final String NonDX_BC = "86";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ProcureLineItem){
        	ContractRequestLineItem pli = (ContractRequestLineItem)object;
            StringBuffer bcUnique = null;
            ClusterRoot acctType = (ClusterRoot)pli.getFieldValue("AccountType");
            ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
            if (plic != null) {
                User user = (User)plic.getRequester();
                String facUN = (String)user.getFieldValue("AccountingFacility");
                   if (facUN != null) {
                    if (acctType != null && acctType.getUniqueName().equals("Capital")) {
                        bcUnique = new StringBuffer(CapitalBC).append(facUN);
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (1) Set for Capital", THISCLASS);
                    }
                    else if (facUN.equals("NA")) {
                        bcUnique = new StringBuffer(NA_BC).append(facUN);
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (2) Set for NA Site", THISCLASS);
                    }
                    else if (!facUN.equals("DX")) {
                        bcUnique = new StringBuffer(NonDX_BC).append(facUN);
                        if (CatConstants.DEBUG)
                            Log.customer.debug("%s *** (3) Set for MX/MY Site", THISCLASS);
                    }
                    else {  // must mean Revenue and DX Site
                        CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
                        if (ceme != null) {
                            String bcPrefix = (String)ceme.getFieldValue("BuyerCodePrefix");
                            if (!StringUtil.nullOrEmptyOrBlankString(bcPrefix)) {
                                bcUnique = new StringBuffer(bcPrefix).append(facUN);
                                if (CatConstants.DEBUG)
                                    Log.customer.debug("%s *** (4) Set for CEME", THISCLASS);
                            }
                        }
                    }
                }
            }
            if (bcUnique != null) {
	            ClusterRoot buyercode = Base.getService().objectMatchingUniqueName(BC_CLASS,
	                    pli.getPartition(),bcUnique.toString());
	            if (buyercode != null)
	                pli.setFieldValue("BuyerCode",buyercode);
	            if (CatConstants.DEBUG)
	                Log.customer.debug("%s *** bcUnique: %s, BuyerCode obj: %s",
	                        THISCLASS, bcUnique.toString(),buyercode);
            }
        }
    }

    public CatSetBuyerCodeForCR() {
        super();
    }

}
