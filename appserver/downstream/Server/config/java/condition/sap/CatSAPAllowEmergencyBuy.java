package config.java.condition.sap;

import java.math.BigDecimal;

import ariba.base.core.Base;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSAPAllowEmergencyBuy extends Condition
{

	private static final String ClassName = "CatEZOAllowEmergencyBuy";
	private static final String param_ebuy = "Application.Caterpillar.Procure.EmergencyBuyLimit";
	private static final String ErrorMsg = ResourceService.getString("cat.java.vcsv3","ErrorEmergencyBuyLimitReached");
	private static String InputParam = "Requisition";

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
		if (plic != null) {
			boolean isEmergencyBuy = false;
			Boolean isEmergencyBuyB = (Boolean) plic.getDottedFieldValue("EmergencyBuy");
			if (isEmergencyBuyB != null) {
				isEmergencyBuy = isEmergencyBuyB.booleanValue();
			}
			Log.customer.debug("%s ::: isEmergencyBuy Boolean: %s", ClassName, isEmergencyBuyB);
			Log.customer.debug("%s ::: isEmergencyBuy boolean: " + isEmergencyBuy, ClassName);

			String emergencyBuyLimit = Base.getService().getParameter(plic.getPartition(), param_ebuy);
			Log.customer.debug("%s ::: Emergency Buy Limit String - emergencyBuyLimit: %s", ClassName, emergencyBuyLimit);

			if (isEmergencyBuy && !StringUtil.nullOrEmptyOrBlankString(emergencyBuyLimit)) {
				Currency baseCurrency = Currency.getBaseCurrency();
				Money emergencyBuyLimitM = new Money(new BigDecimal(emergencyBuyLimit), baseCurrency);
				Money requisitionAmount = plic.getTotalCost();
				Log.customer.debug("%s ::: Emergency Limit Money is: %s", ClassName, emergencyBuyLimitM);
				Log.customer.debug("%s ::: Requisition Amount Money is: %s", ClassName, requisitionAmount);

				if (emergencyBuyLimitM != null && requisitionAmount != null) {
						Log.customer.debug("%s ::: Emergency Limit Money String is: %s", ClassName, emergencyBuyLimitM.toString());
						Log.customer.debug("%s ::: Requisition Amount Money String in Original Currency is: %s", ClassName, requisitionAmount.toString());

						Money reqAmntInBaseCurr = requisitionAmount.convertToCurrency(baseCurrency);

						Log.customer.debug("%s ::: Requisition Amount Money String in Base Currency is: %s", ClassName, reqAmntInBaseCurr.toString());

						if (emergencyBuyLimitM.compareTo(requisitionAmount) < 0) {
							Log.customer.debug("%s ::: Returning false as over Emergency Buy Limit", ClassName);
						return new ConditionResult(ErrorMsg);
					}
				}
			}
			else {
					Log.customer.debug("%s ::: Not Emergency Buy or no limit specified", ClassName, plic);
			}
		}
		else {
				Log.customer.debug("%s ::: ProcureLineItemCollection Input is null %s", ClassName, plic);
		}
		return null;
	}

	public CatSAPAllowEmergencyBuy()
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