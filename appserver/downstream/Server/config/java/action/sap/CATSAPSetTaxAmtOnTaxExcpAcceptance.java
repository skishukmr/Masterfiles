/* Modification History:
*  Manoj .R        18/01/2012    WorkItem 245(Accept/Dispute Functionality)
********************************************************************************************************************/

package config.java.action.sap;

import java.math.BigDecimal;
import java.util.List;

import ariba.approvable.core.LineItemCollection;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseVector;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.basic.core.Money;
import ariba.invoicing.core.InvoiceException;
import ariba.invoicing.core.InvoiceExceptionType;
import ariba.invoicing.core.InvoiceReconciliation;
import ariba.invoicing.core.InvoiceReconciliationLineItem;
import ariba.procure.core.ProcureLineType;
import ariba.statement.core.StatementCoreApprovableLineItem;
import ariba.util.core.PropertyTable;
import ariba.util.log.Log;

public class CATSAPSetTaxAmtOnTaxExcpAcceptance extends Action {
    private static final String THISCLASS = "CATSAPSetTaxAmtOnTaxExcpAcceptance";

    public void fire(ValueSource object, PropertyTable params) throws ActionExecutionException {

        if (object instanceof ariba.invoicing.core.InvoiceException) {
        	InvoiceException invExcp = (InvoiceException)object;
        	BaseObject bo = (BaseObject)invExcp.getParent();
        	if(bo instanceof InvoiceReconciliation){
        		InvoiceReconciliation ir = (InvoiceReconciliation)bo;
        		String shortTaxEnabled = (String)ir.getDottedFieldValue("CompanyCode.ShortTaxEnabled");

        		Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** shortTaxEnabled: " + shortTaxEnabled);
        		if (shortTaxEnabled == null || !shortTaxEnabled.equalsIgnoreCase("Y")){
        			return;
        		}

        		Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** ir.getPartitionNumber(): " + ir.getPartitionNumber());
        		if(ir.getPartitionNumber() != 5){
        			return;
        		}
        		InvoiceExceptionType invExcpType = (InvoiceExceptionType)invExcp.getType();
        		Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** invExcpType: " + invExcpType);

        		if(invExcpType!=null && invExcpType.getUniqueName().equalsIgnoreCase("CATTaxCalculationFailed")){
        			int state = invExcp.getState();
        			Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** state: " + state);

                                //Changed state from 2 to 4. To resolve the issue with Accept/Dispute functionality(245)
        			if(state==4){
        				Money authPOTaxAmt = (Money)ir.getFieldValue("AuthPOTaxAmt");
        				Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** authPOTaxAmt: " + authPOTaxAmt);

        				Money totalTax = (Money)ir.getTaxAmount();
        				Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** totalTax: " + totalTax);

        				if(authPOTaxAmt!=null && totalTax!=null){
        					InvoiceReconciliationLineItem taxline = (InvoiceReconciliationLineItem)getTaxLine(ir);
        					Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** taxline " + taxline);

        					if(taxline!=null){
        						authPOTaxAmt = taxSumOfIRLines(ir);
        						taxline.setAmount(authPOTaxAmt);
        						Log.customer.debug("CATSAPSetTaxAmtOnTaxExcpAcceptance *** TaxAmount Set Completed ir.getTaxAmount() " + ir.getTaxAmount());
        					}
        				}
        			}
        		}
        	}
        }
    }

    private InvoiceReconciliationLineItem getTaxLine(InvoiceReconciliation ir){
    	InvoiceReconciliationLineItem taxline = null;
    	BaseVector lineitems = (BaseVector)ir.getLineItems();
    	for (int i=0; i <lineitems.size() ;i++){
    		InvoiceReconciliationLineItem li = (InvoiceReconciliationLineItem)lineitems.get(i);
    		ProcureLineType lineType = (ProcureLineType)li.getLineType();
    		if(lineType !=null && lineType.getCategory()==2)
    			taxline = li;
    	}
		return taxline;
    }

    // This method does not consider the tax lines, returns sum of tax amt of all procure lines.
    public Money taxSumOfIRLines(LineItemCollection lic)
    {
    	Money taxAmtAtLic = new Money(new BigDecimal(0),lic.getTotalCost().getCurrency());
    	List lineitems = (List)lic.getLineItems();
    	for(int i=0;i<lineitems.size();i++){
    		StatementCoreApprovableLineItem li = (StatementCoreApprovableLineItem)lineitems.get(i);
    		Money lineAmt = (Money)li.getTaxAmount();
    		Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : lineAmt "+lineAmt);
    		if(lineAmt!=null && li.getLineType()!=null && li.getLineType().getCategory()== ProcureLineType.LineItemCategory){
    			taxAmtAtLic = taxAmtAtLic.add(lineAmt);
    		}
    	}
    	Log.customer.debug("CatSAPUpdateLineTaxAmount: getSumOfLineTax : taxAmtAtLic "+taxAmtAtLic);
    	return taxAmtAtLic;
    }


}
