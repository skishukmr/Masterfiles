/*
 * Created by KS on May 7, 2005
 * --------------------------------------------------------------
 * Used to validate 1) lines/splits accounting has matching cat.core.AccountingCombinations
 * and 2) consistent PaymentTerms across lines with same supplier
 */
package config.java.hook.vcsv2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseVector;
import ariba.base.core.Partition;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.action.vcsv2.CatValidateMFGAccounting;
import config.java.common.CatConstants;

public class CatMFGRequisitionSubmitHook implements ApprovableHook {

    private static final String THISCLASS = "CatMFGRequisitionSubmitHook";
    private static final List NOERROR = ListUtil.list(Constants.getInteger(0));
    private static final String LINEMARKER = ResourceService.getString("cat.java.vcsv2","SubmitHookLineMarker_Default");
    private static final String PTMESSAGE = ResourceService.getString("cat.java.vcsv2","SubmitHookPayTermsError");;
    protected static final String RETURNCODE = ResourceService.getString("cat.java.vcsv2","SubmitHookReturnCode_Requisition");
    private static final String ErrorInActiveApprover=ResourceService.getString("cat.vcsv1","ErrorInActiveApprover");
    private static final String ErrorApprover_PreparerCannotApproveRequestionAsDA= ResourceService.getString("cat.vcsv1","ErrorApprover_PreparerCannotApproveRequestionAsDA");
    private static boolean debug = CatConstants.DEBUG;
    private boolean hasErrors = false;
    private boolean hasWarnings = false;

	public List run(Approvable approvable) {

        if (approvable instanceof Requisition) {
            Requisition req = (Requisition)approvable;
            Partition partition=req.getPartition();
            FastStringBuffer acctngMsg = new FastStringBuffer();
    	    FastStringBuffer termsMsg = new FastStringBuffer();
            boolean hasBadTerms = false;
            HashMap termsMap = new HashMap();
        	BaseVector lines = req.getLineItems();
        	int size = lines.size();
        	for (int i = 0; i < size; i++) {
        	    ReqLineItem rli = (ReqLineItem)lines.get(i);

// Test 1 (ERROR): Accounting Validation (all lines, all splits)
        	    String validateError = validatePLIAccountings(rli);
                if (debug)
                    Log.customer.debug("%s *** Line Error? %s", THISCLASS, validateError);
        	    if (validateError != null) {
        	        hasErrors = true;
        	        acctngMsg.append(validateError);
        	    }
// Test 2 (WARNING): PaymentTerms (compare terms if same supplier)
        	    Object payterms = rli.getFieldValue("PaymentTerms");
        	    SupplierLocation suploc = rli.getSupplierLocation();
        	    if (suploc != null && payterms != null) {
       	            if (!termsMap.containsKey(suploc)) {
       	                termsMap.put(suploc, payterms);
       	                if (debug)
       	                    Log.customer.debug("%s *** Adding SupLoc/PayTerm to Map: %s / %s",
       	                            THISCLASS, suploc, payterms);
       	            }
       	            else {
       	                if (payterms != termsMap.get(suploc)) {
       	                    hasWarnings = true;
       	                    termsMsg.append(PTMESSAGE);
       	                    String supplierName = suploc.getSupplier().getName();
       	                    if (supplierName != null)
       	                        termsMsg.append(Fmt.S(" (Supplier Name: %s). ", supplierName));
       	                }
       	                if (debug)
       	                    Log.customer.debug("%s *** PayTerms Msg: %s", THISCLASS, termsMsg.toString());
       	            }
        	    }
        	}
// Return 1) Accounting error OR 2) PayTerms warning (not both)
  	      	if (hasErrors) {
  	      	    acctngMsg.append(CatValidateMFGAccounting.AdditionalMessage);
		      	int rtnCode = -1;
		      	if (!StringUtil.nullOrEmptyOrBlankString(RETURNCODE))
		      	    rtnCode = Integer.valueOf(RETURNCODE).intValue();
  	      		return ListUtil.list(Constants.getInteger(rtnCode), acctngMsg.toString());
  	      	}
  	      	if (hasWarnings)
  	      	    return ListUtil.list(Constants.getInteger(1),termsMsg.toString());
  	      	if (!checkforactiveCCAP(lines,partition))
			    return ListUtil.list(Constants.getInteger(-2), ErrorInActiveApprover);
			if (!checkforPreparerasDA(lines,partition))
                return ListUtil.list(Constants.getInteger(-2), ErrorApprover_PreparerCannotApproveRequestionAsDA);
        }
		return NOERROR;
	}

    protected static String validatePLIAccountings (ProcureLineItem pli) {

        String result = null;
        FastStringBuffer lineMsg = new FastStringBuffer();
        Iterator itr = pli.getAccountings().getSplitAccountingsIterator();
        while (itr.hasNext()) {
            SplitAccounting sa = (SplitAccounting)itr.next();
            int indicator = CatValidateMFGAccounting.validateAccounting(sa);
            if (indicator > 0) {
                String lineError = CatValidateMFGAccounting.getValidationMessage(indicator);
                if (lineError != null) {
                    int errorLine = pli.getLineItemNumber();
                    lineMsg.append(Fmt.S(LINEMARKER, String.valueOf(errorLine)));
                    lineMsg.append(lineError);
                    result = lineMsg.toString();
                }
            }
        }
        return result;
    }
public static boolean checkforactiveCCAP(BaseVector lines, Partition partition){
		for(int i=0;i<lines.size();i++)
		{
		  Log.customer.debug("CatCSVReqSubmitHook***Loop for getting the Requisition LineItem");
		  ReqLineItem rli1 = (ReqLineItem)lines.get(i);
		       if( rli1!=null){
		          SplitAccountingCollection sac = rli1.getAccountings();
		       if (sac != null){
		          Log.customer.debug("CatCSVReqSubmitHook*** SplitAccountingCollection"+sac);
		          BaseVector splits = sac.getSplitAccountings();
		          SplitAccounting sa = (SplitAccounting)splits.get(0);
		       if(sa!=null){
		          Log.customer.debug("CatCSVReqSubmitHook*** SplitAccounting"+sa);
		          ariba.user.core.User approver = (ariba.user.core.User)sa.getFieldValue("CostCenterApprover");
		       if (approver != null){
		          ariba.common.core.User partuser = ariba.common.core.User.getPartitionedUser(approver,partition);
		          Log.customer.debug("CatCSVReqSubmitHook***Common.Core.UserDA"+partuser);
                  Log.customer.debug(partuser.getFieldValue("Active"));
                  Log.customer.debug(partuser);
               if (partuser.getFieldValue("Active").toString().equalsIgnoreCase("false")){
		           Log.customer.debug("CatCSVReqSubmitHook***Designated Approver is not active"+partuser);
		           return false;
		                             }
		       else{

		           Log.customer.debug("CatCSVReqSubmitHook***Designated approver Active"+partuser);
		           return true;
                   }
			      }
		         }
		        }
		       }
		      }
    	     return true;
}
public static boolean checkforPreparerasDA(BaseVector lines, Partition partition){
 for(int i=0;i<lines.size();i++){
     Log.customer.debug("CatCSVReqSubmitHook***Loop for getting the Requisition LineItem");
     ReqLineItem rli1 = (ReqLineItem)lines.get(i);
 if ( rli1!=null){
     SplitAccountingCollection sac = rli1.getAccountings();
 if (sac != null){
    Log.customer.debug("CatCSVReqSubmitHook*** SplitAccountingCollection"+sac);
    BaseVector splits = sac.getSplitAccountings();
    SplitAccounting sa = (SplitAccounting)splits.get(0);
 if (sa!=null){
    LineItemCollection lic = sa.getLineItem().getLineItemCollection();
    Log.customer.debug("CatCSVReqSubmitHook*** SplitAccounting"+sa);
    ariba.user.core.User approver = (ariba.user.core.User)sa.getFieldValue("CostCenterApprover");
if (approver==lic.getPreparer()){
   return false;
   }
else{
   Log.customer.debug("CatCSVReqSubmitHook*** Preparer is not CCA");
     return true;
     }

   }
  }
 }
}
    return true;
}

	public CatMFGRequisitionSubmitHook() {
		super();
	}


}
