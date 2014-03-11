package config.java.invoiceeform.sap;

import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.util.core.Constants;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/**  @author Nagendra.
 Break fix - Santanu

*/
public class CatSAPSetTotAmtOnAmtchange extends Action {

	//private static final ValueInfo valueInfo = new ValueInfo(0, Approvable.ClassName);

	private static final String ClassName = "CatSAPSetTotAmtOnAmtchange";

	public void fire(ValueSource object, PropertyTable params) {

				BaseObject invoiceLI = (BaseObject) object;
				Log.customer.debug(" %s *** invoice %s",ClassName ,invoiceLI);

				//BaseObject PostalAddress = (BaseObject) BaseObject.create("ariba.basic.core.PostalAddress", Base.getSession().getPartition());
	    		//Log.customer.debug(" %s *** invoice %s" ,ClassName ,PostalAddress);

	    		if(invoiceLI.getDottedFieldValue("Amount")!=null)
			  	{
					//ClusterRoot order =(ClusterRoot)invoiceLI.getDottedFieldValue("Order");

					if(invoiceLI.getDottedFieldValue("TaxAmount") == null){
	        		  	Currency currency = ((Money)invoiceLI.getFieldValue("Amount")).getCurrency();
			    		invoiceLI.setFieldValue("TaxAmount", new Money(Constants.ZeroBigDecimal,currency));
					}
					Money taxamount = (Money)invoiceLI.getDottedFieldValue("TaxAmount");
	        	  	Money amount = (Money)invoiceLI.getDottedFieldValue("Amount");
					Log.customer.debug(" *** amount %s",amount);
					if(amount!=null){
					Money TotalAmt = amount.add(taxamount);
					Log.customer.debug(" *** TotalAmt %s",TotalAmt);
				    invoiceLI.setFieldValue("TotalAmount", TotalAmt);
					}
			}
			}


}




