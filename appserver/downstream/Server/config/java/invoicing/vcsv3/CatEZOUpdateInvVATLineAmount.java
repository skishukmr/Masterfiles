
package config.java.invoicing.vcsv3;

import java.util.List;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementCoreApprovable;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**
 * @author KS.
 * Updates Amount for first VAT line found in LineItems using TaxAmount for all non-tax line items.
 */

public class CatEZOUpdateInvVATLineAmount extends Action {

    private static final String ClassName = "CatEZOUpdateInvVATLineAmount";
    private static final String vatCharge = "VATCharge";

    public void fire(ValueSource valuesource, PropertyTable params) throws ActionExecutionException {

    	if (valuesource instanceof StatementCoreApprovableLineItem) {
    		StatementCoreApprovableLineItem invLine = (StatementCoreApprovableLineItem)valuesource;
    		StatementCoreApprovable invoice = (StatementCoreApprovable)invLine.getLineItemCollection();

    		List lines = (List)invoice.getFieldValue("LineItems");
    		if (!ListUtil.nullOrEmptyList(lines))
    			updateVATLineAmount(invoice, lines);
    	}
    }

    public static void updateVATLineAmount(StatementCoreApprovable invoice, List lines) {

        Money totalVAT = null;
        StatementCoreApprovableLineItem vatLine = null;

    	int size = lines.size();
        for (int i=0; i<size; i++) {
            //if (Log.customer.debugOn)
                Log.customer.debug("CatEZOUpdateVATLineAmount *** Processing Line# " + (i+1));
            StatementCoreApprovableLineItem lineItem = (StatementCoreApprovableLineItem)lines.get(i);
            ProcureLineType type = (ProcureLineType)lineItem.getFieldValue("LineType");
            if (type == null || type.getCategory() != ProcureLineType.TaxChargeCategory) {
                Money taxAmt = (Money)lineItem.getFieldValue("TaxAmount");
                if (taxAmt != null) {
                    totalVAT = totalVAT == null ? new Money(taxAmt) : totalVAT.add(taxAmt);
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s *** totalVAT.getAmount(): %s",ClassName,totalVAT.getAmount());
                }
            }
            else if (type.getUniqueName().equals(vatCharge)) {
                vatLine = vatLine == null ? lineItem : vatLine;
                //if (Log.customer.debugOn)
                    Log.customer.debug("%s *** Found VAT Line!",ClassName);
            }
        }
        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** totalVAT: %s, vatLine: %s",ClassName,totalVAT,vatLine);
        if (vatLine != null) {
            if (totalVAT == null)
                totalVAT = new Money(Constants.ZeroBigDecimal, Currency.getDefaultCurrency(invoice.getPartition()));

            vatLine.setAmount(totalVAT);
            //if (Log.customer.debugOn)
                Log.customer.debug("%s *** Amount (after update): %s",ClassName, vatLine.getAmount());
        }
    }
}
