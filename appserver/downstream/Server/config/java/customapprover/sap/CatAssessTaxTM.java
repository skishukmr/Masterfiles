/*
	Author: Archana
    
		This class gets called when there is IP manager is added to the requisition and this will call
	    the CatSAPTaxCustomApproverTM class which reads lineItems and makes call to webservice.

		 Change History
	#	Change By	Change Date		Description
	=============================================================================================
*/
package config.java.customapprover.sap;

import ariba.approvable.core.*;

import java.util.List;
import java.lang.Integer;

import ariba.contract.core.ContractRequest;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.purchasing.core.Requisition;
import ariba.user.core.User;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;
import config.java.tax.CatTaxUtil;

/*
 * AUL : Remove all debugon statements.
 */

public class CatAssessTaxTM extends Action
{

    public CatAssessTaxTM()
    {
    }

    public void fire(ValueSource valuesource, PropertyTable propertytable)
        throws ActionExecutionException
    {

        Log.customer.debug("%s ::: The Tax trigger fired as expected", "CatAssessTaxTM");
        ApprovalRequest  ar = null;	
        if (valuesource instanceof ariba.contract.core.ContractRequest) {
        	ContractRequest rqtn = (ContractRequest)valuesource;  
            CatSAPTaxCustomApproverTMCR.notifyApprovalRequiredTM(ar,valuesource);
        }else if (valuesource instanceof ariba.purchasing.core.Requisition) {
         Requisition rqtn = (Requisition)valuesource; 
        CatSAPTaxCustomApproverTM.notifyApprovalRequiredTM(ar,valuesource);
       }
     }
    private static final String ClassName = "CatAssessTaxTM";
}
