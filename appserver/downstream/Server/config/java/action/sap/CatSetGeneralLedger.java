package config.java.action.sap;

import java.util.List;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatSetGeneralLedger extends Action{

	private static final String StringTable = "aml.CatSAPRequisitionExt";
	private static final String TradingPartnerAccCat = "TradingPartnerAccCat";
	private static final String CBSCapitalAccCat = "CBSCapitalAccCat";
	private static final String MACH1CapitalAccCat = "MACH1CapitalAccCat";

	public void fire(ValueSource object, PropertyTable params)
	{
		LineItem li = (LineItem)object;				
		Log.customer.debug(" CatSetGeneralLedger : li " + li);
		if(li==null){
			return;
		}
		LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
		Log.customer.debug(" CatSetGeneralLedger : lic " + lic);
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
		
		SplitAccountingCollection  sac = (SplitAccountingCollection)li.getDottedFieldValue("Accountings");
		
		if (sac==null)
		{
			return;
		}
		String DefaultGLforCapital = (String)CompCode.getFieldValue("DefaultGLforCapital");
		if(!StringUtil.nullOrEmptyOrBlankString(DefaultGLforCapital) && (acccatstr.equalsIgnoreCase(CBSCapital)||acccatstr.equalsIgnoreCase(MACH1Capital)))
		{		 
			Log.customer.debug(" CatSetGeneralLedger : DefaultGLforCapital " + DefaultGLforCapital);
			setGLinAllSplitAcc(sac,DefaultGLforCapital);
		}
		
		String DefaultGLforInterCompany = (String)CompCode.getFieldValue("DefaultGLforInterCompany");
		if(!StringUtil.nullOrEmptyOrBlankString(DefaultGLforInterCompany) && acccatstr.equalsIgnoreCase(InterCompany))
		{
			Log.customer.debug(" CatSetGeneralLedger : DefaultGLforInterCompany " + DefaultGLforInterCompany);
			setGLinAllSplitAcc(sac,DefaultGLforInterCompany);
		}

	}
	}
	
	public void setGLinAllSplitAcc(SplitAccountingCollection sac,String GLValue){
		
		List splitAccountings = (List)sac.getDottedFieldValue("SplitAccountings");
		Log.customer.debug(" CatSetGeneralLedger : splitAccountings " + splitAccountings);
		Log.customer.debug(" CatSetGeneralLedger : GLValue " + GLValue);
		
		if(splitAccountings!=null && GLValue!=null){
		for(int i=0;i<splitAccountings.size();i++){
			SplitAccounting sa = (SplitAccounting)splitAccountings.get(i);
			
			Log.customer.debug(" CatSetGeneralLedger : SplitAccounting sa " + sa);
			sa.setDottedFieldValue("GeneralLedgerText",GLValue);
			Log.customer.debug(" CatSetGeneralLedger : GeneralLedgerText has been set to " + sa.getDottedFieldValue("GeneralLedgerText"));
			sa.getLineItem().getLineItemCollection().save();
		}
		}
	}

}
