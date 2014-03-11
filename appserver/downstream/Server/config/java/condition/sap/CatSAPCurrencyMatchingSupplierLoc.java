package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.base.core.MultiLingualString;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.Supplier;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.CatConstants;

public class CatSAPCurrencyMatchingSupplierLoc extends Condition {

	private static final String THISCLASS = "CatSAPCurrencyMatchingSupplierLoc";
	private static String ErrorMsg = ResourceService.getString("cat.java.common","ErrorSupplierCurrencyMismatch");
	private static String FmtError;


    public boolean evaluate(Object object, PropertyTable params)
    {
        return matchingCurrency(object);
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException  {

	    if(!evaluate(object, params)) {
			if (CatConstants.DEBUG)
			    Log.customer.debug("%s *** Error Message: %s", THISCLASS, FmtError);
			return new ConditionResult(FmtError);
		}
		return null;
    }

	public static boolean matchingCurrency(Object object) {

		boolean isMatch = true;
		if (CatConstants.DEBUG)
		    Log.customer.debug("%s *** object: " + object);
		if(object instanceof LineItemProductDescription) {
		    LineItemProductDescription lipd = (LineItemProductDescription)object;
		    LineItem li = lipd.getLineItem();
		    if (li instanceof ProcureLineItem) {
		        ProcureLineItem pli = (ProcureLineItem)li;
		        Boolean isCurrencyValidityRequired = (Boolean)pli.getLineItemCollection().getDottedFieldValue("CompanyCode.IsCurrencyValidityRequired");
		        if(isCurrencyValidityRequired !=null && isCurrencyValidityRequired.booleanValue()){
		        SupplierLocation sloc = pli.getSupplierLocation();
		        Supplier suplr = pli.getSupplier();
		        if (sloc != null) {
 					if (CatConstants.DEBUG)
 						Log.customer.debug("%s *** suplr loc: %s",THISCLASS, sloc);
		            Currency curr1 = (Currency)sloc.getFieldValue("DefaultCurrency");
    				if (curr1 != null) {
    				    Money price = lipd.getPrice();
    				    if (price != null && !price.isApproxZero()) {
    				        Currency curr2 = price.getCurrency();
    					    if (CatConstants.DEBUG)
    							Log.customer.debug("%s *** currency2: %s",THISCLASS, curr2);
    				        if (!curr1.equals(curr2))
    				            isMatch = false;
    				        	MultiLingualString curr1Name = curr1.getName();
    				        	if (curr1Name != null)
    				        	    FmtError = Fmt.S(ErrorMsg,curr1.getUniqueName(),curr1Name.getPrimaryString());
    				        	else
    				        	    FmtError = Fmt.S(ErrorMsg,curr1.getUniqueName(),curr1.getUniqueName());
    				    	}
    					}
					}
		        }
		    }
		}
		if (CatConstants.DEBUG)
		    Log.customer.debug("CatSAPCurrencyMatchingSupplierLoc *** isMatch?: " + isMatch);
		return isMatch;
	}


	public CatSAPCurrencyMatchingSupplierLoc() {
		super();
	}

}