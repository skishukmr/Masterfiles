package config.java.invoiceeform.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


/**  @author Nagendra.
 Break fix - Santanu

*/
public class CatSAPSetCustomSuppLoc extends Action {

	//private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);
	
	private static final String ClassName = "CatSAPSetCustomSuppLoc";

	public void fire(ValueSource object, PropertyTable params) {
				
				BaseObject invoiceLI = (BaseObject) object;
				Log.customer.debug(" %s *** invoice %s",ClassName ,invoiceLI);

				BaseObject PostalAddress = (BaseObject) BaseObject.create("ariba.basic.core.PostalAddress", Base.getSession().getPartition());
	    		Log.customer.debug(" %s *** invoice %s" ,ClassName ,PostalAddress);
	    		
	    		if(invoiceLI.getDottedFieldValue("Order")!=null)
			  	{
					ClusterRoot order =(ClusterRoot)invoiceLI.getDottedFieldValue("Order");
					
					if(order.getDottedFieldValue("SupplierLocation") == null){
	        		  return;
					}
						
	        	  	String city =(String)order.getDottedFieldValue("SupplierLocation.City");
					String state  =(String)order.getDottedFieldValue("SupplierLocation.State");
					String postalCode =(String)order.getDottedFieldValue("SupplierLocation.PostalCode");

					Log.customer.debug(" *** city %s",city);
					Log.customer.debug(" *** state %s",state);
					Log.customer.debug(" *** PostalCode %s",postalCode);
					BaseObject country =(BaseObject)order.getDottedFieldValue("SupplierLocation.Country");
					Log.customer.debug(" *** Country %s",country);
					PostalAddress.setFieldValue("Country", country);
					PostalAddress.setFieldValue("State", state );
					PostalAddress.setFieldValue("PostalCode", postalCode );
					PostalAddress.setFieldValue("City", city );
					invoiceLI.setFieldValue("CustomSuppLoc", PostalAddress);
					//Log.customer.debug(" *** CustomSuppLoc %s",CustomSuppLoc);

			}
			}


}




