// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:34 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3) 

package config.java.invoiceeform.sap;

import java.util.List;

import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPValidateNonSummaryInvoice extends Condition
{

    public CatSAPValidateNonSummaryInvoice()
    {

    }

    public boolean evaluate(Object obj, PropertyTable propertytable)
    {
        return evaluateImpl(obj, propertytable);
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
    {
        boolean flag = evaluate(obj, propertytable);
        if(flag)
            return null;
        else
            return new ConditionResult(Fmt.Sil("aml.InvoiceEform", "SummaryInvoiceError", subjectForMessages(propertytable)));
    }

    private boolean evaluateImpl(Object obj, PropertyTable propertytable)
    {
        ClusterRoot clusterroot = (ClusterRoot)propertytable.getPropertyForKey("InvoiceEForm");
        List list = (List)clusterroot.getFieldValue("Orders");
        Log.customer.debug("CatSAPValidateNonSummaryInvoice : " + list.size());
        if((list.size() > 1))
        	return false;       	
        else
        	return !hasMultipleOrder(clusterroot);
    }
    

    public ValueInfo getValueInfo()
    {
        return valueInfo;
    }

    public ValueInfo[] getParameterInfo()
    {
        return parameterInfo;
    }

    public String[] getRequiredParameterNames()
    {
        return requiredParameterNames;
    }

    private static final ValueInfo valueInfo = new ValueInfo(0);
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("InvoiceEForm", 0, "config.java.invoiceeform.InvoiceEform")
    };
    private static final String requiredParameterNames[] = {
        "InvoiceEForm"
    };
    
	public static boolean hasMultipleOrder(ariba.base.core.ClusterRoot clusterroot) {
		
		Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder " + clusterroot);
        
		if(clusterroot!=null){
			List lineitems = (List)clusterroot.getFieldValue("LineItems");
			if(lineitems!=null)
			{
				String ordernumber = (String)clusterroot.getFieldValue("SetOrder");
				if(ordernumber==null || ordernumber.equals("")){
					return false;
				}
				String firstorder ="";
				int firstorderlineno = 1;
				
				int nooflines = lineitems.size();
				Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : nooflines " + nooflines);
		        
				if (nooflines == 0){
					Log.customer.debug("CatSAPValidateNonSummaryInvoice : nooflines is 0 : return false");
					return false;
		        }
				
					for(int i=0;i<nooflines;i++)
					{
						BaseObject lineitem =(BaseObject)lineitems.get(i);
						if(lineitem!=null){
							ariba.base.core.ClusterRoot order = (ClusterRoot)lineitem.getFieldValue("Order");
							Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : 1st loop :order " + order);
					        
							if(order!=null)
							{
								firstorder = (String)order.getUniqueName();
								Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : 1st loop : firstorder " + firstorder);
						        firstorderlineno = (i+1);
								break;
							}
						}
					}

					if(ordernumber!=null && !ordernumber.trim().equalsIgnoreCase(firstorder)){
						return true;
					}
					for(int j=firstorderlineno; j<nooflines;j++)
					{
						BaseObject lineitem1 =(BaseObject)lineitems.get(j);
						if(lineitem1!=null){
							ariba.base.core.ClusterRoot order1 = (ClusterRoot)lineitem1.getFieldValue("Order");
							Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : 2nd loop : order1 " + order1);
					        
							if(order1!=null)
							{
								String nextorder = (String)order1.getUniqueName();
								if(!firstorder.equalsIgnoreCase(nextorder))
								{
									Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : 2nd loop : firstorder " + firstorder);
									Log.customer.debug("CatSAPValidateNonSummaryInvoice : hasMultipleOrder : 2nd loop : nextorder " + nextorder);
							        return true;
								}
							}
						}
					}

			
			}else{
				Log.customer.debug("CatSAPValidateNonSummaryInvoice : lineitems is null : return false");
				return false;
			}
			
		}
		return false;
	}

    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String StringTable = "ariba.common.core.condition";
    private static final String ErrorMsgKey = "SummaryInvoiceError";

}