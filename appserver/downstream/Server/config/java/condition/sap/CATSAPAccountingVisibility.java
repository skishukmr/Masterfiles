/*****************************************************************************
*
*
*   Change History:
*   Change By    	Change Date     Description
*	--------------------------------------------------------------------------------
*   Majid     		01-22-2009        Created
*   S. Sato         03-15-2011        Changed references to MARLineItem to
*                                     ContractRequestLineItem
*******************************************************************************/


package config.java.condition.sap;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.contract.core.ContractRequestLineItem;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.sap.CATSAPUtils;

public class CATSAPAccountingVisibility extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
    	Log.customer.debug("CATSAPAccountingVisibility : started ...");
    	Log.customer.debug("CATSAPAccountingVisibility : called evaluateImpl method ...");
   	 	ProcureLineItem pli = (ariba.procure.core.ProcureLineItem)params.getPropertyForKey("LI");
   	 	Log.customer.debug("CATSAPAccountingVisibility : pli => " + pli);
   	 	if(pli==null)
   		 	return false;

   	 	//Log.customer.debug("CATSAPAccountingVisibility : pli.getBaseId().get() => " + pli.getBaseId().get());

	   	if(pli.instanceOf("ariba.procure.core.ProcureLineItem"))
	   	{
		   	Log.customer.debug("CATSAPAccountingVisibility : It is a ProcureLineItem Object => " + pli);
		   	return evaluateLineItemForAccVal(pli);
	   	}
	   	else
	   	{
		   	Log.customer.debug("CATSAPAccountingVisibility : It is not an instance of ProcureLineItem => " + pli);
		   	return false;
	   	}
    }



	public CATSAPAccountingVisibility()
	{
		super();
	}

	private static boolean evaluateLineItemForAccVal(ProcureLineItem pli){


	   	if(pli.instanceOf("ariba.purchasing.core.ReqLineItem")  )
	   	{
		   	Log.customer.debug("CATSAPAccountingVisibility : It is a ReqLineItem Object => " + pli);
		   	return CATSAPUtils.isAccountValidationRequired((ReqLineItem)pli);
		}
	   	else if (pli.instanceOf("ariba.contract.core.ContractRequestLineItem"))
	   	{
	   		Log.customer.debug("CATSAPAccountingVisibility : It is a MARLineItem Object => " + pli);
		   	return CATSAPUtils.isAccountValidationRequired((ContractRequestLineItem)pli);
	   	}
	   	else if (pli.instanceOf("ariba.invoicing.core.InvoiceReconciliationLineItem"))
	   	{
	   		Log.customer.debug("CATSAPAccountingVisibility : It is a InvoiceReconciliationLineItem Object => " + pli);
		   	return CATSAPUtils.isAccountValidationRequired((InvoiceReconciliationLineItem)pli);
	   	}

	   	else
	   	{
	   		Log.customer.debug("CATSAPAccountingVisibility : It is neither ReqLineItem  or MARLineItem Object => " + pli);
		   	return false;
	   	}
	}
	protected ValueInfo[] getParameterInfo()
    {
    	return parameterInfo;
    }

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

private static ValueInfo parameterInfo[] = {new ValueInfo("LI",Condition.IsScalar,"ariba.procure.core.ProcureLineItem")};
private static final String requiredParameterNames[]= {"LI"};
}
