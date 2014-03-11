// Shaila: Jan 03 08 - have added Null check for LineType
// Vikram: Setting errorCode = 0 (meaning no error) for Special Charge's Ref line number

package config.java.condition.vcsv1;

import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.contract.core.Contract;
import ariba.contract.core.ContractLineItem;
import ariba.invoicing.core.InvoiceLineItem;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.POLineItem;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatCSVValidateRefLineNumber extends Condition
{

    public boolean evaluate(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {

            Log.customer.debug("%s ::: Entering the evaluate method", "CatCSVValidateRefLineNumber");
        validateReferenceLine((BaseObject)obj);

            Log.customer.debug("%s ::: Error Code returning is: " + errorCode, "CatCSVValidateRefLineNumber");
        return errorCode == 0;
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {

            Log.customer.debug("%s ::: Entering the evaluateAndExplain method", "CatCSVValidateRefLineNumber");
        if(!evaluate(obj, propertytable))
        {
            if(errorCode == 1)
                return new ConditionResult(ErrorMsg1);
            if(errorCode == 2)
                return new ConditionResult(ErrorMsg2);
            if(errorCode == 3)
                return new ConditionResult(ErrorMsg3);
            if(errorCode == 4)
                return new ConditionResult(ErrorMsg4);
            if(errorCode == 5)
                return new ConditionResult(ErrorMsg5);
            if(errorCode == 6)
                return new ConditionResult(WarnMsg1, false);
        }
        return null;
    }

    public void validateReferenceLine(BaseObject baseobject)
    {

            Log.customer.debug("%s ::: Entering the validateReferenceLine method", "CatCSVValidateRefLineNumber");
        errorCode = 0;
        Integer integer = (Integer)baseobject.getFieldValue("ReferenceLineNumber");
        int i = 9999;
        if(integer != null)
            i = integer.intValue();
        ClusterRoot clusterroot = baseobject.getClusterRoot();
        BaseVector basevector = (BaseVector)clusterroot.getDottedFieldValue("LineItems");
        ClusterRoot clusterroot1 = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");
        String s = "";
        if(clusterroot1 != null)
            s = clusterroot1.getUniqueName();
        ProcureLineItem procurelineitem = (ProcureLineItem)baseobject.getDottedFieldValue("OrderLineItem");
        if(procurelineitem == null && ((baseobject instanceof InvoiceLineItem) || (baseobject instanceof InvoiceReconciliationLineItem)))
        {
            StatementCoreApprovableLineItem invoicecoreapprovablelineitem = (StatementCoreApprovableLineItem)baseobject;
            procurelineitem = (ProcureLineItem)baseobject.getDottedFieldValue("MALineItem");
        }

        {
            Log.customer.debug("%s ::: The ChargeCode on the lineitem is: %s", "CatCSVValidateRefLineNumber", s);
            Log.customer.debug("%s ::: The RefNum on the lineitem is: " + i, "CatCSVValidateRefLineNumber");
        }
        if(procurelineitem != null)
        {
            if(procurelineitem instanceof POLineItem)
            {

                    Log.customer.debug("%s ::: The pli is instance of POLineItem", "CatCSVValidateRefLineNumber");
                validateReferenceLinePO(baseobject, basevector, procurelineitem, integer, i, clusterroot1, s);
            } else
            if(procurelineitem instanceof ContractLineItem)
            {

                    Log.customer.debug("%s ::: The pli is instance of MALineItem", "CatCSVValidateRefLineNumber");
                validateReferenceLineMA(baseobject, basevector, procurelineitem, integer, i, clusterroot1, s);
            }
        } else
        {

                Log.customer.debug("%s ::: The procure line linked to the passed line is null", "CatCSVValidateRefLineNumber");
            validateReferenceLineFromPLT(baseobject, basevector, integer, i, clusterroot1, s);
        }

            Log.customer.debug("%s ::: Returning errorCode: " + errorCode);
    }

    private void validateReferenceLinePO(BaseObject baseobject, BaseVector basevector, ProcureLineItem procurelineitem, Integer integer, int i, ClusterRoot clusterroot, String s)
    {

            Log.customer.debug("%s ::: Entering the validateReferenceLinePO method", "CatCSVValidateRefLineNumber");
        Object obj = null;
        if(isAdditionalChargeLine(procurelineitem))
        {
            if(integer != null)
            {
                if(i == 0)
                {
                    if(!s.equals("002") && !s.equals("003") && !s.equals("096"))
                    {

                            Log.customer.debug("%s ::: Non Tax line can not have reference line number of 0", "CatCSVValidateRefLineNumber");
                        errorCode = 1;
                    }
                } else
                {
                    if(s.equals("002") || s.equals("003") || s.equals("096"))
                    {

                            Log.customer.debug("%s ::: Tax line does not have reference line number of 0", "CatCSVValidateRefLineNumber");
                        errorCode = 4;
                    }
                    if(basevector != null)
                    {
                        BaseObject baseobject1;
                        try
                        {
                            baseobject1 = (BaseObject)basevector.get(i - 1);
                            Log.customer.debug("%s ::: Test for additional line item -- SUdheer:: %s", "CatCSVValidateRefLineNumber",baseobject1);
                        }
                        catch(Exception exception)
                        {

                                Log.customer.debug("%s ::: Caught the invalid array exception: %s", "CatCSVValidateRefLineNumber", exception.toString());
                            baseobject1 = null;
                        }
                        if(baseobject1 == null)
                        {

                                Log.customer.debug("%s ::: The Reference Line Number references a not-existant line", "CatCSVValidateRefLineNumber");
                            errorCode = 3;
                        } else
                        if(isAdditionalChargeLine((ProcureLineItem)baseobject1.getDottedFieldValue("OrderLineItem")))
                        {

                                Log.customer.debug("%s ::: The Reference Line Number references a not-mat line", "CatCSVValidateRefLineNumber");
                            errorCode = 2;
                        }
                    }
                }
            } else
            {

                    Log.customer.debug("%s ::: The Reference Line Number is not populated, hence invalid", "CatCSVValidateRefLineNumber");
                errorCode = 5;
            }
        } else
        {
            int j = 0;
            if((baseobject instanceof InvoiceReconciliationLineItem) || (baseobject instanceof InvoiceLineItem))
                j = ((Integer)baseobject.getDottedFieldValue("NumberInCollection")).intValue();
            else
                j = ((Integer)baseobject.getDottedFieldValue("InvoiceLineNumber")).intValue();
            if(i != j && !s.equals("007"))
            {

                    Log.customer.debug("CatCSVValidateRefLineNumber :: Assigning valid Ref Number to Material Line");
                baseobject.setDottedFieldValue("ReferenceLineNumber", new Integer(j));
                errorCode = 6;
            }
        }
    }

    private void validateReferenceLineMA(BaseObject baseobject, BaseVector basevector, ProcureLineItem procurelineitem, Integer integer, int i, ClusterRoot clusterroot, String s)
    {

            Log.customer.debug("%s ::: Entering the validateReferenceLineMA method", "CatCSVValidateRefLineNumber");
        Object obj = null;
        Contract masteragreement = ((ContractLineItem)procurelineitem).getMasterAgreement();
        if(masteragreement.getTermTypeString().equals("Item"))
        {

                Log.customer.debug("%s ::: The pli belongs to Item Level MA", "CatCSVValidateRefLineNumber");
            if(isAdditionalChargeLine(procurelineitem))
            {
                if(integer != null)
                {
                    if(i == 0)
                    {
                        if(!s.equals("002") && !s.equals("003") && !s.equals("096"))
                        {

                                Log.customer.debug("%s ::: Non Tax line can not have reference line number of 0", "CatCSVValidateRefLineNumber");
                            errorCode = 1;
                        }
                    } else
                    {
                        if(s.equals("002") || s.equals("003") || s.equals("096"))
                        {

                                Log.customer.debug("%s ::: Tax line does not have reference line number of 0", "CatCSVValidateRefLineNumber");
                            errorCode = 4;
                        }
                        if(basevector != null)
                        {
                            BaseObject baseobject1;
                            try
                            {
                                baseobject1 = (BaseObject)basevector.get(i - 1);
                            }
                            catch(Exception exception)
                            {

                                    Log.customer.debug("%s ::: Caught the invalid array exception: %s", "CatCSVValidateRefLineNumber", exception.toString());
                                baseobject1 = null;
                            }
                            if(baseobject1 == null)
                            {

                                    Log.customer.debug("%s ::: The Reference Line Number references a not-existant line", "CatCSVValidateRefLineNumber");
                                errorCode = 3;
                            } else
                            if(isAdditionalChargeLine((ProcureLineItem)baseobject1.getDottedFieldValue("MALineItem")))
                            {

                                    Log.customer.debug("%s ::: The Reference Line Number references a not-mat line", "CatCSVValidateRefLineNumber");
                                errorCode = 2;
                            }
                        }
                    }
                } else
                {

                        Log.customer.debug("%s ::: The Reference Line Number is not populated, hence invalid", "CatCSVValidateRefLineNumber");
                    errorCode = 5;
                }
            } else
            {
                int j = 0;
                if((baseobject instanceof InvoiceReconciliationLineItem) || (baseobject instanceof InvoiceLineItem))
                    j = ((Integer)baseobject.getDottedFieldValue("NumberInCollection")).intValue();
                else
                    j = ((Integer)baseobject.getDottedFieldValue("InvoiceLineNumber")).intValue();
                if(i != j && !s.equals("007"))
                {

                        Log.customer.debug("%s :: Assigning valid Ref Number to Material Line", "CatCSVValidateRefLineNumber");
                    baseobject.setDottedFieldValue("ReferenceLineNumber", new Integer(j));
                    errorCode = 6;
                }
            }
        } else
        {

                Log.customer.debug("%s ::: The pli belongs to Supplier or Category Level MA", "CatCSVValidateRefLineNumber");
            String s1 = (String)baseobject.getDottedFieldValue("Description.CAPSChargeCodeID");
            if(s1 == null)
                s1 = "001";
            if(s1.equals("002") || s1.equals("003") || s1.equals("096"))
            {
                if(integer != null)
                {
                    if(i != 0)
                        errorCode = 4;
                } else
                {

                        Log.customer.debug("%s ::: The Reference Line Number is not populated, hence invalid", "CatCSVValidateRefLineNumber");
                    errorCode = 5;
                }
            } else
            if(s1.equals("001"))
            {
                int k = 0;
                if((baseobject instanceof InvoiceReconciliationLineItem) || (baseobject instanceof InvoiceLineItem))
                    k = ((Integer)baseobject.getDottedFieldValue("NumberInCollection")).intValue();
                else
                    k = ((Integer)baseobject.getDottedFieldValue("InvoiceLineNumber")).intValue();
                if(i != k && !s.equals("007"))
                {
                    Log.customer.debug("%s :: Assigning valid Ref Number to Material Line", "CatCSVValidateRefLineNumber");
                    baseobject.setDottedFieldValue("ReferenceLineNumber", new Integer(k));
                    errorCode = 6;
                }
            } else
            if(integer != null)
            {
                if(i == 0)
                {
                    errorCode = 1;
                } else
                {
                    BaseObject baseobject2;
                    try
                    {
                        baseobject2 = (BaseObject)basevector.get(i - 1);
                    }
                    catch(Exception exception1)
                    {

                            Log.customer.debug("%s ::: Caught the invalid array exception: %s", "CatCSVValidateRefLineNumber", exception1.toString());
                        baseobject2 = null;
                    }
                    if(baseobject2 == null)
                    {

                            Log.customer.debug("%s ::: The Reference Line Number references a not-existant line", "CatCSVValidateRefLineNumber");
                        errorCode = 3;
                    } else
                    if(isAdditionalChargeLine((ProcureLineItem)baseobject2.getDottedFieldValue("MALineItem")))
                    {

                            Log.customer.debug("%s ::: The Reference Line Number references a not-mat line", "CatCSVValidateRefLineNumber");
                        errorCode = 2;
                    }
                }
            } else
            {

                    Log.customer.debug("%s ::: The Reference Line Number is not populated, hence invalid", "CatCSVValidateRefLineNumber");
                errorCode = 5;
            }
        }
    }

    private void validateReferenceLineFromPLT(BaseObject baseobject, BaseVector basevector, Integer integer, int i, ClusterRoot clusterroot, String s)
    {

            Log.customer.debug("%s ::: Entering the validateReferenceLineFromPLT method", "CatCSVValidateRefLineNumber");
        Object obj = null;
        ProcureLineType procurelinetype = (ProcureLineType)baseobject.getDottedFieldValue("LineType");
        // shaila: issue 739 added the null check to LineType
        if (procurelinetype != null)
        {

        int j = procurelinetype.getCategory();
		// No check for special charge's reference line number - Vikram
		if (j == 16)
		{
			errorCode = 0;
		}
		else
        //if(j == 16 || j == 2 || j == 1 && !"001".equals(baseobject.getDottedFieldValue("CapsChargeCode.UniqueName")))
		if(j == 2 || j == 1 && !"001".equals(baseobject.getDottedFieldValue("CapsChargeCode.UniqueName")))
            if(integer != null)
            {
                if(i == 0)
                {
                    if(j != 2)
                        errorCode = 1;
                } else
                if(j == 2)
                {
                    errorCode = 4;
                } else
                {
                    BaseObject baseobject1;
                    try
                    {
                        baseobject1 = (BaseObject)basevector.get(i - 1);
                    }
                    catch(Exception exception)
                    {

                            Log.customer.debug("%s ::: Caught the invalid array exception: %s", "CatCSVValidateRefLineNumber", exception.toString());
                        baseobject1 = null;
                    }
                    if(baseobject1 == null)
                    {

                            Log.customer.debug("%s ::: The Reference Line Number references a not-existant line", "CatCSVValidateRefLineNumber");
                        errorCode = 3;
                    } else
                    {
                        ProcureLineType procurelinetype1 = (ProcureLineType)baseobject1.getDottedFieldValue("LineType");
                        ClusterRoot clusterroot1 = (ClusterRoot)baseobject1.getDottedFieldValue("CapsChargeCode");
                        String s1 = null;
//Shaila : Issue 739 Added null check to LineType
                        if((clusterroot1 != null) && (procurelinetype1 != null))
                            s1 = clusterroot1.getUniqueName();
                        int l = procurelinetype1.getCategory();
                        if(l != 1 || !"001".equals(s1))
                            errorCode = 2;
                    }
                }
            } else
            {

                    Log.customer.debug("%s ::: The Reference Line Number is not populated, hence invalid", "CatCSVValidateRefLineNumber");
                errorCode = 5;
            }
        if(j == 1 && "001".equals(baseobject.getDottedFieldValue("CapsChargeCode.UniqueName")))
        {
            int k = 0;
            if((baseobject instanceof InvoiceReconciliationLineItem) || (baseobject instanceof InvoiceLineItem))
                k = ((Integer)baseobject.getDottedFieldValue("NumberInCollection")).intValue();
            else
                k = ((Integer)baseobject.getDottedFieldValue("InvoiceLineNumber")).intValue();
            if(i != k && !s.equals("007"))
            {
                Log.customer.debug("CatCSVValidateRefLineNumber :: Assigning valid Ref Number to Material Line");
                baseobject.setDottedFieldValue("ReferenceLineNumber", new Integer(k));
                errorCode = 6;
            }
        }
    }
}

    private static boolean isAdditionalChargeLine(ProcureLineItem procurelineitem)
    {

		Log.customer.debug("procurelineitem  is --- Sudheer" + procurelineitem , "CatCSVValidateRefLineNumber");

            Log.customer.debug("%s ::: Entering the %s method implementation", "CatCSVValidateRefLineNumber", "isAdditionalChargeLine");
        boolean flag = false;
        if(procurelineitem != null && procurelineitem.getIsInternalPartId())
        {
            String s = (String)procurelineitem.getDescription().getDottedFieldValue("CAPSChargeCode.UniqueName");

                Log.customer.debug("%s ::: The chargecode from the pli is: %s", "CatCSVValidateRefLineNumber", s);
              if(s != null && !s.equals("001"))
            //if(s != null && !"001".equals(s))
                flag = true;
        }

            Log.customer.debug("%s ::: The procure line is an additional charge: " + flag, "CatCSVValidateRefLineNumber");
        return flag;
    }

    public CatCSVValidateRefLineNumber()
    {
        errorCode = 0;
    }

    private static final String ClassName = "CatCSVValidateRefLineNumber";
    private static String ErrorMsg1 = "Reference Number cannot be set to \"0\"";
    private static String ErrorMsg2 = "Reference Number cannot be set to reference a non material line";
    private static String ErrorMsg3 = "Reference Number cannot be set to reference a non existant line";
    private static String ErrorMsg4 = "Only Header Level Taxes Allowed: Tax Line cannot refer to another line";
    private static String ErrorMsg5 = "Reference Number cannot be null, needs to be populated";
    private static String WarnMsg1 = "Material Item should always refer to itself, hence will reset the number";
    private int errorCode;

}
