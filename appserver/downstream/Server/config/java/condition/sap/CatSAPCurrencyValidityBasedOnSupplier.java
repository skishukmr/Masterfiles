/*
 * Created by Madhuri  on Nov 14, 2008
 */

package config.java.condition.sap;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.contract.core.ContractRequest;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatSAPCurrencyValidityBasedOnSupplier extends Condition{

	private static final String classname = "CatSAPCurrencyValidityBasedOnSupplier : ";
     boolean IsCurrencyValidityRequired = false;
     private static final String masterAgreementRequest = "MAR";
	private static final ValueInfo valueInfo = new ValueInfo(IsScalar);
	public static final String ComponentStringTable = "cat.java.sap";
		int ErrorCode = 0;
	private static final String headerCurrencymessage = "HeaderCurrencyValidation";

	 	private String[] requiredParameterNames = {masterAgreementRequest};

	 	private static ValueInfo ParameterInfo[] = {

	 	new ValueInfo(masterAgreementRequest, IsScalar, "ariba.contract.core.ContractRequest")};


	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		BaseObject supplierCurrency = null;
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		BaseObject MARObject = (BaseObject) params.getPropertyForKey(masterAgreementRequest);
		Log.customer.debug(" %s : MARObject %s " ,classname,MARObject);
		if (MARObject !=null && MARObject instanceof ContractRequest ){
			BaseObject supplierLoc = (BaseObject) MARObject.getDottedFieldValue("SupplierLocation");
			Log.customer.debug(" %s : supplierLoc %s " ,classname,supplierLoc);
			if(supplierLoc == null) {
				Log.customer.debug(" %s : supplierLoc is null %s " ,classname,supplierLoc);
				return true;
			}
			if (supplierLoc != null){
				supplierCurrency = (BaseObject)supplierLoc.getDottedFieldValue("DefaultCurrency");
				Log.customer.debug(" %s : supplierCurrency %s " ,classname,supplierCurrency);
			}
			if(supplierCurrency == null){
				Log.customer.debug(" %s : supplierCurrency  is null" ,classname);
				return true;
			}
			else if(supplierCurrency != null){
				Log.customer.debug(" %s : supplierCurrency  is not null" ,classname);
				BaseObject companyCode = (BaseObject) MARObject.getDottedFieldValue("CompanyCode");
				Log.customer.debug(" %s : companyCode %s " ,classname,companyCode);
				if(companyCode != null)	{
					Log.customer.debug(" %s : companyCode is not null " ,classname+companyCode);
					if(companyCode.getDottedFieldValue("IsCurrencyValidityRequired")!=null)
					IsCurrencyValidityRequired = ((Boolean)companyCode.getDottedFieldValue("IsCurrencyValidityRequired")).booleanValue();
					Log.customer.debug(" %s : IsCurrencyValidityRequired " ,classname+IsCurrencyValidityRequired);
				}
				if(IsCurrencyValidityRequired) {
					Log.customer.debug(" %s : IsCurrencyValidityRequired is true " ,classname);
					String supplierCurrencyUniqueName = (String) supplierCurrency.getDottedFieldValue("UniqueName");
					Log.customer.debug(" %s : supplierCurrencyUniqueName %s " ,classname,supplierCurrencyUniqueName);
					BaseObject currencyObj =(BaseObject)MARObject.getDottedFieldValue("Currency");
					Log.customer.debug(" %s : Currency Object %s " ,classname,currencyObj);
					if(currencyObj != null)	{
						Log.customer.debug(" %s : Currency Object  is not null%s " ,classname,currencyObj);
						String headerCurrency = (String) currencyObj.getDottedFieldValue("UniqueName");
						Log.customer.debug(" %s : header Currency %s " ,classname,headerCurrency);
						if (headerCurrency.equalsIgnoreCase(supplierCurrencyUniqueName)){
							Log.customer.debug(" %s : header Currency and supplier Currency are Equal" ,classname);
							return true;
						}
						else{
							Log.customer.debug(" %s : header Currency and supplier Currency are not equal returning false" ,classname);
							return false;
						}
					}
					else{
						Log.customer.debug(" %s : header Currency  is null returning false %s " ,classname);
						return false;
					}
				}
				else {
					Log.customer.debug(" %s : IsCurrencyValidityRequired is false" ,classname);
					return true;
				}
			}
			Log.customer.debug(" %s : Supplier Currency is null returning true" ,classname);
			return true;
		}
		Log.customer.debug(" %s : Object is not instance of MAR returning true" ,classname);
			return true;
  }
 protected ValueInfo getValueInfo()

 	{

 		return valueInfo;

 	}

 	protected ValueInfo[] getParameterInfo()

 	{

 		return ParameterInfo;

 	}

 	public String[] getRequiredParameterNames()

 	{

 		return requiredParameterNames;

	}
	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
		{
			if (!evaluate(value, params)) {

				String errorMessage = "";
				errorMessage = ResourceService.getString(ComponentStringTable,headerCurrencymessage);
				return new ConditionResult(errorMessage);

			}
			else {
				return null;
			}
		}




}
