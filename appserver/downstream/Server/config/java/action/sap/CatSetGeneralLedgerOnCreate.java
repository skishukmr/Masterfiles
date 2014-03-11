package config.java.action.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.Accounting;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetGeneralLedgerOnCreate extends Action{

	private static final String StringTable = "aml.CatSAPRequisitionExt";
	private static final String TradingPartnerAccCat = "TradingPartnerAccCat";
	private static final String CBSCapitalAccCat = "CBSCapitalAccCat";
	private static final String MACH1CapitalAccCat = "MACH1CapitalAccCat";

	public void fire(ValueSource object, PropertyTable params)
	{
		Accounting sa =(Accounting)object;
		LineItem li = (LineItem)sa.getFieldValue("LineItem");				
		Log.customer.debug(" CatSetGeneralLedgerOnCreate : li " + li);
		if(li==null){
			return;
		}
		LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
		Log.customer.debug(" CatSetGeneralLedgerOnCreate : lic " + lic);
		if(lic!=null){
		BaseObject CompCode = (BaseObject)lic.getDottedFieldValue("CompanyCode");	
		if(CompCode == null){
			return;
		}
		BaseObject acccat = (BaseObject)li.getDottedFieldValue("AccountCategory");
		if(acccat == null){
			return;
		}

		String InterCompany = (String)ResourceService.getString(StringTable, TradingPartnerAccCat);			
		String CBSCapital = (String)ResourceService.getString(StringTable, CBSCapitalAccCat);
		String MACH1Capital = (String)ResourceService.getString(StringTable, MACH1CapitalAccCat);

		String acccatstr = (String)acccat.getFieldValue("UniqueName");
		
		String DefaultGLforCapital = (String)CompCode.getFieldValue("DefaultGLforCapital");
		if(!StringUtil.nullOrEmptyOrBlankString(DefaultGLforCapital) && (acccatstr.equalsIgnoreCase(CBSCapital)||acccatstr.equalsIgnoreCase(MACH1Capital)))
		{
			 
			Log.customer.debug(" CatSetGeneralLedgerOnCreate : DefaultGLforCapital " + DefaultGLforCapital);
			sa.setDottedFieldValue("GeneralLedgerText",DefaultGLforCapital);		
		}
		
		String DefaultGLforInterCompany = (String)CompCode.getFieldValue("DefaultGLforInterCompany");
		if(!StringUtil.nullOrEmptyOrBlankString(DefaultGLforInterCompany) && acccatstr.equalsIgnoreCase(InterCompany))
		{
			Log.customer.debug(" CatSetGeneralLedgerOnCreate : DefaultGLforInterCompany " + DefaultGLforInterCompany);
			sa.setDottedFieldValue("GeneralLedgerText",DefaultGLforInterCompany);
		}

	}
	}
	
}
