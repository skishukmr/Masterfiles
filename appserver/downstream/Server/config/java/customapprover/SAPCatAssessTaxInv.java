/*
     This class is used as the entry point for the Assess Tax Button. The node initiates the class SAPCatTaxUtilTaxManager.java from this code.
	 This class gives the flexibility to put a approvalRequest as well as value object( This class can be invoked from a button as well a
	 s approver or watcher for future use

   Author: Divya
   Change History
	#	Change By	Change Date		Description
	=============================================================================================
	

*/
package config.java.customapprover;

import ariba.approvable.core.*;

import java.util.List;
import java.lang.Integer;


import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.tax.CatTaxUtil;


public class SAPCatAssessTaxInv extends Action
{

    public SAPCatAssessTaxInv ()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {

        Log.customer.debug("%s ::: The Tax trigger fired as expected", "SAPCatAssessTaxInv");
	ApprovalRequest  ar = null;
	
        InvoiceReconciliation invoicereconciliation = (InvoiceReconciliation)valuesource;
         SAPCatTaxUtilTaxManager.createRequestFile(ar,valuesource);
    
    }

    private static final String ClassName = "CatAssessTaxInv";
}
