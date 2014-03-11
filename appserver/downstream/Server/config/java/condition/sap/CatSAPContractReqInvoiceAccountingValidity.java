/*****************************************************************************************************************************************************
 Change History
 Issue#		Change By		Change Date			Description
========================================================================================================================================================
	325  IBM_AMS_Vikram		Nov-29-2012		Changing java's name from CatSAPContractAccountingValidity to CatSAPContractReqInvoiceAccountingValidity
	325  IBM_AMS_Vikram		Nov-29-2012     MACH1 5.0 change: Making cost center field optional for Req and IR for Acct type S
*****************************************************************************************************************************************************/


package config.java.condition.sap;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ValueInfo;
import ariba.contract.core.ContractCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import ariba.purchasing.core.ReqLineItem;
import ariba.base.core.ClusterRoot;
import ariba.invoicing.core.InvoiceReconciliationLineItem;

public class CatSAPContractReqInvoiceAccountingValidity extends Condition{



        private static final String classname = "CatSAPContractReqInvoiceAccountingValidity";
        public static String LineItemParam = "LineItem";
        private static final ValueInfo valueInfo = new ValueInfo(0);
        private static final ValueInfo parameterInfo[] = {new ValueInfo(LineItemParam, 0, "ariba.approvable.core.LineItem")};

   public boolean evaluate (Object value, PropertyTable params)
        throws ConditionEvaluationException

        {
                Log.customer.debug(" Started1");
                Log.customer.debug(" %s : value %s " ,classname, value);

                LineItem  li =(LineItem)params.getPropertyForKey(LineItemParam);
                Log.customer.debug(" %s : li %s " ,classname , li);
                        if(li==null){
                                return false;
                                }
        if( li instanceof ContractCoreApprovableLineItem)
                        {
                        LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
                        Log.customer.debug(" %s : (MasterAgreement line) lic %s " ,classname , lic);
                        /**
                        Integer  releasetype = (Integer)lic.getDottedFieldValue("ReleaseType");
                    Log.customer.debug(" %s : lic %s " ,classname , releasetype);
                        int value1 = releasetype.intValue();
                        Log.customer.debug(" %s : lic %s " ,classname , value);
                           if ( value1 ==1)
                           {
                                   return true;
                           }

                        Log.customer.debug(" %s : return false " ,classname);
                        return false;
                        **/
                        // Added by Majid - Independent of Release type - Make field as optional
                        return true;
                        }
		//Vikram: Issue 325 code starts. To make cost center field optional for Acct type S
        else if ((li instanceof ReqLineItem) || (li instanceof InvoiceReconciliationLineItem))
                {
						LineItemCollection lic = (LineItemCollection)li.getLineItemCollection();
                        Log.customer.debug(" %s : (Invoice Reconciliation/Requisition line) lic %s " ,classname , lic);

                        String acccat = null;

                        if (li != null){

                                        ClusterRoot acccategory =(ClusterRoot)li.getDottedFieldValue("AccountCategory");

                                        if(acccategory!=null){

                                                Log.customer.debug(" %s *** Finding the category",classname);

                                                acccat = (String)acccategory.getDottedFieldValue("UniqueName");

                                                Log.customer.debug("CatSAPCostCenterInternalOrderValidity *** isCostCenterAndInternalOrderRequired :: %s ",acccat);
                                        }


                        }

                        if(acccat != null && acccat.equalsIgnoreCase("S")){
                                        return true;
                        }

                        else
                                        return false;
                }
				//Vikram: Issue 325 code ends. To make cost center field optional for Acct type S
        else{
                Log.customer.debug(" %s : return false1 " ,classname);
                   return false;
        }
                        }
                protected ValueInfo[] getParameterInfo() {
                return parameterInfo;
                }
}
