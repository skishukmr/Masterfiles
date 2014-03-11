// Vikram: Making Payment Terms optional field for Non-material items like shipping, special charge, handling charge

package config.java.condition.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.payment.core.PaymentTerms;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.procure.core.ProcureLineType;
import ariba.util.log.Log;

public class CatSAPPaymentTermValidity extends Condition{

	private static final String classname = "CatSAPPaymentTermValidity : ";
    public static String LineItemParam = "LineItem";
//    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.procure.core.ProcureLineItem")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException
	{
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" %s : obj %s " ,classname, obj);
		if (obj !=null){
		ProcureLineItem li =(ProcureLineItem)params.getPropertyForKey(LineItemParam);
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
		PaymentTerms paymentTerms = (PaymentTerms)li.getDottedFieldValue("PaymentTerms");
		Log.customer.debug(" %s : taxcode %s " ,classname , paymentTerms);
		if (paymentTerms != null){
		String ccSAPsource = (String)companycode.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : ccSAPsource %s " ,classname , ccSAPsource);
		String ptSAPsource = (String)paymentTerms.getDottedFieldValue("SAPSource");
		Log.customer.debug(" %s : ptSAPsource %s " ,classname , ptSAPsource);

		if(ccSAPsource!=null && ptSAPsource !=null && ccSAPsource.trim().equals(ptSAPsource.trim())){
				Log.customer.debug(" %s :  return true " ,classname);
				return true;
			}
		}
		}

		Log.customer.debug(" %s : return false " ,classname);
		return false;
	}



	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		// Vikram: If LineType.Category == 1, its a material line, hence display error if Payment terms is blank
		// but for non-material line, make it as an optional entry
		ProcureLineItem li =(ProcureLineItem)params.getPropertyForKey(LineItemParam);
		Log.customer.debug(" %s : li %s " ,classname , li);
		if (li.getLineType() != null)
		{
			if (!evaluate(value, params) && li.getLineType().getCategory() == 1) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidPaymentTerm );
				return new ConditionResult(errorMessage);
		}
		else {
				return null;
			 }
		}
		else

		
		if (!evaluate(value, params)) {

			String errorMessage = ResourceService.getString(ComponentStringTable,InvalidPaymentTerm );
				return new ConditionResult(errorMessage);
		}
		else {
			return null;
		}
	}


	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}

	public static final String ComponentStringTable = "cat.java.sap";
	private static final String InvalidPaymentTerm = "InvalidPaymentTerm";
}
