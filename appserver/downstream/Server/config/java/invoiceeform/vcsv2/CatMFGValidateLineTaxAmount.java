/*
 Author: Nani Venkatesan (Ariba Inc.)
   Date; 5/29/2005
Purpose: The purpose of this class is to validate the tax amount in the invoice line.

Amit - CR # 755 5th MArch 2008 - Added condition to check if Vat Class = 5 and allow the vat amount to be calculated
*/

package config.java.invoiceeform.vcsv2;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.basic.core.Money;
import ariba.common.core.Log;
import ariba.procure.core.ProcureLineType;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;

public class CatMFGValidateLineTaxAmount extends Condition
{
    private static final String requiredParameterNames[] = {"LineItem"};
    private static ValueInfo parameterInfo[] = {
        new ValueInfo("LineItem",
                               0,
                               "ariba.base.core.BaseObject")
    };

    private static final String ComponentStringTable = "aml.cat.Invoice";
	private static final double roundOffTolerance = 0.01;

	private String errorMsg = null, ClassName = "config.java.invoiceeform.vcsv2.CatMFGValidateLineTaxAmount";

    public boolean evaluate (Object value, PropertyTable params)
    {
        return evaluateImpl(value, params);
    }

    private boolean evaluateImpl (Object value, PropertyTable params)
    {

		Log.customer.debug(ClassName);

        // Get the invoice line item
        BaseObject invoiceLineItem = (BaseObject) params.getPropertyForKey("LineItem");
        Log.customer.debug("CatMFGValidateLineTaxAmount" +invoiceLineItem);
       // CR # 755 : getting purpose from invoice object
       BaseObject inv = (BaseObject)invoiceLineItem.getFieldValue("LineItemCollection");
    		try {
    	if (inv.getFieldValue("Purpose") != null)
    	{
    	boolean isCreditMemo = "creditMemo".equals((String)inv.getFieldValue("Purpose"));

    	Integer category = (Integer) invoiceLineItem.getDottedFieldValue("LineType.Category");
         Log.customer.debug("CatMFGValidateLineTaxAmount category" +category);
		if (category == null || category.intValue() == ProcureLineType.TaxChargeCategory) {
			  Log.customer.debug("CatMFGValidateLineTaxAmount category 11" +category);
			return true;
		}


			Money invLnAmt  = (Money) invoiceLineItem.getDottedFieldValue("Amount");
			Money taxAmount = (Money) invoiceLineItem.getDottedFieldValue("TaxAmount");
			String vatClass = (String) invoiceLineItem.getDottedFieldValue("VATClass.UniqueName");

			if (taxAmount == null) {
				  Log.customer.debug("CatMFGValidateLineTaxAmount taxAmount is null");
				errorMsg = "InvalidVATAmount";
				return false;
			}

			//make sure that line amount ccy and the tax ccy match
			if (invLnAmt.getCurrency() != taxAmount.getCurrency()) {
			 Log.customer.debug("CatMFGValidateLineTaxAmount taxAmount is having diff currency than line item");
				errorMsg = "InvalidTaxCurrency";
				return false;
			}

			if (category.intValue() == ProcureLineType.DiscountCategory) {
				if (taxAmount.getAmount().doubleValue() > 0.0) {
					Log.customer.debug("CatMFGValidateLineTaxAmount taxAmount is having value more than 0 for discount");
					errorMsg = "InvalidVATAmount";
					return false;
				}
			} else {
				if ( !(isCreditMemo) && (taxAmount.getAmount().doubleValue() < 0.0)) {
					Log.customer.debug("CatMFGValidateLineTaxAmount taxAmount is having less than 0");
					errorMsg = "InvalidVATAmount";
					return false;
				}
			}
          //  CR # 755 Added a check for VATCLass 5
			if (vatClass.equals("1") || vatClass.equals("8") || vatClass.equals("5")) {
				Log.customer.debug("VATClass is 1 or 8 or 5");
				if (taxAmount.getAmount().doubleValue() == 0.0) {
					errorMsg = "VATAmountMustNotBeZero";
					return false;
				}
			} else {
				Log.customer.debug("VATClass is not 1 or 8 or 5");
				if (taxAmount.getAmount().doubleValue() != 0.0) {
					errorMsg = "VATAmountMustBeZero";
					return false;
				}
			}


}
		} catch (NullPointerException ne) {Log.customer.debug("NPE in " + ClassName);}

		return true;

    }

    /**
        Tests the condition and returns an error message
    */
    public ConditionResult evaluateAndExplain (Object value, PropertyTable params)
    {
        if (!evaluateImpl(value, params)) {
            return new ConditionResult(Fmt.Sil(ComponentStringTable,
                                               errorMsg,
                                               subjectForMessages(params)));
        }
        else {
            return null;
        }
    }

    /**
        Returns the valid parameter types
    */
    protected ValueInfo[] getParameterInfo ()
    {
        return parameterInfo;
    }

    /**
        Returns required parameter names for the class
    */
    protected String[] getRequiredParameterNames ()
    {
        return requiredParameterNames;
    }

}
