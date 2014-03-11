package config.java.condition.sap;

import java.util.List;
import java.util.Locale;

import ariba.base.core.Base;
import ariba.base.core.BaseObject;
import ariba.base.core.Log;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.LocaleID;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;

public class CatReqHasCatalogItems  extends Condition{
	
	public static final String ComponentStringTable = "cat.java.sap";
	private static ValueInfo parameterInfo[] = {new ValueInfo("ReqToCheck",Condition.IsScalar,"ariba.purchasing.core.Requisition")};
	private static final String requiredParameterNames[]= {"ReqToCheck"};

	protected ValueInfo[] getParameterInfo()
    {
    	return parameterInfo;
    }

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

	public boolean evaluate (Object value, PropertyTable params)
	{
		return evaluateImpl(value, params);
	}
	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)
	{
		if(!evaluate(value, params)){
			
			User sessionUser = (User) Base.getSession().getEffectiveUser();
			LocaleID userLocaleID = sessionUser.getLocaleID();
			String userLanguage = userLocaleID.getLanguage();
			Locale userLocale = null;
			if (!StringUtil.nullOrEmptyOrBlankString(userLanguage)) {
				userLocale = new Locale(userLanguage);
			}
			else {
				userLocale = Locale.US;
			}
			String CatalogItemsError = Fmt.Sil(userLocale,"cat.java.sap","Error_RFQHasCatalogItems");
			if (StringUtil.nullOrEmptyOrBlankString(CatalogItemsError))
				CatalogItemsError = Fmt.Sil(Locale.US,"cat.java.sap","Error_RFQHasCatalogItems");
				return new ConditionResult(CatalogItemsError);
		}
		else
			return null;			
		
	}

	private boolean evaluateImpl (Object value,PropertyTable params)
	{
		Object paramsObj  = (Object)params.getPropertyForKey("ReqToCheck");
		try
		{
			if (paramsObj != null)
			{
				BaseObject bo = (BaseObject)paramsObj;
				if (bo.instanceOf("ariba.purchasing.core.Requisition"))
				{
					Requisition req = (Requisition)bo;
					List lineItems = (List)req.getLineItems();
					for(int i =0; i<lineItems.size();i++){
						
						boolean hasCatalogItems = false;
						
						ReqLineItem	rli = (ReqLineItem)lineItems.get(i);
						// Test 1 - Test if eRFQ has any Catalog Items				
						if (!rli.getIsAdHoc()) {
							hasCatalogItems = true;
						}
						if (hasCatalogItems){
							return false;
						}

					}
				}
			}
		}
		catch(Exception e)
		{
			Log.customer.debug("Error in file CatReqHasLineItems : " + e);
		}
		return true;
	}
}
