package config.java.condition.sap;

import ariba.base.core.BaseVector;
import ariba.base.fields.Condition;
import ariba.base.fields.ConditionEvaluationException;
import ariba.base.fields.ConditionResult;
import ariba.contract.core.Contract;
import ariba.procure.core.ProcureLineItem;
import ariba.procure.core.ProcureLineItemCollection;
import ariba.purchasing.core.ReqLineItem;
import ariba.util.core.Fmt;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CatSAPValidReferenceLineNumber extends Condition {

	private static final String THISCLASS = "CatSAPValidReferenceLineNumber";

	private int errorCode = 0;

    public boolean evaluate(Object object, PropertyTable params) throws ConditionEvaluationException {

        if (object instanceof ProcureLineItem) {
            errorCode = validReferenceLine((ProcureLineItem)object);
        }
        Log.customer.debug("CatSAPValidReferenceLineNumber *** errorCode: " + errorCode);
        return errorCode==0;
    }

    public ConditionResult evaluateAndExplain(Object object, PropertyTable params)
		throws ConditionEvaluationException  {

	    if(!evaluate(object, params)) {
	        String errorMsg = "Error_ValidReferenceLine_Default";
	        switch (errorCode) {
	        	case 1:
	        	    errorMsg = "Error_ValidReferenceLine_NotSet";
	        	    break;
        	    case 2:
        	        errorMsg = "Error_ValidReferenceLine_BadLineNum";
        	        break;
    	        case 3:
    	            errorMsg = "Error_ValidReferenceLine_NonMaterial";
    	            break;
    	        case 4:
	          		errorMsg = "Error_ValidReferenceLine_ForwardLine";
	                break;
	            case 5:
	                errorMsg = "Error_ValidReferenceLine_Supplier";
	                break;
                case 6:
                    errorMsg = "Error_ValidReferenceLine_Contract";
                    break;
                case 7:
                    errorMsg = "Error_ValidReferenceLine_DeletedLine";
	        }
			return new ConditionResult(Fmt.Sil("cat.java.sap",errorMsg));
		}
		return null;
    }

    public static int validReferenceLine (ProcureLineItem acLine) {

        int code = 0;
        Integer refNumInt = (Integer)acLine.getFieldValue("ReferenceLineNumber");
        if (refNumInt != null) {
	        int refNum = refNumInt.intValue();
	        if (CatSAPAdditionalChargeLineItem.isAdditionalCharge(acLine)) {
	            if (refNum == 0) {
	                code = 1;  // no reference set
	            }
	            else {
	                ProcureLineItemCollection plic = (ProcureLineItemCollection)acLine.getLineItemCollection();
	                if (plic != null) {
	                    ProcureLineItem matLine = (ProcureLineItem)plic.getLineItem(refNum);
	                    if (matLine == null)
	                        code = 2;   // references a non-existant line
	                    else if (CatSAPAdditionalChargeLineItem.isAdditionalCharge(matLine))
	                        code = 3;   // references a non-material line
	                    /* removed per R4 rqmt change */
	                    //      else if (!matLine.getIsAdHoc())
	                    //          code = 4;   // references a catalog line
	                    else if (matLine.getSupplierLocation()!= acLine.getSupplierLocation())
	                        code = 5;   // refenences a different supplier
	                    else if (acLine instanceof ReqLineItem) {
		                    // 04.25.06 (ks) added to not permit referencing line below AC line
		    	            if (refNum > acLine.getNumberInCollection()) {
		    	                code = 4;
		    	            }
		                    // 05.22.06 (ks) Added to prevent referencing a deleted line on V2+
		                    else if (plic.getPreviousVersion() != null) {
		                        BaseVector deletions = plic.getDeletedLineItems();
		                        if (!deletions.isEmpty() && deletions.contains(matLine)) {
		                            // mat line referenced has been deleted (not valid)
		                            code = 7;
		                        }
		                    }
		    	            else if (code == 0) {
		    	                // 05.22.06 (ks) - removed acContract.getTermType() == 2 condition from IF
		    	                Contract acContract = ((ReqLineItem)acLine).getMasterAgreement();
		    	                if (acContract != null && acContract != ((ReqLineItem)matLine).getMasterAgreement())
		    	                    code = 6;
		    	            }
	                    }
	                }
	            }
	        } else {
	            int nic = acLine.getNumberInCollection();
	            if (refNum != nic) {
	        		Log.customer.debug("%s *** RESET RefNum for Material Line!", THISCLASS);
	                acLine.setFieldValue("ReferenceLineNumber",new Integer(nic));
	            }
	        }
        }
        else {
            code = 1;  // pre-R4 approvable (treat null as invalid)
        }
        Log.customer.debug("CatSAPValidReferenceLineNumber *** code: " + code);
        return code;
    }


	public CatSAPValidReferenceLineNumber() {
		super();
	}

}
