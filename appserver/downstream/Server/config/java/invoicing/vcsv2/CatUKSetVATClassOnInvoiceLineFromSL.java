/*
Author:  Shaila Salimath
date ; Feb 21 08
Purpose: CR # 755 The purpose of this class is to default the VATClass on the invoice line from Supplier Location.
 Added code to default 0 VATClass if the LineType is shipping charge
*/

package config.java.invoicing.vcsv2;

import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Log;
import ariba.invoicing.core.Invoice;
import ariba.util.core.Assert;
import ariba.util.core.PropertyTable;
public class CatUKSetVATClassOnInvoiceLineFromSL extends Action
{
       public void fire (ValueSource object, PropertyTable params)
	       {
	           Log.customer.debug("CatMFGUpdateTotalTax: begin\n");
	           Assert.that(object instanceof BaseObject, "BaseObject expected");
	           if (!(object instanceof ClusterRoot)) {
	               object = ((BaseObject)object).getClusterRoot();
	               Assert.that(object != null, "Cluster root not set");
	           }

	          setVATClasss(object);
    }

    private static void setVATClasss(ValueSource valuesource)
	{
		if ((valuesource != null) && (valuesource instanceof Invoice)) {
				ariba.base.core.Log.customer.debug(" CatMFGSetVATClassOnInvoiceLineFromSL Invoice = ");
				ClusterRoot vatClass = (ClusterRoot) valuesource.getDottedFieldValue("SupplierLocation.VATClass");
				if (vatClass!= null)
				     ariba.base.core.Log.customer.debug(" CatMFGSetVATClassOnInvoiceLineFromSL vatClass = " + vatClass.getUniqueName());
	                 List invoiceLine = (List)valuesource.getFieldValue("LineItems");
	                 int j = invoiceLine.size();
	                for(int k = 0; k < j; k++)
                    {
				       BaseObject line = (BaseObject)invoiceLine.get(k);
				       Integer category = (Integer) line.getDottedFieldValue("LineType.Category");

			     	    if (category != null) {
								ariba.base.core.Log.customer.debug(" CatMFGSetVATClassOnInvoiceLineFromSL category = " + category);
								//if shipping charge category, then set VATClass as "0" Zero vat class
								if (category.intValue() == 4) {
									   Log.customer.debug("CatMFGSetVATClassOnInvoiceLineFromSL: Shipping Charge = %s");
										ClusterRoot zeroVAT = Base.getSession().objectFromName("0","cat.core.VATClass", line.getPartition());
										Log.customer.debug("CatMFGSetVATClassOnInvoiceLineFromSL: zeroVAT = %s", zeroVAT);
										line.setFieldValue("VATClass",zeroVAT);
				                 }
							else if (category.intValue() == 2) {
								// is the category is 2 i'e VAT then set VATClass as null
									line.setDottedFieldValue("VATClass",null);
								}
							else {
								  if ((line.getDottedFieldValue("VATClass") != null) && (line.getDottedFieldValue("VATClass") != vatClass))
								  {
									  // is VATClass is already present and is not equal to the supplier's default vatclass ( already user has changed the VATClass on the line
									  // then do not default
								  continue;
							      }
								  else if (line.getDottedFieldValue("VATClass") == null)
								  	 Log.customer.debug("CatMFGSetVATClassOnInvoiceLineFromSL: VATClass is null = %s");
									line.setDottedFieldValue("VATClass",vatClass);
								}
							}
        }

}
}// setVATClasss
}
