package config.java.invoiceeform.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Country;
import ariba.common.core.Address;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPInvoiceEformTaxCodeValidity extends Condition{

	private static final String classname = "CatSAPInvoiceEformTaxCodeValidity : ";
    public static String LineItemParam = "LineItem";
    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.base.core.BaseObject")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException	
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		if (obj !=null){
		BaseObject Invli =(BaseObject)params.getPropertyForKey(LineItemParam);
		if(Invli==null){
			return true;
		}
		
		Integer i = (Integer)Invli.getFieldValue("OrderLineNumber");
		PurchaseOrder order = (PurchaseOrder)Invli.getFieldValue("Order");
		if (order==null){
			return true;
		}else if ( i==null || (i.intValue() <= 0) || i.intValue() > order.getLineItems().size() ){
			i=new Integer(1);
		}
		
		ProcureLineItem li =(ProcureLineItem)order.getLineItems().get(i.intValue()-1);
		Log.customer.debug(" %s : li %s " ,classname , li);
		if(li==null){
			return false;
		}
		LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
		Log.customer.debug(" %s : lic %s " ,classname , lic);
		ClusterRoot companycode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" %s : companycode %s " ,classname , companycode);
		if (companycode == null){
			return false;
		}
		ClusterRoot taxcode = (ClusterRoot)Invli.getDottedFieldValue("TaxCode");
		Log.customer.debug(" %s : taxcode %s " ,classname , taxcode);
		if (taxcode != null){
		String ccSAPsource = (String)companycode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : ccSAPsource %s " ,classname , ccSAPsource);
		String tcSAPsource = (String)taxcode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : tcSAPsource %s " ,classname , tcSAPsource);
		
		Address shipto = (Address)li.getDottedFieldValue("ShipTo");
		Log.customer.debug(" %s : shipto %s " ,classname , shipto);
		if(shipto == null){
			return false;
		}
		Country shiptocountry = (Country)shipto.getCountry();
		Log.customer.debug(" %s : shiptocountry %s " ,classname , shiptocountry);
		if (shiptocountry == null){
			return false;			
		}
		Country taxcodecountry = (Country)taxcode.getDottedFieldValue("Country");
		Log.customer.debug(" %s : taxcodecountry %s" ,classname , taxcodecountry);
		if (taxcodecountry == null){
			return false;			
		}
		if(taxcodecountry == shiptocountry){
			if(ccSAPsource!=null && tcSAPsource !=null && ccSAPsource.trim().equals(tcSAPsource.trim())){
				Log.customer.debug(" %s :  return true " ,classname);
				return true;
			}
		}
		Log.customer.debug(" %s : return false " ,classname);
		return false;
		}
		}
		Log.customer.debug(" %s : return true " ,classname);
		return true;
	}
	
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
	
}
