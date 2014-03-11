/*
 * Created by KS on Sep 25, 2005
 * --------------------------------------------------------------
 * Used to set full accounting distibution for display in LineItemSimpleGeneralFields group
 * Kavitha Udayasankar: Condition to check InvoiceReconciliationLineItem is added to display AccountingDistribution
 * 
 * Issue 355 : Accounting distribution different in summary page and Line item details page.
 * Date : 05th Aug 2011
 * Modified By : Nandini Bheemaiah
 *
 * Description : The Derived Accounting Distribution in the summary page was different from the Accounting details in 
 * Line Item details page. This was because for some of the PR, though the splitAccounting is just one, the NumberInCollection field
 * was set to 2. Due to which the DerivedAccountingDistribution field was not updated in the summary page as in the Line items detail page
 *
 * Fix Description : The condition sa.getNumberInCollection()==1 is removed from the loop to make sure that it however takes the first splitAccounting
 * into consideration and update accordingly.
 */
package config.java.action.vcsv1;

import ariba.approvable.core.LineItem;
import ariba.base.core.ClusterRoot;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.common.core.SplitAccounting;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;
import ariba.invoicing.core.InvoiceReconciliationLineItem;


public class CatSetDerivedAccountDistribution extends Action {

    private static final String THISCLASS = "CatSetDerivedAccountDistribution";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof SplitAccounting) {
            SplitAccounting sa = (SplitAccounting)object;
            LineItem li = sa.getLineItem();
			//Issue 355 : Removing Validation sa.getNumberInCollection()==1. Commented below.

           // if (sa.getNumberInCollection() == 1 && li instanceof ReqLineItem||li instanceof InvoiceReconciliationLineItem) {
			   if (li instanceof ReqLineItem||li instanceof InvoiceReconciliationLineItem) {
				
				Log.customer.debug("Entering the loop to obtain the DerivedAccountingDistribution");
				
                StringBuffer dist = new StringBuffer();

                ClusterRoot type = (ClusterRoot)li.getFieldValue("AccountType");
                if (type != null)
                    dist.append(type.getUniqueName()).append(" - ");

                String aField = (String)sa.getFieldValue("AccountingFacility");
                if (!StringUtil.nullOrEmptyOrBlankString(aField))
                    dist.append(aField);
                aField = (String)sa.getFieldValue("Department");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                aField = (String)sa.getFieldValue("Division");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                aField = (String)sa.getFieldValue("Section");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                aField = (String)sa.getFieldValue("ExpenseAccount");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                aField = (String)sa.getFieldValue("Order");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                aField = (String)sa.getFieldValue("Misc");
                if(!StringUtil.nullOrEmptyOrBlankString(aField))
                {
                    dist.append("-");
                    dist.append(aField);
                }
                Log.customer.debug("%s **** acct dist SB: %s",THISCLASS,dist);
                li.setFieldValue("DerivedAccountDistribution",dist.toString());
            }
        }
    }

    public CatSetDerivedAccountDistribution() {
        super();
    }


}
