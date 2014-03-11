package config.java.integration.ws.sap;
//Added the Changes for MACH1 2.5 - Sandeep to send Internal Order for MACH1 parition.
//Added the Changes for CGM SAP Account validation for Non-MACH1 parition.
import ariba.util.log.Log;
import config.java.common.CatCommonUtil;
import config.java.common.sap.CatSAPAccountingCollector;
import config.java.common.sap.CatSAPAccountingValidator;
import config.java.integration.ws.sap.*;

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.
*/
public class SAPAccountValidator {

	public static CatSAPAccountingValidator validateAccount (CatSAPAccountingCollector input)throws Exception  {
		Log.customer.debug("%s *** inside validateAccount ",classname);
        CatSAPAccountingValidator resp = new CatSAPAccountingValidator();
        Log.customer.debug("%s *** inside validateAccount %s ",classname, resp);

            // S. Sato - AUL - Skip web call if disable integration parameter is set
            // Note: This parameter is not really required except in the lab environment
            // when the integration needs to be disabled. If this parameter is not mentioned
            // or set in Parameters.table, the system just goes ahead and integrates
        if (CatCommonUtil.DisableIntegration) {

            Log.customer.debug(
                    "%s: Parameter: %s has been set to true. The system will not make the  " +
                    "account validation call and will return a response code 000",
                    classname,
                    CatCommonUtil.DisableIntegrationParam);
            resp.setValidationCode("000".toString());
            resp.setValidationMessage("Skip the web call as disble integration has been set.".toString());
            return resp;
        } 

		SAPServiceInitiator init = new SAPServiceInitiator();
		Log.customer.debug("%s *** inside validateAccount SAPServiceInitiator : %s ",classname, init);
		String SAPSource = (String)input.getSapsrc();
		//SAPServiceInitiator init = new SAPServiceInitiator();
		Log.customer.debug("%s *** inside validateAccount SAPServiceInitiator : %s ",classname, init);
		//PK changes START
		CGMSAPServiceInitiator cgmInit = new CGMSAPServiceInitiator();
		Log.customer.debug("%s *** inside validateAccount SAPServiceInitiator : %s ",classname, cgmInit);
		//PK changes END
        	String [] output = {"00","",""};
		
                //String [] output1 = ("00","","");

		if(SAPSource.equalsIgnoreCase("MACH1")){
		//Sandeep - MACH1 2.5 changes to include internal order sent during webcall
		//	init.setMACH1WSInput(input.getComcode(),input.getAcccat(),input.getCstctr(),input.getWbsele(),input.getGenlgr());
		Log.customer.debug("MACH1 2.5 changes to include internal order sent during webcall - Sandeep",classname);

		init.setMACH1WSInput(input.getIntord(),input.getComcode(),input.getAcccat(),input.getCstctr(),input.getWbsele(),input.getGenlgr());
			Log.customer.debug("%s *** inside validateAccount : MACH1 ",classname);
	        output = init.mach1ValidateAccount();
	        Log.customer.debug("%s *** inside validateAccount : MACH1 %s ",classname, output);
                resp.setValidationCode(output[0]);
                resp.setValidationMessage(output[1]);
                //added as a part of MACH1 2.5 to get valid WBS when IO is passed for Acc cat F for MACH1 companycodes
                resp.setValidIoWBSele(output[2]);

		}

		if(SAPSource.equalsIgnoreCase("CBS")){
			init.setCBSWSInput(input.getComcode(),input.getAcccat(),input.getCstctr(),input.getIntord(),input.getGenlgr());
			Log.customer.debug("%s *** inside validateAccount : CBS ",classname);
	        output = init.cbsValidateAccount();
	        Log.customer.debug("%s *** inside validateAccount : CBS %s ",classname, output);
                 resp.setValidationCode(output[0]);
                resp.setValidationMessage(output[1]);
                }
		//PK changes START 
		if(SAPSource.equalsIgnoreCase("CGM")) {

			cgmInit.setCGMWSInput(input.getAcccat(),input.getComcode(),input.getCstctr(),input.getWbsele(),input.getGenlgr(),input.getIntord());
			Log.customer.debug("%s *** inside validateAccount : ZWS ",classname, cgmInit);
			String [] cgmOutput = {"00",""};	        
			cgmOutput = cgmInit.CGMValidateAccount();
	        Log.customer.debug("%s *** inside validateAccount : ZWS %s ",classname, cgmOutput);
            resp.setValidationCode(cgmOutput[0]);
            resp.setValidationMessage(cgmOutput[1]);

		}
		//PK changes END
			return resp;
	}

	public static void main(String [] args)throws Exception {
		CatSAPAccountingValidator resp = SAPAccountValidator.validateAccount(new CatSAPAccountingCollector(args[1],args[2],args[3],args[4],args[5],"",""));
		System.out.println("Result Code is "+resp.getResultCode());
                Log.customer.debug("Gettting the Result Code - Sandeep" +resp.getResultCode());
		System.out.println("Result Message is "+resp.getMessage());
                Log.customer.debug("Getting the Message Code" +resp.getMessage());
	//added as a part of MACH1 2.5 to get valid WBS when IO is passed for Acc cat F for MACH1 companycodes
		System.out.println("Valid WBS for IO passed is "+resp.getIOWBSele());
                Log.customer.debug("Getting the WBS elemet" +resp.getIOWBSele());
	}

	private static String classname = "SAPAccountValidator";
}
