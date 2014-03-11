/*****************************************************************************
*
*
*   Change History:
*   Change By    	Change Date     Description
*	--------------------------------------------------------------------------------
*   Majid     		11-25-2008        Created
*******************************************************************************/


package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.base.fields.*;
import ariba.purchasing.core.PurchaseOrder;
import ariba.util.core.PropertyTable;
import ariba.base.core.*;
import ariba.util.log.Log;
import java.util.*;


public class CATWithHoldTaxCodeVisibility extends Condition
{

    public boolean evaluate(Object value, PropertyTable params)
    {
    	Log.customer.debug("CATWithHoldTaxCodeVisibility : started ...");
        return evaluateImpl(value,params);

    }

    private final boolean evaluateImpl(Object value, PropertyTable params)
    {
    	 Log.customer.debug("CATWithHoldTaxCodeVisibility : called evaluateImpl method ...");
    	 ClusterRoot cs = (ClusterRoot)params.getPropertyForKey("CR");


    	 Log.customer.debug("CATWithHoldTaxCodeVisibility : cs => " + cs);
    	 if(cs==null)
    		 	return FAIL;

    	 Log.customer.debug("CATWithHoldTaxCodeVisibility : cs.getBaseId().get() => " + cs.getBaseId().get());

    	 if(cs.instanceOf("ariba.invoicing.core.Invoice") || cs.instanceOf("ariba.invoicing.core.InvoiceReconciliation"))
    	 {
    		 Log.customer.debug("CATWithHoldTaxCodeVisibility : It is an Invoice Object => " + cs);
    		 return evaluateInvoice(cs);
    	 }
    	 else
    	 {
    		 Log.customer.debug("CATWithHoldTaxCodeVisibility : It is an InvoiceEform Object => " + cs);
    		 return evaluateInvoiceEform(cs);
    	 }

    }

	public CATWithHoldTaxCodeVisibility()
	{
		super();
	}

	private static boolean evaluateInvoiceEform(ClusterRoot inveform){

		 	Log.customer.debug("CATWithHoldTaxCodeVisibility : It is an InvoiceEform call => " + inveform);
		 	boolean isLineType = false;

			List lineItems = (List)inveform.getFieldValue("LineItems");
			Log.customer.debug("CATWithHoldTaxCodeVisibility :evaluateInvoiceEform Line Item Size => " + lineItems.size());
	        int size = lineItems.size();
	        for (int i = 0; i < size; i++)
	        {
	            BaseObject lineItem = (BaseObject)lineItems.get(i);
	            PurchaseOrder po = (PurchaseOrder)lineItem.getFieldValue("Order");
	            Log.customer.debug("CATWithHoldTaxCodeVisibility :evaluateInvoiceEform PurchaseOrder => " +po);

	            if(po!=null)
	            {
	            	Log.customer.debug("CATWithHoldTaxCodeVisibility : PO is not null => " + po.getUniqueName() );
	            	if(i==0 && po.getDottedFieldValue("CompanyCode")!= null)
	            	{
	            	String strwithholdingTaxEnabled = (String) po.getDottedFieldValue("CompanyCode.WithholdingTaxEnabled");
	            	Log.customer.debug("CATWithHoldTaxCodeVisibility : strwithholdingTaxEnabled => " + po.getDottedFieldValue("CompanyCode.WithholdingTaxEnabled") );
	            	if(strwithholdingTaxEnabled == null)
	            			return FAIL;


	            	if(!(strwithholdingTaxEnabled.equals("Y") || strwithholdingTaxEnabled.equals("y")))
	            		{
	            			Log.customer.debug("CATWithHoldTaxCodeVisibility : strwithholdingTaxEnabled => " + strwithholdingTaxEnabled );
	            			return FAIL;
	            		}
	            	}
	            	isLineType = processPO(po);
	            	Log.customer.debug("CATWithHoldTaxCodeVisibility : evaluateInvoiceEform isLineType => "+isLineType);

	            	if(isLineType)
	            	{
	            			Log.customer.debug("CATWithHoldTaxCodeVisibility : PO has got valid Line Item type");
	            			break;
	            	}
	            }
	            else
	            {
	            	Log.customer.debug("CATWithHoldTaxCodeVisibility : PO is null... skipping this line");
	            	continue;
	            }
	        }

	            Log.customer.debug("CATWithHoldTaxCodeVisibility : Returning isLineType =>" + isLineType);
	            return isLineType;

	}

	private static boolean processPO(PurchaseOrder po)
	{

		Log.customer.debug("CATWithHoldTaxCodeVisibility : Inside processPO : Value of SERVICEONLY " +SERVICEONLY );
		boolean ispoLineType = false;
		Log.customer.debug("CATWithHoldTaxCodeVisibility : Inside processPO:: " + po.getUniqueName());
		BaseVector poLineItems = (BaseVector) po.getLineItems();

		Log.customer.debug("CATWithHoldTaxCodeVisibility : Inside processPO PO Line Item Size:: " +poLineItems.size());
		for (int i =0; i<poLineItems.size();i++)
		{
			LineItem poLineItem = (LineItem)poLineItems.get(i);
			String lineItemType = (String) poLineItem.getDottedFieldValue("LineItemType");
			Log.customer.debug("CATWithHoldTaxCodeVisibility : Inside processPO : Line Item # => " + i + " has LineItemType => " + lineItemType);

			if(lineItemType!=null && lineItemType.equals(SERVICEONLY))
			{
				ispoLineType = true;
				break;
			}
			else
				continue;
		}
		return ispoLineType;

	}

	private static boolean evaluateInvoice(ClusterRoot invoice){

	 Log.customer.debug("CATWithHoldTaxCodeVisibility : evaluateInvoice : It is an Invoice => " + invoice);
	 // Don't user Invoice.Order , It may be null for Invocie created against Contract without release
	 boolean isLineType = false;



	 if(invoice.getDottedFieldValue("CompanyCode")!= null)
	 {
		 String strwithholdingTaxEnabled = (String) invoice.getDottedFieldValue("CompanyCode.WithholdingTaxEnabled");
		 Log.customer.debug("CATWithHoldTaxCodeVisibility : strwithholdingTaxEnabled => " + invoice.getDottedFieldValue("CompanyCode.WithholdingTaxEnabled") );
		 if(strwithholdingTaxEnabled == null)
 			return FAIL;

		 if(!(strwithholdingTaxEnabled.equals("Y") || strwithholdingTaxEnabled.equals("y")))
			{
				Log.customer.debug("CATWithHoldTaxCodeVisibility : strwithholdingTaxEnabled => " + strwithholdingTaxEnabled );
				return FAIL;
			}
	 }

	 List invlineItems = (List)invoice.getFieldValue("LineItems");
	 Log.customer.debug("CATWithHoldTaxCodeVisibility :evaluateInvoice Line Item Size => " + invlineItems.size());
     int size = invlineItems.size();
     for (int i = 0; i < size; i++)
     {
         BaseObject lineItem = (BaseObject)invlineItems.get(i);
         String lineItemType = (String) lineItem.getDottedFieldValue("LineItemType");
         Log.customer.debug("CATWithHoldTaxCodeVisibility : evaluateInvoice : Line Item # => " + i + " has LineItemType => " + lineItemType);

			if(lineItemType!=null && lineItemType.equals(SERVICEONLY))
			{
				isLineType = true;
				break;
			}
			else
				continue;

		}
     	Log.customer.debug("CATWithHoldTaxCodeVisibility : evaluateInvoice : ispoLineType => " + isLineType);
     	return isLineType;
}


	protected ValueInfo[] getParameterInfo()
    {
    	return parameterInfo;
    }

	protected String[] getRequiredParameterNames()
	{
		return requiredParameterNames;
	}

private static ValueInfo parameterInfo[] = {new ValueInfo("CR",Condition.IsScalar,"ariba.base.core.ClusterRoot")};
private static final String requiredParameterNames[]= {"CR"};
public static boolean  SUCCESS = true;
public static boolean  FAIL = false;
public static String SERVICEONLY = "Service Only (TQS)";

}
