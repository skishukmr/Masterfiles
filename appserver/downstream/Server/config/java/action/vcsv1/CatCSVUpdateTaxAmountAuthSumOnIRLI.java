package config.java.action.vcsv1;

import java.math.BigDecimal;
import java.util.List;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
/*
 * AUL : Changed all statements with debugon
 */
public class CatCSVUpdateTaxAmountAuthSumOnIRLI extends Action
{

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {
        Log.customer.debug("%s ::: Entering the fire method", "CatCSVUpdateTaxAmountAuthSumOnIRLI");
        if(valuesource instanceof InvoiceReconciliationLineItem)
        {
            Log.customer.debug("%s ::: The object passed in %s is an irli", "CatCSVUpdateTaxAmountAuthSumOnIRLI", valuesource);
            InvoiceReconciliationLineItem invoicereconciliationlineitem = (InvoiceReconciliationLineItem)valuesource;
            InvoiceReconciliation invoicereconciliation = invoicereconciliationlineitem.getInvoiceReconciliation();
            List list = (List)invoicereconciliation.getFieldValue("LineItems");
            BigDecimal bigdecimal = new BigDecimal("0.0000");
            for(int i = 0; i < list.size(); i++)
            {
                InvoiceReconciliationLineItem invoicereconciliationlineitem1 = (InvoiceReconciliationLineItem)list.get(i);
                if(invoicereconciliationlineitem1.getFieldValue("TaxAmountAuth") != null)
                {
                    Log.customer.debug("%s ::: The auth. tax amount is %s", "CatCSVUpdateTaxAmountAuthSumOnIRLI", invoicereconciliationlineitem1.getFieldValue("TaxAmountAuth"));
                    bigdecimal = bigdecimal.add((BigDecimal)invoicereconciliationlineitem1.getFieldValue("TaxAmountAuth"));
                }
            }

            Log.customer.debug("%s ::: Setting the TaxAmountAuthSum to %s", "CatCSVUpdateTaxAmountAuthSumOnIRLI", bigdecimal);
            Money money = new Money(bigdecimal, Currency.getBaseCurrency());
            Money money1 = money.convertToCurrency(invoicereconciliation.getTotalCost().getCurrency());
            invoicereconciliation.setDottedFieldValue("TaxAmountAuthSum", money1);

                Log.customer.debug("%s ::: Successfully set the TaxAmountAuthSum", "CatCSVUpdateTaxAmountAuthSumOnIRLI");
                Log.customer.debug("%s ::: The authorised amount is %s", "CatCSVUpdateTaxAmountAuthSumOnIRLI", bigdecimal);
                Log.customer.debug("%s ::: The authorised amount in Invoice Currency is %s", "CatCSVUpdateTaxAmountAuthSumOnIRLI", money1.getAmount().toString());

            InvoiceReconciliationLineItem invoicereconciliationlineitem2 = null;
            for(int j = 0; j < list.size(); j++)
            {
                ProcureLineType procurelinetype = ((InvoiceReconciliationLineItem)list.get(j)).getLineType();
                if(procurelinetype != null)
                {
                    if(procurelinetype.getCategory() == 2)
                        invoicereconciliationlineitem2 = (InvoiceReconciliationLineItem)list.get(j);
                    continue;
                }
                    Log.customer.debug("%s ::: Encountered a null Procure Line Type", "CatCSVUpdateTaxAmountAuthSumOnIRLI");
            }

            if(invoicereconciliationlineitem2 != null)
            {
                ariba.base.core.BaseVector basevector = invoicereconciliationlineitem2.getExceptions();
                for(int k = 0; k < basevector.size(); k++)
                    if(((InvoiceException)basevector.get(k)).getType().getUniqueName().equals("OverTaxVariance"))
                        ((InvoiceException)basevector.get(k)).setDottedFieldValue("Fields.OldValue.Amount", money1.getAmount());

            }
        }
    }

    public CatCSVUpdateTaxAmountAuthSumOnIRLI()
    {
    }

    private static final String ClassName = "CatCSVUpdateTaxAmountAuthSumOnIRLI";
}
