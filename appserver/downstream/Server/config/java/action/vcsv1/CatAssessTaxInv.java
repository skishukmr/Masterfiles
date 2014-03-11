// Vikram   11/08/2011 Removing the auto-accept functionality on OverTaxVariance upon clicking AccessTaxButton
// Manoj.R  28/02/2012  WI 262 - Checking for OverTaxVariance if TaxCalculationFailed exception exist

package config.java.action.vcsv1;

import java.util.List;
import java.lang.Integer;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.tax.CatTaxUtil;
import ariba.util.core.ResourceService;

/*
 * AUL : Remove all debugon statements.
 */

public class CatAssessTaxInv extends Action
{

    public CatAssessTaxInv()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        Log.customer.debug("%s ::: The Tax trigger fired as expected", "CatAssessTaxInv");
        InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)valuesource;
        String s = "";

        Log.customer.debug("%s ::: Reset the AssessTaxMessage to null value before tax call", "CatAssessTaxInv");
        invoicereconciliation.setFieldValue("AssessTaxMessage", null);
        s = CatTaxUtil.evaluateTax(invoicereconciliation);
        if(s != null)
        {
            Log.customer.debug("%s ::: Required fields are missing hence setting AssessTaxMessage", "CatAssessTaxInv");
            invoicereconciliation.setFieldValue("AssessTaxMessage", s.toString());
        } else
        {
            Log.customer.debug("%s ::: Tax call successful, setting the required fields", "CatAssessTaxInv");
            invoicereconciliation.setFieldValue("TaxOverrideFlag", new Boolean(false));
            invoicereconciliation.setFieldValue("taxCallNotFailed", new Boolean(true));
            invoicereconciliation.setFieldValue("AssessTaxMessage", null);
            addExceptionIfNoneExists(invoicereconciliation);
        }
    }

    public boolean addExceptionIfNoneExists(InvoiceReconciliation invoicereconciliation)
    {
        boolean flag = false;
        List list = (List)invoicereconciliation.getFieldValue("LineItems");
        InvoiceReconciliationLineItem invoicereconciliationlineitem = null;
        Object obj = null;
        for(int i = 0; i < list.size(); i++)
        {
            InvoiceReconciliationLineItem invoicereconciliationlineitem1 = (InvoiceReconciliationLineItem)list.get(i);
            ProcureLineType procurelinetype = invoicereconciliationlineitem1.getLineType();

            Log.customer.debug("%s ::: Procure Line Type is: %s", "CatAssessTaxInv", procurelinetype.getName().toString());
            if(procurelinetype != null && procurelinetype.getCategory() == 2)
                invoicereconciliationlineitem = (InvoiceReconciliationLineItem)list.get(i);
         }

        if(invoicereconciliationlineitem != null)
        {
            Log.customer.debug("%s ::: Inside IRLineitemIf", "CatAssessTaxInv");
            BaseVector basevector = invoicereconciliationlineitem.getExceptions();
            for(int j = 0; j < basevector.size(); j++)
            {
                Log.customer.debug("%s ::: Inside LineItemExceptions", "CatAssessTaxInv");
                InvoiceException invoiceexception = (InvoiceException)basevector.get(j);
                String s = invoiceexception.getType().getUniqueName();
				int state1 = invoiceexception.getState();
				// added state1 != 4 condition meaning, if the OverTaxVariance is disputed, then do not auto accept the variance
                if("OverTaxVariance".equals(s) && state1 != 4)
                {
                    invoiceexception.setState(2);
                    invoiceexception.setReconciledBy(User.getAribaSystemUser(invoiceexception.getPartition()));
                    flag = true;
                }
            }

            /*
				Changed by : Arasan Rajendren
				Changed on : 05/27/2011
				Changes	   : Comment -  Removing the exception as it causes NullPointerException
			*/

            /*if(flag)
            {
            	Log.customer.debug("CatCSVInvoiceReconciliationEngine ::: Removing exception type: %s", "OverTaxVariance");
                basevector.remove(0);
                Log.customer.debug("CatCSVInvoiceReconciliationEngine ::: Successfully removed exception type: %s", "OverTaxVariance");
            }
            CatTaxUtil.addExceptions(invoicereconciliation);
            */
        }
          // Checking for OverTaxVariance if TaxCalculationFailed exception exists(262)

          addOverTaxVariance(invoicereconciliation);

        // End 262

        return !flag;
    }

    // Checking for OverTaxVariance if TaxCalculationFailed exception exists(262)
    private void addOverTaxVariance (InvoiceReconciliation invoicereconciliation)
    {
            String catTaxDesc = ResourceService.getString("cat.java.vcsv1", "catTaxDesc");
            BaseVector headerExceptions = invoicereconciliation.getExceptions();
	    Log.customer.debug("HeaderExceptions = %s", headerExceptions);
			for(int k = 0; k < headerExceptions.size(); k++)
			{
	                 Log.customer.debug("%s ::: Inside HeaderExcFor", "CatAssessTaxInv");
			 InvoiceException invoiceException = (InvoiceException)headerExceptions.get(k);
			 String excType = invoiceException.getType().getUniqueName();
                         if ( excType != null)
                         {
	                    Log.customer.debug("HeaderExceptionName = %s", excType );
                            try
                            {
			      String excDesc = invoiceException.getType().getDescription().getPrimaryString();
	                      Log.customer.debug("HeaderExceptionDescription = %s", excDesc);
			      if ( excType.equals("CATTaxCalculationFailed"))
			      {
	                        Log.customer.debug("%s ::: CatTaxCalcFailedIf", "CatAssessTaxInv");
			        if ( excDesc.equals(catTaxDesc))
			        {
	                          Log.customer.debug("%s ::: BeforeAddingException", "CatAssessTaxInv");
			          CatTaxUtil.addExceptions(invoicereconciliation);
			        }
			      }
                            }
                            catch (NullPointerException npe)
                            {
                             Log.customer.debug("Null Pointer Exception Occured in addOverTaxVariance as ExceptionDescription is null for class %s",ClassName);
                            }
                          }
                        }
    }
    // End 262

    private static final String ClassName = "CatAssessTaxInv";
}
