package config.java.receiving;

import java.text.ParseException;

import ariba.approvable.core.LineItem;
import ariba.approvable.core.LineItemCollection;
import ariba.base.core.Base;
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
import config.java.condition.sap.CatSAPAdditionalChargeLineItem;


public class CatSAPReceivingTypeRule extends ReceivingTypeRule {


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

	    }
	    //End

		//For additional charge line items, no receipts required.
        boolean isAdditionalCharge = CatSAPAdditionalChargeLineItem.isAdditionalCharge(lineItem);
        Log.customer.debug("%s isAddtionalCharge returned from Condition=" + isAdditionalCharge, classname);
        if(isAdditionalCharge)
            return ReceivingTypeMethod.noReceipt;

		  Log.customer.debug("lineItem"+lineItem);
		  Log.customer.debug("lineItem.getLineItemCollection()"+lineItem.getLineItemCollection());

        if(checkAutoReceiveOrder(lineItem.getLineItemCollection())) {
			Log.customer.debug("Inside order");
            return ReceivingTypeMethod.systemReceived;
         } else
          if(checkAutoReceiveLineItem(lineItem)) {
			  Log.customer.debug("Inside orderlineitem");
              return  ReceivingTypeMethod.systemReceived;
		  }

         else {
            receivingType = super.findReceivingTypeByCCOrPN(lineItem);
            Log.customer.debug("***Fixed: %s Receiving Type returned from SUPER after method findReceivingTypeByCCOrPN=" + receivingType, classname);
		}

		Log.customer.debug("%s findRecevingType method returning =" + receivingType, classname);
        return receivingType;
    }


    private final boolean checkAutoReceiveOrder(LineItemCollection lic) {
		Money orderTotalCost = null;
        if (lic.getDottedFieldValue("CompanyCode.AutoReceiveOrderAmount") == null){
            String autoReceiveOrderAmountString = Base.getService().getParameter(lic.getPartition(), "Application.Procure.AutoReceiveOrderAmount");
              Log.customer.debug("autoReceiveOrderAmountString"+autoReceiveOrderAmountString);

			if(!StringUtil.nullOrEmptyOrBlankString(autoReceiveOrderAmountString)) {
				try {
					autoReceiveOrderAmount = User.parseMoney(autoReceiveOrderAmountString);
					Log.customer.debug("%s autoReceiveOrderAmount =" + autoReceiveOrderAmount, classname);
				} catch(ParseException parseexception) { }
			}
     }
     else{
	 				  Log.customer.debug("in else");
	 			      autoReceiveOrderAmount = (Money)lic.getDottedFieldValue("CompanyCode.AutoReceiveOrderAmount");
	 			      Log.customer.debug("CompanyCode autoReceiveOrderAmount"+autoReceiveOrderAmount);

		    }


		orderTotalCost = lic.getTotalCost();
		Log.customer.debug("%s orderTotalCost =" + orderTotalCost, classname);

		if(orderTotalCost == null
				|| ((orderTotalCost.getApproxAmountInBaseCurrency()).compareTo(Constants.ZeroBigDecimal) <= 0))
			return false;
        Log.customer.debug(" %s TotalCost " +orderTotalCost.ClassName,classname );
        Log.customer.debug("CompanyCode autoReceiveOrderAmount"+autoReceiveOrderAmount);
        Log.customer.debug(" %s autoReceiveOrderAmount " +autoReceiveOrderAmount.ClassName,classname );
        Log.customer.debug("%s autoreceive =" + (orderTotalCost.compareTo(autoReceiveOrderAmount)),classname);
          Log.customer.debug("%s autoreceive12 =" + (autoReceiveOrderAmount.compareTo(orderTotalCost)),classname);

		Log.customer.debug("%s orderTotalCostCompare =" + (orderTotalCost.compareTo(autoReceiveOrderAmount) <= 0), classname);

        return autoReceiveOrderAmount != null && (orderTotalCost.compareTo(autoReceiveOrderAmount) <= 0);
    }

   private final boolean checkAutoReceiveLineItem(LineItem lineItem) {
	Money lineAmt = null;
	LineItemCollection lic = (LineItemCollection) lineItem.getLineItemCollection();
	Log.customer.debug("lineItem1"+lineItem);
	Log.customer.debug("lineItem.getLineItemCollection()1"+lineItem.getLineItemCollection());

       if(lic.getDottedFieldValue("CompanyCode.AutoReceiveLineItemAmount") == null) {
           String autoReceiveLineItemAmountString = Base.getService().getParameter(lineItem.getPartition(), "Application.Procure.AutoReceiveLineItemAmount");
           Log.customer.debug("autoReceiveLineItemAmountString"+autoReceiveLineItemAmountString);


           if(!StringUtil.nullOrEmptyOrBlankString(autoReceiveLineItemAmountString)) {
               try {
                   autoReceiveLineItemAmount = User.parseMoney(autoReceiveLineItemAmountString);
                   Log.customer.debug("%s autoReceiveLineItemAmount =" + autoReceiveLineItemAmount, classname);
               } catch(ParseException parseexception) { }
		}
}
else{
		   		Log.customer.debug("in else1");
		   	   autoReceiveLineItemAmount = (Money)lic.getDottedFieldValue("CompanyCode.AutoReceiveLineItemAmount");
		   	    Log.customer.debug("autoReceiveLineItemAmount1"+autoReceiveLineItemAmount);
	  }


	lineAmt = lineItem.getAmount();
	Log.customer.debug("%s lineAmt =" + lineAmt, classname);

	if(lineAmt == null
			||((lineAmt.getApproxAmountInBaseCurrency()).compareTo(Constants.ZeroBigDecimal) <= 0))
		return false;
		//Log.customer.debug("lineAmt1" +lineAmt.Amount);
		Log.customer.debug("%s lineAmt1" +lineAmt.ClassName,classname);
		Log.customer.debug("%s autoReceiveLineItemAmount" +autoReceiveLineItemAmount.ClassName,classname);

    		Log.customer.debug("%s autoreceive1"+(lineAmt.compareTo(autoReceiveLineItemAmount)),classname);

	Log.customer.debug("%s lineAmtCompare =" + (lineAmt.compareTo(autoReceiveLineItemAmount) <= 0), classname);

    return autoReceiveLineItemAmount != null && (lineAmt.compareTo(autoReceiveLineItemAmount) <= 0);
}

    public CatSAPReceivingTypeRule() {}

    public static final String classname = "CatSAPReceivingTypeRule :";
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
