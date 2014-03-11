/*
 * Created by Amit on Aug 29, 2007
 * --------------------------------------------------------------
 * submit hook for receipt back out
 *
 *
 * Modified by Amit Kumar on 29th October 2007
 * Issue 732 - Added the code to include comments on the Receipt object as to who backed out the Corresponding Order
 *
 * Modified by Sudheer on 19 jan 2009 .
 * Issue 887 Receipt backout - LI amount decrease throws error.   Updated the received amount on the PO whenever receipt backout is executed.
 */
package config.java.receiptbackout;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import ariba.approvable.core.Approvable;
import ariba.approvable.core.ApprovableHook;
import ariba.approvable.core.Comment;
import ariba.base.core.Base;
import ariba.base.core.BaseId;
import ariba.base.core.BaseObject;
import ariba.base.core.BaseSession;
import ariba.base.core.BaseVector;
import ariba.base.core.ClusterRoot;
import ariba.base.core.LongString;
import ariba.base.core.Partition;
import ariba.base.core.aql.AQLOptions;
import ariba.base.core.aql.AQLQuery;
import ariba.base.core.aql.AQLResultCollection;
import ariba.base.fields.Fields;
import ariba.basic.core.Money;
import ariba.purchasing.core.POLineItem;
import ariba.purchasing.core.PurchaseOrder;
import ariba.receiving.core.Receipt;
import ariba.receiving.core.ReceiptItem;
import ariba.receiving.core.ReceivableLineItemCollection;
import ariba.user.core.User;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.log.Log;


public class CatReceiptBackOutSubmitHook implements ApprovableHook {

  	 List NoErrorResult = ListUtil.list(Constants.getInteger(0));
     List ErrorResult = ListUtil.list(Constants.getInteger(-1));
     private static final String nullPOError = ResourceService.getString("aml.cat.receiptbackout","nullPOError");
     private static final String poNotFoundError = ResourceService.getString("aml.cat.receiptbackout","poNotFoundError");
     private static final String poNotValidatedError = ResourceService.getString("aml.cat.receiptbackout","poNotValidatedError");

	 public List run(Approvable eform) {

		Partition p = Base.getSession().getPartition();
		//eform.setFieldValue("ValidateErrorMessage", null);
		//eform.setFieldValue("ValidateWarningMessage", null);

		Log.customer.debug("In the run method : %s",classname);
		String poNumber = (String)eform.getFieldValue("OrderNo");
		Log.customer.debug(" The PO Number is :"+poNumber);

		if(poNumber == null) {
			Log.customer.debug("PO Number not entered");
			return ListUtil.list(Constants.getInteger(-1),nullPOError);
		}

		poNumber=poNumber.toUpperCase(); // ocnvert the PO to uppercase

		String queryText = "SELECT obj from ariba.purchasing.core.DirectOrder AS obj WHERE OrderID='"+poNumber+"'";
		AQLQuery query1 = AQLQuery.parseQuery(queryText);
		AQLOptions opt =  new AQLOptions(Base.getSession().getPartition());
		Log.customer.debug("%s : query in here=%s", classname, query1.toString());
		AQLResultCollection result = Base.getService().executeQuery(query1, opt);

		if(!ListUtil.nullOrEmptyList(result.getErrors())) {
		     String erro = result.getErrorStatementText();
		     Log.customer.debug("%s: ERROR: %s",classname, erro);
		}

		if(result.getSize() == 0) {
			 Log.customer.debug("PO is not found");
			 return ListUtil.list(Constants.getInteger(-1),poNotFoundError);
		}

		while(result.next()) {
			BaseId objBaseId = (BaseId)result.getObject(0);
			ClusterRoot crt = (ClusterRoot)objBaseId.get();
        	PurchaseOrder dor = (PurchaseOrder)crt;


			String validateMessage = (String)eform.getFieldValue("ValidateCheckMessage");
			String validateButtonMessage = (String)eform.getFieldValue("CheckValidateButton");
			String errorMessage = (String)eform.getFieldValue("ValidateErrorMessage");

        	try {
				// check if validate button was clicked on the Eform
				if(validateButtonMessage == null) {
					Log.customer.debug("Validate Button was not clicked");
					return ListUtil.list(Constants.getInteger(-1),"Please Validate the Order by clicking Validate Button on the Eform.");
				}

				// Force the user to submit only Valid PO for receipt back out
           		if(validateMessage == null && errorMessage != null) {

					Log.customer.debug("Eform has an error message ");
					return ListUtil.list(Constants.getInteger(-1),"The Order is not valid for receipt back out due to the error: "+errorMessage+".Please submit a Valid Order for receipt back out.");

					//if(!(CatValidatePOForBackOut.validate(dor,eform))) {
					//	return ListUtil.list(Constants.getInteger(-1),poNotValidatedError);
					//}
				}

				// check if validate button was clicked on the Eform - else force them to click Validate button
				if(validateMessage == null && errorMessage == null) {
					Log.customer.debug("Validate Button was not clicked");
					return ListUtil.list(Constants.getInteger(-1),"Please Validate the PO by clicking Validate Button on the Eform.");
				}

				Log.customer.debug(" The PO was validated");

				if(validateMessage != null) {
          			BaseSession baseSession = Base.getSession();
        			String validPOList = (String)eform.getFieldValue("ValidBackOutOrders");
        			Log.customer.debug("The valid orders list for back out "+validPOList);

        			String[]  poList = StringUtil.delimitedStringToArray(validPOList,',');
        			int length = poList.length;
        			Log.customer.debug("Number of PO's valid for back out "+length);

        			for(int n=0;n<length;n++) {
						String order = poList[n].trim();
						Log.customer.debug("Getting the Valid PO "+(n+1)+" : "+order+" from the list");
						PurchaseOrder dor1 = (PurchaseOrder)baseSession.objectFromName(order,"ariba.purchasing.core.PurchaseOrder",p);
						ReceiptBackoutMethod(dor1,eform);
					}
					Log.customer.debug("Completed Receipt back out for all OrderList PO's");
				}
			}
			catch(Exception e) {
				eform.setFieldValue("ValidateErrorMessage", "ERROR: " +e.toString());
				Log.customer.debug("%s *** ERROR=%s", classname, e.toString());
				return ListUtil.list(Constants.getInteger(-1),e.toString());
			}
		}
		eform.setFieldValue("ValidateCheckMessage",null);
		eform.setFieldValue("ValidateErrorMessage", null);
		eform.setFieldValue("ValidateWarningMessage", null);
		Log.customer.debug("Out of Submit hook - receipt back out");
		ariba.base.core.Base.getSession().transactionCommit();
		return NoErrorResult;
	}

	public void ReceiptBackoutMethod(PurchaseOrder po,Approvable eform) {
			Log.customer.debug(" In the method ReceiptBackOutMethod : To back out the receipts");

			Log.customer.debug(" Validating the PO for back out"+po);
		try {

			BaseSession session = Base.getSession();
			List polineList = (List)po.getLineItems();

			boolean hasComposingReceipt = false;
 			for(Iterator iter = po.getReceiptsIterator(); iter.hasNext();) {
           	 	BaseId baseid = (BaseId)iter.next();
				Receipt rece = (Receipt)session.objectFromId(baseid);
				ReceivableLineItemCollection rlitem = (ReceivableLineItemCollection)rece.getOrder();

				String RecID = (String)rece.getDottedFieldValue("UniqueName");
				Log.customer.debug(" Receipt got : %s",RecID);

				String RecPO = (String)rece.getDottedFieldValue("Order.UniqueName");
				Log.customer.debug(" Order got : %s",RecPO);

				String RecStatus = (String)rece.getDottedFieldValue("StatusString");
				Log.customer.debug("Receipt Status is : "+RecStatus);

				// Back out the recceipt if it is in approved state only .

				if(RecStatus.equalsIgnoreCase("Approved")) {
					rece.setFieldValue("ProcessedState",new Integer(1));
					Log.customer.debug("ProcessedState for Receipt is set to 1");

					BaseVector RecItems = rece.getReceiptItems();
					for(int i=0;i<RecItems.size();i++) {
						ReceiptItem ri1 = (ReceiptItem)RecItems.get(i);

						ri1.setFieldValue("NumberPreviouslyAccepted",Constants.ZeroBigDecimal);
						Log.customer.debug(" NumberPreviouslyAccepted set to 0 for receipt");

						ri1.setFieldValue("NumberAccepted",Constants.ZeroBigDecimal);
						Log.customer.debug(" NumberAccepted set to 0 for receipt");

						ReceiptItem ri2 = (ReceiptItem)RecItems.get(i);
						ri2.setFieldValue("NumberRejected",Constants.ZeroBigDecimal);
						Log.customer.debug(" NumberRejected set to 0 for receipt");
					}
					//receNum.add(RecID);
				}

				if(RecStatus.equalsIgnoreCase("Composing")) {
					Log.customer.debug("Composing Receipt");
					hasComposingReceipt = true;
					BaseVector RecItemsComp = rece.getReceiptItems();
					for(int c=0;c<RecItemsComp.size();c++) {
					    ReceiptItem ric1 = (ReceiptItem)RecItemsComp.get(c);
					    ric1.setFieldValue("NumberPreviouslyAccepted",Constants.ZeroBigDecimal);
					    Log.customer.debug("NumberPreviouslyAccepted  set to 0 for receipt");
					}
				}
				po.setFieldValue("ReceivedState",new Integer(1));

                 // Code started by sudheer

				  Log.customer.debug("Setting Zero amount to Amount accepted on header level :CatReceiptBackOutSubmitHook");
				  Money amountAccepted = (Money)po.getFieldValue("AmountAccepted");
				  Log.customer.debug("Value of Ammount accepted on header level :"+amountAccepted+":CatReceiptBackOutSubmitHook");
				  if ( amountAccepted != null )  {
						BigDecimal amount = (BigDecimal)amountAccepted.getFieldValue("Amount");
						Log.customer.debug("Value of Ammount on header level before setting :" + amount+":CatReceiptBackOutSubmitHook");
						if ( amount != null && amount != Constants.ZeroBigDecimal ) {
				               amountAccepted.setFieldValue("Amount",Constants.ZeroBigDecimal);
				               }
				      }

                  //po.setFieldValue("AmountAccepted",Money.setAmount(Constants.ZeroBigDecimal));

                // Code ended by sudheer

				// Issue 732 - To add internal comments on the receipt giving details about the User who submitted the Eform
   	 			User eformPrep = eform.getPreparer();
   	 			Log.customer.debug("Preparer is :"+eformPrep);
    			String prepName = eformPrep.getMyName();//FieldValue("Name.PrimaryString");
    			Log.customer.debug(" Inter Comments Part - Preparer is :"+prepName);
    			String note = "The receipt was backed out using ReceiptBackOut Eform submitted by "+prepName;
    			String title = "MSC Administrator";
    			addComment(rece,note,title);
    			rece.setLastModified(Fields.getService().getNow());
				Log.customer.debug("%s: set the comment, modifieddate and save", classname);
        		rece.save();


			}

                // S. Sato - need to create a composing receipt if there are none..
            if (!hasComposingReceipt) {
                po.openReceipt();
            }

			Log.customer.debug("back out done");

			  Money amountRejected = null;
			  Money amountAccepted = null;

			for(int l=0;l<polineList.size();l++) {
				Log.customer.debug("Got PO Line :"+l);
				POLineItem pol = (POLineItem)polineList.get(l);
				pol.setFieldValue("NumberAccepted",Constants.ZeroBigDecimal);
				pol.setFieldValue("NumberRejected",Constants.ZeroBigDecimal);

				// Code started by sudheer
				// Setting Amountaccepted to zero on Line item level

				Log.customer.debug("Setting Zero amount to Ammount accepted on Line Item level :CatReceiptBackOutSubmitHook");
				amountAccepted = (Money)pol.getFieldValue("AmountAccepted");
				Log.customer.debug("Value of Amount accepted on Line Item level :"+amountAccepted+":CatReceiptBackOutSubmitHook");
				if ( amountAccepted != null ) {
					  BigDecimal amount = (BigDecimal)amountAccepted.getFieldValue("Amount");
					  Log.customer.debug("Value of Ammount on Line Item level before setting :"+amount+"CatReceiptBackOutSubmitHook");
					  if ( amount != null && amount != Constants.ZeroBigDecimal )  {
				               amountAccepted.setFieldValue("Amount",Constants.ZeroBigDecimal);

						}
				    }

				 // Setting AmountRejected to zero on Line item level

				 Log.customer.debug("Setting Zero amount to Ammount Rejected on Line Item level :");
				 amountRejected = (Money)pol.getFieldValue("AmountRejected");
				 Log.customer.debug("Value of Amount Rejected on Line Item level :" + amountRejected);
				 if ( amountAccepted != null )
				    {
				 	  BigDecimal amount = (BigDecimal)amountRejected.getFieldValue("Amount");
				 	  Log.customer.debug("Value of Amount on Line Item level before setting :" + amount+"CatReceiptBackOutSubmitHook");
				 	  if ( amount != null && amount != Constants.ZeroBigDecimal )
				 	       {
				                amountRejected.setFieldValue("Amount",Constants.ZeroBigDecimal);

				 			}
				     }

               // Code ended by sudheer
			}
		}
		catch(Exception e){
			eform.setFieldValue("ValidateErrorMessage", "ERROR: " +e.toString());
			Log.customer.debug("%s *** ERROR=%s", classname, e.toString());
			return;
		}
	}

    private void addComment(Approvable app, String note, String title) {
		Log.customer.debug("%s: addComment=%s", classname, note);
		Comment comment = new Comment (app.getPartition());
		comment.setType(1);
		comment.setDate(Fields.getService().getNow());
		comment.setUser(User.getAribaSystemUser(app.getPartition()));
		comment.setText(new LongString(note));
		comment.setTitle(title);
		comment.setExternalComment(false);
		comment.setParent((BaseObject)app);
		app.getComments().add(comment);
    }

    private static final String classname = "CatReceiptBackOutSubmitHook";
}