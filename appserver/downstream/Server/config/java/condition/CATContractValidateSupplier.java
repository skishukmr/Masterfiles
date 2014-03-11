/*
 * CATContractValidateSupplier.java
 * Developed by Anoma on 13-AUGUST 2005
 */


package config.java.condition;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/*  Restricts users from selecting Suppliers
 *  with UniqueName beginning with 'SU_INTERNAL'
 */
public class CATContractValidateSupplier extends Condition {


	public boolean evaluate(Object value, PropertyTable params) {
        return evaluateAndExplain(value, params) == null;
    }

     public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {
       Log.customer.debug("%s *** firing...", classname);
	   Log.customer.debug("%s *** value=..."+value);

	   if(value != null) {
		  ariba.common.core.Supplier supplier = null;
		  String uniquename = "";

		  supplier = (ariba.common.core.Supplier)value;
		  Log.customer.debug("%s Supplier = "+supplier);

			if(supplier != null) {
				uniquename = supplier.getUniqueName();
				Log.customer.debug("%s supplieruniquename = %s", classname,uniquename);

				if (uniquename.startsWith(SU_INTERNAL)) {
				   Log.customer.debug("%s inside if....", classname);
				   /*Throw the error if UniqueName begins with 'SU_INTERNAL'*/
				   return new ConditionResult(error);
				}
			}
		}


	   return null;
}

 	public CATContractValidateSupplier() {}

    private static final String classname = "CATContractValidateSupplier";
    private final String SU_INTERNAL = "SU_INTERNAL";
    private final String error = "A valid My Supply Cabinet supplier must be selected for this field";
}