package config.java.integration.ws.sap;

import java.net.URL;

import mc_style.functions.soap.sap.document.sap_com.ZFIFM_ACCOUNT_VALIDATIONSoapBindingStub;
import ariba.util.core.Date;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import functions.rfc.sap.document.sap_com.Char1;
import functions.rfc.sap.document.sap_com.Char10;
import mc_style.functions.soap.sap.document.sap_com.Char12;
import functions.rfc.sap.document.sap_com.Char24;
import functions.rfc.sap.document.sap_com.Char4;
import functions.rfc.sap.document.sap_com.ZFIAP_ACCOUNT_VALIDATION_RFCBindingStub;
import functions.rfc.sap.document.sap_com._ZFIAP_ACCOUNT_VALIDATION_RFC_IM_BUKRS;
import functions.rfc.sap.document.sap_com._ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KNTTP;
import functions.rfc.sap.document.sap_com._ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KOSTL;
import functions.rfc.sap.document.sap_com._ZFIAP_ACCOUNT_VALIDATION_RFC_IM_SAKNR;
//import functions.rfc.sap.document.sap_com._ZFIAP_ACCOUNT_VALIDATION_RFC_IM_AUFNR;
import functions.rfc.sap.document.sap_com.holders.Char3Holder;
import functions.rfc.sap.document.sap_com.holders.Char70Holder;
import functions.rfc.sap.document.sap_com.holders.Char24Holder;
import functions.rfc.sap.document.sap_com.holders._ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSCODHolder;
import functions.rfc.sap.document.sap_com.holders._ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSTXTHolder;
//import functions.rfc.sap.document.sap_com.holders._ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_PSPEL;

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.

    Set the default parameters to "". It was previously pointing to prod params
*/
public class SAPServiceInitiator
{

    // String mach1EndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/AccountValidation_Ariba_SAPMach1Web/sca/ZFIFM_ACCOUNT_VALIDATION";
    String mach1EndPointDefault = "";
    String mach1EndPointStr = ResourceService.getString("cat.java.sap","MACH1AccValidationURL");
	String mach1EndPoint = ((!StringUtil.nullOrEmptyOrBlankString(mach1EndPointStr))?mach1EndPointStr:mach1EndPointDefault) ;

	// String cbsEndPointDefault = "http://adwpsq1.ecorp.cat.com:9080/AccountValidation_Ariba_CBSWeb/sca/ZFIAP_ACCOUNT_VALIDATION_RFCPortType";
	String cbsEndPointDefault = "";
	String cbsEndPointStr = ResourceService.getString("cat.java.sap","CBSAccValidationURL");
	String cbsEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(cbsEndPointStr))?cbsEndPointStr:cbsEndPointDefault) ;

    public SAPServiceInitiator()
    {
    	Log.customer.debug("%s *** started  ",classname);
    }


    public void setMACH1WSInput(String InternalOrder,String CompanyCode, String AccountCategory, String CostCenter, String WBSElement, String GeneralLedger)
    {
    	Log.customer.debug("%s *** in setMACH1WSInput ",classname);
    	_companyCode.setValue(CompanyCode);
    	_accountCategory.setValue(AccountCategory);
    	_costCenter.setValue(CostCenter);
    	_wbsElement.setValue(WBSElement);
    	_generalLedger.setValue(GeneralLedger);
   //Added as a part of MACH1 2.5 Changes - Sandeep
   Log.customer.debug("Inside SAPServiceInitiator to get the WBS element - MACH1 2.5 - Sandeep");
    	_internalOrder.setValue(InternalOrder);

    	Log.customer.debug("%s *** in setMACH1WSInput %s %s ",classname,_companyCode ,_accountCategory);
    }

    public void setCBSWSInput(String CompanyCode, String AccountCategory, String CostCenter, String InternalOrder, String GeneralLedger)
    {
    	_cbsCompanyCode.setValue(CompanyCode);
    	_cbsAccountCategory.setValue(AccountCategory);
    	_cbsCostCenter.setValue(CostCenter);
    	//_internalOrder.setValue(InternalOrder);
    	_cbsGeneralLedger.setValue(GeneralLedger);

    }

    public String[] mach1ValidateAccount()
        throws Exception
    {
    	respCode = new Char3Holder();
    	respMessage = new Char70Holder();
    	//Added as part of MACH1 2.5 to get WBS by sending a Valid IO number- Sandeep
    	respioWBSele = new Char24Holder();
    	Log.customer.debug("%s *** MACH1  URL is : %s ",classname,mach1EndPoint);
    	Log.customer.debug(classname + " *** MACH1 Input Parameters are : " + "CompanyCode "+_companyCode+ " Account Category " +_accountCategory + " CostCenter "+ _costCenter +" WbsElement "+_wbsElement +" General Ledger "+_generalLedger +" Internal Order " +_internalOrder +" ResponceCode "+ respCode + " ResponceMessage " + respMessage + " ResponseIOWBS " + respioWBSele);
    	endpoint = new URL(mach1EndPoint);
        stub = new ZFIFM_ACCOUNT_VALIDATIONSoapBindingStub(endpoint, null);
        stub.zfifmIntAccountValidation(_internalOrder,_companyCode,_accountCategory,_costCenter,_wbsElement,_generalLedger,respCode,respMessage,respioWBSele);
        Log.customer.debug("%s *** MACH1  respCode is : %s ",classname,respCode);
        Log.customer.debug("%s *** MACH1  respMessage is : %s ",classname,respMessage);
        Log.customer.debug("%s *** MACH1  respioWBSele is -sandeep mach1 2.5 code: %s ",classname,respioWBSele);
        Log.customer.debug("%s *** MACH1  respCode.getClass() is : %s ",classname,respCode.getClass());
        Log.customer.debug("%s *** MACH1  respCode.value is : %s ",classname,respCode.value);
        Log.customer.debug("%s *** MACH1  respCode.value.toString() is : %s ",classname,respCode.value.toString());


        if(respCode!=null && respMessage !=null && respioWBSele !=null ) {
        Log.customer.debug("Response Code, Response Message and WBS returned is not null hence returning all the three- Sandeep Check success for WBS");
            return (new String[] {

                    respCode.value.toString(), respMessage.value.toString(), respioWBSele.value.toString()
                });
        }

        if(respCode!=null && respMessage !=null && respioWBSele == null)
        {
Log.customer.debug("Response Code, Response Message and WBS returned is null hence returning only 2");
			return (new String[] {
			                    respCode.value.toString(), respMessage.value.toString(),null
			                });
        }
        else

        {
            Log.customer.debug("%s *** is null - stub validate",classname);
        }
        return (new String[] {
                "000", respMessage.value.toString(),null
            });
    }

    public String[] cbsValidateAccount()
    throws Exception
{
    	_ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSCODHolder cbsRespCode = new _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSCODHolder();
    	_ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSTXTHolder cbsRespMessage = new _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSTXTHolder();

    	Log.customer.debug("%s *** CBS  URL is : %s ",classname,cbsEndPoint);
    	Log.customer.debug(classname + " *** CBS Input Parameters are : " + "CompanyCode "+_cbsCompanyCode+ " Account Category " +_cbsAccountCategory + " CostCenter "+ _cbsCostCenter +" General Ledger "+_cbsGeneralLedger +" ResponceCode "+ cbsRespCode + " ResponceMessage " + cbsRespMessage);

        endpoint = new URL(cbsEndPoint);
        cbsstub = new ZFIAP_ACCOUNT_VALIDATION_RFCBindingStub(endpoint, null);
        cbsstub.ZFIAP_ACCOUNT_VALIDATION_RFC(_cbsCompanyCode,_cbsAccountCategory,_cbsCostCenter,_cbsGeneralLedger,cbsRespCode,cbsRespMessage);

        Log.customer.debug("%s *** CBS  respCode is : %s ",classname,cbsRespCode);
        Log.customer.debug("%s *** CBS  respMessage is : %s ",classname,cbsRespMessage);
        Log.customer.debug("%s *** CBS  respCode.getClass() is : %s ",classname,cbsRespCode.getClass());
        Log.customer.debug("%s *** CBS  respCode.value is : %s ",classname,cbsRespMessage.value);
        Log.customer.debug("%s *** CBS  respCode.value.toString() is : %s ",classname,cbsRespCode.value.toString());

        if(cbsRespCode!=null && cbsRespMessage !=null){
            return (new String[] {
            		cbsRespCode.value.toString(), cbsRespMessage.value.toString()
                });
        } else
        {
            Log.customer.debug("%s *** CBS is null - stub validate",classname);
        }

        return (new String[] {
                "000", cbsRespMessage.value.toString()
            });
}


    public static void main(String args[])
        throws Exception
    {
    	SAPServiceInitiator init = new SAPServiceInitiator();
    	init.setMACH1WSInput("1885","F","1010110030",null,"5600000000", null);
    	System.out.println(classname + " Inside main method");

		String mach1EndPointDEV = "http://webwast2.ecorp.cat.com:9080/AccountValidation_Ariba_SAPMach1Web/sca/ZFIFM_ACCOUNT_VALIDATION";
		String mach1EndPointQA = "http://adwpsq1.ecorp.cat.com:9080/AccountValidation_Ariba_SAPMach1Web/sca/ZFIFM_INT_ACCOUNT_VALID";
		String mach1EndPointPROD = "http://wpspha.ecorp.cat.com/AccountValidation_Ariba_SAPMach1Web/sca/ZFIFM_INT_ACCOUNT_VALID";
		String mach1EndPointURL = null;

		String env = (String)args[0];
	    if(env.equals("PROD")){
	    	mach1EndPointURL = mach1EndPointPROD;
	    }else if(env.equals("QA")){
	    	mach1EndPointURL = mach1EndPointQA;
	    }else {
	    	mach1EndPointURL = mach1EndPointDEV;
	    }
    	String no=(String)args[1];
    	int n = Integer.parseInt(no);

    	for (int i=0;i<n;i++)
    	{
    		Date d1 = new Date();
    		System.out.println(d1.toString() +" : "+ classname + " Inside for loop " + (i+1));
    		System.out.println(d1.toString() +" : "+ classname+ " *** START calling mach1ValidateAccount :::: ");
    		Char3Holder respCode = new Char3Holder();
    		Char70Holder respMessage = new Char70Holder();
    		Char24Holder respioWBSele = new Char24Holder();
//Added to accomodate IO number while validating accounting

    		System.out.println(d1.toString() +" : "+ classname + " *** started  URL is : " + mach1EndPointURL);
    		System.out.println(d1.toString() +" : "+ classname + " *** Input Parameters are : " + "CompanyCode "+init._companyCode+ " Account Category " +init._accountCategory + " CostCenter "+ init._costCenter +" WbsElement "+init._wbsElement +" General Ledger "+init._generalLedger +" ResponceCode "+ respCode + " ResponceMessage " + respMessage);
    		endpoint = new URL(mach1EndPointURL);
            stub = new ZFIFM_ACCOUNT_VALIDATIONSoapBindingStub(endpoint, null);
            stub.zfifmIntAccountValidation(init._internalOrder,init._companyCode,init._accountCategory,init._costCenter,init._wbsElement,init._generalLedger,respCode,respMessage,respioWBSele);
            System.out.println(d1.toString() +" : "+ classname + " *** started  respCode is : " + respCode);
            System.out.println(d1.toString() +" : "+ classname + " *** started  respMessage is : " + respMessage);
            System.out.println(d1.toString() +" : "+ classname + " *** started  respioWBSele is : " + respioWBSele);
            System.out.println(d1.toString() +" : "+ classname + " *** started  respCode.getClass() is : " + respCode.getClass());
            System.out.println(d1.toString() +" : "+ classname + " *** started  respCode.value is : " + respCode.value);
            System.out.println(d1.toString() +" : "+ classname + " *** started  respCode.value.toString() is : " + respCode.value.toString());
            System.out.println(d1.toString() +" : "+ classname + " *** started  respMessage.value.toString() is : " + respMessage.value.toString());
            System.out.println(d1.toString() +" : "+ classname + " *** started MACH1 2.4 -sandeep-  respioWBSele.value.toString() is : " + respioWBSele.value.toString());

            if(respCode!=null && respMessage !=null && respioWBSele !=null ){
            	System.out.println(d1.toString() +" : "+ classname + " *** resp and IO wbs is is not null ");
            }
           if(respCode!=null && respMessage !=null && respioWBSele == null )
            {
            	System.out.println(d1.toString() +" : "+ classname + " *** resp is not null , IO wbs is null - sandeep ");
            }
            else
             {
			            	System.out.println(d1.toString() +" : "+ classname + " *** resp is null");
            }

            System.out.println(d1.toString() +" : "+ classname + " *** END calling mach1ValidateAccount :::: " + (i+1));
    	}

    }

    private Char4 _companyCode = new Char4();
    private Char10 _costCenter = new Char10();
    private Char10 _generalLedger = new Char10();
    //Added for MACH1 2.5
    private Char12 _internalOrder = new Char12();
    private Char24 _wbsElement = new Char24();
    private Char1 _accountCategory = new Char1();
    private Char3Holder respCode = new Char3Holder();
    private Char70Holder respMessage = new Char70Holder();
    //Added for MACH1 2.5
    private Char24Holder respioWBSele = new Char24Holder();
    //CBS
    private _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_BUKRS _cbsCompanyCode = new _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_BUKRS();
    private _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KNTTP _cbsAccountCategory = new _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KNTTP();
    private _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KOSTL _cbsCostCenter = new _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_KOSTL();
    private _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_SAKNR _cbsGeneralLedger = new _ZFIAP_ACCOUNT_VALIDATION_RFC_IM_SAKNR();
    //private _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSCODHolder cbsRespCode = new _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSCODHolder();
    //private _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSTXTHolder cbsRespMessage = new _ZFIAP_ACCOUNT_VALIDATION_RFCResponse_EX_MSTXTHolder();
    private static ZFIAP_ACCOUNT_VALIDATION_RFCBindingStub cbsstub;
    //CBS
    private static URL endpoint;
    private static ZFIFM_ACCOUNT_VALIDATIONSoapBindingStub stub;
    private static String classname = "SAPServiceInitiator";

}
