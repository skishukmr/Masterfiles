package config.java.invoicing.vcsv3;

import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.Contract;
import ariba.invoicing.core.Invoice;
import ariba.payment.core.PaymentTerms;
import ariba.util.core.PropertyTable;

 public class CatEZOSetInvoicePayTerms extends Action {

 	private static final String ClassName = "CatEZOSetInvoicePayTerms";

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        if (object instanceof Invoice) {
            Invoice inv = (Invoice)object;

            if(inv.getLoadedFrom() == 4) {

	            Contract ma = inv.getSelectedMasterAgreement();
	    		//if (Log.customer.debugOn)
	    		    Log.customer.debug("%s *** SelectedMasterAgreement: %s",ClassName,ma);
	            if (ma != null) {

	                PaymentTerms payTerms = ma.getPaymentTerms();
	        		//if (Log.customer.debugOn)
	        		    Log.customer.debug("%s *** MA PayTerms: %s",ClassName,payTerms);

	        		inv.setPaymentTerms(payTerms);
	        	}
            }
        }
    }
}
