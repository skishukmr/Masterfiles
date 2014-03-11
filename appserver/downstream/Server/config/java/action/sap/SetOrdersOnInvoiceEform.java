package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.util.core.Constants;

public class SetOrdersOnInvoiceEform extends Action{
	
	private static final String ClassName = "SetOrdersOnInvoiceEform : ";
	private static final String POClassName = "ariba.purchasing.core.PurchaseOrder";
    
	public void fire(ValueSource object, PropertyTable params)
	{
		Log.customer.debug(" *** ENTERING %s",ClassName);
		Approvable invoice = (Approvable) object;
		if (invoice == null){
			return;
		}
		List orders = (List) invoice.getFieldValue("Orders");
		String orderNumber = (String)invoice.getFieldValue("SetOrder");
		
		if(orderNumber==null){
			return;
		}
		PurchaseOrder po = (PurchaseOrder)Base.getService().objectMatchingUniqueName(POClassName, invoice.getPartition(), orderNumber.toUpperCase().trim());
		if(po==null){
			return;
		}
		orders.clear();
		orders.add(po);
		Currency currency = ((Money)po.getFieldValue("TotalCost")).getCurrency();
        invoice.setDottedFieldValue("Orders",orders);
        invoice.setFieldValue("SetOrder",null);
        // Santanu : added as per new requirement during UAT
        invoice.setFieldValue("Supplier",po.getSupplier());
        // Santanu : added as per new requirement during UAT
        invoice.setFieldValue("SupplierLocation",po.getSupplierLocation());
        if (currency != null) {
        	Money enteredAmount = (Money)invoice.getFieldValue("EnteredInvoiceAmount");
        	if(enteredAmount != null && enteredAmount.getAmount()== Constants.ZeroBigDecimal){
        		invoice.setFieldValue("EnteredInvoiceAmount", new Money(Constants.ZeroBigDecimal,currency));
        	}
    	}
	}

}
