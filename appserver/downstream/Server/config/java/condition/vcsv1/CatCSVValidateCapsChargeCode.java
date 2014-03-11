/******************************************************************************

	Change Author: 	Dibya Prakash Sahoo
	Date Modified:  23/07/2008 and 30/07/08
	Description: 	Added logic to check null pointer exception
					issue--838

    Change Author: 	Sudheer Kumar Jain
	Date Modified:  16/09/2008 and 18/09/08
	Description: 	Fixed capschargecode
	                Issue -- 854
******************************************************************************/




package config.java.condition.vcsv1;

import ariba.base.core.BaseObject;
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

public class CatCSVValidateCapsChargeCode extends Condition
{

    public boolean evaluate(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {
        Log.customer.debug("%s ::: Entering the evaluate method", "CatCSVValidateCapsChargeCode");
        return isValidChargeCode((BaseObject)obj);
    }

    public ConditionResult evaluateAndExplain(Object obj, PropertyTable propertytable)
        throws ConditionEvaluationException
    {
        Log.customer.debug("%s ::: Entering the evaluateAndExplain method", "CatCSVValidateCapsChargeCode");
        if(!evaluate(obj, propertytable))
        {
            Log.customer.debug("%s ::: Returning Error Message: %s", "CatCSVValidateCapsChargeCode", ErrorMsg);
            return new ConditionResult(ErrorMsg);
        } else
        {
            return null;
        }
    }

    public boolean isValidChargeCode(BaseObject baseobject)
    {
        Log.customer.debug("%s ::: Entering the isValidChargeCode method", "CatCSVValidateCapsChargeCode");
        Log.customer.debug("%s ::: The ClassName anf TypeName of the passed in li are: %s %s", "CatCSVValidateCapsChargeCode", baseobject.getClassName(), baseobject.getTypeName());

        boolean flag = true;
        ClusterRoot clusterroot = (ClusterRoot)baseobject.getDottedFieldValue("CapsChargeCode");

        //Code Added for issue-838

        if (clusterroot == null)
        {
			Log.customer.debug("Cluster Root for CapsCharge Code is null");
        	return flag;
		}

        String s = null;
        if(clusterroot != null)
            s = clusterroot.getUniqueName();

        Log.customer.debug("%s ::: CapsCC on the passed in line item is %s", "CatCSVValidateCapsChargeCode", s);
        ProcureLineItem procurelineitem = (ProcureLineItem)baseobject.getDottedFieldValue("OrderLineItem");

        if(procurelineitem == null && ((baseobject instanceof InvoiceLineItem) || (baseobject instanceof InvoiceReconciliationLineItem)))
        {
            StatementCoreApprovableLineItem invoicecoreapprovablelineitem = (StatementCoreApprovableLineItem)baseobject;
            procurelineitem = (ProcureLineItem)baseobject.getDottedFieldValue("MALineItem");
        }
        if(procurelineitem != null)
        {
            if(procurelineitem instanceof POLineItem)
            {
            	Log.customer.debug("%s ::: The pli is instance of POLineItem", "CatCSVValidateCapsChargeCode");
                flag = isValidChargeCodePO(baseobject, procurelineitem, clusterroot, s);
            } else
            if(procurelineitem instanceof ContractLineItem)
            {
            	Log.customer.debug("%s ::: The pli is instance of MALineItem", "CatCSVValidateCapsChargeCode");
                flag = isValidChargeCodeMA(baseobject, procurelineitem, clusterroot, s);
            }
        } else
        {
            Log.customer.debug("%s ::: The procure line linked to the passed line is null", "CatCSVValidateCapsChargeCode");
            flag = isValidChargeCodeFromPLT(baseobject, clusterroot, s);
        }
        Log.customer.debug("%s ::: The evaluated validity value passed back is " + flag, "CatCSVValidateCapsChargeCode");
        return flag;
    }

    private boolean isAdditionalChargeLine(ProcureLineItem procurelineitem)
    {
    	Log.customer.debug("%s ::: Entering the %s method implementation", "CatCSVValidateCapsChargeCode", "isAdditionalChargeLine");
        boolean flag = false;
        if(procurelineitem != null && procurelineitem.getIsInternalPartId())
        {
			//Code Added For Issue--838
			// Code Started by sudheer
			BaseObject baseobject=(BaseObject)procurelineitem;
			Log.customer.debug(" Sudheer Logs----procurelineitem before null " + procurelineitem);
			ariba.procure.core.LineItemProductDescription  lipdsud = (ariba.procure.core.LineItemProductDescription )procurelineitem.getDescription();
			ClusterRoot capscode = (ClusterRoot)lipdsud.getFieldValue("CAPSChargeCode");
            Log.customer.debug(" Sudheer Logs----Capschargecode before null " + capscode);
			if (capscode == null)
			{
				Log.customer.debug("Cluster Root for CapsCharge Code is null");
			    return flag;
			}

           // Code ended by sudheer
            String s = (String)procurelineitem.getDescription().getDottedFieldValue("CAPSChargeCode.UniqueName");
            Log.customer.debug("%s ::: The chargecode from the pli is: %s", "CatCSVValidateCapsChargeCode", s);
            if(s != null && !"001".equals(s))
                flag = true;
        }
        Log.customer.debug("%s ::: The order line is an additional charge: " + flag, "CatCSVValidateCapsChargeCode");
        return flag;
    }

    private boolean isValidChargeCodePO(BaseObject baseobject, ProcureLineItem procurelineitem, ClusterRoot clusterroot, String s)
    {
        boolean flag = true;
        if(!isAdditionalChargeLine(procurelineitem))
        {
            Log.customer.debug("%s ::: The pli is not an additional charge line", "CatCSVValidateCapsChargeCode");
            if(clusterroot != null)
            {
                if(!s.equals(materialCodes[0]) && !s.equals(specialCCodes[0]) && !s.equals(specialCCodes[1] ) )
                {
                    Log.customer.debug("%s ::: The Caps Charge Code is not 001 for material line, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }

            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
        } else
        {
        	Log.customer.debug("%s ::: The pli is an additional charge line", "CatCSVValidateCapsChargeCode");
            if(clusterroot != null)
            {
                if(s.equals(materialCodes[0]))
                {

                	Log.customer.debug("%s ::: The Caps Charge Code is 001 for non-material line, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                } else
                {
                    String s1 = (String)procurelineitem.getDescription().getDottedFieldValue("CAPSChargeCodeID");

                    //Code Added for issue-838

                    if (s1 == null)
						{
							Log.customer.debug("Cluster Root for CapsCharge Code is null");
							return flag;
						}

                    if(!s.equals(s1))
                    {
                        Log.customer.debug("%s ::: The Caps Charge Code is for AC but doesn't match the one on PO", "CatCSVValidateCapsChargeCode");
                        flag = false;
                    }
                }
            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
        }
        return flag;
    }

    private boolean isValidChargeCodeMA(BaseObject baseobject, ProcureLineItem procurelineitem, ClusterRoot clusterroot, String s)
    {
        boolean flag = true;
        Contract masteragreement = ((ContractLineItem)procurelineitem).getMasterAgreement();
        if(masteragreement.getTermTypeString().equals("Item"))
        {
            Log.customer.debug("%s ::: The pli belongs to Item Level MA", "CatCSVValidateCapsChargeCode");
            if(!isAdditionalChargeLine(procurelineitem))
            {
                Log.customer.debug("%s ::: The pli is not an additional charge line", "CatCSVValidateCapsChargeCode");
                if(clusterroot != null)
                {
                    if(!s.equals(materialCodes[0]) && (!s.equals(specialCCodes[0]) && !s.equals(specialCCodes[1]) )  )
                    {
                        Log.customer.debug("%s ::: The Caps Charge Code is not 001 for material line, hence invalid", "CatCSVValidateCapsChargeCode");
                        flag = false;
                    }
                } else
                {
                    Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            } else
            {
                Log.customer.debug("%s ::: The pli is an additional charge line", "CatCSVValidateCapsChargeCode");
                if(clusterroot != null)
                {
                    if(s.equals(materialCodes[0]))
                    {
                        Log.customer.debug("%s ::: The Caps Charge Code is 001 for non-material line, hence invalid", "CatCSVValidateCapsChargeCode");
                        flag = false;
                    } else
                    {
                        String s1 = (String)procurelineitem.getDescription().getDottedFieldValue("CAPSChargeCodeID");

                        //Code Added for issue-838

						if (s1 == null)
						{
							Log.customer.debug("Cluster Root for CapsCharge Code is null");
							return true;
						}

                        if(!s.equals(s1))
                        {
                            Log.customer.debug("%s ::: The Caps Charge Code is for AC but doesn't match the one on MA", "CatCSVValidateCapsChargeCode");
                            flag = false;
                        }
                    }
                } else
                {
                    Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            }
        } else
        {
            Log.customer.debug("%s ::: The pli belongs to Supplier or Category Level MA", "CatCSVValidateCapsChargeCode");
            if(clusterroot != null)
            {
                String s2 = (String)baseobject.getDottedFieldValue("Description.CAPSChargeCodeID");
                if(s2 == null)
                    s2 = "001";
                //if(!s.equals(s2) && (!s.equals(specialCCodes[0]) || !s.equals(specialCCodes[1])  )  )
                if(!s.equals(s2)  && !s.equals(specialCCodes[0]) && !s.equals(specialCCodes[1] ) )
                {
                    Log.customer.debug("%s ::: The Caps Charge Code doesn't match the CAPSChargeCodeID on LID, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
        }
        return flag;
    }

    private boolean isValidChargeCodeFromPLT(BaseObject baseobject, ClusterRoot clusterroot, String s)
    {
        boolean flag = true;
        //Code Added For NP Exception-- issue--838
        try
        {
        	ProcureLineType procurelinetype = (ProcureLineType)baseobject.getDottedFieldValue("LineType");
        	Log.customer.debug("Procure Linetype is"+procurelinetype);
        	if(procurelinetype == null)
        	{
				Log.customer.debug("Procure Linetype is null");
        		return flag;
			}
        	int i = procurelinetype.getCategory();
        	Log.customer.debug("Procure LinetypeCategory is"+i);

		//Code Ended For NP Exception
        if(i == 16)
            if(clusterroot != null)
            {
                if(!s.equals(specialCCodes[0]) && !s.equals(specialCCodes[1]) )
                {
                    Log.customer.debug("%s ::: The ProcureLineType is SpecialChargeCategory and CCCode is not 007, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
        if(i == 2)
            if(clusterroot != null)
            {
                if(procurelinetype.getUniqueName().equals("SalesTaxCharge") && !s.equals(taxCodes[0]) || procurelinetype.getUniqueName().equals("ServiceUseTax") && !s.equals(taxCodes[1]) || procurelinetype.getUniqueName().equals("VATCharge") && !s.equals(taxCodes[2]))
                {
                    Log.customer.debug("%s ::: The ProcureLineType is TaxChargeCategory and CCCode is not 002,003 or 096, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
        if(i == 1)
            if(clusterroot != null)
            {
                if(!s.equals(materialCodes[0]))
                {
                    Log.customer.debug("%s ::: The ProcureLineType is LineItemCategory and CCCode is not 001, hence invalid", "CatCSVValidateCapsChargeCode");
                    flag = false;
                }
            } else
            {
                Log.customer.debug("%s ::: The Caps Charge Code is not populated, hence invalid", "CatCSVValidateCapsChargeCode");
                flag = false;
            }
            }

					catch(Exception e)
					{
						Log.customer.debug("In Exceptin:Procure Linetype is null");
					}
        return flag;
    }

    public CatCSVValidateCapsChargeCode()
    {
    }

    private static final String ClassName = "CatCSVValidateCapsChargeCode";
    private static String ErrorMsg = "Selected CAPS Charge Code is invalid";
    private static String materialCodes[] = {
        "001"
    };
    private static String taxCodes[] = {
        "002", "003", "096"
    };
    private static String specialCCodes[] = {
        "007", "019"
    };

}
