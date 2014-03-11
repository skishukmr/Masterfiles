/*****************************************************************************
*
*
*   Change History:
*   Change By    	Change Date     Description
*	--------------------------------------------------------------------------------
*   Divya     		01-May-2010        Created for Value of InboundReceipt in CompanyCode
*******************************************************************************/


package config.java.condition.sap;
import ariba.base.fields.Condition;
import ariba.base.fields.ValueInfo;
import ariba.receiving.core.ReceiptCoreApprovable;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATSAPInboundCheck extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
    	Log.customer.debug("CATSAPInboundCheck : started ...");
    	Log.customer.debug("CATSAPInboundCheck : called evaluateImpl method ...");
   	 	ReceiptCoreApprovable  pli = (ariba.receiving.core.ReceiptCoreApprovable)params.getPropertyForKey("LI");
   	 	Log.customer.debug("CATSAPInboundCheck : pli => " + pli);
   	 	if(pli==null)
   		 	return false;
		//int nolineitems=pli.getLineItemsCount();


   	 	//Log.customer.debug("CATSAPAccountingVisibility : pli.getBaseId().get() => " + pli.getBaseId().get());

	   	if(pli.instanceOf("ariba.receiving.core.ReceiptCoreApprovable"))
	   	{
		   	Log.customer.debug("CATSAPInboundCheck : It is a Receipt Object => " + pli);
		   	return evaluateLineItemForAccVal(pli);
	   	}
	   	else
	   	{
		   	Log.customer.debug("CATSAPInboundCheck : It is not an instance of ProcureLineItem => " + pli);
		   	return false;
	   	}
		//return true;
    }



	public CATSAPInboundCheck()
	{
		super();
	}

	private static boolean evaluateLineItemForAccVal(ReceiptCoreApprovable pli){


	   	if(pli.instanceOf("ariba.receiving.core.ReceiptCoreApprovable")  )
	   	{
		  // ReceiptCoreApprovable receipt=(ReceiptCoreApprovable)pli.getReceipt();
  		if(pli.getDottedFieldValue("Order.CompanyCode.InboundReceipt")!=null)
			{
		   	String inboundValue=(String)pli.getDottedFieldValue("Order.CompanyCode.InboundReceipt");
		  	if(inboundValue.equalsIgnoreCase("Y"))
			{
			   return true;
			}
			}

		}
		return false;

	}
	protected ValueInfo[] getParameterInfo()
    {
    	return parameterInfo;
    }

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

private static ValueInfo parameterInfo[] = {new ValueInfo("LI",Condition.IsScalar,"ariba.receiving.core.ReceiptCoreApprovable")};
private static final String requiredParameterNames[]= {"LI"};
}
