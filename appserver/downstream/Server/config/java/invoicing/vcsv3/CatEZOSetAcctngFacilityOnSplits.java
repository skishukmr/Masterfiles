
package config.java.invoicing.vcsv3;

import java.util.Iterator;

import ariba.approvable.core.LineItem;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.common.core.SplitAccountingCollection;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;

public class CatEZOSetAcctngFacilityOnSplits extends Action
{
    private static final String ClassName = "CatEZOSetAcctngFacilityOnSplits";

    public void fire(ValueSource object, PropertyTable propertytable)
        throws ActionExecutionException
    {
        if (object instanceof SplitAccounting) {

            //if (Log.customer.debugOn)
                Log.customer.debug("%s **** PROCEDEDING - 1! ****",ClassName);
            SplitAccounting sa = (SplitAccounting)object;
            LineItem li = sa.getLineItem();
            if (sa.getNumberInCollection() == 1 && li instanceof StatementCoreApprovableLineItem) {

                //if (Log.customer.debugOn)
                    Log.customer.debug("%s **** PROCEDEDING - 2 ****",ClassName);
                StatementCoreApprovableLineItem ili = (StatementCoreApprovableLineItem)li;
                SplitAccountingCollection sac = ili.getAccountings();
                if(sac != null)
                {
                    //if (Log.customer.debugOn)
                        Log.customer.debug("%s **** PROCEDEDING - 3 ****",ClassName);
                    BaseVector splits = sac.getSplitAccountings();
                    String acctngFac = (String)sa.getFieldValue("AccountingFacility");
                    if(splits != null && !StringUtil.nullOrEmptyOrBlankString(acctngFac) && splits.size() > 1)
                    {
                        //if (Log.customer.debugOn)
                            Log.customer.debug("%s **** PROCEDEDING - 4 ****",ClassName);
                        Iterator iterator = splits.iterator();
                        while (iterator.hasNext()) {
                            SplitAccounting split = (SplitAccounting)iterator.next();
                            if (!split.equals(sa)) {
                                split.setDottedFieldValue("AccountingFacility", acctngFac);
                                //if (Log.customer.debugOn)
                                    Log.customer.debug("%s **** PROCEDEDING - 5 (Copy Acctng Fac)!",ClassName);
                            }
                        }
                    }
                }
            }
        }
    }
}