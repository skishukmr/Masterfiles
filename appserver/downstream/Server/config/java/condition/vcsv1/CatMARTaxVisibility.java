
/*
 *
 * 1. Amit Kumar H.J.		13-12-2007			Visibility Condition for Tax fields on CR
 * -----------------------------------------------------------------------------------------
 * Used to control visibility on the Tax fields on Contract Request .
 *
 */


package config.java.condition.vcsv1;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.contract.core.ContractCoreApprovable;
import ariba.user.core.Permission;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;



public class CatMARTaxVisibility extends Condition {

	private static final String ClassName = "CatMARTaxVisibility";

	public boolean evaluate(Object object, PropertyTable params) {

	   Log.customer.debug(" *** %s - In the Evaluate method *** ",ClassName);
 	   Log.customer.debug(" *** %s - Object *** %s", ClassName,object);
	   return checkTaxFieldVisibility(object);
 	}

 	public boolean checkTaxFieldVisibility(Object object) {

	   Log.customer.debug("*** %s - In the checkTaxFieldVisibility() method ***",ClassName);
	   //int release=0;
	   //int term=0;
	   //boolean isInvoice =false;
	   //boolean isReceive = false;
	   //boolean isVisible =false;
	   ContractCoreApprovable maca =null;
	   LineItem li = null;
	   LineItemCollection lic = null;

	   User realuser = (User)Base.getSession().getRealUser();
       Log.customer.debug(" *** %s - Real user *** %s", ClassName,realuser.getUniqueName());


	   if(realuser.hasPermission(Permission.getPermission("CatTax")))
	   {
		   Log.customer.debug("*** Permission obtained is CatTax ***");

       	    if (object instanceof LineItem) {

				li = (LineItem)object;
				Log.customer.debug(" *** %s - Got Line Item %s *** ",ClassName,li);
				lic = li.getLineItemCollection();
				Log.customer.debug(" *** %s - Got Line Item Collection %s from Line Item %s *** ",ClassName,lic,li);
	   		}

	   		if (object instanceof LineItemCollection){

				lic = (LineItemCollection)object;
				Log.customer.debug(" *** %s - Got Line Item collection %s *** ",ClassName,lic);
	   		}

       		if (lic instanceof ContractCoreApprovable) {

				maca = (ContractCoreApprovable)lic;
				Log.customer.debug(" *** %s - Got MasterAgreementCoreApprovable %s *** ",ClassName,maca);

       			if(maca.getReleaseType()==0 && maca.getTermType()==2)
       			{
		   			Log.customer.debug("*** %s - 1) Show the Tax fields only for Item level CR with No Release ***",ClassName);
		   			if(maca.getIsInvoiceable())
		   			{
			   			Log.customer.debug("*** %s - 2) Tax fields visible only if CR is Invoicible ***",ClassName);
			   			Log.customer.debug("*** %s - The Tax fields are visible and hence returning Visibility=true to evaluate() ***",ClassName);
			   			return true;
		   			}
	   			}
				/*
				release = ;
				Log.customer.debug("*** Release Type :"+release);
				term = ;
				Log.customer.debug("*** Term Type :"+release);
				isInvoice = ;
				Log.customer.debug("*** Is Invoiceable ?:"+release);
				isReceive = ;
				Log.customer.debug("*** Is Releasable ? :"+release);
				*/
	   		}
		}

		Log.customer.debug("*** %s - Returning Visibility=false to evaluate() ***",ClassName);
	   	return false;
   }


   public CatMARTaxVisibility() {
 		super();
   }

}