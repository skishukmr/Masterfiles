/*
Changes:
Chandra    16-nov-07    Since the line item level Assesstax button is added for MAR, checking to get the plic from the line

*/
package config.java.action.vcsv1;

import java.util.ArrayList;
import java.util.List;

import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import config.java.common.TaxInputObject;
import config.java.common.TaxOutputObject;
import config.java.tax.vcsv1.CatTaxCollector;

/**
 *
 * AUL : Changed MARLineItem to ContractRequestLineItem
 *
 */

public class CatAssessTax extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        StringBuffer taxError = null;
        ProcureLineItemCollection plic = null;

        if(object instanceof ariba.contract.core.ContractRequestLineItem) {
        	ContractRequestLineItem marli = (ContractRequestLineItem)object;
			plic = (ProcureLineItemCollection) marli.getLineItemCollection();
		} else if (object instanceof ProcureLineItemCollection) {
			plic = (ProcureLineItemCollection)object;
		}

		if(plic != null) {
            BaseVector lines = plic.getLineItems();
            Log.customer.debug("%s *** Calling triggerTax()!", "CatAssessTax");
            StringBuffer taxResponse = null;
            taxResponse = triggerTax(lines, false);
            if(taxResponse != null)
            {
                Log.customer.debug("%s **** Problem Calling Tax - setting AssessTaxMessage!", "CatAssessTax");
                plic.setFieldValue("AssessTaxMessage", taxResponse.toString());
                if(taxResponse.equals(taxServiceError))
                {
                    Log.customer.debug("%s **** Tax Service Down - reset TaxOverrideFlag!", "CatAssessTax");
                    plic.setFieldValue("TaxOverrideFlag", new Boolean(false));
                }
            } else
            {
                plic.setFieldValue("AssessTaxMessage", null);
                plic.setFieldValue("TaxOverrideFlag", new Boolean(false));
            }
        }
    }

    public static StringBuffer triggerTax(BaseVector lines, boolean isOrdered)
    {
        StringBuffer taxError = null;
        if(lines != null && !lines.isEmpty() && (lines.get(0) instanceof ProcureLineItem))
        {
            Log.customer.debug("CatAssessTax *** total lines (before): " + lines.size());
            List orderedLines = null;
            orderedLines = ListUtil.collectionToList(lines);
            int size = orderedLines.size();
            Log.customer.debug("CatAssessTax *** total lines (after): " + size);
            List inputs = new ArrayList();
            for(int i = 0; i < size; i++)
            {
                ProcureLineItem pli = (ProcureLineItem)orderedLines.get(i);
                Log.customer.debug("CatAssessTax *** LINE NUM #" + (i + 1));
                Object inputResponse[] = CatTaxCollector.createTaxInputObject(pli);
                if(inputResponse != null)
                {
                    TaxInputObject taxInput = (TaxInputObject)inputResponse[1];
                    Log.customer.debug("%s *** TaxInputObject: %s", "CatAssessTax", taxInput);
                    inputs.add(i, taxInput);
                    if(inputResponse[0] != null)
                    {
                        if(taxError == null)
                            taxError = new StringBuffer(ResourceService.getString("cat.java.vcsv1", "Error_AssessTaxMissingFields"));
                        taxError.append((String)inputResponse[0]);
                    }
                }
            }

            Log.customer.debug("CatAssessTax *** TaxInput List Size: " + inputs.size());
            if(taxError == null && !inputs.isEmpty())
            {
                List outputs = CatTaxCollector.callTaxService(inputs);
                Log.customer.debug("%s *** List outputs: %s", "CatAssessTax", outputs);
                if(outputs != null && !outputs.isEmpty())
                {
                    int count = outputs.size();
                    Log.customer.debug("CatAssessTax *** TaxOuputs List Size: " + count);
                    for(int i = 0; i < count; i++)
                    {
                        TaxOutputObject taxout = (TaxOutputObject)outputs.get(i);
                        Log.customer.debug("%s *** TaxOutputObject: %s", "CatAssessTax", taxout);
                        ProcureLineItem proline = (ProcureLineItem)orderedLines.get(i);
                        CatTaxCollector.setPLIFieldsFromTaxResponse(taxout, proline);
                        Log.customer.debug("%s *** (after) setPLIFieldsFromTaxResponse!", "CatAssessTax");
                    }

                } else
                {
                    Log.customer.debug("%s *** TaxOutputs List is NULL, setting DEFAULTS & Header Msg!", "CatAssessTax");
                    taxError = taxServiceError;
                    CatTaxCollector.setPLIFieldTaxDefaults(orderedLines);
                }
            }
        }
        return taxError;
    }

    public CatAssessTax()
    {
    }

    private static final String THISCLASS = "CatAssessTax";
    private static StringBuffer taxServiceError = new StringBuffer(Fmt.Sil("cat.java.vcsv1", "Error_AssessTaxWebServiceUnavailable"));

}