/********************************************************************************************
 Change History
 	#	Change By	Change Date		Description
	===========================================================================================
	   Amit Gupta   05/30/10        Issue # 1098 Removed the account category F from isAccountingValidationRequired()
	                                method
	Purush Kancharla	10-Aug-2012		Added Acoounting validation for Account category P and F for LSAP partition.
325/MGPP1719 Vikram_AMS 08/30/12	Add a new account type S under requisitions for MACH1 5.0 release
********************************************************************************************/
package config.java.common.sap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ariba.approvable.core.LineItem;
import ariba.base.core.Base;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.FieldProperties;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.common.core.SupplierLocation;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;
import ariba.common.core.Address;
import ariba.base.core.*;
import ariba.base.core.aql.*;
import ariba.util.core.StringUtil;
import config.java.integration.ws.sap.SAPAccountValidator;
import ariba.tax.core.TaxCode;
import ariba.basic.core.Country;

public class CATSAPUtils {

private static final String classname = "CATSAPUtils : ";

public static String[] dbParams()
	{
	//code for getting dbparams  from parameters.table
	System.out.println("**** Entered into dbParams Method");
	  //Partition partition= Base.getService().getPartition("None");
		System.out.println("\nBase.getService()  "+ Base.getService());
		System.out.println("\nBase.getServiceParameter  "+ Base.getService().getParameter(Base.getService().getPartition("None"),"System.Base.DBName"));
		String DBName = (String)Base.getService().getParameter(Base.getService().getPartition("None"),"System.Base.DBName");
		System.out.println("\nDBName  "+ DBName);
		DBName = "jdbc:db2:" + DBName;
		System.out.println("\nDBName  "+ DBName);
		String DBUser =(String)Base.getService().getParameter(Base.getService().getPartition("None"),"System.Base.DBUser");
		System.out.println("\nDBUser  "+ DBUser);
		String DBPwd = (String)Base.getService().getParameter(Base.getService().getPartition("None"),"System.Base.DBPwd");
		System.out.println("\nDBPwd  "+ DBPwd);
		String[] dbparams ={ DBName,DBUser,DBPwd};
		return dbparams;
		}

public static String anyBlockVendor(Requisition requisition){

	String blockVendorResult = "0";

	FastStringBuffer lineMsg = new FastStringBuffer();



	List lineitems = (List)requisition.getLineItems();

	for(int i=0; i<lineitems.size();i++){

		ReqLineItem rli = (ReqLineItem)lineitems.get(i);



		SupplierLocation suppLoc = (SupplierLocation)rli.getSupplierLocation();

		if (suppLoc !=null){

		String blockIndicator = (String)suppLoc.getDottedFieldValue("BlockIndicator");

		if(blockIndicator!=null && blockIndicator.equalsIgnoreCase("X")){

			String lineErrorResult = " Line "+ rli.getNumberInCollection() +": " + ResourceService.getString("cat.java.sap","BlockVendorWarningMsg") + ".  ";

			Log.customer.debug("CatSAPReqSubmitHook : anyBlockVendor : lineErrorResult " + lineErrorResult);



			lineMsg.append(lineErrorResult);

			blockVendorResult = lineMsg.toString();

			Log.customer.debug("CatSAPReqSubmitHook : anyBlockVendor : blockVendorResult " + blockVendorResult);

		}

		}

	}



	return blockVendorResult;

}

public static boolean isReqERFQ(Requisition plic) {



	Log.customer.debug("CatSAPReqSubmitHook : isReqERFQ : ***START***");



	boolean iSeRFQ = false;



	Log.customer.debug("CatSAPReqSubmitHook : isReqERFQ : lic " + plic);

	Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");

	Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");

	if ((iSeRFQB != null) && iSeRFQB.booleanValue()) {

		iSeRFQ = true;

	}
	if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {

		iSeRFQ = true;

	}

	Log.customer.debug("CatSAPReqSubmitHook : isReqERFQ : before returning : iSeRFQ " + iSeRFQ);

	Log.customer.debug("CatSAPReqSubmitHook : isReqERFQ : ***END***");

	return iSeRFQ;

}

public static boolean wasReqERFQ(Requisition plic) {



	Log.customer.debug("CatSAPReqSubmitHook : wasReqERFQ : ***START***");



	boolean wasReqERFQ = false;



	Log.customer.debug("CatSAPReqSubmitHook : wasReqERFQ : lic " + plic);

	Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");

	Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");

	if ((iSeRFQB != null) && !iSeRFQB.booleanValue()) {

		if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {

			wasReqERFQ = true;

		}
	}

	Log.customer.debug("CatSAPReqSubmitHook : wasReqERFQ : before returning : wasReqERFQ " + wasReqERFQ);

	Log.customer.debug("CatSAPReqSubmitHook : wasReqERFQ : ***END***");

	return wasReqERFQ;

}

public static String checkAccounting(Requisition requisition) {

	// Added by James -- Dec 04 2008
	String NO_ACCT_VALIDATION = "0";
	if(	CATSAPUtils.isReqERFQ(requisition) && !CATSAPUtils.wasReqERFQ(requisition)){
        Log.customer.debug("CatSAPReqSubmitHook : checkAccounting : It is ERFQ");
		return NO_ACCT_VALIDATION;
	}

	BaseVector splits = getDistinctSplits(requisition);
	String formatLineError ="0";
	String lineErrorResult = "0";
	int lineErrors = 0;
	FastStringBuffer lineMsg = new FastStringBuffer();
	for(int j = 0; j < splits.size(); j++)

            {

				int splitErrors = 0;

				int errorSplit = 0;

				FastStringBuffer splitMsg = new FastStringBuffer();



            	Log.customer.debug("%s *** saci has next",classname);

            	CATSplitAccLineItemCombo catSplitAccLICombo = (CATSplitAccLineItemCombo)splits.get(j);

                SplitAccounting sa = (SplitAccounting)catSplitAccLICombo.getSa();

                Log.customer.debug("%s *** found sa %s ",classname, sa.toString());

                ReqLineItem rli = (ReqLineItem)catSplitAccLICombo.getPli();

                Log.customer.debug("%s *** found ReqLineItem %s ",classname, rli);

                CatSAPAccountingCollector accclr = getCatSAPAccounting(sa,rli);

                Log.customer.debug("%s *** found accclr %s " ,classname, accclr.toString());

                if(accclr != null)

                {

                    Log.customer.debug("%s *** accclr not null",classname);

                    CatSAPAccountingValidator response = null;

                    try

                    {

                        Log.customer.debug("%s *** inside try 1",classname);

                        response = SAPAccountValidator.validateAccount(accclr);

                        Log.customer.debug("%s *** passed cats to validateaccount");

                        Log.customer.debug("%s *** response " + response.toString());

                        if(response != null)

                        {

                            Log.customer.debug("%s *** ResultCode: %s" , classname, response.getResultCode());

                            Log.customer.debug("%s *** Message: %s", classname, response.getMessage());

                            FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
                           //Added by Sandeep for MACH1 2.5 changes
                            if(response.getResultCode().equals("000") ||  response.getResultCode().equals("020") )

                            {
                            Log.customer.debug("Adding null to ValidateAccountingMessage for Response Code 020 as MACH1 2.5 change - Sandeep");
                            	sa.setFieldValue("ValidateAccountingMessage",null);

                            }else

                            {

								splitErrors += 1;

								lineErrors +=1;

								errorSplit = sa.getNumberInCollection();

								String splitErrorResult = " Line "+ rli.getNumberInCollection() +":(Account Distribution #" +errorSplit +")" + response.getMessage();

								splitMsg.append(splitErrorResult + ".  ");

								Log.customer.debug("%s *** Message: splitMsg.toString() %s", classname, splitMsg.toString());



                            	lineErrorResult = splitMsg.toString();

                            	lineMsg.append(lineErrorResult);

                            	Log.customer.debug("%s *** Message: lineMsg.toString() %s", classname, lineMsg.toString());

                            	formatLineError = lineMsg.toString();

                            	Log.customer.debug("%s *** Message: formatLineError %s", classname, formatLineError);

                  			}

                        }

                        }

                    catch(Exception e)

                    {
                    	formatLineError = ResourceService.getString("cat.java.sap", "AccValExcpMessage");
                    	return formatLineError;

                    }

                }

            }



	return formatLineError;

}

public static boolean isAccountValidationRequired(ReqLineItem pli){



	String acccat = null;

	String comcode = null;

	String sapsrc = null;

	String isGLEditableForCapital = null;

	String isGLEditableForInterCompany = null;

	Log.customer.debug("%s *** isAccountValidationRequired pli %s ", classname,pli);

	if(pli == null){

	return false;

	}

	Log.customer.debug("%s *** isAccountValidationRequired pli.getLineItemCollection() %s ", classname, pli.getLineItemCollection());

	if(pli.getLineItemCollection() == null){

		return false;

	}

	ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");

	Log.customer.debug("%s *** isAccountValidationRequired company %s ", classname,company);

    if(company!=null){

    comcode = (String)company.getDottedFieldValue("UniqueName");

    sapsrc = (String)company.getDottedFieldValue("SAPSource");
  Log.customer.debug("%s *** isAccountValidationRequired SAP source %s " , classname,sapsrc);

    isGLEditableForCapital = (String)company.getDottedFieldValue("isGLEditableForCapital");

    isGLEditableForInterCompany = (String)company.getDottedFieldValue("isGLEditableForInterCompany");

    }

    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");

    if(acccategory!=null){

    acccat = (String)acccategory.getDottedFieldValue("UniqueName");

    }

    if(acccat != null && acccat.equalsIgnoreCase("K")){

	return true;

    }

	//Vikram (Issue 325/MGPP1719) Starts - New Acct type S
	else if(acccat != null && acccat.equalsIgnoreCase("S")){

	return true;

    }// Vikram (Issue 325/MGPP1719) Ends

    else if((acccat != null && (acccat.equalsIgnoreCase("P")))

    		&&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

    	return true;

	}

else if((acccat != null && (acccat.equalsIgnoreCase("F")) && sapsrc.equalsIgnoreCase("MACH1"))

                &&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

        return true;

        }
//PK code chnages start
else if((acccat != null && (acccat.equalsIgnoreCase("F")) && sapsrc.equalsIgnoreCase("CGM"))

                &&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

        return true;

        }
//PK code chnages end

    else if((acccat != null && acccat.equalsIgnoreCase("Z"))

    		&&(isGLEditableForInterCompany != null && isGLEditableForInterCompany.equalsIgnoreCase("Y"))){

    	return true;

    }

    return false;

}

public static boolean isAccountValidationRequired(InvoiceReconciliationLineItem pli){



	String acccat = null;

	String comcode = null;

	String sapsrc = null;

	String isGLEditableForCapital = null;

	String isGLEditableForInterCompany = null;

	Log.customer.debug("%s *** isAccountValidationRequired pli %s ", classname,pli);

	if(pli == null){

	return false;

	}

	Log.customer.debug("%s *** isAccountValidationRequired pli.getLineItemCollection() %s ", classname, pli.getLineItemCollection());

	if(pli.getLineItemCollection() == null){

		return false;

	}

	ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");

	Log.customer.debug("%s *** isAccountValidationRequired company %s ", classname,company);

    if(company!=null){

    comcode = (String)company.getDottedFieldValue("UniqueName");

    sapsrc = (String)company.getDottedFieldValue("SAPSource");

    isGLEditableForCapital = (String)company.getDottedFieldValue("isGLEditableForCapital");

    isGLEditableForInterCompany = (String)company.getDottedFieldValue("isGLEditableForInterCompany");

    }

    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");

    if(acccategory!=null){

    acccat = (String)acccategory.getDottedFieldValue("UniqueName");

    }

    if(acccat != null && acccat.equalsIgnoreCase("K")){

	return true;

    }

		//Vikram (Issue 325/MGPP1719) Starts - New Acct type S
	else if(acccat != null && acccat.equalsIgnoreCase("S")){

	return true;

    }// Vikram (Issue 325/MGPP1719) Ends

else if((acccat != null && acccat.equalsIgnoreCase("P"))

                &&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

        return true;

        }

    else if((acccat != null &&  acccat.equalsIgnoreCase("F") && sapsrc.equalsIgnoreCase("MACH1"))

    		&&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

    	return true;

	}

    else if((acccat != null && acccat.equalsIgnoreCase("Z"))

    		&&(isGLEditableForInterCompany != null && isGLEditableForInterCompany.equalsIgnoreCase("Y"))){

    	return true;

    }

    return false;

}

public static CatSAPAccountingCollector getCatSAPAccounting(SplitAccounting sa, ReqLineItem pli)

{

    Log.customer.debug("%s *** inside get cat actng: sa :%s " ,classname, sa.toString());

    Log.customer.debug("%s *** inside get cat actng: pli :%s " ,classname, pli);

    String cstctr = (String)sa.getFieldValue("CostCenterText");

    String genlgr = (String)sa.getFieldValue("GeneralLedgerText");

    String intord = (String)sa.getFieldValue("InternalOrderText");

    String wbsele = (String)sa.getFieldValue("WBSElementText");

    String comcode = null;

    String sapsrc = null;

    String acccat = null;

    Log.customer.debug("%s *** getcatactng pli.getLineItemCollection() %s", classname, pli.getLineItemCollection());

    ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");

    if(company!=null){

    comcode = (String)company.getDottedFieldValue("UniqueName");

    sapsrc = (String)company.getDottedFieldValue("SAPSource");

    }

    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");

    if(acccategory!=null){

    acccat = (String)acccategory.getDottedFieldValue("UniqueName");

    }

    Log.customer.debug("%s *** returning from getcatactng ", classname);

    return new CatSAPAccountingCollector(cstctr, genlgr, intord, wbsele, comcode, sapsrc, acccat);

}

public static CatSAPAccountingCollector getCatSAPAccounting(SplitAccounting sa, InvoiceReconciliationLineItem pli)

{

    Log.customer.debug("%s *** inside get cat actng: sa :%s " ,classname, sa.toString());

    Log.customer.debug("%s *** inside get cat actng: pli :%s " ,classname, pli);

    String cstctr = (String)sa.getFieldValue("CostCenterText");

    String genlgr = (String)sa.getFieldValue("GeneralLedgerText");

    String intord = (String)sa.getFieldValue("InternalOrderText");

    String wbsele = (String)sa.getFieldValue("WBSElementText");

    String comcode = null;

    String sapsrc = null;

    String acccat = null;

    Log.customer.debug("%s *** getcatactng pli.getLineItemCollection() %s", classname, pli.getLineItemCollection());

    ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");

    if(company!=null){

    comcode = (String)company.getDottedFieldValue("UniqueName");

    sapsrc = (String)company.getDottedFieldValue("SAPSource");

    }

    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");

    if(acccategory!=null){

    acccat = (String)acccategory.getDottedFieldValue("UniqueName");

    }

    Log.customer.debug("%s *** returning from getcatactng ", classname);

    return new CatSAPAccountingCollector(cstctr, genlgr, intord, wbsele, comcode, sapsrc, acccat);

}


public static BaseVector getDistinctSplits(Requisition requisition){

	try{

    	BaseVector distinctsplits = new BaseVector();

    	HashMap distinctAccString = new HashMap();

		BaseVector lines = requisition.getLineItems();

		for (int line = 0; line < lines.size(); line++) {

			ReqLineItem rli = (ReqLineItem)lines.get(line);



            if(isAccountValidationRequired(rli)){

    			SplitAccountingCollection sac = rli.getAccountings();

    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());

    	        if(sac != null)

    	        {

    	        	BaseVector splits = sac.getSplitAccountings();

    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());



	                for (int i=0; i<splits.size();i++){

		    		SplitAccounting sa = (SplitAccounting)splits.get(i);

		    		String cstctr = (String)sa.getFieldValue("CostCenterText");

		            String genlgr = (String)sa.getFieldValue("GeneralLedgerText");

		            String intord = (String)sa.getFieldValue("InternalOrderText");

		            String wbsele = (String)sa.getFieldValue("WBSElementText");

		            String comcode = null;

		            String sapsrc = null;

		            String acccat = null;

		            ClusterRoot company =(ClusterRoot)rli.getLineItemCollection().getDottedFieldValue("CompanyCode");

		            if(company!=null){

		            comcode = (String)company.getDottedFieldValue("UniqueName");

		            sapsrc = (String)company.getDottedFieldValue("SAPSource");

		            }

		            ClusterRoot acccategory =(ClusterRoot)rli.getDottedFieldValue("AccountCategory");

		            if(acccategory!=null){

		            acccat = (String)acccategory.getDottedFieldValue("UniqueName");

		            }



		    		cstctr = cstctr == null? cstctr : cstctr.toUpperCase();

		    		genlgr = genlgr == null? genlgr : genlgr.toUpperCase();

		    		intord = intord == null? intord : intord.toUpperCase();

		    		wbsele = wbsele == null? wbsele : wbsele.toUpperCase();

		    		comcode = comcode == null? comcode : comcode.toUpperCase();

		    		sapsrc = sapsrc == null? sapsrc : sapsrc.toUpperCase();

		    		acccat = acccat == null? acccat : acccat.toUpperCase();



		            String accString = cstctr + genlgr + intord + wbsele + comcode + sapsrc + acccat;

		            CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,rli);

		            distinctAccString.put(accString,catSplitAccLICombo);

	                }

    	        }

            }

		}



		if(distinctAccString != null)

    	{

    		Log.customer.debug("%s *** : getDistinctSplits inside distinctAccString" ,classname );

	    	Set set = distinctAccString.entrySet();

	    	Iterator i = set.iterator();

	    	while(i.hasNext()){

	    			Map.Entry map = (Map.Entry)i.next();

	    			Log.customer.debug("%s *** : getDistinctSplits : %s" ,classname ,map.getValue());

	    			distinctsplits.add(map.getValue());

	    	}

    	}

		return distinctsplits;

	}

	catch(Exception e){

    	Log.customer.debug("%s *** : getDistinctSplits %s " ,classname, e.toString() );

    	BaseVector distinctsplits = new BaseVector();

    	BaseVector lines = requisition.getLineItems();

		for (int line = 0; line < lines.size(); line++) {

			ReqLineItem rli = (ReqLineItem)lines.get(line);



            if(isAccountValidationRequired(rli)){

    			SplitAccountingCollection sac = rli.getAccountings();

    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());

    	        if(sac != null)

    	        {

    	        	BaseVector splits = sac.getSplitAccountings();

    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());

	                for (int i=0; i<splits.size();i++){

			    		SplitAccounting sa = (SplitAccounting)splits.get(i);

			    		CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,rli);

			    		distinctsplits.add(catSplitAccLICombo);

	                }

    	        }

            }

		}

		return distinctsplits;

    }

}

public static BaseVector getDistinctSplits(InvoiceReconciliationLineItem irli){

	try{

    	BaseVector distinctsplits = new BaseVector();

    	HashMap distinctAccString = new HashMap();


            if(isAccountValidationRequired(irli)){

    			SplitAccountingCollection sac = irli.getAccountings();

    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());

    	        if(sac != null)

    	        {

    	        	BaseVector splits = sac.getSplitAccountings();

    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());



	                for (int i=0; i<splits.size();i++){

		    		SplitAccounting sa = (SplitAccounting)splits.get(i);

		    		String cstctr = (String)sa.getFieldValue("CostCenterText");

		            String genlgr = (String)sa.getFieldValue("GeneralLedgerText");

		            String intord = (String)sa.getFieldValue("InternalOrderText");

		            String wbsele = (String)sa.getFieldValue("WBSElementText");

		            String comcode = null;

		            String sapsrc = null;

		            String acccat = null;

		            ClusterRoot company =(ClusterRoot)irli.getLineItemCollection().getDottedFieldValue("CompanyCode");

		            if(company!=null){

		            comcode = (String)company.getDottedFieldValue("UniqueName");

		            sapsrc = (String)company.getDottedFieldValue("SAPSource");

		            }

		            ClusterRoot acccategory =(ClusterRoot)irli.getDottedFieldValue("AccountCategory");

		            if(acccategory!=null){

		            acccat = (String)acccategory.getDottedFieldValue("UniqueName");

		            }



		    		cstctr = cstctr == null? cstctr : cstctr.toUpperCase();

		    		genlgr = genlgr == null? genlgr : genlgr.toUpperCase();

		    		intord = intord == null? intord : intord.toUpperCase();

		    		wbsele = wbsele == null? wbsele : wbsele.toUpperCase();

		    		comcode = comcode == null? comcode : comcode.toUpperCase();

		    		sapsrc = sapsrc == null? sapsrc : sapsrc.toUpperCase();

		    		acccat = acccat == null? acccat : acccat.toUpperCase();



		            String accString = cstctr + genlgr + intord + wbsele + comcode + sapsrc + acccat;

		            CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,irli);

		            distinctAccString.put(accString,catSplitAccLICombo);

	                }

    	        }

            }





		if(distinctAccString != null)

    	{

    		Log.customer.debug("%s *** : getDistinctSplits inside distinctAccString" ,classname );

	    	Set set = distinctAccString.entrySet();

	    	Iterator i = set.iterator();

	    	while(i.hasNext()){

	    			Map.Entry map = (Map.Entry)i.next();

	    			Log.customer.debug("%s *** : getDistinctSplits : %s" ,classname ,map.getValue());

	    			distinctsplits.add(map.getValue());

	    	}

    	}

		return distinctsplits;

	}

	catch(Exception e){

    	Log.customer.debug("%s *** : getDistinctSplits %s " ,classname, e.toString() );

    	BaseVector distinctsplits = new BaseVector();


            if(isAccountValidationRequired(irli)){

    			SplitAccountingCollection sac = irli.getAccountings();

    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());

    	        if(sac != null)

    	        {

    	        	BaseVector splits = sac.getSplitAccountings();

    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());

	                for (int i=0; i<splits.size();i++){

			    		SplitAccounting sa = (SplitAccounting)splits.get(i);

			    		CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,irli);

			    		distinctsplits.add(catSplitAccLICombo);

	                }

    	        }

            }



		return distinctsplits;

    }

}

public static String checkAccounting(ContractRequest contract) {

	BaseVector splits = getDistinctSplits(contract);
	String formatLineError ="0";
	String lineErrorResult = "0";
	int lineErrors = 0;
	FastStringBuffer lineMsg = new FastStringBuffer();
	for(int j = 0; j < splits.size(); j++)
            {
				int splitErrors = 0;
				int errorSplit = 0;
				FastStringBuffer splitMsg = new FastStringBuffer();

            	Log.customer.debug("%s *** saci has next",classname);
            	CATSplitAccLineItemCombo catSplitAccLICombo = (CATSplitAccLineItemCombo)splits.get(j);
            	Log.customer.debug("%s *** Inside checkAccounting : after getting catSplitAccLICCombo",classname);
                SplitAccounting sa = (SplitAccounting)catSplitAccLICombo.getSa();
                Log.customer.debug("%s *** found sa %s ",classname, sa.toString());
                ContractRequestLineItem rli = (ContractRequestLineItem)catSplitAccLICombo.getPli();
                Log.customer.debug("%s *** found ContractRequestLineItem %s ",classname, rli);
                CatSAPAccountingCollector accclr = getCatSAPAccounting(sa,rli);
                Log.customer.debug("%s *** found accclr %s " ,classname, accclr.toString());

                //Changed as per the new request that is - Do account validation only when the value of CC and GL are there.
				Log.customer.debug("CATSAPUtils : accclr.getCstctr() " + accclr.getCstctr());
				int cstlen = 0;
				int glacclen = 0;
				if(accclr != null && accclr.getCstctr() != null)
				{
					Log.customer.debug("CATSAPUtils : accclr.getCstctr() => " + accclr.getCstctr());
					Log.customer.debug("CATSAPUtils :  accclr.getCstctr().trim().length() => " + accclr.getCstctr().trim().length());
					cstlen = accclr.getCstctr().trim().length();
				}

				if(accclr != null && accclr.getGenlgr() != null)
				{
					glacclen = accclr.getGenlgr().trim().length();
				}
                //Changed as per the new request that is - Do account validation only when the value of CC and GL are there.

				Log.customer.debug("CATSAPUtils : cstlen => " + cstlen);
				if(accclr != null && (cstlen > 0 || !accclr.getAcccat().equalsIgnoreCase("K")) && glacclen > 0)
                {
                    Log.customer.debug("%s *** accclr not null",classname);
                    CatSAPAccountingValidator response = null;
                    try
                    {
                        Log.customer.debug("%s *** inside try 1",classname);
                        response = SAPAccountValidator.validateAccount(accclr);
                        Log.customer.debug("%s *** passed cats to validateaccount");
                        Log.customer.debug("%s *** response " + response.toString());
                        if(response != null)
                        {
                            Log.customer.debug("%s *** ResultCode: %s" , classname, response.getResultCode());
                            Log.customer.debug("%s *** Message: %s", classname, response.getMessage());
                            FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
                           //Addded by Sandeep as a part of MACH1 2.5 for accepting Code 020 as Valid
                            if(response.getResultCode().equals("000") || response.getResultCode().equals("020"))
                            {
                           Log.customer.debug("Response Code 020 or 000 to set Validate Accounting Message property as null - Sandeep");
                            	sa.setFieldValue("ValidateAccountingMessage",null);
                            }else
                            {
								splitErrors += 1;
								lineErrors +=1;
								errorSplit = sa.getNumberInCollection();
								String splitErrorResult = " Line "+ rli.getNumberInCollection() +":(Account Distribution #" +errorSplit +")" + response.getMessage();
								splitMsg.append(splitErrorResult + ".  ");
								Log.customer.debug("%s *** Message: splitMsg.toString() %s", classname, splitMsg.toString());

                            	lineErrorResult = splitMsg.toString();
                            	lineMsg.append(lineErrorResult);
                            	Log.customer.debug("%s *** Message: lineMsg.toString() %s", classname, lineMsg.toString());
                            	formatLineError = lineMsg.toString();
                            	Log.customer.debug("%s *** Message: formatLineError %s", classname, formatLineError);
                  			}
                        }

                        }
                    catch(Exception e)
                    {
                    	return formatLineError;
                    }
                }
            }

	return formatLineError;
}

public static boolean isAccountValidationRequired(ContractRequestLineItem pli){

	Log.customer.debug("%s *** inside  isAccountValidationRequired pli %s ", classname,pli);

	String acccat = null;
	String comcode = null;
	String sapsrc = null;
	String isGLEditableForCapital = null;
	String isGLEditableForInterCompany = null;
	Log.customer.debug("%s *** isAccountValidationRequired pli %s ", classname,pli);
	if(pli == null){
	return false;
	}
	Log.customer.debug("%s *** isAccountValidationRequired pli.getLineItemCollection() %s ", classname, pli.getLineItemCollection());
	if(pli.getLineItemCollection() == null){
		return false;
	}
	ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");
	Log.customer.debug("%s *** isAccountValidationRequired company %s ", classname,company);
    if(company!=null){
    comcode = (String)company.getDottedFieldValue("UniqueName");
    sapsrc = (String)company.getDottedFieldValue("SAPSource");
    isGLEditableForCapital = (String)company.getDottedFieldValue("isGLEditableForCapital");
    isGLEditableForInterCompany = (String)company.getDottedFieldValue("isGLEditableForInterCompany");
    }
    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");
    if(acccategory!=null){
    acccat = (String)acccategory.getDottedFieldValue("UniqueName");
    }
    //Account validation is not required for Milestone item and global contract.
    String contractAvailability = (String)pli.getDottedFieldValue("LineItemCollection.ContractAvailability");
    if (contractAvailability!=null && contractAvailability.equals("Global")){
    	Log.customer.debug("%s *** isAccountValidationRequired : Account validation not required for %s Contract ", classname, contractAvailability);
    	return false;
    }

    ProcureLineType lineType = (ProcureLineType)pli.getFieldValue("LineType");
    String lineTypeUniqueName =(String)lineType.getUniqueName();
    if(lineTypeUniqueName.equals("_MilestoneItem")){
    	Log.customer.debug("%s *** isAccountValidationRequired : Account validation not required for %s Milestone ", classname, lineTypeUniqueName);
    	return false;
    }

    if(acccat != null && acccat.equalsIgnoreCase("K")){
	return true;
    }

	//Vikram (Issue 325/MGPP1719) Starts - New Acct type S
	else if(acccat != null && acccat.equalsIgnoreCase("S")){

	return true;

    }// Vikram (Issue 325/MGPP1719) Ends
    //Changed as per the new Request that is-To enable budget check and remove accounting validation for account category F when GL is editable

else if((acccat != null && acccat.equalsIgnoreCase("P"))
                &&(isGLEditableForInterCompany != null && isGLEditableForInterCompany.equalsIgnoreCase("Y"))){
        return true;
    }

 else if((acccat != null &&  acccat.equalsIgnoreCase("F") && sapsrc.equalsIgnoreCase("MACH1") )
    		&&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){
    	return true;
	}
//PK code chnages start

	else if((acccat != null && (acccat.equalsIgnoreCase("F")) && sapsrc.equalsIgnoreCase("CGM"))

                &&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

        return true;
	}
	else if((acccat != null && (acccat.equalsIgnoreCase("P")) && sapsrc.equalsIgnoreCase("CGM"))

                &&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){

        return true;
	}	
//PK code chnages end	
    else if((acccat != null && acccat.equalsIgnoreCase("Z"))
    		&&(isGLEditableForInterCompany != null && isGLEditableForInterCompany.equalsIgnoreCase("Y"))){
    	return true;
    }
    return false;
}

public static CatSAPAccountingCollector getCatSAPAccounting(SplitAccounting sa, ContractRequestLineItem pli)
{
    Log.customer.debug("%s *** inside get cat actng: sa :%s " ,classname, sa.toString());
    Log.customer.debug("%s *** inside get cat actng: pli :%s " ,classname, pli);
    String cstctr = (String)sa.getFieldValue("CostCenterText");
    String genlgr = (String)sa.getFieldValue("GeneralLedgerText");
    String intord = (String)sa.getFieldValue("InternalOrderText");
    String wbsele = (String)sa.getFieldValue("WBSElementText");
    String comcode = null;
    String sapsrc = null;
    String acccat = null;
    Log.customer.debug("%s *** getcatactng pli.getLineItemCollection() %s", classname, pli.getLineItemCollection());
    ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");
    if(company!=null){
    comcode = (String)company.getDottedFieldValue("UniqueName");
    sapsrc = (String)company.getDottedFieldValue("SAPSource");
    }
    ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");
    if(acccategory!=null){
    acccat = (String)acccategory.getDottedFieldValue("UniqueName");
    }
    Log.customer.debug("%s *** returning from getcatactng ", classname);
    return new CatSAPAccountingCollector(cstctr, genlgr, intord, wbsele, comcode, sapsrc, acccat);
}

public static BaseVector getDistinctSplits(ContractRequest contract){
	try{
		Log.customer.debug("%s ***  inside found getDistinctSplits %s " ,classname, contract);
    	BaseVector distinctsplits = new BaseVector();
    	HashMap distinctAccString = new HashMap();
		BaseVector lines = contract.getLineItems();
		 Log.customer.debug("%s *** After getting the Contract Line Item vector %s " ,classname, contract);

		for (int line = 0; line < lines.size(); line++) {
			 Log.customer.debug("%s *** before type casting ReqLine Item to contract Line Item %s " ,classname, contract);
			ContractRequestLineItem rli = (ContractRequestLineItem)lines.get(line);
			 Log.customer.debug("%s *** After type casting ReqLine Item to contract Line Item %s " ,classname, contract);

            if(isAccountValidationRequired(rli)){
    			SplitAccountingCollection sac = rli.getAccountings();
    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());
    	        if(sac != null)
    	        {
    	        	BaseVector splits = sac.getSplitAccountings();
    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());

	                for (int i=0; i<splits.size();i++){
		    		SplitAccounting sa = (SplitAccounting)splits.get(i);
		    		String cstctr = (String)sa.getFieldValue("CostCenterText");
		            String genlgr = (String)sa.getFieldValue("GeneralLedgerText");
		            String intord = (String)sa.getFieldValue("InternalOrderText");
		            String wbsele = (String)sa.getFieldValue("WBSElementText");
		            String comcode = null;
		            String sapsrc = null;
		            String acccat = null;
		            ClusterRoot company =(ClusterRoot)rli.getLineItemCollection().getDottedFieldValue("CompanyCode");
		            if(company!=null){
		            comcode = (String)company.getDottedFieldValue("UniqueName");
		            sapsrc = (String)company.getDottedFieldValue("SAPSource");
		            }
		            ClusterRoot acccategory =(ClusterRoot)rli.getDottedFieldValue("AccountCategory");
		            if(acccategory!=null){
		            acccat = (String)acccategory.getDottedFieldValue("UniqueName");
		            }

		    		cstctr = cstctr == null? cstctr : cstctr.toUpperCase();
		    		genlgr = genlgr == null? genlgr : genlgr.toUpperCase();
		    		intord = intord == null? intord : intord.toUpperCase();
		    		wbsele = wbsele == null? wbsele : wbsele.toUpperCase();
		    		comcode = comcode == null? comcode : comcode.toUpperCase();
		    		sapsrc = sapsrc == null? sapsrc : sapsrc.toUpperCase();
		    		acccat = acccat == null? acccat : acccat.toUpperCase();

		            String accString = cstctr + genlgr + intord + wbsele + comcode + sapsrc + acccat;
		            CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,rli);
		              Log.customer.debug("%s *** IngetDistinctSplits : After getting  catSplitAccLICombo %s " ,classname, splits.toString());
		            distinctAccString.put(accString,catSplitAccLICombo);
	                }
    	        }
            }
		}

		if(distinctAccString != null)
    	{
    		Log.customer.debug("%s *** : getDistinctSplits inside distinctAccString" ,classname );
	    	Set set = distinctAccString.entrySet();
	    	Iterator i = set.iterator();
	    	while(i.hasNext()){
	    			Map.Entry map = (Map.Entry)i.next();
	    			Log.customer.debug("%s *** : getDistinctSplits : %s" ,classname ,map.getValue());
	    			distinctsplits.add(map.getValue());
	    	}
    	}
		return distinctsplits;
	}
	catch(Exception e){
    	Log.customer.debug("%s *** : getDistinctSplits %s " ,classname, e.toString() );
    	BaseVector distinctsplits = new BaseVector();
    	BaseVector lines = contract.getLineItems();
		for (int line = 0; line < lines.size(); line++) {
			ContractRequestLineItem rli = (ContractRequestLineItem)lines.get(line);

            if(isAccountValidationRequired(rli)){
    			SplitAccountingCollection sac = rli.getAccountings();
    	        Log.customer.debug("%s *** found sac %s " ,classname, sac.toString());
    	        if(sac != null)
    	        {
    	        	BaseVector splits = sac.getSplitAccountings();
    	            Log.customer.debug("%s *** found saci %s " ,classname, splits.toString());
	                for (int i=0; i<splits.size();i++){
			    		SplitAccounting sa = (SplitAccounting)splits.get(i);
			    		CATSplitAccLineItemCombo catSplitAccLICombo = new CATSplitAccLineItemCombo(sa,rli);
			    		Log.customer.debug("%s *** :Inside catch block :getDistinctSplits: After getting catSplitAccLICombo  %s " ,classname, e.toString() );
			    		distinctsplits.add(catSplitAccLICombo);
	                }
    	        }
            }
		}
		return distinctsplits;
    }
}

public static boolean isValidToSubmit(ProcureLineItemCollection pic)
{
	BaseVector lineItems = (BaseVector) pic.getDottedFieldValue("LineItems");

        // S. Sato - AUL - Verified API change for 9r
        // method changed from getValidationGroup() to
        // getObjectValidationGroup()
    String group = pic.getObjectValidationGroup();
	LineItem lineItem = null;
	Map errHashtable = null;

	for(int i=0;i<lineItems.size();i++)

	{
		lineItem = (LineItem) lineItems.get(i);
		errHashtable = lineItem.getInvalidFields(group, ariba.user.core.User.getEffectiveUser());
		if(errHashtable != null)
		{
			Log.customer.debug("isValidToSubmit debug:   errHashtable = "+ errHashtable);
			return false;
		}
	}
	return true;
}

public static boolean validateIRLineAccounting(InvoiceReconciliationLineItem irli) {

    Log.customer.debug("\n %s ::: ENTERING validateIRLineAccounting()! \n", classname);

    boolean isValid = true;

	BaseVector splits = getDistinctSplits(irli);

	for(int j = 0; j < splits.size(); j++)

            {

            	Log.customer.debug("%s *** saci has next",classname);

            	CATSplitAccLineItemCombo catSplitAccLICombo = (CATSplitAccLineItemCombo)splits.get(j);

                SplitAccounting sa = (SplitAccounting)catSplitAccLICombo.getSa();

                Log.customer.debug("%s *** found sa %s ",classname, sa.toString());

                InvoiceReconciliationLineItem rli = (InvoiceReconciliationLineItem)catSplitAccLICombo.getPli();

                Log.customer.debug("%s *** found ReqLineItem %s ",classname, rli);

                CatSAPAccountingCollector accclr = getCatSAPAccounting(sa,rli);

                Log.customer.debug("%s *** found accclr %s " ,classname, accclr.toString());

                if(accclr != null)

                {

                    Log.customer.debug("%s *** accclr not null",classname);

                    CatSAPAccountingValidator response = null;

                    try

                    {

                        Log.customer.debug("%s *** inside try 1",classname);

                        response = SAPAccountValidator.validateAccount(accclr);

                        Log.customer.debug("%s *** passed cats to validateaccount");

                        Log.customer.debug("%s *** response " + response.toString());

                        if(response != null)

                        {

                            Log.customer.debug("%s *** ResultCode: %s" , classname, response.getResultCode());

                            Log.customer.debug("%s *** Message: %s", classname, response.getMessage());

                           // FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");

                            if(!response.getResultCode().equals("000") && !response.getResultCode().equals("020") )
                            {
                        Log.customer.debug("Setting the isValid as false or Reponse Code other than 000 or 020 - Sandeep");
                            	isValid = false;

                            }

                        }

                        }

                    catch(Exception e)
                    {
                    	isValid=false;

                    }

                }

            }



    Log.customer.debug("\n ::: EXITING validateIRLineAccounting()! isValid? " + isValid);
    return isValid;
}
	
}

