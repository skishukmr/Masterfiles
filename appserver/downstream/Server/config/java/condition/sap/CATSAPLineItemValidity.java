package config.java.condition.sap;

import java.util.HashMap;
import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CATSAPLineItemValidity extends Condition{


	private static final String classname = "CATSAPLineItemValidity : ";
    public static String LICToCheck = "LICToCheck";
//    private static final ValueInfo valueInfo = new ValueInfo(0);
	private static final ValueInfo parameterInfo[] = {new ValueInfo(LICToCheck, 0, "ariba.procure.core.ProcureLineItemCollection")};
    private String requiredParameterNames[];

	public boolean evaluate (Object value, PropertyTable params)
    throws ConditionEvaluationException	
	{
		Log.customer.debug(" %s : obj %s " ,classname, value);
		if (value !=null){
		ProcureLineItemCollection lic =(ProcureLineItemCollection)params.getPropertyForKey(LICToCheck);
		Log.customer.debug(" %s : li %s " ,classname , lic);
		if(lic==null){
			return false;
		}
		Log.customer.debug(" %s : lic %s " ,classname , lic);
		ClusterRoot companycode = (ClusterRoot)lic.getDottedFieldValue("CompanyCode");
		Log.customer.debug(" %s : companycode %s " ,classname , companycode);
		if (companycode == null){
			return false;
		}
		
		if(checkIfMultipleAccCat(lic) || checkIfMultipleIO(lic) || checkIfMultipleWBS(lic) || checkIfMultipleTradingPartner(lic)){
			Log.customer.debug(" %s : return false " ,classname);
			return false;
		}
		
		}
		
		Log.customer.debug(" %s : return true " ,classname);
		return true;
	}
	
	public ConditionResult evaluateAndExplain (Object value,PropertyTable params)throws ConditionEvaluationException
	{
		if (!evaluate(value, params)) {
			
			String errorMessage = "";
			if(ErrorCode == 1){
				errorMessage = ResourceService.getString(ComponentStringTable,MultipleIOMsg );
			}else if(ErrorCode == 2){
				errorMessage = ResourceService.getString(ComponentStringTable,MultipleWBSMsg );
			}else if(ErrorCode == 3){
				errorMessage = ResourceService.getString(ComponentStringTable,MultipleTradingPartnerMsg );
			}else if(ErrorCode == 4){
				errorMessage = ResourceService.getString(ComponentStringTable,MultipleAccountCategoryMsg );
			}
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
	int ErrorCode = 0;
	private static final String MultipleIOMsg = "MultipleIOMsg";
	private static final String MultipleWBSMsg = "MultipleWBSMsg";
	private static final String MultipleTradingPartnerMsg = "MultipleTradingPartnerMsg";
	private static final String MultipleAccountCategoryMsg = "MultipleAccountCategoryMsg";
	
	protected boolean checkIfMultipleIO(LineItemCollection lic){
		
		Log.customer.debug(" %s : return checkIfMultipleIO " ,classname);
		
		List lineItems = (List)lic.getLineItems();
		HashMap ht = new HashMap();
		for(int line=0; line <lineItems.size(); line++)
		{
			ProcureLineItem li = (ProcureLineItem) lineItems.get(line);
			SplitAccountingCollection sac = (SplitAccountingCollection)li.getAccountings();
			List splitAccountings = (List) sac.getSplitAccountings();
			 for (int i=0; i<splitAccountings.size();i++){
				 SplitAccounting sa = (SplitAccounting)splitAccountings.get(i);
				 String internalOrder = (String)sa.getFieldValue("InternalOrderText");
				 if (internalOrder != null){
					 ht.put(internalOrder,(new Integer(i).toString() + "-" + new Integer(line).toString()));					 
				 }		 
			 }
			}
		if(ht.size()>1){
			 Log.customer.debug(" %s : return checkIfMultipleIO - retrun true " ,classname);
			 ErrorCode = 1;
			 return true;			
		}
		
			return false;
	}
	
	protected boolean checkIfMultipleWBS(LineItemCollection lic){
		List lineItems = (List)lic.getLineItems();
		HashMap ht = new HashMap();
		for(int line=0; line <lineItems.size(); line++)
		{
			ProcureLineItem li = (ProcureLineItem) lineItems.get(line);
			SplitAccountingCollection sac = (SplitAccountingCollection)li.getAccountings();
			List splitAccountings = (List) sac.getSplitAccountings();
			 for (int i=0; i<splitAccountings.size();i++){
				 SplitAccounting sa = (SplitAccounting)splitAccountings.get(i);
				 String wbsElement = (String)sa.getFieldValue("WBSElementText");
				 if (wbsElement != null){
					 ht.put(wbsElement,(new Integer(i).toString() + "-" + new Integer(line).toString()));
				 }		 
			 }
			}
		if(ht.size()>1){
			 Log.customer.debug(" %s : return checkIfMultipleWBS - retrun true " ,classname);
			 ErrorCode = 2;
			 return true;			
		}
			return false;
	}
	
	protected boolean checkIfMultipleTradingPartner(LineItemCollection lic){
		List lineItems = (List)lic.getLineItems();
		String firstTP = "";
		int firstTPLine = 0;
		for(int line=0; line <lineItems.size(); line++)
		{
			ProcureLineItem li = (ProcureLineItem) lineItems.get(line);
			String tradingPartner = (String)li.getDottedFieldValue("TradingPartner.UniqueName");
			if (tradingPartner !=null)
			{
				firstTP = tradingPartner;
				Log.customer.debug(" %s : return checkIfMultipleTradingPartner firstTP %s" ,classname,firstTP);
				firstTPLine = line;
				break;
			}
		}
		 if(!firstTP.equals("") && firstTP != null){
			for(int i = firstTPLine; i <lineItems.size(); i++)
			{
				ProcureLineItem li = (ProcureLineItem) lineItems.get(i);
				String tradingPartner = (String)li.getDottedFieldValue("TradingPartner.UniqueName");
				 if (tradingPartner != null && !tradingPartner.equalsIgnoreCase(firstTP)){
					 Log.customer.debug(" %s : return checkIfMultipleTradingPartner retrun true " ,classname);
					 ErrorCode = 3;
					 return true;
				 }
			}
		 }
			return false;
	}

	protected boolean checkIfMultipleAccCat(LineItemCollection lic){
		List lineItems = (List)lic.getLineItems();
		String firstAC = "";
		int firstACLine = 0;
		for(int line=0; line <lineItems.size(); line++)
		{
			ProcureLineItem li = (ProcureLineItem) lineItems.get(line);
			String accCategory = (String)li.getDottedFieldValue("AccountCategory.UniqueName");
			if (accCategory !=null)
			{
				firstAC = accCategory;
				Log.customer.debug(" %s : return checkIfMultipleAccCat firstAC %s" ,classname,firstAC);
				firstACLine = line;
				break;
			}
		}
		 if(!firstAC.equals("") && firstAC != null){
			for(int i = firstACLine; i <lineItems.size(); i++)
			{
				ProcureLineItem li = (ProcureLineItem) lineItems.get(i);
				String accCategory = (String)li.getDottedFieldValue("AccountCategory.UniqueName");
				 if (accCategory != null && !accCategory.equalsIgnoreCase(firstAC)){
					 Log.customer.debug(" %s : return checkIfMultipleAccCat retrun true " ,classname);
					 ErrorCode = 4;
					 return true;
				 }
			}
		 }
			return false;
	}
	
}
