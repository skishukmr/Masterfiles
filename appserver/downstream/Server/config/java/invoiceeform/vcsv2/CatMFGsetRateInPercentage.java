/*****************************************************************************************************
 Name   : CatMFGsetRateInPercentage
 Author : Sudheer Kumar jain
 Purpose: Setting the Vat percent for old invoices
 *****************************************************************************************************/package config.java.invoiceeform.vcsv2;import java.io.BufferedReader;
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
public class CatMFGsetRateInPercentage extends Action{    public void fire (ValueSource object, PropertyTable params)    {		Log.customer.debug("Entered in to the class CatMFGsetRateInPercentage ");		BaseObject invoice = (BaseObject) object;		String resourceFilename = "/msc/arb821/Server/config/variants/vcsv2/data/cat.vcsv2.setRate.csv";		int cnt = 0;        String MessagetoUI = (String)invoice.getDottedFieldValue("VATRatemessage");		Log.customer.debug("Counter value is : " + cnt);        String csvLine = null;        String INV_MONTH = null;        String INV_YEAR = null;        String INV_DATE = null;        String INV = null;        int INV_EFORM_DATE;        int CSV_Date1=0;        int CSV_Date2=0;
        Date INVOICE_DATE =(Date)invoice.getDottedFieldValue("InvoiceDate");        String vatClass = (String)invoice.getDottedFieldValue("SupplierLocation.VATClass.UniqueName");         Log.customer.debug("Invoice Date is :in class CatMFGsetRateInPercentage  " + INVOICE_DATE);		 Log.customer.debug("Invoice Date is :in class CatMFGsetRateInPercentage  " + vatClass);
	        if ((INVOICE_DATE != null) && (vatClass != null))            {
            INV_YEAR = Integer.toString(INVOICE_DATE.getYear() + 1900);            Log.customer.debug("Invoice Year is :in class CatMFGsetRateInPercentage  " + INV_YEAR);            INV_MONTH = Integer.toString(INVOICE_DATE.getMonth() + 1);            if (INV_MONTH.length()==1)                 INV_MONTH = "0" + INV_MONTH;             Log.customer.debug("Invoice Month is :in class CatMFGsetRateInPercentage  " + INV_MONTH);
            INV_DATE = Integer.toString(INVOICE_DATE.getDate());
            if (INV_DATE.length()==1)                 INV_DATE = "0" + INV_DATE;            Log.customer.debug("Invoice Date is :in class CatMFGsetRateInPercentage  " + INV_DATE);
            INV = INV_YEAR + INV_MONTH + INV_DATE;
            Log.customer.debug("Invoice Date in string format is :in class CatMFGsetRateInPercentage  " + INV);
            INV_EFORM_DATE = Integer.parseInt(INV);
            Log.customer.debug("Invoice Date in Integer format is :in class CatMFGsetRateInPercentage  " + INV);
		    if (!StringUtil.nullOrEmptyOrBlankString(resourceFilename)) {            File file = new File(resourceFilename);            Log.customer.debug("File is :in class CatMFGsetRateInPercentage  " + file);  			if (file != null) {			try {				BufferedReader br = new BufferedReader(new FileReader(file));	 			String line = null;	 			Log.customer.debug("Line is :in class CatMFGsetRateInPercentage  " + br.readLine());				while ((line = br.readLine())!= null && !StringUtil.nullOrEmptyOrBlankString(line)) {
				 Log.customer.debug("CSV LINE IS :in class CatMFGsetRateInPercentage  " + INVOICE_DATE);				List values = parseParamString(line);				String vatClass_csv = (String)values.get(0);				Log.customer.debug("DATE1 FOR CSV LINE IS :in class CatMFGsetRateInPercentage  " + vatClass_csv);				if (vatClass_csv.equals(vatClass))				{					String Date1 = (String)values.get(1);                    MessagetoUI = "The Invoice date fall between the" + Date1;					String Date2 = (String)values.get(2);  				    MessagetoUI = MessagetoUI + " and " +  Date2 + " , so the Tax amount is being calculated using old rate ";

					Log.customer.debug("DATE1 FOR CSV LINE IS :in class CatMFGsetRateInPercentage  " + Date1);
					if (Date1 != null){ 					  Date1 = Date1.replaceAll("-","");  					  Log.customer.debug("DATE1 OF CSV FILE after removing the special char :in class CatMFGsetRateInPercentage  " + Date1);						// Converting String Date of CSV file in to Interger                        CSV_Date1 = Integer.parseInt(Date1);                        Log.customer.debug("Integer format DATE1 OF CSV FILE after removing the special char :in class CatMFGsetRateInPercentage  " + Date1);				    }				    if (Date2 != null){ 						Log.customer.debug("DATE2 FOR CSV LINE IS :in class CatMFGsetRateInPercentage  " + Date2); 						Date2 = Date2.replaceAll("-",""); 						Log.customer.debug("DATE2 OF CSV FILE after removing the special char :in class CatMFGsetRateInPercentage  " + Date2); 						// Converting String Date of CSV file in to Interger					    CSV_Date2 = Integer.parseInt(Date2);                        Log.customer.debug("Integer format DATE2 OF CSV FILE after removing the special char :in class CatMFGsetRateInPercentage  " + Date2);				    }					String UKVAT1 = (String)values.get(3);					Log.customer.debug("UKVAT1 FOR CSV LINE IS :in class CatMFGsetRateInPercentage  " + UKVAT1);                    MessagetoUI = MessagetoUI + UKVAT1;
					if (CSV_Date1 <= INV_EFORM_DATE && CSV_Date2 >= INV_EFORM_DATE) {                    Log.customer.debug("Invoice date is matching :in class CatMFGsetRateInPercentage  "  );				   if (UKVAT1 != null) {							Log.customer.debug(" CatMFGsetRateInPercentage UKVAT1 is not null " + UKVAT1);                            BaseVector li =(BaseVector)invoice.getFieldValue("LineItems");                            Log.customer.debug(" CatMFGsetRateInPercentage eform line items li " +li);                            for (cnt=0; cnt < li.size(); cnt++)                               	{									Log.customer.debug(" CatMFGsetRateInPercentage li.size() " + li.size());									BaseObject inv_line = (BaseObject)li.get(cnt);									Log.customer.debug(" CatMFGsetRateInPercentage inv_line " + inv_line);								//	Integer category = (Integer) inv_line.getDottedFieldValue("LineType.Category");								//	if (category != null)								//	Log.customer.debug(" CatMFGsetRateInPercentage category " + category);                                //    if (category.intValue() != ProcureLineType.TaxChargeCategory)                            	    Log.customer.debug("Setting the NewRateInPercentage field value : in class CatMFGsetRateInPercentage  " + UKVAT1 );									inv_line.setDottedFieldValue("NewRateInPercentage",UKVAT1);									invoice.setDottedFieldValue("VATRatemessage",MessagetoUI);
									Log.customer.debug("in class CatMFGsetRateInPercentage MessagetoUI " + MessagetoUI);

								}
                                        break;
		 								}
	 							}
		 				   }
							 }
		 			       br.close();	 			}	   catch (IOException e) {
		 				   Log.customer.debug("%s *** IOException: %s", ClassName, e);
		 				      } 			}
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