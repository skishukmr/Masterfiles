package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.common.core.SplitAccounting;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatGeneralLedgerTextEditability   extends Condition{

	private static final String StringTable = "aml.CatSAPRequisitionExt";
	private static final String TradingPartnerAccCat = "TradingPartnerAccCat";
	private static final String CBSCapitalAccCat = "CBSCapitalAccCat";
	private static final String MACH1CapitalAccCat = "MACH1CapitalAccCat";

	public boolean evaluate (Object object, PropertyTable params)
    throws ConditionEvaluationException
	{
		Log.customer.debug(" CatGeneralLedgerTextEditability : object " + object);
		Log.customer.debug(" CatGeneralLedgerTextEditability : params " + params);

		try
		{
			if (object instanceof SplitAccounting){
			{
					SplitAccounting sa = (SplitAccounting)object;
					String GeneralLedgerText =(String)sa.getFieldValue("GeneralLedgerText");
					if (StringUtil.nullOrEmptyOrBlankString(GeneralLedgerText)){
						return true;
					}
					LineItem li = (LineItem)sa.getFieldValue("LineItem");
					LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
					Log.customer.debug(" CatGeneralLedgerTextEditability : li " + li);
					Log.customer.debug(" CatGeneralLedgerTextEditability : lic " + lic);

						BaseObject CompCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");
						if(CompCode == null){
							return true;
						}
						BaseObject acccat = (BaseObject)li.getDottedFieldValue("AccountCategory");
						if(acccat == null){
							return true;
						}
						String acccatstr = (String)acccat.getFieldValue("UniqueName");
						String isGLEditableForInterCompany = (String)CompCode.getFieldValue("isGLEditableForInterCompany");
						if(StringUtil.nullOrEmptyOrBlankString(isGLEditableForInterCompany))
						{
							isGLEditableForInterCompany = "N";
						}

						String isGLEditableForCapital = (String)CompCode.getFieldValue("isGLEditableForCapital");
						if(StringUtil.nullOrEmptyOrBlankString(isGLEditableForCapital))
						{
							isGLEditableForCapital = "N";
						}

						String InterCompany = (String)ResourceService.getString(StringTable, TradingPartnerAccCat);
						String CBSCapital = (String)ResourceService.getString(StringTable, CBSCapitalAccCat);
						String MACH1Capital = (String)ResourceService.getString(StringTable, MACH1CapitalAccCat);

						Log.customer.debug(" CatGeneralLedgerTextEditability : acccatstr " + acccatstr);
						Log.customer.debug(" CatGeneralLedgerTextEditability : InterCompany %s , CBSCapital %s , MACH1Capital %s " + InterCompany, CBSCapital, MACH1Capital);

						if(acccatstr.equalsIgnoreCase(InterCompany) && isGLEditableForInterCompany.equalsIgnoreCase("N")){
							Log.customer.debug(" CatGeneralLedgerTextEditability : InterCompany : return false");
							return false;
						}
						else if((acccatstr.equalsIgnoreCase(CBSCapital) || acccatstr.equalsIgnoreCase(MACH1Capital)) && isGLEditableForCapital.equalsIgnoreCase("N"))
						{
							Log.customer.debug(" CatGeneralLedgerTextEditability : Capital : return false");
							return false;
						}
				}
				}
		}
		catch(Exception e)
		{
			Log.customer.debug("Error in file CatGeneralLedgerTextEditability : " + e);
			return true;
		}
		return true;
	}

	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)
	{
		return null;
	}

	public CatGeneralLedgerTextEditability() {
		super();
	}
}
