// Decompiled by DJ v3.7.7.81 Copyright 2004 Atanas Neshkov  Date: 9/18/2006 1:55:14 PM
// Home Page : http://members.fortunecity.com/neshkov/dj.html  - Check often for new version!
// Decompiler options: packimports(3)

package config.java.invoiceeform.vcsv1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.invoiceeform.CatInvoiceEformSubmitHook;

public class CatCSVInvoiceEformSubmitHook extends CatInvoiceEformSubmitHook
{

    public CatCSVInvoiceEformSubmitHook()
    {
    }

    public List run(Approvable approvable)
    {
        List list = super.run(approvable);
        Integer integer = (Integer)list.get(0);
        if(integer != null && integer.intValue() != 0)
            return list;
        Approvable approvable1 = approvable;
        List list1 = (List)approvable1.getFieldValue("LineItems");
        int i = ListUtil.getListSize(list1);
        List list2 = ListUtil.list();
        for(int j = 0; j < i; j++)
        {
            BaseObject baseobject = (BaseObject)list1.get(j);
            Object obj = baseobject.getDottedFieldValue("Order");
            ListUtil.addElementIfAbsent(list2, obj);
        }

        if(ListUtil.getListSize(list2) > 1)
        {
            String s = ResourceService.getString("aml.InvoiceEform", "SummaryInvoiceError");
            return ListUtil.list(Constants.getInteger(-2), s);
        }
        boolean flag = hasMultipleTaxLines(list1);
        if(flag)
        {
            String s1 = ResourceService.getString("aml.InvoiceEform", "MultipleTaxLineError");
            return ListUtil.list(Constants.getInteger(-2), s1);
        }
        boolean flag1 = vatReasonablnessCheck(list1);
        if(!flag1)
        {
            String s2 = ResourceService.getString("aml.InvoiceEform", "VATResonablenessError");
            return ListUtil.list(Constants.getInteger(-2), s2);
        }
        boolean flag2 = checkIfAddMatLineValid(list1);
        if(!flag2)
        {
            String s3 = ResourceService.getString("aml.InvoiceEform", "InvalidAdditionalLine");
            return ListUtil.list(Constants.getInteger(-2), s3);
        } else
        {
            List list3 = reorderINEFLineItems(list1);
            BaseVector basevector = new BaseVector();
            basevector.addAll(list3);
            approvable1.setDottedFieldValue("LineItems", basevector);
            return NoErrorResult;
        }
    }

    public static List reorderINEFLineItems(List list)
    {
        ArrayList arraylist = null;
        ArrayList arraylist1 = new ArrayList();
        ArrayList arraylist2 = new ArrayList();
        ArrayList arraylist3 = new ArrayList();
        Object obj = null;
        Object obj1 = null;
        if(list != null && !list.isEmpty())
        {
            int i = list.size();
            for(int j = 0; j < i; j++)
            {
                BaseObject baseobject = (BaseObject)list.get(j);
                Integer integer = (Integer)baseobject.getFieldValue("ReferenceLineNumber");
                Integer integer2 = (Integer)baseobject.getFieldValue("InvoiceLineNumber");
                if(integer != null && integer.intValue() == integer2.intValue())
                {
                    arraylist1.add(baseobject);
                    continue;
                }
                if(integer != null && integer.intValue() == 0)
                    arraylist3.add(baseobject);
                else
                    arraylist2.add(baseobject);
            }

            int k = arraylist3.size();
            int l = arraylist1.size();
            int i1 = arraylist2.size();
            Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: Line Counts(Material/AC/Tax): " + l + "/" + i1 + "/" + k);
            arraylist = new ArrayList();
            if(l > 0)
            {
				//Start of Issue# 708
                for(int j1 = 0, acindex = 0; j1 < l; j1++)
                //End of Issue# 708
                {
                    BaseObject baseobject1 = (BaseObject)arraylist1.get(j1);
                    int l1 = arraylist.size();
                    baseobject1.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                    Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: Updated M Ref Num From to " + (l1 + 1));
                    arraylist.add(baseobject1);
                    if(i1 <= 0)
                        continue;
                    //Start of Issue# 708
                    for(int i2 = acindex; i2 < i1; i2++)
                    //End of Issue# 708
                    {
						Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: Looping through the ACLines ***  Starting From::: " + acindex);
                        BaseObject baseobject2 = (BaseObject)arraylist2.get(i2);
                        Integer integer1 = (Integer)baseobject2.getFieldValue("ReferenceLineNumber");
                        Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: refNumInt: %s", integer1);
                        if(integer1 != null && integer1.intValue() == ((Integer)baseobject1.getFieldValue("InvoiceLineNumber")).intValue())
                        {
							//Start of Issue# 708
							acindex = i2 + 1;
							//End of Issue# 708
							Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: BEFORE UPDATE *** AC refNumInt  " + integer1.intValue() + " InvoiceLineNumber for M Line " + ((Integer)baseobject1.getFieldValue("InvoiceLineNumber")).intValue());
                            baseobject2.setDottedFieldValue("ReferenceLineNumber", new Integer(l1 + 1));
                            Log.customer.debug("CatCSVInvoiceEformSubmitHook ::: Updated AC Ref Num From to " + (l1 + 1));
                            arraylist.add(baseobject2);
                        }
                    }

                }

            }
            if(k > 0)
            {
                for(int k1 = 0; k1 < k; k1++)
                    arraylist.add(arraylist3.get(k1));

            }
        }
        return arraylist;
    }

    public static boolean hasMultipleTaxLines(List list)
    {
        int i = 0;
        int j = 0;
        for(int k = 0; k < list.size(); k++)
        {
            BaseObject baseobject = (BaseObject)list.get(k);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
            if(procurelinetype.getCategory() == 2)
                i++;
            if(clusterroot == null)
                continue;
            String s = clusterroot.getUniqueName();
            if("002".equals(s))
                j++;
            if("003".equals(s))
                j++;
            if("096".equals(s))
                j++;
        }

        return j > 1 || i > 1;
    }

    public static boolean vatReasonablnessCheck(List list)
    {
        BigDecimal bigdecimal = new BigDecimal("0.0000");
        BigDecimal bigdecimal1 = new BigDecimal("0.0000");
        BigDecimal bigdecimal2 = new BigDecimal("100");
        BigDecimal bigdecimal3 = new BigDecimal("50.00");
        Object obj = null;
        boolean flag = false;
        for(int i = 0; i < list.size(); i++)
        {
            BaseObject baseobject = (BaseObject)list.get(i);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
            if(procurelinetype.getCategory() == 2)
            {
                if("VATCharge".equals(procurelinetype.getUniqueName()))
                {
                    flag = true;
                    bigdecimal = (BigDecimal)baseobject.getDottedFieldValue("Amount.Amount");
                }
            } else
            {
                bigdecimal1 = bigdecimal1.add((BigDecimal)baseobject.getDottedFieldValue("Amount.Amount"));
            }
        }

        if(flag)
        {
            BigDecimal bigdecimal4 = bigdecimal.multiply(bigdecimal2);
            bigdecimal4 = bigdecimal4.divide(bigdecimal1, 4);
            //if(Log.customer.debugOn)
                Log.customer.debug("%s ::: vatReasonablnessCheck: The percentage of VAT Charged is %s", "CatCSVInvoiceEformSubmitHook", bigdecimal4.toString());
            if(bigdecimal4.compareTo(bigdecimal3) > 0)
            {
                //if(Log.customer.debugOn)
                    Log.customer.debug("%s ::: vatReasonablnessCheck: Returning false", "CatCSVInvoiceEformSubmitHook");
                return false;
            }
        }
        //if(Log.customer.debugOn)
            Log.customer.debug("%s ::: vatReasonablnessCheck: Returning true", "CatCSVInvoiceEformSubmitHook");
        return true;
    }

    public static boolean checkIfAddChargeLineValid(List list)
    {
        boolean flag = false;
        boolean flag1 = false;
        for(int i = 0; i < list.size(); i++)
        {
            BaseObject baseobject = (BaseObject)list.get(i);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
            if(procurelinetype.getCategory() == 2 && ("ServiceUseTax".equals(procurelinetype.getUniqueName()) || "SalesTaxCharge".equals(procurelinetype.getUniqueName())))
                flag = true;
            if(clusterroot == null)
                continue;
            String s = clusterroot.getUniqueName();
            if("003".equals(s) || "002".equals(s))
                flag = true;
        }

        for(int j = 0; j < list.size(); j++)
        {
            BaseObject baseobject1 = (BaseObject)list.get(j);
            ProcureLineType procurelinetype1 = (ProcureLineType)baseobject1.getFieldValue("LineType");
            if(procurelinetype1.getCategory() == 16)
                flag1 = true;
        }

        return !flag1 || flag;
    }

    public static boolean checkIfAddMatLineValid(List list)
    {
        boolean flag = false;
        for(int i = 0; i < list.size(); i++)
        {
            BaseObject baseobject = (BaseObject)list.get(i);
            ProcureLineType procurelinetype = (ProcureLineType)baseobject.getFieldValue("LineType");
            if(procurelinetype.getCategory() == 1 && baseobject.getFieldValue("OrderLineItem") == null)
                flag = true;
        }

        return !flag;
    }
}