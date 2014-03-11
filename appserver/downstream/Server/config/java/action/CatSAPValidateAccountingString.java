package config.java.action.sap;

import java.util.Iterator;

import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Fmt;import ariba.util.core.StringUtil;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.sap.CatSAPAccountingCollector;
import config.java.common.sap.CatSAPAccountingValidator;
import config.java.integration.ws.sap.SAPAccountValidator;
import ariba.approvable.core.LineItemCollection;
import ariba.purchasing.core.Requisition;

public class CatSAPValidateAccountingString extends Action
{

	public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ProcureLineItem)
        {
            Log.customer.debug("%s *** Validate Acctng fire! ", classname);
            ProcureLineItem pli = (ProcureLineItem)object;
            Log.customer.debug(classname +" *** found pli " + pli);
            SplitAccountingCollection sac = pli.getAccountings();
            Log.customer.debug(classname +" *** found sac " + sac);

            // Added by James - Dec 04 2008
            if(isReqERFQ(pli) && !wasReqERFQ(pli)){
            	Log.customer.debug("CatSAPValidateAccountingString : fire : It is ERFQ. There is no need of accounting validation.");
				return;
			}

            if(sac != null)
            {
                Iterator saci = sac.getAllSplitAccountingsIterator();
                Log.customer.debug("%s *** found saci %s " ,classname, saci.toString());
                while(saci.hasNext())
                {
                    Log.customer.debug("%s *** saci has next",classname);
                    SplitAccounting sa = (SplitAccounting)saci.next();
                    Log.customer.debug("%s *** found sa %s ",classname, sa.toString());
                    if(!isAccountValidationRequired(sa,pli)){
                    	return;
                    }
                    CatSAPAccountingCollector accclr = getCatSAPAccounting(sa,pli);
                    Log.customer.debug("%s *** found accclr %s " ,classname, accclr.toString());
                    if(accclr != null)
                    {
                        Log.customer.debug("%s *** accclr not null",classname);
                        CatSAPAccountingValidator response = null;
                        try
                        {
                            Log.customer.debug("%s *** inside try 1",classname);
                            response = SAPAccountValidator.validateAccount(accclr);
                            Log.customer.debug("%s *** passed cats to validateaccount",classname);
                            Log.customer.debug("%s *** response ",classname + response.toString());
                            if(response != null)
                            {
                                Log.customer.debug("%s *** ResultCode: %s" , classname, response.getResultCode());
                                Log.customer.debug("%s *** Message: %s", classname, response.getMessage());
                                //Added for MACH1 2.5 by Sandeep to receive WBS element by sending IO
                                Log.customer.debug ("%s *** ioWBSele added by Sandeep for MACH1 2.5: %s",classname, response.getIOWBSele());
                                FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
                                if(response.getResultCode().equals("000"))
                                {
                                    sa.setFieldValue("ValidateAccountingMessage", ValidAccountingMsg);            				            // S. Sato - AUL - Added isImmutable check                    				if (!fp.isImmutable()) {
                                        fp.setPropertyForKey("ValueStyle", "brandVeryDkText");                    				}
                                }
                             else if (response.getResultCode().equals("020"))
                                {
                                Log.customer.debug("Response Code returned for MACH1 2.5 for Account Type F is VALID hence set Valid accounting");


                                   StringBuffer sb01 = (new StringBuffer(ValidAccountingMsg)).append(response.getMessage());
                                   sa.setFieldValue("ValidateAccountingMessage", sb01.toString());
                                   //sa.setFieldValue("ValidateAccountingMessage", ValidAccountingMsg);       				                    // S. Sato - AUL - Added isImmutable check                   				    if (!fp.isImmutable()) {
                                        fp.setPropertyForKey("ValueStyle", "brandVeryDkText");                   				    }
                                }
                              else
                                {
                                    StringBuffer sb = (new StringBuffer(InvalidAccountingMsg)).append(response.getMessage()).append(AdditionalMessage);
                                    sa.setFieldValue("ValidateAccountingMessage", sb.toString());        				                // S. Sato - AUL - Added isImmutable check                    				if (!fp.isImmutable()) {
                                        fp.setPropertyForKey("ValueStyle", "catRedTextSm");                    				}
                                }
                                String newWBS = null;

                                Log.customer.debug("Setting the WBS for Acc Cat F for MACH1 2.5 Changes- Sandeep");                                Log.customer.debug("IO Response: '%s' - Sandeep", response.getIOWBSele());                                if (!StringUtil.nullOrEmptyOrBlankString(response.getIOWBSele()))
                                {
					newWBS = response.getIOWBSele();
				       Log.customer.debug("Setting the WBS -Condition True - Set new WBS ");
				        sa.setFieldValue ("WBSElementText",newWBS);
 						}
				else {
                                      	Log.customer.debug("WBS return null - Sandeep");
  				}
                            }
                        }
                        catch(Exception e)
                        {
                        	FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
                        	Log.customer.debug("%s *** Exception: %s", classname, e);
                            sa.setFieldValue("ValidateAccountingMessage", AccValExcpMessage);				                // S. Sato - AUL - Added isImmutable check            				if (!fp.isImmutable()) {
                                fp.setPropertyForKey("ValueStyle", "catRedTextSm");            				}
                           // sa.setFieldValue ("WBSElementText",newWBS);

                        }
                    }
                }
            }
        }
    }

    public CatSAPValidateAccountingString()
    {
    }

    public static CatSAPAccountingCollector getCatSAPAccounting(SplitAccounting sa, ProcureLineItem pli)
    {
        Log.customer.debug("%s *** inside get cat actng::%s " ,classname, sa.toString());
        String cstctr = (String)sa.getFieldValue("CostCenterText");
        String genlgr = (String)sa.getFieldValue("GeneralLedgerText");
        String intord = (String)sa.getFieldValue("InternalOrderText");
        String wbsele = (String)sa.getFieldValue("WBSElementText");
        String comcode = null;
        String sapsrc = null;
        String acccat = null;
        ClusterRoot company =(ClusterRoot)pli.getLineItemCollection().getDottedFieldValue("CompanyCode");
        if(company!=null){
        comcode = (String)company.getDottedFieldValue("UniqueName");
        sapsrc = (String)company.getDottedFieldValue("SAPSource");

        }
        ClusterRoot acccategory =(ClusterRoot)pli.getDottedFieldValue("AccountCategory");
        if(acccategory!=null){
        acccat = (String)acccategory.getDottedFieldValue("UniqueName");
        }
        Log.customer.debug("%s *** returning from getcatactng");
        //Added by Sandeep for MACH1 2.5
      if (company!=null && sapsrc.equalsIgnoreCase("MACH1") && acccategory!=null)
        {
         Log.customer.debug("Its a MACH1 Company Code, checking the Account Category");
         acccat = (String)acccategory.getDottedFieldValue("UniqueName");
         if (acccat.equalsIgnoreCase("F"))
            {
           Log.customer.debug (" Account Category for a MACH1 company Code is F, hence return a WBS element as null");
            wbsele = null;
           }
         }
        return new CatSAPAccountingCollector(cstctr, genlgr, intord, wbsele, comcode, sapsrc, acccat);
    }


    private static final String classname = "CatSAPValidateAccountingString : ";
    private static final String ValidAccountingMsg = Fmt.Sil("cat.java.sap", "AccountDistributionValid");
    private static final String InvalidAccountingMsg = Fmt.Sil("cat.java.sap", "AccountDistributionNotValid");
    private static final String AdditionalMessage = Fmt.Sil("cat.java.sap", "AccountingErrorGuidance");
    private static final String AccValExcpMessage = Fmt.Sil("cat.java.sap", "AccValExcpMessage");
    public boolean isAccountValidationRequired(SplitAccounting sa, ProcureLineItem pli){

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
        else if((acccat != null && (acccat.equalsIgnoreCase("P")||acccat.equalsIgnoreCase("F")))
        		&&(isGLEditableForCapital != null && isGLEditableForCapital.equalsIgnoreCase("Y"))){
        	return true;
    	}
        else if((acccat != null && acccat.equalsIgnoreCase("Z"))
        		&&(isGLEditableForInterCompany != null && isGLEditableForInterCompany.equalsIgnoreCase("Y"))){
        	return true;
        }
        return false;
    }

	public boolean isReqERFQ(ProcureLineItem pli) {
    	Log.customer.debug("CatSAPValidateAccountingString : isReqERFQ : ***START***");
		boolean iSeRFQ = false;
		LineItemCollection plic =  pli.getLineItemCollection();
    	Log.customer.debug("CatSAPValidateAccountingString : isReqERFQ : plic " + plic);
		if (plic instanceof Requisition) {
			Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");
			Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");
			if ((iSeRFQB != null) && iSeRFQB.booleanValue()) {
				iSeRFQ = true;
			}
			if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {
				iSeRFQ = true;
			}
		}
    	Log.customer.debug("CatSAPValidateAccountingString : isReqERFQ : before returning : iSeRFQ " + iSeRFQ);
    	Log.customer.debug("CatSAPValidateAccountingString : isReqERFQ : ***END***");
		return iSeRFQ;
	}
	public boolean wasReqERFQ(ProcureLineItem pli) {    	Log.customer.debug("CatSAPValidateAccountingString : wasReqERFQ : ***START***");		boolean wasReqERFQ = false;		LineItemCollection plic =  pli.getLineItemCollection();    	Log.customer.debug("CatSAPValidateAccountingString : wasReqERFQ : plic " + plic);		if (plic instanceof Requisition) {			Boolean iSeRFQB = (Boolean) plic.getFieldValue("ISeRFQ");			Boolean iSeRFQRequisitionB = (Boolean) plic.getFieldValue("ISeRFQRequisition");			if ((iSeRFQB != null) && !iSeRFQB.booleanValue()) {				if ((iSeRFQRequisitionB != null) && iSeRFQRequisitionB.booleanValue()) {					wasReqERFQ = true;				}			}		}    	Log.customer.debug("CatSAPValidateAccountingString : wasReqERFQ : before returning : wasReqERFQ " + wasReqERFQ);    	Log.customer.debug("CatSAPValidateAccountingString : wasReqERFQ : ***END***");		return wasReqERFQ;	}
}
