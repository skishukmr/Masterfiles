/*Developed by Madhavan Chari-Change Request Issue # 685 All searches/reports to display the Partition Supplier Name instead of Common Supplier-22/11/2007 */

package config.java.action;

import ariba.base.core.Log;
import ariba.base.core.MultiLingualString;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.CommonSupplier;
import ariba.common.core.Supplier;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;




public class CATCSVCommonSupplierNameUpdate extends Action {

    private static final String thisclass = "CATCSVCommonSupplierNameUpdate";
    private CommonSupplier common_Supplier = null;

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException

    {


      if (object instanceof Supplier) {

        Supplier supplierObj = (Supplier)object;
        Log.customer.debug("%s Supplier Object : %s",thisclass,supplierObj);

        if (supplierObj != null) {

        	String supplierName = supplierObj.getName();
        	Log.customer.debug("%s Supplier Name : %s",thisclass,supplierName);

        	common_Supplier = supplierObj.getCommonSupplier();
            Log.customer.debug("%s Common Supplier Object : %s",thisclass,common_Supplier);

            //String commonSupplierName = common_Supplier.getName().getPrimaryString();
             MultiLingualString mlsobj = common_Supplier.getName();
             Log.customer.debug("%s Common Supplier Name Before Change :%s ",thisclass,mlsobj);

            if (!StringUtil.nullOrEmptyOrBlankString(supplierName)) {

                 mlsobj.setPrimaryString(supplierName);
                //String supplierNameDot = supplierName+".";

                //common_Supplier.setDottedFieldValue("Name.PrimaryString",supplierNameDot);


                Log.customer.debug("%s Common Supplier Name After Change same as Supplier Name :%s",thisclass,common_Supplier.getName().getPrimaryString());
		    }

	     }
      }
 	}

 	public CATCSVCommonSupplierNameUpdate() {}
}