/*
	CATInvoiceExceptionEditable.java

	Change History:
	Chandra 14/03/08   Issue 759, For MFG1, hide the Accept & Cannot Resolve button
	Santanu 27/11/08   For SAP hide Dipute button
	Shailaja 27/08/09  Issue 961 For UK,made Accept button visible for all exceptions except PO/MAReceivedQtyVariance
	Shailaja 30/09/09  Issue 1000 Hide Accept button for SAP POReceived Qty variance
*/

package config.java.invoicing;

import ariba.base.fields.ConditionResult;
import ariba.base.fields.ValueInfo;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.condition.InvoiceExceptionEditable;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.log.Log;

public class CATInvoiceExceptionEditable extends InvoiceExceptionEditable {

    public boolean evaluate(Object value, PropertyTable params) {
        Log.customer.debug("%s ::: Entering the evaluate method", "CATInvoiceExceptionEditable");
        return evaluateAndExplain(value, params) == null;
    }

    public ConditionResult evaluateAndExplain(Object value, PropertyTable params) {

        Log.customer.debug("%s ::: Entering the evaluate and explain method", "CATInvoiceExceptionEditable");
        InvoiceException exception = (InvoiceException)propertyForKey(params, "Exception");
        if(exception == null) {
            Log.customer.debug("%s ::: Returning null as exception is null", "CATInvoiceExceptionEditable");
            return null;
        }
        User user = (User)propertyForKey(params, "User");
        if(user == null) {
            Log.customer.debug("%s ::: The passed in user is null hence fetching the session user", "CATInvoiceExceptionEditable");
            user = User.getEffectiveUser();
        }

        Integer operation = (Integer)propertyForKey(params, "operation");
        int opcode = operation != null ? operation.intValue() : 1;

        Log.customer.debug("%s ::: The operation code passed into the method is: " + opcode, "CATInvoiceExceptionEditable");

        if("mfg1".equals(exception.getPartition().getName())) {
            Log.customer.debug("%s ::: Current partition is mfg1 and opcode="+opcode, "CATInvoiceExceptionEditable");
            if(opcode == InvoiceExceptionEditable.Dispute) {
                Log.customer.debug("%s ::: Encountered a Dispute operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Dispute Functionality is not available for UK");
            }
         //issue 961
         /* if(opcode == InvoiceExceptionEditable.Accept) {
                Log.customer.debug("%s ::: Encountered a Accept operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Accept Functionality is not available for UK");
            } */
            if(("POReceivedQuantityVariance".equals(exception.getType().getUniqueName())
                        || "MAReceivedQuantityVariance".equals(exception.getType().getUniqueName()))
                && opcode == InvoiceExceptionEditable.Accept)
                {
                Log.customer.debug("%s ::: Encountered a Accept operation code for RecvdQuantVariance hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Accept Functionality for RecvdQuantVariance is not available for US");

	           }
            if(opcode == InvoiceExceptionEditable.CannotResolve) {

                Log.customer.debug("%s ::: Encountered a CannotResolve operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("CannotResolve Functionality is not available for UK");
            }
        }
        if("pcsv1".equals(exception.getPartition().getName())) {
            Log.customer.debug("%s ::: Current partition is pcsv1 and opcode="+opcode, "CATInvoiceExceptionEditable");
            if(opcode == InvoiceExceptionEditable.CannotResolve) {
                Log.customer.debug("%s ::: Encountered a Cannot Resolve operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Cannot Resolve Functionality is not available for US");
            }
            if(("POReceivedQuantityVariance".equals(exception.getType().getUniqueName())
            		|| "MAReceivedQuantityVariance".equals(exception.getType().getUniqueName()))
            	&& opcode == InvoiceExceptionEditable.Accept) {
                Log.customer.debug("%s ::: Encountered a Accept operation code for RecvdQuantVariance hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Accept Functionality for RecvdQuantVariance is not available for US");
            }
        }
        if("ezopen".equals(exception.getPartition().getName())) {
            Log.customer.debug("%s ::: Current partition is ezopen and opcode="+opcode, "CATInvoiceExceptionEditable");
            if(opcode == InvoiceExceptionEditable.Dispute) {
                Log.customer.debug("%s ::: Encountered a Dispute operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult(Msg_Dispute);
            }
            if(opcode == InvoiceExceptionEditable.Accept && ("POReceivedQuantityVariance".equals(exception.getType().getUniqueName())
            	|| "MAReceivedQuantityVariance".equals(exception.getType().getUniqueName()))) {
                Log.customer.debug("%s ::: Encountered a Accept operation code for RecvdQuantVariance hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult(Msg_AcceptRcvdQtyVar);
            }
        }
        if("SAP".equals(exception.getPartition().getName())) {
            Log.customer.debug("%s ::: Current partition is SAP and opcode="+opcode, "CATInvoiceExceptionEditable");
           //Issue 1000 : Hiding Accept button, and Cannot resolve but need to show Dispute button
           if(opcode == InvoiceExceptionEditable.CannotResolve) {
		   Log.customer.debug("%s ::: Encountered a Cannot Resolve operation code hence returning error", "CATInvoiceExceptionEditable");
		   return new ConditionResult("Cannot Resolve Functionality is not available for US");
            }
           /*if(opcode == InvoiceExceptionEditable.Dispute) {
                Log.customer.debug("%s ::: Encountered a Dispute operation code hence returning error", "CATInvoiceExceptionEditable");
                return new ConditionResult("Dispute Functionality is not available for SAP");
            } */

			  if(("POReceivedQuantityVariance".equals(exception.getType().getUniqueName())
			       || "MAReceivedQuantityVariance".equals(exception.getType().getUniqueName()))
			       && opcode == InvoiceExceptionEditable.Accept) {
	Log.customer.debug("%s Ooperation code for RecvdQuantVariance hence returning error", "CATInvoiceExceptionEditable");
	 return new ConditionResult("Accept Functionality for RecvdQuantVariance is not available for MACH1");
			            }

        }
        Log.customer.debug("%s :::Custom conditions not satisfied - calling super evaluate ", "CATInvoiceExceptionEditable");
        ConditionResult superCR = super.evaluateAndExplain(value, params);
        String errorMessage = null;
        if(superCR != null) {
            errorMessage = superCR.getFirstError();
            if(errorMessage == null)
                errorMessage = superCR.getFirstWarning();
        }

        Log.customer.debug("%s ::: Returning the super evaluate result: " + errorMessage, "CATInvoiceExceptionEditable");
        return super.evaluateAndExplain(value, params);
    }

    protected Object propertyForKey(PropertyTable params, String name) {
        if(params == null)
            return null;
        else
            return params.getPropertyForKey(name);
    }

    protected ConditionResult getErrorMessage(String key) {
        return new ConditionResult(ResourceService.getString("ariba.procure.core", key));
    }

    protected ValueInfo[] getParameterInfo() {
        return ParameterInfo;
    }

    public CATInvoiceExceptionEditable() {
    }

    private static final String ClassName = "CATInvoiceExceptionEditable";
    private static final String Msg_Dispute = Fmt.Sil("cat.invoicejava.vcsv3", "Condition_DisputeNotAllowed");
    private static final String Msg_AcceptRcvdQtyVar = Fmt.Sil("cat.invoicejava.vcsv3", "Condition_AcceptRcvdQtyVarNotAllowed");
    private static final String Msg_CannotResolve = Fmt.Sil("cat.invoicejava.vcsv3", "Condition_CannotResolveNotAllowed");
    private static ValueInfo ParameterInfo[];

    static
    {
        ParameterInfo = (new ValueInfo[] {
            new ValueInfo("Exception", 0, "ariba.invoicing.core.InvoiceException"), new ValueInfo("operation", 0, Constants.IntegerType), new ValueInfo("User", 0, "ariba.user.core.User")
        });
    }
}
