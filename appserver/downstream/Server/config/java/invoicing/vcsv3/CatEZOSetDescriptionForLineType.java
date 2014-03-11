
package config.java.invoicing.vcsv3;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.Invoice;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

/**  Author: KS.
	Simple trigger to reset VAT values for a VAT line type
*/

public class CatEZOSetDescriptionForLineType extends Action {

    private static final String ClassName = "CatEZOSetDescriptionForLineType";
    private static final String vat = "VAT";
    private static final String tax = "Tax";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

         if (object instanceof InvoiceLineItem) {

            InvoiceLineItem invLine = (InvoiceLineItem)object;
            Log.customer.debug("%s *** Setting Description for Line Type.",ClassName);
            ProcureLineType plt = invLine.getLineType();
            if (plt != null && plt.getCategory()==ProcureLineType.TaxChargeCategory) {

                LineItemProductDescription lipd = invLine.getDescription();
                if (lipd != null) {
	                Invoice invoice = (Invoice)invLine.getLineItemCollection();
	                int source = invoice.getLoadedFrom();
	                if (source == Invoice.LoadedFromUI) {
	                    if (plt.getUniqueName().indexOf(vat)<0) { // means non-vat
	                        Log.customer.debug("%s *** Found Paper & non-VAT tax, switching Desc.",ClassName);
		                    String desc = lipd.getDescription();
		                    desc = desc.replaceAll(vat,tax);
		                    lipd.setDescription(desc);
	                    }
	                }
	                else if (source != Invoice.LoadedFromEForm) {  // must mean ASN/cXML
	                    if (plt.getUniqueName().indexOf(vat)>-1) {
		                    Log.customer.debug("%s *** Found ASN/File & VAT tax, switching Desc.",ClassName);
		                    String desc = lipd.getDescription();
		                    Log.customer.debug("%s *** Current Description: %s",ClassName,desc);
			                if (!StringUtil.nullOrEmptyOrBlankString(desc))
			                    desc = desc.replaceAll(tax,vat);
			                else
			                    desc = vat;
		                    Log.customer.debug("%s *** ReplaceAll Description: %s",ClassName,desc);
		                    lipd.setDescription(desc);
		                    Log.customer.debug("%s *** 1. After Setting DESC: %s",ClassName,lipd.getDescription());
		                    lipd.setDescription(vat);
		                    Log.customer.debug("%s *** 2. After Setting VAT: %s",ClassName,lipd.getDescription());
	                    }
	                }
                }
            }
        }
    }

    public CatEZOSetDescriptionForLineType() {
        super();
    }


}
