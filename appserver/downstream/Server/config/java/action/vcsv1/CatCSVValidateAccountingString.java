package config.java.action.vcsv1;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.FieldProperties;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.procure.core.ProcureLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.common.CatAccountingCollector;
import config.java.common.CatAccountingValidator;
import config.java.integration.ws.AccountValidator;

public class CatCSVValidateAccountingString extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        // Code Added For NP Exception
					validateResponse(object);
    }

    public CatAccountingValidator validateResponse(ValueSource object)

    {
		CatAccountingValidator response = null;
		if(object instanceof ProcureLineItem)
		        {
		            Log.customer.debug("**AR** entering prog");
		            Log.customer.debug("%s *** Validate Acctng fire! ", "CatCSVValidateAccountingString");
		            ProcureLineItem pli = (ProcureLineItem)object;
		            Log.customer.debug("**AR** found pli" + pli.toString());
		            SplitAccountingCollection sac = pli.getAccountings();
		            Log.customer.debug("**AR** found sac " + sac.toString());
		            if(sac != null)
		            {
		                Log.customer.debug("**AR** ok, sac not null");
		                Iterator saci = sac.getAllSplitAccountingsIterator();
		                Log.customer.debug("**AR** found saci " + saci.toString());
		                while(saci.hasNext())
		                {
		                    Log.customer.debug("**AR** saci has next");
		                    SplitAccounting sa = (SplitAccounting)saci.next();
		                    Log.customer.debug("**AR** found sa " + sa.toString());
		                    CatAccountingCollector cats = getCatAccounting(sa);
		                    Log.customer.debug("**AR** found cats " + cats.toString());
		                    Log.customer.debug("%s *** CatAccountingCollector: %s", "CatCSVValidateAccountingString", cats);
		                    if(cats != null)
		                    {
		                        Log.customer.debug("**AR** cats not null 2");

		                        try
		                        {
		                            Log.customer.debug("**AR** inside try 1");
		                            response = AccountValidator.validateAccount(cats);
		                            Log.customer.debug("**AR** passed cats to validateaccount");
		                            //Log.customer.debug("**AR** response " + response.toString());
		                            Log.customer.debug("**AR** response " + response);
		                            Log.customer.debug("%s *** CatAccountingValidator(AFTER): %s", "CatCSVValidateAccountingString", response);
		                            //response = null;
		                            if(response != null)
		                            {
		                                Log.customer.debug("%s *** ResultCode: %s", "CatCSVValidateAccountingString", response.getResultCode());
		                                Log.customer.debug("%s *** Message: %s", "CatCSVValidateAccountingString", response.getMessage());
		                                FieldProperties fp = sa.getFieldProperties("ValidateAccountingMessage");
		                                if(response.getResultCode().equals("00"))
		                                {
		                                    sa.setFieldValue("ValidateAccountingMessage", ValidAccountingMsg);

	                				            // S. Sato - AUL - Added isImmutable check
		                    				if (!fp.isImmutable()) {
		                                        fp.setPropertyForKey("ValueStyle", "brandVeryDkText");
		                    				}
		                                } else
		                                {
		                                    StringBuffer sb = (new StringBuffer(InvalidAccountingMsg)).append(response.getMessage()).append(AdditionalMessage);
		                                    sa.setFieldValue("ValidateAccountingMessage", sb.toString());

	                				        // S. Sato - AUL - Added isImmutable check
		                    				if (!fp.isImmutable()) {
		                                        fp.setPropertyForKey("ValueStyle", "catRedTextSm");
		                    				}
		                                }
		                            }
		                            else
		                            {
										sa.setDottedFieldValue("ValidateAccountingMessage", "Accounting Web Serivce Down!!!");
											Log.customer.debug("%s *** Response Object is NULL");
								             return null;
									}

		                        }
		                        catch(Exception e)
		                        {
		                            Log.customer.debug("%s *** Exception: %s", "CatCSVValidateAccountingString", e);
		                        }
		                    }
		                }
		            }
        }
        return response;
	}

    public CatCSVValidateAccountingString()
    {
    }

    public static CatAccountingCollector getCatAccounting(SplitAccounting sa)
    {
        Log.customer.debug("%s::**AR** inside get cat actng::%s " ,classname, sa.toString());
        String fac = (String)sa.getFieldValue("AccountingFacility");
        String dept = (String)sa.getFieldValue("Department");
        String div = (String)sa.getFieldValue("Division");
        String sect = (String)sa.getFieldValue("Section");
        String exp = (String)sa.getFieldValue("ExpenseAccount");
        String order = (String)sa.getFieldValue("Order");
        String misc = (String)sa.getFieldValue("Misc");
        Log.customer.debug("**AR** returning from getcatactng");
        return new CatAccountingCollector(fac, dept, div, sect, exp, order, misc);
    }

    protected CatAccountingValidator callFS7200Placeholder(CatAccountingCollector collector)
        throws Exception
    {
		Log.customer.debug("%s::Inside callFS7200Placeholder Method ",classname);
        String result = "00";
        String message = null;
        Log.customer.debug("%s::Collector is...%s",classname,collector);
        if(collector != null)
        {
            String facility = collector.getFacility();
            if(facility != null && facility.equals("22"))
            {
                result = "005";
                message = "Facility 22 is not valid for this Department.";
            }
        }
        Log.customer.debug("%s::Result is..%s",classname,result);
        Log.customer.debug("%s::Message is..%s",classname,message);
        return new CatAccountingValidator(result, message);
    }

    private static final String classname = "CatCSVValidateAccountingString";
    private static final String ValidAccountingMsg = Fmt.Sil("cat.java.vcsv1", "AccountDistributionValid");
    private static final String InvalidAccountingMsg = Fmt.Sil("cat.java.vcsv1", "AccountDistributionNotValid");
    private static final String AdditionalMessage = Fmt.Sil("cat.java.vcsv1", "AccountingErrorGuidance");

}
