/*
 * Created by KS on May 6, 2005
 * --------------------------------------------------------------
 * Used to validate MAR lines/splits accounting has matching cat.core.AccountingCombinations
 * 08.03.05 Added validation for currency
 */
package config.java.hook.vcsv2;


import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseVector;
import ariba.base.core.MultiLingualString;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.action.vcsv2.CatValidateMFGAccounting;
import config.java.common.CatConstants;


public class CatMFGContractRequestSubmitHook implements ApprovableHook {

    private static final String THISCLASS = "CatMFGContractRequestSubmitHook";
    private static final List NOERROR = ListUtil.list(Constants.getInteger(0));
    private static final String LINEMARKER = ResourceService.getString("cat.java.vcsv2","SubmitHookLineMarker_Default");
    protected static final String RETURNCODE = ResourceService.getString("cat.java.vcsv2","SubmitHookReturnCode_ContractRequest");
    private static String ErrorMsg = ResourceService.getString("cat.java.common","ErrorSupplierCurrencyMismatch");
    private static boolean debug = CatConstants.DEBUG;
    private static String CurrencyError;


	public List run(Approvable approvable) {

        if (approvable instanceof ContractRequest) {
            ContractRequest mar = (ContractRequest)approvable;
            FastStringBuffer totalMsg = new FastStringBuffer ();
            boolean hasErrors = false;
            boolean hasAcctngErrors = false;
            int release = mar.getReleaseType();
        	BaseVector lines = mar.getLineItems();
        	int size = lines.size();
        	for (int i = 0; i < size; i++) {
        	    ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(i);

//  Currency Validation (Line Item vs. Supplier Default Currency) - added 08.03.05
        	    if (!isCurrencyMatch(mali) && CurrencyError != null) {
    	            hasErrors = true;
                	totalMsg.append(Fmt.S(LINEMARKER, String.valueOf(mali.getLineItemNumber())));
    	        	totalMsg.append(CurrencyError);
    	        	totalMsg.append(". ");
        	    }

 //  Accounting Validation
        	    Boolean useAccounting = (Boolean)mali.getFieldValue("UseAccountingOnRLI");
        	    if (debug)
        	        Log.customer.debug("CatContractRequestSubmitHook *** release type / useAccounting: "
        	                + release + useAccounting);
        	    if (release == 0 || (useAccounting != null && useAccounting.booleanValue())) {
        	        String validateError = CatMFGRequisitionSubmitHook.validatePLIAccountings(mali);
                    if (debug)
                        Log.customer.debug("%s *** Line Error? %s", THISCLASS, validateError);
            	    if (validateError != null) {
            	        hasErrors = true;
            	        hasAcctngErrors = true;
            	        totalMsg.append(validateError);
            	    }
        	    }
    	    }
  	      	if (hasErrors) {
		      	int rtnCode = -1;
		      	if (!StringUtil.nullOrEmptyOrBlankString(RETURNCODE))
		      	    rtnCode = Integer.valueOf(RETURNCODE).intValue();
  	      	    if (hasAcctngErrors)
  	      	        totalMsg.append(CatValidateMFGAccounting.AdditionalMessage);
		      	if (debug)
		      	    Log.customer.debug("%s *** Total Error Msg: %s", THISCLASS, totalMsg.toString());
  	      		return ListUtil.list(Constants.getInteger(rtnCode), totalMsg.toString());
  	      	}
        }
		return NOERROR;
	}

	public static boolean isCurrencyMatch(ProcureLineItem pli) {

	    CurrencyError = null;
	    boolean isMatch = true;
		if (pli != null) {
	        SupplierLocation sloc = pli.getSupplierLocation();
	        Supplier suplr = pli.getSupplier();
	        if (sloc != null) {
				if (debug)
					Log.customer.debug("%s *** suplr loc: %s",THISCLASS, sloc);
	            Currency curr1 = (Currency)sloc.getFieldValue("Currency");
				if (curr1 != null) {
					if (debug)
						Log.customer.debug("%s *** currency1: %s",THISCLASS, curr1);
				    LineItemProductDescription lipd = pli.getDescription();
				    if (lipd != null) {
					    Money price = lipd.getPrice();
					    if (price != null && !price.isApproxZero()) {
					        Currency curr2 = price.getCurrency();
						    if (debug)
								Log.customer.debug("%s *** currency2: %s",THISCLASS, curr2);
					        if (!curr1.equals(curr2))
					            isMatch = false;
					        	MultiLingualString curr1Name = curr1.getName();
					        	if (curr1Name != null)
					        	    CurrencyError = Fmt.S(ErrorMsg,curr1.getUniqueName(),curr1Name.getPrimaryString());
					        	else
					        	    CurrencyError = Fmt.S(ErrorMsg,curr1.getUniqueName(),curr1.getUniqueName());
					    }
				    }
				}
			}
		}
		if (debug)
		    Log.customer.debug("CatContractRequestSubmitHook *** isCurrencyMatch?: " + isMatch);
		return isMatch;
	}

	public CatMFGContractRequestSubmitHook() {
		super();
	}


}
