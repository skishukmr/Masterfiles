/*255-IBM AMS_Bijesh Kumar-Budget Check Logic for Account Type "F" and Company Code '1000'*/

package config.java.action.sap;

import java.util.ArrayList;

import config.java.common.CatCommonUtil;
import ariba.base.fields.Action;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.procure.core.ProcureLineItem;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.util.core.PropertyTable;
import config.java.common.sap.CatSAPAccountingCollector;
import config.java.common.sap.CatSAPAccountingValidator;
import config.java.integration.ws.sap.SAPAccountValidator;
import config.java.action.sap.CatSAPValidateAccountingString;
import ariba.util.log.Log;

public class CATSAPSetCheckValidateAcctButton extends Action
{
    final String className = "CATSAPSetCheckValidateAcctButton";
	 public void fire(ValueSource object, PropertyTable params)
	 {
		 Log.customer.debug("CATSAPSetCheckValidateAcctButton : ****START****");
		 try
		 {

	       if (!(object instanceof SplitAccounting))
	       {
			   return;
		   }
	       SplitAccounting splitAccounting = (SplitAccounting)object;
	       ProcureLineItem procLI = (ProcureLineItem)splitAccounting.getLineItem();
	       String acctCategory = (String)procLI.getDottedFieldValue("AccountCategory.UniqueName");
		   Log.customer.debug(className+"%s Account category is : %s",acctCategory);
		   String mach1CompanyCode = (String)procLI.getLineItemCollection().getDottedFieldValue("CompanyCode.UniqueName");
		   Log.customer.debug(className+"%s Company Code is : %s",mach1CompanyCode);
		   ArrayList<String> companyCodeFromFile = CatCommonUtil.readDataFromFile(CatCommonUtil.COMPANYCODE_FileName);
		   Log.customer.debug(className+"%s companyCode From File is : %s",companyCodeFromFile);
		   ArrayList<String> accountCategoryFromFile = CatCommonUtil.readDataFromFile(CatCommonUtil.ACCOUNTTYPE_FileName);
		   Log.customer.debug(className+"%s accountCategory From File is : %s",accountCategoryFromFile);
		   if(mach1CompanyCode != null && acctCategory != null)
	       Log.customer.debug(className+"**** inside if for null check for mach1CompanyCode and acctCategory  in classname= %s ",className);
		   {
			   Boolean validCC = CatCommonUtil.checkValueIsAvailable(mach1CompanyCode,companyCodeFromFile);
			   Boolean validAccountCategory = CatCommonUtil.checkValueIsAvailable(acctCategory,accountCategoryFromFile);
	           Log.customer.debug(className+"validCC from the method checkValueIsAvailable in classname= %s ",className);
	           Log.customer.debug(className+"validAccountCategory from the method checkValueIsAvailable in classname= %s ",className);
	           Boolean validWBS = checkWbsElement(splitAccounting,procLI);
	           Log.customer.debug(className+"%s validWBS is : %s",validWBS);
	           if(validCC && validAccountCategory && validWBS)
			   	{
	    	       Log.customer.debug(className+"**** inside if after reading mach1CompanyCode=%s and acctCategory=%s from file of classname= %s ",mach1CompanyCode,acctCategory,className);
	    	       procLI.setFieldValue("CheckValidateAcctButton",false);
			        Log.customer.debug(className+"After verification,the CompanyCode= %s and Account Category= %s for the Class %s",mach1CompanyCode,acctCategory,className);
			    }
			        Log.customer.debug(className+"After NullCheck,the CompanyCode= %s and Account Category= %s is not null for the Class %s",mach1CompanyCode,acctCategory,className);
			}
                    Log.customer.debug(className+"After try block,the CompanyCode= %s and Account Category= %s is for the Class %s",mach1CompanyCode,acctCategory,className);
      }
		 catch(Exception e)
		 {
			 Log.customer.debug(className+"For fire method in classname CATSAPSetCheckValidateAcctButton : Exception : ****"+ e.toString() +"****");
			 return;
		 }
        }

		public boolean checkWbsElement(SplitAccounting splitAccounting,ProcureLineItem procLI)

	 {
            try 
             {

		String oldWBS = (String)splitAccounting.getFieldValue("WBSElementText");
		Log.customer.debug(className+"%s oldWBS is : %s",oldWBS);
		CatSAPAccountingCollector accclr = CatSAPValidateAccountingString.getCatSAPAccounting(splitAccounting,procLI);
		CatSAPAccountingValidator response = null;
		response = SAPAccountValidator.validateAccount(accclr);
		String newWBS = null ;
		newWBS = response.getIOWBSele();
		Log.customer.debug(className+"%s newWBS is : %s",newWBS);
		if (oldWBS.equals(newWBS))

		{
			return false;

	    }
        else

        {

		   return true;

	     }
         }
             catch(Exception e)
                 {
                         Log.customer.debug(className+"For checkWBSElement method in classname CATSAPSetCheckValidateAcctButton : Exception : ****"+ e.toString() +"****");
                         e.printStackTrace();
                         return false;
                 }


	  }




	 }
