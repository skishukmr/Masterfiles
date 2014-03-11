 /*****************************************************************************************************************************
	Change History
	Change# Change By       Change Date     Description
	================================================================================================================================
	1       Kannan        17-08-06      Condition for MSDS field contains Only Digits and Non Zero.
	2       Nandini       23-06-11      MSDS field in SAP requisitions is limited, but user needs to enter more.
	                                    isNonZeroDigits() : This method is changed to increase the size of msds number  for validation from 
										int to long while parsing the MSDS value to Long to confirm if its non zero digit
                                        
	****************************************************************************************************************************/


package config.java.condition;

import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatMSDSIsNumeric extends Condition {

    public boolean evaluate(Object object, PropertyTable params)
        throws ConditionEvaluationException  {
        boolean isNumeric = true;
        Log.customer.debug("%s *** Object %s", "CatMSDSIsNumeric", object);

        if(object instanceof String) {
            String msds = (String)object;
            if(msds != null) {
                char chars[] = msds.toCharArray();
                Log.customer.debug("%s *** chars 2: %s", "CatMSDSIsNumeric", chars);
                isNumeric = containsOnlyDigits(chars) & isNonZeroDigits (msds);
                Log.customer.debug("CatMSDSIsNumeric **** isNumeric? " + isNumeric);
            }
        }
        return isNumeric;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
        throws ConditionEvaluationException {
        if(!evaluate(object, params)) {
            Log.customer.debug("%s *** evaluateAndExplain error: %s", "CatMSDSIsNumeric", errorMsg);
            return new ConditionResult(errorMsg);
        }
        else {
            return null;
        }
    }

	// Checking for Alpha numeric
    public static boolean containsOnlyDigits(char values[]) {
        Log.customer.debug("%s *** chars %s", "CatMSDSIsNumeric", values);
        boolean isDigits = true;

        if(values != null && values.length > 0) {
            Log.customer.debug("CatMSDSIsNumeric **** chars.length: " + values.length);

            for(int i = 0; i < values.length; i++) {
                char value = values[i];
                Log.customer.debug("CatMSDSIsNumeric **** chars[i]: " + value);
                if(Character.isDigit(value))
                    continue;
                isDigits = false;
                break;
            }
        }
        return isDigits;
    }

   // Checking for Non Zero Digitis
     public static boolean isNonZeroDigits(String msds)   {
			Log.customer.debug("%s *** chars %s", "calling IsNonZeroDigits", msds);

            boolean IsNonZeroDigits = true;
            if(msds != null && msds.length() > 0) {

				try{
					/**Issue 297 : Accepting long value of MSDS number for validation. MSDS number upto a 
					 * range of Long is accepted for validation.
				     * Parsing datatype changed from Integer.parseInt(msds) to Long.parseLong(msds)
				     * */
					if ((Long.parseLong(msds))==0)
					{
						Log.customer.debug("MSDS Number is ZERO");
						IsNonZeroDigits = false;
					}
				    return IsNonZeroDigits;
				}
				catch(Exception e) {
					Log.customer.debug("Exception caught while validating MSDS Number" +e);
					IsNonZeroDigits = false;
					return IsNonZeroDigits;
				}
		    }
        return IsNonZeroDigits;
      }

    public CatMSDSIsNumeric() {}

    private static final String THISCLASS = "CatMSDSIsNumeric";
    private static final String errorMsg = Fmt.Sil("cat.vcsv1", "ErrorMSDSIsNotNumeric");

}
