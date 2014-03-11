package config.java.condition.sap;

import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Country;
import ariba.common.core.Address;
import ariba.contract.core.Contract;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPWithHoldTaxCodeValidity extends Condition{

	private static final String classname = "CatSAPWithHoldTaxCodeValidity : ";
    public static String InvoiceEformParam = "InvoiceEform";
    //private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(InvoiceEformParam, 0, "ariba.base.core.ClusterRoot")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug("CatSAPWithHoldTaxCodeValidity : obj => " + obj);
		if (obj !=null){
		ClusterRoot invoiceEform = (ClusterRoot)params.getPropertyForKey(InvoiceEformParam);
		Log.customer.debug("CatSAPWithHoldTaxCodeValidity : invoiceEform => " + invoiceEform);
		if(invoiceEform == null){
			return false;
		}
		 if(invoiceEform.instanceOf("ariba.invoicing.core.Invoice") || invoiceEform.instanceOf("ariba.invoicing.core.InvoiceReconciliation"))
		 {
			 Log.customer.debug("CatSAPWithHoldTaxCodeValidity : It is an Invoice or IR Object => " + invoiceEform);
			 return validateWHTForInvOrIR(invoiceEform);
		 }




		PurchaseOrder order = null;
		List InvoiceLines = (List)invoiceEform.getFieldValue("LineItems");
		if(InvoiceLines==null || (InvoiceLines!=null && InvoiceLines.size()==0)){
			return true;
		}
		else{
			BaseObject lineitem = (BaseObject)InvoiceLines.get(0);
			order = (PurchaseOrder)lineitem.getFieldValue("Order");
		}
		ProcureLineItem orderline = (ProcureLineItem) order.getLineItems().get(0);
		if (orderline==null){
			return true;
		}
		LineItemCollection lic = (LineItemCollection)orderline.getLineItemCollection();
		Log.customer.debug(" %s : lic %s " ,classname , lic);
		ClusterRoot companycode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" %s : companycode %s " ,classname , companycode);
		if (companycode == null){
			return false;
		}
		ClusterRoot taxcode = (ClusterRoot)invoiceEform.getDottedFieldValue("WithHoldTaxCode");
		Log.customer.debug(" %s : taxcode %s " ,classname , taxcode);
		if (taxcode != null){
		String ccSAPsource = (String)companycode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : ccSAPsource %s " ,classname , ccSAPsource);
		String tcSAPsource = (String)taxcode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : tcSAPsource %s " ,classname , tcSAPsource);

		Address shipto = (Address)orderline.getDottedFieldValue("ShipTo");
		Log.customer.debug(" %s : shipto %s " ,classname , shipto);
		if(shipto == null){
			return false;
		}
		Country shiptocountry = (Country)shipto.getCountry();
		Log.customer.debug(" %s : shiptocountry %s " ,classname , shiptocountry);
		if (shiptocountry == null){
			return false;
		}
		Country taxcodecountry = (Country)taxcode.getFieldValue("Country");
		Log.customer.debug(" %s : taxcodecountry %s " ,classname , taxcodecountry);
		if (taxcodecountry == null){
			return false;
		}
		if(taxcodecountry == shiptocountry){
			if(ccSAPsource!=null && tcSAPsource !=null && ccSAPsource.trim().equals(tcSAPsource.trim())){
				if(taxcode.getFieldValue("IsWithHoldingTax")!=null){
					String isWithHoldingTax = (String)taxcode.getFieldValue("IsWithHoldingTax");
					Log.customer.debug(" %s :  isWithHoldingTax %s " ,classname,isWithHoldingTax);
					if(isWithHoldingTax.equalsIgnoreCase("Y")){
					Log.customer.debug(" %s :  return true " ,classname);
					return true;
					}
				}
			}
		}
		}
		}else{
			Log.customer.debug(" %s :  WithHold taxcode is null - return true " ,classname);
			return true;
		}
		Log.customer.debug(" %s : return false " ,classname);
		return false;
	}

	private static boolean validateWHTForInvOrIR(ClusterRoot cr){

		Log.customer.debug("CatSAPWithHoldTaxCodeValidity : cr => " + cr);
		ClusterRoot companycode = (ClusterRoot) cr.getDottedFieldValue("CompanyCode");
		Log.customer.debug("CatSAPWithHoldTaxCodeValidity : companycode => " + companycode);
		if (companycode == null){
			return false;
		}
		ClusterRoot taxcode = (ClusterRoot)cr.getDottedFieldValue("WithHoldTaxCode");
		Log.customer.debug("CatSAPWithHoldTaxCodeValidity : taxcode => " + taxcode);


		if (taxcode != null)
		{
			String ccSAPsource = (String)companycode.getDottedFieldValue("SAPSource");
			Log.customer.debug("CatSAPWithHoldTaxCodeValidity : ccSAPsource => " + ccSAPsource);

			String tcSAPsource = (String)taxcode.getDottedFieldValue("SAPSource");
			Log.customer.debug("CatSAPWithHoldTaxCodeValidity : ccSAPsource => " + tcSAPsource);
			PurchaseOrder order = null;
			Contract ma = null;
			ProcureLineItem orderline = null;


			// Get the ShipTo from Order, If Invocie is created against Contract then pick Ship To value form MA.
			if(cr.getDottedFieldValue("Order")!= null){
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : Order => " + cr.getDottedFieldValue("Order"));
				order = (PurchaseOrder) cr.getDottedFieldValue("Order");
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : order => " + order.getUniqueName() );
				orderline = (ProcureLineItem) order.getLineItems().get(0);
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : orderline => " + orderline );
				if (orderline==null){
					return true;
				}

			}
			else
			{
				ma = (Contract) cr.getDottedFieldValue("MasterAgreement");
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : ma => " + ma);
				orderline = (ProcureLineItem) ma.getLineItems().get(0);
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : orderline => " + orderline);
				if (orderline==null){
					return true;
				}
			}

			Address shipto = (Address)orderline.getDottedFieldValue("ShipTo");
			Log.customer.debug("CatSAPWithHoldTaxCodeValidity : shipto => " + shipto);

			if(shipto == null){
				return false;
			}

			Country shiptocountry = (Country)shipto.getCountry();
			Log.customer.debug("CatSAPWithHoldTaxCodeValidity : shiptocountry => " + shiptocountry);

			if (shiptocountry == null){
				return false;
			}

			Country taxcodecountry = (Country)taxcode.getFieldValue("Country");
			Log.customer.debug("CatSAPWithHoldTaxCodeValidity : taxcodecountry => " + taxcodecountry);

			if (taxcodecountry == null){
				return false;
			}
				if(taxcodecountry == shiptocountry){
					if(ccSAPsource!=null && tcSAPsource !=null && ccSAPsource.trim().equals(tcSAPsource.trim())){
						if(taxcode.getFieldValue("IsWithHoldingTax")!=null){
							String isWithHoldingTax = (String)taxcode.getFieldValue("IsWithHoldingTax");
							Log.customer.debug("CatSAPWithHoldTaxCodeValidity : isWithHoldingTax => " + isWithHoldingTax);
							if(isWithHoldingTax.equalsIgnoreCase("Y")){
								Log.customer.debug("CatSAPWithHoldTaxCodeValidity : returning true");
								return true;
							}
						}
					}
				}
			}
			else
			{
				Log.customer.debug("CatSAPWithHoldTaxCodeValidity : WithHoldTax is null returning true");
				return true;
			}
		return false;
}

	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

}
