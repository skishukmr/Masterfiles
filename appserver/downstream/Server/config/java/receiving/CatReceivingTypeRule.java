/*
 * Changed by Ashwini on 21-04-09
 * --------------------------------------------------------------
 *Issue 722 : Prevent AutoReceipts on Purchase  Orders when the Purchase Requisition has a NextVersion field that is not NULL
 *
 * Changed by Naresh on 2nd May 2012
 * ----------------------------------------------
 * Issue 267 : Prevent AutoReceipts on Purchase  Orders when lineitem amount is $0.00 and also prevent Auto receipt when change order is submitted for $0.00 amount.
 *
 */

package config.java.receiving;

import java.text.ParseException;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
import ariba.base.core.ClusterRoot;
import ariba.basic.core.Money;
import ariba.contract.core.Contract;
import ariba.purchasing.core.Requisition;
import ariba.receiving.ReceivingTypeRule;
import ariba.receiving.core.Log;
import ariba.receiving.core.ReceivableLineItem;
import ariba.receiving.core.ReceivingTypeMethod;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;
import config.java.condition.vcsv1.CatAdditionalChargeLineItem;
public class CatReceivingTypeRule extends ReceivingTypeRule {


    public int findReceivingType(ReceivableLineItem lineItem) {

        int receivingType = 2;
		Log.customer.debug("%s findReceivingType: ReceivableLineItem="+lineItem, classname);

        //For paving. If Requester is from facility R8, no receipts required for the order.
		if(lineItem instanceof ariba.contract.core.ContractCoreApprovableLineItem) {
		    Contract ma = (Contract)lineItem.getLineItemCollection();
		    Log.customer.debug("%s findReceivingType: ma=" + ma, classname);
		    String requesterAccFac = (String)ma.getRequester().getFieldValue("AccountingFacility");
		    Log.customer.debug("%s findReceivingType: ma requesterAccFac=" + requesterAccFac, classname);
		    if(requesterAccFac != null && requesterAccFac.equals("R8"))
		        return ReceivingTypeMethod.noReceipt;
	    } else if (lineItem instanceof ariba.purchasing.core.POLineItem) {
			ariba.purchasing.core.POLineItem poline = (ariba.purchasing.core.POLineItem)lineItem;
			Requisition req = (Requisition)poline.getRequisition();
			Log.customer.debug("%s findReceivingType: req=" + req, classname);
			String requesterAccFac = (String)req.getRequester().getFieldValue("AccountingFacility");
			Log.customer.debug("%s findReceivingType: po requesterAccFac=" + requesterAccFac, classname);
		    if(requesterAccFac != null && requesterAccFac.equals("R8"))
		        return ReceivingTypeMethod.noReceipt;

		   //issue:267 changed by Naresh:If Line Item amount or order amount  is $0.00 amount, no receipts required for the order.

		   Money orderTotalAmt = null;
		   Money lineItmTotalAmt = null;
		try{
		   orderTotalAmt = (Money)lineItem.getLineItemCollection().getTotalCost();
		   Log.customer.debug("%s orderTotalAmount =" + orderTotalAmt, classname);
		   lineItmTotalAmt = (Money)lineItem.getAmount();
		   Log.customer.debug("%s lineItemTotalAmt =" + lineItmTotalAmt, classname);

		   if(orderTotalAmt == null || lineItmTotalAmt == null || orderTotalAmt.getApproxAmountInBaseCurrency().compareTo(Constants.ZeroBigDecimal) <= 0 || lineItmTotalAmt.getApproxAmountInBaseCurrency().compareTo(Constants.ZeroBigDecimal) <= 0)
		   		return ReceivingTypeMethod.noReceipt;
		}catch(Exception exception){
			Log.customer.debug("%s orderTotalAmount Exception=" + exception, classname);

		}
		   //End issue:267
	    }
	    //End

		//For additional charge line items, no receipts required.
        boolean isAdditionalCharge = CatAdditionalChargeLineItem.isAdditionalCharge(lineItem);
        Log.customer.debug("%s isAddtionalCharge returned from Condition=" + isAdditionalCharge, classname);
        if(isAdditionalCharge)
            return ReceivingTypeMethod.noReceipt;


        if(checkAutoReceiveOrder(lineItem.getLineItemCollection())) {
			return ReceivingTypeMethod.autoReceiving;
        } else
        if(checkAutoReceiveLineItem(lineItem)) {
            return  ReceivingTypeMethod.autoReceiving;
        } else {

            receivingType = super.findReceivingType(lineItem);
            Log.customer.debug("%s Receiving Type returned from SUPER=" + receivingType, classname);
		}

		Log.customer.debug("%s findRecevingType method returning =" + receivingType, classname);
        return receivingType;
    }


    private final boolean checkAutoReceiveOrder(LineItemCollection lic) {
		// Issue 722
			Log.customer.debug("%s::Line Item Collection::%s",classname,lic);
			String nextVerCheck = "notnull";
			ClusterRoot lineItemColl = (ClusterRoot)lic.getPreviousVersion();
			Log.customer.debug("%s *** Previous VERSION *** = " + lineItemColl, classname);
			if (lineItemColl != null){
			nextVerCheck = null;

			}
			// Issue 722
		Money orderTotalCost = null;
        if(autoReceiveOrderAmount == null) {
            String autoReceiveOrderAmountString = Base.getService().getParameter(lic.getPartition(), "Application.Procure.AutoReceiveOrderAmount");
			if(!StringUtil.nullOrEmptyOrBlankString(autoReceiveOrderAmountString)) {
				try {
					autoReceiveOrderAmount = User.parseMoney(autoReceiveOrderAmountString);
					Log.customer.debug("%s autoReceiveOrderAmount =" + autoReceiveOrderAmount, classname);
				} catch(ParseException parseexception) { }
			}
		}

		orderTotalCost = lic.getTotalCost();
		Log.customer.debug("%s orderTotalCost =" + orderTotalCost, classname);

		if(orderTotalCost == null
				|| ((orderTotalCost.getApproxAmountInBaseCurrency()).compareTo(Constants.ZeroBigDecimal) <= 0))
			return false;

		Log.customer.debug("%s orderTotalCostCompare =" + (orderTotalCost.compareTo(autoReceiveOrderAmount) <= 0), classname);

        return autoReceiveOrderAmount != null && nextVerCheck !=null && (orderTotalCost.compareTo(autoReceiveOrderAmount) <= 0) ;

	}

    private final boolean checkAutoReceiveLineItem(LineItem lineItem) {
		// Issue 722
		Log.customer.debug("%s::Line Item ::%s",classname,lineItem);
		String nextVerCheck1 = "notnull";
		LineItemCollection lineItemColl2 = (LineItemCollection)lineItem.getLineItemCollection();
		Log.customer.debug("%s::Line level Line Item Collection::%s",classname,lineItemColl2);
		ClusterRoot previosuVer = (ClusterRoot)lineItemColl2.getPreviousVersion();
		Log.customer.debug("%s *** Previous VERSION *** = " + previosuVer, classname);
		if (previosuVer != null){
		nextVerCheck1 = null;

			}
			// Issue 722
		Money lineAmt = null;
        if(autoReceiveLineItemAmount == null) {
            String autoReceiveLineItemAmountString = Base.getService().getParameter(lineItem.getPartition(), "Application.Procure.AutoReceiveLineItemAmount");

            if(!StringUtil.nullOrEmptyOrBlankString(autoReceiveLineItemAmountString)) {
                try {
                    autoReceiveLineItemAmount = User.parseMoney(autoReceiveLineItemAmountString);
                    Log.customer.debug("%s autoReceiveLineItemAmount =" + autoReceiveLineItemAmount, classname);
                } catch(ParseException parseexception) { }
			}
        }

		lineAmt = lineItem.getAmount();
		Log.customer.debug("%s lineAmt =" + lineAmt, classname);

		if(lineAmt == null
				||((lineAmt.getApproxAmountInBaseCurrency()).compareTo(Constants.ZeroBigDecimal) <= 0))
			return false;

		Log.customer.debug("%s lineAmtCompare =" + (lineAmt.compareTo(autoReceiveLineItemAmount) <= 0), classname);

        return autoReceiveLineItemAmount != null && nextVerCheck1 !=null && (lineAmt.compareTo(autoReceiveLineItemAmount) <= 0);
    }

    public CatReceivingTypeRule() {}

    public static final String classname = "CatReceivingTypeRule :";
    private static Money autoReceiveOrderAmount = null;
    private static Money autoReceiveLineItemAmount = null;
}

/* Receiving Type constants
autoReceiving 	1
manualByAmount 	3
manualByCount 	2
noReceipt 	4
noReceiving 	0
pcardAutoReceiving 	5
systemReceived 	0
*/
