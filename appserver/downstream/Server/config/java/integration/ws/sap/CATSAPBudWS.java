/*************************************************************************************************
 *   Created by: James S Pagadala Oct-05-2008
 *   Changed by Sandeep Vaishnav on 08/10/2010 as a part of MACH1 2.5 changes.
 *
 *
 *************************************************************************************************/

package config.java.integration.ws.sap;

import java.math.BigDecimal;
import java.net.URL;
import mc_style.functions.soap.sap.document.sap_com.ZWSFIFM_CHECK_BUDGETSoapBindingStub;
import ariba.util.log.Log;
import config.java.common.sap.BudgetChkResp;
import functions.rfc.sap.document.sap_com.Char24;
import functions.rfc.sap.document.sap_com.Char4;
import functions.rfc.sap.document.sap_com.Cuky5;
import functions.rfc.sap.document.sap_com.Curr152;
import functions.rfc.sap.document.sap_com.ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub;
import functions.rfc.sap.document.sap_com.ZWS_ACCOUNT_VALIDATIONStub;
import functions.rfc.sap.document.sap_com.ZWS_BUDGET_AVAILABILITYStub;
import functions.rfc.sap.document.sap_com._ZFIAP_BUDGET_AVAILABILITY_RFC_IM_AUFNR;
import functions.rfc.sap.document.sap_com._ZFIAP_BUDGET_AVAILABILITY_RFC_IM_BUKRS;
import functions.rfc.sap.document.sap_com._ZFIAP_BUDGET_AVAILABILITY_RFC_IM_GWERT;
import functions.rfc.sap.document.sap_com._ZFIAP_BUDGET_AVAILABILITY_RFC_IM_KNTTP;
import functions.rfc.sap.document.sap_com._ZFIAP_BUDGET_AVAILABILITY_RFC_IM_WAERS;
import functions.rfc.sap.document.sap_com.holders.Char3Holder;
import functions.rfc.sap.document.sap_com.holders.Char70Holder;
import functions.rfc.sap.document.sap_com.Char1;
import functions.rfc.sap.document.sap_com.holders._ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSCODHolder;
import functions.rfc.sap.document.sap_com.holders._ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSTXTHolder;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.

    Set the default parameters to "". It was previously pointing to prod params
*/
public class CATSAPBudWS
{

public CATSAPBudWS()
    {
  Log.customer.debug("Getting Started");
   }

	public static BudgetChkResp checkBudgetMACH1WS(BigDecimal _totClaimAgaBudget, String _compCode, String _compCodeCurr, String _projElement, String _acctCategory) throws Exception
     {
           // String mach1EndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/BudgetAvailability_Ariba_SAPMach1Web/sca/ZWSFIFM_CHECK_BUDGET";
		   String mach1EndPointDefault = "";
                String mach1EndPointStr = ResourceService.getString("cat.java.sap","MACH1BudgetCheckURL");
                String mach1EndPoint = ((!StringUtil.nullOrEmptyOrBlankString(mach1EndPointStr))?mach1EndPointStr:mach1EndPointDefault) ;

                Log.customer.debug("CATSAPBudWS : checkBudgetWS : mach1EndPointStr : " +mach1EndPointStr);
                Log.customer.debug("CATSAPBudWS : checkBudgetWS : mach1EndPoint : " +mach1EndPoint);

            Log.customer.debug("Under the MACH1 Budget Check - Sandeep ***Step 1***");
           BudgetChkResp budChkResp = new BudgetChkResp();

          Log.customer.debug("CATSAPBudWS : checkBudgetWS : mach1EndPoint : " +mach1EndPoint);

               Curr152 totClaimAgaBudget = new Curr152();
   Char4 compCode = new Char4();
   Cuky5 compCodeCurr = new Cuky5();
   Char24 projElement = new Char24();
   Char3Holder respCode = new Char3Holder();
    Char70Holder respMessage = new Char70Holder();
    Char1 acccat = new Char1();

 		totClaimAgaBudget.setValue(_totClaimAgaBudget);
            Log.customer.debug("Total Clamin amt is - ***Step 2 MACH1 BudgetCheck *** " +_totClaimAgaBudget);
		compCode.setValue(_compCode);
            Log.customer.debug("Company Code is ***Step 3 MACH1 BudgetCheck *** :" +_compCode);
		compCodeCurr.setValue(_compCodeCurr);
            Log.customer.debug("CompanyCode Currency is ***Step 4 MACH1 BudgetCheck*** :" +_compCodeCurr);
		projElement.setValue(_projElement);
            Log.customer.debug("Project Element used is *** Step 5 MACh1 BudgetCheck *** :" +_projElement);
                 acccat.setValue(_acctCategory);
           Log.customer.debug("Account Category Passed is:" +_acctCategory);
            respCode = new Char3Holder();
            respMessage = new Char70Holder();
          URL  endpoint = new URL(mach1EndPoint);
//    ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub  stub = new ZFIFM_INT_BUDGET_AVAILSoapBindingStub(endpoint, null);
     ZWSFIFM_CHECK_BUDGETSoapBindingStub stub = new ZWSFIFM_CHECK_BUDGETSoapBindingStub(endpoint, null);
        	stub.zfifmIntBudgetAvailability(totClaimAgaBudget, compCode, compCodeCurr, acccat, projElement,respCode,respMessage);
            Log.customer.debug(" *** Step 6 *** MACH1 BudgetCheck respCode : " +respCode);
            Log.customer.debug(" *** Step 7 *** MACH1 BudgetCheck respMessage is :" +respMessage);
            Log.customer.debug(" *** Step 8 *** MACH1 BudgetCheck respCode.getClass() is %s:", respCode.getClass());
            Log.customer.debug(" *** Step 9 *** MACH1 BudgetCheck   respCode.value is : " +respCode.value);
            Log.customer.debug(" *** Step 10 *** MACH1 BudgetCheck  respCode.value.toString() is :" +respCode.value.toString());

             if(respCode!=null && respMessage !=null){

    			budChkResp.setBudgetCheckMsgCode(respCode.value.toString());

    			budChkResp.setBudgetCheckMsgTxt(respMessage.value.toString());
            } else
            {
                Log.customer.debug("*** is null - stub validate");
            }

           Log.customer.debug("Return value for MACH1 BudgetCheck ***END***");
       return  budChkResp;
     }

      /* PK code changes Start */

	public static BudgetChkResp checkBudgetCGMWS(BigDecimal _totClaimAgaBudget, String _compCode, String _compCodeCurr, String _projElement, String _acctCategory) throws Exception
	{
		// String mach1EndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/BudgetAvailability_Ariba_SAPMach1Web/sca/ZWSFIFM_CHECK_BUDGET";
		String cgmEndPointDefault = "";
		String cgmEndPointStr = ResourceService.getString("cat.java.sap","CGMBudgetCheckURL");
		//cgmEndPointStr = "http://172.16.51.83:8000/sap/bc/srt/rfc/sap/zws_budget_availability/300/zws_budget_availability/zws_budget_availability";
		String cgmEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(cgmEndPointStr))?cgmEndPointStr:cgmEndPointDefault) ;

		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : cgmEndPointStr : " +cgmEndPointStr);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : cgmEndPoint : " +cgmEndPoint);

		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : ***Step 1***");
		BudgetChkResp budChkResp = new BudgetChkResp();

		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : cgmEndPoint : " +cgmEndPoint);

		ZWS_BUDGET_AVAILABILITYStub.Curr152 totClaimAgaBudget = new ZWS_BUDGET_AVAILABILITYStub.Curr152();
		ZWS_BUDGET_AVAILABILITYStub.Char4 compCode = new ZWS_BUDGET_AVAILABILITYStub.Char4();
		ZWS_BUDGET_AVAILABILITYStub.Cuky5 compCodeCurr = new ZWS_BUDGET_AVAILABILITYStub.Cuky5();
		ZWS_BUDGET_AVAILABILITYStub.Char12 projElement = new ZWS_BUDGET_AVAILABILITYStub.Char12();
		ZWS_BUDGET_AVAILABILITYStub.Char3 respCode = new ZWS_BUDGET_AVAILABILITYStub.Char3();
		ZWS_BUDGET_AVAILABILITYStub.Char70 respMessage = new ZWS_BUDGET_AVAILABILITYStub.Char70();
		ZWS_BUDGET_AVAILABILITYStub.Char1 acccat = new ZWS_BUDGET_AVAILABILITYStub.Char1();

		totClaimAgaBudget.setCurr152(_totClaimAgaBudget);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : Total Clamin amt is - ***Step 2*** " +_totClaimAgaBudget);
		compCode.setChar4(_compCode);
		Log.customer.debug("Company Code is ***Step 3 CGM BudgetCheck *** :" +_compCode);
		compCodeCurr.setCuky5(_compCodeCurr);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : CompanyCode Currency is ***Step 4*** :" +_compCodeCurr);
		projElement.setChar12(_projElement);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : Project Element used is *** Step 5 *** :" +_projElement);
		acccat.setChar1(_acctCategory);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : Account Category Passed is:" +_acctCategory);

		ZWS_BUDGET_AVAILABILITYStub.ZFIFM_INT_BUDGET_AVAILABILITY request = new ZWS_BUDGET_AVAILABILITYStub.ZFIFM_INT_BUDGET_AVAILABILITY();
		request.setIM_AMOUNT(totClaimAgaBudget);
		request.setIM_AUFNR(projElement);
		request.setIM_BUKRS(compCode);
		request.setIM_CURRENCY(compCodeCurr);
		request.setIM_KNTTP(acccat);

		URL  endpoint = new URL(cgmEndPoint);
		//   ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub  stub = new ZFIFM_INT_BUDGET_AVAILSoapBindingStub(endpoint, null);
		ZWS_BUDGET_AVAILABILITYStub stub = new ZWS_BUDGET_AVAILABILITYStub(null, cgmEndPoint);
		ZWS_BUDGET_AVAILABILITYStub.ZFIFM_INT_BUDGET_AVAILABILITYResponse response = stub.zFIFM_INT_BUDGET_AVAILABILITY(request);
		respCode = response.getEX_MSG_CODE();
		respMessage = response.getEX_MSG_TXT();
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS :  *** Step 6 *** respCode : " +respCode);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS :  *** Step 7 *** respMessage is :" +respMessage);
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS :  *** Step 8 *** respCode value is %s:", respCode.getChar3());
		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS :  *** Step 9 *** respMessage value is %s:", respMessage.getChar70());

		if(respCode!=null && respMessage !=null){

			budChkResp.setBudgetCheckMsgCode(respCode.getChar3());

			budChkResp.setBudgetCheckMsgTxt(respMessage.getChar70());
		} else
		{
			Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : *** respCode or respMessage null - stub validate");
		}

		Log.customer.debug("CATSAPBudWS : checkBudgetCGMWS : Return value for CGM BudgetCheck ***END***");
		return  budChkResp;
	}
	 /* PK code changes End */

public static BudgetChkResp checkBudgetCBSWS(BigDecimal _totClaimAgaBudget, String _compCode, String _compCodeCurr, String _projElement, String _acctCategory) throws Exception
     {
         Log.customer.debug("Under the CBS Budget Check - Sandeep ***Step 1 *** " );

         // String cbsEndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/BudgetAvailability_Ariba_CBSWeb/sca/ZFIAP_BUDGET_AVAILABILITY_RFCPortType";
         String cbsEndPointDefault = "";
                String cbsEndPointStr = ResourceService.getString("cat.java.sap","CBSBudgetCheckURL");
                String cbsEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(cbsEndPointStr))?cbsEndPointStr:cbsEndPointDefault) ;

                Log.customer.debug("CATSAPBudWS : checkBudgetWS : cbsEndPointStr : " +cbsEndPointStr);
                Log.customer.debug("CATSAPBudWS : checkBudgetWS : cbsEndPoint : " +cbsEndPoint);

           BudgetChkResp budChkResp = new BudgetChkResp();


             _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_AUFNR cbsProjElement = new _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_AUFNR();
              _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_BUKRS cbsCompCode = new _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_BUKRS();
              _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_GWERT cbsTotClaimAgaBudget = new _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_GWERT();
              _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_KNTTP cbsAcctCategory = new _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_KNTTP();
              _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_WAERS cbsCompCodeCurr = new _ZFIAP_BUDGET_AVAILABILITY_RFC_IM_WAERS();
              _ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSCODHolder cbsRespCode = new _ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSCODHolder();
             _ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSTXTHolder cbsRespMessage = new _ZFIAP_BUDGET_AVAILABILITY_RFCResponse_EX_MSTXTHolder();

	    	cbsProjElement.setValue(_projElement);

         Log.customer.debug("Project Element *** Step 2 *** CBS BudgetCheck is : " +_projElement);

	    	cbsCompCode.setValue(_compCode);

          Log.customer.debug("Company Code *** Step 3 *** CBS BudgetCheck is : " +_compCode);

	    	cbsTotClaimAgaBudget.setValue(_totClaimAgaBudget);

          Log.customer.debug("Total Claim amt is *** Step 4 *** CBS BudgetCheck : " +_totClaimAgaBudget);

	    	cbsAcctCategory.setValue(_acctCategory);

           Log.customer.debug("Accounting Category *** Step 5 *** CBS BudgetCheck : " +_acctCategory);

	    	cbsCompCodeCurr.setValue(_compCodeCurr);

            Log.customer.debug("CBS Account Currency is *** Step 6 *** CBS BudgetCheck :" +_compCodeCurr);

             URL endpoint = new URL(cbsEndPoint);
         //    ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub stub = new ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub(endpoint, null);
               ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub stub = new ZFIAP_BUDGET_AVAILABILITY_RFCBindingStub(endpoint, null);
        	stub.ZFIAP_BUDGET_AVAILABILITY_RFC(cbsProjElement, cbsCompCode, cbsTotClaimAgaBudget, cbsAcctCategory, cbsCompCodeCurr, cbsRespCode,cbsRespMessage);
            Log.customer.debug(" *** Step 7 *** CBS BudgetCheck respCode : %s ", cbsRespCode);
            Log.customer.debug(" *** Step 8 *** CBS BudgetCheck respMessage is  : %s" ,cbsRespMessage);
            Log.customer.debug(" *** Step 9 *** CBS BudgetCheck respCode.getClass() is : %s" ,cbsRespCode.getClass());
            Log.customer.debug(" *** Step 10 *** CBS BudgetCheck   respCode.value is : %s ",cbsRespCode.value);
            Log.customer.debug(" *** Step 11 *** CBS BudgetCheck  respCode.value.toString() is : %s",cbsRespCode.value.toString());


            if(cbsRespCode!=null && cbsRespMessage !=null){

    			budChkResp.setBudgetCheckMsgCode(cbsRespCode.value.toString());

    			budChkResp.setBudgetCheckMsgTxt(cbsRespMessage.value.toString());
            } else
            {
                Log.customer.debug("*** is null - stub validate");
            }
           Log.customer.debug("Return value for CBS BudgetCheck ***END***");
	     return budChkResp;
	}

}
