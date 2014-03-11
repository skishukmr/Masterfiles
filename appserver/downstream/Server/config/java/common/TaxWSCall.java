package config.java.common;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ariba.base.core.Base;
import ariba.base.core.Partition;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import cat.cis.atm.service.SecureMySupplyCabinetServiceSoapBindingStub;
import cat.cis.atm.service.TaxLineItemInput;
import cat.cis.atm.service.TaxLineItemOutput;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorSoapBindingStub;
import cat.cis.tuf.webservices.security.authentication.soap.AuthenticatorWebServiceServiceLocator;

// Referenced classes of package config.java.common:
//            TaxInputObject, TaxOutputObject

/**
    S. Sato - Ariba Upgrade Lab

    This code, which makes a web service call has been modified to ensure that testing goes through
    in the lab w/o issues (Integration tests were could not be done in the lab). The change needs
    to be reverted during onsite testing.

    Set the default parameters to "". It was previously pointing to prod params
*/
public class TaxWSCall
{

    public TaxWSCall()
    {
    }

    public List getTaxResponse(List ListTaxInputObject)
    {
        Log.customer.debug("**AR** called tax get response **");
        List TaxOutputs = new ArrayList(100);
        TaxOutputObject too = null;
        try
        {
            Log.customer.debug("**AR** Entering TaxWSCall **");
            //String theEndPoint = "https://atm.cat.com/atm/services/MySupplyCabinetService";
            // String theEndPointDefault = "https://atm.cat.com/atm/services/MySupplyCabinetService";
            String theEndPointDefault = "";
            String theEndPointStr = ResourceService.getString("cat.ws.util","TaxWSCalltheEndPoint");
            Log.customer.debug("**AR** Endpoint used from Resource File: " + theEndPointStr);
			String theEndPoint = ((!StringUtil.nullOrEmptyOrBlankString(theEndPointStr))?theEndPointStr:theEndPointDefault) ;
            Log.customer.debug("**AR** Endpoint used: " + theEndPoint);

            URL endpoint = new URL(theEndPoint);
            SecureMySupplyCabinetServiceSoapBindingStub stub = new SecureMySupplyCabinetServiceSoapBindingStub(endpoint, null);
            AuthenticatorWebServiceServiceLocator authLocator = new AuthenticatorWebServiceServiceLocator();
            // String urlString = "https://tufws.cat.com/TUFWebServices/services/authenticator";

            // String urlString = "https://tufwsqax.ecorp.cat.com/TUFWebServices/services/authenticator";
            String urlString = "";

                // S. Sato - AUL (Adding code to look up this URL from a resource. This is the only one which
                //                is hardcoded)
            String preferredURLString = ResourceService.getString("cat.ws.util","TufWSAuthenticator");
            if (!StringUtil.nullOrEmptyOrBlankString(preferredURLString)) {
                urlString = preferredURLString;
            }

            Log.customer.debug("**AR** URL used: " + urlString);
            AuthenticatorSoapBindingStub webservice = new AuthenticatorSoapBindingStub(new URL(urlString), authLocator);
            Log.customer.debug("**AR** URL used : AuthenticatorSoapBindingStub object created");
            Log.customer.debug("**AR** call authenticate method");
            String credential = webservice.authenticate("aribaws", "q37NrANaKc");
            Log.customer.debug("**AR** call to authenticate method completed");
            stub.setCredential(credential);
            Log.customer.debug("**AR** heres the cookie: " + credential);
            Log.customer.debug("Logged credential");
            int len = 0;
            len = ListTaxInputObject.size();
            Log.customer.debug("**AR** " + len + " is the input list length **");
            TaxInputObject inpObj = null;
            TaxLineItemInput input[] = new TaxLineItemInput[len];
            for(int i = 0; i < len; i++)
            {
                input[i] = new TaxLineItemInput();
                inpObj = (TaxInputObject)ListTaxInputObject.get(i);
                Log.customer.debug("**AR** Logging input params **");
                input[i].setShipFromCityStateCode(inpObj.getShipFromCityStateCode());
                Log.customer.debug("**AR** getShipFromCityStateCode " + inpObj.getShipFromCityStateCode());
                input[i].setShipToCityStateCode(inpObj.getShipToCityStateCode());
                Log.customer.debug("**AR** getShipToCityStateCode " + inpObj.getShipToCityStateCode());
                input[i].setReceivingFacilityCode(inpObj.getReceivingFacilityCode());
                Log.customer.debug("**AR** getReceivingFacilityCode " + inpObj.getReceivingFacilityCode());
                input[i].setOverridePOTaxState(inpObj.getOverridePOTaxState());
                Log.customer.debug("**AR** getOverridePOTaxState " + inpObj.getOverridePOTaxState());
                input[i].setOverridePOTaxBase(inpObj.getOverridePOTaxBase());
                Log.customer.debug("**AR** getOverridePOTaxBase " + inpObj.getOverridePOTaxBase());
                input[i].setOverridePOTaxRate(inpObj.getOverridePOTaxRate());
                Log.customer.debug("**AR** getOverridePOTaxRate " + inpObj.getOverridePOTaxRate());
                input[i].setOverridePOTaxCode(inpObj.getOverridePOTaxCode());
                Log.customer.debug("**AR** getOverridePOTaxCode " + inpObj.getOverridePOTaxCode());
                input[i].setIsCaptial(inpObj.getIsCapital());
                Log.customer.debug("**AR** getIsCapital " + inpObj.getIsCapital());
                input[i].setAribaLineTypeCode(inpObj.getAribaLineTypeCode());
                Log.customer.debug("**AR** getAribaLineTypeCode " + inpObj.getAribaLineTypeCode());
                input[i].setDefaultAccountingDistributionFacilitySegment(inpObj.getDefaultAccountingDistributionFacilitySegment());
                Log.customer.debug("**AR** getDefaultAccountingDistributionFacilitySegment " + inpObj.getDefaultAccountingDistributionFacilitySegment());
                input[i].setAccountingDistributionFacilitySegment(inpObj.getAccountingDistributionFacilitySegment());
                Log.customer.debug("**AR** getAccountingDistributionFacilitySegment " + inpObj.getAccountingDistributionFacilitySegment());
                input[i].setAccountingDistributionDeptCtrlSegment(inpObj.getAccountingDistributionDeptCtrlSegment());
                Log.customer.debug("**AR** getAccountingDistributionDeptCtrlSegment " + inpObj.getAccountingDistributionDeptCtrlSegment());
                input[i].setAccountingDistributionDivSegment(inpObj.getAccountingDistributionDivSegment());
                Log.customer.debug("**AR** getAccountingDistributionDivSegment " + inpObj.getAccountingDistributionDivSegment());
                input[i].setAccountingDistributionSectSegment(inpObj.getAccountingDistributionSectSegment());
                Log.customer.debug("**AR** getAccountingDistributionSectSegment " + inpObj.getAccountingDistributionSectSegment());
                input[i].setAccountingDistributionExpenseSegment(inpObj.getAccountingDistributionExpenseSegment());
                Log.customer.debug("**AR** getAccountingDistributionExpenseSegment " + inpObj.getAccountingDistributionExpenseSegment());
                input[i].setDefaultGlobalDirectoryPayrollFacilityCode(inpObj.getDefaultGlobalDirectoryPayrollFacilityCode());
                Log.customer.debug("**AR** getDefaultGlobalDirectoryPayrollFacilityCode " + inpObj.getDefaultGlobalDirectoryPayrollFacilityCode());
                input[i].setTaxQualifier(inpObj.getTaxQualifier());
                Log.customer.debug("**AR** getTaxQualifier " + inpObj.getTaxQualifier());
                input[i].setMannerOfUseCode(inpObj.getMannerOfUseCode());
                Log.customer.debug("**AR** getMannerOfUseCode " + inpObj.getMannerOfUseCode());
                input[i].setIsLaborSeparatelyStatedOnInvoice(inpObj.getIsLaborSeparatelyStatedOnInvoice());
                Log.customer.debug("**AR** getIsLaborSeparatelyStatedOnInvoice " + inpObj.getIsLaborSeparatelyStatedOnInvoice());
                input[i].setLineItemAmount(inpObj.getLineItemAmount());
                Log.customer.debug("**AR** getLineItemAmount " + inpObj.getLineItemAmount());
                input[i].setSupplierCode(inpObj.getSupplierCode());
                Log.customer.debug("**AR** getSupplierCode " + inpObj.getSupplierCode());
                input[i].setAribaLineItem(inpObj.getAribaLineItem());
                Log.customer.debug("**AR** getAribaLineItem " + inpObj.getAribaLineItem());
                input[i].setAribaReferenceLineItem(inpObj.getAribaReferenceLineItem());
                Log.customer.debug("**AR** getAribaReferenceLineItem" + inpObj.getAribaReferenceLineItem());
                input[i].setShipToCountry(inpObj.getShipToCountry());
                Log.customer.debug("**AR** getShipToCountry " + inpObj.getShipToCountry());
                input[i].setProcessFlowFlag(inpObj.getProcessFlowFlag());
                Log.customer.debug("**AR** getProcessFlowFlag " + inpObj.getProcessFlowFlag());
                input[i].setTaxLineTypeCode(inpObj.getTaxLineTypeCode());
                Log.customer.debug("**AR** getTaxLineTypeCode " + inpObj.getTaxLineTypeCode());
                input[i].setIsCustomShipTo(inpObj.getIsCustomShipTo());
                Log.customer.debug("**AR** getIsCustomShipTo " + inpObj.getIsCustomShipTo());
                input[i].setIsInvoiceNoRelease(inpObj.getIsInvoiceNoRelease());
                Log.customer.debug("**AR** getIsInvoiceNoRelease " + inpObj.getIsInvoiceNoRelease());
                input[i].setIsCreditInvoice(inpObj.getIsCreditInvoice());
                Log.customer.debug("**AR** getIsCreditInvoice " + inpObj.getIsCreditInvoice());
                input[i].setDocumentNo(inpObj.getDocumentNo());
                Log.customer.debug("**AR** getDocumentNo " + inpObj.getDocumentNo());
                Log.customer.debug("**AR** got all inputs **");
            }

            TaxLineItemOutput output[] = stub.applyTaxForLineItems(input);
            Log.customer.debug("**AR** output created **");
            Log.customer.debug("**AR** output from WS is: " + output.toString());
            Partition pv = Base.getService().getPartition("pcsv1");
            Log.customer.debug("**AR** partition used is: " + pv.getName());
            for(int i = 0; i < len; i++)
            {
                too = new TaxOutputObject();
                Log.customer.debug("**AR** Creating new TaxOutputObject " + too.toString());
                if(output[i].getERPTaxCode() != null)
                {
                    too.setERPTaxCode(output[i].getERPTaxCode());
                    Log.customer.debug("**AR** getERPTaxCode " + output[i].getERPTaxCode());
                }
                if(output[i].getWorkFlowMessage() != null)
                {
                    too.setWorkFlowMessage(output[i].getWorkFlowMessage());
                    Log.customer.debug("**AR** getWorkFlowMessage " + output[i].getWorkFlowMessage());
                }
                if(output[i].getTaxState() != null)
                {
                    Log.customer.debug("**AR** getTaxState " + output[i].getTaxState());
                    ariba.base.core.ClusterRoot tstate = Base.getService().objectMatchingUniqueName("cat.core.State", pv, output[i].getTaxState());
                    if(tstate != null)
                    {
                        too.setTaxState(tstate);
                        Log.customer.debug("**AR** matching tax state object was found");
                    }
                }
                if(output[i].getCalculatedTaxAmount() != null)
                {
                    too.setCalculatedTaxAmount(new BigDecimal(output[i].getCalculatedTaxAmount()));
                    Log.customer.debug("**AR** getCalculatedTaxAmount " + output[i].getCalculatedTaxAmount());
                }
                if(output[i].getTaxRate() != null)
                {
                    too.setTaxRate(new BigDecimal(output[i].getTaxRate()));
                    Log.customer.debug("**AR** getTaxRate " + output[i].getTaxRate());
                }
                if(output[i].getTaxBase() != null)
                {
                    too.setTaxBase(new BigDecimal(output[i].getTaxBase()));
                    Log.customer.debug("**AR** getTaxBase " + output[i].getTaxBase());
                }
                if(output[i].getTaxCode() != null)
                {
                    Log.customer.debug("**AR** getTaxCode " + output[i].getTaxCode());
                    ariba.base.core.ClusterRoot tcode = Base.getService().objectMatchingUniqueName("cat.core.TaxCode", pv, output[i].getTaxCode());
                    if(tcode != null)
                    {
                        Log.customer.debug("Yes, tax code was returned, and yes, tax code object was found!");
                        too.setTaxCode(tcode);
                    } else
                    {
                        Log.customer.debug("Yes, tax code was returned, but no, tax code object not found!");
                        Log.customer.debug(output[i].getTaxCode().toString());
                    }
                } else
                {
                    Log.customer.debug("Tax code was returned null !");
                }
                if(output[i].getTaxMessage() != null)
                {
                    too.setTaxMessage(output[i].getTaxMessage());
                    Log.customer.debug("**AR** getTaxMessage " + output[i].getTaxMessage());
                }
                if(output[i].getTaxRegistrationNumber() != null)
                {
                    too.setTaxRegistrationNumber(output[i].getTaxRegistrationNumber());
                    Log.customer.debug("**AR** getTaxRegistrationNumber " + output[i].getTaxRegistrationNumber());
                }
                if(output[i].getIncludeRegNumberFlag() != null)
                {
                    too.setIncludeRegNumberFlag(output[i].getIncludeRegNumberFlag());
                    Log.customer.debug("**AR** getIncludeRegNumberFlag " + output[i].getIncludeRegNumberFlag());
                }
                if(output[i].getAribaLineItem() != null)
                {
                    too.setAribaLineItem(output[i].getAribaLineItem());
                    Log.customer.debug("**AR** getAribaLineItem " + output[i].getAribaLineItem());
                }
                if(output[i].getTaxLineType() != null)
                {
                    too.setTaxLineType(output[i].getTaxLineType());
                    Log.customer.debug("**AR** getTaxLineType " + output[i].getTaxLineType());
                }
                if(output[i].getMsgForTaxCodeRetrieve() != null)
                {
                    too.setMsgForTaxCodeRetrieve(output[i].getMsgForTaxCodeRetrieve());
                    Log.customer.debug("**AR** getMsgForTaxCodeRetrieve " + output[i].getMsgForTaxCodeRetrieve());
                }
                if(output[i].getWorkFlowIndicator() != null)
                {
                    too.setWorkFlowIndicator(output[i].getWorkFlowIndicator());
                    Log.customer.debug("**AR** getWorkFlowIndicator " + output[i].getWorkFlowIndicator());
                }
                if(output[i].getThresholdAmount() != null)
                {
                    too.setThresholdAmount(new BigDecimal(output[i].getThresholdAmount()));
                    Log.customer.debug("**AR** getThresholdAmount " + output[i].getThresholdAmount());
                }
                if(output[i].getAribaReferenceLineItem() != null)
                {
                    too.setAribaReferenceLineItem(output[i].getAribaReferenceLineItem());
                    Log.customer.debug("**AR** getAribaReferenceLineItem " + output[i].getAribaReferenceLineItem());
                }
                if(output[i].getDocumentNo() != null)
                {
                    too.setDocumentNo(output[i].getDocumentNo());
                    Log.customer.debug("**AR** getDocumentNo " + output[i].getDocumentNo());
                }
                TaxOutputs.add(i, too);
                Log.customer.debug("**AR** ending second loop, added output object to list! ");
            }

        }
        catch(Exception e)
        {
            Log.customer.debug(e.toString());
            TaxOutputs = null;
            return TaxOutputs;
        }
        Log.customer.debug("** returning tax ... **");
        Log.customer.debug(((TaxOutputObject)TaxOutputs.get(0)).getAribaLineItem());
        return TaxOutputs;
    }
}
