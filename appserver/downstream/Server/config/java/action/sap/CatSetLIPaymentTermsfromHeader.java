/****************************************************************************************
Change History
Change# Change By       Change Date     Description
==============================================================================================
1       Nagendra	  05-01-09	Created : Trigger on MAR Change of Header Paymentterms to default Line Item Paymentterms with Header Paymentterms
**********************************************************************************************/

package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.payment.core.PaymentTerms;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
 *
 * AUL : Changed MasterAgreement to Contract
 * AUL : Changed the package for PaymentTerms
 *
 */

public class CatSetLIPaymentTermsfromHeader extends Action{


	public void fire(ValueSource object, PropertyTable params){
		Log.customer.debug(" CatSetLIPaymentTermsfromHeader : Start of the Trigger " +object);

		if(object!=null && object instanceof ContractRequest){
			Log.customer.debug("CatSetLIPaymentTermsfromHeader : Object is instance of object " +object);
			ContractRequest marobj = (ContractRequest)object;
			Log.customer.debug("CatSetLIPaymentTermsfromHeader : marobj " +marobj);
			if(marobj != null){
			PaymentTerms headerpaymentterms = (PaymentTerms)marobj.getPaymentTerms();
			Log.customer.debug("CatSetLIPaymentTermsfromHeader : headerpaymentterms " +headerpaymentterms);
			LineItemCollection lic = (LineItemCollection) object;
			Log.customer.debug("CatSetLIPaymentTermsfromHeader : lic " +lic);
			int count = lic.getLineItemsCount();
			Log.customer.debug("CatSetLIPaymentTermsfromHeader : count " +count);

			   for (int i=0;i<count;i++)
			   {
				  List lineitems=(List)lic.getLineItems();
				  ContractRequestLineItem marli =(ContractRequestLineItem)lineitems.get(i);
				  Log.customer.debug("CatSetLIPaymentTermsfromHeader : marli " +marli);
				  if(headerpaymentterms!=null && marli!=null)
				  {
					  Log.customer.debug("CatSetLIPaymentTermsfromHeader : marli1 " );
					  marli.setDottedFieldValue("PaymentTerms",headerpaymentterms);
				}

	  	  }//end of for loop
		} // End of MARobj
	}//end of object
	} // End Of Fire Method

}// End Of class


