/*
 * Created by Amit on Aug 22, 2007
 * --------------------------------------------------------------
 * Used to validate a PO for receipt back out
 */
package config.java.receiptbackout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Action;
import ariba.base.fields.ActionExecutionException;
import ariba.base.fields.ValueSource;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.purchasing.core.ReqLineItem;
import ariba.purchasing.core.Requisition;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.util.core.ListUtil;
import ariba.util.core.PropertyTable;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CatValidatePOForBackOut extends Action {

	private static final String nullPOError = ResourceService.getString("aml.cat.receiptbackout","nullPOError");
	private static final String poStatusError = ResourceService.getString("aml.cat.receiptbackout","poStatusError");
	private static final String poClosedError = ResourceService.getString("aml.cat.receiptbackout","poClosedError");
	private static final String prNextVersionError = ResourceService.getString("aml.cat.receiptbackout","prNextVersionError");
	private static final String poInvoicedError = ResourceService.getString("aml.cat.receiptbackout","poInvoicedError");
	private static final String poNotFoundError = ResourceService.getString("aml.cat.receiptbackout","poNotFoundError");
	private static final String poValidForBackOut = ResourceService.getString("aml.cat.receiptbackout","poValidForBackOut");

    public void fire(ValueSource object, PropertyTable params)
        		throws ActionExecutionException {


		String poUniqueName = null;

		Approvable eform = (Approvable)object;

		eform.setFieldValue("ValidateErrorMessage", null);
		eform.setFieldValue("ValidateWarningMessage",null);
		eform.setFieldValue("ReceiptsInfo",null);

		//String validateMessage = (String)eform.getFieldValue("ValidateCheckMessage");
		//Log.customer.debug("validateCheckMessage value is "+validateMessage);

		//if(validateMessage == null)
		//{
		eform.setFieldValue("ValidBackOutOrders", null);

        poUniqueName = (String)eform.getFieldValue("OrderNo");

		if(poUniqueName == null) {
			Log.customer.debug("PO not entered");
			eform.setFieldValue("ValidateErrorMessage",nullPOError);
			return;
		}
		poUniqueName=poUniqueName.toUpperCase(); // convert PO number to upper case

        String queryText = "SELECT po FROM ariba.purchasing.core.DirectOrder AS po WHERE OrderID='"+poUniqueName+"'";
        AQLQuery query = AQLQuery.parseQuery(queryText);
		AQLOptions options =  new AQLOptions(Base.getSession().getPartition());
        Log.customer.debug("%s : query in here=%s", classname, query.toString());
        AQLResultCollection res = Base.getService().executeQuery(query, options);

       	if(!ListUtil.nullOrEmptyList(res.getErrors())) {
            String err = res.getErrorStatementText();
            Log.customer.debug("%s: ERROR: %s",classname, err);
        }

		if(res.getSize() == 0) {
			eform.setFieldValue("ValidateErrorMessage",poNotFoundError);
			return;
		}

		while (res.next()) {
       	  	try{
            	Log.customer.debug("%s: the object got =" +res.getObject(0), classname);

          		BaseId objBaseId = (BaseId)res.getObject(0);
            	ClusterRoot cr = (ClusterRoot)objBaseId.get();
          		PurchaseOrder dor = (PurchaseOrder)cr ;

				Log.customer.debug(" Calling Validate() - validate PO for receipt back out");
				if(validate(dor,eform)==true){
					Log.customer.debug(" All checks done - Valid PO for back out ");
					eform.setFieldValue("CheckValidateButton","VALID PO");
	   				eform.setFieldValue("ValidateErrorMessage",poValidForBackOut);
				}
				else{
					Log.customer.debug(" Not a Valid PO ");
					eform.setFieldValue("CheckValidateButton","INVALID PO");
				}
	      	} //end of try
          	catch (Exception e) {
					eform.setFieldValue("ValidateErrorMessage", "ERROR: " +e.toString());
					Log.customer.debug("%s *** ERROR=%s", classname, e.toString());
					return;
		  	}
		 } // end of while

        Log.customer.debug(" PO is validated for back out ");
  	}

	public static boolean validate(PurchaseOrder dor,Approvable eform) {

		Log.customer.debug(" Inside the validate() method");

		// Call function checkPOStatus(dor);
		// check if PO is in canceling,canceled,ordering or ordered - if so dont back out

		Log.customer.debug(" Check 1 - Check for PO status string ");
		if(checkPOStatus(dor)) {
			Log.customer.debug("PO is in canceling / canceled / ordering / ordered - no back out");
			eform.setFieldValue("ValidateErrorMessage",poStatusError);
			return false;
		}
		Log.customer.debug("PO is not in canceling , canceled or ordering , ordered");
		Log.customer.debug(" Check 1 - completed ");


		// call function checkPOCloseOrder(dor)
		// check if the PO is closed - if so dont back out

		Log.customer.debug(" Check 2 - Check for POCloseOrder status");
		if(checkPOCloseOrder(dor)) {
			Log.customer.debug(" Not a Valid PO - Check for PO CloseOrder Status ");
			eform.setFieldValue("ValidateErrorMessage",poClosedError);
			return false;
		}
		Log.customer.debug("PO is not Closed");
		Log.customer.debug(" Check 2 - completed ");

		// call function checkPRNextVersion(dor) - TBD
		//Check if PR has next version in composing - if so No back out

		Log.customer.debug(" Check 3 - Check for PR NextVersion ");
		if(checkPRNextVersion(dor)) {
			Log.customer.debug(" Not a Valid PO - Check for PR Next Version Status ");
			eform.setFieldValue("ValidateErrorMessage",prNextVersionError);
			return false;
		}
		Log.customer.debug("Next Version is null");
		Log.customer.debug(" Check 3 - completed");

		//call function checkInvoiceForPO(dor)
		//check if any PO has invoice - if so dont back

		Log.customer.debug(" Check 4 - Check for Invoice against PO ");
		if(checkInvoiceForPO(dor,eform)) {
			Log.customer.debug(" Not a Valid PO - PO has Invoice ");
			eform.setFieldValue("ValidateErrorMessage",poInvoicedError);
			return false;
		}
		Log.customer.debug("Invoice not present");
		Log.customer.debug(" Check 4 - completed");

		Log.customer.debug("All checks completed - from validate method");
		return true;
	}

    public static boolean checkPOStatus(PurchaseOrder dor) {
		String poStatus = null;
		Log.customer.debug("In the method checkPOStatus : Check 1 - Check for PO status string");
		poStatus = (String)dor.getDottedFieldValue("StatusString");

		// check for po status- if cancelling / ordering / canceled / ordered - no back out
		if(poStatus.equalsIgnoreCase("Canceled")||poStatus.equalsIgnoreCase("Ordering")||poStatus.equalsIgnoreCase("Canceling")||poStatus.equalsIgnoreCase("Ordered")) {
			Log.customer.debug(" Not a Valid PO as the status of PO is %s",poStatus);
			Log.customer.debug(" Not a Valid PO - Check for PO Status String ");
			return true;
		}
		Log.customer.debug(" Status of PO is %s",poStatus);
		return false;
	}


	public static boolean checkPOCloseOrder(PurchaseOrder dor) {
		Log.customer.debug(" In the method checkPOCloseOrder : Check 2 - Check for POCloseOrder status");
		Boolean closeOrderValue = (Boolean)dor.getDottedFieldValue("CloseOrder");
		Log.customer.debug(" Got the Boolean closeOrder value");
		boolean closeOrderBooleanValue = false;
		if(closeOrderValue != null)
			closeOrderBooleanValue= closeOrderValue.booleanValue();
		if(closeOrderBooleanValue == true) {
				// if PO CloseOrder field is true => PO is closed => no back out
				Log.customer.debug(" The PO is closed ");
				Log.customer.debug(" Throwing Error message on the Eform - Check 2");
				return true;
		}
		Log.customer.debug(" Valid PO as the CloseOrder status of PO is not true");
		return false;
	}


	public static boolean checkPRNextVersion(PurchaseOrder dor) {
		String nextVersionStatus = null;
		Log.customer.debug(" In the method checkPRNextVersion : Check 3 - Check for PR's next version status");

		// get PR from PO line
		POLineItem pol = (POLineItem)dor.getLineItem(1);
		Requisition req = (Requisition)pol.getRequisition();

		//get the next verison of the PR
		Requisition nextVer = (Requisition)req.getNextVersion();
		Log.customer.debug("Got the Next Version ");
		if(nextVer != null) {
			// if PR next version status is composing - then no back out
			Log.customer.debug(" Next version is present");
			nextVersionStatus = (String)nextVer.getDottedFieldValue("StatusString");
			Log.customer.debug(" The status of next version of the requisition is %s",nextVersionStatus);
			if(nextVersionStatus.equalsIgnoreCase("Composing")) {
				Log.customer.debug("The next version of the requisition is in composing");
				return true;
			}
		}
		return false;
	}


	public static boolean checkInvoiceForPO(PurchaseOrder dor,Approvable eform) {
		Log.customer.debug(" In the method checkInvoiceForPO : Check 4 - Check for Invoices against the PO's");

		// get all the PR line item ie all split orders
		POLineItem pol = (POLineItem)dor.getLineItem(1);
		Requisition req = (Requisition)pol.getRequisition();
		BaseVector lines = req.getLineItems();
		int lineCount = lines.size();
		Log.customer.debug("Number of line items : %s",lineCount);

		List orderList=new ArrayList();
		List recePONum = new ArrayList();

	 	//check if any split PO has invoice
		for (int i = 0; i < lineCount; i++) {
			ReqLineItem rli = (ReqLineItem)lines.get(i);
			String orderNumber = null;
			orderNumber = (String)rli.getDottedFieldValue("Order.UniqueName");
			Log.customer.debug(" THE PO number is "+orderNumber);

			String orderStatus = (String)rli.getDottedFieldValue("Order.StatusString");
			Log.customer.debug(" THE PO Status is "+orderStatus);

			//check for invoice against the PO
			String invQueryText = "SELECT ir from ariba.invoicing.core.InvoiceReconciliation AS ir WHERE \"Order\".UniqueName = '"+orderNumber+"'";
			AQLQuery invquery = AQLQuery.parseQuery(invQueryText);
			AQLOptions invoptions =  new AQLOptions(Base.getSession().getPartition());
			Log.customer.debug("%s : query in here =%s", classname, invquery.toString());
		    AQLResultCollection invres = Base.getService().executeQuery(invquery, invoptions);

			if(!ListUtil.nullOrEmptyList(invres.getErrors())) {
				 String inverr = invres.getErrorStatementText();
				 Log.customer.debug("%s: ERROR: %s",classname, inverr);
			}
			// if any split PO has invoice - then no back out
			if(invres.getSize() != 0) {
				 Log.customer.debug("Split PO :"+(i+1)+"has Invoice");
				 return true;
			}
			Log.customer.debug("Split PO "+(i+1)+" has no Invoice");

			// to display only those split orders which are not in ordered state
			if(orderStatus.equalsIgnoreCase("Receiving")||orderStatus.equalsIgnoreCase("Received")) {
				ListUtil.addElementIfAbsent(orderList,orderNumber);
				Log.customer.debug("The Order array was updated with PO and its status is :"+orderStatus);
			}
		}
		Log.customer.debug("None of the split PO's has Invoice");


		String orderListString = ListUtil.listToCSVString(orderList);
		Log.customer.debug(" The comma seperated PO list "+orderListString);
		eform.setFieldValue("ValidBackOutOrders",orderListString);

		BaseSession session = Base.getSession();
		Partition p = Base.getSession().getPartition();

		String[]  poList = StringUtil.delimitedStringToArray(orderListString,',');
       	int length = poList.length;
        Log.customer.debug("Number of PO's valid for back out "+length);

        for(int n=0;n<length;n++) {
			String order = poList[n].trim();
			Log.customer.debug("Getting the Valid PO "+(n+1)+" : "+order+" from the list");
			PurchaseOrder dor1 = (PurchaseOrder)session.objectFromName(order,"ariba.purchasing.core.PurchaseOrder",p);
			Log.customer.debug("Getting the Valid PO "+dor1);

			for(Iterator iter = dor1.getReceiptsIterator(); iter.hasNext();) {
			    BaseId baseid = (BaseId)iter.next();
				Receipt rece = (Receipt)session.objectFromId(baseid);
				ReceivableLineItemCollection rlitem = (ReceivableLineItemCollection)rece.getOrder();

				String RecID = (String)rece.getDottedFieldValue("UniqueName");
				Log.customer.debug(" Receipt got : %s",RecID);

				String RecPO = (String)rece.getDottedFieldValue("Order.UniqueName");
				Log.customer.debug(" Order got : %s",RecPO);

				String RecStatus = (String)rece.getDottedFieldValue("StatusString");
				Log.customer.debug("Receipt Status is : "+RecStatus);

				// display the recceipt if it is in approved state only .

				if(RecStatus.equalsIgnoreCase("Approved")) {
					String finalPORec = RecPO.concat(" : ").concat(RecID);
					Log.customer.debug(" Displaying receipts that will be backed out");
					recePONum.add(finalPORec);
					Log.customer.debug(" Added the receipt "+RecID+" and Order "+RecPO+" to the recePO list");
				}
			}
		}

		eform.setFieldValue("ReceiptsInfo",ListUtil.listToCSVString(recePONum));

		// display the split order if any
		// Display all split orders as warning message
		if(orderList.size() > 1) {
			Log.customer.debug(" More than one line item for PR has to be backed out");
			// display the PO numbers in the UI

			String finalPOList = orderList.toString();
			Log.customer.debug(" THE Split order list : %s",finalPOList);
			eform.setFieldValue("ValidateWarningMessage","The Requisition associated with the Purchase Order you selected generated more that one Purchase"+
 								"Order "+finalPOList+ ". All receipts associated with these Purchase Orders will be backed out.");
			Log.customer.debug(" Validated Split PO list on Eform");
		}

		eform.setFieldValue("ValidateCheckMessage",orderListString);
		return false;
	}

    public CatValidatePOForBackOut() {}

    private static final String classname = "CatValidatePOForBackOut";
}