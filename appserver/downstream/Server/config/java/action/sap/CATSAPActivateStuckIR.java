package config.java.action.sap;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovalRequest;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;


public class CATSAPActivateStuckIR  extends Action{

	private static final String classname = "CATSAPActivateStuckIR";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

    	try {
    		if(object instanceof ariba.approvable.core.ApprovalRequest){
    			ApprovalRequest approvalRequest = (ApprovalRequest)object;
            	Approvable app = (Approvable)approvalRequest.getApprovable();
            	if(app instanceof InvoiceReconciliation){
            		//InvoiceReconciliationLineItem irli = (InvoiceReconciliationLineItem)bo;
            		InvoiceReconciliation ir = (InvoiceReconciliation)app;
            		Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** ir.getPartitionNumber(): " + ir.getPartitionNumber());
            		if(ir.getPartitionNumber() != 5){
            			return;
            		}

            			int state = approvalRequest.getState();
            			Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** state: " + state);

            			if(state==1){
						Log.customer.debug("%s: Getting the InvoiceReconciliation objects stuck in approving.....", classname);

			            String irInApprovingQuery="SELECT  ir FROM ariba.invoicing.core.InvoiceReconciliation ir "
			                                        +"where ir.ApprovedState=2 "
			                                        +"and ir not in (SELECT distinct ir1 FROM ariba.invoicing.core.InvoiceReconciliation ir1 "
			                                        +"JOIN ApprovalRequest as ir1ar USING ir1.ApprovalRequests "
				                                    +"WHERE ir1.ApprovedState=2 and ir1ar.State=2) "
				                                    +" AND ir.UniqueName like '" +ir.getUniqueName()+ "'";

						AQLQuery irInApproving = AQLQuery.parseQuery(irInApprovingQuery);
						AQLResultCollection results = Base.getService().executeQuery(irInApproving, baseOptions());
						Log.customer.debug("%s: the query is %s", classname, irInApproving);

						if(results.getErrors() != null) {
							Log.customer.debug("%s:ERROR RESULTS =:%s ", classname, results.getErrors());
							throw new Exception("Error In Results: " + results.getErrors());
					    }

						while (results.next()) {
							BaseId irid = results.getBaseId(0);
				            InvoiceReconciliation ir1 = (InvoiceReconciliation)irid.get();
							Log.customer.debug("%s: IR Object got from the query is "+ ir1, classname);
							ir1.updateApprovals();
							Base.getSession().transactionCommit();
					    }
						Log.customer.debug("%s:Done ", classname);

            			}
            			else{
            				return;
            			}
            		}else{
        			return;
        		}

    		}
    	}catch(Exception e) {
			Log.customer.debug("Error : " + e.toString(), e);
		}

    }
	public AQLOptions baseOptions() {
		   AQLOptions options = new AQLOptions();
		   options.setRowLimit(0);
		   options.setUserLocale(Base.getSession().getLocale());
		   options.setUserPartition(Base.getSession().getPartition());
		   return options;
		}
}
