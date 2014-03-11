package config.java.action.sap;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.common.core.SupplierLocation;
import ariba.procure.core.LineItemProductDescription;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPSetCurrencyOnSupplierChange extends Action {

	//@Override
	public void fire(ValueSource object, PropertyTable params)
			throws ActionExecutionException {
		// TODO Auto-generated method stub
		Log.customer.debug(className+"fire()");
		//Below ReqLineItem need to be substituted with appropriate BaseObject to extend to other line item types
		if(object instanceof ReqLineItem && object!=null){
			pli=(ProcureLineItem) (object);
			lipd=pli.getDescription();
			if(lipd==null && lipd.getPrice()==null){
				Log.customer.debug(className+"Line item description or Line item price null.");
				return;
			}
			SupplierLocation supplierLocation = pli.getSupplierLocation();
			if(supplierLocation==null){
				Log.customer.debug(className+"Supplier location null; Can not set line item currency to supplier currency.");
				return;
			}
			Currency supplierCurrency = (Currency) supplierLocation.getFieldValue("DefaultCurrency");
			if(supplierCurrency==null){
				Log.customer.debug(className+"No Supplier currency defined.");
				return;
			}
			Money lineItemPrice = new Money(lipd.getPrice().getAmount(),supplierCurrency);
			lipd.setPrice(lineItemPrice);
			Log.customer.debug(className+"Line item price set to: " + lipd.getPrice().getCurrency().getPrefix()+lipd.getPrice().getAmountAsDouble()+lipd.getPrice().getCurrency().getUniqueName());
		}

	}

	private String className = this.getClass().getName()+": ";
	private ProcureLineItem pli;
	private LineItemProductDescription lipd;
}
