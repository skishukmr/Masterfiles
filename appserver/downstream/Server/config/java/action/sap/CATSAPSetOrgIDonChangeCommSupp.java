/*********************************************************************************************************************

Created by : James S Pagadala
Date	   : Oct 13 2008

********************************************************************************************************************/

package config.java.action.sap;

import ariba.base.core.BaseVector;

import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.base.fields.Action;
import ariba.util.log.Log;
import ariba.common.core.SupplierLocation;
import ariba.common.core.Supplier;
import ariba.common.core.CommonSupplier;


public class CATSAPSetOrgIDonChangeCommSupp extends Action {

	 public void fire(ValueSource object, PropertyTable params){

	       Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : ****START****");

	       try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	       if (!(object instanceof Supplier)){
			   return;
		   }
	       Supplier supplier = (Supplier)object;
	       if(supplier == null){
			   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier is null");
			   return;
		   }
	       Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier " + supplier);
	       CommonSupplier commSupp = supplier.getCommonSupplier();
	       if(commSupp == null){
			   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Common Supplier is null");
			   return;
		   }
	       Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Common Supplier " + commSupp);

	       BaseVector supplocs = supplier.getLocations();
	       if(supplocs == null )
	       {
	    	   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : List of Supplier Location is null");
			   return;
	       }
	       else
	       {
	    	   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier Location Size " + supplocs.size());
	    	   for(int i=0;i<supplocs.size();i++)
	    		{
	    		   SupplierLocation SL = (SupplierLocation)supplocs.get(i);
	    		   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier " + SL);
	    		   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier " + SL.getDottedFieldValue("UniqueName"));
	    		   Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : Supplier " + SL.getDottedFieldValue("ContactID"));
	    		}
	       }
	       Log.customer.debug("CATSAPSetOrgIDonChangeCommSupp : fire : ****END****");
	}
}