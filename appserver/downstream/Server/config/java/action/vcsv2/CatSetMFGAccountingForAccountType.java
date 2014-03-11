/*
 * Created by KS on April 24, 2005
 * Updated by KS on May 16, 2005 (to set defaults for Capital)
 * --------------------------------------------------------------
 * Used to set RLI and MALI accounting based on AccountType and UseAccounting field
 */
package config.java.action.vcsv2;

import java.util.HashMap;
import java.util.Iterator;

import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Accounting;
import ariba.common.core.CommodityExportMapEntry;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.User;
import ariba.contract.core.ContractCoreApprovableLineItem;
import ariba.contract.core.ContractLineItem;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.CatConstants;

/*
 * AUL: Changed MasterAgreement to Contract
 */


public class CatSetMFGAccountingForAccountType extends Action {

	private static final String THISCLASS = "CatSetMFGAccountingForAccountType";
	private static final String SHIBAURA = "NA";
	private static final String KEY_ACCOUNT = "CapitalAccountHashValues";
	private static final String KEY_COSTCTR = "CapitalCostCenterHashValues";
	private static final String KEYFILE = "cat.java.vcsv2";
	private static boolean debug = CatConstants.DEBUG;


	public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

		if (object instanceof ProcureLineItem) {
		    ProcureLineItem pli = (ProcureLineItem)object;
		    ClusterRoot type = (ClusterRoot)pli.getFieldValue("AccountType");
            if (type != null) {
                String accounttype = type.getUniqueName();
                ClusterRoot [] inputs = null;
	            if (debug)
	                Log.customer.debug("%s *** AccountType: %s",THISCLASS, accounttype);
	// ReqLineItem
	            if (pli instanceof ReqLineItem) {
		             ReqLineItem rli = (ReqLineItem)object;
		             ContractLineItem mali = rli.getMALineItem();
		             if (mali == null || !useMAAccounting(mali)) {

		                 if (accounttype.equals("Revenue")) {
			                 inputs = getRevenueSourceAccounting(rli);
		                     setPLIAccounting(rli,inputs[0],inputs[1],inputs[2],null,false);
		                 }
		                 else { // must mean AccountType = Capital
			                 inputs = getCapitalSourceAccounting(rli);
		                     setPLIAccounting(rli,inputs[0],inputs[1],inputs[2],null,false);
		                 }
		                 if (debug)
		                     Log.customer.debug("%s *** RLI Source - Facility: %s, CostCenter: %s, Account: %s",
		 		     		 		THISCLASS, inputs[0],inputs[1],inputs[2]);
		             }
				}
	// MARLineItem
	            else if (pli instanceof ContractRequestLineItem) {
	            	ContractRequestLineItem mali = (ContractRequestLineItem)pli;
	                ContractRequest mar = mali.getMasterAgreementRequest();
	                if (mar.getReleaseType()==0){
	                    setPLIAccounting(mali,null,null,null,null,false);
	                    if (debug)
	                         Log.customer.debug("%s *** (A)Set MALI Accounting for NON-RELEASE type",THISCLASS);
	                }
	                else {
		                 if (!useMAAccounting(mali)) {
		                     setPLIAccounting(mali,null,null,null,null,false);
		                 	 if (debug)
		                 	    Log.customer.debug("%s *** (B-1)Set MALI Accounting for !useMAAccounting",THISCLASS);
		                 }
		                 else {
		                     if (accounttype.equals("Revenue")) {
		                         inputs = getRevenueSourceAccounting(mali);
		                         setPLIAccounting(mali,inputs[0],null,inputs[2],null,true);
			                     if (debug)
			                         Log.customer.debug("%s *** (B-2)Set MALI Accounting for REVENUE",THISCLASS);
		                     }
		                     else {  // Must mean AccountType = Capital
		                         inputs = getCapitalSourceAccounting(mali);
			                     setPLIAccounting(mali,inputs[0],inputs[1],inputs[2],null,false);
			                     if (debug)
			                         Log.customer.debug("%s *** (B-3)Set MALI Accounting for CAPITAL",THISCLASS);
		                     }
		                 }
	                }
	            }
	         }
		}
    	return;
    }

	public boolean useMAAccounting(ContractCoreApprovableLineItem li) {

	    boolean useIt = false;
	    if (li != null) {
	        Boolean ua = (Boolean)li.getFieldValue("UseAccountingOnRLI");
	        if (ua != null) {
	            useIt = ua.booleanValue();
	        }
	    }
	    return useIt;
	}

	 protected ClusterRoot[] getRevenueSourceAccounting(ProcureLineItem pli) {

	     // populate cValues in this order: (0)Facility, (1)CostCenter, (2)Account
	     ClusterRoot [] rValues = {null,null,null};
	     ariba.user.core.User user = pli.getLineItemCollection().getRequester();
	     CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
	     User puser = User.getPartitionedUser(user, pli.getPartition());
	     if (user != null) {
	     	Accounting ua = puser.getAccounting();
	     	if (ua != null) {
	         	rValues[0] = (ClusterRoot)ua.getFieldValue("Facility");
	         	rValues[1] = (ClusterRoot)ua.getFieldValue("CostCenter");
	     	}
	     }
         if (ceme != null)
             rValues[2] = (ClusterRoot)ceme.getFieldValue("Account");
         if (debug)
             Log.customer.debug("%s *** SOURCE Values - Facility: %s, CostCenter: %s, Account: %s",
	     		 		THISCLASS,rValues[0],rValues[1],rValues[2]);
         return rValues;
	 }

	 protected ClusterRoot[] getCapitalSourceAccounting(ProcureLineItem pli) {

	    // populate cValues in this order: (0)Facility, (1)CostCenter, (2)Account
	    ClusterRoot [] cValues = {null,null,null};
	    ProcureLineItemCollection plic = (ProcureLineItemCollection)pli.getLineItemCollection();
	    ClusterRoot site = (ClusterRoot)plic.getFieldValue("SiteFacility");
        if (debug)
            Log.customer.debug("%s *** Site Facility for Capital: %s", THISCLASS,site);
	    ariba.user.core.User user = plic.getRequester();
	    User puser = User.getPartitionedUser(user, pli.getPartition());
	    if (user != null) {
	        cValues[0] = (ClusterRoot)puser.getAccounting().getFieldValue("Facility");
	        if (debug)
	            Log.customer.debug("%s *** Attempted to set Facility for Capital!",THISCLASS);
	    }
	    if (site != null) {
     	    String fac = site.getUniqueName();
     	    String acctValues = Fmt.Sil(KEYFILE, KEY_ACCOUNT);
            HashMap aHash = CatSetSubAccount.getSubHash(acctValues);
            if (aHash != null && !aHash.isEmpty()) {
                String accountUN = (String)aHash.get(fac);
                if (accountUN != null) {
                    cValues[2] = Base.getService().objectMatchingUniqueName("ariba.core.Account",
                            Base.getSession().getPartition(),accountUN);
                    if (debug)
                        Log.customer.debug("%s *** Attempted to set Account for Capital!",THISCLASS);
                }
            }
            if (fac.equals(SHIBAURA)) {
                cValues[1] = null;
            }
            else {
         	    String ccValues = Fmt.Sil(KEYFILE, KEY_COSTCTR);
                HashMap ccHash = CatSetSubAccount.getSubHash(ccValues);
                if (ccHash != null && !ccHash.isEmpty()) {
                    String ccUN = (String)ccHash.get(fac);
                    if (ccUN != null) {
                        cValues[1]  = Base.getService().objectMatchingUniqueName("ariba.core.CostCenter",
                                Base.getSession().getPartition(),ccUN);
	                    if (debug)
	                        Log.customer.debug("%s *** Attempted to set CostCenter for Capital!",THISCLASS);
                    }
                }
            }
     	}
        if (debug)
            Log.customer.debug("%s *** SOURCE Values - Facility: %s, CostCenter: %s, Account: %s",
	     		 		THISCLASS,cValues[0],cValues[1],cValues[2]);
     	return cValues;
	 }

	 protected void setPLIAccounting (ProcureLineItem pli, ClusterRoot fac, ClusterRoot cc,
	         ClusterRoot acct, ClusterRoot project, boolean respect) {

	     SplitAccountingCollection sac = pli.getAccountings();
         if (sac != null) {
         	BaseVector splits = sac.getSplitAccountings();
         	if (!splits.isEmpty()) {
        		for (Iterator itr = splits.iterator() ; itr.hasNext() ;)  {
        		    SplitAccounting sa = (SplitAccounting)itr.next();
          			sa.setDottedFieldValueRespectingUserData("Project", project);
    				if(respect){
        				sa.setDottedFieldValueRespectingUserData("Facility", fac);
        				sa.setDottedFieldValueRespectingUserData("CostCenter", cc);
        				sa.setDottedFieldValueRespectingUserData("Account", acct);
    				} else {
        				sa.setDottedFieldValue("Facility", fac);
        				sa.setDottedFieldValue("CostCenter", cc);
        				sa.setDottedFieldValue("Account", acct);
    				}
            		if (debug) {
            		    Log.customer.debug("%s *** Account Set**: %s", THISCLASS, sa.getFieldValue("Account"));
            		    Log.customer.debug("%s *** CostCenter Set**: %s", THISCLASS, sa.getFieldValue("CostCenter"));
            		    Log.customer.debug("%s *** Facility Set**: %s", THISCLASS, sa.getFieldValue("Facility"));
            		}
        		}
         	}
         }
	 }

	public CatSetMFGAccountingForAccountType() {
		super();
	}
}