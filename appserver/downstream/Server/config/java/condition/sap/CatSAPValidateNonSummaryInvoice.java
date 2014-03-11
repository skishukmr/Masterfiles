// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:34 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.condition.sap;

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

        Log.customer.debug("CatSAPValidateNonSummaryInvoice : clusterroot " + clusterroot);
        if (clusterroot !=null)
        {
        List lineitems = (List)clusterroot.getFieldValue("LineItems");
        Log.customer.debug("CatSAPValidateNonSummaryInvoice : lineitems " + lineitems);

        for(int i=0; i < lineitems.size();i++)
        {
        	Log.customer.debug("CatSAPValidateNonSummaryInvoice : i " + i);
            BaseObject lineitem = (BaseObject)lineitems.get(i);
            Log.customer.debug("CatSAPValidateNonSummaryInvoice : lineitem " + lineitem);

            ClusterRoot order = (ClusterRoot)lineitem.getFieldValue("Order");
            Log.customer.debug("CatSAPValidateNonSummaryInvoice : order " + order);

            if (order != null)
        	{
            	for(int j=0; j < list.size();j++)
            	{
            		if(list.get(j)!=order)
            		{
            			Log.customer.debug("CatSAPValidateNonSummaryInvoice inside : " + list.size());
            	        list.add((i),order);
            		}
            	}
            }
        }
        }
        //List list = (List)clusterroot.getFieldValue("Orders");
        Log.customer.debug("CatSAPValidateNonSummaryInvoice : " + list.size());
        return list.size() <= 1;
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
    private static final String ComponentStringTable = "aml.InvoiceEform";
    private static final String StringTable = "ariba.common.core.condition";
    private static final String ErrorMsgKey = "SummaryInvoiceError";

}