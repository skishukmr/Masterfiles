package config.java.condition.sap;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSAPCheckForRFQSuppliers extends Condition
{
	private static final String ClassName = "CatSAPCheckForRFQSuppliers";
	private static String InputParam = "Requisition";
	private static final String ErrorMsg = ResourceService.getString("cat.java.sap","ErrorRequiredRFQSupplier");
	private static final String ErrorMsg1 = ResourceService.getString("cat.java.sap","ErrorMoreWriteInSupplier");
	private static final String ErrorMsg2 = ResourceService.getString("cat.java.sap","ErrorInvalidWriteInSupplier");

	private static final ValueInfo parameterInfo[] =
	{
		new ValueInfo(InputParam, 0, "ariba.procure.core.ProcureLineItemCollection")
	};
	private static final String requiredParameterNames[] = { InputParam };

	public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException
	{
		return (evaluateAndExplain(object, params) == null);
	}

	public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
	{
		ProcureLineItemCollection plic = (ProcureLineItemCollection) params.getPropertyForKey(InputParam);
			Log.customer.debug("%s ::: Plic: %s", ClassName, plic);

		boolean isPurchasing = false;
		User currUser = (User) Base.getSession().getEffectiveUser();
		Log.customer.debug("%s ::: Current User: %s", ClassName, currUser);

		if (currUser != null)
			isPurchasing = currUser.hasPermission("CatPurchasing");

		Log.customer.debug("%s ::: Current User isPurchasing: " + isPurchasing, ClassName);

		boolean isERFQ = false;
		Boolean isERFQB = (Boolean) plic.getDottedFieldValue("ISeRFQ");
		if (isERFQB != null) {
			isERFQ = isERFQB.booleanValue();
		}

		Log.customer.debug("%s ::: isERFQ Boolean: %s", ClassName, isERFQB);
		Log.customer.debug("%s ::: isERFQ boolean: " + isERFQ, ClassName);

		if (plic != null) {
			BaseVector lognetSuppliers = (BaseVector) plic.getFieldValue("LognetSuppliers");
			BaseVector writeInSuppliers = (BaseVector) plic.getFieldValue("WriteInSuppliers");

			int lsSize = 0;
			int wiSize = 0;
			boolean validWriteIn = true;

			if (lognetSuppliers != null)
				lsSize = lognetSuppliers.size();
			if (writeInSuppliers != null)
				wiSize = writeInSuppliers.size();

			if (wiSize > 0) {
				for(int i=0;i<wiSize;i++){
					BaseObject wiItem = (BaseObject) writeInSuppliers.get(i);
					if (wiItem != null) {
						String wiName = (String) wiItem.getFieldValue("SupplierName");
						//String wiContact = (String) wiItem.getFieldValue("SupplierContact");
						String wiEMail = (String) wiItem.getFieldValue("SupplierEMail");
						String wiFax = (String) wiItem.getFieldValue("SupplierFax");
						//Language wiLanguage = (Language) wiItem.getFieldValue("Language");

						if (StringUtil.nullOrEmptyOrBlankString(wiName))
							validWriteIn = false;
						if (StringUtil.nullOrEmptyOrBlankString(wiEMail) && StringUtil.nullOrEmptyOrBlankString(wiFax))
							validWriteIn = false;
					}
				}
			}

			if (!isPurchasing || !isERFQ) {
				return null;
			}
			else {
				if (((lsSize > 0) || (wiSize > 0)) && (validWriteIn && wiSize <= 2)) {
					return null;
				}
				else if(wiSize > 2){
					return new ConditionResult(ErrorMsg1);
				}
				else if(!validWriteIn){
					return new ConditionResult(ErrorMsg2);
				}
				else
				{
					return new ConditionResult(ErrorMsg);
				}
			}
		}
		else {
			Log.customer.debug("%s ::: ProcureLineItemCollection Input is null %s", ClassName, plic);
		}
		return null;
	}

	public CatSAPCheckForRFQSuppliers()
	{
		super();
	}

	protected ValueInfo[] getParameterInfo()
	{
		return parameterInfo;
	}

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}
}