/*************************************************************************************************
*   Created by: Santanu
*
*   Purpose:
*   To Validate TradingPartners
*   
*   Change History:
*   Change By    Change Date     Description
*	--------------------------------------------------------------------------------
*   Santanu		 July 16, 2008    Create
*
*
*************************************************************************************************/

package config.java.condition.sap;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
//import ariba.base.fields.ValueInfo;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CatTradingPartnerValidity  extends Condition{

	private static final ValueInfo parameterInfo[] = {new ValueInfo("TestField", IsScalar, "java.lang.String")};
	private static final String requiredParameterNames[] = { "TestField" };
	private static final String StringTable = "aml.CatSAPRequisitionExt";
	private static final String TradingPartnerAccCat = "TradingPartnerAccCat";
	private static final String TradingPartnerStr = "TradingPartnerStr";
	
	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException	
	{
		Log.customer.debug(" CatTradingPartnerValidity : value " + value);
		Log.customer.debug(" CatTradingPartnerValidity : params " + params);
		BaseObject obj = (BaseObject) value;
		Log.customer.debug(" CatTradingPartnerValidity : obj " + obj);
		
		try
		{
            String testfield = (String)params.getPropertyForKey("TestField");
            Log.customer.debug(" CatTradingPartnerValidity : testfield " + testfield);
			
			if (testfield!=null && obj!=null)
			{
					ProcureLineItem li =(ProcureLineItem)obj;
					LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
					Log.customer.debug(" CatTradingPartnerValidity : li " + li);
					Log.customer.debug(" CatTradingPartnerValidity : lic " + lic);
					
					//Requisition lic = (Requisition)lic;
						BaseObject CompCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");	
						if(CompCode == null){
							return false;
						}
						BaseObject acccat = (BaseObject)li.getDottedFieldValue("AccountCategory");
						if(acccat == null){
							return false;
						}
						String acccatstr = (String)acccat.getFieldValue("UniqueName");
						String InterCompany = (String)ResourceService.getString(StringTable, TradingPartnerAccCat);			
						Log.customer.debug(" CatTradingPartnerValidity : acccatstr " + acccatstr);
						Log.customer.debug(" CatTradingPartnerValidity : InterCompany " + InterCompany);
						
						if(!acccatstr.equalsIgnoreCase(InterCompany)){
							Log.customer.debug(" CatTradingPartnerValidity : return true");
							return true;
						}
						else
						{					
							String CCSAPSource = (String)CompCode.getDottedFieldValue("SAPSource");
							Log.customer.debug(" CatTradingPartnerValidity : CCSAPSource " + CCSAPSource);
							String TradingPartner = (String)ResourceService.getString(StringTable, TradingPartnerStr);							
							String TradingPartnerSAPSource = (String)(CCSAPSource + TradingPartner);
							Log.customer.debug(" CatTradingPartnerValidity : TradingPartnerSAPSource " + TradingPartnerSAPSource);
							BaseObject TradPart = (BaseObject)li.getDottedFieldValue("TradingPartner");	
							Log.customer.debug(" CatTradingPartnerValidity : TradPart " + TradPart);
							if(TradPart == null)
							{
								return false;
							}
							String LiTPSource = (String) TradPart.getDottedFieldValue("SAPSource");
							Log.customer.debug(" CatTradingPartnerValidity : LiTPSource " + LiTPSource);
							if(LiTPSource.equalsIgnoreCase(TradingPartnerSAPSource))
							{
								Log.customer.debug(" CatTradingPartnerValidity : return true");
								return true;
							}
						}
				}
		}
		catch(Exception e)
		{
			Log.customer.debug("Error in file CatTradingPartnerValidity : " + e);
		}
		return false;
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)
	{
		return null;
	}

	public CatTradingPartnerValidity() {
		super();
	}
	protected ValueInfo[] getParameterInfo() {
		return parameterInfo;
	}
	protected String[] getRequiredParameterNames() 	{
		return requiredParameterNames;
	}
}
