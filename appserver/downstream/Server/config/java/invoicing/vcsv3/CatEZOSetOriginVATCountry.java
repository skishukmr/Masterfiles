package config.java.invoicing.vcsv3;

import ariba.approvable.core.Approvable;
import ariba.base.core.Log;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.Address;
import ariba.basic.core.Country;
import ariba.invoicing.core.Invoice;
import ariba.util.core.PropertyTable;

 public class CatEZOSetOriginVATCountry extends Action {

 	private static final String ClassName = "CatEZOSetOriginVATCountry";

    public void fire(ValueSource object, PropertyTable params)
            throws ActionExecutionException {

        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** Object: %s",ClassName, object);
        Address sloc = null;
        Country country = null;

        if (object instanceof Invoice) {
            Invoice inv = (Invoice)object;
            sloc = (Address)inv.getSupplierLocation();
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** SupLoc Address: %s",ClassName, sloc);
            country = getVATCountryFromSupLoc(sloc);
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Country from RemitTo: %s",ClassName, country);
            if (country != null) {
                inv.setDottedFieldValueRespectingUserData("OriginVATCountry",country);
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** Set Inv# %s OriginVATCountry to: %s",ClassName,
                            inv.getUniqueName(),country.getUniqueName());
            }
	        else {
	            inv.setDottedFieldValueRespectingUserData("OriginVATCountry",null);
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** Set InvEForm# %s OriginVATCountry to NULL!",ClassName,
                            inv.getUniqueName());
	        }
        }
	    else if (object instanceof Approvable) {  // assume an eForm
	        Approvable eform = (Approvable)object;
	        sloc = (Address)eform.getFieldValue("SupplierLocation");
            country = getVATCountryFromSupLoc(sloc);
            if (country != null) {
                eform.setDottedFieldValueRespectingUserData("OriginVATCountry",country);
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** Set InvEForm# %s OriginVATCountry to: %s",ClassName,
                            eform.getUniqueName(),country.getUniqueName());
            }
	        else {
	            eform.setDottedFieldValueRespectingUserData("OriginVATCountry",null);
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** Set InvEForm# %s OriginVATCountry to NULL!",ClassName,
                            eform.getUniqueName());
	        }
	    }
    }

    private Country getVATCountryFromSupLoc(Address sloc) {

        Country vatCountry = null;
        if (sloc != null) {
            Address remitTo = (Address)sloc.getFieldValue("RemitTo");
	        if (remitTo != null) {
	            vatCountry = (Country)remitTo.getDottedFieldValue("PostalAddress.Country");

	        }
        }
        return vatCountry;
    }
}
