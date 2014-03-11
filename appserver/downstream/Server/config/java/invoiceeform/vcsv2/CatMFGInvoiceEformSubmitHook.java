/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to validate invoice eform during the submit.
*/

package config.java.invoiceeform.vcsv2;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.BaseObject;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import config.java.invoiceeform.CatInvoiceEformSubmitHook;

public class CatMFGInvoiceEformSubmitHook extends CatInvoiceEformSubmitHook
{

    public List run (Approvable approvable)
    {

		List list = super.run(approvable);

		Integer integer = (Integer) list.get(0);

		if (integer != null) {
			if (integer.intValue() != NoError) {
				return list;
			}
		}

        ClusterRoot cr = (ClusterRoot)approvable;

        Money totalTaxAmount = (Money) cr.getFieldValue("TotalTax");

		String supplierTaxID = (String) cr.getDottedFieldValue("SupplierTaxID");

        Money computedTaxAmount = new Money(Constants.ZeroBigDecimal, totalTaxAmount.getCurrency());

        boolean nonTaxLineFound = false;
        boolean taxLineFound = false;

		List lineItems = (List)cr.getFieldValue("LineItems");

        int size = ListUtil.getListSize(lineItems);

        //Suppress Summary Invoice By KM
        List list2 = ListUtil.list();
		for(int j = 0; j < size; j++)
		{
			BaseObject baseobject = (BaseObject)lineItems.get(j);
			Object obj = baseobject.getDottedFieldValue("Order");
			ListUtil.addElementIfAbsent(list2, obj);
		}

		if(ListUtil.getListSize(list2) > 1)
		{
			String s = Fmt.Sil("aml.InvoiceEform", "MFGSummaryInvoiceError");
			return ListUtil.list(Constants.getInteger(-2), s);
        }
        //End Of Suppress Summary Invoice By KM

        int numberOfTaxLines = 0, numberOfNonTaxLines = 0;

        Object po;

        List poList = ListUtil.list();

        for (int i = 0; i < size; i++) {
            BaseObject lineItem = (BaseObject)lineItems.get(i);

            String lineTypeName = (String) lineItem.getDottedFieldValue("LineType.Name.PrimaryString");

            try {
				//make sure that SupplierTaxID is entered when VAT is present
				if (lineTypeName.equals("VAT")) {
					taxLineFound = true;
					if (supplierTaxID == null || supplierTaxID.equals("")) {
						return ListUtil.list(Constants.getInteger(ValidationError),
											 Fmt.Sil(catComponentStringTable,
													 "EnterSupplierTaxID"));
					}
					numberOfTaxLines = numberOfTaxLines + 1;
				} else {
					nonTaxLineFound = true;
					numberOfNonTaxLines = numberOfNonTaxLines + 1;
					po = lineItem.getDottedFieldValue("Order");
					ListUtil.addElementIfAbsent(poList, po);
				}

				Money taxAmount = (Money)lineItem.getFieldValue("TaxAmount");

				taxAmount.setAmount(taxAmount.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));

				if (taxAmount != null) {
					computedTaxAmount = Money.add(computedTaxAmount, taxAmount);
				}

			} catch (NullPointerException ne) {}

        }

		if (nonTaxLineFound) {
			/***
			If an invoice contains one or more non-tax line(s), make sure that there is a tax line.
			***/
			if (!taxLineFound) {
				String fmt =  Fmt.Sil(catComponentStringTable,
									  "EnterTaxLine");
				return ListUtil.list(Constants.getInteger(ValidationError),
										 fmt);
			}

			/***
			If summary invoice, make sure that the user enters one tax line per distinct order.
			***/
			int numberOfOrders = ListUtil.getListSize(poList);
			if (numberOfOrders > 1) {
				if ((numberOfTaxLines < numberOfOrders) || (numberOfTaxLines > numberOfOrders && numberOfTaxLines != numberOfNonTaxLines)) {
					//if there is not a tax line for every distinct order OR if the user has not entered
					//as many tax lines as there are number of non tax lines (line level VAT), give an
					//error message
					String fmt =  Fmt.Sil(catComponentStringTable, "EnterOneTaxLinePerDistinctOrder",new Integer(numberOfOrders));
					return ListUtil.list(Constants.getInteger(ValidationError), fmt);
				}
			}

			/***
			If an invoice contains non tax lines, make sure that the computed tax amount matches
			exactly with entered total tax amount ignoring any round off errors. If it does not,
			return error. If an invoice contains only tax line(s), do not do this validation.
			***/
			if ((totalTaxAmount.approxCompareTo(computedTaxAmount) != 0)) {
				String fmt =  Fmt.Sil(catComponentStringTable,
									  "NonMatchingTotalTax",
									  totalTaxAmount.asString(),
									  computedTaxAmount.asString());
				return ListUtil.list(Constants.getInteger(ValidationError),
										 fmt);
			}
		}

        return NoErrorResult;
    }
}
