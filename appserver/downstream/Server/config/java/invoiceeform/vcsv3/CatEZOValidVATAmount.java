
package config.java.invoiceeform.vcsv3;

import java.math.BigDecimal;

import ariba.base.core.BaseObject;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionResult;
import ariba.basic.core.Money;
import ariba.user.core.User;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

/**  Author: KS.
 	Verifies Inv or Eform line item TaxAmount (VAT) is	1) same currency and 2) within tolerance.
*/

public class CatEZOValidVATAmount extends Condition {

	private static final String CLASSNAME = "CatEZOValidVATAmount";
    private static final String Tolerance = Fmt.Sil("cat.invoicejava.vcsv3","Condition_ValidVATAmountParam");
    private static final BigDecimal Var = new BigDecimal(Tolerance);
    private static final BigDecimal Base = new BigDecimal(".01");

    private String fmtError;
    private int validator;

    public boolean evaluate(Object value, PropertyTable params)
    {
        //if (Log.customer.debugOn)
            Log.customer.debug("%s *** EVALUATING!", CLASSNAME);
        validator = 0;
        fmtError="";
        validate(value, params);
        return validator==0;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params)
    {
    	return evaluate(value, params) ? null : new ConditionResult(fmtError);
    }

	protected void validate(Object value, PropertyTable params)
	{
		try {
			BaseObject line = (BaseObject)value;
			Money lineAmt = (Money)line.getFieldValue("Amount");
			Money actualAmt = (Money)line.getFieldValue("TaxAmount");
			if (lineAmt == null || actualAmt == null) {
			    return;  // no point in continuing (other errors will catch)
			}
			User user = (User)ariba.base.core.Base.getSession().getRealUser();
			// 1. Verify same currency for both amounts
			if (actualAmt.getCurrency() != lineAmt.getCurrency()) {
			    fmtError=Fmt.Sil(user.getLocale(),"cat.invoicejava.vcsv3","Condition_CurrencyMismatchMessage");;
			    validator = 1;
			}
			// 2. Verify TaxAmount is within tolerance
			else {
				BigDecimal rate = (BigDecimal)line.getFieldValue("VATRate");
				Money var = new Money(Var,lineAmt.getCurrency());
				Money calcAmt = lineAmt.multiply(rate.multiply(Base));
				//if (Log.customer.debugOn)
				    Log.customer.debug("%s *** actual/calc/var: %s, %s, %s",CLASSNAME,actualAmt,calcAmt,var);
				if (actualAmt.compareTo(calcAmt.add(var))>0 || actualAmt.compareTo(Money.subtract(calcAmt,var))<0) {
				    setErrorMessage(Money.subtract(calcAmt,var),calcAmt.add(var),user);
				    validator = 2;
				}
			}
		} catch (Exception e) {
		    Log.customer.debug("%s *** PROBLEM VALIDATING TaxAmount! *** Exception: %s", CLASSNAME, e);
		}
    	//if (Log.customer.debugOn)
    	    Log.customer.debug("CatEZOValidVATAmount **** validator: " + validator);
		return;
	}

    protected void setErrorMessage(Money lowAmt, Money highAmt, User user)
    {
        if (lowAmt != null && highAmt != null) {
			//if (Log.customer.debugOn)
			    Log.customer.debug("%s *** lowAmt/highAmt: %s, %s",CLASSNAME,lowAmt,highAmt);
			fmtError = Fmt.Sil(user.getLocale(),"cat.invoicejava.vcsv3","Condition_ValidVATAmountMessage");
            fmtError = Fmt.S(fmtError,lowAmt.asString(),highAmt.asString());
        }
    }

	public CatEZOValidVATAmount() {
		super();
	}

}

