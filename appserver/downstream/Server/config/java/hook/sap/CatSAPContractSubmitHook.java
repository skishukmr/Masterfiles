/*
 * Created by Madhuri  on Nov 06, 2008
 */
package config.java.hook.sap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.base.core.BaseVector;
import ariba.base.core.MultiLingualString;
import ariba.basic.core.Currency;
import ariba.basic.core.Money;
import ariba.basic.core.UnitOfMeasure;
import ariba.common.core.CommodityExportMapEntry;
import ariba.contract.core.ContractRequest;
import ariba.contract.core.ContractRequestLineItem;
import ariba.procure.core.LineItemProductDescription;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;
import config.java.common.sap.BudgetChkResp;
import config.java.common.sap.CATBudgetCheck;
import config.java.common.sap.CATSAPUtils;
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;


/*   This hook performs Account Validation and Budget Check */

public class CatSAPContractSubmitHook implements ApprovableHook {

    private static final String classname = "CatSAPContractSubmitHook : ";
    private static final String LINEMARKER = ResourceService.getString("cat.java.vcsv1","SubmitHookLineMarker_Default");
	private static String ErrorMsg = ResourceService.getString("cat.java.common", "HeaderSupplierCurrencyMismatch");
	private static String errorMsgForItem = ResourceService.getString("cat.java.common","MessageIsNotReceivableInvalid");
    private static final String SubmitMessageInvalidField = ResourceService.getString("cat.java.sap", "SubmitMessageInvalidField");

    private static final List NoErrorResult = ListUtil.list(Constants.getInteger(0));
	FastStringBuffer totalMsg = new FastStringBuffer ();
	boolean hasErrors = false;
	boolean hasCurrencyErrors = false;
	private static String CurrencyError;
	String error = "";
	boolean hasHazMaterialErrors = false;


	public List run(Approvable approvable) {

		Log.customer.debug("%s *** First Statement: %s", classname,approvable );

		if (!(approvable instanceof ariba.contract.core.ContractRequest)){
			return NoErrorResult;
		}

		ContractRequest contract = (ContractRequest) approvable;

		// Check all the fields are valid or not before webservice call.
	    if(!CATSAPUtils.isValidToSubmit(contract))
		{
			Log.customer.debug("CatSAPReqSubmitHook : Requisition has line items with invalid/missed information");
			return ListUtil.list(Constants.getInteger(-1), SubmitMessageInvalidField);
		}


		// Accounting Validation
			String reqresult = CATSAPUtils.checkAccounting(contract);
			if (!reqresult.equals("0")) {
				//String formatLineError = Fmt.S(lineresult, errorLine);
				hasErrors = true;
				totalMsg.append(reqresult);
				Log.customer.debug("%s *** Req Error Msg: %s", classname, totalMsg.toString());
			}

			if(hasErrors){
				return ListUtil.list(Constants.getInteger(-1), totalMsg.toString());
			}

		// Accounting Validation

		// Validate Multiple Currency on Contract
		/* CR-C10A  Contract submit hook should verify that all currencies on the contract are same */
		singleCurrencyCheck(contract);
		if(hasCurrencyErrors){
			return ListUtil.list(Constants.getInteger(-1), totalMsg.toString());
		}
		// End of multiple currency check on contracts

		// Hazardous materials for NO release receivable contracts are not allowed

		hazMaterialCheck(contract);
		if(hasHazMaterialErrors){
			return ListUtil.list(Constants.getInteger(-1), totalMsg.toString());
		}


		if(CATBudgetCheck.isBudgetCheckReq(contract)){

			BudgetChkResp budgetChkResp = null;

			CATBudgetCheck catBudgetCheck = new CATBudgetCheck();

			try {

				budgetChkResp = catBudgetCheck.performBudgetCheck(contract);

			} catch(Exception excp){

				Log.customer.debug("CatSAPContractSubmitHook : run : excp " + excp);

				return ListUtil.list(Constants.getInteger(-1), "BudgetCheck Processing Error. Please contact System Administrator");

			}

			String BUDGET_CHECK_PASS_CODE = "000";

			if (budgetChkResp == null){
				return ListUtil.list(Constants.getInteger(-1), "BudgetCheck response is null. Please contact System Administrator");
			}

			if(!(BUDGET_CHECK_PASS_CODE.equals(budgetChkResp.getBudgetCheckMsgCode()))){
				return ListUtil.list(Constants.getInteger(-1), budgetChkResp.getBudgetCheckMsgTxt());
			}

		}


        List lines = (List)contract.getFieldValue("LineItems");
        int size = ListUtil.getListSize(lines);

        for(int i = 0; i < size; i++)
        {
            ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(i);
            if(mali!=null)
            	SetReferenceLineNumber(mali);
        }

		return NoErrorResult;
	}

	public CatSAPContractSubmitHook() {
		super();
	}

	private static void SetReferenceLineNumber(ContractRequestLineItem mali){
		int refNum = 0;
		if(!CatSAPAdditionalChargeLineItem.isAdditionalCharge(mali))
		{
			refNum = mali.getNumberInCollection();
			Log.customer.debug("CatSetReferenceLineNumber2: non-AC number in collection"+ refNum);
			Log.customer.debug("CatSetReferenceLineNumber *** refNum: " + refNum);
			mali.setFieldValue("ReferenceLineNumber", new Integer(refNum));
			Log.customer.debug("%s *** getRefNum: %s", "CatSetReferenceLineNumber", mali.getFieldValue("ReferenceLineNumber"));
		}
	}


    public void singleCurrencyCheck(ContractRequest contract){
	boolean currencyCheck;
	Currency headerCurrency;
	List lines = (List)contract.getFieldValue("LineItems");
	headerCurrency = (Currency) contract.getDottedFieldValue("Currency");
	 Log.customer.debug("%s *** Inside singleCurrencyCheck :headerCurrency" ,classname,headerCurrency);

	 Log.customer.debug("%s *** Inside singleCurrencyCheck : reading Lines" ,classname);
            //int size = lines.size();
            int size = ListUtil.getListSize(lines);
            Log.customer.debug("%s *** Inside singleCurrencyCheck : Lines Size %s" ,classname,size);
           	for(int i = 0; i < size; i++){
				 currencyCheck = false;
                ContractRequestLineItem mali = (ContractRequestLineItem)lines.get(i);
                Log.customer.debug("%s *** Inside singleCurrencyCheck :MALine Item  %s " ,classname, mali);
                if(!isCurrencyMatch(mali,headerCurrency) && CurrencyError != null){
					currencyCheck = true;
					Log.customer.debug("Inside singleCurrencyCheck currencyCheck value:"+currencyCheck);
					totalMsg.append(Fmt.S(LINEMARKER, String.valueOf(mali.getLineItemNumber())));
					totalMsg.append(CurrencyError);
					totalMsg.append(". ");
                }
                if(currencyCheck){
					hasCurrencyErrors = true;
				}
			}
	}
	public static boolean isCurrencyMatch(ContractRequestLineItem pli,Currency headerCurrency){
		CurrencyError = null;
		boolean isMatch = true;
		if(pli != null)	{
				if(headerCurrency != null)	{
					String curr1Value = (String)headerCurrency.getDottedFieldValue("UniqueName");
					Log.customer.debug("%s *** currency1 UniqueName: %s", classname, curr1Value);
					Log.customer.debug("%s *** currency1: %s", classname, headerCurrency);
					LineItemProductDescription lipd = pli.getDescription();
					if(lipd != null)	{
						Money price = lipd.getPrice();
						if(price != null && !price.isApproxZero()){
							Currency curr2 = price.getCurrency();
							Log.customer.debug("%s *** currency2: %s", classname, curr2);
							if(curr2 != null){
								String curr2Value = (String) curr2.getDottedFieldValue("UniqueName");
								Log.customer.debug("%s *** currency2: %s", classname, curr2Value);
								if(!curr2.equals(headerCurrency)){
									Log.customer.debug("Inside Currency1 != Currency2");
									isMatch = false;
								}
							}
							MultiLingualString curr1Name = headerCurrency.getName();
							if(curr1Name != null)
								CurrencyError = Fmt.S(ErrorMsg, headerCurrency.getUniqueName(), curr1Name.getPrimaryString());
							else
								CurrencyError = Fmt.S(ErrorMsg, headerCurrency.getUniqueName(), headerCurrency.getUniqueName());
						}
					}
				}
			}
		Log.customer.debug("*** isCurrencyMatch: %s " +isMatch);
		return isMatch;
	}
	public void hazMaterialCheck(ContractRequest mar){
		//If It is Item Level Hazmat Contract and IsReceivable false
		if ( ( ((Integer)mar.getDottedFieldValue("TermType")).intValue() == 2) && ( ((Integer)mar.getDottedFieldValue("ReleaseType")).intValue() == 0) && ( ((Boolean)mar.getDottedFieldValue("IsReceivable")).booleanValue() == false) ){
			//String errorMsgForItem = ResourceService.getString("aml.cat.ui1","MessageIsNotReceivableInvalid");
			Log.customer.debug("%s **** It is Item Level and Non-Receivable, checking Is HAZMAT!!", "CatIsValidReceivable");

			if(mar != null)	{
				BaseVector bv = (BaseVector)mar.getLineItems();
				if (bv != null)	{
					for (int i=0; i< bv.size(); i++){
						ContractRequestLineItem pli = (ContractRequestLineItem)bv.get(i);
						if(isHazmat(pli)){
							Log.customer.debug("%s **** Item Level Hazmat MAR!!", "ContractSubmitHook");
							Log.customer.debug("%s *** evaluateAndExplain error: %s", "ContractSubmitHook", errorMsgForItem);
							hasHazMaterialErrors = true;
							totalMsg.append(errorMsgForItem);
							break;
						}
					}
				}
			}
		}
   }
    public static boolean isHazmat(ContractRequestLineItem pli){
   		boolean hazmat = false;
   		if(pli != null){
   			String msds = (String)pli.getFieldValue("MSDSNumber");
   			Log.customer.debug("%s **** MSDS#: %s", "ContractSubmitHook", msds);
   			if(!StringUtil.nullOrEmptyOrBlankString(msds)){
   				Log.customer.debug("%s **** IsHazmat - Non-Null MSDS", "ContractSubmitHook");
   				hazmat = true;
   			} else
   			if(pli.getIsFromCatalog() && !pli.getIsAdHoc()){
   				Boolean isHazardous = (Boolean)pli.getDottedFieldValue("Description.HazardousMaterials");
   				Log.customer.debug("%s **** Catalog Item, HazardousMaterials? %s", "ContractSubmitHook", isHazardous);
   				if(isHazardous != null && isHazardous.booleanValue()){
   					Log.customer.debug("%s **** IsHazmat - Catalog Hazardous!", "ContractSubmitHook");
   					hazmat = true;
   				}
   			} else	{
   				CommodityExportMapEntry ceme = pli.getCommodityExportMapEntry();
   				if(ceme != null){
					Boolean hazmatCEME = (Boolean)ceme.getFieldValue("IsHazmat");
   					Log.customer.debug("%s **** hazmat CEME: ", "ContractSubmitHook" + hazmatCEME);
   					if(hazmatCEME != null && hazmatCEME.booleanValue()){
   						Log.customer.debug("%s **** IsHazmat - CEME!", "ContractSubmitHook");
   						hazmat = true;
   					}
   				}
   				if(!hazmat){
   					UnitOfMeasure uom = pli.getDescription().getUnitOfMeasure();
   					if(uom != null)		{
   						String uomUN = uom.getUniqueName();
   						List uomList = buildValueListFromFile("config/variants/SAP/data/CATHazmatUnitOfMeasure.csv");
   						Log.customer.debug("%s **** UOM List: %s", "ContractSubmitHook", uomList);
   						if(uomList != null)	{
   							int size = uomList.size();
   							Log.customer.debug("ContractSubmitHook **** size (before): " + size);
   							while(size > 0)	{
   								String value = (String)uomList.get(--size);
   								if(uomUN.equals(value)){
   									Log.customer.debug("%s **** IsHazmat - UOM!", "ContractSubmitHook");
   									hazmat = true;
   									break;
   								}
   							}
   						}
   					}
   				}
   			}
   		}
   		return hazmat;
	}
	public static ArrayList buildValueListFromFile(String filename){
			ArrayList valueList = null;
			if(!StringUtil.nullOrEmptyOrBlankString(filename)){
				File file = new File(filename);
				Log.customer.debug("%s *** file: %s", "CatSetHazmatIndicator", file);
				if(file != null)
					try	{
						BufferedReader br = new BufferedReader(new FileReader(file));
						Log.customer.debug("%s *** br: %s", "CatSetHazmatIndicator", br);
						String line = null;
						valueList = new ArrayList();
						while((line = br.readLine()) != null){
							List values = CatCommonUtil.parseParamString(line);
							valueList.add(values.get(0));
						}
						Log.customer.debug("CatSetHazmatIndicator *** valuelist.size(): " + valueList.size());
						br.close();
					}
					catch(IOException e){
						Log.customer.debug("CatSetHazmatIndicator *** IOException: %s", "CatSetHazmatIndicator", e);
					}
			}
			return valueList;
	}


}
