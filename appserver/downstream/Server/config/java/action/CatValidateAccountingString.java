package config.java.action;

import java.util.Iterator;

import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
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

public class CatValidateAccountingString extends Action
{

    public void fire(ValueSource object, PropertyTable params)
        throws ActionExecutionException
    {
        if(object instanceof ProcureLineItem)
        {
            Log.customer.debug("%s *** Validate Acctng fire! ", "CatValidateAccountingString");
            ProcureLineItem pli = (ProcureLineItem)object;
            SplitAccountingCollection sac = pli.getAccountings();
            if(sac != null)
            {
                for(Iterator saci = sac.getAllSplitAccountingsIterator(); saci.hasNext();)
                {
                    SplitAccounting sa = (SplitAccounting)saci.next();
                    Log.customer.debug("%s *** split acctng: %s", "CatValidateAccountingString", sa);
                    CatAccountingCollector cats = getCatAccounting(sa);
                    Log.customer.debug("%s *** CatAccountingCollector: %s", "CatValidateAccountingString", cats);
                    if(cats != null)
                    {
                        Log.customer.debug("%s *** CatAccountingCollector(Fac)): %s", "CatValidateAccountingString", cats.getFacility());
                        CatAccountingValidator response = null;
                        try
                        {
                            response = AccountValidator.validateAccount(cats);
                            Log.customer.debug("%s *** CatAccountingValidator(AFTER): %s", "CatValidateAccountingString", response);
                            if(response != null)
                            {
                                Log.customer.debug("%s *** ResultCode: %s", "CatValidateAccountingString", response.getResultCode());
                                Log.customer.debug("%s *** Message: %s", "CatValidateAccountingString", response.getMessage());
                                if(response.getResultCode().equals("00"))
                                    sa.setFieldValue("ValidateAccountingMessage", ValidAccountingMsg);
                                else
                                    sa.setFieldValue("ValidateAccountingMessage", response.getMessage().concat(AdditionalMessage));
                            }
                        }
                        catch(Exception e)
                        {
                            Log.customer.debug("%s *** Exception: %s", "CatValidateAccountingString", e);
                        }
                    }
                }

            }
        }
    }

    public CatValidateAccountingString()
    {
    }

    public static CatAccountingCollector getCatAccounting(SplitAccounting sa)
    {
        String fac = (String)sa.getFieldValue("AccountingFacility");
        String dept = (String)sa.getFieldValue("Department");
        String div = (String)sa.getFieldValue("Division");
        String sect = (String)sa.getFieldValue("Section");
        String exp = (String)sa.getFieldValue("ExpenseAccount");
        String order = (String)sa.getFieldValue("Order");
        String misc = (String)sa.getFieldValue("Misc");
        return new CatAccountingCollector(fac, dept, div, sect, exp, order, misc);
    }

    private static final String classname = "CatValidateAccountingString";
    private static final String ValidAccountingMsg = Fmt.Sil("cat.vcsv1", "AccountDistributionValid");
    private static final String AdditionalMessage = Fmt.Sil("cat.vcsv1", "AccountingErrorGuidance");

}
