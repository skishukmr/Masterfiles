/*****************************************************************************************************
 Name   : CatMFGsetRateInPercentage
 Author : Sudheer Kumar jain
 Purpose: Setting the Vat percent for old invoices
 *****************************************************************************************************/
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.util.core.Date;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
public class CatMFGsetRateInPercentage extends Action
        Date INVOICE_DATE =(Date)invoice.getDottedFieldValue("InvoiceDate");
	        if ((INVOICE_DATE != null) && (vatClass != null))
            INV_YEAR = Integer.toString(INVOICE_DATE.getYear() + 1900);
            INV_DATE = Integer.toString(INVOICE_DATE.getDate());
            if (INV_DATE.length()==1)
            INV = INV_YEAR + INV_MONTH + INV_DATE;
            Log.customer.debug("Invoice Date in string format is :in class CatMFGsetRateInPercentage  " + INV);
            INV_EFORM_DATE = Integer.parseInt(INV);
            Log.customer.debug("Invoice Date in Integer format is :in class CatMFGsetRateInPercentage  " + INV);
		    if (!StringUtil.nullOrEmptyOrBlankString(resourceFilename)) {
				 Log.customer.debug("CSV LINE IS :in class CatMFGsetRateInPercentage  " + INVOICE_DATE);

					Log.customer.debug("DATE1 FOR CSV LINE IS :in class CatMFGsetRateInPercentage  " + Date1);
					if (Date1 != null){

									Log.customer.debug("in class CatMFGsetRateInPercentage MessagetoUI " + MessagetoUI);

								}
                                        break;
		 								}
	 							}
		 				   }
							 }
		 			       br.close();
		 				   Log.customer.debug("%s *** IOException: %s", ClassName, e);
		 				      }
	    }
     }
 }
 		// function to tokenize CSV line.
 		public static List parseParamString (String paramString)  	{
			     List paramList = new ArrayList();
			     StringTokenizer stk = new StringTokenizer(paramString, ",");
			     while (stk.hasMoreTokens())
			     {
			     	paramList.add(stk.nextToken(","));
			     }
			     return paramList;
		     }




}