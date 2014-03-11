package config.java.integration.ws.sap;

import java.net.URL;

import org.apache.axis2.context.ConfigurationContext;


import functions.rfc.sap.document.sap_com.ZWS_ACCOUNT_VALIDATIONStub;
import functions.rfc.sap.document.sap_com.ZWS_ACCOUNT_VALIDATIONCallbackHandler;
import ariba.util.core.Date;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CGMSAPServiceInitiator
{


  // String devEndPoint = "http://172.20.182.51:8001/sap/bc/srt/rfc/sap/zws_budget_availability/140/zws_budget_availability/zws_budget_availability";
  //   String devEndPoint = "http://172.20.182.51:8001/sap/bc/srt/rfc/sap/zws_account_validation/140/zws_account_validation/zws_account_validation";



    	String newEndPointDefault = "";
	String newEndPointStr = ResourceService.getString("cat.java.sap","CGMAccValidationURL");
	//	newEndPointStr = "http://172.16.51.242:8001/sap/bc/srt/rfc/sap/zws_account_validation/140/zws_account_validation/zws_account_validation";
	//String newEndPointStr = ResourceService.getString("cat.java.sap","CGMAccValidationURL");
	//newEndPointStr = "http://172.16.51.242:8001/sap/bc/srt/rfc/sap/zws_account_validation/140/zws_account_validation/zws_account_validation";
	String newEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(newEndPointStr))?newEndPointStr:newEndPointDefault) ;

    public CGMSAPServiceInitiator()
    {
    	Log.customer.debug("%s *** started  ",classname);
    }


     public void setCGMWSInput(String AccountCategory,String CompanyCode, String CostCenter, String WBSElement, String GeneralLedger, String InternalOrder)
	    {
	    	Log.customer.debug("%s *** in setCGMWSInput ",classname);
	    	_accountCategory.setChar1(AccountCategory);

	        _companyCode.setChar4(CompanyCode);
	    	if(!StringUtil.nullOrEmptyOrBlankString(CostCenter))
	    	{
	    		_costCenter.setChar10(CostCenter);
	    	}
	    	if(!StringUtil.nullOrEmptyOrBlankString(WBSElement))
	    	{
	    		_wbsElement.setChar24(WBSElement);
	    	}
    		_generalLedger.setChar10(GeneralLedger);
	    	if(!StringUtil.nullOrEmptyOrBlankString(InternalOrder))
	    	{
	    		_internalOrder.setChar12(InternalOrder);
	    	}
	    	Log.customer.debug("%s *** in AccountCAtegory ",AccountCategory);
			Log.customer.debug("%s *** in CompanyCode ",CompanyCode);
			Log.customer.debug("%s *** in CostCenter ",CostCenter);
			Log.customer.debug("%s *** in WBSElement ",WBSElement);
			Log.customer.debug("%s *** in GeneralLedger ",GeneralLedger);

	        request = new ZWS_ACCOUNT_VALIDATIONStub.ZFIFM_INT_ACCOUNT_VALIDATION();
	    	if(!StringUtil.nullOrEmptyOrBlankString(InternalOrder))
	    	{
	    		request.setIM_AUFNR(_internalOrder);
	    	}
	        request.setIM_BUKRS(_companyCode);
	        request.setIM_KNTTP(_accountCategory);
	    	if(!StringUtil.nullOrEmptyOrBlankString(CostCenter))
	    	{
	    		request.setIM_KOSTL(_costCenter);
	    	}
	    	if(!StringUtil.nullOrEmptyOrBlankString(WBSElement))
	    	{
	    		request.setIM_POSID(_wbsElement);
	    	}
	        request.setIM_SAKNR(_generalLedger);
			/*Log.customer.debug("%s *** in _accountCategory ",_accountCategory);
			Log.customer.debug("%s *** in _companyCode ",_companyCode);
			Log.customer.debug("%s *** in _costCenter ",_costCenter);
			Log.customer.debug("%s *** in _wbsElement ",_wbsElement);
			Log.customer.debug("%s *** in _generalLedger ",_generalLedger);*/


    }
;

 public String[] CGMValidateAccount()
        throws Exception
    {
    	respCode = new ZWS_ACCOUNT_VALIDATIONStub.Char3();
    	respMessage = new ZWS_ACCOUNT_VALIDATIONStub.Char70();
    	Log.customer.debug("%s *** CGMValidateAccount is :  ",classname);
    	Log.customer.debug("%s *** newEndPoint is :  ",newEndPoint);
      //  Log.customer.debug(classname + " *** new Input Parameters are : " + "CompanyCode "+_companyCode+ " Account Category " +_accountCategory + " CostCenter "+ _costCenter +" WbsElement "+_wbsElement +" General Ledger "+_generalLedger +" Internal Order " +_internalOrder +" ResponceCode "+ respCode + " ResponceMessage " + respMessage + " ResponseIOWBS " );
    	//endpoint = new ConfigurationContext(devEndPoint);
    //	String url="";
        stub = new ZWS_ACCOUNT_VALIDATIONStub(null,newEndPoint);
		Log.customer.debug("%s *** stub :  ",stub);
        //stub.startzFIFM_INT_ACCOUNT_VALIDATION(_internalOrder,_companyCode,_accountCategory,_costCenter,_wbsElement,_generalLedger,respCode,respMessage);
        ZWS_ACCOUNT_VALIDATIONStub.ZFIFM_INT_ACCOUNT_VALIDATIONResponse response = stub.zFIFM_INT_ACCOUNT_VALIDATION(request);

        respCode = response.getEX_MSG_CODE();
		Log.customer.debug("%s *** respCode :  ",respCode);
        respMessage = response.getEX_MSG_TXT();
		Log.customer.debug("%s *** respMessage :  ",respMessage);

        if(respCode!=null && respMessage !=null ) {
        Log.customer.debug("Response Code, Response Message and WBS returned is not null hence returning all the three- Sandeep Check success for WBS");
            return (new String[] {
         respCode.getChar3(), respMessage.getChar70() });
        }


        else
        {
            Log.customer.debug("%s *** is null - stub validate",classname);
        }
        return (new String[] { "000", respMessage.getChar70(),null});
    }

    private static ZWS_ACCOUNT_VALIDATIONStub stub;

    private ZWS_ACCOUNT_VALIDATIONStub.Char1 _accountCategory = new ZWS_ACCOUNT_VALIDATIONStub.Char1();

    private ZWS_ACCOUNT_VALIDATIONStub.Char4 _companyCode = new ZWS_ACCOUNT_VALIDATIONStub.Char4();
	private ZWS_ACCOUNT_VALIDATIONStub.Char10 _costCenter = new ZWS_ACCOUNT_VALIDATIONStub.Char10();
	private ZWS_ACCOUNT_VALIDATIONStub.Char10 _generalLedger = new ZWS_ACCOUNT_VALIDATIONStub.Char10();
	//private ZWS_ACCOUNT_VALIDATIONStub.Char10 _generalLedger = new ZWS_ACCOUNT_VALIDATIONStub.Char10();
	//Added for MACH1 2.5
	private ZWS_ACCOUNT_VALIDATIONStub.Char12 _internalOrder = new ZWS_ACCOUNT_VALIDATIONStub.Char12();
	private ZWS_ACCOUNT_VALIDATIONStub.Char24 _wbsElement = new ZWS_ACCOUNT_VALIDATIONStub.Char24();

	private ZWS_ACCOUNT_VALIDATIONStub.Char3 respCode = new ZWS_ACCOUNT_VALIDATIONStub.Char3();
	private ZWS_ACCOUNT_VALIDATIONStub.Char70 respMessage = new ZWS_ACCOUNT_VALIDATIONStub.Char70();


    private static String classname = "CGMSAPServiceInitiator";
    private ZWS_ACCOUNT_VALIDATIONStub.ZFIFM_INT_ACCOUNT_VALIDATION request = null;
 //   private static ConfigurationContext endpoint = new ConfigurationContext();
}
